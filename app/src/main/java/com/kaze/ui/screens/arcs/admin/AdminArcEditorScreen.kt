package com.kaze.ui.screens.arcs.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.kaze.data.remote.OmdbRepository
import com.kaze.data.remote.OmdbResult
import com.kaze.data.repository.Arc
import com.kaze.data.repository.ArcItem
import com.kaze.data.repository.ArcRepository
import com.kaze.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.*
import java.util.UUID
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.SavedStateHandle

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AdminArcEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val arcRepository: ArcRepository,
    private val omdbRepository: OmdbRepository
) : ViewModel() {
    private val arcId: String = checkNotNull(savedStateHandle.get<String>("arcId"))


    private val _arc = MutableStateFlow<Arc?>(null)
    private val _items = MutableStateFlow<List<ArcItem>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _searchResults = MutableStateFlow<List<OmdbResult>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")

    val arc: StateFlow<Arc?> = _arc.asStateFlow()
    val items: StateFlow<List<ArcItem>> = _items.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val searchResults: StateFlow<List<OmdbResult>> = _searchResults.asStateFlow()
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var searchJob: Job? = null

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val (arc, items) = arcRepository.getArcWithItems(arcId, forceRefresh = true)
            _arc.value = arc
            _items.value = items.sortedBy { it.order_index }
            _isLoading.value = false
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        searchJob = viewModelScope.launch {
            delay(500)
            _isSearching.value = true
            try {
                val results = omdbRepository.search(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            }
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    /** Get last occurrence of this imdbId in the arc for resume logic */
    suspend fun getLastOccurrence(imdbId: String): ArcItem? =
        arcRepository.getLastOccurrence(arcId, imdbId)

    /** Get total seasons for a series */
    suspend fun getTotalSeasons(imdbId: String): Int {
        return try {
            omdbRepository.fetchSeriesMetadata(imdbId).totalSeasons
        } catch (e: Exception) { 0 }
    }

    /** Get total episodes in a season */
    suspend fun getEpisodesInSeason(imdbId: String, season: Int): Int {
        return try {
            omdbRepository.fetchSeason(imdbId, season)?.episodes?.size ?: 0
        } catch (e: Exception) { 0 }
    }

    fun addMovieItem(omdbItem: OmdbResult, notes: String, isOptional: Boolean) {
        viewModelScope.launch {
            val nextIndex = arcRepository.getNextOrderIndex(arcId)
            val item = ArcItem(
                id          = UUID.randomUUID().toString(),
                arc_id      = arcId,
                order_index = nextIndex,
                imdb_id     = omdbItem.omdbId,
                title       = omdbItem.displayTitle,
                year        = omdbItem.displayYear.toString().take(4).toIntOrNull() ?: 0,
                type        = "MOVIE",
                poster_url  = omdbItem.posterUrl.takeIf { it != "N/A" },
                notes       = notes.ifBlank { null },
                is_optional = isOptional
            )
            arcRepository.addArcItem(item)
            load()
        }
    }

    fun addSeriesItem(
        omdbItem: OmdbResult,
        startSeason: Int, startEpisode: Int,
        endSeason: Int, endEpisode: Int,
        totalSeasons: Int,
        notes: String, isOptional: Boolean
    ) {
        viewModelScope.launch {
            val nextIndex = arcRepository.getNextOrderIndex(arcId)
            val item = ArcItem(
                id            = UUID.randomUUID().toString(),
                arc_id        = arcId,
                order_index   = nextIndex,
                imdb_id       = omdbItem.omdbId,
                title         = omdbItem.displayTitle,
                year          = omdbItem.displayYear.toString().take(4).toIntOrNull() ?: 0,
                type          = "SERIES",
                poster_url    = omdbItem.posterUrl.takeIf { it != "N/A" },
                total_seasons = totalSeasons,
                start_season  = startSeason,
                start_episode = startEpisode,
                end_season    = endSeason,
                end_episode   = endEpisode,
                notes         = notes.ifBlank { null },
                is_optional   = isOptional
            )
            arcRepository.addArcItem(item)
            load()
        }
    }

    fun deleteItem(item: ArcItem) {
        viewModelScope.launch {
            arcRepository.deleteArcItem(item.id, arcId)
            load()
        }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val currentItems = _items.value.toMutableList()
        if (fromIndex !in currentItems.indices || toIndex !in currentItems.indices) return
        val temp = currentItems.removeAt(fromIndex)
        currentItems.add(toIndex, temp)
        _items.value = currentItems
    }
    
    fun syncItemOrder() {
        viewModelScope.launch {
            arcRepository.updateArcItemOrder(arcId, _items.value)
        }
    }

    fun setCoverUrl(posterUrl: String) {
        viewModelScope.launch {
            val current = _arc.value ?: return@launch
            arcRepository.updateArc(current.copy(cover_url = posterUrl))
            _arc.value = current.copy(cover_url = posterUrl)
        }
    }

    fun updateArcMeta(name: String, description: String, aliases: String) {
        viewModelScope.launch {
            val current = _arc.value ?: return@launch
            arcRepository.updateArc(current.copy(name = name, description = description, aliases = aliases))
            _arc.value = current.copy(name = name, description = description, aliases = aliases)
        }
    }

    

// ── Screen ────────────────────────────────────────────────────────────────────

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminArcEditorScreen(
    arcId: String,
    arcRepository: ArcRepository,
    omdbRepository: OmdbRepository,
    onBack: () -> Unit
) {
    val vm: AdminArcEditorViewModel = hiltViewModel()
    val arc by vm.arc.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val searchResults by vm.searchResults.collectAsStateWithLifecycle()
    val isSearching by vm.isSearching.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var showRangePicker by remember { mutableStateOf<OmdbResult?>(null) }
    var showMoviePicker by remember { mutableStateOf<OmdbResult?>(null) }
    var deleteTarget by remember { mutableStateOf<ArcItem?>(null) }
    var showMetaEditor by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(arc?.name ?: "Edit Arc", color = TextPrimary, fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBackIosNew, "Back", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { showMetaEditor = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit metadata", tint = TextSecondary)
                    }
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add item", tint = AccentBlue)
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
            else -> {
                val reorderableState = rememberReorderableLazyListState(
                    onMove = { from, to ->
                        vm.moveItem(from.index - 1, to.index - 1)
                    },
                    canDragOver = { draggedOver, _ -> draggedOver.index > 0 }
                )

                LazyColumn(
                    state = reorderableState.listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .reorderable(reorderableState),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Cover picker section
                    item {
                        val posterUrls = items.mapNotNull { it.poster_url }.distinct()
                        if (posterUrls.isNotEmpty()) {
                        Text("Cover Image", color = TextTertiary, fontSize = 11.sp,
                            letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(posterUrls) { url ->
                                val isSelected = arc?.cover_url == url
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 60.dp, height = 84.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) AccentBlue else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { vm.setCoverUrl(url) }
                                )
                            }
                        }
                        HorizontalDivider(color = SurfaceHighlight, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                // Items list
                if (items.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No items yet. Tap + to add.", color = TextTertiary)
                        }
                    }
                } else {
                    items(items, key = { it.id }) { item ->
                        ReorderableItem(reorderableState, key = item.id) { isDragging ->
                            AdminArcItemRow(
                                item = item,
                                modifier = Modifier
                                    .detectReorderAfterLongPress(reorderableState)
                                    .then(if (isDragging) Modifier.background(SurfaceContainer.copy(alpha = 0.8f)) else Modifier),
                                onDelete = { deleteTarget = item }
                            )
                        }
                    }
                    item {
                        LaunchedEffect(reorderableState.draggingItemKey) {
                            if (reorderableState.draggingItemKey == null) {
                                vm.syncItemOrder()
                            }
                        }
                    }
                }
            }
        }
    }
    }

    // Add item bottom sheet
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false; vm.clearSearch() },
            containerColor = SurfaceContainer,
            dragHandle = null
        ) {
            AdminAddItemSheet(
                query = searchQuery,
                results = searchResults,
                isSearching = isSearching,
                onQueryChange = vm::search,
                onSelectMovie = { item ->
                    showAddSheet = false
                    vm.clearSearch()
                    showMoviePicker = item
                },
                onSelectSeries = { item ->
                    showAddSheet = false
                    vm.clearSearch()
                    showRangePicker = item
                },
                onDismiss = { showAddSheet = false; vm.clearSearch() }
            )
        }
    }

    // Range picker for series
    showRangePicker?.let { omdbItem ->
        ModalBottomSheet(
            onDismissRequest = { showRangePicker = null },
            containerColor = SurfaceContainer,
            dragHandle = null
        ) {
            AdminRangePickerSheet(
                omdbItem = omdbItem,
                vm = vm,
                onConfirm = { startS, startE, endS, endE, totalS, notes, optional ->
                    vm.addSeriesItem(omdbItem, startS, startE, endS, endE, totalS, notes, optional)
                    showRangePicker = null
                },
                onDismiss = { showRangePicker = null }
            )
        }
    }

    // Movie picker
    showMoviePicker?.let { omdbItem ->
        ModalBottomSheet(
            onDismissRequest = { showMoviePicker = null },
            containerColor = SurfaceContainer,
            dragHandle = null
        ) {
            AdminMoviePickerSheet(
                omdbItem = omdbItem,
                onConfirm = { notes, optional ->
                    vm.addMovieItem(omdbItem, notes, optional)
                    showMoviePicker = null
                },
                onDismiss = { showMoviePicker = null }
            )
        }
    }

    // Delete confirmation
    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = SurfaceContainer,
            title = { Text("Remove \"${item.title}\"?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            confirmButton = {
                Button(
                    onClick = { vm.deleteItem(item); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    // Meta editor
    if (showMetaEditor) {
        arc?.let { a ->
            MetaEditorDialog(
                arc = a,
                onDismiss = { showMetaEditor = false },
                onSave = { name, desc, aliases ->
                    vm.updateArcMeta(name, desc, aliases)
                    showMetaEditor = false
                }
            )
        }
    }
}

@Composable
private fun AdminArcItemRow(
    item: ArcItem, 
    modifier: Modifier = Modifier,
    onDelete: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!item.poster_url.isNullOrBlank()) {
            AsyncImage(
                model = item.poster_url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 36.dp, height = 50.dp).clip(RoundedCornerShape(5.dp))
                    .background(SurfaceElevated)
            )
        } else {
            Box(Modifier.size(width = 36.dp, height = 50.dp).clip(RoundedCornerShape(5.dp))
                .background(SurfaceElevated), contentAlignment = Alignment.Center) {
                Text(item.title.take(1), color = TextTertiary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val rangeLabel = if (item.type == "SERIES" && item.start_season != null)
                "S${item.start_season}E${item.start_episode ?: 1} → S${item.end_season ?: item.start_season}E${item.end_episode ?: "?"}"
            else "Movie · ${item.year}"
            Text(rangeLabel, color = TextTertiary, fontSize = 11.sp)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextTertiary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.DragHandle, contentDescription = "Drag to reorder", tint = TextTertiary, modifier = Modifier.size(24.dp))
    }
    HorizontalDivider(color = SurfaceHighlight.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
}

// ── Add Item Sheet ─────────────────────────────────────────────────────────────

@Composable
fun AdminAddItemSheet(
    query: String,
    results: List<OmdbResult>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSelectMovie: (OmdbResult) -> Unit,
    onSelectSeries: (OmdbResult) -> Unit,
    onDismiss: () -> Unit
) {
    var localQuery by remember { mutableStateOf(query) }
    LaunchedEffect(localQuery) {
        if (localQuery != query) {
            delay(400)
            onQueryChange(localQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Text("Add Item", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = localQuery,
            onValueChange = { localQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search movies & series...", color = TextTertiary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextTertiary) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = SurfaceHighlight,
                focusedBorderColor = AccentBlue,
                unfocusedContainerColor = SurfaceElevated,
                focusedContainerColor = SurfaceElevated,
                cursorColor = AccentBlue,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(Modifier.height(8.dp))

        when {
            isSearching -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(24.dp))
            }
            results.isEmpty() && query.isNotBlank() -> Box(
                Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center
            ) { Text("No results", color = TextTertiary) }
            else -> LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(results) { item ->
                    SearchResultRow(
                        item = item,
                        onClick = {
                            if (item.mediaType == "tv") onSelectSeries(item)
                            else onSelectMovie(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(item: OmdbResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 36.dp, height = 50.dp).clip(RoundedCornerShape(5.dp))
                .background(SurfaceElevated)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.displayTitle, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.displayYear} · ${if (item.mediaType == "tv") "Series" else "Movie"}",
                color = TextTertiary, fontSize = 12.sp)
        }
        Icon(
            if (item.mediaType == "tv") Icons.Default.Tv else Icons.Default.Movie,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
    HorizontalDivider(color = SurfaceHighlight.copy(alpha = 0.3f))
}

// ── Range Picker Sheet ────────────────────────────────────────────────────────

@Composable
fun AdminRangePickerSheet(
    omdbItem: OmdbResult,
    vm: AdminArcEditorViewModel,
    onConfirm: (Int, Int, Int, Int, Int, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var totalSeasons by remember { mutableIntStateOf(1) }
    var lastOccurrence by remember { mutableStateOf<ArcItem?>(null) }

    // from / to state
    var fromSeason by remember { mutableIntStateOf(1) }
    var fromEpisode by remember { mutableIntStateOf(1) }
    var endSeason by remember { mutableIntStateOf(1) }
    var endEpisode by remember { mutableIntStateOf(1) }
    var fromLocked by remember { mutableStateOf(false) }

    var epsInFromSeason by remember { mutableIntStateOf(1) }
    var epsInEndSeason by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }
    var isOptional by remember { mutableStateOf(false) }
    var isLoadingMeta by remember { mutableStateOf(true) }

    // Load metadata
    LaunchedEffect(omdbItem.omdbId) {
        isLoadingMeta = true
        totalSeasons = vm.getTotalSeasons(omdbItem.omdbId).coerceAtLeast(1)
        lastOccurrence = vm.getLastOccurrence(omdbItem.omdbId)
        if (lastOccurrence != null) {
            val last = lastOccurrence!!
            val lastEndS = last.end_season ?: 1
            val lastEndE = last.end_episode ?: 1
            val maxEps = vm.getEpisodesInSeason(omdbItem.omdbId, lastEndS)
            if (lastEndE < maxEps) {
                fromSeason = lastEndS; fromEpisode = lastEndE + 1
            } else {
                fromSeason = (lastEndS + 1).coerceAtMost(totalSeasons); fromEpisode = 1
            }
            fromLocked = true
        }
        endSeason = totalSeasons; endEpisode = 1
        epsInFromSeason = vm.getEpisodesInSeason(omdbItem.omdbId, fromSeason).coerceAtLeast(1)
        epsInEndSeason = vm.getEpisodesInSeason(omdbItem.omdbId, endSeason).coerceAtLeast(1)
        endEpisode = epsInEndSeason
        isLoadingMeta = false
    }

    LaunchedEffect(fromSeason) {
        epsInFromSeason = vm.getEpisodesInSeason(omdbItem.omdbId, fromSeason).coerceAtLeast(1)
    }

    LaunchedEffect(endSeason) {
        epsInEndSeason = vm.getEpisodesInSeason(omdbItem.omdbId, endSeason).coerceAtLeast(1)
        endEpisode = epsInEndSeason
    }

    val previewLabel = "S${fromSeason}E${fromEpisode} → S${endSeason}E${endEpisode}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("${omdbItem.displayTitle}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("$totalSeasons season${if (totalSeasons != 1) "s" else ""}", color = TextTertiary, fontSize = 12.sp)

        if (lastOccurrence != null) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(AccentBlue.copy(alpha = 0.1f)).padding(10.dp)
            ) {
                Text(
                    "Continues from: S${lastOccurrence!!.start_season}E${lastOccurrence!!.start_episode} → S${lastOccurrence!!.end_season}E${lastOccurrence!!.end_episode}",
                    color = AccentBlue, fontSize = 12.sp
                )
            }
        }

        if (isLoadingMeta) {
            Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(24.dp))
            }
        } else {
            // To / From section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // FROM column
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Start At", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        if (fromLocked) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.Lock, null, tint = AccentBlue, modifier = Modifier.size(12.dp).clickable { fromLocked = false })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SeasonEpisodePicker(label = "Season", value = fromSeason, max = totalSeasons, locked = fromLocked, onValueChange = { fromSeason = it })
                    Spacer(Modifier.height(6.dp))
                    SeasonEpisodePicker(label = "Episode", value = fromEpisode, max = epsInFromSeason, locked = fromLocked, onValueChange = { fromEpisode = it })
                }
                
                // Vertical divider
                Box(modifier = Modifier.width(1.dp).height(80.dp).background(SurfaceHighlight).align(Alignment.CenterVertically))

                // TO column
                Column(modifier = Modifier.weight(1f)) {
                    Text("End At", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    SeasonEpisodePicker(label = "Season", value = endSeason, max = totalSeasons, locked = false, onValueChange = { endSeason = it })
                    Spacer(Modifier.height(6.dp))
                    SeasonEpisodePicker(label = "Episode", value = endEpisode, max = epsInEndSeason, locked = false, onValueChange = { endEpisode = it })
                }
            }

            // Preview
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(SurfaceElevated).padding(14.dp), contentAlignment = Alignment.Center) {
                Text(previewLabel, color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            ArcTextField(value = notes, onValueChange = { notes = it }, label = "Notes (optional)")

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceElevated).padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Optional entry", color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = isOptional,
                    onCheckedChange = { isOptional = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue, checkedThumbColor = Background)
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Text("Cancel", color = TextSecondary)
                }
                Button(
                    onClick = { onConfirm(fromSeason, fromEpisode, endSeason, endEpisode, totalSeasons, notes, isOptional) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Background)
                ) { Text("Add Series", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun AdminMoviePickerSheet(
    omdbItem: OmdbResult,
    onConfirm: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var notes by remember { mutableStateOf("") }
    var isOptional by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("${omdbItem.displayTitle} (${omdbItem.displayYear})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        
        ArcTextField(value = notes, onValueChange = { notes = it }, label = "Notes (optional)")

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceElevated).padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Optional entry", color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = isOptional,
                onCheckedChange = { isOptional = it },
                colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue, checkedThumbColor = Background)
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("Cancel", color = TextSecondary)
            }
            Button(
                onClick = { onConfirm(notes, isOptional) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Background)
            ) { Text("Add Movie", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SeasonEpisodePicker(
    label: String,
    value: Int,
    max: Int,
    locked: Boolean,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceElevated).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(SurfaceHighlight, RoundedCornerShape(8.dp))) {
            IconButton(
                onClick = { if (!locked && value > 1) onValueChange(value - 1) },
                enabled = !locked && value > 1,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Remove, null, tint = if (!locked && value > 1) TextPrimary else TextTertiary, modifier = Modifier.size(16.dp))
            }
            Text(
                "$value",
                color = if (locked) TextTertiary else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.widthIn(min = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(
                onClick = { if (!locked && value < max) onValueChange(value + 1) },
                enabled = !locked && value < max,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = if (!locked && value < max) TextPrimary else TextTertiary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MetaEditorDialog(arc: Arc, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf(arc.name) }
    var description by remember { mutableStateOf(arc.description) }
    var aliases by remember { mutableStateOf(arc.aliases) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        title = { Text("Edit Arc", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ArcTextField(value = name, onValueChange = { name = it }, label = "Name *")
                ArcTextField(value = description, onValueChange = { description = it }, label = "Description", singleLine = false)
                ArcTextField(value = aliases, onValueChange = { aliases = it }, label = "Aliases (comma separated)")
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, description, aliases) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Background),
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
