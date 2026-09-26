package com.example.focusapp.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusTheme

/** A single tappable icon from the Figma icon set, tinted with the theme's [onSurface] color. */
@Composable
fun FocusIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 42.dp,
) {
    IconButton(onClick = onClick, modifier = modifier.size(iconSize + 8.dp)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = FocusTheme.colors.onSurface,
            modifier = Modifier.size(iconSize),
        )
    }
}
