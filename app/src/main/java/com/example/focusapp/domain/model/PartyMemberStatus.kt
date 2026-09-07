package com.example.focusapp.domain.model

/**
 * PartyMemberStatus
 * --------------------
 * Live snapshot of one member of a "Study Party" - the real-time social
 * feature in the project plan (share your focus status + location with
 * friends). Unlike FocusZone/AppGroup/FocusSession, this is NOT stored in
 * Room - it only ever lives in Firebase Realtime Database (see
 * data/remote/FirebaseRemoteDataSource.kt) since it's inherently transient,
 * multi-user, live data rather than something this device owns.
 */
data class PartyMemberStatus(
    val uid: String = "",
    val displayName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isFocusing: Boolean = false
)
