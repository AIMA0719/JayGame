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

// Support grade 1 highlight
private val SupportGoldHighlight = Color(0xFFFFE082)

// Prismatic rainbow colors (Support grade 6)
private val PrismColors = listOf(
    Color(0xFFFF0000), Color(0xFFFF7700), Color(0xFFFFFF00),
    Color(0xFF00FF00), Color(0xFF0077FF), Color(0xFF0000FF), Color(0xFF8800FF)
)

// Grade 5-6 Fire
private val FireInferno = Color(0xFFFF2200)
private val FireInferno04 = Color(0xFFFF2200).copy(alpha = 0.4f)
private val FireWhiteHot = Color(0xFFFFFFCC)
private val FirePlasma = Color(0xFFFF6600).copy(alpha = 0.2f)

// Grade 5-6 Frost
private val FrostCrystal = Color(0xFFB3E5FC)
private val FrostNova = Color(0xFFE1F5FE).copy(alpha = 0.3f)
private val FrostDeep = Color(0xFF0288D1)

// Grade 5-6 Poison
private val PoisonToxic = Color(0xFF00E676)
private val PoisonMiasma = Color(0xFF1B5E20).copy(alpha = 0.3f)
private val PoisonAcid = Color(0xFFCCFF00).copy(alpha = 0.8f)

// Grade 5-6 Lightning
private val LightThunder = Color(0xFFFFFF88)
private val LightArc = Color(0xFFFFEE58).copy(alpha = 0.5f)
private val LightFlash = Color.White.copy(alpha = 0.8f)

