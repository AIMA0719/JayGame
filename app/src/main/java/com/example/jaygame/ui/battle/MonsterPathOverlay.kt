package com.example.jaygame.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draws the visible rectangular path where monsters walk.
 * Uses EXACT same coordinates as C++ (normalized from 1280x720).
 *
 * C++ path: margin=80 around grid (290,190,700,340)
 * Path corners: (210,110) → (1070,110) → (1070,610) → (210,610)
 * Grid inner: (290,190) → (990,530)
 */
@Composable
fun MonsterPathOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // C++ normalized coordinates (dividing by 1280x720)
        // Path outer bounds (where enemies walk) — margin=80
        val pathLeft = (210f / 1280f) * w
        val pathTop = (110f / 720f) * h
        val pathRight = (1070f / 1280f) * w
        val pathBottom = (610f / 720f) * h

        // Grid inner bounds (where units live)
        val gridLeft = (290f / 1280f) * w
        val gridTop = (190f / 720f) * h
        val gridRight = (990f / 1280f) * w
        val gridBottom = (530f / 720f) * h

        val pathW = pathRight - pathLeft
        val pathH = pathBottom - pathTop
        val gridW = gridRight - gridLeft
        val gridH = gridBottom - gridTop

        // Outer path background (dirt/stone road)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF5D4037).copy(alpha = 0.5f),
                    Color(0xFF4E342E).copy(alpha = 0.6f),
                )
            ),
            topLeft = Offset(pathLeft, pathTop),
            size = Size(pathW, pathH),
            cornerRadius = CornerRadius(16f),
        )

        // Inner cutout (match screen background - field draws on top)
        drawRoundRect(
            color = Color(0xFF0A0A1A),
            topLeft = Offset(gridLeft, gridTop),
            size = Size(gridW, gridH),
            cornerRadius = CornerRadius(12f),
        )

        // Outer border
        drawRoundRect(
            color = Color(0xFF8D6E63).copy(alpha = 0.4f),
            topLeft = Offset(pathLeft, pathTop),
            size = Size(pathW, pathH),
            cornerRadius = CornerRadius(16f),
            style = Stroke(width = 2f),
        )
        // Inner border
        drawRoundRect(
            color = Color(0xFF8D6E63).copy(alpha = 0.3f),
            topLeft = Offset(gridLeft, gridTop),
            size = Size(gridW, gridH),
            cornerRadius = CornerRadius(12f),
            style = Stroke(width = 1.5f),
        )

        // Directional dashed lines in the middle of each path segment
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 12f))
        val lineColor = Color.White.copy(alpha = 0.1f)

        // Top segment (going right)
        val midTopY = (pathTop + gridTop) / 2
        drawLine(lineColor, Offset(pathLeft + 20f, midTopY), Offset(pathRight - 20f, midTopY),
            strokeWidth = 1.5f, pathEffect = dash)

        // Right segment (going down)
        val midRightX = (gridRight + pathRight) / 2
        drawLine(lineColor, Offset(midRightX, pathTop + 20f), Offset(midRightX, pathBottom - 20f),
            strokeWidth = 1.5f, pathEffect = dash)

        // Bottom segment (going left)
        val midBottomY = (gridBottom + pathBottom) / 2
        drawLine(lineColor, Offset(pathRight - 20f, midBottomY), Offset(pathLeft + 20f, midBottomY),
            strokeWidth = 1.5f, pathEffect = dash)

        // Left segment (going up)
        val midLeftX = (pathLeft + gridLeft) / 2
        drawLine(lineColor, Offset(midLeftX, pathBottom - 20f), Offset(midLeftX, pathTop + 20f),
            strokeWidth = 1.5f, pathEffect = dash)
    }
}
