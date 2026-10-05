package com.example.focusapp.domain.model

import androidx.annotation.RawRes
import com.example.focusapp.R

/**
 * One Focus Mode music track: an mp3 in res/raw ([audio]), or white noise made on the
 * phone when [audio] is null - played on a loop (see FocusMusicPlayer).
 * A track with [pricePoints] above 0 is bought with points.
 */
data class FocusMusic(
    val id: String,
    val name: String,
    val intro: String,
    @RawRes val audio: Int?,
    val pricePoints: Int = 0,
) {
    fun isUnlocked(ownedIds: Set<String>): Boolean = pricePoints == 0 || id in ownedIds
}

/** Every track the app ships with. White Noise and Track 1 (the default) are free; the rest are bought with points. */
object FocusMusics {
    val WhiteNoise = FocusMusic(
        id = "music_white",
        name = "White Noise",
        intro = "Steady hiss that hides background chatter",
        audio = null,
    )

    // TODO(copy): names and intros are placeholders - replace with the real track titles.
    private val tracks = listOf(
        R.raw.focus_music_01, R.raw.focus_music_02, R.raw.focus_music_03, R.raw.focus_music_04,
        R.raw.focus_music_05, R.raw.focus_music_06, R.raw.focus_music_07, R.raw.focus_music_08,
        R.raw.focus_music_09, R.raw.focus_music_10,
    )

    val all: List<FocusMusic> = listOf(WhiteNoise) + tracks.mapIndexed { index, audio ->
        val number = index + 1
        FocusMusic(
            id = "focus_music_%02d".format(number),
            name = "Track $number",
            intro = "Focus music $number",
            audio = audio,
            // Track 1 is the default, so it has to be free.
            pricePoints = if (index == 0) 0 else RewardRules.MUSIC_PRICE_POINTS,
        )
    }

    val default: FocusMusic = all[1] // Track 1

    /** The track with [id], or [default] if there's none (e.g. nothing picked yet). */
    fun byId(id: String?): FocusMusic = all.find { it.id == id } ?: default
}
