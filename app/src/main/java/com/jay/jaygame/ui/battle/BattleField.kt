@file:Suppress("DEPRECATION")
package com.jay.jaygame.ui.battle

import com.jay.jaygame.ui.components.blueprintIconRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.jay.jaygame.R
import com.jay.jaygame.bridge.BattleBridge
import com.jay.jaygame.data.STAGES
import com.jay.jaygame.data.UnitFamily
import com.jay.jaygame.engine.Grid
import com.jay.jaygame.ui.theme.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Pre-allocated constants
// PERF: Pre-allocated scratch array for buff icon rendering (max 4 buff types, avoids per-frame list allocation)
private val buffScratch = IntArray(4)

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

// ── Skill/Crit animation constants (pre-allocated, GC-free) ──
private val SkillGoldTint = ColorFilter.tint(
    Color(0xFFFFDD66), BlendMode.Modulate
)
private val CritGoldFlash = ColorFilter.tint(
    Color(0xFFFFEE88), BlendMode.Modulate
)
private val SkillGlowColor = Color(0xFFFFD700).copy(alpha = 0.3f)

// GameUnit.Companion에서 정의된 상수 참조
private val SKILL_ANIM_DUR get() = com.jay.jaygame.engine.GameUnit.SKILL_ANIM_DURATION
private val CRIT_ANIM_DUR get() = com.jay.jaygame.engine.GameUnit.CRIT_ANIM_DURATION

// Star badge colors
private val StarColor = Color(0xFFFFD700)
private val StarBgGlow = Color(0xFF1A1A2E).copy(alpha = 0.9f)

// Vignette
private val VignetteColor = Color.Black.copy(alpha = 0.4f)

// E7: Grade 4+ aura ring colors (pre-allocated)
private val MythicAuraGold = Color(0xFFFFD700)        // grade 4 (Mythic)
private val MythicAuraStroke = Stroke(width = 3f)

// Grid cell glow
private val CellHighlight = Color.White.copy(alpha = 0.03f)
private val CellHighlightBright = Color.White.copy(alpha = 0.06f)

// Move mode highlight colors (pre-allocated)
private val MoveHighlightFill = Color(0xFFFFD700).copy(alpha = 0.25f)
private val MoveHighlightBorder = Color(0xFFFFD700).copy(alpha = 0.6f)
private val MoveHighlightStroke = Stroke(width = 2f)
private val MoveSourceFill = Color(0xFF00D4FF).copy(alpha = 0.3f)
private val MoveSourceBorder = Color(0xFF00D4FF).copy(alpha = 0.7f)
private val MoveRangeCircleColor = Color(0xFF00D4FF).copy(alpha = 0.2f)
private val MoveRangeCircleBorder = Color(0xFF00D4FF).copy(alpha = 0.5f)

// Pre-allocated strokes for per-unit draw loop (avoid GC)
private val ThinStroke1f = Stroke(width = 1f)
private val ThinStroke1_5f = Stroke(width = 1.5f)

private val GradeColors = GradeColorsByIndex
private val FamilyAuraColors = FamilyColorsByIndex

// ── G1: Grade-based platform colors (pre-allocated) ──
private val PlatformCommonColor = Color(0xFF9E9E9E)
private val PlatformCommonInner = Color(0xFFBDBDBD)
private val PlatformRareColor = Color(0xFF42A5F5)
private val PlatformRareGlow = Color(0xFF42A5F5).copy(alpha = 0.3f)
private val PlatformRareBeam = Color(0xFF90CAF9)
private val PlatformRareRing = Color(0xFF1E88E5)
private val PlatformHeroColor = Color(0xFFAB47BC)
private val PlatformHeroGlow = Color(0xFFAB47BC).copy(alpha = 0.35f)
private val PlatformHeroParticle = Color(0xFFCE93D8)
private val PlatformHeroRing = Color(0xFF8E24AA)
private val PlatformHeroCore = Color(0xFFE1BEE7)
private val PlatformLegendColor = Color(0xFFFF8F00)
private val PlatformLegendFlame = Color(0xFFFFD54F)
private val PlatformLegendRing = Color(0xFFFF6D00)
private val PlatformMythicGold = Color(0xFFFBBF24)
private val PlatformMythicCore = Color(0xFFFFFFE0)
private val EffectWhite = Color(0xFFFFFFFF) // 공용 백색 (스파크, 별빛, 하이라이트)

// ── SSJ Aura colors (등급별 오라 — Platform과 동일한 색은 재사용) ──
private val AuraRareOuter = Color(0xFF64B5F6)
private val AuraRareInner = Color(0xFFBBDEFB)
private val AuraLegendInner = Color(0xFFFFE082)
private val AuraMythicElectric = Color(0xFF40C4FF)
private const val TWO_PI = (2.0 * Math.PI).toFloat()

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

// Grid area normalized — derived from Grid.kt constants
private val GRID_NORM_X = Grid.ORIGIN_X / Grid.CANVAS_W
private val GRID_NORM_Y = Grid.ORIGIN_Y / Grid.CANVAS_H
private val GRID_NORM_W = Grid.GRID_W / Grid.CANVAS_W
private val GRID_NORM_H = Grid.GRID_H / Grid.CANVAS_H


/**
 * Battlefield overlay — unit sprites rendered in Compose (proper drawable icons),
 * aura/particle effects rendered by C++ engine behind this layer.
 */
