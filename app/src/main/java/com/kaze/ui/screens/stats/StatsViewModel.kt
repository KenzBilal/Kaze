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
    val totalHours: Int = 0,
    val topGenres: Map<String, Int> = emptyMap(),
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
    }.combine(repository.getAllItemsFlow()) { state, allItems ->
        // Total hours estimate
        // movies = ~120 mins, series episodes = ~45 mins per watched ep
        val watchedMovies = allItems.filter { it.isWatched && it.type == com.kaze.model.MediaType.MOVIE }.size
        var seriesEpisodesWatched = 0
        allItems.filter { it.type == com.kaze.model.MediaType.SERIES }.forEach { item ->
            if (item.isWatched) {
                // If marked totally watched, guess 10 eps * seasons? We don't have perfect data here without DB join.
                // We'll estimate based on current season/episode marker:
                val s = item.season ?: 1
                val e = item.episode ?: 10
                seriesEpisodesWatched += ((s - 1) * 10) + e
            } else {
                val s = item.season ?: 1
                val e = item.episode ?: 0
                seriesEpisodesWatched += ((s - 1) * 10) + e
            }
        }
        val totalMinutes = (watchedMovies * 120) + (seriesEpisodesWatched * 45)
        val totalHours = totalMinutes / 60

        // Top genres
        val genreCounts = allItems.filter { it.isWatched }
            .flatMap { it.genreList }
            .groupingBy { it }
            .eachCount()
        
        val topGenresMap = genreCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .associate { it.key to it.value }

        state.copy(totalHours = totalHours, topGenres = topGenresMap)
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
