package com.example.focusapp.ui.screens.wififocus

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.AddItemCard
import com.example.focusapp.ui.components.AppIconStack
import com.example.focusapp.ui.components.FocusSwitch
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * One row of the Wi-Fi list - the entry's name, the network (SSID) it
 * watches, the apps it blocks, and an on/off switch. Tap = [onClick]
 * (summary), hold = [onLongClick] (delete); the switch handles its own taps.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WifiNetworkCard(
    name: String,
    ssid: String,
    appIcons: List<Bitmap?>,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = name,
                style = typography.body,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = ssid,
                style = typography.tileTitle,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (appIcons.isNotEmpty()) {
                AppIconStack(icons = appIcons, iconSize = 22.dp, ringColor = colors.surface)
            }
        }
        Spacer(Modifier.width(8.dp))
        FocusSwitch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Preview(widthDp = 360)
@Composable
private fun WifiNetworkCardPreview() {
    FocusAppTheme {
        Column(
            modifier = Modifier
                .background(FocusTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WifiNetworkCard(
                name = "Home",
                ssid = "MyHome_5G",
                appIcons = List(3) { null },
                enabled = true,
                onEnabledChange = {},
            )
            AddItemCard(onClick = {}, onClickLabel = "Add Wi-Fi network")
        }
    }
}