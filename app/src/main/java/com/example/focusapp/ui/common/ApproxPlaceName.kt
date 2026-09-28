package com.example.focusapp.ui.common

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Best-effort reverse geocoding: a short, human-readable name for the place
 * at (latitude, longitude) - e.g. a building or street name - or null when
 * the device has no geocoder, no network, or nothing matches.
 */
suspend fun resolveApproxPlaceName(context: Context, latitude: Double, longitude: Double): String? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale.getDefault())
    val address = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
    }.getOrNull() ?: return null

    // featureName is often just a street number - skip it when it is.
    val feature = address.featureName?.takeIf { name -> name.any { it.isLetter() } }
    return address.premises ?: feature ?: address.thoroughfare ?: address.locality
}
