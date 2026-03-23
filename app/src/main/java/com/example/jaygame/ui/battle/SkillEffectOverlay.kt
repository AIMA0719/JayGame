package com.example.jaygame.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.bridge.SkillEvent
import com.example.jaygame.bridge.SkillVfxType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ═══════════════════════════════════════════════════════
// Pre-allocated skill VFX colors
// ═══════════════════════════════════════════════════════

// Fire palette
private val FireCore = Color(0xFFFF6B35)
private val FireOrange = Color(0xFFFF9800)
private val FireYellow = Color(0xFFFFDD44)
private val FireDark = Color(0xFFFF2200)
private val FireEmber = Color(0xFFFFAA00)
private val FireWhiteHot = Color(0xFFFFFFCC)

// Frost palette
private val FrostBase = Color(0xFF64B5F6)
private val FrostLight = Color(0xFFBBDDFF)
private val FrostCrystal = Color(0xFFE3F2FD)
private val FrostDeep = Color(0xFF1565C0)

// Poison palette
private val PoisonBase = Color(0xFF81C784)
private val PoisonDark = Color(0xFF388E3C)
private val PoisonAcid = Color(0xFFCCFF00)
private val PoisonNeon = Color(0xFF00E676)
private val PoisonMiasma = Color(0xFF1B5E20)

// Lightning palette
private val LightBase = Color(0xFFFFD54F)
private val LightBright = Color(0xFFFFFF00)

// Support palette
private val SupportPurple = Color(0xFFCE93D8)
private val SupportGold = Color(0xFFD4A847)
private val SupportHoly = Color(0xFFFFF8E1)
private val SupportWhiteGold = Color(0xFFFFE082)

// Heal pulse colors (pre-allocated)
private val HealGreen = Color(0xFF44DD44)
private val HealBright = Color(0xFF88FF88)
private val HealSparkle = Color(0xFF44FF44)

// Wind palette
private val WindTeal = Color(0xFF80CBC4)
private val WindCyan = Color(0xFF4DD0E1)
private val WindLight = Color(0xFFB2EBF2)
private val WindDark = Color(0xFF00695C)

// Shared
private val White = Color.White

/**
 * Skill effect overlay — renders active skill events from BattleBridge.
 * Each SkillVfxType has its own unique Canvas rendering.
 * 운빨존많겜-style flashy, dramatic effects.
 */
/**
 * @param fieldOffset top-left of the battle field in root coordinates (px)
 * @param fieldSize   pixel size of the battle field
 */
@Composable
fun SkillEffectOverlay(
    fieldOffset: Offset = Offset.Zero,
    fieldSize: Size = Size.Zero,
) {
    val skillEvents by BattleBridge.skillEvents.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { _ ->
                BattleBridge.clearExpiredSkillEvents()
            }
        }
    }

    if (skillEvents.isEmpty() || fieldSize == Size.Zero) return

    val eventsSnapshot = skillEvents

    // 배틀필드 좌표계에 맞춰 스킬 이펙트 렌더링
    Canvas(modifier = Modifier.fillMaxSize()) {
        val now = System.currentTimeMillis()
        // Canvas를 필드 원점으로 이동 + FieldSizeDrawScope로 size를 필드 크기로 변경
        drawContext.canvas.save()
        drawContext.canvas.translate(fieldOffset.x, fieldOffset.y)
        val fieldDraw = FieldSizeDrawScope(this, fieldSize)
        with(fieldDraw) {
            for (i in eventsSnapshot.indices) {
                val event = eventsSnapshot.getOrNull(i) ?: continue
                val elapsed = (now - event.startTime) / 1000f
                if (event.duration <= 0f) continue
                val progress = (elapsed / event.duration).coerceIn(0f, 1f)
                renderSkillEvent(event, progress)
            }
        }
        drawContext.canvas.restore()
    }
}

/** DrawScope wrapper — returns [fieldSize] from [size] so render functions
 *  use field-relative coordinates while drawing on a full-screen canvas. */
private class FieldSizeDrawScope(
    private val delegate: DrawScope,
    override val size: Size,
) : DrawScope by delegate

private fun DrawScope.renderSkillEvent(event: SkillEvent, progress: Float) {
    when (event.type) {
        // ── Fire (Hero, Legend, Mythic) ──
        SkillVfxType.LINGERING_FLAME -> renderLingeringFlame(event, progress)
        SkillVfxType.FIRESTORM_METEOR -> renderFirestormMeteor(event, progress)
        SkillVfxType.VOLCANIC_ERUPTION -> renderVolcanicEruption(event, progress)

        // ── Frost (Hero, Legend, Mythic) ──
        SkillVfxType.FROST_NOVA -> renderFrostNova(event, progress)
        SkillVfxType.ABSOLUTE_ZERO -> renderAbsoluteZero(event, progress)
        SkillVfxType.ICE_AGE_BLIZZARD -> renderIceAgeBlizzard(event, progress)

        // ── Poison (Hero, Legend, Mythic) ──
        SkillVfxType.POISON_CLOUD -> renderPoisonCloud(event, progress)
        SkillVfxType.ACID_SPRAY -> renderAcidSpray(event, progress)
        SkillVfxType.TOXIC_DOMAIN -> renderToxicDomain(event, progress)

        // ── Lightning (Hero, Legend, Mythic) ──
        SkillVfxType.LIGHTNING_STRIKE -> renderLightningStrike(event, progress)
        SkillVfxType.STATIC_FIELD -> renderStaticField(event, progress)
        SkillVfxType.THUNDERSTORM -> renderThunderstorm(event, progress)

        // ── Support (Hero, Legend, Mythic) ──
        SkillVfxType.HEAL_PULSE -> renderHealPulse(event, progress)
        SkillVfxType.WAR_SONG_AURA -> renderWarSongAura(event, progress)
        SkillVfxType.DIVINE_SHIELD -> renderDivineShield(event, progress)

        // ── Wind (Hero, Legend, Mythic) ──
        SkillVfxType.CYCLONE_PULL -> renderCyclonePull(event, progress)
        SkillVfxType.EYE_OF_STORM -> renderEyeOfStorm(event, progress)
        SkillVfxType.VACUUM_SLASH -> renderVacuumSlash(event, progress)
    }
}


