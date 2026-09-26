package com.example.focusapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The Figma design uses "LINE Seed JP". Until its font files are added to
 * res/font, this falls back to the system font - swap this one value once
 * they are, and every text style below picks it up.
 */
val LineSeedFontFamily: FontFamily = FontFamily.Default

/** Named text styles from the Figma design. Colors are applied by callers from [FocusColors]. */
@Immutable
data class FocusTypography(
    /** Home greeting first line, e.g. "Hi! User". */
    val greetingTitle: TextStyle,
    /** Home greeting second line. */
    val greetingBody: TextStyle,
    /** Large label inside the Quick Focus button. */
    val primaryActionLabel: TextStyle,
    /** Card heading, e.g. a location group's name. */
    val cardTitle: TextStyle,
    /** Large text inside inputs and secondary buttons ("Enter Group Name", "Schedule"). */
    val inputLarge: TextStyle,
    /** Banner / body copy. */
    val body: TextStyle,
    /** Small supporting text (labels, coordinates). */
    val caption: TextStyle,
)

val DefaultFocusTypography = FocusTypography(
    greetingTitle = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
    ),
    greetingBody = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    primaryActionLabel = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
    ),
    cardTitle = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
    ),
    inputLarge = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    body = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    caption = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
)
