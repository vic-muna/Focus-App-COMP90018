package com.example.focusapp.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.focusapp.R
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.account.AccountManager
import com.example.focusapp.data.blocking.AppItem
import com.example.focusapp.data.preferences.NoiseAlertStorage
import com.example.focusapp.data.preferences.RewardSettingsStorage
import com.example.focusapp.domain.model.formatMinutes
import com.example.focusapp.ui.common.AccessibilityPermissionDialog
import com.example.focusapp.ui.components.card.FocusConfirmDialog
import com.example.focusapp.ui.components.card.FlyCardOverlay
import com.example.focusapp.ui.components.card.consumeTaps
import kotlinx.coroutines.launch
import com.example.focusapp.data.usagestats.hasUsageAccessPermission
import com.example.focusapp.data.wifi.hasLocationPermissionForWifi
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusSpacing
import com.example.focusapp.ui.theme.FocusTheme
import com.example.focusapp.ui.components.bar.SettingsRow
import com.example.focusapp.ui.components.bar.SettingsSection
import com.example.focusapp.ui.components.bar.SettingsTopBar
import com.example.focusapp.ui.components.input.FocusSwitch
import com.example.focusapp.ui.common.rememberMicrophonePermissionState

/**
 * The permissions this app asks for. An app can't switch these on or off
 * by itself - each row opens the matching system settings page, where the
 * user flips it.
 */
private enum class AppPermission(val label: String) {
    PRECISE_LOCATION("Precise Location"),
    APP_BLOCKING("App Blocking"),
    USAGE_ACCESS("Usage Access"),
    NOTIFICATIONS("Notifications"),
    MICROPHONE("Microphone"),
}

/** The system settings page where [permission] is turned on or off. */
private fun settingsIntentFor(context: Context, permission: AppPermission): Intent = when (permission) {
    AppPermission.PRECISE_LOCATION, AppPermission.MICROPHONE ->
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    AppPermission.APP_BLOCKING -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    AppPermission.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    AppPermission.NOTIFICATIONS ->
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
}

/**
 * Figma: "Settings". Reached from Home's gear; the same spot shows an X
 * that closes it ([onClose]).
 *  - Account: who is signed in; a guest can create an account
 *    ([onCreateAccountClick]); Log out returns to the login screen
 *  - Rewards: the daily focus time a day needs to count for the streak
 *  - Noise Alert: the "too loud" banner in Focus Mode, and its threshold
 *  - Flip to Focus: lying the phone face-down starts a session; its own
 *    list of apps to block (kept in NavGraph.kt, like Quick Focus's)
 *  - Music: Focus Music / Home Music switches (TODO: no music player yet -
 *    they only remember their position while the screen is open)
 *  - Permissions: whether each permission is on; tapping a row opens the
 *    system page to change it, and the switches refresh on coming back
 */
