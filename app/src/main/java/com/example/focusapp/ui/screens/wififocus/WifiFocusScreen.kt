package com.example.focusapp.ui.screens.wififocus

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.focusapp.R
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.apps.InstalledAppInfo
import com.example.focusapp.data.blocking.BlockedAppGroup
import com.example.focusapp.data.blocking.defaultTimeSlot
import com.example.focusapp.data.blocking.watchedSsids
import com.example.focusapp.data.wifi.SavedWifiStorage
import com.example.focusapp.data.wifi.WifiBlockingProblem
import com.example.focusapp.data.wifi.WifiCheckResult
import com.example.focusapp.data.wifi.checkCurrentWifi
import com.example.focusapp.data.wifi.findWifiBlockingProblem
import com.example.focusapp.data.wifi.hasLocationPermissionForWifi
import com.example.focusapp.ui.common.AccessibilityPermissionDialog
import com.example.focusapp.ui.common.pickedApps
import com.example.focusapp.ui.common.rememberInstalledApps
import com.example.focusapp.ui.common.rememberLocationPermissionState
import com.example.focusapp.ui.common.toggle
import com.example.focusapp.ui.components.button.BackButton
import com.example.focusapp.ui.components.button.ConfirmButton
import com.example.focusapp.ui.components.card.AppSelectCard
import com.example.focusapp.ui.components.card.DeleteDialog
import com.example.focusapp.ui.components.card.DiscardDialog
import com.example.focusapp.ui.components.card.sunkenPanel
import com.example.focusapp.ui.components.layout.GroupListLayout
import com.example.focusapp.ui.navigation.MainTab
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val HeaderHeight = 210.dp

/** Steps of the add/edit fly card. APPS is opened from the network card's last row and returns to it. */
private enum class WifiEditStep { NETWORK, APPS, NAME }

/** The add/edit card's fields while the user fills them in. */
private data class WifiDraft(
    val ssids: Set<String> = emptySet(),
    val selectedPackages: Set<String> = emptySet(),
    val name: String = "",
)

private fun BlockedAppGroup.toDraft() = WifiDraft(
    ssids = watchedSsids.toSet(),
    selectedPackages = apps.map { it.packageName }.toSet(),
    name = name,
)

