package com.example.focusapp.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.focusapp.data.blocking.AppItem
import com.example.focusapp.ui.common.pickedApps
import com.example.focusapp.ui.common.rememberInstalledApps
import com.example.focusapp.ui.common.toggle
import com.example.focusapp.ui.components.button.ConfirmButton
import com.example.focusapp.ui.components.card.AppSelectCard

/**
 * Settings -> Flip to Focus -> Apps to block: pick the apps a face-down session blocks.
 * Its own list, separate from Quick Focus, locations, schedules and Wi-Fi.
 * [savedApps] start ticked; the check calls [onSave] with the new pick.
 */
@Composable
fun FlipFocusAppsCard(
    savedApps: List<AppItem>,
    onSave: (List<AppItem>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selection by remember { mutableStateOf(savedApps.map { it.packageName }.toSet()) }
    val installedApps = rememberInstalledApps(shouldLoad = true)

    AppSelectCard(
        title = "Flip to Focus Apps",
        apps = installedApps,
        selectedPackages = selection,
        onToggleApp = { pkg -> selection = selection.toggle(pkg) },
        onClose = onClose,
        actionButton = {
            ConfirmButton(
                onClick = { onSave(pickedApps(installedApps, selection, savedApps)) },
                contentDescription = "Save apps",
                enabled = selection.isNotEmpty(),
            )
        },
        modifier = modifier,
    )
}
