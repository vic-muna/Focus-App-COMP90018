package com.example.focusapp.ui.screens.location

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.BackButton
import com.example.focusapp.ui.components.ConfirmButton
import com.example.focusapp.ui.components.formatClock
import com.example.focusapp.ui.components.verticalScrollbar
import com.example.focusapp.ui.screens.home.BlockedAppGroup
import com.example.focusapp.ui.screens.home.generateFakeGroups
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

private val GroupListHeight = 240.dp
private val GroupIconSize = 36.dp

/**
 * Fly card over the add/edit-location card: pick which app group (its
 * apps + schedule) this location uses. The picked row fills with the accent
 * color; one group at most - tapping the picked one again clears it. Back
 * returns without changes; the check keeps [selectedGroupId].
 */
@Composable
fun ScheduleGroupPickerCard(
    groups: List<BlockedAppGroup>,
    selectedGroupId: String?,
    onSelect: (groupId: String?) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

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
            ConfirmButton(onClick = onConfirm, contentDescription = "Use this schedule")
        }

        Text(
            text = "Schedule",
            style = typography.tileTitle,
            color = colors.onSurface,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(GroupListHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceSunken),
            contentAlignment = Alignment.Center,
        ) {
            if (groups.isEmpty()) {
                Text(
                    text = "No app groups yet.\nCreate one in the Blocked Apps tab.",
                    style = typography.caption,
                    color = colors.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(
                            state = listState,
                            thumbColor = colors.onSurfaceMuted,
                            trackColor = colors.surface,
                        ),
                ) {
                    items(groups, key = { it.id }) { group ->
                        val isPicked = group.id == selectedGroupId
                        GroupRow(
                            group = group,
                            selected = isPicked,
                            onToggle = { onSelect(if (isPicked) null else group.id) },
                        )
                    }
                }
            }
        }
    }
}

/** One group: first app icon, name, and "N apps · start - end"; accent-filled when picked. */
@Composable
private fun GroupRow(
    group: BlockedAppGroup,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography
    val start = group.schedule.start.hour * 60 + group.schedule.start.minute
    val end = group.schedule.end.hour * 60 + group.schedule.end.minute
    val textColor = if (selected) colors.onPrimaryAction else colors.onSurface
    val subtextColor = if (selected) colors.onPrimaryAction.copy(alpha = 0.7f) else colors.onSurfaceMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Inset from the list edges (and clear of the scrollbar) so the highlight reads as a pill.
            .padding(start = 8.dp, end = 20.dp, top = 3.dp, bottom = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.accent else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = group.apps.firstNotNullOfOrNull { it.icon }
        val iconModifier = Modifier
            .size(GroupIconSize)
            .clip(RoundedCornerShape(8.dp))
        if (icon != null) {
            Image(bitmap = icon.asImageBitmap(), contentDescription = null, modifier = iconModifier)
        } else {
            Box(iconModifier.background(colors.surface))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = typography.listLabel,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${group.apps.size} apps · ${formatClock(start)} - ${formatClock(end)}",
                style = typography.caption,
                color = subtextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun ScheduleGroupPickerCardPreview() {
    FocusAppTheme {
        val groups = generateFakeGroups()
        ScheduleGroupPickerCard(
            groups = groups,
            selectedGroupId = groups.first().id,
            onSelect = {},
            onBack = {},
            onConfirm = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
