package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** Viewport shared by the Figma confirm/reject icons (confirm_icon.xml, reject_icon.xml). */
private const val GLYPH_VIEWPORT = 34f

/**
 * Just the symbols from the Figma confirm/reject icons, without their
 * circles - drawn in the same 34x34 viewport so they line up when placed
 * over a full-size circle (see [GlyphCircleButton]). Single color: tint
 * them with Icon's `tint`.
 */
object FocusGlyphs {

    /** The "X" from reject_icon.xml: two 3.125-wide round-capped strokes. */
    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "FocusGlyphs.Close",
            defaultWidth = GLYPH_VIEWPORT.dp,
            defaultHeight = GLYPH_VIEWPORT.dp,
            viewportWidth = GLYPH_VIEWPORT,
            viewportHeight = GLYPH_VIEWPORT,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 3.125f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(24.375f, 9.375f)
                lineTo(9.375f, 24.375f)
                moveTo(9.375f, 9.375f)
                lineTo(24.375f, 24.375f)
            }
        }.build()
    }

    /** The check mark cut out of confirm_icon.xml's circle, as a filled shape. */
    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "FocusGlyphs.Check",
            defaultWidth = GLYPH_VIEWPORT.dp,
            defaultHeight = GLYPH_VIEWPORT.dp,
            viewportWidth = GLYPH_VIEWPORT,
            viewportHeight = GLYPH_VIEWPORT,
        ).addPath(
            pathData = PathParser().parsePathString(CHECK_PATH).toNodes(),
            fill = SolidColor(Color.Black),
        ).build()
    }

    private const val CHECK_PATH =
        "M25.5752 9.80957C24.7797 9.14673 23.5975 9.25432 22.9346 10.0498L16.1885 18.1455" +
            "C15.5386 18.9254 15.1726 19.3567 14.8809 19.6221C14.8774 19.6252 14.8735 19.6278 14.8701 19.6309" +
            "C14.8664 19.6281 14.8622 19.6259 14.8584 19.623C14.5438 19.3852 14.1399 18.9886 13.4219 18.2705" +
            "L10.7012 15.5488C9.96894 14.8166 8.78106 14.8166 8.04883 15.5488" +
            "C7.31661 16.2811 7.3166 17.4689 8.04883 18.2012L10.7705 20.9219" +
            "C11.41 21.5613 12.0251 22.1827 12.5957 22.6143C13.1712 23.0494 13.9141 23.4606 14.8535 23.4717" +
            "L15.0439 23.4688L15.2344 23.4541C16.1686 23.3578 16.8707 22.8818 17.4043 22.3965" +
            "C17.9336 21.915 18.4903 21.2408 19.0693 20.5459L25.8154 12.4502" +
            "C26.4783 11.6547 26.3707 10.4725 25.5752 9.80957Z"
}

@Preview
@Composable
private fun FocusGlyphsPreview() {
    FocusAppTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(FocusTheme.colors.surface)
                .padding(8.dp),
        ) {
            Icon(FocusGlyphs.Close, contentDescription = null, tint = FocusTheme.colors.onSurface, modifier = Modifier.size(34.dp))
            Icon(FocusGlyphs.Check, contentDescription = null, tint = FocusTheme.colors.accent, modifier = Modifier.size(34.dp))
        }
    }
}
