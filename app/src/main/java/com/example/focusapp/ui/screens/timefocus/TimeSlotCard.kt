package com.example.focusapp.ui.screens.timefocus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.card.AddItemCard
import com.example.focusapp.ui.components.card.SwitchListCard
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** One time slot in the Time Focus list: name, time, days and an on/off switch. */
@Composable
fun TimeSlotCard(
    name: String,
    timeLabel: String,
    activeDays: Set<String>,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    SwitchListCard(
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        spacing = 8.dp,
    ) {
        Text(
            text = name,
            style = FocusTheme.typography.body,
            color = FocusTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text = timeLabel, style = FocusTheme.typography.tileTitle, color = FocusTheme.colors.onSurface)
        DayIndicators(activeDays = activeDays)
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
                name = "Study Group",
                timeLabel = "06:10am-08:00am",
                activeDays = setOf("Mon", "Tue", "Wed"),
                enabled = true,
                onEnabledChange = {},
            )
            AddItemCard(onClick = {}, onClickLabel = "Add time slot")
        }
    }
}
