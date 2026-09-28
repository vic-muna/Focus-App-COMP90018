package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.button.ConfirmButton
import com.example.focusapp.ui.components.button.RejectButton
import com.example.focusapp.ui.components.card.CardButtonRow
import com.example.focusapp.ui.components.card.CardTitle
import com.example.focusapp.ui.components.card.FocusCard
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * Fly card shown when a Time Focus slot is on but Focus can't read app usage yet.
 * The daily limits (Max Open Times / Max Minutes) need Android's "Usage access";
 * the check calls [onOpenSettings] to turn it on.
 */
@Composable
fun UsageAccessCard(
    groupName: String,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusCard(modifier = modifier) {
        CardButtonRow(
            left = { RejectButton(onClick = onClose, contentDescription = "Close") },
            right = { ConfirmButton(onClick = onOpenSettings, contentDescription = "Open settings") },
        )
        CardTitle("Usage access needed")
        Text(
            text = "\"$groupName\" is on now. To count how often and how long each app is used, " +
                "turn on Usage access for Focus in the settings that open next.",
            style = FocusTheme.typography.body,
            color = FocusTheme.colors.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun UsageAccessCardPreview() {
    FocusAppTheme {
        UsageAccessCard(groupName = "Study Group", onOpenSettings = {}, onClose = {}, modifier = Modifier.padding(16.dp))
    }
}
