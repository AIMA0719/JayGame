package com.example.jaygame.data

object StaminaManager {
    private const val REGEN_INTERVAL_MS = 5 * 60 * 1000L // 5분

    fun refreshStamina(data: GameData): GameData {
        if (data.stamina >= data.maxStamina) {
            return data.copy(lastStaminaRegenTime = System.currentTimeMillis())
        }
        val now = System.currentTimeMillis()
        val elapsed = now - data.lastStaminaRegenTime
        val regenCount = (elapsed / REGEN_INTERVAL_MS).toInt()
        if (regenCount <= 0) return data
        val newStamina = (data.stamina + regenCount).coerceAtMost(data.maxStamina)
        return data.copy(
            stamina = newStamina,
            lastStaminaRegenTime = data.lastStaminaRegenTime + regenCount * REGEN_INTERVAL_MS,
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
