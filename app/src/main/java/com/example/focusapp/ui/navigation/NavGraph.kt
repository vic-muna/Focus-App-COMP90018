package com.example.focusapp.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.notification.FocusTimerService
import com.example.focusapp.ui.screens.apps.AddAppGroupScreen
import com.example.focusapp.ui.screens.apps.AppsScreen
import com.example.focusapp.ui.screens.apps.EditAppGroupScreen
import com.example.focusapp.ui.screens.history.HistoryScreen
import com.example.focusapp.ui.screens.home.BlockedAppGroup
import com.example.focusapp.ui.screens.home.BlockedAppGroupStorage
import com.example.focusapp.ui.screens.home.GroupUsageScreen
import com.example.focusapp.ui.screens.home.HomeScreenWithSheet
import com.example.focusapp.ui.screens.home.generateFakeGroups
import com.example.focusapp.ui.screens.home.generateFakeTimeSlot
import com.example.focusapp.ui.screens.location.LocationScreen
import com.example.focusapp.ui.screens.timefocus.TimeFocusScreen
import com.example.focusapp.ui.screens.wififocus.WifiFocusScreen
import com.example.focusapp.ui.screens.map.MapScreen
import com.example.focusapp.ui.screens.party.PartyModeScreen
import com.example.focusapp.ui.screens.session.ActiveFocusSession
import com.example.focusapp.ui.screens.session.FocusSessionScreen
import com.example.focusapp.ui.screens.session.FocusSessionSource
import com.example.focusapp.ui.screens.settings.SettingsScreen
import com.example.focusapp.ui.theme.WireframeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Any navigation into or out of Home is a dissolve (cross-fade) - both the
// leaving and the arriving screen fade, in place, over the same duration.
private const val DISSOLVE_DURATION_MILLIS = 300
private val dissolveIn = fadeIn(tween(DISSOLVE_DURATION_MILLIS))
private val dissolveOut = fadeOut(tween(DISSOLVE_DURATION_MILLIS))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.involvesHome(): Boolean =
    initialState.destination.route == Destinations.HOME ||
            targetState.destination.route == Destinations.HOME

// Every other screen uses this same vertical motion for consistency - except
// when the other side of the transition is Home, which dissolves instead.
private val enterFromBottom: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (involvesHome()) dissolveIn
    else slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn()
}
private val exitToBottom: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (involvesHome()) dissolveOut
    else slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut()
}

private val homeEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { dissolveIn }
private val homeExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { dissolveOut }

private val partyModeEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    dissolveIn
}
private val partyModeExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    dissolveOut
}

/**
 * A new (empty) per-location group - reuses BlockedAppGroup, but its
 * schedule/limit fields are never used.
 */
private fun defaultGroup(id: String, name: String) = BlockedAppGroup(
    id = id,
    name = name,
    apps = emptyList(),
    schedule = generateFakeTimeSlot()
)

/** A group list kept in sync with a [BlockedAppGroupStorage]. */
private class PersistedGroupList {
    var groups by mutableStateOf(emptyList<BlockedAppGroup>())

    fun updateGroup(groupId: String, transform: (BlockedAppGroup) -> BlockedAppGroup) {
        groups = groups.map { g -> if (g.id == groupId) transform(g) else g }
    }
}

/**
 * [David Shiau, 2026-09-26] Loads the saved list once on first composition,
 * then re-saves automatically whenever the list changes - the
 * same pattern FocusAppNavGraph uses for the Scheduled Limits `groups`,
 * factored out since Location and Wi-Fi both need it.
 */
@Composable
private fun rememberPersistedGroupList(storage: BlockedAppGroupStorage): PersistedGroupList {
    val state = remember { PersistedGroupList() }
    var hasLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val saved = withContext(Dispatchers.IO) { storage.getGroups() }
        if (saved != null) state.groups = saved
        hasLoaded = true
    }

    LaunchedEffect(state.groups, hasLoaded) {
        if (hasLoaded) withContext(Dispatchers.IO) { storage.saveGroups(state.groups) }
    }

    return state
}

