package com.jay.jaygame.engine

import com.jay.jaygame.data.ALL_PETS
import com.jay.jaygame.data.GameData
import com.jay.jaygame.data.PetDef
import com.jay.jaygame.data.PetProgress

/**
 * Active pet system for battle.
 * Call init() at battle start, update() each tick.
 */
class PetBattleSystem {

    data class ActivePet(
        val def: PetDef,
        val level: Int,
        var cooldownRemaining: Float = 0f,
    )

    private val activePets = mutableListOf<ActivePet>()
    private var phoenixReviveAvailable = false
    private var phoenixReviveUsed = false

    // ── Lifecycle ──

    fun init(gameData: GameData) {
        activePets.clear()
        phoenixReviveAvailable = false
        phoenixReviveUsed = false

        for (petId in gameData.equippedPets) {
            val def = ALL_PETS.find { it.id == petId } ?: continue
            val progress = gameData.pets.find { it.petId == petId } ?: continue
            if (!progress.owned) continue
            activePets.add(ActivePet(def = def, level = progress.level))
            if (petId == 8) phoenixReviveAvailable = true
        }
    }

    /**
     * Called every fixed-timestep tick during battle.
     *
     * @param dt          Delta time (seconds)
     * @param enemies     Active enemies (from ObjectPool.toActiveList())
     * @param units       Active units (from ObjectPool.toActiveList())
     * @param onDamageEnemy  Callback: (enemy, damage) — deal direct damage to an enemy
     * @param onBuffUnit     Callback: (unit, type, value, duration)
     * @param onDotEnemy     Callback: (enemy, dpsValue, duration)
     */
    fun update(
        dt: Float,
        enemies: List<Enemy>,
        units: List<GameUnit>,
        onDamageEnemy: (Enemy, Float) -> Unit,
        onBuffUnit: (GameUnit, BuffType, Float, Float) -> Unit,
        onDotEnemy: (Enemy, Float, Float) -> Unit,
    ) {
        for (pet in activePets) {
            if (pet.def.isPassive) continue
            if (pet.cooldownRemaining > 0f) {
                pet.cooldownRemaining -= dt
                continue
            }
            // Trigger skill
            val triggered = triggerSkill(pet, enemies, units, onDamageEnemy, onBuffUnit, onDotEnemy)
            if (triggered) {
                // Higher pet level reduces cooldown (up to -30% at level 10)
                val levelCdReduction = 1f - (pet.level * 0.03f).coerceAtMost(0.3f)
                pet.cooldownRemaining = pet.def.cooldown * levelCdReduction
            }
        }
    }

    private fun triggerSkill(
        pet: ActivePet,
        enemies: List<Enemy>,
        units: List<GameUnit>,
        onDamageEnemy: (Enemy, Float) -> Unit,
        onBuffUnit: (GameUnit, BuffType, Float, Float) -> Unit,
        onDotEnemy: (Enemy, Float, Float) -> Unit,
    ): Boolean {
        val lv = pet.level
        val aliveEnemies = enemies.filter { it.alive }
        val aliveUnits = units.filter { it.alive }

        when (pet.def.id) {
            0 -> {
                // 화염 드래곤: AoE 20*lv damage to all enemies
                if (aliveEnemies.isEmpty()) return false
                val damage = 20f * lv
                aliveEnemies.forEach { onDamageEnemy(it, damage) }
                return true
            }
            1 -> {
                // 독거미: DoT 10*lv for 5s on highest HP enemy
                if (aliveEnemies.isEmpty()) return false
                val target = aliveEnemies.maxByOrNull { it.hp } ?: return false
                onDotEnemy(target, 10f * lv, 5f)
                return true
            }
            2 -> {
                // 번개 매: 30*lv damage to random 3 enemies
                if (aliveEnemies.isEmpty()) return false
                val targets = aliveEnemies.shuffled().take(3)
                val damage = 30f * lv
                targets.forEach { onDamageEnemy(it, damage) }
                return true
            }
            3 -> {
                // 요정: AtkUp buff lv*2% for 8s to all units
                if (aliveUnits.isEmpty()) return false
                val value = lv * 0.02f
                aliveUnits.forEach { onBuffUnit(it, BuffType.AtkUp, value, 8f) }
                return true
            }
            4 -> {
                // 골렘: Shield lv*50 for 10s to all units
                if (aliveUnits.isEmpty()) return false
                val value = lv * 50f
                aliveUnits.forEach { onBuffUnit(it, BuffType.Shield, value, 10f) }
                return true
            }
            5 -> {
                // 유니콘: SpdUp buff lv*3% for 6s to all units
                if (aliveUnits.isEmpty()) return false
                val value = lv * 0.03f
                aliveUnits.forEach { onBuffUnit(it, BuffType.SpdUp, value, 6f) }
                return true
            }
            else -> return false
        }
    }

    // ── Passive accessors ──

    /** ID 6 (두꺼비): gold kill bonus, lv×5% as fraction */
    fun getGoldKillBonus(): Float {
        val pet = activePets.find { it.def.id == 6 } ?: return 0f
        return pet.level * 0.05f
    }

    /** ID 7 (9미호): summon grade-up chance, lv×2% as fraction */
    fun getSummonGradeUpChance(): Float {
        val pet = activePets.find { it.def.id == 7 } ?: return 0f
        return pet.level * 0.02f
    }

    /** ID 8 (봉황): true if the one-time revive is still available */
    fun canPhoenixRevive(): Boolean = phoenixReviveAvailable && !phoenixReviveUsed

    /** Consume the phoenix revive (call when defeat is triggered) */
    fun usePhoenixRevive() {
        phoenixReviveUsed = true
    }
}
