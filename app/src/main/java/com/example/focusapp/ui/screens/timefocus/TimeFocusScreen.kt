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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.ui.components.FocusConfirmDialog
import com.example.focusapp.ui.navigation.MainTab
import com.example.focusapp.ui.navigation.MainTabBar
import com.example.focusapp.ui.screens.home.ClockTime
import com.example.focusapp.ui.screens.home.TimeSlot
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

private val HeaderHeight = 210.dp

/**
 * Figma: "Time Focuse" - the Schedule tab. A header illustration over the
 * list of time slots (each with an on/off switch) and an "add" card.
 * Hold a slot to delete it. Adding / editing opens a fly card (not built
 * yet - waiting on its design).
 */
@Composable
fun TimeFocusScreen(
    onTabClick: (MainTab) -> Unit,
) {
    val context = LocalContext.current
    val storage = remember { TimeSlotStorage(context) }
    var slots by remember { mutableStateOf(storage.getSlots()) }
    var pendingDelete by remember { mutableStateOf<FocusTimeSlot?>(null) }

    fun update(newSlots: List<FocusTimeSlot>) {
        slots = newSlots
        storage.saveSlots(newSlots)
    }

    TimeFocusContent(
        slots = slots,
        onEnabledChange = { item, enabled ->
            update(slots.map { if (it.id == item.id) it.copy(enabled = enabled) else it })
        },
        // TODO: open the add/edit time-slot fly card once its design is in.
        onSlotClick = {},
        onSlotLongClick = { pendingDelete = it },
        onAddClick = {},
        onTabClick = onTabClick,
    )

    pendingDelete?.let { item ->
        FocusConfirmDialog(
            title = "Delete ${formatSlotRange(item.slot)}?",
            message = "This time slot will be removed.",
            confirmLabel = "Delete",
            confirmColor = FocusTheme.colors.rejection,
            onConfirm = {
                pendingDelete = null
                update(slots.filterNot { it.id == item.id })
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** Stateless layout of [TimeFocusScreen]. */
@Composable
private fun TimeFocusContent(
    slots: List<FocusTimeSlot>,
    onEnabledChange: (FocusTimeSlot, Boolean) -> Unit,
    onSlotClick: (FocusTimeSlot) -> Unit,
    onSlotLongClick: (FocusTimeSlot) -> Unit,
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
                slots.forEach { item ->
                    TimeSlotCard(
                        timeLabel = formatSlotRange(item.slot),
                        activeDays = item.slot.activeDays,
                        enabled = item.enabled,
                        onEnabledChange = { onEnabledChange(item, it) },
                        onClick = { onSlotClick(item) },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSlotLongClick(item)
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
            slots = listOf(
                FocusTimeSlot("1", TimeSlot(setOf("Mon", "Wed", "Fri"), ClockTime(6, 10), ClockTime(8, 0))),
                FocusTimeSlot("2", TimeSlot(setOf("Sat", "Sun"), ClockTime(20, 10), ClockTime(23, 0)), enabled = false),
            ),
            onEnabledChange = { _, _ -> },
            onSlotClick = {},
            onSlotLongClick = {},
            onAddClick = {},
            onTabClick = {},
        )
    }
}
