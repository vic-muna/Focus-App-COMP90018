package com.example.focusapp

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds: App Check debug tokens. On first run, Logcat prints
 * "Enter this debug secret into the allow list in the Firebase Console ...";
 * add that token under App Check -> Apps -> (your app) -> Manage debug tokens.
 * The release build's version is in src/release.
 */
fun installAppCheck() {
    Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
