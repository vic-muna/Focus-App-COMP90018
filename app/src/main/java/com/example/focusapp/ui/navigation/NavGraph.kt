package com.example.focusapp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.focusapp.ui.screens.apps.AddAppGroupScreen
import com.example.focusapp.ui.screens.apps.AppsScreen
import com.example.focusapp.ui.screens.apps.EditAppGroupScreen
import com.example.focusapp.ui.screens.map.MapScreen
import com.example.focusapp.ui.screens.settings.SettingsScreen
import com.example.focusapp.ui.theme.WireframeColors

/**
 * FocusAppNavGraph
 * -----------------
 * Root composable for the whole app. It combines:
 *  1. A [Scaffold] whose bottom bar is the 3-tab navigation
 *     (Apps / Map / Settings), styled to match a teammate's wireframe.
 *  2. A [NavHost] that swaps the visible screen based on the selected route.
 *
 * This is pure UI scaffolding - none of the screens reached from here
 * perform any real sensing, restriction, or networking yet. See each
 * screen file's doc comment for exactly what is/isn't implemented.
 *
 * To add a new screen later: add a route constant in [Destinations], add
 * a `composable(...)` line below, and (if it should be a bottom tab) add
 * it to [bottomNavItems].
 */
@Composable
fun FocusAppNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(navController)
        },
        containerColor = WireframeColors.Background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.APPS,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destinations.APPS) {
                AppsScreen(
                    onEditGroupClick = { groupId ->
                        navController.navigate(Destinations.editAppGroupRoute(groupId))
                    },
                    onAddGroupClick = {
                        navController.navigate(Destinations.ADD_APP_GROUP)
                    }
                )
            }
            composable(Destinations.MAP) { MapScreen() }
            composable(Destinations.SETTINGS) { SettingsScreen() }

            // Secondary screens - not tabs themselves, only reached via Apps.
            composable(
                route = Destinations.EDIT_APP_GROUP,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                EditAppGroupScreen(
                    groupId = groupId,
                    onSaveClick = {
                        // "Save" has no real effect on the schedule fields
                        // yet (nothing is persisted for those) - it just
                        // returns to the Apps list. The group's name/apps
                        // themselves were already persisted back when the
                        // group was created on AddAppGroupScreen.
                        navController.popBackStack()
                    }
                )
            }
            composable(Destinations.ADD_APP_GROUP) {
                AddAppGroupScreen(
                    onSaveClick = {
                        // Same as above - purely navigational for now.
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

/**
 * AppBottomNavigationBar
 * -----------------------
 * A custom-drawn bottom bar (rounded pill shape, thin vertical dividers
 * between items) rather than the stock Material3 [androidx.compose.material3.NavigationBar],
 * so it visually matches the teammate-provided wireframe. Behaviourally
 * it works the same as a normal bottom nav: tapping a tab navigates to it
 * and highlights whichever tab is currently selected.
 */
@Composable
private fun AppBottomNavigationBar(navController: NavHostController) {
    val icons = mapOf(
        Destinations.APPS to Icons.Filled.Apps,
        Destinations.MAP to Icons.Filled.Map,
        Destinations.SETTINGS to Icons.Filled.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WireframeColors.BottomBar, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        bottomNavItems.forEachIndexed { index, item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            navController.navigate(item.route) {
                                // Pop back to the graph's start destination so tab-
                                // switching doesn't pile up a huge back stack.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = icons.getValue(item.route),
                        contentDescription = item.label,
                        tint = if (selected) WireframeColors.OnDark else WireframeColors.OnDark.copy(alpha = 0.6f)
                    )
                    Text(
                        text = item.label,
                        color = WireframeColors.OnDark,
                        fontSize = 12.sp
                    )
                }

                // Thin vertical divider between tabs, matching the
                // wireframe's separator lines (not drawn after the last tab).
                if (index != bottomNavItems.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(WireframeColors.OnDark.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}
