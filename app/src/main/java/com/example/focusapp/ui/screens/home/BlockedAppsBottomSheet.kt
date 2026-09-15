package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MAX_VISIBLE_APPS = 5

/**
 * Content of the Blocked Apps summary bottom sheet.
 *
 * Layout rules (designer-specified, do not change without asking):
 * - Group name: display-only text, driven by data. NOT clickable.
 * - Blocker card (apps + time slots): clickable -> opens GroupListScreen.
 * - Edit button (below the blocker): edits THIS group's apps + time slots,
 *   sharing the same screen as "Add New Group".
 */
@Composable
fun BlockedAppsSheetContent(
    groups: List<BlockedAppGroup>,
    selectedGroupId: String,
    onBlockerClick: () -> Unit,
    onEditClick: () -> Unit = {}
) {
    val selectedGroup = groups.find { it.id == selectedGroupId } ?: groups.first()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = selectedGroup.name,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(Modifier.height(16.dp))

        BlockedAppsBlocker(
            group = selectedGroup,
            onClick = onBlockerClick
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onEditClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(48.dp)
        ) {
            Text("Edit", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * The "blocker" card: one dark container wrapping the app icon row and the
 * time slots of a single group. Tapping it opens the group selection list.
 * Pass onClick = null to render it as a non-interactive display card.
 */
@Composable
fun BlockedAppsBlocker(
    group: BlockedAppGroup,
    onClick: (() -> Unit)? = null
) {
    val baseModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(Color(0x19494848))

    Column(
        modifier = (if (onClick != null) baseModifier.clickable { onClick() } else baseModifier)
            .padding(16.dp)
    ) {
        AppIconPreviewRow(apps = group.apps)

        Spacer(Modifier.height(16.dp))

        val visibleSlots = group.timeSlots.take(MAX_TIME_SLOTS)
        visibleSlots.forEachIndexed { index, slot ->
            ScheduleRow(activeDays = slot.activeDays, timeText = slot.timeText)
            if (index != visibleSlots.lastIndex) {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** Green pill showing up to MAX_VISIBLE_APPS icons, plus dots when there are more. */
@Composable
fun AppIconPreviewRow(apps: List<AppItem>) {
    val visibleApps = apps.take(MAX_VISIBLE_APPS)
    val hasMore = apps.size > MAX_VISIBLE_APPS

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF00E6A0))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        visibleApps.forEach { _ ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE800E8))
            )
        }

        if (hasMore) {
            Row(
                modifier = Modifier.padding(start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE800E8))
                    )
                }
            }
        }
    }
}

/** One schedule line: 7 day circles on the left, time range text on the right. */
@Composable
fun ScheduleRow(activeDays: Set<String>, timeText: String) {
    val days = listOf("Mon", "Tue", "Wen", "Thu", "Fri", "Sat", "Sun")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            days.forEach { day ->
                val isActive = activeDays.contains(day)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Color.Black else Color(0xFF9E9E9E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        color = if (isActive) Color.White else Color(0xFF4F4F4F),
                        fontSize = 9.sp
                    )
                }
            }
        }

        Text(
            text = timeText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BlockedAppsSheetContentPreview() {
    val groups = generateFakeGroups()
    BlockedAppsSheetContent(
        groups = groups,
        selectedGroupId = groups.first().id,
        onBlockerClick = {},
        onEditClick = {}
    )
}
