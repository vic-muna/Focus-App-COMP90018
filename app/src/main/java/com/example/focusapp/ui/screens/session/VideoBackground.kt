package com.example.focusapp.ui.screens.session

import android.media.AudioManager
import android.net.Uri
import android.widget.VideoView
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView

/** The background videos are portrait 1080 x 1920. */
private const val VIDEO_ASPECT = 1080f / 1920f

/**
 * Plays the raw video [video] silently on a loop, cropped to fill the space like
 * ContentScale.Crop (VideoView alone would letterbox it).
 */
@Composable
fun VideoBackground(@RawRes video: Int, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        // Grow the video until it covers both dimensions; the overflow is clipped.
        val width = maxOf(maxWidth, maxHeight * VIDEO_ASPECT)
        val height = maxOf(maxHeight, maxWidth / VIDEO_ASPECT)
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    // Don't pause other apps' audio (or Focus Music) - the videos have no sound.
                    setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
                    setOnPreparedListener { player ->
                        player.isLooping = true
                        player.setVolume(0f, 0f)
                    }
                    setVideoURI(Uri.parse("android.resource://${context.packageName}/$video"))
                    start()
                }
            },
            onRelease = { it.stopPlayback() },
            modifier = Modifier.requiredSize(width, height),
        )
    }
}
