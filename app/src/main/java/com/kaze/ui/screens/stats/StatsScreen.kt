package com.kaze.ui.screens.stats

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
import com.kaze.model.MediaType
import com.kaze.model.WatchItem
import com.kaze.ui.components.*
import com.kaze.ui.theme.*
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

                // ── Total Time Spent ───────────────────────────────────────
                item {
                    StatCard(
                        label = "Total Watch Time",
                        value = "${uiState.totalHours} hrs",
                        accent = AccentPurple,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── Top Genres ─────────────────────────────────────────────
                if (uiState.topGenres.isNotEmpty()) {
                    item {
                        SectionHeader(title = "TOP GENRES", subtitle = "Based on watched items")
                    }
                    item {
                        TopGenresCard(genres = uiState.topGenres)
                    }
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
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
}

@Composable
private fun TopGenresCard(genres: Map<String, Int>) {
    val totalTags = genres.values.sum().coerceAtLeast(1)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // "Pie chart" using horizontal segments
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                val colors = listOf(AccentBlue, WatchedGreen, AccentPurple, MovieBadgeFg, SeriesBadgeFg)
                genres.entries.forEachIndexed { index, entry ->
                    val weight = entry.value.toFloat() / totalTags
                    Box(
                        modifier = Modifier
                            .weight(weight.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(colors[index % colors.size])
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            
            // Legend
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val colors = listOf(AccentBlue, WatchedGreen, AccentPurple, MovieBadgeFg, SeriesBadgeFg)
                genres.entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colors[index % colors.size]))
                            Spacer(Modifier.width(8.dp))
                            Text(entry.key, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Text("${entry.value} items", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    }
                }
            }
        }
    }
}
