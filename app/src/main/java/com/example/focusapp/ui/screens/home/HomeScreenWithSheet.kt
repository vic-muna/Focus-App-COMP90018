package com.example.focusapp.ui.screens.home

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.example.focusapp.data.blocking.BlockedAppGroup
import com.example.focusapp.ui.common.previewGroups
import androidx.activity.compose.BackHandler
import com.example.focusapp.data.blocking.AppItem
import com.example.focusapp.ui.components.card.AppPickerCard
import com.example.focusapp.ui.components.card.FlyCardOverlay
import com.example.focusapp.ui.components.card.consumeTaps
import com.example.focusapp.ui.common.AccessibilityPermissionDialog
import android.content.Intent
import android.provider.Settings
import com.example.focusapp.data.usagestats.hasUsageAccessPermission


/** How often Home re-checks for a schedule, location or Wi-Fi match. */
private const val AUTO_TRIGGER_CHECK_INTERVAL_MILLIS = 60_000L

/** Each suggestion banner closes itself after this long. */
private const val BANNER_AUTO_HIDE_MILLIS = 5_000L

@Composable
fun HomeScreenWithSheet(
    // Time Focus's schedule groups - only read here, for the schedule banner.
    groups: List<BlockedAppGroup>,
    // The Wi-Fi tab's networks that are switched on - only read here, for the Wi-Fi banner.
    wifiSsids: List<String> = emptyList(),
    // The apps Quick Focus blocks, and how to save a new pick.
    quickFocusApps: List<AppItem> = emptyList(),
    onQuickFocusAppsChange: (List<AppItem>) -> Unit = {},
    // The picked background theme's Home art (see BackgroundThemes).
    @DrawableRes dashboardArt: Int = R.drawable.img_home_dashboard,
    // False for art without its own frame (the shop backgrounds' thumbnails) - see BackgroundTheme.homeArtHasFrame.
    dashboardArtHasFrame: Boolean = true,
    // [Claude, 2026-10-04] The signed-in username (null for a guest, or while it's still
    // loading) - see NavGraph.kt's call site, which is the only thing that reads
    // AccountManager.account. "Hi! User" used to be literal - GreetingHeader always took the
    // default, since nothing above it ever passed a real name down this far.
    userName: String? = null,
    onFocusSessionStart: (FocusSessionSource) -> Unit = {},
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


    // Blocking only works after the user turns on the accessibility service in Android's settings.
    val isAccessibilityEnabled by AccessibilityBridge.isServiceConnected.collectAsState()
    var showAccessibilityPermissionDialog by remember { mutableStateOf(false) }

    // Used by Quick Focus and by a banner's "start": ask for the permission first if it's missing.
    fun startFocusSessionIfPermitted(source: FocusSessionSource) {
        if (isAccessibilityEnabled) {
            onFocusSessionStart(source)
        } else {
            showAccessibilityPermissionDialog = true
        }
    }

    // --- Quick Focus: every time, pick the apps to block (last pick is pre-ticked), then start ---
    var showQuickFocusPicker by remember { mutableStateOf(false) }
    // The time slot whose "Usage access needed" card is open, or null.
    var usageAccessGroupName by remember { mutableStateOf<String?>(null) }

    fun onQuickFocusClick() {
        // Ask for the permission first, so the apps aren't picked for nothing.
        if (isAccessibilityEnabled) showQuickFocusPicker = true
        else showAccessibilityPermissionDialog = true
    }

    BackHandler(enabled = showQuickFocusPicker || usageAccessGroupName != null) {
        showQuickFocusPicker = false
        usageAccessGroupName = null
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

    // --- Wi-Fi trigger: checks the Wi-Fi tab's switched-on networks. ---
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(AUTO_TRIGGER_CHECK_INTERVAL_MILLIS)
            tick = System.currentTimeMillis()
        }
    }

    var currentWifiSsid by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(tick, wifiSsids.isNotEmpty()) {
        currentWifiSsid = if (wifiSsids.isEmpty()) null
        else withContext(Dispatchers.IO) { getCurrentWifiSsid(context) }
    }

    // Every matching trigger, so several banners can show at once.
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

    // Full-screen box, so Home doesn't jump in size when coming back to it.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusTheme.colors.background)
    ) {

        HomeScreen(
            userName = userName ?: "Guest",
            dashboardArt = dashboardArt,
            dashboardArtHasFrame = dashboardArtHasFrame,
            selectedTab = MainTab.HOME,
            onSettingsClick = onSettingsClick,
            onPartyClick = onPartyModeClick,
            onDashboardClick = onAvatarClick,
            onQuickFocusClick = ::onQuickFocusClick,
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
                                    // A time slot doesn't start a session. Its limits need App Blocking
                                    // and Usage access: ask for whichever is off, else open the Time Focus tab.
                                    is FocusTriggerResult.ScheduleMatch -> {
                                        when {
                                            !isAccessibilityEnabled -> showAccessibilityPermissionDialog = true
                                            !hasUsageAccessPermission(context) -> usageAccessGroupName = suggestion.groupName
                                            else -> onScheduleTabClick()
                                        }
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

        if (showQuickFocusPicker) {
            FlyCardOverlay(onOutsideClick = { showQuickFocusPicker = false }) {
                AppPickerCard(
                    title = "Quick Focus Apps",
                    savedApps = quickFocusApps,
                    confirmDescription = "Start focusing",
                    onConfirm = { apps ->
                        showQuickFocusPicker = false
                        onQuickFocusAppsChange(apps)
                        onFocusSessionStart(FocusSessionSource.Manual)
                    },
                    onClose = { showQuickFocusPicker = false },
                    modifier = Modifier.consumeTaps(),
                )
            }
        }

        usageAccessGroupName?.let { groupName ->
            FlyCardOverlay(onOutsideClick = { usageAccessGroupName = null }) {
                UsageAccessCard(
                    groupName = groupName,
                    onOpenSettings = {
                        usageAccessGroupName = null
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    onClose = { usageAccessGroupName = null },
                    modifier = Modifier.consumeTaps(),
                )
            }
        }

        if (showAccessibilityPermissionDialog) {
            AccessibilityPermissionDialog(onDismiss = { showAccessibilityPermissionDialog = false })
        }
    }
}

/**
 * A banner for a detected trigger: "start" begins a focus session
 * (location/Wi-Fi) or opens the Time Focus tab (schedule).
 */
@Composable
private fun AutoFocusSuggestionBanner(
    result: FocusTriggerResult,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FocusTheme.colors
    val message = when (result) {
        is FocusTriggerResult.ScheduleMatch -> "Your ${result.groupName} time slot is on - apps over their limit will be blocked"
        is FocusTriggerResult.LocationMatch -> "You've arrived at ${result.zoneName} - start a focus session?"
        is FocusTriggerResult.WifiMatch -> "You're on ${result.ssid} Wi-Fi - its apps are blocked. Start a focus session to record it?"
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
    HomeScreenWithSheet(groups = previewGroups())
}