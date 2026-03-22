@file:Suppress("DEPRECATION")
package com.example.jaygame.engine

import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.bridge.SkillEvent
import com.example.jaygame.bridge.SkillVfxType
import com.example.jaygame.data.UNIT_DEFS_MAP

/**
 * Manages unique ability cooldowns, activation, and VFX emission
 * for hero-grade (2+) units.
 *
 * Called from BattleEngine.updateUnits() every frame.
 */
object UniqueAbilitySystem {

    private const val W = 720f
    private const val H = 720f

    /**
     * Initialize unique ability fields on a newly placed unit.
     * Should be called when a unit is placed/summoned/merged.
     *
     * Supports both legacy UnitDef and blueprint-based units:
     * - Legacy: reads cooldown from UnitDef.uniqueAbility
     * - Blueprint: infers cooldown from family+grade if grade >= 2 (Hero+)
     */
    fun initUnit(unit: GameUnit) {
        // Always reset ability fields first (GameUnit pool recycling safety)
        unit.uniqueAbilityType = -1
        unit.uniqueAbilityMaxCd = 0f
        unit.uniqueAbilityCooldown = 0f
        unit.passiveCounter = 0

        // Only Hero (grade 2) and above have unique abilities
        if (unit.grade < 2) return

        val vfxType = resolveVfxType(unit.family, unit.grade)
        if (vfxType < 0) return

        // Try legacy UnitDef for cooldown data first (canonical balance values)
        val legacyDef = UNIT_DEFS_MAP[unit.unitDefId]
        val legacyCooldown = legacyDef?.uniqueAbility?.cooldown?.toFloat()

        // Fallback: infer cooldown from family+grade if legacy data not available
        val cooldown = legacyCooldown ?: inferCooldownFromGrade(unit.family, unit.grade)

        unit.uniqueAbilityType = vfxType
        unit.uniqueAbilityMaxCd = cooldown
        unit.uniqueAbilityCooldown = cooldown * 0.5f // start half-ready
    }

    /**
     * Infer ability cooldown based on family and grade when JSON data is missing.
     * Hero (grade 2) = passive only (cooldown 0, triggers on attack).
     * Legend+ = active abilities with longer cooldowns for stronger (higher-grade) skills.
     * Values are modeled after legacy UnitDef cooldowns in UNIT_DEFS.
     */
    // Pre-allocated cooldown tables per family: [Legend, Ancient, Mythic, Immortal]
    // Values modeled after legacy UnitDef cooldowns in UNIT_DEFS.
    private val COOLDOWNS = arrayOf(
        floatArrayOf(12f, 20f, 25f, 45f),   // 0: Fire
        floatArrayOf(15f, 22f, 30f, 45f),   // 1: Frost
        floatArrayOf(10f, 15f, 20f, 35f),   // 2: Poison
        floatArrayOf(14f, 18f, 25f, 40f),   // 3: Lightning
        floatArrayOf(15f, 20f, 25f, 40f),   // 4: Support
        floatArrayOf(10f, 15f, 20f, 35f),   // 5: Wind
    )
    private val COOLDOWNS_DEFAULT = floatArrayOf(12f, 18f, 25f, 40f)

    private fun inferCooldownFromGrade(family: Int, grade: Int): Float {
        if (grade <= 2) return 0f // Hero: passive only
        val table = COOLDOWNS.getOrElse(family) { COOLDOWNS_DEFAULT }
        return table[(grade - 3).coerceIn(0, table.size - 1)]
    }

    // Reference to engine zone pool — set by BattleEngine
    @Volatile var zonePool: ObjectPool<ZoneEffect>? = null

    /** 유물 쿨다운 감소 (0.0~1.0) */
    @Volatile var cooldownReduction: Float = 0f

    /**
     * Update unique abilities for all active units.
     * Called once per frame from BattleEngine.updateUnits().
     */
    fun update(units: List<GameUnit>, dt: Float, enemies: List<Enemy>) {
        for (unit in units) {
            if (!unit.alive || unit.uniqueAbilityType < 0) continue
            if (unit.grade < 2) continue // only hero+ have unique abilities

            // Tick cooldown
            if (unit.uniqueAbilityCooldown > 0f) {
                unit.uniqueAbilityCooldown -= dt
            }

            // Passive triggers on attack
            if (unit.isAttacking && unit.currentTarget?.alive == true) {
                handlePassive(unit, dt, enemies)
            }

            // Active ability trigger
            if (unit.uniqueAbilityMaxCd > 0f && unit.uniqueAbilityCooldown <= 0f) {
                if (enemies.any { it.alive }) {
                    activateAbility(unit, enemies)
                    unit.uniqueAbilityCooldown = unit.uniqueAbilityMaxCd * (1f - cooldownReduction.coerceIn(0f, 0.8f))
                }
            }
        }
    }

