package com.example.focusapp.data.wifi

import android.content.Context
import org.json.JSONArray

/**
 * The user's saved Wi-Fi list, in the order they were saved. A network is
 * only added when the user taps the connected one on the Wi-Fi tab, and
 * removed when they tap it in the list (Android doesn't let apps read the
 * phone's own saved-network list).
 * Networks tagged in the older [WifiTriggerStorage] are copied in once.
 */
class SavedWifiStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val legacyTagged = WifiTriggerStorage(context)

    suspend fun getSavedSsids(): List<String> {
        if (!prefs.getBoolean(KEY_LEGACY_COPIED, false)) {
            write((readSsids() + legacyTagged.getTaggedSsids()).distinct())
            prefs.edit().putBoolean(KEY_LEGACY_COPIED, true).apply()
        }
        return readSsids()
    }

    /** Adds [ssid] to the end of the list; no-op if it's already saved. */
    fun add(ssid: String) {
        val current = readSsids()
        if (ssid !in current) write(current + ssid)
    }

    fun remove(ssid: String) {
        write(readSsids() - ssid)
    }

    /** Deletes the saved list, including the old [WifiTriggerStorage] networks it copies in
     *  (used when a different user signs in on this phone). */
    fun clear() {
        legacyTagged.clear()
        prefs.edit().clear().apply()
    }

    private fun readSsids(): List<String> {
        val json = prefs.getString(KEY_SSIDS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        }.getOrDefault(emptyList())
    }

    private fun write(ssids: List<String>) {
        prefs.edit().putString(KEY_SSIDS, JSONArray(ssids).toString()).apply()
    }

    private companion object {
        // Same file as the old auto-filled history, so networks already there stay listed.
        const val PREFS_NAME = "focus_wifi_history"
        const val KEY_SSIDS = "ssids_json"
        const val KEY_LEGACY_COPIED = "legacy_tagged_copied"
    }
}
