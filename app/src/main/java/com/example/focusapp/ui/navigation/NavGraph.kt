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
import com.example.focusapp.data.blocking.BlockedAppGroup
import com.example.focusapp.data.blocking.BlockedAppGroupStorage
import com.example.focusapp.data.blocking.defaultTimeSlot
import com.example.focusapp.data.notification.FocusTimerService
import com.example.focusapp.data.preferences.BackgroundThemeStorage
import com.example.focusapp.ui.screens.history.HistoryScreen
import com.example.focusapp.ui.screens.history.ThemePickerScreen
import com.example.focusapp.ui.screens.home.GroupUsageScreen
import com.example.focusapp.ui.screens.home.HomeScreenWithSheet
import com.example.focusapp.ui.screens.location.LocationScreen
import com.example.focusapp.ui.screens.party.FriendsScreen
import com.example.focusapp.ui.screens.session.ActiveFocusSession
import com.example.focusapp.ui.screens.session.FocusSessionScreen
import com.example.focusapp.ui.screens.session.FocusSessionSource
import com.example.focusapp.ui.screens.settings.SettingsScreen
import com.example.focusapp.ui.screens.timefocus.TimeFocusScreen
import com.example.focusapp.ui.screens.wififocus.WifiFocusScreen
import com.example.focusapp.ui.theme.BackgroundThemes
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------- Screen transitions ----------

private typealias Enter = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
private typealias Exit = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

// Tabs, Party Mode and Focus Mode fade in and out.
private val fadeEnter: Enter = { fadeIn(tween(300)) }
private val fadeExit: Exit = { fadeOut(tween(300)) }

// Pages opened from Home's corners slide up from the bottom (but fade when going to or from Home).
private fun AnimatedContentTransitionScope<NavBackStackEntry>.involvesHome(): Boolean =
    initialState.destination.route == Destinations.HOME || targetState.destination.route == Destinations.HOME

private val slideUpEnter: Enter = {
    if (involvesHome()) fadeIn(tween(300)) else slideInVertically { height -> height } + fadeIn()
}
private val slideDownExit: Exit = {
    if (involvesHome()) fadeOut(tween(300)) else slideOutVertically { height -> height } + fadeOut()
}

// ---------- Saved group lists ----------

/** A list of groups that is loaded from [BlockedAppGroupStorage] once, then saved on every change. */
private class SavedGroupList {
    var groups by mutableStateOf(emptyList<BlockedAppGroup>())

    fun updateGroup(groupId: String, change: (BlockedAppGroup) -> BlockedAppGroup) {
        groups = groups.map { group -> if (group.id == groupId) change(group) else group }
    }
}

@Composable
private fun rememberSavedGroupList(storage: BlockedAppGroupStorage): SavedGroupList {
    val list = remember { SavedGroupList() }
    var hasLoaded by remember { mutableStateOf(false) }

    // Load once.
    LaunchedEffect(Unit) {
        val saved = withContext(Dispatchers.IO) { storage.getGroups() }
        if (saved != null) list.groups = saved
        hasLoaded = true
    }

    // Save whenever the list changes (but not before it has been loaded).
    LaunchedEffect(list.groups, hasLoaded) {
        if (hasLoaded) withContext(Dispatchers.IO) { storage.saveGroups(list.groups) }
    }

    return list
}

// ---------- The app's navigation ----------

