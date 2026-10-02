package com.example.focusapp.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.focusapp.R
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.account.AccountManager
import com.example.focusapp.ui.components.card.FocusConfirmDialog
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
}

/** The system settings page where [permission] is turned on or off. */
private fun settingsIntentFor(context: Context, permission: AppPermission): Intent = when (permission) {
    AppPermission.PRECISE_LOCATION ->
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
 *  - Music: Focus Music / Home Music switches (TODO: no music player yet -
 *    they only remember their position while the screen is open)
 *  - Permissions: whether each permission is on; tapping a row opens the
 *    system page to change it, and the switches refresh on coming back
 */
@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    onCreateAccountClick: () -> Unit = {},
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

    // Re-read every time the screen comes back - the user may have just
    // changed a permission on the system page a row opened.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            preciseLocationOn = hasLocationPermissionForWifi(context)
            usageAccessOn = hasUsageAccessPermission(context)
            notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

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
            }
        },
        onPermissionClick = { context.startActivity(settingsIntentFor(context, it)) },
        focusMusicOn = focusMusicOn,
        onFocusMusicChange = { focusMusicOn = it },
        homeMusicOn = homeMusicOn,
        onHomeMusicChange = { homeMusicOn = it },
        onClose = onClose,
    )
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
            focusMusicOn = false,
            onFocusMusicChange = {},
            homeMusicOn = true,
            onHomeMusicChange = {},
            onClose = {},
        )
    }
}