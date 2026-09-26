package com.example.focusapp.ui.screens.appfocus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.DayIndicators
import com.example.focusapp.ui.components.EditButton
import com.example.focusapp.ui.components.formatClock
import com.example.focusapp.ui.screens.home.BlockedAppGroup
import com.example.focusapp.ui.screens.home.generateFakeGroups
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** How many app icons the "Apps Group" row shows before just counting. */
private const val MAX_PREVIEW_ICONS = 2

private val PreviewIconSize = 48.dp

/**
 * Figma: the read-only summary of an existing app group - its apps, days and
 * quiet time, and break settings. The pencil opens the edit flow.
 */
@Composable
fun AppGroupDetailCard(
    group: BlockedAppGroup,
    onEdit: () -> Unit,
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
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            EditButton(onClick = onEdit, contentDescription = "Edit ${group.name}")
        }

        SectionTitle("Apps Group")
        Row(
            modifier = panel.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            group.apps.take(MAX_PREVIEW_ICONS).forEach { app ->
                val icon = app.icon
                val iconModifier = Modifier
                    .size(PreviewIconSize)
                    .clip(RoundedCornerShape(10.dp))
                if (icon != null) {
                    Image(bitmap = icon.asImageBitmap(), contentDescription = app.name, modifier = iconModifier)
                } else {
                    Box(iconModifier.background(colors.surface))
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (group.apps.size == 1) "1 app selected" else "${group.apps.size} apps selected",
                style = typography.listLabel,
                color = colors.onSurface,
            )
        }

        SectionTitle("Days Activity")
        Column(
            modifier = panel.padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val start = group.schedule.start.hour * 60 + group.schedule.start.minute
            val end = group.schedule.end.hour * 60 + group.schedule.end.minute
            Text(
                text = "${formatClock(start)} - ${formatClock(end)}",
                style = typography.tileTitle,
                color = colors.onSurface,
            )
            DayIndicators(activeDays = group.schedule.activeDays)
        }

        SectionTitle("Resting Setting")
        Row(
            modifier = panel.padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Stat(label = "Unfrozen\nTimes", value = group.breakAllowance.toString())
            Stat(label = "Relaxing\nminutes", value = group.breakMinutes.toString())
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = FocusTheme.typography.tileTitle,
        color = FocusTheme.colors.onSurface,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp),
    )
}

@Composable
private fun Stat(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = FocusTheme.typography.listLabel,
            color = FocusTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(text = value, style = FocusTheme.typography.statValue, color = FocusTheme.colors.accent)
    }
}

@Preview(widthDp = 360)
@Composable
private fun AppGroupDetailCardPreview() {
    FocusAppTheme {
        AppGroupDetailCard(
            group = generateFakeGroups().first().copy(breakAllowance = 5, breakMinutes = 10),
            onEdit = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
