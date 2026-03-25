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

// Pre-allocated soul particle colors (avoid .copy() in spawn hot path)
private val SoulParticleColor = Color(0xFF5BA4CF).copy(alpha = 0.9f)
private val SoulParticleColorEnd = Color(0xFFF0E6D3).copy(alpha = 0f)

// Pre-allocated merge particle colorEnd (avoid .copy() per spawn)
private val MergeGoldColorEnd = Color(0xFFD4A847).copy(alpha = 0f)
private val MergeCyanColorEnd = Color(0xFF5BA4CF).copy(alpha = 0f)

// Pre-allocated ambient particle colors
private val AmbientParticleColor = Color(0xFFF0E6D3).copy(alpha = 0.2f)
private val AmbientParticleColorEnd = Color(0xFFF0E6D3).copy(alpha = 0f)

private val GradeParticleColors = arrayOf(
    Color(0xFF9E9E9E), // Common - gray
    Color(0xFF42A5F5), // Rare - blue
    Color(0xFFAB47BC), // Hero - purple
    Color(0xFFFF8F00), // Legend - orange
    Color(0xFFFBBF24), // Mythic - gold
)

// Pre-allocated transparent versions for colorEnd (avoid .copy() per spawn)
private val GradeParticleColorsEnd = Array(GradeParticleColors.size) { i ->
    GradeParticleColors[i].copy(alpha = 0f)
}

/**
 * Compose particle data: lightweight struct for per-frame update.
 * Pooled — fields are reset inline rather than creating new objects.
 */
private class ComposeParticle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var life: Float = 0f,
    var maxLife: Float = 0f,
    var size: Float = 0f,
    var sizeEnd: Float = 0f,
    var color: Color = Color.Transparent,
    var colorEnd: Color = Color.Transparent,
    var gravity: Float = 0f,
    var alive: Boolean = false,
)

private const val MAX_PARTICLES = 200

/**
 * Compose-only particle overlay for battle visual effects:
 * summon sparkles, merge bursts, level-up indicators.
 * Listens to BattleBridge events and spawns particles in response.
 *
 * Uses pre-allocated object pool to avoid per-frame GC pressure.
 * Integrates with [ParticleLOD] for automatic quality reduction under load.
 */
