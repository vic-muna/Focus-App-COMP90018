package com.example.focusapp.ui.screens.home

/** Break length a new group starts with, in minutes. */
const val DEFAULT_BREAK_MINUTES = 10

/**
 * [breakAllowance] / [breakMinutes]: how many "tea breaks" a focus session
 * with this group allows, and how long each lasts ("Unfrozen Times" /
 * "Relaxing minutes" in the add-group card). [Claude, 2026-09-26] Stored
 * only - nothing enforces breaks during a session yet.
 */
data class BlockedAppGroup(
    val id: String,
    val name: String,
    val apps: List<AppItem>,
    val schedule: TimeSlot,
    val breakAllowance: Int = 0,
    val breakMinutes: Int = DEFAULT_BREAK_MINUTES
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
