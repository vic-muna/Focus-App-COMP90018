package com.example.focusapp.data.apps

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap

/** One installed app: its package name (what the blocker checks), its name and its icon. */
data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Bitmap?
)

/**
 * Every app that has a home-screen icon, sorted by name.
 * (The manifest's `<queries>` block lets us see these apps on Android 11+.)
 * This is slow with many apps, so call it off the main thread.
 */
fun getLaunchableApps(context: Context): List<InstalledAppInfo> {
    val packageManager = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    @Suppress("DEPRECATION") // The newer version needs Android 13+.
    val resolvedActivities = packageManager.queryIntentActivities(launcherIntent, 0)

    return resolvedActivities
        .map { resolveInfo ->
            InstalledAppInfo(
                packageName = resolveInfo.activityInfo.packageName,
                label = resolveInfo.loadLabel(packageManager).toString(),
                icon = runCatching { resolveInfo.loadIcon(packageManager).toBitmap() }.getOrNull()
            )
        }
        .distinctBy { it.packageName } // An app can have more than one launcher icon.
        .sortedBy { it.label.lowercase() }
}

/** An app's name (e.g. "Facebook"), or the package name if it can't be found. */
fun getAppLabel(context: Context, packageName: String): String {
    return runCatching {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    }.getOrDefault(packageName)
}

/** An app's icon, or null if it can't be found (e.g. it was uninstalled). */
fun getAppIcon(context: Context, packageName: String): Bitmap? {
    return runCatching {
        context.packageManager.getApplicationIcon(packageName).toBitmap()
    }.getOrNull()
}
