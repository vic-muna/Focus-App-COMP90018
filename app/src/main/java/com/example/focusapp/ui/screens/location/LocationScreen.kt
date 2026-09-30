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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.blocking.AppItem
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.data.sensor.currentLocationFlow
import com.example.focusapp.data.sensor.startGPSUpdates
import com.example.focusapp.data.sensor.stopGPSUpdates
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.ui.common.ErrorBanner
import com.example.focusapp.ui.common.fetchLastKnownLocation
import com.example.focusapp.ui.common.friendlyErrorMessage
import com.example.focusapp.ui.common.pickedApps
import com.example.focusapp.ui.common.rememberInstalledApps
import com.example.focusapp.ui.common.rememberLocationPermissionState
import com.example.focusapp.ui.common.resolveApproxPlaceName
import com.example.focusapp.ui.common.toggle
import com.example.focusapp.ui.components.button.ConfirmButton
import com.example.focusapp.ui.components.card.AppSelectCard
import com.example.focusapp.ui.components.card.DeleteDialog
import com.example.focusapp.ui.components.card.FlyCardOverlay
import com.example.focusapp.ui.components.card.consumeTaps
import com.example.focusapp.ui.navigation.MainTab
import com.example.focusapp.ui.navigation.MainTabBar
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusSpacing
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_RADIUS_METERS = 100f

/** How much of the location list stays visible above the bottom edge when swiped down. */
private val ListPeekHeight = 200.dp

/**
 * A location being added, or a saved one being edited ([editingZoneId] set).
 * Its position is determined by tapping on the interactive map or from GPS/last known location.
 */
private data class LocationDraft(
    val editingZoneId: String? = null,
    val name: String = "",
    val radiusMeters: Float = DEFAULT_RADIUS_METERS,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val blockedApps: List<AppItem> = emptyList(),
)

/**
 * Figma: "Map Only" / "Location Focus" / "Add new location".
 * Displays an interactable map background showing user's current location and focus locations.
 * Users can tap anywhere on the map to set a pin marker and extract the latitude/longitude.
 */