// ═══════════════════════════════════════════════════════
// FIRE SKILLS
// ═══════════════════════════════════════════════════════

/** Lingering flame — ground fire with rising embers and heat distortion */
private fun DrawScope.renderLingeringFlame(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val radius = event.radius * size.minDimension
    val alpha = (1f - progress * 0.7f).coerceIn(0f, 0.7f)

    // Ground heat glow (layered radial)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                FireYellow.copy(alpha = alpha * 0.4f),
                FireCore.copy(alpha = alpha * 0.3f),
                FireDark.copy(alpha = alpha * 0.15f),
                Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = radius * 1.2f,
        ),
        radius = radius * 1.2f,
        center = Offset(cx, cy),
    )

    // Inner fire ring
    drawCircle(
        color = FireOrange.copy(alpha = alpha * 0.6f),
        radius = radius * 0.8f,
        center = Offset(cx, cy),
        style = Stroke(width = 3f),
    )

    // Flame particles around perimeter — rising with flickering
    for (i in 0 until 12) {
        val angle = (i / 12f) * 6.283f + progress * 3f
        val flicker = sin(progress * 20f + i * 2.3f)
        val r = radius * (0.5f + flicker * 0.25f)
        val riseOffset = sin(progress * 15f + i * 1.7f) * 8f
        val pSize = 3f + sin(progress * 12f + i * 1.5f) * 1.5f
        // Outer ember (orange)
        drawCircle(
            color = FireEmber.copy(alpha = alpha * 0.6f),
            radius = pSize,
            center = Offset(cx + cos(angle) * r, cy + sin(angle) * r - riseOffset),
        )
        // Inner hot core (yellow)
        drawCircle(
            color = FireYellow.copy(alpha = alpha * 0.4f),
            radius = pSize * 0.5f,
            center = Offset(cx + cos(angle) * r, cy + sin(angle) * r - riseOffset),
        )
    }

    // Rising ember sparks (float upward and fade)
    for (i in 0 until 6) {
        val sparkPhase = (progress * 3f + i * 0.5f) % 1f
        val sparkAngle = (i / 6f) * 6.283f + progress * 1.5f
        val sparkR = radius * 0.3f * (1f - sparkPhase)
        val sparkRise = sparkPhase * radius * 0.8f
        val sparkAlpha = (1f - sparkPhase) * alpha * 0.5f
        drawCircle(
            color = FireYellow.copy(alpha = sparkAlpha),
            radius = 2f,
            center = Offset(cx + cos(sparkAngle) * sparkR, cy - sparkRise),
        )
    }
}

/** Firestorm meteor — dramatic falling impact + expanding shockwave ring */
private fun DrawScope.renderFirestormMeteor(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension

    if (progress < 0.3f) {
        val fallProgress = progress / 0.3f
        val meteorY = cy * fallProgress
        val meteorSize = 10f + fallProgress * 4f

        // Fiery trail
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, FireOrange.copy(alpha = 0.3f), FireDark.copy(alpha = 0.6f)),
            ),
            start = Offset(cx - 3f, 0f),
            end = Offset(cx, meteorY),
            strokeWidth = 8f,
            cap = StrokeCap.Round,
        )

        // Meteor body (layered)
        drawCircle(FireDark.copy(alpha = 0.8f), radius = meteorSize, center = Offset(cx, meteorY))
        drawCircle(FireOrange.copy(alpha = 0.9f), radius = meteorSize * 0.65f, center = Offset(cx, meteorY))
        drawCircle(FireWhiteHot.copy(alpha = 0.8f), radius = meteorSize * 0.3f, center = Offset(cx, meteorY))

        // Smoke trail particles
        for (i in 0 until 4) {
            val ty = meteorY - i * 15f
            if (ty > 0f) {
                drawCircle(
                    color = Color(0xFF553300).copy(alpha = 0.2f * (1f - i / 4f)),
                    radius = 5f + i * 2f,
                    center = Offset(cx + sin(i * 3f + progress * 10f) * 5f, ty),
                )
            }
        }
    } else {
        // Impact shockwave
        val impactProg = (progress - 0.3f) / 0.7f
        val shockR = maxR * impactProg
        val alpha = (1f - impactProg).coerceIn(0f, 0.8f)

        // Ground scorched area
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    FireDark.copy(alpha = alpha * 0.4f),
                    FireCore.copy(alpha = alpha * 0.2f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
            ),
            radius = shockR,
            center = Offset(cx, cy),
        )

        // Shockwave ring (double layer)
        drawCircle(
            color = FireCore.copy(alpha = alpha * 0.8f),
            radius = shockR,
            center = Offset(cx, cy),
            style = Stroke(width = 4f + (1f - impactProg) * 3f),
        )
        drawCircle(
            color = FireYellow.copy(alpha = alpha * 0.5f),
            radius = shockR * 0.85f,
            center = Offset(cx, cy),
            style = Stroke(width = 2f),
        )

        // Impact debris particles
        for (i in 0 until 6) {
            val angle = (i / 10f) * 6.283f
            val debrisR = shockR * (0.3f + sin(i * 4.5f) * 0.3f)
            val debrisAlpha = alpha * (0.5f + sin(i * 2.2f) * 0.2f)
            drawCircle(
                color = FireEmber.copy(alpha = debrisAlpha),
                radius = 3f * (1f - impactProg),
                center = Offset(cx + cos(angle) * debrisR, cy + sin(angle) * debrisR),
            )
        }

        // Screen flash on impact
        if (impactProg < 0.12f) {
            val flashAlpha = (0.12f - impactProg) / 0.12f * 0.35f
            drawRect(FireOrange.copy(alpha = flashAlpha), size = size)
        }
    }
}

