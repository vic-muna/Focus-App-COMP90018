package com.example.focusapp.ui.screens.party

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusapp.domain.model.Friend
import com.example.focusapp.domain.model.PartyInvite
import com.example.focusapp.ui.common.rememberLocationPermissionState
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusSpacing
import com.example.focusapp.ui.theme.FocusTheme
import com.example.focusapp.ui.components.bar.CloseTopBar
import com.example.focusapp.ui.components.bar.SettingsRow
import com.example.focusapp.ui.components.bar.SettingsSection
import com.example.focusapp.ui.components.button.ConfirmButton
import com.example.focusapp.ui.components.button.FocusPillButton
import com.example.focusapp.ui.components.button.RejectButton
import com.example.focusapp.ui.components.card.AddItemCard
import com.example.focusapp.ui.components.card.FlyCardOverlay
import com.example.focusapp.ui.components.input.FocusSearchField
import com.example.focusapp.ui.components.card.consumeTaps
import com.example.focusapp.data.blocking.AppItem
import com.example.focusapp.ui.components.card.AppPickerCard
import androidx.compose.runtime.remember
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.ui.common.AccessibilityPermissionDialog

/** Which fly card is open on top of the friend list. */
private enum class GroupCard { NONE, CREATE, JOIN, QUICK_FOCUS_APPS, ADD_FRIEND }

/**
 * Party Mode's page. Reached from the group icon on Home (the other side
 * of the gear); the same spot shows an X that closes it ([onClose]).
 *  - Search: filters the friend list by name or ID while typing.
 *  - Friends: [PartyModeViewModel.friends] - saved on this phone, loaded by the ViewModel
 *    itself (see its class doc comment; this is the "friend ID system" the old TODO here was
 *    waiting on). An Add Friend card (last tile in the list) saves a new one by pasting their
 *    [PartyModeViewModel.myFriendCode] - there's still no cloud directory to search by name, so
 *    [query] only filters friends already saved, same as before. Each row also has a delete
 *    icon, calling [PartyModeViewModel.deleteFriend].
 *  - My Code row: [PartyModeViewModel.myFriendCode], with a copy button - what you'd read out
 *    (or paste over text) to a friend so THEY can add YOU.
 *  - Incoming invites: shown above the friend list whenever
 *    [PartyModeViewModel.incomingInvites] isn't empty, each with Accept/Decline.
 *  - Create group / Join group: fly cards ([CreateGroupCard] /
 *    [JoinGroupCard]) backed by David's cloud party logic in
 *    [PartyModeViewModel] - the group code is the party's cloud id.
 *    Closing a card leaves the group; the host's check calls [onStartFocus].
 *    CreateGroupCard also lists friends with their own Invite button, calling
 *    [PartyModeViewModel.inviteFriend] - see that card's doc comment for why only there.
 */
