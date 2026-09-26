package com.example.focusapp.domain.usecase

import com.example.focusapp.data.usagestats.AppWindowUsage
import com.example.focusapp.ui.screens.home.BlockedAppGroup
import com.example.focusapp.ui.screens.home.formatLimitMinutes
import com.example.focusapp.ui.screens.home.formatOpenTimes
import com.example.focusapp.ui.screens.home.windowOn
import java.util.Calendar

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
 * EvaluateUsageLimitUseCase
 * ----------------------------
 * [David Shiau, 2026-09-26] Phase 3 of the daily open-times/duration limit:
 * decides which on-screen apps must be blocked right now. For every group
 * with a limit set whose schedule window is open, each of its apps is
 * checked ON ITS OWN (Facebook gets 3 opens, Instagram gets 3 opens) using
 * only its usage INSIDE the window:
 *  - Max Open Times N: opens 1..N are allowed; the (N+1)th is blocked.
 *  - Max Duration M: blocked once M minutes have been used.
 * Only apps currently on screen are returned - an app that's over its limit
 * but not open has nothing to block until it's opened again.
 *
 * Pure logic, no Context: usage comes from [usageInWindow], so this is unit
 * testable and FocusAccessibilityService supplies the real UsageStats query.
 */
class EvaluateUsageLimitUseCase {

    fun execute(
        groups: List<BlockedAppGroup>,
        usageInWindow: (startMillis: Long, endMillis: Long) -> Map<String, AppWindowUsage>,
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
            val maxOpens = group.maxOpensPerApp
            val maxMillis = group.maxMinutesPerApp?.let { it * 60_000L }
            if (maxOpens == null && maxMillis == null) continue

            val window = group.schedule.windowOn(now) ?: continue
            if (nowMillis < window.startMillis) {
                checkWithin(window.startMillis - nowMillis)
                continue
            }
            if (nowMillis >= window.endMillis) continue

            val usage = usageCache.getOrPut(window.startMillis) { usageInWindow(window.startMillis, nowMillis) }
            for (app in group.apps) {
                val appUsage = usage[app.packageName] ?: continue
                if (!appUsage.isInForeground || app.packageName in violations) continue

                val reason = when {
                    maxOpens != null && appUsage.openCount > maxOpens ->
                        "You've reached your limit of ${formatOpenTimes(maxOpens)} " +
                            "during ${group.name}'s scheduled time today."
                    maxMillis != null && appUsage.foregroundMillis >= maxMillis ->
                        "You've used your ${formatLimitMinutes(group.maxMinutesPerApp!!)} " +
                            "during ${group.name}'s scheduled time today."
                    else -> null
                }
                if (reason != null) {
                    violations[app.packageName] = UsageLimitViolation(app.packageName, reason)
                } else if (maxMillis != null) {
                    // Re-check exactly when the time runs out - capped so an
                    // edge case (e.g. a missed app-switch event) is caught soon.
                    checkWithin(minOf(maxMillis - appUsage.foregroundMillis, MAX_CHECK_DELAY_MILLIS))
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
