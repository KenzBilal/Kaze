package com.watchlater.ui.screens.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.watchlater.model.MediaType
import com.watchlater.ui.components.SubtleDivider
import com.watchlater.ui.theme.*
import androidx.compose.ui.graphics.Color.Companion.White

@Composable
fun AddItemSheet(
    viewModel: AddItemViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Add to Watchlist",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        SubtleDivider()
        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

            // ── Search / Title field ──────────────────────────────────────
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title", color = TextTertiary) },
                placeholder = { Text("Search or type movie / series…", color = TextDisabled) },
                isError = uiState.titleError != null,
                supportingText = uiState.titleError?.let { { Text(it, color = SemanticError) } },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary)
                },
                trailingIcon = {
                    if (uiState.isSearching) {
                        CircularProgressIndicator(
                            color = AccentBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (uiState.title.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onTitleChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextTertiary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    viewModel.dismissSuggestions()
                }),
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedTextFieldColors(),
                singleLine = true
            )

            // ── TMDB Suggestions ─────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.showSuggestions && uiState.searchResults.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(SurfaceElevated)
                ) {
                    uiState.searchResults.forEachIndexed { index, result ->
                        if (index > 0) SubtleDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        TmdbSuggestionRow(
                            result = result,
                            onClick = {
                                keyboard?.hide()
                                viewModel.selectTmdbResult(result)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Year field ───────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.year,
                onValueChange = viewModel::onYearChange,
                label = { Text("Year", color = TextTertiary) },
                placeholder = { Text("e.g. 2024", color = TextDisabled) },
                isError = uiState.yearError != null,
                supportingText = uiState.yearError?.let { { Text(it, color = SemanticError) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedTextFieldColors(),
                singleLine = true
            )

            Spacer(Modifier.height(20.dp))

            // ── Type selector ────────────────────────────────────────────
            Text(
                text = "TYPE",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MediaType.entries.forEach { type ->
                    val selected = uiState.type == type
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onTypeChange(type) },
                        label = {
                            Text(
                                text = if (type == MediaType.MOVIE) "Movie" else "Series",
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceHighlight,
                            labelColor = TextSecondary,
                            selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                            selectedLabelColor = AccentBlue
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = SurfaceBorder,
                            selectedBorderColor = AccentBlue.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // ── Series hint ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.type == MediaType.SERIES,
                enter = fadeIn() + expandVertically()
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Progress tracking will start at S1 • E1",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            // ── Genres preview ───────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.genres.isNotEmpty(),
                enter = fadeIn() + expandVertically()
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "GENRES",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.genres.split(",").filter { it.isNotBlank() }.take(4).forEach { genre ->
                            GenreChip(genre.trim())
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Save button ──────────────────────────────────────────────
            Button(
                onClick = { viewModel.saveItem(onSuccess = onDismiss) },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    contentColor = White,
                    disabledContainerColor = AccentBlueDim
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Add to Watchlist", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TmdbSuggestionRow(
    result: com.watchlater.data.remote.TmdbResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Poster thumbnail
        AsyncImage(
            model = result.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 36.dp, height = 54.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceHighlight)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (result.displayYear > 0) {
                    Text(
                        text = result.displayYear.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
                val typeLabel = if (result.mediaType == "tv") "Series" else "Movie"
                Text(
                    text = "· $typeLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
fun GenreChip(genre: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AccentBlue.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = genre,
            style = MaterialTheme.typography.labelSmall,
            color = AccentBlue,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = SurfaceBorder,
    focusedLabelColor = AccentBlue,
    unfocusedLabelColor = TextTertiary,
    cursorColor = AccentBlue,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    errorBorderColor = SemanticError,
    errorLabelColor = SemanticError
)
