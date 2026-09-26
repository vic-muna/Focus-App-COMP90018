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

    // Replaces the old bottom-left "Map" shortcut on Home - Party Mode is the
    // planned "Study Party" feature (see the doc comments in
    // data/remote/RemoteDataSource.kt and data/repository/FocusRepositoryImpl.kt).
    // UI scaffolding only for now; no networking/P2P logic behind it yet.
    const val PARTY_MODE = "party_mode"

    // Focus History - see ui.screens.history.HistoryScreen. Reachable from the History
    // tile on the Home screen (the block above Quick Focus - see HomeScreen.kt).
    const val HISTORY = "history"

    // Blocked Apps group flow (reachable from Home's bottom sheet). Editing
    // now happens in pickers layered on the summary sheet itself - this is
    // the only extra destination, for choosing/creating/renaming/deleting
    // which group is selected.
    const val GROUP_LIST = "group_list"

    // [David Shiau, 2026-09-26] Location Zone's own group list (select/
    // create) - the same GroupListScreen as GROUP_LIST, but over the
    // separate location groups. Reachable from the Location Zone sheet.
    const val LOCATION_GROUP_LIST = "location_group_list"

    // [David Shiau, 2026-09-26] Same again for Wi-Fi Source Detection's own
    // groups. Reachable from the Wi-Fi sheet on Home.
    const val WIFI_GROUP_LIST = "wifi_group_list"

    // Location Zone edit flow (reachable from Home's bottom sheet). Single
    // zone only - no id/argument needed, unlike the blocked-apps group flow.
    const val EDIT_LOCATION_ZONE = "edit_location_zone"

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
