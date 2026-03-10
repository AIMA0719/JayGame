package com.example.jaygame.data

fun addRandomCardsToUnits(units: List<UnitProgress>, count: Int): List<UnitProgress> {
    val updatedUnits = units.toMutableList()
    val ownedIndices = updatedUnits.indices.filter { updatedUnits[it].owned }
    if (ownedIndices.isEmpty()) return units
    repeat(count) {
        val idx = ownedIndices.random()
        val unit = updatedUnits[idx]
        updatedUnits[idx] = unit.copy(cards = unit.cards + 1)
    }
    return updatedUnits
}
