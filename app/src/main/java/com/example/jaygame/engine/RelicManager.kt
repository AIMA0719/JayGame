package com.example.jaygame.engine

import com.example.jaygame.data.*

class RelicManager(private var gameData: GameData) {

    fun syncData(data: GameData) { gameData = data }

    /** 유물 획득 */
    fun acquireRelic(relicId: Int, grade: RelicGrade): GameData {
        val relics = gameData.relics.toMutableList()
        val existing = relics[relicId]
        if (existing.owned) {
            if (grade.ordinal > existing.grade) {
                relics[relicId] = existing.copy(grade = grade.ordinal, level = 1)
            } else {
                return gameData.copy(
                    relics = relics,
                    gold = gameData.gold + (grade.ordinal + 1) * 100
                )
            }
        } else {
            relics[relicId] = RelicProgress(
                relicId = relicId, grade = grade.ordinal, level = 1, owned = true,
            )
        }
        return gameData.copy(relics = relics)
    }

    /** 유물 강화 */
    fun upgradeRelic(relicId: Int): GameData? {
        val relic = gameData.relics[relicId]
        if (!relic.owned) return null
        val gradeDef = RelicGrade.entries[relic.grade]
        if (relic.level >= gradeDef.maxLevel) return null
        val cost = relicUpgradeCost(relic.level)
        if (gameData.gold < cost) return null
        val relics = gameData.relics.toMutableList()
        relics[relicId] = relic.copy(level = relic.level + 1)
        return gameData.copy(relics = relics, gold = gameData.gold - cost)
    }

    /** 장착 */
    fun equipRelic(relicId: Int): GameData? {
        val relic = gameData.relics[relicId]
        if (!relic.owned) return null
        if (gameData.equippedRelics.contains(relicId)) return null
        if (gameData.equippedRelics.size >= gameData.equippedSlotCount) return null
        return gameData.copy(equippedRelics = gameData.equippedRelics + relicId)
    }

    /** 장착 해제 */
    fun unequipRelic(relicId: Int): GameData {
        return gameData.copy(equippedRelics = gameData.equippedRelics - relicId)
    }

    /** 장착 유물 효과 (lv * effectPerLevel, capped) */
    fun getEquippedEffect(relicId: Int): Float {
        val relic = gameData.relics[relicId]
        if (!relic.owned || !gameData.equippedRelics.contains(relicId)) return 0f
        val def = ALL_RELICS[relicId]
        return (relic.level * def.effectPerLevel).coerceAtMost(def.maxEffectCap)
    }

    // === 전투 보너스 접근자 (percent as 0.0~1.0) ===
    fun totalAtkPercent(): Float = getEquippedEffect(3) / 100f
    fun totalAtkSpeedPercent(): Float = getEquippedEffect(4) / 100f
    fun totalCritChanceBonus(): Float = getEquippedEffect(5) / 100f
    fun totalCritDamageBonus(): Float {
        val relic = gameData.relics[5]
        if (!relic.owned || !gameData.equippedRelics.contains(5)) return 0f
        return (relic.level * 10f).coerceAtMost(200f) / 100f
    }
    fun totalArmorPenPercent(): Float = getEquippedEffect(6) / 100f
    fun totalMagicDmgPercent(): Float = getEquippedEffect(7) / 100f
    fun totalSummonCostReduction(): Float = getEquippedEffect(8) / 100f
    fun totalLuckyMergeBonus(): Float = getEquippedEffect(9) / 100f
    fun totalCooldownReduction(): Float = getEquippedEffect(10) / 100f
    fun totalWaveStartSp(): Float = getEquippedEffect(11)
    fun totalGoldWaveBonus(): Float = getEquippedEffect(0) / 100f
    fun totalGoldKillBonus(): Float = getEquippedEffect(1) / 100f
    fun totalGambleBonus(): Float = getEquippedEffect(2) / 100f

    /** 전투 보상 랜덤 유물 드롭 (10% 확률) */
    fun rollRelicDrop(): Pair<Int, RelicGrade>? {
        if (Math.random() > 0.10) return null
        val relicId = (ALL_RELICS.indices).random()
        val def = ALL_RELICS[relicId]
        val grade = rollGrade(def.minGrade)
        return relicId to grade
    }

    /** Boosted relic drop for dungeon RELIC_HUNT mode */
    fun rollRelicDropBoosted(chance: Double): Pair<Int, RelicGrade>? {
        if (Math.random() > chance) return null
        val relicId = (ALL_RELICS.indices).random()
        val def = ALL_RELICS[relicId]
        val grade = rollGrade(def.minGrade)
        return relicId to grade
    }

    private fun rollGrade(minGrade: RelicGrade): RelicGrade {
        val eligible = RelicGrade.entries.filter { it.ordinal >= minGrade.ordinal }
        val totalWeight = eligible.sumOf { it.dropWeight }
        var roll = (0 until totalWeight).random()
        for (g in eligible) {
            roll -= g.dropWeight
            if (roll < 0) return g
        }
        return eligible.last()
    }
}
