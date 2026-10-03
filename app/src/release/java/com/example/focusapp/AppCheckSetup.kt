package com.example.focusapp

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Release builds: App Check through Play Integrity (needs the app registered for Play Integrity
 * in the Firebase console). The debug build's version is in src/debug.
 */
fun installAppCheck() {
    Firebase.appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
}
