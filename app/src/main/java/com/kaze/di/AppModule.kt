package com.kaze.di

import android.content.Context
import com.kaze.data.local.WatchLaterDatabase
import com.kaze.data.local.WatchItemDao
import com.kaze.data.local.EpisodeProgressDao
import com.kaze.data.local.SeriesCacheDao
import com.kaze.data.local.SeasonEpisodeDao
import com.kaze.data.local.CastCacheDao
import com.kaze.data.local.WhatToWatchDao
import com.kaze.data.remote.SupabaseApi
import io.github.jan.supabase.SupabaseClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWatchLaterDatabase(@ApplicationContext context: Context): WatchLaterDatabase {
        return WatchLaterDatabase.getInstance(context)
    }

    @Provides
    fun provideWatchItemDao(database: WatchLaterDatabase): WatchItemDao = database.watchItemDao()

    @Provides
    fun provideEpisodeProgressDao(database: WatchLaterDatabase): EpisodeProgressDao = database.episodeProgressDao()

    @Provides
    fun provideSeriesCacheDao(database: WatchLaterDatabase): SeriesCacheDao = database.seriesCacheDao()

    @Provides
    fun provideSeasonEpisodeDao(database: WatchLaterDatabase): SeasonEpisodeDao = database.seasonEpisodeDao()

    @Provides
    fun provideCastCacheDao(database: WatchLaterDatabase): CastCacheDao = database.castCacheDao()

    @Provides
    fun provideWhatToWatchDao(database: WatchLaterDatabase): WhatToWatchDao = database.whatToWatchDao()

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return SupabaseApi.client
    }
}
