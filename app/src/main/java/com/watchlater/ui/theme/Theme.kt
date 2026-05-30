package com.watchlater.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary              = AccentBlue,
    onPrimary            = Color.White,
    primaryContainer     = AccentBlueDim,
    onPrimaryContainer   = Color(0xFFCDD5FF),
    secondary            = AccentPurple,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFF3D2A6E),
    onSecondaryContainer = Color(0xFFD9BBFF),
    tertiary             = SemanticSuccess,
    onTertiary           = Color(0xFF003828),
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
    error                = SemanticError,
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
    inverseSurface       = TextPrimary,
    inverseOnSurface     = Background,
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
