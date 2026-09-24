package com.example.focusapp.domain.model

/**
 * PartyInvite
 * -------------
 * One incoming Study Party invite addressed to this device's uid - what
 * [com.example.focusapp.domain.repository.FocusRepository.observeMyIncomingInvites]
 * streams. Not stored in Room (same reasoning as [PartyMemberStatus]: this
 * is live, other-user-originated data, not something this device owns).
 *
 * @param partyId the code to pass to [com.example.focusapp.domain.repository.FocusRepository.respondToPartyInvite]
 *                (and, on accept, to join the party for real).
 * @param fromUid the inviting device's uid - resolve this against the local
 *                Friends list (see ui/screens/party/FriendListViewModel.kt)
 *                to show a nickname instead of a raw uid where possible.
 */
data class PartyInvite(
    val partyId: String,
    val fromUid: String
)
