package com.example.jaygame.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.bridge.SkillEvent
import com.example.jaygame.bridge.SkillVfxType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ──────────────────────────────────────────────────────────────────
// Constants
// ──────────────────────────────────────────────────────────────────
private const val MAX_PARTICLES = 800
private const val MAX_EMITTERS = 32

// ──────────────────────────────────────────────────────────────────
// Pre-allocated element colors (GC-free)
// ──────────────────────────────────────────────────────────────────

// Fire
private val FireStart = Color(0xFFFF6B00)
private val FireEnd = Color(0xFFFF2200)
private val EmberColor = Color(0xFFFFAA33)
private val EmberEnd = Color(0xFFFF4400)

// Ice
private val IceStart = Color(0xFF88DDFF)
private val IceEnd = Color(0xFFCCEEFF)
private val FrostStart = Color(0xFFAAEEFF)
private val FrostEnd = Color(0xFF4488CC)

// Lightning
private val LightningStart = Color(0xFFFFEE44)
private val LightningEnd = Color(0xFFFFFFCC)
private val SparkStart = Color(0xFFFFDD00)
private val SparkEnd = Color(0xFFFFFF88)

// Poison
private val PoisonStart = Color(0xFF44DD44)
private val PoisonEnd = Color(0xFF227722)
private val ToxicStart = Color(0xFF66FF66)
private val ToxicEnd = Color(0xFF115511)

// Holy / Support
private val HolyStart = Color(0xFFFFD700)
private val HolyEnd = Color(0xFFFFF8DC)
private val HealStart = Color(0xFF88FF88)
private val HealEnd = Color(0xFFDDFFDD)

// Wind
private val WindStart = Color(0xFF88FFDD)
private val WindEnd = Color(0xFF44AA88)
private val DustStart = Color(0xFFBBAA88)
private val DustEnd = Color(0xFF887755)

// Dark
private val DarkStart = Color(0xFF9933FF)
private val DarkEnd = Color(0xFF220044)
private val VoidStart = Color(0xFF6600CC)
private val VoidEnd = Color(0xFF110022)

// ── Geometric effect constants (pre-allocated, GC-free) ──
private val StrokeThin = Stroke(width = 1.5f)
private val StrokeMedium = Stroke(width = 2.5f)
private val StrokeThick = Stroke(width = 4f)

private val FireGlowColor = Color(0x44FF4400)
private val FireRingColor = Color(0xFFFF6B00)
private val IceCrystalColor = Color(0xFF88DDFF)
private val IceGroundColor = Color(0x334488CC)
private val PoisonDripColor = Color(0xFF44DD44)
private val LightningBoltColor = LightningStart // 0xFFFFEE44 — reuse existing
private val LightningSparkColor = SparkStart    // 0xFFFFDD00 — reuse existing
private val HealRingColor = Color(0xFF88FF88)
private val HolyCrossColor = Color(0xFFFFD700)
private val BuffAuraColor = Color(0x44FFD700)
private val ShieldRingColor = Color(0xFF88DDFF)
private val WindArcColor = Color(0xFF88FFDD)
private val WindSlashColor = Color(0xFFAAFFDD)

private val BoltXs = FloatArray(12)
private val BoltYs = FloatArray(12)

/** 시드 기반 결정론적 의사난수 (0~1). 같은 시드 → 같은 값, 프레임 안정. */
private fun seededRand(seed: Int): Float {
    val x = (seed * 1103515245 + 12345) and 0x7FFFFFFF
    return (x % 10000) / 10000f
}

// ── Blueprint VFX archetypes ──
private const val VFX_SLASH_ARC = 0
private const val VFX_EXPLOSION_RING = 1
private const val VFX_CRYSTAL_STAR = 2
private const val VFX_VORTEX_SPIRAL = 3
private const val VFX_BEAM_LINES = 4
private const val VFX_SHIELD_PULSE = 5
private const val VFX_TENDRIL_CRAWL = 6
private const val VFX_MANDALA = 7
private const val VFX_SCATTER_BURST = 8
private const val VFX_DRIP_RAIN = 9

private class BlueprintVfxRecipe(
    val archetype: Int,
    val color1: Color,
    val color2: Color,
    val lineWidth: Float = 3f,
    val count: Int = 3,
    val speed: Float = 1f,
    val scale: Float = 1f,
) {
    // Pre-allocated Stroke objects (GC-free in draw loop)
    val stroke = Stroke(width = lineWidth)
    val strokeHalf = Stroke(width = lineWidth * 0.5f)
    val strokeThird = Stroke(width = lineWidth * 0.3f)
}

private val SwordGold = HolyStart // 0xFFFFD700 — reuse
private val AbyssPurple = Color(0xFF440088)
private val ChaosRed = Color(0xFFCC0033)
private val LifeGreen = Color(0xFF44FF88)
private val ForestGold = Color(0xFFDDAA33)
private val TechBlue = Color(0xFF2288FF)
private val PlasmaBlue = Color(0xFF44DDFF)
private val NeonOrange = Color(0xFFFF8800)
private val CelestialGold = Color(0xFFFFDD44)
private val HellfireRed = Color(0xFFFF3300)
private val DeathGreen = Color(0xFF33CC33)
private val CorruptPurple = Color(0xFF9900CC)

