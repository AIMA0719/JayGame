package com.jay.jaygame.data

import android.util.Log

object StaminaManager {
    private const val REGEN_INTERVAL_MS = 5 * 60 * 1000L // 5분

    fun refreshStamina(data: GameData): GameData {
        val now = System.currentTimeMillis()
        if (TimeGuard.isTimeManipulated(data.lastKnownSystemTime)) {
            Log.w("StaminaManager", "Time manipulation detected, skipping regen")
            return data.copy(lastKnownSystemTime = now)
        }
        if (data.stamina >= data.maxStamina) {
            return data.copy(
                lastStaminaRegenTime = now,
                lastKnownSystemTime = now,
            )
        }
        val elapsed = now - data.lastStaminaRegenTime
        val regenCount = (elapsed / REGEN_INTERVAL_MS).coerceAtMost(data.maxStamina.toLong()).toInt()
        if (regenCount <= 0) return data.copy(lastKnownSystemTime = now)
        val newStamina = (data.stamina + regenCount).coerceAtMost(data.maxStamina)
        return data.copy(
            stamina = newStamina,
            lastStaminaRegenTime = data.lastStaminaRegenTime + regenCount.toLong() * REGEN_INTERVAL_MS,
            lastKnownSystemTime = now,
        )
    }

    fun msUntilNextRegen(data: GameData): Long {
        if (data.stamina >= data.maxStamina) return 0L
        val now = System.currentTimeMillis()
        val elapsed = now - data.lastStaminaRegenTime
        return REGEN_INTERVAL_MS - (elapsed % REGEN_INTERVAL_MS)
    }

    fun consume(data: GameData, amount: Int): GameData? {
        val refreshed = refreshStamina(data)
        if (refreshed.stamina < amount) return null
        return refreshed.copy(stamina = refreshed.stamina - amount)
    }
}
