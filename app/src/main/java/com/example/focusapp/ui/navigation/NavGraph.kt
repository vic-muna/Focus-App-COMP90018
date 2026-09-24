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
import com.example.focusapp.ui.screens.home.BlockedAppGroup
import com.example.focusapp.ui.screens.home.BlockedAppGroupStorage
import com.example.focusapp.ui.screens.home.EditLocationZoneScreen
import com.example.focusapp.ui.screens.home.GroupListScreen
import com.example.focusapp.ui.screens.home.HomeScreenWithSheet
import com.example.focusapp.ui.screens.home.generateFakeGroups
import com.example.focusapp.ui.screens.home.generateFakeTimeSlot
import com.example.focusapp.ui.screens.history.HistoryScreen
import com.example.focusapp.ui.screens.map.MapScreen
import com.example.focusapp.ui.screens.party.PartyModeScreen
import com.example.focusapp.ui.screens.session.ActiveFocusSession
import com.example.focusapp.ui.screens.session.FocusSessionScreen
import com.example.focusapp.ui.screens.session.FocusSessionSource
import com.example.focusapp.ui.screens.settings.SettingsScreen
import com.example.focusapp.ui.theme.WireframeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Every screen in the app uses this same vertical motion for consistency
private val enterFromBottom: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn()
}
private val exitToBottom: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut()
}

private val partyModeEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(300))
}
private val partyModeExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(300))
}

// Home fades out (matching Party Mode/Focus Session's own fade-in, no slide)
// specifically when heading to one of those two destinations - every other
// destination Home can go to keeps the normal slide-down.
private val homeExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    when (targetState.destination.route) {
        Destinations.PARTY_MODE, Destinations.FOCUS_SESSION -> partyModeExit()
        else -> exitToBottom()
    }
}

