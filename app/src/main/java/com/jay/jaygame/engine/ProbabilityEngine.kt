package com.jay.jaygame.engine

import kotlin.random.Random

interface ProbabilityEngine {
    fun getGradeWeights(): Map<UnitGrade, Int>
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
    )

    override fun getProbability(grade: UnitGrade): Float {
        val weights = getGradeWeights()
        val total = weights.values.sum().toFloat()
        return (weights[grade] ?: 0) / total
    }

    fun rollGrade(): UnitGrade {
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
