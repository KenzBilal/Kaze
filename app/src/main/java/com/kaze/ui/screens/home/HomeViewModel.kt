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
    private val updateManager: UpdateManager
) : ViewModel() {

    private val _sortFilterState = MutableStateFlow(SortFilterState())
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
        // Silent background sync to ensure cloud always matches local DB perfectly
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                userRepository.getLocalUserId()?.let { uid ->
                    val allItems = repository.getAllItemsSnapshot()
                    if (allItems.isNotEmpty()) {
                        userRepository.syncWatchlist(uid, allItems)
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
        viewModelScope.launch {
            val updated = if (!item.isWatched && item.type == MediaType.SERIES) {
                // BUG-01 Fix: also mark all episodes watched in SeriesRepository
                val totalSeasons = seriesRepository.getTotalSeasons(item.imdbId, item.title)
                if (totalSeasons > 0) {
                    seriesRepository.markAllSeriesWatched(item.id, item.imdbId, totalSeasons)
                }
                item.copy(isWatched = true, lastUpdated = System.currentTimeMillis())
            } else {
                item.copy(isWatched = !item.isWatched, lastUpdated = System.currentTimeMillis())
            }
            repository.updateItem(updated)
            // BUG-02 Fix: sync to Supabase when toggling from Home screen
            userRepository.getLocalUserId()?.let { uid ->
                userRepository.pushWatchItem(uid, updated)
            }
        }
    }

    fun deleteItem(item: WatchItem) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    class Factory(
        private val repository: WatchItemRepository,
        private val seriesRepository: SeriesRepository,
        private val userRepository: UserRepository,
        private val userPreferences: UserPreferences,
        private val updateManager: UpdateManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository, seriesRepository, userRepository, userPreferences, updateManager) as T
    }
}
