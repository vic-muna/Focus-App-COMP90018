package com.example.focusapp.ui.common

import com.example.focusapp.data.blocking.AppItem
import com.example.focusapp.data.blocking.BlockedAppGroup
import com.example.focusapp.data.blocking.defaultTimeSlot

// Sample data for @Preview only - never shown in the real app.

fun previewApps(count: Int): List<AppItem> =
    List(count) { index -> AppItem(packageName = "com.example.fake$index", name = "App $index", isBlocked = false) }

fun previewGroups(): List<BlockedAppGroup> = listOf(
    BlockedAppGroup(id = "group_study", name = "Study Group", apps = previewApps(8), schedule = defaultTimeSlot()),
    BlockedAppGroup(id = "group_sleep", name = "Sleep Group", apps = previewApps(4), schedule = defaultTimeSlot()),
)
