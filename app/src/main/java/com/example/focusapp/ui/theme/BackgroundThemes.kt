package com.example.focusapp.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.focusapp.R

/**
 * One background theme: a character and the art that goes with them. The
 * picked theme shows on the dashboard's ID card, Home's illustration
 * ([homeArt]), the Focus Mode background ([focusArt]), the Time Focus
 * header ([timeFocusArt]) and the Time Focus notification's colour ([timeFocusColor]).
 */
data class BackgroundTheme(
    val id: String,
    val name: String,
    val intro: String,
    @DrawableRes val homeArt: Int,
    @DrawableRes val focusArt: Int,
    @DrawableRes val timeFocusArt: Int,
    val timeFocusColor: Color,
)

/** Every theme the app ships with. Add new ones to [all] once their art is exported. */
object BackgroundThemes {
    // TODO(copy): name and intro are placeholders - replace with the final text.
    val Scene = BackgroundTheme(
        id = "Scene",
        name = "Forest",
        intro = "Enjoy the forest",
        homeArt = R.drawable.img_home_dashboard,
        focusArt = R.drawable.img_focus_background,
        timeFocusArt = R.drawable.img_app_focus_header,
        timeFocusColor = Palette.Fantastic,
    )

    val all: List<BackgroundTheme> = listOf(Scene)

    val default: BackgroundTheme = Scene

    /** How many tiles the picker shows - slots without a theme yet say "Coming soon". */
    const val PICKER_SLOTS = 6

    /** The theme with [id], or [default] if there's none (e.g. nothing picked yet). */
    fun byId(id: String?): BackgroundTheme = all.find { it.id == id } ?: default
}