@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    onCreateAccountClick: () -> Unit = {},
    flipFocusOn: Boolean = false,
    onFlipFocusOnChange: (Boolean) -> Unit = {},
    flipFocusApps: List<AppItem> = emptyList(),
    onFlipFocusAppsChange: (List<AppItem>) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val account by AccountManager.account.collectAsState()
    var showLogOutDialog by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }
    val appBlockingOn by AccessibilityBridge.isServiceConnected.collectAsState()
    var preciseLocationOn by remember { mutableStateOf(false) }
    var usageAccessOn by remember { mutableStateOf(false) }
    var notificationsOn by remember { mutableStateOf(false) }
    var microphoneOn by remember { mutableStateOf(false) }
    val micPermission = rememberMicrophonePermissionState()

    // Re-read every time the screen comes back - the user may have just
    // changed a permission on the system page a row opened.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            preciseLocationOn = hasLocationPermissionForWifi(context)
            usageAccessOn = hasUsageAccessPermission(context)
            notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
            microphoneOn = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    val rewardSettings = remember { RewardSettingsStorage(context) }
    var streakGoalMinutes by remember { mutableIntStateOf(rewardSettings.getStreakGoalMinutes()) }

    val noiseAlertSettings = remember { NoiseAlertStorage(context) }
    var noiseAlertOn by remember { mutableStateOf(noiseAlertSettings.isEnabled()) }
    var noiseThresholdDb by remember { mutableIntStateOf(noiseAlertSettings.getThresholdDb()) }

    var showFlipFocusAppsPicker by remember { mutableStateOf(false) }
    var showAccessibilityPermissionDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = showFlipFocusAppsPicker) { showFlipFocusAppsPicker = false }

    // TODO(music): wire to the music player once the app has one (and persist these).
    var focusMusicOn by rememberSaveable { mutableStateOf(false) }
    var homeMusicOn by rememberSaveable { mutableStateOf(false) }

    // Logging out swaps the whole app for the login screen (see MainActivity).
    if (showLogOutDialog) {
        val isGuest = account?.isGuest == true
        FocusConfirmDialog(
            title = "Log out?",
            message = if (isGuest) {
                "You're a guest, so your data can't be recovered after logging out. Create an account first to keep it."
            } else {
                "Your data is backed up to your account. Log in again on any phone to get it back."
            },
            confirmLabel = "Log out",
            confirmColor = FocusTheme.colors.rejection,
            onConfirm = {
                showLogOutDialog = false
                isLoggingOut = true
                scope.launch { AccountManager.logOut(context) }
            },
            onDismiss = { showLogOutDialog = false },
        )
    }

    if (showAccessibilityPermissionDialog) {
        AccessibilityPermissionDialog(onDismiss = { showAccessibilityPermissionDialog = false })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsContent(
            username = account?.username,
            isLoggingOut = isLoggingOut,
            onCreateAccountClick = onCreateAccountClick,
            onLogOutClick = { showLogOutDialog = true },
            isPermissionOn = { permission ->
                when (permission) {
                    AppPermission.PRECISE_LOCATION -> preciseLocationOn
                    AppPermission.APP_BLOCKING -> appBlockingOn
                    AppPermission.USAGE_ACCESS -> usageAccessOn
                    AppPermission.NOTIFICATIONS -> notificationsOn
                    AppPermission.MICROPHONE -> microphoneOn
                }
            },
            onPermissionClick = { context.startActivity(settingsIntentFor(context, it)) },
            streakGoalMinutes = streakGoalMinutes,
            onStreakGoalChange = {
                streakGoalMinutes = it
                rewardSettings.saveStreakGoalMinutes(it)
            },
            noiseAlertOn = noiseAlertOn,
            onNoiseAlertChange = {
                noiseAlertOn = it
                noiseAlertSettings.saveEnabled(it)
                if (it && !microphoneOn) micPermission.request()
            },
            noiseThresholdDb = noiseThresholdDb,
            onNoiseThresholdChange = {
                noiseThresholdDb = it
                noiseAlertSettings.saveThresholdDb(it)
            },
            flipFocusOn = flipFocusOn,
            onFlipFocusOnChange = {
                onFlipFocusOnChange(it)
                // Flip to Focus can't block apps without App Blocking, same as Quick Focus.
                if (it && !appBlockingOn) showAccessibilityPermissionDialog = true
            },
            flipFocusAppCount = flipFocusApps.size,
            onFlipFocusAppsClick = { showFlipFocusAppsPicker = true },
            focusMusicOn = focusMusicOn,
            onFocusMusicChange = { focusMusicOn = it },
            homeMusicOn = homeMusicOn,
            onHomeMusicChange = { homeMusicOn = it },
            onClose = onClose,
        )

        if (showFlipFocusAppsPicker) {
            FlyCardOverlay(onOutsideClick = { showFlipFocusAppsPicker = false }) {
                FlipFocusAppsCard(
                    savedApps = flipFocusApps,
                    onSave = { apps ->
                        showFlipFocusAppsPicker = false
                        onFlipFocusAppsChange(apps)
                    },
                    onClose = { showFlipFocusAppsPicker = false },
                    modifier = Modifier.consumeTaps(),
                )
            }
        }
    }
}

