package com.kaze.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaze.model.MediaType
import com.kaze.ui.theme.*

// ── Type Badge ────────────────────────────────────────────────────────────────

@Composable
fun TypeBadge(type: MediaType, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (type) {
        MediaType.MOVIE  -> Triple(MovieBadgeBg, MovieBadgeFg, "MOVIE")
        MediaType.SERIES -> Triple(SeriesBadgeBg, SeriesBadgeFg, "SERIES")
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = label, color = fg, fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
    }
}

// ── Progress Chip ─────────────────────────────────────────────────────────────

@Composable
fun ProgressChip(season: Int, episode: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(SurfaceHighlight)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "S$season  •  E$episode",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp
        )
    }
}

// ── Star Rating Display ───────────────────────────────────────────────────────

@Composable
fun StarRatingDisplay(rating: Float, maxStars: Int = 5, modifier: Modifier = Modifier) {
    val filled = rating.coerceIn(0f, 5f)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 1..maxStars) {
            Icon(
                imageVector = if (i <= filled.toInt()) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (i <= filled.toInt()) StarFilled else StarEmpty,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ── Watched Pill ──────────────────────────────────────────────────────────────

@Composable
fun WatchedPill(isWatched: Boolean, modifier: Modifier = Modifier) {
    if (!isWatched) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(WatchedGreenBg)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = "WATCHED", color = WatchedGreen, fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
    }
}

// ── Interactive Star Rating ────────────────────────────────────────────────────

@Composable
fun StarRatingSelector(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    maxRating: Int = 5,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = if (rating == 0f) "Not rated" else "${"%g".format(rating)} / 5",
            style = MaterialTheme.typography.bodyMedium,
            color = if (rating == 0f) TextTertiary else TextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 1..maxRating) {
                val filled = i <= rating.toInt()
                val scale by animateFloatAsState(
                    targetValue = if (filled) 1.15f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "star_scale_$i"
                )
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Rate $i",
                    tint = if (filled) StarFilled else TextDisabled,
                    modifier = Modifier
                        .size((28 * scale).dp)
                        .clickable {
                            onRatingChange(if (rating == i.toFloat()) 0f else i.toFloat())
                        }
                )
            }
        }
    }
}

// ── Number Stepper ────────────────────────────────────────────────────────────

@Composable
fun NumberStepper(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    minValue: Int = 1,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextTertiary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledIconButton(
                onClick = { if (value > minValue) onDecrement() },
                enabled = value > minValue,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = SurfaceHighlight,
                    contentColor = TextPrimary,
                    disabledContainerColor = SurfaceBorder,
                    disabledContentColor = TextDisabled
                ),
                modifier = Modifier.size(38.dp)
            ) {
                Text("−", fontSize = 18.sp, fontWeight = FontWeight.Light, color = TextPrimary)
            }
            Text(
                text = value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 36.dp)
            )
            FilledIconButton(
                onClick = onIncrement,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = SurfaceHighlight,
                    contentColor = TextPrimary
                ),
                modifier = Modifier.size(38.dp)
            ) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Light, color = TextPrimary)
            }
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.SemiBold
        )
        subtitle?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
    }
}

// ── Divider ───────────────────────────────────────────────────────────────────

@Composable
fun SubtleDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 0.5.dp, color = SurfaceBorder)
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
fun WatchLaterLoader(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = AccentBlue,
            strokeWidth = 2.dp,
            modifier = Modifier.size(32.dp)
        )
    }
}

// ── Delete Confirmation Dialog ────────────────────────────────────────────────

@Composable
fun ConfirmDeleteDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Delete \"$title\"?") },
        text = { Text("This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = SemanticError, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

// ── Stat Card ─────────────────────────────────────────────────────────────────

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = AccentBlue
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineMedium,
                color = accent, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
    }
}
