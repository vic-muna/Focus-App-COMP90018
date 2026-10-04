package com.example.focusapp.ui.components.card

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
import com.example.focusapp.ui.theme.FocusAppTheme

/**
 * A one-step "pick the apps to block" card, used by Quick Focus, Party Mode and
 * Flip to Focus. It loads the installed apps and keeps the ticks by itself.
 * [savedApps] start ticked; the check calls [onConfirm] with the new pick.
 */
@Composable
fun AppPickerCard(
    title: String,
    savedApps: List<AppItem>,
    onConfirm: (List<AppItem>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    confirmDescription: String = "Save apps",
) {
    var selection by remember { mutableStateOf(savedApps.map { it.packageName }.toSet()) }
    val installedApps = rememberInstalledApps(shouldLoad = true)

    AppSelectCard(
        title = title,
        apps = installedApps,
        selectedPackages = selection,
        onToggleApp = { pkg -> selection = selection.toggle(pkg) },
        onClose = onClose,
        actionButton = {
            ConfirmButton(
                onClick = { onConfirm(pickedApps(installedApps, selection, savedApps)) },
                contentDescription = confirmDescription,
                enabled = selection.isNotEmpty(),
            )
        },
        modifier = modifier,
    )
}

@Preview(widthDp = 360)
@Composable
private fun AppPickerCardPreview() {
    FocusAppTheme {
        // Shows "Loading apps…" in the preview: installed apps are only read on a phone.
        AppPickerCard(
            title = "Quick Focus Apps",
            savedApps = emptyList(),
            onConfirm = {},
            onClose = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
