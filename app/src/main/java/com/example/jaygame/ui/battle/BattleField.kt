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
import kotlin.math.PI
import kotlin.math.abs
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

// E7: Grade 4+ aura ring colors (pre-allocated)
private val AncientAuraRed = Color(0xFFFF3333)       // grade 4 (Ancient)
private val MythicAuraGold = Color(0xFFFFD700)        // grade 5 (Mythic)
private val ImmortalAuraPink = Color(0xFFFF66CC)      // grade 6 (Immortal)
private val AncientAuraStroke = Stroke(width = 2.5f)
private val MythicAuraStroke = Stroke(width = 3f)
private val ImmortalAuraStroke = Stroke(width = 3.5f)

// Grid cell glow
private val CellHighlight = Color.White.copy(alpha = 0.03f)
private val CellHighlightBright = Color.White.copy(alpha = 0.06f)

private val GradeColors = GradeColorsByIndex
private val FamilyAuraColors = FamilyColorsByIndex

// ── G1: Grade-based platform colors (pre-allocated) ──
private val PlatformCommonColor = Color(0xFF9E9E9E)
private val PlatformRareColor = Color(0xFF42A5F5)
private val PlatformRareGlow = Color(0xFF42A5F5).copy(alpha = 0.3f)
private val PlatformHeroColor = Color(0xFFAB47BC)
private val PlatformHeroGlow = Color(0xFFAB47BC).copy(alpha = 0.35f)
private val PlatformHeroParticle = Color(0xFFCE93D8)
private val PlatformLegendColor = Color(0xFFFF8F00)
private val PlatformLegendFlame = Color(0xFFFFD54F)
private val PlatformAncientColor = Color(0xFFEF4444)
private val PlatformAncientRing = Color(0xFFFF6B35)
private val PlatformMythicGold = Color(0xFFFBBF24)
private val PlatformImmortalWhite = Color.White
private val PlatformImmortalPink = Color(0xFFF0ABFC)

// Rainbow shimmer colors for Mythic platform
private val RainbowShimmer = arrayOf(
    Color(0xFFFF4444), Color(0xFFFF8800), Color(0xFFFFDD00),
    Color(0xFF44FF44), Color(0xFF4488FF), Color(0xFF8844FF),
)

// ── G5: Unit dissolution effect data class ──
private data class UnitDissolutionEffect(
    val x: Float, val y: Float,
    val grade: Int, val family: Int,
    val startTime: Float,
)

// Grid area in 720x720 space — must match Grid.kt (480x480 centered)
private const val CPP_GRID_W = 480f
private const val CPP_GRID_H = 480f
private const val GRID_NORM_X = 120f / 720f      // 0.16667 (Grid.ORIGIN_X / 720)
private const val GRID_NORM_Y = 107.5f / 720f    // 0.14931 (Grid.ORIGIN_Y / 720)
private const val GRID_NORM_W = CPP_GRID_W / 720f   // 0.66667
private const val GRID_NORM_H = CPP_GRID_H / 720f   // 0.66667

