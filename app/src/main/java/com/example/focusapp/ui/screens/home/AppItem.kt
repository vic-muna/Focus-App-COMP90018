package com.example.focusapp.ui.screens.home

import android.graphics.Bitmap

/**
 * One row in the app picker / one entry stored on a [BlockedAppGroup].
 * [packageName] is the real PackageManager package name (matches what
 * FocusAccessibilityService compares against) - kept even after the app
 * leaves the picker's visible list, so a saved group still knows which
 * real app it refers to. [icon] is null for fake/preview data or if the
 * real icon couldn't be resolved.
 *
 * [David Shiau, 2026-09-20] Replaced the old fake `id: Int`/`name`-only
 * shape with real installed-app data (packageName + icon), so the app
 * picker shows and stores the user's actual installed apps.
 */
data class AppItem(
    val packageName: String,
    val name: String,
    val isBlocked: Boolean,
    val icon: Bitmap? = null
)

fun generateFakeApps(count: Int = 100): List<AppItem> =
    List(count) { index ->
        AppItem(packageName = "com.example.fake$index", name = "App $index", isBlocked = false)
    }
