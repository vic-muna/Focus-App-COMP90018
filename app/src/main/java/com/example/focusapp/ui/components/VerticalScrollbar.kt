package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * Draws a thin always-visible scrollbar (track + thumb) along the end edge
 * of a LazyColumn driven by [state]. Compose has no built-in scrollbar;
 * the thumb's size/position are estimated from the visible item count,
 * which is accurate enough for lists of similar-height rows.
 */
fun Modifier.verticalScrollbar(
    state: LazyListState,
    thumbColor: Color,
    trackColor: Color,
    width: Dp = 4.dp,
    endPadding: Dp = 12.dp,
    verticalPadding: Dp = 12.dp,
): Modifier = drawWithContent {
    drawContent()

    val layoutInfo = state.layoutInfo
    val total = layoutInfo.totalItemsCount
    val visible = layoutInfo.visibleItemsInfo
    if (total == 0 || visible.isEmpty()) return@drawWithContent

    val barWidth = width.toPx()
    val x = size.width - endPadding.toPx() - barWidth
    val top = verticalPadding.toPx()
    val trackHeight = size.height - top * 2
    val radius = CornerRadius(barWidth / 2)

    drawRoundRect(trackColor, Offset(x, top), Size(barWidth, trackHeight), radius)
    if (visible.size >= total) return@drawWithContent

    val firstFraction = state.firstVisibleItemIndex +
        state.firstVisibleItemScrollOffset / visible.first().size.coerceAtLeast(1).toFloat()
    val thumbHeight = (trackHeight * visible.size / total).coerceAtLeast(barWidth * 4)
    val thumbTop = top + (trackHeight - thumbHeight) * (firstFraction / (total - visible.size)).coerceIn(0f, 1f)
    drawRoundRect(thumbColor, Offset(x, thumbTop), Size(barWidth, thumbHeight), radius)
}

@Preview
@Composable
private fun VerticalScrollbarPreview() {
    FocusAppTheme {
        val state = rememberLazyListState()
        Box(
            modifier = Modifier
                .height(200.dp)
                .background(FocusTheme.colors.surfaceSunken),
        ) {
            LazyColumn(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollbar(
                        state = state,
                        thumbColor = FocusTheme.colors.onSurfaceMuted,
                        trackColor = FocusTheme.colors.surface,
                    ),
            ) {
                items(30) { Text("Row $it", color = FocusTheme.colors.onSurface) }
            }
        }
    }
}
