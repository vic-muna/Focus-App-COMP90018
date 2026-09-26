package com.example.focusapp.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** One entry of [FocusBottomNavBar]. */
data class FocusNavItem(
    @DrawableRes val iconRes: Int,
    val contentDescription: String,
)

/**
 * Figma: "Navigation Buttom" - a pill-shaped bar with icon-only items and a
 * pill indicator that slides behind the selected one. The indicator keeps
 * a fixed [indicatorWidth] (Figma: ~71 dp) centered in its item, however
 * many items there are.
 */
@Composable
fun FocusBottomNavBar(
    items: List<FocusNavItem>,
    selectedIndex: Int,
    onItemClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    indicatorWidth: Dp = 71.dp,
) {
    val colors = FocusTheme.colors

    BoxWithConstraints(
        modifier = modifier
            .widthIn(max = 299.dp)
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(colors.surface)
    ) {
        val itemWidth = maxWidth / items.size
        val pillWidth = minOf(indicatorWidth, itemWidth)
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex + (itemWidth - pillWidth) / 2,
            label = "navIndicatorOffset",
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(pillWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(colors.surfaceSelected)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(role = Role.Tab) { onItemClick(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = item.contentDescription,
                        tint = colors.onSurface,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun FocusBottomNavBarPreview() {
    FocusAppTheme {
        FocusBottomNavBar(
            items = listOf(
                FocusNavItem(R.drawable.ic_home_fill, "Home"),
                FocusNavItem(R.drawable.ic_pin_alt_fill, "Location"),
                FocusNavItem(R.drawable.ic_clock_fill, "Schedule"),
                FocusNavItem(R.drawable.ic_apps_add, "Blocked apps"),
            ),
            selectedIndex = 0,
            onItemClick = {},
        )
    }
}
