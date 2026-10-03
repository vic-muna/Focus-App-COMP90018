package com.example.focusapp.data.wifi

import android.content.Context
import org.json.JSONArray

/**
 * David's older Wi-Fi setting (on/off plus tagged network names).
 * Now only read, so [SavedWifiStorage] can copy those networks into its list.
 */
class WifiTriggerStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    suspend fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    suspend fun getTaggedSsids(): List<String> {
        val json = prefs.getString(KEY_TAGGED_SSIDS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        }.getOrDefault(emptyList())
    }

    /** No-op if [ssid] is already tagged - the list is a set, not a log. */
    suspend fun addTaggedSsid(ssid: String) {
        val current = getTaggedSsids()
        if (ssid in current) return
        writeTaggedSsids(current + ssid)
    }

    suspend fun removeTaggedSsid(ssid: String) {
        writeTaggedSsids(getTaggedSsids() - ssid)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun writeTaggedSsids(ssids: List<String>) {
        prefs.edit().putString(KEY_TAGGED_SSIDS, JSONArray(ssids).toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "focus_wifi_trigger"
        const val KEY_ENABLED = "enabled"
        const val KEY_TAGGED_SSIDS = "tagged_ssids_json"
    }
}
