package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.focusapp.data.apps.InstalledAppInfo
import com.example.focusapp.data.apps.getLaunchableApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_VISIBLE_APPS_IN_CARD = 4

private val CardSurface = Color.White.copy(alpha = 0.15f)
private val BottomBarSurface = Color.White.copy(alpha = 0.25f)
private val SheetIndigo = Color(0xFF3B3B96)
private val OnSheet = Color.White
private val OnSheetMuted = Color.White.copy(alpha = 0.8f)
private val CardIconPlaceholderColors = listOf(
    Color(0xFFD9D9D9),
    Color(0xFF7A7A7A),
    Color(0xFF4FCFC0),
    Color(0xFFB79FE0)
)

private enum class ActivePicker { NONE, APP_PICKER, SCHEDULE_EDITOR }

/**
 * Content of the Blocked Apps summary bottom sheet ("Auto Blocking").
 *
 * Just two tap zones: the Block Apps card opens the full app list, and the
 * Active Time card opens one combined editor for active days + start/end
 * time together (not three separate pickers). Both are ModalBottomSheets
 * layered on top of this sheet, and every edit applies immediately. The
 * only remaining full-screen destination is GroupListScreen, reached via
 * the bottom chevron bar.
 *
 * Picker-open state is kept local to this composable (not lifted into
 * HomeScreenWithSheet) since nothing outside needs to observe it - matches
 * how EditLocationZoneScreen/PartyModeScreen own their own transient UI
 * state elsewhere in this codebase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoBlockingSheetContent(
    groups: List<BlockedAppGroup>,
    selectedGroupId: String,
    onAppsChange: (groupId: String, apps: List<AppItem>) -> Unit,
    onScheduleChange: (groupId: String, schedule: TimeSlot) -> Unit,
    onRenameGroup: (groupId: String, newName: String) -> Unit = { _, _ -> },
    onBlockerClick: () -> Unit
) {
    val selectedGroup = groups.find { it.id == selectedGroupId } ?: groups.firstOrNull() ?: return

    var activePicker by remember { mutableStateOf(ActivePicker.NONE) }
    var showRenameDialog by remember { mutableStateOf(false) }

    // [David Shiau, 2026-09-20] Real installed apps for the picker - loaded
    // once (lazily, on first open) and reused afterwards rather than
    // re-querying PackageManager every time the sheet is reopened.
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoadingInstalledApps by remember { mutableStateOf(false) }

    LaunchedEffect(activePicker) {
        if (activePicker == ActivePicker.APP_PICKER && installedApps.isEmpty() && !isLoadingInstalledApps) {
            isLoadingInstalledApps = true
            installedApps = withContext(Dispatchers.Default) { getLaunchableApps(context) }
            isLoadingInstalledApps = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = selectedGroup.name,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = OnSheet,
            modifier = Modifier.clickable { showRenameDialog = true }
        )

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .width(60.dp)
                .height(2.dp)
                .background(OnSheet)
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BlockAppsCard(
                group = selectedGroup,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { activePicker = ActivePicker.APP_PICKER }
            )
            ActiveTimeCard(
                group = selectedGroup,
                modifier = Modifier.weight(1.1f).fillMaxHeight(),
                onClick = { activePicker = ActivePicker.SCHEDULE_EDITOR }
            )
        }

        Spacer(Modifier.height(16.dp))

        BottomChevronBar(onClick = onBlockerClick)
    }

    if (activePicker == ActivePicker.APP_PICKER) {
        // [David Shiau, 2026-09-20] Merge every real installed app with
        // this group's already-blocked packages, so the picker shows the
        // whole phone's app list with the group's current selection
        // pre-checked (multi-select - any number of rows can be checked
        // at once).
        val blockedPackageNames = remember(selectedGroup.apps) {
            selectedGroup.apps.filter { it.isBlocked }.map { it.packageName }.toSet()
        }
        val pickerApps = remember(installedApps, blockedPackageNames) {
            installedApps.map { info ->
                AppItem(
                    packageName = info.packageName,
                    name = info.label,
                    isBlocked = info.packageName in blockedPackageNames,
                    icon = info.icon
                )
            }
        }

        ModalBottomSheet(
            onDismissRequest = { activePicker = ActivePicker.NONE },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            // Avoids a doubled/near-black scrim when stacked over the
            // already-open summary sheet.
            scrimColor = Color.Transparent
        ) {
            AppPickerSheet(
                apps = pickerApps,
                isLoading = isLoadingInstalledApps,
                // Only the checked apps are worth persisting on the group -
                // storing the full installed-app list would balloon storage
                // and break "N apps blocked" counts elsewhere.
                onAppsChange = { updated ->
                    onAppsChange(selectedGroup.id, updated.filter { it.isBlocked })
                }
            )
        }
    }

    if (activePicker == ActivePicker.SCHEDULE_EDITOR) {
        ModalBottomSheet(
            onDismissRequest = { activePicker = ActivePicker.NONE },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            scrimColor = Color.Transparent
        ) {
            ScheduleEditorSheet(
                schedule = selectedGroup.schedule,
                onScheduleChange = { onScheduleChange(selectedGroup.id, it) }
            )
        }
    }

    if (showRenameDialog) {
        RenameGroupDialog(
            currentName = selectedGroup.name,
            onConfirm = { newName ->
                onRenameGroup(selectedGroup.id, newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }
}

/** Tapping the sheet's title opens this to rename the currently selected group. */
@Composable
private fun RenameGroupDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Group") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Tapping this card opens the app picker.
 * [David Shiau, 2026-09-20] Icons now draw each app's real launcher icon
 * (falls back to the old colored placeholder box only if it's unavailable).
 */
@Composable
private fun BlockAppsCard(group: BlockedAppGroup, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CardSurface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Block Apps", color = OnSheet, fontSize = 18.sp, fontWeight = FontWeight.Medium)

        val visibleApps = group.apps.take(MAX_VISIBLE_APPS_IN_CARD)
        val overflowCount = group.apps.size - visibleApps.size

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
            Box(
                modifier = Modifier.weight(1f, fill = false),
                contentAlignment = Alignment.CenterStart
            ) {
                visibleApps.forEachIndexed { index, app ->
                    val icon = app.icon
                    if (icon != null) {
                        Image(
                            bitmap = icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = (index * 24).dp)
                                .zIndex(index.toFloat())
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(start = (index * 24).dp)
                                .zIndex(index.toFloat())
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardIconPlaceholderColors[index % CardIconPlaceholderColors.size])
                        )
                    }
                }
            }
            if (overflowCount > 0) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "+$overflowCount",
                    color = OnSheet,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Tapping this card opens the combined schedule editor (days + start/end time). */
