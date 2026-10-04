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

    // [Claude, 2026-10-04] Sign-up's security question (see AccountManager.createAccount) -
    // a preset list rather than a free-text question, so nobody accidentally picks something
    // trivially guessable like "What's 1+1?".
    var securityQuestion by rememberSaveable { mutableStateOf(SECURITY_QUESTIONS.first()) }
    var securityAnswer by remember { mutableStateOf("") }

    // "Forgot password?" is a separate little flow, not a third isSignUp-style mode for the
    // whole screen - see ForgotPasswordContent below, which now owns all of its OWN fields
    // internally (username, question, answer, etc.) rather than this screen hoisting a single
    // combined state object for it. This is deliberately simpler than the first version of this
    // (which did hoist one ForgotPasswordState and update it via .copy()) - that version had a
    // real, reported "type a username, it clears itself" bug, and a rememberUpdatedState fix
    // for the specific race I could find did NOT make it go away, which means the actual
    // mechanism was something else I wasn't seeing. Rather than keep guessing at the exact
    // mechanism, removing the single-combined-object-threaded-through-copy() pattern entirely
    // removes the whole category of "one field's update clobbers another's" bugs it enables -
    // each field below now has its own independent MutableState, set directly, never
    // reconstructed via a stale copy() of something else.
    var forgotPasswordOpen by remember { mutableStateOf(false) }

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
                ?: when {
                    password != confirmPassword -> "Passwords don't match."
                    securityAnswer.isBlank() -> "Answer your security question - it's the only way to recover your password later."
                    else -> null
                }
            else -> null
        }
        if (problem != null) {
            errorMessage = problem
            return
        }
        launchAuth {
            if (isSignUp) {
                AccountManager.createAccount(context, name, password, securityQuestion, securityAnswer)
            } else {
                AccountManager.logIn(context, name, password)
            }
        }
    }

    if (forgotPasswordOpen) {
        ForgotPasswordContent(
            onClose = { forgotPasswordOpen = false },
            onRecovered = { forgotPasswordOpen = false; onDone() },
        )
    } else {
        AccountContent(
            upgradingGuest = upgradingGuest,
            isSignUp = isSignUp,
            onModeChange = {
                isSignUp = it
                errorMessage = null
                confirmPassword = ""
                securityAnswer = ""
            },
            username = username,
            onUsernameChange = { username = it },
            password = password,
            onPasswordChange = { password = it },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = { confirmPassword = it },
            securityQuestion = securityQuestion,
            onSecurityQuestionChange = { securityQuestion = it },
            securityAnswer = securityAnswer,
            onSecurityAnswerChange = { securityAnswer = it },
            errorMessage = errorMessage,
            isBusy = isBusy,
            onSubmit = ::submit,
            onGuestClick = { launchAuth { AccountManager.continueAsGuest(context) } },
            onForgotPasswordClick = { forgotPasswordOpen = true },
            onClose = onClose,
        )
    }
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
    securityQuestion: String = SECURITY_QUESTIONS.first(),
    onSecurityQuestionChange: (String) -> Unit = {},
    securityAnswer: String = "",
    onSecurityAnswerChange: (String) -> Unit = {},
    errorMessage: String?,
    isBusy: Boolean,
    onSubmit: () -> Unit,
    onGuestClick: () -> Unit,
    onForgotPasswordClick: () -> Unit = {},
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
                    FieldLabel("Security question")
                    SecurityQuestionPicker(selected = securityQuestion, onSelect = onSecurityQuestionChange)
                    FieldLabel("Your answer")
                    FocusTextField(
                        value = securityAnswer,
                        onValueChange = onSecurityAnswerChange,
                        placeholder = "Answer",
                        keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Done),
                    )
                    AccountNote("There's no email - this is the only way to recover a forgotten password.")
                }

                if (!isSignUp && !upgradingGuest) {
                    Text(
                        text = "Forgot password?",
                        style = typography.caption,
                        color = colors.accent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable(enabled = !isBusy, role = Role.Button) { onForgotPasswordClick() }
                            .padding(vertical = 4.dp),
                    )
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

/** [Claude, 2026-10-04] Picked from, not freely typed - see the state declaration in
 *  [AccountScreen] for why. */
private val SECURITY_QUESTIONS = listOf(
    "What was your first pet's name?",
    "What city were you born in?",
    "What was the name of your first school?",
    "What's your mother's maiden name?",
    "What was your childhood nickname?",
)

/** A plain tappable list, not a dropdown - this project has no existing dropdown component to
 *  match the rest of its look, and a short, always-visible list of five options doesn't really
 *  need one to collapse. */
@Composable
private fun SecurityQuestionPicker(selected: String, onSelect: (String) -> Unit) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceSunken),
    ) {
        SECURITY_QUESTIONS.forEach { question ->
            Text(
                text = question,
                style = typography.body,
                color = if (question == selected) colors.accent else colors.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onSelect(question) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
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

/** [Claude, 2026-10-04] [ForgotPasswordContent]'s own state - a small two-step form (find the
 *  account, then answer + set a new password), separate from [AccountScreen]'s own
 *  username/password fields since this is recovering a DIFFERENT credential, not reusing them.
 *  [question] being null is what distinguishes the two steps. */
/**
 * [Claude, 2026-10-04, rewritten to fix a reported bug] "Forgot password" - step 1 looks up the
 * account's security question ([AccountManager.getSecurityQuestion]); step 2 answers it and
 * sets a new password ([AccountManager.recoverPassword]). Laid out like [AccountContent]
 * (title, then a card) so it doesn't look like a different screen bolted on.
 *
 * Every field below is its OWN independent `remember { mutableStateOf(...) }`, set directly -
 * NOT one combined state object rebuilt via `.copy()` on every change. The first version of
 * this screen did the latter, hoisting a single `ForgotPasswordState` up to [AccountScreen] and
 * threading it down; that version had a real, reported bug ("type a username, the field clears
 * itself and re-asks for it"), and a `rememberUpdatedState`-based fix for the specific stale-
 * closure race I could identify did NOT make it go away - meaning the actual mechanism was
 * something I wasn't seeing. Rather than keep guessing at it, this removes the entire
 * "reconstruct the whole object from a snapshot of it" pattern that enables a race like that in
 * the first place: there's no single object to go stale, so there's nothing for one field's
 * update to accidentally overwrite another field with an old value of.
 */
@Composable
private fun ForgotPasswordContent(
    onClose: () -> Unit,
    onRecovered: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    var username by remember { mutableStateOf("") }
    var question by remember { mutableStateOf<String?>(null) }
    var answer by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    fun findAccount() {
        if (username.isBlank()) {
            errorMessage = "Enter your username."
            return
        }
        errorMessage = null
        isBusy = true
        scope.launch {
            try {
                val found = AccountManager.getSecurityQuestion(username)
                if (found != null) {
                    question = found
                    // Same vague wording a wrong answer gets later - see recoverPassword()'s
                    // doc comment for why this doesn't say "no such account" outright.
                } else {
                    errorMessage = "Couldn't find that account."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = AccountManager.errorMessageFor(e)
            } finally {
                isBusy = false
            }
        }
    }

    fun resetPassword() {
        val problem = AccountManager.passwordProblem(newPassword) ?: when {
            answer.isBlank() -> "Answer the security question."
            newPassword != confirmNewPassword -> "Passwords don't match."
            else -> null
        }
        if (problem != null) {
            errorMessage = problem
            return
        }
        errorMessage = null
        isBusy = true
        scope.launch {
            try {
                AccountManager.recoverPassword(context, username, answer, newPassword)
                onRecovered()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = AccountManager.errorMessageFor(e)
                isBusy = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, top = FocusSpacing.ScreenTop, bottom = FocusSpacing.ScreenBottom),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.Center) {
                Text(text = "Forgot password", style = typography.primaryActionLabel, color = colors.onSurface)
            }

            FocusCard {
                CardTitle(if (question == null) "Find your account" else "Answer and reset", bottomPadding = 0.dp)

                if (question == null) {
                    FieldLabel("Username")
                    FocusTextField(
                        value = username,
                        onValueChange = { username = it.trim(); errorMessage = null },
                        placeholder = "Enter Username",
                        keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Done),
                    )
                    errorMessage?.let { AccountError(it) }
                    FocusPillButton(
                        label = if (isBusy) "Please wait…" else "Find account",
                        containerColor = colors.primaryAction,
                        contentColor = colors.onPrimaryAction,
                        onClick = ::findAccount,
                        enabled = !isBusy,
                        height = 48.dp,
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    )
                } else {
                    AccountNote(question!!)
                    FieldLabel("Your answer")
                    FocusTextField(
                        value = answer,
                        onValueChange = { answer = it; errorMessage = null },
                        placeholder = "Answer",
                        keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Next),
                    )
                    FieldLabel("New password")
                    FocusTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; errorMessage = null },
                        placeholder = "Enter Password",
                        isPassword = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                    FieldLabel("Confirm new password")
                    FocusTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it; errorMessage = null },
                        placeholder = "Enter Password Again",
                        isPassword = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                    errorMessage?.let { AccountError(it) }
                    FocusPillButton(
                        label = if (isBusy) "Please wait…" else "Reset password",
                        containerColor = colors.primaryAction,
                        contentColor = colors.onPrimaryAction,
                        onClick = ::resetPassword,
                        enabled = !isBusy,
                        height = 48.dp,
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    )
                }
            }
        }

        CloseTopBar(contentDescription = "Close", onCloseClick = onClose, alignment = Alignment.TopEnd)
    }
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
