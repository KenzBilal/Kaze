package com.kaze.di

import android.content.Context
import com.kaze.data.local.WatchLaterDatabase
import com.kaze.data.remote.OmdbRepository
import com.kaze.data.repository.SeriesRepository
import com.kaze.data.repository.WatchItemRepository
import com.kaze.updater.UpdateManager
import com.kaze.utils.BackupManager

class AppContainer(context: Context) {
    val updateManager    = UpdateManager(context)
    private val database = WatchLaterDatabase.getInstance(context)
    val omdbRepository   = OmdbRepository()
    val activityRepository = com.kaze.data.repository.ActivityRepository(context)
    val userRepository = com.kaze.data.repository.UserRepository(context)
    val userPreferences = com.kaze.utils.UserPreferences(context)

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
        context        = context,
        repository     = repository,
        userRepository = userRepository
    )
}
