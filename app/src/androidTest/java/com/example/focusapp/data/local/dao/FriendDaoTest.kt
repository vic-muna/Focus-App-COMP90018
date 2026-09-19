package com.example.focusapp.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.focusapp.data.local.FocusAppDatabase
import com.example.focusapp.data.local.entity.FriendEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FriendDaoTest
 * -----------------
 * Instrumented test - see FocusZoneDaoTest's doc comment for why (Room +
 * real SQLite needs a device/emulator). Fresh in-memory database per test.
 */
@RunWith(AndroidJUnit4::class)
class FriendDaoTest {

    private lateinit var db: FocusAppDatabase
    private lateinit var dao: FriendDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FocusAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.friendDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun getAll_returnsInsertedFriends_sortedByNickname() = runBlocking {
        dao.upsert(FriendEntity(uid = "u2", nickname = "zoe"))
        dao.upsert(FriendEntity(uid = "u1", nickname = "Alex"))

        val all = dao.getAll()

        assertEquals(listOf("u1", "u2"), all.map { it.uid })
    }

    @Test
    fun upsert_sameUidAgain_overwritesRatherThanDuplicating() = runBlocking {
        dao.upsert(FriendEntity(uid = "u1", nickname = "Alex"))
        dao.upsert(FriendEntity(uid = "u1", nickname = "Alexis"))

        val all = dao.getAll()

        assertEquals(1, all.size)
        assertEquals("Alexis", all.first().nickname)
    }

    @Test
    fun deleteByUid_removesOnlyThatRow() = runBlocking {
        dao.upsert(FriendEntity(uid = "u1", nickname = "Alex"))
        dao.upsert(FriendEntity(uid = "u2", nickname = "Sam"))

        dao.deleteByUid("u1")

        assertEquals(listOf("u2"), dao.getAll().map { it.uid })
    }
}
