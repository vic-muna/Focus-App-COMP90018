package com.example.focusapp.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * A single tappable icon from the Figma icon set. [tint] defaults to the
 * theme's onSurface color; pass [Color.Unspecified] for multi-color icons
 * that must keep the colors baked into their drawable.
 */
@Composable
fun FocusIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 42.dp,
    tint: Color = FocusTheme.colors.onSurface,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(iconSize + 8.dp)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Preview
@Composable
private fun FocusIconButtonPreview() {
    FocusAppTheme {
        FocusIconButton(iconRes = R.drawable.ic_setting_fill, contentDescription = "Settings", onClick = {})
    }
}
