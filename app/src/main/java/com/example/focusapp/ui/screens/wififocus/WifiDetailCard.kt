package com.example.focusapp.ui.screens.wififocus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.ui.components.AppIconStack
import com.example.focusapp.ui.components.EditButton
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import com.example.focusapp.data.blocking.BlockedAppGroup
import com.example.focusapp.ui.common.previewGroups
import com.example.focusapp.ui.components.FocusCard
import com.example.focusapp.ui.components.CardSectionTitle
import com.example.focusapp.ui.components.sunkenPanel

/**
 * The read-only summary of a Wi-Fi entry (same layout idea as the time
 * slot summary) - its name, the network it watches ([BlockedAppGroup.id] is
 * the SSID) and the apps it blocks. The pencil opens the edit steps.
 */
@Composable
fun WifiDetailCard(
    group: BlockedAppGroup,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography
    val panel = Modifier.sunkenPanel(colors.surfaceSunken)

    FocusCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = group.name,
                style = typography.cardTitle,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            Spacer(Modifier.width(8.dp))
            EditButton(onClick = onEdit, contentDescription = "Edit ${group.name}")
        }

        CardSectionTitle("Wi-Fi Network")
        Row(
            modifier = panel.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.wifi),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(width = 32.dp, height = 25.dp),
            )
            Text(
                text = group.id,
                style = typography.tileTitle,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        CardSectionTitle("Blocked Apps")
        Row(
            modifier = panel.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconStack(icons = group.apps.map { it.icon }, iconSize = 36.dp)
            Spacer(Modifier.weight(1f))
            Text(
                text = if (group.apps.size == 1) "1 app selected" else "${group.apps.size} apps selected",
                style = typography.listLabel,
                color = colors.onSurface,
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun WifiDetailCardPreview() {
    FocusAppTheme {
        WifiDetailCard(
            group = previewGroups().first().copy(id = "MyHome_5G", name = "Home"),
            onEdit = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}