package com.kaze.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Centralized haptic feedback utility.
 * Uses Compose's LocalHapticFeedback — no Vibrator API, no permissions, no battery overhead.
 *
 * Usage:
 *   val haptic = rememberHapticManager()
 *   haptic.confirm()  // episode marked, item saved, season marked
 *   haptic.light()    // nav tap, card interaction
 *   haptic.error()    // validation blocked, delete action
 */
class HapticManager(private val haptic: HapticFeedback) {

    /** Use for: episode toggle ✓, item saved ✓, mark season watched ✓ */
    fun confirm() = haptic.performHapticFeedback(HapticFeedbackType.LongPress)

    /** Use for: bottom nav taps, card long-press, bottom sheet open */
    fun light() = haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

    /** Use for: validation blocked toast, delete confirmed */
    fun error() = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberHapticManager(): HapticManager {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) { HapticManager(hapticFeedback) }
}
