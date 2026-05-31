package com.watchlater.ui.screens.profile

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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.watchlater.data.local.WatchLaterDatabase
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

// ── Share URL ─────────────────────────────────────────────────────────────────

private const val APP_DOWNLOAD_URL =
    "https://github.com/KenzBilal/Kaze/releases/latest/download/app-release.apk"

// ── ViewModel ─────────────────────────────────────────────────────────────────

class MyProfileViewModel(
    private val repository: UserRepository,
    private val dao: com.watchlater.data.local.WatchItemDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyProfileUiState())
    val uiState: StateFlow<MyProfileUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val userId = repository.getLocalUserId() ?: return@launch
            val user = repository.getUserById(userId)
            val allWatched = dao.getAllItemsOnce().filter { it.isWatched }
            val followersCount = repository.getFollowersCount(userId)
            val followingCount = repository.getFollowingCount(userId)

            // Auto-clear favs if the item was deleted from the list
            val watchedTitles = allWatched.map { it.title }.toSet()
            val safeFavMovie  = user?.fav_movie?.takeIf { it in watchedTitles } ?: ""
            val safeFavSeries = user?.fav_series?.takeIf { it in watchedTitles } ?: ""

            // If either fav is stale, clean up in Supabase silently
            if (user != null) {
                val needsClean = safeFavMovie != (user.fav_movie ?: "") ||
                                 safeFavSeries != (user.fav_series ?: "")
                if (needsClean) {
                    repository.updateProfile(
                        userId,
                        safeFavMovie.ifBlank { null },
                        safeFavSeries.ifBlank { null },
                        user.fav_genre
                    )
                }
            }

            _uiState.update {
                it.copy(
                    user = user?.copy(fav_movie = safeFavMovie.ifBlank { null },
                                     fav_series = safeFavSeries.ifBlank { null }),
                    userId = userId,
                    watchedItems = allWatched,
                    followersCount = followersCount,
                    followingCount = followingCount,
                    isLoading = false
                )
            }
        }
    }

    fun saveProfile() {
        val s = _uiState.value
        val uid = s.userId ?: return
        viewModelScope.launch {
            repository.updateProfile(uid,
                s.pendingFavMovie.ifBlank { null },
                s.pendingFavSeries.ifBlank { null },
                s.pendingFavGenre.ifBlank { null })
            _uiState.update {
                it.copy(
                    user = it.user?.copy(
                        fav_movie  = s.pendingFavMovie.ifBlank { null },
                        fav_series = s.pendingFavSeries.ifBlank { null },
                        fav_genre  = s.pendingFavGenre.ifBlank { null }
                    ),
                    isEditing = false
                )
            }
        }
    }

    fun startEditing() {
        val u = _uiState.value.user
        _uiState.update {
            it.copy(
                isEditing = true,
                pendingFavMovie  = u?.fav_movie  ?: "",
                pendingFavSeries = u?.fav_series ?: "",
                pendingFavGenre  = u?.fav_genre  ?: ""
            )
        }
    }

    fun cancelEditing() = _uiState.update { it.copy(isEditing = false) }
    fun setPendingFavMovie(v: String)  = _uiState.update { it.copy(pendingFavMovie = v) }
    fun setPendingFavSeries(v: String) = _uiState.update { it.copy(pendingFavSeries = v) }
    fun setPendingFavGenre(v: String)  = _uiState.update { it.copy(pendingFavGenre = v) }

    class Factory(private val context: android.content.Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = WatchLaterDatabase.getInstance(context)
            return MyProfileViewModel(UserRepository(context), db.watchItemDao()) as T
        }
    }
}

data class MyProfileUiState(
    val isLoading: Boolean = true,
    val userId: String? = null,
    val user: SupabaseUser? = null,
    val watchedItems: List<WatchItem> = emptyList(),
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isEditing: Boolean = false,
    val pendingFavMovie: String = "",
    val pendingFavSeries: String = "",
    val pendingFavGenre: String = ""
)

// ── Constants ─────────────────────────────────────────────────────────────────

private val GENRES = listOf(
    "Action", "Adventure", "Animation", "Comedy", "Crime", "Documentary",
    "Drama", "Fantasy", "Horror", "Mystery", "Romance", "Sci-Fi", "Thriller"
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen() {
    val context = LocalContext.current
    val viewModel: MyProfileViewModel = viewModel(factory = MyProfileViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
                actions = {
                    if (!uiState.isEditing && uiState.user != null) {
                        // Share button
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Hey! Check out Kaze — my favourite app to track movies & series.\n\nDownload it here:\n$APP_DOWNLOAD_URL"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Kaze"))
                        }) {
                            Icon(Icons.Filled.Share, "Share", tint = TextSecondary)
                        }
                        // Edit button
                        IconButton(onClick = viewModel::startEditing) {
                            Icon(Icons.Filled.Edit, "Edit", tint = TextSecondary)
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

        val user = uiState.user ?: return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Hero Header ───────────────────────────────────────────────────
            item {
                ProfileHeroSection(user = user, uiState = uiState)
            }

            // ── Stats Bar ─────────────────────────────────────────────────────
            item {
                StatsBar(uiState = uiState)
            }

            // ── Divider ───────────────────────────────────────────────────────
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    color = SurfaceHighlight
                )
            }

            // ── Edit or View mode ─────────────────────────────────────────────
            if (uiState.isEditing) {
                item {
                    Spacer(Modifier.height(16.dp))
                    EditFavSection(
                        uiState = uiState,
                        watchedItems = uiState.watchedItems,
                        onFavMovieChange  = viewModel::setPendingFavMovie,
                        onFavSeriesChange = viewModel::setPendingFavSeries,
                        onFavGenreChange  = viewModel::setPendingFavGenre,
                        onSave   = viewModel::saveProfile,
                        onCancel = viewModel::cancelEditing
                    )
                }
            } else {
                item {
                    Spacer(Modifier.height(16.dp))
                    FavouritesSection(user = user)
                }
            }
        }
    }
}

