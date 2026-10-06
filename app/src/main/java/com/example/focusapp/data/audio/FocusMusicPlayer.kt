package com.example.focusapp.data.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes

/**
 * Plays an mp3 in res/raw (e.g. a FocusMusic track's) on a loop through a [MediaPlayer] until [stop],
 * at [volume] (0 to 1) of the media volume.
 */
class FocusMusicPlayer(
    private val context: Context,
    @RawRes private val audio: Int,
    private val volume: Float = 1f,
) {

    private var player: MediaPlayer? = null

    fun start() {
        if (player != null) return
        // create() returns null if the file can't be played - then there's simply no music.
        player = MediaPlayer.create(context, audio)?.apply {
            isLooping = true
            setVolume(volume, volume)
            start()
        }
    }

    fun stop() {
        player?.release()
        player = null
    }
}
