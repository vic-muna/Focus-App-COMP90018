package com.example.focusapp.ui.screens.location

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.ui.common.ErrorBanner
import com.example.focusapp.ui.common.fetchLastKnownLocation
import com.example.focusapp.ui.common.friendlyErrorMessage
import com.example.focusapp.ui.common.rememberLocationPermissionState
import com.example.focusapp.ui.common.resolveApproxPlaceName
import com.example.focusapp.ui.components.FocusConfirmDialog
import com.example.focusapp.ui.components.FocusRangeMarker
import com.example.focusapp.ui.components.PullUpPanel
import com.example.focusapp.ui.components.PullUpPanelState
import com.example.focusapp.ui.components.SettingsTopBar
import com.example.focusapp.ui.components.rememberPullUpPanelState
import com.example.focusapp.ui.navigation.MainTab
import com.example.focusapp.ui.navigation.MainTabBar
import com.example.focusapp.ui.screens.home.BlockedAppGroup
import com.example.focusapp.ui.screens.home.generateFakeGroups
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_RADIUS_METERS = 100f

/** How much of the location list stays visible above the bottom edge when swiped down. */
private val ListPeekHeight = 200.dp

/** Gap between the add-location card and the bottom of the screen. */
private val AddCardBottomMargin = 72.dp

/**
 * A location being added, or a saved one being edited ([editingZoneId] set).
 * Its position is the map's visible center - the pin stays put and the map
 * moves under it - so [latitude]/[longitude] follow the map center, and are
 * null until it's known.
 */
private data class LocationDraft(
    val editingZoneId: String? = null,
    val name: String = "",
    val radiusMeters: Float = DEFAULT_RADIUS_METERS,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** App group ("Schedule") this location uses, if any. */
    val scheduleGroupId: String? = null,
)

/**
 * Figma: "Map Only" / "Location Focuse" / "Add new location".
 * Opens with the location group list partly covering the map; swiping down
 * on it (or its top edge) lowers it - it never leaves the screen - to show
 * more map, swiping up brings it back. Long-pressing the map starts adding
 * a location; tapping a group edits it, holding a group deletes it. The
 * card's Schedule button opens a picker for the app group the location uses.
 */