private val BlueprintRecipes: Map<String, BlueprintVfxRecipe> = mapOf(
    // LEGEND - HUMAN
    "human_legend_01" to BlueprintVfxRecipe(VFX_SLASH_ARC, SwordGold, Color(0xFFF0F0FF), 4f, 3, 1.5f, 1.2f),
    "human_legend_02" to BlueprintVfxRecipe(VFX_MANDALA, Color(0xFF88AAFF), Color(0xFFDDEEFF), 2f, 8, 0.6f, 1.3f),
    "human_legend_03" to BlueprintVfxRecipe(VFX_SHIELD_PULSE, SwordGold, Color(0xFFFFEEAA), 3f, 3, 0.8f, 1.1f),
    // LEGEND - SPIRIT
    "spirit_legend_01" to BlueprintVfxRecipe(VFX_VORTEX_SPIRAL, PlasmaBlue, Color(0xFFAAEEFF), 3f, 4, 2.0f, 1.3f),
    "spirit_legend_02" to BlueprintVfxRecipe(VFX_TENDRIL_CRAWL, LifeGreen, ForestGold, 2.5f, 6, 0.7f, 1.2f),
    "spirit_legend_03" to BlueprintVfxRecipe(VFX_MANDALA, DarkStart, VoidStart, 2f, 6, 0.5f, 1.1f),
    // LEGEND - ANIMAL
    "animal_legend_01" to BlueprintVfxRecipe(VFX_SCATTER_BURST, Color(0xFFFF4400), Color(0xFFFFAA00), 4f, 12, 1.8f, 1.4f),
    "animal_legend_02" to BlueprintVfxRecipe(VFX_SHIELD_PULSE, Color(0xFF2266AA), Color(0xFF88BBDD), 3.5f, 4, 0.6f, 1.3f),
    "animal_legend_03" to BlueprintVfxRecipe(VFX_SLASH_ARC, Color(0xFFEEEEFF), Color(0xFF88DDFF), 4.5f, 3, 2.0f, 1.2f),
    // LEGEND - ROBOT
    "robot_legend_01" to BlueprintVfxRecipe(VFX_EXPLOSION_RING, NeonOrange, Color(0xFFFFEE88), 4f, 3, 1.2f, 1.3f),
    "robot_legend_02" to BlueprintVfxRecipe(VFX_BEAM_LINES, TechBlue, PlasmaBlue, 2f, 8, 1.0f, 1.1f),
    "robot_legend_03" to BlueprintVfxRecipe(VFX_EXPLOSION_RING, Color(0xFF888888), Color(0xFFFFAA44), 5f, 2, 0.8f, 1.5f),
    // LEGEND - DEMON
    "demon_legend_01" to BlueprintVfxRecipe(VFX_SCATTER_BURST, HellfireRed, Color(0xFFFF8800), 3.5f, 10, 1.5f, 1.2f),
    "demon_legend_02" to BlueprintVfxRecipe(VFX_DRIP_RAIN, DeathGreen, Color(0xFF116611), 2f, 8, 0.8f, 1.2f),
    "demon_legend_03" to BlueprintVfxRecipe(VFX_TENDRIL_CRAWL, CorruptPurple, Color(0xFF440066), 2.5f, 5, 0.6f, 1.1f),
    // MYTHIC - HUMAN
    "human_mythic_01" to BlueprintVfxRecipe(VFX_MANDALA, CelestialGold, Color(0xFFFFFFDD), 3f, 10, 0.5f, 1.5f),
    "human_mythic_02" to BlueprintVfxRecipe(VFX_BEAM_LINES, CelestialGold, Color(0xFFFFFFFF), 3.5f, 6, 1.5f, 1.3f),
    "human_mythic_03" to BlueprintVfxRecipe(VFX_MANDALA, Color(0xFFBB88FF), Color(0xFFEEDDFF), 2.5f, 12, 0.4f, 1.4f),
    // MYTHIC - SPIRIT
    "spirit_mythic_01" to BlueprintVfxRecipe(VFX_VORTEX_SPIRAL, Color(0xFFFF6644), PlasmaBlue, 3f, 6, 2.5f, 1.3f),
    "spirit_mythic_02" to BlueprintVfxRecipe(VFX_CRYSTAL_STAR, Color(0xFF88DDFF), Color(0xFFCCEEFF), 3f, 8, 0.4f, 1.4f),
    "spirit_mythic_03" to BlueprintVfxRecipe(VFX_MANDALA, Color(0xFFDD88FF), Color(0xFF8844CC), 1.5f, 6, 1.0f, 1.0f),
    // MYTHIC - ANIMAL
    "animal_mythic_01" to BlueprintVfxRecipe(VFX_MANDALA, CelestialGold, Color(0xFF88FF88), 2.5f, 8, 0.5f, 1.5f),
    "animal_mythic_02" to BlueprintVfxRecipe(VFX_BEAM_LINES, Color(0xFFFFAA44), Color(0xFFFFDD88), 3f, 5, 1.2f, 1.2f),
    "animal_mythic_03" to BlueprintVfxRecipe(VFX_SCATTER_BURST, Color(0xFFFF6600), Color(0xFFFFCC44), 3.5f, 8, 1.4f, 1.2f),
    // MYTHIC - ROBOT
    "robot_mythic_01" to BlueprintVfxRecipe(VFX_BEAM_LINES, TechBlue, Color(0xFFFFFFFF), 3f, 6, 2.0f, 1.3f),
    "robot_mythic_02" to BlueprintVfxRecipe(VFX_SHIELD_PULSE, PlasmaBlue, TechBlue, 3.5f, 3, 0.7f, 1.4f),
    "robot_mythic_03" to BlueprintVfxRecipe(VFX_VORTEX_SPIRAL, Color(0xFFFF4444), TechBlue, 3f, 4, 1.5f, 1.2f),
    // MYTHIC - DEMON
    "demon_mythic_01" to BlueprintVfxRecipe(VFX_TENDRIL_CRAWL, DarkStart, Color(0xFF110022), 4f, 6, 0.5f, 1.4f),
    "demon_mythic_02" to BlueprintVfxRecipe(VFX_BEAM_LINES, Color(0xFFFFDD88), DarkStart, 3f, 7, 1.3f, 1.3f),
    "demon_mythic_03" to BlueprintVfxRecipe(VFX_SCATTER_BURST, Color(0xFFCC0000), Color(0xFF440000), 4.5f, 10, 1.6f, 1.5f),
    // IMMORTAL
    "human_immortal_01" to BlueprintVfxRecipe(VFX_MANDALA, Color(0xFFFFDD00), Color(0xFFFFFFFF), 3.5f, 12, 0.4f, 1.6f),
    "spirit_immortal_01" to BlueprintVfxRecipe(VFX_TENDRIL_CRAWL, LifeGreen, CelestialGold, 3f, 8, 0.6f, 1.6f),
    "animal_immortal_01" to BlueprintVfxRecipe(VFX_CRYSTAL_STAR, CelestialGold, Color(0xFF88DDFF), 4f, 4, 0.5f, 1.6f),
    "robot_immortal_01" to BlueprintVfxRecipe(VFX_MANDALA, PlasmaBlue, TechBlue, 2.5f, 8, 0.8f, 1.5f),
    "demon_immortal_01" to BlueprintVfxRecipe(VFX_VORTEX_SPIRAL, ChaosRed, DarkStart, 4f, 6, 2.0f, 1.6f),
)

// ──────────────────────────────────────────────────────────────────
// Particle class (mutable, pooled)
// ──────────────────────────────────────────────────────────────────
internal class SkillParticle {
    var x = 0f; var y = 0f           // normalized 0-1
    var vx = 0f; var vy = 0f         // velocity (normalized/sec)
    var ax = 0f; var ay = 0f         // acceleration
    var life = 0f; var maxLife = 0f
    var size = 0f; var sizeEnd = 0f
    var colorR = 0f; var colorG = 0f; var colorB = 0f; var colorA = 1f
    var colorEndR = 0f; var colorEndG = 0f; var colorEndB = 0f; var colorEndA = 0f
    var gravity = 0f
    var alive = false
    var emitterId = -1

    fun setColor(c: Color) {
        colorR = c.red; colorG = c.green; colorB = c.blue; colorA = c.alpha
    }
    fun setColorEnd(c: Color) {
        colorEndR = c.red; colorEndG = c.green; colorEndB = c.blue; colorEndA = c.alpha
    }
}

