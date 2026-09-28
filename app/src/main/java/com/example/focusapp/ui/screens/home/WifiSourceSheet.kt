package com.example.focusapp.ui.screens.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.focusapp.data.wifi.WifiCheckResult
import com.example.focusapp.data.wifi.checkCurrentWifi
import com.example.focusapp.data.wifi.hasLocationPermissionForWifi
import com.example.focusapp.ui.common.rememberLocationPermissionState
import kotlinx.coroutines.launch

private val CardSurface = Color.White.copy(alpha = 0.15f)
private val DialogBgColor = Color(0xFF252853)
private val OnSheet = Color.White
private val OnSheetMuted = Color.White.copy(alpha = 0.8f)

/**
 * Content of the Wi-Fi Source Detection bottom sheet.
 *
 * [David Shiau, 2026-09-26] Moved here from the Settings screen, text and
 * logic unchanged: the "Wi-Fi Source Trigger" on/off switch, "Check Wi-Fi
 * Network" (tap the result to tag the current network) and "Stored Wi-Fi
 * Source List" (view/untag). New: Wi-Fi has its OWN app groups, separate
 * from Scheduled Limits' and Location's (see
 * BlockedAppGroupStorage.forWifiGroups), picked/renamed/listed exactly the
 * same way as on the Location Zone sheet. A Wi-Fi-started focus session
 * blocks only the selected Wi-Fi group (see NavGraph.kt's restrictedPackagesFor).
 *
 * The trigger switch and tagged list are hoisted (owned by
 * HomeScreenWithSheet), so the Wi-Fi banner on Home reacts to changes here
 * immediately; the callbacks are expected to persist them too.
 */