@Composable
private fun ActiveTimeCard(
    group: BlockedAppGroup,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val schedule = group.schedule
    val dayLabels = formatActiveDaysForDisplay(schedule.activeDays)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CardSurface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Active Time", color = OnSheet, fontSize = 18.sp, fontWeight = FontWeight.Medium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                SplitTimeLabel(schedule.start.formatted())
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                        .width(2.dp)
                        .height(14.dp)
                        .background(OnSheet)
                )
                SplitTimeLabel(schedule.end.formatted())
            }

            if (dayLabels.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    dayLabels.forEach { label ->
                        Text(label, color = OnSheetMuted, fontSize = 11.sp, fontWeight = FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitTimeLabel(time: Pair<String, String>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(time.first, color = OnSheet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(2.dp))
        Text(
            text = time.second,
            color = OnSheet,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}

/**
 * Collapses a fully-selected week to "Every day", then a Sat+Sun pair to
 * "Weekend" - reflects whatever the group's actual schedule says.
 */
private fun formatActiveDaysForDisplay(activeDays: Set<String>): List<String> {
    if (DAY_KEYS.all { activeDays.contains(it) }) return listOf("Every day")

    val hasWeekend = activeDays.contains("Sat") && activeDays.contains("Sun")
    val weekdayOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
    val labels = mutableListOf<String>()
    if (hasWeekend) labels += "Weekend"
    labels += weekdayOrder.filter { activeDays.contains(it) }
    if (!hasWeekend) {
        if (activeDays.contains("Sat")) labels += "Sat"
        if (activeDays.contains("Sun")) labels += "Sun"
    }
    return labels
}

/** The only navigation trigger on this sheet: opens the group selection list. */
@Composable
private fun BottomChevronBar(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(BottomBarSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = "Open group list",
            tint = SheetIndigo
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF3B3B96)
@Composable
private fun AutoBlockingSheetContentPreview() {
    val fakeGroups = generateFakeGroups()
    AutoBlockingSheetContent(
        groups = fakeGroups,
        selectedGroupId = fakeGroups.first().id,
        onAppsChange = { _, _ -> },
        onScheduleChange = { _, _ -> },
        onBlockerClick = {}
    )
}