// ──────────────────────────────────────────────────────────────────
// Emitter patterns
// ──────────────────────────────────────────────────────────────────
internal const val EMIT_BURST = 0
internal const val EMIT_CONTINUOUS = 1
internal const val EMIT_CONE = 2
internal const val EMIT_RING = 3
internal const val EMIT_SPIRAL = 4

internal class SkillEmitter {
    var active = false
    var id = 0
    var pattern = EMIT_BURST
    var x = 0f; var y = 0f
    var radius = 0f
    var life = 0f; var maxLife = 0f
    var emitRate = 0f            // particles/sec (continuous)
    var burstCount = 0
    var hasBurst = false
    var emitAccum = 0f
    var element = 0
    var vfxType: SkillVfxType = SkillVfxType.LINGERING_FLAME
    var abilityId: String = ""

    // particle template values
    var pLife = 0f
    var pSpeed = 0f
    var pSize = 0f; var pSizeEnd = 0f
    var pGravity = 0f
    var colorStart: Color = Color.White
    var colorEnd: Color = Color.Transparent
    // cone-specific
    var coneAngle = 0f; var coneSpread = 0f
    // spiral-specific
    var spiralPhase = 0f

    fun reset() {
        active = false; hasBurst = false; emitAccum = 0f; spiralPhase = 0f; abilityId = ""
    }
}

// ──────────────────────────────────────────────────────────────────
// Element type mapping from SkillVfxType
// ──────────────────────────────────────────────────────────────────
// Element constants
internal const val ELEM_FIRE = 0
internal const val ELEM_ICE = 1
internal const val ELEM_LIGHTNING = 2
internal const val ELEM_POISON = 3
internal const val ELEM_HOLY = 4
internal const val ELEM_WIND = 5

private fun elementOf(type: SkillVfxType): Int = when (type) {
    SkillVfxType.LINGERING_FLAME, SkillVfxType.FIRESTORM_METEOR, SkillVfxType.VOLCANIC_ERUPTION -> ELEM_FIRE
    SkillVfxType.FROST_NOVA, SkillVfxType.ABSOLUTE_ZERO, SkillVfxType.ICE_AGE_BLIZZARD -> ELEM_ICE
    SkillVfxType.LIGHTNING_STRIKE, SkillVfxType.STATIC_FIELD, SkillVfxType.THUNDERSTORM -> ELEM_LIGHTNING
    SkillVfxType.POISON_CLOUD, SkillVfxType.ACID_SPRAY, SkillVfxType.TOXIC_DOMAIN -> ELEM_POISON
    SkillVfxType.HEAL_PULSE, SkillVfxType.WAR_SONG_AURA, SkillVfxType.DIVINE_SHIELD -> ELEM_HOLY
    SkillVfxType.CYCLONE_PULL, SkillVfxType.EYE_OF_STORM, SkillVfxType.VACUUM_SLASH -> ELEM_WIND
}

// ──────────────────────────────────────────────────────────────────
// Configure emitter based on skill event + element
// ──────────────────────────────────────────────────────────────────
private fun configureEmitter(e: SkillEmitter, event: SkillEvent, emitterId: Int) {
    e.active = true
    e.id = emitterId
    e.x = event.x
    e.y = event.y
    e.radius = event.radius.coerceAtLeast(0.05f)
    e.life = event.duration.coerceAtLeast(0.016f)
    e.maxLife = event.duration.coerceAtLeast(0.016f)
    e.hasBurst = false
    e.emitAccum = 0f
    e.spiralPhase = 0f
    e.element = elementOf(event.type)
    e.vfxType = event.type
    e.abilityId = event.abilityId

    when (e.element) {
        ELEM_FIRE -> { // Fire — cone upward + continuous embers
            e.pattern = EMIT_CONTINUOUS
            e.emitRate = 80f
            e.burstCount = 30
            e.pLife = 0.6f
            e.pSpeed = 0.15f
            e.pSize = 4f; e.pSizeEnd = 1f
            e.pGravity = -0.08f // float up
            e.colorStart = FireStart; e.colorEnd = FireEnd
            e.coneAngle = -PI.toFloat() / 2f // upward
            e.coneSpread = PI.toFloat() / 3f // 60 degree cone
        }
        ELEM_ICE -> { // Ice — ring expanding
            e.pattern = EMIT_RING
            e.emitRate = 60f
            e.burstCount = 40
            e.pLife = 0.8f
            e.pSpeed = 0.12f
            e.pSize = 3.5f; e.pSizeEnd = 1.5f
            e.pGravity = 0.01f
            e.colorStart = IceStart; e.colorEnd = IceEnd
        }
        ELEM_LIGHTNING -> { // Lightning — burst with short life
            e.pattern = EMIT_BURST
            e.emitRate = 0f
            e.burstCount = 25
            e.pLife = 0.2f
            e.pSpeed = 0.25f
            e.pSize = 3f; e.pSizeEnd = 1f
            e.pGravity = 0f
            e.colorStart = LightningStart; e.colorEnd = LightningEnd
        }
        ELEM_POISON -> { // Poison — continuous bubbling
            e.pattern = EMIT_CONTINUOUS
            e.emitRate = 50f
            e.burstCount = 20
            e.pLife = 0.9f
            e.pSpeed = 0.06f
            e.pSize = 3.5f; e.pSizeEnd = 2f
            e.pGravity = -0.04f // float up slowly
            e.colorStart = PoisonStart; e.colorEnd = PoisonEnd
        }
        ELEM_HOLY -> { // Holy — spiral ascending
            e.pattern = EMIT_SPIRAL
            e.emitRate = 70f
            e.burstCount = 25
            e.pLife = 0.7f
            e.pSpeed = 0.1f
            e.pSize = 3f; e.pSizeEnd = 1f
            e.pGravity = -0.1f // ascend
            e.colorStart = HolyStart; e.colorEnd = HolyEnd
        }
        ELEM_WIND -> { // Wind — spiral swirling
            e.pattern = EMIT_SPIRAL
            e.emitRate = 65f
            e.burstCount = 30
            e.pLife = 0.7f
            e.pSpeed = 0.14f
            e.pSize = 3f; e.pSizeEnd = 1.5f
            e.pGravity = -0.02f
            e.colorStart = WindStart; e.colorEnd = WindEnd
        }
        else -> { // Wind fallback (dark 등 미매핑 원소)
            e.pattern = EMIT_RING
            e.emitRate = 55f
            e.burstCount = 35
            e.pLife = 0.8f
            e.pSpeed = -0.1f // negative = inward
            e.pSize = 3.5f; e.pSizeEnd = 0.5f
            e.pGravity = 0f
            e.colorStart = DarkStart; e.colorEnd = DarkEnd
        }
    }

    // Override particle colors for blueprint recipes
    val recipe = if (event.abilityId.isNotEmpty()) BlueprintRecipes[event.abilityId] else null
    if (recipe != null) {
        e.colorStart = recipe.color1
        e.colorEnd = recipe.color2
    }
}

