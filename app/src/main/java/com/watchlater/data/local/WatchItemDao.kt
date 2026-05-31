package com.watchlater.data.local

import androidx.room.*
import com.watchlater.model.WatchItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchItemDao {

    @Query("SELECT * FROM watch_items ORDER BY dateAdded DESC")
    fun getAllItems(): Flow<List<WatchItem>>

    @Query("SELECT * FROM watch_items ORDER BY dateAdded DESC")
    suspend fun getAllItemsOnce(): List<WatchItem>

    @RawQuery(observedEntities = [WatchItem::class])
    fun getItemsViaQuery(query: androidx.sqlite.db.SupportSQLiteQuery): Flow<List<WatchItem>>

    @Query("SELECT * FROM watch_items WHERE id = :id")
    fun getItemByIdFlow(id: Long): Flow<WatchItem?>

    @Query("SELECT * FROM watch_items WHERE id = :id")
    suspend fun getItemById(id: Long): WatchItem?

    @Query("SELECT * FROM watch_items WHERE id = :id")
    suspend fun getItemByIdNow(id: Long): WatchItem?

    @Query("SELECT * FROM watch_items WHERE imdbId = :imdbId LIMIT 1")
    suspend fun getItemByImdbId(imdbId: String): WatchItem?

    @Query("SELECT * FROM watch_items WHERE title = :title AND year = :year AND type = :type LIMIT 1")
    suspend fun getItemByTitleYearType(title: String, year: Int, type: com.watchlater.model.MediaType): WatchItem?

    @Query("""
        SELECT * FROM watch_items 
        WHERE title LIKE '%' || :query || '%' 
        ORDER BY dateAdded DESC
    """)
    fun searchItems(query: String): Flow<List<WatchItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WatchItem): Long

    @Update
    suspend fun updateItem(item: WatchItem)

    @Delete
    suspend fun deleteItem(item: WatchItem)

    @Query("DELETE FROM watch_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("SELECT COUNT(*) FROM watch_items")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM watch_items WHERE type = 'MOVIE'")
    fun getMovieCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM watch_items WHERE type = 'SERIES'")
    fun getSeriesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM watch_items WHERE isWatched = 1")
    fun getWatchedCount(): Flow<Int>

    @Query("""
        SELECT * FROM watch_items 
        WHERE type = 'SERIES' AND isWatched = 0 
        AND (season IS NOT NULL OR episode IS NOT NULL)
        ORDER BY lastUpdated DESC
    """)
    fun getSeriesInProgress(): Flow<List<WatchItem>>

    @Query("SELECT * FROM watch_items ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAdded(limit: Int = 5): Flow<List<WatchItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WatchItem>): List<Long>

    @Query("DELETE FROM watch_items")
    suspend fun deleteAll()

    /** Atomic restore: wipe + re-insert in one transaction */
    @Transaction
    suspend fun replaceAll(items: List<WatchItem>): List<Long> {
        deleteAll()
        return insertAll(items)
    }
}
