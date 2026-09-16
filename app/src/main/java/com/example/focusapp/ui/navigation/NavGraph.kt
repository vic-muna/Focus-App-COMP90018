package com.example.focusapp.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.focusapp.ui.screens.apps.AddAppGroupScreen
import com.example.focusapp.ui.screens.apps.AppsScreen
import com.example.focusapp.ui.screens.apps.EditAppGroupScreen
import com.example.focusapp.ui.screens.home.BlockedAppGroup
import com.example.focusapp.ui.screens.home.EditLocationZoneScreen
import com.example.focusapp.ui.screens.home.GroupListScreen
import com.example.focusapp.ui.screens.home.HomeScreenWithSheet
import com.example.focusapp.ui.screens.home.generateFakeGroups
import com.example.focusapp.ui.screens.home.generateFakeTimeSlot
import com.example.focusapp.ui.screens.map.MapScreen
import com.example.focusapp.ui.screens.party.PartyModeScreen
import com.example.focusapp.ui.screens.session.ActiveFocusSession
import com.example.focusapp.ui.screens.session.FOCUS_SESSION_WIPE_DURATION_MILLIS
import com.example.focusapp.ui.screens.session.FocusSessionScreen
import com.example.focusapp.ui.screens.session.FocusSessionSource
import com.example.focusapp.ui.screens.settings.SettingsScreen
import com.example.focusapp.ui.theme.WireframeColors

// Every screen in the app uses this same vertical motion for consistency,
// matching the ModalBottomSheet's own motion: entering slides up from off
// the bottom of the screen, and exiting/going back slides back down off
// the bottom (the reverse).
private val enterFromBottom: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn()
}
private val exitToBottom: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut()
}

// Party Mode doesn't use the app-wide slide pair above - it never moves
// position, and instead owns its own solid-color wipe reveal/cover
// animation internally (see PartyModeScreen.kt). At the NavGraph level it
// just fades in/out (translucent -> fully appeared, and the reverse going
// back) rather than popping straight to full opacity.
private val partyModeEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(300))
}
private val partyModeExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(300))
}

// Home's own exit, EXCEPT when the target is Focus Session: there, Home fades out
// over the same FOCUS_SESSION_WIPE_DURATION_MILLIS as FocusSessionScreen's own
// reveal wipe (see FocusSessionScreen.kt), so Home's disappearance and the new
// screen's reveal read as one continuous motion instead of Home cutting away
// quickly (the shared exitToBottom's default ~300ms) before the reveal even starts.
private val homeExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (targetState.destination.route == Destinations.FOCUS_SESSION) {
        fadeOut(tween(FOCUS_SESSION_WIPE_DURATION_MILLIS))
    } else {
        exitToBottom()
    }
}

private val focusSessionEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(FOCUS_SESSION_WIPE_DURATION_MILLIS))
}

@Composable
fun FocusAppNavGraph() {
    val navController = rememberNavController()

    // Hoisted here so Home / GroupList / GroupSection / Edit share one source of truth.
    var groups by remember { mutableStateOf(generateFakeGroups()) }
    var selectedGroupId by remember { mutableStateOf(groups.first().id) }

    // null = no focus session running. Set by any of the three trigger sources
    // (Quick Focus, Party Mode's Go Focus Mode, Home's auto-suggestion banner),
    // cleared when FocusSessionScreen's End Session action fires.
    var activeFocusSession by remember { mutableStateOf<ActiveFocusSession?>(null) }

    fun startFocusSession(source: FocusSessionSource) {
        activeFocusSession = ActiveFocusSession(
            startTimeMillis = System.currentTimeMillis(),
            source = source,
            groupId = (source as? FocusSessionSource.Schedule)?.groupId
        )
        navController.navigate(Destinations.FOCUS_SESSION)
    }

    Scaffold(containerColor = WireframeColors.Background) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = Destinations.HOME,
                enterTransition = enterFromBottom,
                exitTransition = homeExitTransition,
                // DIAGNOSTIC (temporary): no animation at all on the way back to
                // Home, to isolate whether the reported "scaling" artifact comes
                // from this transition or from something else (e.g. a layout
                // remeasure) entirely. A plain fadeIn here did NOT fix it, and
                // fadeIn alone cannot produce a scale/position effect - so this
                // rules the transition system in or out definitively.
                popEnterTransition = { EnterTransition.None }
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
                        // Reset so a later reopen that doesn't explicitly set a
                        // type (GroupList's back button or group selection)
                        // doesn't reuse a stale "location_zone" value from a
                        // previous zone edit.
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
                    onAvatarClick = {
                        // TODO: Report screen (History + Rewards merged) not built yet
                    },
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
                enterTransition = focusSessionEnter,
                exitTransition = partyModeExit,
                popExitTransition = partyModeExit
            ) {
                activeFocusSession?.let { session ->
                    FocusSessionScreen(
                        session = session,
                        onEndSessionClick = {
                            activeFocusSession = null
                            navController.popBackStack(Destinations.HOME, inclusive = false)
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
                        // No detail screen anymore - selecting a group just
                        // returns to Home with the sheet reopened on it.
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
