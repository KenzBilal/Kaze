package com.kaze.data.remote

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

// ── OMDB Search Response ───────────────────────────────────────────────────

@Keep
data class OmdbSearchResponse(
    @SerializedName("Search")   val results: List<OmdbSearchItem>? = null,
    @SerializedName("Response") val response: String = "False",
    @SerializedName("Error")    val error: String? = null
)

@Keep
data class OmdbSearchItem(
    @SerializedName("Title")  val title: String,
    @SerializedName("Year")   val year: String,
    @SerializedName("imdbID") val imdbId: String,
    @SerializedName("Type")   val type: String,    // "movie", "series", "episode"
    @SerializedName("Poster") val poster: String   // full URL or "N/A"
)

// ── OMDB Detail / Series Info Response ────────────────────────────────────

@Keep
data class OmdbDetailResponse(
    @SerializedName("Genre")        val genre: String?,
    @SerializedName("totalSeasons") val totalSeasons: String?,  // "5" or null
    @SerializedName("Year")         val year: String?,          // "2008-2013" or "2021-"
    @SerializedName("Poster")       val poster: String?,
    @SerializedName("imdbRating")   val imdbRating: String?,
    @SerializedName("Plot")         val plot: String?,
    @SerializedName("Response")     val response: String = "False"
)

// ── OMDB Season Response ───────────────────────────────────────────────────

@Keep
data class OmdbSeasonResponse(
    @SerializedName("Season")       val season: String?,
    @SerializedName("totalSeasons") val totalSeasons: String?,
    @SerializedName("Episodes")     val episodes: List<OmdbEpisodeItem>? = null,
    @SerializedName("Response")     val response: String = "False"
)

@Keep
data class OmdbEpisodeItem(
    @SerializedName("Title")      val title: String,
    @SerializedName("Released")   val released: String,    // "2008-01-20" or "N/A"
    @SerializedName("Episode")    val episode: String,     // "1"
    @SerializedName("imdbRating") val imdbRating: String,  // "9.0" or "N/A"
    @SerializedName("imdbID")     val imdbId: String
)

// ── Shared Search Result (used by AddItemViewModel + AddItemSheet) ─────────

@Keep
data class OmdbResult(
    val displayTitle: String,
    val displayYear: Int,
    val mediaType: String,   // "movie" or "tv"
    val posterUrl: String?,
    val omdbId: String = "",
    val omdbGenre: String = ""
)
