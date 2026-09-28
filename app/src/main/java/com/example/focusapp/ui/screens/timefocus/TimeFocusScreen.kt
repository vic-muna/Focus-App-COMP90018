package com.example.focusapp.ui.screens.timefocus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.ui.components.FocusConfirmDialog
import com.example.focusapp.ui.navigation.MainTab
import com.example.focusapp.ui.navigation.MainTabBar
import com.example.focusapp.ui.screens.home.AutoBlockingSheetContent
import com.example.focusapp.ui.screens.home.BlockedAppGroup
import com.example.focusapp.ui.screens.home.ClockTime
import com.example.focusapp.ui.screens.home.LegacySheetContainerColor
import com.example.focusapp.ui.screens.home.TimeSlot
import com.example.focusapp.ui.screens.home.generateFakeTimeSlot
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

private val HeaderHeight = 210.dp

/**
 * Figma: "Time Focuse" - the Schedule tab. A header illustration over the
 * list of time slots (each with an on/off switch) and an "add" card.
 *
 * Each slot is one of David's Scheduled Limits groups ([BlockedAppGroup]):
 * its time, apps and daily limits live together, so there's no separate
 * app-group page. Tap a slot to edit it, hold it to delete it, "+" adds one.
 * Editing uses David's Scheduled Limits sheet until the fly card is designed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeFocusScreen(
    groups: List<BlockedAppGroup>,
    onGroupsChange: (List<BlockedAppGroup>) -> Unit,
    onTabClick: (MainTab) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<BlockedAppGroup?>(null) }
    // The slot whose sheet is open, if any.
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun updateGroup(groupId: String, transform: (BlockedAppGroup) -> BlockedAppGroup) {
        onGroupsChange(groups.map { if (it.id == groupId) transform(it) else it })
    }

    TimeFocusContent(
        groups = groups,
        onEnabledChange = { group, enabled -> updateGroup(group.id) { it.copy(enabled = enabled) } },
        onGroupClick = { editingGroupId = it.id },
        onGroupLongClick = { pendingDelete = it },
        onAddClick = {
            val newGroup = BlockedAppGroup(
                id = "group_${System.currentTimeMillis()}",
                name = "New Schedule",
                apps = emptyList(),
                schedule = generateFakeTimeSlot(),
            )
            onGroupsChange(groups + newGroup)
            editingGroupId = newGroup.id
        },
        onTabClick = onTabClick,
    )

    editingGroupId?.let { groupId ->
        ModalBottomSheet(
            onDismissRequest = { editingGroupId = null },
            sheetState = sheetState,
            containerColor = LegacySheetContainerColor,
        ) {
            AutoBlockingSheetContent(
                groups = groups,
                selectedGroupId = groupId,
                onAppsChange = { id, apps -> updateGroup(id) { it.copy(apps = apps) } },
                onScheduleChange = { id, schedule -> updateGroup(id) { it.copy(schedule = schedule) } },
                onRenameGroup = { id, name -> updateGroup(id) { it.copy(name = name) } },
                onMaxOpensChange = { id, maxOpens -> updateGroup(id) { it.copy(maxOpensPerApp = maxOpens) } },
                onMaxDurationChange = { id, maxMinutes -> updateGroup(id) { it.copy(maxMinutesPerApp = maxMinutes) } },
                // No group list page any more - the slot list behind the sheet is the list.
                onBlockerClick = { editingGroupId = null },
            )
        }
    }

    pendingDelete?.let { group ->
        FocusConfirmDialog(
            title = "Delete \"${group.name}\"?",
            message = "This time slot and its app limits will be removed.",
            confirmLabel = "Delete",
            confirmColor = FocusTheme.colors.rejection,
            onConfirm = {
                pendingDelete = null
                onGroupsChange(groups.filterNot { it.id == group.id })
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** Stateless layout of [TimeFocusScreen]. */
@Composable
private fun TimeFocusContent(
    groups: List<BlockedAppGroup>,
    onEnabledChange: (BlockedAppGroup, Boolean) -> Unit,
    onGroupClick: (BlockedAppGroup) -> Unit,
    onGroupLongClick: (BlockedAppGroup) -> Unit,
    onAddClick: () -> Unit,
    onTabClick: (MainTab) -> Unit,
) {
    val colors = FocusTheme.colors
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Image(
                painter = painterResource(R.drawable.img_app_focus_header),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeaderHeight),
            )

            Column(
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                groups.forEach { group ->
                    TimeSlotCard(
                        name = group.name,
                        timeLabel = formatSlotRange(group.schedule),
                        activeDays = group.schedule.activeDays,
                        enabled = group.enabled,
                        onEnabledChange = { onEnabledChange(group, it) },
                        onClick = { onGroupClick(group) },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGroupLongClick(group)
                        },
                    )
                }
                AddTimeSlotCard(onClick = onAddClick)
            }
        }

        MainTabBar(
            selectedTab = MainTab.SCHEDULE,
            onTabClick = onTabClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        )
    }
}

/** e.g. "06:10am-08:00am", as on the Figma cards. */
private fun formatSlotRange(slot: TimeSlot): String {
    fun ClockTime.label(): String = formatted().let { (time, suffix) -> time + suffix.lowercase() }
    return "${slot.start.label()}-${slot.end.label()}"
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun TimeFocusContentPreview() {
    FocusAppTheme {
        TimeFocusContent(
            groups = listOf(
                BlockedAppGroup(
                    id = "1",
                    name = "Morning",
                    apps = emptyList(),
                    schedule = TimeSlot(setOf("Mon", "Wed", "Fri"), ClockTime(6, 10), ClockTime(8, 0)),
                ),
                BlockedAppGroup(
                    id = "2",
                    name = "Weekend night",
                    apps = emptyList(),
                    schedule = TimeSlot(setOf("Sat", "Sun"), ClockTime(20, 10), ClockTime(23, 0)),
                    enabled = false,
                ),
            ),
            onEnabledChange = { _, _ -> },
            onGroupClick = {},
            onGroupLongClick = {},
            onAddClick = {},
            onTabClick = {},
        )
    }
}