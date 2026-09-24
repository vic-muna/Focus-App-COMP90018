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
 *  - saveFocusZone overwrites the single saved zone, matching
 *    RoomLocalDataSource's deleteAll()-then-upsert().
 *  - saveAppGroup is Upsert (replace-by-id), matching AppGroupDao.upsert.
 *  - saveFocusSession is append-only, matching FocusSessionDao.insert.
 *  - getSessionHistory is ordered newest-first, matching the DAO's
 *    `ORDER BY startTimeMillis DESC`.
 *  - synced state (for sessions, zone, and app groups alike) is tracked
 *    the same way the real `synced` columns do, just as separate
 *    Sets/flags here since the domain models themselves don't carry that
 *    field (see FocusSessionEntity's doc comment for why).
 */
class FakeLocalDataSource : LocalDataSource {

    private var zone: FocusZone? = null
    private var zoneSynced: Boolean = false
    private val appGroups = mutableListOf<AppGroup>()
    private val syncedAppGroupIds = mutableSetOf<String>()
    private val sessions = mutableListOf<FocusSession>()
    private val syncedSessionIds = mutableSetOf<String>()
    private val friends = mutableListOf<Friend>()
<<<<<<< Updated upstream
=======
    private val extraZones = mutableListOf<FocusZone>()
>>>>>>> Stashed changes

    override suspend fun getFocusZone(): FocusZone? = zone

    override suspend fun saveFocusZone(zone: FocusZone) {
        this.zone = zone
        zoneSynced = false
    }

<<<<<<< Updated upstream
=======
    override suspend fun getFocusZones(): List<FocusZone> = listOfNotNull(zone) + extraZones

    override suspend fun addFocusZone(zone: FocusZone): Boolean {
        extraZones.removeAll { it.id == zone.id }
        extraZones.add(zone)
        return true
    }

    override suspend fun deleteFocusZone(zoneId: String): Boolean {
        extraZones.removeAll { it.id == zoneId }
        if (this.zone?.id == zoneId) this.zone = null
        return true
    }

>>>>>>> Stashed changes
    override suspend fun getAppGroups(): List<AppGroup> = appGroups.toList()

    override suspend fun saveAppGroup(group: AppGroup) {
        appGroups.removeAll { it.id == group.id }
        appGroups.add(group)
        syncedAppGroupIds.remove(group.id)
    }

    override suspend fun getUnsyncedZone(): FocusZone? = zone?.takeIf { !zoneSynced }

    override suspend fun markZoneSynced(zoneId: String) {
        if (zone?.id == zoneId) zoneSynced = true
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
