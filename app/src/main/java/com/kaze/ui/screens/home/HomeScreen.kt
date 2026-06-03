package com.kaze.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaze.model.FilterOption
import com.kaze.model.WatchItem
import com.kaze.ui.components.*
import com.kaze.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onItemClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(uiState.selectedTab) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(selectedTab) {
        viewModel.setTab(selectedTab)
    }
    val tabs = listOf("TO WATCH", "WATCHED")
    var showSortSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMarkWatchedDialog by remember { mutableStateOf<WatchItem?>(null) }

    // Rating prompt — shown after marking any item as watched from the card
    var ratingDialogItem by remember { mutableStateOf<WatchItem?>(null) }
    var pendingRating by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(viewModel) {
        viewModel.showRatingPromptForItem.collectLatest { item ->
            pendingRating = item.rating
            ratingDialogItem = item
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            HomeTopBar(
                onSearchClick = onSearchClick,
                onFilterClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showSortSheet = true 
                },
                hasActiveFilter = uiState.sortFilterState.filter != FilterOption.ALL
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAddClick() 
                },
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
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = index 
                        },
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
                        items = uiState.items,
                        isLoading = uiState.isLoading,
                        isFiltered = uiState.sortFilterState.filter != FilterOption.ALL,
                        emptyIcon = Icons.Outlined.Bookmark,
                        emptyTitle = "Your watchlist is empty",
                        emptySubtitle = "Tap + to add movies and series\nyou want to watch",
                        onItemClick = onItemClick,
                        onToggleWatched = { item -> 
                            if (item.type == com.kaze.model.MediaType.SERIES && !item.isWatched) showMarkWatchedDialog = item
                            else viewModel.toggleWatched(item)
                        }
                    )
                    1 -> WatchItemList(
                        items = uiState.items,
                        isLoading = uiState.isLoading,
                        isFiltered = uiState.sortFilterState.filter != FilterOption.ALL,
                        emptyIcon = Icons.Outlined.CheckCircle,
                        emptyTitle = "Nothing watched yet",
                        emptySubtitle = "Mark items as watched and\nthey'll appear here",
                        onItemClick = onItemClick,
                        onToggleWatched = { item -> 
                            if (item.type == com.kaze.model.MediaType.SERIES && !item.isWatched) showMarkWatchedDialog = item
                            else viewModel.toggleWatched(item)
                        }
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


    if (showMarkWatchedDialog != null) {
        val itemToMark = showMarkWatchedDialog!!
        AlertDialog(
            onDismissRequest = { showMarkWatchedDialog = null },
            containerColor = SurfaceContainer,
            title = { Text("Mark entire series?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "This will mark all seasons and every episode of ${itemToMark.title} as watched.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleWatched(itemToMark)
                        showMarkWatchedDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WatchedGreen, contentColor = Background)
                ) {
                    Text("Yes, mark all")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkWatchedDialog = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // ── Rating dialog (shown after marking watched from card) ──────────────────
    if (ratingDialogItem != null) {
        val item = ratingDialogItem!!
        AlertDialog(
            onDismissRequest = { ratingDialogItem = null },
            containerColor = SurfaceContainer,
            title = {
                Text(
                    text = "Rate \"${item.title}\"?",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                StarRatingSelector(
                    rating = pendingRating,
                    onRatingChange = { pendingRating = it }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (pendingRating > 0f) viewModel.saveRating(item, pendingRating)
                        ratingDialogItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Background)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { ratingDialogItem = null }) {
                    Text("Skip", color = TextSecondary)
                }
            }
        )
    }

    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()

    var hideDownloadDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(true) }
    // Reset hide flag only when a new download starts (user taps Update Now again)
    LaunchedEffect(updateState) {
        if (updateState == com.kaze.updater.UpdateState.AVAILABLE) {
            hideDownloadDialog = false
            showUpdateDialog = true
        }
    }

    if (showUpdateDialog && updateState == com.kaze.updater.UpdateState.AVAILABLE && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
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
                    onClick = { 
                        showUpdateDialog = false
                        viewModel.downloadUpdate() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Background)
                ) {
                    Text("Update Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Later", color = TextSecondary)
                }
            }
        )
    }

    if (updateState == com.kaze.updater.UpdateState.DOWNLOADING && !hideDownloadDialog) {
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
    isFiltered: Boolean,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptySubtitle: String,
    onItemClick: (Long) -> Unit,
    onToggleWatched: (WatchItem) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    when {
        isLoading -> WatchLaterLoader()
        items.isEmpty() -> {
            if (isFiltered) {
                EmptyState(
                    icon = Icons.Outlined.CheckCircle,
                    title = "No matches found",
                    subtitle = "Try changing your filter settings",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                EmptyState(
                    icon = emptyIcon,
                    title = emptyTitle,
                    subtitle = emptySubtitle,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
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
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onItemClick(item.id) 
                    },
                    onToggleWatched = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleWatched(item) 
                    }
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
