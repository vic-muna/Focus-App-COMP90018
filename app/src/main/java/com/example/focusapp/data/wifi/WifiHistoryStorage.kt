package com.example.focusapp.data.wifi

import android.content.Context
import org.json.JSONArray

/**
 * WifiHistoryStorage
 * ---------------------
 * The Wi-Fi networks this app has seen the phone connected to, newest
 * first, so the Wi-Fi tab can offer them when adding a network. Android
 * doesn't let normal apps read the phone's own saved-network list (since
 * Android 10), so this is the app's own record: [remember] is called
 * whenever a check finds the current network (Home's periodic check, and
 * the Wi-Fi tab's "current Wi-Fi" check).
 *
 * Also includes the networks tagged in David's older Wi-Fi Source screen
 * ([WifiTriggerStorage]), so they can still be picked after the switch to
 * the Wi-Fi tab.
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