package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onAvatarClick: () -> Unit = {},
    onQuickFocusClick: () -> Unit = {},
    onMapClick: () -> Unit = {},              // Study Party
    onBlockedAppCardClick: () -> Unit = {},
    onLocationCardClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(30.dp),
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
            modifier = Modifier.fillMaxWidth().height(155.dp)
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
                            .clickable { onMapClick() }
                    )
                    Text("Map")
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
            val pagerState = rememberPagerState(pageCount = { 2 })

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.85f)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(84.dp)
                ) { page ->
                    when (page) {
                        0 -> Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Color(0xFFF5F5F5))
                                .clickable { onBlockedAppCardClick() }
                        ) { Text("Blocked Apps", modifier = Modifier.align(Alignment.Center)) }

                        1 -> Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Color(0xFFF5F5F5))
                                .clickable { onLocationCardClick() }
                        ) { Text("Location Zone", modifier = Modifier.align(Alignment.Center)) }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(2) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier.padding(4.dp).size(6.dp)
                                .background(
                                    if (isSelected) Color.DarkGray else Color.LightGray,
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}