    /**
     * Handle passive triggers (called when unit is attacking).
     */
    private fun handlePassive(unit: GameUnit, dt: Float, enemies: List<Enemy>) {
        val vfx = resolveVfxTypeEnum(unit.family, unit.grade) ?: return
        val nx = unit.position.x / W
        val ny = unit.position.y / H

        when (vfx) {
            // N1: Fire Hero — Lingering Flame: leave fire zone on attack position
            SkillVfxType.LINGERING_FLAME -> {
                unit.passiveCounter++
                if (unit.passiveCounter % 3 == 0) { // every 3rd attack
                    val target = unit.currentTarget ?: return
                    val tx = target.position.x / W
                    val ty = target.position.y / H
                    emitVfx(vfx, tx, ty, 0.05f, unit, 3f)
                    // Create actual damage zone
                    val zone = zonePool?.acquire() ?: return
                    zone.init(
                        pos = target.position.copy(),
                        radius = 60f, duration = 3f,
                        tickInterval = 1f,
                        tickDamage = unit.effectiveATK() * 0.15f,
                        family = 0, grade = unit.grade,
                    )
                }
            }

            // O1: Frost Hero — Frost Armor (passive aura for allies, VFX on enemy freeze)
            SkillVfxType.FROST_NOVA -> {
                // Passive: periodically emit frost aura VFX
                unit.passiveCounter++
                if (unit.passiveCounter % 20 == 0) {
                    emitVfx(vfx, nx, ny, 0.08f, unit, 0.6f)
                }
            }

            // P1: Poison Hero — Plague spread (passive: poison spreads on death)
            SkillVfxType.POISON_CLOUD -> {
                // Passive: VFX hint on target
                unit.passiveCounter++
                if (unit.passiveCounter % 10 == 0) {
                    val target = unit.currentTarget ?: return
                    val tx = target.position.x / W
                    val ty = target.position.y / H
                    emitVfx(vfx, tx, ty, 0.04f, unit, 1.5f)
                }
            }

            // Q1: Lightning Hero — Overcharge: every 5th attack bonus damage + chain
            SkillVfxType.LIGHTNING_STRIKE -> {
                unit.passiveCounter++
                if (unit.passiveCounter % 5 == 0) {
                    val target = unit.currentTarget ?: return
                    val tx = target.position.x / W
                    val ty = target.position.y / H
                    emitVfx(vfx, tx, ty, 0.08f, unit, 0.5f)
                    // Bonus damage on overcharged hit
                    target.takeDamage(unit.effectiveATK() * 1f) // 200% total (100% normal + 100% bonus)
                    BattleBridge.onDamageDealt(tx, ty, (unit.effectiveATK() * 1f).toInt(), true)
                }
            }

            // R1: Support Hero — Oracle: crit buff aura (passive, periodic VFX)
            SkillVfxType.HEAL_PULSE -> {
                unit.passiveCounter++
                if (unit.passiveCounter % 30 == 0) {
                    emitVfx(vfx, nx, ny, 0.1f, unit, 0.8f)
                }
            }

            // S1: Wind Hero — Cyclone: 25% chance small tornado
            SkillVfxType.CYCLONE_PULL -> {
                unit.passiveCounter++
                if (unit.passiveCounter % 4 == 0 && Math.random() < 0.25) {
                    val target = unit.currentTarget ?: return
                    val tx = target.position.x / W
                    val ty = target.position.y / H
                    emitVfx(vfx, tx, ty, 0.06f, unit, 2f)
                    // Create wind zone with slow
                    val zone = zonePool?.acquire() ?: return
                    zone.init(
                        pos = target.position.copy(),
                        radius = 50f, duration = 2f,
                        tickInterval = 0.5f,
                        tickDamage = unit.effectiveATK() * 0.3f,
                        slowPercent = 0.3f,
                        family = 5, grade = unit.grade,
                    )
                }
            }

            else -> { /* No passive for this type */ }
        }
    }

