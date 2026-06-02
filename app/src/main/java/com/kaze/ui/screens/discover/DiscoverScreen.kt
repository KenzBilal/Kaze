package com.kaze.ui.screens.discover

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.graphics.Color
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
import com.kaze.data.repository.ActivityFeedEntry
import com.kaze.data.repository.ActivityRepository
import com.kaze.data.repository.PublicWatchlistItem
import com.kaze.data.repository.UserRepository
import com.kaze.data.repository.WatchItemRepository
import com.kaze.model.MediaType
import com.kaze.model.WatchItem
import com.kaze.ui.components.EmptyState
import com.kaze.ui.components.WatchLaterLoader
import com.kaze.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

class DiscoverViewModel(
    private val repository: WatchItemRepository,
    private val activityRepo: ActivityRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadInternal()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loadInternal()
        }
    }

    private suspend fun loadInternal() {
            val userId = userRepo.getLocalUserId() ?: run {
                _uiState.update { it.copy(isLoading = false, isEmpty = true, isLoggedIn = false) }
                return
            }

            // Get items user already has in their own list
            val ownItems = repository.getAllItemsSnapshot()
            val ownImdbIds = ownItems.map { it.imdbId }.filter { it.isNotBlank() }.toSet()

            // Calculate Top Genre
            val topGenre = ownItems
                .filter { it.isWatched }
                .flatMap { it.genreList }
                .groupingBy { it }
                .eachCount()
                .entries
                .maxByOrNull { it.value }?.key ?: ""

            // Get followed users
            val following = userRepo.getFollowingList(userId)
            val followedIds = following.map { it.id }

            // Fetch public watchlists of friends
            val friendsWatchlists = userRepo.getWatchlistsByUserIds(followedIds)

            // Filter out own items, then sort by genre match, then rating
            val suggestions = friendsWatchlists
                .filter { it.imdb_id !in ownImdbIds && it.imdb_id.isNotBlank() }
                .groupBy { it.imdb_id }
                .map { entry -> entry.value.maxByOrNull { it.rating }!! }
                .sortedWith(
                    compareByDescending<PublicWatchlistItem> { 
                        topGenre.isNotEmpty() && it.genres.contains(topGenre, ignoreCase = true) 
                    }
                    .thenByDescending { it.rating }
                    .thenByDescending { it.date_added }
                )
                .take(50)

            _uiState.update {
                it.copy(
                    suggestions = suggestions,
                    isLoading = false,
                    isEmpty = suggestions.isEmpty()
                )
            }
    }

    class Factory(private val context: android.content.Context, private val repository: WatchItemRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DiscoverViewModel(
                repository,
                ActivityRepository(context),
                UserRepository(context)
            ) as T
        }
    }
}

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val suggestions: List<PublicWatchlistItem> = emptyList(),
    val isEmpty: Boolean = false,
    val isLoggedIn: Boolean = true
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoverScreen(
    repository: WatchItemRepository,
    onItemClick: (PublicWatchlistItem) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val viewModel: DiscoverViewModel = viewModel(factory = DiscoverViewModel.Factory(context, repository))
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
                            "From your friends' watchlists",
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
                subtitle = if (uiState.isLoggedIn) "Follow friends to see\nwhat they're watching" else "Sign in to see\nwhat friends are watching",
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
                        items(uiState.suggestions, key = { it.user_id + "_" + it.imdb_id }) { item ->
                            DiscoverCard(
                                item = item,
                                onClick = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onItemClick(item) 
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverCard(item: PublicWatchlistItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
    ) {
        if (!item.poster_url.isNullOrBlank()) {
            AsyncImage(
                model = item.poster_url,
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
                    .height(120.dp)
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
