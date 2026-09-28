package com.example.focusapp.ui.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** How dark the screen behind a fly card gets (Figma dims it heavily). */
private const val SCRIM_ALPHA = 0.8f

/**
 * Figma "fly card": dims the whole screen and centers [content] on top.
 * Tapping the dimmed area calls [onOutsideClick]; taps on the card itself
 * don't (give the card's root [Modifier.consumeTaps]). Place it last in a
 * full-screen Box so it covers everything, including the bottom nav.
 */
@Composable
fun FlyCardOverlay(
    onOutsideClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FocusTheme.colors.background.copy(alpha = SCRIM_ALPHA))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOutsideClick,
            )
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Preview(widthDp = 393, heightDp = 400)
@Composable
private fun FlyCardOverlayPreview() {
    FocusAppTheme {
        FlyCardOverlay(onOutsideClick = {}) {
            Text(
                text = "Card",
                color = FocusTheme.colors.onSurface,
                modifier = Modifier
                    .background(FocusTheme.colors.surface)
                    .padding(32.dp)
                    .consumeTaps(),
            )
        }
    }
}

/**
 * Makes this element swallow taps on its otherwise non-interactive areas
 * (padding, gaps, backgrounds), so they don't fall through to whatever is
 * underneath - e.g. a fly card over a scrim or map that treats taps as
 * "tapped outside the card". Its own buttons and fields still work as usual.
 */
fun Modifier.consumeTaps(): Modifier = pointerInput(Unit) { detectTapGestures() }
