package com.example.focusapp.ui.screens.session

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.focusapp.data.preferences.NoiseAlertStorage
import com.example.focusapp.data.sensor.NoiseLevelDataSource
import com.example.focusapp.domain.usecase.NoiseLevel
import com.example.focusapp.domain.usecase.NoiseLevelTracker
import com.example.focusapp.ui.common.rememberMicrophonePermissionState
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.flow.catch

/**
 * The noise alert under the focus timer: a small dB readout, and a "too loud"
 * banner while the room's average level is above the Settings threshold.
 * Shows nothing when the alert is off in Settings. The microphone is only
 * open while this is on screen.
 */
@Composable
fun NoiseAlert(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings = remember { NoiseAlertStorage(context) }
    val isOn = remember { settings.isEnabled() }
    if (!isOn) return

    val micPermission = rememberMicrophonePermissionState()
    var noiseLevel by remember { mutableStateOf<NoiseLevel?>(null) }
    var isMicUnavailable by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!micPermission.hasPermission) micPermission.request()
    }
    LaunchedEffect(micPermission.hasPermission, lifecycleOwner) {
        if (!micPermission.hasPermission) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Start fresh each time the screen comes back, so old readings don't count.
            noiseLevel = null
            val tracker = NoiseLevelTracker(settings.getThresholdDb())
            NoiseLevelDataSource(context).decibelFlow()
                .catch { isMicUnavailable = true }
                .collect { noiseLevel = tracker.onReading(it, SystemClock.elapsedRealtime()) }
        }
    }

    NoiseAlertContent(
        label = when {
            !micPermission.hasPermission -> "Mic off · tap to allow"
            isMicUnavailable -> "Mic unavailable"
            else -> readoutLabel(noiseLevel)
        },
        isTooLoud = noiseLevel?.isTooLoud == true,
        // Tapping the label asks again while the microphone isn't allowed.
        onLabelClick = if (micPermission.hasPermission) null else micPermission::request,
        modifier = modifier,
    )
}

// TODO(noise): the raw/average readout is for tuning only - remove once settled.
private fun readoutLabel(level: NoiseLevel?): String {
    val raw = level?.rawDb?.let { "%.0f".format(it) } ?: "--"
    val average = level?.averageDb?.let { "%.0f".format(it) } ?: "--"
    return "Raw ~$raw dB · Avg ~$average dB"
}

/** Stateless layout of [NoiseAlert]. */
@Composable
private fun NoiseAlertContent(
    label: String,
    isTooLoud: Boolean,
    onLabelClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = FocusTheme.typography.body,
            color = FocusTheme.colors.sessionTimer,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable(enabled = onLabelClick != null) { onLabelClick?.invoke() },
        )
        AnimatedVisibility(
            visible = isTooLoud,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            TooLoudBanner()
        }
    }
}

@Composable
private fun TooLoudBanner() {
    val colors = FocusTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(colors.notification, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            tint = colors.pure,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "It's too loud for studying",
            style = FocusTheme.typography.body,
            color = colors.pure,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Preview
@Composable
private fun NoiseAlertTooLoudPreview() {
    FocusAppTheme {
        NoiseAlertContent(
            label = "Raw ~62 dB · Avg ~58 dB",
            isTooLoud = true,
            onLabelClick = null,
            modifier = Modifier
                .background(FocusTheme.colors.background)
                .padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun NoiseAlertMicOffPreview() {
    FocusAppTheme {
        NoiseAlertContent(
            label = "Mic off · tap to allow",
            isTooLoud = false,
            onLabelClick = {},
            modifier = Modifier
                .background(FocusTheme.colors.background)
                .padding(16.dp),
        )
    }
}
