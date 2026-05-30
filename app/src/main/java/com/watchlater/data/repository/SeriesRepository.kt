package com.watchlater.data.repository

import android.util.Log
import com.watchlater.data.local.EpisodeProgress
import com.watchlater.data.local.EpisodeProgressDao
import com.watchlater.data.local.SeasonEpisode
import com.watchlater.data.local.SeasonEpisodeDao
import com.watchlater.data.local.SeriesCache
import com.watchlater.data.local.SeriesCacheDao
import com.watchlater.data.remote.TmdbRepository
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
    private val omdbRepository: TmdbRepository
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

    // ── Season fetching (shared helper) ───────────────────────────────────

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
     * Returns [EpisodeValidationResult.Allowed] if the episode can be marked watched.
     * Returns [EpisodeValidationResult.Blocked] with a user-friendly message if a
     * previous season is not fully watched yet.
     */
    suspend fun validateEpisodeMarkable(
        watchItemId: Long,
        imdbId: String,
        targetSeason: Int
    ): EpisodeValidationResult {
        if (targetSeason <= 1) return EpisodeValidationResult.Allowed
        for (prevSeason in 1 until targetSeason) {
            val episodes = seasonEpisodeDao.getSeason(imdbId, prevSeason)
            if (episodes.isEmpty()) continue // not cached — give benefit of the doubt
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

    /** Explicit set (not toggle) — used by optimistic UI updates */
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

    /** Legacy toggle — kept for back-compat */
    suspend fun toggleEpisode(watchItemId: Long, season: Int, episodeNumber: Int): Boolean {
        val current = episodeProgressDao.get(watchItemId, season, episodeNumber)
        val nowWatched = !(current?.isWatched ?: false)
        setEpisodeWatched(watchItemId, season, episodeNumber, nowWatched)
        return nowWatched
    }

    suspend fun markSeasonWatched(watchItemId: Long, imdbId: String, season: Int) {
        val episodes = seasonEpisodeDao.getSeason(imdbId, season)
        val now = System.currentTimeMillis()
        episodes.forEach { ep ->
            episodeProgressDao.upsert(
                EpisodeProgress(
                    watchItemId   = watchItemId,
                    season        = season,
                    episodeNumber = ep.episodeNumber,
                    isWatched     = true,
                    watchedAt     = now
                )
            )
        }
    }

    /**
     * Marks every episode of every season as watched.
     * Fetches and caches season data if not already present.
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
            episodes.forEach { ep ->
                episodeProgressDao.upsert(
                    EpisodeProgress(
                        watchItemId   = watchItemId,
                        season        = season,
                        episodeNumber = ep.episodeNumber,
                        isWatched     = true,
                        watchedAt     = now
                    )
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

    suspend fun deleteProgress(watchItemId: Long) {
        episodeProgressDao.deleteAll(watchItemId)
    }
}
