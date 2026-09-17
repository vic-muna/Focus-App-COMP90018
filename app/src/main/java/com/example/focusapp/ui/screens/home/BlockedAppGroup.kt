package com.example.focusapp.ui.screens.home

data class BlockedAppGroup(
    val id: String,
    val name: String,
    val apps: List<AppItem>,
    val schedule: TimeSlot
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
