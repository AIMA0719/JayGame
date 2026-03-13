package com.example.jaygame.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.bridge.SkillEvent
import com.example.jaygame.bridge.SkillVfxType
import kotlin.math.cos
import kotlin.math.sin

// Pre-allocated skill VFX colors
private val SkillFireColor = Color(0xFFFF6B35)
private val SkillFrostColor = Color(0xFF64B5F6)
private val SkillPoisonColor = Color(0xFF81C784)
private val SkillLightningColor = Color(0xFFFFD54F)
private val SkillSupportColor = Color(0xFFCE93D8)
private val SkillWindColor = Color(0xFF80CBC4)

/**
 * Skill effect overlay — renders active skill events from BattleBridge.
 * Each SkillVfxType has its own Canvas rendering function.
 */
@Composable
fun SkillEffectOverlay() {
    val skillEvents by BattleBridge.skillEvents.collectAsState()

    // Cleanup expired events
    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { _ ->
                BattleBridge.clearExpiredSkillEvents()
            }
        }
    }

    if (skillEvents.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val now = System.currentTimeMillis()
        for (event in skillEvents) {
            val elapsed = (now - event.startTime) / 1000f
            val progress = (elapsed / event.duration).coerceIn(0f, 1f)
            renderSkillEvent(event, progress)
        }
    }
}

private fun DrawScope.renderSkillEvent(event: SkillEvent, progress: Float) {
    when (event.type) {
        // ── Fire ──
        SkillVfxType.LINGERING_FLAME -> renderLingeringFlame(event, progress)
        SkillVfxType.FIRESTORM_METEOR -> renderFirestormMeteor(event, progress)
        SkillVfxType.VOLCANIC_ERUPTION -> renderVolcanicEruption(event, progress)
        SkillVfxType.PHOENIX_CARPET_BOMB -> renderGenericAoe(event, progress, SkillFireColor)
        SkillVfxType.PHOENIX_REVIVE -> renderGenericBurst(event, progress, SkillFireColor)
        SkillVfxType.SUPERNOVA -> renderSupernova(event, progress)

        // ── Frost ──
        SkillVfxType.FROST_NOVA -> renderFrostNova(event, progress)
        SkillVfxType.ABSOLUTE_ZERO -> renderGenericAoe(event, progress, SkillFrostColor)
        SkillVfxType.ICE_AGE_BLIZZARD -> renderGenericAoe(event, progress, SkillFrostColor)
        SkillVfxType.ETERNAL_WINTER -> renderGenericAoe(event, progress, SkillFrostColor)
        SkillVfxType.TIME_STOP -> renderTimeStop(event, progress)

        // ── Poison ──
        SkillVfxType.POISON_CLOUD -> renderPoisonCloud(event, progress)
        SkillVfxType.ACID_SPRAY -> renderGenericAoe(event, progress, SkillPoisonColor)
        SkillVfxType.TOXIC_DOMAIN -> renderGenericAoe(event, progress, SkillPoisonColor)
        SkillVfxType.NIDHOGG_BREATH -> renderGenericAoe(event, progress, SkillPoisonColor)
        SkillVfxType.UNIVERSAL_DECAY -> renderGenericAoe(event, progress, SkillPoisonColor)

        // ── Lightning ──
        SkillVfxType.LIGHTNING_STRIKE -> renderLightningStrike(event, progress)
        SkillVfxType.STATIC_FIELD -> renderGenericAoe(event, progress, SkillLightningColor)
        SkillVfxType.THUNDERSTORM -> renderGenericAoe(event, progress, SkillLightningColor)
        SkillVfxType.MJOLNIR_THROW -> renderGenericAoe(event, progress, SkillLightningColor)
        SkillVfxType.DIVINE_PUNISHMENT -> renderGenericAoe(event, progress, SkillLightningColor)

        // ── Support ──
        SkillVfxType.HEAL_PULSE -> renderHealPulse(event, progress)
        SkillVfxType.WAR_SONG_AURA -> renderGenericAoe(event, progress, SkillSupportColor)
        SkillVfxType.DIVINE_SHIELD -> renderGenericAoe(event, progress, SkillSupportColor)
        SkillVfxType.HARMONY_FIELD -> renderGenericAoe(event, progress, SkillSupportColor)
        SkillVfxType.GENESIS_LIGHT -> renderGenericAoe(event, progress, SkillSupportColor)

        // ── Wind ──
        SkillVfxType.CYCLONE_PULL -> renderCyclonePull(event, progress)
        SkillVfxType.EYE_OF_STORM -> renderGenericAoe(event, progress, SkillWindColor)
        SkillVfxType.VACUUM_SLASH -> renderGenericAoe(event, progress, SkillWindColor)
        SkillVfxType.DIMENSIONAL_SLASH -> renderGenericAoe(event, progress, SkillWindColor)
        SkillVfxType.BREATH_OF_ALL -> renderGenericAoe(event, progress, SkillWindColor)
    }
}