@Composable
fun FocusAppNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current

    var groups by remember { mutableStateOf(generateFakeGroups()) }
    var selectedGroupId by remember { mutableStateOf(groups.first().id) }
    var activeFocusSession by remember { mutableStateOf<ActiveFocusSession?>(null) }

    // [David Shiau, 2026-09-23] Persists `groups`/`selectedGroupId` (see
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
            val savedSelectedId = withContext(Dispatchers.IO) { groupStorage.getSelectedGroupId() }
            selectedGroupId = savedSelectedId
                ?.takeIf { id -> savedGroups.any { it.id == id } }
                ?: savedGroups.first().id
        }
        hasLoadedGroups = true
    }

    LaunchedEffect(groups, hasLoadedGroups) {
        if (hasLoadedGroups) {
            withContext(Dispatchers.IO) { groupStorage.saveGroups(groups) }
        }
    }

    LaunchedEffect(selectedGroupId, hasLoadedGroups) {
        if (hasLoadedGroups) {
            withContext(Dispatchers.IO) { groupStorage.saveSelectedGroupId(selectedGroupId) }
        }
    }

    // [David Shiau, 2026-09-20] Which packages a session should restrict,
    // resolved per source: a schedule-triggered session blocks the group
    // that matched it; a manual Quick Focus blocks whichever group is
    // currently selected on Home; and Party/Location/Wifi sessions (no
    // single specific group) fall back to the union of every saved group's
    // packages. [David Shiau, 2026-09-23] Wifi follows Location's behavior
    // here, per the same "no per-trigger group assignment yet" reasoning.
    fun restrictedPackagesFor(source: FocusSessionSource): List<String> {
        return when (source) {
            is FocusSessionSource.Schedule ->
                groups.find { it.id == source.groupId }?.apps?.map { it.packageName } ?: emptyList()
            FocusSessionSource.Manual ->
                groups.find { it.id == selectedGroupId }?.apps?.map { it.packageName } ?: emptyList()
            FocusSessionSource.Party, is FocusSessionSource.Location, is FocusSessionSource.Wifi ->
                groups.flatMap { group -> group.apps.map { it.packageName } }.distinct()
        }
    }

    // [David Shiau, 2026-09-20] Activates real app blocking for the
    // session's resolved packages via FocusAccessibilityService.
    fun startFocusSession(source: FocusSessionSource) {
        AccessibilityBridge.setRestrictedPackages(restrictedPackagesFor(source))
        val startTimeMillis = System.currentTimeMillis()
        activeFocusSession = ActiveFocusSession(
            startTimeMillis = startTimeMillis,
            source = source,
            groupId = (source as? FocusSessionSource.Schedule)?.groupId
        )
        // [Claude, 2026-09-21] Mirrors the in-app timer with a persistent
        // notification-shade entry - see FocusTimerService's doc comment.
        // Additive only: does not change anything about the blocking call
        // above or the navigation below.
        FocusTimerService.start(context, startTimeMillis)
        navController.navigate(Destinations.FOCUS_SESSION)
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
                enterTransition = enterFromBottom,
                exitTransition = homeExitTransition
            ) { backStackEntry ->
                val reopenSheet by backStackEntry.savedStateHandle
                    .getStateFlow("reopenSheet", false)
                    .collectAsState()
                val reopenSheetType by backStackEntry.savedStateHandle
                    .getStateFlow("reopenSheetType", "blocked_apps")
                    .collectAsState()

                HomeScreenWithSheet(
                    groups = groups,
                    selectedGroupId = selectedGroupId,
                    reopenSheetSignal = reopenSheet,
                    reopenSheetType = reopenSheetType,
                    onReopenSheetHandled = {
                        backStackEntry.savedStateHandle["reopenSheet"] = false
                        backStackEntry.savedStateHandle["reopenSheetType"] = "blocked_apps"
                    },
                    onBlockerClick = {
                        navController.navigate(Destinations.GROUP_LIST)
                    },
                    onGroupAppsChange = { groupId, apps ->
                        groups = groups.map { g -> if (g.id == groupId) g.copy(apps = apps) else g }
                    },
                    onGroupScheduleChange = { groupId, schedule ->
                        groups = groups.map { g -> if (g.id == groupId) g.copy(schedule = schedule) else g }
                    },
                    onGroupRename = { groupId, newName ->
                        groups = groups.map { g -> if (g.id == groupId) g.copy(name = newName) else g }
                    },
                    // [HANDOFF -> Kai-Jiun Chan | README task: "Reward/Progress UI"]
                    // 首頁規格.md originally wanted this to open a two-tab (History/
                    // Rewards) Report screen. Rewards isn't built yet, so this routes
                    // straight to History (this branch's own screen) for now rather
                    // than leaving the tap dead - swap this for the real Report screen
                    // once Rewards exists.
                    onAvatarClick = { navController.navigate(Destinations.HISTORY) },
                    onFocusSessionStart = { source -> startFocusSession(source) },
                    onPartyModeClick = { navController.navigate(Destinations.PARTY_MODE) },
                    onSettingsClick = { navController.navigate(Destinations.SETTINGS) },
                    onEditLocationZoneClick = {
                        navController.navigate(Destinations.EDIT_LOCATION_ZONE)
                    }
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
                route = Destinations.GROUP_LIST,
                enterTransition = enterFromBottom,
                exitTransition = exitToBottom
            ) {
                GroupListScreen(
                    groups = groups,
                    selectedGroupId = selectedGroupId,
                    onGroupSelect = { groupId ->
                        selectedGroupId = groupId
                        navController.getBackStackEntry(Destinations.HOME)
                            .savedStateHandle["reopenSheet"] = true
                        navController.popBackStack(Destinations.HOME, inclusive = false)
                    },
                    onAddGroupClick = { name ->
                        val newGroup = BlockedAppGroup(
                            id = "group_${System.currentTimeMillis()}",
                            name = name.ifBlank { "New Group" },
                            apps = emptyList(),
                            schedule = generateFakeTimeSlot()
                        )
                        groups = groups + newGroup
                        selectedGroupId = newGroup.id
                        navController.getBackStackEntry(Destinations.HOME)
                            .savedStateHandle["reopenSheet"] = true
                        navController.popBackStack(Destinations.HOME, inclusive = false)
                    },
                    onBackClick = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("reopenSheet", true)
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Destinations.EDIT_LOCATION_ZONE,
                enterTransition = enterFromBottom,
                exitTransition = exitToBottom
            ) {
                EditLocationZoneScreen(
                    onSaveComplete = {
                        navController.getBackStackEntry(Destinations.HOME).savedStateHandle.apply {
                            set("reopenSheetType", "location_zone")
                            set("reopenSheet", true)
                        }
                        navController.popBackStack(Destinations.HOME, inclusive = false)
                    }
                )
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
            ) { SettingsScreen() }

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
