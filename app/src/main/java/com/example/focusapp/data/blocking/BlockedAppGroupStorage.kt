package com.example.focusapp.data.blocking

import android.content.Context
import com.example.focusapp.data.apps.getAppIcon
import org.json.JSONArray
import org.json.JSONObject

/**
 * Saves a list of [BlockedAppGroup]s on the phone (SharedPreferences, as JSON).
 * Each feature keeps its own list in its own file:
 *  - Time Focus: the default file
 *  - Location: [forLocationGroups]
 *  - Wi-Fi: [forWifiNetworks]
 *  - Quick Focus: [forQuickFocus] (one group: the apps Quick Focus blocks)
 * App icons are not saved; they are loaded again from the package name.
 */
class BlockedAppGroupStorage(
    context: Context,
    prefsName: String = PREFS_NAME
) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    /** The saved groups, or null if nothing was saved yet. */
    suspend fun getGroups(): List<BlockedAppGroup>? = readGroups(loadIcons = true)

    /** Same as [getGroups] but without icons - faster, for the background blocker. */
    fun getGroupsWithoutIcons(): List<BlockedAppGroup>? = readGroups(loadIcons = false)

    private fun readGroups(loadIcons: Boolean): List<BlockedAppGroup>? {
        val json = prefs.getString(KEY_GROUPS, null) ?: return null
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index -> groupFromJson(array.getJSONObject(index), loadIcons) }
        }.getOrNull()
    }

    suspend fun saveGroups(groups: List<BlockedAppGroup>) {
        val array = JSONArray()
        groups.forEach { array.put(groupToJson(it)) }
        prefs.edit().putString(KEY_GROUPS, array.toString()).apply()
    }

    private fun groupToJson(group: BlockedAppGroup): JSONObject = JSONObject().apply {
        put("id", group.id)
        put("name", group.name)
        put("apps", JSONArray().apply {
            group.apps.forEach { app ->
                put(
                    JSONObject().apply {
                        put("packageName", app.packageName)
                        put("name", app.name)
                        put("isBlocked", app.isBlocked)
                    }
                )
            }
        })
        put(
            "schedule",
            JSONObject().apply {
                put("activeDays", JSONArray(group.schedule.activeDays.toList()))
                put("startHour", group.schedule.start.hour)
                put("startMinute", group.schedule.start.minute)
                put("endHour", group.schedule.end.hour)
                put("endMinute", group.schedule.end.minute)
            }
        )
        // Omitted (not stored as JSON null) when there's no limit.
        group.maxOpensPerApp?.let { put("maxOpensPerApp", it) }
        group.maxMinutesPerApp?.let { put("maxMinutesPerApp", it) }
        put("enabled", group.enabled)
    }

    private fun groupFromJson(obj: JSONObject, loadIcons: Boolean): BlockedAppGroup {
        val appsJson = obj.getJSONArray("apps")
        val apps = (0 until appsJson.length()).map { index ->
            val appObj = appsJson.getJSONObject(index)
            val packageName = appObj.getString("packageName")
            AppItem(
                packageName = packageName,
                name = appObj.getString("name"),
                isBlocked = appObj.getBoolean("isBlocked"),
                icon = if (loadIcons) getAppIcon(appContext, packageName) else null
            )
        }
        val scheduleObj = obj.getJSONObject("schedule")
        val activeDaysJson = scheduleObj.getJSONArray("activeDays")
        val activeDays = (0 until activeDaysJson.length()).map { activeDaysJson.getString(it) }.toSet()
        val schedule = TimeSlot(
            activeDays = activeDays,
            start = ClockTime(scheduleObj.getInt("startHour"), scheduleObj.getInt("startMinute")),
            end = ClockTime(scheduleObj.getInt("endHour"), scheduleObj.getInt("endMinute"))
        )
        return BlockedAppGroup(
            id = obj.getString("id"),
            name = obj.getString("name"),
            apps = apps,
            schedule = schedule,
            // Older saves may not have these keys.
            maxOpensPerApp = if (obj.has("maxOpensPerApp")) obj.getInt("maxOpensPerApp") else null,
            maxMinutesPerApp = if (obj.has("maxMinutesPerApp")) obj.getInt("maxMinutesPerApp") else null,
            enabled = obj.optBoolean("enabled", true)
        )
    }

    companion object {
        private const val PREFS_NAME = "focus_blocked_groups"

        private const val LOCATION_PREFS_NAME = "focus_location_groups"

        fun forLocationGroups(context: Context) = BlockedAppGroupStorage(context, LOCATION_PREFS_NAME)

        private const val WIFI_PREFS_NAME = "focus_wifi_networks"

        fun forWifiNetworks(context: Context) = BlockedAppGroupStorage(context, WIFI_PREFS_NAME)

        private const val QUICK_FOCUS_PREFS_NAME = "focus_quick_focus"

        fun forQuickFocus(context: Context) = BlockedAppGroupStorage(context, QUICK_FOCUS_PREFS_NAME)

        private const val KEY_GROUPS = "groups_json"
    }
}
