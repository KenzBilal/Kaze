package com.kaze.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaze.data.remote.OmdbRepository
import com.kaze.data.remote.TraktRepository
import com.kaze.data.repository.EpisodeUiItem
import com.kaze.data.repository.EpisodeValidationResult
import com.kaze.data.repository.SeriesRepository
import com.kaze.data.repository.UserRepository
import com.kaze.data.repository.WatchItemRepository
import com.kaze.model.MediaType
import com.kaze.model.WatchItem
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
    val notes: String = "",
    // Series
    val totalSeasons: Int = 0,
    val selectedSeason: Int = 0,          // 0 = none selected
    val seasonEpisodes: List<EpisodeUiItem> = emptyList(),
    val isLoadingEpisodes: Boolean = false,
    val isMarkingAllWatched: Boolean = false,
    val markAllProgress: Int = 0,
    val currentSeason: Int = 1,
    val currentEpisode: Int = 1,
    // Plot & Trailer
    val trailerUrl: String = "",
    val isLoadingTrailer: Boolean = false,
    // Episode plot dialog
    val episodePlotText: String = "",
    val episodePlotTitle: String = "",
    val isLoadingEpisodePlot: Boolean = false,
    val showEpisodePlotDialog: Boolean = false,
    // UX feedback
    val toastMessage: String? = null,
    val showMarkAllSeriesDialog: Boolean = false,
    val showRatingPrompt: Boolean = false,
    val isPreview: Boolean = false
)

