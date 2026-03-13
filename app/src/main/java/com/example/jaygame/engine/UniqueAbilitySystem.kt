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

    private const val W = 1280f
    private const val H = 720f

    /**
     * Initialize unique ability fields on a newly placed unit.
     * Should be called when a unit is placed/summoned/merged.
     */
    fun initUnit(unit: GameUnit) {
        val def = UNIT_DEFS_MAP[unit.unitDefId] ?: return
        val ability = def.uniqueAbility ?: return

        unit.uniqueAbilityType = resolveVfxType(unit.family, unit.grade)
        unit.uniqueAbilityMaxCd = ability.cooldown.toFloat()
        unit.uniqueAbilityCooldown = ability.cooldown.toFloat() * 0.5f // start half-ready
        unit.passiveCounter = 0
    }

    // Reference to engine zone pool — set by BattleEngine
    var zonePool: ObjectPool<ZoneEffect>? = null

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
                    unit.uniqueAbilityCooldown = unit.uniqueAbilityMaxCd
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

        // Find best target position (densest enemies or strongest)
        val targetEnemy = enemies.filter { it.alive }.maxByOrNull { it.maxHp } ?: return
        val tx = targetEnemy.position.x / W
        val ty = targetEnemy.position.y / H

        when (vfx) {
            // ── Fire actives ──
            SkillVfxType.FIRESTORM_METEOR -> emitVfx(vfx, tx, ty, 0.12f, unit, 1.5f)
            SkillVfxType.VOLCANIC_ERUPTION -> emitVfx(vfx, tx, ty, 0.15f, unit, 8f)
            SkillVfxType.PHOENIX_CARPET_BOMB -> emitVfx(vfx, 0.5f, 0.5f, 0.3f, unit, 6f)
            SkillVfxType.SUPERNOVA -> emitVfx(vfx, nx, ny, 0.5f, unit, 3f)

            // O1: Frost Hero — Frost Nova: freeze enemies in range
            SkillVfxType.FROST_NOVA -> {
                emitVfx(vfx, nx, ny, 0.1f, unit, 1f)
                // Apply 2-sec freeze (100% slow) to nearby enemies
                for (e in enemies) {
                    if (!e.alive) continue
                    val dx = e.position.x - unit.position.x
                    val dy = e.position.y - unit.position.y
                    if (dx * dx + dy * dy <= 150f * 150f) {
                        e.buffs.addBuff(BuffType.Slow, 0.8f, 2f)
                    }
                }
            }
            SkillVfxType.ABSOLUTE_ZERO -> emitVfx(vfx, 0.5f, 0.5f, 0.3f, unit, 2f)
            SkillVfxType.ICE_AGE_BLIZZARD -> emitVfx(vfx, 0.5f, 0.5f, 0.4f, unit, 8f)
            SkillVfxType.ETERNAL_WINTER -> emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 4f)
            SkillVfxType.TIME_STOP -> emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 5f)

            // P1: Poison Hero — Poison Cloud: create poison zone
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
            SkillVfxType.ACID_SPRAY -> emitVfx(vfx, tx, ty, 0.12f, unit, 2f)
            SkillVfxType.TOXIC_DOMAIN -> emitVfx(vfx, tx, ty, 0.15f, unit, 10f)
            SkillVfxType.NIDHOGG_BREATH -> emitVfx(vfx, tx, ty, 0.2f, unit, 6f)
            SkillVfxType.UNIVERSAL_DECAY -> emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 15f)

            // Q1: Lightning Hero — Lightning Strike: single target burst
            SkillVfxType.LIGHTNING_STRIKE -> {
                emitVfx(vfx, tx, ty, 0.08f, unit, 0.5f)
                targetEnemy.takeDamage(unit.effectiveATK() * 2.5f)
                targetEnemy.buffs.addBuff(BuffType.Slow, 0.5f, 1.5f) // stun-like slow
                BattleBridge.onDamageDealt(tx, ty, (unit.effectiveATK() * 2.5f).toInt(), true)
            }
            SkillVfxType.STATIC_FIELD -> emitVfx(vfx, nx, ny, 0.1f, unit, 4f)
            SkillVfxType.THUNDERSTORM -> emitVfx(vfx, 0.5f, 0.5f, 0.3f, unit, 8f)
            SkillVfxType.MJOLNIR_THROW -> emitVfx(vfx, tx, ty, 0.4f, unit, 2f)
            SkillVfxType.DIVINE_PUNISHMENT -> emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 3f)

            // R1: Support Hero — Heal Pulse: buff nearby allies
            SkillVfxType.HEAL_PULSE -> {
                emitVfx(vfx, nx, ny, 0.12f, unit, 1f)
                // Note: ally buff would need allied unit buff system
                // For now, just emit VFX
            }
            SkillVfxType.WAR_SONG_AURA -> emitVfx(vfx, nx, ny, 0.2f, unit, 6f)
            SkillVfxType.DIVINE_SHIELD -> emitVfx(vfx, nx, ny, 0.15f, unit, 8f)
            SkillVfxType.HARMONY_FIELD -> emitVfx(vfx, 0.5f, 0.5f, 0.3f, unit, 10f)
            SkillVfxType.GENESIS_LIGHT -> emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 15f)

            // S1: Wind Hero — Cyclone Pull: pull & damage
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
            SkillVfxType.EYE_OF_STORM -> emitVfx(vfx, nx, ny, 0.15f, unit, 6f)
            SkillVfxType.VACUUM_SLASH -> emitVfx(vfx, tx, ty, 0.2f, unit, 1.5f)
            SkillVfxType.DIMENSIONAL_SLASH -> emitVfx(vfx, tx, ty, 0.3f, unit, 2f)
            SkillVfxType.BREATH_OF_ALL -> emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 10f)

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
