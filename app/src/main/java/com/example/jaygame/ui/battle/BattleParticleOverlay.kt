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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.jaygame.bridge.BattleBridge
import kotlin.math.cos
import kotlin.math.sin

// Pre-allocated particle colors
private val ParticleGold = Color(0xFFD4A847)
private val ParticleCyan = Color(0xFF5BA4CF)
private val ParticleWhite = Color(0xFFF0E6D3)
private val ParticleRed = Color(0xFFFF4444)
private val ParticleGreen = Color(0xFF6DBF67)
private val ParticlePurple = Color(0xFFCE93D8)

private val GradeParticleColors = arrayOf(
    Color(0xFF9E9E9E), // Common - gray
    Color(0xFF42A5F5), // Rare - blue
    Color(0xFFAB47BC), // Hero - purple
    Color(0xFFFF8F00), // Legend - orange
    Color(0xFFEF4444), // Ancient - red
    Color(0xFFFBBF24), // Mythic - gold
    Color(0xFFF0ABFC), // Immortal - pink
)

/**
 * Compose particle data: lightweight struct for per-frame update.
 */
private class ComposeParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var maxLife: Float,
    var size: Float,
    var sizeEnd: Float,
    var color: Color,
    var colorEnd: Color,
    var gravity: Float = 0f,
)

private const val MAX_PARTICLES = 200

/**
 * Compose-only particle overlay for battle visual effects:
 * summon sparkles, merge bursts, level-up indicators.
 * Listens to BattleBridge events and spawns particles in response.
 */
