package com.example.focusapp.domain.model

/**
 * AppGroup
 * ----------
 * A named collection of app package names that get restricted together
 * (e.g. "Social Media" -> Instagram, TikTok, Twitter). Corresponds to the
 * "Add Group" / "Select App" / "Edit Group" nodes in the process-flow
 * diagram.
 *
 * TODO: to be implemented later - populate packageNames by letting the
 * user pick from installed apps (PackageManager).
 */
data class AppGroup(
    val id: String,
    val groupName: String,
    val packageNames: List<String> = emptyList()
)
