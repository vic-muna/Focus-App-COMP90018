package com.example.focusapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.focusapp.domain.model.FocusSession

/**
 * Room row mirroring [FocusSession], plus one data-layer-only field:
 * `synced` tracks whether this row has reached Firebase yet (issue
 * #43 offline caching / #48 local-to-cloud sync). It deliberately does
 * NOT exist on the domain FocusSession model - the Domain/UI layers don't
 * need to know or care whether a session has synced.
 */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val distractingAppOpenCount: Int,
    val wasCompletedSuccessfully: Boolean,
<<<<<<< Updated upstream
    val synced: Boolean = false
=======
    val synced: Boolean = false,
    val groupId: String? = null
>>>>>>> Stashed changes
)

fun FocusSessionEntity.toDomain() = FocusSession(
    id = id,
    startTimeMillis = startTimeMillis,
    endTimeMillis = endTimeMillis,
    distractingAppOpenCount = distractingAppOpenCount,
<<<<<<< Updated upstream
    wasCompletedSuccessfully = wasCompletedSuccessfully
=======
    wasCompletedSuccessfully = wasCompletedSuccessfully,
    groupId = groupId
>>>>>>> Stashed changes
)

fun FocusSession.toEntity(synced: Boolean = false) = FocusSessionEntity(
    id = id,
    startTimeMillis = startTimeMillis,
    endTimeMillis = endTimeMillis,
    distractingAppOpenCount = distractingAppOpenCount,
    wasCompletedSuccessfully = wasCompletedSuccessfully,
<<<<<<< Updated upstream
    synced = synced
=======
    synced = synced,
    groupId = groupId
>>>>>>> Stashed changes
)
