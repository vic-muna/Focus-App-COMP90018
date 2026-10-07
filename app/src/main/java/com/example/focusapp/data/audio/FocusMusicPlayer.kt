package com.example.focusapp.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.RawRes

/** How long the music takes to rise to full [FocusMusicPlayer.volume] after [FocusMusicPlayer.start]. */
private const val FADE_IN_MILLIS = 1_000L

/** How long the music takes to fade away after [FocusMusicPlayer.stop]. */
private const val FADE_OUT_MILLIS = 600L

/** Time between volume steps while fading. */
private const val FADE_STEP_MILLIS = 30L

/**
 * Plays an mp3 in res/raw (e.g. a FocusMusic track's) on a loop through a [MediaPlayer] until [stop],
 * at [volume] (0 to 1) of the media volume. It fades in on [start] and out on [stop], so moving
 * between two players (e.g. Background Music -> Focus Music) cross-fades.
 * Call [start] and [stop] on the main thread.
 */
class FocusMusicPlayer(
    private val context: Context,
    @RawRes private val audio: Int,
    private val volume: Float = 1f,
) {

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var playerVolume = 0f
    private var fadeIn: Runnable? = null

    fun start() {
        if (player != null) return
        // create() returns null if the file can't be played - then there's simply no music.
        val newPlayer = MediaPlayer.create(context, audio) ?: return
        newPlayer.isLooping = true
        newPlayer.setVolume(0f, 0f)
        newPlayer.start()
        player = newPlayer
        playerVolume = 0f
        fadeIn = fade(newPlayer, from = 0f, to = volume, FADE_IN_MILLIS, onStep = { playerVolume = it })
    }

    fun stop() {
        val oldPlayer = player ?: return
        player = null
        fadeIn?.let(handler::removeCallbacks)
        fadeIn = null
        // Fades from wherever the fade-in got to, then lets go of the player.
        fade(oldPlayer, from = playerVolume, to = 0f, FADE_OUT_MILLIS, onDone = oldPlayer::release)
    }

    /** Moves [target]'s volume from [from] to [to] over [durationMillis]; returns the step, to cancel it. */
    private fun fade(
        target: MediaPlayer,
        from: Float,
        to: Float,
        durationMillis: Long,
        onStep: (Float) -> Unit = {},
        onDone: () -> Unit = {},
    ): Runnable {
        val startTime = SystemClock.uptimeMillis()
        val step = object : Runnable {
            override fun run() {
                val progress = ((SystemClock.uptimeMillis() - startTime).toFloat() / durationMillis).coerceIn(0f, 1f)
                val current = from + (to - from) * progress
                target.setVolume(current, current)
                onStep(current)
                if (progress < 1f) handler.postDelayed(this, FADE_STEP_MILLIS) else onDone()
            }
        }
        handler.post(step)
        return step
    }
}