// ─── Common renderers ─────────────────────────────────────────

/** Generic expanding circle AoE */
private fun DrawScope.renderGenericAoe(event: SkillEvent, progress: Float, color: Color) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxRadius = event.radius * size.minDimension
    val alpha = (1f - progress).coerceIn(0f, 0.6f)
    val radius = maxRadius * progress.coerceAtLeast(0.1f)

    drawCircle(
        color = color.copy(alpha = alpha * 0.3f),
        radius = radius,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = color.copy(alpha = alpha * 0.5f),
        radius = radius,
        center = Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
    )
}

/** Generic burst (radial lines) */
private fun DrawScope.renderGenericBurst(event: SkillEvent, progress: Float, color: Color) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val alpha = (1f - progress).coerceIn(0f, 0.7f)

    for (i in 0 until 12) {
        val angle = (i / 12f) * 6.283f
        val r = maxR * progress
        drawLine(
            color = color.copy(alpha = alpha * 0.5f),
            start = Offset(cx, cy),
            end = Offset(cx + cos(angle) * r, cy + sin(angle) * r),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
    }
}

// ─── Fire renderers ───────────────────────────────────────────

/** Lingering flame — ground fire circle with flickering particles */
private fun DrawScope.renderLingeringFlame(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val radius = event.radius * size.minDimension
    val alpha = (1f - progress * 0.7f).coerceIn(0f, 0.6f)

    // Ground glow
    drawCircle(
        color = SkillFireColor.copy(alpha = alpha * 0.4f),
        radius = radius,
        center = Offset(cx, cy),
    )
    // Flame particles around perimeter
    val flicker = sin(progress * 20f)
    for (i in 0 until 8) {
        val angle = (i / 8f) * 6.283f + progress * 3f
        val r = radius * (0.6f + flicker * 0.2f)
        val pSize = 3f + sin(progress * 15f + i) * 1.5f
        drawCircle(
            color = Color(0xFFFFAA00).copy(alpha = alpha * 0.7f),
            radius = pSize,
            center = Offset(cx + cos(angle) * r, cy + sin(angle) * r),
        )
    }
}

/** Firestorm meteor — falling impact + shockwave */
private fun DrawScope.renderFirestormMeteor(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension

    if (progress < 0.3f) {
        // Meteor falling — line from top
        val fallProgress = progress / 0.3f
        val meteorY = cy * fallProgress
        drawCircle(
            color = Color(0xFFFF4400).copy(alpha = 0.8f),
            radius = 8f,
            center = Offset(cx, meteorY),
        )
        // Trail
        drawLine(
            color = Color(0xFFFF6600).copy(alpha = 0.4f),
            start = Offset(cx, 0f),
            end = Offset(cx, meteorY),
            strokeWidth = 4f,
        )
    } else {
        // Impact shockwave
        val impactProg = (progress - 0.3f) / 0.7f
        val shockR = maxR * impactProg
        val alpha = (1f - impactProg).coerceIn(0f, 0.7f)

        drawCircle(
            color = SkillFireColor.copy(alpha = alpha * 0.5f),
            radius = shockR,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = Color(0xFFFF4400).copy(alpha = alpha),
            radius = shockR,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
        )

        // Screen flash on impact
        if (impactProg < 0.15f) {
            drawRect(
                color = Color(0xFFFF6600).copy(alpha = (0.15f - impactProg) / 0.15f * 0.3f),
                size = size,
            )
        }
    }
}

