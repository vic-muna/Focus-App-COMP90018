package com.example.focusapp.domain.model

/**
 * AppGroup
 * ----------
 * A named collection of app package names that get restricted together
 * (e.g. "Social Media" -> Instagram, TikTok, Twitter). Corresponds to the
 * "Add Group" / "Select App" / "Edit Group" nodes in the process-flow
 * diagram. packageNames is populated from the device's real installed-app
 * list (see data/apps/InstalledAppsProvider.kt, used by AddAppGroupScreen),
 * persisted via RoomLocalDataSource, and synced to Firebase - see
 * FocusRepositoryImpl.saveAppGroup().
 */
data class AppGroup(
    val id: String,
    val groupName: String,
    val packageNames: List<String> = emptyList()
)
