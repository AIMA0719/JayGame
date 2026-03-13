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
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.jaygame.R
import com.example.jaygame.bridge.BUFF_BIT_DOT
import com.example.jaygame.bridge.BUFF_BIT_LIGHTNING
import com.example.jaygame.bridge.BUFF_BIT_POISON
import com.example.jaygame.bridge.BUFF_BIT_SLOW
import com.example.jaygame.bridge.BUFF_BIT_WIND
import com.example.jaygame.bridge.BattleBridge
import kotlin.math.cos
import kotlin.math.sin

// Pre-allocated color constants
private val HpBarBg = Color.Black.copy(alpha = 0.6f)
private val HpBarBorder = Color.White.copy(alpha = 0.3f)
private val HpBarBorderStroke = Stroke(width = 1f)
private val HitFlashWhite = Color.White.copy(alpha = 0.7f)
private val BossAuraRed = Color(0xFFFF2222).copy(alpha = 0.15f)
private val BossGlowRed = Color(0xFFFF4444).copy(alpha = 0.25f)
private val BossGlowRedBright = Color(0xFFFF6666).copy(alpha = 0.4f)
private val DeathParticleColors = arrayOf(
    Color(0xFFFF6B35), Color(0xFF64B5F6), Color(0xFF81C784),
    Color(0xFFFFD54F), Color(0xFFCE93D8),
)
private val FallbackColors = arrayOf(
    Color(0xFFFF6B35),
    Color(0xFF64B5F6),
    Color(0xFF81C784),
    Color(0xFFFFD54F),
    Color(0xFFCE93D8),
)

// ── Buff effect colors (pre-allocated to avoid GC) ──

// E1: Fire — lingering flame particles
private val FireFlameOrange = Color(0xFFFF6B00)
private val FireFlameYellow = Color(0xFFFFAA00)
private val FireFlameRed = Color(0xFFFF2200)
private val FireGlow = Color(0xFFFF4400).copy(alpha = 0.15f)

// E2: Frost — ice crystal particles
private val FrostBlue = Color(0xFF88DDFF)
private val FrostWhite = Color(0xFFCCEEFF)
private val FrostGlow = Color(0xFF44AAFF).copy(alpha = 0.12f)

// E3: Poison — green bubble particles
private val PoisonGreen = Color(0xFF44DD44)
private val PoisonDarkGreen = Color(0xFF228822)
private val PoisonBubble = Color(0xFF66FF66)
private val PoisonTint = Color(0xFF22CC22).copy(alpha = 0.18f)

// E4: Lightning — spark lines
private val LightningYellow = Color(0xFFFFEE44)
private val LightningWhite = Color(0xFFFFFFCC)
private val LightningGlow = Color(0xFFFFDD00).copy(alpha = 0.2f)

