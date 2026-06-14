package com.kaze.ui.screens.profile

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kaze.data.local.WatchLaterDatabase
import com.kaze.data.repository.SupabaseUser
import com.kaze.data.repository.UserRepository
import com.kaze.model.MediaType
import com.kaze.model.WatchItem
import com.kaze.ui.components.UserAvatar
import com.kaze.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── Share URL ─────────────────────────────────────────────────────────────────

private const val DEEP_LINK_BASE = "https://kenzbilal.github.io/Kaze/u"
private const val APP_DOWNLOAD_URL = "https://github.com/KenzBilal/Kaze/releases/latest/download/app-release.apk"

// ── ViewModel ─────────────────────────────────────────────────────────────────

class MyProfileViewModel(
    private val repository: UserRepository,
    private val dao: com.kaze.data.local.WatchItemDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyProfileUiState())
    val uiState: StateFlow<MyProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dao.getFavoriteItems().collect { favs ->
                _uiState.update { it.copy(favoriteItems = favs) }
            }
        }
        load() 
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadInternal()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loadInternal()
        }
    }

    private suspend fun loadInternal() {
            val userId = repository.getLocalUserId() ?: return
            val user = repository.getUserById(userId)
            val allWatched = dao.getAllItemsOnce().filter { it.isWatched }
            val followersCount = repository.getFollowersCount(userId)
            val followingCount = repository.getFollowingCount(userId)

            _uiState.update {
                it.copy(
                    user = user,
                    userId = userId,
                    watchedItems = allWatched,
                    followersCount = followersCount,
                    followingCount = followingCount,
                    isLoading = false,
                    isAdmin = user?.username.equals("kenzbilal", ignoreCase = true)
                )
            }
    }

    fun saveProfile() {
        val s = _uiState.value
        val uid = s.userId ?: return
        viewModelScope.launch {
            repository.updateProfile(uid, null, null, null)
            _uiState.update {
                it.copy(
                    isEditing = false
                )
            }
        }
    }

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true) }
    }

    fun cancelEditing() = _uiState.update { it.copy(isEditing = false) }

    class Factory(private val context: android.content.Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = WatchLaterDatabase.getInstance(context)
            return MyProfileViewModel(UserRepository(context), db.watchItemDao()) as T
        }
    }
}

data class MyProfileUiState(
    val user: SupabaseUser? = null,
    val userId: String? = null,
    val watchedItems: List<WatchItem> = emptyList(),
    val favoriteItems: List<WatchItem> = emptyList(),
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isEditing: Boolean = false,
    val isAdmin: Boolean = false
)

// ── Constants ─────────────────────────────────────────────────────────────────

private val GENRES = listOf(
    "Action", "Adventure", "Animation", "Comedy", "Crime", "Documentary",
    "Drama", "Fantasy", "Horror", "Mystery", "Romance", "Sci-Fi", "Thriller"
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(onSettingsClick: () -> Unit = {}) {
    val context = LocalContext.current
    val viewModel: MyProfileViewModel = viewModel(factory = MyProfileViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()

    var showAdminDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
                actions = {
                    if (uiState.user != null) {
                        IconButton(onClick = {
                            val username = uiState.user?.username ?: ""
                            val profileUrl = "$DEEP_LINK_BASE/$username"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Check out @$username on Kaze!\n$profileUrl\n\nDon't have Kaze? Download it:\n$APP_DOWNLOAD_URL"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Profile"))
                        }) {
                            Icon(Icons.Filled.Share, "Share Profile", tint = TextSecondary)
                        }
                    }
                }
            )
        },
        containerColor = Background
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = TextSecondary, strokeWidth = 1.5.dp)
            }
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val user = uiState.user
                if (user != null) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        ProfileHeroSection(user = user, uiState = uiState)
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        StatsBar(uiState = uiState)
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = SurfaceHighlight
                        )
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        SectionLabel("FAVORITES")
                        Spacer(Modifier.height(12.dp))
                    }
                    if (uiState.favoriteItems.isEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                text = "No favorites yet. Mark watched items with the heart icon to see them here.",
                                color = TextTertiary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 20.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(uiState.favoriteItems, key = { it.id }) { item ->
                            FavoriteCard(item = item, onClick = {})
                        }
                        if (uiState.isAdmin) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SurfaceHighlight)
                                SectionLabel("ADMIN")
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { showAdminDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceHighlight, contentColor = TextPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Manage Global Chat Users")
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }

        if (showAdminDialog) {
            com.kaze.ui.components.AdminChatDialog(
                userRepository = com.kaze.data.repository.UserRepository(context),
                onDismiss = { showAdminDialog = false }
            )
        }
    }
}

// ── Profile Hero ──────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeroSection(user: SupabaseUser, uiState: MyProfileUiState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .border(1.dp, SurfaceHighlight, CircleShape)
            )
            UserAvatar(username = user.username, size = 88.dp, fontSize = 34.sp)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            user.username,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            letterSpacing = (-0.5).sp
        )
        Spacer(Modifier.height(4.dp))
        val movieCount  = uiState.watchedItems.count { it.type == MediaType.MOVIE }
        val seriesCount = uiState.watchedItems.count { it.type == MediaType.SERIES }
        Text(
            "$movieCount movies · $seriesCount series watched",
            fontSize = 13.sp,
            color = TextTertiary
        )
    }
}

// ── Stats Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsBar(uiState: MyProfileUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(label = "Followers", value = "${uiState.followersCount}")
        StatDivider()
        StatItem(label = "Following", value = "${uiState.followingCount}")
        StatDivider()
        StatItem(label = "Watched",   value = "${uiState.watchedItems.size}")
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = TextTertiary, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(SurfaceHighlight)
    )
}

@Composable
private fun FavoriteCard(item: WatchItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
    ) {
        Box {
            if (item.posterUrl != null) {
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
                        imageVector = if (item.type.name == "SERIES") Icons.Filled.Tv else Icons.Filled.Movie,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Background.copy(alpha = 0.7f))
                    .padding(6.dp)
            ) {
                Icon(Icons.Filled.Favorite, null, tint = Color.Red, modifier = Modifier.size(16.dp))
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 10.sp,
        color = TextTertiary,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
