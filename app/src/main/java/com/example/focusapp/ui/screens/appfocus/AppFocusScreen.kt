package com.example.focusapp.ui.screens.appfocus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.data.apps.InstalledAppInfo
import com.example.focusapp.data.apps.getLaunchableApps
import com.example.focusapp.ui.components.FocusConfirmDialog
import com.example.focusapp.ui.navigation.MainTab
import com.example.focusapp.ui.navigation.MainTabBar
import com.example.focusapp.ui.screens.home.AppItem
import com.example.focusapp.ui.screens.home.BlockedAppGroup
import com.example.focusapp.ui.screens.home.ClockTime
import com.example.focusapp.ui.screens.home.DEFAULT_BREAK_MINUTES
import com.example.focusapp.ui.screens.home.TimeSlot
import com.example.focusapp.ui.screens.home.generateFakeGroups
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val HeaderHeight = 210.dp
private val GridColumnGap = 20.dp
private val GridRowGap = 35.dp

/** How dark the screen behind a fly card gets (Figma dims it heavily). */
private const val SCRIM_ALPHA = 0.8f

/** Default quiet time for a new group: 10:00 PM - 7:00 AM. */
private const val DEFAULT_START_MINUTES = 22 * 60
private const val DEFAULT_END_MINUTES = 7 * 60

/** Steps of the add-group fly card. */
private enum class AddGroupStep { APPS, SCHEDULE, BREAKS }

/** Everything the add-group fly card collects, handed to the caller on confirm. */
data class NewAppGroup(
    val name: String,
    val apps: List<AppItem>,
    val schedule: TimeSlot,
    val breakAllowance: Int,
    val breakMinutes: Int,
)

/**
 * Figma: "App Focuse" - the Blocked Apps tab. A header illustration over a
 * two-column grid of app-group tiles, ending with an "add group" tile that
 * opens a fly card: step 1 picks the new group's apps, step 2 its schedule,
 * step 3 its breaks.
 */
@Composable
fun AppFocusScreen(
    groups: List<BlockedAppGroup>,
    onGroupClick: (BlockedAppGroup) -> Unit,
    onCreateGroup: (NewAppGroup) -> Unit,
    onTabClick: (MainTab) -> Unit,
) {
    val context = LocalContext.current
    // null = not adding a group; otherwise the fly card's current step.
    var addStep by remember { mutableStateOf<AddGroupStep?>(null) }
    val isAddingGroup = addStep != null
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>?>(null) }
    var selectedPackages by remember { mutableStateOf(emptySet<String>()) }
    var activeDays by remember { mutableStateOf(emptySet<String>()) }
    var startMinutes by remember { mutableIntStateOf(DEFAULT_START_MINUTES) }
    var endMinutes by remember { mutableIntStateOf(DEFAULT_END_MINUTES) }
    var breakAllowance by remember { mutableStateOf<Int?>(null) }
    var breakMinutes by remember { mutableIntStateOf(DEFAULT_BREAK_MINUTES) }
    var groupName by remember { mutableStateOf("") }
    // Tapping outside the card asks before throwing the half-made group away.
    var confirmDiscard by remember { mutableStateOf(false) }

    // Loaded once, the first time the picker opens - it's slow with many apps installed.
    LaunchedEffect(isAddingGroup) {
        if (isAddingGroup && installedApps == null) {
            installedApps = withContext(Dispatchers.Default) { getLaunchableApps(context) }
        }
    }

    fun closePicker() {
        addStep = null
        selectedPackages = emptySet()
        activeDays = emptySet()
        startMinutes = DEFAULT_START_MINUTES
        endMinutes = DEFAULT_END_MINUTES
        breakAllowance = null
        breakMinutes = DEFAULT_BREAK_MINUTES
        groupName = ""
        confirmDiscard = false
    }

    // Back steps backwards through the card, then closes it.
    BackHandler(enabled = isAddingGroup) {
        when (addStep) {
            AddGroupStep.BREAKS -> addStep = AddGroupStep.SCHEDULE
            AddGroupStep.SCHEDULE -> addStep = AddGroupStep.APPS
            else -> closePicker()
        }
    }

    AppFocusContent(
        groups = groups,
        onGroupClick = onGroupClick,
        onAddGroupClick = { addStep = AddGroupStep.APPS },
        onTabClick = onTabClick,
        addStep = addStep,
        pickerApps = installedApps,
        selectedPackages = selectedPackages,
        onToggleApp = { pkg ->
            selectedPackages = if (pkg in selectedPackages) selectedPackages - pkg else selectedPackages + pkg
        },
        onPickerClose = ::closePicker,
        onPickerNext = { addStep = AddGroupStep.SCHEDULE },
        activeDays = activeDays,
        onToggleDay = { day -> activeDays = if (day in activeDays) activeDays - day else activeDays + day },
        startMinutes = startMinutes,
        endMinutes = endMinutes,
        onTimeChange = { start, end -> startMinutes = start; endMinutes = end },
        onScheduleBack = { addStep = AddGroupStep.APPS },
        onScheduleNext = { addStep = AddGroupStep.BREAKS },
        breakAllowance = breakAllowance,
        onBreakAllowanceChange = { breakAllowance = it },
        breakMinutes = breakMinutes,
        onBreakMinutesChange = { breakMinutes = it },
        groupName = groupName,
        onGroupNameChange = { groupName = it },
        onBreaksBack = { addStep = AddGroupStep.SCHEDULE },
        onOutsideCardClick = { confirmDiscard = true },
        onBreaksConfirm = {
            val apps = installedApps.orEmpty()
                .filter { it.packageName in selectedPackages }
                .map { AppItem(packageName = it.packageName, name = it.label, isBlocked = true, icon = it.icon) }
            val schedule = TimeSlot(
                activeDays = activeDays,
                start = ClockTime(startMinutes / 60, startMinutes % 60),
                end = ClockTime(endMinutes / 60, endMinutes % 60),
            )
            onCreateGroup(NewAppGroup(groupName.trim(), apps, schedule, breakAllowance ?: 0, breakMinutes))
            closePicker()
        },
    )

    if (confirmDiscard) {
        FocusConfirmDialog(
            title = "Cancel new group?",
            message = "The apps and settings you picked for this group will be discarded.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            confirmColor = FocusTheme.colors.rejection,
            onConfirm = ::closePicker,
            onDismiss = { confirmDiscard = false },
        )
    }
}

