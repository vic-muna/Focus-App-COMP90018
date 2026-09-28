package com.example.focusapp.ui.screens.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.BackgroundTheme
import com.example.focusapp.ui.theme.BackgroundThemes
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** Relative bar widths of the decorative barcode, repeated across the card. */
private val BarcodePattern = listOf(1, 2, 1, 1, 3, 1, 2, 2, 1, 3, 1, 1, 2, 1)

/**
 * The dashboard's "ID card" for the current background theme: its art on
 * the left (with a "Change" button), its name and intro on the right, and
 * a decorative "identified card" footer. Tapping anywhere calls
 * [onChangeClick] (opens the theme picker).
 */
@Composable
fun ThemeIdCard(
    theme: BackgroundTheme,
    onChangeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .clickable(role = Role.Button, onClickLabel = "Change background", onClick = onChangeClick),
    ) {
        Box(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight(),
        ) {
            Image(
                painter = painterResource(theme.homeArt),
                contentDescription = theme.name,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxHeight(),
            )
            Text(
                text = "Change",
                style = typography.listLabel,
                color = colors.onSurface,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.surfaceSunken)
                    .clickable(role = Role.Button, onClick = onChangeClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = theme.name,
                style = typography.timer,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = theme.intro,
                style = typography.caption,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** A purely decorative barcode strip. */
@Composable
private fun Barcode(modifier: Modifier = Modifier) {
    val color = FocusTheme.colors.onSurfaceMuted

}

@Preview(widthDp = 360)
@Composable
private fun ThemeIdCardPreview() {
    FocusAppTheme {
        ThemeIdCard(
            theme = BackgroundThemes.Scene ,
            onChangeClick = {},
            modifier = Modifier
                .background(FocusTheme.colors.background)
                .padding(16.dp),
        )
    }
}