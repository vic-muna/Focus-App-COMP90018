package com.example.focusapp.ui.screens.timefocus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.example.focusapp.ui.components.consumeTaps
import com.example.focusapp.ui.navigation.MainTab
import com.example.focusapp.ui.navigation.MainTabBar
import com.example.focusapp.ui.screens.home.AppItem
import com.example.focusapp.ui.screens.home.BlockedAppGroup
import com.example.focusapp.ui.screens.home.ClockTime
import com.example.focusapp.ui.screens.home.TimeSlot
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

private val HeaderHeight = 210.dp

/** How dark the screen behind a fly card gets (Figma dims it heavily). */
private const val SCRIM_ALPHA = 0.8f

/** Default range for a new time slot: 9:00 AM - 5:00 PM (same-day, see TimeSlotScheduleCard). */
private const val DEFAULT_START_MINUTES = 9 * 60
private const val DEFAULT_END_MINUTES = 17 * 60

/** Steps of the add/edit fly card. */
private enum class TimeSlotEditStep { APPS, SCHEDULE, LIMITS }

/** The add/edit card's fields while the user fills them in. Times are minutes after midnight. */
private data class TimeSlotDraft(
    val selectedPackages: Set<String> = emptySet(),
    val activeDays: Set<String> = emptySet(),
    val startMinutes: Int = DEFAULT_START_MINUTES,
    val endMinutes: Int = DEFAULT_END_MINUTES,
    val maxOpens: Int? = null,
    val maxMinutes: Int? = null,
    val name: String = "",
)

private fun BlockedAppGroup.toDraft() = TimeSlotDraft(
    selectedPackages = apps.map { it.packageName }.toSet(),
    activeDays = schedule.activeDays,
    startMinutes = schedule.start.hour * 60 + schedule.start.minute,
    endMinutes = schedule.end.hour * 60 + schedule.end.minute,
    maxOpens = maxOpensPerApp,
    // Snap to a wheel option (5-minute steps) - older limits may be any minute count.
    maxMinutes = maxMinutesPerApp?.let { minutes ->
        LimitMinuteOptions.filterNotNull().minByOrNull { abs(it - minutes) }
    },
    name = name,
)

/**
 * Figma: "Time Focuse" - the Schedule tab. A header illustration over the
 * list of time slots (each with an on/off switch) and an "add" card.
 *
 * Each slot is one of David's Scheduled Limits groups ([BlockedAppGroup]):
 * its time, apps and daily limits live together, so there's no separate
 * app-group page.
 *  - tap a slot: its read-only summary card; the pencil opens the
 *    three-step fly card, prefilled
 *  - "+": the same card, empty - apps, then days + time, then limits + name
 *  - hold a slot: delete it (after confirming)
 * Tapping outside closes the summary, or asks before discarding what's been
 * filled in.
 */
