package com.example.app2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_prefs")

class FocusPreferences(private val context: Context) {

    private object Keys {
        val TOTAL_HOURS = floatPreferencesKey("total_hours")
        val FOCUS_POINTS = intPreferencesKey("focus_points")
        val UNLOCKED_IDS = stringSetPreferencesKey("unlocked_ids")
        val SELECTED_BACKGROUND_ID = stringPreferencesKey("selected_background_id")
    }

    val totalFocusHours: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.TOTAL_HOURS] ?: 0f
    }

    val focusPoints: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.FOCUS_POINTS] ?: 0
    }

    val unlockedRewardIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.UNLOCKED_IDS] ?: setOf("bg_forest")
    }

    val selectedBackgroundId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_BACKGROUND_ID] ?: "bg_forest"
    }

    suspend fun addFocusTime(hours: Float) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TOTAL_HOURS] ?: 0f
            prefs[Keys.TOTAL_HOURS] = current + hours
        }
    }

    suspend fun addPoints(points: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FOCUS_POINTS] ?: 0
            prefs[Keys.FOCUS_POINTS] = current + points
        }
    }

    suspend fun unlockReward(rewardId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.UNLOCKED_IDS] ?: setOf("bg_forest")
            prefs[Keys.UNLOCKED_IDS] = current + rewardId
        }
    }

    suspend fun spendPointsAndUnlock(pointsCost: Int, rewardId: String): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val currentPoints = prefs[Keys.FOCUS_POINTS] ?: 0
            if (currentPoints >= pointsCost) {
                prefs[Keys.FOCUS_POINTS] = currentPoints - pointsCost
                val currentUnlocked = prefs[Keys.UNLOCKED_IDS] ?: setOf("bg_forest")
                prefs[Keys.UNLOCKED_IDS] = currentUnlocked + rewardId
                success = true
            }
        }
        return success
    }

    suspend fun setSelectedBackground(backgroundId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_BACKGROUND_ID] = backgroundId
        }
    }
}