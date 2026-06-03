package com.kaze.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

interface TraktApi {
    @GET("movies/trending")
    suspend fun getTrendingMovies(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Header("trakt-api-version") version: String = "2",
        @Header("trakt-api-key") clientId: String
    ): List<TraktTrendingMovieResponse>

    @GET("shows/trending")
    suspend fun getTrendingShows(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Header("trakt-api-version") version: String = "2",
        @Header("trakt-api-key") clientId: String
    ): List<TraktTrendingShowResponse>
}
