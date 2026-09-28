package com.example.focusapp.ui.screens.party

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusapp.domain.model.Friend
import com.example.focusapp.ui.common.rememberLocationPermissionState
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusSpacing
import com.example.focusapp.ui.theme.FocusTheme
import com.example.focusapp.ui.components.bar.CloseTopBar
import com.example.focusapp.ui.components.bar.SettingsRow
import com.example.focusapp.ui.components.bar.SettingsSection
import com.example.focusapp.ui.components.button.FocusPillButton
import com.example.focusapp.ui.components.card.FlyCardOverlay
import com.example.focusapp.ui.components.input.FocusSearchField
import com.example.focusapp.ui.components.card.consumeTaps
import com.example.focusapp.data.blocking.AppItem
import com.example.focusapp.ui.screens.home.QuickFocusAppsCard
import androidx.compose.runtime.remember
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.ui.common.AccessibilityPermissionDialog

/** Which fly card is open on top of the friend list. */
private enum class GroupCard { NONE, CREATE, JOIN, QUICK_FOCUS_APPS }

/**
 * Party Mode's page. Reached from the group icon on Home (the other side
 * of the gear); the same spot shows an X that closes it ([onClose]).
 *  - Search: filters the friend list by name or ID while typing.
 *  - Friends: [friends] - empty for now (see the TODO on the parameter).
 *  - Create group / Join group: fly cards ([CreateGroupCard] /
 *    [JoinGroupCard]) backed by David's cloud party logic in
 *    [PartyModeViewModel] - the group code is the party's cloud id.
 *    Closing a card leaves the group; the host's check calls [onStartFocus].
 */
@Composable
fun FriendsScreen(
    onClose: () -> Unit,
    onStartFocus: () -> Unit,
    // TODO(ID system): load the user's friends from the cloud once IDs exist.
    friends: List<Friend> = emptyList(),
    // The apps a party's focus session blocks (shared with Quick Focus), and how to save a new pick.
    quickFocusApps: List<AppItem> = emptyList(),
    onQuickFocusAppsChange: (List<AppItem>) -> Unit = {},
    viewModel: PartyModeViewModel = viewModel(),
) {
    val permissionState = rememberLocationPermissionState()
    val members by viewModel.members.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    // Blocking only works after the user turns on the accessibility service in Android's settings.
    val isAccessibilityEnabled by AccessibilityBridge.isServiceConnected.collectAsState()
    var showAccessibilityPermissionDialog by remember { mutableStateOf(false) }

    var query by rememberSaveable { mutableStateOf("") }
    var card by rememberSaveable { mutableStateOf(GroupCard.NONE) }
    var createCode by rememberSaveable { mutableStateOf("") }
    var joinCode by rememberSaveable { mutableStateOf("") }
    var joined by rememberSaveable { mutableStateOf(false) }

    fun memberNames(fallback: String) =
        members.map { it.displayName.ifBlank { "Member (${it.uid.take(6)})" } }.ifEmpty { listOf(fallback) }

    fun closeCard() {
        viewModel.leaveParty()
        card = GroupCard.NONE
        joinCode = ""
        joined = false
    }

    // Back closes an open card first, then the page.
    BackHandler(enabled = card != GroupCard.NONE) {
        if (card == GroupCard.QUICK_FOCUS_APPS) card = GroupCard.CREATE else closeCard()
    }

    FriendsContent(
        query = query,
        onQueryChange = { query = it },
        onSearch = {
            // TODO(ID system): look [query] up as a friend ID in the cloud, so
            // someone who isn't a friend yet can be found and added.
        },
        friends = friends.filter { it.matches(query) },
        hasFriends = friends.isNotEmpty(),
        onClose = onClose,
        onCreateGroupClick = {
            createCode = newPartyCode()
            if (!permissionState.hasPermission) permissionState.request()
            viewModel.joinParty(createCode, "You (Host)")
            card = GroupCard.CREATE
        },
        onJoinGroupClick = { card = GroupCard.JOIN },
        showGroupCard = card != GroupCard.NONE,
        onGroupCardOutsideClick = {
            if (card == GroupCard.QUICK_FOCUS_APPS) card = GroupCard.CREATE else closeCard()
        },
    ) {
        when (card) {
            GroupCard.CREATE -> CreateGroupCard(
                code = createCode,
                members = memberNames("You (Host)"),
                errorMessage = errorMessage,
                onClose = ::closeCard,
                onStart = {
                    if (quickFocusApps.isEmpty()) {
                        // Nothing to block yet: pick the apps first, then press start again.
                        card = GroupCard.QUICK_FOCUS_APPS
                    } else if (!isAccessibilityEnabled) {
                        showAccessibilityPermissionDialog = true
                    } else {
                        viewModel.setFocusing(true)
                        onStartFocus()
                    }
                },
                modifier = Modifier.consumeTaps(),
            )
            GroupCard.JOIN -> JoinGroupCard(
                codeInput = joinCode,
                onCodeInputChange = { joinCode = it },
                joined = joined,
                members = memberNames("You"),
                errorMessage = errorMessage,
                onClose = ::closeCard,
                onJoin = {
                    if (!permissionState.hasPermission) permissionState.request()
                    viewModel.joinParty(joinCode.trim(), "You")
                    joined = true
                },
                modifier = Modifier.consumeTaps(),
            )
            GroupCard.QUICK_FOCUS_APPS -> QuickFocusAppsCard(
                savedApps = quickFocusApps,
                onSave = { apps ->
                    onQuickFocusAppsChange(apps)
                    card = GroupCard.CREATE
                },
                onClose = { card = GroupCard.CREATE },
                modifier = Modifier.consumeTaps(),
            )
            GroupCard.NONE -> Unit
        }
    }

    if (showAccessibilityPermissionDialog) {
        AccessibilityPermissionDialog(onDismiss = { showAccessibilityPermissionDialog = false })
    }
}