class DetailViewModel(
    private val repository: WatchItemRepository,
    private val seriesRepository: SeriesRepository,
    private val userRepository: UserRepository,
    private val omdbRepository: OmdbRepository,
    private val traktRepository: TraktRepository,
    private val itemId: Long,
    private val previewImdbId: String? = null,
    private val previewTitle: String? = null,
    private val previewType: String? = null,
    private val previewPoster: String? = null,
    private val previewRating: Float = 0f,
    private val previewNotes: String = "",
    private val previewGenres: String = "",
    private val previewYear: Int = 0,
    private val previewSeason: Int = 1,
    private val previewEpisode: Int = 1
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _savedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            if (itemId == -1L && previewImdbId != null) {
                val mediaType = try { MediaType.valueOf(previewType ?: "MOVIE") } catch (e: Exception) { MediaType.MOVIE }
                val previewItem = WatchItem(
                    id = -1,
                    imdbId = previewImdbId,
                    title = previewTitle ?: "",
                    year = previewYear,
                    type = mediaType,
                    posterUrl = previewPoster,
                    genres = previewGenres,
                    season = if (mediaType == MediaType.SERIES) previewSeason else null,
                    episode = if (mediaType == MediaType.SERIES) previewEpisode else null,
                    dateAdded = System.currentTimeMillis(),
                    lastUpdated = System.currentTimeMillis()
                )
                _uiState.value = DetailUiState(
                    item = previewItem,
                    rating = previewRating,
                    notes = previewNotes,
                    isPreview = true,
                    isLoading = false
                )
                if (previewItem.type == MediaType.SERIES) loadSeriesData(previewItem)
                else fetchTrailerAndPlot(previewItem)
                return@launch
            }

            repository.getItemByIdFlow(itemId).collect { item ->
                val current = _uiState.value
                if (current.isLoading) {
                    if (item != null) {
                        _uiState.value = DetailUiState(
                            item           = item,
                            rating         = item.rating,
                            isWatched      = item.isWatched,
                            notes          = item.notes,
                            currentSeason  = item.season ?: 1,
                            currentEpisode = item.episode ?: 1,
                            selectedSeason = 0,   // None selected by default
                            isLoading      = false,
                            trailerUrl     = item.trailerUrl
                        )
                        if (item.type == MediaType.SERIES) loadSeriesData(item)
                        fetchTrailerAndPlot(item)
                    } else {
                        _uiState.value = DetailUiState(isLoading = false)
                    }
                } else if (item == null) {
                    _uiState.update { it.copy(isDeleted = true) }
                } else {
                    _uiState.update { it.copy(item = item, isWatched = item.isWatched) }
                }
            }
        }
    }

    // ── Series loading ─────────────────────────────────────────────────────

    private fun loadSeriesData(item: WatchItem) {
        viewModelScope.launch {
            val total = seriesRepository.getTotalSeasons(item.imdbId, item.title)
            _uiState.update { it.copy(totalSeasons = total) }
            // Do NOT auto-load episodes — user must click a season chip
        }
    }

    // ── Trailer & Plot fetching ────────────────────────────────────────────

    private fun fetchTrailerAndPlot(item: WatchItem) {
        if (item.imdbId.isBlank()) return
        viewModelScope.launch {
            // Show skeleton while fetching
            _uiState.update { it.copy(isLoadingTrailer = item.trailerUrl.isBlank()) }
            // Fetch trailer
            val trailerUrl = if (item.trailerUrl.isBlank()) {
                val url = traktRepository.fetchTrailerUrl(
                    imdbId  = item.imdbId,
                    isMovie = item.type == MediaType.MOVIE
                )
                if (url != null && !_uiState.value.isPreview && itemId != -1L) {
                    val updated = item.copy(trailerUrl = url)
                    repository.updateItem(updated)
                }
                url ?: ""
            } else item.trailerUrl

            // Fetch plot (movie = full, series = short summary)
            val plot = if (item.plot.isBlank()) {
                val fetched = if (item.type == MediaType.MOVIE) {
                    omdbRepository.fetchMoviePlot(item.imdbId)
                } else {
                    omdbRepository.fetchDetail(item.imdbId, plotLength = "full").plot
                }
                if (fetched.isNotBlank() && !_uiState.value.isPreview && itemId != -1L) {
                    val current = _uiState.value.item ?: item
                    repository.updateItem(current.copy(plot = fetched, trailerUrl = trailerUrl))
                }
                fetched
            } else item.plot

            _uiState.update { state ->
                state.copy(
                    trailerUrl      = trailerUrl,
                    isLoadingTrailer = false,
                    item = state.item?.copy(plot = plot, trailerUrl = trailerUrl)
                )
            }
        }
    }

    // ── Episode plot (on-demand) ───────────────────────────────────────────

    fun fetchEpisodePlot(episode: EpisodeUiItem) {
        val item = _uiState.value.item ?: return
        // Show cached plot immediately if available
        if (episode.plot.isNotBlank()) {
            _uiState.update { it.copy(
                episodePlotText  = episode.plot,
                episodePlotTitle = episode.title,
                showEpisodePlotDialog = true
            ) }
            return
        }
        _uiState.update { it.copy(
            isLoadingEpisodePlot = true,
            episodePlotTitle     = episode.title,
            showEpisodePlotDialog = true
        ) }
        viewModelScope.launch {
            val plot = seriesRepository.fetchAndCacheEpisodePlot(
                imdbId        = item.imdbId,
                season        = episode.season,
                episodeNumber = episode.episodeNumber
            )
            _uiState.update { state ->
                state.copy(
                    isLoadingEpisodePlot = false,
                    episodePlotText = plot.ifBlank { "Plot unavailable." },
                    // Also update the episode in the list so future taps use cache
                    seasonEpisodes = state.seasonEpisodes.map {
                        if (it.season == episode.season && it.episodeNumber == episode.episodeNumber)
                            it.copy(plot = plot) else it
                    }
                )
            }
        }
    }

    fun dismissEpisodePlotDialog() {
        _uiState.update { it.copy(showEpisodePlotDialog = false, episodePlotText = "", episodePlotTitle = "") }
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
                val result = seriesRepository.validateEpisodeMarkable(item.id, item.imdbId, season)
                if (result is EpisodeValidationResult.Blocked) {
                    _uiState.update { it.copy(toastMessage = result.reason) }
                    return@launch
                }
            }

            val newWatched = !currentlyWatched
            _uiState.update { state ->
                state.copy(
                    seasonEpisodes = state.seasonEpisodes.map {
                        if (it.season == season && it.episodeNumber == episodeNumber)
                            it.copy(isWatched = newWatched) else it
                    }
                )
            }

            seriesRepository.setEpisodeWatched(item.id, season, episodeNumber, newWatched)
            // BUG-03 Fix: Call autoAdvancePosition unconditionally to sync episode changes to Supabase
            autoAdvancePosition(item)
        }
    }

    // ── Mark season/series watched ─────────────────────────────────────────

    fun markSeasonWatched() {
        val item   = _uiState.value.item ?: return
        val season = _uiState.value.selectedSeason

        viewModelScope.launch {
            val validation = seriesRepository.validateEpisodeMarkable(item.id, item.imdbId, season)
            if (validation is EpisodeValidationResult.Blocked) {
                _uiState.update { it.copy(toastMessage = validation.reason) }
                return@launch
            }
            // BUG-08 fix: check whether the season actually had data before optimistic update
            val success = seriesRepository.markSeasonWatched(item.id, item.imdbId, season)
            if (!success) {
                _uiState.update { it.copy(toastMessage = "Could not load Season $season episode data. Check your connection.") }
                return@launch
            }
            _uiState.update { state ->
                state.copy(seasonEpisodes = state.seasonEpisodes.map { it.copy(isWatched = true) })
            }
            autoAdvancePosition(item)
            _uiState.update { it.copy(toastMessage = "Season $season marked as watched ✓") }
        }
    }

    fun showMarkAllSeriesDialog()    { _uiState.update { it.copy(showMarkAllSeriesDialog = true) } }
    fun dismissMarkAllSeriesDialog() { _uiState.update { it.copy(showMarkAllSeriesDialog = false) } }

    fun markAllSeriesWatched() {
        val item  = _uiState.value.item ?: return
        val total = _uiState.value.totalSeasons
        _uiState.update { it.copy(showMarkAllSeriesDialog = false, isMarkingAllWatched = true, markAllProgress = 0) }

        viewModelScope.launch {
            try {
                seriesRepository.markAllSeriesWatched(
                    watchItemId  = item.id,
                    imdbId       = item.imdbId,
                    totalSeasons = total,
                    onProgress   = { current, _ ->
                        _uiState.update { it.copy(markAllProgress = current) }
                    }
                )
                loadSeasonEpisodes(item, _uiState.value.selectedSeason)
                val updated = item.copy(isWatched = true, lastUpdated = System.currentTimeMillis())
                repository.updateItem(updated)
                // BUG-02 Fix: Sync the series progress to Supabase
                userRepository.getLocalUserId()?.let { uid ->
                    userRepository.pushWatchItem(uid, updated)
                }
                _uiState.update {
                    it.copy(
                        isMarkingAllWatched = false,
                        isWatched           = true,
                        item                = updated,
                        showRatingPrompt    = true,
                        toastMessage        = "All $total seasons marked as watched ✓"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isMarkingAllWatched = false, toastMessage = "Failed to mark all seasons watched. Check connection.") }
            }
        }
    }

    // ── Auto-advance position ──────────────────────────────────────────────

    /**
     * BUG-05 fix: when all episodes are watched (next == null), compute the final
     * position from the last season's last episode — not from the currently displayed
     * season's episodes in the UI state.
     */
    private suspend fun autoAdvancePosition(item: WatchItem) {
        val total = _uiState.value.totalSeasons
        val next  = seriesRepository.nextUnwatched(item.id, item.imdbId, total)

        val (targetSeason, targetEpisode) = if (next != null) {
            next.first to next.second
        } else {
            // All watched: find the real last episode of the last season
            val last = seriesRepository.lastEpisodeOfSeries(item.imdbId, total)
            (last?.first ?: total) to (last?.second ?: item.episode ?: 1)
        }

        val updated = item.copy(
            season      = targetSeason,
            episode     = targetEpisode,
            isWatched   = next == null,
            lastUpdated = System.currentTimeMillis()
        )
        repository.updateItem(updated)
        // Sync position update to cloud
        viewModelScope.launch {
            userRepository.getLocalUserId()?.let { uid ->
                userRepository.pushWatchItem(uid, updated)
            }
        }
        _uiState.update {
            it.copy(
                currentSeason  = targetSeason,
                currentEpisode = targetEpisode,
                isWatched      = updated.isWatched,
                item           = updated,
                showRatingPrompt = if (updated.isWatched && !it.isWatched) true else it.showRatingPrompt
            )
        }
    }

    // ── Generic ───────────────────────────────────────────────────────────

    fun onRatingChange(rating: Float) { _uiState.update { it.copy(rating = rating) } }
    fun onNotesChange(notes: String)  { _uiState.update { it.copy(notes = notes) } }
    fun dismissRatingPrompt() { _uiState.update { it.copy(showRatingPrompt = false) } }

    /**
     * BUG-07 fix: toggleWatched now also syncs item.isWatched in the state so that
     * saveItem() always writes the correct value regardless of read source.
     */
    fun toggleWatched() {
        val item       = _uiState.value.item ?: return
        val nowWatched = !_uiState.value.isWatched
        
        if (nowWatched && item.type == MediaType.SERIES && item.imdbId.isNotBlank()
            && _uiState.value.totalSeasons > 0) {
            // BUG-04 Fix: Do not immediately change UI state, wait for dialog confirmation
            _uiState.update { it.copy(showMarkAllSeriesDialog = true) }
        } else {
            // BUG-04 Fix: Persist immediately for movies or unwatching
            _uiState.update { 
                it.copy(
                    isWatched = nowWatched, 
                    item = item.copy(isWatched = nowWatched), 
                    showRatingPrompt = if (nowWatched) true else it.showRatingPrompt
                ) 
            }
            saveItem()
        }
    }

    fun clearToast()      { _uiState.update { it.copy(toastMessage = null) } }
    fun showDeleteDialog()    { _uiState.update { it.copy(showDeleteDialog = true) } }
    fun dismissDeleteDialog() { _uiState.update { it.copy(showDeleteDialog = false) } }

    fun saveItem(onSuccess: (() -> Unit)? = null) {
        if (_uiState.value.isPreview) {
            addToWatchlist(onSuccess)
            return
        }
        val state    = _uiState.value
        val original = state.item ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updated = original.copy(
                rating      = state.rating,
                isWatched   = state.isWatched,
                notes       = state.notes,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateItem(updated)
            // Sync save to cloud
            userRepository.getLocalUserId()?.let { uid ->
                userRepository.pushWatchItem(uid, updated)
            }
            _uiState.update { it.copy(isSaving = false, item = updated) }
            _savedEvent.emit(Unit)
            onSuccess?.invoke()
        }
    }

    private fun addToWatchlist(onSuccess: (() -> Unit)? = null) {
        val item = _uiState.value.item ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            // Check for duplicates
            if (item.imdbId.isNotBlank()) {
                val existing = repository.getItemByImdbId(item.imdbId)
                if (existing != null) {
                    _uiState.update { it.copy(isSaving = false, toastMessage = "Already in Watchlist ✓") }
                    onSuccess?.invoke()
                    return@launch
                }
            }
            
            val watchItem = item.copy(id = 0) // reset id for Room auto-generate
            val newId = repository.saveItem(watchItem)
            
            // Sync to cloud
            val updated = watchItem.copy(id = newId)
            userRepository.getLocalUserId()?.let { uid ->
                userRepository.pushWatchItem(uid, updated)
            }
            
            _uiState.update { it.copy(isSaving = false, isPreview = false, item = updated, toastMessage = "Added to Watchlist ✓") }
            _savedEvent.emit(Unit)
            onSuccess?.invoke()
            
            // Since it's now saved, we should reload the item via normal flow to get updates
            repository.getItemByIdFlow(newId).collect { newItem ->
                if (newItem != null) {
                    _uiState.update { it.copy(item = newItem) }
                }
            }
        }
    }

    fun deleteItem(onSuccess: () -> Unit) {
        val item = _uiState.value.item ?: return
        viewModelScope.launch {
            seriesRepository.deleteProgress(item.id)
            repository.deleteItem(item)
            // Remove from cloud
            userRepository.getLocalUserId()?.let { uid ->
                userRepository.deleteFromWatchlist(uid, item)
            }
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    class Factory(
        private val repository: WatchItemRepository,
        private val seriesRepository: SeriesRepository,
        private val userRepository: UserRepository,
        private val omdbRepository: OmdbRepository,
        private val traktRepository: TraktRepository,
        private val itemId: Long,
        private val previewImdbId: String? = null,
        private val previewTitle: String? = null,
        private val previewType: String? = null,
        private val previewPoster: String? = null,
        private val previewRating: Float = 0f,
        private val previewNotes: String = "",
        private val previewGenres: String = "",
        private val previewYear: Int = 0,
        private val previewSeason: Int = 1,
        private val previewEpisode: Int = 1
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DetailViewModel(repository, seriesRepository, userRepository, omdbRepository, traktRepository, itemId, previewImdbId, previewTitle, previewType, previewPoster, previewRating, previewNotes, previewGenres, previewYear, previewSeason, previewEpisode) as T
    }
}