// Grade 5-6 Support
private val SupportDivine = Color(0xFFFFF3E0)
private val SupportSacred = Color(0xFFFFD700).copy(alpha = 0.4f)
private val SupportPrismatic = Color(0xFFF8BBD0).copy(alpha = 0.5f)

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
            val family = com.example.jaygame.data.unitFamilyOf(unitId)
            val grade = com.example.jaygame.data.unitGradeOf(unitId)

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
        // LOD: skip particles when system is under load
        if (ParticleLOD.shouldSkipParticle(j)) continue
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
        4 -> {
            drawLine(FireRed015, start = src, end = dst, strokeWidth = 60f, cap = StrokeCap.Round)
            drawLine(FireDark, start = src, end = dst, strokeWidth = 12f, cap = StrokeCap.Round)
            drawLine(FireBright, alpha = 0.9f, start = src, end = dst, strokeWidth = 3.6f, cap = StrokeCap.Round)
            drawParticleTrail(srcX, srcY, dstX, dstY, 16, 20f, FireGold05, 0.5f, 4f, time, seed)
            drawCircle(FireDDark06, radius = 18f, center = dst)
            drawCircle(White03, radius = 8f, center = dst)
        }
        5 -> {
            // Triple-layer fire beam: outer glow, mid flame, bright core
            drawLine(FirePlasma, start = src, end = dst, strokeWidth = 80f, cap = StrokeCap.Round)
            drawLine(FireInferno04, start = src, end = dst, strokeWidth = 40f, cap = StrokeCap.Round)
            drawLine(FireDark, start = src, end = dst, strokeWidth = 14f, cap = StrokeCap.Round)
            drawLine(FireBright, alpha = 0.95f, start = src, end = dst, strokeWidth = 4f, cap = StrokeCap.Round)
            // Spiral fire particles
            val dx5 = dstX - srcX; val dy5 = dstY - srcY
            val len5 = sqrt(dx5 * dx5 + dy5 * dy5)
            if (len5 > 1f) {
                val px5 = -dy5 / len5; val py5 = dx5 / len5
                for (j in 0 until 14) {
                    val t = j.toFloat() / 14f
                    val spiralAngle = t * 12f + time * 10f + seed
                    val spiralR = 15f * sin(t * PI.toFloat())
                    val sx = srcX + dx5 * t + px5 * cos(spiralAngle) * spiralR
                    val sy = srcY + dy5 * t + py5 * cos(spiralAngle) * spiralR
                    drawCircle(FireGlow, alpha = 0.6f * (1f - t * 0.4f), radius = 4f, center = Offset(sx, sy))
                }
            }
            // Impact explosion with expanding ring
            val impR5 = 20f + sin(time * 8f) * 8f
            drawCircle(FireInferno04, radius = impR5, center = dst)
            drawCircle(FireDark, alpha = 0.5f, radius = impR5 * 0.6f, center = dst, style = Stroke(3f))
            drawCircle(FireBright, alpha = 0.8f, radius = 8f, center = dst)
        }
        else -> {
            // Grade 6 (Immortal): Quad-layer beam with chromatic aberration
            val dx6 = dstX - srcX; val dy6 = dstY - srcY
            val len6 = sqrt(dx6 * dx6 + dy6 * dy6)
            if (len6 < 1f) return
            val px6 = -dy6 / len6; val py6 = dx6 / len6
            // Outer plasma glow
            drawLine(FirePlasma, start = src, end = dst, strokeWidth = 100f, cap = StrokeCap.Round)
            // Chromatic aberration: offset beams in different fire colors
            for (ab in -1..1) {
                val off = ab * 4f
                val abSrc = Offset(srcX + px6 * off, srcY + py6 * off)
                val abDst = Offset(dstX + px6 * off, dstY + py6 * off)
                val abColor = when (ab) { -1 -> FireInferno; 0 -> FireDark; else -> FireGlow }
                drawLine(abColor, alpha = 0.8f, start = abSrc, end = abDst, strokeWidth = 10f, cap = StrokeCap.Round)
            }
            drawLine(FireWhiteHot, alpha = 0.9f, start = src, end = dst, strokeWidth = 4f, cap = StrokeCap.Round)
            // Massive particle storm (20+ particles)
            for (j in 0 until 22) {
                val t = j.toFloat() / 22f
                val wobble = sin(t * 14f + time * 12f + seed + j) * 25f * sin(t * PI.toFloat())
                val wobble2 = cos(t * 10f + time * 8f + seed) * 15f * sin(t * PI.toFloat())
                val fx = srcX + dx6 * t + px6 * wobble
                val fy = srcY + dy6 * t + py6 * wobble
                drawCircle(FireGlow, alpha = 0.5f * (1f - t * 0.3f), radius = 5f, center = Offset(fx, fy))
                val fx2 = srcX + dx6 * t + px6 * wobble2
                val fy2 = srcY + dy6 * t + py6 * wobble2
                drawCircle(FireInferno, alpha = 0.3f, radius = 3f, center = Offset(fx2, fy2))
            }
            // Impact shockwave + white-hot core
            val shockR = 28f + sin(time * 10f) * 12f
            drawCircle(FireInferno04, radius = shockR, center = dst)
            drawCircle(FireDark, alpha = 0.6f, radius = shockR * 0.7f, center = dst, style = Stroke(4f))
            drawCircle(FireDDark06, radius = 14f, center = dst)
            drawCircle(FireWhiteHot, alpha = 0.9f, radius = 8f, center = dst)
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
        4 -> {
            drawLine(FrostLight015, start = src, end = dst, strokeWidth = 48f, cap = StrokeCap.Round)
            drawLine(FrostBase, start = src, end = dst, strokeWidth = 12f, cap = StrokeCap.Round)
            drawLine(White09, start = src, end = dst, strokeWidth = 4.8f, cap = StrokeCap.Round)
            drawParticleTrail(srcX, srcY, dstX, dstY, 10, 15f, White04, 0.4f, 3f, time, seed)
            drawCircle(FrostBright07, radius = 16f, center = dst)
        }
        5 -> {
            // Wide crystalline beam with sharp ice particle trails
            drawLine(FrostNova, start = src, end = dst, strokeWidth = 60f, cap = StrokeCap.Round)
            drawLine(FrostCrystal, start = src, end = dst, strokeWidth = 14f, cap = StrokeCap.Round)
            drawLine(FrostBase, start = src, end = dst, strokeWidth = 8f, cap = StrokeCap.Round)
            drawLine(White09, start = src, end = dst, strokeWidth = 3f, cap = StrokeCap.Round)
            // Sharp ice particle trails
            val dx5 = dstX - srcX; val dy5 = dstY - srcY
            val len5 = sqrt(dx5 * dx5 + dy5 * dy5)
            if (len5 > 1f) {
                val px5 = -dy5 / len5; val py5 = dx5 / len5
                for (j in 0 until 12) {
                    val t = j.toFloat() / 12f
                    val sharpOff = sin(t * 16f + time * 6f + seed) * 18f * sin(t * PI.toFloat())
                    val ix = srcX + dx5 * t + px5 * sharpOff
                    val iy = srcY + dy5 * t + py5 * sharpOff
                    drawCircle(FrostCrystal, alpha = 0.5f * (1f - t * 0.4f), radius = 3.5f, center = Offset(ix, iy))
                }
            }
            // Expanding frost ring at impact
            val frostR5 = 18f + sin(time * 8f) * 6f
            drawCircle(FrostNova, radius = frostR5, center = dst)
            drawCircle(FrostCrystal, alpha = 0.4f, radius = frostR5 * 0.7f, center = dst, style = Stroke(2.5f))
            drawCircle(White07, radius = 6f, center = dst)
        }
        else -> {
            // Grade 6 (Immortal): Blizzard beam - multiple parallel ice beams
            val dx6 = dstX - srcX; val dy6 = dstY - srcY
            val len6 = sqrt(dx6 * dx6 + dy6 * dy6)
            if (len6 < 1f) return
            val px6 = -dy6 / len6; val py6 = dx6 / len6
            // Wide frost aura
            drawLine(FrostNova, start = src, end = dst, strokeWidth = 80f, cap = StrokeCap.Round)
            // Multiple parallel ice beams
            for (b in -2..2) {
                val off = b * 6f
                val bSrc = Offset(srcX + px6 * off, srcY + py6 * off)
                val bDst = Offset(dstX + px6 * off, dstY + py6 * off)
                drawLine(FrostDeep, alpha = 0.6f, start = bSrc, end = bDst, strokeWidth = 3f, cap = StrokeCap.Round)
            }
            drawLine(FrostCrystal, start = src, end = dst, strokeWidth = 10f, cap = StrokeCap.Round)
            drawLine(White09, start = src, end = dst, strokeWidth = 3.5f, cap = StrokeCap.Round)
            // Snowflake particles between beams
            for (j in 0 until 18) {
                val t = j.toFloat() / 18f
                val sOff = sin(t * 12f + time * 8f + seed + j * 0.7f) * 22f * sin(t * PI.toFloat())
                val sx = srcX + dx6 * t + px6 * sOff
                val sy = srcY + dy6 * t + py6 * sOff
                drawCircle(FrostCrystal, alpha = 0.4f * (1f - t * 0.3f), radius = 3f, center = Offset(sx, sy))
                drawCircle(Color.White, alpha = 0.2f, radius = 1.5f, center = Offset(sx, sy))
            }
            // Frost nova at impact
            val novaR = 24f + sin(time * 10f) * 10f
            drawCircle(FrostNova, radius = novaR, center = dst)
            drawCircle(FrostDeep, alpha = 0.4f, radius = novaR * 0.8f, center = dst, style = Stroke(3f))
            drawCircle(FrostCrystal, alpha = 0.5f, radius = novaR * 0.5f, center = dst, style = Stroke(2f))
            drawCircle(White09, radius = 7f, center = dst)
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
        4 -> {
            drawLine(PoisonBase012, start = src, end = dst, strokeWidth = 55f, cap = StrokeCap.Round)
            drawLine(PoisonDark, start = src, end = dst, strokeWidth = 11f, cap = StrokeCap.Round)
            drawLine(PoisonLime07, start = src, end = dst, strokeWidth = 3.3f, cap = StrokeCap.Round)
            drawParticleTrail(srcX, srcY, dstX, dstY, 12, 18f, PoisonBase, 0.4f, 5f, time, seed)
            drawCircle(PoisonGreen04, radius = 16f, center = dst)
        }
        5 -> {
            // Thick toxic beam with bubbling particle effect
            drawLine(PoisonMiasma, start = src, end = dst, strokeWidth = 65f, cap = StrokeCap.Round)
            drawLine(PoisonToxic, alpha = 0.7f, start = src, end = dst, strokeWidth = 14f, cap = StrokeCap.Round)
            drawLine(PoisonDark, start = src, end = dst, strokeWidth = 8f, cap = StrokeCap.Round)
            drawLine(PoisonAcid, start = src, end = dst, strokeWidth = 3f, cap = StrokeCap.Round)
            // Bubbling particle effect
            val dx5 = dstX - srcX; val dy5 = dstY - srcY
            val len5 = sqrt(dx5 * dx5 + dy5 * dy5)
            if (len5 > 1f) {
                val px5 = -dy5 / len5; val py5 = dx5 / len5
                for (j in 0 until 14) {
                    val t = j.toFloat() / 14f
                    val bubbleOff = sin(t * 8f + time * 7f + seed + j * 1.3f) * 16f * sin(t * PI.toFloat())
                    val bx = srcX + dx5 * t + px5 * bubbleOff
                    val by = srcY + dy5 * t + py5 * bubbleOff
                    val bubbleR = 3f + sin(time * 10f + j * 2f) * 2f
                    drawCircle(PoisonToxic, alpha = 0.4f * (1f - t * 0.3f), radius = bubbleR, center = Offset(bx, by))
                }
            }
            // Miasma cloud at impact
            val mR = 20f + sin(time * 6f) * 6f
            drawCircle(PoisonMiasma, radius = mR, center = dst)
            drawCircle(PoisonToxic, alpha = 0.3f, radius = mR * 0.7f, center = dst)
            drawCircle(PoisonAcid, radius = 6f, center = dst)
        }
        else -> {
            // Grade 6 (Immortal): Multi-tendril poison streams weaving around center
            val dx6 = dstX - srcX; val dy6 = dstY - srcY
            val len6 = sqrt(dx6 * dx6 + dy6 * dy6)
            if (len6 < 1f) return
            val px6 = -dy6 / len6; val py6 = dx6 / len6
            // Background toxic fog
            drawLine(PoisonMiasma, start = src, end = dst, strokeWidth = 90f, cap = StrokeCap.Round)
            // Multi-tendril weaving streams
            for (tendril in 0 until 4) {
                val phase = tendril * PI.toFloat() / 2f
                for (seg in 0 until 16) {
                    val t1 = seg.toFloat() / 16f
                    val t2 = (seg + 1).toFloat() / 16f
                    val wave1 = sin(t1 * 8f + time * 6f + phase) * 20f * sin(t1 * PI.toFloat())
                    val wave2 = sin(t2 * 8f + time * 6f + phase) * 20f * sin(t2 * PI.toFloat())
                    val s = Offset(srcX + dx6 * t1 + px6 * wave1, srcY + dy6 * t1 + py6 * wave1)
                    val e = Offset(srcX + dx6 * t2 + px6 * wave2, srcY + dy6 * t2 + py6 * wave2)
                    drawLine(PoisonToxic, alpha = 0.5f, start = s, end = e, strokeWidth = 4f, cap = StrokeCap.Round)
                }
            }
            // Center line
            drawLine(PoisonDark, start = src, end = dst, strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(PoisonAcid, start = src, end = dst, strokeWidth = 2.5f, cap = StrokeCap.Round)
            // Expanding toxic cloud with dripping particles at impact
            val cloudR = 26f + sin(time * 8f) * 10f
            drawCircle(PoisonMiasma, radius = cloudR, center = dst)
            drawCircle(PoisonToxic, alpha = 0.35f, radius = cloudR * 0.7f, center = dst)
            drawCircle(PoisonDark, alpha = 0.4f, radius = cloudR * 0.5f, center = dst, style = Stroke(3f))
            // Dripping particles
            for (d in 0 until 5) {
                val dripOff = (time * 30f + d * 12f) % 30f
                val dAngle = d * 72f * PI.toFloat() / 180f
                drawCircle(PoisonToxic, alpha = 0.5f, radius = 2.5f, center = Offset(
                    dst.x + cos(dAngle) * 10f,
                    dst.y + sin(dAngle) * 10f + dripOff
                ))
            }
            drawCircle(PoisonAcid, radius = 7f, center = dst)
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
        4 -> {
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
        5 -> {
            // Chain lightning: main bolt plus 2 forking branches
            drawLightningBolt(srcX, srcY, dstX, dstY, LightThunder, 12, 22f, 8f, time, seed, path)
            drawLightningBolt(srcX, srcY, dstX, dstY, LightArc, 12, 28f, 14f, time, seed + 5, path)
            // Forking branches from midpoints
            for (fork in 0 until 2) {
                val ft = 0.35f + fork * 0.3f
                val fx = srcX + (dstX - srcX) * ft; val fy = srcY + (dstY - srcY) * ft
                val fAngle = ((seed + fork * 137) % 360) * PI.toFloat() / 180f
                val fLen = 50f + fork * 20f
                drawLightningBolt(fx, fy, fx + cos(fAngle) * fLen, fy + sin(fAngle) * fLen,
                    LightThunder, 6, 12f, 4f, time, seed + fork * 11, path)
            }
            // Bright impact
            val impR5 = 14f + sin(time * 10f) * 6f
            drawCircle(LightArc, radius = impR5 * 1.5f, center = dst)
            drawCircle(LightThunder, alpha = 0.6f, radius = impR5, center = dst)
            drawCircle(White08, radius = 7f, center = dst)
        }
        else -> {
            // Grade 6 (Immortal): Thunder storm - 3 parallel main bolts with cross-connecting arcs
            val dx6 = dstX - srcX; val dy6 = dstY - srcY
            val len6 = sqrt(dx6 * dx6 + dy6 * dy6)
            if (len6 < 1f) return
            val px6 = -dy6 / len6; val py6 = dx6 / len6
            // 3 parallel main bolts
            for (b in -1..1) {
                val off = b * 10f
                drawLightningBolt(srcX + px6 * off, srcY + py6 * off,
                    dstX + px6 * off * 0.5f, dstY + py6 * off * 0.5f,
                    LightThunder, 14, 20f, 7f, time, seed + b * 17, path)
                // Glow layer for each bolt
                drawLightningBolt(srcX + px6 * off, srcY + py6 * off,
                    dstX + px6 * off * 0.5f, dstY + py6 * off * 0.5f,
                    LightArc, 14, 26f, 16f, time, seed + b * 23, path)
            }
            // Cross-connecting arcs between parallel bolts
            for (c in 0 until 4) {
                val ct = 0.2f + c * 0.2f
                val cx = srcX + dx6 * ct; val cy = srcY + dy6 * ct
                val cOff1 = -10f; val cOff2 = 10f
                drawLightningBolt(
                    cx + px6 * cOff1, cy + py6 * cOff1,
                    cx + px6 * cOff2, cy + py6 * cOff2,
                    LightBase, 4, 8f, 3f, time, seed + c * 7, path)
            }
            // Bright flash at impact
            val flashR = 18f + sin(time * 14f) * 10f
            drawCircle(LightFlash, radius = flashR, center = dst)
            // Expanding electrical field
            drawCircle(LightArc, radius = flashR * 1.6f, center = dst)
            drawCircle(LightThunder, alpha = 0.5f, radius = flashR * 1.2f, center = dst, style = Stroke(3f))
            drawCircle(LightBase, alpha = 0.3f, radius = flashR * 2f, center = dst, style = Stroke(1.5f))
            drawCircle(White09, radius = 8f, center = dst)
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
            drawBeam(src, dst, SupportGold, SupportGoldHighlight, 3.5f)
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
        4 -> {
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
        5 -> {
            // Divine beam with golden halo particles
            drawLine(SupportSacred, start = src, end = dst, strokeWidth = 70f, cap = StrokeCap.Round)
            drawLine(SupportDivine, alpha = 0.7f, start = src, end = dst, strokeWidth = 14f, cap = StrokeCap.Round)
            drawLine(SupportGold, start = src, end = dst, strokeWidth = 8f, cap = StrokeCap.Round)
            drawLine(White09, start = src, end = dst, strokeWidth = 3f, cap = StrokeCap.Round)
            // Golden halo particles
            val dx5 = dstX - srcX; val dy5 = dstY - srcY
            val len5 = sqrt(dx5 * dx5 + dy5 * dy5)
            if (len5 > 1f) {
                val px5 = -dy5 / len5; val py5 = dx5 / len5
                for (j in 0 until 10) {
                    val t = j.toFloat() / 10f
                    val haloAngle = t * 10f + time * 8f + seed
                    val haloR = 12f * sin(t * PI.toFloat())
                    val hx = srcX + dx5 * t + px5 * cos(haloAngle) * haloR
                    val hy = srcY + dy5 * t + py5 * cos(haloAngle) * haloR
                    drawCircle(SupportSacred, radius = 4f, center = Offset(hx, hy))
                    drawCircle(SupportDivine, alpha = 0.5f, radius = 2f, center = Offset(hx, hy))
                }
            }
            // Star-burst at impact
            val starR = 16f + sin(time * 10f) * 6f
            for (ray in 0 until 8) {
                val rayAngle = ray * PI.toFloat() / 4f + time * 2f
                val rayEnd = Offset(dst.x + cos(rayAngle) * starR, dst.y + sin(rayAngle) * starR)
                drawLine(SupportSacred, start = dst, end = rayEnd, strokeWidth = 2f, cap = StrokeCap.Round)
            }
            drawCircle(SupportDivine, alpha = 0.5f, radius = starR * 0.6f, center = dst)
            drawCircle(White08, radius = 7f, center = dst)
        }
        else -> {
            // Grade 6 (Immortal): Prismatic rainbow beam with sacred geometry
            val dx6 = dstX - srcX; val dy6 = dstY - srcY
            val len6 = sqrt(dx6 * dx6 + dy6 * dy6)
            if (len6 < 1f) return
            val px6 = -dy6 / len6; val py6 = dx6 / len6
            // Outer divine glow
            drawLine(SupportPrismatic, start = src, end = dst, strokeWidth = 90f, cap = StrokeCap.Round)
            drawLine(SupportSacred, start = src, end = dst, strokeWidth = 50f, cap = StrokeCap.Round)
            // 7 parallel rainbow color lines
            for ((ci, pc) in PrismColors.withIndex()) {
                val off = (ci - 3) * 4f
                val pSrc = Offset(srcX + px6 * off, srcY + py6 * off)
                val pDst = Offset(dstX + px6 * off, dstY + py6 * off)
                drawLine(pc, alpha = 0.6f, start = pSrc, end = pDst, strokeWidth = 3f, cap = StrokeCap.Round)
            }
            // White core beam
            drawLine(White09, start = src, end = dst, strokeWidth = 3f, cap = StrokeCap.Round)
            // Sacred geometry pattern at impact: expanding hexagonal glow
            val hexR = 20f + sin(time * 8f) * 8f
            for (h in 0 until 6) {
                val a1 = h * PI.toFloat() / 3f + time * 1.5f
                val a2 = (h + 1) * PI.toFloat() / 3f + time * 1.5f
                val p1 = Offset(dst.x + cos(a1) * hexR, dst.y + sin(a1) * hexR)
                val p2 = Offset(dst.x + cos(a2) * hexR, dst.y + sin(a2) * hexR)
                drawLine(SupportSacred, start = p1, end = p2, strokeWidth = 2.5f, cap = StrokeCap.Round)
                // Inner hexagon
                val innerR = hexR * 0.5f
                val ip1 = Offset(dst.x + cos(a1) * innerR, dst.y + sin(a1) * innerR)
                val ip2 = Offset(dst.x + cos(a2) * innerR, dst.y + sin(a2) * innerR)
                drawLine(SupportDivine, alpha = 0.5f, start = ip1, end = ip2, strokeWidth = 1.5f, cap = StrokeCap.Round)
            }
            drawCircle(SupportPrismatic, radius = hexR * 1.2f, center = dst)
            drawCircle(SupportDivine, alpha = 0.6f, radius = hexR * 0.4f, center = dst)
            drawCircle(White09, radius = 8f, center = dst)
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
