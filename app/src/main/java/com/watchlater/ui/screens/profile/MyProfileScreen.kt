package com.watchlater.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchlater.data.local.WatchLaterDatabase
import com.watchlater.data.repository.SupabaseUser
import com.watchlater.data.repository.UserRepository
import com.watchlater.model.WatchItem
import coil.compose.AsyncImage
import com.watchlater.ui.components.UserAvatar
import com.watchlater.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            val watchedItems = dao.getAllItemsOnce().filter { it.isWatched }
            val followersCount = repository.getFollowersCount(userId)
            val followingCount = repository.getFollowingCount(userId)
            _uiState.update {
                it.copy(
                    user = user,
                    userId = userId,
                    watchedItems = watchedItems,
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
            repository.updateProfile(uid, s.pendingFavMovie, s.pendingFavSeries, s.pendingFavGenre)
            _uiState.update {
                it.copy(
                    user = it.user?.copy(
                        fav_movie = s.pendingFavMovie,
                        fav_series = s.pendingFavSeries,
                        fav_genre = s.pendingFavGenre
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
                pendingFavMovie = u?.fav_movie ?: "",
                pendingFavSeries = u?.fav_series ?: "",
                pendingFavGenre = u?.fav_genre ?: ""
            )
        }
    }

    fun cancelEditing() = _uiState.update { it.copy(isEditing = false) }
    fun setPendingFavMovie(v: String) = _uiState.update { it.copy(pendingFavMovie = v) }
    fun setPendingFavSeries(v: String) = _uiState.update { it.copy(pendingFavSeries = v) }
    fun setPendingFavGenre(v: String) = _uiState.update { it.copy(pendingFavGenre = v) }

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

// ── Screen ────────────────────────────────────────────────────────────────────

private val GENRES = listOf(
    "Action", "Adventure", "Animation", "Comedy", "Crime", "Documentary",
    "Drama", "Fantasy", "Horror", "Mystery", "Romance", "Sci-Fi", "Thriller"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen() {
    val context = LocalContext.current
    val viewModel: MyProfileViewModel = viewModel(factory = MyProfileViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = TextPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
                actions = {
                    if (!uiState.isEditing && uiState.user != null) {
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
                CircularProgressIndicator(color = TextSecondary, strokeWidth = 2.dp)
            }
            return@Scaffold
        }

        val user = uiState.user ?: return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Avatar + name
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserAvatar(username = user.username, size = 80.dp, fontSize = 30.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(user.username, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    Spacer(Modifier.height(16.dp))

                    // Followers / Following
                    Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${uiState.followersCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Followers", fontSize = 12.sp, color = TextTertiary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${uiState.followingCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Following", fontSize = 12.sp, color = TextTertiary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${uiState.watchedItems.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Watched", fontSize = 12.sp, color = TextTertiary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = SurfaceHighlight)
                }
            }

            if (uiState.isEditing) {
                // Edit mode — pick favs
                item {
                    EditFavSection(
                        uiState = uiState,
                        watchedItems = uiState.watchedItems,
                        onFavMovieChange = viewModel::setPendingFavMovie,
                        onFavSeriesChange = viewModel::setPendingFavSeries,
                        onFavGenreChange = viewModel::setPendingFavGenre,
                        onSave = viewModel::saveProfile,
                        onCancel = viewModel::cancelEditing
                    )
                }
            } else {
                // View mode
                item {
                    FavDisplaySection(user = user)
                }
            }
        }
    }
}

@Composable
private fun FavDisplaySection(user: SupabaseUser) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("FAVOURITES", fontSize = 11.sp, color = TextTertiary, letterSpacing = 1.5.sp)
        FavRow("Movies", user.fav_movie)
        FavRow("Series", user.fav_series)
        FavRow("Genre", user.fav_genre)
    }
}

@Composable
private fun FavRow(label: String, value: String?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(SurfaceElevated).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextTertiary, fontSize = 13.sp, modifier = Modifier.width(90.dp))
        Text(
            value?.takeIf { it.isNotBlank() } ?: "—",
            color = if (value.isNullOrBlank()) TextTertiary else TextPrimary,
            fontSize = 14.sp, fontWeight = FontWeight.Medium
        )
    }
}

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
    val watchedMovies = watchedItems.filter { it.type == com.watchlater.model.MediaType.MOVIE }
    val watchedSeries = watchedItems.filter { it.type == com.watchlater.model.MediaType.SERIES }
    
    var showMoviePicker by remember { mutableStateOf(false) }
    var showSeriesPicker by remember { mutableStateOf(false) }

    if (showMoviePicker) {
        WatchItemPickerSheet(
            title = "Select Movie",
            items = watchedMovies,
            onSelect = { onFavMovieChange(it); showMoviePicker = false },
            onDismiss = { showMoviePicker = false }
        )
    }

    if (showSeriesPicker) {
        WatchItemPickerSheet(
            title = "Select Series",
            items = watchedSeries,
            onSelect = { onFavSeriesChange(it); showSeriesPicker = false },
            onDismiss = { showSeriesPicker = false }
        )
    }

    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("EDIT FAVOURITES", fontSize = 11.sp, color = TextTertiary, letterSpacing = 1.5.sp)

        // Fav Movie picker
        SheetLauncherSection(
            label = "Movies",
            selected = uiState.pendingFavMovie,
            onClick = { showMoviePicker = true }
        )

        // Fav Series picker
        SheetLauncherSection(
            label = "Series",
            selected = uiState.pendingFavSeries,
            onClick = { showSeriesPicker = true }
        )

        // Genre picker
        PickerSection(
            label = "Genre",
            selected = uiState.pendingFavGenre,
            options = GENRES,
            onSelect = onFavGenreChange
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel, modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) { Text("Cancel") }
            Button(
                onClick = onSave, modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = Background),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) { Text("Save", fontWeight = FontWeight.SemiBold) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PickerSection(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(SurfaceElevated).clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                selected.ifBlank { "Select…" },
                color = if (selected.isBlank()) TextTertiary else TextPrimary,
                fontSize = 14.sp
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceContainer)
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Nothing watched yet", color = TextTertiary) },
                    onClick = { expanded = false }
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = TextPrimary) },
                        onClick = { onSelect(option); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetLauncherSection(label: String, selected: String, onClick: () -> Unit) {
    Column {
        Text(label, color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(SurfaceElevated).clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                selected.ifBlank { "Select…" },
                color = if (selected.isBlank()) TextTertiary else TextPrimary,
                fontSize = 14.sp
            )
        }
    }
}

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
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 8.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(SurfaceHighlight)) }
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Nothing watched yet", color = TextTertiary) }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    items(items) { item ->
                        WatchItemPinterestCard(item) {
                            onSelect(item.title)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchItemPinterestCard(item: WatchItem, onClick: () -> Unit) {
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
                    if (item.type == com.watchlater.model.MediaType.SERIES) Icons.Filled.Tv else Icons.Filled.Movie,
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
            if (item.myRating > 0f) {
                Text("★ ${item.myRating}", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}