@Composable
fun LocationScreen(
    groups: List<BlockedAppGroup>,
    onSettingsClick: () -> Unit,
    onTabClick: (MainTab) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val panelState = rememberPullUpPanelState(initiallyExpanded = true)
    val permissionState = rememberLocationPermissionState()

    var zones by remember { mutableStateOf<List<FocusZone>>(emptyList()) }
    // Local-only for now: FocusZone has no "enabled" field in the data layer yet.
    val enabledZoneIds = remember { mutableStateMapOf<String, Boolean>() }

    var draft by remember { mutableStateOf<LocationDraft?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var listError by remember { mutableStateOf<String?>(null) }
    // Reverse-geocoded "Approx. ..." names, keyed by zone id (best-effort, may stay missing).
    val placeNames = remember { mutableStateMapOf<String, String>() }
    var draftPlaceName by remember { mutableStateOf<String?>(null) }

    val scheduleStorage = remember { LocationScheduleStorage(context) }
    // Picker on top of the add/edit card; [pickerGroupId] is its uncommitted choice.
    var isPickingSchedule by remember { mutableStateOf(false) }
    var pickerGroupId by remember { mutableStateOf<String?>(null) }

    // Tapping the map while the card is open asks before discarding the draft.
    var confirmDiscard by remember { mutableStateOf(false) }

    // Set by holding a group card; deleting waits for the confirm dialog.
    var pendingDelete by remember { mutableStateOf<FocusZone?>(null) }

    suspend fun reloadZones() {
        zones = withContext(Dispatchers.IO) {
            listOfNotNull(FocusRepositoryProvider.get(context).getFocusZone())
        }
    }

    LaunchedEffect(Unit) { reloadZones() }

    LaunchedEffect(zones) {
        zones.filter { it.id !in placeNames }.forEach { zone ->
            resolveApproxPlaceName(context, zone.latitude, zone.longitude)?.let { placeNames[zone.id] = it }
        }
    }

    LaunchedEffect(draft?.latitude, draft?.longitude) {
        val lat = draft?.latitude
        val lng = draft?.longitude
        draftPlaceName = if (lat != null && lng != null) resolveApproxPlaceName(context, lat, lng) else null
    }

    // There's no real map yet, so a new location's "map center" is the
    // device's current position. With a real map this becomes the camera
    // target, updated whenever the user stops dragging the map.
    val needsPosition = draft != null && draft?.latitude == null
    LaunchedEffect(needsPosition, permissionState.hasPermission) {
        if (needsPosition && permissionState.hasPermission) {
            fetchLastKnownLocation(context) { lat, lng ->
                draft = draft?.let { if (it.latitude == null) it.copy(latitude = lat, longitude = lng) else it }
            }
        }
    }

    // With a real map, also move the camera so [position] lands under the centered pin.
    fun startDraft(@Suppress("UNUSED_PARAMETER") position: Offset) {
        saveError = null
        draft = LocationDraft()
        if (!permissionState.hasPermission) permissionState.request()
    }

    // With a real map, also move the camera so the zone lands under the centered pin.
    fun startEditing(zone: FocusZone) {
        saveError = null
        draft = LocationDraft(
            editingZoneId = zone.id,
            name = zone.name,
            radiusMeters = zone.radiusMeters.coerceIn(LocationRadiusRange),
            latitude = zone.latitude,
            longitude = zone.longitude,
            scheduleGroupId = scheduleStorage.getGroupId(zone.id),
        )
    }

    fun deleteZone(zone: FocusZone) {
        listError = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) { FocusRepositoryProvider.get(context).deleteFocusZone(zone.id) }
                enabledZoneIds.remove(zone.id)
                placeNames.remove(zone.id)
                scheduleStorage.setGroupId(zone.id, null)
                reloadZones()
            } catch (e: Exception) {
                listError = friendlyErrorMessage(e, "Deleting the location")
            }
        }
    }

    fun saveDraft() {
        val current = draft ?: return
        val lat = current.latitude ?: return
        val lng = current.longitude ?: return
        val zone = FocusZone(
            id = current.editingZoneId ?: "zone_${System.currentTimeMillis()}",
            name = current.name.trim(),
            latitude = lat,
            longitude = lng,
            radiusMeters = current.radiusMeters,
        )
        scope.launch {
            try {
                withContext(Dispatchers.IO) { FocusRepositoryProvider.get(context).saveFocusZone(zone) }
                scheduleStorage.setGroupId(zone.id, current.scheduleGroupId)
                if (current.editingZoneId == null) enabledZoneIds[zone.id] = true
                // The position may have moved - resolve its name again.
                placeNames.remove(zone.id)
                reloadZones()
                draft = null
            } catch (e: Exception) {
                saveError = friendlyErrorMessage(e, "Saving the location")
            }
        }
    }

    BackHandler(enabled = draft != null) {
        if (isPickingSchedule) isPickingSchedule = false else draft = null
    }

    LocationContent(
        zones = zones,
        isZoneEnabled = { zone -> enabledZoneIds[zone.id] ?: true },
        onZoneEnabledChange = { zone, enabled -> enabledZoneIds[zone.id] = enabled },
        placeNameFor = { zone -> placeNames[zone.id] },
        onZoneClick = ::startEditing,
        onZoneLongClick = { pendingDelete = it },
        listError = listError,
        draft = draft,
        draftPlaceName = draftPlaceName,
        saveError = saveError,
        panelState = panelState,
        onMapLongPress = ::startDraft,
        onMapTap = { if (draft != null) confirmDiscard = true },
        groups = groups,
        isPickingSchedule = isPickingSchedule,
        pickerGroupId = pickerGroupId,
        onScheduleClick = {
            pickerGroupId = draft?.scheduleGroupId
            isPickingSchedule = true
        },
        onPickerSelect = { pickerGroupId = it },
        onPickerBack = { isPickingSchedule = false },
        onPickerConfirm = {
            draft = draft?.copy(scheduleGroupId = pickerGroupId)
            isPickingSchedule = false
        },
        onDraftChange = { draft = it },
        onDraftDiscard = { draft = null },
        onDraftConfirm = ::saveDraft,
        onSettingsClick = onSettingsClick,
        onTabClick = onTabClick,
    )

    if (confirmDiscard) {
        val isEditing = draft?.editingZoneId != null
        FocusConfirmDialog(
            title = if (isEditing) "Cancel editing?" else "Cancel new location?",
            message = if (isEditing) "Your changes to this location will be discarded."
            else "This location won't be saved.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            confirmColor = FocusTheme.colors.rejection,
            onConfirm = {
                confirmDiscard = false
                isPickingSchedule = false
                draft = null
            },
            onDismiss = { confirmDiscard = false },
        )
    }

    pendingDelete?.let { zone ->
        FocusConfirmDialog(
            title = "Delete \"${zone.name}\"?",
            message = "This focus location will be removed from this phone.",
            confirmLabel = "Delete",
            confirmColor = FocusTheme.colors.rejection,
            onConfirm = {
                pendingDelete = null
                deleteZone(zone)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** Stateless layout of [LocationScreen] - everything it shows comes in as parameters. */
@Composable
private fun LocationContent(
    zones: List<FocusZone>,
    isZoneEnabled: (FocusZone) -> Boolean,
    onZoneEnabledChange: (FocusZone, Boolean) -> Unit,
    placeNameFor: (FocusZone) -> String?,
    onZoneClick: (FocusZone) -> Unit,
    onZoneLongClick: (FocusZone) -> Unit,
    listError: String?,
    draft: LocationDraft?,
    draftPlaceName: String?,
    saveError: String?,
    panelState: PullUpPanelState,
    onMapLongPress: (Offset) -> Unit,
    onMapTap: (Offset) -> Unit,
    groups: List<BlockedAppGroup>,
    isPickingSchedule: Boolean,
    pickerGroupId: String?,
    onScheduleClick: () -> Unit,
    onPickerSelect: (String?) -> Unit,
    onPickerBack: () -> Unit,
    onPickerConfirm: () -> Unit,
    onDraftChange: (LocationDraft) -> Unit,
    onDraftDiscard: () -> Unit,
    onDraftConfirm: () -> Unit,
    onSettingsClick: () -> Unit,
    onTabClick: (MainTab) -> Unit,
) {
    val colors = FocusTheme.colors
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    // The card covers the bottom of the map; the pin centers on what's left above it.
    var addCardHeight by remember { mutableStateOf(0.dp) }
    val hiddenBottom = if (draft != null) addCardHeight + AddCardBottomMargin else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {
        MapPlaceholder(
            onLongPress = onMapLongPress,
            onTap = onMapTap,
            contentPadding = PaddingValues(bottom = hiddenBottom),
        ) {
            if (draft != null) {
                val ringDiameter by animateDpAsState(
                    targetValue = (draft.radiusMeters * 2 * PLACEHOLDER_DP_PER_METER).dp,
                    label = "zoneRingDiameter",
                )
                FocusRangeMarker(
                    color = colors.mapZoneNew,
                    diameter = ringDiameter,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        SettingsTopBar(onSettingsClick = onSettingsClick)

        // Hidden while adding a location, so the card and the centered pin have the map to themselves.
        if (draft == null) PullUpPanel(
            state = panelState,
            peekHeight = ListPeekHeight,
            collapseOnBack = false,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    // No top padding: the panel's 24 dp drag strip already sits above the list.
                    .padding(start = 32.dp, end = 32.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listError?.let { ErrorBanner(message = it) }
                if (zones.isEmpty()) {
                    Text(
                        text = "No focus locations yet.\nLong-press the map to add one.",
                        style = FocusTheme.typography.body,
                        color = colors.onSurfaceMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    )
                }
                zones.forEach { zone ->
                    LocationGroupCard(
                        name = zone.name,
                        // Falls back to the radius until (or if never) a place name resolves.
                        subtitle = placeNameFor(zone)?.let { "Approx. $it" }
                            ?: "Effective range: ${zone.radiusMeters.toInt()} m",
                        latitude = zone.latitude,
                        longitude = zone.longitude,
                        enabled = isZoneEnabled(zone),
                        onEnabledChange = { onZoneEnabledChange(zone, it) },
                        onClick = { onZoneClick(zone) },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onZoneLongClick(zone)
                        },
                    )
                }
            }
        }

        if (draft == null) {
            MainTabBar(
                selectedTab = MainTab.LOCATION,
                onTabClick = onTabClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 32.dp, end = 32.dp, bottom = AddCardBottomMargin)
                    .onSizeChanged { addCardHeight = with(density) { it.height.toDp() } },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                saveError?.let { ErrorBanner(message = it) }
                if (isPickingSchedule) ScheduleGroupPickerCard(
                    groups = groups,
                    selectedGroupId = pickerGroupId,
                    onSelect = onPickerSelect,
                    onBack = onPickerBack,
                    onConfirm = onPickerConfirm,
                ) else AddLocationCard(
                    locationLabel = when {
                        draftPlaceName != null -> "Approx. $draftPlaceName"
                        draft.editingZoneId != null -> "Saved location"
                        draft.latitude != null -> "Approx. current location"
                        else -> "Finding your location…"
                    },
                    latitude = draft.latitude,
                    longitude = draft.longitude,
                    name = draft.name,
                    onNameChange = { onDraftChange(draft.copy(name = it)) },
                    radiusMeters = draft.radiusMeters,
                    onRadiusChange = { onDraftChange(draft.copy(radiusMeters = it)) },
                    onScheduleClick = onScheduleClick,
                    scheduleName = groups.find { it.id == draft.scheduleGroupId }?.name,
                    scheduleAppCount = groups.find { it.id == draft.scheduleGroupId }?.apps?.size ?: 0,
                    onClose = onDraftDiscard,
                    onConfirm = onDraftConfirm,
                )
            }
        }
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun LocationContentListPreview() {
    FocusAppTheme {
        LocationContent(
            zones = previewZones,
            isZoneEnabled = { it.id == "zone_1" },
            onZoneEnabledChange = { _, _ -> },
            placeNameFor = { if (it.id == "zone_1") "FBE Library" else null },
            onZoneClick = {},
            onZoneLongClick = {},
            listError = null,
            draft = null,
            draftPlaceName = null,
            saveError = null,
            panelState = rememberPullUpPanelState(initiallyExpanded = true),
            onMapLongPress = {},
            onMapTap = {},
            groups = generateFakeGroups(),
            isPickingSchedule = false,
            pickerGroupId = null,
            onScheduleClick = {},
            onPickerSelect = {},
            onPickerBack = {},
            onPickerConfirm = {},
            onDraftChange = {},
            onDraftDiscard = {},
            onDraftConfirm = {},
            onSettingsClick = {},
            onTabClick = {},
        )
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun LocationContentAddPreview() {
    FocusAppTheme {
        LocationContent(
            zones = previewZones,
            isZoneEnabled = { true },
            onZoneEnabledChange = { _, _ -> },
            placeNameFor = { if (it.id == "zone_1") "FBE Library" else null },
            onZoneClick = {},
            onZoneLongClick = {},
            listError = null,
            draft = LocationDraft(latitude = -37.8136, longitude = 144.9631),
            draftPlaceName = "Library at the Dock",
            saveError = null,
            panelState = rememberPullUpPanelState(),
            onMapLongPress = {},
            onMapTap = {},
            groups = generateFakeGroups(),
            isPickingSchedule = false,
            pickerGroupId = null,
            onScheduleClick = {},
            onPickerSelect = {},
            onPickerBack = {},
            onPickerConfirm = {},
            onDraftChange = {},
            onDraftDiscard = {},
            onDraftConfirm = {},
            onSettingsClick = {},
            onTabClick = {},
        )
    }
}

private val previewZones = listOf(
    FocusZone("zone_1", "Library", -37.7983, 144.9610, 100f),
    FocusZone("zone_2", "Cafe", -37.8011, 144.9590, 150f),
)
