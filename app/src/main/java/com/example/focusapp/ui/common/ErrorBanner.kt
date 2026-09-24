package com.example.focusapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ErrorBanner
 * -------------
 * Shared "something went wrong" banner - same pink pill originally written
 * as PartyModeScreen's private PartyErrorBanner, pulled out here so every
 * screen that can fail a save (AddAppGroupScreen, EditLocationZoneScreen,
 * FocusSessionScreen, FriendListScreen, PartyModeScreen) shows failures the
 * same way instead of the app just crashing on an uncaught exception.
 *
 * Deliberately just a static Text, not a dismiss button/timeout - callers
 * own when the message disappears (usually: clearing their own error state
 * on the next attempt, or when the screen is left). See any of the call
 * sites above for the pattern: catch the exception where the suspend call
 * is made, store the message in a bit of state, show it via this composable.
 */
@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = Color.Black,
        fontSize = 12.sp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF5C8C8))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

/** Turns a raw exception into a short, user-facing message - reused by every screen that
 *  wraps a FocusRepository save/read call in try/catch (see [ErrorBanner]'s doc comment).
 *  Not trying to be exhaustive: falls back to the exception's own message/class name for
 *  anything it doesn't specifically recognise, which is still far better than a crash. */
fun friendlyErrorMessage(e: Throwable, action: String): String {
    val raw = e.message.orEmpty()
    return when {
        e is IllegalArgumentException ->
            raw.ifBlank { "$action failed: some of the input isn't valid." }
        raw.contains("permission", ignoreCase = true) ->
            "$action failed: permission denied (check the Firebase Realtime Database Rules)."
        e::class.java.simpleName.contains("FirebaseAuth", ignoreCase = true) ->
            "$action failed: couldn't sign in to Firebase. Check Authentication -> Sign-in method -> Anonymous is enabled."
        else ->
            "$action failed (${e::class.java.simpleName}${if (raw.isNotBlank()) ": $raw" else ""}). Check your connection and try again."
    }
}
