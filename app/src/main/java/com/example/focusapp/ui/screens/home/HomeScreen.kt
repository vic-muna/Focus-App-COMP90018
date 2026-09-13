package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onAvatarClick: () -> Unit = {},
    onQuickFocusClick: () -> Unit = {},
    onMapClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 上方:頭像區
        Box(
            modifier = Modifier
                .padding(0.dp,100.dp,0.dp,0.dp)
                .size(300.dp)
                .background(Color.LightGray)
                .clickable { onAvatarClick() }
        )

        // 中間:Quick Focus
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(Color(0xFFE3E9FF), CircleShape)
                .clickable { onQuickFocusClick() }
        ) {
            Text("Quick Focus", modifier = Modifier.align(Alignment.Center))
        }

        // 底部:先放一個空的 Row 占位,下一步再填內容
        Row(modifier = Modifier.fillMaxWidth()) {
            // 底部:配置列(帶層次感)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左下:Map icon
                Column(
                    modifier = Modifier.align(Alignment.Bottom),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE0E0E0))
                            .clickable { onMapClick() }
                    )
                    Text("Map")
                }

                // 中上:Blocked App 清單占位
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.Top)
                        .padding(horizontal = 12.dp)
                        .height(84.dp)
                        .background(Color(0xFFF5F5F5))
                )

                // 右下:Settings icon
                Column(
                    modifier = Modifier.align(Alignment.Bottom),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE0E0E0))
                            .clickable { onSettingsClick() }
                    )
                    Text("Settings")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
    // Preview 不用傳參數,因為都有預設值 {}
}