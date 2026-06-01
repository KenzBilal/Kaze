package com.kaze.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight SharedPreferences-based settings store for user preferences.
 * Stores: haptic feedback enabled, sound effects enabled, etc.
 */
class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("kaze_user_prefs", Context.MODE_PRIVATE)

    var hapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var sortOption: String
        get() = prefs.getString(KEY_SORT, "DEFAULT") ?: "DEFAULT"
        set(value) = prefs.edit().putString(KEY_SORT, value).apply()

    var filterOption: String
        get() = prefs.getString(KEY_FILTER, "ALL") ?: "ALL"
        set(value) = prefs.edit().putString(KEY_FILTER, value).apply()

    companion object {
        private const val KEY_HAPTIC = "haptic_enabled"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_SORT = "sort_option"
        private const val KEY_FILTER = "filter_option"
    }
}
