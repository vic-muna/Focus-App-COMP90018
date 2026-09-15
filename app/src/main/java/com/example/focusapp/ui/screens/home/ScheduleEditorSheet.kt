package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged

// 🎨 色彩主題定義
private val SheetBgColor = Color(0xFF33386D)         // Sheet 主背景
private val CardBgColor = Color(0xFF252853)          // 卡片背景 (較深暗色)
private val SelectedBgColor = Color(0xFF474E91)      // 選中天數/醒目背景
private val TextPrimary = Color(0xFFFFFFFF)          // 主要白色文字
private val TextSecondary = Color(0xFFA5ABC7)        // 次要淡藍灰色文字

@Composable
fun ScheduleEditorSheet(
    schedule: TimeSlot,
    onScheduleChange: (TimeSlot) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .background(SheetBgColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // 1. 頁面標題
        Text(
            text = "Schedule Editor",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Set active days and focus time range",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(Modifier.height(20.dp))

        // 2. 星期選擇 (橫向圓格按鈕)
        Text(
            text = "Active Days",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DAY_KEYS.forEach { day ->
                val isChecked = schedule.activeDays.contains(day)
                val dayLabel = day.take(1) // 取縮寫，例如 "M", "T", "W"...

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isChecked) SelectedBgColor else CardBgColor)
                        .clickable {
                            onScheduleChange(
                                schedule.copy(
                                    activeDays = if (isChecked) schedule.activeDays - day else schedule.activeDays + day
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayLabel,
                        fontSize = 14.sp,
                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 3. 開始與結束時間設定卡片 (併排雙滾輪)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start Time 區塊
            TimePickerCard(
                title = "Start Time",
                hour = schedule.start.hour,
                minute = schedule.start.minute,
                onTimeChange = { h, m ->
                    onScheduleChange(schedule.copy(start = ClockTime(h, m)))
                },
                modifier = Modifier.weight(1f)
            )

            // End Time 區塊
            TimePickerCard(
                title = "End Time",
                hour = schedule.end.hour,
                minute = schedule.end.minute,
                onTimeChange = { h, m ->
                    onScheduleChange(schedule.copy(end = ClockTime(h, m)))
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * 📦 時間選擇卡片包覆元件
 */
@Composable
private fun TimePickerCard(
    title: String,
    hour: Int,
    minute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = CardBgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 小時滾輪 (00 - 23)
                WheelPicker(
                    items = (0..23).map { String.format("%02d", it) },
                    initialIndex = hour,
                    onItemSelected = { selectedHour ->
                        onTimeChange(selectedHour, minute)
                    }
                )

                Text(
                    text = ":",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // 分鐘滾輪 (00 - 59)
                WheelPicker(
                    items = (0..59).map { String.format("%02d", it) },
                    initialIndex = minute,
                    onItemSelected = { selectedMinute ->
                        onTimeChange(hour, selectedMinute)
                    }
                )
            }
        }
    }
}

/**
 * 🎡 自訂 iOS 風格 3D 上下滑動滾輪元件
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(
    items: List<String>,
    initialIndex: Int = 0,
    onItemSelected: (Int) -> Unit
) {
    val itemHeight = 36.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // 當滾輪停下來時捕捉當前選中的 Index
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index in items.indices) {
                    onItemSelected(index)
                }
            }
    }

    Box(
        modifier = Modifier
            .height(itemHeight * 3) // 只顯示 3 格 Height (上/中/下)
            .width(44.dp),
        contentAlignment = Alignment.Center
    ) {
        // 選中項目的藍紫色背景亮條
        Box(
            modifier = Modifier
                .height(itemHeight)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SelectedBgColor.copy(alpha = 0.5f))
        )

        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            modifier = Modifier.height(itemHeight * 3)
        ) {
            // 頂部墊高
            item { Box(modifier = Modifier.height(itemHeight)) }

            items(items.size) { index ->
                val isSelected = listState.firstVisibleItemIndex == index
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        fontSize = if (isSelected) 18.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }

            // 底部墊高
            item { Box(modifier = Modifier.height(itemHeight)) }
        }
    }
}
@Preview(showBackground = true)
@Composable
private fun ScheduleEditorSheetPreview() {
    ScheduleEditorSheet(schedule = generateFakeTimeSlot(), onScheduleChange = {})
}