/** Volcanic eruption — multiple rising lava columns with ground cracks */
private fun DrawScope.renderVolcanicEruption(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val alpha = if (progress < 0.8f) 0.7f else (1f - progress) / 0.2f * 0.7f

    // Lava ground glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                FireDark.copy(alpha = alpha * 0.5f),
                FireCore.copy(alpha = alpha * 0.2f),
                Color.Transparent,
            ),
        ),
        radius = maxR,
        center = Offset(cx, cy),
    )

    // Ground crack lines radiating from center
    for (i in 0 until 6) {
        val angle = (i / 6f) * 6.283f + 0.3f
        val crackLen = maxR * (0.5f + sin(progress * 4f + i) * 0.2f)
        val crackAlpha = alpha * 0.6f
        drawLine(
            color = FireEmber.copy(alpha = crackAlpha),
            start = Offset(cx, cy),
            end = Offset(cx + cos(angle) * crackLen, cy + sin(angle) * crackLen),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )
    }

    // Rising lava columns at different phases
    for (i in 0 until 8) {
        val angle = (i / 8f) * 6.283f
        val baseX = cx + cos(angle) * maxR * 0.4f
        val baseY = cy + sin(angle) * maxR * 0.4f
        val columnPhase = (progress * 2f + i * 0.35f) % 1f
        val riseHeight = columnPhase * maxR * 0.6f
        val columnAlpha = (1f - columnPhase) * alpha

        // Lava blob rising
        drawCircle(
            color = FireDark.copy(alpha = columnAlpha * 0.7f),
            radius = 5f * (1f - columnPhase * 0.5f),
            center = Offset(baseX, baseY - riseHeight),
        )
        drawCircle(
            color = FireYellow.copy(alpha = columnAlpha * 0.5f),
            radius = 3f * (1f - columnPhase * 0.5f),
            center = Offset(baseX, baseY - riseHeight),
        )

        // Dripping trail
        if (columnPhase > 0.2f) {
            val dripY = baseY - riseHeight + riseHeight * 0.3f
            drawCircle(
                color = FireCore.copy(alpha = columnAlpha * 0.3f),
                radius = 2f,
                center = Offset(baseX, dripY),
            )
        }
    }
}


// ═══════════════════════════════════════════════════════
// FROST SKILLS
// ═══════════════════════════════════════════════════════

/** Frost nova — expanding crystalline ice ring with ice shards */
private fun DrawScope.renderFrostNova(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val alpha = (1f - progress).coerceIn(0f, 0.8f)
    val radius = maxR * progress.coerceAtLeast(0.05f)

    // Frost ground fill
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                FrostLight.copy(alpha = alpha * 0.3f),
                FrostBase.copy(alpha = alpha * 0.15f),
                Color.Transparent,
            ),
        ),
        radius = radius,
        center = Offset(cx, cy),
    )

    // Ice ring (double layer for crystal effect)
    drawCircle(
        color = FrostCrystal.copy(alpha = alpha * 0.7f),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 4f * (1f - progress * 0.5f)),
    )
    drawCircle(
        color = FrostBase.copy(alpha = alpha * 0.4f),
        radius = radius * 0.9f,
        center = Offset(cx, cy),
        style = Stroke(width = 2f),
    )

    // Ice shard particles radiating outward
    for (i in 0 until 8) {
        val angle = (i / 8f) * 6.283f + progress * 2f
        val r = radius * 0.85f
        val shardLen = 6f * (1f - progress * 0.5f)
        val shardAngle = angle + 0.3f
        val sx = cx + cos(angle) * r
        val sy = cy + sin(angle) * r
        drawLine(
            color = FrostCrystal.copy(alpha = alpha * 0.8f),
            start = Offset(sx, sy),
            end = Offset(sx + cos(shardAngle) * shardLen, sy + sin(shardAngle) * shardLen),
            strokeWidth = 2f, cap = StrokeCap.Round,
        )
    }

    // Center flash
    if (progress < 0.2f) {
        drawCircle(
            color = White.copy(alpha = (0.2f - progress) / 0.2f * 0.5f),
            radius = maxR * 0.2f,
            center = Offset(cx, cy),
        )
    }
}

/** Absolute zero — instant freeze: screen-wide ice flash + crystal formation */
private fun DrawScope.renderAbsoluteZero(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val fadeAlpha = if (progress < 0.15f) progress / 0.15f
                    else if (progress > 0.7f) (1f - progress) / 0.3f
                    else 1f

    // Full-area ice tint
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                FrostDeep.copy(alpha = fadeAlpha * 0.25f),
                FrostBase.copy(alpha = fadeAlpha * 0.15f),
                Color.Transparent,
            ),
        ),
        radius = maxR * 1.3f,
        center = Offset(cx, cy),
    )

    // Expanding freeze ring (fast initial burst, then holds)
    val freezeR = maxR * (if (progress < 0.2f) progress / 0.2f else 1f)
    drawCircle(
        color = FrostCrystal.copy(alpha = fadeAlpha * 0.6f),
        radius = freezeR,
        center = Offset(cx, cy),
        style = Stroke(width = 5f * fadeAlpha),
    )

    // Ice crystals forming in ring pattern
    val crystalCount = 12
    for (i in 0 until crystalCount) {
        val angle = (i / crystalCount.toFloat()) * 6.283f
        val crystalPhase = ((progress * 2f - i * 0.05f).coerceIn(0f, 1f))
        if (crystalPhase <= 0f) continue
        val crystalR = freezeR * 0.7f
        val crystalX = cx + cos(angle) * crystalR
        val crystalY = cy + sin(angle) * crystalR
        val crystalSize = 4f * crystalPhase * fadeAlpha

        // Diamond-shaped crystal
        drawCircle(
            color = FrostLight.copy(alpha = fadeAlpha * 0.7f),
            radius = crystalSize,
            center = Offset(crystalX, crystalY),
        )
        drawCircle(
            color = White.copy(alpha = fadeAlpha * 0.4f),
            radius = crystalSize * 0.5f,
            center = Offset(crystalX, crystalY),
        )
    }

    // Flash on activation
    if (progress < 0.1f) {
        drawRect(FrostLight.copy(alpha = (0.1f - progress) / 0.1f * 0.3f), size = size)
    }
}

