package com.example.focusapp.domain.model

import androidx.annotation.RawRes
import com.example.focusapp.R

/**
 * One Focus Mode music track: an mp3 in res/raw ([audio]), played on a loop (see FocusMusicPlayer).
 * A track with [pricePoints] above 0 is bought with points.
 */
data class FocusMusic(
    val id: String,
    val name: String,
    val intro: String,
    @RawRes val audio: Int,
    val pricePoints: Int = 0,
) {
    fun isUnlocked(ownedIds: Set<String>): Boolean = pricePoints == 0 || id in ownedIds
}

/** Every track the app ships with. Default is free; the rest are bought with points. */
object FocusMusics {
    // Keeps the old "focus_music_01" id so anyone who already picked it stays on it.
    val default = FocusMusic(
        id = "focus_music_01",
        name = "Default",
        intro = "The default focus music",
        audio = R.raw.focus_music_01,
    )

    // TODO(copy): intros are placeholders - names come from the file names.
    private val tracks = listOf(
        "lofi_study" to R.raw.lofi_study,
        "lofi_focus" to R.raw.lofi_focus,
        "lofi_relax" to R.raw.lofi_relax,
        "lofi_jazz" to R.raw.lofi_jazz,
        "lofi_rain" to R.raw.lofi_rain,
        "lofi_tokyo" to R.raw.lofi_tokyo,
        "lofi_sad" to R.raw.lofi_sad,
        "lofi_cocktail" to R.raw.lofi_cocktail,
        "lofi_funky" to R.raw.lofi_funky,
        "lofi_gaming" to R.raw.lofi_gaming,
        "lofi_christmas" to R.raw.lofi_christmas,
        "piano_study" to R.raw.piano_study,
        "study_jazz" to R.raw.study_jazz,
        "lightroom_study" to R.raw.lightroom_study,
        "over_nara" to R.raw.over_nara,
        "soft_fly" to R.raw.soft_fly,
        "soft_return" to R.raw.soft_return,
        "still_warm" to R.raw.still_warm,
        "streetlamp_wind" to R.raw.streetlamp_wind,
        "never" to R.raw.never,
    )

    val all: List<FocusMusic> = listOf(default) + tracks.map { (file, audio) ->
        // "lofi_study" -> "Lofi Study"
        val name = file.split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        FocusMusic(
            id = "music_$file",
            name = name,
            intro = name,
            audio = audio,
            pricePoints = RewardRules.MUSIC_PRICE_POINTS,
        )
    }

    /** The track with [id], or [default] if there's none (e.g. nothing picked yet). */
    fun byId(id: String?): FocusMusic = all.find { it.id == id } ?: default
}
