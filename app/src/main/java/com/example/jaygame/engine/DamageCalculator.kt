package com.example.jaygame.engine

/**
 * 데미지 계산 공식 — 메이플 랜덤 디펜스 스타일
 *
 * 물리: FinalDamage = Damage × (100 / (100 + Defense))
 * 마법: FinalDamage = Damage × (100 / (100 + MagicResist))
 */
object DamageCalculator {

    // 속성 상성: FIRE(0)→FROST(1)→WIND(5)→LIGHTNING(3)→POISON(2)→FIRE(0), SUPPORT(4)=중립
    private val ADVANTAGE_MAP = intArrayOf(1, 5, 0, 2, -1, 3)    // [i] = i가 유리한 대상
    private val DISADVANTAGE_MAP = intArrayOf(2, 0, 3, 5, -1, 1)  // [i] = i가 불리한 대상

    /** 물리 데미지 계산 — 방어력 공식 적용 */
    fun calculatePhysicalDamage(
        baseDamage: Float,
        defense: Float,
        armorBreakReduction: Float = 0f, // 방어력 감소 디버프
        relicArmorPenPercent: Float = 0f, // 유물 방어력 관통 (0.0~1.0)
    ): Float {
        val effectiveDefense = (defense - armorBreakReduction).coerceAtLeast(0f)
        val penArmor = effectiveDefense * (1f - relicArmorPenPercent.coerceIn(0f, 1f))
        val finalDamage = baseDamage * (100f / (100f + penArmor))
        return finalDamage.coerceAtLeast(1f)
    }

    /** 마법 데미지 계산 — 마법 저항 diminishing returns 적용 */
    fun calculateMagicDamage(
        baseDamage: Float,
        magicResist: Float,
        relicMagicDmgPercent: Float = 0f, // 유물 마법 데미지 보너스 (0.0~1.0)
    ): Float {
        val effectiveMR = magicResist.coerceAtLeast(0f)
        val finalDamage = baseDamage * (100f / (100f + effectiveMR)) * (1f + relicMagicDmgPercent)
        return finalDamage.coerceAtLeast(1f)
    }

    /** 유닛 기본 데미지 계산 (레벨 × 버프 적용) */
    fun calculateUnitDamage(
        baseATK: Float,
        level: Int,
        buffMultiplier: Float = 1f,
        familyUpgradeLevel: Int = 0,
        relicAtkBonus: Float = 0f, // 유물 공격력 보너스 (0.0~1.0)
        relicCritChanceBonus: Float = 0f, // 유물 크리티컬 확률 보너스
        relicCritDamageBonus: Float = 0f, // 유물 크리티컬 데미지 보너스
    ): Float {
        val levelMultiplier = LEVEL_MULTIPLIERS.getOrElse(level - 1) { 1f }
        val familyBonus = 1f + familyUpgradeLevel * 0.05f // 계열 영구 강화: 레벨당 +5%
        val effectiveATK = baseATK * levelMultiplier * buffMultiplier * familyBonus * (1f + relicAtkBonus)
        val baseCritChance = 0.05f + relicCritChanceBonus
        val critMultiplier = if (Math.random() < baseCritChance) 2f + relicCritDamageBonus else 1f
        return effectiveATK * critMultiplier
    }

    private val LEVEL_MULTIPLIERS = floatArrayOf(1.0f, 1.5f, 2.5f, 4.0f, 6.0f)

    /** 등급(Grade) 기반 데미지 보정 — 레퍼런스: 랜덤디펜스 계열 (최고 등급 ~1.5x) */
    private val GRADE_MULTIPLIERS = floatArrayOf(
        1.0f,   // Common
        1.05f,  // Rare       (+5%)
        1.10f,  // Hero       (+10%)
        1.25f,  // Legend     (+25%)
        1.50f,  // Mythic     (+50%)
    )

    fun gradeMultiplier(grade: Int): Float =
        GRADE_MULTIPLIERS.getOrElse(grade) { 1f }

