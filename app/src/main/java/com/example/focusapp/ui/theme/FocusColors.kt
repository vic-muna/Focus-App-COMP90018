package com.example.focusapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * FocusColors
 * -----------
 * Semantic color roles for the app. Components and screens read colors
 * ONLY from here (via `FocusTheme.colors`), never from [Palette] or raw hex
 * values - so switching theme means building a different FocusColors, with
 * no component changes.
 */
@Immutable
data class FocusColors(
    /** Full-screen background. */
    val background: Color,
    /** Raised containers on the background (bottom nav bar, cards, banners). */
    val surface: Color,
    /** Highlight behind the selected item inside a [surface] (nav indicator). */
    val surfaceSelected: Color,
    /** Recessed areas inside a [surface] (text fields, slider tracks, secondary buttons). */
    val surfaceSunken: Color,
    /** Icons and text drawn on [background] / [surface]. */
    val onSurface: Color,
    /** Placeholder / disabled text and icons on [surface] or [surfaceSunken]. */
    val onSurfaceMuted: Color,
    /** Accent text and "on" states, e.g. the Home greeting, a switched-on switch. */
    val accent: Color,
    /** Primary call-to-action fill (Quick Focus button). */
    val primaryAction: Color,
    /** Text/icons drawn on [primaryAction] or [accent]. */
    val onPrimaryAction: Color,
    /** Positive result (confirm buttons, success states). */
    val confirm: Color,
    /** Negative result (reject/cancel buttons, errors). */
    val rejection: Color,
    /** Attention states (reminders, auto-focus suggestions). */
    val notification: Color,
    /** Pure white, for content that must stay white in every theme. */
    val pure: Color,
    /** Saved focus zone on the map. */
    val mapZone: Color,
    /** Zone being created on the map. */
    val mapZoneNew: Color,
)

/** Default theme, matching the Figma "The Focus" design. */
val DefaultFocusColors = FocusColors(
    background = Palette.AbyssalBlue,
    surface = Palette.Fantastic,
    surfaceSelected = Palette.Oatmeal,
    surfaceSunken = Palette.AbyssalBlue,
    onSurface = Palette.Palladian,
    onSurfaceMuted = Palette.Palladian.copy(alpha = 0.3f),
    accent = Palette.Flame,
    primaryAction = Palette.Palladian,
    onPrimaryAction = Palette.Fantastic,
    confirm = Palette.Confirm,
    rejection = Palette.Rejection,
    notification = Palette.Notification,
    pure = Palette.Pure,
    mapZone = Palette.Vermilion,
    mapZoneNew = Palette.Turquoise,
)
