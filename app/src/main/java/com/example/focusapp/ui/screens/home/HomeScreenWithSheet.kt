package com.example.focusapp.ui.screens.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.data.wifi.WifiTriggerStorage
import com.example.focusapp.data.wifi.getCurrentWifiSsid
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.usecase.EvaluateFocusTriggerUseCase
import com.example.focusapp.domain.usecase.FocusTriggerResult
import com.example.focusapp.ui.common.rememberLocationPermissionState
import com.example.focusapp.ui.screens.session.FocusSessionSource
import com.example.focusapp.ui.theme.WireframeColors
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SheetType { NONE, BLOCKED_APPS, LOCATION_ZONE, WIFI_SOURCE }

/** How often the auto-suggestion check re-evaluates while Home is on screen - schedule
 *  boundaries only need minute-granularity, so there's no need for anything tighter. */
private const val AUTO_TRIGGER_CHECK_INTERVAL_MILLIS = 60_000L

/** How long Home waits, unchanged, before actually navigating to Focus Session -
 *  time for a background animation to play first (not built yet; this is just the
 *  timing seam for it). Mirrored on the way out by FocusSessionScreen's own delay. */
private const val FOCUS_SESSION_START_DELAY_MILLIS = 4_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenWithSheet(
    groups: List<BlockedAppGroup>,
    selectedGroupId: String,
    reopenSheetSignal: Boolean,
    reopenSheetType: String = "blocked_apps",
    onReopenSheetHandled: () -> Unit,
    onBlockerClick: () -> Unit,
    onGroupAppsChange: (groupId: String, apps: List<AppItem>) -> Unit,
    onGroupScheduleChange: (groupId: String, schedule: TimeSlot) -> Unit,
    onGroupRename: (groupId: String, newName: String) -> Unit = { _, _ -> },
    onGroupMaxOpensChange: (groupId: String, maxOpens: Int?) -> Unit = { _, _ -> },
    onGroupMaxDurationChange: (groupId: String, maxMinutes: Int?) -> Unit = { _, _ -> },
    onFocusSessionStart: (FocusSessionSource) -> Unit = {},
    onScheduleBannerClick: (groupId: String) -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onPartyModeClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onEditLocationZoneClick: () -> Unit = {},
    // [David Shiau, 2026-09-26] Location Zone's own app groups - separate
    // from `groups` above (Scheduled Limits).
    locationGroups: List<BlockedAppGroup> = emptyList(),
    selectedLocationGroupId: String = "",
    onLocationGroupAppsChange: (groupId: String, apps: List<AppItem>) -> Unit = { _, _ -> },
    onLocationGroupRename: (groupId: String, newName: String) -> Unit = { _, _ -> },
    onLocationGroupListClick: () -> Unit = {},
    // [David Shiau, 2026-09-26] Wi-Fi Source Detection's own app groups -
    // separate from both of the above.
    wifiGroups: List<BlockedAppGroup> = emptyList(),
    selectedWifiGroupId: String = "",
    onWifiGroupAppsChange: (groupId: String, apps: List<AppItem>) -> Unit = { _, _ -> },
    onWifiGroupRename: (groupId: String, newName: String) -> Unit = { _, _ -> },
    onWifiGroupListClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ModalBottomSheet 專用的 State
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var activeSheet by remember { mutableStateOf(SheetType.NONE) }

    fun openSheet(type: SheetType) {
        activeSheet = type
    }

    fun closeSheet(onFinished: () -> Unit = {}) {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                activeSheet = SheetType.NONE
                onFinished()
            }
        }
    }

    // Home stays fully visible/unchanged for a beat before actually navigating -
    // gives a background animation time to play first (not built yet).
    fun startFocusSessionAfterDelay(source: FocusSessionSource) {
        scope.launch {
            delay(FOCUS_SESSION_START_DELAY_MILLIS)
            onFocusSessionStart(source)
        }
    }

    // [David Shiau, 2026-09-20] Blocking only works while
    // FocusAccessibilityService is enabled - the user has to grant that
    // themselves in system Settings (Android never lets an app enable its
    // own AccessibilityService). Every session-start path checks this first
    // rather than silently starting a session that blocks nothing.
    val isAccessibilityEnabled by AccessibilityBridge.isServiceConnected.collectAsState()
    var showAccessibilityPermissionDialog by remember { mutableStateOf(false) }

    // [David Shiau, 2026-09-23] Shared by Quick Focus AND the auto-suggestion
    // banner's "start a focus session?" accept action - previously only
    // Quick Focus ran this check, so accepting the banner on a first-time
    // (permission not yet granted) tap silently started a session with no
    // blocking instead of prompting for Accessibility access.
    fun startFocusSessionIfPermitted(source: FocusSessionSource) {
        if (isAccessibilityEnabled) {
            startFocusSessionAfterDelay(source)
        } else {
            showAccessibilityPermissionDialog = true
        }
    }

    fun onQuickFocusClick() {
        startFocusSessionIfPermitted(FocusSessionSource.Manual)
    }

    // 重開 Sheet 的 Signal 處理
    LaunchedEffect(reopenSheetSignal) {
        if (reopenSheetSignal) {
            onReopenSheetHandled()
            openSheet(
                when (reopenSheetType) {
                    "location_zone" -> SheetType.LOCATION_ZONE
                    "wifi_source" -> SheetType.WIFI_SOURCE
                    else -> SheetType.BLOCKED_APPS
                }
            )
        }
    }

    // --- Auto-suggestion: UI-simulated only, never navigates on its own -----------
    var savedZone by remember { mutableStateOf<FocusZone?>(null) }
    LaunchedEffect(Unit) {
        savedZone = withContext(Dispatchers.IO) {
            FocusRepositoryProvider.get(context).getFocusZone()
        }
    }

    val permissionState = rememberLocationPermissionState()
    var currentLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(permissionState.hasPermission) {
        if (permissionState.hasPermission) {
            fetchCurrentLocationForAutoCheck(context) { lat, lng -> currentLatLng = lat to lng }
        }
    }

    // --- Wi-Fi-source trigger: same "UI-simulated only" polling approach as
    // the location trigger above - see WifiTriggerStorage's doc comment. ---
    // [David Shiau, 2026-09-26] Edited from the Wi-Fi Source Detection sheet
    // (moved here from Settings), so these writers keep the in-memory state
    // and storage in sync - the banner reacts immediately.
    val wifiTriggerStorage = remember { WifiTriggerStorage(context) }
    var wifiTriggerEnabled by remember { mutableStateOf(false) }
    var taggedWifiSsids by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        wifiTriggerEnabled = withContext(Dispatchers.IO) { wifiTriggerStorage.isEnabled() }
        taggedWifiSsids = withContext(Dispatchers.IO) { wifiTriggerStorage.getTaggedSsids() }
    }

    fun setWifiTriggerEnabled(enabled: Boolean) {
        wifiTriggerEnabled = enabled
        scope.launch { withContext(Dispatchers.IO) { wifiTriggerStorage.setEnabled(enabled) } }
    }

    fun updateTaggedSsids(change: suspend (WifiTriggerStorage) -> Unit) {
        scope.launch {
            taggedWifiSsids = withContext(Dispatchers.IO) {
                change(wifiTriggerStorage)
                wifiTriggerStorage.getTaggedSsids()
            }
        }
    }

    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(AUTO_TRIGGER_CHECK_INTERVAL_MILLIS)
            tick = System.currentTimeMillis()
        }
    }

    var currentWifiSsid by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(wifiTriggerEnabled, tick) {
        currentWifiSsid = if (wifiTriggerEnabled) {
            withContext(Dispatchers.IO) { getCurrentWifiSsid(context) }
        } else {
            null
        }
    }

    // [David Shiau, 2026-09-26] All matching triggers, not just the first -
    // so the schedule banner and the location banner can both show at once.
    val triggers = remember(groups, savedZone, currentLatLng, taggedWifiSsids, currentWifiSsid, tick) {
        EvaluateFocusTriggerUseCase().executeAll(
            groups = groups,
            currentZones = listOfNotNull(savedZone),
            currentLatLng = currentLatLng,
            taggedWifiSsids = taggedWifiSsids,
            currentWifiSsid = currentWifiSsid
        )
    }

    // Each banner is dismissed on its own.
    var dismissedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    fun suggestionKeyOf(trigger: FocusTriggerResult): String? = when (trigger) {
        is FocusTriggerResult.ScheduleMatch -> "schedule:${trigger.groupId}"
        is FocusTriggerResult.LocationMatch -> "zone:${trigger.zoneId}"
        is FocusTriggerResult.WifiMatch -> "wifi:${trigger.ssid}"
        FocusTriggerResult.NoTrigger -> null
    }
    val suggestions = triggers.mapNotNull { trigger ->
        suggestionKeyOf(trigger)?.takeIf { it !in dismissedKeys }?.let { key -> key to trigger }
    }

    // 最外層強制全螢幕 Box，阻斷返回 Home 時 BottomSheet 或繪製節點導致的尺寸縮放跳動
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WireframeColors.Background)
    ) {
        // 1. 主要畫面
        HomeScreen(
            onAvatarClick = onAvatarClick,
            onQuickFocusClick = { onQuickFocusClick() },
            onPartyModeClick = onPartyModeClick,
            onBlockedAppCardClick = { openSheet(SheetType.BLOCKED_APPS) },
            onLocationCardClick = { openSheet(SheetType.LOCATION_ZONE) },
            onWifiCardClick = { openSheet(SheetType.WIFI_SOURCE) },
            onSettingsClick = onSettingsClick
        )

        if (suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                suggestions.forEach { (suggestionKey, suggestion) ->
                    AutoFocusSuggestionBanner(
                        result = suggestion,
                        onAccept = {
                            val source = when (suggestion) {
                                // [David Shiau, 2026-09-26] Un-wired from starting a focus
                                // session - a schedule match now opens that group's
                                // "today's opens/duration" page instead (phase 1 of the
                                // daily open-times/duration limit, see GroupUsageScreen).
                                is FocusTriggerResult.ScheduleMatch -> {
                                    onScheduleBannerClick(suggestion.groupId)
                                    return@AutoFocusSuggestionBanner
                                }
                                is FocusTriggerResult.LocationMatch ->
                                    FocusSessionSource.Location(suggestion.zoneName)
                                is FocusTriggerResult.WifiMatch ->
                                    FocusSessionSource.Wifi(suggestion.ssid)
                                FocusTriggerResult.NoTrigger -> return@AutoFocusSuggestionBanner
                            }
                            startFocusSessionIfPermitted(source)
                        },
                        onDismiss = { dismissedKeys = dismissedKeys + suggestionKey }
                    )
                }
            }
        }

        // 2. BottomSheet 區塊
        if (activeSheet != SheetType.NONE) {
            ModalBottomSheet(
                onDismissRequest = { activeSheet = SheetType.NONE },
                sheetState = sheetState,
                containerColor = Color(0xFF3B3B96)
            ) {
                when (activeSheet) {
                    SheetType.BLOCKED_APPS -> AutoBlockingSheetContent(
                        groups = groups,
                        selectedGroupId = selectedGroupId,
                        onAppsChange = onGroupAppsChange,
                        onScheduleChange = onGroupScheduleChange,
                        onRenameGroup = onGroupRename,
                        onMaxOpensChange = onGroupMaxOpensChange,
                        onMaxDurationChange = onGroupMaxDurationChange,
                        onBlockerClick = {
                            closeSheet { onBlockerClick() }
                        }
                    )

                    SheetType.LOCATION_ZONE -> LocationZoneSheetContent(
                        groups = locationGroups,
                        selectedGroupId = selectedLocationGroupId,
                        onAppsChange = onLocationGroupAppsChange,
                        onRenameGroup = onLocationGroupRename,
                        onEditClick = { closeSheet { onEditLocationZoneClick() } },
                        onGroupListClick = { closeSheet { onLocationGroupListClick() } }
                    )

                    SheetType.WIFI_SOURCE -> WifiSourceSheetContent(
                        groups = wifiGroups,
                        selectedGroupId = selectedWifiGroupId,
                        isTriggerEnabled = wifiTriggerEnabled,
                        taggedSsids = taggedWifiSsids,
                        onTriggerToggle = { setWifiTriggerEnabled(it) },
                        onTagSsid = { ssid -> updateTaggedSsids { it.addTaggedSsid(ssid) } },
                        onUntagSsid = { ssid -> updateTaggedSsids { it.removeTaggedSsid(ssid) } },
                        onAppsChange = onWifiGroupAppsChange,
                        onRenameGroup = onWifiGroupRename,
                        onGroupListClick = { closeSheet { onWifiGroupListClick() } }
                    )

                    SheetType.NONE -> Unit
                }
            }
        }

        if (showAccessibilityPermissionDialog) {
            AccessibilityPermissionDialog(
                onConfirm = {
                    showAccessibilityPermissionDialog = false
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                onDismiss = { showAccessibilityPermissionDialog = false }
            )
        }
    }
}

