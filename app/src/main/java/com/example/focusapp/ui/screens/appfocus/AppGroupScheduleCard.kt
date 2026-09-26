package com.example.focusapp.ui.screens.appfocus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.BackButton
import com.example.focusapp.ui.components.DaySelector
import com.example.focusapp.ui.components.NextButton
import com.example.focusapp.ui.components.ScheduleDial
import com.example.focusapp.ui.components.formatClock
import com.example.focusapp.ui.components.formatDuration
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * Figma: "App Focuse" add-group step 2 - which days the group's quiet time
 * runs, and from when to when (a 24-hour dial). Times are minutes after
 * midnight. The arrow moves on once at least one day is picked.
 */
@Composable
fun AppGroupScheduleCard(
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
    val panel = Modifier
        .fillMaxWidth()
        .background(colors.surfaceSunken, RoundedCornerShape(12.dp))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BackButton(onClick = onBack)
            NextButton(onClick = onNext, enabled = activeDays.isNotEmpty())
        }

        Text(
            text = "Days Activity",
            style = typography.tileTitle,
            color = colors.onSurface,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        )

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
private fun AppGroupScheduleCardPreview() {
    FocusAppTheme {
        AppGroupScheduleCard(
            activeDays = setOf("Mon", "Tue", "Wed", "Thu", "Fri"),
            onToggleDay = {},
            startMinutes = 22 * 60 + 10,
            endMinutes = 8 * 60 + 52,
            onTimeChange = { _, _ -> },
            onBack = {},
            onNext = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
