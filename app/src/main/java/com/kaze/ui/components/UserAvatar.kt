package com.kaze.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimal black-and-white avatar showing the first letter of the username.
 * Professional, clean, no colour accents.
 */
@Composable
fun UserAvatar(
    username: String,
    size: Dp = 48.dp,
    fontSize: TextUnit = 18.sp,
    modifier: Modifier = Modifier
) {
    val initial = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF2A2A2A)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color(0xFFE8E8E8),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold
        )
    }
}
