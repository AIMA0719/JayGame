package com.jay.jaygame.engine

import com.jay.jaygame.data.ALL_DUNGEONS
import com.jay.jaygame.data.DUNGEON_DAILY_LIMIT
import com.jay.jaygame.data.GameData
import java.time.LocalDate

class DungeonManager(private var gameData: GameData) {

    fun syncData(data: GameData) { gameData = data }

    private fun todayString(): String = LocalDate.now().toString()

    private fun resetDailyIfNeeded(): GameData {
        val today = todayString()
        return if (gameData.lastDungeonResetDate != today) {
            val refreshed = gameData.copy(dungeonDailyCount = 0, lastDungeonResetDate = today)
            gameData = refreshed
            refreshed
        } else {
            gameData
        }
    }

    /**
     * Returns true if the player can enter the given dungeon:
     * - trophies meet the requirement
     * - stamina is sufficient
     * - daily limit not exceeded
     */
    fun canEnter(dungeonId: Int): Boolean {
        val def = ALL_DUNGEONS.getOrNull(dungeonId) ?: return false
        val refreshed = resetDailyIfNeeded()
        return refreshed.trophies >= def.requiredTrophies &&
            refreshed.stamina >= def.staminaCost &&
            refreshed.dungeonDailyCount < DUNGEON_DAILY_LIMIT
    }

    /**
     * Deducts stamina and increments daily count.
     * Returns null if cannot enter.
     */
    fun enterDungeon(dungeonId: Int): GameData? {
        val def = ALL_DUNGEONS.getOrNull(dungeonId) ?: return null
        val refreshed = resetDailyIfNeeded()
        if (refreshed.trophies < def.requiredTrophies) return null
        if (refreshed.stamina < def.staminaCost) return null
        if (refreshed.dungeonDailyCount >= DUNGEON_DAILY_LIMIT) return null

        return refreshed.copy(
            stamina = refreshed.stamina - def.staminaCost,
            dungeonDailyCount = refreshed.dungeonDailyCount + 1,
        )
    }

    /**
     * Updates best wave record for the dungeon after completion.
     */
    fun completeDungeon(dungeonId: Int, waveReached: Int): GameData {
        val current = gameData.dungeonClears[dungeonId] ?: 0
        val newClears = gameData.dungeonClears.toMutableMap()
        if (waveReached > current) {
            newClears[dungeonId] = waveReached
        }
        return gameData.copy(dungeonClears = newClears)
    }

    /** Remaining daily attempts today */
    fun remainingAttempts(): Int {
        val refreshed = resetDailyIfNeeded()
        return (DUNGEON_DAILY_LIMIT - refreshed.dungeonDailyCount).coerceAtLeast(0)
    }
}
