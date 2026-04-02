package com.jay.jaygame.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.asImageBitmap
import com.jay.jaygame.R
import com.jay.jaygame.bridge.BUFF_BIT_ARMOR_BREAK
import com.jay.jaygame.bridge.BUFF_BIT_DOT
import com.jay.jaygame.bridge.BUFF_BIT_LIGHTNING
import com.jay.jaygame.bridge.BUFF_BIT_POISON
import com.jay.jaygame.bridge.BUFF_BIT_SLOW
import com.jay.jaygame.bridge.BUFF_BIT_STUN
import com.jay.jaygame.bridge.BUFF_BIT_WIND
import com.jay.jaygame.bridge.BattleBridge
import kotlin.math.cos
import kotlin.math.sin

// Pre-allocated color constants
private val HpBarBg = Color.Black.copy(alpha = 0.6f)
private val HpBarBorder = Color.White.copy(alpha = 0.3f)
private val HpBarBorderStroke = Stroke(width = 1f)
private val EnemyColors = arrayOf(
    Color(0xFFFF6B35), Color(0xFF64B5F6), Color(0xFF81C784),
    Color(0xFFFFD54F), Color(0xFFCE93D8), Color(0xFF43A047),
)

// ── Buff effect colors (pre-allocated to avoid GC) ──
// Lightning, Wind만 Canvas 렌더링 유지 (스프라이트보다 선/파티클이 적합)

// Lightning — spark lines
private val LightningYellow = Color(0xFFFFEE44)
private val LightningWhite = Color(0xFFFFFFCC)
private val LightningGlow = Color(0xFFFFDD00).copy(alpha = 0.2f)

// Wind — swirl and dust
private val WindCyan = Color(0xFF88FFDD)
private val WindDust = Color(0xFFBBAA88)

// Pre-allocated HP bar colors at 10% increments to avoid Color() in draw loop
private val HpBarColors = Array(11) { i ->
    val ratio = i / 10f
    Color(red = 1f - ratio, green = ratio, blue = 0.1f, alpha = 0.9f)
}

/** Map HP ratio (0-1) to nearest pre-allocated color to avoid per-frame allocation */
private fun hpBarColor(clampedHp: Float): Color {
    val idx = (clampedHp * 10f).toInt().coerceIn(0, 10)
    return HpBarColors[idx]
}

// ── Hit/Die animation color filters (pre-allocated, GC-free) ──
private val HitWhiteFlash = ColorFilter.tint(
    Color(0xFFFFFFFF).copy(alpha = 0.7f), BlendMode.SrcAtop
)
private val HitRedTint = ColorFilter.tint(
    Color(0xFFFF4444).copy(alpha = 0.5f), BlendMode.SrcAtop
)
private val DieRedTints = Array(11) { i ->
    ColorFilter.tint(Color(0xFFFF2222).copy(alpha = i / 10f * 0.7f), BlendMode.SrcAtop)
}

/** 죽은 몬스터 스프라이트 페이드아웃 추적 (0.5초) */
internal class DyingEnemy(
    val x: Float, val y: Float, val type: Int,
    var timer: Float = DURATION,
) {
    companion object { const val DURATION = 0.5f }
}

/** Death effect particle data */
internal data class DeathEffect(
    val x: Float, val y: Float, val type: Int,
    var timer: Float = 0.5f,
)

internal data class DeathParticle(
    val vx: Float, val vy: Float,
    val size: Float,
    val colorIdx: Int,
)

private val DeathParticleTemplate = Array(8) {
    val angle = it * 0.785f
    DeathParticle(
        vx = cos(angle) * (40f + it * 10f),
        vy = sin(angle) * (40f + it * 10f) - 30f,
        size = 3f + it % 3,
        colorIdx = it % 5,
    )
}

private fun quantizedEnemyKey(x: Float, y: Float): Int {
    val qx = (x * 1000f).toInt()
    val qy = (y * 1000f).toInt()
    return (qx shl 16) xor (qy and 0xFFFF)
}

/**
 * Renders all active enemies on a full-screen Canvas overlay.
 * Enemy positions come from C++ via BattleBridge as normalized coordinates (0-1).
 * Enemies are drawn as sprite icons with HP bars.
 * Positions are smoothly interpolated between updates for fluid 60fps movement.
 */
