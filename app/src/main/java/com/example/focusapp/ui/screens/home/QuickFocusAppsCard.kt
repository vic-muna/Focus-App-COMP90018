package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.blocking.AppItem
import com.example.focusapp.ui.common.pickedApps
import com.example.focusapp.ui.common.rememberInstalledApps
import com.example.focusapp.ui.common.toggle
import com.example.focusapp.ui.components.button.ConfirmButton
import com.example.focusapp.ui.components.card.AppSelectCard
import com.example.focusapp.ui.theme.FocusAppTheme

/**
 * Shown every time Quick Focus (or a Party Mode group) starts: pick the apps to block.
 * The last pick ([savedApps]) starts ticked; the check calls [onStart] with the new pick.
 */
@Composable
fun QuickFocusAppsCard(
    savedApps: List<AppItem>,
    onStart: (List<AppItem>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selection by remember { mutableStateOf(savedApps.map { it.packageName }.toSet()) }
    val installedApps = rememberInstalledApps(shouldLoad = true)

    AppSelectCard(
        title = "Quick Focus Apps",
        apps = installedApps,
        selectedPackages = selection,
        onToggleApp = { pkg -> selection = selection.toggle(pkg) },
        onClose = onClose,
        actionButton = {
            ConfirmButton(
                onClick = { onStart(pickedApps(installedApps, selection, savedApps)) },
                contentDescription = "Start focusing",
                enabled = selection.isNotEmpty(),
            )
        },
        modifier = modifier,
    )
}

@Preview(widthDp = 360)
@Composable
private fun QuickFocusAppsCardPreview() {
    FocusAppTheme {
        // Shows "Loading apps…" in the preview: installed apps are only read on a phone.
        QuickFocusAppsCard(savedApps = emptyList(), onStart = {}, onClose = {}, modifier = Modifier.padding(16.dp))
    }
}
