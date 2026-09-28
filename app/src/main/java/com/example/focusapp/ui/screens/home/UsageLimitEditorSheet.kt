package com.example.focusapp.ui.screens.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.usagestats.hasUsageAccessPermission

// Same palette as ScheduleEditorSheet, so every picker stacked on the sheet matches.
private val SheetBgColor = Color(0xFF33386D)
private val CardBgColor = Color(0xFF252853)
private val SelectedBgColor = Color(0xFF474E91)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFA5ABC7)
private val WarningBgColor = Color(0xFF8A3B4B)

/**
 * Everything that differs between the "Max Open Times" and "Max Duration"
 * editors - both share the one [UsageLimitEditorSheet] layout below.
 */
data class UsageLimitSpec(
    val title: String,
    val subtitle: String,
    val min: Int,
    val max: Int,
    val step: Int,
    /** Value the stepper starts from when the limit is first switched on. */
    val defaultValue: Int,
    val presets: List<Int>,
    val format: (Int) -> String
)

val MaxOpensLimitSpec = UsageLimitSpec(
    title = "Max Open Times",
    subtitle = "How many times each app in this group can be opened per day, during its active time",
    min = 1,
    max = 50,
    step = 1,
    defaultValue = 5,
    presets = listOf(1, 3, 5, 10),
    format = ::formatOpenTimes
)

val MaxDurationLimitSpec = UsageLimitSpec(
    title = "Max Duration",
    subtitle = "How long each app in this group can be used per day, during its active time",
    min = 1,
    max = 12 * 60,
    step = 1,
    defaultValue = 30,
    presets = listOf(15, 30, 60, 120),
    format = ::formatLimitMinutes
)

fun formatOpenTimes(count: Int): String = if (count == 1) "1 time" else "$count times"

/** "45 min" / "1 h" / "1 h 30 min". */
fun formatLimitMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$rest min"
        rest == 0 -> "$hours h"
        else -> "$hours h $rest min"
    }
}

/**
 * [David Shiau, 2026-09-26] Editor for one daily usage limit: an on/off
 * switch ("No limit" when off), a -/+ stepper, and quick presets. Every
 * change applies immediately via [onValueChange] (null = no limit), the
 * same "no Save button" behavior as ScheduleEditorSheet.
 */
@Composable
fun UsageLimitEditorSheet(
    spec: UsageLimitSpec,
    value: Int?,
    onValueChange: (Int?) -> Unit
) {
    // Remembers the last number used, so switching the limit off and back
    // on restores it instead of jumping back to the default.
    var rememberedValue by remember { mutableIntStateOf(value ?: spec.defaultValue) }
    val lastValue = value ?: rememberedValue
    val isEnabled = value != null

    fun update(newValue: Int) {
        val coerced = newValue.coerceIn(spec.min, spec.max)
        rememberedValue = coerced
        onValueChange(coerced)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SheetBgColor)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(text = spec.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = spec.subtitle, fontSize = 13.sp, color = TextSecondary)

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBgColor)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Limit this group",
                fontSize = 16.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = { checked -> if (checked) update(lastValue) else onValueChange(null) },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = SelectedBgColor,
                    uncheckedTrackColor = CardBgColor
                )
            )
        }

        // [David Shiau, 2026-09-26] A limit is only enforced (by
        // FocusAccessibilityService) with both permissions granted - say so
        // rather than letting it silently do nothing.
        val context = LocalContext.current
        val isAccessibilityEnabled by AccessibilityBridge.isServiceConnected.collectAsState()
        val hasUsageAccess = hasUsageAccessPermission(context)
        if (isEnabled && (!isAccessibilityEnabled || !hasUsageAccess)) {
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WarningBgColor)
                    .clickable {
                        val action = if (!isAccessibilityEnabled) {
                            Settings.ACTION_ACCESSIBILITY_SETTINGS
                        } else {
                            Settings.ACTION_USAGE_ACCESS_SETTINGS
                        }
                        context.startActivity(Intent(action))
                    }
                    .padding(16.dp)
            ) {
                val missing = listOfNotNull(
                    "Accessibility".takeIf { !isAccessibilityEnabled },
                    "Usage Access".takeIf { !hasUsageAccess }
                )
                Text(
                    text = "This limit won't be enforced until Focus has the ${missing.joinToString(" and ")} " +
                        "permission${if (missing.size > 1) "s" else ""}. Tap to open Settings.",
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Stepper - dimmed (and inert) while the limit is off.
        val contentAlpha = if (isEnabled) 1f else 0.4f
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBgColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepperButton(
                icon = Icons.Filled.Remove,
                contentDescription = "Decrease",
                enabled = isEnabled && lastValue > spec.min,
                onClick = { update(lastValue - spec.step) }
            )
            Text(
                text = if (isEnabled) spec.format(lastValue) else "No limit",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary.copy(alpha = contentAlpha),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            StepperButton(
                icon = Icons.Filled.Add,
                contentDescription = "Increase",
                enabled = isEnabled && lastValue < spec.max,
                onClick = { update(lastValue + spec.step) }
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(text = "Quick pick", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            spec.presets.forEach { preset ->
                val isSelected = isEnabled && value == preset
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) SelectedBgColor else CardBgColor)
                        // Picking a preset also switches the limit on.
                        .clickable { update(preset) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = spec.format(preset),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) SelectedBgColor else SelectedBgColor.copy(alpha = 0.3f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextPrimary.copy(alpha = if (enabled) 1f else 0.4f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UsageLimitEditorSheetPreview() {
    UsageLimitEditorSheet(spec = MaxDurationLimitSpec, value = 30, onValueChange = {})
}
