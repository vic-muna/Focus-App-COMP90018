package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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

/** notice.xml's height relative to its width (124 x 111 in Figma). */
private const val NOTICE_HEIGHT_RATIO = 111f / 124f

/**
 * Figma: the blocking screen's warning sign (notice.xml) - a rounded
 * triangle with the exclamation mark cut out of it, so whatever is behind
 * shows through the mark. Only the triangle's [color] is customizable.
 */
@Composable
fun WarningSign(
    modifier: Modifier = Modifier,
    color: Color = FocusTheme.colors.notification,
    width: Dp = 124.dp,
) {
    Icon(
        painter = painterResource(R.drawable.notice),
        contentDescription = null,
        tint = color,
        modifier = modifier.size(width = width, height = width * NOTICE_HEIGHT_RATIO),
    )
}

@Preview
@Composable
private fun WarningSignPreview() {
    FocusAppTheme {
        Box(Modifier.background(FocusTheme.colors.background).padding(16.dp)) {
            WarningSign()
        }
    }
}