// E5: Wind — swirl and dust
private val WindCyan = Color(0xFF88FFDD)
private val WindWhite = Color(0xFFCCFFEE)
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
    val context = LocalContext.current

    // Pre-load enemy bitmaps
    val enemyBitmaps = remember {
        val drawableIds = mapOf(
            0 to R.drawable.ic_enemy_0,
            1 to R.drawable.ic_enemy_1,
            2 to R.drawable.ic_enemy_2,
            3 to R.drawable.ic_enemy_3,
            4 to R.drawable.ic_enemy_4,
            99 to R.drawable.ic_enemy_boss,
        )
        drawableIds.mapValues { (_, resId) ->
            ContextCompat.getDrawable(context, resId)?.toBitmap(48, 48)?.asImageBitmap()
        }
    }

    val bossBitmap = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_enemy_boss)?.toBitmap(72, 72)?.asImageBitmap()
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

            val cellH = h / 5f
            val spriteSize = if (isBoss) cellH * 0.85f else cellH * 0.65f
            val bitmap = if (isBoss) bossBitmap else enemyBitmaps[type % 5]

            // ── Walking Wobble ──
            val wobbleAngle = sin(t * 8f + i * 1.3f) * 3f
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

            // Draw enemy sprite with wobble
            if (bitmap != null) {
                drawImage(
                    image = bitmap,
                    topLeft = Offset(
                        screenX - spriteSize / 2 + wobbleOffsetX,
                        screenY - spriteSize / 2,
                    ),
                )
            } else {
                // Fallback: colored circle
                val color = FallbackColors[type % 5]
                drawCircle(
                    color = color,
                    radius = spriteSize / 2,
                    center = Offset(screenX + wobbleOffsetX, screenY),
                )
            }

            // ── Hit Flash ──
            val flashTimer = if (i < flashTimers.size) flashTimers[i] else 0f
            if (flashTimer > 0f) {
                val flashAlpha = (flashTimer / 0.12f).coerceIn(0f, 1f) * 0.6f
                drawRect(
                    color = HitFlashWhite.copy(alpha = flashAlpha),
                    topLeft = Offset(screenX - spriteSize / 2, screenY - spriteSize / 2),
                    size = Size(spriteSize, spriteSize),
                )
            }

            // ── HP bar (improved: rounded, bordered, smooth) ──
            val barWidth = spriteSize * 1.2f
            val barHeight = if (isBoss) 6f else 4f
            val barX = screenX - barWidth / 2
            val barY = screenY - spriteSize / 2 - 10f

            // Background with rounded corners
            drawRoundRect(
                color = HpBarBg,
                topLeft = Offset(barX - 1f, barY - 1f),
                size = Size(barWidth + 2f, barHeight + 2f),
                cornerRadius = CornerRadius(3f),
            )
            // HP fill — uses pre-allocated color table to avoid per-frame Color() alloc
            val clampedHp = hpRatio.coerceIn(0f, 1f)
            val hpColor = hpBarColor(clampedHp)
            if (clampedHp > 0f) {
                drawRoundRect(
                    color = hpColor,
                    topLeft = Offset(barX, barY),
                    size = Size(barWidth * clampedHp, barHeight),
                    cornerRadius = CornerRadius(2f),
                )
            }
            // Border
            drawRoundRect(
                color = HpBarBorder,
                topLeft = Offset(barX - 1f, barY - 1f),
                size = Size(barWidth + 2f, barHeight + 2f),
                cornerRadius = CornerRadius(3f),
                style = HpBarBorderStroke,
            )

            // ── Buff Visual Effects ──
            val buffBits = if (i < data.buffs.size) data.buffs[i] else 0

            // E1: Fire — flickering flame particles below enemy
            if (buffBits and BUFF_BIT_DOT != 0 && buffBits and BUFF_BIT_POISON == 0) {
                // Glow under enemy
                drawCircle(
                    color = FireGlow,
                    radius = spriteSize * 0.6f,
                    center = Offset(screenX, screenY + spriteSize * 0.2f),
                )
                // 4 flame particles flickering
                for (p in 0 until 4) {
                    val px = screenX + sin(t * 6f + p * 1.6f) * spriteSize * 0.3f
                    val py = screenY + spriteSize * 0.15f - sin(t * 8f + p * 2.1f).coerceAtLeast(0f) * spriteSize * 0.4f
                    val flameAlpha = (0.5f + sin(t * 10f + p * 1.3f) * 0.3f).coerceIn(0.2f, 0.8f)
                    val flameSize = 2.5f + sin(t * 7f + p * 0.9f) * 1f
                    val flameColor = when (p % 3) {
                        0 -> FireFlameOrange
                        1 -> FireFlameYellow
                        else -> FireFlameRed
                    }
                    drawCircle(
                        color = flameColor.copy(alpha = flameAlpha),
                        radius = flameSize,
                        center = Offset(px, py),
                    )
                }
            }

            // E2: Frost — rotating ice crystal shapes around enemy
            if (buffBits and BUFF_BIT_SLOW != 0 && buffBits and BUFF_BIT_POISON == 0) {
                // Frost glow
                drawCircle(
                    color = FrostGlow,
                    radius = spriteSize * 0.55f,
                    center = Offset(screenX, screenY),
                )
                // 3 ice crystals orbiting
                for (p in 0 until 3) {
                    val angle = t * 2.5f + p * 2.094f // 120 deg apart
                    val orbitR = spriteSize * 0.45f
                    val cx = screenX + cos(angle) * orbitR
                    val cy = screenY + sin(angle) * orbitR * 0.6f
                    val crystalSize = 2.5f + sin(t * 4f + p * 1.2f) * 0.8f
                    val crystalAlpha = 0.6f + sin(t * 3f + p * 0.7f) * 0.2f
                    // Diamond shape approximated by small rotated rect
                    drawCircle(
                        color = FrostBlue.copy(alpha = crystalAlpha),
                        radius = crystalSize,
                        center = Offset(cx, cy),
                    )
                    drawCircle(
                        color = FrostWhite.copy(alpha = crystalAlpha * 0.7f),
                        radius = crystalSize * 0.5f,
                        center = Offset(cx, cy),
                    )
                }
            }

            // E3: Poison — green bubbles floating up + green tint
            if (buffBits and BUFF_BIT_POISON != 0) {
                // Green tint overlay on enemy
                drawRect(
                    color = PoisonTint,
                    topLeft = Offset(screenX - spriteSize / 2, screenY - spriteSize / 2),
                    size = Size(spriteSize, spriteSize),
                )
                // 5 green bubbles floating upward
                for (p in 0 until 5) {
                    val bubblePhase = (t * 1.5f + p * 0.4f) % 1f // 0-1 cycle
                    val bx = screenX + sin(t * 2f + p * 1.7f) * spriteSize * 0.35f
                    val by = screenY + spriteSize * 0.2f - bubblePhase * spriteSize * 0.8f
                    val bubbleAlpha = (1f - bubblePhase) * 0.6f
                    val bubbleSize = 1.5f + sin(t * 3f + p * 0.5f) * 0.5f + (1f - bubblePhase) * 1.5f
                    drawCircle(
                        color = PoisonBubble.copy(alpha = bubbleAlpha),
                        radius = bubbleSize,
                        center = Offset(bx, by),
                    )
                    // Inner highlight
                    drawCircle(
                        color = PoisonGreen.copy(alpha = bubbleAlpha * 0.5f),
                        radius = bubbleSize * 0.4f,
                        center = Offset(bx - bubbleSize * 0.2f, by - bubbleSize * 0.2f),
                    )
                }
            }

            // E4: Lightning — spark lines + electric glow
            if (buffBits and BUFF_BIT_LIGHTNING != 0) {
                // Electric glow
                drawCircle(
                    color = LightningGlow,
                    radius = spriteSize * 0.6f,
                    center = Offset(screenX, screenY),
                )
                // 4 spark lines radiating outward
                for (p in 0 until 4) {
                    val angle = t * 12f + p * 1.571f // 90 deg apart, fast rotation
                    val sparkLen = spriteSize * (0.3f + sin(t * 15f + p * 2f) * 0.15f)
                    val sx = screenX + cos(angle) * spriteSize * 0.15f
                    val sy = screenY + sin(angle) * spriteSize * 0.15f
                    val ex = screenX + cos(angle) * sparkLen
                    val ey = screenY + sin(angle) * sparkLen
                    // Zigzag midpoint
                    val mx = (sx + ex) / 2f + sin(t * 20f + p * 3f) * 4f
                    val my = (sy + ey) / 2f + cos(t * 18f + p * 2.5f) * 4f
                    val sparkAlpha = 0.5f + sin(t * 14f + p * 1.1f) * 0.3f
                    drawLine(
                        color = LightningYellow.copy(alpha = sparkAlpha),
                        start = Offset(sx, sy),
                        end = Offset(mx, my),
                        strokeWidth = 1.5f,
                    )
                    drawLine(
                        color = LightningWhite.copy(alpha = sparkAlpha * 0.8f),
                        start = Offset(mx, my),
                        end = Offset(ex, ey),
                        strokeWidth = 1f,
                    )
                }
            }

            // E5: Wind — swirl lines + dust particles
            if (buffBits and BUFF_BIT_WIND != 0) {
                // 3 swirl arc segments
                for (p in 0 until 3) {
                    val swirlAngle = t * 8f + p * 2.094f
                    val swirlR = spriteSize * (0.35f + p * 0.08f)
                    for (seg in 0 until 4) {
                        val a1 = swirlAngle + seg * 0.3f
                        val a2 = swirlAngle + (seg + 1) * 0.3f
                        val swirlAlpha = (0.4f - seg * 0.08f).coerceAtLeast(0.1f)
                        drawLine(
                            color = WindCyan.copy(alpha = swirlAlpha),
                            start = Offset(
                                screenX + cos(a1) * swirlR,
                                screenY + sin(a1) * swirlR * 0.7f,
                            ),
                            end = Offset(
                                screenX + cos(a2) * swirlR,
                                screenY + sin(a2) * swirlR * 0.7f,
                            ),
                            strokeWidth = 1.5f,
                        )
                    }
                }
                // 4 dust particles
                for (p in 0 until 4) {
                    val dustAngle = t * 5f + p * 1.571f
                    val dustR = spriteSize * (0.4f + sin(t * 3f + p * 0.8f) * 0.1f)
                    val dx2 = screenX + cos(dustAngle) * dustR
                    val dy2 = screenY + sin(dustAngle) * dustR * 0.6f
                    val dustAlpha = 0.3f + sin(t * 4f + p * 1.5f) * 0.15f
                    drawCircle(
                        color = WindDust.copy(alpha = dustAlpha),
                        radius = 1.5f + sin(t * 6f + p * 2f) * 0.5f,
                        center = Offset(dx2, dy2),
                    )
                }
            }
        }

        // ── Death Effects ──
        val effects = deathEffects.value
        for (de in effects) {
            val progress = 1f - (de.timer / 0.5f) // 0 -> 1 as timer goes 0.5 -> 0
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val dx = de.x * w
            val dy = de.y * h

            for (p in de.particles) {
                val px = dx + p.vx * progress
                val py = dy + p.vy * progress + 20f * progress * progress // gravity
                val pColor = DeathParticleColors[p.colorIdx % 5]
                val pSize = p.size * (1f - progress * 0.5f)
                drawCircle(
                    color = pColor.copy(alpha = alpha * 0.7f),
                    radius = pSize,
                    center = Offset(px, py),
                )
            }

            // Fading center flash
            if (progress < 0.3f) {
                val flashAlpha = (0.3f - progress) / 0.3f * 0.5f
                drawCircle(
                    color = Color.White.copy(alpha = flashAlpha),
                    radius = 20f * (1f + progress),
                    center = Offset(dx, dy),
                )
            }
        }
    }
}
