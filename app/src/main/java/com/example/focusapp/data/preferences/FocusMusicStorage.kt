package com.example.focusapp.data.preferences

import android.content.Context

/**
 * Remembers whether music plays in Focus Mode, and which track (only its id; see FocusMusics),
 * and whether Background Music (res/raw/homemusic) plays on Home and its tabs, Settings etc.
 */
class FocusMusicStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun saveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isBackgroundEnabled(): Boolean = prefs.getBoolean(KEY_BACKGROUND_ENABLED, false)

    fun saveBackgroundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_ENABLED, enabled).apply()
    }

    /** Null until the user picks a track. */
    fun getSelectedId(): String? = prefs.getString(KEY_SELECTED_ID, null)

    fun saveSelectedId(id: String) {
        prefs.edit().putString(KEY_SELECTED_ID, id).apply()
    }

    private companion object {
        const val PREFS_NAME = "focus_music"
        const val KEY_ENABLED = "enabled"
        const val KEY_SELECTED_ID = "selected_id"
        const val KEY_BACKGROUND_ENABLED = "background_enabled"
    }
}
