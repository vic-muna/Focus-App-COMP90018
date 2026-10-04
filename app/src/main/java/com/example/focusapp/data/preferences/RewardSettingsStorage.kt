package com.example.focusapp.data.preferences

import android.content.Context
import com.example.focusapp.domain.model.RewardRules

/**
 * Remembers the daily focus time a day needs to count for the streak (changed in Settings).
 * The default and the allowed range are in [RewardRules].
 */
class RewardSettingsStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStreakGoalMinutes(): Int =
        prefs.getInt(KEY_STREAK_GOAL_MINUTES, RewardRules.DEFAULT_STREAK_GOAL_MINUTES)

    fun saveStreakGoalMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_STREAK_GOAL_MINUTES, minutes).apply()
    }

    private companion object {
        const val PREFS_NAME = "focus_rewards"
        const val KEY_STREAK_GOAL_MINUTES = "streak_goal_minutes"
    }
}
