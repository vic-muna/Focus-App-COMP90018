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
 *  - synced state is tracked the same way the `synced` column does, just
 *    as a separate Set here since the domain FocusSession model itself
 *    doesn't carry that field (see FocusSessionEntity's doc comment).
 */
class FakeLocalDataSource : LocalDataSource {

    private var zone: FocusZone? = null
    private val appGroups = mutableListOf<AppGroup>()
    private val sessions = mutableListOf<FocusSession>()
    private val syncedSessionIds = mutableSetOf<String>()
    private val friends = mutableListOf<Friend>()

    override suspend fun getFocusZone(): FocusZone? = zone

    override suspend fun saveFocusZone(zone: FocusZone) {
        this.zone = zone
    }

    override suspend fun getAppGroups(): List<AppGroup> = appGroups.toList()

    override suspend fun saveAppGroup(group: AppGroup) {
        appGroups.removeAll { it.id == group.id }
        appGroups.add(group)
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
