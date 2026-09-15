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
import com.example.focusapp.ui.screens.home.EditBlockedAppsScreen
import com.example.focusapp.ui.screens.home.EditLocationZoneScreen
import com.example.focusapp.ui.screens.home.GroupListScreen
import com.example.focusapp.ui.screens.home.GroupSectionScreen
import com.example.focusapp.ui.screens.home.HomeScreenWithSheet
import com.example.focusapp.ui.screens.home.generateFakeGroups
import com.example.focusapp.ui.screens.home.generateFakeTimeSlots
import com.example.focusapp.ui.screens.map.MapScreen
import com.example.focusapp.ui.screens.party.PartyModeScreen
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

@Composable
fun FocusAppNavGraph() {
    val navController = rememberNavController()

    // Hoisted here so Home / GroupList / GroupSection / Edit share one source of truth.
    var groups by remember { mutableStateOf(generateFakeGroups()) }
    var selectedGroupId by remember { mutableStateOf(groups.first().id) }

    Scaffold(containerColor = WireframeColors.Background) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = Destinations.HOME,
                enterTransition = enterFromBottom,
                exitTransition = exitToBottom
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
                        // type (Blocked Apps' GroupList/GroupSection back button,
                        // EDIT_BLOCKED_APPS_GROUP save) doesn't reuse a stale
                        // "location_zone" value from a previous zone edit.
                        backStackEntry.savedStateHandle["reopenSheetType"] = "blocked_apps"
                    },
                    onBlockerClick = {
                        navController.navigate(Destinations.GROUP_LIST)
                    },
                    onAvatarClick = {
                        // TODO: Report screen (History + Rewards merged) not built yet
                    },
                    onQuickFocusClick = {
                        // TODO: manual Focus Mode trigger, no ViewModel/logic yet
                    },
                    onPartyModeClick = { navController.navigate(Destinations.PARTY_MODE) },
                    onSettingsClick = { navController.navigate(Destinations.SETTINGS) },
                    onEditBlockedAppsClick = {
                        navController.navigate(
                            Destinations.editBlockedAppsGroupRoute(selectedGroupId)
                        )
                    },
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
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Destinations.GROUP_LIST,
                enterTransition = enterFromBottom,
                exitTransition = exitToBottom
            ) {
                GroupListScreen(
                    groups = groups,
                    activeGroupId = selectedGroupId,
                    onBackClick = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("reopenSheet", true)
                        navController.popBackStack()
                    },
                    onGroupClick = { groupId ->
                        selectedGroupId = groupId
                        navController.navigate(Destinations.groupSectionRoute(groupId))
                    },
                    onAddGroupClick = {
                        navController.navigate(
                            Destinations.editBlockedAppsGroupRoute(Destinations.NEW_GROUP_ID)
                        )
                    }
                )
            }

            // Single group detail page.
            composable(
                route = Destinations.GROUP_SECTION,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
                enterTransition = enterFromBottom,
                exitTransition = exitToBottom
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")
                val group = groups.find { it.id == groupId }

                if (group != null) {
                    GroupSectionScreen(
                        group = group,
                        onBackClick = {
                            // Skip the list page: go straight Home and reopen the sheet.
                            navController.getBackStackEntry(Destinations.HOME)
                                .savedStateHandle["reopenSheet"] = true
                            navController.popBackStack(Destinations.HOME, inclusive = false)
                        },
                        onEditClick = {
                            navController.navigate(
                                Destinations.editBlockedAppsGroupRoute(group.id)
                            )
                        }
                    )
                }
            }

            // Edit / Add group — one screen for both, told apart by NEW_GROUP_ID.
            composable(
                route = Destinations.EDIT_BLOCKED_APPS_GROUP,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
                enterTransition = enterFromBottom,
                exitTransition = exitToBottom
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")
                    ?: Destinations.NEW_GROUP_ID
                val isNewGroup = groupId == Destinations.NEW_GROUP_ID
                val existingGroup = if (isNewGroup) null else groups.find { it.id == groupId }

                var editedApps by remember(groupId) {
                    mutableStateOf(existingGroup?.apps ?: emptyList())
                }

                EditBlockedAppsScreen(
                    initialGroupName = existingGroup?.name ?: "",
                    apps = editedApps,
                    onAppsChange = { editedApps = it },
                    onSaveClick = { finalName ->
                        if (isNewGroup) {
                            val newGroup = BlockedAppGroup(
                                id = "group_${System.currentTimeMillis()}",
                                name = finalName.ifBlank { "New Group" },
                                apps = editedApps,
                                timeSlots = generateFakeTimeSlots()
                            )
                            groups = groups + newGroup
                            selectedGroupId = newGroup.id
                        } else {
                            groups = groups.map { group ->
                                if (group.id == groupId) {
                                    group.copy(
                                        name = finalName.ifBlank { group.name },
                                        apps = editedApps
                                    )
                                } else {
                                    group
                                }
                            }
                        }

                        // Go back to wherever this screen was actually opened
                        // from - Home's "Edit" sheet button, GroupList's "+",
                        // or GroupSection's "Edit" - rather than always
                        // force-jumping to Home. Only Home needs the
                        // reopenSheet signal, since GroupList/GroupSection
                        // aren't behind a bottom sheet.
                        if (navController.previousBackStackEntry?.destination?.route == Destinations.HOME) {
                            navController.getBackStackEntry(Destinations.HOME)
                                .savedStateHandle["reopenSheet"] = true
                        }
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
