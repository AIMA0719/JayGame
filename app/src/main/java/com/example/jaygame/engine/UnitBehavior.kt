package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2

interface UnitBehavior {
    fun update(unit: GameUnit, dt: Float, findEnemy: (position: Vec2, range: Float) -> Enemy?)
    fun onAttack(unit: GameUnit, target: Enemy): AttackResult
    fun onTakeDamage(unit: GameUnit, damage: Float, isMagic: Boolean)
    fun canAttack(): Boolean = true
    fun reset()

    companion object {
        /** atkSpeed → cooldown 변환 (0 이하 방어) */
        fun cooldownFor(atkSpeed: Float): Float =
            if (atkSpeed > 0f) 1f / atkSpeed else 1f
    }
}

data class AttackResult(
    val damage: Float,
    val isMagic: Boolean,
    val isCrit: Boolean,
    val isInstant: Boolean  // true = melee instant, false = projectile
)
