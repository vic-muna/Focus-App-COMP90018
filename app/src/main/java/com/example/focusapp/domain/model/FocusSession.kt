package com.example.focusapp.domain.model

/**
 * One focus session, shown on the dashboard (History).
 */
data class FocusSession(
    val id: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val distractingAppOpenCount: Int = 0,
    val wasCompletedSuccessfully: Boolean = false,
    /** Which BlockedAppGroup this session used, if any (null for manual/party/location-triggered sessions with no group). */
    val groupId: String? = null
)
