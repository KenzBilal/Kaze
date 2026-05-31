package com.watchlater.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.watchlater.ui.components.UserAvatar
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

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val localId = repository.getLocalUserId()
            val user = repository.getUserById(profileUserId)
            val watchlist = repository.getWatchlistByUserId(profileUserId)
            val followersCount = repository.getFollowersCount(profileUserId)
            val followingCount = repository.getFollowingCount(profileUserId)
            val isFollowing = if (localId != null && localId != profileUserId)
                repository.isFollowing(localId, profileUserId) else false

            _uiState.update {
                it.copy(
                    user = user,
                    watchlist = watchlist,
                    followersCount = followersCount,
                    followingCount = followingCount,
                    isFollowing = isFollowing,
                    isOwnProfile = localId == profileUserId,
                    localUserId = localId,
                    isLoading = false
                )
            }
        }
    }

    fun toggleFollow() {
        val lid = _uiState.value.localUserId ?: return
        viewModelScope.launch {
            if (_uiState.value.isFollowing) {
                repository.unfollowUser(lid, profileUserId)
                _uiState.update { it.copy(isFollowing = false, followersCount = it.followersCount - 1) }
            } else {
                repository.followUser(lid, profileUserId)
                _uiState.update { it.copy(isFollowing = true, followersCount = it.followersCount + 1) }
            }
        }
    }

    fun addToMyWatchlist(item: PublicWatchlistItem) {
        viewModelScope.launch {
            try {
                val mediaType = try { MediaType.valueOf(item.type) } catch (e: Exception) { MediaType.MOVIE }
                val existing = if (item.imdb_id.isNotBlank()) dao.getItemByImdbId(item.imdb_id)
                               else dao.getItemByTitleYearType(item.title, item.year, mediaType)
                if (existing != null) {
                    _uiState.update { s -> s.copy(addedItemIds = s.addedItemIds + item.title) }
                    return@launch
                }
                dao.insertItem(WatchItem(
                    title = item.title, year = item.year, type = mediaType,
                    isWatched = false, rating = 0f, notes = "",
                    posterUrl = item.poster_url, genres = item.genres, imdbId = item.imdb_id,
                    dateAdded = System.currentTimeMillis(), lastUpdated = System.currentTimeMillis()
                ))
                _uiState.update { s -> s.copy(addedItemIds = s.addedItemIds + item.title) }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun showFollowers() {
        viewModelScope.launch {
            _uiState.update { it.copy(listDialogTitle = "Followers", listDialogUsers = null) }
            val list = repository.getFollowersList(profileUserId)
            _uiState.update { it.copy(listDialogUsers = list) }
        }
    }

    fun showFollowing() {
        viewModelScope.launch {
            _uiState.update { it.copy(listDialogTitle = "Following", listDialogUsers = null) }
            val list = repository.getFollowingList(profileUserId)
            _uiState.update { it.copy(listDialogUsers = list) }
        }
    }

    fun dismissDialog() = _uiState.update { it.copy(listDialogUsers = null, listDialogTitle = null) }

    fun selectItem(item: PublicWatchlistItem) = _uiState.update { it.copy(selectedItem = item) }
    fun dismissItem() = _uiState.update { it.copy(selectedItem = null) }

    class Factory(private val context: android.content.Context, private val userId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = WatchLaterDatabase.getInstance(context)
            return UserProfileViewModel(UserRepository(context), db.watchItemDao(), userId) as T
        }
    }
}

data class UserProfileUiState(
    val isLoading: Boolean = true,
    val user: SupabaseUser? = null,
    val watchlist: List<PublicWatchlistItem> = emptyList(),
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val isOwnProfile: Boolean = false,
    val localUserId: String? = null,
    val addedItemIds: Set<String> = emptySet(),
    val listDialogTitle: String? = null,
    val listDialogUsers: List<SupabaseUser>? = null,
    val selectedItem: PublicWatchlistItem? = null
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: UserProfileViewModel = viewModel(factory = UserProfileViewModel.Factory(context, userId))
    val uiState by viewModel.uiState.collectAsState()

    // Followers/Following dialog
    if (uiState.listDialogTitle != null) {
        UserListDialog(
            title = uiState.listDialogTitle!!,
            users = uiState.listDialogUsers,
            onUserClick = { u -> viewModel.dismissDialog(); onUserClick(u.id) },
            onDismiss = viewModel::dismissDialog
        )
    }

    // Item detail bottom sheet
    uiState.selectedItem?.let { item ->
        ItemDetailSheet(
            item = item,
            isAdded = uiState.addedItemIds.contains(item.title),
            isOwnProfile = uiState.isOwnProfile,
            onAdd = { viewModel.addToMyWatchlist(item) },
            onDismiss = viewModel::dismissItem
        )
    }

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
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = TextSecondary, strokeWidth = 2.dp)
            }
            uiState.user == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("User not found", color = TextTertiary)
            }
            else -> {
                val user = uiState.user!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Header
                    item {
                        ProfileHeader(
                            user = user,
                            followersCount = uiState.followersCount,
                            followingCount = uiState.followingCount,
                            isFollowing = uiState.isFollowing,
                            isOwnProfile = uiState.isOwnProfile,
                            onFollowClick = viewModel::toggleFollow,
                            onFollowersClick = viewModel::showFollowers,
                            onFollowingClick = viewModel::showFollowing
                        )
                    }

                    // Watchlist header
                    item {
                        Text(
                            "Watchlist",
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Pinterest grid
                    item {
                        if (uiState.watchlist.isEmpty()) {
                            Box(
                                Modifier.fillMaxWidth().padding(16.dp)
                                    .clip(RoundedCornerShape(12.dp)).background(SurfaceElevated)
                                    .padding(24.dp),
                                Alignment.Center
                            ) { Text("No items yet", color = TextTertiary) }
                        } else {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 2000.dp) // nested scroll workaround
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalItemSpacing = 8.dp,
                                userScrollEnabled = false
                            ) {
                                items(uiState.watchlist) { item ->
                                    PinterestCard(
                                        item = item,
                                        onClick = { viewModel.selectItem(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Profile Header ────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(
    user: SupabaseUser,
    followersCount: Int,
    followingCount: Int,
    isFollowing: Boolean,
    isOwnProfile: Boolean,
    onFollowClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserAvatar(username = user.username, size = 80.dp, fontSize = 30.sp)
        Spacer(Modifier.height(12.dp))
        Text(user.username, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        Spacer(Modifier.height(16.dp))

        // Followers / Following counts
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            StatChip(count = followersCount, label = "Followers", onClick = onFollowersClick)
            StatChip(count = followingCount, label = "Following", onClick = onFollowingClick)
        }

        // Fav fields
        if (!user.fav_movie.isNullOrBlank() || !user.fav_series.isNullOrBlank() || !user.fav_genre.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                user.fav_movie?.takeIf { it.isNotBlank() }?.let {
                    Text("Fav Movie: $it", color = TextSecondary, fontSize = 13.sp)
                }
                user.fav_series?.takeIf { it.isNotBlank() }?.let {
                    Text("Fav Series: $it", color = TextSecondary, fontSize = 13.sp)
                }
                user.fav_genre?.takeIf { it.isNotBlank() }?.let {
                    Text("Genre: $it", color = TextTertiary, fontSize = 12.sp)
                }
            }
        }

        if (!isOwnProfile) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onFollowClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) SurfaceElevated else TextPrimary,
                    contentColor = if (isFollowing) TextPrimary else Background
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(120.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(if (isFollowing) "Following" else "Follow", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = SurfaceHighlight)
    }
}

@Composable
private fun StatChip(count: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text("$count", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, fontSize = 12.sp, color = TextTertiary)
    }
}

// ── Pinterest Card ────────────────────────────────────────────────────────────

@Composable
private fun PinterestCard(item: PublicWatchlistItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
    ) {
        if (!item.poster_url.isNullOrBlank()) {
            AsyncImage(
                model = item.poster_url,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            )
        } else {
            Box(
                Modifier.fillMaxWidth().height(120.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(SurfaceHighlight),
                Alignment.Center
            ) {
                Icon(
                    if (item.type == "SERIES") Icons.Filled.Tv else Icons.Filled.Movie,
                    null, tint = TextTertiary, modifier = Modifier.size(28.dp)
                )
            }
        }
        Column(Modifier.padding(8.dp)) {
            Text(
                item.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${item.year}", fontSize = 11.sp, color = TextTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (item.rating > 0f) {
                Text("★ ${item.rating}", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

// ── Item Detail Bottom Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailSheet(
    item: PublicWatchlistItem,
    isAdded: Boolean,
    isOwnProfile: Boolean,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceContainer,
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 8.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(SurfaceHighlight)) }
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (!item.poster_url.isNullOrBlank()) {
                AsyncImage(
                    model = item.poster_url, contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(90.dp).height(130.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(item.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("${item.year}  ·  ${item.type.lowercase().replaceFirstChar { it.uppercase() }}", color = TextTertiary, fontSize = 13.sp)
                if (item.genres.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(item.genres, color = TextSecondary, fontSize = 12.sp)
                }
                if (item.season != null && item.episode != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("S${item.season} E${item.episode}", color = TextSecondary, fontSize = 12.sp)
                }
                if (item.rating > 0f) {
                    Spacer(Modifier.height(4.dp))
                    Text("★ ${item.rating} / 10", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        if (item.notes.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "\"${item.notes}\"", color = TextSecondary, fontSize = 13.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        if (!isOwnProfile) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAdd, enabled = !isAdded,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAdded) SurfaceElevated else TextPrimary,
                    contentColor = if (isAdded) TextTertiary else Background
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(if (isAdded) Icons.Filled.Check else Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isAdded) "Added to watchlist" else "Add to my watchlist", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ── User List Dialog ──────────────────────────────────────────────────────────

@Composable
private fun UserListDialog(
    title: String,
    users: List<SupabaseUser>?,
    onUserClick: (SupabaseUser) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            if (users == null) {
                Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) {
                    CircularProgressIndicator(color = TextSecondary, strokeWidth = 2.dp)
                }
            } else if (users.isEmpty()) {
                Text("Nobody here yet", color = TextTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users, key = { it.id }) { user ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .clickable { onUserClick(user) }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(username = user.username, size = 36.dp, fontSize = 14.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(user.username, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) }
        }
    )
}