@Composable
fun FocusAppNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current

    var groups by remember { mutableStateOf(generateFakeGroups()) }
    var activeFocusSession by remember { mutableStateOf<ActiveFocusSession?>(null) }

    // [David Shiau, 2026-09-23] Persists `groups` (see
    // BlockedAppGroupStorage's doc comment for why they weren't persisted
    // before) - loads saved data once on first composition, then re-saves
    // automatically whenever either value changes.
    //
    // NOTE (data-layer branch merge): this SharedPreferences-backed store is
    // deliberately separate from domain.model.AppGroup / RoomLocalDataSource
    // (this branch's real Room-backed persistence for the "Apps" tab) - the
    // two are independent systems that both happen to describe "a group of
    // apps". Unifying them is a real, team-level decision, not something
    // this merge attempts; see this file's git history / chat log for that
    // discussion. For now, Quick Focus / Home's auto-blocking flow keeps
    // reading from BlockedAppGroupStorage exactly as it already did, and
    // the Apps tab keeps reading/writing AppGroup via FocusRepositoryProvider
    // exactly as it already did - neither was changed to reach into the
    // other's storage.
    val groupStorage = remember { BlockedAppGroupStorage(context) }
    var hasLoadedGroups by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val savedGroups = withContext(Dispatchers.IO) { groupStorage.getGroups() }
        if (savedGroups != null) {
            groups = savedGroups
        }
        hasLoadedGroups = true
    }

    LaunchedEffect(groups, hasLoadedGroups) {
        if (hasLoadedGroups) {
            withContext(Dispatchers.IO) { groupStorage.saveGroups(groups) }
        }
    }

    // [David Shiau, 2026-09-26] Location Zone's and Wi-Fi Source Detection's
    // own app groups - two more lists, independent of `groups` (Scheduled
    // Limits) and of each other, each persisted in its own file.
    val location = rememberPersistedGroupList(remember { BlockedAppGroupStorage.forLocationGroups(context) })
    // One entry per watched Wi-Fi network - its id is the SSID.
    val wifi = rememberPersistedGroupList(remember { BlockedAppGroupStorage.forWifiNetworks(context) })

    // [David Shiau, 2026-09-20] Which packages a session should restrict,
    // resolved per source: Party sessions (no single specific group) fall
    // back to the union of every saved Scheduled Limits group's packages.
    // [David Shiau, 2026-09-26] Schedule no longer starts sessions (see
    // GroupUsageScreen), so its branch here was removed. Location and Wi-Fi
    // are detached from the Scheduled Limits groups and from each other:
    // each blocks only its OWN selected group - and a manual Quick Focus
    // for now behaves exactly like accepting the location banner.
    fun restrictedPackagesFor(source: FocusSessionSource): List<String> {
        return when (source) {
            // Each location blocks its own list (group id = zone id); Quick Focus
            // isn't tied to a place, so it blocks every location's apps.
            is FocusSessionSource.Location ->
                location.groups.find { it.id == source.zoneId }?.apps?.map { it.packageName }.orEmpty()
            FocusSessionSource.Manual ->
                location.groups.flatMap { group -> group.apps.map { it.packageName } }.distinct()
            is FocusSessionSource.Wifi ->
                wifi.groups.find { it.id == source.ssid }?.apps?.map { it.packageName }.orEmpty()
            FocusSessionSource.Party ->
                groups.flatMap { group -> group.apps.map { it.packageName } }.distinct()
        }
    }

    // [David Shiau, 2026-09-20] Activates real app blocking for the
    // session's resolved packages via FocusAccessibilityService.
    fun startFocusSession(source: FocusSessionSource) {
        AccessibilityBridge.setRestrictedPackages(
            restrictedPackagesFor(source),
            // [David Shiau, 2026-09-26] Shown on the blocked screen, so a
            // location/Wi-Fi block clearly says what caused it.
            reason = when (source) {
                is FocusSessionSource.Location -> "Blocked while you're at ${source.zoneName}."
                is FocusSessionSource.Wifi -> "Blocked while you're connected to ${source.ssid} Wi-Fi."
                else -> null
            }
        )
        val startTimeMillis = System.currentTimeMillis()
        activeFocusSession = ActiveFocusSession(
            startTimeMillis = startTimeMillis,
            source = source
        )
        // [Claude, 2026-09-21] Mirrors the in-app timer with a persistent
        // notification-shade entry - see FocusTimerService's doc comment.
        // Additive only: does not change anything about the blocking call
        // above or the navigation below.
        FocusTimerService.start(context, startTimeMillis)
        navController.navigate(Destinations.FOCUS_SESSION)
    }


    // Bottom-nav tabs: Home is the root; every other tab sits directly on top of it.
    fun navigateToTab(tab: MainTab) {
        when (tab) {
            MainTab.HOME -> navController.popBackStack(Destinations.HOME, inclusive = false)
            MainTab.LOCATION -> navController.navigate(Destinations.LOCATION) {
                popUpTo(Destinations.HOME)
                launchSingleTop = true
            }
            MainTab.SCHEDULE -> navController.navigate(Destinations.TIME_FOCUS) {
                popUpTo(Destinations.HOME)
                launchSingleTop = true
            }
            MainTab.WIFI_SOURCE -> navController.navigate(Destinations.WIFI) {
                popUpTo(Destinations.HOME)
                launchSingleTop = true
            }
        }
    }

    Scaffold(containerColor = WireframeColors.Background) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .clipToBounds()
        ) {
            composable(
                route = Destinations.HOME,
                enterTransition = homeEnter,
                exitTransition = homeExit
            ) {
                HomeScreenWithSheet(
                    groups = groups,
                    // The networks whose switch is on.
                    wifiSsids = wifi.groups.filter { it.enabled }.map { it.id },
                    // [HANDOFF -> Kai-Jiun Chan | README task: "Reward/Progress UI"]
                    // 首頁規格.md originally wanted this to open a two-tab (History/
                    // Rewards) Report screen. Rewards isn't built yet, so this routes
                    // straight to History (this branch's own screen) for now rather
                    // than leaving the tap dead - swap this for the real Report screen
                    // once Rewards exists.
                    onAvatarClick = { navController.navigate(Destinations.HISTORY) },
                    onFocusSessionStart = { source -> startFocusSession(source) },
                    onScheduleBannerClick = { groupId ->
                        navController.navigate(Destinations.groupUsageRoute(groupId))
                    },
                    onPartyModeClick = { navController.navigate(Destinations.PARTY_MODE) },
                    onSettingsClick = { navController.navigate(Destinations.SETTINGS) },
                    onLocationTabClick = { navigateToTab(MainTab.LOCATION) },
                    onScheduleTabClick = { navigateToTab(MainTab.SCHEDULE) },
                    onWifiTabClick = { navigateToTab(MainTab.WIFI_SOURCE) }
                )
            }


            composable(
                route = Destinations.LOCATION,
                enterTransition = partyModeEnter,
                exitTransition = partyModeExit
            ) {
                LocationScreen(
                    onTabClick = ::navigateToTab,
                    blockedAppsFor = { zoneId -> location.groups.find { it.id == zoneId }?.apps.orEmpty() },
                    onZoneBlockedAppsChange = { zone, apps ->
                        if (location.groups.any { it.id == zone.id }) {
                            location.updateGroup(zone.id) { it.copy(name = zone.name, apps = apps) }
                        } else {
                            location.groups = location.groups + defaultGroup(zone.id, zone.name).copy(apps = apps)
                        }
                    },
                    onZoneDeleted = { zoneId -> location.groups = location.groups.filterNot { it.id == zoneId } },
                    // Drops lists whose location no longer exists (the data layer keeps
                    // one location at a time, so saving a new one replaces the old).
                    onZonesLoaded = { zoneIds -> location.groups = location.groups.filter { it.id in zoneIds } }
                )
            }
            composable(
                route = Destinations.TIME_FOCUS,
                enterTransition = partyModeEnter,
                exitTransition = partyModeExit
            ) {
                TimeFocusScreen(
                    groups = groups,
                    onGroupsChange = { groups = it },
                    onTabClick = ::navigateToTab
                )
            }

            composable(
                route = Destinations.WIFI,
                enterTransition = partyModeEnter,
                exitTransition = partyModeExit
            ) {
                WifiFocusScreen(
                    groups = wifi.groups,
                    onGroupsChange = { wifi.groups = it },
                    onTabClick = ::navigateToTab
                )
            }

            composable(
                route = Destinations.PARTY_MODE,
                enterTransition = partyModeEnter,
                exitTransition = partyModeExit
            ) {
                PartyModeScreen(
                    onBackClick = { navController.popBackStack() },
                    onGoFocusModeClick = { startFocusSession(FocusSessionSource.Party) }
                )
            }

            composable(
                route = Destinations.FOCUS_SESSION,
                enterTransition = partyModeEnter,
                exitTransition = partyModeExit
            ) {
                // Snapshot activeFocusSession ONCE per navigation to this route, via
                // remember - deliberately NOT re-reading the live (nullable) hoisted
                // state on every recomposition. AnimatedContent keeps this composable
                // alive and recomposing for the entire exit transition (the fade-out
                // when leaving this screen), which happens AFTER onEndSessionClick has
                // already cleared activeFocusSession back to null below. Without this
                // remember, that recomposition would read a null activeFocusSession and
                // render nothing (`activeFocusSession?.let { ... }` renders empty on
                // null) - a blank screen (just the Scaffold background, no UI at all)
                // for the whole exit transition, which is exactly the "Quick Focus ends
                // -> blank screen until app restart" bug. Capturing it once means this
                // composable keeps rendering its last real frame consistently all the
                // way through the fade-out, regardless of what activeFocusSession does
                // afterward.
                val session = remember { activeFocusSession }
                if (session != null) {
                    FocusSessionScreen(
                        session = session,
                        onEndSessionClick = {
                            // [David Shiau, 2026-09-20] Lifts blocking once the session is over.
                            AccessibilityBridge.clearRestrictedPackages()
                            // [Claude, 2026-09-21] Removes the persistent notification-shade
                            // entry started in startFocusSession above. Runs on both the
                            // normal-completion and hold-to-cancel paths, since both funnel
                            // through this same callback (see FocusSessionScreen.kt).
                            FocusTimerService.stop(context)
                            // Navigate away first, THEN clear the shared state - so the
                            // state change can't affect a composable that's still on
                            // screen (playing its exit transition) when it happens.
                            navController.popBackStack(Destinations.HOME, inclusive = false)
                            activeFocusSession = null
                        }
                    )
                }
            }

            composable(
                route = Destinations.GROUP_USAGE,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
                enterTransition = enterFromBottom,
                exitTransition = exitToBottom
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")
                // Snapshot once, same reason as FOCUS_SESSION above - keeps the
                // exit transition rendering even if the group is deleted meanwhile.
                val group = remember { groups.find { it.id == groupId } }
                if (group != null) {
                    GroupUsageScreen(
                        group = group,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = Destinations.APPS,
                enterTransition = enterFromBottom,
                exitTransition = exitToBottom
            ) {
                AppsScreen(
                    onEditGroupClick = { groupId ->
                        navController.navigate(Destinations.editAppGroupRoute(groupId))
                    },
                    onAddGroupClick = {
                        navController.navigate(Destinations.ADD_APP_GROUP)
                    }
                )
            }

            composable(
                route = Destinations.MAP,
                enterTransition = enterFromBottom,
                exitTransition = exitToBottom
            ) { MapScreen() }

            composable(
                route = Destinations.SETTINGS,
                enterTransition = enterFromBottom,
                exitTransition = exitToBottom
            ) { SettingsScreen(onClose = { navController.popBackStack() }) }

            composable(
                route = Destinations.HISTORY,
                enterTransition = enterFromBottom,
                exitTransition = exitToBottom
            ) { HistoryScreen() }

            composable(
                route = Destinations.EDIT_APP_GROUP,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
                enterTransition = enterFromBottom,
                exitTransition = exitToBottom
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                EditAppGroupScreen(
                    groupId = groupId,
                    onSaveClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Destinations.ADD_APP_GROUP,
                enterTransition = enterFromBottom,
                exitTransition = exitToBottom
            ) {
                AddAppGroupScreen(
                    onSaveClick = { navController.popBackStack() }
                )
            }
        }
    }
}