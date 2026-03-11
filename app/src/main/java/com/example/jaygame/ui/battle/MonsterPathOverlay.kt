package com.example.jaygame.ui.battle

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.STAGES
import kotlin.math.cos
import kotlin.math.sin

// Pre-allocated constants to avoid per-frame allocation
private val DashLineColor = Color.White.copy(alpha = 0.1f)
private val OuterStroke = Stroke(width = 2f)
private val InnerStroke = Stroke(width = 1.5f)

// Arrow colors
private val ArrowColor = Color.White.copy(alpha = 0.15f)
private val ArrowColorBright = Color.White.copy(alpha = 0.25f)

/**
 * Draws the visible rectangular path where monsters walk.
 * Uses EXACT same coordinates as C++ (normalized from 1280x720).
 */
@Composable
fun MonsterPathOverlay() {
    val stageId by BattleBridge.stageId.collectAsState()
    val stage = remember(stageId) { STAGES.getOrNull(stageId) ?: STAGES[0] }

    // Pre-compute stage-dependent colors outside Canvas (recomputed only when stageId changes)
    val pathColor05 = remember(stageId) { stage.pathColor.copy(alpha = 0.5f) }
    val pathColor06 = remember(stageId) { stage.pathColor.copy(alpha = 0.6f) }
    val pathColor04 = remember(stageId) { stage.pathColor.copy(alpha = 0.4f) }
    val pathColor03 = remember(stageId) { stage.pathColor.copy(alpha = 0.3f) }
    val bgColorLast = remember(stageId) { stage.bgColors.last() }
    val pathGradientColors = remember(stageId) { listOf(pathColor05, pathColor06) }

    // Animated dash offset for directional flow
    val infiniteTransition = rememberInfiniteTransition(label = "pathFlow")
    val dashOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dashOffset",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val pathLeft = (130f / 1280f) * w
        val pathTop = (70f / 720f) * h
        val pathRight = (1150f / 1280f) * w
        val pathBottom = (650f / 720f) * h

        val gridLeft = (200f / 1280f) * w
        val gridTop = (140f / 720f) * h
        val gridRight = (1080f / 1280f) * w
        val gridBottom = (580f / 720f) * h

        val pathW = pathRight - pathLeft
        val pathH = pathBottom - pathTop
        val gridW = gridRight - gridLeft
        val gridH = gridBottom - gridTop

        // Path fill (gradient)
        drawRoundRect(
            brush = Brush.verticalGradient(pathGradientColors),
            topLeft = Offset(pathLeft, pathTop),
            size = Size(pathW, pathH),
            cornerRadius = CornerRadius(16f),
        )
        // Inner cutout (grid area background)
        drawRoundRect(
            color = bgColorLast,
            topLeft = Offset(gridLeft, gridTop),
            size = Size(gridW, gridH),
            cornerRadius = CornerRadius(12f),
        )
        // Outer border
        drawRoundRect(
            color = pathColor04,
            topLeft = Offset(pathLeft, pathTop),
            size = Size(pathW, pathH),
            cornerRadius = CornerRadius(16f),
            style = OuterStroke,
        )
        // Inner border
        drawRoundRect(
            color = pathColor03,
            topLeft = Offset(gridLeft, gridTop),
            size = Size(gridW, gridH),
            cornerRadius = CornerRadius(12f),
            style = InnerStroke,
        )

        // ── Animated directional dashed lines (flowing in monster walk direction) ──
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), dashOffset)
        val reverseDashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), 40f - dashOffset)

        // Top: left to right
        val midTopY = (pathTop + gridTop) / 2
        drawLine(DashLineColor, Offset(pathLeft + 20f, midTopY), Offset(pathRight - 20f, midTopY),
            strokeWidth = 1.5f, pathEffect = dashEffect)

        // Right: top to bottom
        val midRightX = (gridRight + pathRight) / 2
        drawLine(DashLineColor, Offset(midRightX, pathTop + 20f), Offset(midRightX, pathBottom - 20f),
            strokeWidth = 1.5f, pathEffect = dashEffect)

        // Bottom: right to left
        val midBottomY = (gridBottom + pathBottom) / 2
        drawLine(DashLineColor, Offset(pathRight - 20f, midBottomY), Offset(pathLeft + 20f, midBottomY),
            strokeWidth = 1.5f, pathEffect = reverseDashEffect)

        // Left: bottom to top
        val midLeftX = (pathLeft + gridLeft) / 2
        drawLine(DashLineColor, Offset(midLeftX, pathBottom - 20f), Offset(midLeftX, pathTop + 20f),
            strokeWidth = 1.5f, pathEffect = reverseDashEffect)

        // ── Direction arrows along the path ──
        val arrowSize = 6f

        // Top path arrows (left → right)
        val arrowCount = 5
        for (i in 0 until arrowCount) {
            val frac = (i + 0.5f) / arrowCount
            val phase = (frac + dashOffset / 40f) % 1f
            val ax = pathLeft + 30f + (pathRight - pathLeft - 60f) * frac
            val ay = midTopY
            val alpha = (0.15f + sin(phase * 3.14f) * 0.1f).coerceIn(0.05f, 0.3f)
            // Right-pointing chevron
            drawLine(ArrowColor.copy(alpha = alpha), Offset(ax - arrowSize, ay - arrowSize), Offset(ax, ay),
                strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(ArrowColor.copy(alpha = alpha), Offset(ax, ay), Offset(ax - arrowSize, ay + arrowSize),
                strokeWidth = 1.5f, cap = StrokeCap.Round)
        }

        // Right path arrows (top → bottom)
        for (i in 0 until arrowCount) {
            val frac = (i + 0.5f) / arrowCount
            val phase = (frac + dashOffset / 40f) % 1f
            val ax = midRightX
            val ay = pathTop + 30f + (pathBottom - pathTop - 60f) * frac
            val alpha = (0.15f + sin(phase * 3.14f) * 0.1f).coerceIn(0.05f, 0.3f)
            // Down-pointing chevron
            drawLine(ArrowColor.copy(alpha = alpha), Offset(ax - arrowSize, ay - arrowSize), Offset(ax, ay),
                strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(ArrowColor.copy(alpha = alpha), Offset(ax, ay), Offset(ax + arrowSize, ay - arrowSize),
                strokeWidth = 1.5f, cap = StrokeCap.Round)
        }

        // Bottom path arrows (right → left)
        for (i in 0 until arrowCount) {
            val frac = (i + 0.5f) / arrowCount
            val phase = (frac + dashOffset / 40f) % 1f
            val ax = pathRight - 30f - (pathRight - pathLeft - 60f) * frac
            val ay = midBottomY
            val alpha = (0.15f + sin(phase * 3.14f) * 0.1f).coerceIn(0.05f, 0.3f)
            // Left-pointing chevron
            drawLine(ArrowColor.copy(alpha = alpha), Offset(ax + arrowSize, ay - arrowSize), Offset(ax, ay),
                strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(ArrowColor.copy(alpha = alpha), Offset(ax, ay), Offset(ax + arrowSize, ay + arrowSize),
                strokeWidth = 1.5f, cap = StrokeCap.Round)
        }

        // Left path arrows (bottom → top)
        for (i in 0 until arrowCount) {
            val frac = (i + 0.5f) / arrowCount
            val phase = (frac + dashOffset / 40f) % 1f
            val ax = midLeftX
            val ay = pathBottom - 30f - (pathBottom - pathTop - 60f) * frac
            val alpha = (0.15f + sin(phase * 3.14f) * 0.1f).coerceIn(0.05f, 0.3f)
            // Up-pointing chevron
            drawLine(ArrowColor.copy(alpha = alpha), Offset(ax - arrowSize, ay + arrowSize), Offset(ax, ay),
                strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(ArrowColor.copy(alpha = alpha), Offset(ax, ay), Offset(ax + arrowSize, ay + arrowSize),
                strokeWidth = 1.5f, cap = StrokeCap.Round)
        }
    }
}