/**
 * Explains why Quick Focus needs the Accessibility permission before
 * sending the user to system Settings to grant it - Android requires this
 * to be an explicit, informed action there, it can't be requested as an
 * ordinary runtime permission dialog (see FocusAccessibilityService's doc
 * comment).
 */
@Composable
private fun AccessibilityPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accessibility permission needed") },
        text = {
            Text(
                "To block apps during a focus session, Focus needs the " +
                    "Accessibility permission. Turn it on for Focus in the " +
                    "Settings screen that opens next."
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

/**
 * Dismissible banner for an auto-detected trigger - accepting a location/Wi-Fi
 * match starts a session, accepting a schedule match opens that group's usage
 * page; nothing here ever navigates on its own.
 */
@Composable
private fun AutoFocusSuggestionBanner(
    result: FocusTriggerResult,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val message = when (result) {
        is FocusTriggerResult.ScheduleMatch -> "You're in your ${result.groupName} schedule - tap to see today's app usage"
        is FocusTriggerResult.LocationMatch -> "You've arrived at ${result.zoneName} - start a focus session?"
        is FocusTriggerResult.WifiMatch -> "You're connecting to ${result.ssid} Wi-Fi - start a focus session?"
        FocusTriggerResult.NoTrigger -> return
    }

    Row(
        modifier = Modifier
            .padding(16.dp)
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF252853))
            .clickable(onClick = onAccept)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier.clickable(onClick = onDismiss)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Dismiss",
                tint = Color.White
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchCurrentLocationForAutoCheck(
    context: Context,
    onResult: (latitude: Double, longitude: Double) -> Unit
) {
    LocationServices.getFusedLocationProviderClient(context)
        .lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                onResult(location.latitude, location.longitude)
            }
        }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenWithSheetPreview() {
    val fakeGroups = generateFakeGroups()
    HomeScreenWithSheet(
        groups = fakeGroups,
        selectedGroupId = fakeGroups.first().id,
        reopenSheetSignal = false,
        onReopenSheetHandled = {},
        onBlockerClick = {},
        onGroupAppsChange = { _, _ -> },
        onGroupScheduleChange = { _, _ -> }
    )
}