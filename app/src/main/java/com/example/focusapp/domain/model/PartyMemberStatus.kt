package com.example.focusapp.domain.model

import com.google.firebase.database.PropertyName

/**
 * One party member's live status. Only kept in Firebase, never on the phone.
 *
 * There is deliberately NO location here. It used to carry latitude/longitude, uploaded on every
 * GPS fix, but nothing in the app ever read them (the party list only shows names), and they put
 * every member's exact position where anyone with the group code could read it. Party Mode only
 * needs who is in the group, who is focusing, and who is ready.
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
    @get:PropertyName("focusing")
    val isFocusing: Boolean = false,
    val accessibilityReady: Boolean = true,
    val waitingForPermission: Boolean = false
)
