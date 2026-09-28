package com.example.focusapp.ui.screens.home

import com.example.focusapp.data.wifi.WifiHistoryStorage
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.repository.FocusRepositoryProvider
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
import kotlinx.coroutines.withContext
import androidx.annotation.DrawableRes
import com.example.focusapp.R


/** How often the auto-suggestion check re-evaluates while Home is on screen - schedule
 *  boundaries only need minute-granularity, so there's no need for anything tighter. */
private const val AUTO_TRIGGER_CHECK_INTERVAL_MILLIS = 60_000L

/** Each suggestion banner closes itself after this long. */
private const val BANNER_AUTO_HIDE_MILLIS = 5_000L

@Composable
fun HomeScreenWithSheet(
    // Time Focus's schedule groups - only read here, for the schedule banner.
    groups: List<BlockedAppGroup>,
    // The Wi-Fi tab's networks that are switched on - only read here, for the Wi-Fi banner.
    wifiSsids: List<String> = emptyList(),
    // The picked background theme's Home art (see BackgroundThemes).
    @DrawableRes dashboardArt: Int = R.drawable.img_home_dashboard,
    onFocusSessionStart: (FocusSessionSource) -> Unit = {},
    onScheduleBannerClick: (groupId: String) -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onPartyModeClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLocationTabClick: () -> Unit = {},
    onScheduleTabClick: () -> Unit = {},
    onWifiTabClick: () -> Unit = {},
) {
    val context = LocalContext.current

    fun onTabClick(tab: MainTab) {
        when (tab) {
            MainTab.HOME -> Unit
            MainTab.LOCATION -> onLocationTabClick()
            MainTab.SCHEDULE -> onScheduleTabClick()
            MainTab.WIFI_SOURCE -> onWifiTabClick()
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

    // --- Wi-Fi trigger: same "UI-simulated only" polling approach as the
    // location trigger above, over the Wi-Fi tab's switched-on networks. ---
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(AUTO_TRIGGER_CHECK_INTERVAL_MILLIS)
            tick = System.currentTimeMillis()
        }
    }

    // Checked even with no networks switched on, so every network the phone
    // is on gets recorded for the Wi-Fi tab's "Known Wi-Fi" list.
    val wifiHistory = remember { WifiHistoryStorage(context) }
    var currentWifiSsid by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(tick) {
        currentWifiSsid = withContext(Dispatchers.IO) { getCurrentWifiSsid(context) }
        currentWifiSsid?.let { wifiHistory.remember(it) }
    }

    // [David Shiau, 2026-09-26] All matching triggers, not just the first -
    // so the schedule banner and the location banner can both show at once.
    val triggers = remember(groups, savedZone, currentLatLng, wifiSsids, currentWifiSsid, tick) {
        EvaluateFocusTriggerUseCase().executeAll(
            groups = groups,
            currentZones = listOfNotNull(savedZone),
            currentLatLng = currentLatLng,
            taggedWifiSsids = wifiSsids,
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
            .background(FocusTheme.colors.background)
    ) {
        // 1. 主要畫面
        // The new Figma Home has no Party Mode entry, so onPartyModeClick is
        // currently unused here - kept so NavGraph's wiring doesn't change.
        HomeScreen(
            dashboardArt = dashboardArt,
            selectedTab = MainTab.HOME,
            onSettingsClick = onSettingsClick,
            onDashboardClick = onAvatarClick,
            onQuickFocusClick = { onQuickFocusClick() },
            onTabClick = { tab -> onTabClick(tab) }
        )

        if (suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                suggestions.forEach { (suggestionKey, suggestion) ->
                    key(suggestionKey) {
                        // Closes itself after a few seconds, same as tapping its X.
                        LaunchedEffect(Unit) {
                            delay(BANNER_AUTO_HIDE_MILLIS)
                            dismissedKeys = dismissedKeys + suggestionKey
                        }
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
                                        FocusSessionSource.Location(suggestion.zoneName, suggestion.zoneId)
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
    val colors = FocusTheme.colors
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
    HomeScreenWithSheet(groups = generateFakeGroups())
}