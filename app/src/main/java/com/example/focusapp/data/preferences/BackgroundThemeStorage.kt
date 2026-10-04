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

    /** Themes whose "unlocked" dialog was already shown, so it only shows once. */
    fun getAnnouncedIds(): Set<String> = prefs.getStringSet(KEY_ANNOUNCED_IDS, emptySet()).orEmpty()

    fun saveAnnounced(id: String) {
        prefs.edit().putStringSet(KEY_ANNOUNCED_IDS, getAnnouncedIds() + id).apply()
    }

    /** Themes unlocked with the test code instead of a streak. */
    fun getCodeUnlockedIds(): Set<String> = prefs.getStringSet(KEY_CODE_UNLOCKED_IDS, emptySet()).orEmpty()

    fun saveCodeUnlocked(id: String) {
        prefs.edit().putStringSet(KEY_CODE_UNLOCKED_IDS, getCodeUnlockedIds() + id).apply()
    }

    private companion object {
        const val PREFS_NAME = "focus_background_theme"
        const val KEY_SELECTED_ID = "selected_id"
        const val KEY_ANNOUNCED_IDS = "announced_ids"
        const val KEY_CODE_UNLOCKED_IDS = "code_unlocked_ids"
    }
}