package com.example.focusapp.data.account

import android.content.Context
import android.util.Log
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.accessibility.BlockSource
import com.example.focusapp.data.blocking.BlockedAppGroupStorage
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.data.sensor.GeofenceBroadcastReceiver
import com.example.focusapp.data.sensor.getGeofencePendingIntent
import com.example.focusapp.data.wifi.SavedWifiStorage
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "AccountManager"

/** Who is signed in. [username] is null for a guest. */
data class Account(val uid: String, val username: String?) {
    val isGuest: Boolean get() = username == null
}

/**
 * Signing in, as a guest or with a username + password account (Firebase Auth).
 *
 * Firebase only knows email + password, so a username is stored as the email
 * "username@[EMAIL_DOMAIN]". No email is ever sent, so a forgotten password
 * can't be reset.
 *
 * A guest who creates an account keeps their uid (the account is linked to the
 * guest), so everything they saved stays theirs. Logging in on another phone
 * gets the same uid back, and [FocusRepository.restoreFromCloud] downloads their data.
 *
 * The phone's storage only ever holds one user's data. Whose it is is remembered
 * ([KEY_OWNER_UID]); when a different user signs in, it is cleared first.
 */
object AccountManager {

    private const val EMAIL_DOMAIN = "users.focusapp.example.com"
    private const val PREFS_NAME = "focus_account"
    // The uid whose data is in the phone's storage right now.
    private const val KEY_OWNER_UID = "local_data_owner_uid"
    // Set when a download after logging in hasn't finished yet - retried on the next app start.
    private const val KEY_RESTORE_PENDING = "restore_pending"
    private const val LOG_OUT_SYNC_TIMEOUT_MILLIS = 10_000L

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    private val _account = MutableStateFlow(auth.currentUser?.toAccount())
    /** The signed-in user, or null when the login screen should show. */
    val account: StateFlow<Account?> = _account.asStateFlow()

    /** Usernames are case-insensitive: "David" and "david" are the same account. */
    fun normalizeUsername(input: String): String = input.trim().lowercase()

    /** A message saying what's wrong with [username] (already normalized), or null if it's fine. */
    fun usernameProblem(username: String): String? = when {
        username.length !in 3..20 -> "Username must be 3-20 characters."
        !username.matches(Regex("[a-z0-9_]+")) -> "Username can only use letters, numbers and _."
        else -> null
    }

    /** A message saying what's wrong with [password], or null if it's fine (Firebase needs 6+). */
    fun passwordProblem(password: String): String? =
        if (password.length < 6) "Password must be at least 6 characters." else null

    /** Call once when the app starts. */
    suspend fun onAppStart(context: Context) {
        val account = _account.value ?: return
        val prefs = prefs(context)
        // Signed in from before accounts existed: the data on the phone is this user's.
        if (prefs.getString(KEY_OWNER_UID, null) == null) {
            prefs.edit().putString(KEY_OWNER_UID, account.uid).apply()
        }
        if (prefs.getBoolean(KEY_RESTORE_PENDING, false)) restore(context)
    }

    suspend fun continueAsGuest(context: Context) {
        val user = auth.signInAnonymously().await().user ?: error("Guest sign-in returned no user")
        prepareLocalData(context, user.uid, restore = false)
        _account.value = user.toAccount()
    }

    /**
     * Creates an account. If a guest is signed in, the account is linked to the guest:
     * the uid stays the same, so their data needs no copying.
     */
    suspend fun createAccount(context: Context, username: String, password: String) {
        val email = emailFor(username)
        val guest = auth.currentUser?.takeIf { it.isAnonymous }
        val user = if (guest != null) {
            guest.linkWithCredential(EmailAuthProvider.getCredential(email, password)).await().user
        } else {
            auth.createUserWithEmailAndPassword(email, password).await().user
        } ?: error("Sign-up returned no user")
        prepareLocalData(context, user.uid, restore = false)
        _account.value = Account(user.uid, username)
    }

