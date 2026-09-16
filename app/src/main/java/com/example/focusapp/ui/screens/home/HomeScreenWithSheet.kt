package com.example.focusapp.ui.screens.home

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.usecase.EvaluateFocusTriggerUseCase
import com.example.focusapp.domain.usecase.FocusTriggerResult
import com.example.focusapp.ui.common.rememberLocationPermissionState
import com.example.focusapp.ui.screens.session.FocusSessionSource
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SheetType { NONE, BLOCKED_APPS, LOCATION_ZONE }

/** How often the auto-suggestion check re-evaluates while Home is on screen - schedule
 *  boundaries only need minute-granularity, so there's no need for anything tighter. */
private const val AUTO_TRIGGER_CHECK_INTERVAL_MILLIS = 60_000L

/** How long Home stays on screen, unchanged, before the transition to Focus Session
 *  actually starts - reserved for a future idle animation on Home, not built yet. */
private const val FOCUS_SESSION_START_DELAY_MILLIS = 3_000L

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
    onEditLocationZoneClick: () -> Unit = {}
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
    // only Home-originated triggers (Quick Focus, the auto-suggestion banner) get
    // this; Party Mode's own "Go Focus Mode" starts its own flow immediately.
    fun startFocusSessionAfterDelay(source: FocusSessionSource) {
        scope.launch {
            delay(FOCUS_SESSION_START_DELAY_MILLIS)
            onFocusSessionStart(source)
        }
    }

    // 重開 Sheet 的 Signal 處理
    LaunchedEffect(reopenSheetSignal) {
        if (reopenSheetSignal) {
            onReopenSheetHandled()
            openSheet(
                if (reopenSheetType == "location_zone") SheetType.LOCATION_ZONE
                else SheetType.BLOCKED_APPS
            )
        }
    }

    // --- Auto-suggestion: UI-simulated only, never navigates on its own -----------
    // Re-checked (schedule + location) every time Home is composed and on a coarse
    // timer while it stays on screen - never a background service. See
    // EvaluateFocusTriggerUseCase for the shared, real-later-swappable check itself.
    var savedZone by remember { mutableStateOf<FocusZone?>(null) }
    LaunchedEffect(Unit) {
        savedZone = withContext(Dispatchers.IO) {
            FocusRepositoryProvider.get(context).getFocusZone()
        }
    }

    val permissionState = rememberLocationPermissionState()
    var currentLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(permissionState.hasPermission) {
        // Never force a permission prompt from this passive check - only read
        // location if it's already been granted elsewhere (e.g. Edit Location Zone).
        if (permissionState.hasPermission) {
            fetchCurrentLocationForAutoCheck(context) { lat, lng -> currentLatLng = lat to lng }
        }
    }

    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(AUTO_TRIGGER_CHECK_INTERVAL_MILLIS)
            tick = System.currentTimeMillis()
        }
    }

    val trigger = remember(groups, savedZone, currentLatLng, tick) {
        EvaluateFocusTriggerUseCase().execute(
            groups = groups,
            currentZones = listOfNotNull(savedZone),
            currentLatLng = currentLatLng
        )
    }

    // Resets whenever this composable is freshly entered (i.e. next real visit to
    // Home) - only suppresses re-nagging about the SAME match within this visit.
    var dismissedKey by remember { mutableStateOf<String?>(null) }
    val suggestionKey = when (trigger) {
        is FocusTriggerResult.ScheduleMatch -> "schedule:${trigger.groupId}"
        is FocusTriggerResult.LocationMatch -> "zone:${trigger.zoneId}"
        FocusTriggerResult.NoTrigger -> null
    }
    val suggestion = trigger.takeIf { suggestionKey != null && suggestionKey != dismissedKey }

    // 1. 主要畫面
    HomeScreen(
        onAvatarClick = onAvatarClick,
        onQuickFocusClick = { startFocusSessionAfterDelay(FocusSessionSource.Manual) },
        onPartyModeClick = onPartyModeClick,
        onBlockedAppCardClick = { openSheet(SheetType.BLOCKED_APPS) },
        onLocationCardClick = { openSheet(SheetType.LOCATION_ZONE) },
        onSettingsClick = onSettingsClick
    )

    if (suggestion != null && suggestionKey != null) {
        AutoFocusSuggestionBanner(
            result = suggestion,
            onAccept = {
                val source = when (suggestion) {
                    is FocusTriggerResult.ScheduleMatch ->
                        FocusSessionSource.Schedule(suggestion.groupId, suggestion.groupName)
                    is FocusTriggerResult.LocationMatch ->
                        FocusSessionSource.Location(suggestion.zoneName)
                    FocusTriggerResult.NoTrigger -> return@AutoFocusSuggestionBanner
                }
                startFocusSessionAfterDelay(source)
            },
            onDismiss = { dismissedKey = suggestionKey }
        )
    }

    // 2. 只有當 activeSheet != NONE 時才掛載 BottomSheet (徹底解決閃現與叫不出來的問題)
    if (activeSheet != SheetType.NONE) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = SheetType.NONE },
            sheetState = sheetState,
            containerColor = if (activeSheet == SheetType.BLOCKED_APPS) Color(0xFF3B3B96) else Color(
                0xFF3B3B96
            )
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
}

/** Dismissible banner for an auto-detected trigger - accepting starts a session, but nothing here ever navigates on its own. */
@Composable
private fun AutoFocusSuggestionBanner(
    result: FocusTriggerResult,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val message = when (result) {
        is FocusTriggerResult.ScheduleMatch -> "You're in your ${result.groupName} schedule - start a focus session?"
        is FocusTriggerResult.LocationMatch -> "You've arrived at ${result.zoneName} - start a focus session?"
        FocusTriggerResult.NoTrigger -> return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
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
            modifier = Modifier.weight(1f)
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