@Composable
fun TimeFocusScreen(
    groups: List<BlockedAppGroup>,
    onGroupsChange: (List<BlockedAppGroup>) -> Unit,
    onTabClick: (MainTab) -> Unit,
) {
    val context = LocalContext.current
    // The slot whose summary card is open, if any.
    var viewingGroupId by remember { mutableStateOf<String?>(null) }
    val viewingGroup = groups.find { it.id == viewingGroupId }
    // null = no fly card open.
    var editStep by remember { mutableStateOf<TimeSlotEditStep?>(null) }
    // Set while editing an existing slot (null while adding a new one).
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf(TimeSlotDraft()) }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BlockedAppGroup?>(null) }

    // Loaded once, the first time the card opens - it's slow with many apps installed.
    LaunchedEffect(editStep != null) {
        if (editStep != null && installedApps == null) {
            installedApps = withContext(Dispatchers.Default) { getLaunchableApps(context) }
        }
    }

    fun updateGroup(groupId: String, transform: (BlockedAppGroup) -> BlockedAppGroup) {
        onGroupsChange(groups.map { if (it.id == groupId) transform(it) else it })
    }

    fun closeEditor() {
        editStep = null
        editingGroupId = null
        draft = TimeSlotDraft()
        confirmDiscard = false
    }

    fun startEditing(group: BlockedAppGroup) {
        viewingGroupId = null
        editingGroupId = group.id
        draft = group.toDraft()
        editStep = TimeSlotEditStep.APPS
    }

    fun saveDraft() {
        val installed = installedApps.orEmpty()
        val picked = installed
            .filter { it.packageName in draft.selectedPackages }
            .map { AppItem(packageName = it.packageName, name = it.label, isBlocked = true, icon = it.icon) }
        // Keep a slot's apps that the picker didn't list (e.g. no launcher icon) if still ticked.
        val kept = groups.find { it.id == editingGroupId }?.apps.orEmpty()
            .filter { app -> app.packageName in draft.selectedPackages && installed.none { it.packageName == app.packageName } }
        val schedule = TimeSlot(
            activeDays = draft.activeDays,
            start = ClockTime(draft.startMinutes / 60, draft.startMinutes % 60),
            end = ClockTime(draft.endMinutes / 60, draft.endMinutes % 60),
        )
        val groupId = editingGroupId
        if (groupId == null) {
            onGroupsChange(
                groups + BlockedAppGroup(
                    id = "group_${System.currentTimeMillis()}",
                    name = draft.name.trim(),
                    apps = picked + kept,
                    schedule = schedule,
                    maxOpensPerApp = draft.maxOpens,
                    maxMinutesPerApp = draft.maxMinutes,
                )
            )
        } else {
            updateGroup(groupId) {
                it.copy(
                    name = draft.name.trim(),
                    apps = picked + kept,
                    schedule = schedule,
                    maxOpensPerApp = draft.maxOpens,
                    maxMinutesPerApp = draft.maxMinutes,
                )
            }
        }
        closeEditor()
    }

    BackHandler(enabled = editStep != null || viewingGroup != null) {
        when (editStep) {
            TimeSlotEditStep.LIMITS -> editStep = TimeSlotEditStep.SCHEDULE
            TimeSlotEditStep.SCHEDULE -> editStep = TimeSlotEditStep.APPS
            TimeSlotEditStep.APPS -> closeEditor()
            null -> viewingGroupId = null
        }
    }

    TimeFocusContent(
        groups = groups,
        onEnabledChange = { group, enabled -> updateGroup(group.id) { it.copy(enabled = enabled) } },
        onGroupClick = { viewingGroupId = it.id },
        onGroupLongClick = { pendingDelete = it },
        onAddClick = {
            editingGroupId = null
            draft = TimeSlotDraft()
            editStep = TimeSlotEditStep.APPS
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
        onEditorConfirm = ::saveDraft,
        onOutsideCardClick = {
            if (editStep != null) confirmDiscard = true else viewingGroupId = null
        },
    )

    if (confirmDiscard) {
        val isEditing = editingGroupId != null
        FocusConfirmDialog(
            title = if (isEditing) "Cancel editing?" else "Cancel new time slot?",
            message = if (isEditing) "Your changes to this time slot will be discarded."
            else "The apps and settings you picked for this time slot will be discarded.",
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
            message = "This time slot and its app limits will be removed.",
            confirmLabel = "Delete",
            confirmColor = FocusTheme.colors.rejection,
            onConfirm = {
                pendingDelete = null
                onGroupsChange(groups.filterNot { it.id == group.id })
                if (viewingGroupId == group.id) viewingGroupId = null
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
    editStep: TimeSlotEditStep?,
    viewingGroup: BlockedAppGroup?,
    onEditViewingGroup: () -> Unit,
    pickerApps: List<InstalledAppInfo>?,
    draft: TimeSlotDraft,
    onDraftChange: (TimeSlotDraft) -> Unit,
    onStepChange: (TimeSlotEditStep) -> Unit,
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
            // consumeTaps: only taps outside the card's background count as "outside".
            val cardModifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .consumeTaps()
            when (editStep) {
                TimeSlotEditStep.APPS -> TimeSlotAppsCard(
                    apps = pickerApps,
                    selectedPackages = draft.selectedPackages,
                    onToggleApp = { pkg ->
                        val picked = draft.selectedPackages
                        onDraftChange(draft.copy(selectedPackages = if (pkg in picked) picked - pkg else picked + pkg))
                    },
                    onClose = onEditorClose,
                    onNext = { onStepChange(TimeSlotEditStep.SCHEDULE) },
                    modifier = cardModifier,
                )
                TimeSlotEditStep.SCHEDULE -> TimeSlotScheduleCard(
                    activeDays = draft.activeDays,
                    onToggleDay = { day ->
                        val days = draft.activeDays
                        onDraftChange(draft.copy(activeDays = if (day in days) days - day else days + day))
                    },
                    startMinutes = draft.startMinutes,
                    endMinutes = draft.endMinutes,
                    onTimeChange = { start, end -> onDraftChange(draft.copy(startMinutes = start, endMinutes = end)) },
                    onBack = { onStepChange(TimeSlotEditStep.APPS) },
                    onNext = { onStepChange(TimeSlotEditStep.LIMITS) },
                    modifier = cardModifier,
                )
                TimeSlotEditStep.LIMITS -> TimeSlotLimitsCard(
                    maxOpens = draft.maxOpens,
                    onMaxOpensChange = { onDraftChange(draft.copy(maxOpens = it)) },
                    maxMinutes = draft.maxMinutes,
                    onMaxMinutesChange = { onDraftChange(draft.copy(maxMinutes = it)) },
                    name = draft.name,
                    onNameChange = { onDraftChange(draft.copy(name = it)) },
                    onBack = { onStepChange(TimeSlotEditStep.SCHEDULE) },
                    onConfirm = onEditorConfirm,
                    modifier = cardModifier,
                )
                null -> viewingGroup?.let { group ->
                    TimeSlotDetailCard(group = group, onEdit = onEditViewingGroup, modifier = cardModifier)
                }
            }
        }
    }
}

/** e.g. "06:10am-08:00am", as on the Figma cards. */
private fun formatSlotRange(slot: TimeSlot): String {
    fun ClockTime.label(): String = formatted().let { (time, suffix) -> time + suffix.lowercase() }
    return "${slot.start.label()}-${slot.end.label()}"
}

private val previewGroups = listOf(
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
)

@Composable
private fun PreviewContent(
    viewingGroup: BlockedAppGroup? = null,
    editStep: TimeSlotEditStep? = null,
    draft: TimeSlotDraft = TimeSlotDraft(),
) {
    FocusAppTheme {
        TimeFocusContent(
            groups = previewGroups,
            onEnabledChange = { _, _ -> },
            onGroupClick = {},
            onGroupLongClick = {},
            onAddClick = {},
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
private fun TimeFocusContentPreview() = PreviewContent()

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun TimeFocusContentDetailPreview() =
    PreviewContent(viewingGroup = previewGroups.first().copy(maxOpensPerApp = 3, maxMinutesPerApp = 30))


@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun TimeFocusContentAppsPreview() =
    PreviewContent(editStep = TimeSlotEditStep.APPS, draft = TimeSlotDraft(selectedPackages = setOf("com.example.app0")))

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun TimeFocusContentSchedulePreview() =
    PreviewContent(editStep = TimeSlotEditStep.SCHEDULE, draft = TimeSlotDraft(activeDays = setOf("Mon", "Wed", "Fri")))

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun TimeFocusContentLimitsPreview() =
    PreviewContent(editStep = TimeSlotEditStep.LIMITS, draft = TimeSlotDraft(maxOpens = 3, maxMinutes = 30))
