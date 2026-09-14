package com.example.focusapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.focusapp.ui.screens.apps.AddAppGroupScreen
import com.example.focusapp.ui.screens.apps.AppsScreen
import com.example.focusapp.ui.screens.apps.EditAppGroupScreen
import com.example.focusapp.ui.screens.home.*
import com.example.focusapp.ui.screens.map.MapScreen
import com.example.focusapp.ui.screens.settings.SettingsScreen
import com.example.focusapp.ui.theme.WireframeColors

@Composable
fun FocusAppNavGraph() {
    val navController = rememberNavController()

    Scaffold(containerColor = WireframeColors.Background) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destinations.HOME) {
                //Home 就是在這邊被用到的
                HomeScreenWithSheet(
                    onAvatarClick = {
                        // TODO: Report screen (History + Rewards merged) not built yet
                    },
                    onQuickFocusClick = {
                        // TODO: manual Focus Mode trigger, no ViewModel/logic yet
                    },
                    onMapClick = {
                        navController.navigate(Destinations.MAP)
                    },
//                    onBlockedAppCardClick = {
//                        // TODO: should open Bottom Sheet, not built yet.
//                        // Temporarily jump straight to the existing Apps list.
//                        navController.navigate(Destinations.APPS)
//                    },
//                    onLocationCardClick = {
//                        // TODO: Location Zone screen not built yet
//                    },
                   onSettingsClick = {
                       navController.navigate(Destinations.SETTINGS)
                   }
                )
            }

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
            //composable(Destinations.SETTINGS) { SettingsScreen() }

            composable(
                route = Destinations.EDIT_APP_GROUP,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                EditAppGroupScreen(
                    groupId = groupId,
                    onSaveClick = { navController.popBackStack() }
                )
            }
            composable(Destinations.ADD_APP_GROUP) {
                AddAppGroupScreen(
                    onSaveClick = { navController.popBackStack() }
                )
            }
        }
    }
}

