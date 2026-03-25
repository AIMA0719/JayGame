package com.example.jaygame.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.asImageBitmap
import com.example.jaygame.R
import com.example.jaygame.bridge.BUFF_BIT_ARMOR_BREAK
import com.example.jaygame.bridge.BUFF_BIT_DOT
import com.example.jaygame.bridge.BUFF_BIT_LIGHTNING
import com.example.jaygame.bridge.BUFF_BIT_POISON
import com.example.jaygame.bridge.BUFF_BIT_SLOW
import com.example.jaygame.bridge.BUFF_BIT_STUN
import com.example.jaygame.bridge.BUFF_BIT_WIND
import com.example.jaygame.bridge.BattleBridge
import kotlin.math.cos
import kotlin.math.sin

// Pre-allocated color constants
private val HpBarBg = Color.Black.copy(alpha = 0.6f)
private val HpBarBorder = Color.White.copy(alpha = 0.3f)
private val HpBarBorderStroke = Stroke(width = 1f)
private val BossAuraRed = Color(0xFFFF2222).copy(alpha = 0.15f)
private val BossGlowRed = Color(0xFFFF4444).copy(alpha = 0.25f)
private val BossGlowRedBright = Color(0xFFFF6666).copy(alpha = 0.4f)
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

/** Death effect particle data */
internal data class DeathEffect(
    val x: Float, val y: Float, val type: Int,
    var timer: Float = 0.5f,
    val particles: List<DeathParticle>,
)

