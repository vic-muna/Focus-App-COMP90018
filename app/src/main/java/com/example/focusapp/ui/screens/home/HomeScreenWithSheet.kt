package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class SheetType { NONE, BLOCKED_APPS, LOCATION_ZONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenWithSheet(
    onAvatarClick: () -> Unit = {},
    onQuickFocusClick: () -> Unit = {},
    onMapClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    var activeSheet by remember { mutableStateOf(SheetType.NONE) }

    // 關鍵:skipHiddenState = false,才允許 Sheet 完全隱藏,不是只能停在 peek
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf(generateFakeApps(8)) }

    val peekHeight = if (activeSheet == SheetType.NONE) 0.dp else 300.dp
    var timeSlots by remember { mutableStateOf(generateFakeTimeSlots()) }

    fun closeSheet() {
        activeSheet = SheetType.NONE
        scope.launch { scaffoldState.bottomSheetState.hide() }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        sheetContent = {
            when (activeSheet) {
                SheetType.BLOCKED_APPS -> {
                    BlockedAppsSheetContent(
                        apps = apps,
                        timeSlots = timeSlots,
                        onEditClick = { /* TODO: 進入編輯模式 */ }
                    )
                }
                SheetType.LOCATION_ZONE -> {

                    LocationZoneSheetContent(onEditClick = { /* TODO: 進入編輯模式 */ }
                    )
                }
                SheetType.NONE -> {}
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HomeScreen(
                // Sheet 開啟時,點畫面空白處(卡片以外的地方)會觸發收合
                modifier = if (activeSheet != SheetType.NONE) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { closeSheet() }
                } else {
                    Modifier
                },
                onAvatarClick = onAvatarClick,
                onQuickFocusClick = onQuickFocusClick,
                onMapClick = onMapClick,
                onBlockedAppCardClick = {
                    activeSheet = SheetType.BLOCKED_APPS
                    scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                },
                onLocationCardClick = {
                    activeSheet = SheetType.LOCATION_ZONE
                    scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                },
                onSettingsClick = onSettingsClick
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenWithSheetPreview() {
    HomeScreenWithSheet()
}