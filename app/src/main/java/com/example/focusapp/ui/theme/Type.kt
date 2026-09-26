package com.example.focusapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.focusapp.R

/**
 * "LINE Seed JP", the Figma design's typeface (res/font). Only these four
 * weights are bundled - any other FontWeight a style asks for (e.g. Medium)
 * is drawn with the closest one.
 */
val LineSeedFontFamily: FontFamily = FontFamily(
    Font(R.font.line_seed_jp_thin, FontWeight.Thin),
    Font(R.font.line_seed_jp_regular, FontWeight.Normal),
    Font(R.font.line_seed_jp_bold, FontWeight.Bold),
    Font(R.font.line_seed_jp_extrabold, FontWeight.ExtraBold),
)

/** Named text styles from the Figma design. Colors are applied by callers from [FocusColors]. */
@Immutable
data class FocusTypography(
    /** Home greeting first line, e.g. "Hi! User". */
    val greetingTitle: TextStyle,
    /** Home greeting second line. */
    val greetingBody: TextStyle,
    /** Large label inside the Quick Focus button. */
    val primaryActionLabel: TextStyle,
    /** Focus Mode's elapsed-time readout. */
    val timer: TextStyle,
    /** A big standalone number in a summary, e.g. a group's break count. */
    val statValue: TextStyle,
    /** Tooltip / hint bubble copy. */
    val hint: TextStyle,
    /** A short question or call to action on a full-screen prompt (blocking screen). */
    val prompt: TextStyle,
    /** Label above a [counterValue], e.g. "Left". */
    val counterLabel: TextStyle,
    /** A large count on a full-screen prompt, e.g. rests left "4/5". */
    val counterValue: TextStyle,
    /** Card heading, e.g. a location group's name. */
    val cardTitle: TextStyle,
    /** Heading on a grid tile, e.g. an app group's name. */
    val tileTitle: TextStyle,
    /** Label on a compact list row, e.g. an app name in the app picker. */
    val listLabel: TextStyle,
    /** Text on a filled pill button. */
    /** Large text inside inputs and secondary buttons ("Enter Group Name", "Schedule"). */
    val inputLarge: TextStyle,
    /** Banner / body copy. */
    val body: TextStyle,
    /** Small supporting text (labels, coordinates). */
    val caption: TextStyle,
    val confirmation : TextStyle,
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
    timer = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
    ),
    statValue = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
    ),
    hint = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
    ),
    prompt = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
    ),
    counterLabel = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
    ),
    counterValue = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 35.sp,
    ),
    cardTitle = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
    ),
    tileTitle = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    listLabel = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
    ),
    inputLarge = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    body = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    caption = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
    confirmation = TextStyle(
        fontFamily = LineSeedFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
    ),
)
