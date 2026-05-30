package com.watchlater.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFFFFFFFF),   // White — primary actions
    onPrimary            = Color(0xFF000000),   // Black on white
    primaryContainer     = Color(0xFF1E1E1E),
    onPrimaryContainer   = Color(0xFFFFFFFF),
    secondary            = Color(0xFFA0A0A0),
    onSecondary          = Color(0xFF000000),
    secondaryContainer   = Color(0xFF2A2A2A),
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary             = Color(0xFFCCCCCC),
    onTertiary           = Color(0xFF000000),
    background           = Background,
    onBackground         = TextPrimary,
    surface              = SurfaceContainer,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceElevated,
    onSurfaceVariant     = TextSecondary,
    surfaceContainer     = SurfaceContainer,
    surfaceContainerHigh = SurfaceElevated,
    surfaceContainerLow  = Background,
    outline              = SurfaceBorder,
    outlineVariant       = SurfaceHighlight,
    error                = Color(0xFFE0E0E0),
    onError              = Color(0xFF000000),
    errorContainer       = Color(0xFF2A2A2A),
    onErrorContainer     = Color(0xFFFFFFFF),
    inverseSurface       = Color(0xFFFFFFFF),
    inverseOnSurface     = Color(0xFF000000),
    scrim                = Color(0x99000000)
)

@Composable
fun WatchLaterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AppTypography,
        shapes      = Shapes,
        content     = content
    )
}