@Composable
fun LocationScreen(
    onTabClick: (MainTab) -> Unit,
    blockedAppsFor: (zoneId: String) -> List<AppItem> = { emptyList() },
    onZoneBlockedAppsChange: (zone: FocusZone, apps: List<AppItem>) -> Unit = { _, _ -> },
    onZoneDeleted: (zoneId: String) -> Unit = {},
    onZonesLoaded: (zoneIds: Set<String>) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val panelState = rememberPullUpPanelState(initiallyExpanded = true)
    val permissionState = rememberLocationPermissionState()

    val currentLocation by currentLocationFlow.collectAsState()

    DisposableEffect(permissionState.hasPermission) {
        if (permissionState.hasPermission) {
            startGPSUpdates(context)
        }
        onDispose {
            stopGPSUpdates(context)
        }
    }

    var zones by remember { mutableStateOf<List<FocusZone>>(emptyList()) }
    val enabledZoneIds = remember { mutableStateMapOf<String, Boolean>() }

    var draft by remember { mutableStateOf<LocationDraft?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var listError by remember { mutableStateOf<String?>(null) }
    val placeNames = remember { mutableStateMapOf<String, String>() }
    var draftPlaceName by remember { mutableStateOf<String?>(null) }

    var pendingDelete by remember { mutableStateOf<FocusZone?>(null) }

    var showAppPicker by remember { mutableStateOf(false) }
    val installedApps = rememberInstalledApps(shouldLoad = showAppPicker)
    var pickerSelection by remember { mutableStateOf<Set<String>>(emptySet()) }

    suspend fun reloadZones() {
        zones = withContext(Dispatchers.IO) {
            listOfNotNull(FocusRepositoryProvider.get(context).getFocusZone())
        }
        onZonesLoaded(zones.map { it.id }.toSet())
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

    val needsPosition = draft != null && draft?.latitude == null
    LaunchedEffect(needsPosition, permissionState.hasPermission, currentLocation) {
        if (needsPosition) {
            if (currentLocation != null) {
                draft = draft?.let { if (it.latitude == null) it.copy(latitude = currentLocation!!.first, longitude = currentLocation!!.second) else it }
            } else if (permissionState.hasPermission) {
                fetchLastKnownLocation(context) { lat, lng ->
                    draft = draft?.let { if (it.latitude == null) it.copy(latitude = lat, longitude = lng) else it }
                }
            }
        }
    }

    fun startDraft(@Suppress("UNUSED_PARAMETER") position: Offset) {
        saveError = null
        draft = LocationDraft(
            latitude = currentLocation?.first,
            longitude = currentLocation?.second,
        )
        if (!permissionState.hasPermission) permissionState.request()
    }

    fun onMapClickLocation(lat: Double, lng: Double) {
        saveError = null
        draft = draft?.copy(latitude = lat, longitude = lng) ?: LocationDraft(latitude = lat, longitude = lng)
        if (!permissionState.hasPermission) permissionState.request()
    }

    fun startEditing(zone: FocusZone) {
        saveError = null
        draft = LocationDraft(
            editingZoneId = zone.id,
            name = zone.name,
            radiusMeters = zone.radiusMeters.coerceIn(LocationRadiusRange),
            latitude = zone.latitude,
            longitude = zone.longitude,
            blockedApps = blockedAppsFor(zone.id),
        )
    }

    fun deleteZone(zone: FocusZone) {
        listError = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) { FocusRepositoryProvider.get(context).deleteFocusZone(zone.id) }
                enabledZoneIds.remove(zone.id)
                placeNames.remove(zone.id)
                onZoneDeleted(zone.id)
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
                if (current.editingZoneId == null) enabledZoneIds[zone.id] = true
                onZoneBlockedAppsChange(zone, current.blockedApps)
                placeNames.remove(zone.id)
                reloadZones()
                draft = null
            } catch (e: Exception) {
                saveError = friendlyErrorMessage(e, "Saving the location")
            }
        }
    }

    BackHandler(enabled = draft != null) { draft = null }
    BackHandler(enabled = showAppPicker) { showAppPicker = false }

    Box(modifier = Modifier.fillMaxSize()) {
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
            currentLocation = currentLocation,
            onMapLongPress = ::startDraft,
            onMapClickLocation = ::onMapClickLocation,
            onDraftChange = { draft = it },
            onDraftDiscard = { draft = null },
            onDraftConfirm = ::saveDraft,
            onBlockedAppsClick = {
                pickerSelection = draft?.blockedApps.orEmpty().map { it.packageName }.toSet()
                showAppPicker = true
            },
            onTabClick = onTabClick,
        )

        val pickerDraft = draft
        if (showAppPicker && pickerDraft != null) {
            FlyCardOverlay(onOutsideClick = { showAppPicker = false }) {
                AppSelectCard(
                    title = "Blocked Apps",
                    apps = installedApps,
                    selectedPackages = pickerSelection,
                    onToggleApp = { pkg -> pickerSelection = pickerSelection.toggle(pkg) },
                    onClose = { showAppPicker = false },
                    actionButton = {
                        ConfirmButton(
                            onClick = {
                                draft = pickerDraft.copy(blockedApps = pickedApps(installedApps, pickerSelection, pickerDraft.blockedApps))
                                showAppPicker = false
                            },
                            contentDescription = "Save blocked apps",
                        )
                    },
                    modifier = Modifier.consumeTaps(),
                )
            }
        }
    }
    pendingDelete?.let { zone ->
        DeleteDialog(
            name = zone.name,
            message = "This focus location will be removed from this phone.",
            onDelete = {
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
    currentLocation: Pair<Double, Double>?,
    onMapLongPress: (Offset) -> Unit,
    onMapClickLocation: (latitude: Double, longitude: Double) -> Unit,
    onDraftChange: (LocationDraft) -> Unit,
    onDraftDiscard: () -> Unit,
    onDraftConfirm: () -> Unit,
    onBlockedAppsClick: () -> Unit,
    onTabClick: (MainTab) -> Unit,
) {
    val colors = FocusTheme.colors
    val density = LocalDensity.current

    var addCardHeight by remember { mutableStateOf(0.dp) }
    val hiddenBottom = if (draft != null) addCardHeight + FocusSpacing.ScreenBottom else 0.dp

    val pinLocation = if (draft?.latitude != null && draft.longitude != null) {
        Pair(draft.latitude, draft.longitude)
    } else null

    Box(modifier = Modifier.fillMaxSize()) {
        MapPlaceholder(
            onLongPress = onMapLongPress,
            contentPadding = PaddingValues(bottom = hiddenBottom),
            currentLocation = currentLocation,
            pinLocation = pinLocation,
            pinRadiusMeters = draft?.radiusMeters ?: DEFAULT_RADIUS_METERS,
            zones = zones,
            onLocationClick = onMapClickLocation,
        ) {
            if (draft != null && draft.latitude == null) {
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

        if (draft == null) PullUpPanel(
            state = panelState,
            peekHeight = ListPeekHeight,
            collapseOnBack = false,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 32.dp, end = 32.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listError?.let { ErrorBanner(message = it) }
                if (zones.isEmpty()) {
                    Text(
                        text = "No focus locations yet.\nTap or long-press the map to add one.",
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
                        subtitle = placeNameFor(zone)?.let { "Approx. $it" }
                            ?: "Effective range: ${zone.radiusMeters.toInt()} m",
                        latitude = zone.latitude,
                        longitude = zone.longitude,
                        enabled = isZoneEnabled(zone),
                        onEnabledChange = { onZoneEnabledChange(zone, it) },
                        onClick = { onZoneClick(zone) },
                        onLongClick = { onZoneLongClick(zone) },
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
                    .padding(bottom = FocusSpacing.ScreenBottom),
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 32.dp, end = 32.dp, bottom = FocusSpacing.ScreenBottom)
                    .onSizeChanged { addCardHeight = with(density) { it.height.toDp() } },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                saveError?.let { ErrorBanner(message = it) }
                AddLocationCard(
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
                    onBlockedAppsClick = onBlockedAppsClick,
                    blockedAppIcons = draft.blockedApps.map { it.icon },
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
            currentLocation = Pair(-37.8136, 144.9631),
            onMapLongPress = {},
            onMapClickLocation = { _, _ -> },
            onDraftChange = {},
            onDraftDiscard = {},
            onDraftConfirm = {},
            onBlockedAppsClick = {},
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
            currentLocation = Pair(-37.8136, 144.9631),
            onMapLongPress = {},
            onMapClickLocation = { _, _ -> },
            onDraftChange = {},
            onDraftDiscard = {},
            onDraftConfirm = {},
            onBlockedAppsClick = {},
            onTabClick = {},
        )
    }
}

private val previewZones = listOf(
    FocusZone("zone_1", "Library", -37.7983, 144.9610, 100f),
    FocusZone("zone_2", "Cafe", -37.8011, 144.9590, 150f),
)
