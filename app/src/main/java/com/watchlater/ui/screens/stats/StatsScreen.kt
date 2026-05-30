package com.watchlater.ui.screens.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watchlater.model.MediaType
import com.watchlater.model.WatchItem
import com.watchlater.ui.components.*
import com.watchlater.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Stats",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            WatchLaterLoader()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Overview Cards ────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            label = "Total",
                            value = uiState.total.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Watched",
                            value = uiState.watched.toString(),
                            modifier = Modifier.weight(1f),
                            accent = WatchedGreen
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            label = "Movies",
                            value = uiState.movies.toString(),
                            modifier = Modifier.weight(1f),
                            accent = MovieBadgeFg
                        )
                        StatCard(
                            label = "Series",
                            value = uiState.series.toString(),
                            modifier = Modifier.weight(1f),
                            accent = SeriesBadgeFg
                        )
                    }
                }

                // ── Completion Bar ────────────────────────────────────────
                item {
                    WatchedPercentageCard(
                        watched = uiState.watched,
                        total = uiState.total
                    )
                }

                // ── Series In Progress ────────────────────────────────────
                if (uiState.seriesInProgress.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "IN PROGRESS",
                            subtitle = "${uiState.seriesInProgress.size} series"
                        )
                    }
                    items(uiState.seriesInProgress, key = { it.id }) { item ->
                        InProgressCard(item = item)
                    }
                }

                // ── Recently Added ────────────────────────────────────────
                if (uiState.recentlyAdded.isNotEmpty()) {
                    item { SectionHeader(title = "RECENTLY ADDED") }
                    items(uiState.recentlyAdded, key = { "recent_${it.id}" }) { item ->
                        RecentlyAddedRow(item = item)
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────

@Composable
private fun WatchedPercentageCard(watched: Int, total: Int) {
    val percentage = if (total == 0) 0f else watched.toFloat() / total
    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 900, easing = EaseOutCubic),
        label = "progress_bar"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Completion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SurfaceHighlight)
            ) {
                if (animatedProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(listOf(AccentBlue, AccentPurple))
                            )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$watched of $total items watched",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun InProgressCard(item: WatchItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.year.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
            if (item.season != null && item.episode != null) {
                ProgressChip(season = item.season, episode = item.episode)
            }
        }
    }
}

@Composable
private fun RecentlyAddedRow(item: WatchItem) {
    val dateStr = remember(item.dateAdded) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(item.dateAdded))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${item.year} · ${if (item.type == MediaType.MOVIE) "Movie" else "Series"}",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = dateStr,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )
    }
}
