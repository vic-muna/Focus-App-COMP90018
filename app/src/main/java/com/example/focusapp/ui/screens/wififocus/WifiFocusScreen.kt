package com.example.focusapp.ui.screens.wififocus

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.data.apps.InstalledAppInfo
import com.example.focusapp.data.blocking.BlockedAppGroup
import com.example.focusapp.data.blocking.defaultTimeSlot
import com.example.focusapp.data.wifi.WifiCheckResult
import com.example.focusapp.data.wifi.WifiHistoryStorage
import com.example.focusapp.data.wifi.checkCurrentWifi
import com.example.focusapp.data.wifi.hasLocationPermissionForWifi
import com.example.focusapp.ui.common.pickedApps
import com.example.focusapp.ui.common.rememberInstalledApps
import com.example.focusapp.ui.common.rememberLocationPermissionState
import com.example.focusapp.ui.common.toggle
import com.example.focusapp.ui.components.button.BackButton
import com.example.focusapp.ui.components.button.NextButton
import com.example.focusapp.ui.components.card.AppSelectCard
import com.example.focusapp.ui.components.card.DeleteDialog
import com.example.focusapp.ui.components.card.DiscardDialog
import com.example.focusapp.ui.components.layout.GroupListLayout
import com.example.focusapp.ui.navigation.MainTab
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val HeaderHeight = 210.dp

/** Steps of the add/edit fly card. */
private enum class WifiEditStep { NETWORK, APPS, NAME }

/** The add/edit card's fields while the user fills them in. */
private data class WifiDraft(
    val ssid: String? = null,
    val selectedPackages: Set<String> = emptySet(),
    val name: String = "",
)

private fun BlockedAppGroup.toDraft() = WifiDraft(
    ssid = id,
    selectedPackages = apps.map { it.packageName }.toSet(),
    name = name,
)

/**
 * The Wi-Fi tab. A Wi-Fi header over the list of watched networks (each
 * with an on/off switch) and an "add" card - the same flow as Time Focus.
 *
 * Each entry is a [BlockedAppGroup] whose id is the network's SSID: while
 * the phone is on that Wi-Fi, a focus session blocks that entry's apps.
 *  - tap an entry: its read-only summary; the pencil opens the edit steps
 *  - "+": network (the one the phone is on now, one it's been on before,
 *    or a typed name), then apps, then name
 *  - hold an entry: delete it (after confirming)
 * Tapping outside closes the summary, or asks before discarding an edit.
 */
