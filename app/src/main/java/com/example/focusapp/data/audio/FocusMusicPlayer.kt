package com.example.focusapp.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import com.example.focusapp.domain.model.FocusMusic
import kotlin.random.Random

/**
 * Plays a [FocusMusic] track on a loop until [stop]: its mp3 through a [MediaPlayer],
 * or - for a track without one - white noise generated on a background thread.
 */
class FocusMusicPlayer(private val context: Context, private val music: FocusMusic) {

    private var player: MediaPlayer? = null
    @Volatile private var noisePlaying = false

    fun start() {
        val audio = music.audio
        if (audio == null) {
            if (noisePlaying) return
            noisePlaying = true
            Thread(::playNoise, "FocusMusicNoise").apply { isDaemon = true }.start()
        } else if (player == null) {
            // create() returns null if the file can't be played - then there's simply no music.
            player = MediaPlayer.create(context, audio)?.apply {
                isLooping = true
                start()
            }
        }
    }

    fun stop() {
        noisePlaying = false
        player?.release()
        player = null
    }

    private fun playNoise() {
        val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuffer, CHUNK_SAMPLES * 2) * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val chunk = ShortArray(CHUNK_SAMPLES)
        try {
            track.play()
            while (noisePlaying) {
                for (i in chunk.indices) chunk[i] = (Random.nextDouble(-1.0, 1.0) * VOLUME * Short.MAX_VALUE).toInt().toShort()
                track.write(chunk, 0, chunk.size)
            }
        } finally {
            track.stop()
            track.release()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val CHUNK_SAMPLES = 2_048
        const val VOLUME = 0.3
    }
}