/** Ice Age Blizzard — horizontal blowing snow/ice particles */
private fun DrawScope.renderIceAgeBlizzard(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val fadeAlpha = if (progress < 0.1f) progress / 0.1f
                    else if (progress > 0.8f) (1f - progress) / 0.2f
                    else 1f

    // Blizzard base area
    drawCircle(
        color = FrostBase.copy(alpha = fadeAlpha * 0.15f),
        radius = maxR,
        center = Offset(cx, cy),
    )

    // Blowing snow particles (moving right to left)
    for (i in 0 until 12) {
        val phase = (progress * 4f + i * 0.15f) % 1f
        val startX = cx + maxR * 0.8f
        val endX = cx - maxR * 0.8f
        val snowX = startX + (endX - startX) * phase
        val snowY = cy + sin(i * 2.3f + progress * 5f) * maxR * 0.6f
        val drift = sin(phase * PI.toFloat() * 3f + i) * 8f
        val snowAlpha = fadeAlpha * sin(phase * PI.toFloat()) * 0.6f

        drawCircle(
            color = FrostCrystal.copy(alpha = snowAlpha),
            radius = 2f + sin(i * 1.7f) * 1.5f,
            center = Offset(snowX, snowY + drift),
        )
    }

    // Ice streak lines (wind streaks)
    for (i in 0 until 6) {
        val streakY = cy + (i - 3) * maxR * 0.25f
        val streakPhase = (progress * 3f + i * 0.2f) % 1f
        val streakX = cx + maxR * (0.5f - streakPhase)
        val streakLen = maxR * 0.3f * sin(streakPhase * PI.toFloat())
        val streakAlpha = fadeAlpha * 0.3f * sin(streakPhase * PI.toFloat())

        drawLine(
            color = FrostLight.copy(alpha = streakAlpha),
            start = Offset(streakX, streakY),
            end = Offset(streakX - streakLen, streakY + 2f),
            strokeWidth = 1.5f, cap = StrokeCap.Round,
        )
    }
}


// ═══════════════════════════════════════════════════════
// POISON SKILLS
// ═══════════════════════════════════════════════════════

/** Poison cloud — bubbling toxic gas with rising vapor */
private fun DrawScope.renderPoisonCloud(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val radius = event.radius * size.minDimension
    val alpha = if (progress < 0.1f) progress / 0.1f * 0.6f
               else if (progress > 0.8f) (1f - progress) / 0.2f * 0.6f
               else 0.6f

    // Cloud base (layered for depth)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                PoisonDark.copy(alpha = alpha * 0.4f),
                PoisonBase.copy(alpha = alpha * 0.25f),
                Color.Transparent,
            ),
        ),
        radius = radius * 1.1f,
        center = Offset(cx, cy),
    )

    // Bubbling sub-clouds
    for (i in 0 until 8) {
        val angle = (i / 8f) * 6.283f + progress * 1.5f
        val dist = radius * (0.3f + sin(progress * 5f + i * 1.7f) * 0.25f)
        val bubbleR = radius * (0.15f + sin(progress * 8f + i * 2.3f) * 0.08f)
        drawCircle(
            color = PoisonBase.copy(alpha = alpha * 0.35f),
            radius = bubbleR,
            center = Offset(cx + cos(angle) * dist, cy + sin(angle) * dist),
        )
    }

    // Rising toxic bubbles
    for (i in 0 until 6) {
        val bubblePhase = (progress * 2f + i * 0.3f) % 1f
        val bx = cx + sin(i * 2.1f + progress * 2f) * radius * 0.4f
        val by = cy - bubblePhase * radius * 0.6f
        val bAlpha = (1f - bubblePhase) * alpha * 0.5f
        val bSize = 3f + sin(i * 1.5f) * 1.5f

        drawCircle(PoisonNeon.copy(alpha = bAlpha), radius = bSize, center = Offset(bx, by))
        drawCircle(PoisonBase.copy(alpha = bAlpha * 0.5f), radius = bSize * 1.3f, center = Offset(bx, by))
    }
}

/** Acid spray — directional spray of acid droplets in a fan pattern */
private fun DrawScope.renderAcidSpray(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val alpha = (1f - progress * 0.5f).coerceIn(0f, 0.7f)

    // Spray base glow
    drawCircle(
        color = PoisonAcid.copy(alpha = alpha * 0.2f),
        radius = maxR * 0.3f,
        center = Offset(cx, cy),
    )

    // Acid droplets in expanding fan pattern
    for (i in 0 until 15) {
        val angle = (i / 15f) * 6.283f
        val dropPhase = (progress * 3f + i * 0.1f) % 1f
        val dist = maxR * dropPhase
        val dropX = cx + cos(angle) * dist
        val dropY = cy + sin(angle) * dist
        val dropAlpha = (1f - dropPhase) * alpha * 0.6f
        val dropSize = 3f + (1f - dropPhase) * 3f

        // Green acid drop
        drawCircle(PoisonNeon.copy(alpha = dropAlpha), radius = dropSize, center = Offset(dropX, dropY))

        // Acid drip trail
        if (dropPhase > 0.2f) {
            val dripLen = 6f * dropPhase
            drawLine(
                color = PoisonBase.copy(alpha = dropAlpha * 0.5f),
                start = Offset(dropX, dropY),
                end = Offset(dropX, dropY + dripLen),
                strokeWidth = 1.5f, cap = StrokeCap.Round,
            )
        }
    }

    // Sizzle ring
    val ringR = maxR * progress.coerceAtMost(0.8f) / 0.8f
    drawCircle(
        color = PoisonAcid.copy(alpha = alpha * 0.3f),
        radius = ringR * maxR,
        center = Offset(cx, cy),
        style = Stroke(width = 2f),
    )
}

