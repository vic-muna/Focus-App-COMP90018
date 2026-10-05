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
import com.example.focusapp.data.preferences.RewardSettingsStorage
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.usecase.CalculateFocusRewardUseCase
import com.example.focusapp.ui.components.card.FocusConfirmDialog
import com.example.focusapp.ui.theme.BackgroundTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.focusapp.data.account.AccountManager
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.accessibility.BlockSource
import com.example.focusapp.data.blocking.BlockedAppGroup
import com.example.focusapp.data.blocking.BlockedAppGroupStorage
import com.example.focusapp.data.blocking.defaultTimeSlot
import com.example.focusapp.data.blocking.watchedSsids
import com.example.focusapp.data.notification.FocusTimerService
import com.example.focusapp.data.preferences.BackgroundThemeStorage
import com.example.focusapp.data.sensor.MotionSensorDataSource
import com.example.focusapp.ui.common.vibrateShort
import com.example.focusapp.ui.screens.account.AccountScreen
import com.example.focusapp.ui.screens.history.HistoryScreen
import com.example.focusapp.ui.screens.history.ThemePickerScreen
import com.example.focusapp.ui.screens.home.HomeScreenWithSheet
import com.example.focusapp.ui.screens.location.LocationScreen
import com.example.focusapp.ui.screens.party.FriendsScreen
import com.example.focusapp.ui.screens.rewards.RewardsScreen
import com.example.focusapp.ui.screens.session.ActiveFocusSession
import com.example.focusapp.ui.screens.session.FocusSessionScreen
import com.example.focusapp.ui.screens.session.FocusSessionSource
import com.example.focusapp.ui.screens.settings.SettingsScreen
import com.example.focusapp.ui.screens.timefocus.TimeFocusScreen
import com.example.focusapp.ui.screens.wififocus.WifiFocusScreen
import com.example.focusapp.ui.theme.BackgroundThemes
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import com.example.focusapp.data.blocking.AppItem

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

/** How long the phone must stay face-down before Flip to Focus starts a session. */
private const val FLIP_HOLD_MILLIS = 2_000L

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

/**
 * The whole app's navigation.
 * [timeSlotToOpen]: a time slot to show (from a Time Focus notification); [onTimeSlotOpened] clears it.
 */
