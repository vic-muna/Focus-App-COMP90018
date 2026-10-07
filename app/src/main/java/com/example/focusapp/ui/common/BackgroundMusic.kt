package com.example.focusapp.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.focusapp.R
import com.example.focusapp.data.audio.FocusMusicPlayer
import kotlinx.coroutines.awaitCancellation

/** Background Music plays quieter than Focus Music - it's only a backdrop. */
private const val BACKGROUND_MUSIC_VOLUME = 0.3f

/**
 * Background Music (res/raw/homemusic): loops while [play] is true and the app is in the
 * foreground; fades out when [play] turns false or the app goes to the background.
 * Used by NavGraph (its screens) and MainActivity (the login screen). The on/off switch is
 * Settings' "Background Music" (FocusMusicStorage.isBackgroundEnabled).
 */
@Composable
fun BackgroundMusic(play: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(play, lifecycleOwner) {
        if (!play) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val player = FocusMusicPlayer(context, R.raw.homemusic, BACKGROUND_MUSIC_VOLUME)
            try {
                player.start()
                awaitCancellation()
            } finally {
                player.stop()
            }
        }
    }
}
