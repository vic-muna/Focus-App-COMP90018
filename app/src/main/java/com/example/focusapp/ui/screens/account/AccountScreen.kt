package com.example.focusapp.ui.screens.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.data.account.AccountManager
import com.example.focusapp.ui.components.bar.CloseTopBar
import com.example.focusapp.ui.components.button.FocusPillButton
import com.example.focusapp.ui.components.card.CardLabel
import com.example.focusapp.ui.components.card.CardTitle
import com.example.focusapp.ui.components.card.FocusCard
import com.example.focusapp.ui.components.input.FocusTextField
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusSpacing
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Log in, create an account, or continue as a guest - see [AccountManager].
 *
 * Shown in two places:
 *  - when nobody is signed in (app start, after logging out): all three choices
 *  - from Settings for a guest ([upgradingGuest]): only "Create account", which
 *    keeps the guest's data; [onClose] closes it and [onDone] runs once it worked
 */
@Composable
fun AccountScreen(
    upgradingGuest: Boolean = false,
    onClose: (() -> Unit)? = null,
    onDone: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isSignUp by rememberSaveable { mutableStateOf(upgradingGuest) }
    var username by rememberSaveable { mutableStateOf("") }
    // Passwords aren't saved across process death on purpose.
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    // Runs one sign-in action, showing its error (if any) on the card.
    fun launchAuth(action: suspend () -> Unit) {
        errorMessage = null
        isBusy = true
        scope.launch {
            try {
                action()
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = AccountManager.errorMessageFor(e)
            } finally {
                isBusy = false
            }
        }
    }

    fun submit() {
        val name = AccountManager.normalizeUsername(username)
        val problem = AccountManager.usernameProblem(name) ?: when {
            password.isEmpty() -> "Enter your password."
            isSignUp -> AccountManager.passwordProblem(password)
                ?: if (password != confirmPassword) "Passwords don't match." else null
            else -> null
        }
        if (problem != null) {
            errorMessage = problem
            return
        }
        launchAuth {
            if (isSignUp) AccountManager.createAccount(context, name, password)
            else AccountManager.logIn(context, name, password)
        }
    }

    AccountContent(
        upgradingGuest = upgradingGuest,
        isSignUp = isSignUp,
        onModeChange = {
            isSignUp = it
            errorMessage = null
            confirmPassword = ""
        },
        username = username,
        onUsernameChange = { username = it },
        password = password,
        onPasswordChange = { password = it },
        confirmPassword = confirmPassword,
        onConfirmPasswordChange = { confirmPassword = it },
        errorMessage = errorMessage,
        isBusy = isBusy,
        onSubmit = ::submit,
        onGuestClick = { launchAuth { AccountManager.continueAsGuest(context) } },
        onClose = onClose,
    )
}

/** Stateless layout of [AccountScreen]. Laid out like Settings: title, banner, then a card. */
@Composable
private fun AccountContent(
    upgradingGuest: Boolean,
    isSignUp: Boolean,
    onModeChange: (isSignUp: Boolean) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    errorMessage: String?,
    isBusy: Boolean,
    onSubmit: () -> Unit,
    onGuestClick: () -> Unit,
    onClose: (() -> Unit)?,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, top = FocusSpacing.ScreenTop, bottom = FocusSpacing.ScreenBottom),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            // Same height as the corner X, so the title lines up with it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (upgradingGuest) "Create account" else "Welcome",
                    style = typography.primaryActionLabel,
                    color = colors.onSurface,
                )
            }

            Image(
                painter = painterResource(R.drawable.img_app_focus_header),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(24.dp)),
            )

            FocusCard {
                CardTitle(if (isSignUp) "Create account" else "Log in", bottomPadding = 0.dp)
                if (upgradingGuest) {
                    AccountNote("Everything you've saved as a guest stays with your new account.")
                }

                FieldLabel("Username")
                FocusTextField(
                    value = username,
                    onValueChange = { onUsernameChange(it.trim()) },
                    placeholder = "Enter Username",
                    keyboardOptions = KeyboardOptions(autoCorrect = false,imeAction = ImeAction.Next),
                )
                FieldLabel("Password")
                FocusTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    placeholder = "Enter Password",
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done),
                )
                if (isSignUp) {
                    FieldLabel("Confirm password")
                    FocusTextField(
                        value = confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        placeholder = "Enter Password Again",
                        isPassword = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                    AccountNote("There's no email, so a forgotten password can't be reset.")
                }

                errorMessage?.let { AccountError(it) }

                FocusPillButton(
                    label = when {
                        isBusy -> "Please wait…"
                        isSignUp -> "Create account"
                        else -> "Log in"
                    },
                    containerColor = colors.primaryAction,
                    contentColor = colors.onPrimaryAction,
                    onClick = onSubmit,
                    enabled = !isBusy,
                    height = 48.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                )

                if (!upgradingGuest) {
                    Text(
                        text = if (isSignUp) "Already have an account? Log in" else "No account yet? Create one",
                        style = typography.caption,
                        color = colors.accent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clickable(enabled = !isBusy, role = Role.Button) { onModeChange(!isSignUp) }
                            .padding(vertical = 8.dp),
                    )
                }
            }

            if (!upgradingGuest) {
                Column {
                    FocusPillButton(
                        label = "Continue as guest",
                        containerColor = colors.surface,
                        contentColor = colors.onSurface,
                        onClick = onGuestClick,
                        enabled = !isBusy,
                        height = 48.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AccountNote("Guest data stays on this phone only, and is lost if the app is reinstalled.")
                }
            }
        }

        if (onClose != null) {
            CloseTopBar(contentDescription = "Close", onCloseClick = onClose, alignment = Alignment.TopEnd)
        }
    }
}

/** A small grey label above a text box. */
@Composable
private fun FieldLabel(text: String) = CardLabel(text, topPadding = 16.dp, bottomPadding = 8.dp)

/** A small centered grey note. */
@Composable
private fun AccountNote(text: String) {
    Text(
        text = text,
        style = FocusTheme.typography.caption,
        color = FocusTheme.colors.onSurfaceMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

/** A small centered error message. */
@Composable
private fun AccountError(text: String) {
    Text(
        text = text,
        style = FocusTheme.typography.caption,
        color = FocusTheme.colors.rejection,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    )
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun AccountContentLogInPreview() {
    FocusAppTheme {
        AccountContent(
            upgradingGuest = false,
            isSignUp = false,
            onModeChange = {},
            username = "david",
            onUsernameChange = {},
            password = "secret",
            onPasswordChange = {},
            confirmPassword = "",
            onConfirmPasswordChange = {},
            errorMessage = "Wrong username or password.",
            isBusy = false,
            onSubmit = {},
            onGuestClick = {},
            onClose = null,
        )
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun AccountContentUpgradePreview() {
    FocusAppTheme {
        AccountContent(
            upgradingGuest = true,
            isSignUp = true,
            onModeChange = {},
            username = "",
            onUsernameChange = {},
            password = "",
            onPasswordChange = {},
            confirmPassword = "",
            onConfirmPasswordChange = {},
            errorMessage = null,
            isBusy = false,
            onSubmit = {},
            onGuestClick = {},
            onClose = {},
        )
    }
}
