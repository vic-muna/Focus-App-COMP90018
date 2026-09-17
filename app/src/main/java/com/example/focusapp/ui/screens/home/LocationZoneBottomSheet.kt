package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.FocusZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 🎨 色彩定義
private val CardSurface = Color.White.copy(alpha = 0.15f)
private val CardBgColor = Color(0xFF252853)
private val OnSheet = Color.White
private val OnSheetMuted = Color.White.copy(alpha = 0.8f)
private val AccentPinColor = Color(0xFFFF5252)

@Composable
fun LocationZoneSheetContent(
    onEditClick: () -> Unit = {},
    onChevronClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var zone by remember { mutableStateOf<FocusZone?>(null) }

    LaunchedEffect(Unit) {
        zone = withContext(Dispatchers.IO) {
            FocusRepositoryProvider.get(context).getFocusZone()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 95.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 頂部標題 + 白線分隔符
        Text(
            text = zone?.name ?: "Activate Location",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = OnSheet
        )

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .width(60.dp)
                .height(2.dp)
                .background(OnSheet)
        )

        Spacer(Modifier.height(24.dp))

        // 2. 地圖預覽卡片
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MapCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onEditClick
            )
        }
    }
}

/** 🗺️ 左側卡片：地圖預覽 */
@Composable
private fun MapCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CardSurface)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Map Area",
                color = OnSheet,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            // 中間模擬雷達 Pin 視覺
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(OnSheet.copy(alpha = 0.15f))
                        .border(1.dp, OnSheet.copy(alpha = 0.4f), CircleShape)
                )
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Map Pin",
                    tint = AccentPinColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "Tap to configure 🗺️",
                color = OnSheetMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // 2D alignment (TopEnd) needs a Box scope, not Column - this is a
        // sibling of the Column above, not a child of it.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .clip(CircleShape)
                .background(CardBgColor.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint = OnSheet,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF3B3B96)
@Composable
private fun LocationZoneSheetContentPreview() {
    LocationZoneSheetContent()
}