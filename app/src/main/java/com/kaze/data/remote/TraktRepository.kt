package com.kaze.data.remote

import android.util.Log
import com.kaze.BuildConfig
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class TraktRepository {

    private val api: TraktApi by lazy {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor { Log.d("TraktRepo", it) }
                .apply { level = HttpLoggingInterceptor.Level.BASIC }
            clientBuilder.addInterceptor(logging)
        }

        Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TraktApi::class.java)
    }

    private val clientId: String get() = BuildConfig.TRAKT_CLIENT_ID

    suspend fun getTrendingMovies(page: Int = 1, limit: Int = 20): List<TraktMovie> {
        return try {
            val response = api.getTrendingMovies(page = page, limit = limit, clientId = clientId)
            response.map { it.movie }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("TraktRepo", "getTrendingMovies failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun getTrendingShows(page: Int = 1, limit: Int = 20): List<TraktShow> {
        return try {
            val response = api.getTrendingShows(page = page, limit = limit, clientId = clientId)
            response.map { it.show }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("TraktRepo", "getTrendingShows failed: ${e.message}")
            emptyList()
        }
    }
}
