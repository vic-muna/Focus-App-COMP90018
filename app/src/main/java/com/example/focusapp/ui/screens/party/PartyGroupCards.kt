package com.example.focusapp.ui.screens.party

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.ConfirmButton
import com.example.focusapp.ui.components.FocusTextField
import com.example.focusapp.ui.components.RejectButton
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

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
    PartyCard(
        title = "Create group",
        onClose = onClose,
        confirmDescription = "Start focusing together",
        onConfirm = onStart,
        modifier = modifier,
    ) {
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
    PartyCard(
        title = "Join group",
        onClose = onClose,
        confirmDescription = "Join group",
        onConfirm = onJoin,
        confirmEnabled = !joined && codeInput.isNotBlank(),
        modifier = modifier,
    ) {
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

/** The shared fly-card frame: X on the left, check on the right, then [title] and [content]. */
@Composable
private fun PartyCard(
    title: String,
    onClose: () -> Unit,
    confirmDescription: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = FocusTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RejectButton(onClick = onClose, contentDescription = "Close")
            ConfirmButton(onClick = onConfirm, contentDescription = confirmDescription, enabled = confirmEnabled)
        }
        Text(
            text = title,
            style = FocusTheme.typography.tileTitle,
            color = colors.onSurface,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp),
        )
        content()
    }
}

private val PanelModifier
    @Composable get() = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(FocusTheme.colors.surfaceSunken)

@Composable
private fun CodePanel(code: String) {
    Text(
        text = code,
        style = FocusTheme.typography.statValue,
        color = FocusTheme.colors.accent,
        textAlign = TextAlign.Center,
        modifier = PanelModifier.padding(vertical = 12.dp),
    )
}

@Composable
private fun MemberPanel(members: List<String>) {
    Column(modifier = PanelModifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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

@Composable
private fun PartyLabel(text: String) {
    Text(
        text = text,
        style = FocusTheme.typography.caption,
        color = FocusTheme.colors.onSurfaceMuted,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    )
}

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
