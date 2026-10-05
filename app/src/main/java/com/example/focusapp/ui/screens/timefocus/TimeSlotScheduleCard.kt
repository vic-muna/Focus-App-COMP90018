package com.example.focusapp.ui.screens.timefocus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.blocking.TimeRangeDraft
import com.example.focusapp.ui.components.button.BackButton
import com.example.focusapp.ui.components.button.NextButton
import com.example.focusapp.ui.components.card.CardButtonRow
import com.example.focusapp.ui.components.card.CardTitle
import com.example.focusapp.ui.components.card.FocusCard
import com.example.focusapp.ui.components.card.sunkenPanel
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

private const val MAX_TIME_FRAMES = 3

/**
 * Figma: "App Focuse" add-group step 2, reused for a time slot - which days it
 * runs, and from when to when (a 24-hour dial).
 * Supports multiple time frames for the same group (e.g. 9am-11am and 2pm-5pm).
 * Scrollable card view with a "+" button below to add additional time frames.
 */
@Composable
fun TimeSlotScheduleCard(
    activeDays: Set<String>,
    onToggleDay: (String) -> Unit,
    timeRanges: List<TimeRangeDraft>,
    onTimeRangesChange: (List<TimeRangeDraft>) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography
    val panel = Modifier.sunkenPanel(colors.surfaceSunken)
    val hasCrossedMidnight = timeRanges.any { it.startMinutes >= it.endMinutes }
    val isValid = activeDays.isNotEmpty() && timeRanges.isNotEmpty() && !hasCrossedMidnight

    FocusCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 620.dp)
    ) {
        CardButtonRow(
            left = { BackButton(onClick = onBack) },
            right = { NextButton(onClick = onNext, enabled = isValid) },
        )
        CardTitle("Days Activity")

        DaySelector(
            selected = activeDays,
            onToggleDay = onToggleDay,
            modifier = panel.padding(horizontal = 12.dp, vertical = 8.dp),
        )

        Column(
            modifier = Modifier
                .padding(top = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            timeRanges.forEachIndexed { index, range ->
                Column(
                    modifier = panel.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (timeRanges.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Time Frame ${index + 1}",
                                style = typography.tileTitle,
                                color = colors.onSurface
                            )
                            IconButton(
                                onClick = {
                                    val updated = timeRanges.toMutableList()
                                    updated.removeAt(index)
                                    onTimeRangesChange(updated)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Remove time frame",
                                    tint = colors.rejection,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        TimeReadout(label = "Quiet Start", minutes = range.startMinutes)
                        TimeReadout(label = "Quiet Ends", minutes = range.endMinutes)
                    }

                    ScheduleDial(
                        startMinutes = range.startMinutes,
                        endMinutes = range.endMinutes,
                        onChange = { start, end ->
                            val updated = timeRanges.toMutableList()
                            updated[index] = TimeRangeDraft(start, end)
                            onTimeRangesChange(updated)
                        },
                    )

                    Text(
                        text = formatDuration(range.startMinutes, range.endMinutes),
                        style = typography.tileTitle,
                        color = colors.onSurface,
                    )

                    if (range.startMinutes >= range.endMinutes) {
                        Text(
                            text = "The time must start and end on the same day.",
                            style = typography.caption,
                            color = colors.rejection,
                        )
                    }
                }
            }

            // "+" button below to indicate that more time frames can be added for the same group (up to 3)
            if (timeRanges.size < MAX_TIME_FRAMES) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceSunken)
                        .clickable(role = Role.Button) {
                            val lastEnd = timeRanges.lastOrNull()?.endMinutes ?: (14 * 60)
                            val newStart = if (lastEnd < 20 * 60) lastEnd + 60 else 14 * 60
                            val newEnd = (newStart + 3 * 60).coerceAtMost(23 * 60 + 59)
                            onTimeRangesChange(timeRanges + TimeRangeDraft(newStart, newEnd))
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add time frame",
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Add time frame",
                        style = typography.confirmation,
                        color = colors.accent,
                    )
                }
            }
        }
    }
}

/** Overload for single start/end range backwards compatibility. */
@Composable
fun TimeSlotScheduleCard(
    activeDays: Set<String>,
    onToggleDay: (String) -> Unit,
    startMinutes: Int,
    endMinutes: Int,
    onTimeChange: (startMinutes: Int, endMinutes: Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TimeSlotScheduleCard(
        activeDays = activeDays,
        onToggleDay = onToggleDay,
        timeRanges = listOf(TimeRangeDraft(startMinutes, endMinutes)),
        onTimeRangesChange = { ranges ->
            val first = ranges.firstOrNull() ?: TimeRangeDraft(startMinutes, endMinutes)
            onTimeChange(first.startMinutes, first.endMinutes)
        },
        onBack = onBack,
        onNext = onNext,
        modifier = modifier
    )
}

@Composable
private fun TimeReadout(label: String, minutes: Int) {
    val colors = FocusTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = FocusTheme.typography.caption, color = colors.onSurface)
        Text(text = formatClock(minutes), style = FocusTheme.typography.tileTitle, color = colors.onSurface)
    }
}

@Preview(widthDp = 360)
@Composable
private fun TimeSlotScheduleCardPreview() {
    FocusAppTheme {
        TimeSlotScheduleCard(
            activeDays = setOf("Mon", "Tue", "Wed", "Thu", "Fri"),
            onToggleDay = {},
            timeRanges = listOf(
                TimeRangeDraft(9 * 60, 11 * 60),
                TimeRangeDraft(14 * 60, 17 * 60)
            ),
            onTimeRangesChange = {},
            onBack = {},
            onNext = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
