package com.watchlater.di

import android.content.Context
import com.watchlater.data.local.WatchLaterDatabase
import com.watchlater.data.remote.TmdbRepository
import com.watchlater.data.repository.SeriesRepository
import com.watchlater.data.repository.WatchItemRepository
import com.watchlater.updater.UpdateManager

class AppContainer(context: Context) {
    val updateManager    = UpdateManager(context)
    private val database = WatchLaterDatabase.getInstance(context)
    val repository       = WatchItemRepository(database.watchItemDao())
    val tmdbRepository   = TmdbRepository()
    val seriesRepository = SeriesRepository(
        seriesCacheDao    = database.seriesCacheDao(),
        seasonEpisodeDao  = database.seasonEpisodeDao(),
        episodeProgressDao = database.episodeProgressDao(),
        omdbRepository    = tmdbRepository
    )
}
