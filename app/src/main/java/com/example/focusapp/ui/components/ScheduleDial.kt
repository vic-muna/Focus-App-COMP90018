package com.example.focusapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

const val MINUTES_PER_DAY = 24 * 60

/** Dragging snaps to this many minutes. */
private const val SNAP_MINUTES = 5

/** A touch within this many degrees of a handle grabs that handle. */
private const val HANDLE_GRAB_DEGREES = 15f

private enum class DragTarget { START, END, BOTH }

/**
 * An Apple-Bedtime-style 24-hour dial: midnight at the top, one lap per day,
 * with an accent arc from [startMinutes] to [endMinutes] (minutes after
 * midnight, 0..1439; the arc may cross midnight). Drag either handle to move
 * that end, or drag the arc itself to shift the whole range.
 */
@Composable
fun ScheduleDial(
    startMinutes: Int,
    endMinutes: Int,
    onChange: (startMinutes: Int, endMinutes: Int) -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 250.dp,
    ringWidth: Dp = 36.dp,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography
    val textMeasurer = rememberTextMeasurer()
    val startIcon = rememberVectorPainter(Icons.Filled.Bedtime)
    val endIcon = rememberVectorPainter(Icons.Filled.Alarm)
    val moonIcon = rememberVectorPainter(Icons.Filled.DarkMode)
    val sunIcon = rememberVectorPainter(Icons.Filled.LightMode)

    // Read the latest values inside the long-lived drag gesture.
    val currentStart by rememberUpdatedState(startMinutes)
    val currentEnd by rememberUpdatedState(endMinutes)
    val currentOnChange by rememberUpdatedState(onChange)

    Canvas(
        modifier = modifier
            .size(diameter)
            .semantics {
                contentDescription = "Quiet time from ${formatClock(startMinutes)} to ${formatClock(endMinutes)}"
            }
            .pointerInput(Unit) {
                var target = DragTarget.BOTH
                var lastMinutes = 0
                // Unsnapped positions, so slow drags still add up across snap steps.
                var rawStart = 0f
                var rawEnd = 0f
                detectDragGestures(
                    onDragStart = { offset ->
                        val touched = offsetToMinutes(offset, size.width / 2f, size.height / 2f)
                        val degToStart = angularDistance(touched, currentStart) * 360f / MINUTES_PER_DAY
                        val degToEnd = angularDistance(touched, currentEnd) * 360f / MINUTES_PER_DAY
                        target = when {
                            degToStart <= HANDLE_GRAB_DEGREES && degToStart <= degToEnd -> DragTarget.START
                            degToEnd <= HANDLE_GRAB_DEGREES -> DragTarget.END
                            isWithinArc(touched, currentStart, currentEnd) -> DragTarget.BOTH
                            degToStart <= degToEnd -> DragTarget.START
                            else -> DragTarget.END
                        }
                        lastMinutes = touched
                        rawStart = currentStart.toFloat()
                        rawEnd = currentEnd.toFloat()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val now = offsetToMinutes(change.position, size.width / 2f, size.height / 2f)
                        // Shortest signed step around the dial, so crossing midnight doesn't jump.
                        var delta = now - lastMinutes
                        if (delta > MINUTES_PER_DAY / 2) delta -= MINUTES_PER_DAY
                        if (delta < -MINUTES_PER_DAY / 2) delta += MINUTES_PER_DAY
                        lastMinutes = now
                        when (target) {
                            DragTarget.START -> rawStart += delta
                            DragTarget.END -> rawEnd += delta
                            DragTarget.BOTH -> { rawStart += delta; rawEnd += delta }
                        }
                        currentOnChange(snap(rawStart), snap(rawEnd))
                    },
                )
            },
    ) {
        val ringPx = ringWidth.toPx()
        val radius = (size.minDimension - ringPx) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val arcTopLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)

        // Track
        drawCircle(colors.surface, radius = radius, center = center, style = Stroke(ringPx))

        // Hour labels inside the ring: every 2 hours, with the quarter hours named.
        val labelRadius = radius - ringPx / 2f - 18.dp.toPx()
        for (hour in 0 until 24 step 2) {
            val label = when (hour) {
                0 -> "12am"
                6 -> "6am"
                12 -> "12pm"
                18 -> "6pm"
                else -> (if (hour > 12) hour - 12 else hour).toString()
            }
            val style: TextStyle = if (hour % 6 == 0) typography.listLabel else typography.caption
            val measured = textMeasurer.measure(label, style.copy(color = colors.onSurface))
            val p = minutesToOffset(hour * 60, center, labelRadius)
            drawText(measured, topLeft = Offset(p.x - measured.size.width / 2f, p.y - measured.size.height / 2f))
        }

        // Night / day hints just inside 12am and 12pm.
        val hintSize = 14.dp.toPx()
        drawIcon(moonIcon, minutesToOffset(0, center, labelRadius - 22.dp.toPx()), hintSize, colors.onSurfaceMuted)
        drawIcon(sunIcon, minutesToOffset(12 * 60, center, labelRadius - 22.dp.toPx()), hintSize, colors.accent)

        // Selected range
        val sweep = ((endMinutes - startMinutes + MINUTES_PER_DAY) % MINUTES_PER_DAY) * 360f / MINUTES_PER_DAY
        drawArc(
            color = colors.accent,
            startAngle = minutesToCanvasDegrees(startMinutes),
            sweepAngle = sweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = ringPx - 4.dp.toPx(), cap = StrokeCap.Round),
        )

        // Grip marks at the arc's midpoint, hinting it can be dragged as a whole.
        if (sweep > 40f) {
            val midMinutes = startMinutes + ((endMinutes - startMinutes + MINUTES_PER_DAY) % MINUTES_PER_DAY) / 2
            val gripHalf = 5.dp.toPx()
            for (offsetMinutes in listOf(-8, 8)) {
                val m = midMinutes + offsetMinutes
                drawLine(
                    color = colors.onPrimaryAction.copy(alpha = 0.5f),
                    start = minutesToOffset(m, center, radius - gripHalf),
                    end = minutesToOffset(m, center, radius + gripHalf),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }

        // Handles
        val handleIconSize = 16.dp.toPx()
        drawIcon(startIcon, minutesToOffset(startMinutes, center, radius), handleIconSize, colors.onPrimaryAction)
        drawIcon(endIcon, minutesToOffset(endMinutes, center, radius), handleIconSize, colors.onPrimaryAction)
    }
}

