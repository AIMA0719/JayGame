package com.example.jaygame.ui.battle

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
import com.example.jaygame.data.STAGES
import com.example.jaygame.data.UNIT_DEFS_MAP
import com.example.jaygame.ui.theme.*

// Pre-allocated constants
private val GroundShadow = Color.Black.copy(alpha = 0.35f)
private val GroundEdgeDark = Color(0xFF2A1A0A).copy(alpha = 0.7f)
private val GroundEdgeLight = Color(0xFF8B6914).copy(alpha = 0.3f)
private val PedestalShadow = Color.Black.copy(alpha = 0.35f)
private val BadgeBg = Color(0xFF1A1A2E).copy(alpha = 0.85f)
private val BadgeBorder = Gold.copy(alpha = 0.5f)
private val GroundBorderStroke = Stroke(width = 2.5f)
private val SelectedStroke = Stroke(width = 2f)
private val BadgeBorderStroke = Stroke(width = 1f)
private val AttackGlow = Color.White.copy(alpha = 0.4f)
private val DragGhost = Color(0xFF00D4FF).copy(alpha = 0.3f)

private val GradeColors = GradeColorsByIndex
private val FamilyAuraColors = FamilyColorsByIndex

// C++ grid area in 1280x720 space
private const val GRID_NORM_X = 290f / 1280f
private const val GRID_NORM_Y = 190f / 720f
private const val GRID_NORM_W = 700f / 1280f
private const val GRID_NORM_H = 340f / 720f

/**
 * Battlefield overlay — unit sprites rendered in Compose (proper drawable icons),
 * aura/particle effects rendered by C++ engine behind this layer.
 */