/**
 * The Wi-Fi tab. A Wi-Fi header over the list of Wi-Fi entries (each
 * with an on/off switch) and an "add" card - the same flow as Time Focus.
 *
 * Each entry is a [BlockedAppGroup] with the networks it watches in
 * [BlockedAppGroup.wifiSsids]: while the phone is on one of them, that
 * entry's apps are blocked straight away (by FocusAccessibilityService, no
 * focus session needed). A warning shows when a permission that needs is missing.
 *  - tap an entry: its read-only summary; the pencil opens the edit steps
 *  - "+": one card with the connected network (tap to save it), the saved
 *    list (tap to remove), the networks that start blocking (tick) and the
 *    blocked apps (tap to pick); then the name
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
    // The entry being edited (null while adding a new one).
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf(WifiDraft()) }
    var checkResult by remember { mutableStateOf<WifiCheckResult?>(null) }
    val currentSsid = (checkResult as? WifiCheckResult.Connected)?.ssid
    // The user's saved Wi-Fi list (see SavedWifiStorage), loaded when the card opens.
    val savedWifi = remember { SavedWifiStorage(context) }
    var savedSsids by remember { mutableStateOf<List<String>>(emptyList()) }
    val installedApps = rememberInstalledApps(shouldLoad = editStep != null)
    var confirmDiscard by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BlockedAppGroup?>(null) }

    // Reading the Wi-Fi name needs PRECISE location (see hasLocationPermissionForWifi).
    val permissionState = rememberLocationPermissionState()
    var pendingCheck by remember { mutableStateOf(false) }

    // What still stops switched-on entries from blocking apps - re-checked on coming back from settings.
    val appBlockingOn by AccessibilityBridge.isServiceConnected.collectAsState()
    var resumeCount by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeCount++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val blockingProblem = remember(appBlockingOn, resumeCount) { findWifiBlockingProblem(context, appBlockingOn) }
        .takeIf { groups.any { it.enabled } }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    fun fixBlockingProblem(problem: WifiBlockingProblem) {
        when (problem) {
            WifiBlockingProblem.APP_BLOCKING_OFF -> showAccessibilityDialog = true
            WifiBlockingProblem.NEEDS_PRECISE_LOCATION, WifiBlockingProblem.NEEDS_ALL_THE_TIME_LOCATION ->
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            WifiBlockingProblem.LOCATION_SERVICES_OFF ->
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    fun checkWifi() {
        if (hasLocationPermissionForWifi(context)) {
            checkResult = null
            scope.launch { checkResult = checkCurrentWifi(context) }
        } else {
            pendingCheck = true
            permissionState.request()
        }
    }

    // Checks once the user answers the permission dialog - the result then says what's still missing.
    LaunchedEffect(permissionState.resultCount) {
        if (pendingCheck && permissionState.resultCount > 0) {
            pendingCheck = false
            checkResult = checkCurrentWifi(context)
        }
    }

    LaunchedEffect(editStep != null) {
        if (editStep != null) {
            savedSsids = withContext(Dispatchers.IO) { savedWifi.getSavedSsids() }
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

    fun saveCurrentSsid(ssid: String) {
        savedWifi.add(ssid)
        if (ssid !in savedSsids) savedSsids = savedSsids + ssid
    }

    // Removing a saved network also unticks it.
    fun removeSavedSsid(ssid: String) {
        savedWifi.remove(ssid)
        savedSsids = savedSsids - ssid
        draft = draft.copy(ssids = draft.ssids - ssid)
    }

    val editingGroup = groups.find { it.id == editingGroupId }
    val draftApps = pickedApps(installedApps, draft.selectedPackages, editingGroup?.apps.orEmpty())

    fun saveDraft() {
        if (draft.ssids.isEmpty()) return
        // The schedule and limit fields aren't used for Wi-Fi entries.
        val newGroup = BlockedAppGroup(
            id = "wifi_${System.currentTimeMillis()}",
            name = "",
            apps = emptyList(),
            schedule = defaultTimeSlot(),
        )
        val saved = (editingGroup ?: newGroup).copy(
            name = draft.name.trim(),
            apps = draftApps,
            wifiSsids = draft.ssids.toList(),
        )
        onGroupsChange(
            if (editingGroup == null) groups + saved
            else groups.map { if (it.id == editingGroup.id) saved else it }
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
            WifiEditStep.NAME, WifiEditStep.APPS -> editStep = WifiEditStep.NETWORK
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
        blockingProblem = blockingProblem,
        onBlockingProblemClick = ::fixBlockingProblem,
        viewingGroup = viewingGroup,
        onEditViewingGroup = { viewingGroup?.let(::startEditing) },
        editStep = editStep,
        draft = draft,
        draftAppIcons = draftApps.map { it.icon },
        currentSsid = currentSsid,
        checkResult = checkResult,
        savedSsids = savedSsids,
        onSaveCurrent = ::saveCurrentSsid,
        onRemoveSaved = ::removeSavedSsid,
        onToggleSsid = { ssid -> draft = draft.copy(ssids = draft.ssids.toggle(ssid)) },
        pickerApps = installedApps,
        onDraftChange = { draft = it },
        onStepChange = { step ->
            // Suggest the first network's name as the entry's name.
            if (step == WifiEditStep.NAME && draft.name.isBlank()) draft = draft.copy(name = draft.ssids.firstOrNull().orEmpty())
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

    if (showAccessibilityDialog) {
        AccessibilityPermissionDialog(onDismiss = { showAccessibilityDialog = false })
    }

    if (confirmDiscard) {
        val isEditing = editingGroupId != null
        DiscardDialog(
            title = if (isEditing) "Cancel editing?" else "Cancel new Wi-Fi?",
            message = if (isEditing) "Your changes to this Wi-Fi will be discarded."
            else "The networks and apps you picked will be discarded.",
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
    blockingProblem: WifiBlockingProblem?,
    onBlockingProblemClick: (WifiBlockingProblem) -> Unit,
    viewingGroup: BlockedAppGroup?,
    onEditViewingGroup: () -> Unit,
    editStep: WifiEditStep?,
    draft: WifiDraft,
    draftAppIcons: List<Bitmap?>,
    currentSsid: String?,
    checkResult: WifiCheckResult?,
    savedSsids: List<String>,
    onSaveCurrent: (String) -> Unit,
    onRemoveSaved: (String) -> Unit,
    onToggleSsid: (String) -> Unit,
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
                    currentSsid = currentSsid,
                    checkResult = checkResult,
                    savedSsids = savedSsids,
                    selectedSsids = draft.ssids,
                    appIcons = draftAppIcons,
                    onSaveCurrent = onSaveCurrent,
                    onRemoveSaved = onRemoveSaved,
                    onToggleSsid = onToggleSsid,
                    onCheckAgain = onCheckAgain,
                    onResultAction = onResultAction,
                    onAppsClick = { onStepChange(WifiEditStep.APPS) },
                    onClose = onEditorClose,
                    onNext = { onStepChange(WifiEditStep.NAME) },
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
                        ConfirmButton(
                            onClick = { onStepChange(WifiEditStep.NETWORK) },
                            contentDescription = "Done picking apps",
                        )
                    },
                    modifier = cardModifier,
                )
                WifiEditStep.NAME -> WifiNameStepCard(
                    name = draft.name,
                    onNameChange = { onDraftChange(draft.copy(name = it)) },
                    onBack = { onStepChange(WifiEditStep.NETWORK) },
                    onConfirm = onEditorConfirm,
                    modifier = cardModifier,
                )
                null -> viewingGroup?.let { group ->
                    WifiDetailCard(group = group, onEdit = onEditViewingGroup, modifier = cardModifier)
                }
            }
        },
    ) {
        blockingProblem?.let { problem ->
            Text(
                text = problem.message(),
                style = FocusTheme.typography.caption,
                color = FocusTheme.colors.rejection,
                modifier = Modifier
                    .sunkenPanel(FocusTheme.colors.surface)
                    .clickable(role = Role.Button) { onBlockingProblemClick(problem) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        groups.forEach { group ->
            WifiNetworkCard(
                name = group.name,
                ssid = group.watchedSsids.joinToString(", "),
                appIcons = group.apps.map { it.icon },
                enabled = group.enabled,
                onEnabledChange = { onEnabledChange(group, it) },
                onClick = { onGroupClick(group) },
                onLongClick = { onGroupLongClick(group) },
            )
        }
    }
}

/** The Wi-Fi tab's warning for [this] - each one says what tapping it does. */
private fun WifiBlockingProblem.message(): String = when (this) {
    WifiBlockingProblem.APP_BLOCKING_OFF ->
        "Turn on App Blocking so these Wi-Fi can block apps. Tap to turn it on."
    WifiBlockingProblem.NEEDS_PRECISE_LOCATION ->
        "Focus needs precise location to read the Wi-Fi name. Tap to open settings."
    WifiBlockingProblem.NEEDS_ALL_THE_TIME_LOCATION ->
        "Set Location to \"Allow all the time\" so Wi-Fi can block apps while Focus is closed. Tap to open settings."
    WifiBlockingProblem.LOCATION_SERVICES_OFF ->
        "Turn on the phone's Location - Android hides the Wi-Fi name without it. Tap to turn it on."
}

