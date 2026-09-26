package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * A themed yes/no dialog. Pass [FocusColors.rejection] as [confirmColor]
 * for destructive actions like deleting.
 */
@Composable
fun FocusConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancel",
    confirmColor: Color = FocusTheme.colors.accent,
) {
    Dialog(onDismissRequest = onDismiss) {
        FocusConfirmDialogContent(
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            dismissLabel = dismissLabel,
            confirmColor = confirmColor,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

/** The card inside [FocusConfirmDialog], split out so it can be previewed without a window. */
@Composable
private fun FocusConfirmDialogContent(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = typography.inputLarge, color = colors.onSurface, textAlign = TextAlign.Center)
        Text(text = message, style = typography.body, color = colors.onSurface, textAlign = TextAlign.Center)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DialogButton(
                label = dismissLabel,
                containerColor = colors.surfaceSunken,
                contentColor = colors.onSurface,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            DialogButton(
                label = confirmLabel,
                containerColor = confirmColor,
                contentColor = colors.pure,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = FocusTheme.typography.body, color = contentColor)
    }
}

@Preview(widthDp = 320)
@Composable
private fun FocusConfirmDialogPreview() {
    FocusAppTheme {
        FocusConfirmDialogContent(
            title = "Delete \"Library\"?",
            message = "This focus location will be removed from this phone.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            confirmColor = FocusTheme.colors.rejection,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
