package com.example.focusapp.data.wifi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * WifiProvider
 * --------------
 * Phase 1 of the planned Wi-Fi-source trigger (see the location-zone
 * geofencing trigger for the eventual shape this is expected to follow):
 * just answering "what Wi-Fi network is the device connected to right now".
 * No blocking/trigger logic lives here yet.
 */

/**
 * Whether the app currently holds the location permission needed to read a
 * real SSID (see [getCurrentWifiSsid]'s doc comment for why).
 */
fun hasLocationPermissionForWifi(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
}

/**
 * getCurrentWifiSsid
 * ---------------------
 * Returns the SSID (network name) of the Wi-Fi network the device is
 * currently connected to, or null if it's not on Wi-Fi at all, or the name
 * can't be resolved.
 *
 * WHY THIS NEEDS LOCATION PERMISSION: since Android 8.1 (API 27), the OS
 * treats "which Wi-Fi network you're near" as location-sensitive data -
 * without [hasLocationPermissionForWifi], the SSID comes back as the
 * literal placeholder string "<unknown ssid>" instead of throwing, so
 * callers should check that first rather than just checking for null here.
 *
 * Uses the modern NetworkCapabilities/transportInfo API on Android 10+
 * (API 29+, when it was introduced), falling back to the older, now-
 * deprecated WifiManager.connectionInfo on 26-28 so this still works all
 * the way back to minSdk.
 */
fun getCurrentWifiSsid(context: Context): String? {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return null
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null
    if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null

    val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        capabilities.transportInfo as? WifiInfo
    } else {
        @Suppress("DEPRECATION")
        (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo
    }

    val rawSsid = wifiInfo?.ssid ?: return null
    if (rawSsid.isBlank() || rawSsid == WifiManager.UNKNOWN_SSID) return null
    // A visible SSID comes back wrapped in literal double quotes; a hidden
    // network's comes back as an unquoted hex string - only strip quotes
    // when they're actually there.
    return rawSsid.removeSurrounding("\"")
}
