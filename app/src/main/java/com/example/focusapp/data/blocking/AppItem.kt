package com.example.focusapp.data.blocking

import android.graphics.Bitmap

/**
 * One installed app inside a [BlockedAppGroup].
 * [packageName] is the real Android package name - the blocker compares against it.
 * [icon] is null in previews or when the icon can't be loaded.
 */
data class AppItem(
    val packageName: String,
    val name: String,
    val isBlocked: Boolean,
    val icon: Bitmap? = null
)
