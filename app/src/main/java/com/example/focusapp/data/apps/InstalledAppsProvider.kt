package com.example.focusapp.data.apps

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * InstalledAppInfo
 * -------------------
 * One entry in the "pick an app to restrict" list: enough to render a row
 * (icon + name) and enough to actually restrict it later ([packageName] is
 * the exact value FocusAccessibilityService compares incoming events
 * against).
 *
 * [icon] is a plain Android [Bitmap], not a Compose type - this file lives
 * in the Data layer, which should not depend on Compose. The UI layer is
 * responsible for converting it to a Compose `ImageBitmap` (via
 * `.asImageBitmap()`) only where it's actually drawn.
 */
data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Bitmap?
)

/**
 * getLaunchableApps
 * --------------------
 * Returns every app on the device that has a launcher icon (shows up on
 * the home screen / app drawer) - deliberately NOT every installed
 * package, since that raw list also includes background services and
 * system components that would make a confusing "pick an app to block" list.
 *
 * HOW THIS SEES OTHER APPS AT ALL:
 * On Android 11+ (API 30+), an app can't see the full list of what else
 * is installed by default ("package visibility", a privacy restriction
 * added in that release). Querying by an explicit intent filter
 * (ACTION_MAIN + CATEGORY_LAUNCHER) is exempt from that restriction, as
 * long as AndroidManifest.xml declares a matching `<queries>` element
 * (see the manifest's `<queries>` block) - this avoids needing the much
 * more sensitive, Play-Store-restricted QUERY_ALL_PACKAGES permission.
 *
 * This performs real PackageManager/binder I/O and can take a noticeable
 * moment with 100+ apps installed, so callers should run it off the main
 * thread (see AppsScreen.kt's `withContext(Dispatchers.Default) { ... }`).
 */
fun getLaunchableApps(context: Context): List<InstalledAppInfo> {
    val packageManager = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    // The single-argument overload used here is deprecated in favor of one
    // taking a PackageManager.ResolveInfoFlags object (added in API 33),
    // but the deprecated form still works correctly and is simpler to
    // support all the way back to this project's minSdk of 26 without an
    // API-level branch.
    @Suppress("DEPRECATION")
    val resolvedActivities = packageManager.queryIntentActivities(launcherIntent, 0)

    return resolvedActivities
        .map { resolveInfo ->
            InstalledAppInfo(
                packageName = resolveInfo.activityInfo.packageName,
                label = resolveInfo.loadLabel(packageManager).toString(),
                // Icon loading could theoretically fail for an unusual
                // package - never let one bad icon crash the whole list.
                icon = runCatching {
                    resolveInfo.loadIcon(packageManager).toBitmap()
                }.getOrNull()
            )
        }
        // A single app can register more than one launcher activity (rare,
        // but happens) - de-duplicate by package so it appears once.
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

/**
 * getAppLabel
 * -------------
 * Best-effort human-readable name for a single package (e.g. "Facebook"
 * instead of "com.facebook.katana"), used by the usage-duration panel to
 * show friendly names for apps it's tracking. Falls back to the raw
 * package name if the app can't be resolved (e.g. it was uninstalled
 * after being observed, or it's this app's own package on some devices
 * before PackageManager has indexed it).
 */
fun getAppLabel(context: Context, packageName: String): String {
    return runCatching {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    }.getOrDefault(packageName)
}

/**
 * getAppIcon
 * ------------
 * Best-effort real launcher icon for a single package, used to draw
 * actual icons for the packages already saved inside an [com.example.focusapp.domain.model.AppGroup]
 * (AppGroupRow, EditAppGroupScreen) - as opposed to [getLaunchableApps],
 * which loads icons for EVERY launchable app at once and would be
 * wasteful to call just to look up 2-3 already-known package names.
 * Returns null (rather than throwing) if the package can't be resolved,
 * e.g. it was uninstalled after being added to a group.
 */
fun getAppIcon(context: Context, packageName: String): Bitmap? {
    return runCatching {
        context.packageManager.getApplicationIcon(packageName).toBitmap()
    }.getOrNull()
}
