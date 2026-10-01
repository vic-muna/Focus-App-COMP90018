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
import com.example.focusapp.domain.model.FocusZone
import org.json.JSONArray
import org.json.JSONObject

private const val APP_USER_AGENT = "FocusApp/1.0 (com.example.focusapp; contact@example.com)"

private fun zonesToJson(zones: List<FocusZone>): String {
    val array = JSONArray()
    zones.forEach { z ->
        array.put(
            JSONObject().apply {
                put("id", z.id)
                put("name", z.name)
                put("latitude", z.latitude)
                put("longitude", z.longitude)
                put("radiusMeters", z.radiusMeters)
            }
        )
    }
    return array.toString()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveMapView(
    currentLocation: Pair<Double, Double>?,
    pinLocation: Pair<Double, Double>?,
    pinRadiusMeters: Float,
    zones: List<FocusZone> = emptyList(),
    onLocationClick: ((latitude: Double, longitude: Double) -> Unit)?,
    onZoneClick: ((zoneId: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val centerLat = currentLocation?.first ?: pinLocation?.first ?: zones.firstOrNull()?.latitude ?: -37.8136
    val centerLng = currentLocation?.second ?: pinLocation?.second ?: zones.firstOrNull()?.longitude ?: 144.9631
    val htmlContent = remember(centerLat, centerLng) { buildMapHtml(centerLat, centerLng) }

    val zonesJson = remember(zones) { zonesToJson(zones) }

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
                        view?.evaluateJavascript("setZones($zonesJson);", null)
                    }
                }
                addJavascriptInterface(
                    MapJavaScriptInterface(
                        onMapClickCallback = { lat, lng -> onLocationClick?.invoke(lat, lng) },
                        onZoneClickCallback = { zoneId -> onZoneClick?.invoke(zoneId) }
                    ),
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
                view.evaluateJavascript("setZones($zonesJson);", null)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
