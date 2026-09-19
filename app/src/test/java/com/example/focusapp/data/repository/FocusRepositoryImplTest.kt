package com.example.focusapp.data.repository

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.Friend
import com.example.focusapp.domain.model.PartyMemberStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * FocusRepositoryImplTest
 * ---------------------------
 * Plain local unit test (host JVM, no emulator/Robolectric) - both
 * dependencies are fakes (FakeLocalDataSource / FakeRemoteDataSource), so
 * this exercises FocusRepositoryImpl's own orchestration logic:
 * validation-before-persist, single-zone overwrite-on-save, and
 * offline-first session sync. DAO-level behaviour against a real
 * (in-memory) Room database is
 * covered separately by FocusZoneDaoTest (app/src/androidTest - needs a
 * device/emulator, since Room needs the real Android SQLite implementation).
 */
class FocusRepositoryImplTest {

    private lateinit var localDataSource: FakeLocalDataSource
    private lateinit var remoteDataSource: FakeRemoteDataSource
    private lateinit var repository: FocusRepositoryImpl

    // Fake wall clock, advanced manually by throttle tests - see updateMyPartyStatus() tests below.
    private var fakeNowMillis = 0L

    @Before
    fun setUp() {
        localDataSource = FakeLocalDataSource()
        remoteDataSource = FakeRemoteDataSource()
        repository = FocusRepositoryImpl(localDataSource, remoteDataSource, clock = { fakeNowMillis })
    }

    // ---------------- validation ----------------

