package com.example.focusapp.ui.screens.timefocus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.DayIndicators
import com.example.focusapp.ui.components.FocusSwitch
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

private val CardShape = RoundedCornerShape(14.dp)

/** Height of the "add time slot" card (Figma: same footprint as a time-slot card). */
private val AddCardHeight = 84.dp

/**
 * Figma: one row of the "Time Focuse" list - the time range, the days it
 * repeats on, and an on/off switch. Tap = [onClick] (edit), hold =
 * [onLongClick] (delete); the switch handles its own taps.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimeSlotCard(
    timeLabel: String,
    activeDays: Set<String>,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val colors = FocusTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = timeLabel, style = FocusTheme.typography.tileTitle, color = colors.onSurface)
            DayIndicators(activeDays = activeDays)
        }
        Spacer(Modifier.width(8.dp))
        FocusSwitch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

/** Figma: the last card in the list - a muted circle with a plus, for adding a time slot. */
@Composable
fun AddTimeSlotCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AddCardHeight)
            .clip(CardShape)
            .background(colors.surface)
            .clickable(role = Role.Button, onClickLabel = "Add time slot", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colors.onSurfaceMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val stroke = 3.dp.toPx()
                val mid = size.width / 2
                drawLine(colors.surface, Offset(mid, 0f), Offset(mid, size.height), stroke, StrokeCap.Round)
                drawLine(colors.surface, Offset(0f, mid), Offset(size.width, mid), stroke, StrokeCap.Round)
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun TimeSlotCardPreview() {
    FocusAppTheme {
        Column(
            modifier = Modifier
                .background(FocusTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TimeSlotCard(
                timeLabel = "06:10am-08:00am",
                activeDays = setOf("Mon", "Tue", "Wed"),
                enabled = true,
                onEnabledChange = {},
            )
            AddTimeSlotCard(onClick = {})
        }
    }
}
