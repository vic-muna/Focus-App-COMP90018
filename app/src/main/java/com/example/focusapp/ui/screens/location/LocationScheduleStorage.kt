package com.example.focusapp.ui.screens.location

import android.content.Context

/**
 * LocationScheduleStorage
 * --------------------------
 * Which app group ("Schedule") each focus location uses, keyed by zone id.
 *
 * [Claude, 2026-09-26] UI-side only (SharedPreferences), deliberately not a
 * FocusZone field: the data layer's zone model is a team decision (see the
 * README's 26/09/2026 update). Nothing reads this when a location triggers
 * a session yet - NavGraph's restrictedPackagesFor() still blocks every
 * group's apps for location sessions.
 */
class LocationScheduleStorage(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getGroupId(zoneId: String): String? = prefs.getString(zoneId, null)

    /** Links [zoneId] to [groupId], or removes the link when [groupId] is null. */
    fun setGroupId(zoneId: String, groupId: String?) {
        prefs.edit().apply {
            if (groupId == null) remove(zoneId) else putString(zoneId, groupId)
        }.apply()
    }

    private companion object {
        const val PREFS_NAME = "focus_location_schedules"
    }
}
