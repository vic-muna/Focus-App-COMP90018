package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** The settings gear pinned to the top-right corner, shared by every top-level screen. */
@Composable
fun SettingsTopBar(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, end = 21.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        FocusIconButton(
            iconRes = R.drawable.ic_setting_fill,
            contentDescription = "Settings",
            onClick = onSettingsClick,
        )
    }
}

@Preview(widthDp = 393)
@Composable
private fun SettingsTopBarPreview() {
    FocusAppTheme {
        SettingsTopBar(onSettingsClick = {}, modifier = Modifier.background(FocusTheme.colors.background))
    }
}
