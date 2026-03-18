package com.example.jaygame.data

import kotlin.random.Random

/**
 * Gacha grade weights for card pulls.
 * Total = 10000 (basis points for precision).
 */
private val CARD_GRADE_WEIGHTS = listOf(
    UnitGrade.COMMON   to 5900,
    UnitGrade.RARE     to 2500,
    UnitGrade.HERO     to 1200,
    UnitGrade.LEGEND   to  300,
    UnitGrade.ANCIENT  to   80,
    UnitGrade.MYTHIC   to   15,
    UnitGrade.IMMORTAL to    5,
)

private val CARD_GRADE_TOTAL = CARD_GRADE_WEIGHTS.sumOf { it.second }

private fun rollCardGrade(): UnitGrade {
    var roll = Random.nextInt(CARD_GRADE_TOTAL)
    for ((grade, weight) in CARD_GRADE_WEIGHTS) {
        roll -= weight
        if (roll < 0) return grade
    }
    return UnitGrade.COMMON
}

@Suppress("DEPRECATION")
fun addRandomCardsToUnits(units: Map<String, UnitProgress>, count: Int): Map<String, UnitProgress> {
    if (count <= 0) return units
    val updatedUnits = units.toMutableMap()

    repeat(count) {
        val grade = rollCardGrade()
        val candidates = UNIT_DEFS.filter { it.grade == grade }
        if (candidates.isEmpty()) return@repeat
        val picked = candidates.random()
        val blueprintId = LegacyMigration.blueprintIdForLegacyIndex(picked.id) ?: return@repeat

        val current = updatedUnits[blueprintId] ?: UnitProgress()
        updatedUnits[blueprintId] = if (current.owned) {
            current.copy(cards = current.cards + 1)
        } else {
            current.copy(owned = true, cards = current.cards + 1)
        }
    }
    return updatedUnits
}
