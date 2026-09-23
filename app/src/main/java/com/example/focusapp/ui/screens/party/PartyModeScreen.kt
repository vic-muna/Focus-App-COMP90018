package com.example.focusapp.ui.screens.party

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.focusapp.ui.theme.WireframeColors
import kotlinx.coroutines.launch

private const val WIPE_DURATION_MILLIS = 800

private enum class PartySelection { NONE, INVITE, JOIN }

/** A plausible-looking placeholder P2P code - no real pairing behind it yet. */
private fun generatePartyCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no O/0/I/1, easy to read aloud
    return (1..6).map { chars.random() }.joinToString("")
}

/**
 * PartyModeScreen
 * -----------------
 * Bare placeholder for the upcoming "Party Mode" / study-party feature
 * (see the "Study Party" doc comments in data/remote/RemoteDataSource.kt
 * and data/repository/FocusRepositoryImpl.kt) - this screen is only the
 * UI entry point for that feature; no networking/P2P logic lives here.
 *
 * Tapping Invite or Join opens a centered pop-up dialog over the whole
 * screen (dim scrim, black card - matching the buttons' own color).
 * Invite shows a locally-generated placeholder code, a participant list
 * (just the host until real joining exists), a share-link row, and a "Go
 * Focus Mode" button. Join shows a code entry field; submitting any
 * non-blank code "succeeds" (simulated - no real validation) and swaps
 * the dialog to a participant list plus a "waiting for host" message.
 * None of this is backed by real P2P logic yet - onInviteClick/onJoinClick
 * /onInviteFriendClick/onGoFocusModeClick stay available as no-op hooks so
 * a future connection layer can wire in without touching this screen's
 * layout.
 *
 * The screen's own content never moves, and the wipe layer itself never
 * moves or resizes either - it's a fixed, full-screen gradient whose
 * transition band sweeps along the bottom-left -> top-right diagonal as a
 * fraction (revealProgress) animates 0 -> 1, uncovering the content
 * beneath as it passes. Going back reverses it - the band sweeps back the
 * other way, re-covering the screen before popping back. This is owned
 * entirely by this screen (not a NavGraph-level transition - see the
 * partyModeEnter/partyModeExit fade used for this route in NavGraph.kt),
 * so it also intercepts the system back gesture via BackHandler to play
 * the same reverse animation.
 */
// [HANDOFF -> Yu-Hao Lu | README task: "Firebase real-time sync for Study Party feature"]
// Wire onInviteClick / onJoinClick / onInviteFriendClick into real
// create-room / join-room calls (see data/remote/RemoteDataSource.kt),
// replacing generatePartyCode() and the local participant lists with
// live Firebase-backed data.
@Composable
fun PartyModeScreen(
    onBackClick: () -> Unit = {},
    onInviteClick: () -> Unit = {},
    onJoinClick: () -> Unit = {},
    onInviteFriendClick: () -> Unit = {},
    onGoFocusModeClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    // 0f = fully covered by the wipe layer, 1f = fully revealed.
    val revealProgress = remember { Animatable(0f) }
    var screenWidthPx by remember { mutableIntStateOf(0) }
    var screenHeightPx by remember { mutableIntStateOf(0) }

    // Which pop-up (if any) is currently open.
    var selection by remember { mutableStateOf(PartySelection.NONE) }

    val inviteCode = remember { generatePartyCode() }
    val inviteParticipants = remember { mutableStateListOf("You (Host)") }

    var joinCodeInput by remember { mutableStateOf("") }
    var joinSucceeded by remember { mutableStateOf(false) }
    val joinParticipants = remember { mutableStateListOf("Host", "You") }

    LaunchedEffect(Unit) {
        revealProgress.animateTo(1f, tween(WIPE_DURATION_MILLIS, easing = FastOutSlowInEasing))
    }

    fun goBack() {
        scope.launch {
            revealProgress.animateTo(0f, tween(WIPE_DURATION_MILLIS, easing = FastOutSlowInEasing))
            onBackClick()
        }
    }

    BackHandler(onBack = ::goBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WireframeColors.Background)
            .onSizeChanged {
                screenWidthPx = it.width
                screenHeightPx = it.height
            }
    ) {
        IconButton(
            onClick = ::goBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5C8C8))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black
            )
        }

        Text(
            text = "Join or Create A Group",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = WireframeColors.OnLight,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp, start = 24.dp, end = 24.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    selection = PartySelection.INVITE
                    onInviteClick()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Invite", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(25.dp))

            Button(
                onClick = {
                    selection = PartySelection.JOIN
                    onJoinClick()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Join", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Wipe layer - stays put, fillMaxSize, never translated or scaled.
        // Only the gradient's own transition band sweeps along the
        // bottom-left -> top-right diagonal as revealProgress goes 0 -> 1:
        // everything "behind" the band (bottom-left side) is transparent,
        // everything "ahead of" it (top-right side) is still fully opaque,
        // and the band itself is a soft feathered edge of fixed width. A
        // moving line, not a shrinking rectangle - translating/scaling the
        // whole box read as the solid color resizing, which looked off.
        val bandWidth = 0.18f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            (revealProgress.value - bandWidth).coerceIn(0f, 1f) to
                                WireframeColors.Background.copy(alpha = 0f),
                            revealProgress.value.coerceIn(0f, 1f) to
                                WireframeColors.Background
                        ),
                        start = Offset(0f, screenHeightPx.toFloat()),
                        end = Offset(screenWidthPx.toFloat(), 0f)
                    )
                )
        )
    }

    if (selection == PartySelection.INVITE) {
        Dialog(onDismissRequest = { selection = PartySelection.NONE }) {
            InviteDialogContent(
                code = inviteCode,
                participants = inviteParticipants,
                onInviteFriendClick = onInviteFriendClick,
                onGoFocusModeClick = onGoFocusModeClick
            )
        }
    }

    if (selection == PartySelection.JOIN) {
        Dialog(
            onDismissRequest = {
                selection = PartySelection.NONE
                joinSucceeded = false
                joinCodeInput = ""
            }
        ) {
            JoinDialogContent(
                codeInput = joinCodeInput,
                onCodeInputChange = { joinCodeInput = it },
                succeeded = joinSucceeded,
                participants = joinParticipants,
                onJoinClick = { if (joinCodeInput.isNotBlank()) joinSucceeded = true }
            )
        }
    }
}

@Composable
private fun InviteDialogContent(
    code: String,
    participants: List<String>,
    onInviteFriendClick: () -> Unit,
    onGoFocusModeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Your party code", color = Color.White, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = code,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )

        Spacer(Modifier.height(24.dp))

        Text(text = "Participants", color = Color.White, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        participants.forEach { name ->
            Text(
                text = name,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onInviteFriendClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, Color.White)
        ) {
            Text("Invite your friend")
        }
        Text(
            text = "focusapp.app/party/$code",
            color = Color.LightGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onGoFocusModeClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Go Focus Mode", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun JoinDialogContent(
    codeInput: String,
    onCodeInputChange: (String) -> Unit,
    succeeded: Boolean,
    participants: List<String>,
    onJoinClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!succeeded) {
            Text(
                text = "Enter the inviting code",
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = codeInput,
                onValueChange = onCodeInputChange,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onJoinClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Join", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(text = "Participants", color = Color.White, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            participants.forEach { name ->
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Waiting for host to start...",
                color = Color.LightGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PartyModeScreenPreview() {
    PartyModeScreen()
}
