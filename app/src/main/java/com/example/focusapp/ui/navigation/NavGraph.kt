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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import com.example.focusapp.ui.screens.session.FocusSessionScreen
import com.example.focusapp.ui.screens.session.FocusSessionSource
import com.example.focusapp.ui.screens.settings.SettingsScreen
import com.example.focusapp.ui.theme.WireframeColors

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

    var groups by remember { mutableStateOf(generateFakeGroups()) }
    var selectedGroupId by remember { mutableStateOf(groups.first().id) }
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
                    onAvatarClick = { },
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