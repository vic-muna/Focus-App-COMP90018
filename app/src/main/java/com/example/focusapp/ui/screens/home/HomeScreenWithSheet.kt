package com.example.focusapp.ui.screens.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

enum class SheetType { NONE, BLOCKED_APPS, LOCATION_ZONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenWithSheet(
    groups: List<BlockedAppGroup>,
    selectedGroupId: String,
    reopenSheetSignal: Boolean,
    reopenSheetType: String = "blocked_apps",
    onReopenSheetHandled: () -> Unit,
    onBlockerClick: () -> Unit,
    onGroupAppsChange: (groupId: String, apps: List<AppItem>) -> Unit,
    onGroupScheduleChange: (groupId: String, schedule: TimeSlot) -> Unit,
    onAvatarClick: () -> Unit = {},
    onQuickFocusClick: () -> Unit = {},
    onPartyModeClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onEditLocationZoneClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    // ModalBottomSheet 專用的 State
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var activeSheet by remember { mutableStateOf(SheetType.NONE) }

    fun openSheet(type: SheetType) {
        activeSheet = type
    }

    fun closeSheet(onFinished: () -> Unit = {}) {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                activeSheet = SheetType.NONE
                onFinished()
            }
        }
    }

    // 重開 Sheet 的 Signal 處理
    LaunchedEffect(reopenSheetSignal) {
        if (reopenSheetSignal) {
            onReopenSheetHandled()
            openSheet(
                if (reopenSheetType == "location_zone") SheetType.LOCATION_ZONE
                else SheetType.BLOCKED_APPS
            )
        }
    }

    // 1. 主要畫面
    HomeScreen(
        onAvatarClick = onAvatarClick,
        onQuickFocusClick = onQuickFocusClick,
        onPartyModeClick = onPartyModeClick,
        onBlockedAppCardClick = { openSheet(SheetType.BLOCKED_APPS) },
        onLocationCardClick = { openSheet(SheetType.LOCATION_ZONE) },
        onSettingsClick = onSettingsClick
    )

    // 2. 只有當 activeSheet != NONE 時才掛載 BottomSheet (徹底解決閃現與叫不出來的問題)
    if (activeSheet != SheetType.NONE) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = SheetType.NONE },
            sheetState = sheetState,
            containerColor = if (activeSheet == SheetType.BLOCKED_APPS) Color(0xFF3B3B96) else Color(0xFFF5C8C8)
        ) {
            when (activeSheet) {
                SheetType.BLOCKED_APPS -> AutoBlockingSheetContent(
                    groups = groups,
                    selectedGroupId = selectedGroupId,
                    onAppsChange = onGroupAppsChange,
                    onScheduleChange = onGroupScheduleChange,
                    onBlockerClick = {
                        closeSheet { onBlockerClick() }
                    }
                )

                SheetType.LOCATION_ZONE -> LocationZoneSheetContent(
                    onEditClick = { closeSheet { onEditLocationZoneClick() } }
                )

                SheetType.NONE -> Unit
            }
        }
    }
}