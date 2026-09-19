package com.example.focusapp.ui.screens.apps

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.apps.InstalledAppInfo
import com.example.focusapp.data.apps.getAppIcon
import com.example.focusapp.data.apps.getAppLabel
import com.example.focusapp.data.apps.getLaunchableApps
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.ui.theme.WireframeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * AppsScreen
 * ------------
 * Main / landing page of the app (matches the teammate-provided wireframe
 * "Image 1"). Lists the user's app groups, each as a dark pill showing
 * real icons for its member apps and an "Edit" link, plus a big "+" pill
 * at the bottom to add a new group.
 *
 * [groups] is now loaded for real from [com.example.focusapp.data.local.LocalDataSource]'s
 * persisted storage (via [FocusRepositoryProvider]), instead of a
 * hard-coded placeholder list. It's reloaded every time this screen
 * re-enters composition (e.g. after Save on AddAppGroupScreen pops back
 * here), because Navigation Compose disposes and recomposes a
 * destination's content on simple push/pop navigation - which is exactly
 * the "refresh" behaviour this screen wants, with no extra plumbing.
 *
 * @param onEditGroupClick called with the tapped group's [AppGroup.id],
 *        so the Edit screen can load that exact group's real data.
 * @param onAddGroupClick called when the "+" pill is tapped.
 */
@Composable
fun AppsScreen(
    onEditGroupClick: (String) -> Unit,
    onAddGroupClick: () -> Unit
) {
    val context = LocalContext.current
    var groups by remember { mutableStateOf<List<AppGroup>>(emptyList()) }
    var isLoadingGroups by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        groups = withContext(Dispatchers.IO) { FocusRepositoryProvider.get(context).getAppGroups() }
        isLoadingGroups = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WireframeColors.Background)
            // The two test panels below add a lot of vertical content
            // (log entries can grow), so this screen now needs to scroll -
            // it didn't before.
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // --- AccessibilityService prototype/testing panel ---
        // Temporary developer-facing UI so you can watch app-detection
        // and app-blocking happen live, without needing logcat. Remove or
        // hide this once the real restriction UI (driven by AppGroup's
        // schedule fields) replaces it.
        AccessibilityTestPanel()

        Box(modifier = Modifier.height(20.dp))

        // --- App usage-duration test panel ---
        // Shows how long each app has been used since Focus was opened,
        // computed entirely from FocusAccessibilityService's foreground-
        // change events (see AccessibilityBridge's usage-tracking section)
        // - no extra permission needed beyond the accessibility service
        // already required above.
        UsageDurationTestPanel()

        Box(modifier = Modifier.height(32.dp))

        when {
            isLoadingGroups -> Text(text = "Loading groups…", color = WireframeColors.OnLight)
            groups.isEmpty() -> Text(
                text = "No app groups yet - tap + below to create one.",
                color = WireframeColors.OnLight
            )
            else -> groups.forEach { group ->
                AppGroupRow(group = group, onEditClick = { onEditGroupClick(group.id) })
                Box(modifier = Modifier.height(24.dp)) // simple vertical spacer between cards
            }
        }

        AddPillButton(onClick = onAddGroupClick)
    }
}

/**
 * AccessibilityTestPanel
 * -------------------------
 * A self-contained developer test panel for [com.example.focusapp.data.accessibility.FocusAccessibilityService].
 * Everything it shows comes from [AccessibilityBridge]'s StateFlows via
 * `collectAsState()`, so this Composable automatically redraws the
 * instant the service (running independently, driven by the OS) reports
 * something new - no manual refresh needed.
 *
 * What each piece does:
 *  - "Service status" line: true only after the OS has actually connected
 *    the service, i.e. after you've enabled it in system Settings.
 *  - "Open Accessibility Settings" button: launches the system screen
 *    where you flip that toggle - an app is not allowed to enable its own
 *    accessibility service, so this is as far as code can take you.
 *  - "Current foreground app" line: updates live every time you switch
 *    apps (minimize this app, open something else, come back) - proof
 *    the detection side works.
 *  - "Select App to Block" button: opens [AppPickerDialog] so you can pick
 *    one real installed app instead of typing a raw package name.
 *  - "Select Group to Block" button: opens [GroupPickerDialog] so you can
 *    block every app in one of your saved groups at once - this is the
 *    "select the grouped apps you want to block when you start Focus"
 *    behaviour, implemented on top of the same restricted-package list
 *    the single-app picker already uses.
 *  - Restricted-package list: shows what's currently blocked, with a
 *    "Remove" link to un-block it.
 *  - Event log: every time the service actually bounces you home because
 *    you opened a restricted app, a line appears here - proof the
 *    blocking side works.
 */
