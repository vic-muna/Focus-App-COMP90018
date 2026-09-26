package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.ui.components.FocusBottomNavBar
import com.example.focusapp.ui.components.FocusIconButton
import com.example.focusapp.ui.components.FocusNavItem
import com.example.focusapp.ui.components.GreetingHeader
import com.example.focusapp.ui.components.QuickFocusButton
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** Home's bottom-nav destinations, in Figma order. */
enum class HomeNavTab(val item: FocusNavItem) {
    HOME(FocusNavItem(R.drawable.ic_home_fill, "Home")),
    LOCATION(FocusNavItem(R.drawable.ic_pin_alt_fill, "Location zone")),
    SCHEDULE(FocusNavItem(R.drawable.ic_clock_fill, "Schedule")),
    BLOCKED_APPS(FocusNavItem(R.drawable.ic_apps_add, "Blocked apps")),
}

/** Figma: "HomePage" frame (node 2:4). Layout only - behavior is hoisted to [HomeScreenWithSheet]. */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userName: String = "User",
    selectedTab: HomeNavTab = HomeNavTab.HOME,
    onSettingsClick: () -> Unit = {},
    onDashboardClick: () -> Unit = {},
    onQuickFocusClick: () -> Unit = {},
    onTabClick: (HomeNavTab) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FocusTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, end = 21.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            FocusIconButton(
                iconRes = R.drawable.ic_setting_fill,
                contentDescription = "Settings",
                onClick = onSettingsClick,
            )
        }

        GreetingHeader(
            userName = userName,
            subtitle = "Let's speed up your production",
            modifier = Modifier.padding(top = 12.dp),
        )

        Image(
            painter = painterResource(R.drawable.img_home_dashboard),
            contentDescription = "Focus history",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(top = 28.dp)
                .size(width = 285.dp, height = 268.dp)
                .clickable(onClick = onDashboardClick),
        )

        Spacer(Modifier.weight(1f).heightIn(min = 24.dp))

        QuickFocusButton(onClick = onQuickFocusClick)

        Spacer(Modifier.weight(1f).heightIn(min = 24.dp))

        FocusBottomNavBar(
            items = HomeNavTab.entries.map { it.item },
            selectedIndex = selectedTab.ordinal,
            onItemClick = { index -> onTabClick(HomeNavTab.entries[index]) },
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun HomeScreenPreview() {
    FocusAppTheme {
        HomeScreen()
    }
}
