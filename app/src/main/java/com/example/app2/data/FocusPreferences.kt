package com.example.app2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_prefs")

class FocusPreferences(private val context: Context) {

    companion object {
        val TOTAL_FOCUS_HOURS = floatPreferencesKey("total_focus_hours")
        val UNLOCKED_REWARDS = stringSetPreferencesKey("unlocked_rewards")
        val SELECTED_SOUND_ID = stringSetPreferencesKey("selected_sound_id") // Should be string
        val SELECTED_THEME_ID = stringSetPreferencesKey("selected_theme_id")
    }

    // Fix: Selected IDs should be string keys, not sets, but DataStore preferences doesn't have a direct String key for singular without initial value? 
    // Actually it does: stringPreferencesKey
    
    private object Keys {
        val TOTAL_HOURS = floatPreferencesKey("total_hours")
        val UNLOCKED_IDS = stringSetPreferencesKey("unlocked_ids")
    }

    val totalFocusHours: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.TOTAL_HOURS] ?: 0f
    }

    val unlockedRewardIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.UNLOCKED_IDS] ?: emptySet()
    }

    suspend fun addFocusTime(hours: Float) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TOTAL_HOURS] ?: 0f
            prefs[Keys.TOTAL_HOURS] = current + hours
        }
    }

    suspend fun unlockReward(rewardId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.UNLOCKED_IDS] ?: emptySet()
            prefs[Keys.UNLOCKED_IDS] = current + rewardId
        }
    }
}