package com.example.focusapp.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Makes this element swallow taps on its otherwise non-interactive areas
 * (padding, gaps, backgrounds), so they don't fall through to whatever is
 * underneath - e.g. a fly card over a scrim or map that treats taps as
 * "tapped outside the card". Its own buttons and fields still work as usual.
 */
fun Modifier.consumeTaps(): Modifier = pointerInput(Unit) { detectTapGestures() }
