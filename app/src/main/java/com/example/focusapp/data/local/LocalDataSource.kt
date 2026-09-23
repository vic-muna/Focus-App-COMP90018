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
 * app restart. FocusSession is still the original in-memory placeholder
 * from before; it wasn't part of this round of work.
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
// a stopgap), and implement getSessionHistory()/saveFocusSession() for real.

private const val TAG = "LocalDataSource"

class LocalDataSource(context: Context) {

    private val appContext = context.applicationContext

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Temporary in-memory placeholder, unchanged from before - see class
    // doc comment. Not part of this round of work.
    private val cachedSessions = mutableListOf<FocusSession>()
    
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
    // FocusSession - IN-MEMORY PLACEHOLDER
    // -----------------------------------------------------------------

    /** TODO: to be implemented later - replace with real persistence. */
    suspend fun getSessionHistory(): List<FocusSession> = cachedSessions

    /** TODO: to be implemented later - replace with real persistence. */
    suspend fun saveFocusSession(session: FocusSession) {
        cachedSessions.add(session)
    }

    /**
     * readAppGroupsFromPrefs
     * -------------------------
     * Parses the single JSON-array string stored under [KEY_APP_GROUPS]
     * back into a `List<AppGroup>`. Storing all groups as ONE string
     * under ONE key (rather than one SharedPreferences key per group) is
     * deliberate: SharedPreferences has no query language, so "one key
     * per group" would still require reading every key and parsing every
     * value just to list them all - a single combined key is no less
     * capable here and is simpler to reason about.
     */
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
                    radiusMeters = obj.getDouble("radius").toFloat()
                )
            }
        }.getOrDefault(emptyList())
        // If the stored JSON is ever malformed for any reason, fail safe
        // to an empty list rather than crashing the Apps screen.
    }

    private fun writeFocusZonesToPrefs(zones: List<FocusZone>) {
        val array = JSONArray()
        zones.forEach { zone ->
            val obj = JSONObject()
            obj.put("id", zone.id)
            obj.put("name", zone.name)
            obj.put("latitude", zone.latitude)
            obj.put("longitude", zone.longitude)
            obj.put("radius", zone.radiusMeters.toDouble())
            array.put(obj)
        }
        prefs.edit().putString(KEY_FOCUS_ZONES, array.toString()).apply()
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
    }
}
