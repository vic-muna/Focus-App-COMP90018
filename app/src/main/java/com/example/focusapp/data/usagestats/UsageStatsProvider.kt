package com.example.focusapp.data.usagestats

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.util.Calendar

/**
 * hasUsageAccessPermission
 * ---------------------------
 * UsageStatsManager access is gated by the special "Usage access" toggle
 * in system Settings (opened via Settings.ACTION_USAGE_ACCESS_SETTINGS) -
 * conceptually similar to AccessibilityService's own toggle, but a
 * completely separate permission with its own settings screen and its own
 * check API. There is no runtime permission dialog for it; the only way
 * to find out if it's granted is to ask [AppOpsManager] directly.
 */
@Suppress("DEPRECATION") // checkOpNoThrow works down to API 19; the newer
// unsafeCheckOpNoThrow needs API 29+, which would be higher than this
// project's minSdk of 26 without an extra API-level branch.
fun hasUsageAccessPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

/**
 * queryUsageDurationsToday
 * ---------------------------
 * Returns, for every app used at all today (since local midnight), how
 * many milliseconds it has spent in the foreground - a real historical
 * total tracked by the OS itself, independent of whether Focus happens to
 * be running. This is what replaced this project's earlier approach of
 * hand-rolling a stopwatch off FocusAccessibilityService's foreground
 * events: that version reset every time Focus's process died and only
 * counted time since Focus was opened, which is a less useful number than
 * "how long have I used Facebook today" - the number this function
 * actually answers.
 *
 * WHY `queryUsageStats`, NOT `queryEvents`: an earlier version of this
 * file used `UsageStatsManager.queryEvents()` to get a raw log of
 * MOVE_TO_FOREGROUND/MOVE_TO_BACKGROUND transitions and would have had to
 * manually pair them up into durations - exactly the same "stopwatch"
 * bookkeeping this rewrite was meant to get rid of.
 * `queryUsageStats(INTERVAL_DAILY, ...)` does that pairing internally and
 * hands back a ready-to-use total per package, including whatever time
 * has accumulated in the CURRENTLY ongoing session up to the moment this
 * function is called - which is what makes repeatedly polling this (see
 * AppsScreen's usage panel) feel like a live-updating counter, with no
 * manual "is this the current app, add elapsed time" logic needed here.
 *
 * @return empty map if usage access hasn't been granted - callers should
 *         check [hasUsageAccessPermission] to distinguish "no access" from
 *         "access granted, but nothing used yet".
 */
fun queryUsageDurationsToday(context: Context): Map<String, Long> {
    if (!hasUsageAccessPermission(context)) return emptyMap()

    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val startOfToday = startOfTodayMillis()
    val now = System.currentTimeMillis()

    val statsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfToday, now)

    // A single package can appear more than once in the returned list if
    // the query range crosses an internal stats "bucket" boundary, so sum
    // by package rather than assuming one entry per app.
    return statsList
        .filter { it.totalTimeInForeground > 0 }
        .groupBy { it.packageName }
        .mapValues { (_, statsForPackage) -> statsForPackage.sumOf { it.totalTimeInForeground } }
}

/**
 * One app's usage inside a time window - see [queryAppUsageInWindow].
 * [isInForeground] is true if the app is on screen at the window's end
 * (only meaningful when the window ends "now").
 */
data class AppWindowUsage(
    val openCount: Int,
    val foregroundMillis: Long,
    val isInForeground: Boolean
)