@Composable
private fun AccessibilityTestPanel() {
    val context = LocalContext.current
    val isConnected by AccessibilityBridge.isServiceConnected.collectAsState()
    val currentForegroundApp by AccessibilityBridge.currentForegroundApp.collectAsState()
    val restrictedPackages by AccessibilityBridge.restrictedPackages.collectAsState()
    val blockLog by AccessibilityBridge.blockLog.collectAsState()

    var showAppPicker by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WireframeColors.Card, shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(text = "AccessibilityService Test Panel", color = WireframeColors.OnDark)

        Text(
            text = "Service status: ${if (isConnected) "Connected ✅" else "Not enabled ❌"}",
            color = WireframeColors.OnDark,
            modifier = Modifier.padding(top = 10.dp)
        )

        PillButton(
            text = "Open Accessibility Settings",
            onClick = {
                // Deep-links straight into system Settings' accessibility
                // list - this app cannot flip the toggle itself.
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        )

        Text(
            text = "Current foreground app: ${currentForegroundApp ?: "(none detected yet - switch apps to test)"}",
            color = WireframeColors.OnDark,
            modifier = Modifier.padding(top = 14.dp)
        )

        Text(
            text = "Restricted packages:",
            color = WireframeColors.OnDark,
            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
        )
        if (restrictedPackages.isEmpty()) {
            Text(text = "(none yet)", color = WireframeColors.OnDark)
        } else {
            restrictedPackages.forEach { pkg -> RestrictedPackageRow(packageName = pkg) }
        }

        Row(modifier = Modifier.padding(top = 10.dp)) {
            PillButton(text = "Select App to Block", onClick = { showAppPicker = true }, topPadding = 0.dp)
            Spacer(modifier = Modifier.padding(start = 8.dp))
            PillButton(text = "Select Group to Block", onClick = { showGroupPicker = true }, topPadding = 0.dp)
        }

        Text(
            text = "Recent block events:",
            color = WireframeColors.OnDark,
            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
        )
        if (blockLog.isEmpty()) {
            Text(text = "(none yet)", color = WireframeColors.OnDark)
        } else {
            // Newest first, easiest to read while testing.
            blockLog.asReversed().forEach { entry ->
                Text(text = entry, color = WireframeColors.OnDark)
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            onAppSelected = { packageName ->
                AccessibilityBridge.addRestrictedPackage(packageName)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    if (showGroupPicker) {
        GroupPickerDialog(
            onGroupSelected = { group ->
                AccessibilityBridge.addRestrictedPackages(group.packageNames)
                showGroupPicker = false
            },
            onDismiss = { showGroupPicker = false }
        )
    }
}

/**
 * PillButton
 * ------------
 * A small reusable "text on a rounded dark pill, clickable" component -
 * used by both test panels for their settings-shortcut buttons, so they
 * look and behave identically.
 */
@Composable
private fun PillButton(text: String, onClick: () -> Unit, topPadding: Dp = 6.dp) {
    Text(
        text = text,
        color = WireframeColors.OnDark,
        modifier = Modifier
            .padding(top = topPadding)
            .background(WireframeColors.CardLight, shape = RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

/**
 * AppPickerDialog
 * ------------------
 * A full-screen-ish scrollable dialog listing every launchable app on the
 * device (via [getLaunchableApps]), with real icons and names, so the
 * user picks an app instead of typing a package name by hand.
 *
 * @param onAppSelected called with the tapped app's package name.
 * @param onDismiss called when the user taps outside the dialog or wants
 *        to close it without picking anything.
 */
@Composable
private fun AppPickerDialog(onAppSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.Default) { getLaunchableApps(context) }
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = WireframeColors.Card) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 480.dp)
            ) {
                Text(text = "Select an app to block", color = WireframeColors.OnDark)
                Spacer(modifier = Modifier.height(12.dp))

                when {
                    isLoading -> Text(text = "Loading installed apps…", color = WireframeColors.OnDark)
                    apps.isEmpty() -> Text(text = "No launchable apps found.", color = WireframeColors.OnDark)
                    else -> {
                        // LazyColumn instead of Column.forEach: a phone
                        // easily has 100+ launchable apps, and LazyColumn
                        // only composes the rows currently on screen.
                        LazyColumn {
                            items(apps, key = { it.packageName }) { app ->
                                AppPickerRow(app = app, onClick = { onAppSelected(app.packageName) })
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * AppPickerRow
 * --------------
 * One row inside [AppPickerDialog]: the app's real launcher icon (falling
 * back to the same grey square used elsewhere if the icon failed to
 * load), its display name, and its raw package name underneath (useful
 * for a developer test panel, even though a normal end user wouldn't
 * need to see it).
 */
@Composable
private fun AppPickerRow(app: InstalledAppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (app.icon != null) {
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(40.dp)
            )
        } else {
            AppIconPlaceholder()
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = app.label, color = WireframeColors.OnDark)
            Text(text = app.packageName, color = WireframeColors.OnDark, fontSize = 11.sp)
        }
    }
}

/**
 * GroupPickerDialog
 * --------------------
 * Lists the user's saved [AppGroup]s (loaded via [FocusRepositoryProvider],
 * the same real, persisted data [AppsScreen] itself shows), so tapping one
 * blocks every app inside it at once, via
 * [AccessibilityBridge.addRestrictedPackages].
 *
 * @param onGroupSelected called with the tapped group.
 * @param onDismiss called when the user taps outside the dialog.
 */
@Composable
private fun GroupPickerDialog(onGroupSelected: (AppGroup) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var groups by remember { mutableStateOf<List<AppGroup>>(emptyList()) }

    LaunchedEffect(Unit) {
        groups = withContext(Dispatchers.IO) { FocusRepositoryProvider.get(context).getAppGroups() }
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = WireframeColors.Card) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 480.dp)
            ) {
                Text(text = "Select a group to block", color = WireframeColors.OnDark)
                Spacer(modifier = Modifier.height(12.dp))

                when {
                    isLoading -> Text(text = "Loading groups…", color = WireframeColors.OnDark)
                    groups.isEmpty() -> Text(
                        text = "No saved groups yet - create one from the Apps screen's + button first.",
                        color = WireframeColors.OnDark
                    )
                    else -> LazyColumn {
                        items(groups, key = { it.id }) { group ->
                            GroupPickerRow(group = group, onClick = { onGroupSelected(group) })
                        }
                    }
                }
            }
        }
    }
}

/**
 * GroupPickerRow
 * -----------------
 * One row inside [GroupPickerDialog]: the group's name, how many apps it
 * contains, and up to 3 real icons for a quick visual reminder of what's
 * actually in it.
 */
@Composable
private fun GroupPickerRow(group: AppGroup, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = "${group.groupName}  (${group.packageNames.size} apps)",
            color = WireframeColors.OnDark
        )
        Row(modifier = Modifier.padding(top = 6.dp)) {
            group.packageNames.take(3).forEach { packageName ->
                GroupMemberIcon(packageName = packageName)
                Spacer(modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

/**
 * RestrictedPackageRow
 * -----------------------
 * One row in the restricted-packages list: the package name plus a
 * "Remove" link that calls [AccessibilityBridge.removeRestrictedPackage].
 */
@Composable
private fun RestrictedPackageRow(packageName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = packageName,
            color = WireframeColors.OnDark,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Remove",
            color = WireframeColors.OnDark,
            modifier = Modifier.clickable {
                AccessibilityBridge.removeRestrictedPackage(packageName)
            }
        )
    }
}

/**
 * UsageDurationTestPanel
 * -------------------------
 * Answers "since opening Focus, how long has the user spent in app X" -
 * e.g. Facebook - for every app that's been in the foreground at least
 * once, built entirely on top of [AccessibilityBridge]'s usage tracking
 * (see that file's section 5), which in turn is driven by
 * FocusAccessibilityService's foreground-change events. No extra
 * permission or API is needed beyond the accessibility service already
 * required for the panel above.
 *
 * WHY THIS NEEDS ITS OWN TIMER (unlike AccessibilityTestPanel above):
 * [AccessibilityBridge.getUsageMillisSoFar] intentionally includes time
 * from the CURRENTLY ONGOING session (so the number keeps growing while
 * you're still inside Facebook, say, not just after you switch away from
 * it) - but nothing pushes a new value while you just sit in one app, so
 * there's no StateFlow to `collectAsState()` for "the current second".
 * Instead, a `LaunchedEffect` ticks a plain `remember`ed counter once a
 * second purely to force this Composable to recompute (and therefore
 * re-call `getUsageMillisSoFar` for a fresh "now") - the underlying data
 * hasn't necessarily changed, just how much time has passed since it was
 * last measured.
 */
@Composable
private fun UsageDurationTestPanel() {
    val context = LocalContext.current
    val finishedUsageMillis by AccessibilityBridge.finishedUsageMillis.collectAsState()
    val currentForegroundApp by AccessibilityBridge.currentForegroundApp.collectAsState()

    // Forces a recompute of "how much time has passed" once a second,
    // even when no underlying StateFlow actually changed - see doc above.
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick = System.currentTimeMillis()
        }
    }

    // Recomputed whenever a session finishes (finishedUsageMillis changes),
    // whenever the foreground app changes, OR once a second via `tick` -
    // any of the three could change what should be displayed.
    val rankedUsage = remember(finishedUsageMillis, currentForegroundApp, tick) {
        AccessibilityBridge.getTrackedPackages()
            .map { packageName -> packageName to AccessibilityBridge.getUsageMillisSoFar(packageName) }
            .sortedByDescending { (_, millis) -> millis }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WireframeColors.Card, shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(text = "App Usage Duration (since Focus was opened)", color = WireframeColors.OnDark)

        Text(
            text = "Switch to another app (e.g. Facebook) and back to see its time start counting.",
            color = WireframeColors.OnDark,
            modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)
        )

        if (rankedUsage.isEmpty()) {
            Text(text = "(nothing tracked yet)", color = WireframeColors.OnDark)
        } else {
            rankedUsage.forEach { (packageName, millis) ->
                val label = remember(packageName) { getAppLabel(context, packageName) }
                val isCurrent = packageName == currentForegroundApp
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = if (isCurrent) "$label (now open)" else label,
                        color = WireframeColors.OnDark,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = formatDurationMillis(millis), color = WireframeColors.OnDark)
                }
            }
        }
    }
}