@Composable
fun BattleParticleOverlay() {
    val particles = remember { mutableListOf<ComposeParticle>() }
    val animTime = remember { mutableFloatStateOf(0f) }

    // Watch summon/merge/enemy events to spawn particles
    val summonResult by BattleBridge.summonResult.collectAsState()
    val mergeEffect by BattleBridge.mergeEffect.collectAsState()
    val enemies by BattleBridge.enemyPositions.collectAsState()

    // Track previous values to detect new events
    val prevSummonResult = remember { mutableStateOf(summonResult) }
    val prevMergeEffect = remember { mutableStateOf(mergeEffect) }
    val prevEnemyCount = remember { mutableStateOf(0) }
    val prevEnemyXs = remember { mutableStateOf(FloatArray(0)) }
    val prevEnemyYs = remember { mutableStateOf(FloatArray(0)) }

    // Detect enemy deaths → spawn soul particles flying to SP bar (bottom center)
    val curEnemyCount = enemies.count
    if (curEnemyCount < prevEnemyCount.value && prevEnemyCount.value > 0) {
        val oldXs = prevEnemyXs.value
        val oldYs = prevEnemyYs.value
        val oldCount = prevEnemyCount.value

        for (oi in 0 until oldCount.coerceAtMost(oldXs.size)) {
            var found = false
            for (ni in 0 until curEnemyCount) {
                val dx = oldXs[oi] - enemies.xs[ni]
                val dy = oldYs[oi] - enemies.ys[ni]
                if (dx * dx + dy * dy < 0.003f) { found = true; break }
            }
            if (!found && particles.size < MAX_PARTICLES - 3) {
                // Soul particle: flies toward bottom center (SP bar at ~0.5, 0.95)
                val spTargetX = 0.5f
                val spTargetY = 0.95f
                val dx = spTargetX - oldXs[oi]
                val dy = spTargetY - oldYs[oi]
                for (s in 0 until 3) {
                    val speed = 300f + s * 80f
                    val jitterX = sin(oi.toFloat() + s * 2f) * 40f
                    val jitterY = cos(oi.toFloat() + s * 1.5f) * 20f
                    particles.add(ComposeParticle(
                        x = oldXs[oi], y = oldYs[oi],
                        vx = dx * speed + jitterX,
                        vy = dy * speed + jitterY,
                        life = 0.5f + s * 0.1f,
                        maxLife = 0.5f + s * 0.1f,
                        size = 3f - s * 0.5f,
                        sizeEnd = 1f,
                        color = ParticleCyan.copy(alpha = 0.9f),
                        colorEnd = ParticleWhite.copy(alpha = 0f),
                    ))
                }
            }
        }
    }
    if (curEnemyCount > 0) {
        prevEnemyXs.value = enemies.xs.copyOf(curEnemyCount)
        prevEnemyYs.value = enemies.ys.copyOf(curEnemyCount)
    }
    prevEnemyCount.value = curEnemyCount

    // Spawn summon particles
    if (summonResult != null && summonResult != prevSummonResult.value) {
        prevSummonResult.value = summonResult
        val grade = summonResult!!.grade
        val color = GradeParticleColors.getOrElse(grade) { ParticleWhite }
        val count = (8 + grade * 4).coerceAtMost(30)
        for (i in 0 until count) {
            if (particles.size >= MAX_PARTICLES) break
            val angle = (i.toFloat() / count) * 6.283f
            val speed = 60f + grade * 15f + (i % 3) * 20f
            particles.add(ComposeParticle(
                x = 0.5f, y = 0.5f, // center of screen (normalized)
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = 0.8f + (i % 3) * 0.2f,
                maxLife = 0.8f + (i % 3) * 0.2f,
                size = 3f + grade * 0.5f,
                sizeEnd = 0.5f,
                color = color,
                colorEnd = color.copy(alpha = 0f),
                gravity = 30f,
            ))
        }
    }

    // Spawn merge particles
    if (mergeEffect != null && mergeEffect != prevMergeEffect.value) {
        prevMergeEffect.value = mergeEffect
        val isLucky = mergeEffect!!.isLucky
        val color = if (isLucky) ParticleGold else ParticleCyan
        val count = if (isLucky) 24 else 12
        for (i in 0 until count) {
            if (particles.size >= MAX_PARTICLES) break
            val angle = (i.toFloat() / count) * 6.283f
            val speed = 50f + if (isLucky) 40f else 0f
            particles.add(ComposeParticle(
                x = 0.5f, y = 0.5f,
                vx = cos(angle) * speed + sin(i.toFloat()) * 10f,
                vy = sin(angle) * speed - 20f,
                life = 0.6f + (i % 4) * 0.15f,
                maxLife = 0.6f + (i % 4) * 0.15f,
                size = if (isLucky) 4f else 2.5f,
                sizeEnd = 0.5f,
                color = color,
                colorEnd = color.copy(alpha = 0f),
                gravity = if (isLucky) 40f else 20f,
            ))
        }
    }

    // Ambient floating particles (very subtle)
    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { _ ->
                val dt = 1f / 60f
                animTime.floatValue += dt

                // Spawn occasional ambient sparkle
                if (particles.size < MAX_PARTICLES && (animTime.floatValue * 60f).toInt() % 30 == 0) {
                    val x = 0.2f + (animTime.floatValue * 1.7f % 0.6f)
                    val y = 0.3f + sin(animTime.floatValue * 0.5f) * 0.15f
                    particles.add(ComposeParticle(
                        x = x, y = y,
                        vx = sin(animTime.floatValue) * 5f,
                        vy = -15f,
                        life = 1.5f,
                        maxLife = 1.5f,
                        size = 1.5f,
                        sizeEnd = 0.3f,
                        color = ParticleWhite.copy(alpha = 0.2f),
                        colorEnd = ParticleWhite.copy(alpha = 0f),
                    ))
                }

                // Update particles
                val iter = particles.iterator()
                while (iter.hasNext()) {
                    val p = iter.next()
                    p.life -= dt
                    if (p.life <= 0f) {
                        iter.remove()
                        continue
                    }
                    p.x += p.vx * dt / 1000f // vx is in pixels, but x is normalized
                    p.y += p.vy * dt / 1000f
                    p.vy += p.gravity * dt
                }
            }
        }
    }

    if (particles.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        for (p in particles) {
            val lifeFrac = (p.life / p.maxLife).coerceIn(0f, 1f)
            val curSize = p.sizeEnd + (p.size - p.sizeEnd) * lifeFrac
            val alpha = lifeFrac.coerceIn(0f, 1f)

            val screenX = p.x * w
            val screenY = p.y * h

            // Interpolate color
            val curColor = Color(
                red = p.colorEnd.red + (p.color.red - p.colorEnd.red) * lifeFrac,
                green = p.colorEnd.green + (p.color.green - p.colorEnd.green) * lifeFrac,
                blue = p.colorEnd.blue + (p.color.blue - p.colorEnd.blue) * lifeFrac,
                alpha = (p.color.alpha * alpha).coerceIn(0f, 1f),
            )

            drawCircle(
                color = curColor,
                radius = curSize,
                center = Offset(screenX, screenY),
            )
        }
    }
}
