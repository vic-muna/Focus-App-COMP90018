package com.example.focusapp.data.local

import android.content.Context
import android.util.Log
import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import org.json.JSONArray
import org.json.JSONObject

/**
 * LocalDataSource
 * ------------------
 * Offline storage for the app. AppGroup and FocusZone persistence are both
 * REAL (backed by SharedPreferences + hand-rolled JSON, both built into
 * the Android SDK - no new dependency needed) - they genuinely survive an
 * app restart. FocusSession is now also REAL persistence (same approach),
 * added for the Settings screen's "today's total focus time" feature -
 * it needs sessions to survive an app restart within the day.
 *
 * WHY SharedPreferences + JSON, NOT ROOM, "AT THIS STAGE": Room needs an
 * extra Gradle dependency, an annotation-processor setup, and Entity/DAO
 * classes - real value for a finished app, but more machinery than a
 * single list of small objects needs right now. SharedPreferences +
 * manual JSON (via [org.json], already on the classpath) gets the exact
 * same "actually persists" outcome for AppGroup with far less setup.
 * TODO: to be implemented later - migrate to Room if/when AppGroup grows
 * more relational structure (e.g. a separate table of per-app schedule
 * overrides) that a flat JSON blob stops being a good fit for.
 *
 * @param context used only to reach SharedPreferences; we immediately
 *        take `.applicationContext` so this class can't accidentally
 *        leak an Activity.
 */

// [HANDOFF -> Yu-Hao Lu | README task: "Local data layer (Room/SQLite)"]
// Migrate this class's SharedPreferences+JSON storage to Room when ready
// (see the class doc comment above for why SharedPreferences was used as
// a stopgap). FocusSession now has real SharedPreferences+JSON persistence
// too (David Shiau, 2026-09-23), same caveat applies.

private const val TAG = "LocalDataSource"

class LocalDataSource(context: Context) {

    private val appContext = context.applicationContext

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


    // -----------------------------------------------------------------
    // FocusZone - REAL persistence (SharedPreferences + JSON)
    // -----------------------------------------------------------------

    /** Reads every saved FocusZone out of SharedPreferences. */
    suspend fun getFocusZones(): List<FocusZone> = readFocusZonesFromPrefs()

    /** Adds a new FocusZone, or overwrites an existing one with the same ID. */
    suspend fun saveFocusZone(zone: FocusZone): Boolean {
        return try {
            val updated = readFocusZonesFromPrefs().toMutableList()
            updated.removeAll { it.id == zone.id }
            updated.add(zone)
            writeFocusZonesToPrefs(updated)
            Log.d(TAG, "Geofence Saved Locally!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save Geofence locally: ${e.message}")
            false
        }
    }

    /** Deletes a specific FocusZone by its ID. */
    suspend fun deleteFocusZone(zoneId: String): Boolean {
        return try {
            val updated = readFocusZonesFromPrefs().toMutableList()
            updated.removeAll { it.id == zoneId }
            writeFocusZonesToPrefs(updated)
            Log.d(TAG, "Geofence Deleted Locally!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete Geofence locally: ${e.message}")
            false
        }
    }

    private fun readFocusZonesFromPrefs(): List<FocusZone> {
        val json = prefs.getString(KEY_FOCUS_ZONES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                FocusZone(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude"),
                    radiusMeters = obj.getDouble("radiusMeters").toFloat()
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun writeFocusZonesToPrefs(zones: List<FocusZone>) {
        val array = JSONArray()
        zones.forEach { zone ->
            val obj = JSONObject().apply {
                put("id", zone.id)
                put("name", zone.name)
                put("latitude", zone.latitude)
                put("longitude", zone.longitude)
                put("radiusMeters", zone.radiusMeters.toDouble())
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_FOCUS_ZONES, array.toString()).apply()
    }

    // -----------------------------------------------------------------
    // AppGroup - REAL persistence (SharedPreferences + JSON)
    // -----------------------------------------------------------------

    /**
     * Reads every saved [AppGroup] out of SharedPreferences.
     */
    suspend fun getAppGroups(): List<AppGroup> = readAppGroupsFromPrefs()

    /**
     * Adds a new [AppGroup], or overwrites the existing one with the same
     * [AppGroup.id] if it already exists.
     */
    suspend fun saveAppGroup(group: AppGroup) {
        val updated = readAppGroupsFromPrefs().toMutableList()
        updated.removeAll { it.id == group.id }
        updated.add(group)
        writeAppGroupsToPrefs(updated)
    }

    // -----------------------------------------------------------------
    // FocusSession - REAL persistence (SharedPreferences + JSON)
    // -----------------------------------------------------------------

    /** Reads every saved FocusSession out of SharedPreferences. */
    suspend fun getSessionHistory(): List<FocusSession> = readFocusSessionsFromPrefs()

    /** Adds a new FocusSession, or overwrites an existing one with the same ID. */
    suspend fun saveFocusSession(session: FocusSession) {
        val updated = readFocusSessionsFromPrefs().toMutableList()
        updated.removeAll { it.id == session.id }
        updated.add(session)
        writeFocusSessionsToPrefs(updated)
    }

    private fun readFocusSessionsFromPrefs(): List<FocusSession> {
        val json = prefs.getString(KEY_FOCUS_SESSIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                FocusSession(
                    id = obj.getString("id"),
                    startTimeMillis = obj.getLong("startTimeMillis"),
                    endTimeMillis = if (obj.isNull("endTimeMillis")) null else obj.getLong("endTimeMillis"),
                    distractingAppOpenCount = obj.optInt("distractingAppOpenCount", 0),
                    wasCompletedSuccessfully = obj.optBoolean("wasCompletedSuccessfully", false),
                    groupId = if (obj.isNull("groupId")) null else obj.optString("groupId")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun writeFocusSessionsToPrefs(sessions: List<FocusSession>) {
        val array = JSONArray()
        sessions.forEach { session ->
            val obj = JSONObject().apply {
                put("id", session.id)
                put("startTimeMillis", session.startTimeMillis)
                put("endTimeMillis", session.endTimeMillis ?: JSONObject.NULL)
                put("distractingAppOpenCount", session.distractingAppOpenCount)
                put("wasCompletedSuccessfully", session.wasCompletedSuccessfully)
                put("groupId", session.groupId ?: JSONObject.NULL)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_FOCUS_SESSIONS, array.toString()).apply()
    }

    private fun readAppGroupsFromPrefs(): List<AppGroup> {
        val json = prefs.getString(KEY_APP_GROUPS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                val packageNamesJson = obj.getJSONArray("packageNames")
                val packageNames = (0 until packageNamesJson.length()).map { packageNamesJson.getString(it) }
                AppGroup(
                    id = obj.getString("id"),
                    groupName = obj.getString("groupName"),
                    packageNames = packageNames
                )
            }
        }.getOrDefault(emptyList())
    }

    /** Serializes the given list back to JSON and writes it to SharedPreferences. */
    private fun writeAppGroupsToPrefs(groups: List<AppGroup>) {
        val array = JSONArray()
        groups.forEach { group ->
            val obj = JSONObject()
            obj.put("id", group.id)
            obj.put("groupName", group.groupName)
            obj.put("packageNames", JSONArray(group.packageNames))
            array.put(obj)
        }
        prefs.edit().putString(KEY_APP_GROUPS, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "focus_local_data"
        const val KEY_APP_GROUPS = "app_groups_json"
        const val KEY_FOCUS_ZONES = "focus_zones_json"
        const val KEY_FOCUS_SESSIONS = "focus_sessions_json"
    }
}
