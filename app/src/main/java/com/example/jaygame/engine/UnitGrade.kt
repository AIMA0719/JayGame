package com.example.jaygame.engine

import androidx.compose.ui.graphics.Color

/**
 * 유닛 등급 체계 (5단계)
 * 일반 → 희귀 → 영웅 → 전설 → 신화
 * 동일 유닛 3개 → 상위 등급 1개로 조합(Merge)
 * 신화는 레시피 전용
 */
enum class UnitGrade(
    val label: String,
    val color: Color,
    val tier: Int,
    @Deprecated("Use UnitBlueprint.summonWeight instead")
    val summonWeight: Int,
) {
    COMMON("일반", Color(0xFF9ca3af), 1, 60),
    RARE("희귀", Color(0xFF60a5fa), 2, 25),
    HERO("영웅", Color(0xFFc084fc), 3, 12),
    LEGEND("전설", Color(0xFFfb923c), 4, 3),
    MYTHIC("신화", Color(0xFFfbbf24), 5, 0);

    @Deprecated("Use UnitBlueprint.summonWeight instead")
    val canSummon: Boolean get() = summonWeight > 0

    companion object {
        fun fromTier(tier: Int): UnitGrade? = entries.find { it.tier == tier }

        /** 다음 등급 반환. 신화면 null */
        fun UnitGrade.nextGrade(): UnitGrade? = fromTier(tier + 1)
    }
}
