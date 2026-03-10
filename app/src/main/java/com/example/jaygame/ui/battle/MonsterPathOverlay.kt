package com.example.jaygame.ui.battle

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
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.STAGES

// Pre-allocated constants to avoid per-frame allocation
private val DashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f))
private val DashLineColor = Color.White.copy(alpha = 0.1f)
private val OuterStroke = Stroke(width = 2f)
private val InnerStroke = Stroke(width = 1.5f)

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

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val pathLeft = (210f / 1280f) * w
        val pathTop = (110f / 720f) * h
        val pathRight = (1070f / 1280f) * w
        val pathBottom = (610f / 720f) * h

        val gridLeft = (290f / 1280f) * w
        val gridTop = (190f / 720f) * h
        val gridRight = (990f / 1280f) * w
        val gridBottom = (530f / 720f) * h

        val pathW = pathRight - pathLeft
        val pathH = pathBottom - pathTop
        val gridW = gridRight - gridLeft
        val gridH = gridBottom - gridTop

        // Outer path background (stage-themed)
        drawRoundRect(
            brush = Brush.verticalGradient(pathGradientColors),
            topLeft = Offset(pathLeft, pathTop),
            size = Size(pathW, pathH),
            cornerRadius = CornerRadius(16f),
        )

        // Inner cutout (match stage background)
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

        // Directional dashed lines
        val midTopY = (pathTop + gridTop) / 2
        drawLine(DashLineColor, Offset(pathLeft + 20f, midTopY), Offset(pathRight - 20f, midTopY),
            strokeWidth = 1.5f, pathEffect = DashEffect)

        val midRightX = (gridRight + pathRight) / 2
        drawLine(DashLineColor, Offset(midRightX, pathTop + 20f), Offset(midRightX, pathBottom - 20f),
            strokeWidth = 1.5f, pathEffect = DashEffect)

        val midBottomY = (gridBottom + pathBottom) / 2
        drawLine(DashLineColor, Offset(pathRight - 20f, midBottomY), Offset(pathLeft + 20f, midBottomY),
            strokeWidth = 1.5f, pathEffect = DashEffect)

        val midLeftX = (pathLeft + gridLeft) / 2
        drawLine(DashLineColor, Offset(midLeftX, pathBottom - 20f), Offset(midLeftX, pathTop + 20f),
            strokeWidth = 1.5f, pathEffect = DashEffect)
    }
}
