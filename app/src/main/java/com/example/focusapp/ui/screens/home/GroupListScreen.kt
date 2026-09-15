package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

/**
 * Scrollable group selection list, opened by tapping the blocker card.
 *
 * - Back arrow (top-left) -> back to Home, reopening the Blocked Apps sheet.
 * - Group card -> selects that group and opens its GroupSectionScreen.
 * - "+" item, placed AFTER the last group inside the scrolling list,
 *   opens the same edit screen used by "Edit" but with a blank new group.
 */
@Composable
fun GroupListScreen(
    groups: List<BlockedAppGroup>,
    activeGroupId: String,
    onBackClick: () -> Unit,
    onGroupClick: (String) -> Unit,
    onAddGroupClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5C8C8))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items = groups, key = { it.id }) { group ->
                GroupListCard(
                    group = group,
                    isActive = group.id == activeGroupId,
                    onClick = { onGroupClick(group.id) }
                )
            }

            // "+" always sits below the LAST group and scrolls with the list.
            item(key = "add_new_group") {
                AddGroupCard(onClick = onAddGroupClick)
            }
        }
    }
}

@Composable
private fun GroupListCard(
    group: BlockedAppGroup,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF5C8C8))
            .clickable { onClick() }
    ) {
        Text(
            text = group.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
        )

        // Red dot marks the currently active group.
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
        }
    }
}

@Composable
private fun AddGroupCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFAE6E6))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8A5A5A)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupListScreenPreview() {
    val groups = generateFakeGroups()
    GroupListScreen(
        groups = groups,
        activeGroupId = groups.first().id,
        onBackClick = {},
        onGroupClick = {},
        onAddGroupClick = {}
    )
}
