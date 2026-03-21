package com.example.jaygame.ui.battle

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.jaygame.bridge.BattleBridge
import kotlin.math.*

// ═══════════════════════════════════════════════════════
// Pre-allocated color constants (avoid per-frame allocation)
// ═══════════════════════════════════════════════════════

// Fire
private val FireCore = Color(0xFFFF6B35)
private val FireGlow = Color(0xFFFF9800)
private val FireBright = Color(0xFFFFDD44)
private val FireDark = Color(0xFFFF4400)
private val FireWhiteHot = Color(0xFFFFFFCC)
private val FireInferno = Color(0xFFFF2200)

// Frost
private val FrostBase = Color(0xFF64B5F6)
private val FrostGlow = Color(0xFF90CAF9)
private val FrostBright = Color(0xFFE3F2FD)
private val FrostDeep = Color(0xFF0288D1)
private val FrostCrystal = Color(0xFFB3E5FC)

// Poison
private val PoisonBase = Color(0xFF81C784)
private val PoisonGlow = Color(0xFFA5D6A7)
private val PoisonDark = Color(0xFF388E3C)
private val PoisonNeon = Color(0xFF00E676)
private val PoisonAcid = Color(0xFFCCFF00)

// Lightning
private val LightBase = Color(0xFFFFD54F)
private val LightGlow = Color(0xFFFFFF00)
private val LightWhite = Color(0xFFFFFDE7)
private val LightArc = Color(0xFFFFEE58)

// Support
private val SupportGold = Color(0xFFD4A847)
private val SupportHoly = Color(0xFFFFF8E1)
private val SupportDivine = Color(0xFFFFF3E0)

// Wind
private val WindTeal = Color(0xFF80CBC4)
private val WindCyan = Color(0xFF4DD0E1)
private val WindLight = Color(0xFFB2EBF2)
private val WindDark = Color(0xFF00695C)

// Common
private val White = Color.White

// ── Pre-allocated alpha variants (avoid .copy() in hot path) ──

// Fire alpha
private val FireGlow50 = FireGlow.copy(alpha = 0.5f)
private val FireWhiteHot40 = FireWhiteHot.copy(alpha = 0.4f)
private val FireInferno20 = FireInferno.copy(alpha = 0.2f)

// Frost alpha
private val FrostGlow20 = FrostGlow.copy(alpha = 0.2f)
private val FrostBright30 = FrostBright.copy(alpha = 0.3f)
private val FrostDeep15 = FrostDeep.copy(alpha = 0.15f)
private val FrostCrystal30 = FrostCrystal.copy(alpha = 0.3f)
private val FrostCrystal50 = FrostCrystal.copy(alpha = 0.5f)

// Poison alpha
private val PoisonNeon20 = PoisonNeon.copy(alpha = 0.2f)
private val PoisonDark10 = PoisonDark.copy(alpha = 0.1f)
private val PoisonAcid50 = PoisonAcid.copy(alpha = 0.5f)
private val PoisonAcid60 = PoisonAcid.copy(alpha = 0.6f)
private val PoisonAcid70 = PoisonAcid.copy(alpha = 0.7f)
private val PoisonAcid80 = PoisonAcid.copy(alpha = 0.8f)
private val PoisonBase40 = PoisonBase.copy(alpha = 0.4f)
private val PoisonBase25 = PoisonBase.copy(alpha = 0.25f)
private val PoisonNeon25 = PoisonNeon.copy(alpha = 0.25f)
private val PoisonNeon15 = PoisonNeon.copy(alpha = 0.15f)

// Lightning alpha
private val LightGlow20 = LightGlow.copy(alpha = 0.2f)
private val LightArc40 = LightArc.copy(alpha = 0.4f)
private val White60 = White.copy(alpha = 0.6f)

// Support alpha
private val SupportHoly35 = SupportHoly.copy(alpha = 0.35f)
private val SupportGold30 = SupportGold.copy(alpha = 0.3f)
private val SupportGold40 = SupportGold.copy(alpha = 0.4f)
private val SupportGold80 = SupportGold.copy(alpha = 0.8f)
private val SupportDivine15 = SupportDivine.copy(alpha = 0.15f)
private val SupportHoly50 = SupportHoly.copy(alpha = 0.5f)

// Wind alpha
private val WindCyan20 = WindCyan.copy(alpha = 0.2f)
private val WindDark10 = WindDark.copy(alpha = 0.1f)

// Pre-allocated Stroke objects (avoid per-frame allocation)
private val StrokeW1 = Stroke(width = 1f)
private val StrokeW1_5 = Stroke(width = 1.5f)
private val StrokeW2 = Stroke(width = 2f)

// Prismatic (Support grade 6) — pre-allocated lists
private val PrismColors = listOf(
    Color(0xFFFF0000), Color(0xFFFF7700), Color(0xFFFFFF00),
    Color(0xFF00FF00), Color(0xFF0077FF), Color(0xFF0000FF), Color(0xFF8800FF)
)
private val PrismColors50 = PrismColors.map { it.copy(alpha = 0.5f) }

// Rainbow shimmer (Support grade 4)
private val RainbowColors40 = listOf(
    Color(0xFFFF6B6B).copy(alpha = 0.4f), Color(0xFFFFD93D).copy(alpha = 0.4f),
    Color(0xFF6BCB77).copy(alpha = 0.4f), Color(0xFF4D96FF).copy(alpha = 0.4f),
    Color(0xFFAB46D2).copy(alpha = 0.4f),
)