// ──────────────────────────────────────────────────────────────────
// Particle spawning (per pattern)
// ──────────────────────────────────────────────────────────────────
// Pool scan hint: start searching from this index to avoid O(N^2) during bursts
private var nextFreeHint = 0

private fun spawnParticle(
    pool: Array<SkillParticle>,
    e: SkillEmitter,
    phase: Float,
): Boolean {
    // Find free particle starting from hint (O(1) amortized during bursts)
    var p: SkillParticle? = null
    val size = pool.size
    for (offset in 0 until size) {
        val idx = (nextFreeHint + offset) % size
        if (!pool[idx].alive) {
            p = pool[idx]
            nextFreeHint = (idx + 1) % size
            break
        }
    }
    if (p == null) return false

    p.alive = true
    p.emitterId = e.id
    p.life = e.pLife * (0.8f + Random.nextFloat() * 0.4f) // ±20% variance
    p.maxLife = p.life
    p.size = e.pSize * (0.8f + Random.nextFloat() * 0.4f)
    p.sizeEnd = e.pSizeEnd
    p.gravity = e.pGravity
    p.ax = 0f; p.ay = 0f
    p.setColor(e.colorStart)
    p.setColorEnd(e.colorEnd)

    when (e.pattern) {
        EMIT_CONE -> {
            val angle = e.coneAngle + (Random.nextFloat() - 0.5f) * e.coneSpread
            val speed = e.pSpeed * (0.7f + Random.nextFloat() * 0.6f)
            p.x = e.x + (Random.nextFloat() - 0.5f) * e.radius * 0.3f
            p.y = e.y + (Random.nextFloat() - 0.5f) * e.radius * 0.3f
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed
        }
        EMIT_RING -> {
            val angle = phase + Random.nextFloat() * 0.5f
            val dist = e.radius * (0.8f + Random.nextFloat() * 0.4f)
            p.x = e.x + cos(angle) * dist
            p.y = e.y + sin(angle) * dist
            val speed = e.pSpeed * (0.7f + Random.nextFloat() * 0.6f)
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed
        }
        EMIT_SPIRAL -> {
            val angle = phase
            val dist = e.radius * 0.3f * (0.5f + Random.nextFloat())
            p.x = e.x + cos(angle) * dist
            p.y = e.y + sin(angle) * dist
            val speed = e.pSpeed * (0.6f + Random.nextFloat() * 0.8f)
            // tangential + outward velocity
            p.vx = cos(angle + 1.2f) * speed
            p.vy = sin(angle + 1.2f) * speed
        }
        else -> { // BURST
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = e.pSpeed * (0.4f + Random.nextFloat() * 0.6f)
            p.x = e.x + (Random.nextFloat() - 0.5f) * e.radius * 0.5f
            p.y = e.y + (Random.nextFloat() - 0.5f) * e.radius * 0.5f
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed
        }
    }
    return true
}

// ──────────────────────────────────────────────────────────────────
// Update all emitters + particles
// ──────────────────────────────────────────────────────────────────
internal fun updateSkillParticles(
    pool: Array<SkillParticle>,
    emitters: Array<SkillEmitter>,
    realDt: Float,
    gameDt: Float,
    battleSpeed: Float,
): Int {
    // 배속별 방출 계수: 1x=100%, 2x=75%, 4x=50%, 8x=30%
    val emitMult = when {
        battleSpeed >= 8f -> 0.3f
        battleSpeed >= 4f -> 0.5f
        battleSpeed >= 2f -> 0.75f
        else -> 1f
    }

    // Update emitters — life는 실제 시간 (배속 무관, 최소 1.5초 보장)
    for (e in emitters) {
        if (!e.active) continue
        e.life -= realDt
        if (e.life <= 0f) { e.reset(); continue }

        when (e.pattern) {
            EMIT_BURST -> {
                if (!e.hasBurst) {
                    e.hasBurst = true
                    val count = (e.burstCount * emitMult).toInt().coerceAtLeast(5)
                    for (j in 0 until count) {
                        val angle = (j.toFloat() / count) * 2f * PI.toFloat()
                        if (!spawnParticle(pool, e, angle)) break
                    }
                }
            }
            EMIT_CONTINUOUS, EMIT_CONE -> {
                e.emitAccum += e.emitRate * emitMult * realDt
                while (e.emitAccum >= 1f) {
                    e.emitAccum -= 1f
                    val phase = Random.nextFloat() * 2f * PI.toFloat()
                    if (!spawnParticle(pool, e, phase)) break
                }
            }
            EMIT_RING -> {
                e.emitAccum += e.emitRate * emitMult * realDt
                val progress = 1f - (e.life / e.maxLife)
                while (e.emitAccum >= 1f) {
                    e.emitAccum -= 1f
                    val angle = progress * 2f * PI.toFloat() + Random.nextFloat() * 1f
                    if (!spawnParticle(pool, e, angle)) break
                }
            }
            EMIT_SPIRAL -> {
                e.spiralPhase += realDt * 8f
                e.emitAccum += e.emitRate * emitMult * realDt
                while (e.emitAccum >= 1f) {
                    e.emitAccum -= 1f
                    if (!spawnParticle(pool, e, e.spiralPhase)) break
                }
            }
        }
    }

    // Update particles — 실제 시간 기반 (배속 무관)
    var activeCount = 0
    for (p in pool) {
        if (!p.alive) continue
        p.life -= realDt
        if (p.life <= 0f) { p.alive = false; continue }

        p.vx += p.ax * realDt
        p.vy += p.ay * realDt + p.gravity * realDt
        p.x += p.vx * realDt
        p.y += p.vy * realDt
        activeCount++
    }
    return activeCount
}

// ──────────────────────────────────────────────────────────────────
// Render all alive particles
// ──────────────────────────────────────────────────────────────────
// ──────────────────────────────────────────────────────────────────
// Per-emitter geometric effects (6 element functions)
// ──────────────────────────────────────────────────────────────────