@Composable
fun FocusAppNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Each feature keeps its own group list.
    val schedule = rememberSavedGroupList(remember { BlockedAppGroupStorage(context) })
    val location = rememberSavedGroupList(remember { BlockedAppGroupStorage.forLocationGroups(context) })
    val wifi = rememberSavedGroupList(remember { BlockedAppGroupStorage.forWifiNetworks(context) })

    var activeFocusSession by remember { mutableStateOf<ActiveFocusSession?>(null) }

    // The picked background theme - shown on the dashboard, Home and Focus Mode.
    val themeStorage = remember { BackgroundThemeStorage(context) }
    var backgroundTheme by remember { mutableStateOf(BackgroundThemes.byId(themeStorage.getSelectedId())) }

    /** The apps a focus session blocks, depending on what started it. */
    fun blockedPackagesFor(source: FocusSessionSource): List<String> {
        val groups = when (source) {
            is FocusSessionSource.Location -> location.groups.filter { it.id == source.zoneId }
            is FocusSessionSource.Wifi -> wifi.groups.filter { it.id == source.ssid }
            FocusSessionSource.Manual -> location.groups // Quick Focus blocks every location's apps.
            FocusSessionSource.Party -> schedule.groups
        }
        return groups.flatMap { group -> group.apps.map { it.packageName } }.distinct()
    }

    fun startFocusSession(source: FocusSessionSource) {
        // Turn on app blocking. The reason is shown on the blocked screen.
        AccessibilityBridge.setRestrictedPackages(
            blockedPackagesFor(source),
            reason = when (source) {
                is FocusSessionSource.Location -> "Blocked while you're at ${source.zoneName}."
                is FocusSessionSource.Wifi -> "Blocked while you're connected to ${source.ssid} Wi-Fi."
                else -> null
            }
        )
        val startTimeMillis = System.currentTimeMillis()
        activeFocusSession = ActiveFocusSession(startTimeMillis = startTimeMillis, source = source)
        FocusTimerService.start(context, startTimeMillis) // Timer in the notification shade.
        navController.navigate(Destinations.FOCUS_SESSION)
    }

    fun endFocusSession() {
        AccessibilityBridge.clearRestrictedPackages()
        FocusTimerService.stop(context)
        // Leave the screen first, then clear the session, so the fading-out screen still has it.
        navController.popBackStack(Destinations.HOME, inclusive = false)
        activeFocusSession = null
    }

    // Home is the root; every other tab sits directly on top of it.
    fun navigateToTab(tab: MainTab) {
        val route = when (tab) {
            MainTab.HOME -> null
            MainTab.LOCATION -> Destinations.LOCATION
            MainTab.SCHEDULE -> Destinations.TIME_FOCUS
            MainTab.WIFI_SOURCE -> Destinations.WIFI
        }
        if (route == null) {
            navController.popBackStack(Destinations.HOME, inclusive = false)
        } else {
            navController.navigate(route) {
                popUpTo(Destinations.HOME)
                launchSingleTop = true
            }
        }
    }

    Scaffold(containerColor = FocusTheme.colors.background) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .clipToBounds()
        ) {
            composable(Destinations.HOME, enterTransition = fadeEnter, exitTransition = fadeExit) {
                HomeScreenWithSheet(
                    groups = schedule.groups,
                    dashboardArt = backgroundTheme.homeArt,
                    wifiSsids = wifi.groups.filter { it.enabled }.map { it.id },
                    onAvatarClick = { navController.navigate(Destinations.HISTORY) },
                    onFocusSessionStart = ::startFocusSession,
                    onScheduleBannerClick = { groupId -> navController.navigate(Destinations.groupUsageRoute(groupId)) },
                    onPartyModeClick = { navController.navigate(Destinations.FRIENDS) },
                    onSettingsClick = { navController.navigate(Destinations.SETTINGS) },
                    onLocationTabClick = { navigateToTab(MainTab.LOCATION) },
                    onScheduleTabClick = { navigateToTab(MainTab.SCHEDULE) },
                    onWifiTabClick = { navigateToTab(MainTab.WIFI_SOURCE) }
                )
            }

            composable(Destinations.LOCATION, enterTransition = fadeEnter, exitTransition = fadeExit) {
                LocationScreen(
                    onTabClick = ::navigateToTab,
                    blockedAppsFor = { zoneId -> location.groups.find { it.id == zoneId }?.apps.orEmpty() },
                    onZoneBlockedAppsChange = { zone, apps ->
                        if (location.groups.any { it.id == zone.id }) {
                            location.updateGroup(zone.id) { it.copy(name = zone.name, apps = apps) }
                        } else {
                            val newGroup = BlockedAppGroup(id = zone.id, name = zone.name, apps = apps, schedule = defaultTimeSlot())
                            location.groups = location.groups + newGroup
                        }
                    },
                    onZoneDeleted = { zoneId -> location.groups = location.groups.filterNot { it.id == zoneId } },
                    // Drop lists whose location no longer exists.
                    onZonesLoaded = { zoneIds -> location.groups = location.groups.filter { it.id in zoneIds } }
                )
            }

            composable(Destinations.TIME_FOCUS, enterTransition = fadeEnter, exitTransition = fadeExit) {
                TimeFocusScreen(
                    groups = schedule.groups,
                    onGroupsChange = { schedule.groups = it },
                    onTabClick = ::navigateToTab
                )
            }

            composable(Destinations.WIFI, enterTransition = fadeEnter, exitTransition = fadeExit) {
                WifiFocusScreen(
                    groups = wifi.groups,
                    onGroupsChange = { wifi.groups = it },
                    onTabClick = ::navigateToTab
                )
            }

            composable(Destinations.FRIENDS, enterTransition = fadeEnter, exitTransition = fadeExit) {
                FriendsScreen(
                    onClose = { navController.popBackStack() },
                    onStartFocus = { startFocusSession(FocusSessionSource.Party) }
                )
            }

            composable(Destinations.FOCUS_SESSION, enterTransition = fadeEnter, exitTransition = fadeExit) {
                // Remember the session once: while this screen fades out, activeFocusSession is already null.
                val session = remember { activeFocusSession }
                if (session != null) {
                    FocusSessionScreen(
                        session = session,
                        backgroundArt = backgroundTheme.focusArt,
                        onEndSessionClick = ::endFocusSession
                    )
                }
            }

            composable(
                route = Destinations.GROUP_USAGE,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
                enterTransition = slideUpEnter,
                exitTransition = slideDownExit
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")
                // Remember once, so the screen can still fade out if the group gets deleted.
                val group = remember { schedule.groups.find { it.id == groupId } }
                if (group != null) {
                    GroupUsageScreen(group = group, onBackClick = { navController.popBackStack() })
                }
            }

            composable(Destinations.SETTINGS, enterTransition = slideUpEnter, exitTransition = slideDownExit) {
                SettingsScreen(onClose = { navController.popBackStack() })
            }

            composable(Destinations.HISTORY, enterTransition = slideUpEnter, exitTransition = slideDownExit) {
                HistoryScreen(
                    theme = backgroundTheme,
                    onChangeThemeClick = { navController.navigate(Destinations.THEME_PICKER) },
                    onClose = { navController.popBackStack() }
                )
            }

            composable(Destinations.THEME_PICKER, enterTransition = slideUpEnter, exitTransition = slideDownExit) {
                ThemePickerScreen(
                    selectedId = backgroundTheme.id,
                    onSelect = { theme ->
                        backgroundTheme = theme
                        themeStorage.saveSelectedId(theme.id)
                        navController.popBackStack()
                    },
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}
