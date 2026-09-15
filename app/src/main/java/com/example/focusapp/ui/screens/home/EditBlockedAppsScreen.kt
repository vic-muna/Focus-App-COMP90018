package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One screen for BOTH "edit an existing group" and "add a new group".
 * A blank initialGroupName means it is a new group.
 *
 * onSaveClick returns the final group name so NavGraph can create or update
 * the BlockedAppGroup — this screen holds no business logic.
 */
@Composable
fun EditBlockedAppsScreen(
    initialGroupName: String,
    apps: List<AppItem>,
    onAppsChange: (List<AppItem>) -> Unit,
    onSaveClick: (String) -> Unit = {}
) {
    var groupName by remember(initialGroupName) { mutableStateOf(initialGroupName) }
    val isNewGroup = initialGroupName.isEmpty()
    val selectedCount = apps.count { it.isBlocked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isNewGroup) "New Group" else "Edit Group",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Button(
                onClick = { onSaveClick(groupName) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Group name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Text("Active Days", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)

        Spacer(Modifier.height(8.dp))

        DayOfWeekSelector(
            onSelectionChanged = { /* TODO: persist into the group's TimeSlot list */ }
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Apps ($selectedCount selected)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items = apps, key = { it.id }) { app ->
                val toggle = {
                    onAppsChange(
                        apps.map {
                            if (it.id == app.id) it.copy(isBlocked = !it.isBlocked) else it
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { toggle() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = app.isBlocked, onCheckedChange = { toggle() })
                    Spacer(Modifier.width(8.dp))
                    Text(text = app.name, fontSize = 16.sp, color = Color.Black)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditBlockedAppsScreenPreview() {
    EditBlockedAppsScreen(
        initialGroupName = "Study Group",
        apps = generateFakeApps(20),
        onAppsChange = {},
        onSaveClick = {}
    )
}
