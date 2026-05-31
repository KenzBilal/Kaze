package com.watchlater.ui.screens.add

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.watchlater.data.remote.OmdbRepository
import com.watchlater.data.remote.OmdbResult
import com.watchlater.data.repository.WatchItemRepository
import com.watchlater.model.MediaType
import com.watchlater.model.WatchItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddItemUiState(
    val title: String = "",
    val year: String = "",
    val type: MediaType = MediaType.MOVIE,
    val posterUrl: String? = null,
    val genres: String = "",
    val imdbId: String = "",
    val titleError: String? = null,
    val yearError: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val searchResults: List<OmdbResult> = emptyList(),
    val isSearching: Boolean = false,
    val showSuggestions: Boolean = false
)

class AddItemViewModel(
    private val repository: WatchItemRepository,
    private val omdbRepository: OmdbRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddItemUiState(
            title  = savedState.get<String>("title")  ?: "",
            year   = savedState.get<String>("year")   ?: "",
            type   = savedState.get<String>("type")?.let { MediaType.valueOf(it) } ?: MediaType.MOVIE,
            genres = savedState.get<String>("genres") ?: "",
            imdbId = savedState.get<String>("imdbId") ?: ""
        )
    )
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onTitleChange(value: String) {
        savedState["title"] = value
        _uiState.update { it.copy(title = value, titleError = null) }
        searchJob?.cancel()
        Log.d("AddItemVM", "onTitleChange: '${value}' len=${value.length} hasKey=${omdbRepository.hasApiKey}")
        if (value.length >= 2 && omdbRepository.hasApiKey) {
            searchJob = viewModelScope.launch {
                delay(400)
                _uiState.update { it.copy(isSearching = true, showSuggestions = true, titleError = null) }
                try {
                    val results = omdbRepository.search(value)
                    Log.d("AddItemVM", "Results: ${results.size}")
                    _uiState.update {
                        it.copy(
                            searchResults   = results,
                            isSearching     = false,
                            showSuggestions = results.isNotEmpty()
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isSearching     = false,
                            showSuggestions = false,
                            titleError      = "Network error: check connection"
                        )
                    }
                } finally {
                    _uiState.update { if (it.isSearching) it.copy(isSearching = false) else it }
                }
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList(), showSuggestions = false, isSearching = false) }
        }
    }

    fun onYearChange(value: String) {
        savedState["year"] = value
        _uiState.update { it.copy(year = value, yearError = null) }
    }

    fun onTypeChange(value: MediaType) {
        savedState["type"] = value.name
        _uiState.update { it.copy(type = value) }
    }

    fun dismissSuggestions() {
        _uiState.update { it.copy(showSuggestions = false) }
    }

    fun selectOmdbResult(result: OmdbResult) {
        val mediaType = if (result.mediaType == "tv") MediaType.SERIES else MediaType.MOVIE
        val year = result.displayYear.takeIf { it > 0 }?.toString() ?: ""

        savedState["title"]  = result.displayTitle
        savedState["year"]   = year
        savedState["type"]   = mediaType.name
        savedState["imdbId"] = result.omdbId

        _uiState.update {
            it.copy(
                title           = result.displayTitle,
                year            = year,
                type            = mediaType,
                posterUrl       = result.posterUrl,
                genres          = "",
                imdbId          = result.omdbId,
                showSuggestions = false,
                searchResults   = emptyList()
            )
        }

        // Fetch genre async
        if (result.omdbId.isNotBlank()) {
            viewModelScope.launch {
                val genre = omdbRepository.fetchGenre(result.omdbId)
                if (genre.isNotBlank()) {
                    savedState["genres"] = genre
                    _uiState.update { it.copy(genres = genre) }
                }
            }
        }
    }

    fun saveItem(onSuccess: () -> Unit) {
        val state = _uiState.value
        var hasError = false

        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Title is required") }
            hasError = true
        }
        val yearInt = state.year.toIntOrNull()
        if (state.year.isNotBlank() && (yearInt == null || yearInt < 1900 || yearInt > 2100)) {
            _uiState.update { it.copy(yearError = "Enter a valid year") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            val isDuplicate = repository.isDuplicate(
                imdbId = state.imdbId,
                title = state.title.trim(),
                year = yearInt ?: 0,
                type = state.type
            )
            
            if (isDuplicate) {
                _uiState.update { it.copy(titleError = "Item already in watchlist", isSaving = false) }
                return@launch
            }

            val item = WatchItem(
                title     = state.title.trim(),
                year      = yearInt ?: 0,
                type      = state.type,
                posterUrl = state.posterUrl,
                genres    = state.genres,
                imdbId    = state.imdbId,
                season    = if (state.type == MediaType.SERIES) 1 else null,
                episode   = if (state.type == MediaType.SERIES) 1 else null
            )
            repository.saveItem(item)
            savedState.remove<String>("title")
            savedState.remove<String>("year")
            savedState.remove<String>("type")
            savedState.remove<String>("genres")
            savedState.remove<String>("imdbId")
            _uiState.value = AddItemUiState()
            onSuccess()
        }
    }

    class Factory(
        private val repository: WatchItemRepository,
        private val omdbRepository: OmdbRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedState = extras.createSavedStateHandle()
            return AddItemViewModel(repository, omdbRepository, savedState) as T
        }
    }
}
