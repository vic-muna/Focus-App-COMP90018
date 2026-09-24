package com.example.focusapp.domain.model

import com.google.firebase.database.PropertyName

/**
 * PartyMemberStatus
 * --------------------
 * Live snapshot of one member of a "Study Party" - the real-time social
 * feature in the project plan (share your focus status + location with
 * friends). Unlike FocusZone/AppGroup/FocusSession, this is NOT stored in
 * Room - it only ever lives in Firebase Realtime Database (see
 * data/remote/FirebaseRemoteDataSource.kt) since it's inherently transient,
 * multi-user, live data rather than something this device owns.
 *
 * [isFocusing] carries an explicit @PropertyName. Without it, Firebase's
 * reflection-based object mapper derives two DIFFERENT JSON keys for the
 * same Kotlin property depending on direction: writing calls the getter
 * `isFocusing()` and strips the "is" prefix -> key "focusing"; reading
 * back tries to match that key against the raw field name `isFocusing`
 * directly -> no match. The mismatch doesn't throw - it just logs
 * "ClassMapper: No setter/field for focusing found" and silently leaves
 * this field at its default (false) on every read, which is exactly the
 * "party members always show as not-focusing" bug this fixes. Pinning
 * both directions to the same explicit key ("focusing", matching what
 * was already being written) makes old and new data compatible.
 */
data class PartyMemberStatus(
    val uid: String = "",
    val displayName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @get:PropertyName("focusing")
    val isFocusing: Boolean = false
)
