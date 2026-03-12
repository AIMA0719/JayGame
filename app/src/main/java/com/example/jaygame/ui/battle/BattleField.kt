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
import kotlin.math.cos
import kotlin.math.sin

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

// Star badge colors
private val StarColor = Color(0xFFFFD700)
private val StarBgGlow = Color(0xFF1A1A2E).copy(alpha = 0.9f)

// Pedestal particle colors (pre-allocated per grade)
private val PedestalParticleAlpha = 0.4f

// Vignette
private val VignetteColor = Color.Black.copy(alpha = 0.4f)

// Grid cell glow
private val CellHighlight = Color.White.copy(alpha = 0.03f)
private val CellHighlightBright = Color.White.copy(alpha = 0.06f)

private val GradeColors = GradeColorsByIndex
private val FamilyAuraColors = FamilyColorsByIndex

// C++ grid area in 1280x720 space — must match Grid.h (880x440 centered)
private const val CPP_GRID_W = 880f
private const val CPP_GRID_H = 440f
private const val GRID_NORM_X = (1280f - CPP_GRID_W) / 2f / 1280f   // 0.15625
private const val GRID_NORM_Y = (720f - CPP_GRID_H) / 2f / 720f     // 0.19444
private const val GRID_NORM_W = CPP_GRID_W / 1280f                   // 0.6875
private const val GRID_NORM_H = CPP_GRID_H / 720f                    // 0.61111

