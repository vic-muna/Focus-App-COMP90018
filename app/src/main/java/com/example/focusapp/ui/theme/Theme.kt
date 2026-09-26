package com.example.focusapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalFocusColors = staticCompositionLocalOf { DefaultFocusColors }
private val LocalFocusTypography = staticCompositionLocalOf { DefaultFocusTypography }

/**
 * Root theme. Wrap the app once (MainActivity) - every component then reads
 * `FocusTheme.colors` / `FocusTheme.typography`.
 *
 * [applyToMaterial]: when true, Material3 components (dialogs, sheets, default
 * Text color...) also take their colors from [colors]. It defaults to false
 * because the screens not yet migrated to the new design still rely on
 * Material's default light scheme - flip it on once they're migrated.
 */
@Composable
fun FocusAppTheme(
    colors: FocusColors = DefaultFocusColors,
    typography: FocusTypography = DefaultFocusTypography,
    applyToMaterial: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalFocusColors provides colors,
        LocalFocusTypography provides typography,
    ) {
        if (applyToMaterial) {
            MaterialTheme(colorScheme = colors.toMaterialColorScheme(), content = content)
        } else {
            MaterialTheme(content = content)
        }
    }
}

/** Accessors for the current theme's tokens, e.g. `FocusTheme.colors.background`. */
object FocusTheme {
    val colors: FocusColors
        @Composable @ReadOnlyComposable get() = LocalFocusColors.current

    val typography: FocusTypography
        @Composable @ReadOnlyComposable get() = LocalFocusTypography.current
}

private fun FocusColors.toMaterialColorScheme(): ColorScheme = darkColorScheme(
    primary = primaryAction,
    onPrimary = onPrimaryAction,
    secondary = accent,
    onSecondary = background,
    background = background,
    onBackground = onSurface,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceSelected,
    onSurfaceVariant = onPrimaryAction,
    error = rejection,
    onError = pure,
)
