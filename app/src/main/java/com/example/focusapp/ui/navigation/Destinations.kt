package com.example.focusapp.ui.navigation

/**
 * Destinations
 * -------------
 * Central list of every navigation route string used in the app. Keeping
 * them all in one object avoids typos when different files need to
 * navigate to the same screen.
 *
 * NOTE ON APP STRUCTURE (updated after a teammate's wireframes came in):
 * The bottom navigation is now Apps / Map / Settings, matching the
 * teammate-provided mockups. The Focus Mode / History / Rewards screens
 * built earlier are still in the project (see
 * ui.screens.focus / ui.screens.history / ui.screens.rewards) but are NOT
 * currently reachable from anywhere - the team still needs to decide
 * where (or whether) they fit into this navigation, since the new
 * wireframes don't show them. They're kept, not deleted, so that decision
 * doesn't cost re-doing the work.
 */
object Destinations {
    // The 3 bottom-navigation-bar destinations, matching the
    // teammate-provided wireframe's bottom bar.
    const val APPS = "apps"
    const val MAP = "map"
    const val SETTINGS = "settings"

    // Secondary screens, reachable only from the Apps tab.
    const val EDIT_APP_GROUP = "edit_app_group/{groupId}"
    const val ADD_APP_GROUP = "add_app_group"

    /** Builds a real, navigable route for [EDIT_APP_GROUP] with a given group id filled in. */
    fun editAppGroupRoute(groupId: String) = "edit_app_group/$groupId"
}

/**
 * BottomNavItem
 * ---------------
 * Describes one tab in the bottom navigation bar: which route it navigates
 * to, and what text label to show under its icon.
 */
data class BottomNavItem(
    val route: String,
    val label: String
)

/**
 * The 3 tabs shown in the bottom navigation bar, in display order.
 */
val bottomNavItems = listOf(
    BottomNavItem(Destinations.APPS, "Apps"),
    BottomNavItem(Destinations.MAP, "Map"),
    BottomNavItem(Destinations.SETTINGS, "Settings")
)
