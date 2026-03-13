package com.example.jaygame.engine

/**
 * 데미지 계산 공식 — 메이플 랜덤 디펜스 스타일
 *
 * 물리: FinalDamage = Damage × (100 / (100 + Defense))
 * 마법: FinalDamage = Damage × (1 - MagicResist)
 */
object DamageCalculator {

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

    /** 마법 데미지 계산 — 방어력 무시, 마법 저항 적용 */
    fun calculateMagicDamage(
        baseDamage: Float,
        magicResist: Float, // 0.0 ~ 1.0
        relicMagicDmgPercent: Float = 0f, // 유물 마법 데미지 보너스 (0.0~1.0)
    ): Float {
        val finalDamage = baseDamage * (1f - magicResist.coerceIn(0f, 0.9f)) * (1f + relicMagicDmgPercent)
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

    /** 공격 속도 쿨다운 계산 (프레임 단위) */
    fun calculateAttackCooldownFrames(
        atkSpeed: Float,
        spdBuffMultiplier: Float = 1f,
        fps: Int = 60,
    ): Int {
        val effectiveSpeed = atkSpeed * spdBuffMultiplier
        if (effectiveSpeed <= 0f) return fps // 1초 기본값
        val cooldownSeconds = 1f / effectiveSpeed
        return (cooldownSeconds * fps).toInt().coerceAtLeast(1) // 최소 1프레임
    }

    /** 공격 속도 쿨다운 (초 단위) */
    fun calculateAttackCooldownSeconds(
        atkSpeed: Float,
        spdBuffMultiplier: Float = 1f,
    ): Float {
        val effectiveSpeed = atkSpeed * spdBuffMultiplier
        if (effectiveSpeed <= 0f) return 1f
        return (1f / effectiveSpeed).coerceAtLeast(1f / 60f) // 최소 1/60초
    }

    /** 행(Row) 기반 사거리 보너스 */
    fun calculateRangeWithRowBonus(
        baseRange: Float,
        gridRow: Int, // 0=후열, 1=중열, 2=전열
    ): Float = when (gridRow) {
        0 -> baseRange * 1.2f   // 후열: +20% 사거리
        2 -> baseRange * 0.85f  // 전열: -15% 사거리 (근접 우선)
        else -> baseRange       // 중열: 기본
    }

    /** 잭팟 머지 확률 체크 (5%) */
    fun isLuckyMerge(): Boolean = (Math.random() * 100) < 5.0

    /** 보스 제한 시간 계산 */
    fun calculateBossTimeLimit(currentWave: Int): Float {
        // Wave 10: 60s, Wave 20: 55s, ... 최소 30s
        val timeLimit = 60f - (currentWave / 10) * 5f
        return timeLimit.coerceAtLeast(30f)
    }

    private val LEVEL_MULTIPLIERS = floatArrayOf(1.0f, 1.5f, 2.2f, 3.2f, 4.5f, 6.0f, 8.0f)
}
