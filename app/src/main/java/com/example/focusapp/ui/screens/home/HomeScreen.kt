package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.focusapp.ui.navigation.MainTab
import com.example.focusapp.ui.navigation.MainTabBar
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.draw.clipToBounds
import com.example.focusapp.ui.theme.FocusSpacing
import androidx.annotation.DrawableRes
import com.example.focusapp.ui.components.bar.SettingsTopBar
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
/**
 * img_home_dashboard.png is 960 x 903 px, but the artwork only covers a
 * 692 x 795 px area (the rest is background-colored margin), sitting ~19 px
 * below center. The image is scaled so the artwork itself is
 * [DashboardArtWidth] wide, and the margin is clipped away.
 */
private val DashboardArtWidth = 285.dp
private val DashboardArtHeight = DashboardArtWidth * (795f / 692f)
private val DashboardImageWidth = DashboardArtWidth * (960f / 692f)
private val DashboardImageHeight = DashboardArtWidth * (903f / 692f)
private val DashboardArtOffsetY = DashboardArtWidth * (-19.5f / 692f)

/** Figma: "HomePage" frame (node 2:4). Layout only - behavior is hoisted to [HomeScreenWithSheet]. */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userName: String = "User",
    // The picked background theme's Home art (see BackgroundThemes).
    @DrawableRes dashboardArt: Int = R.drawable.img_home_dashboard,
    selectedTab: MainTab = MainTab.HOME,
    onSettingsClick: () -> Unit = {},
    onPartyClick: () -> Unit = {},
    onDashboardClick: () -> Unit = {},
    onQuickFocusTap: () -> Unit = {},
    onQuickFocusStart: () -> Unit = {},
    onQuickFocusSettings: () -> Unit = {},
    // A short message above Quick Focus (e.g. how to use it), or null.
    quickFocusHint: String? = null,
    onTabClick: (MainTab) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FocusTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SettingsTopBar(onSettingsClick = onSettingsClick, onPartyClick = onPartyClick)

        GreetingHeader(
            userName = userName,
            subtitle = "Let's speed up your production",
            modifier = Modifier.padding(top = 12.dp),
        )

        Box(
            modifier = Modifier
                .padding(top = 28.dp)
                .size(width = DashboardArtWidth, height = DashboardArtHeight)
                .clipToBounds()
                .clickable(onClick = onDashboardClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(dashboardArt),
                contentDescription = "Focus history",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    // requiredSize lets the image overflow the box; clipToBounds trims the margin.
                    .requiredSize(width = DashboardImageWidth, height = DashboardImageHeight)
                    .offset(y = DashboardArtOffsetY),
            )
        }

        Spacer(Modifier.height(40.dp))

        Box(contentAlignment = Alignment.Center) {
            QuickFocusButton(
                onTap = onQuickFocusTap,
                onHoldStart = onQuickFocusStart,
                onHoldSettings = onQuickFocusSettings,
            )
            if (quickFocusHint != null) {
                QuickFocusHint(
                    text = quickFocusHint,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-40).dp),
                )
            }
        }

        Spacer(Modifier.weight(1f).heightIn(min = 24.dp))

        MainTabBar(selectedTab = selectedTab, onTabClick = onTabClick)

        Spacer(Modifier.height(FocusSpacing.ScreenBottom))
    }
}

/** A small pill with a message, shown above the Quick Focus button. */
@Composable
private fun QuickFocusHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = FocusTheme.typography.caption,
        color = FocusTheme.colors.onPrimaryAction,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(FocusTheme.colors.primaryAction, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun HomeScreenPreview() {
    FocusAppTheme {
        HomeScreen()
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun HomeScreenHintPreview() {
    FocusAppTheme {
        HomeScreen(quickFocusHint = "Hold 1 sec to start focusing\nHold 3 sec to choose apps")
    }
}
