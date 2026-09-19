package com.example.focusapp.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.focusapp.data.local.FocusAppDatabase
import com.example.focusapp.data.local.entity.FocusZoneEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FocusZoneDaoTest
 * -------------------
 * Instrumented test - run it from Android Studio (right-click the file ->
 * Run 'FocusZoneDaoTest') or `./gradlew connectedAndroidTest`; needs a
 * device/emulator because Room needs the real Android SQLite
 * implementation, which the plain JVM `test` source set doesn't have.
 * See FocusRepositoryImplTest (app/src/test) for the fake-backed
 * equivalent that runs instantly with no device.
 *
 * Uses a fresh in-memory database per test (thrown away in @After), so
 * these tests never touch the real focus_app.db on the test device.
 *
 * NOTE: this DAO/table is intentionally still general-purpose (able to
 * hold several rows) - it's RoomLocalDataSource.saveFocusZone() one layer
 * up that enforces the app's "at most one saved zone" contract, by
 * calling deleteAll() before every upsert(). The multi-row test below is
 * still valid: it's checking the DAO mechanics, not the app-level policy.
 */
@RunWith(AndroidJUnit4::class)
class FocusZoneDaoTest {

    private lateinit var db: FocusAppDatabase
    private lateinit var dao: FocusZoneDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FocusAppDatabase::class.java)
            .allowMainThreadQueries() // test-only convenience; production code never does this
            .build()
        dao = db.focusZoneDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun getAll_returnsEveryZoneThatWasUpserted_multiZone() = runBlocking {
        dao.upsert(FocusZoneEntity("z1", "Library", -37.8, 144.9, 50f))
        dao.upsert(FocusZoneEntity("z2", "Home", -37.81, 144.96, 15f))
        dao.upsert(FocusZoneEntity("z3", "Cafe", -37.79, 144.95, 30f))

        val all = dao.getAll()

        assertEquals(3, all.size)
        assertEquals(setOf("z1", "z2", "z3"), all.map { it.id }.toSet())
    }

    @Test
    fun upsert_withSameId_overwritesInsteadOfDuplicating() = runBlocking {
        dao.upsert(FocusZoneEntity("z1", "Library", -37.8, 144.9, 50f))
        dao.upsert(FocusZoneEntity("z1", "Library (renamed)", -37.8, 144.9, 80f))

        val all = dao.getAll()

        assertEquals(1, all.size)
        assertEquals("Library (renamed)", all.first().name)
        assertEquals(80f, all.first().radiusMeters)
    }

    @Test
    fun deleteAll_removesEveryZone() = runBlocking {
        dao.upsert(FocusZoneEntity("z1", "Library", -37.8, 144.9, 50f))
        dao.upsert(FocusZoneEntity("z2", "Home", -37.81, 144.96, 15f))

        dao.deleteAll()

        assertEquals(0, dao.getAll().size)
    }
}
