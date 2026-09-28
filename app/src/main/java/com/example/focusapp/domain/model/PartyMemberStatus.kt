package com.example.focusapp.domain.model

import com.google.firebase.database.PropertyName

/**
 * One party member's live status. Only kept in Firebase, never on the phone.
 *
 * [isFocusing] needs @PropertyName("focusing"): without it Firebase writes
 * "focusing" but reads "isFocusing", so it would always read false.
 */
data class PartyMemberStatus(
    val uid: String = "",
    val displayName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @get:PropertyName("focusing")
    val isFocusing: Boolean = false
)
