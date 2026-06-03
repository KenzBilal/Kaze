package com.kaze.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaze.data.repository.WatchItemRepository
import com.kaze.data.repository.SeriesRepository
import com.kaze.data.repository.UserRepository
import com.kaze.model.*
import com.kaze.updater.UpdateInfo
import com.kaze.updater.UpdateManager
import com.kaze.updater.UpdateState
import com.kaze.utils.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

data class HomeUiState(
    val items: List<WatchItem> = emptyList(),
    val sortFilterState: SortFilterState = SortFilterState(),
    val isLoading: Boolean = true,
    val selectedTab: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: WatchItemRepository,
    private val seriesRepository: SeriesRepository,
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences,
    private val updateManager: UpdateManager,
    private val backupManager: com.kaze.utils.BackupManager
) : ViewModel() {

    private val _sortFilterState = MutableStateFlow(SortFilterState())

    private val _showRatingPromptForItem = MutableSharedFlow<WatchItem>(extraBufferCapacity = 1)
    val showRatingPromptForItem: SharedFlow<WatchItem> = _showRatingPromptForItem.asSharedFlow()
    private val _selectedTab = MutableStateFlow(0)

    private val _items: StateFlow<List<WatchItem>?> = combine(_sortFilterState, _selectedTab) { sf, tab ->
        Pair(sf, tab)
    }.flatMapLatest { (sf, tab) ->
        if (tab == 0) repository.getToWatchItems(sf)
        else repository.getWatchedItems(sf)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<HomeUiState> = combine(
        _items, _sortFilterState, _selectedTab
    ) { items, sortFilter, tab ->
        HomeUiState(
            items           = items ?: emptyList(),
            sortFilterState = sortFilter,
            isLoading       = items == null,
            selectedTab     = tab
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setTab(tab: Int) {
        _selectedTab.value = tab
    }

    val updateState: StateFlow<UpdateState> = updateManager.updateState
    val updateInfo: StateFlow<UpdateInfo?> = updateManager.updateInfo

    init {
        val initialSort = try { SortOption.valueOf(userPreferences.sortOption) } catch (e: Exception) { SortOption.DATE_ADDED_DESC }
        val initialFilter = try { FilterOption.valueOf(userPreferences.filterOption) } catch (e: Exception) { FilterOption.ALL }
        _sortFilterState.value = SortFilterState(sort = initialSort, filter = initialFilter)
        
        viewModelScope.launch {
            updateManager.checkForUpdates()
        }

        if (!userPreferences.hasMigratedWatchedSeries) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val watchedItems = repository.getWatchedItems(SortFilterState(filter = FilterOption.SERIES)).first()
                    watchedItems.forEach { item ->
                        if (item.type == MediaType.SERIES && item.isWatched) {
                            val totalSeasons = seriesRepository.getTotalSeasons(item.imdbId, item.title)
                            if (totalSeasons > 0) {
                                seriesRepository.markAllSeriesWatched(item.id, item.imdbId, totalSeasons)
                            }
                        }
                    }
                    userPreferences.hasMigratedWatchedSeries = true
                } catch (e: Exception) {
                    // Ignore, try again next time
                }
            }
        }
        // Silent background sync
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.getLocalUserId()?.let { uid ->
                    val localItems = repository.getAllItemsSnapshot()
                    if (localItems.isEmpty()) {
                        // New device / fresh install — safe to restore from cloud (nothing to lose)
                        backupManager.restoreFromCloud(uid)
                    }
                    // Always push local → cloud to keep cloud up-to-date
                    val allItems = repository.getAllItemsSnapshot()
                    if (allItems.isNotEmpty()) {
                        userRepository.syncWatchlist(uid, allItems)
                        // Also sync episode progress
                        val allProgress = repository.getAllEpisodeProgressSnapshot()
                        if (allProgress.isNotEmpty()) {
                            userRepository.syncEpisodeProgress(uid, allProgress, allItems)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun downloadUpdate() = updateManager.downloadUpdate()
    fun installUpdate()  = updateManager.installApk()

    fun updateSort(sort: SortOption) {
        userPreferences.sortOption = sort.name
        _sortFilterState.update { it.copy(sort = sort) }
    }

    fun updateFilter(filter: FilterOption) {
        userPreferences.filterOption = filter.name
        _sortFilterState.update { it.copy(filter = filter) }
    }

    fun toggleWatched(item: WatchItem) {
        val updated = if (!item.isWatched && item.type == MediaType.SERIES) {
            item.copy(isWatched = true, lastUpdated = System.currentTimeMillis())
        } else {
            item.copy(isWatched = !item.isWatched, lastUpdated = System.currentTimeMillis())
        }

        viewModelScope.launch {
            // Show rating prompt immediately so UI reacts instantly without blocking
            if (updated.isWatched) {
                _showRatingPromptForItem.emit(updated)
            }

            launch(Dispatchers.IO) {
                if (!item.isWatched && item.type == MediaType.SERIES) {
                    val totalSeasons = seriesRepository.getTotalSeasons(item.imdbId, item.title)
                    if (totalSeasons > 0) {
                        seriesRepository.markAllSeriesWatched(item.id, item.imdbId, totalSeasons)
                        // Sync episode progress to cloud
                        userRepository.getLocalUserId()?.let { uid ->
                            val allProgress = repository.getAllEpisodeProgressSnapshot()
                            userRepository.syncEpisodeProgress(uid, allProgress, listOf(item))
                        }
                    }
                }
                repository.updateItem(updated)
                userRepository.getLocalUserId()?.let { uid ->
                    userRepository.pushWatchItem(uid, updated)
                }
            }
        }
    }

    fun saveRating(item: WatchItem, rating: Float) {
        viewModelScope.launch {
            val updated = item.copy(rating = rating, lastUpdated = System.currentTimeMillis())
            repository.updateItem(updated)
            userRepository.getLocalUserId()?.let { uid ->
                userRepository.pushWatchItem(uid, updated)
            }
        }
    }

    fun deleteItem(item: WatchItem) {
        viewModelScope.launch { 
            repository.deleteItem(item)
            userRepository.getLocalUserId()?.let { uid ->
                userRepository.deleteFromWatchlist(uid, item)
            }
        }
    }

    class Factory(
        private val repository: WatchItemRepository,
        private val seriesRepository: SeriesRepository,
        private val userRepository: UserRepository,
        private val userPreferences: UserPreferences,
        private val updateManager: UpdateManager,
        private val backupManager: com.kaze.utils.BackupManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository, seriesRepository, userRepository, userPreferences, updateManager, backupManager) as T
    }
}
