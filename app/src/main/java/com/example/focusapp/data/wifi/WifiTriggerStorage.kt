package com.example.focusapp.data.wifi

import android.content.Context
import org.json.JSONArray

/**
 * WifiTriggerStorage
 * ---------------------
 * Real persistence (SharedPreferences + JSON, same approach used elsewhere
 * - see LocalDataSource/BlockedAppGroupStorage) for the Wi-Fi-source
 * trigger: whether it's turned on, and which SSIDs the user has tagged as
 * "start a focus session when connected to this network".
 *
 * Phase 2 of the planned Wi-Fi-source trigger (see WifiProvider.kt for
 * phase 1, just reading the current SSID). No auto-detection of *which*
 * organization a network belongs to - see the design discussion this was
 * built from - the user tags networks themselves, same as a Focus Zone.
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

    private fun writeTaggedSsids(ssids: List<String>) {
        prefs.edit().putString(KEY_TAGGED_SSIDS, JSONArray(ssids).toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "focus_wifi_trigger"
        const val KEY_ENABLED = "enabled"
        const val KEY_TAGGED_SSIDS = "tagged_ssids_json"
    }
}
