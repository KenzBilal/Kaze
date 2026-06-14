package com.kaze.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                // We reuse DiscoverItemCard if it exists, or just a simple UI
                Text(item.title, fontWeight = FontWeight.Bold)
                Text(item.genres, color = TextSecondary)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onItemClick(item) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("View Details")
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                Text("1. What are you in the mood for?", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WatchType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue,
                                selectedLabelColor = Background
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
