package com.example.jaygame.engine

/**
 * 덱 시너지 — 같은 가족(Family)을 덱에 2개 이상 넣으면 해당 가족 유닛에 보너스.
 *
 * 2개: 기본 시너지 (약한 보너스)
 * 3개: 풀 시너지 (강한 보너스, 모든 유닛에 적용)
 */
object SynergySystem {

    data class SynergyBonus(
        val atkMultiplier: Float = 1f,
        val spdMultiplier: Float = 1f,
        val rangeMultiplier: Float = 1f,
        val specialEffect: SpecialEffect = SpecialEffect.NONE,
    )

    enum class SpecialEffect {
        NONE,
        FIRE_BURN_EXTEND,      // 화염: DoT 지속시간 +50%
        FROST_SLOW_BOOST,      // 냉기: 둔화 효과 +30%
        POISON_SPREAD,         // 독: 적 사망 시 주변 독 전파
        LIGHTNING_CHAIN_EXTRA, // 번개: 체인 +1 타겟
        SUPPORT_HEAL_BOOST,    // 보조: 힐/버프 효과 +25%
        WIND_KNOCKBACK_EXTRA,  // 바람: 넉백 거리 +40%
    }

    /**
     * 덱에서 시너지 보너스 계산.
     * @param deck 가족 ordinal 리스트 (길이 3)
     * @param unitFamily 보너스를 받을 유닛의 가족 ordinal
     */
    fun getSynergyBonus(deck: IntArray, unitFamily: Int): SynergyBonus {
        val counts = IntArray(6)
        for (family in deck) {
            if (family in 0..5) counts[family]++
        }

        val familyCount = counts.getOrElse(unitFamily) { 0 }

        // 매칭 안 됨: 기본
        if (familyCount < 2) return SynergyBonus()

        val isFull = familyCount >= 3

        // 가족별 시너지 보너스 (2개: 3~5%, 3개: 6~8% + 특수 효과)
        return when (unitFamily) {
            0 -> SynergyBonus( // 화염: 공격력 증가
                atkMultiplier = if (isFull) 1.08f else 1.04f,
                specialEffect = if (isFull) SpecialEffect.FIRE_BURN_EXTEND else SpecialEffect.NONE,
            )
            1 -> SynergyBonus( // 냉기: 공속 + 둔화 강화
                spdMultiplier = if (isFull) 1.06f else 1.03f,
                specialEffect = if (isFull) SpecialEffect.FROST_SLOW_BOOST else SpecialEffect.NONE,
            )
            2 -> SynergyBonus( // 독: 공격력 + 독 전파
                atkMultiplier = if (isFull) 1.07f else 1.04f,
                specialEffect = if (isFull) SpecialEffect.POISON_SPREAD else SpecialEffect.NONE,
            )
            3 -> SynergyBonus( // 번개: 공속 + 체인 추가
                spdMultiplier = if (isFull) 1.08f else 1.04f,
                specialEffect = if (isFull) SpecialEffect.LIGHTNING_CHAIN_EXTRA else SpecialEffect.NONE,
            )
            4 -> SynergyBonus( // 보조: 사거리 + 힐 강화
                rangeMultiplier = if (isFull) 1.06f else 1.03f,
                specialEffect = if (isFull) SpecialEffect.SUPPORT_HEAL_BOOST else SpecialEffect.NONE,
            )
            5 -> SynergyBonus( // 바람: 공격력 + 넉백 강화
                atkMultiplier = if (isFull) 1.05f else 1.03f,
                rangeMultiplier = if (isFull) 1.05f else 1.02f,
                specialEffect = if (isFull) SpecialEffect.WIND_KNOCKBACK_EXTRA else SpecialEffect.NONE,
            )
            else -> SynergyBonus()
        }
    }

    /**
     * 전체 덱 시너지 요약 (UI 표시용)
     */
    fun getActiveSynergies(deck: IntArray): Map<Int, Int> {
        val counts = IntArray(6)
        for (family in deck) {
            if (family in 0..5) counts[family]++
        }
        return counts.mapIndexed { idx, count -> idx to count }
            .filter { it.second >= 2 }
            .toMap()
    }
}
