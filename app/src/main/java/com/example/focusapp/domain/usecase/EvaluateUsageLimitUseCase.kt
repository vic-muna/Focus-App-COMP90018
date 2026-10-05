package com.example.focusapp.domain.usecase

import com.example.focusapp.data.usagestats.AppWindowUsage
import java.util.Calendar
import com.example.focusapp.data.blocking.BlockedAppGroup
import com.example.focusapp.data.blocking.formatClockTime
import com.example.focusapp.data.blocking.windowOn

/** One app that is on screen right now and over one of its group's limits. */
data class UsageLimitViolation(
    val packageName: String,
    /** Human-readable "why", shown on the blocked screen. */
    val reason: String
)

data class UsageLimitCheckResult(
    val violations: List<UsageLimitViolation>,
    /**
     * How long until the next check is worth running (an on-screen app's
     * time limit running out, or a limited group's window opening), or
     * null if nothing is pending - the next app switch triggers one anyway.
     */
    val nextCheckInMillis: Long?
)

/**
 * Decides which open apps must be blocked for going over a daily limit.
 * Each app in a group is checked on its own, only counting use inside today's scheduled time:
 *  - Max Open Times N: opens 1..N are fine, open N+1 is blocked.
 *  - Max Minutes M: blocked after M minutes.
 * Usage is passed in ([usageInWindow]), so this can be unit tested.
 */
class EvaluateUsageLimitUseCase {

    fun execute(
        groups: List<BlockedAppGroup>,
        usageInWindow: (startMillis: Long, endMillis: Long) -> Map<String, AppWindowUsage>,
        getUsedOpensToday: (packageName: String) -> Int = { 0 },
        now: Calendar = Calendar.getInstance()
    ): UsageLimitCheckResult {
        val nowMillis = now.timeInMillis
        val violations = mutableMapOf<String, UsageLimitViolation>()
        var nextCheck: Long? = null
        fun checkWithin(millis: Long) {
            nextCheck = minOf(nextCheck ?: Long.MAX_VALUE, millis.coerceAtLeast(MIN_CHECK_DELAY_MILLIS))
        }

        // Several groups can share the same window - query each window once.
        val usageCache = mutableMapOf<Long, Map<String, AppWindowUsage>>()

        for (group in groups) {
            if (!group.enabled) continue
            val maxOpens = group.maxOpensPerApp
            val maxMillis = group.maxMinutesPerApp?.let { it * 60_000L }

            val windows = group.schedule.timeRanges.mapNotNull { it.windowOn(now, group.schedule.activeDays) }
            for (window in windows) {
                if (nowMillis < window.startMillis) {
                    checkWithin(window.startMillis - nowMillis)
                    continue
                }
                if (nowMillis >= window.endMillis) continue

                val usage = usageCache.getOrPut(window.startMillis) { usageInWindow(window.startMillis, nowMillis) }
                for (app in group.apps) {
                    val appUsage = usage[app.packageName]
                        ?: if (usage.isEmpty()) AppWindowUsage(openCount = 1, foregroundMillis = 0L, isInForeground = true)
                        else continue
                    if (!appUsage.isInForeground || app.packageName in violations) continue

                    val groupName = group.name.ifBlank { "Focus Schedule" }
                    val timeRangeStr = group.schedule.timeRanges.joinToString(", ") { range ->
                        "${formatClockTime(range.start)} to ${formatClockTime(range.end)}"
                    }
                    val scheduleReason = "Blocked based on the schedule $groupName from $timeRangeStr"

                    val usedOpens = getUsedOpensToday(app.packageName)
                    val isNoLimitsConfigured = maxOpens == null && maxMillis == null
                    val isOverOpens = maxOpens != null && (usedOpens >= maxOpens || appUsage.openCount > maxOpens)
                    val isOverMinutes = maxMillis != null && appUsage.foregroundMillis >= maxMillis

                    val reason = when {
                        isNoLimitsConfigured -> scheduleReason
                        isOverOpens -> scheduleReason
                        isOverMinutes -> scheduleReason
                        else -> null
                    }
                    if (reason != null) {
                        violations[app.packageName] = UsageLimitViolation(app.packageName, reason)
                    } else if (maxMillis != null) {
                        checkWithin(minOf(maxMillis - appUsage.foregroundMillis, MAX_CHECK_DELAY_MILLIS))
                    }
                }
            }
        }
        return UsageLimitCheckResult(violations.values.toList(), nextCheck)
    }

    private companion object {
        const val MIN_CHECK_DELAY_MILLIS = 1_000L
        const val MAX_CHECK_DELAY_MILLIS = 60_000L
    }
}
