package com.kaze.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import androidx.annotation.Keep

@Keep
enum class MediaType { MOVIE, SERIES }

@Entity(
    tableName = "watch_items",
    indices = [
        Index(value = ["isWatched"]),
        Index(value = ["type"]),
        Index(value = ["dateAdded"]),
        Index(value = ["imdbId"])
    ]
)
data class WatchItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val year: Int,
    val type: MediaType,
    val isWatched: Boolean = false,
    val rating: Float = 0f,
    val season: Int? = null,          // current watching position (series)
    val episode: Int? = null,         // current watching position (series)
    val notes: String = "",
    val posterUrl: String? = null,
    val genres: String = "",
    val imdbId: String = "",          // OMDB imdbID — used to fetch series data
    val dateAdded: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val plot: String = "",            // Short plot (series) or full plot (movie)
    val trailerUrl: String = ""       // YouTube trailer URL from Trakt
) {
    val genreList: List<String> get() =
        genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

