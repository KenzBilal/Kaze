package com.kaze.ui.screens.discover

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kaze.data.remote.DiscoverItem
import com.kaze.data.remote.OmdbRepository
import com.kaze.data.remote.TraktMovie
import com.kaze.data.remote.TraktRepository
import com.kaze.data.remote.TraktShow
import com.kaze.data.repository.PublicWatchlistItem
import com.kaze.data.repository.UserRepository
import com.kaze.data.repository.WatchItemRepository
import com.kaze.ui.components.EmptyState
import com.kaze.ui.components.WatchLaterLoader
import com.kaze.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

class DiscoverViewModel(
    private val repository: WatchItemRepository,
    private val userRepo: UserRepository,
    private val traktRepo: TraktRepository,
    private val omdbRepo: OmdbRepository,
    private val cacheRepo: com.kaze.data.repository.DiscoverCacheRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var currentFriendsWatchlists: List<PublicWatchlistItem> = emptyList()
    private var currentTraktMovies: MutableList<TraktMovie> = mutableListOf()
    private var currentTraktShows: MutableList<TraktShow> = mutableListOf()
    private val posterCache = mutableMapOf<String, String?>() // IMDB ID -> Poster URL
    
    private var currentPage = 1
    private var isLoadingMore = false

    init { 
        load() 
        viewModelScope.launch {
            repository.getAllItemsFlow().collect { ownItems ->
                val ownImdbIds = ownItems.map { it.imdbId }.filter { it.isNotBlank() }.toSet()
                _uiState.update { it.copy(ownImdbIds = ownImdbIds) }
                if (currentFriendsWatchlists.isNotEmpty() || currentTraktMovies.isNotEmpty()) {
                    recalculateSuggestions()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            currentPage = 1
            currentTraktMovies.clear()
            currentTraktShows.clear()
            loadInternal()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            currentPage = 1
            currentTraktMovies.clear()
            currentTraktShows.clear()
            loadInternal()
        }
    }
    
    fun loadMore() {
        if (isLoadingMore) return
        isLoadingMore = true
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            currentPage++
            // Trakt pagination: limit 20. But the user asked for 30 (15 movies, 15 shows)
            val limit = if (currentPage == 1) 25 else 15
            
            val moviesDeferred = async { traktRepo.getTrendingMovies(currentPage, limit) }
            val showsDeferred = async { traktRepo.getTrendingShows(currentPage, limit) }
            
            val newMovies = moviesDeferred.await()
            val newShows = showsDeferred.await()
            
            currentTraktMovies.addAll(newMovies)
            currentTraktShows.addAll(newShows)
            
            recalculateSuggestions()
            isLoadingMore = false
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    private suspend fun loadInternal() {
        val userId = userRepo.getLocalUserId() ?: run {
            _uiState.update { it.copy(isLoading = false, isEmpty = true, isLoggedIn = false) }
            return
        }

        val ownItems = repository.getAllItemsSnapshot()
        val ownImdbIds = ownItems.map { it.imdbId }.filter { it.isNotBlank() }.toSet()

        val topGenre = ownItems
            .filter { it.isWatched }
            .flatMap { it.genreList }
            .groupingBy { it }
            .eachCount()
            .entries
            .maxByOrNull { it.value }?.key ?: ""
            
        _uiState.update { it.copy(topGenre = topGenre) }

        val following = userRepo.getFollowingList(userId)
        val followedIds = following.map { it.id }

        // Fetch all data concurrently
        // Initial limit is 25 per Trakt category so we get 50 total Trakt items + Friends items.
        val friendsDeferred = viewModelScope.async { userRepo.getWatchlistsByUserIds(followedIds) }
        val moviesDeferred = viewModelScope.async { traktRepo.getTrendingMovies(currentPage, 25) }
        val showsDeferred = viewModelScope.async { traktRepo.getTrendingShows(currentPage, 25) }

        currentFriendsWatchlists = friendsDeferred.await()
        currentTraktMovies.addAll(moviesDeferred.await())
        currentTraktShows.addAll(showsDeferred.await())
        
        recalculateSuggestions()
    }
    
    private suspend fun recalculateSuggestions() {
        val state = _uiState.value
        val ownImdbIds = state.ownImdbIds
        val topGenre = state.topGenre

        // 1. Process Friends Items
        val friendsSuggestions = currentFriendsWatchlists
            .filter { it.imdb_id !in ownImdbIds && it.imdb_id.isNotBlank() }
            .groupBy { it.imdb_id }
            .map { entry -> entry.value.maxByOrNull { it.rating }!! }
            .sortedWith(
                compareByDescending<PublicWatchlistItem> { topGenre.isNotEmpty() && it.genres.contains(topGenre, ignoreCase = true) }
                .thenByDescending { it.rating }
                .thenByDescending { it.date_added }
            )
            .take(30)
            .map {
                DiscoverItem(
                    title = it.title,
                    year = it.year,
                    type = it.type,
                    imdbId = it.imdb_id,
                    posterUrl = it.poster_url,
                    rating = it.rating,
                    notes = "Recommended by friend",
                    genres = it.genres
                )
            }
            
        friendsSuggestions.forEach { if (it.posterUrl != null) posterCache[it.imdbId] = it.posterUrl }

        // 2. Process Trakt Movies
        val traktMovies = currentTraktMovies
            .filter { it.ids.imdb != null && it.ids.imdb !in ownImdbIds }
            .map { 
                DiscoverItem(
                    title = it.title,
                    year = it.year ?: 0,
                    type = "MOVIE",
                    imdbId = it.ids.imdb!!,
                    posterUrl = posterCache[it.ids.imdb],
                    notes = "Trending Movie"
                )
            }

        // 3. Process Trakt Shows
        val traktShows = currentTraktShows
            .filter { it.ids.imdb != null && it.ids.imdb !in ownImdbIds }
            .map { 
                DiscoverItem(
                    title = it.title,
                    year = it.year ?: 0,
                    type = "SERIES",
                    imdbId = it.ids.imdb!!,
                    posterUrl = posterCache[it.ids.imdb],
                    notes = "Trending Series"
                )
            }

        // 4. Mix them (Friends first, then alternate Movies/Shows)
        val mixedList = mutableListOf<DiscoverItem>()
        mixedList.addAll(friendsSuggestions)
        
        val maxLen = maxOf(traktMovies.size, traktShows.size)
        for (i in 0 until maxLen) {
            if (i < traktShows.size) mixedList.add(traktShows[i])
            if (i < traktMovies.size) mixedList.add(traktMovies[i])
        }

        // Remove duplicates just in case
        val finalSuggestions = mixedList.distinctBy { it.imdbId }
        
        // 5. Check global Supabase cache for missing posters before updating UI
        val missingImdbIds = finalSuggestions.filter { it.posterUrl == null }.map { it.imdbId }
        if (missingImdbIds.isNotEmpty()) {
            val cachedMap = cacheRepo.getCachedItems(missingImdbIds)
            cachedMap.forEach { (imdb, item) ->
                if (item.posterUrl != null) posterCache[imdb] = item.posterUrl
            }
        }
        
        val suggestionsWithCachedPosters = finalSuggestions.map { 
            if (it.posterUrl == null && posterCache[it.imdbId] != null) {
                it.copy(posterUrl = posterCache[it.imdbId])
            } else {
                it
            }
        }

        _uiState.update {
            it.copy(
                suggestions = suggestionsWithCachedPosters,
                isLoading = false,
                isEmpty = suggestionsWithCachedPosters.isEmpty()
            )
        }

        // 6. Lazy load remaining missing posters from OMDB and save to Supabase Cache
        val missingFromOmdb = suggestionsWithCachedPosters.filter { it.posterUrl == null }.take(15)
        if (missingFromOmdb.isNotEmpty()) {
            viewModelScope.launch {
                missingFromOmdb.forEach { item ->
                    try {
                        val detail = omdbRepo.api.getDetail(item.imdbId, omdbRepo.apiKey)
                        val poster = detail.poster?.takeIf { it != "N/A" }
                        if (poster != null) {
                            posterCache[item.imdbId] = poster
                            
                            val fullItem = item.copy(
                                posterUrl = poster,
                                genres = detail.genre ?: "",
                                rating = detail.imdbRating?.toFloatOrNull() ?: 0f
                            )
                            
                            // Save to global cache so others don't have to query OMDB
                            cacheRepo.cacheItem(fullItem)
                            
                            // Trigger re-emit to update UI with fetched poster
                            val updatedList = _uiState.value.suggestions.map { 
                                if (it.imdbId == item.imdbId) fullItem else it
                            }
                            _uiState.update { it.copy(suggestions = updatedList) }
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }

    class Factory(
        private val context: android.content.Context, 
        private val repository: WatchItemRepository,
        private val traktRepository: TraktRepository,
        private val omdbRepository: OmdbRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appContainer = (context.applicationContext as com.kaze.KazeApplication).container
            return DiscoverViewModel(
                repository,
                UserRepository(context),
                traktRepository,
                omdbRepository,
                appContainer.discoverCacheRepository
            ) as T
        }
    }
}

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val suggestions: List<DiscoverItem> = emptyList(),
    val isEmpty: Boolean = false,
    val isLoggedIn: Boolean = true,
    val ownImdbIds: Set<String> = emptySet(),
    val topGenre: String = ""
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoverScreen(
    repository: WatchItemRepository,
    traktRepository: TraktRepository,
    omdbRepository: OmdbRepository,
    onItemClick: (DiscoverItem) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val viewModel: DiscoverViewModel = viewModel(
        factory = DiscoverViewModel.Factory(context, repository, traktRepository, omdbRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Discover",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Trending and from friends",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        when {
            uiState.isLoading -> WatchLaterLoader()
            uiState.isEmpty -> EmptyState(
                icon = if (uiState.isLoggedIn) Icons.Outlined.Explore else Icons.Default.Movie,
                title = if (uiState.isLoggedIn) "Nothing to discover yet" else "Not signed in",
                subtitle = if (uiState.isLoggedIn) "Check back later" else "Sign in to see\nwhat friends are watching",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp,
                        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                    ) {
                        items(uiState.suggestions, key = { it.imdbId }) { item ->
                            DiscoverCard(
                                item = item,
                                onClick = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onItemClick(item) 
                                }
                            )
                        }
                        
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { viewModel.loadMore() },
                                    enabled = !uiState.isLoadingMore,
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                ) {
                                    if (uiState.isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Background,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Load More", color = Background, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverCard(item: DiscoverItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
    ) {
        if (!item.posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(SurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.type.uppercase() == "SERIES") Icons.Filled.Tv else Icons.Filled.Movie,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.type.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                    if (item.rating > 0f) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = androidx.compose.ui.graphics.Color(0xFFFFC107),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", item.rating),
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}