    /** Logs in to an existing account and downloads its data. */
    suspend fun logIn(context: Context, username: String, password: String) {
        val user = auth.signInWithEmailAndPassword(emailFor(username), password).await().user
            ?: error("Log-in returned no user")
        prepareLocalData(context, user.uid, restore = true)
        _account.value = Account(user.uid, username)
    }

    /**
     * Uploads what hasn't reached the cloud yet, then signs out. The phone's data stays
     * until someone else signs in. A guest can't sign back in, so their data is lost.
     */
    suspend fun logOut(context: Context) {
        val repository = FocusRepositoryProvider.get(context)
        withTimeoutOrNull(LOG_OUT_SYNC_TIMEOUT_MILLIS) {
            repository.syncPendingSessions()
            repository.syncPendingZoneAndAppGroups()
        }
        auth.signOut()
        _account.value = null
    }

    /** A message for the user explaining why signing in or up failed. */
    fun errorMessageFor(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException -> "That username is already taken."
        is FirebaseAuthWeakPasswordException -> "That password is too weak. Try a longer one."
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Wrong username or password."
        is FirebaseNetworkException -> "No internet connection. Please try again."
        is FirebaseTooManyRequestsException -> "Too many tries. Please wait a moment and try again."
        is FirebaseAuthException ->
            if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED") {
                "Accounts aren't turned on for this app yet (Firebase: enable Email/Password sign-in)."
            } else {
                "Couldn't sign in (${e.errorCode})."
            }
        else -> "Something went wrong. Please try again."
    }

    private fun emailFor(username: String) = "$username@$EMAIL_DOMAIN"

    private fun FirebaseUser.toAccount() = Account(
        uid = uid,
        username = if (isAnonymous) null else email?.substringBefore('@'),
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Makes the phone's storage belong to [uid]: clears another user's data, then downloads if [restore]. */
    private suspend fun prepareLocalData(context: Context, uid: String, restore: Boolean) {
        val prefs = prefs(context)
        val owner = prefs.getString(KEY_OWNER_UID, null)
        if (owner != null && owner != uid) clearLocalData(context)
        prefs.edit()
            .putString(KEY_OWNER_UID, uid)
            .putBoolean(KEY_RESTORE_PENDING, restore)
            .commit()
        if (restore) restore(context)
    }

    // A failed download doesn't stop the log-in; it's retried on the next app start.
    private suspend fun restore(context: Context) {
        try {
            FocusRepositoryProvider.get(context).restoreFromCloud()
            prefs(context).edit().putBoolean(KEY_RESTORE_PENDING, false).apply()
            // Downloaded zones need their geofences to start blocking.
            GeofenceBroadcastReceiver().restoreGeofences(context.applicationContext)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Restoring from the cloud failed - will retry on next app start.", e)
        }
    }

    /** Deletes the previous user's data from the phone and stops their location / Wi-Fi blocking. */
    private suspend fun clearLocalData(context: Context) {
        val appContext = context.applicationContext
        val repository = FocusRepositoryProvider.get(appContext)

        repository.getFocusZones().forEach { GeofenceBroadcastReceiver.onGeofenceRemoved(appContext, it.id) }
        LocationServices.getGeofencingClient(appContext).removeGeofences(getGeofencePendingIntent(appContext))

        repository.clearLocalData()
        BlockedAppGroupStorage(appContext).clear()
        BlockedAppGroupStorage.forLocationGroups(appContext).clear()
        BlockedAppGroupStorage.forWifiNetworks(appContext).clear()
        BlockedAppGroupStorage.forQuickFocus(appContext).clear()
        SavedWifiStorage(appContext).clear()

        AccessibilityBridge.clearBlocks(BlockSource.LOCATION)
        AccessibilityBridge.clearBlocks(BlockSource.WIFI)
    }
}