@Composable
fun BattleField() {
    val unitPositions by BattleBridge.unitPositions.collectAsState()
    val battleSpeed by BattleBridge.battleSpeed.collectAsState()
    val selectedTile by BattleBridge.selectedTile.collectAsState()
    val moveModeTile by BattleBridge.moveModeTile.collectAsState()
    val validMoveTargets by BattleBridge.validMoveTargets.collectAsState()
    val context = LocalContext.current

    // Blueprint bitmaps — lazy 로딩 캐시 (배틀에 등장하는 유닛만 디코딩, ~107개 중 10~20개만 사용)
    val blueprintBitmapCache = remember { mutableMapOf<String, ImageBitmap?>() }
    // Resolve bitmap lazily — decodes only on first access per blueprintId
    val resolveBlueprintBitmap: (String) -> ImageBitmap? = remember(context) {
        { bpId: String ->
            blueprintBitmapCache.getOrPut(bpId) {
                if (!com.jay.jaygame.engine.BlueprintRegistry.isReady) null
                else {
                    val bp = com.jay.jaygame.engine.BlueprintRegistry.instance.findById(bpId)
                    if (bp != null) {
                        val resId = blueprintIconRes(bp)
                        ContextCompat.getDrawable(context, resId)?.toBitmap(128, 128)?.asImageBitmap()
                    } else null
                }
            }
        }
    }

    // 유닛 버프 스프라이트 (공격력/속도/방어/실드)
    val buffSprites = remember {
        mapOf(
            com.jay.jaygame.bridge.UNIT_BUFF_ATK_UP to decodeAssetBitmap(context, "fx/fx_buff_attack.png"),
            com.jay.jaygame.bridge.UNIT_BUFF_SPD_UP to decodeAssetBitmap(context, "fx/fx_buff_speed.png"),
            com.jay.jaygame.bridge.UNIT_BUFF_DEF_UP to decodeAssetBitmap(context, "fx/fx_buff_defense.png"),
            com.jay.jaygame.bridge.UNIT_BUFF_SHIELD to decodeAssetBitmap(context, "fx/fx_buff_shield.png"),
        )
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
                        0
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

    /** 터치 좌표에서 가장 가까운 유닛의 tileIndex 반환 (-1 = 없음) */
    fun findClosestUnit(offset: Offset, w: Float, h: Float): Int {
        val normX = offset.x / w
        val normY = offset.y / h
        val data = BattleBridge.unitPositions.value
        var closestIdx = -1
        var closestDistSq = Float.MAX_VALUE
        val threshold = 0.06f
        for (i in 0 until data.count) {
            val dx = normX - data.xs[i]
            val dy = normY - data.ys[i]
            val distSq = dx * dx + dy * dy
            if (distSq < threshold * threshold && distSq < closestDistSq) {
                closestDistSq = distSq
                closestIdx = i
            }
        }
        return if (closestIdx >= 0) data.tileIndices[closestIdx] else -1
    }

    /** 화면 좌표에서 그리드 슬롯 인덱스 반환 (-1 = 그리드 밖) */
    fun findGridSlot(offset: Offset, w: Float, h: Float): Int {
        val normX = offset.x / w
        val normY = offset.y / h
        val col = ((normX - GRID_NORM_X) / (GRID_NORM_W / Grid.COLS)).toInt()
        val row = ((normY - GRID_NORM_Y) / (GRID_NORM_H / Grid.ROWS)).toInt()
        if (col !in 0 until Grid.COLS || row !in 0 until Grid.ROWS) return -1
        return row * Grid.COLS + col
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            // 탭: 이동 모드, 롱프레스: 유닛 상세 팝업
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        if (BattleBridge.moveModeTile.value >= 0) {
                            val tappedSlot = findGridSlot(offset, w, h)
                            val targets = BattleBridge.validMoveTargets.value
                            if (tappedSlot >= 0 && tappedSlot < targets.size && targets[tappedSlot]) {
                                BattleBridge.executeMoveMode(tappedSlot)
                            } else {
                                BattleBridge.exitMoveMode()
                            }
                        } else {
                            val tile = findClosestUnit(offset, w, h)
                            if (tile >= 0) {
                                BattleBridge.enterMoveMode(tile)
                            }
                        }
                    },
                    onLongPress = { offset ->
                        val tile = findClosestUnit(offset, size.width.toFloat(), size.height.toFloat())
                        if (tile >= 0) {
                            BattleBridge.exitMoveMode()
                            BattleBridge.showUnitPopup(tile)
                        }
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

        // ── Move mode: highlight valid target slots ──
        if (moveModeTile >= 0 && validMoveTargets.isNotEmpty()) {
            val slotCellW = gridW / Grid.COLS
            val slotCellH = gridH / Grid.ROWS

            fun highlightSlot(slot: Int, fill: Color, border: Color) {
                val col = slot % Grid.COLS
                val row = slot / Grid.COLS
                if (col !in 0 until Grid.COLS || row !in 0 until Grid.ROWS) return
                val left = gridLeft + col * slotCellW + 2f
                val top = gridTop + row * slotCellH + 2f
                val sz = Size(slotCellW - 4f, slotCellH - 4f)
                drawRect(color = fill, topLeft = Offset(left, top), size = sz)
                drawRect(color = border, topLeft = Offset(left, top), size = sz, style = MoveHighlightStroke)
            }

            highlightSlot(moveModeTile, MoveSourceFill, MoveSourceBorder)
            for (slot in validMoveTargets.indices) {
                if (validMoveTargets[slot]) highlightSlot(slot, MoveHighlightFill, MoveHighlightBorder)
            }

        }

        // ── Draw unit sprites (Compose drawable icons) ──
        val data = unitPositions
        val sxArr = smoothXs.value
        val syArr = smoothYs.value
        val useSmooth = sxArr.size == data.count && data.count > 0
        val moArr = microOffsets.value
        val useMicro = moArr.size == data.count * 2

        val cellW = gridW / Grid.COLS
        val cellH = gridH / Grid.ROWS
        val unitSizeNormal = minOf(cellW, cellH) * 1.1f
        val gridState = BattleBridge.gridState.value
        // PERF: Reduce visual detail when many units exist
        val fastBattleMode = battleSpeed >= 6f
        val highUnitCount = data.count > 50 || fastBattleMode

        for (i in 0 until data.count) {
            if (i >= data.xs.size || i >= data.ys.size || i >= data.grades.size ||
                i >= data.tileIndices.size || i >= data.levels.size) continue
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
            val unitSize = unitSizeNormal
            val pedestalRx = unitSize * 0.45f
            val pedestalRy = pedestalRx * 0.4f
            val isSelected = selectedTile == tileIdx
            val family = if (i < data.familiesList.size && data.familiesList[i].isNotEmpty()) {
                data.familiesList[i].first().ordinal
            } else {
                0
            }
            val animSeed = if (i < data.blueprintIds.size) data.blueprintIds[i].hashCode() else i
            val level = data.levels[i]

            // ── G3: Breathing scale (slow sine 0.97-1.03, skip for low grade in high-count) ──
            val breathScale = if (skipMicro) 1f else 1f + sin(t * 1.8f + animSeed * 0.3f) * 0.03f

            // ── Attack anim timer (ATTACK_ANIM_DURATION→0 감소, 공격 발사 시 리셋) ──
            val attackAnimTimer = if (i < data.attackAnimTimers.size) data.attackAnimTimers[i] else 0f
            val bouncePhase = (attackAnimTimer / com.jay.jaygame.engine.GameUnit.ATTACK_ANIM_DURATION).coerceIn(0f, 1f)
            val kick = if (bouncePhase > 0f) sin(bouncePhase * 3.1416f).toFloat() else 0f

            // ── G2: One-shot attack motion (공격 순간 1회만) ──
            var attackOffsetX = 0f
            var attackOffsetY = 0f
            var attackRotation = 0f
            val attackScale: Float
            if (kick > 0f) {
                attackScale = when (family) {
                    0 -> { // Fire: thrust forward
                        attackOffsetY = -kick * 6f
                        1f + kick * 0.1f
                    }
                    1 -> { // Frost: pulse/expand
                        1f + kick * 0.2f
                    }
                    2 -> { // Poison: lean forward
                        attackOffsetY = -kick * 5f
                        attackOffsetX = kick * 2f
                        1f + kick * 0.08f
                    }
                    3 -> { // Lightning: quick jitter
                        attackOffsetX = kick * 3f
                        attackOffsetY = -kick * 2f
                        1f + kick * 0.12f
                    }
                    4 -> { // Support: raise up
                        attackOffsetY = -kick * 8f
                        1f + kick * 0.1f
                    }
                    5 -> { // Wind: spin
                        attackRotation = kick * 0.3f
                        attackOffsetX = kick * 4f
                        1f + kick * 0.1f
                    }
                    else -> 1f + kick * 0.15f
                }
            } else {
                attackScale = 1f
            }

            val finalScale = breathScale * attackScale
            val screenX = screenXBase + attackOffsetX
            val screenY = screenYBase + attackOffsetY

            // ── E7: Grade 4 (Mythic) aura ring ──
            if (grade >= 4 && !fastBattleMode) {
                val auraColor = MythicAuraGold
                val auraStroke = MythicAuraStroke
                val auraPulseAlpha = (0.35f + sin(t * 3f + animSeed * 0.4f) * 0.2f).coerceIn(0.15f, 0.55f)
                val auraRadius = unitSize * (0.55f + sin(t * 2f + animSeed * 0.3f) * 0.05f)

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
            }

            // ── Aura/Shield range circle (Support family=4, or high-grade aura units) ──
            if (!fastBattleMode && family == 4 && grade >= 2 && (!highUnitCount || grade >= 3)) {
                val auraRadiusScreen = (45f / Grid.CANVAS_W) * w
                val auraPulse = 0.08f + sin(t * 2f + animSeed * 0.5f) * 0.04f
                // Layered circles instead of Brush.radialGradient to avoid per-frame allocation
                drawCircle(
                    color = FamilyAuraColors[4].copy(alpha = auraPulse * 0.5f),
                    radius = auraRadiusScreen * 0.5f,
                    center = Offset(screenX, screenY),
                )
                drawCircle(
                    color = FamilyAuraColors[4].copy(alpha = auraPulse * 0.3f),
                    radius = auraRadiusScreen * 0.8f,
                    center = Offset(screenX, screenY),
                )
                drawCircle(
                    color = FamilyAuraColors[4].copy(alpha = auraPulse * 1.5f),
                    radius = auraRadiusScreen,
                    center = Offset(screenX, screenY),
                    style = ThinStroke1f,
                )
            }

            // ── Selected unit: range circle ──
            if (isSelected) {
                val rangeScreen = (200f / Grid.CANVAS_W) * w
                val rangePulse = 0.15f + sin(t * 3f) * 0.05f
                drawCircle(
                    color = gradeColor.copy(alpha = rangePulse),
                    radius = rangeScreen,
                    center = Offset(screenX, screenY),
                    style = ThinStroke1_5f,
                )
            }

            // ── Attack Bounce (one-shot, 공격 시에만 위로 튕김) ──
            val bounceOffset = kick * -6f

            // ── G1: Grade-based platform effect ──
            drawGradePlatform(
                grade = grade,
                screenX = screenX,
                screenY = screenY,
                pedestalRx = pedestalRx,
                pedestalRy = pedestalRy,
                t = t,
                unitDefId = animSeed,
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

            // ── 유닛 버프 아이콘 (발밑 장판 위에 가로 나열, GC-free) ──
            val unitBuffBits = if (i < data.buffs.size) data.buffs[i] else 0
            if (unitBuffBits != 0) {
                var buffCount = 0
                if (unitBuffBits and com.jay.jaygame.bridge.UNIT_BUFF_ATK_UP != 0) { buffScratch[buffCount++] = com.jay.jaygame.bridge.UNIT_BUFF_ATK_UP }
                if (unitBuffBits and com.jay.jaygame.bridge.UNIT_BUFF_SPD_UP != 0) { buffScratch[buffCount++] = com.jay.jaygame.bridge.UNIT_BUFF_SPD_UP }
                if (unitBuffBits and com.jay.jaygame.bridge.UNIT_BUFF_DEF_UP != 0) { buffScratch[buffCount++] = com.jay.jaygame.bridge.UNIT_BUFF_DEF_UP }
                if (unitBuffBits and com.jay.jaygame.bridge.UNIT_BUFF_SHIELD != 0) { buffScratch[buffCount++] = com.jay.jaygame.bridge.UNIT_BUFF_SHIELD }
                // 페데스탈 폭에 맞춰 아이콘 크기 결정
                val buffIconSize = ((pedestalRx * 2f) / buffCount.coerceAtLeast(1)).toInt().coerceIn(8, (pedestalRx * 0.7f).toInt().coerceAtLeast(8))
                val totalWidth = buffCount * buffIconSize
                val startX = screenX - totalWidth / 2f
                val buffY = screenY + pedestalRy * 0.3f  // 페데스탈 중심 약간 아래
                for (bi in 0 until buffCount) {
                    val bmp = buffSprites[buffScratch[bi]] ?: continue
                    val bx = startX + bi * buffIconSize
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(bx.toInt(), (buffY - buffIconSize / 2f).toInt()),
                        dstSize = IntSize(buffIconSize, buffIconSize),
                        alpha = 0.7f,
                    )
                }
            }

            // ── Family-specific idle ambient effects (grade 1+, skip common in high-count) ──
            if (grade >= 1 && !skipMicro && !fastBattleMode) {
                when (family) {
                    0 -> { // Fire: rising ember particles
                        val emberCount = if (highUnitCount) 2 else (1 + grade).coerceAtMost(4)
                        for (e in 0 until emberCount) {
                            val emberPhase = (t * 0.8f + e * 0.5f + animSeed * 0.17f) % 1f
                            val emberX = screenX + sin(t * 1.5f + e * 2.3f + animSeed * 0.3f) * unitSize * 0.2f
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
                            val snowPhase = (t * 0.5f + s * 0.6f + animSeed * 0.13f) % 1f
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
                            val dripPhase = (t * 0.6f + d * 0.7f + animSeed * 0.19f) % 1f
                            val dripX = screenX + sin(d * 2.7f + animSeed * 0.2f) * unitSize * 0.15f
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
                        val sparkPhase = (t * 2f + animSeed * 0.31f) % 1f
                        if (sparkPhase < 0.15f) { // Brief flicker
                            val sparkAlpha = (0.15f - sparkPhase) / 0.15f * 0.5f
                            val sparkAngle = sin(t * 7f + animSeed * 0.5f) * PI.toFloat()
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
                        val haloPulse = 0.12f + sin(t * 1.5f + animSeed * 0.4f) * 0.06f
                        drawCircle(
                            color = SupportGlowColor.copy(alpha = haloPulse),
                            radius = unitSize * 0.4f,
                            center = Offset(screenX, screenY - unitSize * 0.35f),
                        )
                    }
                    5 -> { // Wind: gentle breeze drift (frost 스타일 + 바람 색상)
                        val breezeCount = if (highUnitCount) 1 else (1 + grade / 2).coerceAtMost(3)
                        for (s in 0 until breezeCount) {
                            val breezePhase = (t * 0.5f + s * 0.6f + animSeed * 0.13f) % 1f
                            val drift = sin(t * 1.2f + s * 1.7f) * unitSize * 0.15f
                            val breezeX = screenX + drift
                            val breezeY = screenY - unitSize * 0.5f + breezePhase * unitSize * 0.3f
                            val breezeAlpha = sin(breezePhase * PI.toFloat()) * 0.35f
                            drawCircle(
                                color = WindLeafColor.copy(alpha = breezeAlpha),
                                radius = 1.5f,
                                center = Offset(breezeX, breezeY),
                            )
                        }
                    }
                }
            }

            // Unit sprite + SSJ aura + skill/crit animations
            val bitmap = if (i < data.blueprintIds.size) resolveBlueprintBitmap(data.blueprintIds[i]) else null
            if (bitmap != null) {
                // ── Skill/Crit animation timers ──
                val skillTimer = if (i < data.skillAnimTimers.size) data.skillAnimTimers[i] else 0f
                val critTimer = if (i < data.critAnimTimers.size) data.critAnimTimers[i] else 0f

                // Skill scale pulse: 1.0 → 1.3 → 1.0 (sin curve)
                val skillPhase = if (skillTimer > 0f) (skillTimer / SKILL_ANIM_DUR).coerceIn(0f, 1f) else 0f
                val skillScaleBoost = if (skillPhase > 0f) sin(skillPhase * PI.toFloat()) * 0.3f else 0f
                val skillYOffset = if (skillPhase > 0f) sin(skillPhase * PI.toFloat()) * -10f else 0f

                // Crit scale punch: 1.0 → 1.25 → 1.0 (fast)
                val critPhase = if (critTimer > 0f) (critTimer / CRIT_ANIM_DUR).coerceIn(0f, 1f) else 0f
                val critScaleBoost = if (critPhase > 0f) sin(critPhase * PI.toFloat()) * 0.25f else 0f
                val critShakeX = if (critPhase > 0f) sin(critPhase * 40f) * 3f else 0f

                // Combined scale
                val animScale = 1f + skillScaleBoost + critScaleBoost
                val spriteSize = unitSize * 0.85f * finalScale * animScale
                val spriteY = screenY - spriteSize * 0.85f + bounceOffset + skillYOffset
                val spriteCenterY = spriteY + spriteSize * 0.5f

                // ── SSJ Aura: 등급별 타오르는 오라 (스프라이트 뒤에 그림) ──
                // SSJ Aura 비활성화

                // Skill glow circle (behind sprite)
                if (skillPhase > 0f) {
                    val glowAlpha = sin(skillPhase * PI.toFloat()) * 0.4f
                    drawCircle(
                        color = SkillGlowColor.copy(alpha = glowAlpha),
                        radius = spriteSize * 0.8f,
                        center = Offset(screenX, spriteCenterY),
                    )
                }

                // ColorFilter: skill > crit > none (Modulate 블렌딩으로 원본 색상 유지)
                val spriteColorFilter: ColorFilter? = when {
                    skillPhase > 0f -> SkillGoldTint
                    critPhase > 0f -> CritGoldFlash
                    else -> null
                }

                // Apply rotation (attack + crit shake) + draw
                val totalRotation = attackRotation * (180f / PI.toFloat()) + // radians to degrees
                    if (critPhase > 0f) sin(critPhase * 30f) * 2f else 0f
                val spritePivot = Offset(screenX + critShakeX, spriteCenterY)
                val dstOff = IntOffset(
                    (screenX - spriteSize / 2 + critShakeX).toInt(),
                    spriteY.toInt(),
                )
                val dstSz = IntSize(spriteSize.toInt(), spriteSize.toInt())

                if (totalRotation != 0f) {
                    rotate(degrees = totalRotation, pivot = spritePivot) {
                        drawImage(
                            image = bitmap,
                            dstOffset = dstOff,
                            dstSize = dstSz,
                            colorFilter = spriteColorFilter,
                        )
                    }
                } else {
                    drawImage(
                        image = bitmap,
                        dstOffset = dstOff,
                        dstSize = dstSz,
                        colorFilter = spriteColorFilter,
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
            val unitRole = data.roles.getOrElse(i) { com.jay.jaygame.engine.UnitRole.RANGED_DPS }
            val unitState = data.states.getOrElse(i) { com.jay.jaygame.engine.UnitState.IDLE }
            val unitHp = data.hps.getOrElse(i) { 0f }
            val unitMaxHp = data.maxHps.getOrElse(i) { 0f }
            val unitCategory = data.unitCategories.getOrElse(i) { com.jay.jaygame.engine.UnitCategory.NORMAL }

            if (unitRole == com.jay.jaygame.engine.UnitRole.TANK && unitState == com.jay.jaygame.engine.UnitState.BLOCKING && unitMaxHp > 0f) {
                val hpPercent = (unitHp / unitMaxHp).coerceIn(0f, 1f)
                val barWidth = 40f
                val barHeight = 4f
                val barX = screenX - barWidth / 2
                val barY = screenY - unitSize * 0.9f
                drawRect(TankHpBg, Offset(barX, barY), Size(barWidth, barHeight))
                drawRect(TankHpBar, Offset(barX, barY), Size(barWidth * hpPercent, barHeight))
            }

            // ── Task 18: Dash trail (activated) ──
            if (unitState == com.jay.jaygame.engine.UnitState.DASHING) {
                val homeScreenX = data.homeXs.getOrElse(i) { data.xs[i] } * w
                val homeScreenY = data.homeYs.getOrElse(i) { data.ys[i] } * h
                drawLine(
                    color = DashTrailWhite03,
                    start = Offset(homeScreenX, homeScreenY),
                    end = Offset(screenX, screenY),
                    strokeWidth = 2f,
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

        // ── 공격 범위 원: 유닛 스프라이트 위에 그려야 가려지지 않음 ──
        if (moveModeTile >= 0) {
            val uData = unitPositions
            for (i in 0 until uData.count) {
                if (uData.tileIndices[i] == moveModeTile && i < uData.ranges.size) {
                    val unitRange = uData.ranges[i]
                    if (unitRange > 0f) {
                        val rangeScreen = (unitRange / Grid.CANVAS_W) * w
                        val cx = uData.xs[i] * w
                        val cy = uData.ys[i] * h
                        drawCircle(
                            color = MoveRangeCircleColor,
                            radius = rangeScreen,
                            center = Offset(cx, cy),
                        )
                        drawCircle(
                            color = MoveRangeCircleBorder,
                            radius = rangeScreen,
                            center = Offset(cx, cy),
                            style = MoveHighlightStroke,
                        )
                    }
                    break
                }
            }
        }

        // ── G5: Draw death/dissolution effects ──
        val unitSizeDefault = unitSizeNormal
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
                    radius = unitSizeDefault * 0.4f * (1f + progress * 0.5f),
                    center = Offset(ex, ey),
                )
            } else if (effect.grade <= 2) {
                // Rare/Hero: particles scatter outward in family color
                val particleCount = 8 + effect.grade * 4
                for (p in 0 until particleCount) {
                    val angle = (p.toFloat() / particleCount) * TWO_PI
                    val dist = progress * unitSizeDefault * 0.8f
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
                val burstRadius = progress * unitSizeDefault * 1.2f
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
                    val angle = (p.toFloat() / particleCount) * TWO_PI + progress * 2f
                    val dist = progress * unitSizeDefault * (0.6f + sin(p * 1.3f) * 0.3f)
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
            // Common: 은은한 호흡 애니메이션 + 내부 밝은 코어
            val breathAlpha = 0.2f + sin(t * 1.5f + unitDefId * 0.3f) * 0.06f
            drawOval(
                color = PlatformCommonColor.copy(alpha = breathAlpha),
                topLeft = Offset(screenX - pedestalRx * 1.0f, screenY - pedestalRy * 0.25f),
                size = Size(pedestalRx * 2.0f, pedestalRy * 2.0f),
            )
            drawOval(
                color = PlatformCommonInner.copy(alpha = breathAlpha + 0.08f),
                topLeft = Offset(screenX - pedestalRx * 0.6f, screenY - pedestalRy * 0.05f),
                size = Size(pedestalRx * 1.2f, pedestalRy * 1.2f),
            )
        }
        1 -> {
            // Rare: 파란 이중 글로우 + 회전 빛줄기 2개 + 내부 링
            val glowAlpha = 0.22f + sin(t * 2f + unitDefId * 0.5f) * 0.06f
            drawOval(
                color = PlatformRareGlow.copy(alpha = glowAlpha),
                topLeft = Offset(screenX - pedestalRx * 1.15f, screenY - pedestalRy * 0.45f),
                size = Size(pedestalRx * 2.3f, pedestalRy * 2.3f),
            )
            drawOval(
                color = PlatformRareColor.copy(alpha = glowAlpha + 0.12f),
                topLeft = Offset(screenX - pedestalRx * 0.8f, screenY - pedestalRy * 0.2f),
                size = Size(pedestalRx * 1.6f, pedestalRy * 1.6f),
            )
            // 회전 빛줄기
            for (p in 0 until 2) {
                val angle = t * 1.8f + p * PI.toFloat()
                val bx = screenX + cos(angle) * pedestalRx * 0.85f
                val by = screenY + sin(angle) * pedestalRy * 0.55f
                val bAlpha = 0.35f + sin(t * 3f + p * 2f) * 0.15f
                drawCircle(
                    color = PlatformRareBeam.copy(alpha = bAlpha),
                    radius = 2f,
                    center = Offset(bx, by),
                )
            }
            // 내부 링
            drawOval(
                color = PlatformRareRing.copy(alpha = glowAlpha * 0.5f),
                topLeft = Offset(screenX - pedestalRx * 0.95f, screenY - pedestalRy * 0.3f),
                size = Size(pedestalRx * 1.9f, pedestalRy * 1.7f),
                style = ThinStroke1_5f,
            )
        }
        2 -> {
            // Hero: 보라 에너지 글로우 + 펄싱 코어 + 8개 궤도 파티클 + 외곽 에너지 링
            val glowAlpha = 0.28f + sin(t * 2.5f + unitDefId * 0.4f) * 0.1f
            drawOval(
                color = PlatformHeroGlow.copy(alpha = glowAlpha),
                topLeft = Offset(screenX - pedestalRx * 1.2f, screenY - pedestalRy * 0.5f),
                size = Size(pedestalRx * 2.4f, pedestalRy * 2.4f),
            )
            // 펄싱 코어
            val coreAlpha = 0.2f + sin(t * 4f) * 0.1f
            drawOval(
                color = PlatformHeroCore.copy(alpha = coreAlpha),
                topLeft = Offset(screenX - pedestalRx * 0.5f, screenY - pedestalRy * 0.05f),
                size = Size(pedestalRx * 1.0f, pedestalRy * 1.0f),
            )
            drawOval(
                color = PlatformHeroColor.copy(alpha = glowAlpha + 0.08f),
                topLeft = Offset(screenX - pedestalRx * 0.85f, screenY - pedestalRy * 0.25f),
                size = Size(pedestalRx * 1.7f, pedestalRy * 1.7f),
            )
            // 궤도 파티클 8개
            for (p in 0 until 8) {
                val angle = t * 1.5f + p * (PI.toFloat() / 4f)
                val orbitR = pedestalRx * (0.65f + sin(t * 2f + p * 0.8f) * 0.1f)
                val px = screenX + cos(angle) * orbitR
                val py = screenY + sin(angle) * orbitR * 0.6f
                val pAlpha = 0.35f + sin(t * 3.5f + p * 1.2f) * 0.2f
                drawCircle(
                    color = PlatformHeroParticle.copy(alpha = pAlpha),
                    radius = 1.8f,
                    center = Offset(px, py),
                )
            }
            // 외곽 에너지 링
            drawOval(
                color = PlatformHeroRing.copy(alpha = glowAlpha * 0.6f),
                topLeft = Offset(screenX - pedestalRx * 1.1f, screenY - pedestalRy * 0.4f),
                size = Size(pedestalRx * 2.2f, pedestalRy * 2.0f),
                style = ThinStroke1_5f,
            )
        }
        3 -> {
            // Legend: 이중 불꽃 오라 + 불꽃 링 + 스파크 파티클 6개 + 내부 골드 코어
            val flameAlpha = 0.35f + sin(t * 4f + unitDefId * 0.6f) * 0.14f
            // 외곽 불꽃
            drawOval(
                color = PlatformLegendColor.copy(alpha = flameAlpha * 0.5f),
                topLeft = Offset(screenX - pedestalRx * 1.3f, screenY - pedestalRy * 0.55f),
                size = Size(pedestalRx * 2.6f, pedestalRy * 2.6f),
            )
            // 내부 불꽃
            drawOval(
                color = PlatformLegendFlame.copy(alpha = flameAlpha + 0.06f),
                topLeft = Offset(screenX - pedestalRx * 0.85f, screenY - pedestalRy * 0.25f),
                size = Size(pedestalRx * 1.7f, pedestalRy * 1.7f),
            )
            // 불꽃 외곽 링
            drawOval(
                color = PlatformLegendRing.copy(alpha = flameAlpha + 0.08f),
                topLeft = Offset(screenX - pedestalRx * 1.1f, screenY - pedestalRy * 0.35f),
                size = Size(pedestalRx * 2.2f, pedestalRy * 2.0f),
                style = GroundBorderStroke,
            )
            // 내부 골드 코어
            val coreAlpha = 0.15f + sin(t * 5f) * 0.1f
            drawOval(
                color = PlatformLegendFlame.copy(alpha = coreAlpha),
                topLeft = Offset(screenX - pedestalRx * 0.45f, screenY + pedestalRy * 0.0f),
                size = Size(pedestalRx * 0.9f, pedestalRy * 0.9f),
            )
            // 스파크 파티클 6개 — 불꽃 주변에서 튀는 불씨
            for (p in 0 until 6) {
                val angle = t * 2.5f + p * (PI.toFloat() / 3f)
                val sparkR = pedestalRx * (0.75f + sin(t * 4f + p * 1.5f) * 0.2f)
                val px = screenX + cos(angle) * sparkR
                val py = screenY + sin(angle) * sparkR * 0.55f
                val sAlpha = 0.5f + sin(t * 6f + p * 2f) * 0.3f
                drawCircle(
                    color = EffectWhite.copy(alpha = sAlpha),
                    radius = 1.2f,
                    center = Offset(px, py),
                )
            }
        }
        4 -> {
            // Mythic: 레인보우 쉬머 + 골드 코어 펄스 + 이중 홀로 링 + 별빛 파티클 8개
            val shimmerPhase = t * 2f + unitDefId * 0.5f
            val shimmerIdx = ((shimmerPhase % (TWO_PI)) / (TWO_PI) * RainbowShimmer.size).toInt().coerceIn(0, RainbowShimmer.size - 1)
            val nextIdx = (shimmerIdx + 1) % RainbowShimmer.size
            val lerpFrac = (shimmerPhase % 1f)
            val shimmerColor = Color(
                red = RainbowShimmer[shimmerIdx].red * (1f - lerpFrac) + RainbowShimmer[nextIdx].red * lerpFrac,
                green = RainbowShimmer[shimmerIdx].green * (1f - lerpFrac) + RainbowShimmer[nextIdx].green * lerpFrac,
                blue = RainbowShimmer[shimmerIdx].blue * (1f - lerpFrac) + RainbowShimmer[nextIdx].blue * lerpFrac,
                alpha = 0.4f,
            )
            val mythicAlpha = 0.35f + sin(t * 3f) * 0.12f
            // 외곽 글로우
            drawOval(
                color = PlatformMythicGold.copy(alpha = mythicAlpha * 0.35f),
                topLeft = Offset(screenX - pedestalRx * 1.4f, screenY - pedestalRy * 0.65f),
                size = Size(pedestalRx * 2.8f, pedestalRy * 2.8f),
            )
            // 내부 글로우
            drawOval(
                color = PlatformMythicGold.copy(alpha = mythicAlpha + 0.06f),
                topLeft = Offset(screenX - pedestalRx * 0.9f, screenY - pedestalRy * 0.3f),
                size = Size(pedestalRx * 1.8f, pedestalRy * 1.8f),
            )
            // 레인보우 외곽 링
            drawOval(
                color = shimmerColor,
                topLeft = Offset(screenX - pedestalRx * 1.2f, screenY - pedestalRy * 0.4f),
                size = Size(pedestalRx * 2.4f, pedestalRy * 2.2f),
                style = GroundBorderStroke,
            )
            // 골드 코어 펄스
            val corePulse = 0.25f + sin(t * 5f) * 0.15f
            drawOval(
                color = PlatformMythicCore.copy(alpha = corePulse),
                topLeft = Offset(screenX - pedestalRx * 0.4f, screenY + pedestalRy * 0.0f),
                size = Size(pedestalRx * 0.8f, pedestalRy * 0.8f),
            )
            // 내부 골드 링
            drawOval(
                color = PlatformMythicGold.copy(alpha = mythicAlpha + 0.18f),
                topLeft = Offset(screenX - pedestalRx * 0.9f, screenY - pedestalRy * 0.2f),
                size = Size(pedestalRx * 1.8f, pedestalRy * 1.6f),
                style = ThinStroke1_5f,
            )
            // 별빛 파티클 8개
            for (p in 0 until 8) {
                val angle = t * 1.2f + p * (PI.toFloat() / 4f)
                val starR = pedestalRx * (0.9f + sin(t * 2.5f + p * 1.1f) * 0.2f)
                val px = screenX + cos(angle) * starR
                val py = screenY + sin(angle) * starR * 0.55f
                val sAlpha = 0.5f + sin(t * 4f + p * 1.8f) * 0.35f
                drawCircle(
                    color = EffectWhite.copy(alpha = sAlpha),
                    radius = 1.5f,
                    center = Offset(px, py),
                )
            }
        }
    }
}

/**
 * SSJ Aura: 드래곤볼 슈퍼사이어인 스타일 — 유닛 몸체를 감싸며 위로 타오르는 에너지 불꽃.
 * 등급이 높을수록 불꽃이 크고 밝고 층이 많다.
 */
private fun DrawScope.drawSsjAura(
    grade: Int,
    cx: Float,
    cy: Float,
    spriteSize: Float,
    t: Float,
    unitId: Int,
) {
    val seed = unitId * 0.7f
    val halfW = spriteSize * 0.5f

    when (grade) {
        1 -> {
            // Rare: 얇은 파란 오라 — 불꽃 기둥 3개
            for (f in 0 until 3) {
                val phase = t * 4f + f * 2.1f + seed
                val fx = cx + sin(phase * 0.7f) * halfW * 0.4f + (f - 1) * halfW * 0.3f
                val rise = (phase % (TWO_PI)) / (TWO_PI)
                val fy = cy + spriteSize * 0.3f - rise * spriteSize * 1.0f
                val fAlpha = (1f - rise) * 0.25f
                val fRadius = halfW * (0.15f + (1f - rise) * 0.12f)
                drawCircle(color = AuraRareOuter.copy(alpha = fAlpha), radius = fRadius * 1.4f, center = Offset(fx, fy))
                drawCircle(color = AuraRareInner.copy(alpha = fAlpha * 0.7f), radius = fRadius * 0.7f, center = Offset(fx, fy))
            }
        }
        2 -> {
            // Hero: 보라 오라 — 불꽃 기둥 5개 + 내부 밝은 코어
            for (f in 0 until 5) {
                val phase = t * 4.5f + f * 1.26f + seed
                val fx = cx + sin(phase * 0.6f) * halfW * 0.55f + (f - 2) * halfW * 0.22f
                val rise = (phase % (TWO_PI)) / (TWO_PI)
                val fy = cy + spriteSize * 0.3f - rise * spriteSize * 1.2f
                val fAlpha = (1f - rise) * 0.3f
                val fRadius = halfW * (0.18f + (1f - rise) * 0.14f)
                drawCircle(color = PlatformHeroColor.copy(alpha = fAlpha), radius = fRadius * 1.5f, center = Offset(fx, fy))
                drawCircle(color = PlatformHeroCore.copy(alpha = fAlpha * 0.6f), radius = fRadius * 0.6f, center = Offset(fx, fy))
            }
            val coreAlpha = 0.12f + sin(t * 5f + seed) * 0.06f
            drawOval(
                color = PlatformHeroCore.copy(alpha = coreAlpha),
                topLeft = Offset(cx - halfW * 0.4f, cy - spriteSize * 0.4f),
                size = Size(halfW * 0.8f, spriteSize * 0.8f),
            )
        }
        3 -> {
            // Legend: 금빛 슈퍼사이어인 오라 — 불꽃 기둥 7개 + 이중 코어 + 백색 하이라이트
            for (f in 0 until 7) {
                val phase = t * 5f + f * 0.9f + seed
                val spread = (f - 3) * halfW * 0.18f
                val fx = cx + sin(phase * 0.5f) * halfW * 0.6f + spread
                val rise = (phase % (TWO_PI)) / (TWO_PI)
                val fy = cy + spriteSize * 0.35f - rise * spriteSize * 1.4f
                val fAlpha = (1f - rise) * 0.35f
                val fRadius = halfW * (0.2f + (1f - rise) * 0.16f)
                drawCircle(color = PlatformLegendColor.copy(alpha = fAlpha), radius = fRadius * 1.6f, center = Offset(fx, fy))
                drawCircle(color = AuraLegendInner.copy(alpha = fAlpha * 0.7f), radius = fRadius * 0.8f, center = Offset(fx, fy))
                if (f % 2 == 0) {
                    drawCircle(color = EffectWhite.copy(alpha = fAlpha * 0.4f), radius = fRadius * 0.35f, center = Offset(fx, fy))
                }
            }
            val coreAlpha = 0.15f + sin(t * 6f + seed) * 0.08f
            drawOval(
                color = PlatformLegendColor.copy(alpha = coreAlpha),
                topLeft = Offset(cx - halfW * 0.55f, cy - spriteSize * 0.5f),
                size = Size(halfW * 1.1f, spriteSize * 1.0f),
            )
            drawOval(
                color = AuraLegendInner.copy(alpha = coreAlpha * 0.6f),
                topLeft = Offset(cx - halfW * 0.3f, cy - spriteSize * 0.35f),
                size = Size(halfW * 0.6f, spriteSize * 0.7f),
            )
        }
        4 -> {
            // Mythic: 황금+백색 폭발 오라 — 불꽃 기둥 10개 + 전기 스파크 + 삼중 코어
            for (f in 0 until 10) {
                val phase = t * 5.5f + f * 0.63f + seed
                val spread = (f - 4.5f) * halfW * 0.14f
                val wobble = sin(phase * 0.4f + f * 0.3f) * halfW * 0.65f
                val fx = cx + wobble + spread
                val rise = (phase % (TWO_PI)) / (TWO_PI)
                val fy = cy + spriteSize * 0.4f - rise * spriteSize * 1.6f
                val fAlpha = (1f - rise) * 0.38f
                val fRadius = halfW * (0.22f + (1f - rise) * 0.18f)
                drawCircle(color = PlatformMythicGold.copy(alpha = fAlpha), radius = fRadius * 1.7f, center = Offset(fx, fy))
                drawCircle(color = PlatformMythicCore.copy(alpha = fAlpha * 0.7f), radius = fRadius * 0.8f, center = Offset(fx, fy))
                if (f % 3 == 0) {
                    drawCircle(color = EffectWhite.copy(alpha = fAlpha * 0.5f), radius = fRadius * 0.3f, center = Offset(fx, fy))
                }
            }
            for (s in 0 until 4) {
                val sPhase = t * 8f + s * 1.57f + seed
                val sx = cx + cos(sPhase) * halfW * 0.7f
                val sy = cy - spriteSize * 0.2f + sin(sPhase * 1.3f) * spriteSize * 0.4f
                val sAlpha = (0.4f + sin(sPhase * 3f) * 0.4f).coerceAtLeast(0f)
                drawCircle(color = AuraMythicElectric.copy(alpha = sAlpha), radius = 2f, center = Offset(sx, sy))
            }
            val coreAlpha = 0.18f + sin(t * 7f + seed) * 0.1f
            drawOval(
                color = PlatformMythicGold.copy(alpha = coreAlpha),
                topLeft = Offset(cx - halfW * 0.65f, cy - spriteSize * 0.55f),
                size = Size(halfW * 1.3f, spriteSize * 1.1f),
            )
            drawOval(
                color = PlatformMythicCore.copy(alpha = coreAlpha * 0.7f),
                topLeft = Offset(cx - halfW * 0.35f, cy - spriteSize * 0.4f),
                size = Size(halfW * 0.7f, spriteSize * 0.8f),
            )
            drawOval(
                color = EffectWhite.copy(alpha = coreAlpha * 0.3f),
                topLeft = Offset(cx - halfW * 0.2f, cy - spriteSize * 0.3f),
                size = Size(halfW * 0.4f, spriteSize * 0.6f),
            )
        }
    }
}