    @Test
    fun saveAppGroup_rejectsBlankName_andDoesNotPersist() = runBlocking {
        try {
            repository.saveAppGroup(AppGroup(id = "g1", groupName = "   "))
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
        assertTrue(repository.getAppGroups().isEmpty())
    }

    @Test
    fun saveFocusZone_rejectsNonPositiveRadius_andDoesNotPersist() = runBlocking {
        try {
            repository.saveFocusZone(FocusZone("z1", "Library", -37.8, 144.9, 0f))
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
        assertEquals(null, repository.getFocusZone())
    }

    // ---------------- single-zone ----------------

    @Test
    fun saveFocusZone_thenGetFocusZone_returnsWhatWasSaved() = runBlocking {
        repository.saveFocusZone(FocusZone("z1", "Library", -37.8, 144.9, 50f))

        val zone = repository.getFocusZone()

        assertEquals("z1", zone?.id)
        assertEquals("Library", zone?.name)
    }

    @Test
    fun saveFocusZone_overwritesAnyPreviouslySavedZone() = runBlocking {
        repository.saveFocusZone(FocusZone("z1", "Library", -37.8, 144.9, 50f))
        repository.saveFocusZone(FocusZone("z2", "Home", -37.81, 144.96, 15f))

        val zone = repository.getFocusZone()

        // Only the most recently saved zone remains - saveFocusZone
        // replaces the user's one zone, it does not add a second one.
        assertEquals("z2", zone?.id)
        assertEquals("Home", zone?.name)
    }

    @Test
    fun saveFocusZone_withSameId_updatesInPlace() = runBlocking {
        repository.saveFocusZone(FocusZone("z1", "Library", -37.8, 144.9, 50f))
        repository.saveFocusZone(FocusZone("z1", "Library (renamed)", -37.8, 144.9, 80f))

        val zone = repository.getFocusZone()

        assertEquals("Library (renamed)", zone?.name)
        assertEquals(80f, zone?.radiusMeters)
    }

    // ---------------- offline-first session sync ----------------

    @Test
    fun saveFocusSession_succeedsLocallyEvenWhenRemotePushFails() = runBlocking {
        remoteDataSource.shouldFailPush = true
        val session = FocusSession(id = "s1", startTimeMillis = 1000L)

        repository.saveFocusSession(session) // must NOT throw just because "offline"

        assertEquals(listOf(session), repository.getSessionHistory())
        assertEquals(listOf(session), localDataSource.getUnsyncedSessions())
        assertTrue(remoteDataSource.pushedSessions.isEmpty())
    }

    @Test
    fun saveFocusSession_marksSessionSyncedWhenRemotePushSucceeds() = runBlocking {
        val session = FocusSession(id = "s1", startTimeMillis = 1000L)

        repository.saveFocusSession(session)

        assertEquals(listOf(session), remoteDataSource.pushedSessions)
        assertTrue(localDataSource.getUnsyncedSessions().isEmpty())
    }

    @Test
    fun syncPendingSessions_marksSessionSyncedOnceRemoteSucceeds() = runBlocking {
        remoteDataSource.shouldFailPush = true
        val session = FocusSession(id = "s1", startTimeMillis = 1000L)
        repository.saveFocusSession(session) // fails to push, stays pending

        remoteDataSource.shouldFailPush = false
        repository.syncPendingSessions() // retry, e.g. once back online

        assertTrue(localDataSource.getUnsyncedSessions().isEmpty())
        assertEquals(listOf(session), remoteDataSource.pushedSessions)
    }

    // ---------------- time-interval query ----------------

    @Test
    fun getSessionsBetween_returnsOnlySessionsInRange_newestFirst() = runBlocking {
        repository.saveFocusSession(FocusSession(id = "before", startTimeMillis = 500L))
        repository.saveFocusSession(FocusSession(id = "in-range-1", startTimeMillis = 1_000L))
        repository.saveFocusSession(FocusSession(id = "in-range-2", startTimeMillis = 2_000L))
        repository.saveFocusSession(FocusSession(id = "after", startTimeMillis = 5_000L))

        val result = repository.getSessionsBetween(fromMillis = 1_000L, toMillis = 3_000L)

        assertEquals(listOf("in-range-2", "in-range-1"), result.map { it.id })
    }

    @Test
    fun getSessionsBetween_boundsAreInclusive() = runBlocking {
        repository.saveFocusSession(FocusSession(id = "s1", startTimeMillis = 1_000L))
        repository.saveFocusSession(FocusSession(id = "s2", startTimeMillis = 2_000L))

        val result = repository.getSessionsBetween(fromMillis = 1_000L, toMillis = 2_000L)

        assertEquals(setOf("s1", "s2"), result.map { it.id }.toSet())
    }

    // ---------------- study party pass-through ----------------

    @Test
    fun partyCalls_delegateStraightToRemoteDataSource() = runBlocking {
        repository.sendPartyInvite("party-1", "uid-2")
        repository.respondToPartyInvite("party-1", accept = true)

        assertEquals(listOf("party-1" to "uid-2"), remoteDataSource.sentInvites)
        assertEquals(listOf("party-1" to true), remoteDataSource.respondedInvites)
    }

    @Test
    fun getMyUid_delegatesToRemoteDataSource() = runBlocking {
        assertEquals("fake-uid", repository.getMyUid())
    }

    // ---------------- updateMyPartyStatus throttling (battery/Firebase-quota answer) ----------------

    @Test
    fun updateMyPartyStatus_firstCallForAPartyAndUid_alwaysPushes() {
        val status = PartyMemberStatus(uid = "me", latitude = -37.8, longitude = 144.9)

        repository.updateMyPartyStatus("party-1", status)

        assertEquals(listOf("party-1" to status), remoteDataSource.updatedStatuses)
    }

    @Test
    fun updateMyPartyStatus_repeatedCallsWithBarelyMovedLocation_withinInterval_areThrottled() {
        val first = PartyMemberStatus(uid = "me", latitude = -37.8000, longitude = 144.9000)
        repository.updateMyPartyStatus("party-1", first)

        // Same-ish location, well under the 20m threshold, and no time has passed.
        val second = first.copy(latitude = -37.80001, longitude = 144.90001)
        repository.updateMyPartyStatus("party-1", second)

        assertEquals(1, remoteDataSource.updatedStatuses.size)
    }

    @Test
    fun updateMyPartyStatus_pushesAgainOnceMinIntervalElapsed_evenWithoutMoving() {
        val status = PartyMemberStatus(uid = "me", latitude = -37.8, longitude = 144.9)
        repository.updateMyPartyStatus("party-1", status)

        fakeNowMillis += 30_000L
        repository.updateMyPartyStatus("party-1", status)

        assertEquals(2, remoteDataSource.updatedStatuses.size)
    }

    @Test
    fun updateMyPartyStatus_pushesAgainOnceMovedFarEnough_evenWithinInterval() {
        val first = PartyMemberStatus(uid = "me", latitude = -37.8000, longitude = 144.9000)
        repository.updateMyPartyStatus("party-1", first)

        // Roughly 100m north - well over the 20m threshold - with no time elapsed.
        val moved = first.copy(latitude = -37.7991)
        repository.updateMyPartyStatus("party-1", moved)

        assertEquals(2, remoteDataSource.updatedStatuses.size)
    }

    @Test
    fun updateMyPartyStatus_isFocusingChange_alwaysPushesImmediately() {
        val notFocusing = PartyMemberStatus(uid = "me", latitude = -37.8, longitude = 144.9, isFocusing = false)
        repository.updateMyPartyStatus("party-1", notFocusing)

        val startedFocusing = notFocusing.copy(isFocusing = true)
        repository.updateMyPartyStatus("party-1", startedFocusing)

        assertEquals(2, remoteDataSource.updatedStatuses.size)
        assertEquals(startedFocusing, remoteDataSource.updatedStatuses.last().second)
    }

    @Test
    fun updateMyPartyStatus_throttlesIndependentlyPerPartyAndPerUid() {
        val status = PartyMemberStatus(uid = "me", latitude = -37.8, longitude = 144.9)

        repository.updateMyPartyStatus("party-1", status)
        repository.updateMyPartyStatus("party-2", status) // different party -> separate throttle bucket
        repository.updateMyPartyStatus("party-1", status.copy(uid = "someone-else")) // different uid, same party

        assertEquals(3, remoteDataSource.updatedStatuses.size)
    }

    // ---------------- friends (local address book for the invite picker) ----------------

    @Test
    fun saveFriend_thenGetFriends_returnsIt() = runBlocking {
        repository.saveFriend(Friend(uid = "friend-1", nickname = "Alex"))

        assertEquals(listOf(Friend(uid = "friend-1", nickname = "Alex")), repository.getFriends())
    }

    @Test
    fun saveFriend_sameUidAgain_overwritesRatherThanDuplicating() = runBlocking {
        repository.saveFriend(Friend(uid = "friend-1", nickname = "Alex"))
        repository.saveFriend(Friend(uid = "friend-1", nickname = "Alexis")) // renamed

        assertEquals(listOf(Friend(uid = "friend-1", nickname = "Alexis")), repository.getFriends())
    }

    @Test
    fun deleteFriend_removesOnlyThatUid() = runBlocking {
        repository.saveFriend(Friend(uid = "friend-1", nickname = "Alex"))
        repository.saveFriend(Friend(uid = "friend-2", nickname = "Sam"))

        repository.deleteFriend("friend-1")

        assertEquals(listOf(Friend(uid = "friend-2", nickname = "Sam")), repository.getFriends())
    }
}
