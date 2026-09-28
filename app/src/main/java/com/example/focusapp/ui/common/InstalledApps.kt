package com.example.focusapp.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.focusapp.data.apps.InstalledAppInfo
import com.example.focusapp.data.apps.getLaunchableApps
import com.example.focusapp.data.blocking.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The phone's installed apps, or null while they are still loading.
 * They are loaded once, the first time [shouldLoad] is true (it's slow with many apps).
 */
@Composable
fun rememberInstalledApps(shouldLoad: Boolean): List<InstalledAppInfo>? {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledAppInfo>?>(null) }
    LaunchedEffect(shouldLoad) {
        if (shouldLoad && apps == null) {
            apps = withContext(Dispatchers.Default) { getLaunchableApps(context) }
        }
    }
    return apps
}

/** Adds [item] if it isn't in the set, or removes it if it is (for tick boxes). */
fun Set<String>.toggle(item: String): Set<String> = if (item in this) this - item else this + item

/**
 * Turns the ticked package names into [AppItem]s to save.
 * Apps from [previousApps] that the picker didn't list (e.g. no home-screen icon)
 * are kept too, as long as they are still ticked.
 */
fun pickedApps(
    installed: List<InstalledAppInfo>?,
    selectedPackages: Set<String>,
    previousApps: List<AppItem> = emptyList(),
): List<AppItem> {
    val listed = installed.orEmpty()
    val picked = listed
        .filter { it.packageName in selectedPackages }
        .map { AppItem(packageName = it.packageName, name = it.label, isBlocked = true, icon = it.icon) }
    val kept = previousApps.filter { app ->
        app.packageName in selectedPackages && listed.none { it.packageName == app.packageName }
    }
    return picked + kept
}
