package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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

/**
 * Scrollable group selection list, opened by tapping the bottom chevron bar
 * on the Auto Blocking sheet.
 *
 * - Back arrow (top-left) -> back to Home, reopening the Blocked Apps sheet.
 * - Group card tap -> selects that group (NavGraph pops back to Home with
 *   the sheet reopened on it - there's no separate detail screen anymore).
 * - Group card long-press -> Rename / Delete.
 * - "+" item, placed AFTER the last group inside the scrolling list, opens
 *   a name dialog that creates a new (empty) group.
 */
// 定義主題色系
private val SheetBgColor = Color(0xFF33386D)         // Sheet 主背景色
private val InputBgColor = Color(0xFF455DA2)         // 搜尋框背景色 (更深的暗色增加深度)
private val SelectedItemBg = Color(0xC46880D5)       // 選中項目的醒目背景色
private val TextPrimary = Color(0xFFFFFFFF)
private val CircleNavigator= Color(0xFFB1B2F8)       // 主要白色文字
private val TextSecondary = Color(0xFFA5ABC7)
private val SetDefault= Color(0xFFEFEED8)   // 次要淡藍灰色文字


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupListScreen(
    groups: List<BlockedAppGroup>,
    activeGroupId: String,
    onBackClick: () -> Unit,
    onGroupClick: (String) -> Unit,
    onCreateGroup: (String) -> Unit,
    onRenameGroup: (groupId: String, newName: String) -> Unit,
    onDeleteGroup: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var managingGroup by remember { mutableStateOf<BlockedAppGroup?>(null) }
    var renamingGroup by remember { mutableStateOf<BlockedAppGroup?>(null) }
    var deletingGroup by remember { mutableStateOf<BlockedAppGroup?>(null) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SheetBgColor)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CircleNavigator)
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
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            items(items = groups, key = { it.id }) { group ->
                GroupListCard(
                    group = group,
                    isActive = group.id == activeGroupId,
                    onClick = { onGroupClick(group.id) },
                    onLongClick = { managingGroup = group }
                )
            }

            // "+" always sits below the LAST group and scrolls with the list.
            item(key = "add_new_group") {
                AddGroupCard(onClick = { showCreateDialog = true })
            }
        }
    }

    if (showCreateDialog) {
        GroupNameDialog(
            title = "New Group",
            confirmLabel = "Create",
            initialName = "",
            onConfirm = { name ->
                onCreateGroup(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    managingGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { managingGroup = null },
            title = { Text(group.name) },
            text = {
                if (groups.size <= 1) {
                    Text("This is the only group left, so it can't be deleted.")
                } else {
                    Text("Rename this group, or delete it.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    renamingGroup = group
                    managingGroup = null
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = groups.size > 1,
                    onClick = {
                        deletingGroup = group
                        managingGroup = null
                    }
                ) {
                    Text("Delete")
                }
            }
        )
    }

    renamingGroup?.let { group ->
        GroupNameDialog(
            title = "Rename Group",
            confirmLabel = "Save",
            initialName = group.name,
            onConfirm = { name ->
                onRenameGroup(group.id, name)
                renamingGroup = null
            },
            onDismiss = { renamingGroup = null }
        )
    }

    deletingGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { deletingGroup = null },
            title = { Text("Delete \"${group.name}\"?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteGroup(group.id)
                    deletingGroup = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingGroup = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/** Shared text-field dialog backing both group creation and renaming. */
@Composable
private fun GroupNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupListCard(
    group: BlockedAppGroup,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(InputBgColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Text(
            text = group.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
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
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(SetDefault)
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
            .clip(RoundedCornerShape(16.dp))
            .background(SelectedItemBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
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
        onCreateGroup = {},
        onRenameGroup = { _, _ -> },
        onDeleteGroup = {}
    )
}
