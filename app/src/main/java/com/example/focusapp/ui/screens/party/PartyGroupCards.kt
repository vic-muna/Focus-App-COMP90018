package com.example.focusapp.ui.screens.party

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
 */
@Composable
fun CreateGroupCard(
    code: String,
    members: List<String>,
    errorMessage: String?,
    onClose: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
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
        errorMessage?.let { PartyError(it) }
    }
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
