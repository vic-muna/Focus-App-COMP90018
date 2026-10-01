package com.example.focusapp.ui.screens.location.map

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

internal class MapJavaScriptInterface(
    private val onMapClickCallback: (lat: Double, lng: Double) -> Unit,
    private val onZoneClickCallback: ((zoneId: String) -> Unit)? = null,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Suppress("unused")
    @JavascriptInterface
    fun onMapClick(lat: Double, lng: Double) {
        mainHandler.post {
            onMapClickCallback(lat, lng)
        }
    }

    @Suppress("unused")
    @JavascriptInterface
    fun onZoneClick(zoneId: String) {
        mainHandler.post {
            onZoneClickCallback?.invoke(zoneId)
        }
    }
}
