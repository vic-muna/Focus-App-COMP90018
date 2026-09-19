package com.example.focusapp.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.focusapp.data.local.FocusAppDatabase
import com.example.focusapp.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FocusSessionDaoTest
 * -----------------------
 * Instrumented test - run from Android Studio or `./gradlew connectedAndroidTest`; needs a
 * device/emulator, same reason as FocusZoneDaoTest (Room needs the real Android SQLite
 * implementation). Covers getBetween() specifically - the other queries (getAll/insert/
 * getUnsynced/markSynced) are already exercised indirectly through FocusRepositoryImplTest's
 * fake-backed equivalents.
 *
 * Uses a fresh in-memory database per test (thrown away in @After).
 */
@RunWith(AndroidJUnit4::class)
class FocusSessionDaoTest {

    private lateinit var db: FocusAppDatabase
    private lateinit var dao: FocusSessionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FocusAppDatabase::class.java)
            .allowMainThreadQueries() // test-only convenience; production code never does this
            .build()
        dao = db.focusSessionDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun session(id: String, startTimeMillis: Long) = FocusSessionEntity(
        id = id,
        startTimeMillis = startTimeMillis,
        endTimeMillis = startTimeMillis + 1_000L,
        distractingAppOpenCount = 0,
        wasCompletedSuccessfully = true
    )

    @Test
    fun getBetween_excludesSessionsOutsideRange() = runBlocking {
        dao.insert(session("before", 500L))
        dao.insert(session("in-range-1", 1_000L))
        dao.insert(session("in-range-2", 2_000L))
        dao.insert(session("after", 5_000L))

        val result = dao.getBetween(fromMillis = 1_000L, toMillis = 3_000L)

        assertEquals(setOf("in-range-1", "in-range-2"), result.map { it.id }.toSet())
    }

    @Test
    fun getBetween_boundsAreInclusive() = runBlocking {
        dao.insert(session("s1", 1_000L))
        dao.insert(session("s2", 2_000L))

        val result = dao.getBetween(fromMillis = 1_000L, toMillis = 2_000L)

        assertEquals(2, result.size)
    }

    @Test
    fun getBetween_ordersNewestFirst() = runBlocking {
        dao.insert(session("older", 1_000L))
        dao.insert(session("newer", 2_000L))

        val result = dao.getBetween(fromMillis = 0L, toMillis = 10_000L)

        assertEquals(listOf("newer", "older"), result.map { it.id })
    }

    @Test
    fun getBetween_returnsEmpty_whenNothingInRange() = runBlocking {
        dao.insert(session("s1", 1_000L))

        val result = dao.getBetween(fromMillis = 5_000L, toMillis = 6_000L)

        assertEquals(0, result.size)
    }
}