/** Volcanic eruption — rising lava particles */
private fun DrawScope.renderVolcanicEruption(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val alpha = if (progress < 0.8f) 0.6f else (1f - progress) / 0.2f * 0.6f

    // Base glow
    drawCircle(
        color = Color(0xFFFF2200).copy(alpha = alpha * 0.3f),
        radius = maxR,
        center = Offset(cx, cy),
    )

    // Rising lava particles
    for (i in 0 until 10) {
        val angle = (i / 10f) * 6.283f
        val riseOffset = (progress * 80f + i * 20f) % 60f
        val px = cx + cos(angle) * maxR * 0.5f
        val py = cy - riseOffset
        drawCircle(
            color = Color(0xFFFF6600).copy(alpha = alpha * 0.6f),
            radius = 3f,
            center = Offset(px, py),
        )
    }
}

/** Supernova — full-screen white explosion */
private fun DrawScope.renderSupernova(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height

    if (progress < 0.3f) {
        // Charge-up — shrinking bright center
        val chargeProg = progress / 0.3f
        val radius = size.minDimension * 0.3f * (1f - chargeProg * 0.8f)
        drawCircle(
            color = Color.White.copy(alpha = 0.5f + chargeProg * 0.3f),
            radius = radius,
            center = Offset(cx, cy),
        )
    } else {
        // Explosion
        val explodeProg = (progress - 0.3f) / 0.7f
        val radius = size.maxDimension * explodeProg
        val alpha = (1f - explodeProg).coerceIn(0f, 0.8f)

        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.6f),
            radius = radius,
            center = Offset(cx, cy),
        )

        if (explodeProg < 0.3f) {
            drawRect(
                color = Color.White.copy(alpha = (0.3f - explodeProg) / 0.3f * 0.5f),
                size = size,
            )
        }
    }
}

// ─── Frost renderers ──────────────────────────────────────────

/** Frost nova — expanding ice ring */
private fun DrawScope.renderFrostNova(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val alpha = (1f - progress).coerceIn(0f, 0.7f)
    val radius = maxR * progress.coerceAtLeast(0.05f)

    // Ice ring
    drawCircle(
        color = SkillFrostColor.copy(alpha = alpha * 0.4f),
        radius = radius,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.6f),
        radius = radius,
        center = Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
    )

    // Ice crystal particles
    for (i in 0 until 6) {
        val angle = (i / 6f) * 6.283f + progress * 2f
        val r = radius * 0.8f
        drawCircle(
            color = Color(0xFFBBDDFF).copy(alpha = alpha * 0.7f),
            radius = 3f,
            center = Offset(cx + cos(angle) * r, cy + sin(angle) * r),
        )
    }
}

/** Time stop — grayscale overlay + clock gears */
private fun DrawScope.renderTimeStop(event: SkillEvent, progress: Float) {
    val alpha = if (progress < 0.1f) progress / 0.1f * 0.3f
               else if (progress > 0.9f) (1f - progress) / 0.1f * 0.3f
               else 0.3f

    // Semi-transparent dark overlay
    drawRect(
        color = Color(0xFF334466).copy(alpha = alpha),
        size = size,
    )

    // Clock gear in center
    val cx = size.width / 2f
    val cy = size.height / 2f
    val gearR = size.minDimension * 0.15f
    for (i in 0 until 12) {
        val angle = (i / 12f) * 6.283f + progress * 4f
        drawLine(
            color = Color.White.copy(alpha = alpha * 1.5f),
            start = Offset(cx + cos(angle) * gearR * 0.7f, cy + sin(angle) * gearR * 0.7f),
            end = Offset(cx + cos(angle) * gearR, cy + sin(angle) * gearR),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )
    }
}

// ─── Poison renderers ─────────────────────────────────────────

