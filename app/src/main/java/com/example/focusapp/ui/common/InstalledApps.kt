package com.example.focusapp.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.focusapp.data.apps.InstalledAppInfo
import com.example.focusapp.data.apps.getLaunchableApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The phone's installed apps, or null while they are still loading.
 * They are loaded once, the first time [shouldLoad] is true (it's slow with many apps).
 */
@Composable
fun rememberInstalledApps(shouldLoad: Boolean): List<InstalledAppInfo>? {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledAppInfo>?>(null) }
    LaunchedEffect(shouldLoad) {
        if (shouldLoad && apps == null) {
            apps = withContext(Dispatchers.Default) { getLaunchableApps(context) }
        }
    }
    return apps
}
