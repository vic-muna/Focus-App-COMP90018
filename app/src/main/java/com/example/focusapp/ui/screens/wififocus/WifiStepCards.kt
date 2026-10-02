package com.example.focusapp.ui.screens.wififocus

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.data.wifi.WifiCheckResult
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import com.example.focusapp.ui.components.button.BackButton
import com.example.focusapp.ui.components.button.ConfirmButton
import com.example.focusapp.ui.components.button.NextButton
import com.example.focusapp.ui.components.button.RejectButton
import com.example.focusapp.ui.components.card.AppIconStack
import com.example.focusapp.ui.components.card.CardButtonRow
import com.example.focusapp.ui.components.card.CardLabel
import com.example.focusapp.ui.components.card.CardTitle
import com.example.focusapp.ui.components.card.FocusCard
import com.example.focusapp.ui.components.input.FocusTextField
import com.example.focusapp.ui.components.bar.verticalScrollbar
import com.example.focusapp.ui.components.card.sunkenPanel

private val WifiListMaxHeight = 110.dp

/**
 * Step 1 of adding / editing a Wi-Fi entry, four rows:
 *  1. "Connected now": [currentSsid], the network the phone is on - tap it to
 *     save it to the list ([onSaveCurrent]). Null when it can't be read -
 *     [checkResult] says why; tapping that message calls [onResultAction],
 *     e.g. to open settings.
 *  2. "Saved Wi-Fi": [savedSsids] - tap one to remove it ([onRemoveSaved]).
 *  3. "Block on these Wi-Fi": tick the networks that start blocking
 *     ([selectedSsids], [onToggleSsid]). Lists the saved networks plus any
 *     ticked ones that aren't saved any more, so an edit doesn't lose them.
 *  4. "Blocked Apps": [appIcons] picked so far - tap to pick ([onAppsClick]).
 * Android doesn't let apps read the phone's saved-network list, so networks
 * are only added from row 1.
 */
