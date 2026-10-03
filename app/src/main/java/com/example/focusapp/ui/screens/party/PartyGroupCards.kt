package com.example.focusapp.ui.screens.party

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import com.example.focusapp.domain.model.Friend
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import com.example.focusapp.ui.components.button.ConfirmButton
import com.example.focusapp.ui.components.button.RejectButton
import com.example.focusapp.ui.components.card.CardButtonRow
import com.example.focusapp.ui.components.card.CardLabel
import com.example.focusapp.ui.components.card.CardTitle
import com.example.focusapp.ui.components.card.FocusCard
import com.example.focusapp.ui.components.input.FocusTextField
import com.example.focusapp.ui.components.card.sunkenPanel

/**
 * A random, easy-to-read group code (no O/0/I/1). It is also the group's id
 * in the cloud - see PartyModeViewModel's doc comment.
 */
fun newPartyCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars.random() }.joinToString("")
}

/**
 * Fly card for "Create group": the group [code] to share, who has joined
 * so far ([members]), and a check that starts the group's focus session.
 * [errorMessage] shows when the cloud can't be reached.
 *
 * [Claude, 2026-10-03] [friends]/[onInviteFriend] add a second way in besides sharing [code]
 * out loud: an Invite button per saved friend, right while hosting - see
 * PartyModeViewModel.inviteFriend()'s own doc comment for why this only works here (it needs
 * an active party id, which only exists once this card is already open). Omitted entirely
 * (section just doesn't render) when there are no saved friends yet, rather than showing an
 * empty list - FriendsScreen's own Add Friend card is the place to fix that, not this one.
 */
@Composable
fun CreateGroupCard(
    code: String,
    members: List<String>,
    errorMessage: String?,
    onClose: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    friends: List<Friend> = emptyList(),
    onInviteFriend: (String) -> Unit = {},
) {
    FocusCard(modifier = modifier) {
        CardButtonRow(
            left = { RejectButton(onClick = onClose, contentDescription = "Close") },
            right = { ConfirmButton(onClick = onStart, contentDescription = "Start focusing together") },
        )
        CardTitle("Create group", bottomPadding = 0.dp)
        PartyLabel("Group code")
        CodePanel(code)
        PartyNote("Share this code so friends can join.")
        PartyLabel("Members")
        MemberPanel(members)
        if (friends.isNotEmpty()) {
            PartyLabel("Invite a friend")
            InviteFriendsPanel(friends, onInviteFriend)
        }
        errorMessage?.let { PartyError(it) }
    }
}

/** One row per saved friend, each with its own small Invite button - see [CreateGroupCard]'s
 *  doc comment for why this only appears there. */
@Composable
private fun InviteFriendsPanel(friends: List<Friend>, onInviteFriend: (String) -> Unit) {
    Column(
        modifier = Modifier
            .sunkenPanel(FocusTheme.colors.surfaceSunken)
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        friends.forEach { friend ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = friend.nickname,
                    style = FocusTheme.typography.listLabel,
                    color = FocusTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                InviteButton(onClick = { onInviteFriend(friend.uid) })
            }
        }
    }
}

/** A small text pill, matching this card's other buttons' tap-target style but without a
 *  dedicated Figma glyph of its own (unlike [ConfirmButton]/[RejectButton]) - "Invite" is a
 *  word every friend row needs room for, not an icon. */
