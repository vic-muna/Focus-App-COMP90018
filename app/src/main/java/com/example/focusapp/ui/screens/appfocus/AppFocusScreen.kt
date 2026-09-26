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

/** Steps of the add/edit-group fly card. */
private enum class GroupEditStep { APPS, SCHEDULE, BREAKS }

/** The add/edit card's fields while the user fills them in. Times are minutes after midnight. */
private data class AppGroupDraft(
    val selectedPackages: Set<String> = emptySet(),
    val activeDays: Set<String> = emptySet(),
    val startMinutes: Int = DEFAULT_START_MINUTES,
    val endMinutes: Int = DEFAULT_END_MINUTES,
    val breakAllowance: Int? = null,
    val breakMinutes: Int = DEFAULT_BREAK_MINUTES,
    val name: String = "",
)

private fun BlockedAppGroup.toDraft() = AppGroupDraft(
    selectedPackages = apps.map { it.packageName }.toSet(),
    activeDays = schedule.activeDays,
    startMinutes = schedule.start.hour * 60 + schedule.start.minute,
    endMinutes = schedule.end.hour * 60 + schedule.end.minute,
    breakAllowance = breakAllowance,
    breakMinutes = breakMinutes,
    name = name,
)

/** What the add/edit card produces on confirm. */
data class AppGroupInput(
    val name: String,
    val apps: List<AppItem>,
    val schedule: TimeSlot,
    val breakAllowance: Int,
    val breakMinutes: Int,
)

/**
 * Figma: "App Focuse" - the Blocked Apps tab. A header illustration over a
 * two-column grid of app-group tiles, ending with an "add group" tile.
 *  - tap a tile: read-only detail card; its pencil opens the edit flow
 *  - hold a tile: delete it (after confirming)
 *  - add tile: the same three-step card (apps, schedule, breaks + name),
 *    empty instead of prefilled
 * Tapping outside a card closes the detail card, or asks before discarding
 * an add/edit in progress.
 */
