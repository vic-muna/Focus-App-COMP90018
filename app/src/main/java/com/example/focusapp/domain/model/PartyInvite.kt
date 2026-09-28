package com.example.focusapp.domain.model

/**
 * An invite to a party, sent to this phone.
 * @param partyId the party's code
 * @param fromUid who sent it
 */
data class PartyInvite(
    val partyId: String,
    val fromUid: String
)