private fun DrawScope.drawIcon(painter: VectorPainter, center: Offset, sizePx: Float, tint: Color) {
    translate(left = center.x - sizePx / 2f, top = center.y - sizePx / 2f) {
        with(painter) { draw(Size(sizePx, sizePx), colorFilter = ColorFilter.tint(tint)) }
    }
}

/** Canvas angles start at 3 o'clock; the dial's midnight is at 12 o'clock. */
private fun minutesToCanvasDegrees(minutes: Int): Float = minutes * 360f / MINUTES_PER_DAY - 90f

private fun minutesToOffset(minutes: Int, center: Offset, radius: Float): Offset {
    val radians = minutesToCanvasDegrees(minutes) * PI / 180.0
    return Offset(center.x + (radius * cos(radians)).toFloat(), center.y + (radius * sin(radians)).toFloat())
}

private fun offsetToMinutes(offset: Offset, cx: Float, cy: Float): Int {
    // atan2 with x/y swapped so 0 = straight up, increasing clockwise.
    val degrees = Math.toDegrees(atan2((offset.x - cx).toDouble(), (cy - offset.y).toDouble()))
    return (((degrees + 360) % 360) / 360.0 * MINUTES_PER_DAY).roundToInt() % MINUTES_PER_DAY
}

private fun angularDistance(a: Int, b: Int): Int {
    val d = abs(a - b) % MINUTES_PER_DAY
    return minOf(d, MINUTES_PER_DAY - d)
}

private fun isWithinArc(minutes: Int, start: Int, end: Int): Boolean {
    val length = (end - start + MINUTES_PER_DAY) % MINUTES_PER_DAY
    val fromStart = (minutes - start + MINUTES_PER_DAY) % MINUTES_PER_DAY
    return fromStart <= length
}

private fun snap(rawMinutes: Float): Int {
    val snapped = (rawMinutes / SNAP_MINUTES).roundToInt() * SNAP_MINUTES
    return ((snapped % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
}

/** e.g. 1330 -> "10:10 PM", 532 -> "8:52 AM". */
fun formatClock(minutes: Int): String {
    val hour = minutes / 60
    val minute = minutes % 60
    val suffix = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%d:%02d %s".format(displayHour, minute, suffix)
}

/** e.g. 642 -> "10 h 42 m" - the length of a start..end range that may cross midnight. */
fun formatDuration(startMinutes: Int, endMinutes: Int): String {
    val length = (endMinutes - startMinutes + MINUTES_PER_DAY) % MINUTES_PER_DAY
    return "${length / 60} h ${length % 60} m"
}

@Preview
@Composable
private fun ScheduleDialPreview() {
    FocusAppTheme {
        var start by remember { mutableIntStateOf(22 * 60 + 10) }
        var end by remember { mutableIntStateOf(8 * 60 + 52) }
        ScheduleDial(
            startMinutes = start,
            endMinutes = end,
            onChange = { s, e -> start = s; end = e },
            modifier = Modifier
                .background(FocusTheme.colors.surfaceSunken)
                .padding(16.dp),
        )
    }
}
