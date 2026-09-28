package com.example.focusapp.domain.model

/**
 * A named list of app package names, saved with Room and backed up to Firebase.
 */
data class AppGroup(
    val id: String,
    val groupName: String,
    val packageNames: List<String> = emptyList()
)
