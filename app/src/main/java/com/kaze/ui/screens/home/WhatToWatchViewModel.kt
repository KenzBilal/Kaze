package com.kaze.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.kaze.data.local.WhatToWatchDao
import com.kaze.data.repository.WatchItemRepository
import com.kaze.model.WatchItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class WatchType { MOVIE, SERIES, BOTH }
enum class WatchLength { SHORT, MID, LONG } // Short 1-2, Mid 3-4, Long 5+

class WhatToWatchViewModel(
    private val dao: WhatToWatchDao,
    private val repository: WatchItemRepository
) : ViewModel() {

    private val _availableGenres = MutableStateFlow<List<String>>(emptyList())
    val availableGenres: StateFlow<List<String>> = _availableGenres.asStateFlow()

    private val _selectedType = MutableStateFlow(WatchType.BOTH)
    val selectedType: StateFlow<WatchType> = _selectedType.asStateFlow()

    private val _selectedGenres = MutableStateFlow<Set<String>>(emptySet())
    val selectedGenres: StateFlow<Set<String>> = _selectedGenres.asStateFlow()

    private val _selectedLength = MutableStateFlow<WatchLength?>(null)
    val selectedLength: StateFlow<WatchLength?> = _selectedLength.asStateFlow()

    private val _suggestedItem = MutableStateFlow<WatchItem?>(null)
    val suggestedItem: StateFlow<WatchItem?> = _suggestedItem.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllItemsSnapshot().let { items ->
                val genres = items.flatMap { it.genreList }.distinct().sorted()
                _availableGenres.value = genres
            }
        }
    }

    fun setType(type: WatchType) {
        _selectedType.value = type
        if (type == WatchType.MOVIE) {
            _selectedLength.value = null // Length not applicable to Movies
        }
    }

    fun toggleGenre(genre: String) {
        val current = _selectedGenres.value.toMutableSet()
        if (current.contains(genre)) current.remove(genre) else current.add(genre)
        _selectedGenres.value = current
    }

    fun setLength(length: WatchLength?) {
        _selectedLength.value = length
    }

    fun suggest() {
        viewModelScope.launch {
            val type = _selectedType.value
            val genres = _selectedGenres.value
            val length = _selectedLength.value

            var queryStr = "SELECT w.* FROM watch_items w "
            if (type == WatchType.SERIES || type == WatchType.BOTH) {
                queryStr += "LEFT JOIN series_cache s ON w.imdbId = s.imdbId "
            }
            queryStr += "WHERE w.isWatched = 0 "

            if (genres.isNotEmpty()) {
                val genreConditions = genres.joinToString(" OR ") { "w.genres LIKE '%$it%'" }
                queryStr += "AND ($genreConditions) "
            }

            when (type) {
                WatchType.MOVIE -> {
                    queryStr += "AND w.type = 'MOVIE' "
                }
                WatchType.SERIES -> {
                    queryStr += "AND w.type = 'SERIES' "
                    queryStr += getLengthCondition(length)
                }
                WatchType.BOTH -> {
                    queryStr += "AND (w.type = 'MOVIE' OR (w.type = 'SERIES' " + getLengthCondition(length) + ")) "
                }
            }

            queryStr += "ORDER BY RANDOM() LIMIT 1"
            
            val result = dao.getRandomSuggestion(SimpleSQLiteQuery(queryStr))
            _suggestedItem.value = result
        }
    }

    private fun getLengthCondition(length: WatchLength?): String {
        return when (length) {
            WatchLength.SHORT -> "AND s.totalSeasons BETWEEN 1 AND 2"
            WatchLength.MID -> "AND s.totalSeasons BETWEEN 3 AND 4"
            WatchLength.LONG -> "AND s.totalSeasons >= 5"
            null -> ""
        }
    }

    class Factory(
        private val dao: WhatToWatchDao,
        private val repository: WatchItemRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WhatToWatchViewModel(dao, repository) as T
    }
}
