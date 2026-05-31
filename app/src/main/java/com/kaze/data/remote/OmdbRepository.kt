package com.kaze.data.remote

import android.util.Log
import com.kaze.BuildConfig
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class OmdbRepository {

    val api: OmdbApi by lazy {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)

        // S2 fix: only log in debug builds — API key is part of the URL
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor { Log.d("OmdbRepo", it) }
                .apply { level = HttpLoggingInterceptor.Level.BASIC }
            clientBuilder.addInterceptor(logging)
        }

        Retrofit.Builder()
            .baseUrl("https://www.omdbapi.com/")
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OmdbApi::class.java)
    }

    val hasApiKey: Boolean get() = BuildConfig.OMDB_API_KEY.isNotBlank()
    val apiKey: String get() = BuildConfig.OMDB_API_KEY

    suspend fun search(query: String): List<OmdbResult> {
        if (!hasApiKey || query.isBlank()) return emptyList()
        return try {
            val response = api.search(query = query, apiKey = apiKey)
            if (response.response != "True" || response.results == null) return emptyList()
            response.results
                .filter { it.type == "movie" || it.type == "series" }
                .take(8)
                .map { item ->
                    OmdbResult(
                        displayTitle = item.title,
                        displayYear  = item.year.take(4).toIntOrNull() ?: 0,
                        mediaType    = if (item.type == "series") "tv" else "movie",
                        posterUrl    = item.poster.takeIf { it != "N/A" },
                        omdbId       = item.imdbId
                    )
                }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("OmdbRepo", "Search '$query' failed: ${e.message}")
            throw e
        }
    }

    /**
     * A3 fix: Consolidated fetchGenre + fetchTotalSeasons into a single network call.
     * Returns a Pair of (genres string, totalSeasons int).
     */
    suspend fun fetchDetail(omdbId: String): Pair<String, Int> {
        if (!hasApiKey || omdbId.isBlank()) return "" to 0
        return try {
            val detail = api.getDetail(imdbId = omdbId, apiKey = apiKey)
            val genres = detail.genre?.takeIf { it != "N/A" } ?: ""
            val seasons = detail.totalSeasons?.toIntOrNull() ?: 0
            genres to seasons
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("OmdbRepo", "fetchDetail failed: ${e.message}")
            "" to 0
        }
    }

    /** Returns genres string — single call wrapper over fetchDetail */
    suspend fun fetchGenre(omdbId: String): String = fetchDetail(omdbId).first

    /** Returns total seasons for a series, or 0 on failure */
    suspend fun fetchTotalSeasons(imdbId: String): Int = fetchDetail(imdbId).second

    /** Returns episode list for one season */
    suspend fun fetchSeason(imdbId: String, season: Int): OmdbSeasonResponse? {
        if (!hasApiKey || imdbId.isBlank()) return null
        return try {
            val result = api.getSeason(imdbId = imdbId, season = season, apiKey = apiKey)
            if (result.response == "True") result else null
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("OmdbRepo", "fetchSeason $season failed: ${e.message}")
            null
        }
    }
}
