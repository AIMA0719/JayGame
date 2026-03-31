package com.jay.jaygame.engine

import com.jay.jaygame.data.*

class PetManager(private var gameData: GameData) {

    fun syncData(data: GameData) {
        gameData = data
    }

    // ── Pull ──

    fun pullPet(): GameData? {
        if (gameData.diamonds < PET_PULL_COST) return null
        val pity = gameData.petPullPity
        val grade = rollPetGrade(pity)
        val petId = rollPetId(grade)
        val newPity = if (grade == PetGrade.LEGEND || grade == PetGrade.MYTHIC) 0 else (pity + 1).coerceAtMost(49)
        val data = addPetCards(gameData, petId, 1)
        return data.copy(
            diamonds = data.diamonds - PET_PULL_COST,
            petPullPity = newPity,
        )
    }

    fun pullPet10(): GameData? {
        if (gameData.diamonds < PET_PULL_10_COST) return null
        var data = gameData.copy(diamonds = gameData.diamonds - PET_PULL_10_COST)
        repeat(10) {
            val pity = data.petPullPity
            val grade = rollPetGrade(pity)
            val petId = rollPetId(grade)
            val newPity = if (grade == PetGrade.LEGEND || grade == PetGrade.MYTHIC) 0 else (pity + 1).coerceAtMost(49)
            data = addPetCards(data, petId, 1).copy(petPullPity = newPity)
        }
        return data
    }

    // ── Upgrade ──

    fun upgradePet(petId: Int): GameData? {
        val progress = gameData.pets.getOrNull(petId) ?: return null
        if (!progress.owned) return null
        val def = ALL_PETS.find { it.id == petId } ?: return null
        if (progress.level >= def.grade.maxLevel) return null
        val cardsNeeded = petCardsRequired(progress.level)
        val goldNeeded = petUpgradeCost(progress.level)
        if (progress.cards < cardsNeeded || gameData.gold < goldNeeded) return null
        val pets = gameData.pets.toMutableList()
        pets[petId] = progress.copy(
            cards = progress.cards - cardsNeeded,
            level = progress.level + 1,
        )
        return gameData.copy(
            pets = pets,
            gold = gameData.gold - goldNeeded,
        )
    }

    // ── Equip / Unequip ──

    fun equipPet(petId: Int): GameData? {
        val progress = gameData.pets.getOrNull(petId) ?: return null
        if (!progress.owned) return null
        if (gameData.equippedPets.contains(petId)) return null
        if (gameData.equippedPets.size >= gameData.equippedPetSlotCount) return null
        return gameData.copy(equippedPets = gameData.equippedPets + petId)
    }

    fun unequipPet(petId: Int): GameData {
        return gameData.copy(equippedPets = gameData.equippedPets - petId)
    }

    // ── Private helpers ──

    private fun rollPetGrade(pity: Int): PetGrade {
        // 50 pity guarantees LEGEND or above
        if (pity >= 49) return PetGrade.LEGEND
        val totalWeight = PetGrade.entries.sumOf { it.pullWeight }
        var roll = (0 until totalWeight).random()
        for (grade in PetGrade.entries) {
            roll -= grade.pullWeight
            if (roll < 0) return grade
        }
        return PetGrade.RARE
    }

    private fun rollPetId(grade: PetGrade): Int {
        val candidates = ALL_PETS.filter { it.grade == grade }
        return if (candidates.isEmpty()) ALL_PETS.random().id
        else candidates.random().id
    }

    private fun addPetCards(data: GameData, petId: Int, count: Int): GameData {
        val pets = data.pets.toMutableList()
        val existing = pets.getOrNull(petId) ?: return data
        pets[petId] = if (!existing.owned) {
            existing.copy(owned = true, cards = existing.cards + count)
        } else {
            existing.copy(cards = existing.cards + count)
        }
        return data.copy(pets = pets)
    }
}
