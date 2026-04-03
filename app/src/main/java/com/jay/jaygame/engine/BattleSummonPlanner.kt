package com.jay.jaygame.engine

internal object BattleSummonPlanner {
    fun rollWeightedBlueprint(candidates: List<UnitBlueprint>): UnitBlueprint? {
        if (candidates.isEmpty()) return null
        val totalWeight = candidates.sumOf { it.summonWeight }
        if (totalWeight <= 0) return null
        var roll = (Math.random() * totalWeight).toInt()
        for (candidate in candidates) {
            roll -= candidate.summonWeight
            if (roll < 0) return candidate
        }
        return null
    }

    fun maybeUpgradeSummon(
        selected: UnitBlueprint,
        selectedRaces: Set<com.jay.jaygame.data.UnitRace>,
        blueprintRegistry: BlueprintRegistry,
        summonUpgradeEnabled: Boolean,
    ): UnitBlueprint {
        if (!summonUpgradeEnabled || Math.random() >= 0.07 || selected.grade >= UnitGrade.LEGEND) {
            return selected
        }
        val nextGrade = UnitGrade.entries.getOrNull(selected.grade.ordinal + 1) ?: selected.grade
        val upgradeCandidates = blueprintRegistry.findByRacesAndGradeAndSummonable(selectedRaces, nextGrade)
        return if (upgradeCandidates.isNotEmpty()) upgradeCandidates.random() else selected
    }
}
