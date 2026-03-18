package com.example.jaygame.engine

import androidx.compose.ui.graphics.Color

/**
 * 유닛 등급 체계 (7단계)
 * 일반 → 희귀 → 영웅 → 전설 → 고대 → 신화 → 불멸
 * 동일 등급/종류 3개 → 상위 등급 1개로 조합(Merge)
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
    ANCIENT("고대", Color(0xFFef4444), 5, 0),
    MYTHIC("신화", Color(0xFFfbbf24), 6, 0),
    IMMORTAL("불멸", Color(0xFFf0abfc), 7, 0);

    @Deprecated("Use UnitBlueprint.summonWeight instead")
    val canSummon: Boolean get() = summonWeight > 0

    companion object {
        fun fromTier(tier: Int): UnitGrade? = entries.find { it.tier == tier }

        /** 다음 등급 반환. 불멸이면 null */
        fun UnitGrade.nextGrade(): UnitGrade? = fromTier(tier + 1)
    }
}
