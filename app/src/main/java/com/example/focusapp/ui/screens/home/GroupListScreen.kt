package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 🎨 延用專案統一的深色主題配色
private val ScreenBgColor = Color(0xFF33386D)       // 主頁面深藍紫背景
private val CardBgColor = Color(0xFF252853)         // 卡片背景
private val SelectedCardBg = Color(0xFF474E91)      // 被選中卡片的醒目背景
private val TextPrimary = Color(0xFFFFFFFF)         // 主要白色文字
private val TextSecondary = Color(0xFFA5ABC7)       // 次要淡藍灰色文字

@Composable
fun GroupListScreen(
    groups: List<BlockedAppGroup>,
    selectedGroupId: String,
    onGroupSelect: (String) -> Unit,
    onAddGroupClick: (String) -> Unit,
    onBackClick: () -> Unit,
    title: String = "Block Groups"
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBgColor)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // 1. 頂部導覽列 (Back 按鈕 + 標題)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CardBgColor)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(Modifier.height(24.dp))

        // 2. 群組清單列表
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(groups, key = { it.id }) { group ->
                val isSelected = group.id == selectedGroupId
                GroupItemCard(
                    group = group,
                    isSelected = isSelected,
                    onClick = { onGroupSelect(group.id) }
                )
            }

            // 3. 新增群組按鈕 (Add New Group Card)
            item {
                AddGroupCard(onClick = { showCreateDialog = true })
            }
        }
    }

    if (showCreateDialog) {
        GroupNameDialog(
            onConfirm = { name ->
                onAddGroupClick(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

/** 📝 新增群組時輸入名稱的彈窗 */
@Composable
private fun GroupNameDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Group") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/** 📦 群組項目卡片 */
@Composable
private fun GroupItemCard(
    group: BlockedAppGroup,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) SelectedCardBg else CardBgColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${group.apps.size} apps blocked",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            // 選中狀態標示（圓圈 Radio）
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) TextPrimary else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = SelectedCardBg,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** ➕ 新增群組卡片 */
@Composable
private fun AddGroupCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = CardBgColor.copy(alpha = 0.5f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Group",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Add New Group",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupListScreenPreview() {
    val fakeGroups = generateFakeGroups()
    GroupListScreen(
        groups = fakeGroups,
        selectedGroupId = fakeGroups.first().id,
        onGroupSelect = {},
        onAddGroupClick = {},
        onBackClick = {}
    )
}