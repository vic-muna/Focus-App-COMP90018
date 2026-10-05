package com.example.focusapp.data.accessibility

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.focusapp.BlockedActivity
import com.example.focusapp.data.blocking.BlockedAppGroup
import com.example.focusapp.data.blocking.BlockedAppGroupStorage
import com.example.focusapp.data.blocking.formatClockTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "AppOpenAllowanceManager"
private const val PREFS_NAME = "focus_app_open_allowances"
private const val KEY_DATE = "allowance_date"

/**
 * Manages daily open limits and temporary unlock timers for restricted apps.
 *
 * When an app with a daily open limit is blocked, the user can consume 1 open
 * from the interruption screen ([BlockedActivity]) to unlock the app for the group's
 * set duration ([BlockedAppGroup.maxMinutesPerApp]). When the timer expires, 1 open is consumed,
 * and [BlockedActivity] immediately appears on top of the screen to halt app usage.
 */
object AppOpenAllowanceManager {

    private val activeUnlocks = ConcurrentHashMap<String, Long>()
    private val unlockHandlers = ConcurrentHashMap<String, Handler>()
    private val unlockRunnables = ConcurrentHashMap<String, Runnable>()

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun getPrefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Finds the group containing [packageName] across Time Focus, Location, or Wi-Fi groups.
     */
    fun findGroupForPackage(context: Context, packageName: String): BlockedAppGroup? {
        val timeFocusGroups = BlockedAppGroupStorage(context).getGroupsWithoutIcons().orEmpty()
        val locationGroups = BlockedAppGroupStorage.forLocationGroups(context).getGroupsWithoutIcons().orEmpty()
        val wifiGroups = BlockedAppGroupStorage.forWifiNetworks(context).getGroupsWithoutIcons().orEmpty()

        val allGroups = timeFocusGroups + locationGroups + wifiGroups
        return allGroups.find { group ->
            group.enabled && group.apps.any { it.packageName == packageName }
        }
    }

    /**
     * How many opens are left today for [packageName].
     * Returns null if no `maxOpensPerApp` limit is configured for [packageName].
     */
    fun getOpensRemainingToday(context: Context, packageName: String): Int? {
        val group = findGroupForPackage(context, packageName) ?: return null
        val maxOpens = group.maxOpensPerApp ?: return null
        val used = getUsedOpensToday(context, packageName)
        return (maxOpens - used).coerceAtLeast(0)
    }

    /**
     * Unlocked duration in minutes for [packageName].
     * Defaults to 5 minutes if the group doesn't specify `maxMinutesPerApp`.
     */
    fun getUnlockMinutesForPackage(context: Context, packageName: String): Int {
        val group = findGroupForPackage(context, packageName)
        return group?.maxMinutesPerApp ?: 5
    }

    /** How many opens have been consumed today for [packageName]. */
    fun getUsedOpensToday(context: Context, packageName: String): Int {
        val prefs = getPrefs(context)
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_DATE, null)
        if (savedDate != today) {
            // Reset for a new day
            prefs.edit().clear().putString(KEY_DATE, today).apply()
            return 0
        }
        return prefs.getInt("used_$packageName", 0)
    }

    /** Decreases the remaining opens by 1 (increments used opens today). */
    fun consumeOpen(context: Context, packageName: String) {
        val prefs = getPrefs(context)
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_DATE, null)
        val currentUsed = if (savedDate == today) prefs.getInt("used_$packageName", 0) else 0
        prefs.edit()
            .putString(KEY_DATE, today)
            .putInt("used_$packageName", currentUsed + 1)
            .apply()
        Log.d(TAG, "Consumed 1 open for $packageName. Total used today: ${currentUsed + 1}")
    }

    /** Returns true if [packageName] is currently in an active temporary unlock period. */
    fun isTemporarilyUnlocked(packageName: String): Boolean {
        val expiry = activeUnlocks[packageName] ?: return false
        return if (System.currentTimeMillis() < expiry) {
            true
        } else {
            activeUnlocks.remove(packageName)
            false
        }
    }

    /**
     * Starts a temporary unlock for [packageName] lasting [durationMinutes] minutes.
     * When the duration ends, 1 open is consumed and [BlockedActivity] is re-launched.
     */
    fun startTemporaryUnlock(context: Context, packageName: String, durationMinutes: Int) {
        val durationMillis = durationMinutes * 60_000L
        val expiry = System.currentTimeMillis() + durationMillis
        activeUnlocks[packageName] = expiry

        // Cancel previous pending timer for this package if any
        unlockHandlers[packageName]?.removeCallbacks(unlockRunnables[packageName] ?: Runnable {})

        val appContext = context.applicationContext
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            Log.d(TAG, "Unlock period of $durationMinutes min ended for $packageName!")
            activeUnlocks.remove(packageName)
            unlockHandlers.remove(packageName)
            unlockRunnables.remove(packageName)

            // 1. Consume 1 open when time limit ends
            consumeOpen(appContext, packageName)

            // 2. Launch interruption screen immediately on top of the app
            val group = findGroupForPackage(appContext, packageName)
            val reason = if (group != null) {
                val groupName = group.name.ifBlank { "Focus Schedule" }
                val timeRangeStr = group.schedule.timeRanges.joinToString(", ") { range ->
                    "${formatClockTime(range.start)} to ${formatClockTime(range.end)}"
                }
                "Blocked based on the schedule $groupName from $timeRangeStr"
            } else {
                "Your time limit ended."
            }

            val intent = Intent(appContext, BlockedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(BlockedActivity.EXTRA_BLOCKED_PACKAGE, packageName)
                putExtra(BlockedActivity.EXTRA_BLOCK_REASON, reason)
            }
            appContext.startActivity(intent)
        }

        unlockHandlers[packageName] = handler
        unlockRunnables[packageName] = runnable
        handler.postDelayed(runnable, durationMillis)

        Log.d(TAG, "Started $durationMinutes min temporary unlock for $packageName")
    }
}