/** Whether [query] appears in this friend's name or ID (a blank query matches everyone). */
private fun Friend.matches(query: String): Boolean {
    val text = query.trim()
    return text.isEmpty() || nickname.contains(text, ignoreCase = true) || uid.contains(text, ignoreCase = true)
}

/**
 * Stateless layout of [FriendsScreen]. [friends] is already filtered by
 * [query]. While [showGroupCard], [groupCard] flies in over the page.
 */
@Composable
private fun FriendsContent(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    friends: List<Friend>,
    hasFriends: Boolean,
    onClose: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onJoinGroupClick: () -> Unit,
    showGroupCard: Boolean = false,
    onGroupCardOutsideClick: () -> Unit = {},
    groupCard: @Composable () -> Unit = {},
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, end = 32.dp, top = FocusSpacing.ScreenTop, bottom = FocusSpacing.ScreenBottom),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                // Same height as the X in the top-left corner, so the title lines up with it.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Party Mode", style = typography.primaryActionLabel, color = colors.onSurface)
                }

                FocusSearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = "Search friend ID",
                    onSearch = onSearch,
                )

                SettingsSection(title = "Friends", icon = Icons.Filled.Group) {
                    friends.forEach { friend ->
                        SettingsRow(label = friend.nickname) {
                            Text(
                                text = "ID ${friend.uid}",
                                style = typography.caption,
                                color = colors.onSurfaceMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (friends.isEmpty()) {
                        Text(
                            text = if (hasFriends) "No friend matches \"${query.trim()}\"." else "No friends yet.",
                            style = typography.body,
                            color = colors.onSurfaceMuted,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            ) {
                FocusPillButton(
                    label = "Create group",
                    containerColor = colors.primaryAction,
                    contentColor = colors.onPrimaryAction,
                    onClick = onCreateGroupClick,
                    modifier = Modifier.weight(1f),
                    height = 48.dp,
                )
                FocusPillButton(
                    label = "Join group",
                    containerColor = colors.primaryAction,
                    contentColor = colors.onPrimaryAction,
                    onClick = onJoinGroupClick,
                    modifier = Modifier.weight(1f),
                    height = 48.dp,
                )
            }
        }

        CloseTopBar(contentDescription = "Close Party Mode", onCloseClick = onClose)

        if (showGroupCard) {
            FlyCardOverlay(onOutsideClick = onGroupCardOutsideClick) { groupCard() }
        }
    }
}

private val PreviewFriends = listOf(
    Friend(uid = "alison0927", nickname = "Alison"),
    Friend(uid = "david0926", nickname = "David"),
    Friend(uid = "kevin1004", nickname = "Kevin"),
)

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun FriendsContentPreview() {
    FocusAppTheme {
        FriendsContent(
            query = "",
            onQueryChange = {},
            onSearch = {},
            friends = PreviewFriends,
            hasFriends = true,
            onClose = {},
            onCreateGroupClick = {},
            onJoinGroupClick = {},
        )
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun FriendsContentEmptyPreview() {
    FocusAppTheme {
        FriendsContent(
            query = "",
            onQueryChange = {},
            onSearch = {},
            friends = emptyList(),
            hasFriends = false,
            onClose = {},
            onCreateGroupClick = {},
            onJoinGroupClick = {},
        )
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun FriendsContentNoMatchPreview() {
    FocusAppTheme {
        FriendsContent(
            query = "zoe",
            onQueryChange = {},
            onSearch = {},
            friends = emptyList(),
            hasFriends = true,
            onClose = {},
            onCreateGroupClick = {},
            onJoinGroupClick = {},
        )
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun FriendsContentCreateCardPreview() {
    FocusAppTheme {
        FriendsContent(
            query = "",
            onQueryChange = {},
            onSearch = {},
            friends = PreviewFriends,
            hasFriends = true,
            onClose = {},
            onCreateGroupClick = {},
            onJoinGroupClick = {},
            showGroupCard = true,
        ) {
            CreateGroupCard(
                code = "K7Q2MX",
                members = listOf("You (Host)", "David"),
                errorMessage = null,
                onClose = {},
                onStart = {},
            )
        }
    }
}
