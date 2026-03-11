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
import kotlin.math.sin

// Pre-allocated constants to avoid per-frame allocation
private val DashLineColor = Color.White.copy(alpha = 0.1f)
private val OuterStroke = Stroke(width = 2f)
private val InnerStroke = Stroke(width = 1.5f)

// Arrow colors
private val ArrowColor = Color.White.copy(alpha = 0.15f)

// Coordinates in 1280x720 space — matching C++ Grid.h + BattleScene::setupPath()
// Grid: (200, 140) ~ (1080, 580) = 880x440
// Path: margin=70 outside grid → matches C++ pathWaypoints_
private const val GRID_LEFT = 200f
private const val GRID_TOP = 140f
private const val GRID_RIGHT = 1080f
private const val GRID_BOTTOM = 580f
private const val PATH_MARGIN = 70f
private const val PATH_LEFT = GRID_LEFT - PATH_MARGIN     // 130
private const val PATH_TOP = GRID_TOP - PATH_MARGIN       // 70
private const val PATH_RIGHT = GRID_RIGHT + PATH_MARGIN   // 1150
private const val PATH_BOTTOM = GRID_BOTTOM + PATH_MARGIN // 650

/**
 * Draws the visible path where monsters walk around the rectangular field (880x440).
 * Uses EXACT same coordinates as C++ Grid.h (normalized from 1280x720).
 */
@Composable
fun MonsterPathOverlay() {
    val stageId by BattleBridge.stageId.collectAsState()
    val stage = remember(stageId) { STAGES.getOrNull(stageId) ?: STAGES[0] }

    // Pre-compute stage-dependent colors outside Canvas
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

        val pathLeft = (PATH_LEFT / 1280f) * w
        val pathTop = (PATH_TOP / 720f) * h
        val pathRight = (PATH_RIGHT / 1280f) * w
        val pathBottom = (PATH_BOTTOM / 720f) * h

        val gridLeft = (GRID_LEFT / 1280f) * w
        val gridTop = (GRID_TOP / 720f) * h
        val gridRight = (GRID_RIGHT / 1280f) * w
        val gridBottom = (GRID_BOTTOM / 720f) * h

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
        val arrowCount = 5

        // Top path arrows (left → right)
        for (i in 0 until arrowCount) {
            val frac = (i + 0.5f) / arrowCount
            val phase = (frac + dashOffset / 40f) % 1f
            val ax = pathLeft + 30f + (pathRight - pathLeft - 60f) * frac
            val ay = midTopY
            val alpha = (0.15f + sin(phase * 3.14f) * 0.1f).coerceIn(0.05f, 0.3f)
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
            drawLine(ArrowColor.copy(alpha = alpha), Offset(ax - arrowSize, ay + arrowSize), Offset(ax, ay),
                strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(ArrowColor.copy(alpha = alpha), Offset(ax, ay), Offset(ax + arrowSize, ay + arrowSize),
                strokeWidth = 1.5f, cap = StrokeCap.Round)
        }
    }
}