    // NEW: Behavior-based damage calculation — unified physical/magic with resist formula
    /**
     * Calculate damage to a GameUnit applying defense or magicResist reduction.
     * Formula: rawDamage * (100 / (100 + resist))
     */
    fun calculateDamageToUnit(rawDamage: Float, isMagic: Boolean, targetUnit: GameUnit): Float {
        val baseResist = if (isMagic) targetUnit.magicResist else targetUnit.defense
        val defBuff = if (!isMagic) targetUnit.buffs.getDefMultiplier() else 1f
        val resist = (baseResist * defBuff).coerceAtLeast(0f)
        return rawDamage * (100f / (100f + resist))
    }

    /**
     * 속성 상성 — 화염→냉기→바람→번개→독→화염 (1.2x),
     * 보조는 상성 없음 (1.0x)
     * @param attackerFamily 공격하는 유닛의 가족 (0-5)
     * @param defenderType 적 타입 (0-5, 연관)
     */
    fun familyAdvantageMultiplier(attackerFamily: Int, defenderType: Int): Float {
        // 보조(4)는 상성 없음, 범위 밖도 중립
        if (attackerFamily == 4 || defenderType == 4 || attackerFamily > 5 || defenderType > 5) return 1f
        // 상성 루프: 0(화염)→1(냉기)→5(바람)→3(번개)→2(독)→0(화염)
        val mapped = defenderType % 6
        return when (mapped) {
            ADVANTAGE_MAP.getOrElse(attackerFamily) { -1 } -> 1.2f  // 유리
            DISADVANTAGE_MAP.getOrElse(attackerFamily) { -1 } -> 0.85f // 불리
            else -> 1f
        }
    }

    /**
     * 크리티컬 판정 분리 — isCrit 여부와 배율을 함께 반환
     */
    data class CritResult(val isCrit: Boolean, val multiplier: Float)

    fun rollCrit(
        baseCritChance: Float = 0.05f,
        relicCritChanceBonus: Float = 0f,
        relicCritDamageBonus: Float = 0f,
        upgradeCritRate: Float = 0f,
    ): CritResult {
        val totalChance = (baseCritChance + relicCritChanceBonus + upgradeCritRate).coerceAtMost(0.8f)
        val isCrit = Math.random() < totalChance
        val multiplier = if (isCrit) 2f + relicCritDamageBonus else 1f
        return CritResult(isCrit, multiplier)
    }

    /**
     * 통합 데미지 계산 — 등급, 상성, 크리티컬 모두 반영
     */
    data class DamageResult(val damage: Float, val isCrit: Boolean)

    fun calculateFullDamage(
        baseATK: Float,
        level: Int,
        grade: Int,
        attackerFamily: Int,
        defenderType: Int,
        defense: Float,
        magicResist: Float,
        isPhysical: Boolean = true,
        buffMultiplier: Float = 1f,
        familyUpgradeLevel: Int = 0,
        relicAtkBonus: Float = 0f,
        relicArmorPenPercent: Float = 0f,
        relicCritChanceBonus: Float = 0f,
        relicCritDamageBonus: Float = 0f,
        upgradeCritRate: Float = 0f,
        armorBreakReduction: Float = 0f,
    ): DamageResult {
        val levelMult = LEVEL_MULTIPLIERS.getOrElse(level - 1) { 1f }
        val gradeMult = gradeMultiplier(grade)
        val familyBonus = 1f + familyUpgradeLevel * 0.05f // 계열 영구 강화: 레벨당 +5%
        val advantageMult = familyAdvantageMultiplier(attackerFamily, defenderType)

        val effectiveATK = baseATK * levelMult * gradeMult * buffMultiplier * familyBonus * (1f + relicAtkBonus) * advantageMult

        val crit = rollCrit(0.05f, relicCritChanceBonus, relicCritDamageBonus, upgradeCritRate)
        val dmgAfterCrit = effectiveATK * crit.multiplier

        val finalDmg = if (isPhysical) {
            calculatePhysicalDamage(dmgAfterCrit, defense, armorBreakReduction, relicArmorPenPercent)
        } else {
            calculateMagicDamage(dmgAfterCrit, magicResist)
        }

        return DamageResult(finalDmg, crit.isCrit)
    }
}