private fun DrawScope.drawBlueprintGeometric(
    e: SkillEmitter, w: Float, h: Float, progress: Float, recipe: BlueprintVfxRecipe
) {
    val cx = e.x * w; val cy = e.y * h
    val r = e.radius * w.coerceAtMost(h) * recipe.scale
    val a = (1f - progress).coerceIn(0f, 1f)
    val t = progress * recipe.speed
    val lw = recipe.lineWidth
    val n = recipe.count
    val c1 = recipe.color1; val c2 = recipe.color2

    when (recipe.archetype) {
        VFX_SLASH_ARC -> {
            for (i in 0 until n) {
                val baseAngle = (i.toFloat() / n) * 360f
                val sweep = 150f * (t * 3f).coerceAtMost(1f)
                val startAngle = baseAngle - sweep / 2f
                val arcR = r * (0.5f + t * 0.5f)
                drawArc(c1, startAngle, sweep, false, Offset(cx - arcR, cy - arcR), Size(arcR * 2, arcR * 2), a * 0.9f, recipe.stroke)
            }
        }
        VFX_EXPLOSION_RING -> {
            for (i in 0 until n) {
                val rp = ((progress - i * 0.15f) / 0.7f).coerceIn(0f, 1f)
                if (progress > i * 0.15f) {
                    val ringR = r * 0.2f + r * rp
                    drawCircle(c1, ringR, Offset(cx, cy), (1f - rp) * a * 0.85f, recipe.stroke)
                }
            }
            drawCircle(c2, r * 0.3f, Offset(cx, cy), alpha = a * 0.4f)
        }
        VFX_CRYSTAL_STAR -> {
            val spokeLen = r * (0.2f + t.coerceAtMost(1f) * 0.8f)
            for (i in 0 until n) {
                val angle = (i.toFloat() / n) * 2f * PI.toFloat() + t * 0.3f
                drawLine(c1, Offset(cx, cy), Offset(cx + cos(angle) * spokeLen, cy + sin(angle) * spokeLen), lw, alpha = a * 0.9f)
                // Small cross at tip
                val tipX = cx + cos(angle) * spokeLen; val tipY = cy + sin(angle) * spokeLen
                val crossR = r * 0.1f
                val perpAngle = angle + PI.toFloat() / 2f
                drawLine(c2, Offset(tipX - cos(perpAngle) * crossR, tipY - sin(perpAngle) * crossR),
                    Offset(tipX + cos(perpAngle) * crossR, tipY + sin(perpAngle) * crossR), lw * 0.5f, alpha = a * 0.6f)
            }
        }
        VFX_VORTEX_SPIRAL -> {
            for (i in 0 until n) {
                val arcR = r * (0.2f + i.toFloat() / n * 0.8f)
                val speed = (n - i) * 1.5f
                val startAngle = (t * 360f * speed + i * (360f / n)) % 360f
                drawArc(c1, startAngle, 80f, false, Offset(cx - arcR, cy - arcR), Size(arcR * 2, arcR * 2), a * 0.8f, recipe.stroke)
            }
        }
        VFX_BEAM_LINES -> {
            for (i in 0 until n) {
                val angle = (i.toFloat() / n) * 2f * PI.toFloat()
                val beamProgress = (t * 2f).coerceAtMost(1f)
                val startDist = r * 1.5f * (1f - beamProgress)
                val endDist = r * 0.1f
                val sx = cx + cos(angle) * startDist; val sy = cy + sin(angle) * startDist
                val ex = cx + cos(angle) * endDist; val ey = cy + sin(angle) * endDist
                drawLine(c1, Offset(sx, sy), Offset(ex, ey), lw, alpha = a * 0.85f)
            }
            // Center glow
            drawCircle(c2, r * 0.2f * (t * 2f).coerceAtMost(1f), Offset(cx, cy), alpha = a * 0.5f)
        }
        VFX_SHIELD_PULSE -> {
            for (i in 0 until n) {
                val pulse = 1f + sin(t * 8f + i * 2f) * 0.1f
                val ringR = r * (0.3f + i.toFloat() / n * 0.7f) * pulse
                drawCircle(c1, ringR, Offset(cx, cy), a * 0.7f, recipe.stroke)
            }
            drawCircle(c2, r * 0.25f, Offset(cx, cy), alpha = a * 0.3f)
        }
        VFX_TENDRIL_CRAWL -> {
            val growLen = r * (t * 2f).coerceAtMost(1f)
            for (i in 0 until n) {
                val baseAngle = (i.toFloat() / n) * 2f * PI.toFloat()
                // Two-segment tendril with slight curve
                val midAngle = baseAngle + sin(t * 3f + i.toFloat()) * 0.3f
                val midX = cx + cos(midAngle) * growLen * 0.5f
                val midY = cy + sin(midAngle) * growLen * 0.5f
                val tipAngle = baseAngle + sin(t * 2f + i * 0.7f) * 0.5f
                val tipX = cx + cos(tipAngle) * growLen
                val tipY = cy + sin(tipAngle) * growLen
                drawLine(c1, Offset(cx, cy), Offset(midX, midY), lw, alpha = a * 0.8f)
                drawLine(c1, Offset(midX, midY), Offset(tipX, tipY), lw * 0.7f, alpha = a * 0.6f)
                // Glow at tip
                drawCircle(c2, lw, Offset(tipX, tipY), alpha = a * 0.5f)
            }
        }
        VFX_MANDALA -> {
            // Outer ring
            drawCircle(c1, r * 0.9f, Offset(cx, cy), a * 0.6f, recipe.strokeHalf)
            // Inner ring
            drawCircle(c2, r * 0.5f, Offset(cx, cy), a * 0.4f, recipe.strokeHalf)
            // Radial spokes
            for (i in 0 until n) {
                val angle = (i.toFloat() / n) * 2f * PI.toFloat() + t * 0.2f
                drawLine(c1, Offset(cx + cos(angle) * r * 0.2f, cy + sin(angle) * r * 0.2f),
                    Offset(cx + cos(angle) * r * 0.85f, cy + sin(angle) * r * 0.85f), lw * 0.5f, alpha = a * 0.5f)
            }
            // Cross-connections at mid-radius
            for (i in 0 until n) {
                val a1 = (i.toFloat() / n) * 2f * PI.toFloat() + t * 0.2f
                val a2 = ((i + 1f) / n) * 2f * PI.toFloat() + t * 0.2f
                val mr = r * 0.6f
                drawLine(c2, Offset(cx + cos(a1) * mr, cy + sin(a1) * mr),
                    Offset(cx + cos(a2) * mr, cy + sin(a2) * mr), lw * 0.3f, alpha = a * 0.4f)
            }
        }
        VFX_SCATTER_BURST -> {
            val burstLen = r * (t * 3f).coerceAtMost(1f)
            for (i in 0 until n) {
                val angle = (i.toFloat() / n) * 2f * PI.toFloat()
                val len = burstLen * (0.7f + seededRand(e.id * 100 + i) * 0.6f)
                drawLine(c1, Offset(cx, cy), Offset(cx + cos(angle) * len, cy + sin(angle) * len), lw, alpha = a * 0.85f)
            }
            // Center flash
            if (progress < 0.3f) drawCircle(c2, r * 0.4f * (1f - progress / 0.3f), Offset(cx, cy), alpha = a * 0.6f)
        }
        VFX_DRIP_RAIN -> {
            for (i in 0 until n) {
                val dx = cx + ((i.toFloat() / n) - 0.5f) * r * 2f
                val dripPhase = (t * 3f + i * 0.2f) % 1f
                val dy = cy - r * 0.3f + dripPhase * r * 0.8f
                drawLine(c1, Offset(dx, dy), Offset(dx, dy + r * 0.15f), lw, alpha = a * (1f - dripPhase) * 0.8f)
            }
        }
        else -> {}
    }
}

