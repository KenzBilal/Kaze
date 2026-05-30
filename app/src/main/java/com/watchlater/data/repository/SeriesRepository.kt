package com.watchlater.data.repository

import android.util.Log
import com.watchlater.data.local.EpisodeProgress
import com.watchlater.data.local.EpisodeProgressDao
import com.watchlater.data.local.SeasonEpisode
import com.watchlater.data.local.SeasonEpisodeDao
import com.watchlater.data.local.SeriesCache
import com.watchlater.data.local.SeriesCacheDao
import com.watchlater.data.remote.OmdbRepository
import kotlinx.coroutines.flow.Flow

private const val CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1000 // 30 days

data class EpisodeUiItem(
    val season: Int,
    val episodeNumber: Int,
    val title: String,
    val released: String,
    val imdbRating: String,
    val isWatched: Boolean
)

/** Result of episode validation — null = allowed, non-null = user-friendly error */
sealed class EpisodeValidationResult {
    object Allowed : EpisodeValidationResult()
    data class Blocked(val reason: String) : EpisodeValidationResult()
}

class SeriesRepository(
    private val seriesCacheDao: SeriesCacheDao,
    private val seasonEpisodeDao: SeasonEpisodeDao,
    private val episodeProgressDao: EpisodeProgressDao,
    private val omdbRepository: OmdbRepository
) {

    // ── Series metadata ────────────────────────────────────────────────────

    suspend fun getTotalSeasons(imdbId: String, title: String): Int {
        if (imdbId.isBlank()) return 0
        val cached = seriesCacheDao.get(imdbId)
        if (cached != null && System.currentTimeMillis() - cached.cachedAt < CACHE_TTL_MS) {
            return cached.totalSeasons
        }
        val total = omdbRepository.fetchTotalSeasons(imdbId)
        if (total > 0) {
            seriesCacheDao.insert(SeriesCache(imdbId = imdbId, title = title, totalSeasons = total))
        }
        return total
    }

    // ── Season fetching ────────────────────────────────────────────────────

    private suspend fun fetchAndCacheSeasonIfNeeded(imdbId: String, season: Int): List<SeasonEpisode> {
        val cachedAt = seasonEpisodeDao.getCachedAt(imdbId, season)
        val valid    = cachedAt != null && System.currentTimeMillis() - cachedAt < CACHE_TTL_MS
        if (!valid) {
            val response = omdbRepository.fetchSeason(imdbId, season)
            response?.episodes?.mapNotNull { ep ->
                val num = ep.episode.toIntOrNull() ?: return@mapNotNull null
                SeasonEpisode(
                    imdbId        = imdbId,
                    season        = season,
                    episodeNumber = num,
                    title         = ep.title,
                    released      = ep.released.takeIf { it != "N/A" } ?: "",
                    imdbRating    = ep.imdbRating.takeIf { it != "N/A" } ?: ""
                )
            }?.let {
                seasonEpisodeDao.insertAll(it)
                Log.d("SeriesRepo", "Cached ${it.size} eps for S$season")
            }
        }
        return seasonEpisodeDao.getSeason(imdbId, season)
    }

    suspend fun getSeasonEpisodes(
        imdbId: String,
        season: Int,
        watchItemId: Long
    ): List<EpisodeUiItem> {
        if (imdbId.isBlank()) return emptyList()
        val episodes = fetchAndCacheSeasonIfNeeded(imdbId, season)
        val progress = episodeProgressDao.getSeason(watchItemId, season)
            .associateBy { it.episodeNumber }
        return episodes.map { ep ->
            EpisodeUiItem(
                season        = ep.season,
                episodeNumber = ep.episodeNumber,
                title         = ep.title,
                released      = ep.released,
                imdbRating    = ep.imdbRating,
                isWatched     = progress[ep.episodeNumber]?.isWatched ?: false
            )
        }
    }

    // ── Validation ─────────────────────────────────────────────────────────

    /**
     * Validates whether an episode in [targetSeason] can be marked watched.
     *
     * FIX (BUG-04): If a previous season is NOT cached we now attempt to fetch it.
     * If the fetch fails (offline / no imdbId), we return Blocked rather than silently
     * bypassing enforcement — this prevents users from skipping seasons offline.
     */
    suspend fun validateEpisodeMarkable(
        watchItemId: Long,
        imdbId: String,
        targetSeason: Int
    ): EpisodeValidationResult {
        if (targetSeason <= 1) return EpisodeValidationResult.Allowed
        for (prevSeason in 1 until targetSeason) {
            // Fetch and cache if needed so we always validate against real data
            val episodes = fetchAndCacheSeasonIfNeeded(imdbId, prevSeason)
            if (episodes.isEmpty()) {
                // Cache fetch failed (offline / invalid imdbId) — block to be safe
                return EpisodeValidationResult.Blocked(
                    "Cannot verify Season $prevSeason progress. Check your connection and try again."
                )
            }
            val progress = episodeProgressDao.getSeason(watchItemId, prevSeason)
                .associateBy { it.episodeNumber }
            val watched = episodes.count { progress[it.episodeNumber]?.isWatched == true }
            val total   = episodes.size
            if (watched < total) {
                return EpisodeValidationResult.Blocked(
                    "Finish Season $prevSeason first — $watched of $total episodes watched"
                )
            }
        }
        return EpisodeValidationResult.Allowed
    }

    // ── Progress writes ────────────────────────────────────────────────────

    fun observeProgress(watchItemId: Long): Flow<List<EpisodeProgress>> =
        episodeProgressDao.observeAll(watchItemId)

    suspend fun setEpisodeWatched(
        watchItemId: Long,
        season: Int,
        episodeNumber: Int,
        watched: Boolean
    ) {
        episodeProgressDao.upsert(
            EpisodeProgress(
                watchItemId   = watchItemId,
                season        = season,
                episodeNumber = episodeNumber,
                isWatched     = watched,
                watchedAt     = if (watched) System.currentTimeMillis() else null
            )
        )
    }

    suspend fun toggleEpisode(watchItemId: Long, season: Int, episodeNumber: Int): Boolean {
        val current    = episodeProgressDao.get(watchItemId, season, episodeNumber)
        val nowWatched = !(current?.isWatched ?: false)
        setEpisodeWatched(watchItemId, season, episodeNumber, nowWatched)
        return nowWatched
    }

    /**
     * Marks all episodes in a season as watched in a single transaction.
     *
     * FIX (BUG-08): Returns false and leaves DB unchanged if season is not cached
     * (or fetch failed) so callers can show a meaningful error.
     * FIX (P2): Uses upsertAll() @Transaction instead of N individual upsert() calls.
     */
    suspend fun markSeasonWatched(watchItemId: Long, imdbId: String, season: Int): Boolean {
        val episodes = fetchAndCacheSeasonIfNeeded(imdbId, season)
        if (episodes.isEmpty()) return false   // offline / not found — tell caller
        val now = System.currentTimeMillis()
        episodeProgressDao.upsertAll(
            episodes.map { ep ->
                EpisodeProgress(
                    watchItemId   = watchItemId,
                    season        = season,
                    episodeNumber = ep.episodeNumber,
                    isWatched     = true,
                    watchedAt     = now
                )
            }
        )
        return true
    }

    /**
     * Marks every episode of every season as watched.
     * FIX (P3): Uses upsertAll() @Transaction per season instead of N individual upserts.
     */
    suspend fun markAllSeriesWatched(
        watchItemId: Long,
        imdbId: String,
        totalSeasons: Int,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ) {
        val now = System.currentTimeMillis()
        for (season in 1..totalSeasons) {
            onProgress(season, totalSeasons)
            val episodes = fetchAndCacheSeasonIfNeeded(imdbId, season)
            if (episodes.isNotEmpty()) {
                episodeProgressDao.upsertAll(
                    episodes.map { ep ->
                        EpisodeProgress(
                            watchItemId   = watchItemId,
                            season        = season,
                            episodeNumber = ep.episodeNumber,
                            isWatched     = true,
                            watchedAt     = now
                        )
                    }
                )
            }
            Log.d("SeriesRepo", "Marked S$season fully watched (${episodes.size} eps)")
        }
    }

    /** Returns next unwatched episode across all seasons, or null if all done */
    suspend fun nextUnwatched(
        watchItemId: Long,
        imdbId: String,
        totalSeasons: Int
    ): Pair<Int, Int>? {
        for (s in 1..totalSeasons) {
            val episodes = seasonEpisodeDao.getSeason(imdbId, s)
            val progress = episodeProgressDao.getSeason(watchItemId, s)
                .associateBy { it.episodeNumber }
            val next = episodes.firstOrNull { !(progress[it.episodeNumber]?.isWatched ?: false) }
            if (next != null) return Pair(next.season, next.episodeNumber)
        }
        return null
    }

    /**
     * Returns the last episode of the last season — used by auto-advance when series
     * is fully watched (BUG-05 fix: don't rely on currently displayed season).
     */
    suspend fun lastEpisodeOfSeries(imdbId: String, totalSeasons: Int): Pair<Int, Int>? {
        if (totalSeasons <= 0) return null
        val episodes = seasonEpisodeDao.getSeason(imdbId, totalSeasons)
        val last = episodes.maxByOrNull { it.episodeNumber } ?: return null
        return Pair(totalSeasons, last.episodeNumber)
    }

    suspend fun deleteProgress(watchItemId: Long) {
        episodeProgressDao.deleteAll(watchItemId)
    }
}