@Composable
fun BattleField() {
    val unitPositions by BattleBridge.unitPositions.collectAsState()
    val selectedTile by BattleBridge.selectedTile.collectAsState()
    val context = LocalContext.current

    val unitBitmaps = remember {
        UNIT_DEFS_MAP.mapValues { (_, def) ->
            ContextCompat.getDrawable(context, def.iconRes)?.toBitmap(64, 64)?.asImageBitmap()
        }
    }

    val stageId by BattleBridge.stageId.collectAsState()
    val stage = remember(stageId) { STAGES.getOrNull(stageId) ?: STAGES[0] }
    val fieldBorderColor = remember(stageId) { stage.fieldColors.last().copy(alpha = 0.6f) }

    // Smooth unit positions
    val smoothXs = remember { mutableStateOf(FloatArray(0)) }
    val smoothYs = remember { mutableStateOf(FloatArray(0)) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { _ ->
                val data = BattleBridge.unitPositions.value
                val sx = smoothXs.value
                val sy = smoothYs.value

                if (data.count != sx.size) {
                    smoothXs.value = data.xs.copyOf(data.count)
                    smoothYs.value = data.ys.copyOf(data.count)
                } else if (data.count > 0) {
                    val lerpFactor = 0.25f
                    val newX = FloatArray(data.count)
                    val newY = FloatArray(data.count)
                    for (i in 0 until data.count) {
                        newX[i] = sx[i] + (data.xs[i] - sx[i]) * lerpFactor
                        newY[i] = sy[i] + (data.ys[i] - sy[i]) * lerpFactor
                    }
                    smoothXs.value = newX
                    smoothYs.value = newY
                }
            }
        }
    }

    // Drag state for unit relocation
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
                        val tapNormX = startOffset.x / w
                        val tapNormY = startOffset.y / h

                        val data = BattleBridge.unitPositions.value
                        var closestIdx = -1
                        var closestDistSq = Float.MAX_VALUE
                        val threshold = 0.05f

                        for (i in 0 until data.count) {
                            val dx = tapNormX - data.xs[i]
                            val dy = tapNormY - data.ys[i]
                            val distSq = dx * dx + dy * dy
                            if (distSq < threshold * threshold && distSq < closestDistSq) {
                                closestDistSq = distSq
                                closestIdx = i
                            }
                        }

                        if (closestIdx >= 0) {
                            dragFromTile = data.tileIndices[closestIdx]
                            isDragging = true
                            dragOffset = startOffset
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
                            BattleBridge.requestRelocate(dragFromTile, normX, normY)
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
                    val tapNormX = tapOffset.x / w
                    val tapNormY = tapOffset.y / h

                    val data = BattleBridge.unitPositions.value
                    var closestIdx = -1
                    var closestDistSq = Float.MAX_VALUE
                    val threshold = 0.04f

                    for (i in 0 until data.count) {
                        val dx = tapNormX - data.xs[i]
                        val dy = tapNormY - data.ys[i]
                        val distSq = dx * dx + dy * dy
                        if (distSq < threshold * threshold && distSq < closestDistSq) {
                            closestDistSq = distSq
                            closestIdx = i
                        }
                    }

                    if (closestIdx >= 0) {
                        BattleBridge.requestClickTile(data.tileIndices[closestIdx])
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        val gridLeft = GRID_NORM_X * w
        val gridTop = GRID_NORM_Y * h
        val gridW = GRID_NORM_W * w
        val gridH = GRID_NORM_H * h

        // ── Raised Ground Platform ──
        val groundThickness = gridH * 0.08f

        // Side face
        drawRoundRect(
            color = GroundEdgeDark,
            topLeft = Offset(gridLeft, gridTop + gridH),
            size = Size(gridW, groundThickness),
            cornerRadius = CornerRadius(8f),
        )
        drawRoundRect(
            color = GroundEdgeLight,
            topLeft = Offset(gridLeft, gridTop + gridH),
            size = Size(gridW, groundThickness * 0.3f),
            cornerRadius = CornerRadius(4f),
        )

        // Drop shadow
        drawRoundRect(
            color = GroundShadow,
            topLeft = Offset(gridLeft + 6f, gridTop + gridH + groundThickness * 0.5f),
            size = Size(gridW, groundThickness * 0.5f),
            cornerRadius = CornerRadius(12f),
        )

        // Main ground surface
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = stage.fieldColors,
                startY = gridTop,
                endY = gridTop + gridH,
            ),
            topLeft = Offset(gridLeft, gridTop),
            size = Size(gridW, gridH),
            cornerRadius = CornerRadius(12f),
        )

        // Top highlight
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                startY = gridTop,
                endY = gridTop + gridH * 0.3f,
            ),
            topLeft = Offset(gridLeft, gridTop),
            size = Size(gridW, gridH * 0.3f),
            cornerRadius = CornerRadius(12f),
        )

        // Ground border
        drawRoundRect(
            color = fieldBorderColor,
            topLeft = Offset(gridLeft, gridTop),
            size = Size(gridW, gridH),
            cornerRadius = CornerRadius(12f),
            style = GroundBorderStroke,
        )

        // ── Draw unit sprites (Compose drawable icons) ──
        // Aura particles are rendered by C++ behind this layer
        val data = unitPositions
        val sxArr = smoothXs.value
        val syArr = smoothYs.value
        val useSmooth = sxArr.size == data.count && data.count > 0

        val unitSize = gridW / 6f * 0.55f
        val pedestalRx = unitSize * 0.4f
        val pedestalRy = pedestalRx * 0.35f
        val gridState = BattleBridge.gridState.value

        for (i in 0 until data.count) {
            val screenX = if (useSmooth) sxArr[i] * w else data.xs[i] * w
            val screenY = if (useSmooth) syArr[i] * h else data.ys[i] * h
            val gradeColor = GradeColors.getOrElse(data.grades[i]) { Color.Gray }
            val tileIdx = data.tileIndices[i]
            val isSelected = selectedTile == tileIdx
            val isAttacking = data.isAttacking.getOrElse(i) { false }
            val family = data.unitDefIds[i] % 5

            // Skip dragged unit at original position
            if (isDragging && dragFromTile == tileIdx) continue

            // Ground shadow
            drawOval(
                color = PedestalShadow,
                topLeft = Offset(screenX - pedestalRx, screenY + unitSize * 0.05f),
                size = Size(pedestalRx * 2, pedestalRy * 2),
            )

            // Pedestal glow (grade + family blend)
            val familyTint = FamilyAuraColors.getOrElse(family) { FamilyAuraColors[0] }
            val pedestalColor = if (isSelected) NeonCyan else Color(
                red = gradeColor.red * 0.65f + familyTint.red * 0.35f,
                green = gradeColor.green * 0.65f + familyTint.green * 0.35f,
                blue = gradeColor.blue * 0.65f + familyTint.blue * 0.35f,
                alpha = 1f,
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        pedestalColor.copy(alpha = 0.5f),
                        pedestalColor.copy(alpha = 0.15f),
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(screenX - pedestalRx * 1.2f, screenY - pedestalRy * 0.5f),
                size = Size(pedestalRx * 2.4f, pedestalRy * 2.5f),
            )

            // Selected ring
            if (isSelected) {
                drawOval(
                    color = NeonCyan.copy(alpha = 0.7f),
                    topLeft = Offset(screenX - pedestalRx * 1.1f, screenY - pedestalRy * 0.3f),
                    size = Size(pedestalRx * 2.2f, pedestalRy * 2.2f),
                    style = SelectedStroke,
                )
            }

            // Attack glow
            if (isAttacking) {
                drawCircle(
                    color = AttackGlow,
                    radius = unitSize * 0.5f,
                    center = Offset(screenX, screenY - unitSize * 0.3f),
                )
            }

            // Unit sprite (proper drawable icon)
            val bitmap = unitBitmaps[data.unitDefIds[i]]
            if (bitmap != null) {
                val spriteSize = unitSize * 0.85f
                val spriteY = screenY - spriteSize - pedestalRy * 0.3f
                drawImage(
                    image = bitmap,
                    topLeft = Offset(screenX - spriteSize / 2, spriteY),
                )
            }

            // Level badge
            if (data.levels[i] > 1) {
                val badgeY = screenY + pedestalRy * 1.2f
                drawRoundRect(
                    color = BadgeBg,
                    topLeft = Offset(screenX - 14f, badgeY),
                    size = Size(28f, 12f),
                    cornerRadius = CornerRadius(6f),
                )
                drawRoundRect(
                    color = BadgeBorder,
                    topLeft = Offset(screenX - 14f, badgeY),
                    size = Size(28f, 12f),
                    cornerRadius = CornerRadius(6f),
                    style = BadgeBorderStroke,
                )
            }

            // Merge indicator
            val tile = gridState.getOrNull(tileIdx)
            if (tile != null && tile.canMerge) {
                drawCircle(
                    color = Gold,
                    radius = 5f,
                    center = Offset(screenX + unitSize * 0.35f, screenY - unitSize * 0.7f),
                )
            }
        }

        // Draw drag ghost at finger position
        if (isDragging && dragFromTile >= 0) {
            val data2 = BattleBridge.unitPositions.value
            val dragUnitIdx = (0 until data2.count).firstOrNull {
                data2.tileIndices[it] == dragFromTile
            }
            if (dragUnitIdx != null) {
                val bitmap = unitBitmaps[data2.unitDefIds[dragUnitIdx]]
                if (bitmap != null) {
                    drawImage(
                        image = bitmap,
                        topLeft = Offset(dragOffset.x - unitSize / 2, dragOffset.y - unitSize / 2),
                        alpha = 0.7f,
                    )
                }
                drawCircle(
                    color = DragGhost,
                    radius = unitSize * 0.6f,
                    center = Offset(dragOffset.x, dragOffset.y),
                )
            }
        }
    }
}