private fun DrawScope.drawFireGeometric(e: SkillEmitter, w: Float, h: Float, progress: Float) {
    val cx = e.x * w; val cy = e.y * h
    val r = e.radius * w.coerceAtMost(h)
    when (e.vfxType) {
        SkillVfxType.LINGERING_FLAME -> {
            val pulse = 1f + sin(progress * 12f) * 0.15f
            drawCircle(FireGlowColor, r * 0.8f * pulse, Offset(cx, cy), alpha = (1f - progress) * 0.8f)
        }
        SkillVfxType.FIRESTORM_METEOR -> {
            if (progress < 0.3f) {
                val mp = progress / 0.3f
                drawLine(FireRingColor, Offset(cx, cy - r * 3f * (1f - mp)), Offset(cx, cy), 4f, alpha = mp)
            }
            if (progress > 0.2f) {
                val rp = ((progress - 0.2f) / 0.8f).coerceIn(0f, 1f)
                drawCircle(FireRingColor, r * 0.3f + r * 1.2f * rp, Offset(cx, cy), (1f - rp) * 0.9f, StrokeThick)
            }
            drawCircle(FireGlowColor, r * 0.6f, Offset(cx, cy), alpha = (if (progress < 0.3f) progress / 0.3f else (1f - progress) * 0.8f).coerceIn(0f, 1f))
        }
        SkillVfxType.VOLCANIC_ERUPTION -> {
            for (i in 0 until 3) {
                val rp = ((progress - i * 0.15f) / 0.6f).coerceIn(0f, 1f)
                if (progress > i * 0.15f) drawCircle(FireRingColor, r * 0.2f + r * 1.5f * rp, Offset(cx, cy), (1f - rp) * 0.85f, StrokeThick)
            }
            drawCircle(FireGlowColor, r * (1f + sin(progress * 8f) * 0.1f), Offset(cx, cy), alpha = (1f - progress) * 0.7f)
        }
        else -> {}
    }
}

private fun DrawScope.drawIceGeometric(e: SkillEmitter, w: Float, h: Float, progress: Float) {
    val cx = e.x * w; val cy = e.y * h
    val r = e.radius * w.coerceAtMost(h)
    val ga = (if (progress < 0.2f) progress / 0.2f else (1f - progress) * 0.6f).coerceIn(0f, 1f)
    drawCircle(IceGroundColor, r * (0.5f + progress * 0.5f), Offset(cx, cy), alpha = ga)
    when (e.vfxType) {
        SkillVfxType.FROST_NOVA -> {
            drawCircle(IceCrystalColor, r * 0.2f + r * progress, Offset(cx, cy), (1f - progress) * 0.9f, StrokeThick)
        }
        SkillVfxType.ABSOLUTE_ZERO -> {
            val spokeLen = r * (0.3f + progress * 0.7f)
            val a = (1f - progress) * 0.9f
            for (i in 0 until 6) {
                val angle = i * (PI.toFloat() / 3f)
                drawLine(IceCrystalColor, Offset(cx, cy), Offset(cx + cos(angle) * spokeLen, cy + sin(angle) * spokeLen), 3f, alpha = a)
            }
        }
        SkillVfxType.ICE_AGE_BLIZZARD -> {
            val a = (1f - progress) * 0.8f
            for (i in 0 until 6) {
                val angle = i * (PI.toFloat() / 3f) + progress * 0.5f
                drawLine(IceCrystalColor, Offset(cx, cy), Offset(cx + cos(angle) * r * 0.8f, cy + sin(angle) * r * 0.8f), 2.5f, alpha = a)
            }
            drawCircle(IceCrystalColor, r * (0.5f + progress * 0.5f), Offset(cx, cy), (1f - progress) * 0.7f, StrokeMedium)
        }
        else -> {}
    }
}

private fun DrawScope.drawLightningGeometric(e: SkillEmitter, w: Float, h: Float, progress: Float) {
    val cx = e.x * w; val cy = e.y * h
    val r = e.radius * w.coerceAtMost(h)
    when (e.vfxType) {
        SkillVfxType.LIGHTNING_STRIKE -> {
            if (progress < 0.4f) {
                val ba = (1f - progress / 0.4f) * 0.9f
                val seg = 8
                // 볼트 경로를 emitter ID + 양자화된 progress로 시딩 → 프레임 안정 (4프레임마다 갱신)
                val boltSeed = e.id * 1000 + ((progress * 15f).toInt())
                BoltXs[0] = cx + (seededRand(boltSeed) - 0.5f) * r * 0.3f; BoltYs[0] = 0f
                for (i in 1 until seg) { val t = i.toFloat() / seg; BoltXs[i] = cx + (seededRand(boltSeed + i * 7) - 0.5f) * r * 0.5f * (1f - t); BoltYs[i] = cy * t }
                BoltXs[seg] = cx; BoltYs[seg] = cy
                for (i in 0 until seg) drawLine(LightningBoltColor, Offset(BoltXs[i], BoltYs[i]), Offset(BoltXs[i + 1], BoltYs[i + 1]), 3f, alpha = ba)
            }
            if (progress in 0.1f..0.6f) {
                val sp = (progress - 0.1f) / 0.5f
                drawCircle(LightningSparkColor, r * 0.3f * sp, Offset(cx, cy), (1f - sp) * 0.8f, StrokeMedium)
            }
        }
        SkillVfxType.STATIC_FIELD -> {
            val ca = (1f - progress) * 0.9f
            val targets = ((1f - progress) * 8).toInt().coerceIn(1, 8)
            for (i in 0 until targets) {
                val angle = (i.toFloat() / 8f) * 2f * PI.toFloat() + e.id * 0.7f
                val dist = r * (0.5f + 0.5f * ((i * 37 + e.id * 13) % 10) / 10f)
                val tx = cx + cos(angle) * dist; val ty = cy + sin(angle) * dist
                val arcSeed = e.id * 100 + i * 17 + ((progress * 10f).toInt())
                val mx = (cx + tx) / 2f + (seededRand(arcSeed) - 0.5f) * r * 0.3f
                val my = (cy + ty) / 2f + (seededRand(arcSeed + 3) - 0.5f) * r * 0.3f
                drawLine(LightningBoltColor, Offset(cx, cy), Offset(mx, my), 1.5f, alpha = ca)
                drawLine(LightningBoltColor, Offset(mx, my), Offset(tx, ty), 1.5f, alpha = ca)
            }
        }
        SkillVfxType.THUNDERSTORM -> {
            val boltCount = if (progress < 0.7f) 3 else 1
            val ba = (1f - progress) * 0.7f
            val stormSeed = e.id * 500 + ((progress * 8f).toInt())
            for (b in 0 until boltCount) {
                val bx = cx + (seededRand(stormSeed + b * 31) - 0.5f) * r * 2f
                var px = bx + (seededRand(stormSeed + b * 31 + 1) - 0.5f) * r * 0.2f; var py = cy - r * 0.8f
                for (seg in 0 until 4) {
                    val t = (seg + 1f) / 4f
                    val nx = bx + (seededRand(stormSeed + b * 31 + seg * 7 + 2) - 0.5f) * r * 0.3f * (1f - t); val ny = cy - r * 0.8f * (1f - t)
                    drawLine(LightningBoltColor, Offset(px, py), Offset(nx, ny), 2f, alpha = ba)
                    px = nx; py = ny
                }
            }
            drawCircle(LightningSparkColor, r, Offset(cx, cy), (1f - progress) * 0.5f, StrokeMedium)
        }
        else -> {}
    }
}

