package com.example.app2

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app2.model.RewardItem
import com.example.app2.ui.components.VideoBackground
import com.example.app2.ui.components.dialog.BackgroundSettingDialog
import com.example.app2.ui.components.dialog.ShopDialog
import com.example.app2.ui.theme.App2Theme
import com.example.app2.ui.viewmodel.FocusViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App2Theme {
                FocusApp()
            }
        }
    }
}

@Composable
fun FocusApp(viewModel: FocusViewModel = viewModel()) {
    val context = LocalContext.current
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val totalHours by viewModel.totalHours.collectAsState(0f)
    val focusPoints by viewModel.focusPoints.collectAsState(initial = 0)
    val unlockedReward by viewModel.newlyUnlockedReward.collectAsState()
    val earnedPoints by viewModel.earnedPointsNotification.collectAsState()
    val selectedBackground by viewModel.selectedBackground.collectAsState()
    val unlockedIds by viewModel.unlockedIds.collectAsState(initial = emptySet())

    var isMuted by remember { mutableStateOf(false) }
    var showShopDialog by remember { mutableStateOf(false) }
    var showSettingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(earnedPoints) {
        earnedPoints?.let { pts ->
            if (pts > 0) {
                Toast.makeText(context, "🎉 Earned $pts Focus Point${if (pts > 1) "s" else ""}!", Toast.LENGTH_LONG).show()
            }
            viewModel.clearEarnedPointsNotification()
        }
    }

    // Reset sound to ON whenever starting a new focus session
    LaunchedEffect(isRunning) {
        if (isRunning) {
            isMuted = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isRunning) {
                if (isRunning) {
                    detectTapGestures(
                        onLongPress = { viewModel.stopTimer() }
                    )
                }
            }
    ) {
        // Background Alpha Animation
        val bgAlpha by animateFloatAsState(
            targetValue = if (isRunning) 1f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "bgAlpha"
        )

        // Delayed Alpha for "READY" state elements to avoid premature appearance
        val readyAlpha by animateFloatAsState(
            targetValue = if (isRunning) 0f else 1f,
            animationSpec = if (isRunning) {
                tween(durationMillis = 300) // Fast fade out when starting
            } else {
                tween(durationMillis = 300, delayMillis = 500) // 0.5s delay when stopping
            },
            label = "readyAlpha"
        )

        // Video Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = bgAlpha),
            contentAlignment = Alignment.Center
        ) {
            VideoBackground(
                videoResId = selectedBackground.rawResId ?: R.raw.forest1,
                modifier = Modifier.fillMaxSize(0.8f),
                playWhenReady = isRunning,
                isMuted = isMuted
            )
        }
        
        // Dark Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f * bgAlpha))
        )

        // Sound Toggle Button (Top Right, only visible when progressing)
        if (isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, end = 24.dp)
                    .graphicsLayer(alpha = bgAlpha),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier.size(36.dp)
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(
                            id = if (isMuted) R.drawable.soundoff else R.drawable.soundon
                        ),
                        contentDescription = if (isMuted) "Sound Off" else "Sound On",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Setting & Shop Buttons (Top Right, visible when not running)
        if (!isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, end = 24.dp)
                    .graphicsLayer(alpha = readyAlpha),
                contentAlignment = Alignment.TopEnd
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Setting (Background Selection) Button
                    IconButton(
                        onClick = { showSettingDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.setting),
                            contentDescription = "Setting",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Shop Button
                    IconButton(
                        onClick = { showShopDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.shop),
                            contentDescription = "Shop",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // 1. Top Section (Always at the top)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Focus: %.2f Hours  •  Points: %d pts".format(Locale.ENGLISH, totalHours, focusPoints),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isRunning) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondary.copy(alpha = readyAlpha)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Status Text (READY / STAY) - Now moved to TOP
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "READY TO FOCUS?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = readyAlpha),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "STAY FOCUSED",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White.copy(alpha = bgAlpha),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Timer Section (Exactly at the CENTER)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatTime(timerSeconds),
                fontSize = 80.sp,
                fontWeight = FontWeight.Thin,
                color = if (isRunning) Color.White.copy(alpha = bgAlpha) else MaterialTheme.colorScheme.onSurface.copy(alpha = readyAlpha)
            )
        }

        // 3. Bottom Section (Buttons)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp, start = 32.dp, end = 32.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isRunning) {
                Box(modifier = Modifier.graphicsLayer(alpha = readyAlpha)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { viewModel.startTimer(25) },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("Start 25 Min")
                        }
                        
                        TextButton(onClick = { viewModel.startTimer(1) }) {
                            Text("Start 1 Min (Test)")
                        }
                    }
                }
            } else {
                // Instruction text moved down by 1dp, size increased by 30%
                Text(
                    text = "LONG PRESS SCREEN TO STOP",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = bgAlpha * 0.6f),
                    modifier = Modifier.padding(top = 1.dp),
                    letterSpacing = 1.sp
                )
            }
        }

        // Background Setting Dialog
        if (showSettingDialog) {
            BackgroundSettingDialog(
                availableBackgrounds = viewModel.availableBackgrounds,
                unlockedIds = unlockedIds,
                selectedBackgroundId = selectedBackground.id,
                onSelectBackground = { bg ->
                    viewModel.selectBackground(bg)
                },
                onOpenShop = {
                    showSettingDialog = false
                    showShopDialog = true
                },
                onDismiss = { showSettingDialog = false }
            )
        }

        // Shop Dialog
        if (showShopDialog) {
            ShopDialog(
                points = focusPoints,
                availableBackgrounds = viewModel.availableBackgrounds,
                unlockedIds = unlockedIds,
                selectedBackgroundId = selectedBackground.id,
                onSelectBackground = { bg ->
                    viewModel.selectBackground(bg)
                },
                onUnlockBackground = { bg ->
                    viewModel.unlockBackgroundWithPoints(
                        background = bg,
                        onSuccess = {
                            Toast.makeText(context, "Unlocked '${bg.name}'!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onDismiss = { showShopDialog = false }
            )
        }

        // Unlock Notification Dialog
        unlockedReward?.let { reward ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissUnlockDialog() },
                title = { Text("New Milestone Reached!") },
                text = {
                    Column {
                        Text("Congratulations! You've unlocked:")
                        Text(
                            text = reward.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(reward.description)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissUnlockDialog() }) {
                        Text("Awesome!")
                    }
                }
            )
        }
    }
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(Locale.ENGLISH, m, s)
}