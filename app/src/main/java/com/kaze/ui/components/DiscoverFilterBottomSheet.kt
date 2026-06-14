package com.kaze.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kaze.data.remote.DiscoverItem
import com.kaze.ui.screens.home.WatchType
import com.kaze.ui.screens.home.WatchLength
import com.kaze.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverFilterBottomSheet(
    items: List<DiscoverItem>,
    onDismiss: () -> Unit,
    onItemClick: (DiscoverItem) -> Unit
) {
    val availableGenres = remember(items) {
        items.flatMap { it.genres.split(",").map { g -> g.trim() } }
            .filter { it.isNotBlank() }
            .toSet().sorted()
    }

    var selectedType by remember { mutableStateOf(WatchType.BOTH) }
    var selectedGenres by remember { mutableStateOf(setOf<String>()) }
    var selectedLength by remember { mutableStateOf<WatchLength?>(null) }
    var suggestedItem by remember { mutableStateOf<DiscoverItem?>(null) }
    var showResult by remember { mutableStateOf(false) }

    fun suggest() {
        var filtered = items
        if (selectedType == WatchType.MOVIE) filtered = filtered.filter { it.type.equals("MOVIE", true) }
        else if (selectedType == WatchType.SERIES) filtered = filtered.filter { it.type.equals("SERIES", true) }

        if (selectedGenres.isNotEmpty()) {
            filtered = filtered.filter { item ->
                val itemGenres = item.genres.split(",").map { it.trim() }
                itemGenres.any { it in selectedGenres }
            }
        }
        // Series length filter — uses notes field or genres since DiscoverItem has no season count
        // We filter by type only since external discover items don't have season data
        suggestedItem = if (filtered.isNotEmpty()) filtered.random() else null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showResult) "Your Suggestion" else "What To Watch?",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showResult) {
            if (suggestedItem != null) {
                val item = suggestedItem!!
                // Full discover card matching the main discover grid style
                DiscoverSuggestionCard(
                    item = item,
                    onClick = {
                        onItemClick(item)
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { suggest() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = TextPrimary)
                ) {
                    Text("Re-roll Suggestion")
                }
            } else {
                EmptyState(
                    icon = Icons.Default.Close,
                    title = "No Matches",
                    subtitle = "We couldn't find anything matching your filters.",
                    modifier = Modifier.fillMaxWidth().padding(32.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showResult = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = TextPrimary)
                ) {
                    Text("Change Filters")
                }
            }
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Q1 — Type
                Text("1. What are you in the mood for?", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WatchType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                if (type == WatchType.MOVIE) selectedLength = null
                            },
                            label = { Text(type.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue,
                                selectedLabelColor = Background
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Q2 — Genres
                Text("2. Select Genres (Optional)", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableGenres.forEach { genre ->
                        val isSelected = selectedGenres.contains(genre)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedGenres = if (isSelected) selectedGenres - genre else selectedGenres + genre
                            },
                            label = { Text(genre) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SurfaceElevated,
                                selectedLabelColor = TextPrimary,
                                containerColor = Background
                            )
                        )
                    }
                }

                // Q3 — Series length (only when SERIES or BOTH selected)
                if (selectedType == WatchType.SERIES || selectedType == WatchType.BOTH) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("3. Series Length (Optional)", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val lengths = listOf(
                            WatchLength.SHORT to "Short (1-2 S)",
                            WatchLength.MID to "Mid (3-4 S)",
                            WatchLength.LONG to "Long (5+ S)"
                        )
                        lengths.forEach { (len, label) ->
                            FilterChip(
                                selected = selectedLength == len,
                                onClick = {
                                    selectedLength = if (selectedLength == len) null else len
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentBlue,
                                    selectedLabelColor = Background
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        suggest()
                        showResult = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Background)
                ) {
                    Text("Suggest Something")
                }
            }
        }
    }
}

@Composable
private fun DiscoverSuggestionCard(item: DiscoverItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
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
                    .height(240.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(SurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.type.uppercase() == "SERIES") Icons.Filled.Tv else Icons.Filled.Movie,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                item.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.type.lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                if (item.year > 0) {
                    Text("· ${item.year}", fontSize = 12.sp, color = TextTertiary)
                }
                if (item.rating > 0f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color(0xFFFFC107),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text("${"%.1f".format(item.rating)}/5", fontSize = 12.sp, color = TextTertiary)
                    }
                }
            }
            if (item.genres.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(item.genres, fontSize = 11.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Tap to view details →",
                fontSize = 12.sp,
                color = AccentBlue,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