@Composable
fun AppFocusScreen(
    groups: List<BlockedAppGroup>,
    onCreateGroup: (AppGroupInput) -> Unit,
    onUpdateGroup: (groupId: String, AppGroupInput) -> Unit,
    onDeleteGroup: (BlockedAppGroup) -> Unit,
    onTabClick: (MainTab) -> Unit,
) {
    val context = LocalContext.current
    var viewingGroupId by remember { mutableStateOf<String?>(null) }
    // null = no add/edit card open.
    var editStep by remember { mutableStateOf<GroupEditStep?>(null) }
    // Set while editing an existing group (null while adding a new one).
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf(AppGroupDraft()) }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BlockedAppGroup?>(null) }

    val viewingGroup = groups.find { it.id == viewingGroupId }

    // Loaded once, the first time the picker opens - it's slow with many apps installed.
    LaunchedEffect(editStep != null) {
        if (editStep != null && installedApps == null) {
            installedApps = withContext(Dispatchers.Default) { getLaunchableApps(context) }
        }
    }

    fun closeEditor() {
        editStep = null
        editingGroupId = null
        draft = AppGroupDraft()
        confirmDiscard = false
    }

    fun startEditing(group: BlockedAppGroup) {
        viewingGroupId = null
        editingGroupId = group.id
        draft = group.toDraft()
        editStep = GroupEditStep.APPS
    }

    fun buildInput(): AppGroupInput {
        val installed = installedApps.orEmpty()
        val picked = installed
            .filter { it.packageName in draft.selectedPackages }
            .map { AppItem(packageName = it.packageName, name = it.label, isBlocked = true, icon = it.icon) }
        // Keep a group's apps that the picker didn't list (e.g. no launcher icon) if still ticked.
        val kept = groups.find { it.id == editingGroupId }?.apps.orEmpty()
            .filter { app -> app.packageName in draft.selectedPackages && installed.none { it.packageName == app.packageName } }
        return AppGroupInput(
            name = draft.name.trim(),
            apps = picked + kept,
            schedule = TimeSlot(
                activeDays = draft.activeDays,
                start = ClockTime(draft.startMinutes / 60, draft.startMinutes % 60),
                end = ClockTime(draft.endMinutes / 60, draft.endMinutes % 60),
            ),
            breakAllowance = draft.breakAllowance ?: 0,
            breakMinutes = draft.breakMinutes,
        )
    }

    BackHandler(enabled = editStep != null || viewingGroup != null) {
        when (editStep) {
            GroupEditStep.BREAKS -> editStep = GroupEditStep.SCHEDULE
            GroupEditStep.SCHEDULE -> editStep = GroupEditStep.APPS
            GroupEditStep.APPS -> closeEditor()
            null -> viewingGroupId = null
        }
    }

    AppFocusContent(
        groups = groups,
        onGroupClick = { viewingGroupId = it.id },
        onGroupLongClick = { pendingDelete = it },
        onAddGroupClick = {
            editingGroupId = null
            draft = AppGroupDraft()
            editStep = GroupEditStep.APPS
        },
        onTabClick = onTabClick,
        viewingGroup = viewingGroup,
        onEditViewingGroup = { viewingGroup?.let(::startEditing) },
        editStep = editStep,
        pickerApps = installedApps,
        draft = draft,
        onDraftChange = { draft = it },
        onStepChange = { editStep = it },
        onEditorClose = ::closeEditor,
        onEditorConfirm = {
            val input = buildInput()
            val groupId = editingGroupId
            if (groupId == null) onCreateGroup(input) else onUpdateGroup(groupId, input)
            closeEditor()
        },
        onOutsideCardClick = {
            if (editStep != null) confirmDiscard = true else viewingGroupId = null
        },
    )

    if (confirmDiscard) {
        val isEditing = editingGroupId != null
        FocusConfirmDialog(
            title = if (isEditing) "Cancel editing?" else "Cancel new group?",
            message = if (isEditing) "Your changes to this group will be discarded."
            else "The apps and settings you picked for this group will be discarded.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            confirmColor = FocusTheme.colors.rejection,
            onConfirm = ::closeEditor,
            onDismiss = { confirmDiscard = false },
        )
    }

    pendingDelete?.let { group ->
        FocusConfirmDialog(
            title = "Delete \"${group.name}\"?",
            message = "This app group and its schedule will be removed.",
            confirmLabel = "Delete",
            confirmColor = FocusTheme.colors.rejection,
            onConfirm = {
                pendingDelete = null
                if (viewingGroupId == group.id) viewingGroupId = null
                onDeleteGroup(group)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** Stateless layout of [AppFocusScreen]. */
@Composable
private fun AppFocusContent(
    groups: List<BlockedAppGroup>,
    onGroupClick: (BlockedAppGroup) -> Unit,
    onGroupLongClick: (BlockedAppGroup) -> Unit,
    onAddGroupClick: () -> Unit,
    onTabClick: (MainTab) -> Unit,
    viewingGroup: BlockedAppGroup?,
    onEditViewingGroup: () -> Unit,
    editStep: GroupEditStep?,
    pickerApps: List<InstalledAppInfo>?,
    draft: AppGroupDraft,
    onDraftChange: (AppGroupDraft) -> Unit,
    onStepChange: (GroupEditStep) -> Unit,
    onEditorClose: () -> Unit,
    onEditorConfirm: () -> Unit,
    onOutsideCardClick: () -> Unit,
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
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onGroupLongClick(group)
                                    },
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

        if (editStep != null || viewingGroup != null) {
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
            when (editStep) {
                GroupEditStep.APPS -> AppGroupAppsCard(
                    apps = pickerApps,
                    selectedPackages = draft.selectedPackages,
                    onToggleApp = { pkg ->
                        val picked = draft.selectedPackages
                        onDraftChange(draft.copy(selectedPackages = if (pkg in picked) picked - pkg else picked + pkg))
                    },
                    onClose = onEditorClose,
                    onNext = { onStepChange(GroupEditStep.SCHEDULE) },
                    modifier = cardModifier,
                )
                GroupEditStep.SCHEDULE -> AppGroupScheduleCard(
                    activeDays = draft.activeDays,
                    onToggleDay = { day ->
                        val days = draft.activeDays
                        onDraftChange(draft.copy(activeDays = if (day in days) days - day else days + day))
                    },
                    startMinutes = draft.startMinutes,
                    endMinutes = draft.endMinutes,
                    onTimeChange = { start, end -> onDraftChange(draft.copy(startMinutes = start, endMinutes = end)) },
                    onBack = { onStepChange(GroupEditStep.APPS) },
                    onNext = { onStepChange(GroupEditStep.BREAKS) },
                    modifier = cardModifier,
                )
                GroupEditStep.BREAKS -> AppGroupBreakCard(
                    breakAllowance = draft.breakAllowance,
                    onBreakAllowanceChange = { onDraftChange(draft.copy(breakAllowance = it)) },
                    breakMinutes = draft.breakMinutes,
                    onBreakMinutesChange = { onDraftChange(draft.copy(breakMinutes = it)) },
                    name = draft.name,
                    onNameChange = { onDraftChange(draft.copy(name = it)) },
                    onBack = { onStepChange(GroupEditStep.SCHEDULE) },
                    onConfirm = onEditorConfirm,
                    modifier = cardModifier,
                )
                null -> viewingGroup?.let { group ->
                    AppGroupDetailCard(group = group, onEdit = onEditViewingGroup, modifier = cardModifier)
                }
            }
        }
    }
}

@Composable
private fun PreviewContent(
    viewingGroup: BlockedAppGroup? = null,
    editStep: GroupEditStep? = null,
    draft: AppGroupDraft = AppGroupDraft(),
) {
    FocusAppTheme {
        AppFocusContent(
            groups = generateFakeGroups(),
            onGroupClick = {},
            onGroupLongClick = {},
            onAddGroupClick = {},
            onTabClick = {},
            viewingGroup = viewingGroup,
            onEditViewingGroup = {},
            editStep = editStep,
            pickerApps = List(12) { InstalledAppInfo("com.example.app$it", "App $it", icon = null) },
            draft = draft,
            onDraftChange = {},
            onStepChange = {},
            onEditorClose = {},
            onEditorConfirm = {},
            onOutsideCardClick = {},
        )
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun AppFocusContentPreview() = PreviewContent()

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun AppFocusContentDetailPreview() =
    PreviewContent(viewingGroup = generateFakeGroups().first().copy(breakAllowance = 5))

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun AppFocusContentAppsPreview() =
    PreviewContent(editStep = GroupEditStep.APPS, draft = AppGroupDraft(selectedPackages = setOf("com.example.app0")))

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun AppFocusContentSchedulePreview() =
    PreviewContent(editStep = GroupEditStep.SCHEDULE, draft = AppGroupDraft(activeDays = setOf("Mon", "Wed", "Fri")))

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun AppFocusContentBreaksPreview() =
    PreviewContent(editStep = GroupEditStep.BREAKS)
