package com.example.jaygame.engine

import com.example.jaygame.data.UnitFamily
import com.example.jaygame.data.UnitRace
import com.example.jaygame.engine.math.Vec2
import kotlin.math.cos
import kotlin.math.sin

class GameUnit {
    var alive = false
    @Deprecated("Use blueprintId instead")
    var unitDefId = -1
    var grade = 0
    @Deprecated("Use families instead")
    var family = 0
    var level = 1
    var tileIndex = -1
    var position = Vec2()
    var homePosition = Vec2()
    var baseATK = 0f
    var atkSpeed = 0f
    var range = 0f
    var abilityType = 0
    var abilityValue = 0f
    var isAttacking = false
    /** 공격 발사 시 ATTACK_ANIM_DURATION→0 감소하는 1회성 애니메이션 타이머 */
    var attackAnimTimer = 0f
    /** 스킬/궁극기 발동 모션 타이머 (0.4초) */
    var skillAnimTimer = 0f
    /** 크리티컬 히트 모션 타이머 (0.2초) */
    var critAnimTimer = 0f
    /** Combined speed multiplier (group upgrade + synergy) — set by BattleEngine each frame */
    var spdMultiplier = 1f
    /** 통합 강화 ATK 보너스 — BattleEngine이 매 프레임 그룹별로 설정 */
    var groupAtkBonus = 0f
    val buffs = BuffContainer()

    // ── Unique ability fields (M4) ──
    var uniqueAbilityType = -1       // index into UniqueAbilitySystem types, -1 = none
    var passiveCounter = 0           // generic counter for passive triggers (e.g., hit count)

    // ── New Strategy-pattern fields (Task 4) ──
    var blueprintId: String = ""
    var families: List<UnitFamily> = emptyList()
    var role: UnitRole = UnitRole.RANGED_DPS
    var attackRange: AttackRange = AttackRange.RANGED
    var damageType: DamageType = DamageType.PHYSICAL
    var unitCategory: UnitCategory = UnitCategory.NORMAL
    var race: UnitRace = UnitRace.HUMAN
    var hp: Float = 0f
    var maxHp: Float = 0f
    var defense: Float = 0f
    var magicResist: Float = 0f
    var blockCount: Int = 0
    var behavior: UnitBehavior? = null
    var fieldController: FieldEffectController? = null
    var state: UnitState = UnitState.IDLE

    // ── 마나/궁극기 필드 ──
    var mana: Float = 0f                // 현재 마나 (0~maxMana)
    var maxMana: Float = 100f           // 최대 마나
    var manaPerHit: Float = 0f          // 공격 시 마나 획득량
    var hasUltimate: Boolean = false    // 궁극기 보유 여부

    var moveSpeed = 75f

    // ── AbilityEngine fields (data-driven ability system) ──
    var activeAbility: ActiveAbility? = null
    var abilityTimer: Float = 0f      // cooldown timer for periodic/self-buff abilities
    var abilityCounter: Int = 0       // attack counter for Nth-attack triggers
    var abilityStacks: Int = 0        // for stackable self-buffs
    var abilityAuraTick: Float = 0f   // aura tick accumulator

    private var attackCooldown = 0f
    var currentTarget: Enemy? = null

    // ── New blueprint-based init (Task 4) ──
    fun initFromBlueprint(bp: UnitBlueprint) {
        blueprintId = bp.id
        race = bp.race
        family = raceToFamily(bp.race)
        families = bp.families.ifEmpty {
            listOf(com.example.jaygame.data.UnitFamily.entries[family])
        }
        grade = bp.grade.ordinal
        role = bp.role
        attackRange = bp.attackRange
        damageType = bp.damageType
        unitCategory = bp.unitCategory
        hp = bp.stats.hp
        maxHp = bp.stats.hp
        baseATK = bp.stats.baseATK
        atkSpeed = bp.stats.baseSpeed
        range = bp.stats.range
        defense = bp.stats.defense
        magicResist = bp.stats.magicResist
        moveSpeed = bp.stats.moveSpeed
        blockCount = bp.stats.blockCount
        state = UnitState.IDLE
        alive = true
    }