@Composable
fun FriendsScreen(
    onClose: () -> Unit,
    onStartFocus: () -> Unit,
    // The apps a party's focus session blocks (shared with Quick Focus), and how to save a new pick.
    quickFocusApps: List<AppItem> = emptyList(),
    onQuickFocusAppsChange: (List<AppItem>) -> Unit = {},
    viewModel: PartyModeViewModel = viewModel(),
) {
    val permissionState = rememberLocationPermissionState()
    val members by viewModel.members.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val myFriendCode by viewModel.myFriendCode.collectAsState()
    val incomingInvites by viewModel.incomingInvites.collectAsState()
    // Blocking only works after the user turns on the accessibility service in Android's settings.
    val isAccessibilityEnabled by AccessibilityBridge.isServiceConnected.collectAsState()
    var showAccessibilityPermissionDialog by remember { mutableStateOf(false) }

    var query by rememberSaveable { mutableStateOf("") }
    var card by rememberSaveable { mutableStateOf(GroupCard.NONE) }
    var createCode by rememberSaveable { mutableStateOf("") }
    var joinCode by rememberSaveable { mutableStateOf("") }
    var joined by rememberSaveable { mutableStateOf(false) }
    var addFriendCode by rememberSaveable { mutableStateOf("") }
    var addFriendNickname by rememberSaveable { mutableStateOf("") }

    fun memberNames(fallback: String) =
        members.map { it.displayName.ifBlank { "Member (${it.uid.take(6)})" } }
            .ifEmpty { listOf(fallback) }

    fun closeCard() {
        viewModel.leaveParty()
        card = GroupCard.NONE
        joinCode = ""
        joined = false
        addFriendCode = ""
        addFriendNickname = ""
    }

    // Back closes an open card first, then the page.
    BackHandler(enabled = card != GroupCard.NONE) {
        if (card == GroupCard.QUICK_FOCUS_APPS) card = GroupCard.CREATE else closeCard()
    }

    val context = LocalContext.current

    // A joined participant follows the host into focus (the host's own Start never reaches
    // this branch: only someone else going from not focusing to focusing emits the event).
    val currentJoined by rememberUpdatedState(joined)
    val currentAccessibilityEnabled by rememberUpdatedState(isAccessibilityEnabled)
    val currentOnStartFocus by rememberUpdatedState(onStartFocus)
    LaunchedEffect(Unit) {
        viewModel.partyFocusStarted.collect {
            if (!currentJoined) return@collect
            if (currentAccessibilityEnabled) {
                viewModel.setFocusing(true)
                currentOnStartFocus()
            } else {
                showAccessibilityPermissionDialog = true
            }
        }
    }

    FriendsContent(
        isGuest = viewModel.isGuest,
        query = query,
        onQueryChange = { query = it },
        onSearch = {},
        friends = friends.filter { it.matches(query) },
        hasFriends = friends.isNotEmpty(),
        myFriendCode = myFriendCode,
        onCopyMyFriendCode = { code -> copyToClipboard(context, code) },
        incomingInvites = incomingInvites,
        inviterLabel = { invite ->
            friends.firstOrNull { it.uid == invite.fromUid }?.nickname ?: invite.fromUid
        },
        onAcceptInvite = { invite -> viewModel.respondToInvite(invite, accept = true) },
        onDeclineInvite = { invite -> viewModel.respondToInvite(invite, accept = false) },
        onDeleteFriend = { friend -> viewModel.deleteFriend(friend.uid) },
        onClose = onClose,
        onCreateGroupClick = {
            createCode = newPartyCode()
            if (!permissionState.hasPermission) permissionState.request()
            viewModel.joinParty(createCode, "You (Host)")
            card = GroupCard.CREATE
        },
        onJoinGroupClick = { card = GroupCard.JOIN },
        onAddFriendClick = { card = GroupCard.ADD_FRIEND },
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
                    // Permission first, then pick the apps to block (same card as Quick Focus).
                    if (isAccessibilityEnabled) card = GroupCard.QUICK_FOCUS_APPS
                    else showAccessibilityPermissionDialog = true
                },
                friends = friends,
                onInviteFriend = viewModel::inviteFriend,
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

            GroupCard.QUICK_FOCUS_APPS -> AppPickerCard(
                title = "Quick Focus Apps",
                savedApps = quickFocusApps,
                confirmDescription = "Start focusing",
                onConfirm = { apps ->
                    onQuickFocusAppsChange(apps)
                    card =
                        GroupCard.CREATE // Coming back after focusing shows the group card again.
                    viewModel.setFocusing(true)
                    onStartFocus()
                },
                onClose = { card = GroupCard.CREATE },
                modifier = Modifier.consumeTaps(),
            )

            GroupCard.ADD_FRIEND -> AddFriendCard(
                codeInput = addFriendCode,
                onCodeInputChange = {
                    addFriendCode = it
                    viewModel.clearError() // The message was about the code that's being changed.
                },
                nicknameInput = addFriendNickname,
                onNicknameInputChange = { addFriendNickname = it },
                errorMessage = errorMessage,
                onClose = ::closeCard,
                onSave = {
                    // Stays open until the friend is saved, so a wrong code's message can show.
                    viewModel.addFriend(addFriendCode, addFriendNickname) {
                        if (card == GroupCard.ADD_FRIEND) closeCard()
                    }
                },
                modifier = Modifier.consumeTaps(),
            )

            GroupCard.NONE -> Unit
        }
    }

    if (showAccessibilityPermissionDialog) {
        AccessibilityPermissionDialog(onDismiss = { showAccessibilityPermissionDialog = false })
    }
}