internal data class DeathParticle(
    val vx: Float, val vy: Float,
    val size: Float,
    val colorIdx: Int,
)

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
        mapOf(
            0 to R.drawable.ic_enemy_0, 1 to R.drawable.ic_enemy_1,
            2 to R.drawable.ic_enemy_2, 3 to R.drawable.ic_enemy_3,
            4 to R.drawable.ic_enemy_4, 5 to R.drawable.ic_enemy_5,
            6 to R.drawable.ic_enemy_6, 99 to R.drawable.ic_enemy_boss,
        ).mapValues { (_, resId) -> decodeScaledBitmap(context, resId, 96) }
    }

    val bossBitmap = remember { decodeScaledBitmap(context, R.drawable.ic_enemy_boss, 128) }

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

    // Death effects
    val deathEffects = remember { mutableStateOf(listOf<DeathEffect>()) }

    // Continuously lerp toward target positions at display frame rate
    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { _ ->
                val dt = 1f / 60f
                animTime.floatValue += dt

                val data = BattleBridge.enemyPositions.value
                val sx = smoothXs.value
                val sy = smoothYs.value

                if (data.count != sx.size) {
                    // Enemy count changed — detect deaths
                    if (data.count < prevEnemyCount.value && prevEnemyCount.value > 0) {
                        // Find which enemies disappeared (simple: add death effects at old positions
                        // not found in new data)
                        val newDeaths = mutableListOf<DeathEffect>()
                        val oldXs = prevEnemyXs.value
                        val oldYs = prevEnemyYs.value
                        val oldTypes = prevEnemyTypes.value
                        val oldCount = prevEnemyCount.value

                        for (oi in 0 until oldCount.coerceAtMost(oldXs.size)) {
                            var found = false
                            for (ni in 0 until data.count) {
                                val dx = oldXs[oi] - data.xs[ni]
                                val dy = oldYs[oi] - data.ys[ni]
                                if (dx * dx + dy * dy < 0.002f) {
                                    found = true
                                    break
                                }
                            }
                            if (!found && newDeaths.size < 5) {
                                val particles = List(8) {
                                    val angle = it * 0.785f // ~45 deg
                                    DeathParticle(
                                        vx = cos(angle) * (40f + it * 10f),
                                        vy = sin(angle) * (40f + it * 10f) - 30f,
                                        size = 3f + it % 3,
                                        colorIdx = oldTypes.getOrElse(oi) { 0 } % 5,
                                    )
                                }
                                newDeaths.add(DeathEffect(oldXs[oi], oldYs[oi],
                                    oldTypes.getOrElse(oi) { 0 }, particles = particles))
                            }
                        }
                        if (newDeaths.isNotEmpty()) {
                            deathEffects.value = deathEffects.value + newDeaths
                        }
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
                    val newX = FloatArray(data.count)
                    val newY = FloatArray(data.count)
                    for (i in 0 until data.count) {
                        newX[i] = sx[i] + (data.xs[i] - sx[i]) * lerpFactor
                        newY[i] = sy[i] + (data.ys[i] - sy[i]) * lerpFactor
                    }
                    smoothXs.value = newX
                    smoothYs.value = newY

                    // Hit flash detection + HP smooth lerp
                    val oldHp = prevHpRatios.value
                    val flash = hitFlashTimers.value
                    val sHp = smoothHpRatios.value
                    val newFlash = FloatArray(data.count)
                    val newSmoothHp = FloatArray(data.count)
                    for (i in 0 until data.count) {
                        // Detect HP decrease
                        val prevHp = if (i < oldHp.size) oldHp[i] else 1f
                        if (data.hpRatios[i] < prevHp - 0.01f) {
                            newFlash[i] = 0.12f // flash for 0.12 seconds
                        } else {
                            newFlash[i] = (if (i < flash.size) flash[i] else 0f) - dt
                            if (newFlash[i] < 0f) newFlash[i] = 0f
                        }
                        // Smooth HP bar
                        val curSmooth = if (i < sHp.size) sHp[i] else data.hpRatios[i]
                        newSmoothHp[i] = curSmooth + (data.hpRatios[i] - curSmooth) * 0.15f
                    }
                    hitFlashTimers.value = newFlash
                    smoothHpRatios.value = newSmoothHp
                    prevHpRatios.value = data.hpRatios.copyOf(data.count)
                }

                // Save for death detection
                prevEnemyCount.value = data.count
                if (data.count > 0) {
                    prevEnemyXs.value = data.xs.copyOf(data.count)
                    prevEnemyYs.value = data.ys.copyOf(data.count)
                    prevEnemyTypes.value = data.types.copyOf(data.count)
                }

                // Update death effects
                val effects = deathEffects.value
                if (effects.isNotEmpty()) {
                    val updated = effects.mapNotNull { de ->
                        val newTimer = de.timer - dt
                        if (newTimer <= 0f) null else de.copy(timer = newTimer)
                    }
                    deathEffects.value = updated
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
            val isBoss = type == 99

            // 적 크기: 경로 폭 기준 (그리드 가로 기준, 세로 비율 무관)
            val pathWidth = w * (70f / 720f)  // 경로 마진 70px in 720-space
            val spriteSize = when {
                isBoss -> pathWidth * 1.955f
                type == 6 -> pathWidth * 1.615f
                else -> pathWidth * 1.36f
            }
            val bitmap = if (isBoss) bossBitmap else (enemyBitmaps[type] ?: enemyBitmaps[0])

            // ── Walking Wobble ──
            val wobbleOffsetX = sin(t * 8f + i * 1.3f) * 1.5f

            // ── Boss aura ──
            if (isBoss) {
                val auraR = spriteSize * 0.9f + sin(t * 3f) * 6f
                drawCircle(
                    color = BossAuraRed,
                    radius = auraR,
                    center = Offset(screenX, screenY),
                )
                drawCircle(
                    color = BossGlowRed,
                    radius = auraR * 0.7f,
                    center = Offset(screenX, screenY),
                )
                // Pulsing red glow ring
                val ringAlpha = 0.2f + sin(t * 4f) * 0.15f
                drawCircle(
                    color = BossGlowRedBright.copy(alpha = ringAlpha),
                    radius = auraR,
                    center = Offset(screenX, screenY),
                    style = Stroke(width = 2f),
                )
            }

            // ── Hit reaction: shrink + red outline ──
            val flashTimer = if (i < flashTimers.size) flashTimers[i] else 0f
            val hitScale = if (flashTimer > 0f) {
                val t01 = (flashTimer / 0.12f).coerceIn(0f, 1f)
                1f - t01 * 0.15f  // shrink up to 15%
            } else 1f
            val drawSize = spriteSize * hitScale

            // Draw enemy sprite with wobble + hit shrink
            if (bitmap != null) {
                drawImage(
                    image = bitmap,
                    dstOffset = androidx.compose.ui.unit.IntOffset(
                        (screenX - drawSize / 2 + wobbleOffsetX).toInt(),
                        (screenY - drawSize / 2).toInt(),
                    ),
                    dstSize = androidx.compose.ui.unit.IntSize(drawSize.toInt(), drawSize.toInt()),
                )
            } else {
                // Fallback: colored circle
                val color = EnemyColors[type % EnemyColors.size]
                drawCircle(
                    color = color,
                    radius = drawSize / 2,
                    center = Offset(screenX + wobbleOffsetX, screenY),
                )
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

        // ── Death Effects (fx_execute 스프라이트 + 파티클) ──
        val effects = deathEffects.value
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

            for (p in de.particles) {
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
