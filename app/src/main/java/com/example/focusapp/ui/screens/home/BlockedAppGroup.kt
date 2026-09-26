package com.example.focusapp.ui.screens.home

/**
 * [David Shiau, 2026-09-26] [maxOpensPerApp]/[maxMinutesPerApp] are the
 * daily open-times/duration limits, applied to EACH app in the group on
 * its own (not a combined total), during [schedule]. null = no limit.
 * Enforced by FocusAccessibilityService (see EvaluateUsageLimitUseCase).
 */
data class BlockedAppGroup(
    val id: String,
    val name: String,
    val apps: List<AppItem>,
    val schedule: TimeSlot,
    val maxOpensPerApp: Int? = null,
    val maxMinutesPerApp: Int? = null
)

fun generateFakeGroups(): List<BlockedAppGroup> = listOf(
    BlockedAppGroup(
        id = "group_study",
        name = "Study Group",
        apps = generateFakeApps(8),
        schedule = generateFakeTimeSlot()
    ),
    BlockedAppGroup(
        id = "group_sleep",
        name = "Sleep Group",
        apps = generateFakeApps(4),
        schedule = generateFakeTimeSlot()
    )
)
