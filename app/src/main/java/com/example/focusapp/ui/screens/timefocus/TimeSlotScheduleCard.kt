package com.example.focusapp.ui.screens.timefocus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import com.example.focusapp.ui.components.button.BackButton
import com.example.focusapp.ui.components.button.NextButton
import com.example.focusapp.ui.components.card.CardButtonRow
import com.example.focusapp.ui.components.card.CardTitle
import com.example.focusapp.ui.components.card.FocusCard
import com.example.focusapp.ui.components.card.sunkenPanel

/**
 * Figma: "App Focuse" add-group step 2, reused for a time slot - which days it
 * runs, and from when to when (a 24-hour dial). Times are minutes after
 * midnight. The arrow moves on once at least one day is picked and the
 * range doesn't cross midnight - David's daily limits only run on
 * same-day ranges (see TimeSlot.windowOn).
 */
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
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography
    val crossesMidnight = startMinutes >= endMinutes
    val panel = Modifier.sunkenPanel(colors.surfaceSunken)

    FocusCard(modifier = modifier) {
        CardButtonRow(
            left = { BackButton(onClick = onBack) },
            right = { NextButton(onClick = onNext, enabled = activeDays.isNotEmpty() && !crossesMidnight) },
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
                .then(panel)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TimeReadout(label = "Quiet Start", minutes = startMinutes)
                TimeReadout(label = "Quiet Ends", minutes = endMinutes)
            }
            ScheduleDial(
                startMinutes = startMinutes,
                endMinutes = endMinutes,
                onChange = onTimeChange,
            )
            Text(
                text = formatDuration(startMinutes, endMinutes),
                style = typography.tileTitle,
                color = colors.onSurface,
            )
            if (crossesMidnight) {
                Text(
                    text = "The time must start and end on the same day.",
                    style = typography.caption,
                    color = colors.rejection,
                )
            }
        }
    }
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
            startMinutes = 9 * 60,
            endMinutes = 17 * 60 + 30,
            onTimeChange = { _, _ -> },
            onBack = {},
            onNext = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
