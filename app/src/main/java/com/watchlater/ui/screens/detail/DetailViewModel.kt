package com.watchlater.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.watchlater.data.repository.EpisodeUiItem
import com.watchlater.data.repository.EpisodeValidationResult
import com.watchlater.data.repository.SeriesRepository
import com.watchlater.data.repository.WatchItemRepository
import com.watchlater.model.MediaType
import com.watchlater.model.WatchItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DetailUiState(
    val item: WatchItem? = null,
    val rating: Float = 0f,
    val isWatched: Boolean = false,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val isSaving: Boolean = false,
    val showDeleteDialog: Boolean = false,
    // Series
    val totalSeasons: Int = 0,
    val selectedSeason: Int = 1,
    val seasonEpisodes: List<EpisodeUiItem> = emptyList(),
    val isLoadingEpisodes: Boolean = false,
    val isMarkingAllWatched: Boolean = false,
    val markAllProgress: Int = 0,       // which season currently being marked
    val currentSeason: Int = 1,
    val currentEpisode: Int = 1,
    // UX feedback
    val toastMessage: String? = null,
    val showMarkAllSeriesDialog: Boolean = false
)

class DetailViewModel(
    private val repository: WatchItemRepository,
    private val seriesRepository: SeriesRepository,
    private val itemId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _savedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getItemByIdFlow(itemId).collect { item ->
                val current = _uiState.value
                if (current.isLoading) {
                    if (item != null) {
                        _uiState.value = DetailUiState(
                            item           = item,
                            rating         = item.rating,
                            isWatched      = item.isWatched,
                            currentSeason  = item.season ?: 1,
                            currentEpisode = item.episode ?: 1,
                            selectedSeason = item.season ?: 1,
                            isLoading      = false
                        )
                        if (item.type == MediaType.SERIES) loadSeriesData(item)
                    } else {
                        _uiState.value = DetailUiState(isLoading = false)
                    }
                } else if (item == null) {
                    _uiState.update { it.copy(isDeleted = true) }
                } else {
                    _uiState.update { it.copy(item = item) }
                }
            }
        }
    }

    // ── Series loading ─────────────────────────────────────────────────────

    private fun loadSeriesData(item: WatchItem) {
        viewModelScope.launch {
            val total = seriesRepository.getTotalSeasons(item.imdbId, item.title)
            _uiState.update { it.copy(totalSeasons = total) }
            if (total > 0) loadSeasonEpisodes(item, _uiState.value.selectedSeason)
        }
    }

    fun selectSeason(season: Int) {
        val item = _uiState.value.item ?: return
        _uiState.update { it.copy(selectedSeason = season, seasonEpisodes = emptyList(), isLoadingEpisodes = true) }
        viewModelScope.launch { loadSeasonEpisodes(item, season) }
    }

    private suspend fun loadSeasonEpisodes(item: WatchItem, season: Int) {
        _uiState.update { it.copy(isLoadingEpisodes = true) }
        val episodes = seriesRepository.getSeasonEpisodes(item.imdbId, season, item.id)
        _uiState.update { it.copy(seasonEpisodes = episodes, isLoadingEpisodes = false) }
    }

    // ── Episode toggle with validation + optimistic update ─────────────────

    fun toggleEpisode(season: Int, episodeNumber: Int) {
        val item = _uiState.value.item ?: return
        val currentlyWatched = _uiState.value.seasonEpisodes
            .find { it.season == season && it.episodeNumber == episodeNumber }?.isWatched ?: false

        viewModelScope.launch {
            if (!currentlyWatched) {
                // Validate before marking watched
                val result = seriesRepository.validateEpisodeMarkable(item.id, item.imdbId, season)
                if (result is EpisodeValidationResult.Blocked) {
                    _uiState.update { it.copy(toastMessage = result.reason) }
                    return@launch
                }
            }

            // Optimistic update — flip in UI immediately
            val newWatched = !currentlyWatched
            _uiState.update { state ->
                state.copy(
                    seasonEpisodes = state.seasonEpisodes.map {
                        if (it.season == season && it.episodeNumber == episodeNumber)
                            it.copy(isWatched = newWatched) else it
                    }
                )
            }

            // Persist to DB (background — UI already updated)
            seriesRepository.setEpisodeWatched(item.id, season, episodeNumber, newWatched)

            // Auto-advance position if marking watched
            if (newWatched) autoAdvancePosition(item)
        }
    }

    // ── Mark season/series watched ─────────────────────────────────────────

    fun markSeasonWatched() {
        val item   = _uiState.value.item ?: return
        val season = _uiState.value.selectedSeason

        // Validate: previous seasons must be complete first
        viewModelScope.launch {
            val validation = seriesRepository.validateEpisodeMarkable(item.id, item.imdbId, season)
            if (validation is EpisodeValidationResult.Blocked) {
                _uiState.update { it.copy(toastMessage = validation.reason) }
                return@launch
            }
            // Optimistic update
            _uiState.update { state ->
                state.copy(seasonEpisodes = state.seasonEpisodes.map { it.copy(isWatched = true) })
            }
            seriesRepository.markSeasonWatched(item.id, item.imdbId, season)
            autoAdvancePosition(item)
            _uiState.update { it.copy(toastMessage = "Season $season marked as watched ✓") }
        }
    }

    fun showMarkAllSeriesDialog() {
        _uiState.update { it.copy(showMarkAllSeriesDialog = true) }
    }

    fun dismissMarkAllSeriesDialog() {
        _uiState.update { it.copy(showMarkAllSeriesDialog = false) }
    }

    fun markAllSeriesWatched() {
        val item  = _uiState.value.item ?: return
        val total = _uiState.value.totalSeasons
        _uiState.update { it.copy(showMarkAllSeriesDialog = false, isMarkingAllWatched = true, markAllProgress = 0) }

        viewModelScope.launch {
            seriesRepository.markAllSeriesWatched(
                watchItemId  = item.id,
                imdbId       = item.imdbId,
                totalSeasons = total,
                onProgress   = { current, _ ->
                    _uiState.update { it.copy(markAllProgress = current) }
                }
            )
            // Reload current season and set watched flag
            loadSeasonEpisodes(item, _uiState.value.selectedSeason)
            val updated = item.copy(isWatched = true, lastUpdated = System.currentTimeMillis())
            repository.updateItem(updated)
            _uiState.update {
                it.copy(
                    isMarkingAllWatched = false,
                    isWatched           = true,
                    toastMessage        = "All ${total} seasons marked as watched ✓"
                )
            }
        }
    }

    // ── Auto-advance position ──────────────────────────────────────────────

    private suspend fun autoAdvancePosition(item: WatchItem) {
        val total = _uiState.value.totalSeasons
        val next  = seriesRepository.nextUnwatched(item.id, item.imdbId, total)
        val updated = item.copy(
            season      = next?.first ?: _uiState.value.totalSeasons,
            episode     = next?.second ?: (
                _uiState.value.seasonEpisodes.lastOrNull()?.episodeNumber ?: item.episode
            ),
            isWatched   = next == null,
            lastUpdated = System.currentTimeMillis()
        )
        repository.updateItem(updated)
        _uiState.update {
            it.copy(
                currentSeason  = updated.season ?: 1,
                currentEpisode = updated.episode ?: 1,
                isWatched      = updated.isWatched
            )
        }
    }

    // ── Generic ───────────────────────────────────────────────────────────

    fun onRatingChange(rating: Float) { _uiState.update { it.copy(rating = rating) } }

    fun toggleWatched() {
        val item      = _uiState.value.item ?: return
        val nowWatched = !_uiState.value.isWatched
        _uiState.update { it.copy(isWatched = nowWatched) }

        // For series: show dialog to mark all episodes
        if (nowWatched && item.type == MediaType.SERIES && item.imdbId.isNotBlank()
            && _uiState.value.totalSeasons > 0) {
            _uiState.update { it.copy(showMarkAllSeriesDialog = true) }
        }
    }

    fun clearToast() { _uiState.update { it.copy(toastMessage = null) } }
    fun showDeleteDialog()   { _uiState.update { it.copy(showDeleteDialog = true) } }
    fun dismissDeleteDialog() { _uiState.update { it.copy(showDeleteDialog = false) } }

    fun saveItem(onSuccess: (() -> Unit)? = null) {
        val state    = _uiState.value
        val original = state.item ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updated = original.copy(
                rating      = state.rating,
                isWatched   = state.isWatched,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateItem(updated)
            _uiState.update { it.copy(isSaving = false, item = updated) }
            _savedEvent.emit(Unit)
            onSuccess?.invoke()
        }
    }

    fun deleteItem(onSuccess: () -> Unit) {
        val item = _uiState.value.item ?: return
        viewModelScope.launch {
            seriesRepository.deleteProgress(item.id)
            repository.deleteItem(item)
            _uiState.update { it.copy(isDeleted = true) }
            onSuccess()
        }
    }

    class Factory(
        private val repository: WatchItemRepository,
        private val seriesRepository: SeriesRepository,
        private val itemId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DetailViewModel(repository, seriesRepository, itemId) as T
    }
}
