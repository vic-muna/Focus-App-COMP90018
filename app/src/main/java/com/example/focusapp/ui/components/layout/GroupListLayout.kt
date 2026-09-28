package com.example.focusapp.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.card.AddItemCard
import com.example.focusapp.ui.components.card.FlyCardOverlay
import com.example.focusapp.ui.components.card.consumeTaps
import com.example.focusapp.ui.navigation.MainTab
import com.example.focusapp.ui.navigation.MainTabBar
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusSpacing
import com.example.focusapp.ui.theme.FocusTheme

/**
 * The page layout shared by the Time Focus and Wi-Fi tabs:
 *
 *   [header]            picture at the top
 *   [items]             one card per group
 *   "+" card            calls [onAddClick]
 *   bottom nav bar      [selectedTab] highlighted
 *
 * While [showCard] is true, [card] flies in over everything.
 * The `Modifier` it gets makes taps on the card not count as "outside".
 */
@Composable
fun GroupListLayout(
    selectedTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    addLabel: String,
    onAddClick: () -> Unit,
    showCard: Boolean,
    onOutsideCardClick: () -> Unit,
    header: @Composable () -> Unit,
    card: @Composable (Modifier) -> Unit,
    emptyText: String? = null,
    items: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusTheme.colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            header()

            Column(
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (emptyText != null) {
                    Text(
                        text = emptyText,
                        style = FocusTheme.typography.body,
                        color = FocusTheme.colors.onSurfaceMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                items()
                AddItemCard(onClick = onAddClick, onClickLabel = addLabel)
            }
        }

        MainTabBar(
            selectedTab = selectedTab,
            onTabClick = onTabClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = FocusSpacing.ScreenBottom),
        )

        if (showCard) {
            FlyCardOverlay(onOutsideClick = onOutsideCardClick) {
                card(Modifier.consumeTaps())
            }
        }
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun GroupListLayoutPreview() {
    FocusAppTheme {
        GroupListLayout(
            selectedTab = MainTab.SCHEDULE,
            onTabClick = {},
            addLabel = "Add",
            onAddClick = {},
            showCard = false,
            onOutsideCardClick = {},
            header = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(FocusTheme.colors.surface),
                )
            },
            card = {},
            emptyText = "Nothing here yet.",
        ) {}
    }
}
