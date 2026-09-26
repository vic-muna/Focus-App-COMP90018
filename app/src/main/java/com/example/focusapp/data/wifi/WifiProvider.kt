package com.example.focusapp.data.wifi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * WifiProvider
 * --------------
 * Phase 1 of the planned Wi-Fi-source trigger (see the location-zone
 * geofencing trigger for the eventual shape this is expected to follow):
 * just answering "what Wi-Fi network is the device connected to right now".
 * No blocking/trigger logic lives here yet.
 */

/** Outcome of [checkCurrentWifi] - says WHY there's no network name, not just that there isn't one. */
sealed class WifiCheckResult {
    data class Connected(val ssid: String) : WifiCheckResult()
    data object NotOnWifi : WifiCheckResult()

    /** On Wi-Fi, but only "approximate" (coarse) location was granted - the name needs precise (fine). */
    data object NeedsPreciseLocation : WifiCheckResult()

    /** On Wi-Fi, but the device-wide Location toggle is off - Android hides the name then too. */
    data object LocationServicesOff : WifiCheckResult()

    /** On Wi-Fi with everything granted, but Android still didn't give a name. */
    data object NameUnavailable : WifiCheckResult()
}

/**
 * Whether the app holds the permission needed to read a real SSID.
 * [David Shiau, 2026-09-26] FINE location specifically: since Android 10
 * the SSID needs ACCESS_FINE_LOCATION, and COARSE alone (what the user gets
 * by picking "Approximate" in the Android 12+ permission dialog) returns
 * the "<unknown ssid>" placeholder. This used to accept coarse too, which
 * hid that case.
 */
fun hasLocationPermissionForWifi(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * getCurrentWifiSsid
 * ---------------------
 * The SSID (network name) of the Wi-Fi network the device is connected to,
 * or null for any reason it can't be read - see [checkCurrentWifi] for the
 * reason.
 */
suspend fun getCurrentWifiSsid(context: Context): String? =
    (checkCurrentWifi(context) as? WifiCheckResult.Connected)?.ssid

/**
 * checkCurrentWifi
 * ---------------------
 * [David Shiau, 2026-09-26] Reads the current Wi-Fi network name. Android
 * returns the literal placeholder "<unknown ssid>" (not an error) whenever
 * it withholds the name, for any of these reasons - each is checked
 * separately so the caller can say which one it was:
 *  1. No precise (fine) location permission - see [hasLocationPermissionForWifi].
 *  2. The device's Location setting is off.
 *  3. On Android 12+ (API 31+), `getNetworkCapabilities(activeNetwork)`
 *     ALWAYS redacts the SSID, even with permission. The name is only
 *     included when read from a NetworkCallback registered with
 *     FLAG_INCLUDE_LOCATION_INFO. This was the original bug: this function
 *     used to read getNetworkCapabilities directly, so on Android 12+ it
 *     never found a name.
 * The old WifiManager.connectionInfo (deprecated in API 31, still working)
 * is kept as a fallback on every API level.
 */
suspend fun checkCurrentWifi(context: Context): WifiCheckResult {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return WifiCheckResult.NotOnWifi
    val capabilities = connectivityManager.getNetworkCapabilities(network)
        ?: return WifiCheckResult.NotOnWifi
    if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return WifiCheckResult.NotOnWifi

    if (!hasLocationPermissionForWifi(context)) return WifiCheckResult.NeedsPreciseLocation
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!LocationManagerCompat.isLocationEnabled(locationManager)) return WifiCheckResult.LocationServicesOff

    val ssid = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> readSsidWithLocationInfo(connectivityManager)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> (capabilities.transportInfo as? WifiInfo)?.ssid?.toValidSsid()
        else -> null
    } ?: readSsidFromWifiManager(context)

    return ssid?.let { WifiCheckResult.Connected(it) } ?: WifiCheckResult.NameUnavailable
}

/**
 * Android 12+ path: a NetworkCallback with FLAG_INCLUDE_LOCATION_INFO is the
 * only non-deprecated way to get an un-redacted WifiInfo. Registering it
 * immediately reports the currently-connected Wi-Fi network's capabilities,
 * so this just waits for that first report (briefly - null on timeout).
 */
@RequiresApi(Build.VERSION_CODES.S)
private suspend fun readSsidWithLocationInfo(connectivityManager: ConnectivityManager): String? {
    val ssid = CompletableDeferred<String>()
    val callback = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            (capabilities.transportInfo as? WifiInfo)?.ssid?.toValidSsid()?.let { ssid.complete(it) }
        }
    }
    val request = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()
    connectivityManager.registerNetworkCallback(request, callback)
    return try {
        withTimeoutOrNull(WIFI_CALLBACK_TIMEOUT_MILLIS) { ssid.await() }
    } finally {
        connectivityManager.unregisterNetworkCallback(callback)
    }
}

@Suppress("DEPRECATION")
private fun readSsidFromWifiManager(context: Context): String? =
    (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
        .connectionInfo?.ssid?.toValidSsid()

/**
 * Null for the "<unknown ssid>" placeholder / blank. A visible SSID comes
 * back wrapped in literal double quotes; a hidden network's comes back as an
 * unquoted hex string - only strip quotes when they're actually there.
 */
private fun String.toValidSsid(): String? {
    if (isBlank() || this == WifiManager.UNKNOWN_SSID) return null
    return removeSurrounding("\"")
}

private const val WIFI_CALLBACK_TIMEOUT_MILLIS = 2_000L
