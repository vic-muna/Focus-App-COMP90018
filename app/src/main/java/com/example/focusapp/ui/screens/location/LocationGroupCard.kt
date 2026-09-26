package com.example.focusapp.ui.screens.location

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
import com.example.focusapp.ui.components.FocusSwitch
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import java.util.Locale

/**
 * Figma: one row of the location group list - name, details and an on/off
 * switch. Tapping the card calls [onClick] (edit); holding it calls
 * [onLongClick] (delete). The switch handles its own taps.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocationGroupCard(
    name: String,
    subtitle: String,
    latitude: Double,
    longitude: Double,
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
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = name,
                style = typography.cardTitle,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 2.dp),
            )
            Text(
                text = subtitle,
                style = typography.caption,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = String.format(
                    Locale.US,
                    "Latitude: %.1f | Longitude: %.1f",
                    latitude,
                    longitude,
                ),
                style = typography.caption,
                color = colors.onSurface,
            )
        }
        Spacer(Modifier.width(8.dp))
        FocusSwitch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Preview
@Composable
private fun LocationGroupCardPreview() {
    FocusAppTheme {
        LocationGroupCard(
            name = "Group001",
            subtitle = "Approx. FBE Library",
            latitude = 40.7,
            longitude = -74.1,
            enabled = true,
            onEnabledChange = {},
        )
    }
}
