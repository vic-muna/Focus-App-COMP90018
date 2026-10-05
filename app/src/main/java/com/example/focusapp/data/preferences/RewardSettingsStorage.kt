package com.example.focusapp.data.preferences

import android.content.Context
import com.example.focusapp.domain.model.RewardRules

/**
 * Remembers what was bought in the Rewards shop and the points it cost.
 * Points themselves aren't stored - they're worked out from the focus sessions
 * (see CalculateFocusRewardUseCase), minus [getSpentPoints].
 */
class RewardSettingsStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The daily focus time a day needs to count for the streak. */
    fun getStreakGoalMinutes(): Int =
        prefs.getInt(KEY_STREAK_GOAL_MINUTES, RewardRules.DEFAULT_STREAK_GOAL_MINUTES)

    /** Points already spent in the shop. */
    fun getSpentPoints(): Long = prefs.getLong(KEY_SPENT_POINTS, 0L)

    /** Ids of the backgrounds and music tracks bought in the shop. */
    fun getOwnedIds(): Set<String> = prefs.getStringSet(KEY_OWNED_IDS, emptySet()).orEmpty()

    /** Records [id] as bought for [pricePoints]. Checking the user can afford it is up to the caller. */
    fun buy(id: String, pricePoints: Int) {
        prefs.edit()
            .putStringSet(KEY_OWNED_IDS, getOwnedIds() + id)
            .putLong(KEY_SPENT_POINTS, getSpentPoints() + pricePoints)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "focus_rewards"
        const val KEY_STREAK_GOAL_MINUTES = "streak_goal_minutes"
        const val KEY_SPENT_POINTS = "spent_points"
        const val KEY_OWNED_IDS = "owned_ids"
    }
}
