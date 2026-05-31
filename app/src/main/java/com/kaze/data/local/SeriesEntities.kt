package com.kaze.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Series-level metadata cached from OMDB — total seasons, etc. */
@Entity(tableName = "series_cache")
data class SeriesCache(
    @PrimaryKey val imdbId: String,
    val title: String,
    val totalSeasons: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

/** One episode in a season, cached from OMDB */
@Entity(
    tableName = "season_episodes",
    primaryKeys = ["imdbId", "season", "episodeNumber"]
)
data class SeasonEpisode(
    val imdbId: String,
    val season: Int,
    val episodeNumber: Int,
    val title: String,
    val released: String = "",      // "2008-01-20" or "N/A"
    val imdbRating: String = "",    // "9.0" or "N/A"
    val cachedAt: Long = System.currentTimeMillis()
)

/** Per-user episode watched progress — stored forever, never expires */
@Entity(
    tableName = "episode_progress",
    primaryKeys = ["watchItemId", "season", "episodeNumber"]
)
data class EpisodeProgress(
    val watchItemId: Long,
    val season: Int,
    val episodeNumber: Int,
    val isWatched: Boolean = false,
    val watchedAt: Long? = null
)
