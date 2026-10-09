package com.example.focusapp.data.repository

import com.example.focusapp.data.local.LocalDataSource
import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.Friend

/**
 * FakeLocalDataSource
 * ----------------------
 * Plain in-memory test double for [LocalDataSource] - no Room, no
 * Context, no emulator needed. Mirrors the *behaviour* the real
 * RoomLocalDataSource/DAOs promise closely enough to exercise
 * FocusRepositoryImpl's logic in a fast local JVM test:
 *  - saveFocusZone and addFocusZone both upsert a zone by id and keep the
 *    other zones, matching RoomLocalDataSource (which no longer clears the
 *    table first - several zones are allowed).
 *  - saveAppGroup is Upsert (replace-by-id), matching AppGroupDao.upsert.
 *  - saveFocusSession is append-only, matching FocusSessionDao.insert.
 *  - getSessionHistory is ordered newest-first, matching the DAO's
 *    `ORDER BY startTimeMillis DESC`.
 *  - synced state (for sessions, zones, and app groups alike) is tracked
 *    the same way the real `synced` columns do, just as separate
 *    Sets here since the domain models themselves don't carry that
 *    field (see FocusSessionEntity's doc comment for why).
 *    A zone/app group that is saved (again) becomes unsynced, like the
 *    real upsert, which writes synced = false.
 */
class FakeLocalDataSource : LocalDataSource {

    private val savedZones = mutableListOf<FocusZone>()
    private val syncedZoneIds = mutableSetOf<String>()
    private val appGroups = mutableListOf<AppGroup>()
    private val syncedAppGroupIds = mutableSetOf<String>()
    private val sessions = mutableListOf<FocusSession>()
    private val syncedSessionIds = mutableSetOf<String>()
    private val friends = mutableListOf<Friend>()

    // Add or replace by id, and mark it as not yet synced - what Room's upsert does.
    private fun upsertZone(zone: FocusZone) {
        savedZones.removeAll { it.id == zone.id }
        savedZones.add(zone)
        syncedZoneIds.remove(zone.id)
    }

    override suspend fun getFocusZone(): FocusZone? = savedZones.firstOrNull()

    override suspend fun saveFocusZone(zone: FocusZone) {
        upsertZone(zone)
    }

    override suspend fun getFocusZones(): List<FocusZone> = savedZones.toList()

    override suspend fun addFocusZone(zone: FocusZone): Boolean {
        upsertZone(zone)
        return true
    }

    override suspend fun deleteFocusZone(zoneId: String): Boolean {
        savedZones.removeAll { it.id == zoneId }
        syncedZoneIds.remove(zoneId)
        return true
    }

    override suspend fun getAppGroups(): List<AppGroup> = appGroups.toList()

    override suspend fun saveAppGroup(group: AppGroup) {
        appGroups.removeAll { it.id == group.id }
        appGroups.add(group)
        syncedAppGroupIds.remove(group.id)
    }

    // Like RoomLocalDataSource.getUnsyncedZone(): only the first unsynced zone.
    override suspend fun getUnsyncedZone(): FocusZone? =
        savedZones.firstOrNull { it.id !in syncedZoneIds }

    override suspend fun markZoneSynced(zoneId: String) {
        syncedZoneIds.add(zoneId)
    }

    override suspend fun getUnsyncedAppGroups(): List<AppGroup> =
        appGroups.filter { it.id !in syncedAppGroupIds }

    override suspend fun markAppGroupSynced(groupId: String) {
        syncedAppGroupIds.add(groupId)
    }

    override suspend fun getSessionHistory(): List<FocusSession> =
        sessions.sortedByDescending { it.startTimeMillis }

    override suspend fun getSessionsBetween(fromMillis: Long, toMillis: Long): List<FocusSession> =
        sessions.filter { it.startTimeMillis in fromMillis..toMillis }
            .sortedByDescending { it.startTimeMillis }

    override suspend fun saveFocusSession(session: FocusSession) {
        sessions.add(session)
    }

    override suspend fun getUnsyncedSessions(): List<FocusSession> =
        sessions.filter { it.id !in syncedSessionIds }

    override suspend fun markSessionSynced(sessionId: String) {
        syncedSessionIds.add(sessionId)
    }

    override suspend fun saveSessionFeedback(sessionId: String, feedback: String) {
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index != -1) sessions[index] = sessions[index].copy(aiFeedback = feedback)
    }

    // Rows already on the phone (same id) are kept; downloaded rows count as already synced.
    override suspend fun importFromCloud(sessions: List<FocusSession>, zones: List<FocusZone>, appGroups: List<AppGroup>) {
        sessions.filter { s -> this.sessions.none { it.id == s.id } }.forEach {
            this.sessions.add(it)
            syncedSessionIds.add(it.id)
        }
        val localZoneIds = savedZones.map { it.id }.toSet()
        zones.filter { it.id !in localZoneIds }.forEach {
            savedZones.add(it)
            syncedZoneIds.add(it.id)
        }
        appGroups.filter { g -> this.appGroups.none { it.id == g.id } }.forEach {
            this.appGroups.add(it)
            syncedAppGroupIds.add(it.id)
        }
    }

    override suspend fun clearAll() {
        savedZones.clear()
        syncedZoneIds.clear()
        appGroups.clear()
        syncedAppGroupIds.clear()
        sessions.clear()
        syncedSessionIds.clear()
        friends.clear()
    }

    override suspend fun getFriends(): List<Friend> =
        friends.sortedBy { it.nickname.lowercase() }

    override suspend fun saveFriend(friend: Friend) {
        friends.removeAll { it.uid == friend.uid }
        friends.add(friend)
    }

    override suspend fun deleteFriend(uid: String) {
        friends.removeAll { it.uid == uid }
    }
}
