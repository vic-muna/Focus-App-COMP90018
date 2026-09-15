package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

// 定義主題色系
private val SheetBgColor = Color(0xFF33386D)         // Sheet 主背景色
private val InputBgColor = Color(0xFF252853)         // 搜尋框背景色 (更深的暗色增加深度)
private val SelectedItemBg = Color(0xFF474E91)       // 選中項目的醒目背景色
private val TextPrimary = Color(0xFFFFFFFF)          // 主要白色文字
private val TextSecondary = Color(0xFFA5ABC7)        // 次要淡藍灰色文字

@Composable
fun AppPickerSheet(
    apps: List<AppItem>,
    onAppsChange: (List<AppItem>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .background(SheetBgColor) // 1. 設定背景色 #33386d
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // 標題
        Text(
            text = "Block Apps",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(Modifier.height(4.dp))

        // 副標題
        Text(
            text = "Select apps to restrict during focus sessions",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(Modifier.height(16.dp))

        // 2. 搜尋列（搭配深背景調整顏色）
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps...", color = TextSecondary, fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = InputBgColor,
                unfocusedContainerColor = InputBgColor,
                focusedBorderColor = Color(0xFF6C75CE),
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // 3. 列表項目
        LazyColumn(
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(items = filtered, key = { it.id }) { app ->
                val toggle = {
                    onAppsChange(
                        apps.map { if (it.id == app.id) it.copy(isBlocked = !it.isBlocked) else it }
                    )
                }

                // 4. 深色選中與未選中狀態
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { toggle() },
                    color = if (app.isBlocked) SelectedItemBg else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircleCheckbox(
                            checked = app.isBlocked,
                            onCheckedChange = { toggle() }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = app.name,
                            fontSize = 16.sp,
                            fontWeight = if (app.isBlocked) FontWeight.Bold else FontWeight.Normal,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppPickerSheetPreview() {
    AppPickerSheet(apps = generateFakeApps(20), onAppsChange = {})
}