private fun DrawScope.drawPoisonGeometric(e: SkillEmitter, w: Float, h: Float, progress: Float) {
    val cx = e.x * w; val cy = e.y * h
    val r = e.radius * w.coerceAtMost(h)
    val a = (1f - progress) * 0.85f
    when (e.vfxType) {
        SkillVfxType.POISON_CLOUD -> {
            for (i in 0 until 4) {
                val dx = cx + (i - 1.5f) * r * 0.3f
                val dp = (progress * 3f + i * 0.25f) % 1f
                drawLine(PoisonDripColor, Offset(dx, cy + dp * r * 0.6f), Offset(dx, cy + dp * r * 0.6f + r * 0.15f), 3f, alpha = a * (1f - dp))
            }
        }
        SkillVfxType.ACID_SPRAY -> {
            for (i in 0 until 6) {
                val angle = -PI.toFloat() / 4f + (i.toFloat() / 5f) * PI.toFloat() / 2f
                val len = r * (0.5f + progress * 0.5f)
                drawLine(PoisonDripColor, Offset(cx, cy), Offset(cx + cos(angle) * len, cy + sin(angle) * len), 2.5f, alpha = a)
            }
        }
        SkillVfxType.TOXIC_DOMAIN -> {
            drawCircle(PoisonDripColor, r, Offset(cx, cy), a * 0.6f, StrokeMedium)
            for (i in 0 until 6) {
                val angle = i * PI.toFloat() / 3f
                val px = cx + cos(angle) * r; val py = cy + sin(angle) * r
                val dp = (progress * 2f + i * 0.17f) % 1f
                drawLine(PoisonDripColor, Offset(px, py), Offset(px, py + r * 0.2f * dp), 1.5f, alpha = a * (1f - dp))
            }
        }
        else -> {}
    }
}

private fun DrawScope.drawHolyGeometric(e: SkillEmitter, w: Float, h: Float, progress: Float) {
    val cx = e.x * w; val cy = e.y * h
    val r = e.radius * w.coerceAtMost(h)
    when (e.vfxType) {
        SkillVfxType.HEAL_PULSE -> {
            drawCircle(HealRingColor, r * 0.2f + r * 0.8f * progress, Offset(cx, cy), (1f - progress) * 0.85f, StrokeThick)
            val cl = r * 0.3f * (1f - progress); val ca = (1f - progress) * 0.9f; val crossY = cy - progress * r * 0.4f
            drawLine(HolyCrossColor, Offset(cx - cl, crossY), Offset(cx + cl, crossY), 3f, alpha = ca)
            drawLine(HolyCrossColor, Offset(cx, crossY - cl), Offset(cx, crossY + cl), 3f, alpha = ca)
        }
        SkillVfxType.WAR_SONG_AURA -> {
            for (i in 0 until 2) {
                val rp = ((progress - i * 0.2f) / 0.8f).coerceIn(0f, 1f)
                drawCircle(BuffAuraColor, r * 0.3f + r * 0.7f * rp, Offset(cx, cy), (1f - rp) * 0.7f, StrokeMedium)
            }
            val cl = r * 0.25f; val ca = (1f - progress) * 0.85f
            drawLine(HolyCrossColor, Offset(cx - cl, cy), Offset(cx + cl, cy), 3f, alpha = ca)
            drawLine(HolyCrossColor, Offset(cx, cy - cl), Offset(cx, cy + cl), 3f, alpha = ca)
        }
        SkillVfxType.DIVINE_SHIELD -> {
            val pulse = 1f + sin(progress * 10f) * 0.08f
            val sr = r * 0.9f * pulse
            drawCircle(ShieldRingColor, sr, Offset(cx, cy), (1f - progress) * 0.9f, StrokeThick)
            drawCircle(ShieldRingColor, sr * 0.7f, Offset(cx, cy), alpha = (1f - progress) * 0.4f)
        }
        else -> {}
    }
}

private fun DrawScope.drawWindGeometric(e: SkillEmitter, w: Float, h: Float, progress: Float) {
    val cx = e.x * w; val cy = e.y * h
    val r = e.radius * w.coerceAtMost(h)
    when (e.vfxType) {
        SkillVfxType.CYCLONE_PULL -> {
            for (i in 0 until 3) {
                val arcR = r * (0.3f + i * 0.25f)
                val startAngle = (progress * 720f + i * 120f) % 360f
                drawArc(WindArcColor, startAngle, 90f, false, Offset(cx - arcR, cy - arcR), Size(arcR * 2f, arcR * 2f), (1f - progress) * 0.85f, StrokeMedium)
            }
        }
        SkillVfxType.EYE_OF_STORM -> {
            for (i in 0 until 4) {
                val arcR = r * (0.2f + i * 0.2f)
                val speed = (4 - i) * 1.5f
                val startAngle = (progress * 360f * speed + i * 90f) % 360f
                drawArc(WindArcColor, startAngle, 70f, false, Offset(cx - arcR, cy - arcR), Size(arcR * 2f, arcR * 2f), (1f - progress) * 0.7f, StrokeMedium)
            }
            drawCircle(WindArcColor, r, Offset(cx, cy), (1f - progress) * 0.4f, StrokeMedium)
        }
        SkillVfxType.VACUUM_SLASH -> {
            val sp = (progress * 3f).coerceAtMost(1f)
            val sweep = 180f * sp
            val slashR = r * 1.2f
            val sa = if (progress < 0.33f) 0.8f else (1f - progress) * 0.6f
            drawArc(WindSlashColor, -90f - sweep / 2f, sweep, false, Offset(cx - slashR, cy - slashR), Size(slashR * 2f, slashR * 2f), sa, StrokeThick)
            if (progress > 0.15f) {
                val sp2 = ((progress - 0.15f) * 3f).coerceAtMost(1f)
                val sweep2 = 160f * sp2; val r2 = slashR * 0.9f
                drawArc(WindSlashColor, 90f - sweep2 / 2f, sweep2, false, Offset(cx - r2, cy - r2), Size(r2 * 2f, r2 * 2f), sa * 0.7f, StrokeMedium)
            }
        }
        else -> {}
    }
}

