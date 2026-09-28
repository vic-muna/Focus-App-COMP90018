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
 * A pink "something went wrong" message. The screen decides when it goes
 * away (usually by clearing its error on the next try).
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
