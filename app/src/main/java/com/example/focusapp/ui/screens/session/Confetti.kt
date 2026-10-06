package com.example.focusapp.ui.screens.session

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.focusapp.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val CONFETTI_DURATION_MILLIS = 3_200
private const val PIECES_PER_CANNON = 70

// In screen heights per second (squared), so the burst looks the same on every screen size.
private const val GRAVITY = 1.5f
private const val MIN_SPEED = 1.1f
private const val MAX_SPEED = 1.7f

private val ConfettiColors = listOf(
    Palette.Flame, Palette.Turquoise, Palette.Vermilion, Palette.Palladian, Palette.Confirm, Palette.Notification,
)

/** One ribbon. Speeds are in screen heights, so they scale with the canvas. */
private class ConfettiPiece(
    /** 0 = left edge, 1 = right edge. */
    val startX: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val widthDp: Float,
    val lengthDp: Float,
    val spinDegreesPerSecond: Float,
    val swayPhase: Float,
)

/**
 * Two confetti cannons, one in each bottom corner, firing ribbons up and inwards once. They arc,
 * flutter and fade out. Draws nothing after the burst, so it can stay in the layout.
 */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, seed: Int = 0) {
    val pieces = remember(seed) { makePieces(Random(seed)) }
    val progress = remember(seed) { Animatable(0f) }
    LaunchedEffect(seed) {
        progress.animateTo(1f, tween(CONFETTI_DURATION_MILLIS, easing = LinearEasing))
    }

    Canvas(modifier) {
        val t = progress.value * CONFETTI_DURATION_MILLIS / 1000f
        if (progress.value >= 1f) return@Canvas
        val unit = size.height
        // Fade out over the last third.
        val alpha = ((1f - progress.value) * 3f).coerceIn(0f, 1f)

        pieces.forEach { piece ->
            // Air drag slows the sideways speed, so the ribbons fall almost straight at the end.
            val drag = 1f / (1f + t * 1.4f)
            val sway = sin(t * 6f + piece.swayPhase) * 0.015f
            val x = piece.startX * size.width + (piece.velocityX * t * drag + sway) * unit
            val y = unit + (-piece.velocityY * t + 0.5f * GRAVITY * t * t) * unit
            if (y > unit + 40f) return@forEach

            // Turning ribbons look thinner side-on: squash the width with the spin.
            val angle = piece.spinDegreesPerSecond * t
            val flip = abs(cos(angle * PI.toFloat() / 180f)).coerceAtLeast(0.15f)
            val w = piece.widthDp * density * flip
            val h = piece.lengthDp * density
            rotate(angle, pivot = Offset(x, y)) {
                drawRect(
                    color = piece.color,
                    topLeft = Offset(x - w / 2, y - h / 2),
                    size = Size(w, h),
                    alpha = alpha,
                )
            }
        }
    }
}

private fun makePieces(random: Random): List<ConfettiPiece> {
    fun range(from: Float, to: Float) = from + random.nextFloat() * (to - from)

    return listOf(0f to 1f, 1f to -1f).flatMap { (cannonX, inwards) ->
        List(PIECES_PER_CANNON) {
            val angle = range(55f, 80f) * PI.toFloat() / 180f // From the floor, leaning inwards.
            val speed = range(MIN_SPEED, MAX_SPEED)
            ConfettiPiece(
                startX = cannonX,
                velocityX = cos(angle) * speed * inwards * 0.5f,
                velocityY = sin(angle) * speed,
                color = ConfettiColors[random.nextInt(ConfettiColors.size)],
                widthDp = range(5f, 8f),
                lengthDp = range(10f, 16f),
                spinDegreesPerSecond = range(-540f, 540f),
                swayPhase = range(0f, 2 * PI.toFloat()),
            )
        }
    }
}
