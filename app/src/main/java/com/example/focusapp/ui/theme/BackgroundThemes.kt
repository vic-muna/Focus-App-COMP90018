package com.example.focusapp.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.focusapp.R
import com.example.focusapp.domain.model.RewardRules

/**
 * One background theme: a character and the art that goes with them. The
 * picked theme shows on the dashboard's ID card, Home's illustration
 * ([homeArt]), the Focus Mode background ([focusArt]), the Time Focus
 * header ([timeFocusArt]) and the Time Focus notification's colour ([timeFocusColor]).
 * A theme with [unlockStreakDays] above 0 is a reward: it stays locked until the
 * user has reached the daily goal that many days in a row (at any time in the past).
 */
data class BackgroundTheme(
    val id: String,
    val name: String,
    val intro: String,
    @DrawableRes val homeArt: Int,
    @DrawableRes val focusArt: Int,
    @DrawableRes val timeFocusArt: Int,
    val timeFocusColor: Color,
    /** Days in a row reaching the daily goal needed to use this theme. 0 = free from the start. */
    val unlockStreakDays: Int = 0,
) {
    fun isUnlocked(bestStreakDays: Int): Boolean = bestStreakDays >= unlockStreakDays
}

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

    val Valley = BackgroundTheme(
        id = "theme002",
        name = "Valley",
        intro = "Enjoy the calm of the valley",
        homeArt = R.drawable.img_home_dashboard_theme002,
        focusArt = R.drawable.img_focus_background_theme002,
        // TODO(design): no Valley header yet - crops the Focus Mode art for now.
        timeFocusArt = R.drawable.img_focus_background_theme002,
        timeFocusColor = Palette.Truffle,
        unlockStreakDays = RewardRules.VALLEY_UNLOCK_STREAK_DAYS,
    )

    val all: List<BackgroundTheme> = listOf(Scene, Valley)

    val default: BackgroundTheme = Scene

    /** How many tiles the picker shows - slots without a theme yet say "Coming soon". */
    const val PICKER_SLOTS = 6

    /** The theme with [id], or [default] if there's none (e.g. nothing picked yet). */
    fun byId(id: String?): BackgroundTheme = all.find { it.id == id } ?: default
}