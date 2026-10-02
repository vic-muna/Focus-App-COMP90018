package com.example.focusapp.data.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.os.Build
import java.util.concurrent.ConcurrentHashMap

/**
 * Follows which Wi-Fi the phone is on, for as long as it runs (between
 * [start] and [stop]) - Android tells it when the phone joins or leaves a
 * Wi-Fi, so nothing is polled. [onChange] is called with the new [currentSsid]
 * (null = not on Wi-Fi, or the name can't be read), on a background thread.
 *
 * Like [checkCurrentWifi], reading the name needs precise location and the
 * phone's Location setting on; when Focus isn't on screen it also needs
 * location "Allow all the time".
 */
class WifiWatcher(
    context: Context,
    private val onChange: (String?) -> Unit,
) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Each connected Wi-Fi network and its name ("" while the name can't be read).
    private val networks = ConcurrentHashMap<Network, String>()

    @Volatile
    var currentSsid: String? = null
        private set

    private val callback =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Without this flag Android 12+ hides the name from the callback.
            object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
                    onNetworkChanged(network, capabilities)
                override fun onLost(network: Network) = onNetworkLost(network)
            }
        } else {
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
                    onNetworkChanged(network, capabilities)
                override fun onLost(network: Network) = onNetworkLost(network)
            }
        }

    fun start() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
    }

    fun stop() {
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        networks.clear()
        currentSsid = null
    }

    /**
     * Reads the name again if the phone is on Wi-Fi but the name couldn't be
     * read before (e.g. location permission was only just given).
     */
    fun retryIfUnknown() {
        if (currentSsid != null || networks.isEmpty()) return
        val ssid = readSsidFromWifiManager(appContext) ?: return
        networks.keys.forEach { networks[it] = ssid }
        publish()
    }

    private fun onNetworkChanged(network: Network, capabilities: NetworkCapabilities) {
        val ssid = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (capabilities.transportInfo as? WifiInfo)?.ssid?.toValidSsid()
        } else null) ?: readSsidFromWifiManager(appContext)
        networks[network] = ssid.orEmpty()
        publish()
    }

    private fun onNetworkLost(network: Network) {
        networks.remove(network)
        publish()
    }

    @Synchronized
    private fun publish() {
        val ssid = networks.values.firstOrNull { it.isNotEmpty() }
        if (ssid == currentSsid) return
        currentSsid = ssid
        onChange(ssid)
    }
}