/** Stateless layout of [SettingsScreen]. */
@Composable
private fun SettingsContent(
    username: String?,
    isLoggingOut: Boolean,
    onCreateAccountClick: () -> Unit,
    onLogOutClick: () -> Unit,
    isPermissionOn: (AppPermission) -> Boolean,
    onPermissionClick: (AppPermission) -> Unit,
    streakGoalMinutes: Int,
    onStreakGoalChange: (Int) -> Unit,
    noiseAlertOn: Boolean,
    onNoiseAlertChange: (Boolean) -> Unit,
    noiseThresholdDb: Int,
    onNoiseThresholdChange: (Int) -> Unit,
    flipFocusOn: Boolean,
    onFlipFocusOnChange: (Boolean) -> Unit,
    flipFocusAppCount: Int,
    onFlipFocusAppsClick: () -> Unit,
    focusMusicOn: Boolean,
    onFocusMusicChange: (Boolean) -> Unit,
    homeMusicOn: Boolean,
    onHomeMusicChange: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val colors = FocusTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, top = FocusSpacing.ScreenTop, bottom = FocusSpacing.ScreenBottom),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            // Same height as the X in the top-right corner, so the title lines up with it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Settings",
                    style = FocusTheme.typography.primaryActionLabel,
                    color = colors.onSurface,
                )
            }

            // TODO(design): placeholder art - swap in the Settings banner once it's exported.
            Image(
                painter = painterResource(R.drawable.img_app_focus_header),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(24.dp)),
            )

            // username == null means a guest.
            SettingsSection(title = "Account", icon = Icons.Filled.AccountCircle) {
                SettingsRow(label = "Signed in as") {
                    Text(
                        text = username ?: "Guest",
                        style = FocusTheme.typography.rowLabel,
                        color = colors.accent,
                    )
                }
                if (username == null) {
                    SettingsRow(label = "Create account", onClick = onCreateAccountClick) { RowArrow() }
                }
                SettingsRow(
                    label = if (isLoggingOut) "Logging out…" else "Log out",
                    onClick = if (isLoggingOut) null else onLogOutClick,
                ) { RowArrow() }
            }

            // A day counts for the Rewards streak once it has this much focus time.
            SettingsSection(title = "Rewards", icon = Icons.Filled.EmojiEvents) {
                SettingsRow(label = "Daily streak goal") {
                    GoalStepper(minutes = streakGoalMinutes, onMinutesChange = onStreakGoalChange)
                }
            }

            // Focus Mode shows a "too loud" banner when the room's average level passes the threshold.
            SettingsSection(title = "Noise Alert", icon = Icons.AutoMirrored.Filled.VolumeUp) {
                SettingsRow(label = "Too-loud alert") {
                    FocusSwitch(checked = noiseAlertOn, onCheckedChange = onNoiseAlertChange)
                }
                if (noiseAlertOn) {
                    SettingsRow(label = "Threshold") {
                        ThresholdStepper(thresholdDb = noiseThresholdDb, onThresholdChange = onNoiseThresholdChange)
                    }
                }
            }

            // Lying the phone face-down while the app is open starts a session that blocks these apps.
            SettingsSection(title = "Flip to Focus", icon = Icons.Filled.Flip) {
                SettingsRow(label = "Start when face-down") {
                    FocusSwitch(checked = flipFocusOn, onCheckedChange = onFlipFocusOnChange)
                }
                if (flipFocusOn) {
                    SettingsRow(label = "Apps to block", onClick = onFlipFocusAppsClick) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (flipFocusAppCount) {
                                    0 -> "None"
                                    1 -> "1 app"
                                    else -> "$flipFocusAppCount apps"
                                },
                                style = FocusTheme.typography.rowLabel,
                                color = colors.accent,
                            )
                            RowArrow()
                        }
                    }
                }
            }

            SettingsSection(title = "Music", icon = Icons.Filled.MusicNote) {
                SettingsRow(label = "Focus Music") {
                    FocusSwitch(checked = focusMusicOn, onCheckedChange = onFocusMusicChange)
                }
                SettingsRow(label = "Home Music") {
                    FocusSwitch(checked = homeMusicOn, onCheckedChange = onHomeMusicChange)
                }
            }

            SettingsSection(title = "Permissions", icon = Icons.Filled.Security) {
                AppPermission.entries.forEach { permission ->
                    SettingsRow(label = permission.label, onClick = { onPermissionClick(permission) }) {
                        // Shows the current state; changing it happens on the system page.
                        FocusSwitch(
                            checked = isPermissionOn(permission),
                            onCheckedChange = { onPermissionClick(permission) },
                        )
                    }
                }
            }
        }

        SettingsTopBar(onSettingsClick = onClose, isOpen = true)
    }
}

