package com.example.focusapp.ui.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** The "RUN" tag on a Run background's art (see BackgroundTheme.isRun), placed in its top-right corner. */
@Composable
fun RunBadge(modifier: Modifier = Modifier) {
    Text(
        text = "RUN",
        style = FocusTheme.typography.microLabel,
        color = FocusTheme.colors.background,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(FocusTheme.colors.accent)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Preview
@Composable
private fun RunBadgePreview() {
    FocusAppTheme {
        RunBadge()
    }
}
