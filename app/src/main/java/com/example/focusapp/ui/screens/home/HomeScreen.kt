package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.Text
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import com.example.focusapp.ui.theme.FocusSpacing
import androidx.annotation.DrawableRes
import com.example.focusapp.ui.components.bar.SettingsTopBar
import com.example.focusapp.ui.components.card.RunBadge
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
    // False for art without its own frame (the shop backgrounds' thumbnails) - see BackgroundTheme.homeArtHasFrame.
    dashboardArtHasFrame: Boolean = true,
    // True for a Run background (BackgroundTheme.isRun): its art gets a "RUN" badge.
    dashboardArtIsRun: Boolean = false,
    selectedTab: MainTab = MainTab.HOME,
    onSettingsClick: () -> Unit = {},
    onPartyClick: () -> Unit = {},
    onDashboardClick: () -> Unit = {},
    onQuickFocusClick: () -> Unit = {},
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
            if (dashboardArtHasFrame) {
                Image(
                    painter = painterResource(dashboardArt),
                    contentDescription = "Focus history",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        // requiredSize lets the image overflow the box; clipToBounds trims the margin.
                        .requiredSize(width = DashboardImageWidth, height = DashboardImageHeight)
                        .offset(y = DashboardArtOffsetY),
                )
            } else {
                // A shop thumbnail: it has its own dark frame and rounded corners, so show it whole.
                Image(
                    painter = painterResource(dashboardArt),
                    contentDescription = "Focus history",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // [Claude, 2026-10-04] On the artwork itself now, centered, bigger - a caption
            // underneath read as part of the layout, not as a label FOR the picture above it.
            // The Image's own contentDescription ("Focus history") already covers screen
            // readers; this is the sighted-user equivalent, not a second tap target - the
            // whole Box is already one. A translucent pill behind the text (rather than relying
            // on the text color alone) keeps it readable regardless of which background theme's
            // art is showing underneath - dashboardArt changes per theme, this doesn't.
            Text(
                text = "History",
                style = FocusTheme.typography.tileTitle,
                color = FocusTheme.colors.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(FocusTheme.colors.surface.copy(alpha = 0.72f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )

            if (dashboardArtIsRun) RunBadge(Modifier.align(Alignment.TopEnd).padding(16.dp))
        }

        Spacer(Modifier.height(40.dp))

        QuickFocusButton(onClick = onQuickFocusClick)

        Spacer(Modifier.weight(1f).heightIn(min = 24.dp))

        MainTabBar(selectedTab = selectedTab, onTabClick = onTabClick)

        Spacer(Modifier.height(FocusSpacing.ScreenBottom))
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun HomeScreenPreview() {
    FocusAppTheme {
        HomeScreen()
    }
}

