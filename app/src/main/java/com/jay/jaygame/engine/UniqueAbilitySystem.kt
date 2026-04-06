@file:Suppress("DEPRECATION")
package com.jay.jaygame.engine

import com.jay.jaygame.audio.SfxManager
import com.jay.jaygame.audio.SoundEvent
import com.jay.jaygame.bridge.BattleBridge
import com.jay.jaygame.bridge.SkillEvent
import com.jay.jaygame.bridge.SkillVfxType
/**
 * Manages unique ability cooldowns, activation, and VFX emission
 * for hero-grade (2+) units.
 *
 * Called from BattleEngine.updateUnits() every frame.
 */
object UniqueAbilitySystem {

    private const val W = BattleEngine.W
    private const val H = BattleEngine.H
    /** VFX 키 해석 — grade ≤ 2(패시브)면 passiveAbilityId, 그 외(궁극기)면 blueprintId */
    private fun resolveVfxKey(unit: GameUnit): String =
        if (unit.grade <= 2 && unit.passiveAbilityId.isNotEmpty()) unit.passiveAbilityId else unit.blueprintId
    /** Blueprint 기반 ability marker 기준값 (uniqueAbilityType >= 이 값이면 blueprint) */
    const val BLUEPRINT_ABILITY_BASE = 100
    /** 유닛 위치 PNG 이펙트 반경 — 그리드 1칸 크기 (96/720 ≈ 0.067) */
    private const val UNIT_VFX_RADIUS = 0.07f

    private fun manaPerHitForGrade(grade: Int): Float =
        if (grade >= 4) 7f else if (grade >= 3) 9f else 0f

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
        unit.resetMana()
        unit.manaPerHit = 0f
        unit.hasUltimate = false

        // Only Hero (grade 2) and above have unique abilities
        if (unit.grade < 2) return

        // Blueprint 기반 유닛: uniqueAbility JSON에서 마나/궁극기 설정
        if (unit.blueprintId.isNotEmpty() && unit.activeAbility != null) {
            val bp = if (BlueprintRegistry.isReady) BlueprintRegistry.instance.findById(unit.blueprintId) else null
            if (bp?.uniqueAbility != null) {
                unit.manaPerHit = manaPerHitForGrade(unit.grade)
                unit.maxMana = 100f
                unit.hasUltimate = unit.manaPerHit > 0f
                unit.uniqueAbilityType = BLUEPRINT_ABILITY_BASE + unit.grade
                unit.bpPassiveId = bp.uniqueAbility.passive?.id ?: ""
            }
            return  // skip legacy family-based init
        }

        val vfxType = resolveVfxType(unit.familyOrdinal, unit.grade)
        if (vfxType < 0) return

        unit.uniqueAbilityType = vfxType

