package com.example.focusapp.domain.model

/**
 * A friend: their Firebase user id and a nickname.
 * Saved only on this phone (there are no friend requests).
 */
data class Friend(
    val uid: String,
    val nickname: String
)
