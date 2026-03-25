@file:Suppress("DEPRECATION")
package com.example.jaygame.engine

import com.example.jaygame.audio.SfxManager
import com.example.jaygame.audio.SoundEvent
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.bridge.SkillEvent
import com.example.jaygame.bridge.SkillVfxType
/**
 * Manages unique ability cooldowns, activation, and VFX emission
 * for hero-grade (2+) units.
 *
 * Called from BattleEngine.updateUnits() every frame.
 */
object UniqueAbilitySystem {

    private const val W = BattleEngine.W
    private const val H = BattleEngine.H

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
        unit.passiveCounter = 0
        unit.mana = 0f
        unit.manaPerHit = 0f
        unit.hasUltimate = false

        // Only Hero (grade 2) and above have unique abilities
        if (unit.grade < 2) return

        val vfxType = resolveVfxType(unit.family, unit.grade)
        if (vfxType < 0) return

        unit.uniqueAbilityType = vfxType

        // 마나 시스템: 전설(3)/신화(4) = 마나 축적 궁극기, 고대(2) = 패시브
        when {
            unit.grade >= 3 -> {
                // 전설/신화: 마나 기반 궁극기
                unit.hasUltimate = true
                unit.manaPerHit = if (unit.grade >= 4) 6f else 9f  // 신화: 느리게 충전, 강한 궁극기
                unit.maxMana = 100f
                unit.mana = 0f
            }
            else -> {
                // 고대: 패시브 (기존 방식 유지)
            }
        }
    }

    /**
     * Infer ability cooldown based on family and grade when JSON data is missing.
     * Hero (grade 2) = passive only (cooldown 0, triggers on attack).
     * Legend+ = active abilities with longer cooldowns for stronger (higher-grade) skills.
     * Values are modeled after legacy UnitDef cooldowns in UNIT_DEFS.
     */
    // Reference to engine zone pool — set by BattleEngine
    @Volatile var zonePool: ObjectPool<ZoneEffect>? = null
    // Reference to active units — set by BattleEngine (Support 궁극기용)
    @Volatile var activeUnits: List<GameUnit>? = null

    /**
     * Update unique abilities for all active units.
     * Called once per frame from BattleEngine.updateUnits().
     */
    fun update(units: List<GameUnit>, dt: Float, enemies: List<Enemy>) {
        for (unit in units) {
            if (!unit.alive || unit.uniqueAbilityType < 0) continue
            if (unit.grade < 2) continue // only hero+ have unique abilities

            // 고대(2): 패시브 triggers on attack
            if (unit.isAttacking && unit.currentTarget?.alive == true) {
                handlePassive(unit, dt, enemies)
            }

            // 마나 기반 궁극기 발동 (전설/신화)
            if (unit.hasUltimate && unit.mana >= unit.maxMana) {
                if (enemies.isNotEmpty()) {
                    activateAbility(unit, enemies)
                    unit.mana = 0f  // 마나 리셋
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

        // Dual-family: also emit secondary family VFX
        emitSecondaryFamilyPassive(unit, nx, ny)

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
        SfxManager.play(SoundEvent.SkillActivate, 0.8f)
        val vfx = resolveVfxTypeEnum(unit.family, unit.grade) ?: return
        val nx = unit.position.x / W
        val ny = unit.position.y / H
        val gradeScale = DamageCalculator.gradeMultiplier(unit.grade)

        // Dual-family: also emit secondary family VFX on active skill
        emitSecondaryFamilyActive(unit, enemies)

        // Find best target position (densest enemies or strongest)
        val targetEnemy = enemies.maxByOrNull { it.maxHp } ?: return
        val tx = targetEnemy.position.x / W
        val ty = targetEnemy.position.y / H

        when (vfx) {
            // ── Fire ──
            // N2: Inferno — Firestorm Meteor: AoE meteor strike
            SkillVfxType.FIRESTORM_METEOR -> {
                emitVfx(vfx, tx, ty, 0.15f, unit, 3f)
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
            // ── Frost ──
            // O1: Frost Nova
            SkillVfxType.FROST_NOVA -> {
                emitVfx(vfx, tx, ty, 0.12f, unit, 2.5f)
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
                emitVfx(vfx, 0.5f, 0.5f, 0.3f, unit, 4f)
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
                        pos = com.example.jaygame.engine.math.Vec2(360f, 640f),
                        radius = 250f, duration = 8f,
                        tickInterval = 1f,
                        tickDamage = unit.effectiveATK() * 0.8f,
                        slowPercent = 0.7f,
                        family = 1, grade = unit.grade,
                    )
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
                emitVfx(vfx, tx, ty, 0.15f, unit, 3f)
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
            // ── Lightning ──
            // Q1: Thunder — Lightning Strike
            SkillVfxType.LIGHTNING_STRIKE -> {
                emitVfx(vfx, tx, ty, 0.12f, unit, 2f)
                targetEnemy.takeDamage(unit.effectiveATK() * 2.5f)
                targetEnemy.buffs.addBuff(BuffType.Slow, 0.5f, 1.5f)
                BattleBridge.onDamageDealt(tx, ty, (unit.effectiveATK() * 2.5f).toInt(), true)
            }
            // Q2: Storm — Static Field: chain lightning
            SkillVfxType.STATIC_FIELD -> {
                emitVfx(vfx, tx, ty, 0.15f, unit, 4f)
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
                        pos = com.example.jaygame.engine.math.Vec2(360f, 640f),
                        radius = 200f, duration = 8f,
                        tickInterval = 1f,
                        tickDamage = unit.effectiveATK() * 1.5f,
                        family = 3, grade = unit.grade,
                    )
                }
            }
            // ── Support ──
            // R1: Oracle — Heal Pulse
            SkillVfxType.HEAL_PULSE -> {
                emitVfx(vfx, nx, ny, 0.15f, unit, 3f)
            }
            // R2: Valkyrie — War Song: 아군 전체 ATK +25% 버프 (6초)
            SkillVfxType.WAR_SONG_AURA -> {
                emitVfx(vfx, 0.5f, 0.45f, 0.25f, unit, 6f)
                val atkBonus = 0.25f * gradeScale
                activeUnits?.forEach { ally ->
                    if (ally.alive) {
                        ally.buffs.addBuff(BuffType.AtkUp, atkBonus, 6f)
                    }
                }
            }
            // R3: Seraphim — Divine Shield: 아군 전체 실드 부여 (8초)
            SkillVfxType.DIVINE_SHIELD -> {
                emitVfx(vfx, 0.5f, 0.45f, 0.25f, unit, 8f)
                val shieldValue = unit.effectiveATK() * 2f * gradeScale
                activeUnits?.forEach { ally ->
                    if (ally.alive && !ally.buffs.hasBuff(BuffType.Shield)) {
                        ally.buffs.addBuff(BuffType.Shield, shieldValue, 8f)
                    }
                }
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
                emitVfx(vfx, tx, ty, 0.18f, unit, 6f)
                val zone = zonePool?.acquire()
                if (zone != null) {
                    zone.init(
                        pos = targetEnemy.position.copy(),
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
                emitVfx(vfx, tx, ty, 0.25f, unit, 3f)
                val atk = unit.effectiveATK()
                for (e in enemies) {
                    if (!e.alive) continue
                    e.takeDamage(atk * 3f)
                    e.buffs.addBuff(BuffType.Slow, 0.3f, 3f) // silence as slow
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
                abilityId = unit.blueprintId,
            )
        )
    }

    /**
     * Map (family, grade) to VfxType ordinal.
     * Grade 2=Hero, 3=Legend, 4=Mythic
     */
    private fun resolveVfxType(family: Int, grade: Int): Int {
        return resolveVfxTypeEnum(family, grade)?.ordinal ?: -1
    }

    /**
     * Dual-family passive: emit the secondary family's hero-grade VFX as a visual hint.
     * Only fires if the unit has 2+ families and the secondary family resolves to a VFX.
     */
    private fun emitSecondaryFamilyPassive(unit: GameUnit, nx: Float, ny: Float) {
        if (unit.families.size < 2) return
        val secondaryFamily = unit.families[1].ordinal
        if (secondaryFamily == unit.family) return
        val secondaryVfx = resolveVfxTypeEnum(secondaryFamily, unit.grade) ?: return
        // Emit a smaller, shorter version of the secondary family effect
        unit.passiveCounter.let { counter ->
            if (counter % 6 == 3) { // offset from primary so they alternate
                emitVfx(secondaryVfx, nx, ny, 0.06f, unit, 0.4f)
            }
        }
    }

    /**
     * Dual-family active: emit the secondary family's VFX simultaneously with the primary.
     * Uses slightly smaller radius and shorter duration than the primary.
     */
    private fun emitSecondaryFamilyActive(unit: GameUnit, enemies: List<Enemy>) {
        if (unit.families.size < 2) return
        val secondaryFamily = unit.families[1].ordinal
        if (secondaryFamily == unit.family) return
        val secondaryVfx = resolveVfxTypeEnum(secondaryFamily, unit.grade) ?: return
        val nx = unit.position.x / W
        val ny = unit.position.y / H

        // Find target position (same logic as primary)
        val targetEnemy = enemies.maxByOrNull { e ->
            enemies.count { o -> o.position.distanceSqTo(e.position) < 10000f }
        } ?: return
        val tx = targetEnemy.position.x / W
        val ty = targetEnemy.position.y / H

        // Emit secondary VFX at 80% size of what the primary would use, half duration
        val baseRadius = when {
            unit.grade >= 4 -> 0.25f   // Mythic
            unit.grade >= 3 -> 0.12f   // Legend
            else -> 0.08f
        }
        val baseDuration = when {
            unit.grade >= 4 -> 2.5f    // Mythic
            unit.grade >= 3 -> 1.5f    // Legend
            else -> 1f
        }
        emitVfx(secondaryVfx, tx, ty, baseRadius, unit, baseDuration)
    }

    private fun resolveVfxTypeEnum(family: Int, grade: Int): SkillVfxType? {
        val gradeOffset = grade - 2 // 0=Hero, 1=Legend, 2=Mythic
        if (gradeOffset < 0) return null

        return when (family) {
            0 -> { // Fire: LINGERING_FLAME(Hero), FIRESTORM_METEOR(Legend), VOLCANIC_ERUPTION(Mythic)
                when (gradeOffset) {
                    0 -> SkillVfxType.LINGERING_FLAME
                    1 -> SkillVfxType.FIRESTORM_METEOR
                    2 -> SkillVfxType.VOLCANIC_ERUPTION
                    else -> SkillVfxType.LINGERING_FLAME
                }
            }
            1 -> { // Frost: FROST_NOVA(Hero), ABSOLUTE_ZERO(Legend), ICE_AGE_BLIZZARD(Mythic)
                when (gradeOffset) {
                    0 -> SkillVfxType.FROST_NOVA
                    1 -> SkillVfxType.ABSOLUTE_ZERO
                    2 -> SkillVfxType.ICE_AGE_BLIZZARD
                    else -> SkillVfxType.FROST_NOVA
                }
            }
            2 -> { // Poison: POISON_CLOUD(Hero), ACID_SPRAY(Legend), TOXIC_DOMAIN(Mythic)
                when (gradeOffset) {
                    0 -> SkillVfxType.POISON_CLOUD
                    1 -> SkillVfxType.ACID_SPRAY
                    2 -> SkillVfxType.TOXIC_DOMAIN
                    else -> SkillVfxType.POISON_CLOUD
                }
            }
            3 -> { // Lightning: LIGHTNING_STRIKE(Hero), STATIC_FIELD(Legend), THUNDERSTORM(Mythic)
                when (gradeOffset) {
                    0 -> SkillVfxType.LIGHTNING_STRIKE
                    1 -> SkillVfxType.STATIC_FIELD
                    2 -> SkillVfxType.THUNDERSTORM
                    else -> SkillVfxType.LIGHTNING_STRIKE
                }
            }
            4 -> { // Support: HEAL_PULSE(Hero), WAR_SONG_AURA(Legend), DIVINE_SHIELD(Mythic)
                when (gradeOffset) {
                    0 -> SkillVfxType.HEAL_PULSE
                    1 -> SkillVfxType.WAR_SONG_AURA
                    2 -> SkillVfxType.DIVINE_SHIELD
                    else -> SkillVfxType.HEAL_PULSE
                }
            }
            5 -> { // Wind: CYCLONE_PULL(Hero), EYE_OF_STORM(Legend), VACUUM_SLASH(Mythic)
                when (gradeOffset) {
                    0 -> SkillVfxType.CYCLONE_PULL
                    1 -> SkillVfxType.EYE_OF_STORM
                    2 -> SkillVfxType.VACUUM_SLASH
                    else -> SkillVfxType.CYCLONE_PULL
                }
            }
            else -> null
        }
    }
}
