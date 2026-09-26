package com.example.focusapp.ui.screens.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.data.wifi.WifiTriggerStorage
import com.example.focusapp.data.wifi.getCurrentWifiSsid
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.usecase.EvaluateFocusTriggerUseCase
import com.example.focusapp.domain.usecase.FocusTriggerResult
import com.example.focusapp.ui.common.rememberLocationPermissionState
import com.example.focusapp.ui.navigation.MainTab
import com.example.focusapp.ui.screens.session.FocusSessionSource
import com.example.focusapp.ui.theme.FocusTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SheetType { NONE, BLOCKED_APPS, LOCATION_ZONE }

/** How often the auto-suggestion check re-evaluates while Home is on screen - schedule
 *  boundaries only need minute-granularity, so there's no need for anything tighter. */
private const val AUTO_TRIGGER_CHECK_INTERVAL_MILLIS = 60_000L

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
    onFocusSessionStart: (FocusSessionSource) -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onPartyModeClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onEditLocationZoneClick: () -> Unit = {},
    onLocationTabClick: () -> Unit = {},
    onBlockedAppsTabClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ModalBottomSheet 專用的 State
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var activeSheet by remember { mutableStateOf(SheetType.NONE) }

    // Which bottom-nav tab the open sheet belongs to, so the nav indicator
    // highlights it while the sheet is up.
    var sheetTab by remember { mutableStateOf(MainTab.HOME) }
    val selectedTab = if (activeSheet == SheetType.NONE) MainTab.HOME else sheetTab

    fun openSheet(type: SheetType, tab: MainTab) {
        sheetTab = tab
        activeSheet = type
    }

    fun onTabClick(tab: MainTab) {
        when (tab) {
            MainTab.HOME -> Unit
            MainTab.LOCATION -> onLocationTabClick()
            MainTab.BLOCKED_APPS -> onBlockedAppsTabClick()
        }
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
            onFocusSessionStart(source)
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
            when (reopenSheetType) {
                "location_zone" -> openSheet(SheetType.LOCATION_ZONE, MainTab.LOCATION)
                else -> openSheet(SheetType.BLOCKED_APPS, MainTab.BLOCKED_APPS)
            }
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
    val wifiTriggerStorage = remember { WifiTriggerStorage(context) }
    var wifiTriggerEnabled by remember { mutableStateOf(false) }
    var taggedWifiSsids by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        wifiTriggerEnabled = withContext(Dispatchers.IO) { wifiTriggerStorage.isEnabled() }
        taggedWifiSsids = withContext(Dispatchers.IO) { wifiTriggerStorage.getTaggedSsids() }
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

    val trigger = remember(groups, savedZone, currentLatLng, taggedWifiSsids, currentWifiSsid, tick) {
        EvaluateFocusTriggerUseCase().execute(
            groups = groups,
            currentZones = listOfNotNull(savedZone),
            currentLatLng = currentLatLng,
            taggedWifiSsids = taggedWifiSsids,
            currentWifiSsid = currentWifiSsid
        )
    }

    var dismissedKey by remember { mutableStateOf<String?>(null) }
    val suggestionKey = when (trigger) {
        is FocusTriggerResult.ScheduleMatch -> "schedule:${trigger.groupId}"
        is FocusTriggerResult.LocationMatch -> "zone:${trigger.zoneId}"
        is FocusTriggerResult.WifiMatch -> "wifi:${trigger.ssid}"
        FocusTriggerResult.NoTrigger -> null
    }
    val suggestion = trigger.takeIf { suggestionKey != null && suggestionKey != dismissedKey }

    // 最外層強制全螢幕 Box，阻斷返回 Home 時 BottomSheet 或繪製節點導致的尺寸縮放跳動
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusTheme.colors.background)
    ) {
        // 1. 主要畫面
        // The new Figma Home has no Party Mode entry, so onPartyModeClick is
        // currently unused here - kept so NavGraph's wiring doesn't change.
        HomeScreen(
            selectedTab = selectedTab,
            onSettingsClick = onSettingsClick,
            onDashboardClick = onAvatarClick,
            onQuickFocusClick = { onQuickFocusClick() },
            onTabClick = { tab -> onTabClick(tab) }
        )

        if (suggestion != null && suggestionKey != null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                AutoFocusSuggestionBanner(
                    result = suggestion,
                    onAccept = {
                        val source = when (suggestion) {
                            is FocusTriggerResult.ScheduleMatch ->
                                FocusSessionSource.Schedule(suggestion.groupId, suggestion.groupName)
                            is FocusTriggerResult.LocationMatch ->
                                FocusSessionSource.Location(suggestion.zoneName)
                            is FocusTriggerResult.WifiMatch ->
                                FocusSessionSource.Wifi(suggestion.ssid)
                            FocusTriggerResult.NoTrigger -> return@AutoFocusSuggestionBanner
                        }
                        startFocusSessionIfPermitted(source)
                    },
                    onDismiss = { dismissedKey = suggestionKey }
                )
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
                        onBlockerClick = {
                            closeSheet { onBlockerClick() }
                        }
                    )

                    SheetType.LOCATION_ZONE -> LocationZoneSheetContent(
                        onEditClick = { closeSheet { onEditLocationZoneClick() } }
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

/** Dismissible banner for an auto-detected trigger - accepting starts a session, but nothing here ever navigates on its own. */
@Composable
private fun AutoFocusSuggestionBanner(
    result: FocusTriggerResult,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FocusTheme.colors
    val message = when (result) {
        is FocusTriggerResult.ScheduleMatch -> "You're in your ${result.groupName} schedule - start a focus session?"
        is FocusTriggerResult.LocationMatch -> "You've arrived at ${result.zoneName} - start a focus session?"
        is FocusTriggerResult.WifiMatch -> "You're connecting to ${result.ssid} Wi-Fi - start a focus session?"
        FocusTriggerResult.NoTrigger -> return
    }

    Row(
        modifier = Modifier
            .padding(16.dp)
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .clickable(onClick = onAccept)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = FocusTheme.typography.body,
            color = colors.onSurface,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier.clickable(onClick = onDismiss)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Dismiss",
                tint = colors.onSurface
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