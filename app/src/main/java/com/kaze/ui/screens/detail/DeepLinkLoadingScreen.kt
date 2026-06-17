package com.kaze.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaze.data.remote.OmdbRepository
import com.kaze.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Safe deep link loading screen.
 *
 * Flow:
 *  1. Show loading spinner while fetching title from OMDB using imdbId.
 *  2. On success → navigate to DetailPreview.
 *  3. On failure (bad link / no internet) → show friendly error with "Search Instead" fallback.
 *
 * This guarantees the app NEVER shows a blank or crash screen when a deep link fires.
 */
@Composable
fun DeepLinkLoadingScreen(
    imdbId: String,
    omdbRepository: OmdbRepository,
    onTitleResolved: (title: String, type: String, poster: String?, year: Int) -> Unit,
    onSearchInstead: () -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorType by remember { mutableStateOf<ErrorType?>(null) }

    LaunchedEffect(imdbId) {
        if (imdbId.isBlank()) {
            errorType = ErrorType.BAD_LINK
            isLoading = false
            return@LaunchedEffect
        }
        try {
            // Small artificial delay so screen doesn't flash
            delay(300)
            val detail = omdbRepository.fetchDetail(imdbId, plotLength = "short")
            if (detail.title.isNotBlank()) {
                onTitleResolved(
                    detail.title,
                    if (detail.type.equals("series", ignoreCase = true)) "SERIES" else "MOVIE",
                    detail.poster,
                    detail.year
                )
            } else {
                errorType = ErrorType.NOT_FOUND
                isLoading = false
            }
        } catch (e: Exception) {
            errorType = if (e is java.net.UnknownHostException || e is java.io.IOException) {
                ErrorType.NO_INTERNET
            } else {
                ErrorType.NOT_FOUND
            }
            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Loading state
        AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp)
                Text(
                    "Loading title…",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Error state
        AnimatedVisibility(visible = !isLoading && errorType != null, enter = fadeIn(), exit = fadeOut()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = if (errorType == ErrorType.NO_INTERNET) Icons.Filled.WifiOff else Icons.Filled.SearchOff,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = when (errorType) {
                        ErrorType.NO_INTERNET -> "No internet connection"
                        ErrorType.BAD_LINK    -> "Couldn't load this title"
                        else                  -> "Title not found"
                    },
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = when (errorType) {
                        ErrorType.NO_INTERNET -> "Check your connection and try again."
                        else                  -> "The link may be broken or the title isn't available."
                    },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                // Primary action: Search Instead
                Button(
                    onClick = onSearchInstead,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Background),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Search Instead", fontWeight = FontWeight.SemiBold)
                }
                // Secondary action: Go Back
                TextButton(onClick = onBack) {
                    Text("Go Back", color = TextSecondary)
                }
            }
        }
    }
}

private enum class ErrorType { NO_INTERNET, NOT_FOUND, BAD_LINK }
