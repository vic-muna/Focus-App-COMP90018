package com.example.focusapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.focusapp.domain.model.Friend

/** Room row mirroring [Friend]. uid is the primary key - saving a friend again with the
 *  same uid (e.g. editing their nickname) overwrites the existing row rather than duplicating it. */
@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val uid: String,
    val nickname: String
)

fun FriendEntity.toDomain() = Friend(uid, nickname)
fun Friend.toEntity() = FriendEntity(uid, nickname)
