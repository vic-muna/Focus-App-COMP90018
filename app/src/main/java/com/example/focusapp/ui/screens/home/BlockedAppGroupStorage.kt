package com.example.focusapp.ui.screens.home

import android.content.Context
import com.example.focusapp.data.apps.getAppIcon
import org.json.JSONArray
import org.json.JSONObject

/**
 * BlockedAppGroupStorage
 * -------------------------
 * Real persistence (SharedPreferences + JSON, same approach LocalDataSource
 * uses for FocusZone/AppGroup) for Home's blocked-app groups and which one
 * is currently selected.
 *
 * Lives here (ui.screens.home), not in data/local/LocalDataSource, because
 * BlockedAppGroup/AppItem/TimeSlot are UI-layer types (this project hasn't
 * split out separate domain models for them yet) - keeping this file here
 * avoids importing UI types into the data layer.
 *
 * [David Shiau, 2026-09-23] Added: previously `groups`/`selectedGroupId`
 * lived only in NavGraph's `remember { mutableStateOf(...) }`, so both
 * reset back to generateFakeGroups() every time the app process was killed
 * and restarted - the selected group (and any edits made to it) didn't
 * survive.
 *
 * AppItem.icon is intentionally NOT persisted - a Bitmap doesn't round-trip
 * through JSON. It's re-resolved from packageName via [getAppIcon] on load,
 * the same way AppsScreen's GroupMemberIcon resolves icons on demand.
 */
class BlockedAppGroupStorage(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Null if nothing has ever been saved (first launch) - caller should fall back to default/fake data. */
    suspend fun getGroups(): List<BlockedAppGroup>? {
        val json = prefs.getString(KEY_GROUPS, null) ?: return null
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index -> groupFromJson(array.getJSONObject(index)) }
        }.getOrNull()
    }

    suspend fun saveGroups(groups: List<BlockedAppGroup>) {
        val array = JSONArray()
        groups.forEach { array.put(groupToJson(it)) }
        prefs.edit().putString(KEY_GROUPS, array.toString()).apply()
    }

    suspend fun getSelectedGroupId(): String? = prefs.getString(KEY_SELECTED_GROUP_ID, null)

    suspend fun saveSelectedGroupId(groupId: String) {
        prefs.edit().putString(KEY_SELECTED_GROUP_ID, groupId).apply()
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
    }

    private fun groupFromJson(obj: JSONObject): BlockedAppGroup {
        val appsJson = obj.getJSONArray("apps")
        val apps = (0 until appsJson.length()).map { index ->
            val appObj = appsJson.getJSONObject(index)
            val packageName = appObj.getString("packageName")
            AppItem(
                packageName = packageName,
                name = appObj.getString("name"),
                isBlocked = appObj.getBoolean("isBlocked"),
                icon = getAppIcon(appContext, packageName)
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
            schedule = schedule
        )
    }

    private companion object {
        const val PREFS_NAME = "focus_blocked_groups"
        const val KEY_GROUPS = "groups_json"
        const val KEY_SELECTED_GROUP_ID = "selected_group_id"
    }
}
