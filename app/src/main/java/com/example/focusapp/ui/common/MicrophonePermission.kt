package com.example.focusapp.ui.common

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Holds whether the app has microphone permission, and exposes [request] to
 * launch the system permission dialog. Use with [rememberMicrophonePermissionState].
 */
class MicrophonePermissionState internal constructor(initialGranted: Boolean) {
    var hasPermission by mutableStateOf(initialGranted)
        internal set

    internal lateinit var launcher: ActivityResultLauncher<String>

    fun request() {
        launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

@Composable
fun rememberMicrophonePermissionState(): MicrophonePermissionState {
    val context = LocalContext.current
    val state = remember {
        MicrophonePermissionState(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    state.launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> state.hasPermission = granted }

    return state
}
