package com.example.focusapp.domain.model

/**
 * Friend
 * --------
 * A locally-saved contact: someone else's Firebase uid plus a nickname so
 * the person adding them doesn't have to remember/paste a raw uid every
 * time. There's no server-side "friend request" concept here (that would
 * need its own backend flow) - adding a friend is purely local, one-sided
 * bookkeeping so *you* can pick them again later (e.g. from
 * [com.example.focusapp.ui.screens.party.FriendListScreen] when inviting
 * someone to a Study Party). For two people to be able to invite each
 * other, each side separately saves the other's uid.
 */
data class Friend(
    val uid: String,
    val nickname: String
)
