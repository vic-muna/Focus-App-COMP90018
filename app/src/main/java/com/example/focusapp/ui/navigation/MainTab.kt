package com.example.focusapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.focusapp.R
import com.example.focusapp.ui.components.FocusBottomNavBar
import com.example.focusapp.ui.components.FocusNavItem
import com.example.focusapp.ui.theme.FocusAppTheme

/** The app's bottom-nav destinations, in Figma order. */
enum class MainTab(val item: FocusNavItem) {
    HOME(FocusNavItem(R.drawable.ic_home_fill, "Home")),
    LOCATION(FocusNavItem(R.drawable.ic_pin_alt_fill, "Location focus")),
    BLOCKED_APPS(FocusNavItem(R.drawable.ic_apps_add, "Blocked apps")),
}

/** [FocusBottomNavBar] preloaded with every [MainTab] - drop this into any top-level screen. */
@Composable
fun MainTabBar(
    selectedTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusBottomNavBar(
        items = MainTab.entries.map { it.item },
        selectedIndex = selectedTab.ordinal,
        onItemClick = { index -> onTabClick(MainTab.entries[index]) },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun MainTabBarPreview() {
    FocusAppTheme {
        MainTabBar(selectedTab = MainTab.LOCATION, onTabClick = {})
    }
}
