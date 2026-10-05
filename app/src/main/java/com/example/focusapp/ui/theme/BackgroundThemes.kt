package com.example.focusapp.ui.theme

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.ui.graphics.Color
import com.example.focusapp.R
import com.example.focusapp.domain.model.RewardRules

/**
 * One background theme: a character and the art that goes with them. The
 * picked theme shows on the dashboard's ID card, Home's illustration
 * ([homeArt]), the Focus Mode background ([focusArt]), the Time Focus
 * header ([timeFocusArt]) and the Time Focus notification's colour ([timeFocusColor]).
 * A theme with a [focusVideo] plays it on a loop in Focus Mode instead of [focusArt].
 * [focusTextColor] colours the Focus Mode timer and noise label so they read well on it.
 * A theme with [pricePoints] above 0 is a reward: it stays locked until the
 * user buys it with focus points in the Rewards shop.
 */
data class BackgroundTheme(
    val id: String,
    val name: String,
    val intro: String,
    @DrawableRes val homeArt: Int,
    @DrawableRes val focusArt: Int,
    @DrawableRes val timeFocusArt: Int,
    val timeFocusColor: Color,
    /** Points this theme costs in the Rewards shop. 0 = free from the start. */
    val pricePoints: Int = 0,
    /** A looping portrait video for Focus Mode; null = the still [focusArt]. */
    @RawRes val focusVideo: Int? = null,
    /**
     * True when [homeArt] already has its own dark frame around it (Forest's art), so Home
     * shows it as-is. Otherwise Home crops it into a rounded card.
     */
    val homeArtHasFrame: Boolean = false,
    /** The Focus Mode timer and noise label colour, picked to stand out on [focusArt] / [focusVideo]. */
    val focusTextColor: Color = Palette.Truffle,
) {
    fun isUnlocked(ownedIds: Set<String>): Boolean = pricePoints == 0 || id in ownedIds
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
        homeArtHasFrame = true,
    )

    // TODO(copy): intros are placeholders - replace with the final text.
    val Desert = videoTheme("desert", "Desert", "Warm dunes under a wide sky", R.drawable.img_thumb_desert, R.raw.bg_desert, Palette.Truffle, Color(0xff2e1220))
    val East = videoTheme("east", "East", "Quiet temples and soft light", R.drawable.img_thumb_east, R.raw.bg_east, Palette.Rejection, Color(0xfff4f1de))
    val Europe = videoTheme("europe", "Europe", "Old streets and rooftops", R.drawable.img_thumb_europe, R.raw.bg_europe, Palette.Fantastic, Color(0xffffebd0))
    val Fantasy = videoTheme("fantasy", "Fantasy", "A world full of magic", R.drawable.img_thumb_fantasy, R.raw.bg_fantasy, Palette.Fantastic, Color(0xffece3ff))
    val Ocean = videoTheme("ocean", "Ocean", "Calm waves by the lighthouse", R.drawable.img_thumb_ocean, R.raw.bg_ocean, Palette.Fantastic, Color(0xff14304f))
    val Rock = videoTheme("rock", "Rock", "Turn it up and focus", R.drawable.img_thumb_rock, R.raw.bg_rock, Palette.Rejection, Color(0xffffe0f0))
    val SciFi = videoTheme("scifi", "Sci-Fi", "Focus among the stars", R.drawable.img_thumb_scifi, R.raw.bg_scifi, Palette.AbyssalBlue, Color(0xffb5f5ec))
    val Spring = videoTheme("spring", "Spring", "Fresh blossoms and green fields", R.drawable.img_thumb_spring, R.raw.bg_spring, Palette.Confirm, Color(0xff3e2148))
    val Western = videoTheme("western", "Western", "Sunset over the frontier", R.drawable.img_thumb_western, R.raw.bg_western, Palette.Truffle, Color(0xffffe2bc))
    val Winter = videoTheme("winter", "Winter", "Snow falling softly", R.drawable.img_thumb_winter, R.raw.bg_winter, Palette.AbyssalBlue, Color(0xffeef3ff))

    val all: List<BackgroundTheme> =
        listOf(Scene, Desert, East, Europe, Fantasy, Ocean, Rock, SciFi, Spring, Western, Winter)

    val default: BackgroundTheme = Scene

    /** How many tiles the picker shows - slots without a theme yet say "Coming soon". */
    const val PICKER_SLOTS = 12

    /** The theme with [id], or [default] if there's none (e.g. nothing picked yet). */
    fun byId(id: String?): BackgroundTheme = all.find { it.id == id } ?: default

    /** A shop theme with a thumbnail ([thumbnail]) and a looping Focus Mode [video]. */
    private fun videoTheme(
        id: String,
        name: String,
        intro: String,
        @DrawableRes thumbnail: Int,
        @RawRes video: Int,
        timeFocusColor: Color,
        focusTextColor: Color,
    ) = BackgroundTheme(
        id = id,
        name = name,
        intro = intro,
        homeArt = thumbnail,
        focusArt = thumbnail, // Shown until the video's first frame.
        timeFocusArt = thumbnail,
        timeFocusColor = timeFocusColor,
        pricePoints = RewardRules.BACKGROUND_PRICE_POINTS,
        focusVideo = video,
        focusTextColor = focusTextColor,
    )
}
