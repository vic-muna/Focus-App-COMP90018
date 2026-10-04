package com.example.focusapp.ui.common

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

private const val SHORT_VIBRATION_MILLIS = 150L

/** A short buzz - confirms a gesture worked (flip started a session, shake ended one). */
fun vibrateShort(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(VibrationEffect.createOneShot(SHORT_VIBRATION_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE))
}
