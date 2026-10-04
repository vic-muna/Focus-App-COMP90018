package com.example.focusapp.data.preferences

import android.content.Context

/**
 * Remembers the Focus Mode noise alert settings (changed in Settings): whether
 * it's on, and how loud the room may get before the "too loud" banner shows.
 */
class NoiseAlertStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)

    fun saveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getThresholdDb(): Int = prefs.getInt(KEY_THRESHOLD_DB, DEFAULT_THRESHOLD_DB)

    fun saveThresholdDb(thresholdDb: Int) {
        prefs.edit().putInt(KEY_THRESHOLD_DB, thresholdDb).apply()
    }

    companion object {
        /** The threshold goes up and down in these steps, between [MIN_THRESHOLD_DB] and [MAX_THRESHOLD_DB]. */
        const val THRESHOLD_STEP_DB = 5
        const val MIN_THRESHOLD_DB = 40
        const val MAX_THRESHOLD_DB = 85
        const val DEFAULT_THRESHOLD_DB = 55

        private const val PREFS_NAME = "focus_noise_alert"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_THRESHOLD_DB = "threshold_db"
    }
}
