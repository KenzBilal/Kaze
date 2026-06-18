package com.kaze.ui.screens.discover

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kaze.data.remote.DiscoverItem
import com.kaze.data.remote.OmdbRepository
import com.kaze.data.remote.TraktMovie
import com.kaze.data.remote.TraktRepository
import com.kaze.data.remote.TraktShow
import com.kaze.data.repository.PublicWatchlistItem
import com.kaze.data.repository.UserRepository
import com.kaze.data.repository.WatchItemRepository
import com.kaze.ui.components.EmptyState
import com.kaze.ui.components.WatchLaterLoader
import com.kaze.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── Tab enum ──────────────────────────────────────────────────────────────────

enum class DiscoverTab { FRIENDS, GLOBAL }

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: WatchItemRepository,
    private val userRepo: UserRepository,
    private val traktRepo: TraktRepository,
    private val omdbRepo: OmdbRepository,
    private val cacheRepo: com.kaze.data.repository.DiscoverCacheRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var currentFriendsWatchlists: List<PublicWatchlistItem> = emptyList()
    private var currentTraktMovies: MutableList<TraktMovie> = mutableListOf()
    private var currentTraktShows: MutableList<TraktShow> = mutableListOf()
    private val posterCache = mutableMapOf<String, String?>()

    private var currentPage = 1
    private var isLoadingMore = false

    // ── Chat visibility ────────────────────────────────────────────────────────
    private var localUsername: String = ""

    init {
        load()
        viewModelScope.launch {
            repository.getAllItemsFlow().collect { ownItems ->
                val ownImdbIds = ownItems.map { it.imdbId }.filter { it.isNotBlank() }.toSet()
                _uiState.update { it.copy(ownImdbIds = ownImdbIds) }
            }
        }
    }



    fun selectTab(tab: DiscoverTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            currentPage = 1
            currentTraktMovies.clear()
            currentTraktShows.clear()
            currentFriendsWatchlists = emptyList()
            loadInternal()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            currentPage = 1
            currentTraktMovies.clear()
            currentTraktShows.clear()
            loadInternal()
        }
    }

    fun loadMoreGlobal() {
        if (isLoadingMore) return
        isLoadingMore = true
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            currentPage++
            val moviesDeferred = async { traktRepo.getTrendingMovies(currentPage, 15) }
            val showsDeferred = async { traktRepo.getTrendingShows(currentPage, 15) }
            currentTraktMovies.addAll(moviesDeferred.await())
            currentTraktShows.addAll(showsDeferred.await())
            recalculateSuggestions()
            isLoadingMore = false
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    private suspend fun loadInternal() {
        val userId = userRepo.getLocalUserId() ?: run {
            _uiState.update { it.copy(isLoading = false, isLoggedIn = false) }
            return
        }

        val ownItems = repository.getAllItemsSnapshot()
        val ownImdbIds = ownItems.map { it.imdbId }.filter { it.isNotBlank() }.toSet()
        val topGenre = ownItems.filter { it.isWatched }
            .flatMap { it.genreList }
            .groupingBy { it }.eachCount()
            .entries.maxByOrNull { it.value }?.key ?: ""

        _uiState.update { it.copy(topGenre = topGenre, ownImdbIds = ownImdbIds) }

        val following = userRepo.getFollowingList(userId)
        val followedIds = following.map { it.id }

        val friendsDeferred = viewModelScope.async { userRepo.getWatchlistsByUserIds(followedIds) }
        val moviesDeferred = viewModelScope.async { traktRepo.getTrendingMovies(currentPage, 25) }
        val showsDeferred = viewModelScope.async { traktRepo.getTrendingShows(currentPage, 25) }

        currentFriendsWatchlists = friendsDeferred.await()
        currentTraktMovies.addAll(moviesDeferred.await())
        currentTraktShows.addAll(showsDeferred.await())

        recalculateSuggestions()
    }

    private suspend fun recalculateSuggestions() {
        val state = _uiState.value
        val ownImdbIds = state.ownImdbIds
        val topGenre = state.topGenre

        val followingMap = userRepo.getFollowingList(userRepo.getLocalUserId() ?: "")
            .associateBy({ it.id }, { it.username })

        // ── Friends tab items ──────────────────────────────────────────────
        val friendsSuggestions = currentFriendsWatchlists
            .filter { it.imdb_id !in ownImdbIds && it.imdb_id.isNotBlank() }
            .groupBy { it.imdb_id }
            .map { entry -> entry.value.maxByOrNull { it.rating }!! }
            .sortedWith(
                compareByDescending<PublicWatchlistItem> { topGenre.isNotEmpty() && it.genres.contains(topGenre, ignoreCase = true) }
                    .thenByDescending { it.rating }
                    .thenByDescending { it.date_added }
            )
            .take(30)
            .map {
                val friendName = followingMap[it.user_id] ?: "friend"
                DiscoverItem(
                    title = it.title, year = it.year, type = it.type,
                    imdbId = it.imdb_id, posterUrl = it.poster_url,
                    rating = it.rating, notes = "", genres = it.genres
                )
            }
        friendsSuggestions.forEach { if (it.posterUrl != null) posterCache[it.imdbId] = it.posterUrl }

        // ── Global tab items (Trakt) ───────────────────────────────────────
        val traktMovies = currentTraktMovies
            .filter { it.ids.imdb != null && it.ids.imdb !in ownImdbIds }
            .map {
                DiscoverItem(
                    title = it.title, year = it.year ?: 0, type = "MOVIE",
                    imdbId = it.ids.imdb!!, posterUrl = posterCache[it.ids.imdb],
                    notes = ""
                )
            }
        val traktShows = currentTraktShows
            .filter { it.ids.imdb != null && it.ids.imdb !in ownImdbIds }
            .map {
                DiscoverItem(
                    title = it.title, year = it.year ?: 0, type = "SERIES",
                    imdbId = it.ids.imdb!!, posterUrl = posterCache[it.ids.imdb],
                    notes = ""
                )
            }

        val globalMixed = mutableListOf<DiscoverItem>()
        val maxLen = maxOf(traktMovies.size, traktShows.size)
        for (i in 0 until maxLen) {
            if (i < traktShows.size) globalMixed.add(traktShows[i])
            if (i < traktMovies.size) globalMixed.add(traktMovies[i])
        }
        val globalFinal = globalMixed.distinctBy { it.imdbId }

        // Fetch missing posters from Supabase cache
        val allItems = (friendsSuggestions + globalFinal)
        val missingIds = allItems.filter { it.posterUrl == null }.map { it.imdbId }
        val ratingCache = mutableMapOf<String, Float>()
        if (missingIds.isNotEmpty()) {
            val cachedMap = cacheRepo.getCachedItems(missingIds)
            cachedMap.forEach { (imdb, item) -> 
                if (item.posterUrl != null) posterCache[imdb] = item.posterUrl
                if (item.rating > 0f) ratingCache[imdb] = item.rating
            }
        }

        fun applyCache(list: List<DiscoverItem>) = list.map {
            var updated = it
            if (updated.posterUrl == null && posterCache[updated.imdbId] != null) updated = updated.copy(posterUrl = posterCache[updated.imdbId])
            if (updated.rating <= 0f && ratingCache[updated.imdbId] != null) updated = updated.copy(rating = ratingCache[updated.imdbId]!!)
            updated
        }

        val friendsFinal = applyCache(friendsSuggestions)
        val globalWithPosters = applyCache(globalFinal)

        _uiState.update {
            it.copy(
                friendsItems = friendsFinal,
                globalItems = globalWithPosters,
                isLoading = false,
                isFriendsEmpty = friendsFinal.isEmpty(),
                isGlobalEmpty = globalWithPosters.isEmpty()
            )
        }

        // Lazy-load missing posters from OMDB
        val missingFromOmdb = globalWithPosters.filter { it.posterUrl == null }.take(15)
        if (missingFromOmdb.isNotEmpty()) {
            viewModelScope.launch {
                missingFromOmdb.forEach { item ->
                    try {
                        val detail = omdbRepo.api.getDetail(item.imdbId, omdbRepo.apiKey)
                        val poster = detail.poster?.takeIf { it != "N/A" }
                        if (poster != null) {
                            posterCache[item.imdbId] = poster
                            // Rating out of 5 (IMDB is /10)
                            val rawRating = detail.imdbRating?.toFloatOrNull() ?: 0f
                            val ratingOutOf5 = if (rawRating > 0) kotlin.math.round(rawRating / 2f).toFloat() else 0f
                            val fullItem = item.copy(posterUrl = poster, genres = detail.genre ?: "", rating = ratingOutOf5)
                            cacheRepo.cacheItem(fullItem)
                            val updated = _uiState.value.globalItems.map { if (it.imdbId == item.imdbId) fullItem else it }
                            _uiState.update { it.copy(globalItems = updated) }
                        }
                    } catch (e: Exception) { /* ignore */ }
                }
            }
        }
    }

    // ── Dice roller ────────────────────────────────────────────────────────────

    fun rollDice(tab: DiscoverTab): DiscoverItem? {
        val list = if (tab == DiscoverTab.FRIENDS) _uiState.value.friendsItems
                   else _uiState.value.globalItems
        return list.filter { it.posterUrl != null }.randomOrNull() ?: list.randomOrNull()
    }

    
}

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val activeTab: DiscoverTab = DiscoverTab.FRIENDS,
    val friendsItems: List<DiscoverItem> = emptyList(),
    val globalItems: List<DiscoverItem> = emptyList(),
    val isFriendsEmpty: Boolean = false,
    val isGlobalEmpty: Boolean = false,
    val isLoggedIn: Boolean = true,
    val ownImdbIds: Set<String> = emptySet(),
    val topGenre: String = "",

)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoverScreen(
    repository: WatchItemRepository,
    traktRepository: TraktRepository,
    omdbRepository: OmdbRepository,
    onItemClick: (DiscoverItem) -> Unit,

) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val viewModel: DiscoverViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showDiscoverFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Discover", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                actions = {
                    // Discover Filter icon (Wand)
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showDiscoverFilterSheet = true
                    }) {
                        Icon(Icons.Default.AutoAwesome, "Discover Filter", tint = TextSecondary)
                    }

                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->

        // Discover Filter bottom sheet
        if (showDiscoverFilterSheet) {
            val listToFilter = if (uiState.activeTab == DiscoverTab.FRIENDS) uiState.friendsItems else uiState.globalItems
            ModalBottomSheet(
                onDismissRequest = { showDiscoverFilterSheet = false },
                containerColor = Background,
                dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary) }
            ) {
                com.kaze.ui.components.DiscoverFilterBottomSheet(
                    items = listToFilter,
                    onDismiss = { showDiscoverFilterSheet = false },
                    onItemClick = { item ->
                        onItemClick(item)
                        showDiscoverFilterSheet = false
                    }
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Tab selector ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                DiscoverTabButton(
                    label = "Friends",
                    selected = uiState.activeTab == DiscoverTab.FRIENDS,
                    onClick = { viewModel.selectTab(DiscoverTab.FRIENDS) },
                    modifier = Modifier.weight(1f)
                )
                DiscoverTabButton(
                    label = "Global",
                    selected = uiState.activeTab == DiscoverTab.GLOBAL,
                    onClick = { viewModel.selectTab(DiscoverTab.GLOBAL) },
                    modifier = Modifier.weight(1f)
                )
            }

            when {
                uiState.isLoading -> WatchLaterLoader()
                !uiState.isLoggedIn -> EmptyState(
                    icon = Icons.Default.Person,
                    title = "Not signed in",
                    subtitle = "Sign in to see what friends are watching",
                    modifier = Modifier.fillMaxSize()
                )
                else -> {
                    val activeItems = if (uiState.activeTab == DiscoverTab.FRIENDS)
                        uiState.friendsItems else uiState.globalItems
                    val isEmpty = if (uiState.activeTab == DiscoverTab.FRIENDS)
                        uiState.isFriendsEmpty else uiState.isGlobalEmpty

                    if (isEmpty && !uiState.isRefreshing) {
                        val (icon, title, subtitle) = if (uiState.activeTab == DiscoverTab.FRIENDS)
                            Triple(Icons.Outlined.Explore, "No friends suggestions yet", "Follow people to see their watchlists here")
                        else
                            Triple(Icons.Default.TrendingUp, "Nothing trending", "Check back later")
                        EmptyState(
                            icon = icon,
                            title = title,
                            subtitle = subtitle,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        PullToRefreshBox(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.refresh() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 100.dp),
                                verticalItemSpacing = 10.dp,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(activeItems, key = { it.imdbId }) { item ->
                                    DiscoverCard(
                                        item = item,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onItemClick(item)
                                        }
                                    )
                                }

                                // Load more (only Global tab uses API)
                                if (uiState.activeTab == DiscoverTab.GLOBAL) {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Button(
                                                onClick = { viewModel.loadMoreGlobal() },
                                                enabled = !uiState.isLoadingMore,
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                                            ) {
                                                if (uiState.isLoadingMore)
                                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Background, strokeWidth = 2.dp)
                                                else
                                                    Text("Load More", color = Background, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Tab button ────────────────────────────────────────────────────────────────

@Composable
private fun DiscoverTabButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AccentBlue else SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Background else TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

// ── Dice result dialog ────────────────────────────────────────────────────────

@Composable
private fun DiceResultDialog(
    item: DiscoverItem,
    onDismiss: () -> Unit,
    onAddToWatchlist: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceContainer)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🎲 Your Pick", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (!item.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }
            Text(item.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.type.lowercase().replaceFirstChar { it.uppercase() }, color = TextTertiary, fontSize = 12.sp)
                if (item.year > 0) Text("· ${item.year}", color = TextTertiary, fontSize = 12.sp)
                if (item.rating > 0f) Text("· ★ ${kotlin.math.round(item.rating).toInt()}/5", color = TextTertiary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onAddToWatchlist,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add to Watchlist", fontWeight = FontWeight.SemiBold, color = Background)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Dismiss", color = TextSecondary)
            }
        }
    }
}

// ── Discover Card ─────────────────────────────────────────────────────────────

@Composable
private fun DiscoverCard(item: DiscoverItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
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
                            text = "${kotlin.math.round(item.rating).toInt()}/5",
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }
                }
                if (item.notes.isNotBlank()) {
                    Text(item.notes, fontSize = 9.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
