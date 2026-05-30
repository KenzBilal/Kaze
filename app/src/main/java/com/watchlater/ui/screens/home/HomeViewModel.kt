package com.watchlater.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.watchlater.data.repository.WatchItemRepository
import com.watchlater.model.*
import com.watchlater.updater.UpdateInfo
import com.watchlater.updater.UpdateManager
import com.watchlater.updater.UpdateState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val toWatchItems: List<WatchItem> = emptyList(),
    val watchedItems: List<WatchItem> = emptyList(),
    val sortFilterState: SortFilterState = SortFilterState(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: WatchItemRepository,
    private val updateManager: UpdateManager
) : ViewModel() {

    private val _sortFilterState = MutableStateFlow(SortFilterState())

    private val _toWatchItems: StateFlow<List<WatchItem>?> = _sortFilterState
        .flatMapLatest { sf -> repository.getToWatchItems(sf) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _watchedItems: StateFlow<List<WatchItem>?> = _sortFilterState
        .flatMapLatest { sf -> repository.getWatchedItems(sf) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<HomeUiState> = combine(
        _toWatchItems, _watchedItems, _sortFilterState
    ) { toWatch, watched, sortFilter ->
        HomeUiState(
            toWatchItems    = toWatch ?: emptyList(),
            watchedItems    = watched ?: emptyList(),
            sortFilterState = sortFilter,
            isLoading       = toWatch == null || watched == null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val updateState: StateFlow<UpdateState> = updateManager.updateState
    val updateInfo: StateFlow<UpdateInfo?> = updateManager.updateInfo

    init {
        viewModelScope.launch {
            updateManager.checkForUpdates()
        }
    }

    // BUG-01 fix: release BroadcastReceiver when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        updateManager.release()
    }

    fun downloadUpdate() = updateManager.downloadUpdate()
    fun installUpdate()  = updateManager.installApk()

    fun updateSort(sort: SortOption) {
        _sortFilterState.update { it.copy(sort = sort) }
    }

    fun updateFilter(filter: FilterOption) {
        _sortFilterState.update { it.copy(filter = filter) }
    }

    fun toggleWatched(item: WatchItem) {
        viewModelScope.launch {
            val updated = if (!item.isWatched && item.type == MediaType.SERIES) {
                item.copy(isWatched = true, lastUpdated = System.currentTimeMillis())
            } else {
                item.copy(isWatched = !item.isWatched, lastUpdated = System.currentTimeMillis())
            }
            repository.updateItem(updated)
        }
    }

    fun deleteItem(item: WatchItem) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    class Factory(
        private val repository: WatchItemRepository,
        private val updateManager: UpdateManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository, updateManager) as T
    }
}
