package com.example.jaygame.engine

import androidx.compose.ui.graphics.Color

/**
 * 유닛 등급 체계 (메이플 랜덤 디펜스 기반)
 * 하급 → 중급 → 상급 → 최상급 → 초월
 * 동일 등급/종류 3개 → 상위 등급 1개로 조합(Merge)
 */
enum class UnitGrade(
    val label: String,
    val color: Color,
    val tier: Int,
    val summonWeight: Int, // 가챠 확률 가중치 (하급이 가장 높음)
) {
    LOW("하급", Color(0xFF9ca3af), 1, 80),
    MEDIUM("중급", Color(0xFF60a5fa), 2, 15),
    HIGH("상급", Color(0xFFc084fc), 3, 5),
    SUPREME("최상급", Color(0xFFfb923c), 4, 0),  // 조합으로만 획득
    TRANSCENDENT("초월", Color(0xFFf472b6), 5, 0); // 조합으로만 획득

    val canSummon: Boolean get() = summonWeight > 0

    companion object {
        fun fromTier(tier: Int): UnitGrade? = entries.find { it.tier == tier }

        /** 다음 등급 반환. 초월이면 null */
        fun UnitGrade.nextGrade(): UnitGrade? = fromTier(tier + 1)
    }
}
