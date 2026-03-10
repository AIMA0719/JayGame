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
private val FireBase = Color(0xFFFF6B35)
private val FireGlow = Color(0xFFFF9800)
private val FireBright = Color(0xFFFFDD44)
private val FireGlow06 = FireGlow.copy(alpha = 0.6f)
private val FireBright08 = FireBright.copy(alpha = 0.8f)
private val FireDark = Color(0xFFFF4400)
private val FireDark07 = FireDark.copy(alpha = 0.7f)
private val FireDark05 = FireDark.copy(alpha = 0.5f)
private val FireOrange06 = Color(0xFFFF5500).copy(alpha = 0.6f)
private val FireOrange04 = Color(0xFFFF6600).copy(alpha = 0.4f)
private val FireRed015 = Color(0xFFFF3300).copy(alpha = 0.15f)
private val FireGold05 = Color(0xFFFFD700).copy(alpha = 0.5f)
private val FireDDark06 = Color(0xFFFFDD00).copy(alpha = 0.6f)

// Frost
private val FrostBase = Color(0xFF64B5F6)
private val FrostGlow = Color(0xFF90CAF9)
private val FrostBright = Color(0xFFE3F2FD)
private val FrostBright07 = FrostBright.copy(alpha = 0.7f)
private val FrostLight015 = Color(0xFF4FC3F7).copy(alpha = 0.15f)
private val FrostGlow02 = FrostGlow.copy(alpha = 0.2f)
private val White04 = Color.White.copy(alpha = 0.4f)
private val White06 = Color.White.copy(alpha = 0.6f)
private val White07 = Color.White.copy(alpha = 0.7f)
private val White09 = Color.White.copy(alpha = 0.9f)
private val White03 = Color.White.copy(alpha = 0.3f)
private val White08 = Color.White.copy(alpha = 0.8f)
private val White01 = Color.White.copy(alpha = 0.1f)

// Poison
private val PoisonBase = Color(0xFF81C784)
private val PoisonGlow = Color(0xFFA5D6A7)
private val PoisonDark = Color(0xFF388E3C)
private val PoisonDark06 = PoisonDark.copy(alpha = 0.6f)
private val PoisonLime05 = Color(0xFFCCFF00).copy(alpha = 0.5f)
private val PoisonBase015 = PoisonBase.copy(alpha = 0.15f)
private val PoisonBase012 = PoisonBase.copy(alpha = 0.12f)
private val PoisonLime07 = Color(0xFFAAFF00).copy(alpha = 0.7f)
private val PoisonGreen04 = Color(0xFF00FF00).copy(alpha = 0.4f)

// Lightning
private val LightBase = Color(0xFFFFD54F)
private val LightGlow = Color(0xFFFFFF00)
private val LightWhite = Color(0xFFFFFDE7)
private val LightGlow04 = LightGlow.copy(alpha = 0.4f)
private val LightGlow05 = LightGlow.copy(alpha = 0.5f)
private val LightGlow06 = LightGlow.copy(alpha = 0.6f)
private val LightGlow015 = LightGlow.copy(alpha = 0.15f)
private val LightBase04 = LightBase.copy(alpha = 0.4f)

// Support
private val SupportBase = Color(0xFFCE93D8)
private val SupportGlow = Color(0xFFE1BEE7)
private val SupportGold = Color(0xFFD4A847)
private val SupportHoly = Color(0xFFFFF8E1)
private val SupportHoly06 = SupportHoly.copy(alpha = 0.6f)
private val SupportGold04 = SupportGold.copy(alpha = 0.4f)
private val SupportGold03 = SupportGold.copy(alpha = 0.3f)
private val SupportGold05 = SupportGold.copy(alpha = 0.5f)
private val SupportGold02 = SupportGold.copy(alpha = 0.2f)
private val SupportGold015 = SupportGold.copy(alpha = 0.15f)
private val GoldBeam = Color(0xFFFFCC80)

// Rainbow (support grade 4)
private val RainbowColors = listOf(
    Color(0xFFFF6B6B), Color(0xFFFFD93D), Color(0xFF6BCB77),
    Color(0xFF4D96FF), Color(0xFFAB46D2),
)
private val RainbowAlpha = RainbowColors.map { it.copy(alpha = 0.6f) }

// Common beam colors
private val BeamWhite07 = Color.White.copy(alpha = 0.7f)
private val BeamWhite06 = Color.White.copy(alpha = 0.6f)
private val DefaultBeamColor = Color.White
private val DefaultGlow = Color(0xFFE0E0E0)

