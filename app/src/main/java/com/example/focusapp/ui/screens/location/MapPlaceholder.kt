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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

private const val GRID_STEP_DP = 48

/**
 * Fixed scale of the placeholder "map": how many dp one meter covers.
 * Matches the Figma marker (a 100 m zone drawn 116 dp wide). A real map
 * replaces this with its projection at the current zoom level.
 */
const val PLACEHOLDER_DP_PER_METER = 116f / (2 * 100f)

/**
 * Stand-in for the real map until the team picks a map SDK (Google Maps vs
 * OpenStreetMap). Keeps the same contract a real map would need - fill the
 * area, report long-presses, host overlays like zone markers - so swapping
 * it later only touches this file and its call site.
 *
 * [contentPadding] marks the parts of the map hidden behind other UI, like
 * GoogleMap's `contentPadding`: [overlays] are laid out inside the visible
 * remainder, so an overlay aligned to the center sits at the visible center.
 */
@Composable
fun MapPlaceholder(
    onLongPress: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    onTap: (Offset) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    overlays: @Composable BoxScope.() -> Unit = {},
) {
    val colors = FocusTheme.colors
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnTap by rememberUpdatedState(onTap)
    val gridColor = colors.onSurface.copy(alpha = 0.05f)

    Box(
        modifier = modifier
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
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnTap(it) },
                    onLongPress = { currentOnLongPress(it) },
                )
            },
    ) {
        Text(
            text = "Map coming soon\nLong-press anywhere to add a focus location",
            style = FocusTheme.typography.caption,
            color = colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            content = overlays,
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
