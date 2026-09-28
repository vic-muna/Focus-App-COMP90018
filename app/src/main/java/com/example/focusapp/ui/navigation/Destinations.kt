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
    const val HOME = "home"

    // Location Focus tab (bottom nav pin icon) - map + pull-up list of
    // location groups. See ui.screens.location.LocationScreen.
    const val LOCATION = "location"

    // Schedule tab (bottom nav clock icon) - list of time slots. Not routed
    // while the tab is a disabled placeholder; see ui.screens.timefocus.TimeFocusScreen.
    const val TIME_FOCUS = "time_focus"

    // Replaces the old bottom-left "Map" shortcut on Home - Party Mode is the
    // planned "Study Party" feature (see the doc comments in
    // data/remote/RemoteDataSource.kt and data/repository/FocusRepositoryImpl.kt).
    // UI scaffolding only for now; no networking/P2P logic behind it yet.
    const val PARTY_MODE = "party_mode"

    // Party Mode's first page - the friend list, reached from Home's group icon;
    // Create / Join group open as fly cards on it. See ui.screens.party.FriendsScreen.
    const val FRIENDS = "friends"

    // Focus History - see ui.screens.history.HistoryScreen. Reachable from the History
    // tile on the Home screen (the block above Quick Focus - see HomeScreen.kt).
    const val HISTORY = "history"

    // Background theme picker - opened from the ID card on HISTORY (the dashboard).
    const val THEME_PICKER = "theme_picker"

    // The Wi-Fi tab - see ui.screens.wififocus.WifiFocusScreen.
    const val WIFI = "wifi"

    // [David Shiau, 2026-09-26] Today's open counts/durations for one
    // Blocked-App-Group - reached by tapping Home's schedule banner. See
    // ui.screens.home.GroupUsageScreen.
    const val GROUP_USAGE = "group_usage/{groupId}"

    /** Builds a real, navigable route for [GROUP_USAGE] with a given group id filled in. */
    fun groupUsageRoute(groupId: String) = "group_usage/$groupId"

    // Focus Session screen - reached from Home's Quick Focus button, Party
    // Mode's "Go Focus Mode" button, or Home's location/Wi-Fi banner. See
    // ui.screens.session.FocusSessionScreen and NavGraph.kt's hoisted
    // `activeFocusSession` state.
    const val FOCUS_SESSION = "focus_session"

    // Secondary screens, reachable only from the Apps tab.
    const val EDIT_APP_GROUP = "edit_app_group/{groupId}"
    const val ADD_APP_GROUP = "add_app_group"

    /** Builds a real, navigable route for [EDIT_APP_GROUP] with a given group id filled in. */
    fun editAppGroupRoute(groupId: String) = "edit_app_group/$groupId"
}
