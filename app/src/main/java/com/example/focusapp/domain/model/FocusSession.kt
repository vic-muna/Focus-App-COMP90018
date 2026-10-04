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
    val groupId: String? = null,
    /** One short Focus Coach line about this specific session (see data/ai/FocusCoach.kt's
     *  sessionFeedback) - null until FocusRepositoryImpl's fire-and-forget generation finishes,
     *  or if AI consent hasn't been given, or the day's AI request limit is already used up.
     *  History treats null as "nothing to show", not an error. */
    val aiFeedback: String? = null
)
