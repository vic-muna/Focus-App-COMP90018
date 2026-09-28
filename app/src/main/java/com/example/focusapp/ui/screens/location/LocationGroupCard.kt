package com.example.focusapp.ui.screens.location

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.card.SwitchListCard
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import java.util.Locale

/** One saved location in the list: name, place, coordinates and an on/off switch. */
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

    SwitchListCard(
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        spacing = 4.dp,
        verticalPadding = 10.dp,
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
            text = String.format(Locale.US, "Latitude: %.1f | Longitude: %.1f", latitude, longitude),
            style = typography.caption,
            color = colors.onSurface,
        )
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
