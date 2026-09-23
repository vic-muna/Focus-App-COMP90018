package com.example.focusapp.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.focusapp.data.apps.getAppLabel
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.data.usagestats.hasUsageAccessPermission
import com.example.focusapp.data.usagestats.queryUsageDurationsToday
import com.example.focusapp.data.wifi.WifiTriggerStorage
import com.example.focusapp.data.wifi.getCurrentWifiSsid
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.ui.common.rememberLocationPermissionState
import com.example.focusapp.ui.theme.WireframeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * SettingsScreen
 * ----------------
 * The "Settings" tab. Still mostly a placeholder (see the TODO below), but
 * now also hosts a "Check App Usage Duration" button - a real,
 * on-demand call into the OS's [android.app.usage.UsageStatsManager] via
 * [queryUsageDurationsToday], showing today's real per-app usage totals.
 * Display-only for now: tapping it doesn't feed into blocking or anything
 * else, it just answers "how long have I used each app today" the same
 * way a phone's own Digital Wellbeing / Screen Time screen would.
 *
 * [David Shiau, 2026-09-20] Added the "Check App Usage Duration" button and
 * everything below it in this file - previously this screen had no
 * functional content at all.
 *
 * [David Shiau, 2026-09-23] Added the "Today's Focus Time" button - sums
 * FocusRepository.getSessionHistory() (now really persisted, see
 * LocalDataSource) for sessions that started today and shows the total.
 * No cloud sync yet (no backend exists), so this is local/today-only;
 * see FocusRepository.saveFocusSession's TODO for the future remote push.
 *
 * [David Shiau, 2026-09-23] Added the "Check Wi-Fi Network" button - phase
 * 1 of the planned Wi-Fi-source trigger (see data/wifi/WifiProvider.kt):
 * just displays the currently-connected SSID, requesting location
 * permission first if needed (required by Android to read a real SSID).
 *
 * [David Shiau, 2026-09-23] Added phase 2 of the Wi-Fi-source trigger (see
 * data/wifi/WifiTriggerStorage.kt): a "Wi-Fi Source Trigger" on/off switch,
 * tap-to-tag on the "Check Wi-Fi Network" result (adds the current SSID to
 * the tagged list), and a "Stored Wi-Fi Source List" button to view/untag
 * what's saved. HomeScreenWithSheet polls the tagged list the same way it
 * already polls the saved FocusZone, and shows the same kind of
 * auto-suggestion banner when connected to a tagged network.
 *
 * TODO: to be implemented later - actual settings content (notifications,
 * account, language, etc.) once that's designed.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // null = not checked yet this screen visit; empty = checked, nothing used today.
    var usageResults by remember { mutableStateOf<List<Pair<String, Long>>?>(null) }
    var isLoadingUsage by remember { mutableStateOf(false) }
    var showUsagePermissionDialog by remember { mutableStateOf(false) }

    // null = not checked yet this screen visit.
    var todayFocusDurationMillis by remember { mutableStateOf<Long?>(null) }
    var isLoadingFocusTime by remember { mutableStateOf(false) }

    // null = not checked yet this screen visit; also null if checked but not
    // on Wi-Fi (or the name couldn't be resolved) - wifiCheckAttempted tells
    // those two apart.
    val locationPermissionState = rememberLocationPermissionState()
    var wifiSsid by remember { mutableStateOf<String?>(null) }
    var wifiCheckAttempted by remember { mutableStateOf(false) }
    var pendingWifiCheck by remember { mutableStateOf(false) }

    val wifiTriggerStorage = remember { WifiTriggerStorage(context) }
    var wifiTriggerEnabled by remember { mutableStateOf(false) }
    var taggedWifiSsids by remember { mutableStateOf<List<String>>(emptyList()) }
    var showWifiSourceListDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        wifiTriggerEnabled = withContext(Dispatchers.IO) { wifiTriggerStorage.isEnabled() }
        taggedWifiSsids = withContext(Dispatchers.IO) { wifiTriggerStorage.getTaggedSsids() }
    }

    // [David Shiau, 2026-09-20] Triggers UsageStatsManager (via
    // queryUsageDurationsToday) to fetch and display today's real per-app
    // usage totals - gated on the Usage Access permission first.
    fun checkAppUsageDuration() {
        // Usage access has no runtime permission dialog - the only way to
        // find out if it's granted is to ask AppOpsManager directly (see
        // hasUsageAccessPermission's doc comment), so check first rather
        // than querying and silently getting an empty map back.
        if (!hasUsageAccessPermission(context)) {
            showUsagePermissionDialog = true
            return
        }
        isLoadingUsage = true
        scope.launch {
            val durationsByPackage = withContext(Dispatchers.Default) {
                queryUsageDurationsToday(context)
            }
            usageResults = durationsByPackage.entries
                .sortedByDescending { it.value }
                .map { (packageName, millis) -> getAppLabel(context, packageName) to millis }
            isLoadingUsage = false
        }
    }

    // [David Shiau, 2026-09-23] Sums today's completed/cancelled focus
    // sessions (from FocusRepository.getSessionHistory()) into one total.
    fun checkTodayFocusTime() {
        isLoadingFocusTime = true
        scope.launch {
            val totalMillis = withContext(Dispatchers.IO) {
                val sessions = FocusRepositoryProvider.get(context).getSessionHistory()
                sumTodayFocusDurationMillis(sessions)
            }
            todayFocusDurationMillis = totalMillis
            isLoadingFocusTime = false
        }
    }

    // [David Shiau, 2026-09-23] Reads the current Wi-Fi SSID, requesting
    // location permission first if it's not already granted (needed to
    // read a real network name - see WifiProvider.kt). If the permission
    // dialog is showing, the actual read happens in the LaunchedEffect
    // below once the user responds, so this doesn't need a second tap.
    fun checkCurrentWifi() {
        if (locationPermissionState.hasPermission) {
            wifiSsid = getCurrentWifiSsid(context)
            wifiCheckAttempted = true
        } else {
            pendingWifiCheck = true
            locationPermissionState.request()
        }
    }

    LaunchedEffect(locationPermissionState.hasPermission) {
        if (pendingWifiCheck && locationPermissionState.hasPermission) {
            wifiSsid = getCurrentWifiSsid(context)
            wifiCheckAttempted = true
            pendingWifiCheck = false
        }
    }

    // [David Shiau, 2026-09-23] Flips the Wi-Fi-source trigger on/off -
    // HomeScreenWithSheet reads this same WifiTriggerStorage on its own
    // next load, same "no live cross-screen refresh yet" caveat as the
    // Blocked Apps groups (see BlockedAppGroupStorage's doc comment).
    fun onWifiTriggerToggle(enabled: Boolean) {
        wifiTriggerEnabled = enabled
        scope.launch {
            withContext(Dispatchers.IO) { wifiTriggerStorage.setEnabled(enabled) }
        }
    }

    // Tags the currently-displayed SSID (no-op if already tagged or nothing checked yet).
    fun tagCurrentWifiSsid() {
        val ssid = wifiSsid ?: return
        scope.launch {
            withContext(Dispatchers.IO) { wifiTriggerStorage.addTaggedSsid(ssid) }
            taggedWifiSsids = withContext(Dispatchers.IO) { wifiTriggerStorage.getTaggedSsids() }
        }
    }

    fun untagWifiSsid(ssid: String) {
        scope.launch {
            withContext(Dispatchers.IO) { wifiTriggerStorage.removeTaggedSsid(ssid) }
            taggedWifiSsids = withContext(Dispatchers.IO) { wifiTriggerStorage.getTaggedSsids() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WireframeColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(text = "Settings")
        Text(
            text = "No settings options yet - this screen is a placeholder.",
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(24.dp))

        Button(onClick = { checkAppUsageDuration() }) {
            Text("Check App Usage Duration")
        }

        Spacer(Modifier.height(16.dp))

        when {
            isLoadingUsage -> CircularProgressIndicator()
            usageResults != null -> UsageDurationResults(usageResults.orEmpty())
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = { checkTodayFocusTime() }) {
            Text("Today's Focus Time")
        }

        Spacer(Modifier.height(16.dp))

        when {
            isLoadingFocusTime -> CircularProgressIndicator()
            todayFocusDurationMillis != null ->
                Text(
                    text = "Total focus time today: ${formatUsageDuration(todayFocusDurationMillis!!)}",
                    color = WireframeColors.OnLight
                )
        }

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Wi-Fi Source Trigger",
                color = WireframeColors.OnLight,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = wifiTriggerEnabled, onCheckedChange = { onWifiTriggerToggle(it) })
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = { checkCurrentWifi() }) {
            Text("Check Wi-Fi Network")
        }

        Spacer(Modifier.height(16.dp))

        if (wifiCheckAttempted) {
            if (wifiSsid != null) {
                val isTagged = wifiSsid in taggedWifiSsids
                Text(
                    text = "Connected to: $wifiSsid" + if (isTagged) " (already tagged)" else " - tap to tag",
                    color = WireframeColors.OnLight,
                    modifier = Modifier.clickable(enabled = !isTagged) { tagCurrentWifiSsid() }
                )
            } else {
                Text(
                    text = "Not connected to Wi-Fi (or the network name couldn't be read).",
                    color = WireframeColors.OnLight
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = { showWifiSourceListDialog = true }) {
            Text("Stored Wi-Fi Source List")
        }
    }

    if (showWifiSourceListDialog) {
        WifiSourceListDialog(
            ssids = taggedWifiSsids,
            onRemove = { ssid -> untagWifiSsid(ssid) },
            onDismiss = { showWifiSourceListDialog = false }
        )
    }

    if (showUsagePermissionDialog) {
        UsageAccessPermissionDialog(
            onConfirm = {
                showUsagePermissionDialog = false
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            },
            onDismiss = { showUsagePermissionDialog = false }
        )
    }
}

/** Today's per-app totals, most-used first - a plain list, since this is display-only. */
@Composable
private fun UsageDurationResults(results: List<Pair<String, Long>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WireframeColors.Card, shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(text = "Today's App Usage", color = WireframeColors.OnDark)
        Spacer(Modifier.height(8.dp))

        if (results.isEmpty()) {
            Text(text = "No app usage recorded yet today.", color = WireframeColors.OnDark)
        } else {
            results.forEach { (label, millis) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = label, color = WireframeColors.OnDark, modifier = Modifier.weight(1f))
                    Text(text = formatUsageDuration(millis), color = WireframeColors.OnDark)
                }
            }
        }
    }
}

