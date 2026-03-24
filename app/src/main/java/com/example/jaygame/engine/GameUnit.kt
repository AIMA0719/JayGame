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
    /** Combined speed multiplier (group upgrade + synergy) — set by BattleEngine each frame */
    var spdMultiplier = 1f
    /** 통합 강화 ATK 보너스 — BattleEngine이 매 프레임 그룹별로 설정 */
    var groupAtkBonus = 0f
    val buffs = BuffContainer()

    // ── Unique ability fields (M4) ──
    var uniqueAbilityType = -1       // index into UniqueAbilitySystem types, -1 = none
    var uniqueAbilityCooldown = 0f   // seconds until next activation
    var uniqueAbilityMaxCd = 0f      // max cooldown for this ability
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

    private var attackCooldown = 0f
    var currentTarget: Enemy? = null

    // ── New blueprint-based init (Task 4) ──
    fun initFromBlueprint(bp: UnitBlueprint) {
        blueprintId = bp.id
        families = bp.families
        family = bp.families.firstOrNull()?.ordinal ?: 0
        grade = bp.grade.ordinal
        race = bp.race
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
        this.attackCooldown = 0f
        this.buffs.clear()
        this.uniqueAbilityCooldown = 0f
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
        val LEVEL_MULTIPLIERS = floatArrayOf(1f, 1.5f, 2.2f, 3.2f, 4.5f, 6f, 8f)
    }

    fun canAttack(): Boolean = isAttacking && attackCooldown <= 0f && currentTarget?.alive == true

    fun onAttack() {
        attackCooldown = 1f / atkSpeed
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

        // Existing reset logic
        alive = false
        currentTarget = null
        buffs.clear()
        uniqueAbilityType = -1
        uniqueAbilityCooldown = 0f
        uniqueAbilityMaxCd = 0f
        passiveCounter = 0

        // Reset mana fields
        mana = 0f
        maxMana = 100f
        manaPerHit = 0f
        hasUltimate = false
    }
}