@Composable
fun EnemyOverlay() {
    val enemies by BattleBridge.enemyPositions.collectAsState()
    val hpBarMode by BattleBridge.healthBarMode.collectAsState()
    val context = LocalContext.current

    // Pre-load enemy bitmaps (PNG from drawable-xxhdpi, fallback to vector XML)
    val enemyBitmaps = remember {
        val baseRes = intArrayOf(
            R.drawable.ic_enemy_0, R.drawable.ic_enemy_1, R.drawable.ic_enemy_2,
            R.drawable.ic_enemy_3, R.drawable.ic_enemy_4, R.drawable.ic_enemy_5,
            R.drawable.ic_enemy_6,
        )
        // 타입 0~10 + 보스(99) — drawable 없는 7~10은 기존 리소스 순환
        (0..10).associate { i -> i to decodeScaledBitmap(context, baseRes[i % baseRes.size], 96) } +
            (com.jay.jaygame.engine.WaveSystem.BOSS_ENEMY_TYPE to decodeScaledBitmap(context, R.drawable.ic_enemy_boss, 96))
    }

    val bossBitmap = remember { decodeScaledBitmap(context, R.drawable.ic_enemy_boss, 128) }

    // 스프라이트 시트 애니메이터 (assets/enemies/ 에 시트가 있으면 사용, 없으면 null → 정적 fallback)
    val enemyAnimators = remember {
        mapOf(
            0 to decodeAssetSpriteSheet(context, "enemies/enemy_0_sheet.png", 96, 96, 8, 4),
            1 to decodeAssetSpriteSheet(context, "enemies/enemy_1_sheet.png", 96, 96, 8, 4),
            2 to decodeAssetSpriteSheet(context, "enemies/enemy_2_sheet.png", 96, 96, 8, 4),
            3 to decodeAssetSpriteSheet(context, "enemies/enemy_3_sheet.png", 96, 96, 8, 4),
            4 to decodeAssetSpriteSheet(context, "enemies/enemy_4_sheet.png", 96, 96, 8, 4),
            5 to decodeAssetSpriteSheet(context, "enemies/enemy_5_sheet.png", 96, 96, 8, 4),
            6 to decodeAssetSpriteSheet(context, "enemies/enemy_6_sheet.png", 96, 96, 8, 4),
            7 to decodeAssetSpriteSheet(context, "enemies/enemy_7_sheet.png", 96, 96, 8, 4),
            8 to decodeAssetSpriteSheet(context, "enemies/enemy_8_sheet.png", 96, 96, 8, 4),
            9 to decodeAssetSpriteSheet(context, "enemies/enemy_9_sheet.png", 96, 96, 8, 4),
            10 to decodeAssetSpriteSheet(context, "enemies/enemy_10_sheet.png", 96, 96, 8, 4),
            99 to decodeAssetSpriteSheet(context, "enemies/enemy_boss_sheet.png", 128, 128, 8, 4),
        )
    }

    // Per-enemy animation state — pre-allocated pool (256 max, GC-free)
    val animStates = remember { Array(256) { EnemyAnimState() } }

    // Battle speed for animation & LOD
    val battleSpeed by BattleBridge.battleSpeed.collectAsState()

    // Pre-load VFX sprite bitmaps from assets/fx/
    val fxBitmaps = remember {
        listOf("fx_slow", "fx_dot_fire", "fx_dot_poison", "fx_armor_break",
            "fx_stun", "fx_freeze", "fx_execute",
            "fx_chain_lightning", "fx_aoe_earth")
            .associateWith { name -> decodeAssetBitmap(context, "fx/$name.png") }
    }

    // Smooth interpolated positions
    val smoothXs = remember { mutableStateOf(FloatArray(0)) }
    val smoothYs = remember { mutableStateOf(FloatArray(0)) }

    // Animation time
    val animTime = remember { mutableFloatStateOf(0f) }

    // HP tracking for hit flash
    val prevHpRatios = remember { mutableStateOf(FloatArray(0)) }
    val hitFlashTimers = remember { mutableStateOf(FloatArray(0)) }

    // Smooth HP bar values (lerped)
    val smoothHpRatios = remember { mutableStateOf(FloatArray(0)) }

    // Previous enemy count for death detection
    val prevEnemyCount = remember { mutableStateOf(0) }
    val prevEnemyXs = remember { mutableStateOf(FloatArray(0)) }
    val prevEnemyYs = remember { mutableStateOf(FloatArray(0)) }
    val prevEnemyTypes = remember { mutableStateOf(IntArray(0)) }

    // Dying enemies (sprite fade-out after leaving active list)
    val dyingEnemies = remember { mutableStateListOf<DyingEnemy>() }

    // Death effects
    val deathEffects = remember { mutableStateListOf<DeathEffect>() }
    val currentEnemyPositionKeys = remember { HashSet<Int>(256) }

    // Continuously lerp toward target positions at display frame rate
    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { _ ->
                val dt = 1f / 60f
                animTime.floatValue += dt

                val data = BattleBridge.enemyPositions.value
                val sx = smoothXs.value
                val sy = smoothYs.value

                // 배속 가져오기
                val speed = BattleBridge.battleSpeed.value

                if (data.count != sx.size) {
                    // Enemy count changed — reset animation states for new indices
                    for (ai in 0 until data.count.coerceAtMost(256)) {
                        animStates[ai].reset()
                    }

                    // Enemy count changed — detect deaths
                    if (data.count < prevEnemyCount.value && prevEnemyCount.value > 0) {
                        // Find which enemies disappeared (simple: add death effects at old positions
                        // not found in new data)
                        val oldXs = prevEnemyXs.value
                        val oldYs = prevEnemyYs.value
                        val oldTypes = prevEnemyTypes.value
                        val oldCount = prevEnemyCount.value
                        currentEnemyPositionKeys.clear()
                        for (ni in 0 until data.count) {
                            currentEnemyPositionKeys.add(quantizedEnemyKey(data.xs[ni], data.ys[ni]))
                        }

                        for (oi in 0 until oldCount.coerceAtMost(oldXs.size)) {
                            val key = quantizedEnemyKey(oldXs[oi], oldYs[oi])
                            if (key !in currentEnemyPositionKeys && deathEffects.size < 5) {
                                val type = oldTypes.getOrElse(oi) { 0 }
                                deathEffects.add(DeathEffect(oldXs[oi], oldYs[oi], type))
                                dyingEnemies.add(DyingEnemy(x = oldXs[oi], y = oldYs[oi], type = type))
                            }
                        }
                        
                            // 죽는 몬스터 스프라이트 페이드아웃 추가
                        
                    }

                    // Snap to new positions
                    smoothXs.value = data.xs.copyOf(data.count)
                    smoothYs.value = data.ys.copyOf(data.count)
                    smoothHpRatios.value = data.hpRatios.copyOf(data.count)
                    prevHpRatios.value = data.hpRatios.copyOf(data.count)
                    hitFlashTimers.value = FloatArray(data.count)
                } else if (data.count > 0) {
                    // Lerp toward target positions
                    val lerpFactor = 0.2f
                    for (i in 0 until data.count) {
                        sx[i] = sx[i] + (data.xs[i] - sx[i]) * lerpFactor
                        sy[i] = sy[i] + (data.ys[i] - sy[i]) * lerpFactor
                    }
                    smoothXs.value = sx
                    smoothYs.value = sy

                    // Hit flash detection + HP smooth lerp
                    val oldHp = prevHpRatios.value
                    val flashTimers = hitFlashTimers.value
                    val sHp = smoothHpRatios.value
                    for (i in 0 until data.count) {
                        // Detect HP decrease
                        val prevHp = if (i < oldHp.size) oldHp[i] else 1f
                        if (data.hpRatios[i] < prevHp - 0.01f) {
                            flashTimers[i] = 0.12f // flash for 0.12 seconds
                        } else {
                            flashTimers[i] = (if (i < flashTimers.size) flashTimers[i] else 0f) - dt
                            if (flashTimers[i] < 0f) flashTimers[i] = 0f
                        }
                        // Smooth HP bar
                        val curSmooth = if (i < sHp.size) sHp[i] else data.hpRatios[i]
                        sHp[i] = curSmooth + (data.hpRatios[i] - curSmooth) * 0.15f
                        oldHp[i] = data.hpRatios[i]
                    }
                    hitFlashTimers.value = flashTimers
                    smoothHpRatios.value = sHp
                    prevHpRatios.value = oldHp

                    // 스프라이트 시트 애니메이션 상태 업데이트 (8x 배속 이상이면 스킵)
                    if (speed < 6f) {
                        for (ai in 0 until data.count.coerceAtMost(256)) {
                            val anim = animStates[ai]
                            val flash = if (ai < flashTimers.size) flashTimers[ai] else 0f
                            val buffBits = if (ai < data.buffs.size) data.buffs[ai] else 0
                            val isStunned = buffBits and BUFF_BIT_STUN != 0
                            // 상태 전이: hit > stun(idle) > walk
                            when {
                                flash > 0f && anim.state != SpriteSheetAnimator.STATE_DIE ->
                                    anim.transition(SpriteSheetAnimator.STATE_HIT)
                                anim.state == SpriteSheetAnimator.STATE_HIT && flash <= 0f ->
                                    anim.transition(if (isStunned) SpriteSheetAnimator.STATE_IDLE else SpriteSheetAnimator.STATE_WALK)
                                isStunned && anim.state == SpriteSheetAnimator.STATE_WALK ->
                                    anim.transition(SpriteSheetAnimator.STATE_IDLE)
                                !isStunned && anim.state == SpriteSheetAnimator.STATE_IDLE ->
                                    anim.transition(SpriteSheetAnimator.STATE_WALK)
                            }
                            anim.advance(dt, 8, speed)
                        }
                    }
                }

                // Save for death detection
                prevEnemyCount.value = data.count
                if (data.count > 0) {
                    prevEnemyXs.value = data.xs.copyOf(data.count)
                    prevEnemyYs.value = data.ys.copyOf(data.count)
                    prevEnemyTypes.value = data.types.copyOf(data.count)
                }

                // Update death effects
                if (deathEffects.isNotEmpty()) {
                    for (idx in deathEffects.lastIndex downTo 0) {
                        val de = deathEffects[idx]
                        de.timer -= dt
                        if (de.timer <= 0f) deathEffects.removeAt(idx)
                    }
                }

                // Update dying enemy sprite fade-outs
                if (dyingEnemies.isNotEmpty()) {
                    for (idx in dyingEnemies.lastIndex downTo 0) {
                        val de = dyingEnemies[idx]
                        de.timer -= dt
                        if (de.timer <= 0f) dyingEnemies.removeAt(idx)
                    }
                }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val data = enemies
        val sxArr = smoothXs.value
        val syArr = smoothYs.value
        val t = animTime.floatValue
        val w = size.width
        val h = size.height
        // Use smooth positions if sizes match, otherwise fall back to raw
        val useSmooth = sxArr.size == data.count && data.count > 0
        val flashTimers = hitFlashTimers.value
        val sHp = smoothHpRatios.value

        for (i in 0 until data.count) {
            val screenX = if (useSmooth) sxArr[i] * w else data.xs[i] * w
            val screenY = if (useSmooth) syArr[i] * h else data.ys[i] * h
            val type = data.types[i]
            val hpRatio = if (i < sHp.size) sHp[i] else data.hpRatios[i]
            val isBoss = type == com.jay.jaygame.engine.WaveSystem.BOSS_ENEMY_TYPE

            // 적 크기: 경로 폭 기준 (그리드 가로 기준, 세로 비율 무관)
            val pathWidth = w * (70f / 720f)  // 경로 마진 70px in 720-space
            val spriteSize = when {
                isBoss -> pathWidth * 1.955f
                type == 6 -> pathWidth * 1.615f
                else -> pathWidth * 1.36f
            }
            val bitmap = if (isBoss) bossBitmap else (enemyBitmaps[type] ?: enemyBitmaps[0])

            val bSpeed = battleSpeed

            // ── WALK animation: wobble + bounce + tilt ──
            val wobbleOffsetX: Float
            val wobbleOffsetY: Float
            val wobbleRotation: Float
            if (bSpeed < 8f) {
                val speedMul = bSpeed.coerceAtMost(4f)
                val phase = t * 8f + i * 1.3f
                wobbleOffsetX = sin(phase) * 2.5f * speedMul
                val bounceAmp = if (isBoss) 4.5f else 3f
                wobbleOffsetY = sin(t * 6f + i * 1.5f) * bounceAmp * speedMul
                wobbleRotation = sin(phase) * 3f
            } else {
                wobbleOffsetX = 0f; wobbleOffsetY = 0f; wobbleRotation = 0f
            }

            // ── HIT animation: squash/stretch + tint + knockback ──
            val flashTimer = if (i < flashTimers.size) flashTimers[i] else 0f
            val hitScaleX: Float
            val hitScaleY: Float
            val hitKnockbackX: Float
            val hitColorFilter: ColorFilter?
            if (flashTimer > 0f) {
                val t01 = (flashTimer / 0.12f).coerceIn(0f, 1f)
                hitScaleX = 1f + t01 * 0.15f   // 1.15 at impact → 1.0
                hitScaleY = 1f - t01 * 0.15f   // 0.85 at impact → 1.0
                hitKnockbackX = t01 * 3f
                hitColorFilter = if (flashTimer > 0.09f) HitWhiteFlash else HitRedTint
            } else {
                hitScaleX = 1f; hitScaleY = 1f; hitKnockbackX = 0f; hitColorFilter = null
            }

            // ── Draw enemy sprite with transforms ──
            val finalX = screenX + wobbleOffsetX + hitKnockbackX
            val finalY = screenY + wobbleOffsetY
            val drawSize = spriteSize
            val pivot = Offset(finalX, finalY)
            val dstOff = IntOffset(
                (finalX - drawSize / 2).toInt(),
                (finalY - drawSize / 2).toInt(),
            )
            val dstSz = IntSize(drawSize.toInt(), drawSize.toInt())

            val animator = enemyAnimators[type]

            if (bSpeed >= 6f) {
                // x3: static draw, no transforms
                bitmap?.let {
                    drawImage(image = it, dstOffset = dstOff, dstSize = dstSz)
                }
            } else {
                rotate(degrees = wobbleRotation, pivot = pivot) {
                    scale(scaleX = hitScaleX, scaleY = hitScaleY, pivot = pivot) {
                        if (animator != null && i < 256) {
                            val anim = animStates[i]
                            drawImage(
                                image = animator.sheet,
                                srcOffset = animator.srcOffset(anim.state, anim.frame),
                                srcSize = animator.srcSize(),
                                dstOffset = dstOff,
                                dstSize = dstSz,
                                colorFilter = hitColorFilter,
                            )
                        } else if (bitmap != null) {
                            drawImage(
                                image = bitmap,
                                dstOffset = dstOff,
                                dstSize = dstSz,
                                colorFilter = hitColorFilter,
                            )
                        } else {
                            val color = EnemyColors[type % EnemyColors.size]
                            drawCircle(
                                color = color,
                                radius = drawSize / 2,
                                center = pivot,
                            )
                        }
                    }
                }
            }


            // ── HP bar (improved: rounded, bordered, smooth) ──
            // hpBarMode: 0=항상, 1=피격 시만, 2=숨김
            val clampedHp = hpRatio.coerceIn(0f, 1f)
            val showHpBar = when (hpBarMode) {
                2 -> false
                1 -> flashTimer > 0f || clampedHp < 1f
                else -> true
            }
            if (showHpBar) {
                val barWidth = drawSize
                val barHeight = if (isBoss) 6f else 4f
                val barX = screenX - barWidth / 2
                val barY = screenY - spriteSize / 2 - 10f

                drawRoundRect(
                    color = HpBarBg,
                    topLeft = Offset(barX - 1f, barY - 1f),
                    size = Size(barWidth + 2f, barHeight + 2f),
                    cornerRadius = CornerRadius(3f),
                )
                val hpColor = hpBarColor(clampedHp)
                if (clampedHp > 0f) {
                    drawRoundRect(
                        color = hpColor,
                        topLeft = Offset(barX, barY),
                        size = Size(barWidth * clampedHp, barHeight),
                        cornerRadius = CornerRadius(2f),
                    )
                }
                drawRoundRect(
                    color = HpBarBorder,
                    topLeft = Offset(barX - 1f, barY - 1f),
                    size = Size(barWidth + 2f, barHeight + 2f),
                    cornerRadius = CornerRadius(3f),
                    style = HpBarBorderStroke,
                )
            }

            // ── Buff Visual Effects (sprite-based, 몹 크기의 1/3) ──
            val buffBits = if (i < data.buffs.size) data.buffs[i] else 0
            val fxSize = (spriteSize * 0.4f).toInt().coerceAtLeast(8)

            // E1: Fire DoT — pulsing flame sprite
            if (buffBits and BUFF_BIT_DOT != 0 && buffBits and BUFF_BIT_POISON == 0) {
                fxBitmaps["fx_dot_fire"]?.let { bmp ->
                    val pulse = 1f + sin(t * 6f) * 0.1f
                    val s = (fxSize * pulse).toInt()
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset((screenX - s / 2f).toInt(), (screenY - s / 2f).toInt()),
                        dstSize = IntSize(s, s),
                        alpha = 0.8f,
                    )
                }
            }

            // E2: Frost Slow — breathing ice sprite
            if (buffBits and BUFF_BIT_SLOW != 0 && buffBits and BUFF_BIT_POISON == 0) {
                fxBitmaps["fx_slow"]?.let { bmp ->
                    val pulse = 1f + sin(t * 3f) * 0.08f
                    val s = (fxSize * pulse).toInt()
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset((screenX - s / 2f).toInt(), (screenY - s / 2f).toInt()),
                        dstSize = IntSize(s, s),
                        alpha = 0.75f,
                    )
                }
            }

            // E3: Poison — toxic green sprite
            if (buffBits and BUFF_BIT_POISON != 0) {
                fxBitmaps["fx_dot_poison"]?.let { bmp ->
                    val pulse = 1f + sin(t * 2.5f) * 0.08f
                    val s = (fxSize * pulse).toInt()
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset((screenX - s / 2f).toInt(), (screenY - s / 2f).toInt()),
                        dstSize = IntSize(s, s),
                        alpha = 0.8f,
                    )
                }
            }

            // E4: Armor Break
            if (buffBits and BUFF_BIT_ARMOR_BREAK != 0) {
                fxBitmaps["fx_armor_break"]?.let { bmp ->
                    val s = fxSize
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset((screenX - s / 2f).toInt(), (screenY - s / 2f).toInt()),
                        dstSize = IntSize(s, s),
                        alpha = 0.8f,
                    )
                }
            }

            // E5: Lightning — sprite
            if (buffBits and BUFF_BIT_LIGHTNING != 0) {
                fxBitmaps["fx_chain_lightning"]?.let { bmp ->
                    val pulse = 1f + sin(t * 10f) * 0.15f
                    val s = (fxSize * pulse).toInt()
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset((screenX - s / 2f).toInt(), (screenY - s / 2f).toInt()),
                        dstSize = IntSize(s, s),
                        alpha = 0.85f,
                    )
                }
            }

            // E6: Wind — sprite
            if (buffBits and BUFF_BIT_WIND != 0) {
                fxBitmaps["fx_aoe_earth"]?.let { bmp ->
                    val pulse = 1f + sin(t * 5f) * 0.1f
                    val s = (fxSize * pulse).toInt()
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset((screenX - s / 2f).toInt(), (screenY - s / 2f).toInt()),
                        dstSize = IntSize(s, s),
                        alpha = 0.75f,
                    )
                }
            }

            // E7: Stun — 적 머리 위 스턴 아이콘
            if (buffBits and BUFF_BIT_STUN != 0) {
                fxBitmaps["fx_stun"]?.let { bmp ->
                    val s = fxSize
                    val stunY = screenY - spriteSize * 0.5f
                    val wobble = sin(t * 4f) * 1.5f
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset((screenX - s / 2f + wobble).toInt(), (stunY - s / 2f).toInt()),
                        dstSize = IntSize(s, s),
                        alpha = 0.85f,
                    )
                }
            }

            // E8: Freeze — Stun + Slow 동시 적용 시 빙결
            if (buffBits and BUFF_BIT_STUN != 0 && buffBits and BUFF_BIT_SLOW != 0) {
                fxBitmaps["fx_freeze"]?.let { bmp ->
                    val pulse = 1f + sin(t * 2f) * 0.05f
                    val s = (fxSize * 1.2f * pulse).toInt()
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset((screenX - s / 2f).toInt(), (screenY - s / 2f).toInt()),
                        dstSize = IntSize(s, s),
                        alpha = 0.8f,
                    )
                }
            }
        }

        // ── Dying enemy sprite fade-out ──
        val dyingList = dyingEnemies
        for (de in dyingList) {
            val progress = 1f - (de.timer / DyingEnemy.DURATION) // 0 → 1
            val dx = de.x * w
            val dy = de.y * h
            val deType = de.type
            val deIsBoss = deType == com.jay.jaygame.engine.WaveSystem.BOSS_ENEMY_TYPE

            val pathWidth = w * (70f / 720f)
            val deSpriteSize = when {
                deIsBoss -> pathWidth * 1.955f
                deType == 6 -> pathWidth * 1.615f
                else -> pathWidth * 1.36f
            }

            val deAlpha = (1f - progress).coerceIn(0f, 1f)
            val deScale = 1f - progress * 0.5f
            val fallY = dy + progress * progress * 40f
            val deRotation = progress * 15f
            val tintIdx = (progress * 10f).toInt().coerceIn(0, 10)
            val deTint = DieRedTints[tintIdx]

            val dePivot = Offset(dx, fallY)
            val deOff = IntOffset(
                (dx - deSpriteSize / 2).toInt(),
                (fallY - deSpriteSize / 2).toInt(),
            )
            val deSz = IntSize(deSpriteSize.toInt(), deSpriteSize.toInt())

            // 스프라이트 시트 die 애니메이션 우선, 없으면 정적 PNG fallback
            val deAnimator = enemyAnimators[deType]
            rotate(degrees = deRotation, pivot = dePivot) {
                scale(scaleX = deScale, scaleY = deScale, pivot = dePivot) {
                    if (deAnimator != null) {
                        val dieFrames = deAnimator.frameCount()
                        val frameIdx = (progress * dieFrames).toInt().coerceIn(0, dieFrames - 1)
                        val srcOff = deAnimator.srcOffset(SpriteSheetAnimator.STATE_DIE, frameIdx)
                        drawImage(
                            image = deAnimator.sheet,
                            srcOffset = srcOff,
                            srcSize = deAnimator.srcSize(),
                            dstOffset = deOff,
                            dstSize = deSz,
                            alpha = deAlpha,
                            colorFilter = deTint,
                        )
                    } else {
                        val deBitmap = if (deIsBoss) bossBitmap else (enemyBitmaps[deType] ?: enemyBitmaps[0])
                        deBitmap?.let {
                            drawImage(
                                image = it,
                                dstOffset = deOff,
                                dstSize = deSz,
                                alpha = deAlpha,
                                colorFilter = deTint,
                            )
                        }
                    }
                }
            }
        }

        // ── Death Effects (fx_execute 스프라이트 + 파티클) ──
        val effects = deathEffects
        val executeBmp = fxBitmaps["fx_execute"]
        for (de in effects) {
            val progress = 1f - (de.timer / 0.5f) // 0 -> 1 as timer goes 0.5 -> 0
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val dx = de.x * w
            val dy = de.y * h

            // 처형 스프라이트 (초반 페이드인 → 페이드아웃)
            if (executeBmp != null) {
                val scale = 0.5f + progress * 0.8f
                val s = (48f * scale).toInt().coerceAtLeast(1)
                drawImage(
                    image = executeBmp,
                    dstOffset = IntOffset((dx - s / 2f).toInt(), (dy - s / 2f).toInt()),
                    dstSize = IntSize(s, s),
                    alpha = alpha * 0.9f,
                )
            }

            for (template in DeathParticleTemplate) {
                val colorIdx = de.type % EnemyColors.size
                val p = template.copy(colorIdx = colorIdx)
                val px = dx + p.vx * progress
                val py = dy + p.vy * progress + 20f * progress * progress
                val pColor = EnemyColors[p.colorIdx % EnemyColors.size]
                val pSize = p.size * (1f - progress * 0.5f)
                drawCircle(
                    color = pColor.copy(alpha = alpha * 0.7f),
                    radius = pSize,
                    center = Offset(px, py),
                )
            }
        }
    }
}