/** Puts [text] on the system clipboard - used for the "My Code" row's copy button so a
 *  friend's code can be pasted into [AddFriendCard] without retyping it by hand. */
private fun copyToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService<ClipboardManager>() ?: return
    clipboardManager.setPrimaryClip(ClipData.newPlainText("Focus App friend code", text))
}

/** Whether [query] appears in this friend's name or ID (a blank query matches everyone). */
private fun Friend.matches(query: String): Boolean {
    val text = query.trim()
    return text.isEmpty() || nickname.contains(text, ignoreCase = true) || uid.contains(
        text,
        ignoreCase = true
    )
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
    onAddFriendClick: () -> Unit = {},
    // A guest only gets the group buttons - see PartyModeViewModel.isGuest.
    isGuest: Boolean = false,
    myFriendCode: String? = null,
    onCopyMyFriendCode: (String) -> Unit = {},
    incomingInvites: List<PartyInvite> = emptyList(),
    inviterLabel: (PartyInvite) -> String = { it.fromUid },
    onAcceptInvite: (PartyInvite) -> Unit = {},
    onDeclineInvite: (PartyInvite) -> Unit = {},
    onDeleteFriend: (Friend) -> Unit = {},
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
                .padding(
                    start = 32.dp,
                    end = 32.dp,
                    top = FocusSpacing.ScreenTop,
                    bottom = FocusSpacing.ScreenBottom
                ),
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
                    Text(
                        text = "Party Mode",
                        style = typography.primaryActionLabel,
                        color = colors.onSurface
                    )
                }

                if (isGuest) {
                    SettingsSection(title = "Friends", icon = Icons.Filled.Group) {
                        Text(
                            text = "Friends need an account - create one in Settings. " +
                                    "As a guest you can still create or join a group with a group code.",
                            style = typography.body,
                            color = colors.onSurfaceMuted,
                            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                        )
                    }
                }

                if (!isGuest) FocusSearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = "Search friend ID",
                    onSearch = onSearch,
                )

                // What a friend needs from you before they can add you back - see
                // FriendsScreen's class doc comment for why there's no directory to search.
                if (!isGuest && myFriendCode != null) {
                    SettingsSection(title = "My Code", icon = Icons.Filled.Group) {
                        SettingsRow(label = myFriendCode) {
                            IconButton(onClick = { onCopyMyFriendCode(myFriendCode) }) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy my code",
                                    tint = colors.onSurface,
                                )
                            }
                        }
                    }
                }

                // Only rendered when there's something to act on - an empty section here would
                // just be noise above the friend list on every normal visit to this screen.
                if (!isGuest && incomingInvites.isNotEmpty()) {
                    SettingsSection(title = "Invites", icon = Icons.Filled.Mail) {
                        incomingInvites.forEach { invite ->
                            SettingsRow(label = "${inviterLabel(invite)} invited you") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RejectButton(
                                        onClick = { onDeclineInvite(invite) },
                                        contentDescription = "Decline invite",
                                        size = 28.dp,
                                    )
                                    ConfirmButton(
                                        onClick = { onAcceptInvite(invite) },
                                        contentDescription = "Accept invite",
                                        size = 28.dp,
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isGuest) SettingsSection(title = "Friends", icon = Icons.Filled.Group) {
                    friends.forEach { friend ->
                        SettingsRow(label = friend.nickname) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ID ${friend.uid}",
                                    style = typography.caption,
                                    color = colors.onSurfaceMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 96.dp),
                                )
                                IconButton(onClick = { onDeleteFriend(friend) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Remove ${friend.nickname}",
                                        tint = colors.onSurfaceMuted,
                                    )
                                }
                            }
                        }
                    }
                    if (friends.isEmpty()) {
                        Text(
                            text = if (hasFriends) "No friend matches \"${query.trim()}\"." else "No friends yet.",
                            style = typography.body,
                            color = colors.onSurfaceMuted,
                            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                        )
                    }
                    AddItemCard(
                        onClick = onAddFriendClick,
                        onClickLabel = "Add friend",
                        modifier = Modifier.padding(top = 8.dp),
                    )
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
private fun FriendsContentGuestPreview() {
    FocusAppTheme {
        FriendsContent(
            isGuest = true,
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
