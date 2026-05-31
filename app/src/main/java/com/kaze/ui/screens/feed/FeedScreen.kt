package com.kaze.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.RssFeed
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
import com.kaze.data.repository.ActivityFeedItem
import com.kaze.data.repository.ActivityRepository
import com.kaze.data.repository.UserRepository
import com.kaze.ui.components.EmptyState
import com.kaze.ui.components.WatchLaterLoader
import com.kaze.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

class FeedViewModel(
    private val activityRepo: ActivityRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private var followedUserIds: List<String> = emptyList()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = userRepo.getLocalUserId() ?: run {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            // Get all followed user IDs
            val following = userRepo.getFollowingList(userId)
            followedUserIds = following.map { it.id }

            val feed = activityRepo.getFeedForUsers(followedUserIds, page = 0)
            currentPage = 0
            _uiState.update {
                it.copy(
                    feedItems = feed,
                    isLoading = false,
                    isEmpty = feed.isEmpty()
                )
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            currentPage++
            val more = activityRepo.getFeedForUsers(followedUserIds, page = currentPage)
            _uiState.update {
                it.copy(
                    feedItems = it.feedItems + more,
                    isLoadingMore = false
                )
            }
        }
    }

    fun refresh() { load() }

    class Factory(private val context: android.content.Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FeedViewModel(
                ActivityRepository(context),
                UserRepository(context)
            ) as T
        }
    }
}

data class FeedUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val feedItems: List<ActivityFeedItem> = emptyList(),
    val isEmpty: Boolean = false
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen() {
    val context = LocalContext.current
    val viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Activity",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        when {
            uiState.isLoading -> WatchLaterLoader()
            uiState.isEmpty -> EmptyState(
                icon = Icons.Outlined.RssFeed,
                title = "No activity yet",
                subtitle = "Follow people to see\nwhat they're watching",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp, ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.feedItems, key = { it.id }) { event ->
                        FeedEventCard(event = event)
                    }

                    if (uiState.feedItems.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                if (uiState.isLoadingMore) {
                                    CircularProgressIndicator(
                                        color = TextTertiary,
                                        strokeWidth = 1.5.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    TextButton(onClick = viewModel::loadMore) {
                                        Text("Load more", color = TextTertiary, fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedEventCard(event: ActivityFeedItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster or icon
        if (!event.item_poster_url.isNullOrBlank()) {
            AsyncImage(
                model = event.item_poster_url,
                contentDescription = event.item_title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    event.action_type == "followed" -> Icons.Filled.PersonAdd
                    event.item_type == "SERIES" -> Icons.Filled.Tv
                    else -> Icons.Filled.Movie
                }
                Icon(icon, null, tint = TextTertiary, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val actionText = when (event.action_type) {
                "added_item" -> "added to watchlist"
                "marked_watched" -> "marked as watched"
                "followed" -> "followed someone"
                else -> event.action_type
            }
            Text(
                buildString {
                    append(event.user_id.take(8)) // Short user ID until username resolved
                    append(" $actionText")
                },
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (!event.item_title.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    event.item_title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
