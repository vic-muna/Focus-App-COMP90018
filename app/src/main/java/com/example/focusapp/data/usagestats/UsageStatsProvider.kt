package com.example.focusapp.data.usagestats

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
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

    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
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
