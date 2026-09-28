package com.example.focusapp.ui.screens.wififocus

import androidx.compose.foundation.background
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
import com.example.focusapp.ui.components.card.CardButtonRow
import com.example.focusapp.ui.components.card.CardLabel
import com.example.focusapp.ui.components.card.CardTitle
import com.example.focusapp.ui.components.card.FocusCard
import com.example.focusapp.ui.components.input.FocusTextField
import com.example.focusapp.ui.components.bar.verticalScrollbar
import com.example.focusapp.ui.components.card.sunkenPanel

private val KnownListMaxHeight = 150.dp

/**
 * Step 1 of adding / editing a Wi-Fi entry: which network it watches.
 * Android doesn't let apps read the phone's saved-network list, so the
 * choices are:
 *  - "Connected now": [currentSsid], the network the phone is on (null when
 *    it can't be read - [checkResult] says why; tapping that message calls
 *    [onResultAction], e.g. to open settings)
 *  - "Known Wi-Fi": [knownSsids], networks this app has seen before
 *  - a typed name ([manualSsid]) for anything else
 * [selectedSsid] is the pick so far. Networks in [addedSsids] are already
 * in the list, so they can't be picked again.
 */
@Composable
fun WifiNetworkStepCard(
    selectedSsid: String?,
    currentSsid: String?,
    checkResult: WifiCheckResult?,
    knownSsids: List<String>,
    addedSsids: Set<String>,
    manualSsid: String,
    onSelect: (String) -> Unit,
    onManualChange: (String) -> Unit,
    onCheckAgain: () -> Unit,
    onResultAction: (WifiCheckResult) -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography
    val selectedTaken = selectedSsid != null && selectedSsid in addedSsids
    val panel = Modifier.sunkenPanel(colors.surfaceSunken)

    FocusCard(modifier = modifier) {
        CardButtonRow(
            left = { RejectButton(onClick = onClose) },
            right = { NextButton(onClick = onNext, enabled = selectedSsid != null && !selectedTaken) },
        )
        CardTitle("Wi-Fi Network", bottomPadding = 0.dp)

        CardLabel("Connected now")
        Column(modifier = panel) {
            if (currentSsid != null) {
                WifiChoiceRow(
                    ssid = currentSsid,
                    selected = currentSsid == selectedSsid,
                    added = currentSsid in addedSsids,
                    onClick = { onSelect(currentSsid) },
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

        CardLabel("Known Wi-Fi")
        val others = knownSsids.filter { it != currentSsid }
        if (others.isEmpty()) {
            Text(
                text = "Networks this phone connects to will show up here.",
                style = typography.caption,
                color = colors.onSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = panel.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = panel
                    .heightIn(max = KnownListMaxHeight)
                    .verticalScrollbar(
                        state = listState,
                        thumbColor = colors.onSurfaceMuted,
                        trackColor = colors.surface,
                    ),
            ) {
                items(others, key = { it }) { ssid ->
                    WifiChoiceRow(
                        ssid = ssid,
                        selected = ssid == selectedSsid,
                        added = ssid in addedSsids,
                        onClick = { onSelect(ssid) },
                    )
                }
            }
        }

        CardLabel("Or type its name")
        FocusTextField(
            value = manualSsid,
            onValueChange = onManualChange,
            placeholder = "Wi-Fi name",
        )

        if (selectedTaken) {
            Text(
                text = "This Wi-Fi is already in your list.",
                style = typography.caption,
                color = colors.rejection,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
    }
}

/**
 * One pickable network: Wi-Fi icon and name, highlighted when [selected].
 * An [added] network is already in the list - shown dimmed, not pickable.
 */
@Composable
private fun WifiChoiceRow(
    ssid: String,
    selected: Boolean,
    added: Boolean,
    onClick: () -> Unit,
) {
    val colors = FocusTheme.colors
    val tint = when {
        added -> colors.onSurfaceMuted
        selected -> colors.accent
        else -> colors.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !added, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
        if (added) {
            Text(text = "Added", style = FocusTheme.typography.caption, color = colors.onSurfaceMuted)
        } else if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(colors.accent, RoundedCornerShape(5.dp)),
            )
        }
    }
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

/** Step 3 (last): the entry's name. The check saves once a name is entered. */
@Composable
fun WifiNameStepCard(
    name: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors

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
            selectedSsid = "Library-Guest",
            currentSsid = "MyHome_5G",
            checkResult = WifiCheckResult.Connected("MyHome_5G"),
            knownSsids = listOf("MyHome_5G", "Library-Guest", "Cafe Free WiFi", "Office"),
            addedSsids = setOf("Office"),
            manualSsid = "",
            onSelect = {},
            onManualChange = {},
            onCheckAgain = {},
            onResultAction = {},
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
            selectedSsid = null,
            currentSsid = null,
            checkResult = WifiCheckResult.NeedsPreciseLocation,
            knownSsids = emptyList(),
            addedSsids = emptySet(),
            manualSsid = "",
            onSelect = {},
            onManualChange = {},
            onCheckAgain = {},
            onResultAction = {},
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