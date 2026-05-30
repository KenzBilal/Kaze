package com.watchlater.data.remote

import android.util.Log
import com.watchlater.BuildConfig
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class OmdbRepository {

    val api: OmdbApi by lazy {
        val logging = HttpLoggingInterceptor { Log.d("OmdbRepo", it) }
            .apply { level = HttpLoggingInterceptor.Level.BASIC }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://www.omdbapi.com/")
            .client(client)
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
            Log.d("OmdbRepo", "Search '$query' → response=${response.response} error=${response.error}")
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
        } catch (e: CancellationException) {
            Log.d("OmdbRepo", "Search '$query' cancelled"); throw e
        } catch (e: Exception) {
            Log.e("OmdbRepo", "Search '$query' failed: ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }
    }

    suspend fun fetchGenre(omdbId: String): String {
        if (!hasApiKey || omdbId.isBlank()) return ""
        return try {
            val detail = api.getDetail(imdbId = omdbId, apiKey = apiKey)
            detail.genre?.takeIf { it != "N/A" } ?: ""
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { "" }
    }

    /** Returns total seasons for a series, or 0 on failure */
    suspend fun fetchTotalSeasons(imdbId: String): Int {
        if (!hasApiKey || imdbId.isBlank()) return 0
        return try {
            val detail = api.getDetail(imdbId = imdbId, apiKey = apiKey)
            detail.totalSeasons?.toIntOrNull() ?: 0
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            Log.e("OmdbRepo", "fetchTotalSeasons failed: ${e.message}"); 0
        }
    }

    /** Returns episode list for one season */
    suspend fun fetchSeason(imdbId: String, season: Int): OmdbSeasonResponse? {
        if (!hasApiKey || imdbId.isBlank()) return null
        return try {
            val result = api.getSeason(imdbId = imdbId, season = season, apiKey = apiKey)
            if (result.response == "True") result else null
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            Log.e("OmdbRepo", "fetchSeason $season failed: ${e.message}"); null
        }
    }
}