// ── Profile Hero ──────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeroSection(user: SupabaseUser, uiState: MyProfileUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large avatar with subtle ring
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

        // Watched count label
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
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

// ── Favourites Display ────────────────────────────────────────────────────────

@Composable
private fun FavouritesSection(user: SupabaseUser) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionLabel("FAVOURITES")
        Spacer(Modifier.height(2.dp))

        FavCard(label = "Movie",  value = user.fav_movie)
        FavCard(label = "Series", value = user.fav_series)
        FavCard(label = "Genre",  value = user.fav_genre)
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

@Composable
private fun FavCard(label: String, value: String?) {
    val hasValue = !value.isNullOrBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(56.dp)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(14.dp)
                .background(SurfaceHighlight)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            value?.takeIf { it.isNotBlank() } ?: "—",
            color = if (hasValue) TextPrimary else TextTertiary,
            fontSize = 14.sp,
            fontWeight = if (hasValue) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ── Edit Favourites ───────────────────────────────────────────────────────────

@Composable
private fun EditFavSection(
    uiState: MyProfileUiState,
    watchedItems: List<WatchItem>,
    onFavMovieChange: (String) -> Unit,
    onFavSeriesChange: (String) -> Unit,
    onFavGenreChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val watchedMovies  = watchedItems.filter { it.type == MediaType.MOVIE }
    val watchedSeries  = watchedItems.filter { it.type == MediaType.SERIES }

    var showMoviePicker  by remember { mutableStateOf(false) }
    var showSeriesPicker by remember { mutableStateOf(false) }

    if (showMoviePicker) {
        WatchItemPickerSheet(
            title   = "Select Favourite Movie",
            items   = watchedMovies,
            onSelect = { onFavMovieChange(it); showMoviePicker = false },
            onDismiss = { showMoviePicker = false }
        )
    }
    if (showSeriesPicker) {
        WatchItemPickerSheet(
            title   = "Select Favourite Series",
            items   = watchedSeries,
            onSelect = { onFavSeriesChange(it); showSeriesPicker = false },
            onDismiss = { showSeriesPicker = false }
        )
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionLabel("EDIT FAVOURITES")
        Spacer(Modifier.height(2.dp))

        SheetLauncherRow(
            label    = "Movie",
            selected = uiState.pendingFavMovie,
            onClick  = { showMoviePicker = true }
        )
        SheetLauncherRow(
            label    = "Series",
            selected = uiState.pendingFavSeries,
            onClick  = { showSeriesPicker = true }
        )
        GenrePickerRow(
            selected = uiState.pendingFavGenre,
            onSelect = onFavGenreChange
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) { Text("Cancel", fontWeight = FontWeight.Medium) }

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    contentColor = Background
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SheetLauncherRow(label: String, selected: String, onClick: () -> Unit) {
    val hasValue = selected.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(56.dp)
        )
        Box(Modifier.width(1.dp).height(14.dp).background(SurfaceHighlight))
        Spacer(Modifier.width(14.dp))
        Text(
            if (hasValue) selected else "Tap to select…",
            color = if (hasValue) TextPrimary else TextTertiary,
            fontSize = 14.sp,
            fontWeight = if (hasValue) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text("›", color = TextTertiary, fontSize = 18.sp)
    }
}

@Composable
private fun GenrePickerRow(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val hasValue = selected.isNotBlank()

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Genre",
                color = TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(56.dp)
            )
            Box(Modifier.width(1.dp).height(14.dp).background(SurfaceHighlight))
            Spacer(Modifier.width(14.dp))
            Text(
                if (hasValue) selected else "Tap to select…",
                color = if (hasValue) TextPrimary else TextTertiary,
                fontSize = 14.sp,
                fontWeight = if (hasValue) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Text("›", color = TextTertiary, fontSize = 18.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceContainer)
        ) {
            GENRES.forEach { genre ->
                DropdownMenuItem(
                    text = { Text(genre, color = TextPrimary, fontSize = 14.sp) },
                    onClick = { onSelect(genre); expanded = false }
                )
            }
        }
    }
}

// ── Pinterest Picker Sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchItemPickerSheet(
    title: String,
    items: List<WatchItem>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceContainer,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceHighlight)
            )
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            HorizontalDivider(color = SurfaceHighlight)
            Spacer(Modifier.height(8.dp))

            if (items.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(40.dp),
                    Alignment.Center
                ) {
                    Text(
                        "Nothing watched yet",
                        color = TextTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(items) { item ->
                        PinterestPickerCard(item = item, onClick = { onSelect(item.title) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PinterestPickerCard(item: WatchItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
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
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(SurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.type == MediaType.SERIES) Icons.Filled.Tv else Icons.Filled.Movie,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Column(Modifier.padding(horizontal = 9.dp, vertical = 8.dp)) {
            Text(
                item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${item.year}",
                fontSize = 11.sp,
                color = TextTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (item.rating > 0f) {
                Text("★ ${item.rating}", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}