private val previewGroups = listOf(
    BlockedAppGroup(id = "wifi_1", name = "Home", apps = emptyList(), schedule = defaultTimeSlot(), wifiSsids = listOf("MyHome_5G", "MyHome_2G")),
    BlockedAppGroup(id = "wifi_2", name = "Library", apps = emptyList(), schedule = defaultTimeSlot(), enabled = false, wifiSsids = listOf("Library-Guest")),
)

@Composable
private fun PreviewContent(
    groups: List<BlockedAppGroup> = previewGroups,
    viewingGroup: BlockedAppGroup? = null,
    editStep: WifiEditStep? = null,
    draft: WifiDraft = WifiDraft(),
    checkResult: WifiCheckResult? = null,
    blockingProblem: WifiBlockingProblem? = null,
) {
    FocusAppTheme {
        WifiFocusContent(
            groups = groups,
            onEnabledChange = { _, _ -> },
            onGroupClick = {},
            onGroupLongClick = {},
            onAddClick = {},
            onTabClick = {},
            blockingProblem = blockingProblem,
            onBlockingProblemClick = {},
            viewingGroup = viewingGroup,
            onEditViewingGroup = {},
            editStep = editStep,
            draft = draft,
            draftAppIcons = List(draft.selectedPackages.size) { null },
            currentSsid = (checkResult as? WifiCheckResult.Connected)?.ssid,
            checkResult = checkResult,
            savedSsids = listOf("MyHome_5G", "Library-Guest", "Cafe Free WiFi"),
            onSaveCurrent = {},
            onRemoveSaved = {},
            onToggleSsid = {},
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
private fun WifiFocusContentProblemPreview() =
    PreviewContent(blockingProblem = WifiBlockingProblem.NEEDS_ALL_THE_TIME_LOCATION)

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun WifiFocusContentEmptyPreview() = PreviewContent(groups = emptyList())

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun WifiFocusContentDetailPreview() = PreviewContent(viewingGroup = previewGroups.first())

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun WifiFocusContentNetworkPreview() = PreviewContent(
    editStep = WifiEditStep.NETWORK,
    draft = WifiDraft(ssids = setOf("MyHome_5G"), selectedPackages = setOf("com.example.app0")),
    checkResult = WifiCheckResult.Connected("Office"),
)

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun WifiFocusContentAppsPreview() =
    PreviewContent(editStep = WifiEditStep.APPS, draft = WifiDraft(ssids = setOf("MyHome_5G"), selectedPackages = setOf("com.example.app0")))

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun WifiFocusContentNamePreview() =
    PreviewContent(editStep = WifiEditStep.NAME, draft = WifiDraft(ssids = setOf("MyHome_5G"), name = "Home"))
