package com.example.focusapp.ui.screens.location

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.ui.screens.location.map.InteractiveMapView
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

private const val GRID_STEP_DP = 48

const val PLACEHOLDER_DP_PER_METER = 116f / (2 * 100f)

/**
 * Map container component that renders the interactive map view (or a grid in preview mode).
 */
@Composable
fun MapPlaceholder(
    onLongPress: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    currentLocation: Pair<Double, Double>? = null,
    pinLocation: Pair<Double, Double>? = null,
    pinRadiusMeters: Float = 100f,
    zones: List<FocusZone> = emptyList(),
    onLocationClick: ((latitude: Double, longitude: Double) -> Unit)? = null,
    onZoneClick: ((zoneId: String) -> Unit)? = null,
    overlays: @Composable BoxScope.() -> Unit = {},
) {
    val isPreview = LocalInspectionMode.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        if (isPreview) {
            FallbackGridMap(onLongPress = onLongPress)
        } else {
            InteractiveMapView(
                currentLocation = currentLocation,
                pinLocation = pinLocation,
                pinRadiusMeters = pinRadiusMeters,
                zones = zones,
                onLocationClick = onLocationClick,
                onZoneClick = onZoneClick,
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            content = overlays,
        )
    }
}

@Composable
private fun FallbackGridMap(onLongPress: (Offset) -> Unit) {
    val colors = FocusTheme.colors
    val gridColor = colors.onSurface.copy(alpha = 0.05f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .drawBehind {
                val step = GRID_STEP_DP.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height))
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y))
                    y += step
                }
            }
            .pointerInput(Unit) { detectTapGestures(onLongPress = onLongPress) },
    ) {
        Text(
            text = "Map Preview\nLong-press anywhere to set location pin",
            style = FocusTheme.typography.caption,
            color = colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
        )
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun MapPlaceholderPreview() {
    FocusAppTheme {
        MapPlaceholder(onLongPress = {})
    }
}
