package com.kaze.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.kaze.data.repository.*
import com.kaze.data.remote.*
import com.kaze.data.local.CastCacheDao
import com.kaze.updater.UpdateManager
import com.kaze.utils.BackupManager
import com.kaze.utils.UserPreferences

@HiltViewModel
class NavigationViewModel @Inject constructor(
    val repository: WatchItemRepository,
    val userRepository: UserRepository,
    val traktRepository: TraktRepository,
    val omdbRepository: OmdbRepository,
    val arcRepository: ArcRepository,
    val seriesRepository: SeriesRepository,
    val activityRepository: ActivityRepository,
    val castCacheDao: CastCacheDao,
    val whatToWatchDao: com.kaze.data.local.WhatToWatchDao,
    val updateManager: UpdateManager,
    val backupManager: BackupManager,
    val userPreferences: UserPreferences
) : ViewModel()
