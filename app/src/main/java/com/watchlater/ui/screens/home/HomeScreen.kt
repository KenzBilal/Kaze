package com.watchlater.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watchlater.model.FilterOption
import com.watchlater.model.WatchItem
import com.watchlater.ui.components.*
import com.watchlater.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onItemClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("TO WATCH", "WATCHED")
    var showSortSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = Background,
        topBar = {
            HomeTopBar(
                onSearchClick = onSearchClick,
                onFilterClick = { showSortSheet = true },
                hasActiveFilter = uiState.sortFilterState.filter != FilterOption.ALL
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = AccentBlue,
                contentColor = Background,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add item", modifier = Modifier.size(24.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Background,
                contentColor = AccentBlue,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AccentBlue,
                            height = 2.dp
                        )
                    }
                },
                divider = { SubtleDivider() }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedTab == index) AccentBlue else TextTertiary
                            )
                        }
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "tab_content"
            ) { tab ->
                when (tab) {
                    0 -> WatchItemList(
                        items = uiState.toWatchItems,
                        isLoading = uiState.isLoading,
                        emptyIcon = Icons.Outlined.Bookmark,
                        emptyTitle = "Your watchlist is empty",
                        emptySubtitle = "Tap + to add movies and series\nyou want to watch",
                        onItemClick = onItemClick,
                        onToggleWatched = { viewModel.toggleWatched(it) }
                    )
                    1 -> WatchItemList(
                        items = uiState.watchedItems,
                        isLoading = uiState.isLoading,
                        emptyIcon = Icons.Outlined.CheckCircle,
                        emptyTitle = "Nothing watched yet",
                        emptySubtitle = "Mark items as watched and\nthey'll appear here",
                        onItemClick = onItemClick,
                        onToggleWatched = { viewModel.toggleWatched(it) }
                    )
                }
            }
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceContainer,
            dragHandle = null
        ) {
            SortFilterSheet(
                state = uiState.sortFilterState,
                onSortChange = viewModel::updateSort,
                onFilterChange = viewModel::updateFilter,
                onDismiss = { showSortSheet = false }
            )
        }
    }

    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()

    var hideDownloadDialog by remember(updateState) { mutableStateOf(false) }

    if (updateState == com.watchlater.updater.UpdateState.AVAILABLE && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { /* Force them to dismiss via a button if we wanted, or allow dismiss */ },
            containerColor = SurfaceContainer,
            title = { Text("Update Available", color = TextPrimary) },
            text = {
                Column {
                    Text("Version ${updateInfo!!.versionName} is ready.", color = TextSecondary)
                    if (updateInfo!!.releaseNotes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(updateInfo!!.releaseNotes, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.downloadUpdate() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Background)
                ) {
                    Text("Update Now", color = Color.Unspecified)
                }
            },
            dismissButton = {
                TextButton(onClick = { /* Could add a dismiss mechanism to viewmodel if needed, for now just ignore */ }) {
                    Text("Later", color = TextSecondary)
                }
            }
        )
    }

    if (updateState == com.watchlater.updater.UpdateState.DOWNLOADING && !hideDownloadDialog) {
        // Simple overlay or toast indicating downloading
        // We'll show an AlertDialog so user knows it's happening
        AlertDialog(
            onDismissRequest = { hideDownloadDialog = true },
            containerColor = SurfaceContainer,
            title = { Text("Downloading Update", color = TextPrimary) },
            text = {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(24.dp))
                    Text("Please wait...", color = TextSecondary)
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { hideDownloadDialog = true }) {
                    Text("Hide", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun WatchItemList(
    items: List<WatchItem>,
    isLoading: Boolean,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptySubtitle: String,
    onItemClick: (Long) -> Unit,
    onToggleWatched: (WatchItem) -> Unit
) {
    when {
        isLoading -> WatchLaterLoader()
        items.isEmpty() -> EmptyState(
            icon = emptyIcon,
            title = emptyTitle,
            subtitle = emptySubtitle,
            modifier = Modifier.fillMaxSize()
        )
        else -> LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = items,
                key = { it.id }
            ) { item ->
                WatchItemCard(
                    item = item,
                    onClick = { onItemClick(item.id) },
                    onToggleWatched = { onToggleWatched(item) }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    hasActiveFilter: Boolean
) {
    TopAppBar(
        title = {
            Text(
                text = "Kaze",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
            }
            BadgedBox(
                badge = { if (hasActiveFilter) Badge(containerColor = AccentBlue) }
            ) {
                IconButton(onClick = onFilterClick) {
                    Icon(Icons.Default.Tune, contentDescription = "Sort & Filter", tint = TextSecondary)
                }
            }
        }
    )
}
