package com.example.jaygame.ui.battle

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.UNIT_DEFS_MAP
import com.example.jaygame.ui.theme.*

/**
 * Canvas-based battlefield. Green field with units standing on pedestals.
 * Uses EXACT same coordinates as C++ (normalized from 1280x720).
 *
 * C++ Grid: GRID_X=290, GRID_Y=190, GRID_W=700, GRID_H=340
 * Cell size: 140 x 113.3
 * Cell center(row,col) = (290 + col*140 + 70, 190 + row*113.3 + 56.7)
 */
@Composable
fun BattleField() {
    val gridState by BattleBridge.gridState.collectAsState()
    val selectedTile by BattleBridge.selectedTile.collectAsState()
    val context = LocalContext.current

    val unitBitmaps = remember {
        UNIT_DEFS_MAP.mapValues { (_, def) ->
            ContextCompat.getDrawable(context, def.iconRes)?.toBitmap(64, 64)?.asImageBitmap()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fieldPulse")
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    // C++ grid constants (normalized to 0-1)
    val gridNormX = 290f / 1280f
    val gridNormY = 190f / 720f
    val gridNormW = 700f / 1280f
    val gridNormH = 340f / 720f
    val cellNormW = (700f / 5f) / 1280f  // 140 / 1280
    val cellNormH = (340f / 3f) / 720f   // 113.3 / 720

    // Drag state for unit repositioning
    var dragFromTile by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { startOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val normX = startOffset.x / w
                        val normY = startOffset.y / h

                        // Find cell at drag start
                        for (row in 0 until 3) {
                            for (col in 0 until 5) {
                                val cx = gridNormX + (col + 0.5f) * cellNormW
                                val cy = gridNormY + (row + 0.5f) * cellNormH
                                val dx = normX - cx
                                val dy = normY - cy
                                if (kotlin.math.sqrt(dx * dx + dy * dy) < cellNormW * 0.5f) {
                                    val idx = row * 5 + col
                                    val tile = BattleBridge.gridState.value.getOrNull(idx)
                                    if (tile != null && tile.unitDefId >= 0) {
                                        dragFromTile = idx
                                        isDragging = true
                                        dragOffset = startOffset
                                    }
                                    return@detectDragGesturesAfterLongPress
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    },
                    onDragEnd = {
                        if (isDragging && dragFromTile >= 0) {
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val normX = dragOffset.x / w
                            val normY = dragOffset.y / h

                            // Find destination cell
                            var destTile = -1
                            for (row in 0 until 3) {
                                for (col in 0 until 5) {
                                    val cx = gridNormX + (col + 0.5f) * cellNormW
                                    val cy = gridNormY + (row + 0.5f) * cellNormH
                                    val dx = normX - cx
                                    val dy = normY - cy
                                    if (kotlin.math.sqrt(dx * dx + dy * dy) < cellNormW * 0.5f) {
                                        destTile = row * 5 + col
                                    }
                                }
                            }

                            if (destTile >= 0 && destTile != dragFromTile) {
                                BattleBridge.requestSwap(dragFromTile, destTile)
                            }
                        }
                        isDragging = false
                        dragFromTile = -1
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        isDragging = false
                        dragFromTile = -1
                        dragOffset = Offset.Zero
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    // Convert tap to normalized coordinates
                    val tapNormX = tapOffset.x / w
                    val tapNormY = tapOffset.y / h

                    // Find closest grid cell
                    val tapRadius = cellNormW * 0.5f
                    var closestIdx = -1
                    var closestDist = Float.MAX_VALUE
                    for (row in 0 until 3) {
                        for (col in 0 until 5) {
                            val cx = gridNormX + (col + 0.5f) * cellNormW
                            val cy = gridNormY + (row + 0.5f) * cellNormH
                            val dx = tapNormX - cx
                            val dy = tapNormY - cy
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            if (dist < tapRadius && dist < closestDist) {
                                closestDist = dist
                                closestIdx = row * 5 + col
                            }
                        }
                    }
                    if (closestIdx >= 0) {
                        BattleBridge.requestClickTile(closestIdx)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // Grid screen coordinates
        val gridLeft = gridNormX * w
        val gridTop = gridNormY * h
        val gridW = gridNormW * w
        val gridH = gridNormH * h

        // Field shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.4f),
            topLeft = Offset(gridLeft + 4f, gridTop + 4f),
            size = Size(gridW, gridH),
            cornerRadius = CornerRadius(16f),
        )

        // Green field background
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF4CAF50),
                    Color(0xFF388E3C),
                    Color(0xFF2E7D32),
                ),
                startY = gridTop,
                endY = gridTop + gridH,
            ),
            topLeft = Offset(gridLeft, gridTop),
            size = Size(gridW, gridH),
            cornerRadius = CornerRadius(16f),
        )

        // Field border
        drawRoundRect(
            color = Color(0xFF1B5E20),
            topLeft = Offset(gridLeft, gridTop),
            size = Size(gridW, gridH),
            cornerRadius = CornerRadius(16f),
            style = Stroke(width = 3f),
        )

        // Subtle grid lines
        val gridLineColor = Color.White.copy(alpha = 0.06f)
        val cellW = gridW / 5f
        val cellH = gridH / 3f
        for (col in 1 until 5) {
            val x = gridLeft + cellW * col
            drawLine(gridLineColor, Offset(x, gridTop + 8f), Offset(x, gridTop + gridH - 8f), 1f)
        }
        for (row in 1 until 3) {
            val y = gridTop + cellH * row
            drawLine(gridLineColor, Offset(gridLeft + 8f, y), Offset(gridLeft + gridW - 8f, y), 1f)
        }

        // Draw units
        val unitSize = cellW * 0.5f
        val pedestalRadius = unitSize * 0.45f

        for (row in 0 until 3) {
            for (col in 0 until 5) {
                val index = row * 5 + col
                val tile = gridState.getOrNull(index)
                val isEmpty = tile == null || tile.unitDefId < 0
                val isSelected = selectedTile == index

                // Cell center in screen coordinates (matching C++ exactly)
                val cx = gridLeft + (col + 0.5f) * cellW
                val cy = gridTop + (row + 0.5f) * cellH

                if (!isEmpty && tile != null) {
                    val gradeColor = when (tile.grade) {
                        0 -> Color(0xFF9E9E9E)
                        1 -> Color(0xFF42A5F5)
                        2 -> Color(0xFFAB47BC)
                        3 -> Color(0xFFFF8F00)
                        4 -> Color(0xFFE94560)
                        else -> Color.Gray
                    }

                    // Pedestal shadow
                    drawOval(
                        color = Color.Black.copy(alpha = 0.3f),
                        topLeft = Offset(cx - pedestalRadius, cy + unitSize * 0.15f),
                        size = Size(pedestalRadius * 2, pedestalRadius * 0.5f),
                    )

                    // Pedestal glow
                    val pedestalColor = if (isSelected) NeonCyan else gradeColor
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                pedestalColor.copy(alpha = 0.6f),
                                pedestalColor.copy(alpha = 0.2f),
                                Color.Transparent,
                            ),
                        ),
                        topLeft = Offset(cx - pedestalRadius, cy + unitSize * 0.05f),
                        size = Size(pedestalRadius * 2, pedestalRadius * 0.6f),
                    )

                    // Selected ring
                    if (isSelected) {
                        drawOval(
                            color = NeonCyan.copy(alpha = 0.5f + breathe * 0.3f),
                            topLeft = Offset(cx - pedestalRadius * 1.1f, cy + unitSize * 0.02f),
                            size = Size(pedestalRadius * 2.2f, pedestalRadius * 0.7f),
                            style = Stroke(width = 2.5f),
                        )
                    }

                    // Unit sprite
                    val bitmap = unitBitmaps[tile.unitDefId]
                    if (bitmap != null) {
                        val spriteY = cy - unitSize * 0.6f - breathe * 3f
                        drawImage(
                            image = bitmap,
                            topLeft = Offset(cx - unitSize / 2, spriteY),
                        )
                    }

                    // Level badge
                    if (tile.level > 1) {
                        val badgeY = cy + unitSize * 0.3f
                        drawRoundRect(
                            color = Color(0xFF1A1A2E).copy(alpha = 0.8f),
                            topLeft = Offset(cx - 18f, badgeY),
                            size = Size(36f, 14f),
                            cornerRadius = CornerRadius(7f),
                        )
                        drawRoundRect(
                            color = Gold.copy(alpha = 0.5f),
                            topLeft = Offset(cx - 18f, badgeY),
                            size = Size(36f, 14f),
                            cornerRadius = CornerRadius(7f),
                            style = Stroke(width = 1f),
                        )
                    }

                    // Grade dot
                    drawCircle(
                        color = gradeColor,
                        radius = 5f,
                        center = Offset(cx - unitSize / 2 + 5f, cy - unitSize * 0.55f),
                    )

                    // Merge indicator
                    if (tile.canMerge) {
                        drawCircle(
                            color = Gold,
                            radius = 8f,
                            center = Offset(cx + unitSize / 2 - 5f, cy - unitSize * 0.55f),
                        )
                    }
                } else {
                    // Empty slot
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = pedestalRadius * 0.6f,
                        center = Offset(cx, cy),
                    )
                }
            }
        }

        // Draw drag indicator
        if (isDragging && dragFromTile >= 0) {
            val tile = gridState.getOrNull(dragFromTile)
            if (tile != null && tile.unitDefId >= 0) {
                val bitmap = unitBitmaps[tile.unitDefId]
                if (bitmap != null) {
                    // Draw ghost unit at drag position
                    drawImage(
                        image = bitmap,
                        topLeft = Offset(dragOffset.x - unitSize / 2, dragOffset.y - unitSize / 2),
                        alpha = 0.7f,
                    )
                }
                // Draw highlight on source cell
                val fromRow = dragFromTile / 5
                val fromCol = dragFromTile % 5
                val fromCx = gridLeft + (fromCol + 0.5f) * cellW
                val fromCy = gridTop + (fromRow + 0.5f) * cellH
                drawCircle(
                    color = Color(0xFF00D4FF).copy(alpha = 0.3f),
                    radius = pedestalRadius,
                    center = Offset(fromCx, fromCy),
                )
            }
        }
    }
}
