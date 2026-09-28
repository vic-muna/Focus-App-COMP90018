package com.example.focusapp.data.preferences

import android.content.Context

/**
 * Remembers which background theme was picked (only its id; see ui.theme.BackgroundThemes).
 */
class BackgroundThemeStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Null until the user picks a theme for the first time. */
    fun getSelectedId(): String? = prefs.getString(KEY_SELECTED_ID, null)

    fun saveSelectedId(id: String) {
        prefs.edit().putString(KEY_SELECTED_ID, id).apply()
    }

    private companion object {
        const val PREFS_NAME = "focus_background_theme"
        const val KEY_SELECTED_ID = "selected_id"
    }
}