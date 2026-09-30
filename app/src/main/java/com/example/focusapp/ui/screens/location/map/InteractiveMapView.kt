package com.example.focusapp.ui.screens.location.map

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val APP_USER_AGENT = "FocusApp/1.0 (com.example.focusapp; contact@example.com)"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveMapView(
    currentLocation: Pair<Double, Double>?,
    pinLocation: Pair<Double, Double>?,
    pinRadiusMeters: Float,
    onLocationClick: ((latitude: Double, longitude: Double) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val centerLat = currentLocation?.first ?: pinLocation?.first ?: -37.8136
    val centerLng = currentLocation?.second ?: pinLocation?.second ?: 144.9631
    val htmlContent = remember(centerLat, centerLng) { buildMapHtml(centerLat, centerLng) }

    var isPageLoaded by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    userAgentString = APP_USER_AGENT
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isPageLoaded = true
                        currentLocation?.let { (lat, lng) ->
                            view?.evaluateJavascript("setLocation($lat, $lng);", null)
                        }
                        if (pinLocation != null) {
                            view?.evaluateJavascript("setPin(${pinLocation.first}, ${pinLocation.second}, $pinRadiusMeters);", null)
                        }
                    }
                }
                addJavascriptInterface(
                    MapJavaScriptInterface { lat, lng ->
                        onLocationClick?.invoke(lat, lng)
                    },
                    "AndroidBridge"
                )
                loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { view ->
            if (isPageLoaded) {
                currentLocation?.let { (lat, lng) ->
                    view.evaluateJavascript("setLocation($lat, $lng);", null)
                }
                if (pinLocation != null) {
                    view.evaluateJavascript("setPin(${pinLocation.first}, ${pinLocation.second}, $pinRadiusMeters);", null)
                } else {
                    view.evaluateJavascript("removePin();", null)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
