package com.example.focusapp.ui.screens.party

// Shown as the content of PartyModeScreen's Invite dialog when the user taps
// "Select from friends" (see PartyModeScreen.kt) - not its own NavGraph route,
// since Party Mode's whole invite/join flow is already dialog-based rather than
// a sequence of full-screen navigation destinations. Written as a standalone
// @Composable (not a `private fun` in PartyModeScreen.kt) so it CAN be lifted
// into its own route later if the team decides friend management deserves a
// permanent home (e.g. off SettingsScreen) instead of only living inside Party
// Mode's invite flow.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusapp.domain.model.Friend

/**
 * FriendListScreen
 * --------------------
 * Local friend picker: shows this device's own uid (to hand to someone
 * else so they can add you back), a small add-friend form (uid + optional
 * nickname), and the saved friend list - tapping a row calls
 * [onFriendSelected]. There's no server-side directory/search - "adding a
 * friend" is just saving their uid locally (see [FriendListViewModel] and
 * FriendDao), so both sides need to add each other manually for now.
 *
 * @param onFriendSelected called with the tapped [Friend]; the caller
 *                          (PartyModeScreen) is responsible for closing
 *                          this screen/dialog afterwards.
 */
@Composable
fun FriendListScreen(
    onFriendSelected: (Friend) -> Unit,
    viewModel: FriendListViewModel = viewModel()
) {
    val friends by viewModel.friends.collectAsState()
    val myUid by viewModel.myUid.collectAsState()
    val clipboard = LocalClipboardManager.current

    var uidInput by remember { mutableStateOf("") }
    var nicknameInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .padding(24.dp)
    ) {
        Text("Your uid", color = Color.White, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = myUid.ifBlank { "…" },
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f, fill = false)
            )
            TextButton(onClick = { clipboard.setText(AnnotatedString(myUid)) }) {
                Text("Copy", color = Color.White)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Add a friend", color = Color.White, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = uidInput,
            onValueChange = { uidInput = it },
            singleLine = true,
            placeholder = { Text("Friend's uid") },
            colors = friendFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = nicknameInput,
            onValueChange = { nicknameInput = it },
            singleLine = true,
            placeholder = { Text("Nickname (optional)") },
            colors = friendFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                viewModel.addFriend(uidInput, nicknameInput)
                uidInput = ""
                nicknameInput = ""
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Text("Add friend", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))
        Text("Your friends", color = Color.White, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))

        if (friends.isEmpty()) {
            Text(
                text = "No saved friends yet - ask them for their uid above and add them.",
                color = Color.LightGray,
                fontSize = 13.sp
            )
        } else {
            // Bounded height (rather than fillMaxSize) since this sits inside a Dialog
            // alongside the form above it, not on its own full screen.
            LazyColumn(
                modifier = Modifier.heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(friends, key = { it.uid }) { friend ->
                    FriendRow(
                        friend = friend,
                        onClick = { onFriendSelected(friend) },
                        onRemoveClick = { viewModel.removeFriend(friend.uid) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendRow(friend: Friend, onClick: () -> Unit, onRemoveClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = friend.nickname, color = Color.White, fontSize = 15.sp)
            Text(text = friend.uid, color = Color.LightGray, fontSize = 11.sp)
        }
        IconButton(onClick = onRemoveClick) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Remove friend", tint = Color.LightGray)
        }
    }
}

@Composable
private fun friendFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color.Gray,
    cursorColor = Color.White,
    focusedPlaceholderColor = Color.Gray,
    unfocusedPlaceholderColor = Color.Gray
)