private const val GRID_COLS = 6
private const val GRID_ROWS = 5

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
            ContextCompat.getDrawable(context, def.iconRes)?.toBitmap(128, 128)?.asImageBitmap()
        }
    }

    val stageId by BattleBridge.stageId.collectAsState()
    val stage = remember(stageId) { STAGES.getOrNull(stageId) ?: STAGES[0] }
    val fieldBorderColor = remember(stageId) { stage.fieldColors.last().copy(alpha = 0.6f) }

    // Smooth unit positions
    val smoothXs = remember { mutableStateOf(FloatArray(0)) }
    val smoothYs = remember { mutableStateOf(FloatArray(0)) }

    // Animation time for idle bounce, attack pulse, pedestal particles
    val animTime = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                val dt = 1f / 60f // approximate
                animTime.floatValue += dt

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
        val t = animTime.floatValue

        val gridLeft = GRID_NORM_X * w
        val gridTop = GRID_NORM_Y * h
        val gridW = GRID_NORM_W * w
        val gridH = GRID_NORM_H * h

        // ── Draw unit sprites (Compose drawable icons) ──
        val data = unitPositions
        val sxArr = smoothXs.value
        val syArr = smoothYs.value
        val useSmooth = sxArr.size == data.count && data.count > 0

        // Size based on cell height (grid is 880x440, 6cols x 5rows → cells are 146x88)
        val cellH = gridH / GRID_ROWS.toFloat()
        val unitSize = cellH * 0.85f
        val pedestalRx = unitSize * 0.45f
        val pedestalRy = pedestalRx * 0.4f
        val gridState = BattleBridge.gridState.value

        for (i in 0 until data.count) {
            val screenX = if (useSmooth) sxArr[i] * w else data.xs[i] * w
            val screenY = if (useSmooth) syArr[i] * h else data.ys[i] * h
            val grade = data.grades[i]
            val gradeColor = GradeColors.getOrElse(grade) { Color.Gray }
            val tileIdx = data.tileIndices[i]
            val isSelected = selectedTile == tileIdx
            val isAttacking = data.isAttacking.getOrElse(i) { false }
            val family = com.example.jaygame.data.unitFamilyOf(data.unitDefIds[i])
            val unitDefId = data.unitDefIds[i]
            val level = data.levels[i]

            // Skip dragged unit at original position
            if (isDragging && dragFromTile == tileIdx) continue

            // ── Idle Bounce ──
            val bounceOffset = sin(t * 2.5f + unitDefId * 0.7f) * 3f

            // ── Attack Scale Pulse ──
            val attackScale = if (isAttacking) {
                1f + sin(t * 15f) * 0.15f
            } else {
                1f
            }

            // Ground shadow (bounces slightly with unit)
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

            // ── Pedestal Particles (grade 3+) ──
            if (grade >= 3) {
                val particleCount = (grade - 1) * 2
                for (p in 0 until particleCount.coerceAtMost(12)) {
                    val px = screenX + cos(t * 1.5f + p * 1.2f) * pedestalRx * 0.8f
                    val py = screenY + sin(t * 2f + p * 0.9f) * pedestalRy * 0.5f
                    val pAlpha = (0.3f + sin(t * 3f + p * 0.5f) * 0.15f).coerceIn(0.1f, 0.5f)
                    drawCircle(
                        color = gradeColor.copy(alpha = pAlpha),
                        radius = 2f + sin(t * 4f + p * 1.7f) * 0.8f,
                        center = Offset(px, py),
                    )
                }
            }

            // Selected ring
            if (isSelected) {
                drawOval(
                    color = NeonCyan.copy(alpha = 0.7f),
                    topLeft = Offset(screenX - pedestalRx * 1.1f, screenY - pedestalRy * 0.3f),
                    size = Size(pedestalRx * 2.2f, pedestalRy * 2.2f),
                    style = SelectedStroke,
                )
            }

            // Attack glow (pulsing with family color)
            if (isAttacking) {
                val glowAlpha = 0.3f + sin(t * 12f) * 0.15f
                drawCircle(
                    color = familyTint.copy(alpha = glowAlpha),
                    radius = unitSize * 0.55f,
                    center = Offset(screenX, screenY - unitSize * 0.3f + bounceOffset),
                )
                drawCircle(
                    color = AttackGlow,
                    radius = unitSize * 0.35f,
                    center = Offset(screenX, screenY - unitSize * 0.3f + bounceOffset),
                )
            }

            // Unit sprite (proper drawable icon) with bounce + attack scale
            val bitmap = unitBitmaps[unitDefId]
            if (bitmap != null) {
                val spriteSize = unitSize * 0.85f * attackScale
                val spriteY = screenY - spriteSize - pedestalRy * 0.3f + bounceOffset

                drawImage(
                    image = bitmap,
                    dstOffset = androidx.compose.ui.unit.IntOffset(
                        (screenX - spriteSize / 2).toInt(),
                        spriteY.toInt(),
                    ),
                    dstSize = androidx.compose.ui.unit.IntSize(
                        spriteSize.toInt(),
                        spriteSize.toInt(),
                    ),
                )
            }

            // ── Level badge: Stars instead of number ──
            if (level > 1) {
                val badgeY = screenY + pedestalRy * 1.2f
                val starCount = (level - 1).coerceAtMost(6)
                val badgeWidth = 8f + starCount * 8f
                val badgeHeight = 13f

                // Badge background
                drawRoundRect(
                    color = gradeColor.copy(alpha = 0.8f),
                    topLeft = Offset(screenX - badgeWidth / 2, badgeY),
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = CornerRadius(6f),
                )
                drawRoundRect(
                    color = StarBgGlow,
                    topLeft = Offset(screenX - badgeWidth / 2 + 1f, badgeY + 1f),
                    size = Size(badgeWidth - 2f, badgeHeight - 2f),
                    cornerRadius = CornerRadius(5f),
                )

                // Stars
                for (s in 0 until starCount) {
                    val sx = screenX - badgeWidth / 2 + 6f + s * 8f
                    val sy = badgeY + badgeHeight / 2
                    drawCircle(
                        color = StarColor,
                        radius = 2.5f,
                        center = Offset(sx, sy),
                    )
                }
            }

            // Merge indicator (pulsing)
            val tile = gridState.getOrNull(tileIdx)
            if (tile != null && tile.canMerge) {
                val mergeAlpha = 0.7f + sin(t * 6f) * 0.3f
                drawCircle(
                    color = Gold.copy(alpha = mergeAlpha),
                    radius = 5f + sin(t * 4f) * 1.5f,
                    center = Offset(screenX + unitSize * 0.35f, screenY - unitSize * 0.7f + bounceOffset),
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
