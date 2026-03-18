package com.example.jaygame.engine

import com.example.jaygame.data.UnitFamily

/**
 * 필드 시너지 — 같은 가족(Family) 유닛이 필드에 2개 이상이면 해당 가족에 보너스.
 *
 * 2개: 기본 시너지 (약한 보너스)
 * 3개+: 풀 시너지 (강한 보너스 + 특수 효과)
 *
 * 듀얼 패밀리 지원: 히든 유닛 등 families가 2개인 유닛은 양쪽 모두 카운트.
 * SPECIAL 유닛은 시너지 카운트에서 제외.
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

    // ── GameUnit-based methods ──

    /** Reusable map for countFamilies to avoid per-call allocation */
    private val cachedCounts = mutableMapOf<UnitFamily, Int>()

    /**
     * 활성 유닛 리스트에서 가족별 카운트를 계산.
     * - 듀얼 패밀리 유닛은 양쪽 모두 카운트
     * - SPECIAL 카테고리 유닛은 제외
     * - 죽은 유닛은 제외
     *
     * NOTE: Returns a reused internal map — do not store the reference across calls.
     */
    fun countFamilies(units: List<GameUnit>): Map<UnitFamily, Int> {
        cachedCounts.clear()
        for (unit in units) {
            if (!unit.alive || unit.unitCategory == UnitCategory.SPECIAL) continue
            for (family in unit.families) {
                cachedCounts[family] = (cachedCounts[family] ?: 0) + 1
            }
        }
        return cachedCounts
    }

    /**
     * GameUnit 리스트 기반 시너지 보너스 조회.
     * countFamilies()로 카운트 후 기존 보너스 테이블 활용.
     */
    fun getSynergyBonus(units: List<GameUnit>, family: UnitFamily): SynergyBonus {
        val counts = countFamilies(units)
        val familyCount = counts[family] ?: 0
        return getSynergyBonusByCount(family.ordinal, familyCount)
    }

    /**
     * GameUnit 리스트 기반 활성 시너지 요약 (UI 표시용)
     */
    fun getActiveSynergies(units: List<GameUnit>): Map<UnitFamily, Int> {
        return countFamilies(units).filter { it.value >= 2 }
    }

    private fun getSynergyBonusByCount(familyOrdinal: Int, count: Int): SynergyBonus {
        if (count < 2) return SynergyBonus()
        val isFull = count >= 3
        return when (familyOrdinal) {
            0 -> SynergyBonus(
                atkMultiplier = if (isFull) 1.08f else 1.04f,
                specialEffect = if (isFull) SpecialEffect.FIRE_BURN_EXTEND else SpecialEffect.NONE,
            )
            1 -> SynergyBonus(
                spdMultiplier = if (isFull) 1.06f else 1.03f,
                specialEffect = if (isFull) SpecialEffect.FROST_SLOW_BOOST else SpecialEffect.NONE,
            )
            2 -> SynergyBonus(
                atkMultiplier = if (isFull) 1.07f else 1.04f,
                specialEffect = if (isFull) SpecialEffect.POISON_SPREAD else SpecialEffect.NONE,
            )
            3 -> SynergyBonus(
                spdMultiplier = if (isFull) 1.08f else 1.04f,
                specialEffect = if (isFull) SpecialEffect.LIGHTNING_CHAIN_EXTRA else SpecialEffect.NONE,
            )
            4 -> SynergyBonus(
                rangeMultiplier = if (isFull) 1.06f else 1.03f,
                specialEffect = if (isFull) SpecialEffect.SUPPORT_HEAL_BOOST else SpecialEffect.NONE,
            )
            5 -> SynergyBonus(
                atkMultiplier = if (isFull) 1.05f else 1.03f,
                rangeMultiplier = if (isFull) 1.05f else 1.02f,
                specialEffect = if (isFull) SpecialEffect.WIND_KNOCKBACK_EXTRA else SpecialEffect.NONE,
            )
            else -> SynergyBonus()
        }
    }
}
