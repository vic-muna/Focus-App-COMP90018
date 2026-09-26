package com.example.focusapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** Figma: the two-line greeting above the Home dashboard. */
@Composable
fun GreetingHeader(
    userName: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Hi! $userName", style = typography.greetingTitle, color = colors.accent)
        Text(text = subtitle, style = typography.greetingBody, color = colors.accent)
    }
}

@Preview
@Composable
private fun GreetingHeaderPreview() {
    FocusAppTheme {
        GreetingHeader(userName = "User", subtitle = "Let's speed up your production")
    }
}
