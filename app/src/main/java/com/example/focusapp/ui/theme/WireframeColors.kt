package com.example.focusapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * WireframeColors
 * ------------------
 * Plain color constants used to visually match the low-fidelity greyscale
 * wireframes a teammate designed for the Apps / Map / Settings flow
 * (grey background, dark rounded "pill" cards and buttons, light grey
 * icon placeholders).
 *
 * IMPORTANT: this is NOT a real design system. There's no light/dark mode
 * support, no elevation system, and no brand colors - it exists purely so
 * the skeleton screens look like the provided mockups. Swap these out (and
 * probably move to a proper MaterialTheme color scheme) once the team has
 * final visual designs.
 */
object WireframeColors {
    /** Overall screen background - matches the mid-grey mockup backdrop. */
    val Background = Color(0xFF9E9E9E)

    /** Dark rounded "pill" cards/buttons (app group rows, add button, etc.). */
    val Card = Color(0xFF4A4A4A)

    /** Slightly lighter dark surface, used for input-field placeholders. */
    val CardLight = Color(0xFF5C5C5C)

    /** Light grey rounded squares standing in for app icons (no real icons yet). */
    val IconPlaceholder = Color(0xFFD9D9D9)

    /** Bottom navigation bar background. */
    val BottomBar = Color(0xFF8C8C8C)

    /** Text/icon color when drawn on top of a dark Card/BottomBar. */
    val OnDark = Color(0xFFFFFFFF)

    /** Text color when drawn directly on the light grey Background. */
    val OnLight = Color(0xFF2B2B2B)
}