/** "−  2h  +": changes the streak goal in [RewardSettingsStorage.GOAL_STEP_MINUTES] steps. */
@Composable
private fun GoalStepper(minutes: Int, onMinutesChange: (Int) -> Unit) {
    ValueStepper(
        text = formatMinutes(minutes.toLong()),
        what = "goal",
        value = minutes,
        step = RewardSettingsStorage.GOAL_STEP_MINUTES,
        range = RewardSettingsStorage.MIN_GOAL_MINUTES..RewardSettingsStorage.MAX_GOAL_MINUTES,
        onValueChange = onMinutesChange,
    )
}

/** "−  55 dB  +": changes the noise threshold in [NoiseAlertStorage.THRESHOLD_STEP_DB] steps. */
@Composable
private fun ThresholdStepper(thresholdDb: Int, onThresholdChange: (Int) -> Unit) {
    ValueStepper(
        text = "$thresholdDb dB",
        what = "threshold",
        value = thresholdDb,
        step = NoiseAlertStorage.THRESHOLD_STEP_DB,
        range = NoiseAlertStorage.MIN_THRESHOLD_DB..NoiseAlertStorage.MAX_THRESHOLD_DB,
        onValueChange = onThresholdChange,
    )
}

/** "−  [text]  +": moves [value] by [step] within [range]. [what] names it for screen readers. */
@Composable
private fun ValueStepper(
    text: String,
    what: String,
    value: Int,
    step: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(
            icon = Icons.Filled.Remove,
            contentDescription = "Lower $what",
            enabled = value > range.first,
            onClick = { onValueChange(value - step) },
        )
        Text(
            text = text,
            style = FocusTheme.typography.rowLabel,
            color = FocusTheme.colors.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp),
        )
        StepButton(
            icon = Icons.Filled.Add,
            contentDescription = "Raise $what",
            enabled = value < range.last,
            onClick = { onValueChange(value + step) },
        )
    }
}

@Composable
private fun StepButton(icon: ImageVector, contentDescription: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) FocusTheme.colors.onSurface else FocusTheme.colors.onSurfaceMuted,
        )
    }
}

/** The ">" at the end of a row that opens something. */
@Composable
private fun RowArrow() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = FocusTheme.colors.onSurfaceMuted,
    )
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun SettingsContentPreview() {
    FocusAppTheme {
        SettingsContent(
            username = null,
            isLoggingOut = false,
            onCreateAccountClick = {},
            onLogOutClick = {},
            isPermissionOn = { it == AppPermission.PRECISE_LOCATION || it == AppPermission.NOTIFICATIONS },
            onPermissionClick = {},
            streakGoalMinutes = 120,
            onStreakGoalChange = {},
            noiseAlertOn = true,
            onNoiseAlertChange = {},
            noiseThresholdDb = NoiseAlertStorage.DEFAULT_THRESHOLD_DB,
            onNoiseThresholdChange = {},
            flipFocusOn = true,
            onFlipFocusOnChange = {},
            flipFocusAppCount = 3,
            onFlipFocusAppsClick = {},
            focusMusicOn = false,
            onFocusMusicChange = {},
            homeMusicOn = true,
            onHomeMusicChange = {},
            onClose = {},
        )
    }
}