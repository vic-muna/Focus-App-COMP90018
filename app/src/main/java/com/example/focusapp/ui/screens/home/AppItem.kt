package com.example.focusapp.ui.screens.home

data class AppItem(
    val id: Int,
    val name: String,
    val isBlocked: Boolean
)

fun generateFakeApps(count: Int = 100): List<AppItem> =
    List(count) { index ->
        AppItem(id = index, name = "App $index", isBlocked = false)
    }