package com.watchlater.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── SeriesCache DAO ────────────────────────────────────────────────────────

@Dao
interface SeriesCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: SeriesCache)

    @Query("SELECT * FROM series_cache WHERE imdbId = :imdbId")
    suspend fun get(imdbId: String): SeriesCache?

    @Query("DELETE FROM series_cache WHERE imdbId = :imdbId")
    suspend fun delete(imdbId: String)
}

// ── SeasonEpisode DAO ──────────────────────────────────────────────────────

@Dao
interface SeasonEpisodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<SeasonEpisode>)

    @Query("SELECT * FROM season_episodes WHERE imdbId = :imdbId AND season = :season ORDER BY episodeNumber ASC")
    suspend fun getSeason(imdbId: String, season: Int): List<SeasonEpisode>

    @Query("SELECT cachedAt FROM season_episodes WHERE imdbId = :imdbId AND season = :season LIMIT 1")
    suspend fun getCachedAt(imdbId: String, season: Int): Long?

    @Query("DELETE FROM season_episodes WHERE imdbId = :imdbId")
    suspend fun deleteAll(imdbId: String)
}

// ── EpisodeProgress DAO ────────────────────────────────────────────────────

@Dao
interface EpisodeProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: EpisodeProgress)

    @Query("SELECT * FROM episode_progress WHERE watchItemId = :watchItemId ORDER BY season ASC, episodeNumber ASC")
    fun observeAll(watchItemId: Long): Flow<List<EpisodeProgress>>

    @Query("SELECT * FROM episode_progress WHERE watchItemId = :watchItemId AND season = :season ORDER BY episodeNumber ASC")
    suspend fun getSeason(watchItemId: Long, season: Int): List<EpisodeProgress>

    @Query("SELECT * FROM episode_progress WHERE watchItemId = :watchItemId AND season = :season AND episodeNumber = :ep LIMIT 1")
    suspend fun get(watchItemId: Long, season: Int, ep: Int): EpisodeProgress?

    @Query("DELETE FROM episode_progress WHERE watchItemId = :watchItemId")
    suspend fun deleteAll(watchItemId: Long)
}
