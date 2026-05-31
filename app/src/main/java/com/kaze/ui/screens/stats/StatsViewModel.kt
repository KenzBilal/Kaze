package com.kaze.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaze.data.repository.WatchItemRepository
import com.kaze.model.WatchItem
import kotlinx.coroutines.flow.*

data class StatsUiState(
    val total: Int = 0,
    val movies: Int = 0,
    val series: Int = 0,
    val watched: Int = 0,
    val seriesInProgress: List<WatchItem> = emptyList(),
    val recentlyAdded: List<WatchItem> = emptyList(),
    val isLoading: Boolean = true
)

class StatsViewModel(
    private val repository: WatchItemRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        repository.getTotalCount(),
        repository.getMovieCount(),
        repository.getSeriesCount(),
        repository.getWatchedCount(),
        repository.getSeriesInProgress()
    ) { total, movies, series, watched, inProgress ->
        StatsUiState(
            total            = total,
            movies           = movies,
            series           = series,
            watched          = watched,
            seriesInProgress = inProgress,
            isLoading        = false
        )
    }.combine(repository.getRecentlyAdded(5)) { state, recent ->
        state.copy(recentlyAdded = recent)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )

    class Factory(
        private val repository: WatchItemRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel(repository) as T
    }
}
