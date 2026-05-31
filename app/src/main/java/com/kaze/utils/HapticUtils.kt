package com.kaze.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.media.AudioManager
import android.media.SoundPool

/**
 * Global haptic feedback and UI sound utility.
 * All calls respect the user's settings in UserPreferences.
 */
object HapticUtils {

    fun tick(context: Context) {
        val prefs = UserPreferences(context)
        if (!prefs.hapticEnabled) return
        vibrate(context, 18)
    }

    fun success(context: Context) {
        val prefs = UserPreferences(context)
        if (!prefs.hapticEnabled) return
        vibrate(context, 40)
    }

    fun error(context: Context) {
        val prefs = UserPreferences(context)
        if (!prefs.hapticEnabled) return
        // Double pulse for errors
        vibrate(context, 60)
    }

    private fun vibrate(context: Context, durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = manager.defaultVibrator
                vibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            // Silently fail — vibration is non-critical
        }
    }
}
