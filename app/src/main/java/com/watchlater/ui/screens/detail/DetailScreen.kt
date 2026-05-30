package com.watchlater.ui.screens.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.watchlater.data.repository.EpisodeUiItem
import com.watchlater.model.MediaType
import com.watchlater.ui.components.*
import com.watchlater.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost   = remember { SnackbarHostState() }

    // Snackbar for saves
    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect { snackbarHost.showSnackbar("Saved ✓") }
    }
    // Snackbar for toasts (errors / confirmations)
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearToast()
        }
    }
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBack()
    }

    // Dialogs
    if (uiState.showDeleteDialog) {
        ConfirmDeleteDialog(
            title     = uiState.item?.title ?: "",
            onConfirm = { viewModel.deleteItem(onBack) },
            onDismiss = viewModel::dismissDeleteDialog
        )
    }
    if (uiState.showMarkAllSeriesDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissMarkAllSeriesDialog,
            containerColor   = SurfaceContainer,
            title = { Text("Mark entire series?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = {
                Text(
                    "This will mark all ${uiState.totalSeasons} seasons and every episode as watched.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markAllSeriesWatched()
                        viewModel.dismissMarkAllSeriesDialog()
                    },
                    colors  = ButtonDefaults.buttonColors(containerColor = WatchedGreen, contentColor = Background)
                ) {
                    Text("Yes, mark all", color = Color.Unspecified)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissMarkAllSeriesDialog) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = SurfaceElevated,
                    contentColor   = TextPrimary,
                    shape          = RoundedCornerShape(10.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextSecondary)
                    }
                },
                actions = {
                    // Mark series watched button (shows dialog)
                    if (uiState.item?.type == MediaType.SERIES && uiState.totalSeasons > 0) {
                        IconButton(onClick = viewModel::showMarkAllSeriesDialog) {
                            Icon(Icons.Filled.DoneAll, "Mark all watched", tint = TextSecondary)
                        }
                    }
                    IconButton(onClick = viewModel::toggleWatched) {
                        Icon(
                            imageVector = if (uiState.isWatched) Icons.Filled.CheckCircle
                                          else Icons.Outlined.CheckCircle,
                            contentDescription = "Toggle watched",
                            tint = if (uiState.isWatched) WatchedGreen else TextSecondary
                        )
                    }
                    IconButton(onClick = viewModel::showDeleteDialog) {
                        Icon(Icons.Outlined.DeleteOutline, "Delete", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        bottomBar = {
            Surface(color = Background, tonalElevation = 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick  = { viewModel.saveItem() },
                        enabled  = !uiState.isSaving && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Background),
                        shape    = MaterialTheme.shapes.medium
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(color = Background, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Save Changes", fontWeight = FontWeight.SemiBold, color = Color.Unspecified)
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> WatchLaterLoader()
            uiState.item == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Item not found", color = TextTertiary) }
            else -> {
                val item = uiState.item!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {
                    Spacer(Modifier.height(8.dp))

                    // ── Marking-all progress overlay ─────────────────────────
                    AnimatedVisibility(visible = uiState.isMarkingAllWatched) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentBlue.copy(alpha = 0.12f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color       = AccentBlue,
                                    strokeWidth = 2.dp,
                                    modifier    = Modifier.size(18.dp)
                                )
                                Text(
                                    "Marking Season ${uiState.markAllProgress} of ${uiState.totalSeasons}…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // ── Poster ───────────────────────────────────────────────
                    if (item.posterUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceHighlight)
                        ) {
                            AsyncImage(
                                model              = item.posterUrl,
                                contentDescription = "${item.title} poster",
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                        // ── Header ────────────────────────────────────────────
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TypeBadge(item.type)
                            if (uiState.isWatched) WatchedPill(isWatched = true)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(item.title, style = MaterialTheme.typography.headlineLarge, color = TextPrimary, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
                        Spacer(Modifier.height(4.dp))
                        if (item.year > 0) {
                            Text(item.year.toString(), style = MaterialTheme.typography.bodyLarge, color = TextTertiary)
                        }

                        // ── Genres ────────────────────────────────────────────
                        val genres = item.genreList
                        if (genres.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                genres.forEach { com.watchlater.ui.screens.add.GenreChip(it) }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        SubtleDivider()
                        Spacer(Modifier.height(24.dp))

                        // ── Series Episode Tracker ────────────────────────────
                        if (item.type == MediaType.SERIES) {
                            SeriesEpisodeSection(
                                uiState              = uiState,
                                onSeasonSelect       = viewModel::selectSeason,
                                onEpisodeToggle      = { s, ep -> viewModel.toggleEpisode(s, ep) },
                                onMarkSeasonWatched  = viewModel::markSeasonWatched
                            )
                            Spacer(Modifier.height(24.dp))
                            SubtleDivider()
                            Spacer(Modifier.height(24.dp))
                        }

                        // ── Rating ────────────────────────────────────────────
                        SectionHeader("RATING")
                        Spacer(Modifier.height(16.dp))
                        StarRatingSelector(rating = uiState.rating, onRatingChange = viewModel::onRatingChange)
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

// ── Series Episode Section ─────────────────────────────────────────────────

@Composable
private fun SeriesEpisodeSection(
    uiState: DetailUiState,
    onSeasonSelect: (Int) -> Unit,
    onEpisodeToggle: (Int, Int) -> Unit,
    onMarkSeasonWatched: () -> Unit
) {
    val totalSeasons   = uiState.totalSeasons
    val selectedSeason = uiState.selectedSeason
    val episodes       = uiState.seasonEpisodes

    // derivedStateOf prevents recomposition when other state changes
    val watchedCount by remember(episodes) {
        derivedStateOf { episodes.count { it.isWatched } }
    }
    val progressFraction by remember(watchedCount, episodes) {
        derivedStateOf { if (episodes.isEmpty()) 0f else watchedCount.toFloat() / episodes.size }
    }

    SectionHeader("EPISODES")
    Spacer(Modifier.height(14.dp))

    // ── Current position badge ─────────────────────────────────────────────
    if (uiState.currentSeason > 0 && uiState.currentEpisode > 0) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AccentBlue.copy(alpha = 0.09f))
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Icon(Icons.Filled.PlayCircle, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
            Text(
                "Currently at S${uiState.currentSeason} · E${uiState.currentEpisode}",
                style      = MaterialTheme.typography.bodySmall,
                color      = AccentBlue,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(14.dp))
    }

    // ── Season tabs ────────────────────────────────────────────────────────
    if (totalSeasons > 0) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding        = PaddingValues(end = 8.dp)
        ) {
            items(totalSeasons, key = { it + 1 }) { idx ->
                val season    = idx + 1
                val isSelected = season == selectedSeason
                FilterChip(
                    selected  = isSelected,
                    onClick   = { onSeasonSelect(season) },
                    label     = { Text("Season $season", fontSize = 13.sp, color = Color.Unspecified) },
                    colors    = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue,
                        selectedLabelColor     = Background,
                        containerColor         = SurfaceElevated,
                        labelColor             = TextSecondary
                    ),
                    border    = FilterChipDefaults.filterChipBorder(
                        enabled             = true,
                        selected            = isSelected,
                        borderColor         = SurfaceHighlight,
                        selectedBorderColor = AccentBlue
                    )
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // ── Loading / empty states ─────────────────────────────────────────────
    when {
        uiState.isLoadingEpisodes -> {
            Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        }
        episodes.isEmpty() && totalSeasons == 0 -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
                    .padding(16.dp)
            ) {
                Text(
                    "Search and add this series via the '+' button to enable episode tracking",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
        episodes.isEmpty() -> {
            Text("No episodes found for Season $selectedSeason", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        else -> {
            // ── Progress summary ───────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "$watchedCount / ${episodes.size} episodes watched",
                    style      = MaterialTheme.typography.bodySmall,
                    color      = TextTertiary,
                    fontWeight = FontWeight.Medium
                )
                if (watchedCount < episodes.size) {
                    TextButton(
                        onClick        = onMarkSeasonWatched,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Mark season watched", style = MaterialTheme.typography.bodySmall, color = AccentBlue, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Animated progress bar
            val animatedProgress by animateFloatAsState(
                targetValue    = progressFraction,
                animationSpec  = tween(400, easing = FastOutSlowInEasing),
                label          = "progress"
            )
            LinearProgressIndicator(
                progress   = { animatedProgress },
                modifier   = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                color      = AccentBlue,
                trackColor = SurfaceHighlight
            )
            Spacer(Modifier.height(12.dp))

            // ── Episode rows ───────────────────────────────────────────────
            episodes.forEachIndexed { index, ep ->
                key(ep.season, ep.episodeNumber) {
                    EpisodeRow(
                        episode   = ep,
                        isCurrent = ep.season == uiState.currentSeason && ep.episodeNumber == uiState.currentEpisode,
                        onToggle  = { onEpisodeToggle(ep.season, ep.episodeNumber) }
                    )
                    if (index < episodes.lastIndex) {
                        SubtleDivider(modifier = Modifier.padding(start = 56.dp))
                    }
                }
            }
        }
    }
}

// ── Episode Row ────────────────────────────────────────────────────────────

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EpisodeRow(
    episode: EpisodeUiItem,
    isCurrent: Boolean,
    onToggle: () -> Unit
) {
    val bgColor = if (isCurrent) AccentBlue.copy(alpha = 0.07f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (isCurrent) Modifier.border(1.dp, AccentBlue.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Episode number bubble
        Box(
            modifier         = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (episode.isWatched) WatchedGreen.copy(alpha = 0.13f) else SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "E${episode.episodeNumber}",
                style      = MaterialTheme.typography.labelSmall,
                color      = if (episode.isWatched) WatchedGreen else TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }

        // Title + release
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = episode.title,
                style      = MaterialTheme.typography.bodyMedium,
                color      = if (episode.isWatched) TextSecondary else TextPrimary,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            if (episode.released.isNotBlank()) {
                Text(episode.released, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
        }

        // Animated checkbox
        AnimatedContent(
            targetState  = episode.isWatched,
            transitionSpec = {
                (scaleIn(initialScale = 0.6f, animationSpec = spring(Spring.DampingRatioMediumBouncy)) +
                    fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut())
            },
            label = "ep_check_${episode.episodeNumber}"
        ) { watched ->
            Icon(
                imageVector        = if (watched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                contentDescription = if (watched) "Watched" else "Not watched",
                tint               = if (watched) WatchedGreen else SurfaceHighlight,
                modifier           = Modifier.size(22.dp)
            )
        }
    }
}
