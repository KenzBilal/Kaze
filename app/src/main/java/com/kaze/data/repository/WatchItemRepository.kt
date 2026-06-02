package com.kaze.data.repository

import com.kaze.data.local.EpisodeProgress
import com.kaze.data.local.EpisodeProgressDao
import com.kaze.data.local.WatchItemDao
import com.kaze.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import androidx.sqlite.db.SimpleSQLiteQuery

class WatchItemRepository(
    private val dao: WatchItemDao,
    private val episodeProgressDao: EpisodeProgressDao
) {

    fun getToWatchItems(sortFilter: SortFilterState): Flow<List<WatchItem>> =
        dao.getItemsViaQuery(buildQuery(false, sortFilter))

    fun getWatchedItems(sortFilter: SortFilterState): Flow<List<WatchItem>> =
        dao.getItemsViaQuery(buildQuery(true, sortFilter))

    fun searchItems(query: String): Flow<List<WatchItem>> =
        dao.searchItems(query.trim())

    fun getItemByIdFlow(id: Long): Flow<WatchItem?> = dao.getItemByIdFlow(id)

    suspend fun getItemById(id: Long): WatchItem? = dao.getItemById(id)

    suspend fun getItemByImdbId(imdbId: String): WatchItem? = dao.getItemByImdbId(imdbId)

    suspend fun saveItem(item: WatchItem): Long = dao.insertItem(item)

    suspend fun updateItem(item: WatchItem) {
        dao.updateItem(item.copy(lastUpdated = System.currentTimeMillis()))
    }

    suspend fun deleteItem(item: WatchItem) = dao.deleteItem(item)

    suspend fun deleteItemById(id: Long) = dao.deleteItemById(id)

    suspend fun isDuplicate(imdbId: String, title: String, year: Int, type: MediaType): Boolean {
        if (imdbId.isNotBlank()) {
            val byImdb = dao.getItemByImdbId(imdbId)
            if (byImdb != null) return true
        }
        val byDetails = dao.getItemByTitleYearType(title.trim(), year, type)
        return byDetails != null
    }

    // Stats flows
    fun getTotalCount(): Flow<Int> = dao.getTotalCount()
    fun getMovieCount(): Flow<Int> = dao.getMovieCount()
    fun getSeriesCount(): Flow<Int> = dao.getSeriesCount()
    fun getWatchedCount(): Flow<Int> = dao.getWatchedCount()
    fun getSeriesInProgress(): Flow<List<WatchItem>> = dao.getSeriesInProgress()
    fun getRecentlyAdded(limit: Int = 5): Flow<List<WatchItem>> = dao.getRecentlyAdded(limit)
    
    fun getAllItemsFlow(): Flow<List<WatchItem>> = dao.getAllItems()

    /** One-shot snapshot of all items for backup export. */
    suspend fun getAllItemsSnapshot(): List<WatchItem> =
        dao.getAllItems().first()

    /** All episode progress for a given item — used for backup export. */
    suspend fun getEpisodeProgressSnapshot(watchItemId: Long): List<EpisodeProgress> =
        episodeProgressDao.getAll(watchItemId)

    /** All episode progress across all items — used for batch backup export. */
    suspend fun getAllEpisodeProgressSnapshot(): List<EpisodeProgress> =
        episodeProgressDao.getAllEpisodeProgressOnce()

    /**
     * Atomically replaces all watch_items.
     * Returns a map of (old index → new DB id) for episode progress remapping.
     */
    suspend fun restoreItems(items: List<WatchItem>): List<Long> =
        dao.replaceAll(items)

    /** Restores episode progress rows after a backup import. */
    suspend fun restoreEpisodeProgress(list: List<EpisodeProgress>) {
        episodeProgressDao.insertAll(list)
    }

    // ── Sorting & Filtering via SQLite ─────────────────────────────────────

    private fun buildQuery(isWatched: Boolean, state: SortFilterState): SimpleSQLiteQuery {
        val whereClause = mutableListOf<String>()
        whereClause.add("isWatched = ${if (isWatched) 1 else 0}")

        when (state.filter) {
            FilterOption.ALL -> {}
            FilterOption.MOVIES -> whereClause.add("type = 'MOVIE'")
            FilterOption.SERIES -> whereClause.add("type = 'SERIES'")
            FilterOption.IN_PROGRESS -> {
                whereClause.add("type = 'SERIES'")
                whereClause.add("(season IS NOT NULL OR episode IS NOT NULL)")
            }
        }

        val orderClause = when (state.sort) {
            SortOption.DATE_ADDED_DESC -> "dateAdded DESC"
            SortOption.DATE_ADDED_ASC  -> "dateAdded ASC"
            SortOption.TITLE_ASC       -> "title COLLATE NOCASE ASC"
            SortOption.TITLE_DESC      -> "title COLLATE NOCASE DESC"
            SortOption.YEAR_DESC       -> "year DESC"
            SortOption.YEAR_ASC        -> "year ASC"
            SortOption.RATING_DESC     -> "rating DESC"
            SortOption.PROGRESS        -> "CASE WHEN type = 'SERIES' THEN 1 ELSE 0 END DESC, COALESCE(season, 0) DESC, COALESCE(episode, 0) DESC"
        }

        val queryString = "SELECT * FROM watch_items WHERE ${whereClause.joinToString(" AND ")} ORDER BY $orderClause"
        return SimpleSQLiteQuery(queryString)
    }
}
