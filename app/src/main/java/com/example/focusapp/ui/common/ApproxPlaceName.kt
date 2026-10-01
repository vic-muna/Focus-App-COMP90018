package com.example.focusapp.ui.common

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Best-effort reverse geocoding: resolves human-readable place name for (latitude, longitude).
 * Uses Android's built-in Geocoder with a direct OpenStreetMap Nominatim HTTP fallback
 * to guarantee location resolution on Emulators and non-Play-Services devices.
 */
suspend fun resolveApproxPlaceName(context: Context, latitude: Double, longitude: Double): String? {
    // 1. Try Android's built-in system Geocoder
    val systemResult = runCatching {
        if (!Geocoder.isPresent()) null
        else {
            val geocoder = Geocoder(context, Locale.getDefault())
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(null)
                        }
                    })
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                }
            }
            address?.let { extractPlaceFromAddress(it) }
        }
    }.getOrNull()

    if (!systemResult.isNullOrBlank()) {
        return systemResult
    }

    // 2. Direct OpenStreetMap Nominatim reverse geocoding fallback (works reliably on Emulators & all devices)
    return fetchNominatimPlaceName(latitude, longitude)
}

private fun extractPlaceFromAddress(address: Address): String? {
    val feature = address.featureName?.takeIf { name -> name.any { it.isLetter() } }
    val street = address.thoroughfare
    val suburb = address.subLocality ?: address.locality
    val fullAddress = if (address.maxAddressLineIndex >= 0) address.getAddressLine(0) else null

    return (address.premises ?: feature ?: street ?: suburb ?: fullAddress)?.trim()
}

private suspend fun fetchNominatimPlaceName(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
    runCatching {
        val url = URL(
            String.format(Locale.US, "https://nominatim.openstreetmap.org/reverse?format=json&lat=%.6f&lon=%.6f&zoom=18&addressdetails=1", latitude, longitude)
        )
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "FocusApp/1.0 (com.example.focusapp; contact@example.com)")
        connection.connectTimeout = 4000
        connection.readTimeout = 4000
        if (connection.responseCode != 200) return@withContext null

        val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(jsonStr)
        val address = json.optJSONObject("address")

        val amenity = address?.optString("amenity")?.takeIf { it.isNotBlank() }
        val building = address?.optString("building")?.takeIf { it.isNotBlank() }
        val shop = address?.optString("shop")?.takeIf { it.isNotBlank() }
        val road = address?.optString("road")?.takeIf { it.isNotBlank() }
        val suburb = address?.optString("suburb")?.takeIf { it.isNotBlank() }
            ?: address?.optString("city")?.takeIf { it.isNotBlank() }
            ?: address?.optString("town")?.takeIf { it.isNotBlank() }

        val place = amenity ?: building ?: shop ?: road ?: suburb ?: json.optString("display_name").split(",").firstOrNull()
        place?.trim()
    }.getOrNull()
}