    /**
     * Activate an active ability (cooldown-based).
     */
    private fun activateAbility(unit: GameUnit, enemies: List<Enemy>) {
        val vfx = resolveVfxTypeEnum(unit.family, unit.grade) ?: return
        val nx = unit.position.x / W
        val ny = unit.position.y / H
        // Grade-based scaling: Hero=1.0, Legend=1.3, Ancient=1.7, Mythic=2.2, Immortal=3.0
        val gradeScale = DamageCalculator.gradeMultiplier(unit.grade)

        // Find best target position (densest enemies or strongest)
        val targetEnemy = enemies.filter { it.alive }.maxByOrNull { it.maxHp } ?: return
        val tx = targetEnemy.position.x / W
        val ty = targetEnemy.position.y / H

        when (vfx) {
            // ── Fire ──
            // N2: Inferno — Firestorm Meteor: AoE meteor strike
            SkillVfxType.FIRESTORM_METEOR -> {
                emitVfx(vfx, tx, ty, 0.12f, unit, 1.5f)
                val atk = unit.effectiveATK() * gradeScale
                for (e in enemies) {
                    if (!e.alive) continue
                    val dx = e.position.x - targetEnemy.position.x
                    val dy = e.position.y - targetEnemy.position.y
                    if (dx * dx + dy * dy <= 120f * 120f) {
                        e.takeDamage(atk * 3f)
                        e.buffs.addBuff(BuffType.DoT, atk * 0.2f, 4f) // burn debuff
                    }
                }
            }
            // N3: Volcano King — Volcanic Eruption: summon volcano zone
            SkillVfxType.VOLCANIC_ERUPTION -> {
                emitVfx(vfx, tx, ty, 0.15f, unit, 8f)
                val zone = zonePool?.acquire()
                if (zone != null) {
                    zone.init(
                        pos = targetEnemy.position.copy(),
                        radius = 100f, duration = 8f,
                        tickInterval = 1.5f,
                        tickDamage = unit.effectiveATK() * 2f * gradeScale,
                        family = 0, grade = unit.grade,
                    )
                }
            }
            // N4: Phoenix — Carpet Bomb
            SkillVfxType.PHOENIX_CARPET_BOMB -> {
                emitVfx(vfx, 0.5f, 0.5f, 0.3f, unit, 6f)
                val atk = unit.effectiveATK()
                for (e in enemies) {
                    if (!e.alive) continue
                    e.takeDamage(atk * 1.5f)
                }
            }
            // N5-N6: Ra — Supernova
            SkillVfxType.SUPERNOVA -> {
                emitVfx(vfx, nx, ny, 0.5f, unit, 3f)
                val atk = unit.effectiveATK()
                for (e in enemies) {
                    if (!e.alive) continue
                    e.takeDamage(atk * 10f)
                    if (e.alive && e.hp < e.maxHp * 0.3f) e.hp = 0f // execute
                }
            }

            // ── Frost ──
            // O1: Frost Nova
            SkillVfxType.FROST_NOVA -> {
                emitVfx(vfx, nx, ny, 0.1f, unit, 1f)
                for (e in enemies) {
                    if (!e.alive) continue
                    val dx = e.position.x - unit.position.x
                    val dy = e.position.y - unit.position.y
                    if (dx * dx + dy * dy <= 150f * 150f) {
                        e.buffs.addBuff(BuffType.Slow, 0.8f, 2f)
                    }
                }
            }
            // O2: Iceborn — Absolute Zero: global freeze + shatter
            SkillVfxType.ABSOLUTE_ZERO -> {
                emitVfx(vfx, 0.5f, 0.5f, 0.3f, unit, 2f)
                for (e in enemies) {
                    if (!e.alive) continue
                    e.buffs.addBuff(BuffType.Slow, 0.8f, 3f) // 3s freeze
                    e.takeDamage(e.hp * 0.15f) // 15% current HP shatter
                }
            }
            // O3: Glacier Emperor — Ice Age Blizzard zone
            SkillVfxType.ICE_AGE_BLIZZARD -> {
                emitVfx(vfx, 0.5f, 0.5f, 0.4f, unit, 8f)
                val zone = zonePool?.acquire()
                if (zone != null) {
                    zone.init(
                        pos = com.example.jaygame.engine.math.Vec2(640f, 360f),
                        radius = 250f, duration = 8f,
                        tickInterval = 1f,
                        tickDamage = unit.effectiveATK() * 0.8f,
                        slowPercent = 0.7f,
                        family = 1, grade = unit.grade,
                    )
                }
            }
            // O4: Yuki — Eternal Winter
            SkillVfxType.ETERNAL_WINTER -> {
                emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 4f)
                for (e in enemies) {
                    if (!e.alive) continue
                    e.buffs.addBuff(BuffType.Slow, 0.8f, 4f)
                    e.takeDamage(e.maxHp * 0.2f)
                }
            }
            // O5: Chronos — Time Stop
            SkillVfxType.TIME_STOP -> {
                emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 5f)
                for (e in enemies) {
                    if (!e.alive) continue
                    e.buffs.addBuff(BuffType.Slow, 0.8f, 5f) // near-frozen
                    if (e.hp < e.maxHp * 0.25f) e.hp = 0f // execute
                }
            }