@Composable
fun WifiFocusScreen(
    groups: List<BlockedAppGroup>,
    onGroupsChange: (List<BlockedAppGroup>) -> Unit,
    onTabClick: (MainTab) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewingGroupId by remember { mutableStateOf<String?>(null) }
    val viewingGroup = groups.find { it.id == viewingGroupId }
    // null = no fly card open.
    var editStep by remember { mutableStateOf<WifiEditStep?>(null) }
    // The SSID of the entry being edited (null while adding a new one).
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf(WifiDraft()) }
    var checkResult by remember { mutableStateOf<WifiCheckResult?>(null) }
    val currentSsid = (checkResult as? WifiCheckResult.Connected)?.ssid
    // Networks seen before (see WifiHistoryStorage), loaded when the network step opens.
    val history = remember { WifiHistoryStorage(context) }
    var knownSsids by remember { mutableStateOf<List<String>>(emptyList()) }
    var manualSsid by remember { mutableStateOf("") }
    val installedApps = rememberInstalledApps(shouldLoad = editStep != null)
    var confirmDiscard by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BlockedAppGroup?>(null) }

    // Reading the Wi-Fi name needs PRECISE location (see hasLocationPermissionForWifi).
    val permissionState = rememberLocationPermissionState()
    var pendingCheck by remember { mutableStateOf(false) }

    // A found network is remembered, and picked if nothing is picked yet.
    fun onWifiChecked(result: WifiCheckResult) {
        checkResult = result
        if (result is WifiCheckResult.Connected) {
            history.remember(result.ssid)
            if (draft.ssid == null) draft = draft.copy(ssid = result.ssid)
        }
    }

    fun checkWifi() {
        if (hasLocationPermissionForWifi(context)) {
            checkResult = null
            scope.launch { onWifiChecked(checkCurrentWifi(context)) }
        } else {
            pendingCheck = true
            permissionState.request()
        }
    }

    // Checks once the user answers the permission dialog - the result then says what's still missing.
    LaunchedEffect(permissionState.resultCount) {
        if (pendingCheck && permissionState.resultCount > 0) {
            pendingCheck = false
            onWifiChecked(checkCurrentWifi(context))
        }
    }

    LaunchedEffect(editStep == WifiEditStep.NETWORK) {
        if (editStep == WifiEditStep.NETWORK) {
            knownSsids = withContext(Dispatchers.IO) { history.getKnownSsids() }
        }
    }

    fun updateGroup(groupId: String, transform: (BlockedAppGroup) -> BlockedAppGroup) {
        onGroupsChange(groups.map { if (it.id == groupId) transform(it) else it })
    }

    fun closeEditor() {
        editStep = null
        editingGroupId = null
        draft = WifiDraft()
        checkResult = null
        manualSsid = ""
        confirmDiscard = false
    }

    fun startAdding() {
        editingGroupId = null
        draft = WifiDraft()
        editStep = WifiEditStep.NETWORK
        checkWifi()
    }

    fun startEditing(group: BlockedAppGroup) {
        viewingGroupId = null
        editingGroupId = group.id
        draft = group.toDraft()
        editStep = WifiEditStep.NETWORK
        checkWifi()
    }

    fun saveDraft() {
        val ssid = draft.ssid ?: return
        val original = groups.find { it.id == editingGroupId }
        val apps = pickedApps(installedApps, draft.selectedPackages, original?.apps.orEmpty())
        // The schedule and limit fields aren't used for Wi-Fi entries.
        val newGroup = BlockedAppGroup(id = ssid, name = "", apps = emptyList(), schedule = defaultTimeSlot())
        val saved = (original ?: newGroup).copy(id = ssid, name = draft.name.trim(), apps = apps)
        onGroupsChange(
            if (original == null) groups + saved
            else groups.map { if (it.id == original.id) saved else it }
        )
        closeEditor()
    }

    fun openSettingsFor(result: WifiCheckResult) {
        val intent = when (result) {
            WifiCheckResult.NeedsPreciseLocation ->
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            WifiCheckResult.LocationServicesOff -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            else -> return
        }
        context.startActivity(intent)
    }

    BackHandler(enabled = editStep != null || viewingGroup != null) {
        when (editStep) {
            WifiEditStep.NAME -> editStep = WifiEditStep.APPS
            WifiEditStep.APPS -> editStep = WifiEditStep.NETWORK
            WifiEditStep.NETWORK -> closeEditor()
            null -> viewingGroupId = null
        }
    }

    WifiFocusContent(
        groups = groups,
        onEnabledChange = { group, enabled -> updateGroup(group.id) { it.copy(enabled = enabled) } },
        onGroupClick = { viewingGroupId = it.id },
        onGroupLongClick = { pendingDelete = it },
        onAddClick = ::startAdding,
        onTabClick = onTabClick,
        viewingGroup = viewingGroup,
        onEditViewingGroup = { viewingGroup?.let(::startEditing) },
        editStep = editStep,
        draft = draft,
        currentSsid = currentSsid,
        checkResult = checkResult,
        // The entry being edited keeps its own network on the list.
        knownSsids = (listOfNotNull(editingGroupId) + knownSsids).distinct(),
        addedSsids = groups.map { it.id }.toSet() - setOfNotNull(editingGroupId),
        manualSsid = manualSsid,
        onSelectSsid = { ssid ->
            draft = draft.copy(ssid = ssid)
            manualSsid = ""
        },
        onManualSsidChange = { text ->
            manualSsid = text
            draft = draft.copy(ssid = text.trim().ifBlank { null })
        },
        pickerApps = installedApps,
        onDraftChange = { draft = it },
        onStepChange = { step ->
            // Suggest the network's name as the entry's name.
            if (step == WifiEditStep.NAME && draft.name.isBlank()) draft = draft.copy(name = draft.ssid.orEmpty())
            editStep = step
        },
        onCheckAgain = ::checkWifi,
        onResultAction = ::openSettingsFor,
        onEditorClose = ::closeEditor,
        onEditorConfirm = ::saveDraft,
        onOutsideCardClick = {
            if (editStep != null) confirmDiscard = true else viewingGroupId = null
        },
    )

    if (confirmDiscard) {
        val isEditing = editingGroupId != null
        DiscardDialog(
            title = if (isEditing) "Cancel editing?" else "Cancel new Wi-Fi?",
            message = if (isEditing) "Your changes to this Wi-Fi will be discarded."
            else "The network and apps you picked will be discarded.",
            onDiscard = ::closeEditor,
            onKeepEditing = { confirmDiscard = false },
        )
    }

    pendingDelete?.let { group ->
        DeleteDialog(
            name = group.name,
            message = "This Wi-Fi and its blocked apps will be removed.",
            onDelete = {
                pendingDelete = null
                if (viewingGroupId == group.id) viewingGroupId = null
                onGroupsChange(groups.filterNot { it.id == group.id })
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** Stateless layout of [WifiFocusScreen]. */
@Composable
private fun WifiFocusContent(
    groups: List<BlockedAppGroup>,
    onEnabledChange: (BlockedAppGroup, Boolean) -> Unit,
    onGroupClick: (BlockedAppGroup) -> Unit,
    onGroupLongClick: (BlockedAppGroup) -> Unit,
    onAddClick: () -> Unit,
    onTabClick: (MainTab) -> Unit,
    viewingGroup: BlockedAppGroup?,
    onEditViewingGroup: () -> Unit,
    editStep: WifiEditStep?,
    draft: WifiDraft,
    currentSsid: String?,
    checkResult: WifiCheckResult?,
    knownSsids: List<String>,
    addedSsids: Set<String>,
    manualSsid: String,
    onSelectSsid: (String) -> Unit,
    onManualSsidChange: (String) -> Unit,
    pickerApps: List<InstalledAppInfo>?,
    onDraftChange: (WifiDraft) -> Unit,
    onStepChange: (WifiEditStep) -> Unit,
    onCheckAgain: () -> Unit,
    onResultAction: (WifiCheckResult) -> Unit,
    onEditorClose: () -> Unit,
    onEditorConfirm: () -> Unit,
    onOutsideCardClick: () -> Unit,
) {
    GroupListLayout(
        selectedTab = MainTab.WIFI_SOURCE,
        onTabClick = onTabClick,
        addLabel = "Add Wi-Fi network",
        onAddClick = onAddClick,
        showCard = editStep != null || viewingGroup != null,
        onOutsideCardClick = onOutsideCardClick,
        emptyText = if (groups.isEmpty()) "No Wi-Fi yet.\nConnect to a network, then tap + to add it." else null,
        header = {
            // TODO(design): swap for a Wi-Fi header illustration once there is one.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeaderHeight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.wifi),
                    contentDescription = null,
                    tint = FocusTheme.colors.accent,
                    modifier = Modifier.size(width = 138.dp, height = 108.dp),
                )
            }
        },
        card = { cardModifier ->
            when (editStep) {
                WifiEditStep.NETWORK -> WifiNetworkStepCard(
                    selectedSsid = draft.ssid,
                    currentSsid = currentSsid,
                    checkResult = checkResult,
                    knownSsids = knownSsids,
                    addedSsids = addedSsids,
                    manualSsid = manualSsid,
                    onSelect = onSelectSsid,
                    onManualChange = onManualSsidChange,
                    onCheckAgain = onCheckAgain,
                    onResultAction = onResultAction,
                    onClose = onEditorClose,
                    onNext = { onStepChange(WifiEditStep.APPS) },
                    modifier = cardModifier,
                )
                WifiEditStep.APPS -> AppSelectCard(
                    title = "Blocked Apps",
                    apps = pickerApps,
                    selectedPackages = draft.selectedPackages,
                    onToggleApp = { pkg -> onDraftChange(draft.copy(selectedPackages = draft.selectedPackages.toggle(pkg))) },
                    onClose = onEditorClose,
                    leadingButton = { BackButton(onClick = { onStepChange(WifiEditStep.NETWORK) }) },
                    actionButton = {
                        NextButton(
                            onClick = { onStepChange(WifiEditStep.NAME) },
                            enabled = draft.selectedPackages.isNotEmpty(),
                        )
                    },
                    modifier = cardModifier,
                )
                WifiEditStep.NAME -> WifiNameStepCard(
                    name = draft.name,
                    onNameChange = { onDraftChange(draft.copy(name = it)) },
                    onBack = { onStepChange(WifiEditStep.APPS) },
                    onConfirm = onEditorConfirm,
                    modifier = cardModifier,
                )
                null -> viewingGroup?.let { group ->
                    WifiDetailCard(group = group, onEdit = onEditViewingGroup, modifier = cardModifier)
                }
            }
        },
    ) {
        groups.forEach { group ->
            WifiNetworkCard(
                name = group.name,
                ssid = group.id,
                appIcons = group.apps.map { it.icon },
                enabled = group.enabled,
                onEnabledChange = { onEnabledChange(group, it) },
                onClick = { onGroupClick(group) },
                onLongClick = { onGroupLongClick(group) },
            )
        }
    }
}

private val previewGroups = listOf(
    BlockedAppGroup(id = "MyHome_5G", name = "Home", apps = emptyList(), schedule = defaultTimeSlot()),
    BlockedAppGroup(id = "Library-Guest", name = "Library", apps = emptyList(), schedule = defaultTimeSlot(), enabled = false),
)

@Composable
private fun PreviewContent(
    groups: List<BlockedAppGroup> = previewGroups,
    viewingGroup: BlockedAppGroup? = null,
    editStep: WifiEditStep? = null,
    draft: WifiDraft = WifiDraft(),
    checkResult: WifiCheckResult? = null,
) {
    FocusAppTheme {
        WifiFocusContent(
            groups = groups,
            onEnabledChange = { _, _ -> },
            onGroupClick = {},
            onGroupLongClick = {},
            onAddClick = {},
            onTabClick = {},
            viewingGroup = viewingGroup,
            onEditViewingGroup = {},
            editStep = editStep,
            draft = draft,
            currentSsid = (checkResult as? WifiCheckResult.Connected)?.ssid,
            checkResult = checkResult,
            knownSsids = listOf("MyHome_5G", "Library-Guest", "Cafe Free WiFi"),
            addedSsids = groups.map { it.id }.toSet(),
            manualSsid = "",
            onSelectSsid = {},
            onManualSsidChange = {},
            pickerApps = List(12) { InstalledAppInfo("com.example.app$it", "App $it", icon = null) },
            onDraftChange = {},
            onStepChange = {},
            onCheckAgain = {},
            onResultAction = {},
            onEditorClose = {},
            onEditorConfirm = {},
            onOutsideCardClick = {},
        )
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun WifiFocusContentPreview() = PreviewContent()

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun WifiFocusContentEmptyPreview() = PreviewContent(groups = emptyList())

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun WifiFocusContentDetailPreview() = PreviewContent(viewingGroup = previewGroups.first())

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun WifiFocusContentNetworkPreview() = PreviewContent(
    groups = previewGroups.drop(1),
    editStep = WifiEditStep.NETWORK,
    draft = WifiDraft(ssid = "MyHome_5G"),
    checkResult = WifiCheckResult.Connected("MyHome_5G"),
)

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun WifiFocusContentAppsPreview() =
    PreviewContent(editStep = WifiEditStep.APPS, draft = WifiDraft(ssid = "MyHome_5G", selectedPackages = setOf("com.example.app0")))

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun WifiFocusContentNamePreview() =
    PreviewContent(editStep = WifiEditStep.NAME, draft = WifiDraft(ssid = "MyHome_5G", name = "Home"))