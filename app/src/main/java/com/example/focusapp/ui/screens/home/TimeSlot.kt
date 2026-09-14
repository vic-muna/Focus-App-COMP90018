package com.example.focusapp.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

// 一個時段 = 選了哪些星期 + 時間文字
data class TimeSlot(
    val activeDays: Set<String>,
    val timeText: String
)

// Blocked Apps 摘要卡最多只能顯示 2 個時段
const val MAX_TIME_SLOTS = 2

fun generateFakeTimeSlots(): List<TimeSlot> = listOf(
    TimeSlot(activeDays = setOf("Mon", "Tue", "Thu", "Sat", "Sun"), timeText = "04:00pm - 06:00pm"),
    TimeSlot(activeDays = setOf("Wen", "Thu", "Fri", "Sun"), timeText = "04:00pm - 06:00pm")
)

