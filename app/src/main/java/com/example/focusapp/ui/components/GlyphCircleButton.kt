package com.example.focusapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * A circle with a glyph from [FocusGlyphs] on top. The circle and the glyph
 * are styled separately: [container] takes any Brush (solid, gradient,
 * translucent) and [border] adds an outline - together enough for a
 * glass-style look. For a true backdrop blur, pass a blur modifier/effect
 * through [modifier] (it is applied before the circle is drawn).
 */
@Composable
fun GlyphCircleButton(
    glyph: ImageVector,
    contentDescription: String,
    container: Brush,
    glyphColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    border: BorderStroke? = null,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container, CircleShape)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = glyph,
            contentDescription = contentDescription,
            tint = glyphColor,
            modifier = Modifier.size(size),
        )
    }
}

/**
 * Figma edit button (edit.xml): an accent circle with a pencil cut out of
 * it, so the surface behind the button shows through the pencil - only the
 * circle's [color] is customizable.
 */
@Composable
fun EditButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Edit",
    color: Color = FocusTheme.colors.accent,
    size: Dp = 34.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.edit),
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(size),
        )
    }
}

/** Figma reject icon: a dark circle with a light X. */
@Composable
fun RejectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Discard",
    container: Brush = SolidColor(FocusTheme.colors.surfaceSunken),
    glyphColor: Color = FocusTheme.colors.onSurface,
    border: BorderStroke? = null,
    size: Dp = 34.dp,
) {
    GlyphCircleButton(
        glyph = FocusGlyphs.Close,
        contentDescription = contentDescription,
        container = container,
        glyphColor = glyphColor,
        onClick = onClick,
        modifier = modifier,
        size = size,
        border = border,
    )
}

/**
 * Figma confirm icon: an accent circle with a check. In Figma the check is
 * cut out of the circle; here it's drawn in [glyphColor], which defaults to
 * the card surface so it reads the same on the card.
 */
@Composable
fun ConfirmButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Confirm",
    enabled: Boolean = true,
    container: Brush = SolidColor(
        if (enabled) FocusTheme.colors.accent else FocusTheme.colors.onSurfaceMuted
    ),
    glyphColor: Color = FocusTheme.colors.surface,
    border: BorderStroke? = null,
    size: Dp = 34.dp,
) {
    GlyphCircleButton(
        glyph = FocusGlyphs.Check,
        contentDescription = contentDescription,
        container = container,
        glyphColor = glyphColor,
        onClick = onClick,
        modifier = modifier,
        size = size,
        border = border,
        enabled = enabled,
    )
}

/** Figma "previous step" button: a light circle with a dark left arrow. */
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Back",
    container: Brush = SolidColor(FocusTheme.colors.primaryAction.copy(alpha = 0.75f)),
    glyphColor: Color = FocusTheme.colors.surface,
    border: BorderStroke? = null,
    size: Dp = 34.dp,
) {
    GlyphCircleButton(
        glyph = FocusGlyphs.ArrowLeft,
        contentDescription = contentDescription,
        container = container,
        glyphColor = glyphColor,
        onClick = onClick,
        modifier = modifier,
        size = size,
        border = border,
    )
}

/** Figma "next step" button: an accent circle with a right arrow - same look as [ConfirmButton]. */
@Composable
fun NextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Next",
    enabled: Boolean = true,
    container: Brush = SolidColor(
        if (enabled) FocusTheme.colors.accent else FocusTheme.colors.onSurfaceMuted
    ),
    glyphColor: Color = FocusTheme.colors.surface,
    border: BorderStroke? = null,
    size: Dp = 34.dp,
) {
    GlyphCircleButton(
        glyph = FocusGlyphs.ArrowRight,
        contentDescription = contentDescription,
        container = container,
        glyphColor = glyphColor,
        onClick = onClick,
        modifier = modifier,
        size = size,
        border = border,
        enabled = enabled,
    )
}

/**
 * Figma Focus Mode "i" button (hints.xml). Unlike [GlyphCircleButton], the
 * "i" is cut out of the circle, so whatever is behind the button shows
 * through it - only the circle's [color] is customizable.
 */
@Composable
fun InfoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Info",
    color: Color = FocusTheme.colors.primaryAction,
    size: Dp = 43.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.hints),
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(size),
        )
    }
}

@Preview
@Composable
private fun GlyphCircleButtonPreview() {
    FocusAppTheme {
        val colors = FocusTheme.colors
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(colors.surface)
                .padding(12.dp),
        ) {
            RejectButton(onClick = {})
            ConfirmButton(onClick = {})
            ConfirmButton(onClick = {}, enabled = false)
            NextButton(onClick = {})
            EditButton(onClick = {})
            BackButton(onClick = {})
            InfoButton(onClick = {})
            // Glass-style example: translucent gradient + light rim.
            RejectButton(
                onClick = {},
                container = Brush.verticalGradient(
                    listOf(colors.onSurface.copy(alpha = 0.35f), colors.onSurface.copy(alpha = 0.08f))
                ),
                border = BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.45f)),
            )
        }
    }
}