// ──────────────────────────────────────────────────────────────────
// Render all alive particles + geometric effects
// ──────────────────────────────────────────────────────────────────
internal fun DrawScope.renderSkillParticles(
    pool: Array<SkillParticle>,
    emitters: Array<SkillEmitter>,
    w: Float, h: Float,
) {
    // Per-emitter geometric effects (skip at LOD 2)
    if (ParticleLOD.currentLevel < 2) {
        for (e in emitters) {
            if (!e.active) continue
            val progress = (1f - (e.life / e.maxLife)).coerceIn(0f, 1f)
            val recipe = if (e.abilityId.isNotEmpty()) BlueprintRecipes[e.abilityId] else null
            if (recipe != null) {
                drawBlueprintGeometric(e, w, h, progress, recipe)
            } else {
                when (e.element) {
                    ELEM_FIRE -> drawFireGeometric(e, w, h, progress)
                    ELEM_ICE -> drawIceGeometric(e, w, h, progress)
                    ELEM_LIGHTNING -> drawLightningGeometric(e, w, h, progress)
                    ELEM_POISON -> drawPoisonGeometric(e, w, h, progress)
                    ELEM_HOLY -> drawHolyGeometric(e, w, h, progress)
                    ELEM_WIND -> drawWindGeometric(e, w, h, progress)
                }
            }
        }
    }

    // Per-particle rendering
    var drawIdx = 0

    for (p in pool) {
        if (!p.alive) continue

        // LOD skip
        if (ParticleLOD.shouldSkipParticle(drawIdx)) {
            drawIdx++; continue
        }
        drawIdx++

        val lifeFrac = (p.life / p.maxLife).coerceIn(0f, 1f)
        val invFrac = 1f - lifeFrac

        // interpolate color (component-wise, no allocation)
        val r = p.colorEndR + (p.colorR - p.colorEndR) * lifeFrac
        val g = p.colorEndG + (p.colorG - p.colorEndG) * lifeFrac
        val b = p.colorEndB + (p.colorB - p.colorEndB) * lifeFrac
        val a = (p.colorA * lifeFrac).coerceIn(0f, 1f)

        // interpolate size
        val curSize = p.sizeEnd + (p.size - p.sizeEnd) * lifeFrac

        val sx = p.x * w
        val sy = p.y * h

        drawCircle(
            color = Color(red = r, green = g, blue = b, alpha = a),
            radius = curSize,
            center = Offset(sx, sy),
        )
    }

    // Lightning chains: draw lines between nearby particles of same emitter
    for (e in emitters) {
        if (!e.active || e.element != ELEM_LIGHTNING) continue
        drawLightningChains(pool, e.id, w, h)
    }
}

/** 번개 체인: 같은 이미터의 가까운 파티클 쌍을 선으로 연결 */
private fun DrawScope.drawLightningChains(
    pool: Array<SkillParticle>,
    emitterId: Int,
    w: Float, h: Float,
) {
    // Collect alive lightning particles (max 20 for perf)
    var count = 0
    val tempXs = LightningTempXs
    val tempYs = LightningTempYs
    val tempAlphas = LightningTempAlphas

    for (p in pool) {
        if (!p.alive || p.emitterId != emitterId) continue
        if (count >= 20) break
        tempXs[count] = p.x * w
        tempYs[count] = p.y * h
        tempAlphas[count] = (p.life / p.maxLife).coerceIn(0f, 1f)
        count++
    }

    // Draw lines between close pairs
    val maxDist = w * 0.15f
    val maxDistSq = maxDist * maxDist
    for (i in 0 until count) {
        for (j in i + 1 until count) {
            val dx = tempXs[i] - tempXs[j]
            val dy = tempYs[i] - tempYs[j]
            val distSq = dx * dx + dy * dy
            if (distSq < maxDistSq) {
                val alpha = (tempAlphas[i] * tempAlphas[j] * 0.6f).coerceIn(0f, 1f)
                drawLine(
                    color = LightningChainColor,
                    start = Offset(tempXs[i], tempYs[i]),
                    end = Offset(tempXs[j], tempYs[j]),
                    strokeWidth = 1.5f,
                    alpha = alpha,
                )
            }
        }
    }
}

// Pre-allocated scratch arrays for lightning chain rendering (avoid per-frame alloc)
private val LightningTempXs = FloatArray(20)
private val LightningTempYs = FloatArray(20)
private val LightningTempAlphas = FloatArray(20)
private val LightningChainColor = LightningStart // same as 0xFFFFEE44

// ──────────────────────────────────────────────────────────────────
// Composable overlay
// ──────────────────────────────────────────────────────────────────

@Composable
fun SkillParticleOverlay(
    fieldOffset: Offset = Offset.Zero,
    fieldSize: Size = Size.Zero,
) {
    val skillEvents by BattleBridge.skillEvents.collectAsState()
    val prevEventCount = remember { mutableIntStateOf(0) }
    val activeCount = remember { mutableIntStateOf(0) }

    // Pre-allocated pools
    val particlePool = remember { Array(MAX_PARTICLES) { SkillParticle() } }
    val emitterPool = remember { Array(MAX_EMITTERS) { SkillEmitter() } }
    val nextEmitterId = remember { mutableIntStateOf(0) }

    // Track processed events to avoid re-processing
    val lastProcessedTime = remember { mutableStateOf(0L) }

    // Detect new skill events → spawn emitters
    val events = skillEvents
    if (events.size != prevEventCount.intValue) {
        prevEventCount.intValue = events.size
        // Process only new events (those with startTime > lastProcessedTime)
        val cutoff = lastProcessedTime.value
        for (event in events) {
            if (event.startTime <= cutoff) continue
            // Find free emitter
            var emitter: SkillEmitter? = null
            for (em in emitterPool) {
                if (!em.active) { emitter = em; break }
            }
            if (emitter == null) continue
            val eid = nextEmitterId.intValue++
            configureEmitter(emitter, event, eid)
            lastProcessedTime.value = event.startTime
        }
    }

    // Update loop
    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { _ ->
                val speed = BattleBridge.battleSpeed.value
                if (speed <= 0f) return@withFrameNanos // paused

                val realDt = 1f / 60f // 실제 시간 (이미터 life용, 배속 무관)
                val gameDt = realDt * speed // 게임 시간 (파티클 물리용)
                val count = updateSkillParticles(particlePool, emitterPool, realDt, gameDt, speed)
                activeCount.intValue = count

                // Report to LOD (aggregate)
                ParticleLOD.addParticleCount(count)
            }
        }
    }

    // Skip rendering if no active particles
    if (activeCount.intValue == 0) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Translate to field coordinates (same as SkillEffectOverlay)
        val fw = if (fieldSize.width > 0f) fieldSize.width else size.width
        val fh = if (fieldSize.height > 0f) fieldSize.height else size.height
        val fx = fieldOffset.x
        val fy = fieldOffset.y

        // Draw with field translation
        drawContext.transform.translate(fx, fy)
        renderSkillParticles(particlePool, emitterPool, fw, fh)
        drawContext.transform.translate(-fx, -fy)
    }
}
