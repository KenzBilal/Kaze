package com.watchlater.di

import android.content.Context
import com.watchlater.data.local.WatchLaterDatabase
import com.watchlater.data.remote.OmdbRepository
import com.watchlater.data.repository.SeriesRepository
import com.watchlater.data.repository.WatchItemRepository
import com.watchlater.updater.UpdateManager
import com.watchlater.utils.BackupManager

class AppContainer(context: Context) {
    val updateManager    = UpdateManager(context)
    private val database = WatchLaterDatabase.getInstance(context)
    val omdbRepository   = OmdbRepository()

    val repository = WatchItemRepository(
        dao                = database.watchItemDao(),
        episodeProgressDao = database.episodeProgressDao()
    )

    val seriesRepository = SeriesRepository(
        seriesCacheDao     = database.seriesCacheDao(),
        seasonEpisodeDao   = database.seasonEpisodeDao(),
        episodeProgressDao = database.episodeProgressDao(),
        omdbRepository     = omdbRepository
    )

    val backupManager = BackupManager(
        context    = context,
        repository = repository
    )
}
