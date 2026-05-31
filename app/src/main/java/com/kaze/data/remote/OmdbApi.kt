package com.kaze.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OmdbApi {

    /** Search by title */
    @GET("/")
    suspend fun search(
        @Query("s")      query: String,
        @Query("apikey") apiKey: String
    ): OmdbSearchResponse

    /** Get item detail — genre + totalSeasons (for series) */
    @GET("/")
    suspend fun getDetail(
        @Query("i")      imdbId: String,
        @Query("apikey") apiKey: String
    ): OmdbDetailResponse

    /** Get all episodes for a specific season */
    @GET("/")
    suspend fun getSeason(
        @Query("i")      imdbId: String,
        @Query("Season") season: Int,
        @Query("apikey") apiKey: String
    ): OmdbSeasonResponse
}
