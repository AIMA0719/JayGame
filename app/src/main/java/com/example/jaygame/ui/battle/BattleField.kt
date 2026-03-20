package com.example.jaygame.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import com.example.jaygame.R
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

// Family idle effect colors (pre-allocated)
private val FireEmberColor = Color(0xFFFF9800)
private val FireEmberBright = Color(0xFFFFDD44)
private val FrostSnowColor = Color(0xFFBBDDFF)
private val FrostSnowBright = Color(0xFFE3F2FD)
private val PoisonDripColor = Color(0xFF81C784)
private val PoisonDripBright = Color(0xFFCCFF00)
private val LightSparkColor = Color(0xFFFFD54F)
private val LightSparkBright = Color(0xFFFFFF88)
private val SupportGlowColor = Color(0xFFCE93D8)
private val SupportGlowBright = Color(0xFFF0ABFC)
private val WindLeafColor = Color(0xFF80CBC4)
private val WindLeafBright = Color(0xFFB2EBF2)
private val WindLeafIdle03 = Color(0xFF80CBC4).copy(alpha = 0.3f)

// Attack flash colors per family (pre-allocated)
private val FireAttackFlash = Color(0xFFFF4400)
private val FrostAttackFlash = Color(0xFF90CAF9)
private val PoisonAttackFlash = Color(0xFF66BB6A)
private val LightAttackFlash = Color(0xFFFFFF00)
private val SupportAttackFlash = Color(0xFFE1BEE7)
private val WindAttackFlash = Color(0xFF4DD0E1)

// Pre-allocated constant-alpha colors for hot loop
private val SelectedCyan07 = Color(0xFF5BA4CF).copy(alpha = 0.7f)
private val TankHpBg = Color(0xFF333333)
private val TankHpBar = Color(0xFF4CAF50)
private val DashTrailWhite03 = Color.White.copy(alpha = 0.3f)
private val SpecialFieldFill = Color(0xFF9C27B0).copy(alpha = 0.12f)
private val SpecialFieldStroke = Color(0xFF9C27B0).copy(alpha = 0.25f)
private val DragShadow03 = Color.Black.copy(alpha = 0.3f)

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


/**
 * Battlefield overlay — unit sprites rendered in Compose (proper drawable icons),
 * aura/particle effects rendered by C++ engine behind this layer.
 */
