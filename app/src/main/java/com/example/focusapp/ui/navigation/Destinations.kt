package com.example.focusapp.ui.navigation

/** Every screen's route name, in one place so they can't be mistyped. */
object Destinations {
    // Bottom-nav tabs
    const val HOME = "home"
    const val LOCATION = "location"
    const val TIME_FOCUS = "time_focus"
    const val WIFI = "wifi"

    // Opened from Home
    const val FRIENDS = "friends"               // Party Mode (top-left icon)
    const val SETTINGS = "settings"             // Top-right gear
    const val HISTORY = "history"               // Dashboard (avatar)
    const val THEME_PICKER = "theme_picker"     // From the dashboard's ID card
    const val FOCUS_SESSION = "focus_session"   // The running focus timer

    // Opened from Settings
    const val CREATE_ACCOUNT = "create_account" // A guest turning into an account
}
