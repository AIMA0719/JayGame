package com.jay.jaygame.engine

import com.jay.jaygame.data.DungeonDef
import com.jay.jaygame.data.DungeonType

internal data class BattleRewardSummary(
    val goldEarned: Int,
    val trophyChange: Int,
    val noHpLost: Boolean,
    val fastClear: Boolean,
    val cardsEarned: Int,
    val relicDropId: Int,
    val relicDropGrade: Int,
)

internal object BattleOutcomeSummaryCalculator {
    fun calculate(
        victory: Boolean,
        stageId: Int,
        difficulty: Int,
        currentWave: Int,
        elapsedTime: Float,
        maxWaves: Int,
        peakEnemyCount: Int,
        defeatEnemyCount: Int,
        isDungeonMode: Boolean,
        dungeonDef: DungeonDef?,
        relicManager: RelicManager?,
    ): BattleRewardSummary {
        val difficultyBonus = if (isDungeonMode && dungeonDef != null) {
            dungeonDef.difficultyMultiplier
        } else {
            when (difficulty) {
                1 -> 1.5f
                2 -> 2.5f
                else -> 1f
            }
        }
        val dungeonRewardMult = if (isDungeonMode) dungeonDef?.rewardMultiplier ?: 1f else 1f
        val baseGold = if (victory) 100 + currentWave * 10 else currentWave * 5
        val relicWaveBonus = if (victory) 1f + (relicManager?.totalGoldWaveBonus() ?: 0f) else 1f
        val goldEarned = (baseGold * difficultyBonus * relicWaveBonus * dungeonRewardMult).toInt().coerceAtLeast(1)
        val baseTrophy = if (victory) 20 + stageId * 5 else -(10 + stageId * 3)
        val trophyChange = if (baseTrophy > 0) (baseTrophy * difficultyBonus).toInt() else baseTrophy
        val noHpLost = peakEnemyCount <= defeatEnemyCount / 5
        val fastClear = victory && elapsedTime < maxWaves * 8f
        val baseCards = if (victory) 3 + stageId + difficulty * 2 else 1
        val dungeonCardBonus = if (isDungeonMode && victory) currentWave / 5 else 0
        val cardsEarned = baseCards + dungeonCardBonus
        val relicDrop = if (victory) {
            val isRelicHunt = isDungeonMode && dungeonDef?.type == DungeonType.RELIC_HUNT
            if (isRelicHunt) relicManager?.rollRelicDropBoosted(0.50) else relicManager?.rollRelicDrop()
        } else {
            null
        }
        return BattleRewardSummary(
            goldEarned = goldEarned,
            trophyChange = trophyChange,
            noHpLost = noHpLost,
            fastClear = fastClear,
            cardsEarned = cardsEarned,
            relicDropId = relicDrop?.first ?: -1,
            relicDropGrade = relicDrop?.second?.ordinal ?: -1,
        )
    }
}
