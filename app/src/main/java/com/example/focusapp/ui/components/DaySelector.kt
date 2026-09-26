package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** One day in a [DaySelector]: the key callers store, its one-letter label, and a spoken name. */
data class DayOption(val key: String, val letter: String, val name: String)

/**
 * Figma "Days Activity" order: Sunday first. Keys match the Mon-first
 * `DAY_KEYS` used by the schedule model, so selections can be stored as-is.
 */
val SundayFirstDays = listOf(
    DayOption("Sun", "S", "Sunday"),
    DayOption("Mon", "M", "Monday"),
    DayOption("Tue", "T", "Tuesday"),
    DayOption("Wed", "W", "Wednesday"),
    DayOption("Thu", "T", "Thursday"),
    DayOption("Fri", "F", "Friday"),
    DayOption("Sat", "S", "Saturday"),
)

/**
 * Figma: the row of weekday circles - outlined when off, filled with the
 * accent color when on. Tapping a day toggles it in [selected].
 */
@Composable
fun DaySelector(
    selected: Set<String>,
    onToggleDay: (key: String) -> Unit,
    modifier: Modifier = Modifier,
    days: List<DayOption> = SundayFirstDays,
    circleSize: Dp = 26.dp,
) {
    val colors = FocusTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        days.forEach { day ->
            val isOn = day.key in selected
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
                    .then(
                        if (isOn) Modifier.background(colors.accent)
                        else Modifier.border(1.5.dp, colors.onSurface, CircleShape)
                    )
                    .toggleable(value = isOn, role = Role.Checkbox, onValueChange = { onToggleDay(day.key) })
                    .semantics { contentDescription = day.name },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.letter,
                    style = FocusTheme.typography.listLabel,
                    color = if (isOn) colors.onPrimaryAction else colors.onSurface,
                )
            }
        }
    }
}

/**
 * Read-only version of [DaySelector] for summaries: active days get an
 * accent outline and letter, the rest stay plain.
 */
@Composable
fun DayIndicators(
    activeDays: Set<String>,
    modifier: Modifier = Modifier,
    days: List<DayOption> = SundayFirstDays,
    circleSize: Dp = 26.dp,
) {
    val colors = FocusTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        days.forEach { day ->
            val color = if (day.key in activeDays) colors.accent else colors.onSurface
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .border(1.5.dp, color, CircleShape)
                    .semantics { contentDescription = day.name },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = day.letter, style = FocusTheme.typography.listLabel, color = color)
            }
        }
    }
}

@Preview(widthDp = 320)
@Composable
private fun DayIndicatorsPreview() {
    FocusAppTheme {
        DayIndicators(
            activeDays = setOf("Wed", "Thu", "Fri"),
            modifier = Modifier
                .background(FocusTheme.colors.surfaceSunken)
                .padding(12.dp),
        )
    }
}

@Preview(widthDp = 320)
@Composable
private fun DaySelectorPreview() {
    FocusAppTheme {
        DaySelector(
            selected = setOf("Mon", "Wed", "Fri"),
            onToggleDay = {},
            modifier = Modifier
                .background(FocusTheme.colors.surfaceSunken)
                .padding(12.dp),
        )
    }
}