        // 마나 시스템: 전설(3)/신화(4) = 마나 축적 궁극기, 고대(2) = 패시브
        when {
            unit.grade >= 3 -> {
                // 전설/신화: 마나 기반 궁극기
                unit.hasUltimate = true
                unit.manaPerHit = manaPerHitForGrade(unit.grade)  // 신화/불멸: ~14타 충전, 전설: ~11타 충전
                unit.maxMana = 100f
                unit.resetMana()
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
    fun update(units: List<GameUnit>, dt: Float, enemies: List<Enemy>,
               spatialHash: SpatialHash<Enemy>? = null, allUnits: ObjectPool<GameUnit>? = null) {
        for (unit in units) {
            if (!unit.alive || unit.uniqueAbilityType < 0) continue
            if (unit.grade < 2) continue // only hero+ have unique abilities

            // Blueprint 패시브 (uniqueAbility.passive) — 타이머/카운터/확률 기반
            if (unit.uniqueAbilityType >= BLUEPRINT_ABILITY_BASE && spatialHash != null && allUnits != null) {
                handleBlueprintPassive(unit, dt, enemies, spatialHash, allUnits)
            }

            // 레거시 패시브 triggers on attack
            if (unit.isAttacking && unit.currentTarget?.alive == true) {
                if (unit.uniqueAbilityType < 100) {
                    handlePassive(unit, dt, enemies)
                }
            }

            // 마나 기반 궁극기 발동 (전설/신화)
            if (unit.hasUltimate && unit.mana >= unit.maxMana) {
                if (enemies.isNotEmpty()) {
                    unit.skillAnimTimer = GameUnit.SKILL_ANIM_DURATION
                    // Blueprint-based ultimate (type >= 100)
                    if (unit.uniqueAbilityType >= BLUEPRINT_ABILITY_BASE && spatialHash != null && allUnits != null) {
                        activateBlueprintUltimate(unit, enemies, spatialHash, allUnits)
                    } else {
                        activateAbility(unit, enemies)
                        unit.resetMana()  // 마나 리셋
                    }
                }
            }

        }
    }

    /**
     * Handle passive triggers (called when unit is attacking).
     */
    private fun handlePassive(unit: GameUnit, dt: Float, enemies: List<Enemy>) {
        val vfx = resolveVfxTypeEnum(unit.familyOrdinal, unit.grade) ?: return
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
                    val ty = target.position.y / H - 0.025f
                    emitVfx(vfx, tx, ty, 0.05f, unit, 3f)
                    // Create actual damage zone
                    val zone = zonePool?.acquire()
                    if (zone == null) { android.util.Log.w("UniqueAbility", "Zone pool exhausted — ultimate skipped"); return }
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
                    emitVfx(vfx, nx, ny, 0.08f, unit, 1.5f)
                }
            }

            // P1: Poison Hero — Plague spread (passive: poison spreads on death)
            SkillVfxType.POISON_CLOUD -> {
                // Passive: VFX hint on target
                unit.passiveCounter++
                if (unit.passiveCounter % 10 == 0) {
                    val target = unit.currentTarget ?: return
                    val tx = target.position.x / W
                    val ty = target.position.y / H - 0.025f
                    emitVfx(vfx, tx, ty, 0.04f, unit, 1.5f)
                }
            }

            // Q1: Lightning Hero — Overcharge: every 5th attack bonus damage + chain
            SkillVfxType.LIGHTNING_STRIKE -> {
                unit.passiveCounter++
                if (unit.passiveCounter % 5 == 0) {
                    val target = unit.currentTarget ?: return
                    val tx = target.position.x / W
                    val ty = target.position.y / H - 0.025f
                    emitVfx(vfx, tx, ty, 0.08f, unit, 1.5f)
                    // Bonus damage on overcharged hit
                    target.takeDamage(unit.effectiveATK() * 1f) // 200% total (100% normal + 100% bonus)
                    BattleBridge.onDamageDealt(tx, ty, (unit.effectiveATK() * 1f).toInt(), true)
                }
            }

            // R1: Support Hero — Oracle: crit buff aura (passive, periodic VFX)
            SkillVfxType.HEAL_PULSE -> {
                unit.passiveCounter++
                if (unit.passiveCounter % 30 == 0) {
                    emitVfx(vfx, nx, ny, 0.1f, unit, 1.5f)
                }
            }

            // S1: Wind Hero — Cyclone: 25% chance small tornado
            SkillVfxType.CYCLONE_PULL -> {
                unit.passiveCounter++
                if (unit.passiveCounter % 4 == 0 && Math.random() < 0.25) {
                    val target = unit.currentTarget ?: return
                    val tx = target.position.x / W
                    val ty = target.position.y / H - 0.025f
                    emitVfx(vfx, tx, ty, 0.06f, unit, 2f)
                    // Create wind zone with slow
                    val zone = zonePool?.acquire()
                    if (zone == null) { android.util.Log.w("UniqueAbility", "Zone pool exhausted — ultimate skipped"); return }
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
        val vfx = resolveVfxTypeEnum(unit.familyOrdinal, unit.grade) ?: return
        val nx = unit.position.x / W
        val ny = unit.position.y / H
        val gradeScale = DamageCalculator.gradeMultiplier(unit.grade)

        // Dual-family: also emit secondary family VFX on active skill
        emitSecondaryFamilyActive(unit, enemies)

        // Find best target position (densest enemies or strongest)
        val targetEnemy = enemies.maxByOrNull { it.maxHp } ?: return
        val tx = targetEnemy.position.x / W
        val ty = targetEnemy.position.y / H - 0.025f // 몹 몸통 중앙으로 보정

        when (vfx) {
            // ── Fire ──
            // N2: Inferno — Firestorm Meteor: AoE meteor strike
            SkillVfxType.FIRESTORM_METEOR -> {
                emitSequentialVfx(vfx, tx, ty, unit, count = 4, delayMs = 250L, radius = 0.15f, duration = 1.5f, scatter = 0.06f)
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
                emitSequentialVfx(vfx, tx, ty, unit, count = 3, delayMs = 400L, radius = 0.20f, duration = 2.0f, scatter = 0.04f)
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
                emitSequentialVfx(vfx, tx, ty, unit, count = 3, delayMs = 150L, radius = 0.14f, duration = 1.2f, scatter = 0.05f)
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
                emitSequentialVfx(vfx, nx, ny, unit, count = 3, delayMs = 300L, radius = 0.22f, duration = 1.5f, scatter = 0.08f)
                for (e in enemies) {
                    if (!e.alive) continue
                    e.buffs.addBuff(BuffType.Slow, 0.8f, 3f) // 3s freeze
                    e.takeDamage(e.hp * 0.15f) // 15% current HP shatter
                }
            }
            // O3: Glacier Emperor — Ice Age Blizzard zone
            SkillVfxType.ICE_AGE_BLIZZARD -> {
                emitSequentialVfx(vfx, nx, ny, unit, count = 4, delayMs = 350L, radius = 0.18f, duration = 2.0f, scatter = 0.07f)
                val zone = zonePool?.acquire()
                if (zone != null) {
                    zone.init(
                        pos = com.jay.jaygame.engine.math.Vec2(360f, 580f),
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
                emitSequentialVfx(vfx, tx, ty, unit, count = 3, delayMs = 300L, radius = 0.14f, duration = 1.8f, scatter = 0.05f)
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
                emitSequentialVfx(vfx, tx, ty, unit, count = 3, delayMs = 200L, radius = 0.16f, duration = 1.5f, scatter = 0.06f)
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
                emitSequentialVfx(vfx, tx, ty, unit, count = 4, delayMs = 400L, radius = 0.18f, duration = 2.5f, scatter = 0.06f)
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
                emitSequentialVfx(vfx, tx, ty, unit, count = 3, delayMs = 120L, radius = 0.13f, duration = 1.0f, scatter = 0.04f)
                targetEnemy.takeDamage(unit.effectiveATK() * 2.5f)
                targetEnemy.buffs.addBuff(BuffType.Slow, 0.5f, 1.5f)
                BattleBridge.onDamageDealt(tx, ty, (unit.effectiveATK() * 2.5f).toInt(), true)
            }
            // Q2: Storm — Static Field: chain lightning
            SkillVfxType.STATIC_FIELD -> {
                emitSequentialVfx(vfx, tx, ty, unit, count = 3, delayMs = 200L, radius = 0.16f, duration = 1.5f, scatter = 0.07f)
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
                    var nearest: Enemy? = null
                    var nearestDistSq = Float.MAX_VALUE
                    for (e in enemies) {
                        if (!e.alive || e in hit) continue
                        val dSq = e.position.distanceSqTo(current.position)
                        if (dSq < nearestDistSq) { nearestDistSq = dSq; nearest = e }
                    }
                    current = nearest ?: return@repeat
                }
            }
            // Q3: Thunder King — Thunderstorm zone
            SkillVfxType.THUNDERSTORM -> {
                emitSequentialVfx(vfx, nx, ny, unit, count = 5, delayMs = 250L, radius = 0.16f, duration = 1.5f, scatter = 0.09f)
                val zone = zonePool?.acquire()
                if (zone != null) {
                    zone.init(
                        pos = com.jay.jaygame.engine.math.Vec2(360f, 580f),
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
                emitSequentialVfx(vfx, nx, ny, unit, count = 3, delayMs = 300L, radius = 0.18f, duration = 1.5f, scatter = 0.0f, firstExact = false)
            }
            // R2: Valkyrie — War Song: 아군 전체 ATK +25% 버프 (6초)
            SkillVfxType.WAR_SONG_AURA -> {
                emitSequentialVfx(vfx, nx, ny, unit, count = 3, delayMs = 250L, radius = 0.20f, duration = 2.0f, scatter = 0.0f, firstExact = false)
                val atkBonus = 0.25f * gradeScale
                activeUnits?.forEach { ally ->
                    if (ally.alive) {
                        ally.buffs.addBuff(BuffType.AtkUp, atkBonus, 6f)
                    }
                }
            }
            // R3: Seraphim — Divine Shield: 아군 전체 실드 부여 (8초)
            SkillVfxType.DIVINE_SHIELD -> {
                emitSequentialVfx(vfx, nx, ny, unit, count = 3, delayMs = 350L, radius = 0.22f, duration = 2.0f, scatter = 0.0f, firstExact = false)
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
                emitSequentialVfx(vfx, tx, ty, unit, count = 3, delayMs = 200L, radius = 0.14f, duration = 1.5f, scatter = 0.05f)
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
                emitSequentialVfx(vfx, tx, ty, unit, count = 4, delayMs = 250L, radius = 0.16f, duration = 1.8f, scatter = 0.07f)
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
                emitSequentialVfx(vfx, tx, ty, unit, count = 3, delayMs = 180L, radius = 0.20f, duration = 1.2f, scatter = 0.08f)
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

    private fun emitVfx(
        type: SkillVfxType, x: Float, y: Float, radius: Float,
        unit: GameUnit, duration: Float,
        startTime: Long = System.currentTimeMillis(),
    ) {
        BattleBridge.emitSkillEvent(
            SkillEvent(
                type = type,
                x = x, y = y,
                radius = radius,
                grade = unit.grade,
                family = unit.familyOrdinal,
                startTime = startTime,
                duration = duration,
                vfxKey = resolveVfxKey(unit),
            )
        )
    }

    /**
     * 범용 순차 VFX 헬퍼 — 여러 SkillEvent를 startTime 오프셋으로 시간차 발동.
     * scatter > 0이면 중심 주변 랜덤 산포, scatter == 0이면 같은 위치에 반복.
     * firstExact=true면 첫 발은 정확 위치, false면 첫 발도 산포 적용.
     */
    private fun emitSequentialVfx(
        type: SkillVfxType,
        cx: Float,
        cy: Float,
        unit: GameUnit,
        count: Int,
        delayMs: Long = 200L,
        radius: Float = 0.15f,
        duration: Float = 1.5f,
        scatter: Float = 0.06f,
        firstExact: Boolean = true,
    ) {
        val now = System.currentTimeMillis()
        for (i in 0 until count) {
            val mx: Float
            val my: Float
            if (scatter <= 0f || (firstExact && i == 0)) {
                mx = cx
                my = cy
            } else {
                mx = (cx + (kotlin.random.Random.nextFloat() * 2f - 1f) * scatter).coerceIn(0.02f, 0.98f)
                my = (cy + (kotlin.random.Random.nextFloat() * 2f - 1f) * scatter).coerceIn(0.02f, 0.98f)
            }
            emitVfx(type, mx, my, radius, unit, duration, startTime = now + i * delayMs)
        }
    }

    /** 유닛 위치에 등급/종족 기반 VFX emit (패시브용 단축 헬퍼) */
    private fun emitUnitVfx(unit: GameUnit, duration: Float) {
        val vfx = resolveVfxTypeEnum(unit.familyOrdinal, unit.grade) ?: return
        emitVfx(vfx, unit.position.x / W, unit.position.y / H, UNIT_VFX_RADIUS, unit, duration)
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
        if (secondaryFamily == unit.familyOrdinal) return
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
        if (secondaryFamily == unit.familyOrdinal) return
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

    // ── Pre-compiled regex patterns (GC 방지) ──
    private val RE_ALLY_ATK = Regex("아군.*?ATK\\s*(\\d+)%")
    private val RE_ALLY_SPD = Regex("공격속도\\s*(\\d+)%\\s*증가")
    private val RE_DURATION = Regex("(\\d+)초간")
    private val RE_HP_PERCENT = Regex("HP.*?(\\d+)%")
    private val RE_STUN = Regex("(\\d+\\.?\\d*)초[간\\s]*(?:(?:전체\\s*)?(?:스턴|동결|공포|정지|이동불가|완전\\s*동결))")
    private val RE_CONFUSE = Regex("(\\d+\\.?\\d*)초\\s*혼란")
    private val RE_SLOW = Regex("(\\d+\\.?\\d*)초[간\\s]*(?:전체\\s*)?감속\\s*(\\d+)%")
    private val RE_ARMOR = Regex("방어[력]?[/마저]*\\s*(\\d+)%\\s*감소(?:\\s*\\((\\d+)초\\))?")
    private val RE_MR = Regex("(?:마법저항|마저)\\s*(\\d+)%\\s*감소(?:\\s*\\((\\d+)초\\))?")
    private val RE_DOT = Regex("(\\d+)초.*?(?:DoT|화염)")
    private val RE_DOT_STRENGTH = Regex("초당\\s*ATK\\s*(\\d+)%")
    private val RE_DOT_DUR = Regex("(\\d+)초\\s*(?:화염\\s*)?DoT")

    // ── 패시브 헬퍼 함수 ──

    /** N번째 공격마다 ATK 배율 크리티컬 */
    private fun handleNthAttackCrit(unit: GameUnit, every: Int, atkMult: Float) {
        if (!unit.isAttacking || unit.currentTarget?.alive != true) return
        unit.passiveCounter++
        if (unit.passiveCounter % every == 0) {
            val target = unit.currentTarget ?: return
            target.takeDamage(unit.effectiveATK() * atkMult, unit.damageType == DamageType.MAGIC)
            unit.critAnimTimer = GameUnit.CRIT_ANIM_DURATION
        }
    }

    /** 확률 기반 단일 타겟 CC */
    private fun handleChanceCC(unit: GameUnit, chance: Float, type: BuffType, value: Float, duration: Float) {
        if (!unit.isAttacking || unit.currentTarget?.alive != true) return
        if (kotlin.random.Random.nextFloat() < chance) {
            unit.currentTarget!!.buffs.addBuff(type, value, duration, unit.tileIndex)
        }
    }

    /** 종족 오라 버프 (0.5초 틱) */
    private fun handleRaceAura(unit: GameUnit, dt: Float, allUnits: ObjectPool<GameUnit>,
                               race: com.jay.jaygame.data.UnitRace, atkPercent: Float, spdPercent: Float = 0f) {
        unit.bpPassiveTimer += dt
        if (unit.bpPassiveTimer < 0.5f) return
        unit.bpPassiveTimer = 0f
        allUnits.forEach { a ->
            if (a.alive && a.race == race) {
                a.buffs.addBuff(BuffType.AtkUp, atkPercent, 1f, unit.tileIndex)
                if (spdPercent > 0f) a.buffs.addBuff(BuffType.SpdUp, spdPercent, 1f, unit.tileIndex)
            }
        }
    }

    /** 보스/엘리트 추가 데미지 */
    private fun handleBossBonus(unit: GameUnit, atkPercent: Float) {
        if (!unit.isAttacking || unit.currentTarget?.alive != true) return
        val target = unit.currentTarget!!
        if (target.isBoss || target.isElite) {
            target.takeDamage(unit.effectiveATK() * atkPercent, unit.damageType == DamageType.MAGIC)
        }
    }

    /** 영구 ATK 보너스 추가 */
    private fun addPermanentAtk(unit: GameUnit, increment: Float, cap: Float) {
        if (unit.permanentAtkBonus < cap) unit.permanentAtkBonus += increment
    }

    /** allUnits에서 랜덤 아군 N체 선택 (GC 최소화 — 랜덤 인덱스 기반) */
    private fun pickRandomAllies(allUnits: ObjectPool<GameUnit>, exclude: GameUnit, count: Int): List<GameUnit> {
        val candidates = mutableListOf<GameUnit>()
        allUnits.forEach { a -> if (a.alive && a.hasUltimate && a !== exclude) candidates.add(a) }
        if (candidates.size <= count) return candidates
        val picked = mutableListOf<GameUnit>()
        val indices = candidates.indices.toMutableList()
        repeat(count) {
            val idx = indices.removeAt(kotlin.random.Random.nextInt(indices.size))
            picked.add(candidates[idx])
        }
        return picked
    }

    /**
     * Blueprint 기반 uniqueAbility.passive 처리.
     * 패시브 ID별로 분기하여 타이머/카운터/확률 기반 효과 적용.
     */
    private fun handleBlueprintPassive(
        unit: GameUnit, dt: Float, enemies: List<Enemy>,
        spatialHash: SpatialHash<Enemy>, allUnits: ObjectPool<GameUnit>,
    ) {
        val passiveId = unit.bpPassiveId
        if (passiveId.isEmpty()) return

        when (passiveId) {
            // ── 타이머 기반 AoE CC ──
            "guardian_x_passive" -> timedAoECC(unit, dt, spatialHash, 12f, 200f, BuffType.Stun, 1f, 2f)
            "nexus_passive", "digital_apocalypse_passive" -> timedAoECC(unit, dt, spatialHash, 10f, 9999f, BuffType.Stun, 1f, 1.5f)

            // ── 타이머 + 아군 마나 충전 ──
            "yggdrasil_passive" -> {
                unit.bpPassiveTimer += dt
                if (unit.bpPassiveTimer >= 10f) {
                    unit.bpPassiveTimer = 0f
                    pickRandomAllies(allUnits, unit, 2).forEach { it.mana += 15f }
                    applyAoECC(unit, spatialHash, 9999f, BuffType.Slow, 0.10f, 2f)
                    emitUnitVfx(unit, 2f)
                }
            }
            "grand_sage_passive" -> {
                unit.bpPassiveTimer += dt
                if (unit.bpPassiveTimer >= 10f) {
                    unit.bpPassiveTimer = 0f
                    pickRandomAllies(allUnits, unit, 1).forEach { it.mana += 20f }
                    emitUnitVfx(unit, 2f)
                }
            }

            // ── 공격 카운터 기반 ──
            "emperor_passive" -> handleNthAttackCrit(unit, 5, 3f)
            "abadon_passive", "abyss_king_passive" -> handleNthAttackCrit(unit, 4, 4f)
            "god_emperor_passive" -> handleNthAttackCrit(unit, 3, 5f)
            "chaos_lord_passive", "world_end_passive" -> handleNthAttackCrit(unit, 3, 4f)

            // ── 확률 기반 CC ──
            "frost_demon_passive" -> handleChanceCC(unit, 0.15f, BuffType.Stun, 1f, 3f)
            "nemesis_passive", "extermination_protocol_passive" -> handleChanceCC(unit, 0.15f, BuffType.Stun, 1f, 2.5f)
            "illusionist_passive" -> handleChanceCC(unit, 0.10f, BuffType.Stun, 1f, 3f)

            // ── 종족 오라 버프 ──
            "primal_spirit_passive" -> handleRaceAura(unit, dt, allUnits, com.jay.jaygame.data.UnitRace.SPIRIT, 0.15f, 0.10f)
            "demon_king_passive" -> handleRaceAura(unit, dt, allUnits, com.jay.jaygame.data.UnitRace.DEMON, 0.20f)
            "lucifer_passive", "paradise_lost_passive" -> handleRaceAura(unit, dt, allUnits, com.jay.jaygame.data.UnitRace.DEMON, 0.20f, 0.15f)

            // ── 보스/엘리트 추가 데미지 ──
            "judge_passive" -> handleBossBonus(unit, 0.15f)
            "haetae_passive" -> handleBossBonus(unit, 0.25f)

            // ── on-kill / wave-start 전용 (여기선 no-op) ──
            "omega_passive", "divine_beast_passive", "kirin_passive", "celestial_grace_passive" -> {}

            // ── 원소 순환 ──
            "four_beasts_passive" -> {
                if (unit.isAttacking && unit.currentTarget?.alive == true) {
                    val target = unit.currentTarget!!
                    val atk = unit.effectiveATK()
                    when (unit.passiveCounter % 4) {
                        0 -> target.buffs.addBuff(BuffType.DoT, atk * 0.10f, 3f, unit.tileIndex)
                        1 -> target.buffs.addBuff(BuffType.Slow, 0.30f, 2f, unit.tileIndex)
                        2 -> {}
                        3 -> target.buffs.addBuff(BuffType.Stun, 1f, 0.5f, unit.tileIndex)
                    }
                    unit.passiveCounter++
                }
            }
        }
    }

    /** 타이머 기반 AoE CC 헬퍼 */
    private fun timedAoECC(unit: GameUnit, dt: Float, spatialHash: SpatialHash<Enemy>,
                           cooldown: Float, range: Float, type: BuffType, value: Float, duration: Float) {
        unit.bpPassiveTimer += dt
        if (unit.bpPassiveTimer >= cooldown) {
            unit.bpPassiveTimer = 0f
            applyAoECC(unit, spatialHash, range, type, value, duration)
            emitUnitVfx(unit, 2f)
        }
    }

    /** 적 처치 시 blueprint 패시브 on-kill 효과 처리. 코인 보너스 반환. */
    fun onBlueprintPassiveKill(unit: GameUnit, allUnits: ObjectPool<GameUnit>?): Float {
        if (unit.uniqueAbilityType < 100) return 0f
        val passiveId = unit.bpPassiveId
        if (passiveId.isEmpty()) return 0f
        var coinBonus = 0f

        when (passiveId) {
            "omega_passive" -> addPermanentAtk(unit, 0.05f, 0.50f)
            "nexus_passive", "digital_apocalypse_passive" -> addPermanentAtk(unit, 0.03f, 0.60f)
            "demon_king_passive" -> addPermanentAtk(unit, 0.05f, 0.30f)
            "abadon_passive", "abyss_king_passive" -> addPermanentAtk(unit, 0.05f, 0.35f)
            "chaos_lord_passive", "world_end_passive" -> addPermanentAtk(unit, 0.05f, 0.40f)
            "god_emperor_passive" -> unit.mana += 25f
            "emperor_passive" -> coinBonus += 3f
        }
        return coinBonus
    }

    /** 웨이브 시작 시 blueprint 패시브 효과 처리. */
    fun onBlueprintPassiveWaveStart(allUnits: ObjectPool<GameUnit>, addCoins: (Float) -> Unit) {
        allUnits.forEach { unit ->
            if (!unit.alive || unit.uniqueAbilityType < 100) return@forEach
            val passiveId = unit.bpPassiveId
            if (passiveId.isEmpty()) return@forEach

            when (passiveId) {
                "divine_beast_passive" -> addCoins(5f)
                "god_emperor_passive" -> addCoins(5f)
                "kirin_passive", "celestial_grace_passive" -> {
                    addCoins(3f)
                    pickRandomAllies(allUnits, unit, 1).forEach { it.mana += 10f }
                }
            }
        }
    }

    /** AoE CC — 범위 내 적에게 버프 적용 (forEach로 리스트 할당 없이 처리) */
    private fun applyAoECC(unit: GameUnit, spatialHash: SpatialHash<Enemy>, range: Float, type: BuffType, value: Float, duration: Float) {
        val r = range.coerceAtLeast(100f)
        spatialHash.forEach(unit.position.x - r, unit.position.y - r, unit.position.x + r, unit.position.y + r) { enemy ->
            if (!enemy.alive) return@forEach
            if (range < 9000f && enemy.position.distanceTo(unit.position) > r) return@forEach
            enemy.buffs.addBuff(type, value, duration, unit.tileIndex)
        }
    }

    private fun activateBlueprintUltimate(
        unit: GameUnit,
        enemies: List<Enemy>,
        spatialHash: SpatialHash<Enemy>,
        allUnits: ObjectPool<GameUnit>,
    ) {
        val bp = (if (BlueprintRegistry.isReady) BlueprintRegistry.instance.findById(unit.blueprintId) else null) ?: return
        val ua = bp.uniqueAbility ?: return
        val active = ua.active ?: return
        val atk = unit.effectiveATK()
        val atkMult = active.value / 100f
        val range = active.range.coerceAtLeast(200f)
        val isMagic = active.damageType == DamageType.MAGIC
        val desc = active.description

        val ccEffects = parseUltimateCCFromDescription(desc, atk)

        // ── Ally buffs (ATK/SPD 증가) ──
        val allyAtkMatch = RE_ALLY_ATK.find(desc)
        val allySpdMatch = RE_ALLY_SPD.find(desc)
        val buffDur = RE_DURATION.find(desc)?.groupValues?.get(1)?.toFloatOrNull() ?: 5f
        if (allyAtkMatch != null) {
            val percent = allyAtkMatch.groupValues[1].toFloatOrNull()?.div(100f) ?: 0f
            allUnits.forEach { ally -> if (ally.alive) ally.buffs.addBuff(BuffType.AtkUp, percent, buffDur, unit.tileIndex) }
        }
        if (allySpdMatch != null) {
            val percent = allySpdMatch.groupValues[1].toFloatOrNull()?.div(100f) ?: 0f
            allUnits.forEach { ally -> if (ally.alive) ally.buffs.addBuff(BuffType.SpdUp, percent, buffDur, unit.tileIndex) }
        }

        // %HP 데미지 (루프 밖에서 1회만 파싱)
        val hpPercent = RE_HP_PERCENT.find(desc)?.groupValues?.get(1)?.toFloatOrNull()?.div(100f) ?: 0f

        var hitTarget: Enemy? = null
        for (enemy in spatialHash.query(unit.position.x - range, unit.position.y - range, unit.position.x + range, unit.position.y + range)) {
            if (!enemy.alive) continue
            if (enemy.position.distanceTo(unit.position) > range) continue
            enemy.takeDamage(atk * atkMult, isMagic)
            for (cc in ccEffects) {
                enemy.buffs.addBuff(cc.type, cc.value, cc.duration, unit.tileIndex)
            }
            if (hpPercent > 0f) enemy.takeDamage(enemy.hp * hpPercent, true)
            if (hitTarget == null || enemy.maxHp > hitTarget!!.maxHp) hitTarget = enemy
        }

        if (hitTarget != null) {
            val htx = hitTarget!!.position.x / 720f
            val hty = hitTarget!!.position.y / 1280f - 0.025f
            val ultVfxType = resolveVfxTypeEnum(
                family = unit.familyOrdinal,
                grade = unit.grade.coerceAtMost(UnitGrade.MYTHIC.ordinal),
            ) ?: SkillVfxType.VOLCANIC_ERUPTION
            emitVfx(ultVfxType, htx, hty, 0.1f, unit, 2f)
        }

        unit.resetMana()
    }

    private data class ParsedCC(val type: BuffType, val value: Float, val duration: Float)

    private fun parseUltimateCCFromDescription(desc: String, atk: Float): List<ParsedCC> {
        val results = mutableListOf<ParsedCC>()

        RE_STUN.find(desc)?.let { m ->
            results.add(ParsedCC(BuffType.Stun, 1f, m.groupValues[1].toFloatOrNull() ?: 3f))
        }
        if (results.none { it.type == BuffType.Stun }) {
            RE_CONFUSE.find(desc)?.let { m ->
                results.add(ParsedCC(BuffType.Stun, 1f, m.groupValues[1].toFloatOrNull() ?: 3f))
            }
        }
        RE_SLOW.find(desc)?.let { m ->
            val dur = m.groupValues[1].toFloatOrNull() ?: 5f
            val strength = m.groupValues[2].toFloatOrNull()?.div(100f) ?: 0.4f
            results.add(ParsedCC(BuffType.Slow, strength, dur))
        }
        RE_ARMOR.find(desc)?.let { m ->
            val percent = m.groupValues[1].toFloatOrNull()?.div(100f) ?: 0.4f
            val dur = m.groupValues[2].toFloatOrNull() ?: 5f
            results.add(ParsedCC(BuffType.ArmorBreak, percent, dur))
        }
        RE_MR.find(desc)?.let { m ->
            val dur = m.groupValues[2].toFloatOrNull() ?: 5f
            results.add(ParsedCC(BuffType.MagicResistBreak, m.groupValues[1].toFloatOrNull()?.div(100f) ?: 0.4f, dur))
        }
        RE_DOT.find(desc)?.let { m ->
            val dur = m.groupValues[1].toFloatOrNull() ?: 5f
            val dotPercent = RE_DOT_STRENGTH.find(desc)?.groupValues?.get(1)?.toFloatOrNull()?.div(100f) ?: 0.10f
            results.add(ParsedCC(BuffType.DoT, atk * dotPercent, dur))
        }
        if (results.none { it.type == BuffType.DoT } && (desc.contains("DoT") || desc.contains("화염"))) {
            val dur = RE_DOT_DUR.find(desc)?.groupValues?.get(1)?.toFloatOrNull() ?: 5f
            results.add(ParsedCC(BuffType.DoT, atk * 0.10f, dur))
        }

        return results
    }
}
