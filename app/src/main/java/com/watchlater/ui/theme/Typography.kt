package com.watchlater.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 57.sp,
        lineHeight = 64.sp, letterSpacing = (-0.25).sp, color = TextPrimary
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 28.sp,
        lineHeight = 36.sp, letterSpacing = (-0.3).sp, color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
        lineHeight = 30.sp, letterSpacing = (-0.2).sp, color = TextPrimary
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
        lineHeight = 26.sp, color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
        lineHeight = 24.sp, letterSpacing = (-0.1).sp, color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium, fontSize = 15.sp,
        lineHeight = 22.sp, color = TextPrimary
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium, fontSize = 13.sp,
        lineHeight = 20.sp, color = TextSecondary
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal, fontSize = 16.sp,
        lineHeight = 24.sp, color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal, fontSize = 14.sp,
        lineHeight = 21.sp, color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal, fontSize = 12.sp,
        lineHeight = 18.sp, color = TextTertiary
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium, fontSize = 14.sp,
        lineHeight = 20.sp, letterSpacing = 0.1.sp, color = TextPrimary
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium, fontSize = 12.sp,
        lineHeight = 16.sp, letterSpacing = 0.5.sp, color = TextSecondary
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium, fontSize = 10.sp,
        lineHeight = 14.sp, letterSpacing = 0.5.sp, color = TextTertiary
    )
)
