package com.example.focusapp.ui.screens.wififocus

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.card.AddItemCard
import com.example.focusapp.ui.components.card.AppIconStack
import com.example.focusapp.ui.components.card.SwitchListCard
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** One Wi-Fi in the Wi-Fi list: name, network name (SSID), blocked apps and an on/off switch. */
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

    SwitchListCard(
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Text(
            text = name,
            style = FocusTheme.typography.body,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = ssid,
            style = FocusTheme.typography.tileTitle,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (appIcons.isNotEmpty()) {
            AppIconStack(icons = appIcons, iconSize = 22.dp, ringColor = colors.surface)
        }
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
