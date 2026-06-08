package com.kaze.ui.screens.detail

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kaze.data.repository.EpisodeUiItem
import com.kaze.model.MediaType
import com.kaze.ui.components.*
import com.kaze.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost   = remember { SnackbarHostState() }
    val haptic         = LocalHapticFeedback.current

    // Snackbar for saves
    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect { snackbarHost.showSnackbar("Saved ✓") }
    }
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearToast()
        }
    }
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBack()
    }

    // ── Dialogs ────────────────────────────────────────────────────────────
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
                ) { Text("Yes, mark all", color = Color.Unspecified) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissMarkAllSeriesDialog) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (uiState.showRatingPrompt) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRatingPrompt,
            containerColor   = SurfaceContainer,
            title = { Text("Rate this title", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = {
                Column {
                    Text(
                        "You just finished watching it! How would you rate it?",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    StarRatingSelector(
                        rating = uiState.rating,
                        onRatingChange = { r ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onRatingChange(r)
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.saveItem()
                        viewModel.dismissRatingPrompt()
                    },
                    colors  = ButtonDefaults.buttonColors(containerColor = WatchedGreen, contentColor = Background)
                ) { Text("Save", color = Color.Unspecified) }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.dismissRatingPrompt()
                }) { Text("Rate Later", color = TextSecondary) }
            }
        )
    }

    // ── Episode Plot Dialog ────────────────────────────────────────────────
    if (uiState.showEpisodePlotDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEpisodePlotDialog,
            containerColor   = SurfaceContainer,
            shape            = RoundedCornerShape(16.dp),
            title = {
                Text(
                    uiState.episodePlotTitle,
                    color      = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
            },
            text = {
                if (uiState.isLoadingEpisodePlot) {
                    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    }
                } else {
                    Text(
                        uiState.episodePlotText,
                        color  = TextSecondary,
                        style  = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissEpisodePlotDialog) {
                    Text("Close", color = AccentBlue, fontWeight = FontWeight.Medium)
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
                    if (!uiState.isPreview) {
                        if (!uiState.isWatched) {
                            if (uiState.item?.type == MediaType.SERIES && uiState.totalSeasons > 0) {
                                IconButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.showMarkAllSeriesDialog()
                                }) {
                                    Icon(Icons.Filled.DoneAll, "Mark all watched", tint = TextSecondary)
                                }
                            } else if (uiState.item?.type != MediaType.SERIES) {
                                IconButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.toggleWatched()
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = "Toggle watched",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.showDeleteDialog()
                        }) {
                            Icon(Icons.Outlined.DeleteOutline, "Delete", tint = TextSecondary)
                        }
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
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.saveItem(onSuccess = if (uiState.isPreview) onBack else null)
                        },
                        enabled  = !uiState.isSaving && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Background),
                        shape    = MaterialTheme.shapes.medium
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(color = Background, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            val btnText = if (uiState.isPreview) "Add to Watchlist" else "Save Changes"
                            Text(btnText, fontWeight = FontWeight.SemiBold, color = Color.Unspecified)
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

                    // ── Marking-all progress overlay ────────────────────────
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

                    // ── Poster ──────────────────────────────────────────────
                    if (item.posterUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceHighlight),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model              = item.posterUrl,
                                contentDescription = "${item.title} poster",
                                contentScale       = ContentScale.Fit,
                                modifier           = Modifier.fillMaxSize().padding(16.dp)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                        // ── Header ──────────────────────────────────────────
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

                        // ── Genres ──────────────────────────────────────────
                        val genres = item.genreList
                        if (genres.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                genres.forEach { com.kaze.ui.screens.add.GenreChip(it) }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        SubtleDivider()
                        Spacer(Modifier.height(24.dp))

                        // ── Trailer + Plot (shown for ALL items, movies and series, watched or not) ──
                        val plot = uiState.item?.plot ?: ""
                        val trailerUrl = uiState.trailerUrl

                        if (trailerUrl.isNotBlank() || uiState.isLoadingTrailer) {
                            if (uiState.isLoadingTrailer) {
                                // Skeleton placeholder while trailer URL loads
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                                }
                            } else {
                                TapToPlayTrailer(trailerUrl = trailerUrl)
                            }
                            Spacer(Modifier.height(20.dp))
                        }

                        if (plot.isNotBlank()) {
                            Text(
                                text       = plot,
                                style      = MaterialTheme.typography.bodyMedium,
                                color      = TextSecondary,
                                lineHeight = 22.sp
                            )
                            Spacer(Modifier.height(24.dp))
                            SubtleDivider()
                            Spacer(Modifier.height(24.dp))
                        }

                        // ── Series episode section (only when not yet fully watched) ──
                        if (item.type == MediaType.SERIES && !uiState.isWatched) {
                            SeriesEpisodeSection(
                                uiState              = uiState,
                                onSeasonSelect       = viewModel::selectSeason,
                                onEpisodeToggle      = { s, ep ->
                                    if (!uiState.isPreview) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.toggleEpisode(s, ep)
                                    }
                                },
                                onMarkSeasonWatched  = {
                                    if (!uiState.isPreview) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.markSeasonWatched()
                                    }
                                },
                                onEpisodePlotClick   = viewModel::fetchEpisodePlot
                            )
                            Spacer(Modifier.height(24.dp))
                            SubtleDivider()
                            Spacer(Modifier.height(24.dp))
                        }

                        // ── Rating / Review ─────────────────────────────────
                        if (!uiState.isPreview && uiState.isWatched) {
                            SectionHeader("RATING")
                            Spacer(Modifier.height(16.dp))
                            StarRatingSelector(
                                rating = uiState.rating,
                                onRatingChange = { r ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.onRatingChange(r)
                                }
                            )
                            Spacer(Modifier.height(32.dp))

                            SectionHeader("REVIEW & NOTES")
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = uiState.notes,
                                onValueChange = viewModel::onNotesChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 120.dp),
                                placeholder = { Text("What did you think of it?", color = TextTertiary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor      = AccentBlue,
                                    unfocusedBorderColor    = SurfaceHighlight,
                                    focusedContainerColor   = SurfaceElevated,
                                    unfocusedContainerColor = SurfaceElevated,
                                    focusedTextColor        = TextPrimary,
                                    unfocusedTextColor      = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else if (uiState.rating > 0f || uiState.notes.isNotBlank()) {
                            val isFriendRecommendation = uiState.isPreview && uiState.notes.startsWith("Recommended by ")
                            val headerText = if (isFriendRecommendation) {
                                val friendName = uiState.notes.removePrefix("Recommended by ")
                                "${friendName.uppercase()}'S RATING"
                            } else {
                                "RATING"
                            }
                            SectionHeader(headerText)
                            Spacer(Modifier.height(16.dp))
                            if (uiState.rating > 0f) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, contentDescription = "Rating", tint = androidx.compose.ui.graphics.Color(0xFFFFC107), modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = "${"%.4g".format(uiState.rating)} / 5", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                            if (uiState.notes.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceElevated)
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text      = "\"${uiState.notes}\"",
                                        color     = TextSecondary,
                                        fontSize  = 15.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

// ── Tap-to-Play Trailer ────────────────────────────────────────────────────
// Shows a YouTube thumbnail + play button. Loads WebView only after user taps.
// This avoids scroll jank caused by WebView intercepting touch events.

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TapToPlayTrailer(trailerUrl: String) {
    val videoId = remember(trailerUrl) {
        Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([A-Za-z0-9_-]{11})")
            .find(trailerUrl)?.groupValues?.getOrNull(1)
    } ?: return

    var playing by remember { mutableStateOf(false) }

    val embedHtml = remember(videoId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          * { margin: 0; padding: 0; background: #000; }
          html, body { width: 100%; height: 100%; }
          iframe { width: 100%; height: 100%; border: none; }
        </style>
        </head>
        <body>
        <iframe
          src="https://www.youtube.com/embed/$videoId?autoplay=1&controls=1&modestbranding=1&rel=0&playsinline=1"
          allow="autoplay; encrypted-media"
          allowfullscreen="true">
        </iframe>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!playing) {
            // Static thumbnail — no scroll conflict
            AsyncImage(
                model              = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                contentDescription = "Trailer thumbnail",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
            // Dark overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
            // Play button + label
            Column(
                modifier              = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { playing = true }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Play Trailer",
                    tint     = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    "WATCH TRAILER",
                    color      = Color.White,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        } else {
            // WebView loads only after tap — no scroll jank
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        loadDataWithBaseURL(
                            "https://www.youtube.com",
                            embedHtml,
                            "text/html",
                            "utf-8",
                            null
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ── Series Episode Section ─────────────────────────────────────────────────

@Composable
private fun SeriesEpisodeSection(
    uiState: DetailUiState,
    onSeasonSelect: (Int) -> Unit,
    onEpisodeToggle: (Int, Int) -> Unit,
    onMarkSeasonWatched: () -> Unit,
    onEpisodePlotClick: (EpisodeUiItem) -> Unit
) {
    val totalSeasons   = uiState.totalSeasons
    val selectedSeason = uiState.selectedSeason
    val episodes       = uiState.seasonEpisodes

    val watchedCount by remember(episodes) {
        derivedStateOf { episodes.count { it.isWatched } }
    }
    val progressFraction by remember(watchedCount, episodes) {
        derivedStateOf { if (episodes.isEmpty()) 0f else watchedCount.toFloat() / episodes.size }
    }

    SectionHeader("EPISODES")
    Spacer(Modifier.height(14.dp))

    // ── Season tabs ────────────────────────────────────────────────────────
    if (totalSeasons > 0) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding        = PaddingValues(end = 8.dp)
        ) {
            items(totalSeasons, key = { it + 1 }) { idx ->
                val season     = idx + 1
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

    // ── No season selected state ───────────────────────────────────────────
    if (selectedSeason == 0) {
        return
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
            // ── Progress summary ─────────────────────────────────────────
            if (!uiState.isPreview) {
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
                val animatedProgress by animateFloatAsState(
                    targetValue   = progressFraction,
                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                    label         = "progress"
                )
                LinearProgressIndicator(
                    progress   = { animatedProgress },
                    modifier   = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                    color      = AccentBlue,
                    trackColor = SurfaceHighlight
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── Episode rows ─────────────────────────────────────────────
            episodes.forEachIndexed { index, ep ->
                key(ep.season, ep.episodeNumber) {
                    EpisodeRow(
                        episode            = ep,
                        isCurrent          = ep.season == uiState.currentSeason && ep.episodeNumber == uiState.currentEpisode,
                        onToggle           = { onEpisodeToggle(ep.season, ep.episodeNumber) },
                        onPlotClick        = { onEpisodePlotClick(ep) },
                        isPreview          = uiState.isPreview
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
    onToggle: () -> Unit,
    onPlotClick: () -> Unit,
    isPreview: Boolean = false
) {
    val bgColor = if (isCurrent) AccentBlue.copy(alpha = 0.07f) else Color.Transparent
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (isCurrent) Modifier.border(1.dp, AccentBlue.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .then(if (!isPreview) Modifier.clickable(onClick = onToggle) else Modifier)
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

        // Title only (no release date)
        Text(
            text       = episode.title,
            modifier   = Modifier.weight(1f),
            style      = MaterialTheme.typography.bodyMedium,
            color      = if (episode.isWatched) TextSecondary else TextPrimary,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )

        if (!isPreview) {
            // 3-dot menu
            Box {
                IconButton(
                    onClick  = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint     = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded         = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor   = SurfaceContainer
                ) {
                    DropdownMenuItem(
                        text    = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Info, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                Text("Plot", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onPlotClick()
                        }
                    )
                }
            }

            // Animated checkbox
            AnimatedContent(
                targetState = episode.isWatched,
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
}
