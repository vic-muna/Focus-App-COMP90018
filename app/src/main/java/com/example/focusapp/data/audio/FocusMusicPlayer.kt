package com.example.focusapp.data.audio

import android.content.Context
import android.media.MediaPlayer
import com.example.focusapp.domain.model.FocusMusic

/** Plays a [FocusMusic] track's mp3 on a loop through a [MediaPlayer] until [stop]. */
class FocusMusicPlayer(private val context: Context, private val music: FocusMusic) {

    private var player: MediaPlayer? = null

    fun start() {
        if (player != null) return
        // create() returns null if the file can't be played - then there's simply no music.
        player = MediaPlayer.create(context, music.audio)?.apply {
            isLooping = true
            start()
        }
    }

    fun stop() {
        player?.release()
        player = null
    }
}
