package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onAvatarClick: () -> Unit = {},
    onQuickFocusClick: () -> Unit = {},
    onPartyModeClick: () -> Unit = {},
    onBlockedAppCardClick: () -> Unit = {},
    onLocationCardClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxSize().padding(30.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 上方:頭像區(不變)
        Box(
            modifier = Modifier.size(250.dp)
                .offset(0.dp,50.dp)
                .background(Color.LightGray)
                .clickable { onAvatarClick() }
        )

        // 中間:Quick Focus(不變)
        Box(
            modifier = Modifier.size(160.dp)
                .offset(0.dp,50.dp)
                .background(Color(0xFFE3E9FF), CircleShape)
                .clickable { onQuickFocusClick() }
        ) {
            Text("Quick Focus", modifier = Modifier.align(Alignment.Center))
        }

        // 底部:Map + Carousel(Blocked Apps / Location Zone)+ Settings
        // 底部:用 Box 疊層做出卡片蓋過圖示的效果
        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp)
        ) {
            // 背景層:Map + Settings,貼在左右下角
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Bottom),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(48.dp)
                            .background(Color(0xFFE0E0E0))
                            .clickable { onPartyModeClick() }
                    )
                    Text("Party")
                }

                Column(
                    modifier = Modifier.align(Alignment.Bottom),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(48.dp)
                            .background(Color(0xFFE0E0E0))
                            .clickable { onSettingsClick() }
                    )
                    Text("Settings")
                }
            }

            // 前景層:卡片故意做寬一點(佔 85% 版面),疊在上面
            // 兩張卡片並排同一列,不用滑動切換
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.75f)
                    .height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .fillMaxHeight()
                        .background(Color(0xFFF5F5F5))
                        .clickable { onBlockedAppCardClick() }
                ) { Text("Blocked Apps", modifier = Modifier.align(Alignment.Center)) }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFFF5F5F5))
                        .clickable { onLocationCardClick() }
                ) { Text("Location Zone", modifier = Modifier.align(Alignment.Center)) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}