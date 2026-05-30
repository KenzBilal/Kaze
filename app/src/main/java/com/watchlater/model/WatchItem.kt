package com.watchlater.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType { MOVIE, SERIES }

@Entity(tableName = "watch_items")
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
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val genreList: List<String> get() =
        genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
