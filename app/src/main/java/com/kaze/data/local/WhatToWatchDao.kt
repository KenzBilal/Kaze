package com.kaze.data.local

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.kaze.model.WatchItem

@Dao
interface WhatToWatchDao {
    @RawQuery(observedEntities = [WatchItem::class, SeriesCache::class])
    suspend fun getRandomSuggestion(query: SupportSQLiteQuery): WatchItem?
}
