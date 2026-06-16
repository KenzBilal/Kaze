package com.kaze.ui.screens.arcs

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.kaze.data.repository.*
import com.kaze.model.MediaType
import com.kaze.model.WatchItem
import com.kaze.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ArcDetailViewModel(
    private val arcId: String,
    private val arcRepository: ArcRepository,
    private val watchItemRepo: WatchItemRepository,
    private val userRepository: UserRepository,
    private val activityRepo: ActivityRepository
) : ViewModel() {

    private val _arc = MutableStateFlow<Arc?>(null)
    private val _items = MutableStateFlow<List<ArcItemUiState>>(emptyList())
    private val _isLoading = MutableStateFlow(true)

    val arc: StateFlow<Arc?> = _arc.asStateFlow()
    val items: StateFlow<List<ArcItemUiState>> = _items.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var userId: String? = null
    private var localWatchlist: List<WatchItem> = emptyList()

    init { load() }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            userId = userRepository.getLocalUserId()
            localWatchlist = watchItemRepo.getAllItemsSnapshot()

            val (arc, arcItems) = arcRepository.getArcWithItems(arcId, forceRefresh)
            _arc.value = arc

            val arcItemIds = arcItems.map { it.id }
            val progressMap = userId?.let { arcRepository.getProgressForArc(arcItemIds, it) } ?: emptyMap()

            _items.value = arcItems.map { item ->
                ArcItemUiState(item, computeRowState(item, localWatchlist, progressMap))
            }
            _isLoading.value = false
        }
    }

    fun toggleMark(arcItem: ArcItem) {
        val uid = userId ?: return
        viewModelScope.launch {
            val current = _items.value.firstOrNull { it.arcItem.id == arcItem.id }
            val isMarked = current?.rowState == ArcRowState.MANUALLY_MARKED
            arcRepository.markArcItem(arcItem.id, uid, !isMarked)
            // Refresh row states
            val arcItemIds = _items.value.map { it.arcItem.id }
            val progressMap = arcRepository.getProgressForArc(arcItemIds, uid)
            _items.value = _items.value.map { uiState ->
                uiState.copy(rowState = computeRowState(uiState.arcItem, localWatchlist, progressMap))
            }
        }
    }

    fun addToWatchlist(arcItem: ArcItem) {
        val inWatchlist = localWatchlist.any { it.imdbId == arcItem.imdb_id }
        if (inWatchlist) return
        viewModelScope.launch {
            val watchItem = WatchItem(
                title     = arcItem.title,
                year      = arcItem.year,
                type      = if (arcItem.type == "SERIES") MediaType.SERIES else MediaType.MOVIE,
                imdbId    = arcItem.imdb_id,
                posterUrl = arcItem.poster_url,
                season    = arcItem.start_season,
                episode   = arcItem.start_episode
            )
            val newId = watchItemRepo.saveItem(watchItem)
            userId?.let { uid ->
                userRepository.pushWatchItem(uid, watchItem.copy(id = newId))
                activityRepo.postActivity(
                    ActivityFeedEntry(
                        user_id       = uid,
                        action_type   = "added_item",
                        item_title    = arcItem.title,
                        item_type     = arcItem.type,
                        item_poster_url = arcItem.poster_url,
                        item_imdb_id  = arcItem.imdb_id
                    )
                )
            }
            // Refresh local watchlist + row states
            localWatchlist = watchItemRepo.getAllItemsSnapshot()
            val arcItemIds = _items.value.map { it.arcItem.id }
            val progressMap = userId?.let { arcRepository.getProgressForArc(arcItemIds, it) } ?: emptyMap()
            _items.value = _items.value.map { uiState ->
                uiState.copy(rowState = computeRowState(uiState.arcItem, localWatchlist, progressMap))
            }
        }
    }

    fun addAllToWatchlist() {
        viewModelScope.launch {
            _items.value.forEach { uiState ->
                if (uiState.rowState == ArcRowState.NOT_IN_WATCHLIST) {
                    addToWatchlist(uiState.arcItem)
                }
            }
        }
    }

    private fun computeRowState(
        arcItem: ArcItem,
        watchlist: List<WatchItem>,
        progress: Map<String, Boolean>
    ): ArcRowState {
        val inWatchlist = watchlist.firstOrNull { it.imdbId == arcItem.imdb_id }
        return when {
            inWatchlist?.isWatched == true    -> ArcRowState.WATCHED
            progress[arcItem.id] == true      -> ArcRowState.MANUALLY_MARKED
            inWatchlist != null               -> ArcRowState.IN_WATCHLIST
            else                              -> ArcRowState.NOT_IN_WATCHLIST
        }
    }

    class Factory(
        private val arcId: String,
        private val arcRepository: ArcRepository,
        private val watchItemRepo: WatchItemRepository,
        private val userRepository: UserRepository,
        private val activityRepo: ActivityRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            ArcDetailViewModel(arcId, arcRepository, watchItemRepo, userRepository, activityRepo) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcDetailScreen(
    arcId: String,
    arcRepository: ArcRepository,
    watchItemRepo: WatchItemRepository,
    userRepository: UserRepository,
    activityRepo: ActivityRepository,
    onBack: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    val vm: ArcDetailViewModel = viewModel(
        factory = ArcDetailViewModel.Factory(arcId, arcRepository, watchItemRepo, userRepository, activityRepo)
    )
    val arc by vm.arc.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    var showAddAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        arc?.name ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBackIosNew, "Back", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Header: cover image + description + add all button
                item {
                    arc?.let { a ->
                        if (!a.cover_url.isNullOrBlank()) {
                            AsyncImage(
                                model = a.cover_url,
                                contentDescription = a.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(SurfaceElevated)
                            )
                        }
                        if (a.description.isNotBlank()) {
                            Text(
                                a.description,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                    Button(
                        onClick = { showAddAllDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Background),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add all to watchlist", fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = SurfaceHighlight, modifier = Modifier.padding(top = 4.dp))
                }

                // Arc items grouped by phase
                val grouped = items.groupBy { it.arcItem.phase_label }
                grouped.forEach { (phase, groupItems) ->
                    if (phase != null) {
                        item(key = "phase_$phase") {
                            Text(
                                phase.uppercase(),
                                color = AccentBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                    items(groupItems, key = { it.arcItem.id }) { uiState ->
                        ArcItemRow(
                            uiState = uiState,
                            onRowTap = { vm.toggleMark(uiState.arcItem) },
                            onAddClick = { vm.addToWatchlist(uiState.arcItem) }
                        )
                    }
                }
            }
        }
    }

    if (showAddAllDialog) {
        AlertDialog(
            onDismissRequest = { showAddAllDialog = false },
            containerColor = SurfaceContainer,
            title = {
                Text("Add all items to watchlist?", color = TextPrimary, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium)
            },
            confirmButton = {
                Button(
                    onClick = { vm.addAllToWatchlist(); showAddAllDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Background)
                ) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showAddAllDialog = false }) {
                    Text("Not now", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ArcItemRow(
    uiState: ArcItemUiState,
    onRowTap: () -> Unit,
    onAddClick: () -> Unit
) {
    val item = uiState.arcItem
    val isDone = uiState.rowState == ArcRowState.WATCHED || uiState.rowState == ArcRowState.MANUALLY_MARKED

    val alpha by animateColorAsState(
        targetValue = if (isDone) Color.Gray.copy(alpha = 0.4f) else Color.Transparent,
        label = "rowAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isDone) 0.5f else 1f)
            .clickable { onRowTap() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster
        if (!item.poster_url.isNullOrBlank()) {
            AsyncImage(
                model = item.poster_url,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 44.dp, height = 60.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceElevated)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 60.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(item.title.take(1), color = TextTertiary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)

            // Range label
            val rangeLabel = buildString {
                if (item.type == "SERIES" && item.start_season != null) {
                    append("S${item.start_season}E${item.start_episode ?: 1}")
                    val endS = item.end_season ?: item.start_season
                    val endE = item.end_episode
                    if (endS != null && endE != null) {
                        append(" → S${endS}E${endE}")
                    }
                } else if (item.type == "MOVIE") {
                    append("Movie · ${item.year}")
                }
                if (item.is_optional) append(if (isNotEmpty()) "  ·  Optional" else "Optional")
            }
            if (rangeLabel.isNotBlank()) {
                Text(rangeLabel, color = TextTertiary, fontSize = 12.sp)
            }

            if (!item.notes.isNullOrBlank()) {
                Text(item.notes, color = AccentBlue.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.width(8.dp))

        // Action button
        when (uiState.rowState) {
            ArcRowState.NOT_IN_WATCHLIST -> {
                OutlinedButton(
                    onClick = onAddClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AccentBlue)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Add", color = AccentBlue, fontSize = 12.sp)
                }
            }
            ArcRowState.IN_WATCHLIST -> {
                Text("✓ Added", color = TextTertiary, fontSize = 12.sp)
            }
            ArcRowState.WATCHED -> {
                Icon(Icons.Default.CheckCircle, contentDescription = "Watched",
                    tint = WatchedGreen, modifier = Modifier.size(20.dp))
            }
            ArcRowState.MANUALLY_MARKED -> {
                Icon(Icons.Default.Check, contentDescription = "Marked",
                    tint = TextTertiary, modifier = Modifier.size(20.dp))
            }
        }
    }

    HorizontalDivider(color = SurfaceHighlight.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
}