@Composable
fun ProjectileOverlay() {
    val projData by BattleBridge.projectiles.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "projFx")
    val fxTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fxTime",
    )

    val lightningPath = remember { Path() }
    val shapePath = remember { Path() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val data = projData
        val w = size.width
        val h = size.height
        for (i in 0 until data.count) {
            val srcX = data.srcXs[i] * w
            val srcY = data.srcYs[i] * h
            val curX = data.dstXs[i] * w   // current projectile position
            val curY = data.dstYs[i] * h
            // Use direct family/grade arrays from engine (blueprint-aware)
            val family = if (i < data.families.size) data.families[i] else 0
            val grade = if (i < data.grades.size) data.grades[i] else 0

            when (family) {
                0 -> drawFireProjectile(srcX, srcY, curX, curY, grade, fxTime, i, shapePath)
                1 -> drawFrostProjectile(srcX, srcY, curX, curY, grade, fxTime, i, shapePath)
                2 -> drawPoisonProjectile(srcX, srcY, curX, curY, grade, fxTime, i, shapePath)
                3 -> drawLightningProjectile(srcX, srcY, curX, curY, grade, fxTime, i, lightningPath)
                4 -> drawSupportProjectile(srcX, srcY, curX, curY, grade, fxTime, i, shapePath)
                5 -> drawWindProjectile(srcX, srcY, curX, curY, grade, fxTime, i, shapePath)
                else -> drawDefaultProjectile(srcX, srcY, curX, curY, fxTime)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Shared helpers
// ═══════════════════════════════════════════════════════

/** Direction angle from src to current pos (radians) */
private fun dirAngle(srcX: Float, srcY: Float, curX: Float, curY: Float): Float {
    return atan2(curY - srcY, curX - srcX)
}

/** Distance from src to current pos */
private fun dist(srcX: Float, srcY: Float, curX: Float, curY: Float): Float {
    val dx = curX - srcX; val dy = curY - srcY
    return sqrt(dx * dx + dy * dy)
}

/** Layered glow — concentric circles approximating radial gradient (no heap alloc).
 *  Uses full-alpha base colors; alpha controlled by parameters only. */
private fun DrawScope.drawGlow(
    cx: Float, cy: Float, radius: Float,
    innerColor: Color, innerAlpha: Float,
    outerColor: Color, outerAlpha: Float,
) {
    drawCircle(outerColor, alpha = outerAlpha, radius = radius, center = Offset(cx, cy))
    drawCircle(innerColor, alpha = innerAlpha, radius = radius * 0.6f, center = Offset(cx, cy))
    drawCircle(innerColor, alpha = innerAlpha * 0.5f, radius = radius * 0.3f, center = Offset(cx, cy))
}

/** Draw trail particles fading behind the projectile */
private fun DrawScope.drawTrail(
    srcX: Float, srcY: Float, curX: Float, curY: Float,
    count: Int, color: Color, maxRadius: Float,
    spread: Float, time: Float, seed: Int,
) {
    val d = dist(srcX, srcY, curX, curY)
    if (d < 2f) return
    val dirX = (curX - srcX) / d
    val dirY = (curY - srcY) / d
    val perpX = -dirY
    val perpY = dirX
    val trailLen = minOf(d, maxRadius * 12f)
    for (j in 0 until count) {
        if (ParticleLOD.shouldSkipParticle(j)) continue
        val t = (j + 1).toFloat() / (count + 1)
        val wobble = sin(t * 8f + time * 6f + seed + j * 1.7f) * spread * t
        val x = curX - dirX * trailLen * t + perpX * wobble
        val y = curY - dirY * trailLen * t + perpY * wobble
        val alpha = (1f - t) * 0.7f
        val r = maxRadius * (1f - t * 0.6f)
        drawCircle(color, alpha = alpha, radius = r, center = Offset(x, y))
    }
}

/** Draw orbiting particles — reusable for all families */
private fun DrawScope.drawOrbit(
    cx: Float, cy: Float, count: Int, radius: Float,
    color: Color, alpha: Float, particleR: Float,
    time: Float, speed: Float, seed: Float = 0f,
) {
    val step = 2f * PI.toFloat() / count
    for (s in 0 until count) {
        if (ParticleLOD.shouldSkipParticle(s)) continue
        val sa = s * step + time * speed + seed
        drawCircle(color, alpha = alpha, radius = particleR, center = Offset(
            cx + cos(sa) * radius, cy + sin(sa) * radius
        ))
    }
}

/** Draw a rotated N-pointed star at position */
private fun DrawScope.drawStar(
    cx: Float, cy: Float, points: Int, outerR: Float, innerR: Float,
    rotation: Float, color: Color, alpha: Float, path: Path,
) {
    path.reset()
    val step = PI.toFloat() / points
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerR else innerR
        val angle = i * step + rotation - PI.toFloat() / 2f
        val x = cx + cos(angle) * r
        val y = cy + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, alpha = alpha)
}

/** Draw a rotated diamond (rhombus) — no array allocation */
private fun DrawScope.drawDiamond(
    cx: Float, cy: Float, w: Float, h: Float,
    rotation: Float, color: Color, alpha: Float, path: Path,
) {
    path.reset()
    val cosR = cos(rotation); val sinR = sin(rotation)
    val hw = w / 2f; val hh = h / 2f
    // top (0, -hh)
    path.moveTo(-hh * sinR + cx, -hh * cosR + cy)
    // right (hw, 0) — note: local x rotated
    path.lineTo(hw * cosR + cx, hw * sinR + cy)
    // bottom (0, hh)
    path.lineTo(hh * sinR + cx, hh * cosR + cy)
    // left (-hw, 0)
    path.lineTo(-hw * cosR + cx, -hw * sinR + cy)
    path.close()
    drawPath(path, color, alpha = alpha)
}

/** Draw a crescent / arc blade shape (reduced segments for perf) */
private fun DrawScope.drawCrescent(
    cx: Float, cy: Float, radius: Float, thickness: Float,
    rotation: Float, color: Color, alpha: Float, path: Path,
) {
    path.reset()
    val segments = 8  // reduced from 12 for performance
    val arcStart = rotation - PI.toFloat() / 3f
    val arcSpan = 2f * PI.toFloat() / 3f
    // outer arc
    for (i in 0..segments) {
        val a = arcStart + (i.toFloat() / segments) * arcSpan
        val x = cx + cos(a) * radius
        val y = cy + sin(a) * radius
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    // inner arc (reverse)
    val innerR = radius - thickness
    for (i in segments downTo 0) {
        val a = arcStart + (i.toFloat() / segments) * arcSpan
        val x = cx + cos(a) * innerR
        val y = cy + sin(a) * innerR
        path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, alpha = alpha)
}

// ═══════════════════════════════════════════════════════
// FIRE — Comet-like fireball, elongated along travel direction
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawFireProjectile(
    srcX: Float, srcY: Float, curX: Float, curY: Float,
    grade: Int, time: Float, seed: Int, path: Path,
) {
    val angle = dirAngle(srcX, srcY, curX, curY)
    val baseSize = 10f + grade * 4f
    val trailCount = 5 + grade * 3

    // Flickering flame tail (spread outward behind projectile)
    val d = dist(srcX, srcY, curX, curY)
    if (d > 2f) {
        val dirX = (curX - srcX) / d; val dirY = (curY - srcY) / d
        val perpX = -dirY; val perpY = dirX
        val tailLen = minOf(d, baseSize * 14f)
        for (j in 0 until trailCount) {
            if (ParticleLOD.shouldSkipParticle(j)) continue
            val t = (j + 1).toFloat() / (trailCount + 1)
            val flicker = sin(t * 10f + time * 18f + seed + j * 2.3f) * (4f + grade * 2f) * t
            val spreadWobble = sin(t * 6f + time * 12f + j * 3.1f) * (3f + grade) * t
            val x = curX - dirX * tailLen * t + perpX * (flicker + spreadWobble)
            val y = curY - dirY * tailLen * t + perpY * (flicker + spreadWobble)
            val alpha = (1f - t) * 0.7f
            val r = baseSize * 0.8f * (1f - t * 0.7f)
            if (t < 0.4f) {
                drawCircle(FireCore, alpha = alpha, radius = r, center = Offset(x, y))
            } else {
                drawCircle(FireGlow, alpha = alpha * 0.7f, radius = r * 0.8f, center = Offset(x, y))
            }
        }
    }

    // Glow around fireball head
    val glowR = baseSize * (2f + grade * 0.4f)
    drawGlow(curX, curY, glowR, FireBright, 0.3f, FireGlow, 0.15f)

    when {
        grade <= 2 -> {
            val pulse = 1f + sin(time * 12f + seed) * 0.15f
            // Elongated fireball head: draw rotated oval via scale + rotate
            val headW = baseSize * 1.8f * pulse
            val headH = baseSize * 1.0f * pulse
            // Draw elongated shape using a diamond (approximation of oval)
            drawDiamond(curX, curY, headW, headH, angle, FireDark, 1.0f, path)
            drawDiamond(curX, curY, headW * 0.7f, headH * 0.7f, angle, FireCore, 0.9f, path)
            drawDiamond(curX, curY, headW * 0.35f, headH * 0.4f, angle, FireBright, 0.8f, path)
            // Spark embers around head
            drawOrbit(curX, curY, 2 + grade, baseSize * 1.2f, FireBright, 0.6f, 2f, time, 8f, angle)
        }
        grade <= 4 -> {
            val pulse = 1f + sin(time * 10f + seed) * 0.1f
            val headW = baseSize * 2.2f * pulse
            val headH = baseSize * 1.1f * pulse
            // Outer inferno layer
            drawDiamond(curX, curY, headW * 1.3f, headH * 1.3f, angle, FireInferno, 0.5f, path)
            // Main fireball head
            drawDiamond(curX, curY, headW, headH, angle, FireDark, 1.0f, path)
            drawStar(curX, curY, 4 + grade - 3, baseSize * 0.8f, baseSize * 0.4f,
                time * 6f + seed, FireBright, 0.8f, path)
            drawDiamond(curX, curY, headW * 0.3f, headH * 0.3f, angle, FireWhiteHot, 0.6f, path)
            drawOrbit(curX, curY, 4 + grade, baseSize * 1.5f, FireGlow, 0.7f, 2.5f, time, 5f)
        }
        grade == 5 -> {
            val pulse = 1f + sin(time * 8f + seed) * 0.08f
            val headW = baseSize * 2.6f * pulse
            val headH = baseSize * 1.2f * pulse
            drawDiamond(curX, curY, headW * 1.4f, headH * 1.4f, angle, FireInferno, 0.4f, path)
            drawDiamond(curX, curY, headW, headH, angle, FireDark, 1.0f, path)
            drawStar(curX, curY, 6, baseSize * 0.9f, baseSize * 0.45f,
                time * 5f, FireBright, 0.85f, path)
            drawDiamond(curX, curY, headW * 0.35f, headH * 0.35f, angle, FireWhiteHot, 0.7f, path)
            // Triple orbital rings (with LOD)
            for (ring in 0 until 3) {
                val ringAngle = time * (4f + ring) + ring * 2.1f
                val ringR = baseSize * (1.4f + ring * 0.3f)
                drawOrbit(curX, curY, 5, ringR, FireGlow, 0.5f, 2f, time, 4f + ring, ringAngle)
            }
        }
        else -> {
            val pulse = 1f + sin(time * 6f + seed) * 0.06f
            val headW = baseSize * 3f * pulse
            val headH = baseSize * 1.4f * pulse
            // Corona glow
            drawCircle(FireInferno20, radius = baseSize * 3f, center = Offset(curX, curY))
            drawCircle(FireWhiteHot40, radius = baseSize * 2f, center = Offset(curX, curY))
            // Massive elongated fireball
            drawDiamond(curX, curY, headW * 1.3f, headH * 1.3f, angle, FireInferno, 0.5f, path)
            drawDiamond(curX, curY, headW, headH, angle, FireDark, 1.0f, path)
            drawStar(curX, curY, 8, baseSize, baseSize * 0.5f, time * 4f, FireBright, 0.9f, path)
            drawStar(curX, curY, 8, baseSize * 0.8f, baseSize * 0.4f, -time * 3f, FireWhiteHot, 0.7f, path)
            drawDiamond(curX, curY, headW * 0.3f, headH * 0.3f, angle, FireWhiteHot, 0.8f, path)
            // Solar flare tendrils (with LOD)
            for (t in 0 until 6) {
                if (ParticleLOD.shouldSkipParticle(t)) continue
                val ta = t * (PI.toFloat() / 3f) + time * 2f + seed
                val flareLen = baseSize * (1.8f + sin(time * 8f + t * 1.5f) * 0.5f)
                val endX = curX + cos(ta) * flareLen
                val endY = curY + sin(ta) * flareLen
                drawLine(FireGlow50, start = Offset(curX, curY), end = Offset(endX, endY),
                    strokeWidth = 3f, cap = StrokeCap.Round)
                drawCircle(FireBright, alpha = 0.6f, radius = 3f, center = Offset(endX, endY))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// FROST — Spinning ice shard / crystal with sparkle trail
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawFrostProjectile(
    srcX: Float, srcY: Float, curX: Float, curY: Float,
    grade: Int, time: Float, seed: Int, path: Path,
) {
    val angle = dirAngle(srcX, srcY, curX, curY)
    val baseSize = 9f + grade * 3.5f
    val trailCount = 3 + grade * 2

    // Sparkle trail — small diamonds that twinkle behind the shard
    val d = dist(srcX, srcY, curX, curY)
    if (d > 2f) {
        val dirX = (curX - srcX) / d; val dirY = (curY - srcY) / d
        val perpX = -dirY; val perpY = dirX
        val trailLen = minOf(d, baseSize * 12f)
        for (j in 0 until trailCount) {
            if (ParticleLOD.shouldSkipParticle(j)) continue
            val t = (j + 1).toFloat() / (trailCount + 1)
            val sparkle = sin(t * 12f + time * 20f + seed + j * 3.1f) * 3f * t
            val x = curX - dirX * trailLen * t + perpX * sparkle
            val y = curY - dirY * trailLen * t + perpY * sparkle
            val alpha = (1f - t) * 0.6f
            val sparkSize = baseSize * 0.3f * (1f - t * 0.5f)
            val sparkSpin = time * 15f + j * 2f
            drawDiamond(x, y, sparkSize, sparkSize * 1.4f, sparkSpin, FrostCrystal, alpha, path)
        }
    }

    // Cold aura glow (layered circles)
    val glowR = baseSize * (1.8f + grade * 0.3f)
    drawGlow(curX, curY, glowR, FrostBright, 0.25f, FrostGlow, 0.1f)

    when {
        grade <= 2 -> {
            // Prominent spinning diamond shard at ALL grades
            val spin = angle + time * 12f + seed
            // Sharp elongated diamond — the main shard
            drawDiamond(curX, curY, baseSize * 1.4f, baseSize * 2.6f, spin, FrostBase, 0.9f, path)
            drawDiamond(curX, curY, baseSize * 0.9f, baseSize * 1.8f, spin, FrostBright, 0.8f, path)
            drawDiamond(curX, curY, baseSize * 0.4f, baseSize * 0.8f, spin, White, 0.7f, path)
            // Small orbiting sparkles
            drawOrbit(curX, curY, 2 + grade, baseSize * 1.5f, White, 0.6f, 1.5f, time, 8f)
        }
        grade <= 4 -> {
            val spin = angle + time * 8f
            // Main crystal shard
            drawDiamond(curX, curY, baseSize * 1.6f, baseSize * 2.8f, spin, FrostBase, 0.85f, path)
            drawDiamond(curX, curY, baseSize * 1.0f, baseSize * 2.0f, spin, FrostBright, 0.75f, path)
            // Counter-rotating inner shard
            drawDiamond(curX, curY, baseSize * 0.6f, baseSize * 1.2f, -spin * 0.7f, White, 0.6f, path)
            drawCircle(White, alpha = 0.6f, radius = baseSize * 0.25f, center = Offset(curX, curY))
            // Ice shard orbits (with LOD)
            for (s in 0 until 3 + grade) {
                if (ParticleLOD.shouldSkipParticle(s)) continue
                val sa = s * (2f * PI.toFloat() / (3 + grade)) + time * 5f
                val sr = baseSize * 1.6f
                drawDiamond(
                    curX + cos(sa) * sr, curY + sin(sa) * sr,
                    4f, 7f, sa * 2f, FrostCrystal, 0.6f, path
                )
            }
        }
        grade == 5 -> {
            val spin = time * 6f
            drawCircle(FrostGlow20, radius = baseSize * 2f, center = Offset(curX, curY), style = StrokeW2)
            // Large crystal shard
            drawDiamond(curX, curY, baseSize * 1.8f, baseSize * 3.0f, spin, FrostBase, 0.8f, path)
            drawDiamond(curX, curY, baseSize * 1.2f, baseSize * 2.2f, -spin * 1.3f, FrostBright, 0.7f, path)
            drawCircle(FrostBright, alpha = 0.8f, radius = baseSize * 0.35f, center = Offset(curX, curY))
            // Snowflake arms (with LOD)
            for (arm in 0 until 6) {
                if (ParticleLOD.shouldSkipParticle(arm)) continue
                val a = arm * (PI.toFloat() / 3f) + spin
                val len = baseSize * 1.8f
                drawLine(FrostCrystal50, start = Offset(curX, curY),
                    end = Offset(curX + cos(a) * len, curY + sin(a) * len),
                    strokeWidth = 1.5f, cap = StrokeCap.Round)
                // Branch tips
                val midX = curX + cos(a) * len * 0.6f
                val midY = curY + sin(a) * len * 0.6f
                val bLen = len * 0.4f
                for (b in -1..1 step 2) {
                    val ba = a + b * 0.5f
                    drawLine(FrostCrystal30, start = Offset(midX, midY),
                        end = Offset(midX + cos(ba) * bLen, midY + sin(ba) * bLen),
                        strokeWidth = 1f, cap = StrokeCap.Round)
                }
            }
        }
        else -> {
            val spin = time * 4f
            // Layered glow
            drawCircle(FrostDeep15, radius = baseSize * 3f, center = Offset(curX, curY))
            drawCircle(FrostBright30, radius = baseSize * 2f, center = Offset(curX, curY))
            // Massive crystal shard complex
            drawDiamond(curX, curY, baseSize * 2.2f, baseSize * 3.5f, spin, FrostBase, 0.7f, path)
            drawDiamond(curX, curY, baseSize * 1.6f, baseSize * 2.8f, -spin * 1.5f, FrostBright, 0.6f, path)
            drawStar(curX, curY, 6, baseSize * 0.9f, baseSize * 0.3f, spin * 2f, White, 0.5f, path)
            drawCircle(White, alpha = 0.8f, radius = baseSize * 0.3f, center = Offset(curX, curY))
            drawCircle(FrostCrystal30, radius = baseSize * 1.8f, center = Offset(curX, curY), style = StrokeW1_5)
            drawCircle(FrostGlow20, radius = baseSize * 2.4f, center = Offset(curX, curY), style = StrokeW1)
        }
    }
}

// ═══════════════════════════════════════════════════════
// POISON — Bubbling toxic glob with dripping acid trail
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawPoisonProjectile(
    srcX: Float, srcY: Float, curX: Float, curY: Float,
    grade: Int, time: Float, seed: Int, path: Path,
) {
    val baseSize = 9f + grade * 3.5f
    val trailCount = 4 + grade * 3

    // Dripping acid trail — droplets that "fall" with gravity
    val d = dist(srcX, srcY, curX, curY)
    if (d > 2f) {
        val dirX = (curX - srcX) / d; val dirY = (curY - srcY) / d
        val trailLen = minOf(d, baseSize * 12f)
        for (j in 0 until trailCount) {
            if (ParticleLOD.shouldSkipParticle(j)) continue
            val t = (j + 1).toFloat() / (trailCount + 1)
            val wobble = sin(time * 4f + j * 2.3f + seed) * 3f
            // Gravity drip effect — droplets fall downward the further back they are
            val gravityDrop = t * t * 18f
            val x = curX - dirX * trailLen * t + wobble
            val y = curY - dirY * trailLen * t + gravityDrop
            val alpha = (1f - t) * 0.6f
            val r = baseSize * 0.4f * (1f - t * 0.4f)
            drawCircle(PoisonBase, alpha = alpha, radius = r, center = Offset(x, y))
            // Small acid drip below each droplet
            if (j % 2 == 0) {
                val dripLen = r * 1.5f * t
                drawLine(PoisonNeon, alpha = alpha * 0.5f,
                    start = Offset(x, y + r * 0.5f), end = Offset(x, y + r * 0.5f + dripLen),
                    strokeWidth = 1.5f, cap = StrokeCap.Round)
                drawCircle(PoisonNeon, alpha = alpha * 0.4f, radius = r * 0.3f,
                    center = Offset(x, y + r * 0.5f + dripLen))
            }
        }
    }

    // Toxic aura (layered circles)
    val glowR = baseSize * (1.5f + grade * 0.3f)
    drawGlow(curX, curY, glowR, PoisonNeon, 0.2f, PoisonDark, 0.1f)

    when {
        grade <= 2 -> {
            // Wobbling blob — center shifts with time
            val wobX = sin(time * 8f + seed) * baseSize * 0.15f
            val wobY = cos(time * 10f + seed * 1.3f) * baseSize * 0.15f
            val cx = curX + wobX; val cy = curY + wobY
            val pulse = 1f + sin(time * 10f + seed) * 0.2f
            // Main glob body
            drawCircle(PoisonDark, radius = baseSize * pulse, center = Offset(cx, cy))
            drawCircle(PoisonBase, alpha = 0.85f, radius = baseSize * 0.75f * pulse, center = Offset(cx, cy))
            drawCircle(PoisonNeon, alpha = 0.5f, radius = baseSize * 0.3f * pulse, center = Offset(cx, cy))
            // Bubbling surface bubbles (small circles at varying positions)
            for (b in 0 until 3 + grade * 2) {
                if (ParticleLOD.shouldSkipParticle(b)) continue
                val bPhase = time * 7f + b * 1.9f + seed
                val bRise = (sin(bPhase) * 0.5f + 0.5f) // 0..1 rising cycle
                val bAngle = b * 2.4f + seed
                val bDist = baseSize * (0.3f + bRise * 0.6f)
                val bx = cx + cos(bAngle + time * 3f) * bDist
                val by = cy + sin(bAngle + time * 3f) * bDist - bRise * baseSize * 0.4f
                val bSize = 1.5f + (1f - bRise) * 2f
                drawCircle(PoisonGlow, alpha = 0.5f * (1f - bRise), radius = bSize, center = Offset(bx, by))
                drawCircle(White, alpha = 0.3f * (1f - bRise), radius = bSize * 0.4f,
                    center = Offset(bx - 0.5f, by - 0.5f))
            }
        }
        grade <= 4 -> {
            val wobX = sin(time * 6f + seed) * baseSize * 0.1f
            val wobY = cos(time * 8f + seed * 1.3f) * baseSize * 0.1f
            val cx = curX + wobX; val cy = curY + wobY
            val pulse = 1f + sin(time * 8f + seed) * 0.12f
            drawCircle(PoisonDark, alpha = 0.7f, radius = baseSize * 1.2f * pulse, center = Offset(cx, cy))
            drawCircle(PoisonBase, radius = baseSize * 0.9f * pulse, center = Offset(cx, cy))
            drawStar(cx, cy, 5, baseSize * 0.7f, baseSize * 0.35f,
                time * 4f + seed, PoisonNeon, 0.6f, path)
            drawCircle(PoisonAcid50, radius = baseSize * 0.25f, center = Offset(cx, cy))
            // Orbiting spores (with LOD)
            for (s in 0 until 3 + grade) {
                if (ParticleLOD.shouldSkipParticle(s)) continue
                val sa = s * (2f * PI.toFloat() / (3 + grade)) + time * 3f
                val sr = baseSize * (1.4f + sin(time * 5f + s) * 0.2f)
                val sporeSize = 3f + sin(time * 7f + s * 1.3f) * 1f
                val ox = cx + cos(sa) * sr; val oy = cy + sin(sa) * sr
                drawCircle(PoisonBase, alpha = 0.7f, radius = sporeSize, center = Offset(ox, oy))
                drawCircle(PoisonNeon, alpha = 0.3f, radius = sporeSize * 0.5f, center = Offset(ox, oy))
            }
        }
        grade == 5 -> {
            val wobX = sin(time * 5f + seed) * baseSize * 0.08f
            val wobY = cos(time * 7f + seed * 1.3f) * baseSize * 0.08f
            val cx = curX + wobX; val cy = curY + wobY
            val pulse = 1f + sin(time * 6f + seed) * 0.08f
            drawCircle(PoisonDark, alpha = 0.6f, radius = baseSize * 1.5f * pulse, center = Offset(cx, cy))
            drawCircle(PoisonBase, radius = baseSize * 1.1f * pulse, center = Offset(cx, cy))
            drawStar(cx, cy, 5, baseSize * 0.9f, baseSize * 0.4f,
                time * 3f, PoisonNeon, 0.7f, path)
            drawCircle(PoisonAcid60, radius = baseSize * 0.3f, center = Offset(cx, cy))
            // Miasma tendrils (with LOD)
            for (t in 0 until 5) {
                if (ParticleLOD.shouldSkipParticle(t)) continue
                val ta = t * (2f * PI.toFloat() / 5f) + time * 1.5f + seed
                val tLen = baseSize * (1.8f + sin(time * 4f + t * 1.7f) * 0.4f)
                val midX = cx + cos(ta) * tLen * 0.5f
                val midY = cy + sin(ta) * tLen * 0.5f
                val endX = cx + cos(ta + sin(time * 3f + t) * 0.3f) * tLen
                val endY = cy + sin(ta + sin(time * 3f + t) * 0.3f) * tLen
                drawLine(PoisonBase40, start = Offset(cx, cy), end = Offset(midX, midY),
                    strokeWidth = 3f, cap = StrokeCap.Round)
                drawLine(PoisonNeon25, start = Offset(midX, midY), end = Offset(endX, endY),
                    strokeWidth = 2f, cap = StrokeCap.Round)
                drawCircle(PoisonNeon, alpha = 0.4f, radius = 2f, center = Offset(endX, endY))
            }
        }
        else -> {
            val wobX = sin(time * 4f + seed) * baseSize * 0.06f
            val wobY = cos(time * 6f + seed * 1.3f) * baseSize * 0.06f
            val cx = curX + wobX; val cy = curY + wobY
            val pulse = 1f + sin(time * 5f + seed) * 0.06f
            // Layered glow
            drawCircle(PoisonDark10, radius = baseSize * 3f, center = Offset(cx, cy))
            drawCircle(PoisonNeon20, radius = baseSize * 2f, center = Offset(cx, cy))
            drawCircle(PoisonDark, alpha = 0.5f, radius = baseSize * 1.8f * pulse, center = Offset(cx, cy))
            drawCircle(PoisonBase, radius = baseSize * 1.3f * pulse, center = Offset(cx, cy))
            drawStar(cx, cy, 6, baseSize * 1.2f, baseSize * 0.5f,
                time * 2.5f, PoisonNeon, 0.6f, path)
            drawStar(cx, cy, 5, baseSize * 0.8f, baseSize * 0.3f,
                -time * 3.5f, PoisonAcid80, 0.5f, path)
            drawCircle(PoisonAcid70, radius = baseSize * 0.3f, center = Offset(cx, cy))
            drawCircle(PoisonBase25, radius = baseSize * 2f, center = Offset(cx, cy), style = StrokeW2)
            drawCircle(PoisonNeon15, radius = baseSize * 2.5f, center = Offset(cx, cy), style = StrokeW1_5)
            drawOrbit(cx, cy, 8, baseSize * 2.2f, PoisonBase, 0.3f, 3f, time, 2f)
        }
    }
}

// ═══════════════════════════════════════════════════════
// LIGHTNING — Jagged bolt from src to cur at ALL grades
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawLightningProjectile(
    srcX: Float, srcY: Float, curX: Float, curY: Float,
    grade: Int, time: Float, seed: Int, path: Path,
) {
    val baseSize = 9f + grade * 3.5f
    // High-frequency flicker — bolt appearance changes rapidly
    val flicker = sin(time * 30f + seed) * 0.5f + 0.5f  // 0..1 rapid flicker

    when {
        grade <= 1 -> {
            // Single bolt from src to cur — this IS the projectile
            val boltWidth = 2f + grade * 0.5f
            val jitter = 8f + grade * 2f
            drawLightningBolt(srcX, srcY, curX, curY,
                LightBase, 5 + grade, jitter, boltWidth, time, seed, path)

            // Bright flash at the tip
            val flashR = baseSize * (0.8f + flicker * 0.4f)
            drawCircle(LightGlow, alpha = 0.4f + flicker * 0.3f, radius = flashR, center = Offset(curX, curY))
            drawCircle(LightWhite, alpha = 0.6f + flicker * 0.3f, radius = flashR * 0.5f, center = Offset(curX, curY))
            drawCircle(White, alpha = 0.8f, radius = flashR * 0.2f, center = Offset(curX, curY))
        }
        grade <= 3 -> {
            // Main bolt from src to cur
            val boltWidth = 2.5f + grade * 0.5f
            val jitter = 10f + grade * 2f
            drawLightningBolt(srcX, srcY, curX, curY,
                LightBase, 6 + grade, jitter, boltWidth, time, seed, path)

            // 1-2 forking branches near the tip
            val dd = dist(srcX, srcY, curX, curY)
            val forkCount = grade - 1  // grade 2 = 1 fork, grade 3 = 2 forks
            for (f in 0 until forkCount) {
                if (ParticleLOD.shouldSkipParticle(f)) continue
                // Fork origin is 60-80% along the bolt
                val forkT = 0.6f + f * 0.1f
                val forkSrcX = srcX + (curX - srcX) * forkT
                val forkSrcY = srcY + (curY - srcY) * forkT
                val forkAngle = dirAngle(srcX, srcY, curX, curY) + (f * 2 - 1) * (0.4f + sin(time * 8f + f) * 0.2f)
                val forkLen = dd * 0.3f
                val forkDstX = forkSrcX + cos(forkAngle) * forkLen
                val forkDstY = forkSrcY + sin(forkAngle) * forkLen
                drawLightningBolt(forkSrcX, forkSrcY, forkDstX, forkDstY,
                    LightArc, 3, jitter * 0.7f, boltWidth * 0.6f, time, seed + f * 17, path)
            }

            // Bright flash at the tip
            val flashR = baseSize * (1.0f + flicker * 0.5f)
            drawCircle(LightGlow, alpha = 0.5f + flicker * 0.3f, radius = flashR, center = Offset(curX, curY))
            drawCircle(LightWhite, alpha = 0.7f + flicker * 0.2f, radius = flashR * 0.5f, center = Offset(curX, curY))
            drawCircle(White, alpha = 0.85f, radius = flashR * 0.2f, center = Offset(curX, curY))
        }
        grade == 4 || grade == 5 -> {
            // Thicker main bolt from src to cur
            val boltWidth = 3f + grade * 0.5f
            val jitter = 12f + grade * 2f
            drawLightningBolt(srcX, srcY, curX, curY,
                LightWhite, 8 + grade, jitter, boltWidth, time, seed, path)

            // 3-4 forking branches
            val dd = dist(srcX, srcY, curX, curY)
            val forkCount = 2 + grade - 3  // grade 4 = 3 forks, grade 5 = 4 forks
            for (f in 0 until forkCount) {
                if (ParticleLOD.shouldSkipParticle(f)) continue
                val forkT = 0.5f + f * 0.12f
                val forkSrcX = srcX + (curX - srcX) * forkT
                val forkSrcY = srcY + (curY - srcY) * forkT
                val forkAngle = dirAngle(srcX, srcY, curX, curY) +
                    (if (f % 2 == 0) 1f else -1f) * (0.3f + sin(time * 6f + f * 1.5f) * 0.25f)
                val forkLen = dd * (0.25f + f * 0.05f)
                val forkDstX = forkSrcX + cos(forkAngle) * forkLen
                val forkDstY = forkSrcY + sin(forkAngle) * forkLen
                drawLightningBolt(forkSrcX, forkSrcY, forkDstX, forkDstY,
                    LightBase, 4, jitter * 0.6f, boltWidth * 0.5f, time, seed + f * 23, path)
            }

            // Electric field at impact point
            val fieldR = baseSize * (1.5f + flicker * 0.5f)
            drawCircle(LightGlow, alpha = 0.3f + flicker * 0.2f, radius = fieldR, center = Offset(curX, curY))
            drawCircle(LightWhite, alpha = 0.6f + flicker * 0.3f, radius = fieldR * 0.5f, center = Offset(curX, curY))
            drawCircle(White, alpha = 0.9f, radius = fieldR * 0.2f, center = Offset(curX, curY))
            // Crackling arcs around impact point (with LOD)
            for (arc in 0 until 3 + grade - 3) {
                if (ParticleLOD.shouldSkipParticle(arc + 10)) continue
                val arcAngle = arc * (2f * PI.toFloat() / (3 + grade - 3)) + time * 8f + seed
                val arcR = fieldR * 0.8f
                val arcEndX = curX + cos(arcAngle) * arcR
                val arcEndY = curY + sin(arcAngle) * arcR
                drawLightningBolt(curX, curY, arcEndX, arcEndY,
                    LightArc, 3, 5f, 1.5f, time, seed + arc * 41, path)
            }
        }
        else -> {
            // Grade 6: Massive bolt chain + multiple forks + pulsing electric aura
            val boltWidth = 4f + grade * 0.5f
            val jitter = 15f
            // Main massive bolt
            drawLightningBolt(srcX, srcY, curX, curY,
                LightWhite, 12, jitter, boltWidth, time, seed, path)
            // Second parallel bolt for thickness feel
            drawLightningBolt(srcX, srcY, curX, curY,
                LightBase, 10, jitter * 1.2f, boltWidth * 0.6f, time, seed + 77, path)

            // Multiple forks along the bolt
            val dd = dist(srcX, srcY, curX, curY)
            for (f in 0 until 5) {
                if (ParticleLOD.shouldSkipParticle(f)) continue
                val forkT = 0.3f + f * 0.13f
                val forkSrcX = srcX + (curX - srcX) * forkT
                val forkSrcY = srcY + (curY - srcY) * forkT
                val forkAngle = dirAngle(srcX, srcY, curX, curY) +
                    (if (f % 2 == 0) 1f else -1f) * (0.35f + sin(time * 5f + f * 2f) * 0.2f)
                val forkLen = dd * (0.2f + f * 0.04f)
                val forkDstX = forkSrcX + cos(forkAngle) * forkLen
                val forkDstY = forkSrcY + sin(forkAngle) * forkLen
                drawLightningBolt(forkSrcX, forkSrcY, forkDstX, forkDstY,
                    LightArc, 4, jitter * 0.5f, boltWidth * 0.4f, time, seed + f * 31, path)
            }

            // Pulsing electric aura at impact
            val auraR = baseSize * (2.5f + sin(time * 10f) * 0.5f)
            drawCircle(LightGlow, alpha = 0.2f + flicker * 0.15f, radius = auraR, center = Offset(curX, curY))
            drawCircle(LightGlow20, radius = auraR * 1.3f, center = Offset(curX, curY), style = StrokeW2)
            drawCircle(LightWhite, alpha = 0.7f + flicker * 0.2f, radius = auraR * 0.4f, center = Offset(curX, curY))
            drawCircle(White, alpha = 0.9f, radius = auraR * 0.15f, center = Offset(curX, curY))
            // Cross-connecting arcs around impact (with LOD)
            for (c in 0 until 4) {
                if (ParticleLOD.shouldSkipParticle(c + 10)) continue
                val a1 = c * (PI.toFloat() / 2f) + time * 3f + seed
                val a2 = a1 + PI.toFloat() / 3f
                val r = auraR * 0.8f
                drawLightningBolt(
                    curX + cos(a1) * r, curY + sin(a1) * r,
                    curX + cos(a2) * r, curY + sin(a2) * r,
                    LightArc40, 3, 5f, 1.5f, time, seed + c * 31, path
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// SUPPORT — Holy light beam with cross/plus and halo
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawSupportProjectile(
    srcX: Float, srcY: Float, curX: Float, curY: Float,
    grade: Int, time: Float, seed: Int, path: Path,
) {
    val baseSize = 9f + grade * 3.5f
    val trailCount = 3 + grade * 2

    drawTrail(srcX, srcY, curX, curY, trailCount, SupportGold, baseSize * 0.4f, 2f + grade * 0.5f, time, seed)

    // Holy glow (layered circles)
    val glowR = baseSize * (1.8f + grade * 0.3f)
    drawGlow(curX, curY, glowR, SupportHoly, 0.3f, SupportGold, 0.1f)

    when {
        grade <= 2 -> {
            val pulse = 1f + sin(time * 10f + seed) * 0.12f
            // Rotating cross/plus shape — the signature of Support at ALL grades
            val crossAngle = time * 5f + seed
            val crossLen = baseSize * 1.2f * pulse
            val crossW = 2.5f + grade * 0.5f
            for (arm in 0 until 4) {
                val a = crossAngle + arm * (PI.toFloat() / 2f)
                drawLine(SupportGold80, start = Offset(curX, curY),
                    end = Offset(curX + cos(a) * crossLen, curY + sin(a) * crossLen),
                    strokeWidth = crossW, cap = StrokeCap.Round)
            }
            // Inner glow at center
            drawCircle(SupportHoly, alpha = 0.8f, radius = baseSize * 0.4f * pulse, center = Offset(curX, curY))
            drawCircle(White, alpha = 0.6f, radius = baseSize * 0.2f, center = Offset(curX, curY))
            // Halo ring
            val haloR = baseSize * 1.4f + sin(time * 6f) * 2f
            drawCircle(SupportGold30, radius = haloR, center = Offset(curX, curY), style = StrokeW1_5)
            drawOrbit(curX, curY, 2 + grade, baseSize * 1.3f, SupportHoly, 0.5f, 1.5f, time, 7f, seed.toFloat())
        }
        grade <= 4 -> {
            val pulse = 1f + sin(time * 8f + seed) * 0.1f
            // Warm golden glow
            drawCircle(SupportGold, alpha = 0.5f, radius = baseSize * 1.2f * pulse, center = Offset(curX, curY))
            drawCircle(SupportHoly, alpha = 0.7f, radius = baseSize * 0.8f * pulse, center = Offset(curX, curY))
            // Rotating cross — larger and more defined
            val crossAngle = time * 4f + seed
            val crossLen = baseSize * 1.5f
            val crossW = 3f + grade * 0.5f
            for (arm in 0 until 4) {
                val a = crossAngle + arm * (PI.toFloat() / 2f)
                drawLine(SupportGold80, start = Offset(curX, curY),
                    end = Offset(curX + cos(a) * crossLen, curY + sin(a) * crossLen),
                    strokeWidth = crossW, cap = StrokeCap.Round)
            }
            // Second smaller counter-rotating cross
            val crossAngle2 = -time * 3f + seed
            val crossLen2 = baseSize * 0.9f
            for (arm in 0 until 4) {
                val a = crossAngle2 + arm * (PI.toFloat() / 2f)
                drawLine(SupportHoly50, start = Offset(curX, curY),
                    end = Offset(curX + cos(a) * crossLen2, curY + sin(a) * crossLen2),
                    strokeWidth = crossW * 0.6f, cap = StrokeCap.Round)
            }
            drawCircle(White, alpha = 0.7f, radius = baseSize * 0.3f, center = Offset(curX, curY))
            // Halo ring
            val haloR = baseSize * 1.7f + sin(time * 5f) * 2f
            drawCircle(SupportGold30, radius = haloR, center = Offset(curX, curY), style = StrokeW1_5)
            if (grade == 4) {
                // Rainbow shimmer (pre-allocated list)
                for (ci in RainbowColors40.indices) {
                    val ra = ci * (2f * PI.toFloat() / 5f) + time * 3f
                    drawCircle(RainbowColors40[ci], radius = 2f, center = Offset(
                        curX + cos(ra) * baseSize * 1.6f, curY + sin(ra) * baseSize * 1.6f
                    ))
                }
            }
        }
        grade == 5 -> {
            val pulse = 1f + sin(time * 6f + seed) * 0.08f
            drawCircle(SupportGold, alpha = 0.6f, radius = baseSize * 1.5f * pulse, center = Offset(curX, curY))
            drawCircle(SupportHoly, alpha = 0.85f, radius = baseSize * 1f * pulse, center = Offset(curX, curY))
            // Cross + star combination
            val crossAngle = time * 3f + seed
            val crossLen = baseSize * 1.6f
            for (arm in 0 until 4) {
                val a = crossAngle + arm * (PI.toFloat() / 2f)
                drawLine(SupportGold80, start = Offset(curX, curY),
                    end = Offset(curX + cos(a) * crossLen, curY + sin(a) * crossLen),
                    strokeWidth = 4f, cap = StrokeCap.Round)
            }
            drawStar(curX, curY, 8, baseSize * 1.3f, baseSize * 0.65f, -time * 2f, SupportGold, 0.5f, path)
            drawCircle(White, alpha = 0.8f, radius = baseSize * 0.35f, center = Offset(curX, curY))
            // Halo ring
            val haloR = baseSize * 2f + sin(time * 4f) * 3f
            drawCircle(SupportGold30, radius = haloR, center = Offset(curX, curY), style = StrokeW2)
            // Radiating light rays (with LOD)
            for (ray in 0 until 12) {
                if (ParticleLOD.shouldSkipParticle(ray)) continue
                val ra = ray * (PI.toFloat() / 6f) + time * 1.5f
                val rayLen = baseSize * (1.6f + sin(time * 5f + ray * 0.7f) * 0.3f)
                drawLine(SupportGold40, start = Offset(curX, curY),
                    end = Offset(curX + cos(ra) * rayLen, curY + sin(ra) * rayLen),
                    strokeWidth = 1.5f, cap = StrokeCap.Round)
            }
        }
        else -> {
            val spin = time * 2f
            // Layered glow
            drawCircle(SupportDivine15, radius = baseSize * 3.5f, center = Offset(curX, curY))
            drawCircle(SupportHoly35, radius = baseSize * 2f, center = Offset(curX, curY))
            drawCircle(SupportGold, alpha = 0.6f, radius = baseSize * 1.6f, center = Offset(curX, curY))
            drawCircle(SupportHoly, alpha = 0.8f, radius = baseSize * 1.1f, center = Offset(curX, curY))
            // Grand cross
            val crossLen = baseSize * 2f
            for (arm in 0 until 4) {
                val a = spin * 1.5f + arm * (PI.toFloat() / 2f)
                drawLine(SupportGold80, start = Offset(curX, curY),
                    end = Offset(curX + cos(a) * crossLen, curY + sin(a) * crossLen),
                    strokeWidth = 5f, cap = StrokeCap.Round)
            }
            drawStar(curX, curY, 8, baseSize * 1.5f, baseSize * 0.7f, spin, SupportGold, 0.7f, path)
            drawStar(curX, curY, 8, baseSize * 1.1f, baseSize * 0.5f, -spin * 1.3f, SupportHoly, 0.5f, path)
            // Sacred hexagon (with LOD)
            for (h in 0 until 6) {
                if (ParticleLOD.shouldSkipParticle(h)) continue
                val a1 = h * (PI.toFloat() / 3f) + spin * 0.5f
                val a2 = (h + 1) * (PI.toFloat() / 3f) + spin * 0.5f
                val hexR = baseSize * 2f
                drawLine(SupportGold40, start = Offset(curX + cos(a1) * hexR, curY + sin(a1) * hexR),
                    end = Offset(curX + cos(a2) * hexR, curY + sin(a2) * hexR),
                    strokeWidth = 2f, cap = StrokeCap.Round)
            }
            drawCircle(White, alpha = 0.85f, radius = baseSize * 0.35f, center = Offset(curX, curY))
            // Double halo
            val haloR1 = baseSize * 2.2f + sin(time * 4f) * 3f
            val haloR2 = baseSize * 2.8f + sin(time * 3f + 1f) * 3f
            drawCircle(SupportGold30, radius = haloR1, center = Offset(curX, curY), style = StrokeW2)
            drawCircle(SupportGold30, radius = haloR2, center = Offset(curX, curY), style = StrokeW1)
            // Prismatic color orbit (pre-allocated alpha list)
            for (ci in PrismColors50.indices) {
                val ca = ci * (2f * PI.toFloat() / PrismColors50.size) + time * 2.5f
                val cr = baseSize * 2.3f
                drawCircle(PrismColors50[ci], radius = 2.5f, center = Offset(
                    curX + cos(ca) * cr, curY + sin(ca) * cr
                ))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// WIND — Slicing air blade with speed lines
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawWindProjectile(
    srcX: Float, srcY: Float, curX: Float, curY: Float,
    grade: Int, time: Float, seed: Int, path: Path,
) {
    val angle = dirAngle(srcX, srcY, curX, curY)
    val baseSize = 9f + grade * 3.5f
    val trailCount = 3 + grade * 2

    // Speed lines in direction of travel — streaks behind the blade
    val d = dist(srcX, srcY, curX, curY)
    if (d > 4f) {
        val dirX = (curX - srcX) / d; val dirY = (curY - srcY) / d
        val perpX = -dirY; val perpY = dirX
        val lineCount = 3 + grade
        for (j in 0 until lineCount) {
            if (ParticleLOD.shouldSkipParticle(j)) continue
            val spread = (j - lineCount / 2f) * (baseSize * 0.4f)
            val lineLen = baseSize * (1.5f + grade * 0.5f)
            val startOff = baseSize * 0.5f + sin(time * 12f + j * 3f + seed) * baseSize * 0.3f
            val sx = curX - dirX * startOff + perpX * spread
            val sy = curY - dirY * startOff + perpY * spread
            val ex = sx - dirX * lineLen
            val ey = sy - dirY * lineLen
            val alpha = 0.3f - abs(j - lineCount / 2f) * 0.06f
            drawLine(WindCyan, alpha = alpha, start = Offset(sx, sy), end = Offset(ex, ey),
                strokeWidth = 1.5f, cap = StrokeCap.Round)
        }
    }

    // Standard trail behind
    drawTrail(srcX, srcY, curX, curY, trailCount, WindCyan, baseSize * 0.4f, 5f + grade, time, seed)

    when {
        grade <= 2 -> {
            // Fast-spinning aggressive crescent blades
            val spin = angle + time * 14f + seed
            drawCrescent(curX, curY, baseSize * 1.4f, baseSize * 0.6f, spin, WindTeal, 0.85f, path)
            drawCrescent(curX, curY, baseSize * 1.4f, baseSize * 0.6f, spin + PI.toFloat(), WindCyan, 0.6f, path)
            drawCrescent(curX, curY, baseSize * 0.8f, baseSize * 0.3f, -spin * 1.5f, WindLight, 0.5f, path)
            drawCircle(WindCyan, alpha = 0.6f, radius = baseSize * 0.2f, center = Offset(curX, curY))
            drawOrbit(curX, curY, 2 + grade, baseSize * 1.3f, WindLight, 0.4f, 1.5f, time, 12f, spin)
        }
        grade <= 4 -> {
            val spin = angle + time * 10f
            // Dual-layer aggressive spinning blades
            drawCrescent(curX, curY, baseSize * 1.6f, baseSize * 0.7f, spin, WindTeal, 0.8f, path)
            drawCrescent(curX, curY, baseSize * 1.6f, baseSize * 0.7f, spin + PI.toFloat(), WindCyan, 0.65f, path)
            drawCrescent(curX, curY, baseSize * 1.1f, baseSize * 0.5f, -spin * 1.3f, WindTeal, 0.5f, path)
            drawCrescent(curX, curY, baseSize * 1.1f, baseSize * 0.5f, -spin * 1.3f + PI.toFloat(), WindLight, 0.4f, path)
            drawCircle(WindLight, alpha = 0.5f, radius = baseSize * 0.3f, center = Offset(curX, curY))
            // Spiral wind particles (with LOD)
            for (s in 0 until 6 + grade * 2) {
                if (ParticleLOD.shouldSkipParticle(s)) continue
                val t = s.toFloat() / (6 + grade * 2)
                val spiralAngle = spin + t * 4f * PI.toFloat()
                val spiralR = baseSize * (0.5f + t * 1.2f)
                drawCircle(WindCyan, alpha = 0.4f * (1f - t * 0.5f), radius = 1.5f + (1f - t) * 1.5f,
                    center = Offset(curX + cos(spiralAngle) * spiralR, curY + sin(spiralAngle) * spiralR))
            }
        }
        grade == 5 -> {
            val spin = time * 8f
            // Tornado rings — fast spinning (with LOD)
            for (ring in 0 until 3) {
                if (ParticleLOD.shouldSkipParticle(ring)) continue
                val ringR = baseSize * (0.8f + ring * 0.6f)
                val ringAlpha = 0.45f - ring * 0.1f
                val ringAngle = spin * (1f + ring * 0.4f) + ring * 0.5f
                drawCrescent(curX, curY, ringR, ringR * 0.35f, ringAngle, WindTeal, ringAlpha, path)
                drawCrescent(curX, curY, ringR, ringR * 0.35f, ringAngle + PI.toFloat(), WindCyan, ringAlpha * 0.7f, path)
            }
            drawCircle(WindLight, alpha = 0.6f, radius = baseSize * 0.4f, center = Offset(curX, curY))
            drawCircle(White, alpha = 0.5f, radius = baseSize * 0.2f, center = Offset(curX, curY))
        }
        else -> {
            val spin = time * 6f
            // Layered glow
            drawCircle(WindDark10, radius = baseSize * 3.5f, center = Offset(curX, curY))
            drawCircle(WindCyan20, radius = baseSize * 2f, center = Offset(curX, curY))
            // Multi-layer tornado — fast (with LOD)
            for (ring in 0 until 4) {
                if (ParticleLOD.shouldSkipParticle(ring)) continue
                val ringR = baseSize * (0.6f + ring * 0.6f)
                val ringAlpha = 0.4f - ring * 0.06f
                val ringAngle = spin * (1f + ring * 0.5f) + ring * 0.7f
                drawCrescent(curX, curY, ringR, ringR * 0.35f, ringAngle, WindTeal, ringAlpha, path)
                drawCrescent(curX, curY, ringR, ringR * 0.35f, ringAngle + PI.toFloat(), WindCyan, ringAlpha * 0.7f, path)
            }
            drawCircle(WindLight, alpha = 0.7f, radius = baseSize * 0.5f, center = Offset(curX, curY))
            drawCircle(White, alpha = 0.6f, radius = baseSize * 0.25f, center = Offset(curX, curY))
            // Orbital debris (with LOD)
            for (dd in 0 until 6) {
                if (ParticleLOD.shouldSkipParticle(dd)) continue
                val da = dd * (PI.toFloat() / 3f) + spin * 1.5f
                val dr = baseSize * 2.8f
                drawDiamond(curX + cos(da) * dr, curY + sin(da) * dr,
                    3f, 5f, da * 2f, WindTeal, 0.5f, path)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// DEFAULT — Simple glowing orb for unknown families
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawDefaultProjectile(
    srcX: Float, srcY: Float, curX: Float, curY: Float, time: Float,
) {
    drawTrail(srcX, srcY, curX, curY, 4, Color.LightGray, 5f, 3f, time, 0)
    val pulse = 1f + sin(time * 10f) * 0.15f
    drawCircle(Color.LightGray, alpha = 0.3f, radius = 18f * pulse, center = Offset(curX, curY))
    drawCircle(White, radius = 7f * pulse, center = Offset(curX, curY))
    drawCircle(White, alpha = 0.7f, radius = 3.5f, center = Offset(curX, curY))
}

// ═══════════════════════════════════════════════════════
// HELPER — Lightning bolt drawing
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawLightningBolt(
    srcX: Float, srcY: Float, dstX: Float, dstY: Float,
    color: Color, segments: Int, jitter: Float, width: Float,
    time: Float, seed: Int, path: Path,
) {
    val dx = dstX - srcX; val dy = dstY - srcY
    val len = sqrt(dx * dx + dy * dy)
    if (len < 1f) return
    val perpX = -dy / len; val perpY = dx / len

    path.reset()
    path.moveTo(srcX, srcY)
    for (i in 1 until segments) {
        val t = i.toFloat() / segments
        val h = (seed * 127 + i * 311 + (time * 3f).toInt()) * 0x9E3779B1.toInt()
        val hash = (h ushr 16 xor h).toFloat() / Int.MAX_VALUE.toFloat()
        val offset = hash * jitter * (1f - t * 0.3f)
        path.lineTo(srcX + dx * t + perpX * offset, srcY + dy * t + perpY * offset)
    }
    path.lineTo(dstX, dstY)

    drawPath(path, color, alpha = 0.2f, style = Stroke(width * 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, White60, style = Stroke(width * 0.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