/**
 * formatDurationMillis
 * -----------------------
 * Turns raw milliseconds into a short "1m 42s" / "45s" style string for
 * display. Kept intentionally simple (no hours) since this panel only
 * tracks usage within a single app session (see AccessibilityBridge's
 * doc comment on why the counter resets when the app process restarts).
 */
private fun formatDurationMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}

/**
 * AppGroupRow
 * -------------
 * One row in the group list: the group's real name above a dark pill
 * containing real icons (up to 3, plus a "+N" overflow badge) for its
 * member apps, and an "Edit" text button.
 */
@Composable
private fun AppGroupRow(group: AppGroup, onEditClick: () -> Unit) {
    Column {
        Text(
            text = group.groupName,
            color = WireframeColors.OnLight,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WireframeColors.Card, shape = RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val shown = group.packageNames.take(3)
            shown.forEachIndexed { index, packageName ->
                GroupMemberIcon(packageName = packageName)
                if (index != shown.lastIndex) {
                    Box(modifier = Modifier.padding(end = 10.dp))
                }
            }
            val overflowCount = group.packageNames.size - shown.size
            if (overflowCount > 0) {
                Text(
                    text = "+$overflowCount",
                    color = WireframeColors.OnDark,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            // weight(1f) (a RowScope-only modifier) makes this Box consume
            // exactly the remaining horizontal space after the icons, so
            // "Edit" lands at the far right without overflowing the pill.
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Edit",
                    color = WireframeColors.OnDark,
                    modifier = Modifier.clickable(onClick = onEditClick)
                )
            }
        }
    }
}

