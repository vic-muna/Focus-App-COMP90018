package com.example.focusapp.data.wifi

import android.content.Context
import org.json.JSONArray

/**
 * The Wi-Fi networks this phone has been seen on, newest first,
 * so the Wi-Fi tab can offer them (Android doesn't let apps read the saved-network list).
 * Also includes networks saved in the older [WifiTriggerStorage].
 */
class WifiHistoryStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val legacyTagged = WifiTriggerStorage(context)

    /** Every known network, newest first, without duplicates. */
    suspend fun getKnownSsids(): List<String> =
        (readSsids() + legacyTagged.getTaggedSsids()).distinct()

    /** Moves [ssid] to the front (adding it if new), keeping at most [MAX_SSIDS]. */
    fun remember(ssid: String) {
        val updated = (listOf(ssid) + readSsids().filter { it != ssid }).take(MAX_SSIDS)
        prefs.edit().putString(KEY_SSIDS, JSONArray(updated).toString()).apply()
    }

    private fun readSsids(): List<String> {
        val json = prefs.getString(KEY_SSIDS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS_NAME = "focus_wifi_history"
        const val KEY_SSIDS = "ssids_json"
        const val MAX_SSIDS = 30
    }
}