package com.example.focusapp.ui.screens.location

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.FocusSwitch
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import java.util.Locale

/** Figma: one row of the location group list - name, details and an on/off switch. */
@Composable
fun LocationGroupCard(
    name: String,
    subtitle: String,
    latitude: Double,
    longitude: Double,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = typography.cardTitle,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = subtitle, style = typography.caption, color = colors.onSurface)
            Text(
                text = String.format(
                    Locale.US,
                    "Latitude: %.4f | Longitude: %.4f",
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
            subtitle = "Effective range: 100 m",
            latitude = 40.7,
            longitude = -74.1,
            enabled = true,
            onEnabledChange = {},
        )
    }
}