/** Toxic domain — persistent poison zone with bubbling ground and skull motif */
private fun DrawScope.renderToxicDomain(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val fadeAlpha = if (progress < 0.15f) progress / 0.15f
                    else if (progress > 0.85f) (1f - progress) / 0.15f
                    else 1f

    // Toxic ground layer
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                PoisonMiasma.copy(alpha = fadeAlpha * 0.4f),
                PoisonDark.copy(alpha = fadeAlpha * 0.25f),
                PoisonBase.copy(alpha = fadeAlpha * 0.1f),
                Color.Transparent,
            ),
        ),
        radius = maxR,
        center = Offset(cx, cy),
    )

    // Pulsing toxic ring (domain boundary)
    val pulseR = maxR * (0.95f + sin(progress * 6f) * 0.05f)
    drawCircle(
        color = PoisonNeon.copy(alpha = fadeAlpha * 0.5f),
        radius = pulseR,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f),
    )

    // Bubbling surface
    for (i in 0 until 10) {
        val angle = (i / 10f) * 6.283f + sin(progress * 2f + i)
        val dist = maxR * (0.2f + (i % 4) * 0.15f)
        val bubblePhase = (progress * 3f + i * 0.2f) % 1f
        val bubbleSize = 4f * sin(bubblePhase * PI.toFloat())
        val bubbleAlpha = fadeAlpha * sin(bubblePhase * PI.toFloat()) * 0.4f

        drawCircle(
            color = PoisonBase.copy(alpha = bubbleAlpha),
            radius = bubbleSize,
            center = Offset(cx + cos(angle) * dist, cy + sin(angle) * dist),
        )
    }

    // Toxic mist rising
    for (i in 0 until 5) {
        val mistPhase = (progress * 1.5f + i * 0.3f) % 1f
        val mistX = cx + sin(i * 3.1f + progress) * maxR * 0.4f
        val mistY = cy - mistPhase * maxR * 0.5f
        val mistAlpha = sin(mistPhase * PI.toFloat()) * fadeAlpha * 0.2f
        drawCircle(
            color = PoisonBase.copy(alpha = mistAlpha),
            radius = 8f + mistPhase * 5f,
            center = Offset(mistX, mistY),
        )
    }
}


// ═══════════════════════════════════════════════════════
// LIGHTNING SKILLS
// ═══════════════════════════════════════════════════════

/** Lightning strike — zigzag bolt from sky with bright flash */
private fun DrawScope.renderLightningStrike(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val alpha = if (progress < 0.5f) 0.9f else (1f - progress) / 0.5f * 0.9f

    if (progress < 0.6f) {
        // Main bolt — zigzag with branching
        var boltX = cx
        var boltY = 0f
        val segCount = 10
        val segH = cy / segCount

        for (s in 0 until segCount) {
            val jitter = sin(s * 3.7f + progress * 8f) * 25f + cos(s * 5.1f) * 10f
            val nextX = cx + jitter
            val nextY = boltY + segH

            // Glow layer
            drawLine(
                color = LightBright.copy(alpha = alpha * 0.2f),
                start = Offset(boltX, boltY),
                end = Offset(nextX, nextY),
                strokeWidth = 16f, cap = StrokeCap.Round,
            )
            // Main bolt
            drawLine(
                color = LightBase.copy(alpha = alpha),
                start = Offset(boltX, boltY),
                end = Offset(nextX, nextY),
                strokeWidth = 5f, cap = StrokeCap.Round,
            )
            // Bright core
            drawLine(
                color = White.copy(alpha = alpha * 0.8f),
                start = Offset(boltX, boltY),
                end = Offset(nextX, nextY),
                strokeWidth = 2f, cap = StrokeCap.Round,
            )

            // Branch at some segments
            if (s % 3 == 1) {
                val branchAngle = if (jitter > 0) 0.5f else -0.5f
                val branchLen = segH * 0.6f
                drawLine(
                    color = LightBase.copy(alpha = alpha * 0.4f),
                    start = Offset(nextX, nextY),
                    end = Offset(nextX + branchAngle * branchLen, nextY + branchLen * 0.7f),
                    strokeWidth = 2f, cap = StrokeCap.Round,
                )
            }

            boltX = nextX
            boltY = nextY
        }
    }

    // Impact flash + expanding ring
    if (progress < 0.4f) {
        val flashProg = progress / 0.4f
        val flashR = size.minDimension * 0.12f * (1f - flashProg)
        drawCircle(
            color = White.copy(alpha = (1f - flashProg) * 0.7f),
            radius = flashR,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = LightBright.copy(alpha = (1f - flashProg) * 0.3f),
            radius = flashR * 2f,
            center = Offset(cx, cy),
        )
    }

    // Ground scorching ring
    if (progress > 0.2f) {
        val ringProg = (progress - 0.2f) / 0.8f
        val ringR = size.minDimension * 0.08f * ringProg
        drawCircle(
            color = LightBase.copy(alpha = (1f - ringProg) * 0.4f),
            radius = ringR,
            center = Offset(cx, cy),
            style = Stroke(width = 2f),
        )
    }
}

