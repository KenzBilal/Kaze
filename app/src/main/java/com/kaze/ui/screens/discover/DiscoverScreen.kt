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
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.kaze.data.local.WatchLaterDatabase
import com.kaze.data.repository.ActivityFeedItem
import com.kaze.data.repository.ActivityRepository
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

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = userRepo.getLocalUserId() ?: run {
                _uiState.update { it.copy(isLoading = false, isEmpty = true, isLoggedIn = false) }
                return@launch
            }

            // Get items user already has in their own list
            val ownItems = repository.getAllItemsOnce()
            val ownImdbIds = ownItems.map { it.imdbId }.filter { it.isNotBlank() }.toSet()

            // Get followed users
            val following = userRepo.getFollowingList(userId)
            val followedIds = following.map { it.id }

            // Fetch suggestions from friends, excluding own items
            val suggestions = activityRepo.getSocialSuggestions(followedIds, ownImdbIds)

            _uiState.update {
                it.copy(
                    suggestions = suggestions,
                    isLoading = false,
                    isEmpty = suggestions.isEmpty()
                )
            }
        }
    }

    fun addToList(item: ActivityFeedItem) {
        viewModelScope.launch {
            val watchItem = WatchItem(
                imdbId = item.item_imdb_id ?: return@launch,
                title = item.item_title ?: "Unknown",
                year = 0,
                type = try { MediaType.valueOf(item.item_type ?: "MOVIE") } catch(e: Exception) { MediaType.MOVIE },
                posterUrl = item.item_poster_url,
                dateAdded = System.currentTimeMillis(),
                lastUpdated = System.currentTimeMillis()
            )
            repository.insertItem(watchItem)
            val uid = userRepo.getLocalUserId()
            if (uid != null) {
                userRepo.pushWatchItem(uid, watchItem)
                activityRepo.postActivity(
                    com.kaze.data.repository.ActivityFeedEntry(
                        user_id = uid,
                        action_type = "added_item",
                        item_title = watchItem.title,
                        item_type = watchItem.type.name,
                        item_poster_url = watchItem.posterUrl,
                        item_imdb_id = watchItem.imdbId
                    )
                )
            }
            load() // refresh
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
    val suggestions: List<ActivityFeedItem> = emptyList(),
    val isEmpty: Boolean = false,
    val isLoggedIn: Boolean = true
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoverScreen(
    repository: WatchItemRepository,
    onItemClick: (Long) -> Unit
) {
    val context = LocalContext.current
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
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                ) {
                    items(uiState.suggestions, key = { it.id }) { item ->
                        DiscoverCard(
                            item = item,
                            onClick = { /* Could navigate to detail if we fetch item first, for now disabled since we don't have local item ID yet */ },
                            onAdd = { viewModel.addToList(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverCard(item: ActivityFeedItem, onClick: () -> Unit, onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
    ) {
        if (!item.item_poster_url.isNullOrBlank()) {
            AsyncImage(
                model = item.item_poster_url,
                contentDescription = item.item_title,
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
                    imageVector = if (item.item_type == "SERIES") Icons.Filled.Tv else Icons.Filled.Movie,
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
                    item.item_title ?: "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.item_type.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.item_type.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                }
            }
            IconButton(
                onClick = onAdd,
                modifier = Modifier.size(28.dp).background(AccentBlue, RoundedCornerShape(14.dp))
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add to list", tint = Background, modifier = Modifier.size(16.dp))
            }
        }
    }
}
