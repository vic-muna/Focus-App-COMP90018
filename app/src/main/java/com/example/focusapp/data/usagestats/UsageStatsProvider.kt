package com.example.focusapp.data.usagestats

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.util.Calendar

/**
 * Whether the user allowed "Usage access" for this app in Android's settings.
 * There is no pop-up for this permission, so we can only ask the system.
 */
@Suppress("DEPRECATION") // The newer call needs Android 10+, but we support Android 8+.
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
 * For each app used between [windowStartMillis] and [windowEndMillis] (today):
 * how many times it was opened and how long it was on screen.
 * Time Focus's daily limits are checked against these numbers.
 *
 * It reads Android's usage event log from midnight:
 *  - An "open" = the app comes to the front after a DIFFERENT app (or after the
 *    screen was off). Moving between screens inside one app is not a new open.
 *  - An app already on screen when the window starts counts as 1 open.
 *
 * Returns an empty map without usage access (see [hasUsageAccessPermission]).
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

/** "1h 23m" / "45m 10s" / "12s". */
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

/** Today's midnight, in epoch milliseconds. */
private fun startOfTodayMillis(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
