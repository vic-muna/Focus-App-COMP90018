package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** One weekday: the key schedules store (matches `DAY_KEYS`), its letter, and a spoken name. */
data class DayOption(val key: String, val letter: String, val name: String)

/** Figma order: Sunday first. */
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
 * Figma: the row of small outlined weekday circles on a time-slot card.
 * Read-only; days in [activeDays] get the accent color, the rest stay plain.
 */
@Composable
fun DayIndicators(
    activeDays: Set<String>,
    modifier: Modifier = Modifier,
    days: List<DayOption> = SundayFirstDays,
    circleSize: Dp = 16.dp,
    spacing: Dp = 6.dp,
) {
    val colors = FocusTheme.colors

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        days.forEach { day ->
            val color = if (day.key in activeDays) colors.accent else colors.onSurface
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .border(1.dp, color, CircleShape)
                    .semantics { contentDescription = day.name },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = day.letter, style = FocusTheme.typography.microLabel, color = color)
            }
        }
    }
}

@Preview
@Composable
private fun DayIndicatorsPreview() {
    FocusAppTheme {
        DayIndicators(
            activeDays = setOf("Mon", "Wed", "Fri"),
            modifier = Modifier
                .background(FocusTheme.colors.surface)
                .padding(12.dp),
        )
    }
}