@Composable
fun BattleParticleOverlay() {
    // Pre-allocated particle pool — no allocations during gameplay
    val pool = remember {
        Array(MAX_PARTICLES) { ComposeParticle() }
    }
    val animTime = remember { mutableFloatStateOf(0f) }
    val activeCount = remember { mutableStateOf(0) }

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

    // Helper to acquire a particle from the pool
    fun acquireParticle(): ComposeParticle? {
        for (p in pool) {
            if (!p.alive) {
                p.alive = true
                return p
            }
        }
        return null
    }

    // Update LOD based on current active particle count
    ParticleLOD.updateLOD(activeCount.value)

    // Detect enemy deaths -> spawn soul particles flying to SP bar (bottom center)
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
            if (!found) {
                // Soul particle: flies toward bottom center (SP bar at ~0.5, 0.95)
                val spTargetX = 0.5f
                val spTargetY = 0.95f
                val dx = spTargetX - oldXs[oi]
                val dy = spTargetY - oldYs[oi]
                for (s in 0 until 3) {
                    val p = acquireParticle() ?: break
                    val speed = 300f + s * 80f
                    val jitterX = sin(oi.toFloat() + s * 2f) * 40f
                    val jitterY = cos(oi.toFloat() + s * 1.5f) * 20f
                    p.x = oldXs[oi]; p.y = oldYs[oi]
                    p.vx = dx * speed + jitterX
                    p.vy = dy * speed + jitterY
                    p.life = 0.5f + s * 0.1f
                    p.maxLife = 0.5f + s * 0.1f
                    p.size = 3f - s * 0.5f
                    p.sizeEnd = 1f
                    p.color = SoulParticleColor
                    p.colorEnd = SoulParticleColorEnd
                    p.gravity = 0f
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
    val sr = summonResult
    if (sr != null && sr != prevSummonResult.value) {
        prevSummonResult.value = sr
        val grade = sr.grade
        val color = GradeParticleColors.getOrElse(grade) { ParticleWhite }
        val colorEnd = GradeParticleColorsEnd.getOrElse(grade) { AmbientParticleColorEnd }
        val count = (8 + grade * 4).coerceAtMost(30)
        for (i in 0 until count) {
            val p = acquireParticle() ?: break
            val angle = (i.toFloat() / count) * 6.283f
            val speed = 60f + grade * 15f + (i % 3) * 20f
            p.x = 0.5f; p.y = 0.5f
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed
            p.life = 0.8f + (i % 3) * 0.2f
            p.maxLife = 0.8f + (i % 3) * 0.2f
            p.size = 3f + grade * 0.5f
            p.sizeEnd = 0.5f
            p.color = color
            p.colorEnd = colorEnd
            p.gravity = 30f
        }
    }

    // Spawn merge particles
    val me = mergeEffect
    if (me != null && me != prevMergeEffect.value) {
        prevMergeEffect.value = me
        val isLucky = me.isLucky
        val color = if (isLucky) ParticleGold else ParticleCyan
        val colorEnd = if (isLucky) MergeGoldColorEnd else MergeCyanColorEnd
        val count = if (isLucky) 24 else 12
        for (i in 0 until count) {
            val p = acquireParticle() ?: break
            val angle = (i.toFloat() / count) * 6.283f
            val speed = 50f + if (isLucky) 40f else 0f
            p.x = 0.5f; p.y = 0.5f
            p.vx = cos(angle) * speed + sin(i.toFloat()) * 10f
            p.vy = sin(angle) * speed - 20f
            p.life = 0.6f + (i % 4) * 0.15f
            p.maxLife = 0.6f + (i % 4) * 0.15f
            p.size = if (isLucky) 4f else 2.5f
            p.sizeEnd = 0.5f
            p.color = color
            p.colorEnd = colorEnd
            p.gravity = if (isLucky) 40f else 20f
        }
    }

    // Ambient floating particles (very subtle)
    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { _ ->
                val dt = 1f / 60f
                animTime.floatValue += dt

                // Spawn occasional ambient sparkle
                if ((animTime.floatValue * 60f).toInt() % 30 == 0) {
                    val p = acquireParticle()
                    if (p != null) {
                        val x = 0.2f + (animTime.floatValue * 1.7f % 0.6f)
                        val y = 0.3f + sin(animTime.floatValue * 0.5f) * 0.15f
                        p.x = x; p.y = y
                        p.vx = sin(animTime.floatValue) * 5f
                        p.vy = -15f
                        p.life = 1.5f
                        p.maxLife = 1.5f
                        p.size = 1.5f
                        p.sizeEnd = 0.3f
                        p.color = AmbientParticleColor
                        p.colorEnd = AmbientParticleColorEnd
                        p.gravity = 0f
                    }
                }

                // Update particles and count active ones
                var count = 0
                for (p in pool) {
                    if (!p.alive) continue
                    p.life -= dt
                    if (p.life <= 0f) {
                        p.alive = false
                        continue
                    }
                    p.x += p.vx * dt / 1000f
                    p.y += p.vy * dt / 1000f
                    p.vy += p.gravity * dt
                    count++
                }
                activeCount.value = count
            }
        }
    }

    if (activeCount.value == 0) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        var drawIndex = 0

        for (p in pool) {
            if (!p.alive) continue

            // LOD: skip particles based on current quality level
            if (ParticleLOD.shouldSkipParticle(drawIndex)) {
                drawIndex++
                continue
            }
            drawIndex++

            val lifeFrac = (p.life / p.maxLife).coerceIn(0f, 1f)
            val curSize = p.sizeEnd + (p.size - p.sizeEnd) * lifeFrac
            val alpha = lifeFrac.coerceIn(0f, 1f)

            val screenX = p.x * w
            val screenY = p.y * h

            // Interpolate color — using lerp on pre-allocated Color components
            // This still creates a Color value (inline class on JVM), but avoids
            // the overhead of .copy() calls with named params
            val r = p.colorEnd.red + (p.color.red - p.colorEnd.red) * lifeFrac
            val g = p.colorEnd.green + (p.color.green - p.colorEnd.green) * lifeFrac
            val b = p.colorEnd.blue + (p.color.blue - p.colorEnd.blue) * lifeFrac
            val a = (p.color.alpha * alpha).coerceIn(0f, 1f)

            drawCircle(
                color = Color(red = r, green = g, blue = b, alpha = a),
                radius = curSize,
                center = Offset(screenX, screenY),
            )
        }
    }
}
