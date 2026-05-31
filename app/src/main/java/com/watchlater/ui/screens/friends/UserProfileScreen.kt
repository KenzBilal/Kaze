package com.watchlater.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tv
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.watchlater.data.local.WatchLaterDatabase
import com.watchlater.data.repository.PublicWatchlistItem
import com.watchlater.data.repository.SupabaseUser
import com.watchlater.data.repository.UserRepository
import com.watchlater.model.MediaType
import com.watchlater.model.WatchItem
import com.watchlater.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

class UserProfileViewModel(
    private val repository: UserRepository,
    private val dao: com.watchlater.data.local.WatchItemDao,
    private val profileUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Fetch user profile and their public watchlist in parallel
            val user = repository.getUserById(profileUserId)
            val watchlist = repository.getWatchlistByUserId(profileUserId)

            // Check current follow state from local prefs (simple local tracking for now)
            val localUserId = repository.getLocalUserId()

            _uiState.update {
                it.copy(
                    user = user,
                    watchlist = watchlist,
                    isLoading = false,
                    isOwnProfile = localUserId == profileUserId
                )
            }
        }
    }

    fun toggleFollow() {
        _uiState.update { it.copy(isFollowing = !it.isFollowing) }
        // TODO: persist to Supabase follows table when implemented
    }

    /**
     * Adds a public watchlist item from another user into the local Room database.
     * Does NOT touch their Supabase record — purely local operation on the viewer's device.
     */
    fun addToMyWatchlist(item: PublicWatchlistItem) {
        viewModelScope.launch {
            try {
                val mediaType = try {
                    MediaType.valueOf(item.type)
                } catch (e: IllegalArgumentException) {
                    MediaType.MOVIE
                }

                // Duplicate check: skip if already in local list
                val existing = if (item.imdb_id.isNotBlank()) {
                    dao.getItemByImdbId(item.imdb_id)
                } else {
                    dao.getItemByTitleYearType(item.title, item.year, mediaType)
                }

                if (existing != null) {
                    // Mark as already added in UI
                    _uiState.update { state ->
                        state.copy(addedItemIds = state.addedItemIds + item.title)
                    }
                    return@launch
                }

                val newItem = WatchItem(
                    title     = item.title,
                    year      = item.year,
                    type      = mediaType,
                    isWatched = false,
                    rating    = 0f,
                    notes     = "",
                    posterUrl = item.poster_url,
                    genres    = item.genres,
                    imdbId    = item.imdb_id,
                    dateAdded = System.currentTimeMillis(),
                    lastUpdated = System.currentTimeMillis()
                )
                dao.insertItem(newItem)

                _uiState.update { state ->
                    state.copy(addedItemIds = state.addedItemIds + item.title)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    class Factory(
        private val context: android.content.Context,
        private val userId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = WatchLaterDatabase.getInstance(context)
            return UserProfileViewModel(
                UserRepository(context),
                db.watchItemDao(),
                userId
            ) as T
        }
    }
}

data class UserProfileUiState(
    val isLoading: Boolean = true,
    val user: SupabaseUser? = null,
    val watchlist: List<PublicWatchlistItem> = emptyList(),
    val isFollowing: Boolean = false,
    val isOwnProfile: Boolean = false,
    val addedItemIds: Set<String> = emptySet()
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: UserProfileViewModel = viewModel(
        factory = UserProfileViewModel.Factory(context, userId)
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            }
            uiState.user == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("User not found", color = TextTertiary)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Profile Header
                    item {
                        ProfileHeader(
                            user = uiState.user!!,
                            isFollowing = uiState.isFollowing,
                            isOwnProfile = uiState.isOwnProfile,
                            onFollowClick = viewModel::toggleFollow
                        )
                    }

                    // Watchlist section title
                    item {
                        Text(
                            text = "Watchlist",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    if (uiState.watchlist.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceElevated)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No public items yet.",
                                    color = TextTertiary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        items(uiState.watchlist) { item ->
                            PublicWatchlistItemRow(
                                item = item,
                                isAdded = uiState.addedItemIds.contains(item.title),
                                isOwnProfile = uiState.isOwnProfile,
                                onAddClick = { viewModel.addToMyWatchlist(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    user: SupabaseUser,
    isFollowing: Boolean,
    isOwnProfile: Boolean,
    onFollowClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = "Profile",
                tint = AccentBlue,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "@${user.username}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Only show Follow button if not own profile
        if (!isOwnProfile) {
            Button(
                onClick = onFollowClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) SurfaceElevated else AccentBlue,
                    contentColor = if (isFollowing) TextPrimary else Background
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.width(130.dp)
            ) {
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = SurfaceHighlight)
    }
}

@Composable
private fun PublicWatchlistItemRow(
    item: PublicWatchlistItem,
    isAdded: Boolean,
    isOwnProfile: Boolean,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster thumbnail
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 76.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceHighlight),
            contentAlignment = Alignment.Center
        ) {
            if (!item.poster_url.isNullOrBlank()) {
                AsyncImage(
                    model = item.poster_url,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = if (item.type == "SERIES") Icons.Filled.Tv else Icons.Filled.Movie,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title + info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.year}  ·  ${item.type.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                fontSize = 12.sp
            )
            if (item.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${item.notes}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Add to my watchlist button — NOT shown on own profile
        if (!isOwnProfile) {
            IconButton(
                onClick = onAddClick,
                enabled = !isAdded,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAdded) AccentBlue.copy(alpha = 0.15f)
                        else AccentBlue.copy(alpha = 0.1f)
                    )
            ) {
                Icon(
                    imageVector = if (isAdded) Icons.Filled.Check else Icons.Filled.Add,
                    contentDescription = if (isAdded) "Already added" else "Add to my watchlist",
                    tint = AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
