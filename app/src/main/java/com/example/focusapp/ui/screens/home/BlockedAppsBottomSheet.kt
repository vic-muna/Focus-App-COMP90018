package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MAX_VISIBLE_APPS = 5

@Composable
fun BlockedAppsSheetContent(
    apps: List<AppItem>,
    timeSlots: List<TimeSlot>,
    onEditClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Group Name",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        AppIconPreviewRow(apps = apps)

        Spacer(modifier = Modifier.height(20.dp))

        // 最多只顯示 MAX_TIME_SLOTS 個時段,清單有多少就畫多少(但不超過上限)
        timeSlots.take(MAX_TIME_SLOTS).forEachIndexed { index, slot ->
            ScheduleRow(activeDays = slot.activeDays, timeText = slot.timeText)
            if (index != timeSlots.take(MAX_TIME_SLOTS).lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(50.dp))

        Button(
            onClick = onEditClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.6f).height(48.dp)
        ) {
            Text(text = "Edit", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AppIconPreviewRow(apps: List<AppItem>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF00E5A3), shape = RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            apps.take(MAX_VISIBLE_APPS).forEach { _ ->
                Box(
                    modifier = Modifier.size(48.dp)
                        .background(Color(0xFFFF00EC), shape = RoundedCornerShape(16.dp))
                )
            }
        }
        if (apps.size > MAX_VISIBLE_APPS) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier.size(8.dp)
                            .background(Color(0xFFFF00EC), shape = CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(activeDays: Set<String>, timeText: String) {
    val allDays = listOf("Mon", "Tue", "Wen", "Thu", "Fri", "Sat", "Sun")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            allDays.forEach { day ->
                val isActive = activeDays.contains(day)
                Box(
                    modifier = Modifier.size(24.dp)
                        .background(color = if (isActive) Color.Black else Color.Gray, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = day, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            text = timeText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = Color.Black
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5C0C0)
@Composable
fun BlockedAppsSheetContentPreview() {
    BlockedAppsSheetContent(
        apps = generateFakeApps(8),
        timeSlots = generateFakeTimeSlots()
    )
}