            // ── Poison ──
            // P1: Plague — Poison Cloud
            SkillVfxType.POISON_CLOUD -> {
                emitVfx(vfx, tx, ty, 0.08f, unit, 5f)
                val zone = zonePool?.acquire()
                if (zone != null) {
                    zone.init(
                        pos = targetEnemy.position.copy(),
                        radius = 80f, duration = 5f,
                        tickInterval = 1f,
                        tickDamage = unit.effectiveATK() * 0.4f,
                        slowPercent = 0.15f,
                        family = 2, grade = unit.grade,
                    )
                }
            }
            // P2: Corrosive — Acid Spray: cone AoE + defense reduction
            SkillVfxType.ACID_SPRAY -> {
                emitVfx(vfx, tx, ty, 0.12f, unit, 2f)
                val atk = unit.effectiveATK()
                for (e in enemies) {
                    if (!e.alive) continue
                    val dx = e.position.x - unit.position.x
                    val dy = e.position.y - unit.position.y
                    if (dx * dx + dy * dy <= 180f * 180f) {
                        e.takeDamage(atk * 2.5f)
                        e.buffs.addBuff(BuffType.Slow, 0.4f, 6f)
                    }
                }
            }
            // P3: Hecate — Toxic Domain: poison swamp
            SkillVfxType.TOXIC_DOMAIN -> {
                emitVfx(vfx, tx, ty, 0.15f, unit, 10f)
                val zone = zonePool?.acquire()
                if (zone != null) {
                    zone.init(
                        pos = targetEnemy.position.copy(),
                        radius = 120f, duration = 10f,
                        tickInterval = 1f,
                        tickDamage = unit.effectiveATK() * 0.6f,
                        slowPercent = 0.5f,
                        family = 2, grade = unit.grade,
                    )
                }
            }
            // P4: Nidhogg — Poison Breath: massive cone
            SkillVfxType.NIDHOGG_BREATH -> {
                emitVfx(vfx, tx, ty, 0.2f, unit, 6f)
                val atk = unit.effectiveATK()
                for (e in enemies) {
                    if (!e.alive) continue
                    e.takeDamage(atk * 4f)
                    e.buffs.addBuff(BuffType.DoT, atk * 0.3f, 6f)
                    e.buffs.addBuff(BuffType.Slow, 0.5f, 4f)
                    if (e.hp < e.maxHp * 0.2f) e.hp = 0f
                }
            }
            // P5: Apocalypse — Universal Decay
            SkillVfxType.UNIVERSAL_DECAY -> {
                emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 15f)
                val zone = zonePool?.acquire()
                if (zone != null) {
                    zone.init(
                        pos = com.example.jaygame.engine.math.Vec2(640f, 360f),
                        radius = 400f, duration = 15f,
                        tickInterval = 1f,
                        tickDamage = unit.effectiveATK() * 0.5f,
                        slowPercent = 0.3f,
                        family = 2, grade = unit.grade,
                    )
                }
            }

            // ── Lightning ──
            // Q1: Thunder — Lightning Strike
            SkillVfxType.LIGHTNING_STRIKE -> {
                emitVfx(vfx, tx, ty, 0.08f, unit, 0.5f)
                targetEnemy.takeDamage(unit.effectiveATK() * 2.5f)
                targetEnemy.buffs.addBuff(BuffType.Slow, 0.5f, 1.5f)
                BattleBridge.onDamageDealt(tx, ty, (unit.effectiveATK() * 2.5f).toInt(), true)
            }
            // Q2: Storm — Static Field: chain lightning
            SkillVfxType.STATIC_FIELD -> {
                emitVfx(vfx, nx, ny, 0.1f, unit, 4f)
                val atk = unit.effectiveATK()
                var chainDmg = atk * 1.8f
                val hit = mutableSetOf<Enemy>()
                var current = targetEnemy
                repeat(8) {
                    if (!current.alive || current in hit) return@repeat
                    hit.add(current)
                    current.takeDamage(chainDmg)
                    current.buffs.addBuff(BuffType.Slow, 0.5f, 1f)
                    chainDmg *= 1.1f
                    current = enemies.filter { it.alive && it !in hit }
                        .minByOrNull { it.position.distanceSqTo(current.position) } ?: return@repeat
                }
            }
            // Q3: Thunder King — Thunderstorm zone
            SkillVfxType.THUNDERSTORM -> {
                emitVfx(vfx, 0.5f, 0.5f, 0.3f, unit, 8f)
                val zone = zonePool?.acquire()
                if (zone != null) {
                    zone.init(
                        pos = com.example.jaygame.engine.math.Vec2(640f, 360f),
                        radius = 200f, duration = 8f,
                        tickInterval = 1f,
                        tickDamage = unit.effectiveATK() * 1.5f,
                        family = 3, grade = unit.grade,
                    )
                }
            }
            // Q4: Thor — Mjolnir Throw: massive single + AoE
            SkillVfxType.MJOLNIR_THROW -> {
                emitVfx(vfx, tx, ty, 0.4f, unit, 2f)
                val atk = unit.effectiveATK()
                for (e in enemies) {
                    if (!e.alive) continue
                    e.takeDamage(atk * 3.5f)
                    e.buffs.addBuff(BuffType.Slow, 0.8f, 2f)
                }
            }
            // Q5: Zeus — Divine Punishment
            SkillVfxType.DIVINE_PUNISHMENT -> {
                emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 3f)
                val atk = unit.effectiveATK()
                for (e in enemies) {
                    if (!e.alive) continue
                    e.takeDamage(atk * 6f)
                    e.buffs.addBuff(BuffType.Slow, 0.8f, 3f)
                    if (e.hp < e.maxHp * 0.1f) e.hp = 0f
                }
            }

            // ── Support ──
            // R1: Oracle — Heal Pulse
            SkillVfxType.HEAL_PULSE -> {
                emitVfx(vfx, nx, ny, 0.12f, unit, 1f)
            }
            // R2: Valkyrie — War Song: global ATK buff (emit VFX)
            SkillVfxType.WAR_SONG_AURA -> {
                emitVfx(vfx, nx, ny, 0.2f, unit, 6f)
            }
            // R3: Seraphim — Divine Shield
            SkillVfxType.DIVINE_SHIELD -> {
                emitVfx(vfx, nx, ny, 0.15f, unit, 8f)
            }
            // R4: Arcana — Harmony Field
            SkillVfxType.HARMONY_FIELD -> {
                emitVfx(vfx, 0.5f, 0.5f, 0.3f, unit, 10f)
            }
            // R5: Gaia — Genesis Light
            SkillVfxType.GENESIS_LIGHT -> {
                emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 15f)
            }

            // ── Wind ──
            // S1: Cyclone — Pull & damage
            SkillVfxType.CYCLONE_PULL -> {
                emitVfx(vfx, tx, ty, 0.08f, unit, 3f)
                val zone = zonePool?.acquire()
                if (zone != null) {
                    zone.init(
                        pos = targetEnemy.position.copy(),
                        radius = 70f, duration = 3f,
                        tickInterval = 0.5f,
                        tickDamage = unit.effectiveATK() * 1.5f,
                        slowPercent = 0.4f,
                        family = 5, grade = unit.grade,
                    )
                }
            }
            // S2: Typhoon — Eye of Storm zone
            SkillVfxType.EYE_OF_STORM -> {
                emitVfx(vfx, nx, ny, 0.15f, unit, 6f)
                val zone = zonePool?.acquire()
                if (zone != null) {
                    zone.init(
                        pos = unit.position.copy(),
                        radius = 130f, duration = 6f,
                        tickInterval = 0.5f,
                        tickDamage = unit.effectiveATK() * 2f,
                        slowPercent = 0.5f,
                        family = 5, grade = unit.grade,
                    )
                }
            }
            // S3: Sky Lord — Vacuum Slash: line AoE
            SkillVfxType.VACUUM_SLASH -> {
                emitVfx(vfx, tx, ty, 0.2f, unit, 1.5f)
                val atk = unit.effectiveATK()
                for (e in enemies) {
                    if (!e.alive) continue
                    e.takeDamage(atk * 3f)
                    e.buffs.addBuff(BuffType.Slow, 0.3f, 3f) // silence as slow
                }
            }
            // S4: Sylph — Dimensional Slash
            SkillVfxType.DIMENSIONAL_SLASH -> {
                emitVfx(vfx, tx, ty, 0.3f, unit, 2f)
                val atk = unit.effectiveATK()
                for (e in enemies) {
                    if (!e.alive) continue
                    e.takeDamage(atk * 4f)
                    e.buffs.addBuff(BuffType.Slow, 0.5f, 3f)
                }
            }
            // S5: Vayu — Breath of All
            SkillVfxType.BREATH_OF_ALL -> {
                emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 10f)
                val atk = unit.effectiveATK()
                for (e in enemies) {
                    if (!e.alive) continue
                    e.takeDamage(atk * 7f)
                    e.buffs.addBuff(BuffType.Slow, 0.8f, 3f)
                }
            }

            else -> {}
        }
    }

    private fun emitVfx(type: SkillVfxType, x: Float, y: Float, radius: Float, unit: GameUnit, duration: Float) {
        BattleBridge.emitSkillEvent(
            SkillEvent(
                type = type,
                x = x, y = y,
                radius = radius,
                grade = unit.grade,
                family = unit.family,
                duration = duration,
            )
        )
    }

    /**
     * Map (family, grade) to VfxType ordinal.
     * Grade 2=Hero, 3=Legend, 4=Ancient, 5=Mythic, 6=Immortal
     */
    private fun resolveVfxType(family: Int, grade: Int): Int {
        return resolveVfxTypeEnum(family, grade)?.ordinal ?: -1
    }

    private fun resolveVfxTypeEnum(family: Int, grade: Int): SkillVfxType? {
        val gradeOffset = grade - 2 // 0=Hero, 1=Legend, 2=Ancient, 3=Mythic, 4=Immortal
        if (gradeOffset < 0) return null

        return when (family) {
            0 -> { // Fire: LINGERING_FLAME(0), FIRESTORM_METEOR(1), VOLCANIC_ERUPTION(2), PHOENIX_CARPET_BOMB(3)/PHOENIX_REVIVE(3), SUPERNOVA(4)
                when (gradeOffset) {
                    0 -> SkillVfxType.LINGERING_FLAME
                    1 -> SkillVfxType.FIRESTORM_METEOR
                    2 -> SkillVfxType.VOLCANIC_ERUPTION
                    3 -> SkillVfxType.PHOENIX_CARPET_BOMB
                    4 -> SkillVfxType.SUPERNOVA
                    else -> null
                }
            }
            1 -> { // Frost
                when (gradeOffset) {
                    0 -> SkillVfxType.FROST_NOVA
                    1 -> SkillVfxType.ABSOLUTE_ZERO
                    2 -> SkillVfxType.ICE_AGE_BLIZZARD
                    3 -> SkillVfxType.ETERNAL_WINTER
                    4 -> SkillVfxType.TIME_STOP
                    else -> null
                }
            }
            2 -> { // Poison
                when (gradeOffset) {
                    0 -> SkillVfxType.POISON_CLOUD
                    1 -> SkillVfxType.ACID_SPRAY
                    2 -> SkillVfxType.TOXIC_DOMAIN
                    3 -> SkillVfxType.NIDHOGG_BREATH
                    4 -> SkillVfxType.UNIVERSAL_DECAY
                    else -> null
                }
            }
            3 -> { // Lightning
                when (gradeOffset) {
                    0 -> SkillVfxType.LIGHTNING_STRIKE
                    1 -> SkillVfxType.STATIC_FIELD
                    2 -> SkillVfxType.THUNDERSTORM
                    3 -> SkillVfxType.MJOLNIR_THROW
                    4 -> SkillVfxType.DIVINE_PUNISHMENT
                    else -> null
                }
            }
            4 -> { // Support
                when (gradeOffset) {
                    0 -> SkillVfxType.HEAL_PULSE
                    1 -> SkillVfxType.WAR_SONG_AURA
                    2 -> SkillVfxType.DIVINE_SHIELD
                    3 -> SkillVfxType.HARMONY_FIELD
                    4 -> SkillVfxType.GENESIS_LIGHT
                    else -> null
                }
            }
            5 -> { // Wind
                when (gradeOffset) {
                    0 -> SkillVfxType.CYCLONE_PULL
                    1 -> SkillVfxType.EYE_OF_STORM
                    2 -> SkillVfxType.VACUUM_SLASH
                    3 -> SkillVfxType.DIMENSIONAL_SLASH
                    4 -> SkillVfxType.BREATH_OF_ALL
                    else -> null
                }
            }
            else -> null
        }
    }
}