/**
 * GroupMemberIcon
 * ------------------
 * One real app icon resolved by package name via [getAppIcon]. Falls back
 * to the grey placeholder square if the icon can't be loaded (e.g. the
 * app was uninstalled after being added to a group). Shared by
 * [AppGroupRow] and [GroupPickerRow].
 */
@Composable
private fun GroupMemberIcon(packageName: String) {
    val context = LocalContext.current
    val icon = remember(packageName) { getAppIcon(context, packageName) }
    if (icon != null) {
        Image(
            bitmap = icon.asImageBitmap(),
            contentDescription = packageName,
            modifier = Modifier.size(44.dp)
        )
    } else {
        AppIconPlaceholder()
    }
}

/**
 * AppIconPlaceholder
 * ---------------------
 * A plain light-grey rounded square standing in for a real app icon, used
 * as a fallback wherever a real icon fails to load (uninstalled app,
 * PackageManager hiccup, etc.) across the whole Apps flow.
 */
// Not `private`: reused as-is from EditAppGroupScreen.kt and
// AddAppGroupScreen.kt (both in this same `ui.screens.apps` package) so
// every screen in the Apps flow shows an identical icon placeholder.
@Composable
fun AppIconPlaceholder() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(WireframeColors.IconPlaceholder, shape = RoundedCornerShape(10.dp))
    )
}

/**
 * AddPillButton
 * ---------------
 * The big dark "+" pill used at the bottom of the Apps screen. Purely
 * navigational - creating a real new AppGroup happens on
 * [AddAppGroupScreen], which this navigates to.
 *
 * The Map screen uses its own near-identical
 * `ui.screens.map.AddLocationPillButton` rather than this one, since the
 * two "+" flows aren't coupled to each other - see that file's doc
 * comment for why it wasn't shared instead.
 */
@Composable
fun AddPillButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WireframeColors.Card, shape = RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(WireframeColors.CardLight, shape = RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add", tint = WireframeColors.OnDark)
        }
    }
}