// Pre-allocated Stroke objects
private val StrokeGlow = Stroke(width = 2f)
private val StrokeThin = Stroke(width = 1.5f)

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

    // Reusable Path for lightning effects
    val lightningPath = remember { Path() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val data = projData
        val w = size.width
        val h = size.height
        for (i in 0 until data.count) {
            val srcX = data.srcXs[i] * w
            val srcY = data.srcYs[i] * h
            val dstX = data.dstXs[i] * w
            val dstY = data.dstYs[i] * h
            val unitId = data.types[i]
            val family = unitId % 5
            val grade = unitId / 5

            when (family) {
                0 -> drawFireEffect(srcX, srcY, dstX, dstY, grade, fxTime, i)
                1 -> drawFrostEffect(srcX, srcY, dstX, dstY, grade, fxTime, i)
                2 -> drawPoisonEffect(srcX, srcY, dstX, dstY, grade, fxTime, i)
                3 -> drawLightningEffect(srcX, srcY, dstX, dstY, grade, fxTime, i, lightningPath)
                4 -> drawSupportEffect(srcX, srcY, dstX, dstY, grade, fxTime, i)
                else -> drawDefaultBeam(srcX, srcY, dstX, dstY)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Shared helper: particle trail along beam (reduces copy-paste)
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawParticleTrail(
    srcX: Float, srcY: Float, dstX: Float, dstY: Float,
    count: Int, spread: Float, color: Color, alpha: Float,
    radius: Float, time: Float, seed: Int,
) {
    val dx = dstX - srcX; val dy = dstY - srcY
    val len = sqrt(dx * dx + dy * dy)
    if (len < 1f) return
    val px = -dy / len; val py = dx / len
    for (j in 0 until count) {
        val t = j.toFloat() / count
        val wobble = sin(t * 10f + time * 8f + seed + j) * spread * sin(t * PI.toFloat())
        val x = srcX + dx * t + px * wobble
        val y = srcY + dy * t + py * wobble
        drawCircle(color, alpha = alpha * (1f - t * 0.5f), radius = radius, center = Offset(x, y))
    }
}

// ═══════════════════════════════════════════════════════
// FIRE FAMILY
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawFireEffect(
    srcX: Float, srcY: Float, dstX: Float, dstY: Float,
    grade: Int, time: Float, seed: Int,
) {
    val src = Offset(srcX, srcY); val dst = Offset(dstX, dstY)
    when (grade) {
        0 -> {
            for (j in 0 until 3) {
                val t = ((time * 3 + j * 0.33f + seed * 0.17f) % 1f)
                val mx = srcX + (dstX - srcX) * t; val my = srcY + (dstY - srcY) * t
                val wobble = sin((t + seed) * 12f) * 6f
                drawCircle(FireGlow06, radius = 5f, center = Offset(mx + wobble, my - wobble * 0.5f))
            }
            drawBeam(src, dst, FireBase, FireGlow, 3f)
        }
        1 -> {
            drawBeam(src, dst, FireBase, FireGlow, 5f)
            val bt = 0.3f + time * 0.4f
            val bx = srcX + (dstX - srcX) * bt; val by = srcY + (dstY - srcY) * bt
            drawCircle(FireDark07, radius = 10f, center = Offset(bx, by))
            drawCircle(FireBright, alpha = 0.9f, radius = 5f, center = Offset(bx, by))
            for (j in 1..4) {
                val t2 = (bt - j * 0.06f).coerceIn(0f, 1f)
                val tx = srcX + (dstX - srcX) * t2; val ty = srcY + (dstY - srcY) * t2
                drawCircle(FireGlow, alpha = 0.3f / j, radius = 7f / j, center = Offset(tx, ty))
            }
        }
        2 -> {
            drawBeam(src, dst, FireBase, FireGlow, 6f)
            drawParticleTrail(srcX, srcY, dstX, dstY, 12, 8f, FireOrange06, 0.6f, 3f, time, seed)
        }
        3 -> {
            drawLine(FireGlow, alpha = 0.2f, start = src, end = dst, strokeWidth = 40f, cap = StrokeCap.Round)
            drawLine(FireDark, start = src, end = dst, strokeWidth = 10f, cap = StrokeCap.Round)
            drawLine(FireBright, alpha = 0.8f, start = src, end = dst, strokeWidth = 3f, cap = StrokeCap.Round)
            drawParticleTrail(srcX, srcY, dstX, dstY, 8, 12f, FireOrange04, 0.6f, 5f, time, seed)
            drawCircle(FireDark05, radius = 14f, center = dst)
        }
        else -> {
            drawLine(FireRed015, start = src, end = dst, strokeWidth = 60f, cap = StrokeCap.Round)
            drawLine(FireDark, start = src, end = dst, strokeWidth = 12f, cap = StrokeCap.Round)
            drawLine(FireBright, alpha = 0.9f, start = src, end = dst, strokeWidth = 3.6f, cap = StrokeCap.Round)
            drawParticleTrail(srcX, srcY, dstX, dstY, 16, 20f, FireGold05, 0.5f, 4f, time, seed)
            drawCircle(FireDDark06, radius = 18f, center = dst)
            drawCircle(White03, radius = 8f, center = dst)
        }
    }
}

// ═══════════════════════════════════════════════════════
// FROST FAMILY
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawFrostEffect(
    srcX: Float, srcY: Float, dstX: Float, dstY: Float,
    grade: Int, time: Float, seed: Int,
) {
    val src = Offset(srcX, srcY); val dst = Offset(dstX, dstY)
    when (grade) {
        0 -> {
            drawBeam(src, dst, FrostBase, FrostGlow, 3f)
            val t = 0.5f + time * 0.3f
            drawCircle(FrostBright07, radius = 4f, center = Offset(srcX + (dstX - srcX) * t, srcY + (dstY - srcY) * t))
        }
        1 -> {
            drawBeam(src, dst, FrostBase, FrostGlow, 4f)
            for (j in 0 until 5) {
                val t = ((time + j * 0.2f + seed * 0.13f) % 1f)
                val cx = srcX + (dstX - srcX) * t; val cy = srcY + (dstY - srcY) * t
                drawCircle(FrostBright, alpha = 0.5f * (1f - t), radius = 3f + j * 0.5f, center = Offset(cx, cy))
            }
        }
        2 -> {
            drawBeam(src, dst, FrostBase, FrostGlow, 6f)
            for (j in 0 until 3) {
                val t = ((time + j * 0.33f) % 1f)
                val rx = srcX + (dstX - srcX) * t; val ry = srcY + (dstY - srcY) * t
                val ringR = 6f + sin(time * 8f + j) * 3f
                drawCircle(FrostBase, alpha = 0.3f, radius = ringR, center = Offset(rx, ry))
                drawCircle(FrostBright, alpha = 0.2f, radius = ringR * 0.5f, center = Offset(rx, ry), style = StrokeThin)
            }
        }
        3 -> {
            drawLine(FrostGlow02, start = src, end = dst, strokeWidth = 27f, cap = StrokeCap.Round)
            drawLine(FrostBase, start = src, end = dst, strokeWidth = 9f, cap = StrokeCap.Round)
            drawLine(White07, start = src, end = dst, strokeWidth = 2.7f, cap = StrokeCap.Round)
            drawParticleTrail(srcX, srcY, dstX, dstY, 6, 10f, White06, 0.6f, 2.5f, time, seed)
        }
        else -> {
            drawLine(FrostLight015, start = src, end = dst, strokeWidth = 48f, cap = StrokeCap.Round)
            drawLine(FrostBase, start = src, end = dst, strokeWidth = 12f, cap = StrokeCap.Round)
            drawLine(White09, start = src, end = dst, strokeWidth = 4.8f, cap = StrokeCap.Round)
            drawParticleTrail(srcX, srcY, dstX, dstY, 10, 15f, White04, 0.4f, 3f, time, seed)
            drawCircle(FrostBright07, radius = 16f, center = dst)
        }
    }
}

// ═══════════════════════════════════════════════════════
// POISON FAMILY
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawPoisonEffect(
    srcX: Float, srcY: Float, dstX: Float, dstY: Float,
    grade: Int, time: Float, seed: Int,
) {
    val src = Offset(srcX, srcY); val dst = Offset(dstX, dstY)
    when (grade) {
        0 -> {
            drawBeam(src, dst, PoisonBase, PoisonGlow, 2.5f)
            val t = 0.7f
            drawCircle(PoisonDark06, radius = 3f, center = Offset(srcX + (dstX - srcX) * t, srcY + (dstY - srcY) * t))
        }
        1 -> {
            drawBeam(src, dst, PoisonBase, PoisonGlow, 3f)
            for (j in 0 until 4) {
                val t = ((time * 2 + j * 0.25f) % 1f)
                val px = srcX + (dstX - srcX) * t; val py = srcY + (dstY - srcY) * t
                val puffSize = 5f + sin(time * 6f + j * 2f) * 2f
                drawCircle(PoisonBase, alpha = 0.3f * (1f - t), radius = puffSize, center = Offset(px, py))
            }
        }
        2 -> {
            drawBeam(src, dst, PoisonBase, PoisonGlow, 5f)
            val ot = 0.4f + time * 0.3f
            val ox = srcX + (dstX - srcX) * ot; val oy = srcY + (dstY - srcY) * ot
            drawCircle(PoisonDark06, radius = 8f, center = Offset(ox, oy))
            drawCircle(PoisonLime05, radius = 4f, center = Offset(ox, oy))
            for (j in 0 until 3) {
                val dripY = oy + (time * 20f + j * 8f) % 20f
                drawCircle(PoisonBase, alpha = 0.4f, radius = 2f, center = Offset(ox + (j - 1) * 4f, dripY))
            }
        }
        3 -> {
            drawLine(PoisonBase015, start = src, end = dst, strokeWidth = 32f, cap = StrokeCap.Round)
            drawLine(PoisonDark, start = src, end = dst, strokeWidth = 8f, cap = StrokeCap.Round)
            drawLine(PoisonLime05, start = src, end = dst, strokeWidth = 2.4f, cap = StrokeCap.Round)
            drawParticleTrail(srcX, srcY, dstX, dstY, 8, 12f, PoisonBase, 0.25f, 6f, time, seed)
        }
        else -> {
            drawLine(PoisonBase012, start = src, end = dst, strokeWidth = 55f, cap = StrokeCap.Round)
            drawLine(PoisonDark, start = src, end = dst, strokeWidth = 11f, cap = StrokeCap.Round)
            drawLine(PoisonLime07, start = src, end = dst, strokeWidth = 3.3f, cap = StrokeCap.Round)
            drawParticleTrail(srcX, srcY, dstX, dstY, 12, 18f, PoisonBase, 0.4f, 5f, time, seed)
            drawCircle(PoisonGreen04, radius = 16f, center = dst)
        }
    }
}

// ═══════════════════════════════════════════════════════
// LIGHTNING FAMILY
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawLightningEffect(
    srcX: Float, srcY: Float, dstX: Float, dstY: Float,
    grade: Int, time: Float, seed: Int, path: Path,
) {
    val src = Offset(srcX, srcY); val dst = Offset(dstX, dstY)
    when (grade) {
        0 -> {
            drawBeam(src, dst, LightBase, LightGlow, 3f)
            drawCircle(LightGlow06, radius = 4f, center = Offset((srcX + dstX) * 0.5f, (srcY + dstY) * 0.5f))
        }
        1 -> {
            drawLightningBolt(srcX, srcY, dstX, dstY, LightBase, 6, 10f, 3f, time, seed, path)
            drawCircle(LightGlow04, radius = 6f, center = dst)
        }
        2 -> {
            drawLightningBolt(srcX, srcY, dstX, dstY, LightBase, 8, 14f, 4f, time, seed, path)
            val mx = (srcX + dstX) * 0.5f; val my = (srcY + dstY) * 0.5f
            val fx = dstX + 20f * sin(seed.toFloat()); val fy = dstY - 15f
            drawLightningBolt(mx, my, fx, fy, LightBase04, 4, 8f, 2f, time, seed + 7, path)
            drawCircle(LightGlow05, radius = 8f, center = dst)
        }
        3 -> {
            drawLightningBolt(srcX, srcY, dstX, dstY, LightWhite, 10, 18f, 6f, time, seed, path)
            drawLightningBolt(srcX, srcY, dstX, dstY, LightBase04, 10, 22f, 10f, time, seed + 3, path)
            for (b in 0 until 3) {
                val bt = 0.3f + b * 0.2f
                val bx = srcX + (dstX - srcX) * bt; val by = srcY + (dstY - srcY) * bt
                val angle = (seed + b * 97) % 360 * PI.toFloat() / 180f
                drawLightningBolt(bx, by, bx + cos(angle) * 30f, by + sin(angle) * 30f, LightBase04, 3, 6f, 2f, time, seed + b, path)
            }
            drawCircle(LightGlow06, radius = 12f, center = dst)
        }
        else -> {
            val dx = dstX - srcX; val dy = dstY - srcY
            val len = sqrt(dx * dx + dy * dy)
            if (len < 1f) return
            val px = -dy / len; val py = dx / len
            for (b in -1..1) {
                val off = b * 8f
                drawLightningBolt(srcX + px * off, srcY + py * off, dstX + px * off * 0.5f, dstY + py * off * 0.5f, LightWhite, 10, 16f, 5f, time, seed + b * 13, path)
            }
            val ringR = 10f + sin(time * 12f) * 8f
            drawCircle(LightGlow015, radius = ringR * 2f, center = dst)
            drawCircle(LightBase, alpha = 0.4f, radius = ringR, center = dst, style = StrokeGlow)
            drawCircle(White06, radius = 6f, center = dst)
        }
    }
}

// ═══════════════════════════════════════════════════════
// SUPPORT FAMILY
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawSupportEffect(
    srcX: Float, srcY: Float, dstX: Float, dstY: Float,
    grade: Int, time: Float, seed: Int,
) {
    val src = Offset(srcX, srcY); val dst = Offset(dstX, dstY)
    when (grade) {
        0 -> {
            drawBeam(src, dst, SupportHoly, SupportGlow, 2f)
            val t = 0.5f + time * 0.3f
            drawCircle(SupportHoly06, radius = 4f, center = Offset(srcX + (dstX - srcX) * t, srcY + (dstY - srcY) * t))
        }
        1 -> {
            drawBeam(src, dst, SupportGold, Color(0xFFFFE082), 3.5f)
            for (j in 0 until 4) {
                val t = ((time * 2 + j * 0.25f) % 1f)
                val tx = srcX + (dstX - srcX) * t; val ty = srcY + (dstY - srcY) * t
                drawCircle(SupportGold04, radius = 3f, center = Offset(tx, ty))
            }
        }
        2 -> {
            drawLine(SupportGold015, start = src, end = dst, strokeWidth = 18f, cap = StrokeCap.Round)
            drawLine(GoldBeam, start = src, end = dst, strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(SupportHoly, alpha = 0.7f, start = src, end = dst, strokeWidth = 1.8f, cap = StrokeCap.Round)
            for (j in 0 until 4) {
                val t = j / 4f
                val px = srcX + (dstX - srcX) * t; val py = srcY + (dstY - srcY) * t
                val pulse = sin(time * 6f + j * 2f) * 3f
                drawCircle(SupportGold03, radius = 5f + pulse, center = Offset(px, py))
            }
        }
        3 -> {
            drawLine(SupportGold02, start = src, end = dst, strokeWidth = 36f, cap = StrokeCap.Round)
            drawLine(SupportGold, start = src, end = dst, strokeWidth = 9f, cap = StrokeCap.Round)
            drawLine(White08, start = src, end = dst, strokeWidth = 2.7f, cap = StrokeCap.Round)
            drawParticleTrail(srcX, srcY, dstX, dstY, 5, 4f, SupportGold, 0.6f, 2f, time, seed)
            drawCircle(SupportGold05, radius = 14f, center = dst)
        }
        else -> {
            drawLine(White01, start = src, end = dst, strokeWidth = 60f, cap = StrokeCap.Round)
            val dx = dstX - srcX; val dy = dstY - srcY
            val len = sqrt(dx * dx + dy * dy)
            if (len < 1f) return
            val px = -dy / len; val py = dx / len
            for ((ci, rc) in RainbowAlpha.withIndex()) {
                val off = (ci - 2) * 3f
                drawLine(rc, start = Offset(srcX + px * off, srcY + py * off), end = Offset(dstX + px * off, dstY + py * off), strokeWidth = 2.5f, cap = StrokeCap.Round)
            }
            drawLine(White07, start = src, end = dst, strokeWidth = 2f, cap = StrokeCap.Round)
            val impR = 12f + sin(time * 10f) * 4f
            drawCircle(White04, radius = impR, center = dst)
            drawCircle(SupportGold03, radius = impR * 1.5f, center = dst, style = StrokeGlow)
        }
    }
}

// ═══════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════

private fun DrawScope.drawBeam(src: Offset, dst: Offset, color: Color, glowColor: Color, width: Float) {
    drawLine(glowColor, alpha = 0.3f, start = src, end = dst, strokeWidth = width * 3f, cap = StrokeCap.Round)
    drawLine(color, start = src, end = dst, strokeWidth = width, cap = StrokeCap.Round)
    drawLine(BeamWhite07, start = src, end = dst, strokeWidth = width * 0.4f, cap = StrokeCap.Round)
    drawCircle(glowColor, alpha = 0.4f, radius = width * 2.5f, center = src)
}

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
        val hash = sin((seed * 127.1f + i * 311.7f + time * 3f).toDouble()).toFloat()
        val offset = hash * jitter * (1f - t * 0.3f)
        path.lineTo(srcX + dx * t + perpX * offset, srcY + dy * t + perpY * offset)
    }
    path.lineTo(dstX, dstY)

    drawPath(path, color, alpha = 0.2f, style = Stroke(width * 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, BeamWhite06, style = Stroke(width * 0.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawDefaultBeam(srcX: Float, srcY: Float, dstX: Float, dstY: Float) {
    val src = Offset(srcX, srcY); val dst = Offset(dstX, dstY)
    drawBeam(src, dst, DefaultBeamColor, DefaultGlow, 2f)
}