@Composable
fun WifiNetworkStepCard(
    currentSsid: String?,
    checkResult: WifiCheckResult?,
    savedSsids: List<String>,
    selectedSsids: Set<String>,
    appIcons: List<Bitmap?>,
    onSaveCurrent: (String) -> Unit,
    onRemoveSaved: (String) -> Unit,
    onToggleSsid: (String) -> Unit,
    onCheckAgain: () -> Unit,
    onResultAction: (WifiCheckResult) -> Unit,
    onAppsClick: () -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography
    val panel = Modifier.sunkenPanel(colors.surfaceSunken)

    FocusCard(modifier = modifier) {
        CardButtonRow(
            left = { RejectButton(onClick = onClose) },
            right = { NextButton(onClick = onNext, enabled = selectedSsids.isNotEmpty() && appIcons.isNotEmpty()) },
        )
        CardTitle("Wi-Fi Network", bottomPadding = 0.dp)

        // 1. The network the phone is on now - tap to save it.
        CardLabel("Connected now")
        Column(modifier = panel) {
            if (currentSsid != null) {
                val saved = currentSsid in savedSsids
                WifiRow(
                    ssid = currentSsid,
                    onClick = if (saved) null else ({ onSaveCurrent(currentSsid) }),
                    trailing = {
                        Text(
                            text = if (saved) "Saved" else "Tap to save",
                            style = typography.caption,
                            color = if (saved) colors.onSurfaceMuted else colors.accent,
                        )
                    },
                )
            } else {
                val message = checkResult?.message() ?: "Checking the Wi-Fi this phone is on…"
                val canFix = checkResult?.hasAction() == true
                Text(
                    text = message,
                    style = typography.caption,
                    color = if (canFix) colors.rejection else colors.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (canFix) Modifier.clickable { onResultAction(checkResult!!) } else Modifier)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
            Text(
                text = "Check again",
                style = typography.caption,
                color = colors.accent,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(role = Role.Button, onClick = onCheckAgain)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // 2. The saved list - tap a network to remove it.
        CardLabel("Saved Wi-Fi (tap to remove)")
        WifiList(
            ssids = savedSsids,
            emptyText = "Tap the network above to save it here.",
        ) { ssid ->
            WifiRow(
                ssid = ssid,
                onClick = { onRemoveSaved(ssid) },
                trailing = { Text(text = "Remove", style = typography.caption, color = colors.rejection) },
            )
        }

        // 3. Which networks start blocking.
        CardLabel("Block apps on these Wi-Fi")
        WifiList(
            ssids = (savedSsids + selectedSsids).distinct(),
            emptyText = "Save a Wi-Fi first, then tick it here.",
        ) { ssid ->
            val ticked = ssid in selectedSsids
            WifiRow(
                ssid = ssid,
                onClick = { onToggleSsid(ssid) },
                role = Role.Checkbox,
                highlighted = ticked,
                trailing = { TickBox(ticked) },
            )
        }

        // 4. The apps to block - opens the app picker.
        CardLabel("Blocked Apps")
        Row(
            modifier = panel
                .clickable(role = Role.Button, onClick = onAppsClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (appIcons.isNotEmpty()) {
                AppIconStack(icons = appIcons, iconSize = 28.dp)
                Spacer(Modifier.weight(1f))
            }
            Text(
                text = when (appIcons.size) {
                    0 -> "Tap to pick apps"
                    1 -> "1 app selected"
                    else -> "${appIcons.size} apps selected"
                },
                style = typography.caption,
                color = if (appIcons.isEmpty()) colors.accent else colors.onSurface,
                textAlign = TextAlign.End,
                modifier = if (appIcons.isEmpty()) Modifier.fillMaxWidth() else Modifier,
            )
        }
    }
}

/** A height-capped list of Wi-Fi rows, or [emptyText] when there are none. */
@Composable
private fun WifiList(
    ssids: List<String>,
    emptyText: String,
    row: @Composable (String) -> Unit,
) {
    val colors = FocusTheme.colors
    val panel = Modifier.sunkenPanel(colors.surfaceSunken)
    if (ssids.isEmpty()) {
        Text(
            text = emptyText,
            style = FocusTheme.typography.caption,
            color = colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = panel.padding(horizontal = 16.dp, vertical = 14.dp),
        )
    } else {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = panel
                .heightIn(max = WifiListMaxHeight)
                .verticalScrollbar(
                    state = listState,
                    thumbColor = colors.onSurfaceMuted,
                    trackColor = colors.surface,
                ),
        ) {
            items(ssids, key = { it }) { ssid -> row(ssid) }
        }
    }
}

/** One network: Wi-Fi icon, name and [trailing]. Not tappable when [onClick] is null. */
@Composable
private fun WifiRow(
    ssid: String,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit,
    role: Role = Role.Button,
    highlighted: Boolean = false,
) {
    val colors = FocusTheme.colors
    val tint = if (highlighted) colors.accent else colors.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = role, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.wifi),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(width = 23.dp, height = 18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = ssid,
            style = FocusTheme.typography.body,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

/** A small round tick box: filled when [ticked]. */
@Composable
private fun TickBox(ticked: Boolean) {
    val colors = FocusTheme.colors
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .size(18.dp)
            .border(2.dp, if (ticked) colors.accent else colors.onSurfaceMuted, shape)
            .background(if (ticked) colors.accent else Color.Transparent, shape),
    )
}

/** Why a check found no network name - worded for the step card. */
private fun WifiCheckResult.message(): String? = when (this) {
    is WifiCheckResult.Connected -> null
    WifiCheckResult.NotOnWifi -> "This phone isn't connected to Wi-Fi."
    WifiCheckResult.NeedsPreciseLocation ->
        "Tap to open precise location."
    WifiCheckResult.LocationServicesOff ->
        " Tap to turn on phone's location."
    WifiCheckResult.NameUnavailable -> "Android didn't share the Wi-Fi name. Try again in a moment."
}

/** Whether tapping the message can fix the problem (it opens a settings screen). */
private fun WifiCheckResult.hasAction(): Boolean =
    this == WifiCheckResult.NeedsPreciseLocation || this == WifiCheckResult.LocationServicesOff

/** Step 2 (last): the entry's name. The check saves once a name is entered. */
@Composable
fun WifiNameStepCard(
    name: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusCard(modifier = modifier) {
        CardButtonRow(
            left = { BackButton(onClick = onBack) },
            right = { ConfirmButton(onClick = onConfirm, contentDescription = "Save Wi-Fi", enabled = name.isNotBlank()) },
        )
        CardTitle("Name")

        FocusTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = "Enter Group Name",
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun WifiNetworkStepCardPreview() {
    FocusAppTheme {
        WifiNetworkStepCard(
            currentSsid = "Cafe Free WiFi",
            checkResult = WifiCheckResult.Connected("Cafe Free WiFi"),
            savedSsids = listOf("MyHome_5G", "Library-Guest", "Office"),
            selectedSsids = setOf("Library-Guest"),
            appIcons = List(3) { null },
            onSaveCurrent = {},
            onRemoveSaved = {},
            onToggleSsid = {},
            onCheckAgain = {},
            onResultAction = {},
            onAppsClick = {},
            onClose = {},
            onNext = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun WifiNetworkStepCardNoWifiPreview() {
    FocusAppTheme {
        WifiNetworkStepCard(
            currentSsid = null,
            checkResult = WifiCheckResult.NeedsPreciseLocation,
            savedSsids = emptyList(),
            selectedSsids = emptySet(),
            appIcons = emptyList(),
            onSaveCurrent = {},
            onRemoveSaved = {},
            onToggleSsid = {},
            onCheckAgain = {},
            onResultAction = {},
            onAppsClick = {},
            onClose = {},
            onNext = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun WifiNameStepCardPreview() {
    FocusAppTheme {
        WifiNameStepCard(
            name = "Home",
            onNameChange = {},
            onBack = {},
            onConfirm = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
