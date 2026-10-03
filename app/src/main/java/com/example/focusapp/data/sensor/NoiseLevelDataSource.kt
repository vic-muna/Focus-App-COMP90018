package com.example.focusapp.data.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.log10
import kotlin.math.sqrt

// 44.1 kHz mono 16-bit is the one format every Android device must support.
private const val SAMPLE_RATE = 44_100
/** One reading per chunk - see NoiseLevelTracker.READING_INTERVAL_MILLIS. */
private const val CHUNK_MILLIS = 500
private const val CHUNK_SAMPLES = SAMPLE_RATE * CHUNK_MILLIS / 1000

// Phone mics aren't calibrated, so this is a rough shift from dBFS (0 = loudest the
// mic can capture) to everyday dB SPL. Readings are approximate and vary by device.
private const val DBFS_TO_SPL_OFFSET = 90.0

/**
 * The room's loudness for the Focus Mode screen, measured through the microphone.
 * Audio is only measured, never saved.
 */
class NoiseLevelDataSource(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * An approximate sound level in dB, about every 500 ms, while collected.
     * The mic opens when collection starts and closes when it's cancelled.
     * Throws [IllegalStateException] if the mic can't be opened.
     * Note: Ensure RECORD_AUDIO is granted before collecting this.
     */
    fun decibelFlow(): Flow<Double> = flow {
        val recorder = openRecorder() ?: throw IllegalStateException("Microphone unavailable")

        val buffer = ShortArray(CHUNK_SAMPLES)
        try {
            recorder.startRecording()
            while (currentCoroutineContext().isActive) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) emit(toDecibels(buffer, read))
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * UNPROCESSED skips the mic's automatic gain control, so loud and quiet rooms read
     * differently. Phones without it fall back to VOICE_RECOGNITION, which has the least
     * processing of the always-available sources. Null if neither opens.
     */
    private fun openRecorder(): AudioRecord? {
        val supportsUnprocessed =
            audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        if (supportsUnprocessed) {
            createRecorder(MediaRecorder.AudioSource.UNPROCESSED)?.let { return it }
        }
        return createRecorder(MediaRecorder.AudioSource.VOICE_RECOGNITION)
    }

    /** A recorder on [audioSource], or null if the phone can't open it. */
    @SuppressLint("MissingPermission")
    private fun createRecorder(audioSource: Int): AudioRecord? {
        val minBufferBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val recorder = AudioRecord(
            audioSource,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferBytes, CHUNK_SAMPLES * 2)
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return null
        }
        return recorder
    }

    /** Root-mean-square loudness of the samples, as approximate dB SPL (never below 0). */
    private fun toDecibels(samples: ShortArray, count: Int): Double {
        var sumOfSquares = 0.0
        for (i in 0 until count) {
            val sample = samples[i].toDouble()
            sumOfSquares += sample * sample
        }
        val rms = sqrt(sumOfSquares / count)
        if (rms < 1.0) return 0.0
        val dbfs = 20 * log10(rms / Short.MAX_VALUE)
        return (dbfs + DBFS_TO_SPL_OFFSET).coerceAtLeast(0.0)
    }
}
