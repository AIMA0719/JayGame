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
}
