package com.example.focusapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.focusapp.data.local.entity.FriendEntity

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY nickname COLLATE NOCASE ASC")
    suspend fun getAll(): List<FriendEntity>

    @Upsert
    suspend fun upsert(friend: FriendEntity)

    @Query("DELETE FROM friends WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)
}