/** Stateless layout of [AppFocusScreen]. */
@Composable
private fun AppFocusContent(
    groups: List<BlockedAppGroup>,
    onGroupClick: (BlockedAppGroup) -> Unit,
    onAddGroupClick: () -> Unit,
    onTabClick: (MainTab) -> Unit,
    addStep: AddGroupStep?,
    pickerApps: List<InstalledAppInfo>?,
    selectedPackages: Set<String>,
    onToggleApp: (String) -> Unit,
    onPickerClose: () -> Unit,
    onPickerNext: () -> Unit,
    activeDays: Set<String>,
    onToggleDay: (String) -> Unit,
    startMinutes: Int,
    endMinutes: Int,
    onTimeChange: (Int, Int) -> Unit,
    onScheduleBack: () -> Unit,
    onScheduleNext: () -> Unit,
    breakAllowance: Int?,
    onBreakAllowanceChange: (Int?) -> Unit,
    breakMinutes: Int,
    onBreakMinutesChange: (Int) -> Unit,
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    onBreaksBack: () -> Unit,
    onBreaksConfirm: () -> Unit,
    onOutsideCardClick: () -> Unit,
) {
    val colors = FocusTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
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
                modifier = Modifier.padding(top = 32.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(GridRowGap),
            ) {
                // Every group, then the add tile - two per row.
                val tiles: List<BlockedAppGroup?> = groups + null
                tiles.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(GridColumnGap)) {
                        row.forEach { group ->
                            if (group == null) {
                                AddAppGroupTile(onClick = onAddGroupClick)
                            } else {
                                AppGroupTile(
                                    name = group.name,
                                    appCount = group.apps.size,
                                    icon = group.apps.firstNotNullOfOrNull { it.icon },
                                    onClick = { onGroupClick(group) },
                                )
                            }
                        }
                    }
                }
            }
        }

        MainTabBar(
            selectedTab = MainTab.BLOCKED_APPS,
            onTabClick = onTabClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        )

        if (addStep != null) {
            // Scrim: dims everything behind the card; tapping it means "leave".
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background.copy(alpha = SCRIM_ALPHA))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOutsideCardClick,
                    ),
            )
            val cardModifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
            when (addStep) {
                AddGroupStep.APPS -> AppGroupAppsCard(
                    apps = pickerApps,
                    selectedPackages = selectedPackages,
                    onToggleApp = onToggleApp,
                    onClose = onPickerClose,
                    onNext = onPickerNext,
                    modifier = cardModifier,
                )
                AddGroupStep.SCHEDULE -> AppGroupScheduleCard(
                    activeDays = activeDays,
                    onToggleDay = onToggleDay,
                    startMinutes = startMinutes,
                    endMinutes = endMinutes,
                    onTimeChange = onTimeChange,
                    onBack = onScheduleBack,
                    onNext = onScheduleNext,
                    modifier = cardModifier,
                )
                AddGroupStep.BREAKS -> AppGroupBreakCard(
                    breakAllowance = breakAllowance,
                    onBreakAllowanceChange = onBreakAllowanceChange,
                    breakMinutes = breakMinutes,
                    onBreakMinutesChange = onBreakMinutesChange,
                    name = groupName,
                    onNameChange = onGroupNameChange,
                    onBack = onBreaksBack,
                    onConfirm = onBreaksConfirm,
                    modifier = cardModifier,
                )
            }
        }
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun AppFocusContentPreview() {
    FocusAppTheme {
        AppFocusContent(
            groups = generateFakeGroups(),
            onGroupClick = {},
            onAddGroupClick = {},
            onTabClick = {},
            addStep = null,
            pickerApps = null,
            selectedPackages = emptySet(),
            onToggleApp = {},
            onPickerClose = {},
            onPickerNext = {},
            activeDays = emptySet(),
            onToggleDay = {},
            startMinutes = DEFAULT_START_MINUTES,
            endMinutes = DEFAULT_END_MINUTES,
            onTimeChange = { _, _ -> },
            onScheduleBack = {},
            onScheduleNext = {},
            breakAllowance = null,
            onBreakAllowanceChange = {},
            breakMinutes = DEFAULT_BREAK_MINUTES,
            onBreakMinutesChange = {},
            groupName = "",
            onGroupNameChange = {},
            onBreaksBack = {},
            onBreaksConfirm = {},
            onOutsideCardClick = {},
        )
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun AppFocusContentPickerPreview() {
    FocusAppTheme {
        AppFocusContent(
            groups = generateFakeGroups(),
            onGroupClick = {},
            onAddGroupClick = {},
            onTabClick = {},
            addStep = AddGroupStep.APPS,
            pickerApps = List(12) { InstalledAppInfo("com.example.app$it", "App $it", icon = null) },
            selectedPackages = setOf("com.example.app0", "com.example.app2"),
            onToggleApp = {},
            onPickerClose = {},
            onPickerNext = {},
            activeDays = emptySet(),
            onToggleDay = {},
            startMinutes = DEFAULT_START_MINUTES,
            endMinutes = DEFAULT_END_MINUTES,
            onTimeChange = { _, _ -> },
            onScheduleBack = {},
            onScheduleNext = {},
            breakAllowance = null,
            onBreakAllowanceChange = {},
            breakMinutes = DEFAULT_BREAK_MINUTES,
            onBreakMinutesChange = {},
            groupName = "",
            onGroupNameChange = {},
            onBreaksBack = {},
            onBreaksConfirm = {},
            onOutsideCardClick = {},
        )
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun AppFocusContentSchedulePreview() {
    FocusAppTheme {
        AppFocusContent(
            groups = generateFakeGroups(),
            onGroupClick = {},
            onAddGroupClick = {},
            onTabClick = {},
            addStep = AddGroupStep.SCHEDULE,
            pickerApps = null,
            selectedPackages = emptySet(),
            onToggleApp = {},
            onPickerClose = {},
            onPickerNext = {},
            activeDays = setOf("Mon", "Wed", "Fri"),
            onToggleDay = {},
            startMinutes = DEFAULT_START_MINUTES,
            endMinutes = DEFAULT_END_MINUTES,
            onTimeChange = { _, _ -> },
            onScheduleBack = {},
            onScheduleNext = {},
            breakAllowance = null,
            onBreakAllowanceChange = {},
            breakMinutes = DEFAULT_BREAK_MINUTES,
            onBreakMinutesChange = {},
            groupName = "",
            onGroupNameChange = {},
            onBreaksBack = {},
            onBreaksConfirm = {},
            onOutsideCardClick = {},
        )
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun AppFocusContentBreaksPreview() {
    FocusAppTheme {
        AppFocusContent(
            groups = generateFakeGroups(),
            onGroupClick = {},
            onAddGroupClick = {},
            onTabClick = {},
            addStep = AddGroupStep.BREAKS,
            pickerApps = null,
            selectedPackages = emptySet(),
            onToggleApp = {},
            onPickerClose = {},
            onPickerNext = {},
            activeDays = emptySet(),
            onToggleDay = {},
            startMinutes = DEFAULT_START_MINUTES,
            endMinutes = DEFAULT_END_MINUTES,
            onTimeChange = { _, _ -> },
            onScheduleBack = {},
            onScheduleNext = {},
            breakAllowance = null,
            onBreakAllowanceChange = {},
            breakMinutes = DEFAULT_BREAK_MINUTES,
            onBreakMinutesChange = {},
            groupName = "",
            onGroupNameChange = {},
            onBreaksBack = {},
            onBreaksConfirm = {},
            onOutsideCardClick = {},
        )
    }
}