/** Poison cloud — bubbling green circle */
private fun DrawScope.renderPoisonCloud(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val radius = event.radius * size.minDimension
    val alpha = if (progress < 0.1f) progress / 0.1f * 0.5f
               else if (progress > 0.8f) (1f - progress) / 0.2f * 0.5f
               else 0.5f

    // Cloud base
    drawCircle(
        color = SkillPoisonColor.copy(alpha = alpha * 0.3f),
        radius = radius,
        center = Offset(cx, cy),
    )

    // Bubbles
    for (i in 0 until 6) {
        val angle = (i / 6f) * 6.283f + progress * 2f
        val r = radius * (0.3f + sin(progress * 10f + i * 2f) * 0.3f)
        val bubbleSize = 4f + sin(progress * 8f + i) * 2f
        drawCircle(
            color = Color(0xFF44AA44).copy(alpha = alpha * 0.5f),
            radius = bubbleSize,
            center = Offset(cx + cos(angle) * r, cy + sin(angle) * r),
        )
    }
}

// ─── Lightning renderers ──────────────────────────────────────

/** Lightning strike — zigzag bolt from top */
private fun DrawScope.renderLightningStrike(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val alpha = if (progress < 0.5f) 0.8f else (1f - progress) / 0.5f * 0.8f

    if (progress < 0.6f) {
        // Main bolt — zigzag
        var boltX = cx
        var boltY = 0f
        val segCount = 8
        val segH = cy / segCount
        for (s in 0 until segCount) {
            val nextX = cx + sin(s.toFloat() * 3f + progress * 5f) * 20f
            val nextY = boltY + segH
            drawLine(
                color = SkillLightningColor.copy(alpha = alpha),
                start = Offset(boltX, boltY),
                end = Offset(nextX, nextY),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
            boltX = nextX
            boltY = nextY
        }
    }

    // Impact flash
    if (progress < 0.3f) {
        val flashR = size.minDimension * 0.1f * (1f - progress / 0.3f)
        drawCircle(
            color = Color.White.copy(alpha = (1f - progress / 0.3f) * 0.5f),
            radius = flashR,
            center = Offset(cx, cy),
        )
    }
}

// ─── Support renderers ────────────────────────────────────────

/** Heal pulse — expanding green ring + rising particles */
private fun DrawScope.renderHealPulse(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val alpha = (1f - progress).coerceIn(0f, 0.6f)

    // Expanding ring
    val radius = maxR * progress.coerceAtLeast(0.1f)
    drawCircle(
        color = Color(0xFF44DD44).copy(alpha = alpha * 0.3f),
        radius = radius,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = Color(0xFF88FF88).copy(alpha = alpha * 0.5f),
        radius = radius,
        center = Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
    )

    // Rising heal particles
    for (i in 0 until 5) {
        val angle = (i / 5f) * 6.283f
        val r = radius * 0.6f
        val rise = progress * 30f
        drawCircle(
            color = Color(0xFF44FF44).copy(alpha = alpha * 0.6f),
            radius = 2.5f,
            center = Offset(cx + cos(angle) * r, cy + sin(angle) * r - rise),
        )
    }
}

// ─── Wind renderers ───────────────────────────────────────────

/** Cyclone pull — rotating spiral lines */
private fun DrawScope.renderCyclonePull(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val alpha = if (progress < 0.1f) progress / 0.1f * 0.5f
               else if (progress > 0.8f) (1f - progress) / 0.2f * 0.5f
               else 0.5f

    // Spiral wind lines
    for (i in 0 until 8) {
        val baseAngle = (i / 8f) * 6.283f + progress * 8f
        val r1 = maxR * 0.9f
        val r2 = maxR * 0.3f
        drawLine(
            color = SkillWindColor.copy(alpha = alpha * 0.6f),
            start = Offset(cx + cos(baseAngle) * r1, cy + sin(baseAngle) * r1),
            end = Offset(cx + cos(baseAngle + 0.5f) * r2, cy + sin(baseAngle + 0.5f) * r2),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )
    }

    // Center vortex
    drawCircle(
        color = SkillWindColor.copy(alpha = alpha * 0.2f),
        radius = maxR * 0.3f,
        center = Offset(cx, cy),
    )
}