@Composable
fun BattleField() {
    val unitPositions by BattleBridge.unitPositions.collectAsState()
    val selectedTile by BattleBridge.selectedTile.collectAsState()
    val context = LocalContext.current

    // Legacy unit bitmaps (keyed by unitDefId)
    val unitBitmaps = remember {
        UNIT_DEFS_MAP.mapValues { (_, def) ->
            ContextCompat.getDrawable(context, def.iconRes)?.toBitmap(128, 128)?.asImageBitmap()
        }
    }

    // Blueprint fallback bitmaps — map family+grade to existing icon resources.
    // Legacy icon pattern: family 0-4 → id = grade*5+family; family 5 (wind) → id = 35+grade
    // UNIT_ICONS covers ids 0-24 and 35-39, higher grades fall back to family's base icon.
    val blueprintBitmapCache = remember {
        val cache = mutableMapOf<String, ImageBitmap?>()
        val iconMap = mapOf(
            0 to R.drawable.ic_unit_0, 1 to R.drawable.ic_unit_1, 2 to R.drawable.ic_unit_2,
            3 to R.drawable.ic_unit_3, 4 to R.drawable.ic_unit_4, 5 to R.drawable.ic_unit_5,
            6 to R.drawable.ic_unit_6, 7 to R.drawable.ic_unit_7, 8 to R.drawable.ic_unit_8,
            9 to R.drawable.ic_unit_9, 10 to R.drawable.ic_unit_10, 11 to R.drawable.ic_unit_11,
            12 to R.drawable.ic_unit_12, 13 to R.drawable.ic_unit_13, 14 to R.drawable.ic_unit_14,
            15 to R.drawable.ic_unit_15, 16 to R.drawable.ic_unit_16, 17 to R.drawable.ic_unit_17,
            18 to R.drawable.ic_unit_18, 19 to R.drawable.ic_unit_19, 20 to R.drawable.ic_unit_20,
            21 to R.drawable.ic_unit_21, 22 to R.drawable.ic_unit_22, 23 to R.drawable.ic_unit_23,
            24 to R.drawable.ic_unit_24,
            35 to R.drawable.ic_unit_35, 36 to R.drawable.ic_unit_36, 37 to R.drawable.ic_unit_37,
            38 to R.drawable.ic_unit_38, 39 to R.drawable.ic_unit_39,
        )
        fun iconForFamilyGrade(familyOrd: Int, gradeOrd: Int): Int {
            val id = if (familyOrd < 5) gradeOrd * 5 + familyOrd else 35 + gradeOrd
            return iconMap[id] ?: iconMap[familyOrd.coerceIn(0, 4)] ?: R.drawable.ic_unit_0
        }
        // Pre-load bitmaps for all blueprints
        val registry = com.example.jaygame.engine.BlueprintRegistry.instance
        for (bp in registry.all()) {
            val familyOrd = bp.families.firstOrNull()?.ordinal ?: 0
            val gradeOrd = bp.grade.ordinal
            val resId = iconForFamilyGrade(familyOrd, gradeOrd)
            cache[bp.id] = ContextCompat.getDrawable(context, resId)?.toBitmap(128, 128)?.asImageBitmap()
        }
        cache
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
    // PERF: Persistent mutable maps — reused each frame (clear + refill) to avoid HashMap allocation
    val prevTileXs = remember { mutableStateOf(mutableMapOf<Int, Float>()) }
    val prevTileYs = remember { mutableStateOf(mutableMapOf<Int, Float>()) }
    val prevGradeMap = remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    val prevFamilyMap = remember { mutableStateOf(mutableMapOf<Int, Int>()) }

    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                val dt = if (lastFrameNanos == 0L) 1f / 60f
                         else ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastFrameNanos = frameTimeNanos
                animTime.floatValue += dt

                val data = BattleBridge.unitPositions.value
                val sx = smoothXs.value
                val sy = smoothYs.value

                if (data.count != sx.size) {
                    smoothXs.value = data.xs.copyOf(data.count)
                    smoothYs.value = data.ys.copyOf(data.count)
                    microOffsets.value = FloatArray(data.count * 2)
                } else if (data.count > 0) {
                    // PERF: Mutate arrays in-place instead of allocating new ones each frame
                    val lerpFactor = 0.25f
                    for (i in 0 until data.count) {
                        sx[i] = sx[i] + (data.xs[i] - sx[i]) * lerpFactor
                        sy[i] = sy[i] + (data.ys[i] - sy[i]) * lerpFactor
                    }
                    // Trigger recomposition by reassigning the same array wrapped in a new state
                    smoothXs.value = sx
                    smoothYs.value = sy
                }

                // G3: Update micro-movement offsets slowly (in-place)
                val mo = microOffsets.value
                if (mo.size == data.count * 2) {
                    for (idx in 0 until data.count * 2) {
                        val target = sin(animTime.floatValue * 0.3f + idx * 1.7f) * 1.2f
                        mo[idx] = mo[idx] + (target - mo[idx]) * 0.02f
                    }
                    microOffsets.value = mo
                }

                // G5: Detect removed units by comparing tile sets
                // PERF: Reuse persistent maps — clear + refill avoids HashMap allocation per frame
                val pxs = prevTileXs.value
                val pys = prevTileYs.value
                val pGrades = prevGradeMap.value
                val pFamilies = prevFamilyMap.value

                // Check for removals before clearing
                if (pxs.isNotEmpty()) {
                    var hasRemoved = false
                    // Build current tile set cheaply (just check tileIndices)
                    for (tile in pxs.keys) {
                        var found = false
                        for (j in 0 until data.count) {
                            if (data.tileIndices[j] == tile) { found = true; break }
                        }
                        if (!found) { hasRemoved = true; break }
                    }
                    if (hasRemoved) {
                        val newEffects = deathEffects.value.toMutableList()
                        for (tile in pxs.keys) {
                            var found = false
                            for (j in 0 until data.count) {
                                if (data.tileIndices[j] == tile) { found = true; break }
                            }
                            if (!found) {
                                val ex = pxs[tile] ?: continue
                                val ey = pys[tile] ?: continue
                                val gr = pGrades[tile] ?: 0
                                val fam = pFamilies[tile] ?: 0
                                newEffects.add(UnitDissolutionEffect(ex, ey, gr, fam, animTime.floatValue))
                            }
                        }
                        deathEffects.value = newEffects
                    }
                }

                // Clear and refill (no allocation — reuses existing maps)
                pxs.clear(); pys.clear(); pGrades.clear(); pFamilies.clear()
                for (i in 0 until data.count) {
                    val tile = data.tileIndices[i]
                    pxs[tile] = data.xs[i]
                    pys[tile] = data.ys[i]
                    pGrades[tile] = data.grades[i]
                    pFamilies[tile] = if (i < data.familiesList.size && data.familiesList[i].isNotEmpty()) {
                        data.familiesList[i].first().ordinal
                    } else {
                        com.example.jaygame.data.unitFamilyOf(data.unitDefIds[i])
                    }
                }

                // Expire old death effects (> 1s)
                val tNow = animTime.floatValue
                val effects = deathEffects.value
                if (effects.isNotEmpty()) {
                    deathEffects.value = effects.filter { tNow - it.startTime < 1f }
                }
            }
        }
    }

    // Drag state for unit relocation (immediate drag, no long-press)
    var dragFromTile by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var dragTotalDistance by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
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
                            dragTotalDistance = 0f
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                        dragTotalDistance += dragAmount.getDistance()
                    },
                    onDragEnd = {
                        if (isDragging && dragFromTile >= 0) {
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            if (dragTotalDistance < 15f) {
                                // Short drag = tap → show unit popup
                                BattleBridge.requestClickTile(dragFromTile)
                            } else {
                                // Real drag → relocate unit
                                val normX = dragOffset.x / w
                                val normY = dragOffset.y / h
                                BattleBridge.requestRelocate(dragFromTile, normX, normY)
                            }
                        }
                        isDragging = false
                        dragFromTile = -1
                        dragOffset = Offset.Zero
                        dragTotalDistance = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragFromTile = -1
                        dragOffset = Offset.Zero
                        dragTotalDistance = 0f
                    },
                )
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

        // Unit size based on field height (no grid cells)
        val unitSize = gridH * 0.12f
        val pedestalRx = unitSize * 0.45f
        val pedestalRy = pedestalRx * 0.4f
        val gridState = BattleBridge.gridState.value
        // PERF: Reduce visual detail when many units exist
        val highUnitCount = data.count > 50

        for (i in 0 until data.count) {
            if (i >= data.xs.size || i >= data.ys.size || i >= data.grades.size ||
                i >= data.tileIndices.size || i >= data.unitDefIds.size || i >= data.levels.size) continue
            val baseScreenX = if (useSmooth) sxArr[i] * w else data.xs[i] * w
            val baseScreenY = if (useSmooth) syArr[i] * h else data.ys[i] * h
            val grade = data.grades[i]
            // G3: micro-movement offset (skip for common units in high-count mode)
            val skipMicro = highUnitCount && grade <= 1
            val microDx = if (useMicro && !skipMicro) moArr[i * 2] else 0f
            val microDy = if (useMicro && !skipMicro) moArr[i * 2 + 1] else 0f
            val screenXBase = baseScreenX + microDx
            val screenYBase = baseScreenY + microDy
            val gradeColor = GradeColors.getOrElse(grade) { Color.Gray }
            val tileIdx = data.tileIndices[i]
            val isSelected = selectedTile == tileIdx
            val isAttacking = data.isAttacking.getOrElse(i) { false }
            val family = if (i < data.familiesList.size && data.familiesList[i].isNotEmpty()) {
                data.familiesList[i].first().ordinal
            } else {
                com.example.jaygame.data.unitFamilyOf(data.unitDefIds[i])
            }
            val unitDefId = data.unitDefIds[i]
            val level = data.levels[i]

            // Skip dragged unit at original position
            if (isDragging && dragFromTile == tileIdx) continue

            // ── G3: Breathing scale (slow sine 0.97-1.03, skip for low grade in high-count) ──
            val breathScale = if (skipMicro) 1f else 1f + sin(t * 1.8f + unitDefId * 0.3f) * 0.03f

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
            if (family == 4 && grade >= 2 && (!highUnitCount || grade >= 3)) {
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

            // ── Selected unit: range circle ──
            if (isSelected) {
                // Approximate range from UnitDefs (default ~200 world units in 720 world)
                val rangeScreen = (200f / 720f) * w // fallback range
                val rangePulse = 0.15f + sin(t * 3f) * 0.05f
                drawCircle(
                    color = gradeColor.copy(alpha = rangePulse),
                    radius = rangeScreen,
                    center = Offset(screenX, screenY),
                    style = Stroke(width = 1.5f),
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
            // PERF: Use layered ovals instead of Brush.radialGradient to avoid per-frame list+brush allocation
            val familyTint = FamilyAuraColors.getOrElse(family) { FamilyAuraColors[0] }
            val pedestalColor = if (isSelected) NeonCyan else Color(
                red = gradeColor.red * 0.65f + familyTint.red * 0.35f,
                green = gradeColor.green * 0.65f + familyTint.green * 0.35f,
                blue = gradeColor.blue * 0.65f + familyTint.blue * 0.35f,
                alpha = 1f,
            )
            // Outer soft glow
            drawOval(
                color = pedestalColor.copy(alpha = 0.15f),
                topLeft = Offset(screenX - pedestalRx * 1.2f, screenY - pedestalRy * 0.5f),
                size = Size(pedestalRx * 2.4f, pedestalRy * 2.5f),
            )
            // Inner bright glow
            drawOval(
                color = pedestalColor.copy(alpha = 0.4f),
                topLeft = Offset(screenX - pedestalRx * 0.7f, screenY - pedestalRy * 0.1f),
                size = Size(pedestalRx * 1.4f, pedestalRy * 1.5f),
            )

            // ── Pedestal Particles (grade 3+, reduced when many units) ──
            if (grade >= 3 && !(highUnitCount && grade <= 3)) {
                val maxParticles = if (highUnitCount) 4 else 12
                val particleCount = (grade - 1) * 2
                for (p in 0 until particleCount.coerceAtMost(maxParticles)) {
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
                    color = SelectedCyan07,
                    topLeft = Offset(screenX - pedestalRx * 1.1f, screenY - pedestalRy * 0.3f),
                    size = Size(pedestalRx * 2.2f, pedestalRy * 2.2f),
                    style = SelectedStroke,
                )
            }

            // ── Family-specific attack flash (unique per family) ──
            if (isAttacking) {
                val attackPhase = t * 12f
                when (family) {
                    0 -> { // Fire: expanding flame burst
                        val burstAlpha = (0.35f + sin(attackPhase) * 0.2f).coerceIn(0.1f, 0.55f)
                        val burstR = unitSize * (0.5f + sin(attackPhase * 1.5f) * 0.1f)
                        drawCircle(
                            color = FireAttackFlash.copy(alpha = burstAlpha * 0.4f),
                            radius = burstR,
                            center = Offset(screenX, screenY - unitSize * 0.3f + bounceOffset),
                        )
                        drawCircle(
                            color = FireEmberBright.copy(alpha = burstAlpha * 0.3f),
                            radius = burstR * 0.6f,
                            center = Offset(screenX, screenY - unitSize * 0.3f + bounceOffset),
                        )
                    }
                    1 -> { // Frost: ice crystal flash
                        val iceAlpha = (0.3f + sin(attackPhase) * 0.15f).coerceIn(0.1f, 0.5f)
                        val iceR = unitSize * 0.5f
                        drawCircle(
                            color = FrostAttackFlash.copy(alpha = iceAlpha * 0.4f),
                            radius = iceR,
                            center = Offset(screenX, screenY - unitSize * 0.3f + bounceOffset),
                        )
                        // Crystal sparkle points
                        for (sp in 0 until 4) {
                            val spAngle = sp * (PI.toFloat() / 2f) + attackPhase * 0.5f
                            val spR = iceR * 0.7f
                            drawCircle(
                                color = FrostSnowBright.copy(alpha = iceAlpha * 0.5f),
                                radius = 1.5f,
                                center = Offset(
                                    screenX + cos(spAngle) * spR,
                                    screenY - unitSize * 0.3f + bounceOffset + sin(spAngle) * spR,
                                ),
                            )
                        }
                    }
                    2 -> { // Poison: toxic splash
                        val toxicAlpha = (0.3f + sin(attackPhase) * 0.15f).coerceIn(0.1f, 0.45f)
                        drawCircle(
                            color = PoisonAttackFlash.copy(alpha = toxicAlpha * 0.35f),
                            radius = unitSize * 0.5f,
                            center = Offset(screenX, screenY - unitSize * 0.3f + bounceOffset),
                        )
                        // Drip splashes
                        for (sp in 0 until 3) {
                            val spAngle = sp * 2.094f + attackPhase * 0.3f
                            val spDist = unitSize * 0.35f
                            drawCircle(
                                color = PoisonDripBright.copy(alpha = toxicAlpha * 0.4f),
                                radius = 2f,
                                center = Offset(
                                    screenX + cos(spAngle) * spDist,
                                    screenY - unitSize * 0.2f + bounceOffset + sin(spAngle) * spDist * 0.5f,
                                ),
                            )
                        }
                    }
                    3 -> { // Lightning: electric spark burst
                        val sparkAlpha = (0.35f + sin(attackPhase * 2f) * 0.25f).coerceIn(0.1f, 0.6f)
                        drawCircle(
                            color = LightAttackFlash.copy(alpha = sparkAlpha * 0.35f),
                            radius = unitSize * 0.5f,
                            center = Offset(screenX, screenY - unitSize * 0.3f + bounceOffset),
                        )
                        // Electric arc lines
                        for (sp in 0 until 4) {
                            val spAngle = sp * (PI.toFloat() / 2f) + sin(attackPhase * 3f + sp) * 0.8f
                            val spLen = unitSize * 0.4f
                            drawLine(
                                color = LightSparkBright.copy(alpha = sparkAlpha * 0.5f),
                                start = Offset(screenX, screenY - unitSize * 0.3f + bounceOffset),
                                end = Offset(
                                    screenX + cos(spAngle) * spLen,
                                    screenY - unitSize * 0.3f + bounceOffset + sin(spAngle) * spLen,
                                ),
                                strokeWidth = 1.5f,
                            )
                        }
                    }
                    4 -> { // Support: holy glow pulse
                        val holyAlpha = (0.25f + sin(attackPhase * 0.8f) * 0.15f).coerceIn(0.1f, 0.4f)
                        drawCircle(
                            color = SupportAttackFlash.copy(alpha = holyAlpha * 0.3f),
                            radius = unitSize * 0.6f,
                            center = Offset(screenX, screenY - unitSize * 0.3f + bounceOffset),
                        )
                        drawCircle(
                            color = SupportGlowBright.copy(alpha = holyAlpha * 0.2f),
                            radius = unitSize * 0.4f,
                            center = Offset(screenX, screenY - unitSize * 0.3f + bounceOffset),
                        )
                    }
                    5 -> { // Wind: swirl gust
                        val gustAlpha = (0.3f + sin(attackPhase) * 0.15f).coerceIn(0.1f, 0.45f)
                        drawCircle(
                            color = WindAttackFlash.copy(alpha = gustAlpha * 0.3f),
                            radius = unitSize * 0.5f,
                            center = Offset(screenX, screenY - unitSize * 0.3f + bounceOffset),
                        )
                        // Wind streak arcs
                        for (sp in 0 until 3) {
                            val spAngle = sp * 2.094f + attackPhase * 2f
                            val spR = unitSize * 0.35f
                            drawLine(
                                color = WindLeafBright.copy(alpha = gustAlpha * 0.4f),
                                start = Offset(
                                    screenX + cos(spAngle) * spR * 0.5f,
                                    screenY - unitSize * 0.3f + bounceOffset + sin(spAngle) * spR * 0.3f,
                                ),
                                end = Offset(
                                    screenX + cos(spAngle + 0.5f) * spR,
                                    screenY - unitSize * 0.3f + bounceOffset + sin(spAngle + 0.5f) * spR * 0.5f,
                                ),
                                strokeWidth = 1.5f,
                            )
                        }
                    }
                    else -> {
                        val glowAlpha = 0.3f + sin(attackPhase) * 0.15f
                        drawCircle(
                            color = familyTint.copy(alpha = glowAlpha),
                            radius = unitSize * 0.55f,
                            center = Offset(screenX, screenY - unitSize * 0.3f + bounceOffset),
                        )
                    }
                }
            }

            // ── Family-specific idle ambient effects (grade 1+, skip common in high-count) ──
            if (grade >= 1 && !skipMicro) {
                when (family) {
                    0 -> { // Fire: rising ember particles
                        val emberCount = if (highUnitCount) 2 else (1 + grade).coerceAtMost(4)
                        for (e in 0 until emberCount) {
                            val emberPhase = (t * 0.8f + e * 0.5f + unitDefId * 0.17f) % 1f
                            val emberX = screenX + sin(t * 1.5f + e * 2.3f + unitDefId * 0.3f) * unitSize * 0.2f
                            val emberY = screenY - unitSize * 0.2f - emberPhase * unitSize * 0.5f
                            val emberAlpha = sin(emberPhase * PI.toFloat()) * 0.4f
                            drawCircle(
                                color = FireEmberColor.copy(alpha = emberAlpha),
                                radius = 1.5f + sin(t * 3f + e) * 0.5f,
                                center = Offset(emberX, emberY),
                            )
                        }
                    }
                    1 -> { // Frost: gentle snowflake drift
                        val snowCount = if (highUnitCount) 1 else (1 + grade / 2).coerceAtMost(3)
                        for (s in 0 until snowCount) {
                            val snowPhase = (t * 0.5f + s * 0.6f + unitDefId * 0.13f) % 1f
                            val drift = sin(t * 1.2f + s * 1.7f) * unitSize * 0.15f
                            val snowX = screenX + drift
                            val snowY = screenY - unitSize * 0.5f + snowPhase * unitSize * 0.3f
                            val snowAlpha = sin(snowPhase * PI.toFloat()) * 0.35f
                            drawCircle(
                                color = FrostSnowColor.copy(alpha = snowAlpha),
                                radius = 1.5f,
                                center = Offset(snowX, snowY),
                            )
                        }
                    }
                    2 -> { // Poison: dripping droplets
                        val dripCount = if (highUnitCount) 1 else (1 + grade / 2).coerceAtMost(3)
                        for (d in 0 until dripCount) {
                            val dripPhase = (t * 0.6f + d * 0.7f + unitDefId * 0.19f) % 1f
                            val dripX = screenX + sin(d * 2.7f + unitDefId * 0.2f) * unitSize * 0.15f
                            val dripY = screenY - unitSize * 0.1f + dripPhase * unitSize * 0.25f
                            val dripAlpha = sin(dripPhase * PI.toFloat()) * 0.35f
                            drawCircle(
                                color = PoisonDripColor.copy(alpha = dripAlpha),
                                radius = 1.5f + (1f - dripPhase) * 0.5f,
                                center = Offset(dripX, dripY),
                            )
                        }
                    }
                    3 -> { // Lightning: occasional spark flickers
                        val sparkPhase = (t * 2f + unitDefId * 0.31f) % 1f
                        if (sparkPhase < 0.15f) { // Brief flicker
                            val sparkAlpha = (0.15f - sparkPhase) / 0.15f * 0.5f
                            val sparkAngle = sin(t * 7f + unitDefId * 0.5f) * PI.toFloat()
                            val sparkLen = unitSize * 0.2f
                            drawLine(
                                color = LightSparkColor.copy(alpha = sparkAlpha),
                                start = Offset(screenX, screenY - unitSize * 0.3f),
                                end = Offset(
                                    screenX + cos(sparkAngle) * sparkLen,
                                    screenY - unitSize * 0.3f + sin(sparkAngle) * sparkLen,
                                ),
                                strokeWidth = 1f,
                            )
                        }
                    }
                    4 -> { // Support: soft pulsing halo
                        val haloPulse = 0.12f + sin(t * 1.5f + unitDefId * 0.4f) * 0.06f
                        drawCircle(
                            color = SupportGlowColor.copy(alpha = haloPulse),
                            radius = unitSize * 0.4f,
                            center = Offset(screenX, screenY - unitSize * 0.35f),
                        )
                    }
                    5 -> { // Wind: orbiting leaf particles
                        val leafCount = if (highUnitCount) 1 else (1 + grade / 2).coerceAtMost(3)
                        for (l in 0 until leafCount) {
                            val leafAngle = t * 2.5f + l * (6.283f / leafCount) + unitDefId * 0.2f
                            val leafR = unitSize * 0.25f
                            val leafX = screenX + cos(leafAngle) * leafR
                            val leafY = screenY - unitSize * 0.3f + sin(leafAngle) * leafR * 0.4f
                            drawCircle(
                                color = WindLeafIdle03,
                                radius = 1.5f,
                                center = Offset(leafX, leafY),
                            )
                        }
                    }
                }
            }

            // Unit sprite (proper drawable icon) with bounce + attack scale + breathing
            // Try legacy unitDefId first, fallback to blueprintId-based cache
            val bitmap = unitBitmaps[unitDefId]
                ?: (if (i < data.blueprintIds.size) blueprintBitmapCache[data.blueprintIds[i]] else null)
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

            // ── Task 18: Tank HP bar (activated) ──
            val unitRole = data.roles.getOrElse(i) { com.example.jaygame.engine.UnitRole.RANGED_DPS }
            val unitState = data.states.getOrElse(i) { com.example.jaygame.engine.UnitState.IDLE }
            val unitHp = data.hps.getOrElse(i) { 0f }
            val unitMaxHp = data.maxHps.getOrElse(i) { 0f }
            val unitCategory = data.unitCategories.getOrElse(i) { com.example.jaygame.engine.UnitCategory.NORMAL }

            if (unitRole == com.example.jaygame.engine.UnitRole.TANK && unitState == com.example.jaygame.engine.UnitState.BLOCKING && unitMaxHp > 0f) {
                val hpPercent = (unitHp / unitMaxHp).coerceIn(0f, 1f)
                val barWidth = 40f
                val barHeight = 4f
                val barX = screenX - barWidth / 2
                val barY = screenY - unitSize * 0.9f
                drawRect(TankHpBg, Offset(barX, barY), Size(barWidth, barHeight))
                drawRect(TankHpBar, Offset(barX, barY), Size(barWidth * hpPercent, barHeight))
            }

            // ── Task 18: Dash trail (activated) ──
            if (unitState == com.example.jaygame.engine.UnitState.DASHING) {
                val homeScreenX = data.homeXs.getOrElse(i) { data.xs[i] } * w
                val homeScreenY = data.homeYs.getOrElse(i) { data.ys[i] } * h
                drawLine(
                    color = DashTrailWhite03,
                    start = Offset(homeScreenX, homeScreenY),
                    end = Offset(screenX, screenY),
                    strokeWidth = 2f,
                )
            }

            // ── Task 18: Field effect range circles for SPECIAL units (activated) ──
            if (unitCategory == com.example.jaygame.engine.UnitCategory.SPECIAL) {
                // Use unit range as field effect radius (approximation)
                val effectRadius = (200f / 720f) * w // default field effect radius
                drawCircle(
                    color = SpecialFieldFill,
                    radius = effectRadius,
                    center = Offset(screenX, screenY),
                )
                drawCircle(
                    color = SpecialFieldStroke,
                    radius = effectRadius,
                    center = Offset(screenX, screenY),
                    style = Stroke(width = 1.5f),
                )
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

        // Draw drag ghost at finger position — lifted Z-axis effect
        if (isDragging && dragFromTile >= 0) {
            val data2 = BattleBridge.unitPositions.value
            val dragUnitIdx = (0 until data2.count).firstOrNull {
                data2.tileIndices[it] == dragFromTile
            }
            if (dragUnitIdx != null) {
                val liftOffset = 12f  // Z-axis lift: unit drawn higher
                val liftScale = 1.25f // Scaled up when lifted
                val liftedSize = unitSize * liftScale
                val liftedX = dragOffset.x
                val liftedY = dragOffset.y - liftOffset

                // Drop shadow (on ground)
                drawOval(
                    color = DragShadow03,
                    topLeft = Offset(dragOffset.x - liftedSize * 0.4f, dragOffset.y + liftedSize * 0.1f),
                    size = Size(liftedSize * 0.8f, liftedSize * 0.3f),
                )

                // Lifted unit
                val bitmap = unitBitmaps[data2.unitDefIds[dragUnitIdx]]
                    ?: blueprintBitmapCache[data2.blueprintIds.getOrElse(dragUnitIdx) { "" }]
                if (bitmap != null) {
                    drawImage(
                        image = bitmap,
                        topLeft = Offset(liftedX - liftedSize / 2, liftedY - liftedSize / 2),
                        alpha = 0.85f,
                    )
                }
                drawCircle(
                    color = DragGhost,
                    radius = liftedSize * 0.55f,
                    center = Offset(liftedX, liftedY),
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
            // Rare: blue soft glow circle — PERF: layered ovals instead of gradient
            val glowAlpha = 0.2f + sin(t * 2f + unitDefId * 0.5f) * 0.05f
            drawOval(
                color = PlatformRareGlow.copy(alpha = glowAlpha),
                topLeft = Offset(screenX - pedestalRx * 1.1f, screenY - pedestalRy * 0.4f),
                size = Size(pedestalRx * 2.2f, pedestalRy * 2.2f),
            )
            drawOval(
                color = PlatformRareColor.copy(alpha = glowAlpha + 0.1f),
                topLeft = Offset(screenX - pedestalRx * 0.8f, screenY - pedestalRy * 0.2f),
                size = Size(pedestalRx * 1.6f, pedestalRy * 1.6f),
            )
        }
        2 -> {
            // Hero: purple glow with subtle orbiting particles — PERF: layered ovals
            val glowAlpha = 0.25f + sin(t * 2.5f + unitDefId * 0.4f) * 0.08f
            drawOval(
                color = PlatformHeroGlow.copy(alpha = glowAlpha),
                topLeft = Offset(screenX - pedestalRx * 1.15f, screenY - pedestalRy * 0.45f),
                size = Size(pedestalRx * 2.3f, pedestalRy * 2.3f),
            )
            drawOval(
                color = PlatformHeroColor.copy(alpha = glowAlpha + 0.05f),
                topLeft = Offset(screenX - pedestalRx * 0.85f, screenY - pedestalRy * 0.25f),
                size = Size(pedestalRx * 1.7f, pedestalRy * 1.7f),
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
            // Legend: gold flaming circle — PERF: layered ovals
            val flameAlpha = 0.3f + sin(t * 4f + unitDefId * 0.6f) * 0.12f
            drawOval(
                color = PlatformLegendColor.copy(alpha = flameAlpha * 0.6f),
                topLeft = Offset(screenX - pedestalRx * 1.2f, screenY - pedestalRy * 0.5f),
                size = Size(pedestalRx * 2.4f, pedestalRy * 2.4f),
            )
            drawOval(
                color = PlatformLegendFlame.copy(alpha = flameAlpha + 0.05f),
                topLeft = Offset(screenX - pedestalRx * 0.85f, screenY - pedestalRy * 0.25f),
                size = Size(pedestalRx * 1.7f, pedestalRy * 1.7f),
            )
            drawOval(
                color = PlatformLegendColor.copy(alpha = flameAlpha + 0.1f),
                topLeft = Offset(screenX - pedestalRx * 1.0f, screenY - pedestalRy * 0.3f),
                size = Size(pedestalRx * 2.0f, pedestalRy * 1.8f),
                style = Stroke(width = 1.5f),
            )
        }
        4 -> {
            // Ancient: red fire ring — PERF: layered ovals
            val pulseScale = 1f + sin(t * 3f + unitDefId * 0.4f) * 0.08f
            val ringAlpha = 0.35f + sin(t * 5f + unitDefId * 0.3f) * 0.15f
            val rx = pedestalRx * 1.1f * pulseScale
            val ry = pedestalRy * 1.1f * pulseScale
            drawOval(
                color = PlatformAncientColor.copy(alpha = ringAlpha * 0.5f),
                topLeft = Offset(screenX - rx, screenY - ry * 0.4f),
                size = Size(rx * 2f, ry * 2f),
            )
            drawOval(
                color = PlatformAncientRing.copy(alpha = ringAlpha * 0.7f),
                topLeft = Offset(screenX - rx * 0.7f, screenY - ry * 0.2f),
                size = Size(rx * 1.4f, ry * 1.4f),
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
            // PERF: layered ovals instead of gradient
            drawOval(
                color = PlatformMythicGold.copy(alpha = mythicAlpha * 0.4f),
                topLeft = Offset(screenX - pedestalRx * 1.3f, screenY - pedestalRy * 0.6f),
                size = Size(pedestalRx * 2.6f, pedestalRy * 2.6f),
            )
            drawOval(
                color = PlatformMythicGold.copy(alpha = mythicAlpha + 0.05f),
                topLeft = Offset(screenX - pedestalRx * 0.9f, screenY - pedestalRy * 0.3f),
                size = Size(pedestalRx * 1.8f, pedestalRy * 1.8f),
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
            // PERF: layered ovals instead of gradient
            drawOval(
                color = PlatformImmortalPink.copy(alpha = divineAlpha * 0.3f),
                topLeft = Offset(screenX - rx, screenY - ry * 0.5f),
                size = Size(rx * 2f, ry * 2f),
            )
            drawOval(
                color = PlatformImmortalWhite.copy(alpha = divineAlpha * 0.4f),
                topLeft = Offset(screenX - rx * 0.7f, screenY - ry * 0.3f),
                size = Size(rx * 1.4f, ry * 1.4f),
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
