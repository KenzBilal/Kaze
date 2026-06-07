package com.kaze.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaze.ui.screens.home.WatchLength
import com.kaze.ui.screens.home.WatchType
import com.kaze.ui.screens.home.WhatToWatchViewModel
import com.kaze.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatToWatchBottomSheet(
    viewModel: WhatToWatchViewModel,
    onDismiss: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    val availableGenres by viewModel.availableGenres.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val selectedGenres by viewModel.selectedGenres.collectAsStateWithLifecycle()
    val selectedLength by viewModel.selectedLength.collectAsStateWithLifecycle()
    val suggestedItem by viewModel.suggestedItem.collectAsStateWithLifecycle()

    var showResult by remember { mutableStateOf(false) }

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
                WatchItemCard(
                    item = item,
                    onClick = { onItemClick(item.id) },
                    onToggleWatched = {} // Don't allow toggle here, just click to view
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.suggest() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = TextPrimary)
                ) {
                    Text("Re-roll Suggestion")
                }
            } else {
                EmptyState(
                    icon = Icons.Default.Close,
                    title = "No Matches",
                    subtitle = "We couldn't find anything matching your filters in your 'To Watch' list.",
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
                            onClick = { viewModel.setType(type) },
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
                            onClick = { viewModel.toggleGenre(genre) },
                            label = { Text(genre) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SurfaceElevated,
                                selectedLabelColor = TextPrimary,
                                containerColor = Background
                            )
                        )
                    }
                }

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
                                    if (selectedLength == len) viewModel.setLength(null) 
                                    else viewModel.setLength(len) 
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
                        viewModel.suggest()
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