@Composable
fun FocusAppNavGraph(
    timeSlotToOpen: String? = null,
    onTimeSlotOpened: () -> Unit = {},
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Null for a guest (Account.isGuest) - HomeScreenWithSheet falls back to "Guest" in that
    // case. See that parameter's own doc comment for why this wasn't flowing through before.
    val account by AccountManager.account.collectAsState()

    // Each feature keeps its own group list.
    val schedule = rememberSavedGroupList(remember { BlockedAppGroupStorage(context) })
    val location = rememberSavedGroupList(remember { BlockedAppGroupStorage.forLocationGroups(context) })
    val wifi = rememberSavedGroupList(remember { BlockedAppGroupStorage.forWifiNetworks(context) })
    // Quick Focus keeps a single group: the apps it blocks.
    val quickFocus = rememberSavedGroupList(remember { BlockedAppGroupStorage.forQuickFocus(context) })
    // Flip to Focus keeps a single group too: on/off ("enabled") and the apps it blocks.
    val flipFocus = rememberSavedGroupList(remember { BlockedAppGroupStorage.forFlipFocus(context) })
    val flipFocusGroup = flipFocus.groups.firstOrNull()
        ?: BlockedAppGroup(id = "flip_focus", name = "Flip to Focus", apps = emptyList(), schedule = defaultTimeSlot(), enabled = false)

    var activeFocusSession by remember { mutableStateOf<ActiveFocusSession?>(null) }

    // The picked background theme - shown on the dashboard, Home and Focus Mode.
    val themeStorage = remember { BackgroundThemeStorage(context) }
    var backgroundTheme by remember { mutableStateOf(BackgroundThemes.byId(themeStorage.getSelectedId())) }
    // Longest daily streak so far - reward backgrounds unlock as it grows.
    var bestStreakDays by remember { mutableStateOf(0) }
    // Backgrounds unlocked with the test code instead of a streak.
    var codeUnlockedThemeIds by remember { mutableStateOf(themeStorage.getCodeUnlockedIds()) }
    // A background that has just unlocked: shown once in a dialog.
    var newlyUnlockedTheme by remember { mutableStateOf<BackgroundTheme?>(null) }

    fun saveQuickFocusApps(apps: List<AppItem>) {
        quickFocus.groups = listOf(
            BlockedAppGroup(id = "quick_focus", name = "Quick Focus", apps = apps, schedule = defaultTimeSlot())
        )
    }

    /** The apps a focus session blocks, depending on what started it. */
    fun blockedPackagesFor(source: FocusSessionSource): List<String> {
        val groups = when (source) {
            is FocusSessionSource.Location -> location.groups.filter { it.id == source.zoneId }
            // The Wi-Fi's apps are already blocked by the App Blocking service for as long as the
            // phone is on it (see FocusAccessibilityService), so this session only records the time.
            is FocusSessionSource.Wifi -> emptyList()
            // Quick Focus and Party Mode block the apps picked for Quick Focus.
            FocusSessionSource.Manual, FocusSessionSource.Party -> quickFocus.groups
            FocusSessionSource.Flip -> listOf(flipFocusGroup)
        }
        return groups.flatMap { group -> group.apps.map { it.packageName } }.distinct()
    }

    fun startFocusSession(source: FocusSessionSource) {
        // Turn on app blocking. The reason is shown on the blocked screen.
        AccessibilityBridge.setBlocks(
            BlockSource.SESSION,
            blockedPackagesFor(source),
            reason = when (source) {
                is FocusSessionSource.Location -> "Blocked while you're at ${source.zoneName}."
                else -> null
            }
        )
        AccessibilityBridge.startCountingBlockedOpens() // Saved with the session, shown in History.
        val startTimeMillis = System.currentTimeMillis()
        activeFocusSession = ActiveFocusSession(startTimeMillis = startTimeMillis, source = source)
        FocusTimerService.start(context, startTimeMillis) // Timer in the notification shade.
        navController.navigate(Destinations.FOCUS_SESSION)
    }

    fun endFocusSession() {
        AccessibilityBridge.clearBlocks(BlockSource.SESSION) // Location and Wi-Fi blocks stay.
        AccessibilityBridge.stopCountingBlockedOpens()
        FocusTimerService.stop(context)
        // Leave the screen first, then clear the session, so the fading-out screen still has it.
        navController.popBackStack(Destinations.HOME, inclusive = false)
        activeFocusSession = null
    }

    // Flip to Focus: while the app is open and no session is running, lying the phone
    // face-down for a moment starts a session that blocks the Flip to Focus apps.
    // Like Quick Focus, it needs App Blocking on and at least one app picked.
    val isAppBlockingOn by AccessibilityBridge.isServiceConnected.collectAsState()
    val canFlipToFocus = activeFocusSession == null && isAppBlockingOn &&
        flipFocusGroup.enabled && flipFocusGroup.apps.isNotEmpty()
    val lifecycleOwner = LocalLifecycleOwner.current
    // Keyed on the apps too, so a session started by this effect blocks the latest pick.
    LaunchedEffect(canFlipToFocus, flipFocusGroup.apps, lifecycleOwner) {
        if (!canFlipToFocus) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            MotionSensorDataSource(context).isFaceDownFlow().collectLatest { isFaceDown ->
                if (!isFaceDown) return@collectLatest
                // Cancelled if the phone is picked up again before the hold ends.
                delay(FLIP_HOLD_MILLIS)
                vibrateShort(context)
                startFocusSession(FocusSessionSource.Flip)
            }
        }
    }

    fun saveFlipFocus(change: (BlockedAppGroup) -> BlockedAppGroup) {
        flipFocus.groups = listOf(change(flipFocusGroup))
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

    // A slot to show on the Time Focus tab (from a Time Focus notification).
    var timeSlotToShow by remember { mutableStateOf<String?>(null) }

    // Re-read when the app opens and every time a session ends: the session that just
    // finished may have unlocked a background.
    val isSessionRunning = activeFocusSession != null
    LaunchedEffect(isSessionRunning) {
        if (isSessionRunning) return@LaunchedEffect
        val sessions = runCatching { FocusRepositoryProvider.get(context).getSessionHistory() }.getOrNull()
            ?: return@LaunchedEffect
        val streakGoalMinutes = RewardSettingsStorage(context).getStreakGoalMinutes()
        bestStreakDays = CalculateFocusRewardUseCase().execute(sessions, streakGoalMinutes).bestStreakDays
        val announcedIds = themeStorage.getAnnouncedIds()
        newlyUnlockedTheme = BackgroundThemes.all.firstOrNull {
            it.unlockStreakDays > 0 && it.isUnlocked(bestStreakDays) && it.id !in announcedIds
        }
    }

    newlyUnlockedTheme?.let { theme ->
        fun close() {
            themeStorage.saveAnnounced(theme.id)
            newlyUnlockedTheme = null
        }
        FocusConfirmDialog(
            title = "New background unlocked!",
            message = "You reached your daily goal ${theme.unlockStreakDays} days in a row. " +
                "\"${theme.name}\" is now yours - ${theme.intro.replaceFirstChar { it.lowercase() }}.",
            confirmLabel = "Use it now",
            dismissLabel = "Later",
            onConfirm = {
                backgroundTheme = theme
                themeStorage.saveSelectedId(theme.id)
                close()
            },
            onDismiss = ::close,
        )
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
                    userName = account?.username,
                    groups = schedule.groups,
                    dashboardArt = backgroundTheme.homeArt,
                    wifiSsids = wifi.groups.filter { it.enabled }.flatMap { it.watchedSsids }.distinct(),
                    quickFocusApps = quickFocus.groups.firstOrNull()?.apps.orEmpty(),
                    onQuickFocusAppsChange = ::saveQuickFocusApps,
                    onAvatarClick = { navController.navigate(Destinations.HISTORY) },
                    onFocusSessionStart = ::startFocusSession,
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
                    isZoneEnabled = { zoneId -> location.groups.find { it.id == zoneId }?.enabled ?: true },
                    onZoneBlockedAppsChange = { zone, apps ->
                        if (location.groups.any { it.id == zone.id }) {
                            location.updateGroup(zone.id) { it.copy(name = zone.name, apps = apps) }
                        } else {
                            val newGroup = BlockedAppGroup(id = zone.id, name = zone.name, apps = apps, schedule = defaultTimeSlot())
                            location.groups = location.groups + newGroup
                        }
                    },
                    onZoneEnabledToggle = { zoneId, enabled ->
                        if (location.groups.any { it.id == zoneId }) {
                            location.updateGroup(zoneId) { it.copy(enabled = enabled) }
                        } else {
                            val newGroup = BlockedAppGroup(id = zoneId, name = "", apps = emptyList(), schedule = defaultTimeSlot(), enabled = enabled)
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
                    headerArt = backgroundTheme.timeFocusArt,
                    groupToShow = timeSlotToShow,
                    onGroupShown = { timeSlotToShow = null },
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
                    quickFocusApps = quickFocus.groups.firstOrNull()?.apps.orEmpty(),
                    onQuickFocusAppsChange = ::saveQuickFocusApps,
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

            composable(Destinations.SETTINGS, enterTransition = slideUpEnter, exitTransition = slideDownExit) {
                SettingsScreen(
                    onClose = { navController.popBackStack() },
                    onCreateAccountClick = { navController.navigate(Destinations.CREATE_ACCOUNT) },
                    flipFocusOn = flipFocusGroup.enabled,
                    onFlipFocusOnChange = { on -> saveFlipFocus { it.copy(enabled = on) } },
                    flipFocusApps = flipFocusGroup.apps,
                    onFlipFocusAppsChange = { apps -> saveFlipFocus { it.copy(apps = apps) } },
                )
            }

            composable(Destinations.CREATE_ACCOUNT, enterTransition = slideUpEnter, exitTransition = slideDownExit) {
                AccountScreen(
                    upgradingGuest = true,
                    onClose = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }

            composable(Destinations.HISTORY, enterTransition = slideUpEnter, exitTransition = slideDownExit) {
                HistoryScreen(
                    theme = backgroundTheme,
                    onChangeThemeClick = { navController.navigate(Destinations.THEME_PICKER) },
                    onRewardsClick = { navController.navigate(Destinations.REWARDS) },
                    onClose = { navController.popBackStack() }
                )
            }

            composable(Destinations.REWARDS, enterTransition = slideUpEnter, exitTransition = slideDownExit) {
                RewardsScreen(onClose = { navController.popBackStack() })
            }

            composable(Destinations.THEME_PICKER, enterTransition = slideUpEnter, exitTransition = slideDownExit) {
                ThemePickerScreen(
                    selectedId = backgroundTheme.id,
                    bestStreakDays = bestStreakDays,
                    codeUnlockedIds = codeUnlockedThemeIds,
                    onUnlockWithCode = { theme ->
                        themeStorage.saveCodeUnlocked(theme.id)
                        themeStorage.saveAnnounced(theme.id) // Already unlocked: no "unlocked" dialog later.
                        codeUnlockedThemeIds = themeStorage.getCodeUnlockedIds()
                    },
                    onSelect = { theme ->
                        backgroundTheme = theme
                        themeStorage.saveSelectedId(theme.id)
                        navController.popBackStack()
                    },
                    onClose = { navController.popBackStack() }
                )
            }
        }

        // Opened from a Time Focus notification: go to the Time Focus tab, which then shows the slot.
        // Placed after NavHost so its screens exist before navigating.
        LaunchedEffect(timeSlotToOpen) {
            if (timeSlotToOpen != null) {
                timeSlotToShow = timeSlotToOpen
                onTimeSlotOpened()
                navigateToTab(MainTab.SCHEDULE)
            }
        }
    }
}
