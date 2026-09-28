package com.example.focusapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Figma: the "Relaxing minutes" scroll wheel - three rows visible, the
 * middle one highlighted and selected. Scrolling snaps to a row; tapping a
 * row scrolls it into the middle. [onValueChange] fires whenever the middle
 * row changes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    values: List<T>,
    selected: T,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
    rowHeight: Dp = 44.dp,
    width: Dp = 100.dp,
) {
    val colors = FocusTheme.colors
    val scope = rememberCoroutineScope()
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = values.indexOf(selected).coerceAtLeast(0))
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    // With one row of padding above and below, the first visible item is the middle row.
    val centerIndex by remember {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex +
                if (listState.firstVisibleItemScrollOffset > rowHeightPx / 2) 1 else 0
            index.coerceIn(0, values.lastIndex)
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { centerIndex }
            .distinctUntilChanged()
            .collect { currentOnValueChange(values[it]) }
    }

    val lineColor = colors.surfaceSunken
    Box(
        modifier = modifier
            .width(width)
            .height(rowHeight * 3)
            // Divider lines along the wheel's top and bottom edges.
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawLine(lineColor, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
                drawLine(lineColor, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
            },
        contentAlignment = Alignment.Center,
    ) {
        // Highlight behind the middle row.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight - 6.dp)
                .background(colors.surfaceSunken, RoundedCornerShape(10.dp)),
        )
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(vertical = rowHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(values) { index, value ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { scope.launch { listState.animateScrollToItem(index) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(value),
                        style = FocusTheme.typography.tileTitle,
                        color = if (index == centerIndex) colors.onSurface else colors.onSurfaceMuted,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun WheelPickerPreview() {
    FocusAppTheme {
        var minutes by remember { mutableIntStateOf(10) }
        Box(Modifier.background(FocusTheme.colors.surface).padding(16.dp)) {
            WheelPicker(
                values = (5..60 step 5).toList(),
                selected = minutes,
                onValueChange = { minutes = it },
                label = { "%02d".format(it) },
            )
        }
    }
}
