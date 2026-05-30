package com.watchlater.data.remote

import com.google.gson.annotations.SerializedName

// ── OMDB Search Response ───────────────────────────────────────────────────

data class OmdbSearchResponse(
    @SerializedName("Search")   val results: List<OmdbSearchItem>? = null,
    @SerializedName("Response") val response: String = "False",
    @SerializedName("Error")    val error: String? = null
)

data class OmdbSearchItem(
    @SerializedName("Title")  val title: String,
    @SerializedName("Year")   val year: String,
    @SerializedName("imdbID") val imdbId: String,
    @SerializedName("Type")   val type: String,    // "movie", "series", "episode"
    @SerializedName("Poster") val poster: String   // full URL or "N/A"
)

// ── OMDB Detail / Series Info Response ────────────────────────────────────

data class OmdbDetailResponse(
    @SerializedName("Genre")        val genre: String?,
    @SerializedName("totalSeasons") val totalSeasons: String?,  // "5" or null
    @SerializedName("Response")     val response: String = "False"
)

// ── OMDB Season Response ───────────────────────────────────────────────────

data class OmdbSeasonResponse(
    @SerializedName("Season")       val season: String?,
    @SerializedName("totalSeasons") val totalSeasons: String?,
    @SerializedName("Episodes")     val episodes: List<OmdbEpisodeItem>? = null,
    @SerializedName("Response")     val response: String = "False"
)

data class OmdbEpisodeItem(
    @SerializedName("Title")      val title: String,
    @SerializedName("Released")   val released: String,    // "2008-01-20" or "N/A"
    @SerializedName("Episode")    val episode: String,     // "1"
    @SerializedName("imdbRating") val imdbRating: String,  // "9.0" or "N/A"
    @SerializedName("imdbID")     val imdbId: String
)

// ── Shared Search Result (used by AddItemViewModel + AddItemSheet) ─────────

data class OmdbResult(
    val displayTitle: String,
    val displayYear: Int,
    val mediaType: String,   // "movie" or "tv"
    val posterUrl: String?,
    val omdbId: String = "",
    val omdbGenre: String = ""
)