/** Static field — persistent electrical aura with arcing sparks */
private fun DrawScope.renderStaticField(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val fadeAlpha = if (progress < 0.1f) progress / 0.1f
                    else if (progress > 0.85f) (1f - progress) / 0.15f
                    else 1f

    // Electric field area
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                LightBright.copy(alpha = fadeAlpha * 0.1f),
                LightBase.copy(alpha = fadeAlpha * 0.05f),
                Color.Transparent,
            ),
        ),
        radius = maxR,
        center = Offset(cx, cy),
    )

    // Pulsing field boundary
    val pulseR = maxR * (0.95f + sin(progress * 8f) * 0.05f)
    drawCircle(
        color = LightBase.copy(alpha = fadeAlpha * 0.4f),
        radius = pulseR,
        center = Offset(cx, cy),
        style = Stroke(width = 2f),
    )

    // Random arcing sparks between points in the field
    for (i in 0 until 6) {
        val sparkPhase = (progress * 5f + i * 0.7f) % 1f
        if (sparkPhase > 0.3f) continue // Sparks are brief

        val a1 = (i * 1.3f + progress * 3f) % 6.283f
        val a2 = a1 + 0.8f + sin(i * 2.1f) * 0.5f
        val r1 = maxR * (0.3f + sin(i * 1.7f) * 0.2f)
        val r2 = maxR * (0.4f + cos(i * 2.3f) * 0.2f)
        val sparkAlpha = fadeAlpha * (0.3f - sparkPhase) / 0.3f * 0.7f

        val sx = cx + cos(a1) * r1
        val sy = cy + sin(a1) * r1
        val ex = cx + cos(a2) * r2
        val ey = cy + sin(a2) * r2

        // Mini zigzag
        val midX = (sx + ex) / 2f + sin(progress * 20f + i) * 10f
        val midY = (sy + ey) / 2f + cos(progress * 15f + i) * 8f
        drawLine(LightBase.copy(alpha = sparkAlpha), Offset(sx, sy), Offset(midX, midY), 2f, StrokeCap.Round)
        drawLine(LightBase.copy(alpha = sparkAlpha), Offset(midX, midY), Offset(ex, ey), 2f, StrokeCap.Round)

        // Spark endpoint glow
        drawCircle(LightBright.copy(alpha = sparkAlpha * 0.5f), 3f, Offset(ex, ey))
    }
}

/** Thunderstorm — multiple random lightning bolts striking from sky */
private fun DrawScope.renderThunderstorm(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val fadeAlpha = if (progress < 0.1f) progress / 0.1f
                    else if (progress > 0.8f) (1f - progress) / 0.2f
                    else 1f

    // Dark storm cloud overlay
    drawCircle(
        color = Color(0xFF1A1A2E).copy(alpha = fadeAlpha * 0.2f),
        radius = maxR * 1.2f,
        center = Offset(cx, cy - maxR * 0.3f),
    )

    // Multiple staggered lightning bolts
    val boltCount = 3
    for (b in 0 until boltCount) {
        val boltDelay = b * 0.12f
        val boltProgress = ((progress - boltDelay) * 5f).coerceIn(0f, 1f)
        if (boltProgress <= 0f || boltProgress >= 1f) continue

        val boltAlpha = fadeAlpha * sin(boltProgress * PI.toFloat()) * 0.8f
        val boltX = cx + (b - boltCount / 2) * maxR * 0.3f + sin(b * 3.1f) * maxR * 0.15f
        val boltY = cy + sin(b * 2.7f) * maxR * 0.2f

        // Bolt from top
        var bx = boltX
        var by = 0f
        val segs = 6
        val segH = boltY / segs
        for (s in 0 until segs) {
            val jitter = sin(s * 4.3f + b * 2.1f) * 15f
            val nx = boltX + jitter
            val ny = by + segH
            drawLine(LightBase.copy(alpha = boltAlpha), Offset(bx, by), Offset(nx, ny), 3f, StrokeCap.Round)
            drawLine(White.copy(alpha = boltAlpha * 0.6f), Offset(bx, by), Offset(nx, ny), 1.5f, StrokeCap.Round)
            bx = nx; by = ny
        }

        // Impact flash
        if (boltProgress < 0.5f) {
            drawCircle(
                color = White.copy(alpha = boltAlpha * 0.5f),
                radius = 8f * (1f - boltProgress * 2f),
                center = Offset(boltX, boltY),
            )
        }
    }

    // Ambient flicker
    val flickerAlpha = sin(progress * 30f).coerceAtLeast(0f) * fadeAlpha * 0.05f
    if (flickerAlpha > 0.01f) {
        drawRect(LightBase.copy(alpha = flickerAlpha), size = size)
    }
}


// ═══════════════════════════════════════════════════════
// SUPPORT SKILLS
// ═══════════════════════════════════════════════════════

/** Heal pulse — expanding green ring with rising sparkle particles */
private fun DrawScope.renderHealPulse(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val alpha = (1f - progress).coerceIn(0f, 0.7f)

    val radius = maxR * progress.coerceAtLeast(0.1f)

    // Expanding ring (layered for soft glow)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                HealGreen.copy(alpha = alpha * 0.2f),
                HealGreen.copy(alpha = alpha * 0.1f),
                Color.Transparent,
            ),
        ),
        radius = radius,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = HealBright.copy(alpha = alpha * 0.6f),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 3f * (1f - progress * 0.5f)),
    )
    drawCircle(
        color = White.copy(alpha = alpha * 0.3f),
        radius = radius * 0.95f,
        center = Offset(cx, cy),
        style = Stroke(width = 1f),
    )

    // Rising heal sparkles
    for (i in 0 until 8) {
        val angle = (i / 8f) * 6.283f
        val r = radius * 0.6f
        val rise = progress * 35f + sin(i * 1.5f) * 5f
        val sparkAlpha = alpha * (0.5f + sin(progress * 6f + i) * 0.2f)
        drawCircle(
            color = HealSparkle.copy(alpha = sparkAlpha),
            radius = 2.5f,
            center = Offset(cx + cos(angle) * r, cy + sin(angle) * r - rise),
        )
    }

    // Cross heal symbol at center
    if (progress < 0.5f) {
        val crossAlpha = (0.5f - progress) / 0.5f * alpha * 0.6f
        val crossLen = 8f
        drawLine(HealBright.copy(alpha = crossAlpha), Offset(cx - crossLen, cy), Offset(cx + crossLen, cy), 3f, StrokeCap.Round)
        drawLine(HealBright.copy(alpha = crossAlpha), Offset(cx, cy - crossLen), Offset(cx, cy + crossLen), 3f, StrokeCap.Round)
    }
}

