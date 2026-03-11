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

/** Death effect particle data */
private data class DeathEffect(
    val x: Float, val y: Float, val type: Int,
    var timer: Float = 0.5f,
    val particles: List<DeathParticle>,
)

private data class DeathParticle(
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

            val spriteSize = if (isBoss) 54f else 36f
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
            // HP fill
            val clampedHp = hpRatio.coerceIn(0f, 1f)
            val hpColor = Color(
                red = 1f - clampedHp,
                green = clampedHp,
                blue = 0.1f,
                alpha = 0.9f,
            )
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
