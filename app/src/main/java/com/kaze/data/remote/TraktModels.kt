package com.kaze.data.remote

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

// ── Trakt Trending Response Models ────────────────────────────────────────────

@Keep
data class TraktTrendingMovieResponse(
    @SerializedName("watchers") val watchers: Int = 0,
    @SerializedName("movie") val movie: TraktMovie
)

@Keep
data class TraktMovie(
    @SerializedName("title") val title: String,
    @SerializedName("year") val year: Int?,
    @SerializedName("ids") val ids: TraktIds
)

@Keep
data class TraktTrendingShowResponse(
    @SerializedName("watchers") val watchers: Int = 0,
    @SerializedName("show") val show: TraktShow
)

@Keep
data class TraktShow(
    @SerializedName("title") val title: String,
    @SerializedName("year") val year: Int?,
    @SerializedName("ids") val ids: TraktIds
)

@Keep
data class TraktIds(
    @SerializedName("imdb") val imdb: String?,
    @SerializedName("trakt") val trakt: Int,
    @SerializedName("slug") val slug: String? = null
)

// ── Trakt Summary Response (for trailer URL) ──────────────────────────────────

@Keep
data class TraktMovieSummary(
    @SerializedName("title")   val title: String = "",
    @SerializedName("trailer") val trailer: String? = null
)

@Keep
data class TraktShowSummary(
    @SerializedName("title")   val title: String = "",
    @SerializedName("trailer") val trailer: String? = null
)

// ── Unified Discover Item ─────────────────────────────────────────────────────

data class DiscoverItem(
    val title: String,
    val year: Int,
    val type: String,       // "MOVIE" or "SERIES"
    val imdbId: String,
    val posterUrl: String?,
    val rating: Float = 0f,
    val notes: String = "",
    val genres: String = "",
    val season: Int = 1,
    val episode: Int = 1
)