/** War song aura — musical notes and rhythmic pulse rings */
private fun DrawScope.renderWarSongAura(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val fadeAlpha = if (progress < 0.1f) progress / 0.1f
                    else if (progress > 0.85f) (1f - progress) / 0.15f
                    else 1f

    // Aura field
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                SupportPurple.copy(alpha = fadeAlpha * 0.15f),
                SupportPurple.copy(alpha = fadeAlpha * 0.05f),
                Color.Transparent,
            ),
        ),
        radius = maxR,
        center = Offset(cx, cy),
    )

    // Rhythmic pulse rings (expanding outward in sequence)
    for (ring in 0 until 3) {
        val ringPhase = (progress * 3f + ring * 0.33f) % 1f
        val ringR = maxR * ringPhase
        val ringAlpha = (1f - ringPhase) * fadeAlpha * 0.4f
        drawCircle(
            color = SupportPurple.copy(alpha = ringAlpha),
            radius = ringR,
            center = Offset(cx, cy),
            style = Stroke(width = 2f * (1f - ringPhase)),
        )
    }

    // Floating musical note particles (represented as small dots with tails)
    for (i in 0 until 6) {
        val notePhase = (progress * 2f + i * 0.3f) % 1f
        val noteAngle = (i / 6f) * 6.283f + progress * 2f
        val noteR = maxR * (0.3f + notePhase * 0.4f)
        val noteX = cx + cos(noteAngle) * noteR
        val noteY = cy + sin(noteAngle) * noteR - notePhase * 15f
        val noteAlpha = (1f - notePhase) * fadeAlpha * 0.6f

        // Note head
        drawCircle(SupportPurple.copy(alpha = noteAlpha), 3f, Offset(noteX, noteY))
        // Note stem
        drawLine(
            color = SupportPurple.copy(alpha = noteAlpha * 0.7f),
            start = Offset(noteX + 2f, noteY),
            end = Offset(noteX + 2f, noteY - 8f),
            strokeWidth = 1.5f,
        )
    }
}

/** Divine shield — hexagonal shield barrier with energy surface */
private fun DrawScope.renderDivineShield(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val fadeAlpha = if (progress < 0.15f) progress / 0.15f
                    else if (progress > 0.8f) (1f - progress) / 0.2f
                    else 1f

    // Shield radius (quick expand then hold)
    val shieldR = maxR * (if (progress < 0.15f) progress / 0.15f else 1f)

    // Hexagonal shield
    for (h in 0 until 6) {
        val a1 = h * PI.toFloat() / 3f
        val a2 = (h + 1) * PI.toFloat() / 3f
        val p1 = Offset(cx + cos(a1) * shieldR, cy + sin(a1) * shieldR)
        val p2 = Offset(cx + cos(a2) * shieldR, cy + sin(a2) * shieldR)

        // Hex edge
        drawLine(
            color = SupportGold.copy(alpha = fadeAlpha * 0.7f),
            start = p1, end = p2,
            strokeWidth = 3f, cap = StrokeCap.Round,
        )

        // Inner hex
        val innerR = shieldR * 0.6f
        val ip1 = Offset(cx + cos(a1) * innerR, cy + sin(a1) * innerR)
        val ip2 = Offset(cx + cos(a2) * innerR, cy + sin(a2) * innerR)
        drawLine(
            color = SupportGold.copy(alpha = fadeAlpha * 0.3f),
            start = ip1, end = ip2,
            strokeWidth = 1.5f,
        )

        // Connecting spokes
        drawLine(
            color = SupportGold.copy(alpha = fadeAlpha * 0.2f),
            start = Offset(cx, cy), end = p1,
            strokeWidth = 1f,
        )
    }

    // Shield surface glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                SupportGold.copy(alpha = fadeAlpha * 0.1f),
                SupportWhiteGold.copy(alpha = fadeAlpha * 0.05f),
                Color.Transparent,
            ),
        ),
        radius = shieldR,
        center = Offset(cx, cy),
    )

    // Energy shimmer (rotating highlight)
    val shimmerAngle = progress * 4f
    val shimmerX = cx + cos(shimmerAngle) * shieldR * 0.5f
    val shimmerY = cy + sin(shimmerAngle) * shieldR * 0.5f
    drawCircle(
        color = White.copy(alpha = fadeAlpha * 0.2f),
        radius = shieldR * 0.15f,
        center = Offset(shimmerX, shimmerY),
    )
}


// ═══════════════════════════════════════════════════════
// WIND SKILLS
// ═══════════════════════════════════════════════════════

