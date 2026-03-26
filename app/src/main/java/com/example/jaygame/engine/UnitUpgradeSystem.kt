package com.example.jaygame.engine

import kotlin.math.pow

/**
 * 배틀 중 등급 그룹 통합 강화 시스템.
 * 같은 그룹의 모든 유닛에 동일 강화 보너스 적용.
 *
 * 4개 그룹:
 * - 그룹 0: [일반/희귀] (grade 0, 1)
 * - 그룹 1: [영웅/전설] (grade 2, 3)
 * - 그룹 2: [신화]       (grade 4)
 * - 그룹 3: [불멸]       (grade 5)
 *
 * 업그레이드당 기본 ATK의 50% 증가. 최대 레벨 15.
 */
object UnitUpgradeSystem {

    const val MAX_UPGRADE_LEVEL = 15
    const val ATK_PER_LEVEL = 0.10f
    const val GROUP_COUNT = 4

    private val BASE_COSTS = intArrayOf(15, 35, 70, 100)
    private val COST_GROWTH = floatArrayOf(1.25f, 1.28f, 1.30f, 1.35f)

    fun gradeToGroup(grade: Int): Int = when (grade) {
        0, 1 -> 0  // 일반/희귀
        2, 3 -> 1  // 영웅/전설
        4    -> 2  // 신화
        5    -> 3  // 불멸
        else -> 0
    }

    fun getGroupUpgradeCost(group: Int, currentLevel: Int): Int {
        if (currentLevel >= MAX_UPGRADE_LEVEL) return -1
        val base = BASE_COSTS.getOrElse(group) { 10 }
        val growth = COST_GROWTH.getOrElse(group) { 1.18f }
        return (base * growth.toDouble().pow(currentLevel.toDouble())).toInt()
    }

    fun getTotalAtkBonus(upgradeLevel: Int): Float {
        var bonus = upgradeLevel * ATK_PER_LEVEL
        if (upgradeLevel >= 5) bonus += 0.05f
        if (upgradeLevel >= 10) bonus += 0.05f
        if (upgradeLevel >= 15) bonus += 0.10f
        return bonus
    }

    fun getTotalSpdBonus(upgradeLevel: Int): Float {
        var bonus = 0f
        if (upgradeLevel >= 10) bonus += 0.05f
        if (upgradeLevel >= 15) bonus += 0.05f
        return bonus
    }

    fun nextMilestoneHint(currentLevel: Int): String = when {
        currentLevel < 5 -> "Lv.5: ATK +5%"
        currentLevel < 10 -> "Lv.10: ATK +5%, 속도 +5%"
        currentLevel < 15 -> "Lv.15: ATK +10%, 속도 +5%"
        else -> "MAX"
    }
}
