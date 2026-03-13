package com.example.jaygame.engine

import kotlin.random.Random

/**
 * 가챠 & 확률 엔진.
 * 미네랄(재화)을 소모하여 유닛을 랜덤 소환.
 * 등급별 확률 테이블 적용.
 */
interface ProbabilityEngine {
    /** 등급별 소환 확률 테이블 (합 = 100) */
    fun getGradeWeights(): Map<UnitGrade, Int>

    /** 소환 가능한 유닛 중 랜덤으로 1개 선택 */
    fun rollUnit(availableUnits: List<UnitSpec>): UnitSpec

    /** 특정 등급이 나올 확률 (0.0 ~ 1.0) */
    fun getProbability(grade: UnitGrade): Float
}

class DefaultProbabilityEngine(
    private val random: Random = Random.Default,
) : ProbabilityEngine {

    override fun getGradeWeights(): Map<UnitGrade, Int> = mapOf(
        UnitGrade.COMMON to 60,
        UnitGrade.RARE to 25,
        UnitGrade.HERO to 12,
        UnitGrade.LEGEND to 3,
        // ANCIENT, MYTHIC, IMMORTAL은 조합으로만 획득 — 소환 불가
    )

    override fun rollUnit(availableUnits: List<UnitSpec>): UnitSpec {
        // 1단계: 등급 결정
        val grade = rollGrade()
        // 2단계: 해당 등급 유닛 중 랜덤 선택
        val candidates = availableUnits.filter { it.grade == grade }
        if (candidates.isEmpty()) {
            // 해당 등급 유닛이 없으면 하급에서 선택
            val fallback = availableUnits.filter { it.grade == UnitGrade.COMMON }
            return fallback.random(random)
        }
        return candidates.random(random)
    }

    override fun getProbability(grade: UnitGrade): Float {
        val weights = getGradeWeights()
        val total = weights.values.sum().toFloat()
        return (weights[grade] ?: 0) / total
    }

    private fun rollGrade(): UnitGrade {
        val weights = getGradeWeights()
        val totalWeight = weights.values.sum()
        var roll = random.nextInt(totalWeight)

        for ((grade, weight) in weights) {
            roll -= weight
            if (roll < 0) return grade
        }
        return UnitGrade.COMMON
    }

    /**
     * 천장(Pity)을 고려한 등급 롤.
     * @param pity 현재 누적 소환 횟수
     * @return Pair(gradeOrdinal, shouldResetPity)
     *   - gradeOrdinal: 0=COMMON, 1=RARE, 2=HERO, 3=LEGEND
     *   - shouldResetPity: HERO 이상이 나왔을 때 true (천장 초기화)
     */
    fun rollGradeWithPity(pity: Int): Pair<Int, Boolean> {
        // 하드 천장: 100회 소환 시 무조건 LEGEND
        if (pity >= 100) return 3 to true

        // 소프트 천장: 30회 이후 HERO 확률 2배
        val heroBoost = if (pity >= 30) 2f else 1f

        val r = random.nextFloat() * 100f
        return when {
            r < 60f -> 0 to false                        // COMMON 60%
            r < 85f -> 1 to false                        // RARE 25%
            r < 85f + 12f * heroBoost -> {               // HERO (부스트 후 최대 24%)
                val reset = pity >= 30
                2 to reset
            }
            else -> 3 to true                            // LEGEND
        }
    }
}
