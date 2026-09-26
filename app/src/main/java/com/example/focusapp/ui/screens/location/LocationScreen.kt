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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.ui.common.ErrorBanner
import com.example.focusapp.ui.common.fetchLastKnownLocation
import com.example.focusapp.ui.common.friendlyErrorMessage
import com.example.focusapp.ui.common.rememberLocationPermissionState
import com.example.focusapp.ui.components.FocusRangeMarker
import com.example.focusapp.ui.components.PullUpPanel
import com.example.focusapp.ui.components.PullUpPanelState
import com.example.focusapp.ui.components.SettingsTopBar
import com.example.focusapp.ui.components.rememberPullUpPanelState
import com.example.focusapp.ui.navigation.MainTab
import com.example.focusapp.ui.navigation.MainTabBar
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
 * A location being added. Its position is always the map's visible center
 * (the pin stays put and the map moves under it), so only the card's fields
 * live here.
 */
private data class LocationDraft(
    val name: String = "",
    val radiusMeters: Float = DEFAULT_RADIUS_METERS,
)

/**
 * Figma: "Map Only" / "Location Focuse" / "Add new location".
 * Opens with the location group list partly covering the map; swiping down
 * on it (or its top edge) lowers it - it never leaves the screen - to show
 * more map, swiping up brings it back. Long-pressing the map starts adding
 * a location.
 */
@Composable
fun LocationScreen(
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
    var currentLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }

    suspend fun reloadZones() {
        zones = withContext(Dispatchers.IO) {
            listOfNotNull(FocusRepositoryProvider.get(context).getFocusZone())
        }
    }

    LaunchedEffect(Unit) { reloadZones() }

    // There's no real map yet, so the "map center" is the device's current
    // position. With a real map this becomes the camera target, updated
    // whenever the user stops dragging the map.
    LaunchedEffect(draft != null, permissionState.hasPermission) {
        if (draft != null && permissionState.hasPermission) {
            fetchLastKnownLocation(context) { lat, lng -> currentLatLng = lat to lng }
        }
    }

    // With a real map, also move the camera so [position] lands under the centered pin.
    fun startDraft(@Suppress("UNUSED_PARAMETER") position: Offset) {
        saveError = null
        draft = LocationDraft()
        if (!permissionState.hasPermission) permissionState.request()
    }

    fun saveDraft() {
        val current = draft ?: return
        val (lat, lng) = currentLatLng ?: return
        val zone = FocusZone(
            id = "zone_${System.currentTimeMillis()}",
            name = current.name.trim(),
            latitude = lat,
            longitude = lng,
            radiusMeters = current.radiusMeters,
        )
        scope.launch {
            try {
                withContext(Dispatchers.IO) { FocusRepositoryProvider.get(context).saveFocusZone(zone) }
                enabledZoneIds[zone.id] = true
                reloadZones()
                draft = null
            } catch (e: Exception) {
                saveError = friendlyErrorMessage(e, "Saving the location")
            }
        }
    }

    BackHandler(enabled = draft != null) { draft = null }

    LocationContent(
        zones = zones,
        isZoneEnabled = { zone -> enabledZoneIds[zone.id] ?: true },
        onZoneEnabledChange = { zone, enabled -> enabledZoneIds[zone.id] = enabled },
        draft = draft,
        draftLatLng = currentLatLng,
        saveError = saveError,
        panelState = panelState,
        onMapLongPress = ::startDraft,
        onDraftChange = { draft = it },
        onDraftDiscard = { draft = null },
        onDraftConfirm = ::saveDraft,
        onSettingsClick = onSettingsClick,
        onTabClick = onTabClick,
    )
}

/** Stateless layout of [LocationScreen] - everything it shows comes in as parameters. */
@Composable
private fun LocationContent(
    zones: List<FocusZone>,
    isZoneEnabled: (FocusZone) -> Boolean,
    onZoneEnabledChange: (FocusZone, Boolean) -> Unit,
    draft: LocationDraft?,
    draftLatLng: Pair<Double, Double>?,
    saveError: String?,
    panelState: PullUpPanelState,
    onMapLongPress: (Offset) -> Unit,
    onDraftChange: (LocationDraft) -> Unit,
    onDraftDiscard: () -> Unit,
    onDraftConfirm: () -> Unit,
    onSettingsClick: () -> Unit,
    onTabClick: (MainTab) -> Unit,
) {
    val colors = FocusTheme.colors
    val density = LocalDensity.current

    // The card covers the bottom of the map; the pin centers on what's left above it.
    var addCardHeight by remember { mutableStateOf(0.dp) }
    val hiddenBottom = if (draft != null) addCardHeight + AddCardBottomMargin else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {
        MapPlaceholder(
            onLongPress = onMapLongPress,
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
                        subtitle = "Effective range: ${zone.radiusMeters.toInt()} m",
                        latitude = zone.latitude,
                        longitude = zone.longitude,
                        enabled = isZoneEnabled(zone),
                        onEnabledChange = { onZoneEnabledChange(zone, it) },
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
                AddLocationCard(
                    latitude = draftLatLng?.first,
                    longitude = draftLatLng?.second,
                    name = draft.name,
                    onNameChange = { onDraftChange(draft.copy(name = it)) },
                    radiusMeters = draft.radiusMeters,
                    onRadiusChange = { onDraftChange(draft.copy(radiusMeters = it)) },
                    // The Time Focus flow isn't built yet - see the "Time Focuse" Figma frames.
                    onScheduleClick = {},
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
            draft = null,
            draftLatLng = null,
            saveError = null,
            panelState = rememberPullUpPanelState(initiallyExpanded = true),
            onMapLongPress = {},
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
            draft = LocationDraft(),
            draftLatLng = -37.8136 to 144.9631,
            saveError = null,
            panelState = rememberPullUpPanelState(),
            onMapLongPress = {},
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
