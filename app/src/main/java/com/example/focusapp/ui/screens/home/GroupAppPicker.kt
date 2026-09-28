package com.example.focusapp.ui.screens.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.focusapp.data.apps.InstalledAppInfo
import com.example.focusapp.data.apps.getLaunchableApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [David Shiau, 2026-09-26] The "pick which apps are in this group" flow,
 * shared by the Scheduled Limits sheet and the Location Zone sheet (moved
 * here out of AutoBlockingSheetContent so both use the exact same one).
 */
class InstalledAppsState {
    var apps by mutableStateOf<List<InstalledAppInfo>>(emptyList())
    var isLoading by mutableStateOf(false)
}

/**
 * [David Shiau, 2026-09-20] Real installed apps for the picker - loaded
 * once (lazily, the first time [shouldLoad] is true) and reused afterwards
 * rather than re-querying PackageManager every time the picker is reopened.
 */
@Composable
fun rememberInstalledApps(shouldLoad: Boolean): InstalledAppsState {
    val context = LocalContext.current
    val state = remember { InstalledAppsState() }
    LaunchedEffect(shouldLoad) {
        if (shouldLoad && state.apps.isEmpty() && !state.isLoading) {
            state.isLoading = true
            state.apps = withContext(Dispatchers.Default) { getLaunchableApps(context) }
            state.isLoading = false
        }
    }
    return state
}

/**
 * The app picker, as a sheet stacked on top of whichever summary sheet
 * opened it. [onAppsChange] receives only the checked apps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupAppPickerBottomSheet(
    installedApps: InstalledAppsState,
    selectedApps: List<AppItem>,
    onAppsChange: (List<AppItem>) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Limited Apps",
    subtitle: String = "Select apps to limit during this group's active time"
) {
    // [David Shiau, 2026-09-20] Merge every real installed app with
    // this group's already-blocked packages, so the picker shows the
    // whole phone's app list with the group's current selection
    // pre-checked (multi-select - any number of rows can be checked
    // at once).
    val blockedPackageNames = remember(selectedApps) {
        selectedApps.filter { it.isBlocked }.map { it.packageName }.toSet()
    }
    val pickerApps = remember(installedApps.apps, blockedPackageNames) {
        installedApps.apps.map { info ->
            AppItem(
                packageName = info.packageName,
                name = info.label,
                isBlocked = info.packageName in blockedPackageNames,
                icon = info.icon
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // Avoids a doubled/near-black scrim when stacked over the
        // already-open summary sheet.
        scrimColor = Color.Transparent
    ) {
        AppPickerSheet(
            apps = pickerApps,
            isLoading = installedApps.isLoading,
            title = title,
            subtitle = subtitle,
            // Only the checked apps are worth persisting on the group -
            // storing the full installed-app list would balloon storage
            // and break "N apps blocked" counts elsewhere.
            onAppsChange = { updated -> onAppsChange(updated.filter { it.isBlocked }) }
        )
    }
}