/** Lists every tagged Wi-Fi SSID with a "Remove" link - the counterpart to tap-to-tag above. */
@Composable
private fun WifiSourceListDialog(
    ssids: List<String>,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = WireframeColors.Card) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 480.dp)
            ) {
                Text(text = "Stored Wi-Fi Sources", color = WireframeColors.OnDark)
                Spacer(Modifier.height(12.dp))

                if (ssids.isEmpty()) {
                    Text(text = "No Wi-Fi networks tagged yet.", color = WireframeColors.OnDark)
                } else {
                    ssids.forEach { ssid ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = ssid, color = WireframeColors.OnDark, modifier = Modifier.weight(1f))
                            Text(
                                text = "Remove",
                                color = WireframeColors.OnDark,
                                modifier = Modifier.clickable { onRemove(ssid) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Explains why the Usage Access permission is needed before sending the user to grant it. */
@Composable
private fun UsageAccessPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Usage access needed") },
        text = {
            Text(
                "To show today's app usage, Focus needs the Usage Access " +
                    "permission. Find Focus in the list on the Settings " +
                    "screen that opens next and turn it on."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Open Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Sums the duration of every session that started on today's calendar date (device-local). */
private fun sumTodayFocusDurationMillis(sessions: List<FocusSession>): Long {
    val today = LocalDate.now(ZoneId.systemDefault())
    return sessions
        .filter { session ->
            Instant.ofEpochMilli(session.startTimeMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate() == today
        }
        .sumOf { session ->
            val end = session.endTimeMillis ?: session.startTimeMillis
            (end - session.startTimeMillis).coerceAtLeast(0)
        }
}

/** Turns raw milliseconds into a short "1h 23m" / "45m" / "12s" style string. */
private fun formatUsageDuration(millis: Long): String {
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