    @Deprecated("Use initFromBlueprint instead")
    fun init(
        unitDefId: Int, grade: Int, family: Int, level: Int,
        tileIndex: Int, homePos: Vec2,
        baseATK: Float, atkSpeed: Float, range: Float,
        abilityType: Int, abilityValue: Float,
    ) {
        this.alive = true
        this.unitDefId = unitDefId
        this.grade = grade
        this.family = family
        this.level = level
        this.tileIndex = tileIndex
        this.position = homePos.copy()
        this.homePosition = homePos.copy()
        this.baseATK = baseATK
        this.atkSpeed = atkSpeed
        this.range = range
        this.abilityType = abilityType
        this.abilityValue = abilityValue
        this.isAttacking = false
        this.attackAnimTimer = 0f
        this.attackCooldown = 0f
        this.buffs.clear()
        this.passiveCounter = 0
        this.moveSpeed = when (family) {
            0 -> 110f
            1 -> 55f
            2 -> 75f
            3 -> 65f
            4 -> 90f
            5 -> 100f   // Wind: fast
            else -> 75f
        }
    }

    fun update(dt: Float, findEnemy: (Vec2, Float) -> Enemy?) {
        if (!alive) return

        buffs.update(dt)
        val spdMult = buffs.getSpdMultiplier()
        attackCooldown -= dt * spdMult
        if (attackAnimTimer > 0f) attackAnimTimer = (attackAnimTimer - dt).coerceAtLeast(0f)

        // 셀 고정: 유닛은 homePosition에 고정, 사거리 내 적 탐지 시 제자리 공격
        position.x = homePosition.x
        position.y = homePosition.y

        val enemy = findEnemy(position, range)
        if (enemy != null) {
            isAttacking = true
            currentTarget = enemy
        } else {
            isAttacking = false
            currentTarget = null
        }
    }

    companion object {
        const val ATTACK_ANIM_DURATION = 0.15f
        const val SKILL_ANIM_DURATION = 0.4f
        const val CRIT_ANIM_DURATION = 0.2f
        val LEVEL_MULTIPLIERS = floatArrayOf(1f, 1.5f, 2.2f, 3.2f, 4.5f, 6f, 8f)

        /**
         * 종족(Race) → 레거시 가문(Family) 매핑.
         * HUMAN→Fire(0), SPIRIT→Frost(1), DEMON→Poison(2),
         * ROBOT→Lightning(3), ANIMAL→Support(4)
         */
        fun raceToFamily(race: UnitRace): Int = when (race) {
            UnitRace.HUMAN  -> 0  // Fire: Splash
            UnitRace.SPIRIT -> 1  // Frost: Slow/CC
            UnitRace.DEMON  -> 2  // Poison: DoT
            UnitRace.ROBOT  -> 3  // Lightning: Chain
            UnitRace.ANIMAL -> 4  // Support: Buff
        }
    }

    fun canAttack(): Boolean = isAttacking && attackCooldown <= 0f && currentTarget?.alive == true

    fun onAttack() {
        attackCooldown = 1f / atkSpeed
        attackAnimTimer = ATTACK_ANIM_DURATION
    }

    /** 공격 성공 시 마나 축적 (전설/신화 궁극기용) */
    fun chargeMana() {
        if (hasUltimate && manaPerHit > 0f) {
            mana = (mana + manaPerHit).coerceAtMost(maxMana)
        }
    }

    fun effectiveATK(): Float {
        val levelMult = LEVEL_MULTIPLIERS.getOrElse(level - 1) { 1f }
        return baseATK * levelMult * buffs.getAtkMultiplier() * (1f + groupAtkBonus)
    }

    fun projectileVisualType(): Int = race.ordinal * 2 + if (damageType == DamageType.MAGIC) 1 else 0

    fun reset() {
        // Reset new strategy-pattern fields
        behavior?.reset()
        fieldController?.reset()
        behavior = null
        fieldController = null
        state = UnitState.IDLE
        blueprintId = ""
        families = emptyList()
        role = UnitRole.RANGED_DPS
        attackRange = AttackRange.RANGED
        damageType = DamageType.PHYSICAL
        unitCategory = UnitCategory.NORMAL
        race = UnitRace.HUMAN
        hp = 0f
        maxHp = 0f
        defense = 0f
        magicResist = 0f
        moveSpeed = 75f
        blockCount = 0
        groupAtkBonus = 0f

        // Legacy ability fields (pool reuse 시 잔존 방지)
        abilityType = 0
        abilityValue = 0f

        // Existing reset logic
        alive = false
        currentTarget = null
        attackAnimTimer = 0f
        buffs.clear()
        uniqueAbilityType = -1
        passiveCounter = 0

        // Reset AbilityEngine fields
        activeAbility = null
        abilityTimer = 0f
        abilityCounter = 0
        abilityStacks = 0
        abilityAuraTick = 0f

        // Reset mana fields
        mana = 0f
        maxMana = 100f
        manaPerHit = 0f
        hasUltimate = false
    }

}
