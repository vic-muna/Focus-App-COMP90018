package com.example.focusapp.ui.common

import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.focusapp.ui.theme.FocusAppTheme

/**
 * Explains why app blocking needs the accessibility permission, then opens
 * Android's Accessibility settings (it can't be asked with a normal pop-up).
 * Shown before a focus session starts without it (Quick Focus, banners, Party Mode).
 */
@Composable
fun AccessibilityPermissionDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accessibility permission needed") },
        text = {
            Text(
                "To block apps during a focus session, Focus needs the " +
                    "Accessibility permission. Turn it on for Focus in the " +
                    "Settings screen that opens next."
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            ) { Text("Open Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview
@Composable
private fun AccessibilityPermissionDialogPreview() {
    FocusAppTheme {
        AccessibilityPermissionDialog(onDismiss = {})
    }
}
