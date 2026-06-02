package com.kaze.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kaze.model.MediaType
import com.kaze.model.WatchItem
import com.kaze.ui.theme.*

@Composable
fun WatchItemCard(
    item: WatchItem,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top
        ) {
            // ── Poster ────────────────────────────────────────────────────
            if (item.posterUrl != null) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = "${item.title} poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(60.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                        .background(SurfaceHighlight)
                )
            }

            // ── Content ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (item.posterUrl != null) 12.dp else 4.dp,
                        end = 8.dp,
                        top = 14.dp,
                        bottom = 14.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Watched toggle
                IconButton(
                    onClick = onToggleWatched,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (item.isWatched) Icons.Filled.CheckCircle
                                      else Icons.Outlined.CheckCircle,
                        contentDescription = if (item.isWatched) "Mark as unwatched" else "Mark as watched",
                        tint = if (item.isWatched) WatchedGreen else TextDisabled,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (item.isWatched) TextSecondary else TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.year > 0) {
                            Text(
                                text = item.year.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        TypeBadge(type = item.type)
                        if (item.isWatched) WatchedPill(isWatched = true)
                        if (item.type == MediaType.SERIES && !item.isWatched &&
                            item.season != null && item.episode != null) {
                            ProgressChip(season = item.season, episode = item.episode)
                        }
                    }
                    // Genre tags
                    val genres = item.genreList.take(3)
                    if (genres.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            genres.forEach { genre ->
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentBlue.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                                if (genre != genres.last()) {
                                    Text("·", style = MaterialTheme.typography.labelSmall, color = TextDisabled, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    if (item.rating > 0f) {
                        StarRatingDisplay(rating = item.rating)
                    }
                }
            }
        }

        // Progress gradient for series
        if (item.type == MediaType.SERIES && (item.isWatched || (item.season != null || item.episode != null))) {
            val progressFraction = if (item.isWatched) 1f else {
                val s = item.season ?: 1
                val e = item.episode ?: 0
                // Asymptotic fake math. Assumes ~15 eps/season.
                // Moves fast early on, slows down later. Never hits 100%.
                val watchedWeight = ((s - 1) * 15f) + e
                val frac = 1f - (1f / (1f + (watchedWeight * 0.04f)))
                frac.coerceIn(0.05f, 0.95f)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFraction)
                    .height(2.dp)
                    .background(
                        if (item.isWatched) {
                            Brush.horizontalGradient(listOf(AccentBlue, AccentBlue))
                        } else {
                            Brush.horizontalGradient(listOf(AccentBlue.copy(alpha = 0.7f), AccentPurple.copy(alpha = 0.3f)))
                        }
                    )
            )
        }
    }
}
