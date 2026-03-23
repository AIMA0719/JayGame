package com.example.jaygame.engine

/**
 * 배틀 중 유닛 개별 강화 시스템 (운빨존많겜 스타일).
 * 유닛을 탭하여 개별적으로 강화. 업그레이드당 기본 ATK의 50% 증가.
 *
 * 비용: 일반~영웅 = 코인, 전설~신화 = 행운석
 * 최대 레벨: 15
 *
 * 마일스톤:
 * - Lv3: ATK +10%
 * - Lv6: 고유 업그레이드 (영웅별 특수 강화)
 * - Lv9: 공격속도 +10%
 * - Lv12: 고유 업그레이드 (영웅별 특수 강화)
 * - Lv15: ATK +10% + 공격속도 +10%
 */
object UnitUpgradeSystem {

    const val MAX_UPGRADE_LEVEL = 15
    /** 업그레이드당 기본 ATK의 50% 증가 */
    const val ATK_PER_LEVEL = 0.50f

    data class UpgradeResult(
        val success: Boolean,
        val newLevel: Int,
        val costPaid: Int,
        val usesLuckyStones: Boolean,
    )

    /** 강화 비용 계산. 등급에 따라 코인 or 행운석. */
    fun getUpgradeCost(grade: Int, upgradeLevel: Int): Int {
        if (upgradeLevel >= MAX_UPGRADE_LEVEL) return -1
        // 기본 비용: 등급별 차등
        val baseCost = when (grade) {
            0 -> 5    // Common
            1 -> 8    // Rare
            2 -> 12   // Hero
            3 -> 18   // Legend (행운석)
            4 -> 25   // Mythic (행운석)
            else -> 10
        }
        return (baseCost * (1f + upgradeLevel * 0.3f)).toInt()
    }

    /** 전설(3) 이상은 행운석 사용 */
    fun usesLuckyStones(grade: Int): Boolean = grade >= 3

    /** 유닛 강화 시도 */
    fun tryUpgrade(unit: GameUnit, availableCoins: Float, availableLuckyStones: Int = Int.MAX_VALUE): UpgradeResult {
        if (unit.upgradeLevel >= MAX_UPGRADE_LEVEL) {
            return UpgradeResult(false, unit.upgradeLevel, 0, false)
        }

        val cost = getUpgradeCost(unit.grade, unit.upgradeLevel)
        val needsStones = usesLuckyStones(unit.grade)

        if (needsStones) {
            if (availableLuckyStones < cost) {
                return UpgradeResult(false, unit.upgradeLevel, 0, true)
            }
        } else {
            if (availableCoins < cost) {
                return UpgradeResult(false, unit.upgradeLevel, 0, false)
            }
        }

        unit.upgradeLevel++
        applyUpgradeBonuses(unit)

        return UpgradeResult(true, unit.upgradeLevel, cost, needsStones)
    }

    /** 주어진 upgradeLevel에 대한 총 ATK 보너스 비율 계산 */
    fun getTotalAtkBonus(upgradeLevel: Int): Float {
        var bonus = upgradeLevel * ATK_PER_LEVEL
        if (upgradeLevel >= 3) bonus += 0.10f
        if (upgradeLevel >= 6) bonus += 0.15f
        if (upgradeLevel >= 12) bonus += 0.15f
        if (upgradeLevel >= 15) bonus += 0.10f
        return bonus
    }

    /** 현재 upgradeLevel에 맞는 총 보너스를 재계산 */
    private fun applyUpgradeBonuses(unit: GameUnit) {
        // 기본: 레벨 × 50% ATK
        var atkBonus = unit.upgradeLevel * ATK_PER_LEVEL
        var spdBonus = 0f

        // 마일스톤 보너스
        if (unit.upgradeLevel >= 3) atkBonus += 0.10f     // Lv3: ATK +10%
        if (unit.upgradeLevel >= 9) spdBonus += 0.10f     // Lv9: 공격속도 +10%
        if (unit.upgradeLevel >= 15) {                     // Lv15: ATK+속도 +10%
            atkBonus += 0.10f
            spdBonus += 0.10f
        }
        // Lv6, Lv12: 고유 업그레이드 (TODO: 영웅별 특수 강화)
        // 현재는 ATK +15% 일괄 적용
        if (unit.upgradeLevel >= 6) atkBonus += 0.15f
        if (unit.upgradeLevel >= 12) atkBonus += 0.15f

        unit.upgradeBonusATK = atkBonus
        unit.upgradeBonusSpd = spdBonus
    }
}
