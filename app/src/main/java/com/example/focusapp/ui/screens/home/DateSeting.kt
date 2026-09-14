package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// 星期的顯示文字,index 0 = 一, index 6 = 日
private val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 星期選擇器(可複選)。
 * UI 內部自己管理「目前選了哪幾天」的狀態,
 * 每次改變都會透過 onSelectionChanged 把最新結果丟出去,
 * 之後接 ViewModel 的人只要監聽這個 callback 就好,不用碰這支檔案。
 */
@Composable
fun DayOfWeekSelector(
    onSelectionChanged: (Set<Int>) -> Unit = {}
) {
    // Set<Int> 存「被選中的星期 index」,天生不會重複
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        dayLabels.forEachIndexed { index, label ->
            val isSelected = selectedDays.contains(index)

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isSelected) Color(0xFF6C7BFF) else Color(0xFFE0E0E0),
                        CircleShape
                    )
                    .clickable {
                        // 已選中 → 移除;沒選中 → 加入
                        selectedDays = if (isSelected) {
                            selectedDays - index
                        } else {
                            selectedDays + index
                        }
                        onSelectionChanged(selectedDays)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color.DarkGray
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DayOfWeekSelectorPreview() {
    DayOfWeekSelector()
}