/**
 * queryAppUsageInWindow
 * ---------------------------
 * [David Shiau, 2026-09-26] For every app used inside [windowStartMillis,
 * windowEndMillis] (both today), how many times it was opened and how long
 * it was on screen - only counting the part inside the window. This is what
 * the Scheduled Limits (daily open times / duration) are checked against.
 *
 * Both numbers come from walking the raw `queryEvents` log, not from
 * `queryUsageStats` (like [queryUsageDurationsToday] does): that API only
 * has whole-day buckets, which can't be cut down to e.g. 16:00-18:00, and
 * has no public per-app launch count at all (`appLaunchCount` is hidden).
 *
 * What counts as one "open": the package coming to the foreground when the
 * previously-foregrounded package was a DIFFERENT app (or, on API 28+, the
 * screen was turned off in between). Moving between two activities inside
 * the same app fires a fresh foreground event each time - without the
 * "different from the last one" check, those internal screen changes
 * would each be miscounted as a separate open. An app that is already on
 * screen when the window starts counts as 1 open, and only its time from
 * the window start onwards counts.
 *
 * The log is read from local midnight (not from the window start), so the
 * state at the window start - which app was already open - is known.
 *
 * @return empty map if usage access hasn't been granted - callers should
 *         check [hasUsageAccessPermission] to tell the two cases apart.
 */
fun queryAppUsageInWindow(
    context: Context,
    windowStartMillis: Long,
    windowEndMillis: Long
): Map<String, AppWindowUsage> {
    if (!hasUsageAccessPermission(context) || windowEndMillis <= windowStartMillis) return emptyMap()

    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val events = usageStatsManager.queryEvents(
        minOf(startOfTodayMillis(), windowStartMillis),
        minOf(windowEndMillis, System.currentTimeMillis())
    )

    val openCounts = mutableMapOf<String, Int>()
    val foregroundMillis = mutableMapOf<String, Long>()
    // Package -> when its current on-screen stretch started.
    val openSince = mutableMapOf<String, Long>()
    var lastOpenedPackage: String? = null
    var hasCrossedWindowStart = false

    fun addOpen(packageName: String) {
        openCounts[packageName] = (openCounts[packageName] ?: 0) + 1
    }

    fun closeStretch(packageName: String, endMillis: Long) {
        val start = openSince.remove(packageName) ?: return
        val clipped = minOf(endMillis, windowEndMillis) - maxOf(start, windowStartMillis)
        if (clipped > 0) foregroundMillis[packageName] = (foregroundMillis[packageName] ?: 0L) + clipped
    }

    // Whatever is on screen at the moment the window opens counts as 1 open.
    fun crossWindowStartIfNeeded(timestamp: Long) {
        if (hasCrossedWindowStart || timestamp < windowStartMillis) return
        hasCrossedWindowStart = true
        openSince.keys.forEach(::addOpen)
    }

    val event = UsageEvents.Event()
    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val timestamp = event.timeStamp
        crossWindowStartIfNeeded(timestamp)
        val packageName = event.packageName

        @Suppress("DEPRECATION") // MOVE_TO_FOREGROUND/BACKGROUND == ACTIVITY_RESUMED/PAUSED (API 29+ names), same values.
        when {
            event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                // Another app coming to the front means anything else still
                // marked "open" (e.g. a missing pause event) isn't any more.
                openSince.keys.filter { it != packageName }.forEach { closeStretch(it, timestamp) }
                if (packageName != lastOpenedPackage) {
                    if (timestamp >= windowStartMillis) addOpen(packageName)
                    lastOpenedPackage = packageName
                }
                openSince.putIfAbsent(packageName, timestamp)
            }
            event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND ->
                closeStretch(packageName, timestamp)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                openSince.keys.toList().forEach { closeStretch(it, timestamp) }
                lastOpenedPackage = null
            }
        }
    }

    val windowEnd = minOf(windowEndMillis, System.currentTimeMillis())
    crossWindowStartIfNeeded(windowEnd)
    val stillOpen = openSince.keys.toSet()
    stillOpen.forEach { closeStretch(it, windowEnd) }

    return (openCounts.keys + foregroundMillis.keys).associateWith { packageName ->
        AppWindowUsage(
            openCount = openCounts[packageName] ?: 0,
            foregroundMillis = foregroundMillis[packageName] ?: 0L,
            isInForeground = packageName in stillOpen
        )
    }
}

/** Turns raw milliseconds into a short "1h 23m" / "45m 10s" / "12s" style string. */
fun formatUsageDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

/** Local midnight today, as epoch millis - the shared "today" window for both queries above. */
private fun startOfTodayMillis(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