/** Cyclone pull — rotating spiral vortex with debris */
private fun DrawScope.renderCyclonePull(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val fadeAlpha = if (progress < 0.1f) progress / 0.1f * 0.6f
                    else if (progress > 0.8f) (1f - progress) / 0.2f * 0.6f
                    else 0.6f

    // Vortex center
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                WindDark.copy(alpha = fadeAlpha * 0.3f),
                WindTeal.copy(alpha = fadeAlpha * 0.15f),
                Color.Transparent,
            ),
        ),
        radius = maxR * 0.4f,
        center = Offset(cx, cy),
    )

    // Spiral wind lines (multiple interleaved spirals)
    for (spiral in 0 until 3) {
        val spiralOffset = spiral * 6.283f / 3f
        for (seg in 0 until 8) {
            val t1 = seg / 8f
            val t2 = (seg + 1) / 8f
            val angle1 = t1 * 6.283f * 2f + progress * 10f + spiralOffset
            val angle2 = t2 * 6.283f * 2f + progress * 10f + spiralOffset
            val r1 = maxR * (0.1f + t1 * 0.8f)
            val r2 = maxR * (0.1f + t2 * 0.8f)

            drawLine(
                color = WindTeal.copy(alpha = fadeAlpha * (0.5f + t1 * 0.3f)),
                start = Offset(cx + cos(angle1) * r1, cy + sin(angle1) * r1),
                end = Offset(cx + cos(angle2) * r2, cy + sin(angle2) * r2),
                strokeWidth = 2f, cap = StrokeCap.Round,
            )
        }
    }

    // Debris particles being pulled in
    for (i in 0 until 8) {
        val pullAngle = (i / 8f) * 6.283f + progress * 6f
        val pullR = maxR * (1f - (progress * 2f + i * 0.1f) % 1f)
        val debrisAlpha = fadeAlpha * (pullR / maxR) * 0.5f
        drawCircle(
            color = WindCyan.copy(alpha = debrisAlpha),
            radius = 2f,
            center = Offset(cx + cos(pullAngle) * pullR, cy + sin(pullAngle) * pullR),
        )
    }
}

/** Eye of storm — large persistent tornado with rotation layers */
private fun DrawScope.renderEyeOfStorm(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val fadeAlpha = if (progress < 0.15f) progress / 0.15f
                    else if (progress > 0.8f) (1f - progress) / 0.2f
                    else 1f

    // Storm area
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                WindDark.copy(alpha = fadeAlpha * 0.2f),
                WindTeal.copy(alpha = fadeAlpha * 0.1f),
                Color.Transparent,
            ),
        ),
        radius = maxR,
        center = Offset(cx, cy),
    )

    // Multiple rotation layers at different speeds
    for (layer in 0 until 4) {
        val layerR = maxR * (0.3f + layer * 0.2f)
        val layerSpeed = (4 - layer) * 2f
        val layerAlpha = fadeAlpha * (0.5f - layer * 0.1f)

        // Dashed ring effect
        for (dash in 0 until 4) {
            val dashAngle = (dash / 4f) * 6.283f + progress * layerSpeed
            val dashLen = 0.35f
            val a1 = dashAngle
            val a2 = dashAngle + dashLen

            drawArc(
                color = WindTeal.copy(alpha = layerAlpha),
                startAngle = (a1 * 180f / PI.toFloat()),
                sweepAngle = (dashLen * 180f / PI.toFloat()),
                useCenter = false,
                topLeft = Offset(cx - layerR, cy - layerR),
                size = Size(layerR * 2f, layerR * 2f),
                style = Stroke(width = 2f, cap = StrokeCap.Round),
            )
        }
    }

    // Eye center (calm, bright)
    drawCircle(
        color = WindLight.copy(alpha = fadeAlpha * 0.3f),
        radius = maxR * 0.15f,
        center = Offset(cx, cy),
    )

    // Wind streak particles
    for (i in 0 until 10) {
        val streakAngle = (i / 10f) * 6.283f + progress * 5f
        val streakR = maxR * (0.4f + sin(progress * 3f + i) * 0.15f)
        val sx = cx + cos(streakAngle) * streakR
        val sy = cy + sin(streakAngle) * streakR
        val streakLen = maxR * 0.1f
        drawLine(
            color = WindCyan.copy(alpha = fadeAlpha * 0.3f),
            start = Offset(sx, sy),
            end = Offset(sx + cos(streakAngle + 1.5f) * streakLen, sy + sin(streakAngle + 1.5f) * streakLen),
            strokeWidth = 1.5f, cap = StrokeCap.Round,
        )
    }
}

/** Vacuum slash — sharp crescent blade slashes */
private fun DrawScope.renderVacuumSlash(event: SkillEvent, progress: Float) {
    val cx = event.x * size.width
    val cy = event.y * size.height
    val maxR = event.radius * size.minDimension
    val alpha = (1f - progress).coerceIn(0f, 0.8f)

    // Multiple slash arcs at different angles
    val slashCount = 3
    for (s in 0 until slashCount) {
        val slashDelay = s * 0.15f
        val slashProg = ((progress - slashDelay) / 0.5f).coerceIn(0f, 1f)
        if (slashProg <= 0f) continue

        val slashAngle = s * PI.toFloat() / 3f - PI.toFloat() / 6f
        val slashAlpha = (1f - slashProg) * alpha

        // Slash arc
        val arcR = maxR * (0.5f + slashProg * 0.3f)
        val sweepDeg = 120f * slashProg

        drawArc(
            color = WindCyan.copy(alpha = slashAlpha * 0.7f),
            startAngle = (slashAngle * 180f / PI.toFloat()) - sweepDeg / 2f,
            sweepAngle = sweepDeg,
            useCenter = false,
            topLeft = Offset(cx - arcR, cy - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = Stroke(width = 4f * (1f - slashProg * 0.5f), cap = StrokeCap.Round),
        )

        // Slash trail (thinner, brighter)
        drawArc(
            color = White.copy(alpha = slashAlpha * 0.4f),
            startAngle = (slashAngle * 180f / PI.toFloat()) - sweepDeg / 2f,
            sweepAngle = sweepDeg,
            useCenter = false,
            topLeft = Offset(cx - arcR, cy - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = Stroke(width = 1.5f, cap = StrokeCap.Round),
        )
    }

    // Wind scatter particles
    for (i in 0 until 6) {
        val pAngle = (i / 6f) * 6.283f + progress * 4f
        val pDist = maxR * progress * 0.6f
        drawCircle(
            color = WindTeal.copy(alpha = alpha * 0.3f),
            radius = 2f,
            center = Offset(cx + cos(pAngle) * pDist, cy + sin(pAngle) * pDist),
        )
    }
}