@Composable
fun WifiSourceSheetContent(
    groups: List<BlockedAppGroup>,
    selectedGroupId: String,
    isTriggerEnabled: Boolean,
    taggedSsids: List<String>,
    onTriggerToggle: (Boolean) -> Unit,
    onTagSsid: (String) -> Unit,
    onUntagSsid: (String) -> Unit,
    onAppsChange: (groupId: String, apps: List<AppItem>) -> Unit = { _, _ -> },
    onRenameGroup: (groupId: String, newName: String) -> Unit = { _, _ -> },
    onGroupListClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedGroup = groups.find { it.id == selectedGroupId } ?: groups.firstOrNull()

    var showAppPicker by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showSourceListDialog by remember { mutableStateOf(false) }
    val installedApps = rememberInstalledApps(shouldLoad = showAppPicker)

    // null = not checked yet this sheet visit.
    val locationPermissionState = rememberLocationPermissionState()
    var wifiResult by remember { mutableStateOf<WifiCheckResult?>(null) }
    var pendingWifiCheck by remember { mutableStateOf(false) }

    // Needs PRECISE location specifically (see hasLocationPermissionForWifi) -
    // asks for it if missing, which on Android 12+ also offers the
    // "Approximate -> Precise" upgrade.
    fun onCheckWifiClick() {
        if (hasLocationPermissionForWifi(context)) {
            scope.launch { wifiResult = checkCurrentWifi(context) }
        } else {
            pendingWifiCheck = true
            locationPermissionState.request()
        }
    }

    // Runs the check once the user answers the permission dialog, whatever
    // they chose - the result then says what's still missing, if anything.
    LaunchedEffect(locationPermissionState.resultCount) {
        if (pendingWifiCheck && locationPermissionState.resultCount > 0) {
            pendingWifiCheck = false
            wifiResult = checkCurrentWifi(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wi-Fi Source Detection",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = OnSheet,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .width(60.dp)
                .height(2.dp)
                .background(OnSheet)
        )

        Spacer(Modifier.height(16.dp))

        // Which Wi-Fi group is selected - tap to rename it.
        if (selectedGroup != null) {
            Text(
                text = "Group: ${selectedGroup.name}  ✎",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = OnSheetMuted,
                modifier = Modifier.clickable { showRenameDialog = true }
            )
            Spacer(Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardSurface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Wi-Fi Source Trigger",
                color = OnSheet,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isTriggerEnabled,
                onCheckedChange = onTriggerToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF474E91))
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetPillButton("Check Wi-Fi Network", Modifier.weight(1f)) { onCheckWifiClick() }
            SheetPillButton("Stored Wi-Fi Source List", Modifier.weight(1f)) { showSourceListDialog = true }
        }

        wifiResult?.let { result ->
            Spacer(Modifier.height(12.dp))
            WifiCheckResultText(
                result = result,
                taggedSsids = taggedSsids,
                onTag = onTagSsid
            )
        }

        Spacer(Modifier.height(12.dp))

        if (selectedGroup != null) {
            BlockAppsCard(
                group = selectedGroup,
                title = "Blocked Apps",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                onClick = { showAppPicker = true }
            )
        }

        Spacer(Modifier.height(16.dp))

        BottomChevronBar(onClick = onGroupListClick)
    }

    if (showAppPicker && selectedGroup != null) {
        GroupAppPickerBottomSheet(
            installedApps = installedApps,
            selectedApps = selectedGroup.apps,
            onAppsChange = { apps -> onAppsChange(selectedGroup.id, apps) },
            onDismiss = { showAppPicker = false },
            title = "Blocked Apps",
            subtitle = "Select apps to block when you're on a tagged Wi-Fi network"
        )
    }

    if (showRenameDialog && selectedGroup != null) {
        RenameGroupDialog(
            currentName = selectedGroup.name,
            onConfirm = { newName ->
                onRenameGroup(selectedGroup.id, newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showSourceListDialog) {
        WifiSourceListDialog(
            ssids = taggedSsids,
            onRemove = onUntagSsid,
            onDismiss = { showSourceListDialog = false }
        )
    }
}

@Composable
private fun SheetPillButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(CardSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = OnSheet, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

/**
 * One specific message per [WifiCheckResult] (moved from SettingsScreen,
 * unchanged). A connected result can be tapped to tag that network.
 */
@Composable
private fun WifiCheckResultText(
    result: WifiCheckResult,
    taggedSsids: List<String>,
    onTag: (String) -> Unit
) {
    val context = LocalContext.current
    val modifier = Modifier.fillMaxWidth()
    when (result) {
        is WifiCheckResult.Connected -> {
            val isTagged = result.ssid in taggedSsids
            Text(
                text = "Connected to: ${result.ssid}" + if (isTagged) " (already tagged)" else " - tap to tag",
                color = OnSheet,
                modifier = modifier.clickable(enabled = !isTagged) { onTag(result.ssid) }
            )
        }
        WifiCheckResult.NotOnWifi -> Text(
            text = "Not connected to Wi-Fi.",
            color = OnSheet,
            modifier = modifier
        )
        WifiCheckResult.NeedsPreciseLocation -> Text(
            text = "Connected to Wi-Fi, but Android only shares the network name with apps " +
                "that have PRECISE location. Tap to open Focus's app settings, then set " +
                "Location to allowed with \"Use precise location\" on.",
            color = OnSheet,
            modifier = modifier.clickable {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            }
        )
        WifiCheckResult.LocationServicesOff -> Text(
            text = "Connected to Wi-Fi, but the phone's Location setting is off - Android " +
                "hides the network name then. Tap to turn Location on.",
            color = OnSheet,
            modifier = modifier.clickable {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        )
        WifiCheckResult.NameUnavailable -> Text(
            text = "Connected to Wi-Fi, but Android didn't provide the network name. Try again in a moment.",
            color = OnSheet,
            modifier = modifier
        )
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
        Surface(shape = RoundedCornerShape(20.dp), color = DialogBgColor) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 480.dp)
            ) {
                Text(text = "Stored Wi-Fi Sources", color = OnSheet)
                Spacer(Modifier.height(12.dp))

                if (ssids.isEmpty()) {
                    Text(text = "No Wi-Fi networks tagged yet.", color = OnSheet)
                } else {
                    ssids.forEach { ssid ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = ssid, color = OnSheet, modifier = Modifier.weight(1f))
                            Text(
                                text = "Remove",
                                color = OnSheet,
                                modifier = Modifier.clickable { onRemove(ssid) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF3B3B96)
@Composable
private fun WifiSourceSheetContentPreview() {
    val fakeGroups = generateFakeGroups()
    WifiSourceSheetContent(
        groups = fakeGroups,
        selectedGroupId = fakeGroups.first().id,
        isTriggerEnabled = true,
        taggedSsids = listOf("Library"),
        onTriggerToggle = {},
        onTagSsid = {},
        onUntagSsid = {}
    )
}
