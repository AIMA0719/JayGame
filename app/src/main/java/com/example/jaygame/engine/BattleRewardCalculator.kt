package com.example.jaygame.engine

import com.example.jaygame.data.ALL_DUNGEONS
import com.example.jaygame.data.DungeonType
import com.example.jaygame.data.GameData
import com.example.jaygame.data.RelicGrade
import com.example.jaygame.data.addRandomCardsToUnits
import com.example.jaygame.bridge.BattleResultData
import com.example.jaygame.bridge.BattleBridge

object BattleRewardCalculator {

    fun calculateRewards(
        current: GameData,
        battleResult: BattleResultData,
        stageId: Int,
        isDungeon: Boolean,
        dungeonId: Int,
        engine: BattleEngine?,
    ): GameData {
        val dungeonDef = if (dungeonId >= 0) ALL_DUNGEONS.getOrNull(dungeonId) else null

        // Apply dungeon reward multiplier to gold
        val goldMultiplier = dungeonDef?.rewardMultiplier ?: 1f
        val finalGold = (battleResult.goldEarned * goldMultiplier).toInt()

        val bestWaves = if (!isDungeon) {
            val list = current.stageBestWaves.toMutableList()
            while (list.size <= stageId) list.add(0)
            if (battleResult.waveReached > list[stageId]) {
                list[stageId] = battleResult.waveReached
            }
            list
        } else current.stageBestWaves

        val dungeonClears = if (isDungeon && dungeonId >= 0) {
            val map = current.dungeonClears.toMutableMap()
            val prev = map[dungeonId] ?: 0
            if (battleResult.waveReached > prev) {
                map[dungeonId] = battleResult.waveReached
            }
            map
        } else current.dungeonClears

        // Dungeon-specific bonus rewards
        var petCardBonus = 0
        if (isDungeon && dungeonDef != null) {
            when (dungeonDef.type) {
                DungeonType.PET_EXPEDITION -> {
                    petCardBonus = battleResult.waveReached / 4
                }
                else -> { /* other bonuses handled by rewardMultiplier */ }
            }
        }

        // Star rating bonus
        val starCount = if (!battleResult.victory) 0 else {
            var s = 1
            if (battleResult.noHpLost || battleResult.fastClear) s++
            if (battleResult.noHpLost && battleResult.fastClear) s++
            s
        }
        val starGoldBonus = when (starCount) {
            3 -> 0.5f
            2 -> 0.25f
            else -> 0f
        }
        val starBonusGold = (finalGold * starGoldBonus).toInt()

        // Cards earned
        val updatedUnits = addRandomCardsToUnits(current.units, battleResult.cardsEarned)

        // XP from battle
        val xpGained = battleResult.waveReached * 10 + if (battleResult.victory) 50 else 0
        val newTotalXP = current.totalXP + xpGained
        val newPlayerLevel = (newTotalXP / 100) + 1

        // Season XP
        val dungeonSeasonBonus = if (isDungeon && battleResult.victory) 50 else 0
        val seasonXpGained = battleResult.waveReached * 5 + (if (battleResult.victory) 30 else 0) + dungeonSeasonBonus
        val newSeasonXP = current.seasonXP + seasonXpGained

        // Single-type win detection
        val singleTypeWin = engine?.let { eng ->
            val families = mutableSetOf<Int>()
            eng.units.forEach { u -> if (u.alive) families.add(u.familyOrdinal) }
            families.size == 1 && families.firstOrNull()?.let { it >= 0 } == true
        } ?: false

        // Apply relic drop
        val afterRelicData = if (battleResult.relicDropId >= 0 && battleResult.relicDropGrade >= 0) {
            val grade = RelicGrade.entries.getOrNull(battleResult.relicDropGrade)
            if (grade != null) {
                RelicManager(current).acquireRelic(battleResult.relicDropId, grade)
            } else current
        } else current

        // Pity counter
        val finalPity = engine?.currentPity ?: BattleBridge.unitPullPity.value

        // Pet card bonus for PET_EXPEDITION dungeon
        val finalPets = if (petCardBonus > 0) {
            val ownedCount = afterRelicData.pets.count { it.owned }.coerceAtLeast(1)
            val perPet = petCardBonus / ownedCount
            val remainder = petCardBonus % ownedCount
            var given = 0
            afterRelicData.pets.mapIndexed { _, pet ->
                if (pet.owned) {
                    val extra = if (given < remainder) 1 else 0
                    given++
                    pet.copy(cards = pet.cards + perPet + extra)
                } else pet
            }
        } else afterRelicData.pets

        // 조합석 — 배틀 중 소모된 조합석 반영
        val finalLuckyStones = engine?.luckyStones ?: current.luckyStones

        return afterRelicData.copy(
            gold = afterRelicData.gold + finalGold + starBonusGold,
            trophies = if (isDungeon) afterRelicData.trophies else (afterRelicData.trophies + battleResult.trophyChange).coerceAtLeast(0),
            totalKills = afterRelicData.totalKills + battleResult.killCount,
            totalMerges = afterRelicData.totalMerges + battleResult.mergeCount,
            totalGoldEarned = afterRelicData.totalGoldEarned + finalGold + starBonusGold,
            totalWins = afterRelicData.totalWins + if (battleResult.victory) 1 else 0,
            totalLosses = afterRelicData.totalLosses + if (!battleResult.victory) 1 else 0,
            highestWave = maxOf(afterRelicData.highestWave, battleResult.waveReached),
            wonWithoutDamage = afterRelicData.wonWithoutDamage || battleResult.noHpLost,
            wonWithSingleType = afterRelicData.wonWithSingleType || singleTypeWin,
            stageBestWaves = bestWaves,
            units = updatedUnits,
            totalXP = newTotalXP,
            playerLevel = newPlayerLevel,
            seasonXP = newSeasonXP,
            unitPullPity = finalPity,
            dungeonClears = dungeonClears,
            pets = finalPets,
            tutorialCompleted = true,
            luckyStones = finalLuckyStones,
        )
    }
}