private const val GRID_COLS = 5
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

    // G3: Per-unit micro-movement offsets (interleaved [dx0,dy0,dx1,dy1,...])
    val microOffsets = remember { mutableStateOf(FloatArray(0)) }

    // G5: Death effects list + previous tile set for detection
    val deathEffects = remember { mutableStateOf(listOf<UnitDissolutionEffect>()) }
    val prevTileMap = remember { mutableStateOf(mapOf<Int, Pair<Float, Float>>()) }
    val prevGradeMap = remember { mutableStateOf(mapOf<Int, Int>()) }
    val prevFamilyMap = remember { mutableStateOf(mapOf<Int, Int>()) }

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
                    microOffsets.value = FloatArray(data.count * 2)
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

                // G3: Update micro-movement offsets slowly
                val mo = microOffsets.value
                if (mo.size == data.count * 2) {
                    val newMo = mo.copyOf()
                    for (idx in 0 until data.count * 2) {
                        val target = sin(animTime.floatValue * 0.3f + idx * 1.7f) * 1.2f
                        newMo[idx] = newMo[idx] + (target - newMo[idx]) * 0.02f
                    }
                    microOffsets.value = newMo
                }

                // G5: Detect removed units by comparing tile sets
                val currentTiles = mutableMapOf<Int, Pair<Float, Float>>()
                val currentGrades = mutableMapOf<Int, Int>()
                val currentFamilies = mutableMapOf<Int, Int>()
                for (i in 0 until data.count) {
                    val tile = data.tileIndices[i]
                    currentTiles[tile] = Pair(data.xs[i], data.ys[i])
                    currentGrades[tile] = data.grades[i]
                    currentFamilies[tile] = com.example.jaygame.data.unitFamilyOf(data.unitDefIds[i])
                }
                val prev = prevTileMap.value
                if (prev.isNotEmpty()) {
                    val removed = prev.keys - currentTiles.keys
                    if (removed.isNotEmpty()) {
                        val newEffects = deathEffects.value.toMutableList()
                        for (tile in removed) {
                            val pos = prev[tile] ?: continue
                            val gr = prevGradeMap.value[tile] ?: 0
                            val fam = prevFamilyMap.value[tile] ?: 0
                            newEffects.add(UnitDissolutionEffect(pos.first, pos.second, gr, fam, animTime.floatValue))
                        }
                        deathEffects.value = newEffects
                    }
                }
                prevTileMap.value = currentTiles
                prevGradeMap.value = currentGrades
                prevFamilyMap.value = currentFamilies

                // Expire old death effects (> 1s)
                val tNow = animTime.floatValue
                deathEffects.value = deathEffects.value.filter { tNow - it.startTime < 1f }
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
        val moArr = microOffsets.value
        val useMicro = moArr.size == data.count * 2

        // Size based on cell height (grid is 880x440, 6cols x 5rows -> cells are 146x88)
        val cellH = gridH / GRID_ROWS.toFloat()
        val unitSize = cellH * 0.85f
        val pedestalRx = unitSize * 0.45f
        val pedestalRy = pedestalRx * 0.4f
        val gridState = BattleBridge.gridState.value

        for (i in 0 until data.count) {
            val baseScreenX = if (useSmooth) sxArr[i] * w else data.xs[i] * w
            val baseScreenY = if (useSmooth) syArr[i] * h else data.ys[i] * h
            // G3: micro-movement offset
            val microDx = if (useMicro) moArr[i * 2] else 0f
            val microDy = if (useMicro) moArr[i * 2 + 1] else 0f
            val screenXBase = baseScreenX + microDx
            val screenYBase = baseScreenY + microDy

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

            // ── G3: Breathing scale (slow sine 0.97-1.03) ──
            val breathScale = 1f + sin(t * 1.8f + unitDefId * 0.3f) * 0.03f

            // ── G2: Family-specific attack motion offsets ──
            var attackOffsetX = 0f
            var attackOffsetY = 0f
            var attackRotation = 0f
            val attackScale: Float
            if (isAttacking) {
                val attackT = t * 15f
                attackScale = when (family) {
                    0 -> { // Fire: thrust forward
                        attackOffsetY = -sin(attackT) * 6f
                        1f + sin(attackT) * 0.1f
                    }
                    1 -> { // Frost: pulse/expand
                        1f + sin(attackT) * 0.2f
                    }
                    2 -> { // Poison: lean forward
                        attackOffsetY = -abs(sin(attackT)) * 5f
                        attackOffsetX = sin(attackT * 0.5f) * 2f
                        1f + sin(attackT) * 0.08f
                    }
                    3 -> { // Lightning: quick jitter/vibrate
                        attackOffsetX = sin(attackT * 3f) * 3f
                        attackOffsetY = cos(attackT * 2.7f) * 2f
                        1f + sin(attackT) * 0.12f
                    }
                    4 -> { // Support: raise up slightly
                        attackOffsetY = -abs(sin(attackT * 0.7f)) * 8f
                        1f + sin(attackT) * 0.1f
                    }
                    5 -> { // Wind: spin motion
                        attackRotation = sin(attackT) * 0.3f
                        attackOffsetX = sin(attackT) * 4f
                        1f + sin(attackT) * 0.1f
                    }
                    else -> 1f + sin(attackT) * 0.15f
                }
            } else {
                attackScale = 1f
            }

            val finalScale = breathScale * attackScale
            val screenX = screenXBase + attackOffsetX
            val screenY = screenYBase + attackOffsetY

            // ── E7: Grade 4+ aura ring ──
            if (grade >= 4) {
                val auraColor = when (grade) {
                    4 -> AncientAuraRed
                    5 -> MythicAuraGold
                    else -> ImmortalAuraPink // grade 6+
                }
                val auraStroke = when (grade) {
                    4 -> AncientAuraStroke
                    5 -> MythicAuraStroke
                    else -> ImmortalAuraStroke
                }
                val auraPulseAlpha = (0.35f + sin(t * 3f + unitDefId * 0.4f) * 0.2f).coerceIn(0.15f, 0.55f)
                val auraRadius = unitSize * (0.55f + sin(t * 2f + unitDefId * 0.3f) * 0.05f)

                // Outer glow fill
                drawCircle(
                    color = auraColor.copy(alpha = auraPulseAlpha * 0.3f),
                    radius = auraRadius * 1.2f,
                    center = Offset(screenX, screenY - unitSize * 0.2f),
                )
                // Aura ring
                drawCircle(
                    color = auraColor.copy(alpha = auraPulseAlpha),
                    radius = auraRadius,
                    center = Offset(screenX, screenY - unitSize * 0.2f),
                    style = auraStroke,
                )
                // Inner bright core for grade 6 (holographic shimmer)
                if (grade >= 6) {
                    val shimmerAlpha = (0.2f + sin(t * 5f + unitDefId * 0.6f) * 0.15f).coerceIn(0.05f, 0.35f)
                    drawCircle(
                        color = Color.White.copy(alpha = shimmerAlpha),
                        radius = auraRadius * 0.7f,
                        center = Offset(screenX, screenY - unitSize * 0.2f),
                    )
                }
            }

            // ── Aura/Shield range circle (Support family=4, or high-grade aura units) ──
            if (family == 4 && grade >= 2) {
                // AURA_RADIUS=150 in game world (720x720), convert to screen
                val auraRadiusScreen = (150f / 720f) * w
                val auraPulse = 0.08f + sin(t * 2f + unitDefId * 0.5f) * 0.04f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            FamilyAuraColors[4].copy(alpha = auraPulse * 0.5f),
                            FamilyAuraColors[4].copy(alpha = auraPulse),
                            Color.Transparent,
                        ),
                    ),
                    radius = auraRadiusScreen,
                    center = Offset(screenX, screenY),
                )
                // Aura ring border
                drawCircle(
                    color = FamilyAuraColors[4].copy(alpha = auraPulse * 1.5f),
                    radius = auraRadiusScreen,
                    center = Offset(screenX, screenY),
                    style = Stroke(width = 1f),
                )
            }

            // ── Idle Bounce ──
            val bounceOffset = sin(t * 2.5f + unitDefId * 0.7f) * 3f

            // ── G1: Grade-based platform effect ──
            drawGradePlatform(
                grade = grade,
                screenX = screenX,
                screenY = screenY,
                pedestalRx = pedestalRx,
                pedestalRy = pedestalRy,
                t = t,
                unitDefId = unitDefId,
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

            // Unit sprite (proper drawable icon) with bounce + attack scale + breathing
            val bitmap = unitBitmaps[unitDefId]
            if (bitmap != null) {
                val spriteSize = unitSize * 0.85f * finalScale
                val spriteY = screenY - spriteSize - pedestalRy * 0.3f + bounceOffset

                // G2: Wind family spin via canvas rotation
                if (isAttacking && family == 5 && attackRotation != 0f) {
                    drawContext.canvas.save()
                    drawContext.canvas.translate(screenX, spriteY + spriteSize / 2f)
                    drawContext.canvas.rotate(attackRotation * (180f / PI.toFloat()))
                    drawContext.canvas.translate(-screenX, -(spriteY + spriteSize / 2f))
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
                    drawContext.canvas.restore()
                } else {
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
                    val sx2 = screenX - badgeWidth / 2 + 6f + s * 8f
                    val sy2 = badgeY + badgeHeight / 2
                    drawCircle(
                        color = StarColor,
                        radius = 2.5f,
                        center = Offset(sx2, sy2),
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

        // ── G5: Draw death/dissolution effects ──
        for (effect in deathEffects.value) {
            val elapsed = t - effect.startTime
            val progress = (elapsed / 1f).coerceIn(0f, 1f)
            val effectColor = GradeColors.getOrElse(effect.grade) { Color.Gray }
            val famColor = FamilyAuraColors.getOrElse(effect.family) { FamilyAuraColors[0] }
            val ex = effect.x * w
            val ey = effect.y * h

            if (effect.grade <= 0) {
                // Common: simple fade out circle
                val fadeAlpha = (1f - progress) * 0.6f
                drawCircle(
                    color = effectColor.copy(alpha = fadeAlpha),
                    radius = unitSize * 0.4f * (1f + progress * 0.5f),
                    center = Offset(ex, ey),
                )
            } else if (effect.grade <= 2) {
                // Rare/Hero: particles scatter outward in family color
                val particleCount = 8 + effect.grade * 4
                for (p in 0 until particleCount) {
                    val angle = (p.toFloat() / particleCount) * 2f * PI.toFloat()
                    val dist = progress * unitSize * 0.8f
                    val px = (ex + cos(angle) * dist).toFloat()
                    val py = (ey + sin(angle) * dist).toFloat()
                    val pAlpha = (1f - progress) * 0.7f
                    drawCircle(
                        color = famColor.copy(alpha = pAlpha),
                        radius = 2f + (1f - progress) * 2f,
                        center = Offset(px, py),
                    )
                }
            } else {
                // Legend+: dramatic burst with color-coded explosion
                val burstRadius = progress * unitSize * 1.2f
                val burstAlpha = (1f - progress) * 0.5f
                // Outer ring
                drawCircle(
                    color = effectColor.copy(alpha = burstAlpha),
                    radius = burstRadius,
                    center = Offset(ex, ey),
                    style = Stroke(width = 3f + (1f - progress) * 4f),
                )
                // Inner glow
                drawCircle(
                    color = effectColor.copy(alpha = burstAlpha * 0.6f),
                    radius = burstRadius * 0.5f,
                    center = Offset(ex, ey),
                )
                // Scatter particles
                val particleCount = 12 + effect.grade * 3
                for (p in 0 until particleCount) {
                    val angle = (p.toFloat() / particleCount) * 2f * PI.toFloat() + progress * 2f
                    val dist = progress * unitSize * (0.6f + sin(p * 1.3f) * 0.3f)
                    val px = (ex + cos(angle) * dist).toFloat()
                    val py = (ey + sin(angle) * dist).toFloat()
                    val pAlpha = (1f - progress) * 0.8f
                    drawCircle(
                        color = famColor.copy(alpha = pAlpha),
                        radius = 1.5f + (1f - progress) * 2.5f,
                        center = Offset(px, py),
                    )
                }
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

/**
 * G1: Draw grade-based platform/pedestal circle beneath the unit.
 * Each grade gets a distinct visual treatment.
 */
private fun DrawScope.drawGradePlatform(
    grade: Int,
    screenX: Float,
    screenY: Float,
    pedestalRx: Float,
    pedestalRy: Float,
    t: Float,
    unitDefId: Int,
) {
    // Ground shadow (always drawn)
    drawOval(
        color = PedestalShadow,
        topLeft = Offset(screenX - pedestalRx, screenY + pedestalRx * 0.05f),
        size = Size(pedestalRx * 2, pedestalRy * 2),
    )

    when (grade) {
        0 -> {
            // Common: simple gray circle
            drawOval(
                color = PlatformCommonColor.copy(alpha = 0.25f),
                topLeft = Offset(screenX - pedestalRx * 0.9f, screenY - pedestalRy * 0.2f),
                size = Size(pedestalRx * 1.8f, pedestalRy * 1.8f),
            )
        }
        1 -> {
            // Rare: blue soft glow circle
            val glowAlpha = 0.2f + sin(t * 2f + unitDefId * 0.5f) * 0.05f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PlatformRareColor.copy(alpha = glowAlpha + 0.15f),
                        PlatformRareGlow,
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(screenX - pedestalRx * 1.1f, screenY - pedestalRy * 0.4f),
                size = Size(pedestalRx * 2.2f, pedestalRy * 2.2f),
            )
        }
        2 -> {
            // Hero: purple glow with subtle orbiting particles
            val glowAlpha = 0.25f + sin(t * 2.5f + unitDefId * 0.4f) * 0.08f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PlatformHeroColor.copy(alpha = glowAlpha + 0.1f),
                        PlatformHeroGlow,
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(screenX - pedestalRx * 1.15f, screenY - pedestalRy * 0.45f),
                size = Size(pedestalRx * 2.3f, pedestalRy * 2.3f),
            )
            for (p in 0 until 4) {
                val angle = t * 1.2f + p * (PI.toFloat() / 2f)
                val px = screenX + cos(angle) * pedestalRx * 0.7f
                val py = screenY + sin(angle) * pedestalRy * 0.5f
                val pAlpha = 0.3f + sin(t * 3f + p * 1.5f) * 0.15f
                drawCircle(
                    color = PlatformHeroParticle.copy(alpha = pAlpha),
                    radius = 1.5f,
                    center = Offset(px, py),
                )
            }
        }
        3 -> {
            // Legend: gold flaming circle with animated alpha
            val flameAlpha = 0.3f + sin(t * 4f + unitDefId * 0.6f) * 0.12f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PlatformLegendFlame.copy(alpha = flameAlpha + 0.1f),
                        PlatformLegendColor.copy(alpha = flameAlpha),
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(screenX - pedestalRx * 1.2f, screenY - pedestalRy * 0.5f),
                size = Size(pedestalRx * 2.4f, pedestalRy * 2.4f),
            )
            drawOval(
                color = PlatformLegendColor.copy(alpha = flameAlpha + 0.1f),
                topLeft = Offset(screenX - pedestalRx * 1.0f, screenY - pedestalRy * 0.3f),
                size = Size(pedestalRx * 2.0f, pedestalRy * 1.8f),
                style = Stroke(width = 1.5f),
            )
        }
        4 -> {
            // Ancient: red fire ring with animated pulse
            val pulseScale = 1f + sin(t * 3f + unitDefId * 0.4f) * 0.08f
            val ringAlpha = 0.35f + sin(t * 5f + unitDefId * 0.3f) * 0.15f
            val rx = pedestalRx * 1.1f * pulseScale
            val ry = pedestalRy * 1.1f * pulseScale
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PlatformAncientRing.copy(alpha = ringAlpha),
                        PlatformAncientColor.copy(alpha = ringAlpha * 0.7f),
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(screenX - rx, screenY - ry * 0.4f),
                size = Size(rx * 2f, ry * 2f),
            )
            drawOval(
                color = PlatformAncientColor.copy(alpha = ringAlpha + 0.1f),
                topLeft = Offset(screenX - rx * 0.9f, screenY - ry * 0.3f),
                size = Size(rx * 1.8f, ry * 1.6f),
                style = Stroke(width = 2f),
            )
            drawOval(
                color = PlatformAncientRing.copy(alpha = ringAlpha * 0.6f),
                topLeft = Offset(screenX - rx * 0.7f, screenY - ry * 0.15f),
                size = Size(rx * 1.4f, ry * 1.2f),
                style = Stroke(width = 1f),
            )
        }
        5 -> {
            // Mythic: golden holographic ring with rainbow shimmer
            val shimmerPhase = t * 2f + unitDefId * 0.5f
            val shimmerIdx = ((shimmerPhase % (2f * PI.toFloat())) / (2f * PI.toFloat()) * RainbowShimmer.size).toInt().coerceIn(0, RainbowShimmer.size - 1)
            val nextIdx = (shimmerIdx + 1) % RainbowShimmer.size
            val lerpFrac = (shimmerPhase % 1f)
            val shimmerColor = Color(
                red = RainbowShimmer[shimmerIdx].red * (1f - lerpFrac) + RainbowShimmer[nextIdx].red * lerpFrac,
                green = RainbowShimmer[shimmerIdx].green * (1f - lerpFrac) + RainbowShimmer[nextIdx].green * lerpFrac,
                blue = RainbowShimmer[shimmerIdx].blue * (1f - lerpFrac) + RainbowShimmer[nextIdx].blue * lerpFrac,
                alpha = 0.35f,
            )
            val mythicAlpha = 0.3f + sin(t * 3f) * 0.1f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PlatformMythicGold.copy(alpha = mythicAlpha + 0.1f),
                        PlatformMythicGold.copy(alpha = mythicAlpha * 0.5f),
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(screenX - pedestalRx * 1.3f, screenY - pedestalRy * 0.6f),
                size = Size(pedestalRx * 2.6f, pedestalRy * 2.6f),
            )
            drawOval(
                color = shimmerColor,
                topLeft = Offset(screenX - pedestalRx * 1.1f, screenY - pedestalRy * 0.35f),
                size = Size(pedestalRx * 2.2f, pedestalRy * 2.0f),
                style = Stroke(width = 2.5f),
            )
            drawOval(
                color = PlatformMythicGold.copy(alpha = mythicAlpha + 0.15f),
                topLeft = Offset(screenX - pedestalRx * 0.9f, screenY - pedestalRy * 0.2f),
                size = Size(pedestalRx * 1.8f, pedestalRy * 1.6f),
                style = Stroke(width = 1.5f),
            )
        }
        6 -> {
            // Immortal: divine white+pink radiant glow with rays
            val divineAlpha = 0.3f + sin(t * 2f + unitDefId * 0.3f) * 0.1f
            val pulseScale = 1f + sin(t * 1.5f) * 0.05f
            val rx = pedestalRx * 1.4f * pulseScale
            val ry = pedestalRy * 1.4f * pulseScale
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PlatformImmortalWhite.copy(alpha = divineAlpha * 0.5f),
                        PlatformImmortalPink.copy(alpha = divineAlpha * 0.4f),
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(screenX - rx, screenY - ry * 0.5f),
                size = Size(rx * 2f, ry * 2f),
            )
            // Radiant rays
            for (r in 0 until 8) {
                val angle = (r.toFloat() / 8f) * 2f * PI.toFloat() + t * 0.5f
                val rayLen = pedestalRx * (0.8f + sin(t * 3f + r * 1.1f) * 0.2f)
                val rayAlpha = divineAlpha * (0.6f + sin(t * 4f + r * 0.8f) * 0.3f)
                val endX = screenX + cos(angle) * rayLen
                val endY = screenY + sin(angle) * rayLen * 0.5f
                drawLine(
                    color = PlatformImmortalWhite.copy(alpha = rayAlpha),
                    start = Offset(screenX, screenY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.5f,
                )
            }
            drawOval(
                color = PlatformImmortalPink.copy(alpha = divineAlpha + 0.15f),
                topLeft = Offset(screenX - pedestalRx * 1.0f, screenY - pedestalRy * 0.3f),
                size = Size(pedestalRx * 2.0f, pedestalRy * 1.8f),
                style = Stroke(width = 2f),
            )
            drawOval(
                color = PlatformImmortalWhite.copy(alpha = divineAlpha * 0.7f),
                topLeft = Offset(screenX - pedestalRx * 0.8f, screenY - pedestalRy * 0.15f),
                size = Size(pedestalRx * 1.6f, pedestalRy * 1.4f),
                style = Stroke(width = 1f),
            )
        }
    }
}
