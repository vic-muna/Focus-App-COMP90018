package com.example.focusapp.ui.screens.history

import android.media.MediaPlayer
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.delay

/** How long a preview lasts. */
private const val PREVIEW_MILLIS = 15_000

/** Where the preview starts, as a share of the track - past the intro, into the main part. */
private const val PREVIEW_START_FRACTION = 0.3f

/**
 * A short preview of the raw mp3 [audio]: a 15-second clip from the middle of the track,
 * with play / pause and a slider to drag anywhere within the clip. Starts playing right
 * away, stops at the end of the clip, and pauses when the app goes to the background.
 */
@Composable
fun MusicPreview(@RawRes audio: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // create() returns null if the file can't be played - then the preview just doesn't show.
    val player = remember(audio) { MediaPlayer.create(context, audio) } ?: return
    val clipStart = remember(player) {
        ((player.duration - PREVIEW_MILLIS) * PREVIEW_START_FRACTION).toInt().coerceAtLeast(0)
    }
    val clipLength = remember(player) { minOf(PREVIEW_MILLIS, player.duration - clipStart).coerceAtLeast(1) }

    var isPlaying by remember { mutableStateOf(false) }
    // Milliseconds into the clip. While dragging it follows the finger, not the player.
    var position by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    fun play() {
        player.start()
        isPlaying = true
    }

    fun pause() {
        player.pause()
        isPlaying = false
    }

    DisposableEffect(player) {
        player.seekTo(clipStart)
        play()
        onDispose { player.release() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && isPlaying) pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Moves the slider along, and stops (back at the start) at the end of the clip.
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val inClip = player.currentPosition - clipStart
            if (inClip >= clipLength) {
                pause()
                player.seekTo(clipStart)
                position = 0f
            } else if (!isDragging) {
                position = inClip.coerceAtLeast(0).toFloat()
            }
            delay(100)
        }
    }

    val colors = FocusTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { if (isPlaying) pause() else play() }, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                tint = colors.accent,
            )
        }
        Slider(
            value = position,
            onValueChange = {
                isDragging = true
                position = it
            },
            onValueChangeFinished = {
                player.seekTo(clipStart + position.toInt())
                isDragging = false
            },
            valueRange = 0f..clipLength.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.surfaceSunken,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        )
        Text(
            text = formatSeconds(position.toInt()) + " / " + formatSeconds(clipLength),
            style = FocusTheme.typography.caption,
            color = colors.onSurfaceMuted,
        )
    }
}

/** "0:07". */
private fun formatSeconds(millis: Int): String {
    val seconds = millis / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
