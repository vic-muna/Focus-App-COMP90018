package com.example.focusapp.data.local

import android.content.Context
import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import org.json.JSONArray
import org.json.JSONObject

/**
 * LocalDataSource
 * ------------------
 * Offline storage for the app. As of this update, AppGroup persistence is
 * REAL (backed by SharedPreferences + hand-rolled JSON, both built into
 * the Android SDK - no new dependency needed) - it genuinely survives an
 * app restart. FocusZone/FocusSession are still the original in-memory
 * placeholder from before; they weren't part of this round of work.
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
class LocalDataSource(context: Context) {

    private val appContext = context.applicationContext

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Temporary in-memory placeholders, unchanged from before - see class
    // doc comment. Not part of this round of work.
    private val cachedZones = mutableListOf<FocusZone>()
    private val cachedSessions = mutableListOf<FocusSession>()

    // -----------------------------------------------------------------
    // AppGroup - REAL persistence (SharedPreferences + JSON)
    // -----------------------------------------------------------------

    /** TODO: to be implemented later - replace with a real Room query. */
    suspend fun getFocusZones(): List<FocusZone> = cachedZones

    /** TODO: to be implemented later - replace with a real Room insert/update. */
    suspend fun saveFocusZone(zone: FocusZone) {
        cachedZones.removeAll { it.id == zone.id }
        cachedZones.add(zone)
    }

    /**
     * Reads every saved [AppGroup] out of SharedPreferences. Genuinely
     * reflects whatever was last written by [saveAppGroup] - including
     * across app restarts, unlike the FocusZone/FocusSession methods
     * above/below.
     */
    suspend fun getAppGroups(): List<AppGroup> = readAppGroupsFromPrefs()

    /**
     * Adds a new [AppGroup], or overwrites the existing one with the same
     * [AppGroup.id] if it already exists (an "upsert" - this is how
     * editing an existing group would also work, once EditAppGroupScreen
     * saves real changes instead of just its still-placeholder schedule
     * fields).
     */
    suspend fun saveAppGroup(group: AppGroup) {
        val updated = readAppGroupsFromPrefs().toMutableList()
        updated.removeAll { it.id == group.id }
        updated.add(group)
        writeAppGroupsToPrefs(updated)
    }

    /** TODO: to be implemented later - replace with a real Room query. */
    suspend fun getSessionHistory(): List<FocusSession> = cachedSessions

    /** TODO: to be implemented later - replace with a real Room insert. */
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
        // If the stored JSON is ever malformed for any reason, fail safe
        // to an empty list rather than crashing the Apps screen.
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
    }
}
