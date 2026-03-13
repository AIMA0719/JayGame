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
            // Fire: Lingering Flame — leave fire on attack position
            SkillVfxType.LINGERING_FLAME -> {
                unit.passiveCounter++
                if (unit.passiveCounter % 3 == 0) { // every 3rd attack
                    val target = unit.currentTarget ?: return
                    val tx = target.position.x / W
                    val ty = target.position.y / H
                    emitVfx(vfx, tx, ty, 0.05f, unit, 3f)
                }
            }

            // Lightning: Overcharge — every 5th attack bonus
            SkillVfxType.LIGHTNING_STRIKE -> {
                unit.passiveCounter++
                if (unit.passiveCounter % 5 == 0) {
                    val target = unit.currentTarget ?: return
                    val tx = target.position.x / W
                    val ty = target.position.y / H
                    emitVfx(vfx, tx, ty, 0.08f, unit, 0.5f)
                }
            }

            // Support: Oracle crit buff (passive aura, VFX periodically)
            SkillVfxType.HEAL_PULSE -> {
                unit.passiveCounter++
                if (unit.passiveCounter % 30 == 0) { // periodic visual
                    emitVfx(vfx, nx, ny, 0.1f, unit, 0.8f)
                }
            }

            // Wind: Cyclone pull chance
            SkillVfxType.CYCLONE_PULL -> {
                unit.passiveCounter++
                if (unit.passiveCounter % 4 == 0 && Math.random() < 0.25) {
                    val target = unit.currentTarget ?: return
                    val tx = target.position.x / W
                    val ty = target.position.y / H
                    emitVfx(vfx, tx, ty, 0.06f, unit, 2f)
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
            SkillVfxType.FIRESTORM_METEOR -> emitVfx(vfx, tx, ty, 0.12f, unit, 1.5f)
            SkillVfxType.VOLCANIC_ERUPTION -> emitVfx(vfx, tx, ty, 0.15f, unit, 8f)
            SkillVfxType.PHOENIX_CARPET_BOMB -> emitVfx(vfx, 0.5f, 0.5f, 0.3f, unit, 6f)
            SkillVfxType.SUPERNOVA -> emitVfx(vfx, nx, ny, 0.5f, unit, 3f)

            SkillVfxType.FROST_NOVA -> emitVfx(vfx, nx, ny, 0.1f, unit, 1f)
            SkillVfxType.ABSOLUTE_ZERO -> emitVfx(vfx, 0.5f, 0.5f, 0.3f, unit, 2f)
            SkillVfxType.ICE_AGE_BLIZZARD -> emitVfx(vfx, 0.5f, 0.5f, 0.4f, unit, 8f)
            SkillVfxType.ETERNAL_WINTER -> emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 4f)
            SkillVfxType.TIME_STOP -> emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 5f)

            SkillVfxType.POISON_CLOUD -> emitVfx(vfx, tx, ty, 0.08f, unit, 5f)
            SkillVfxType.ACID_SPRAY -> emitVfx(vfx, tx, ty, 0.12f, unit, 2f)
            SkillVfxType.TOXIC_DOMAIN -> emitVfx(vfx, tx, ty, 0.15f, unit, 10f)
            SkillVfxType.NIDHOGG_BREATH -> emitVfx(vfx, tx, ty, 0.2f, unit, 6f)
            SkillVfxType.UNIVERSAL_DECAY -> emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 15f)

            SkillVfxType.LIGHTNING_STRIKE -> emitVfx(vfx, tx, ty, 0.08f, unit, 0.5f)
            SkillVfxType.STATIC_FIELD -> emitVfx(vfx, nx, ny, 0.1f, unit, 4f)
            SkillVfxType.THUNDERSTORM -> emitVfx(vfx, 0.5f, 0.5f, 0.3f, unit, 8f)
            SkillVfxType.MJOLNIR_THROW -> emitVfx(vfx, tx, ty, 0.4f, unit, 2f)
            SkillVfxType.DIVINE_PUNISHMENT -> emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 3f)

            SkillVfxType.HEAL_PULSE -> emitVfx(vfx, nx, ny, 0.12f, unit, 1f)
            SkillVfxType.WAR_SONG_AURA -> emitVfx(vfx, nx, ny, 0.2f, unit, 6f)
            SkillVfxType.DIVINE_SHIELD -> emitVfx(vfx, nx, ny, 0.15f, unit, 8f)
            SkillVfxType.HARMONY_FIELD -> emitVfx(vfx, 0.5f, 0.5f, 0.3f, unit, 10f)
            SkillVfxType.GENESIS_LIGHT -> emitVfx(vfx, 0.5f, 0.5f, 0.5f, unit, 15f)

            SkillVfxType.CYCLONE_PULL -> emitVfx(vfx, tx, ty, 0.08f, unit, 3f)
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
