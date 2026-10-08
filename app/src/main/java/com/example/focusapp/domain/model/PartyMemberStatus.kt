package com.example.focusapp.domain.model

import com.google.firebase.database.PropertyName

/**
 * One party member's live status. Only kept in Firebase, never on the phone.
 *
 * [isFocusing] needs @PropertyName("focusing"): without it Firebase writes
 * "focusing" but reads "isFocusing", so it would always read false.
 *
 * [accessibilityReady] is whether this phone has App Blocking (Accessibility) turned on - the
 * host can't start the group's session until every member has it. [waitingForPermission] is the
 * host saying "I tried to start but someone isn't ready"; members who aren't ready see it and
 * get a notification. The default is ready, so a member who never publishes it doesn't block.
 */
data class PartyMemberStatus(
    val uid: String = "",
    val displayName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @get:PropertyName("focusing")
    val isFocusing: Boolean = false,
    val accessibilityReady: Boolean = true,
    val waitingForPermission: Boolean = false
)
