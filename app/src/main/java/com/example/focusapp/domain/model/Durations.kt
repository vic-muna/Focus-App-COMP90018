package com.example.focusapp.domain.model

/** "45m", "2h", "1h 20m". */
fun formatMinutes(minutes: Long): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0L -> "${rest}m"
        rest == 0L -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}