@Composable
private fun InviteButton(onClick: () -> Unit) {
    Text(
        text = "Invite",
        style = FocusTheme.typography.caption,
        color = FocusTheme.colors.accent,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/**
 * Fly card for "Join group": type a group code, then the check joins it.
 * Once [joined], it shows the group's [members] and waits for the host.
 */
@Composable
fun JoinGroupCard(
    codeInput: String,
    onCodeInputChange: (String) -> Unit,
    joined: Boolean,
    members: List<String>,
    errorMessage: String?,
    onClose: () -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusCard(modifier = modifier) {
        CardButtonRow(
            left = { RejectButton(onClick = onClose, contentDescription = "Close") },
            right = {
                ConfirmButton(
                    onClick = onJoin,
                    contentDescription = "Join group",
                    enabled = !joined && codeInput.isNotBlank(),
                )
            },
        )
        CardTitle("Join group", bottomPadding = 0.dp)
        if (joined) {
            PartyLabel("Group code")
            CodePanel(codeInput.trim())
            PartyLabel("Members")
            MemberPanel(members)
            PartyNote("Waiting for the host to start…")
        } else {
            FocusTextField(
                value = codeInput,
                onValueChange = { onCodeInputChange(it.uppercase()) },
                placeholder = "Enter Group Code",
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        errorMessage?.let { PartyError(it) }
    }
}

/**
 * [Claude, 2026-10-03] Fly card for "Add friend": paste a friend's short code (they get it from
 * their own copy of [FriendsScreen]'s "My Code" row) and give them a nickname, then Save.
 * There's no cloud directory to search - see PartyModeViewModel's class doc comment for why
 * typing in a code you were told out-of-band is the only way in right now.
 */
@Composable
fun AddFriendCard(
    codeInput: String,
    onCodeInputChange: (String) -> Unit,
    nicknameInput: String,
    onNicknameInputChange: (String) -> Unit,
    errorMessage: String?,
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusCard(modifier = modifier) {
        CardButtonRow(
            left = { RejectButton(onClick = onClose, contentDescription = "Close") },
            right = {
                ConfirmButton(
                    onClick = onSave,
                    contentDescription = "Save friend",
                    enabled = codeInput.isNotBlank() && nicknameInput.isNotBlank(),
                )
            },
        )
        CardTitle("Add friend", bottomPadding = 0.dp)
        PartyLabel("Friend's code")
        FocusTextField(
            value = codeInput,
            onValueChange = { onCodeInputChange(it.uppercase()) },
            placeholder = "Paste their code",
        )
        PartyLabel("Nickname")
        FocusTextField(
            value = nicknameInput,
            onValueChange = onNicknameInputChange,
            placeholder = "What do you call them?",
        )
        errorMessage?.let { PartyError(it) }
    }
}

@Composable
private fun CodePanel(code: String) {
    Text(
        text = code,
        style = FocusTheme.typography.statValue,
        color = FocusTheme.colors.accent,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .sunkenPanel(FocusTheme.colors.surfaceSunken)
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun MemberPanel(members: List<String>) {
    Column(
        modifier = Modifier
            .sunkenPanel(FocusTheme.colors.surfaceSunken)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        members.forEach { name ->
            Text(
                text = name,
                style = FocusTheme.typography.listLabel,
                color = FocusTheme.colors.onSurface,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
    }
}

/** A label with Party's spacing (more room above than other cards). */
@Composable
private fun PartyLabel(text: String) = CardLabel(text, topPadding = 16.dp, bottomPadding = 8.dp)

/** A small centered grey note. */
@Composable
private fun PartyNote(text: String) {
    Text(
        text = text,
        style = FocusTheme.typography.caption,
        color = FocusTheme.colors.onSurfaceMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

/** A small centered error message. */
@Composable
private fun PartyError(text: String) {
    Text(
        text = text,
        style = FocusTheme.typography.caption,
        color = FocusTheme.colors.rejection,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    )
}

@Preview(widthDp = 360)
@Composable
private fun CreateGroupCardPreview() {
    FocusAppTheme {
        CreateGroupCard(
            code = "K7Q2MX",
            members = listOf("You (Host)", "David"),
            errorMessage = null,
            onClose = {},
            onStart = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun JoinGroupCardPreview() {
    FocusAppTheme {
        JoinGroupCard(
            codeInput = "",
            onCodeInputChange = {},
            joined = false,
            members = emptyList(),
            errorMessage = null,
            onClose = {},
            onJoin = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun JoinGroupCardJoinedPreview() {
    FocusAppTheme {
        JoinGroupCard(
            codeInput = "K7Q2MX",
            onCodeInputChange = {},
            joined = true,
            members = listOf("Alison", "You"),
            errorMessage = "Party Mode needs an internet connection.",
            onClose = {},
            onJoin = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
