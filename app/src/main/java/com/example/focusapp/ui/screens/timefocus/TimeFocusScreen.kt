package com.example.focusapp.ui.screens.timefocus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.data.apps.InstalledAppInfo
import com.example.focusapp.data.blocking.BlockedAppGroup
import com.example.focusapp.data.blocking.ClockTime
import com.example.focusapp.data.blocking.TimeSlot
import com.example.focusapp.ui.common.pickedApps
import com.example.focusapp.ui.common.rememberInstalledApps
import com.example.focusapp.ui.common.toggle
import com.example.focusapp.ui.components.button.NextButton
import com.example.focusapp.ui.components.card.AppSelectCard
import com.example.focusapp.ui.components.card.DeleteDialog
import com.example.focusapp.ui.components.card.DiscardDialog
import com.example.focusapp.ui.components.layout.GroupListLayout
import com.example.focusapp.ui.navigation.MainTab
import com.example.focusapp.ui.theme.FocusAppTheme
import kotlin.math.abs

private val HeaderHeight = 210.dp

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
 * Each slot is one [BlockedAppGroup]:
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
    // The slot whose summary card is open, if any.
    var viewingGroupId by remember { mutableStateOf<String?>(null) }
    val viewingGroup = groups.find { it.id == viewingGroupId }
    // null = no fly card open.
    var editStep by remember { mutableStateOf<TimeSlotEditStep?>(null) }
    // Set while editing an existing slot (null while adding a new one).
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf(TimeSlotDraft()) }
    val installedApps = rememberInstalledApps(shouldLoad = editStep != null)
    var confirmDiscard by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BlockedAppGroup?>(null) }

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
        val original = groups.find { it.id == editingGroupId }
        val apps = pickedApps(installedApps, draft.selectedPackages, original?.apps.orEmpty())
        val schedule = TimeSlot(
            activeDays = draft.activeDays,
            start = ClockTime(draft.startMinutes / 60, draft.startMinutes % 60),
            end = ClockTime(draft.endMinutes / 60, draft.endMinutes % 60),
        )
        val newGroup = BlockedAppGroup(id = "group_${System.currentTimeMillis()}", name = "", apps = emptyList(), schedule = schedule)
        val saved = (original ?: newGroup).copy(
            name = draft.name.trim(),
            apps = apps,
            schedule = schedule,
            maxOpensPerApp = draft.maxOpens,
            maxMinutesPerApp = draft.maxMinutes,
        )
        onGroupsChange(
            if (original == null) groups + saved
            else groups.map { if (it.id == original.id) saved else it }
        )
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
        DiscardDialog(
            title = if (isEditing) "Cancel editing?" else "Cancel new time slot?",
            message = if (isEditing) "Your changes to this time slot will be discarded."
            else "The apps and settings you picked for this time slot will be discarded.",
            onDiscard = ::closeEditor,
            onKeepEditing = { confirmDiscard = false },
        )
    }

    pendingDelete?.let { group ->
        DeleteDialog(
            name = group.name,
            message = "This time slot and its app limits will be removed.",
            onDelete = {
                pendingDelete = null
                if (viewingGroupId == group.id) viewingGroupId = null
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
    GroupListLayout(
        selectedTab = MainTab.SCHEDULE,
        onTabClick = onTabClick,
        addLabel = "Add time slot",
        onAddClick = onAddClick,
        showCard = editStep != null || viewingGroup != null,
        onOutsideCardClick = onOutsideCardClick,
        header = {
            Image(
                painter = painterResource(R.drawable.img_app_focus_header),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeaderHeight),
            )
        },
        card = { cardModifier ->
            when (editStep) {
                TimeSlotEditStep.APPS -> AppSelectCard(
                    title = "Apps Group",
                    apps = pickerApps,
                    selectedPackages = draft.selectedPackages,
                    onToggleApp = { pkg -> onDraftChange(draft.copy(selectedPackages = draft.selectedPackages.toggle(pkg))) },
                    onClose = onEditorClose,
                    actionButton = {
                        NextButton(
                            onClick = { onStepChange(TimeSlotEditStep.SCHEDULE) },
                            enabled = draft.selectedPackages.isNotEmpty(),
                        )
                    },
                    modifier = cardModifier,
                )
                TimeSlotEditStep.SCHEDULE -> TimeSlotScheduleCard(
                    activeDays = draft.activeDays,
                    onToggleDay = { day -> onDraftChange(draft.copy(activeDays = draft.activeDays.toggle(day))) },
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
        },
    ) {
        groups.forEach { group ->
            TimeSlotCard(
                name = group.name,
                timeLabel = formatSlotRange(group.schedule),
                activeDays = group.schedule.activeDays,
                enabled = group.enabled,
                onEnabledChange = { onEnabledChange(group, it) },
                onClick = { onGroupClick(group) },
                onLongClick = { onGroupLongClick(group) },
            )
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
