package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2

interface UnitBehavior {
    fun update(unit: GameUnit, dt: Float, findEnemy: (position: Vec2, range: Float) -> Enemy?)
    fun onAttack(unit: GameUnit, target: Enemy): AttackResult
    fun onTakeDamage(unit: GameUnit, damage: Float, isMagic: Boolean)
    fun canAttack(): Boolean = true
    fun reset()
}

data class AttackResult(
    val damage: Float,
    val isMagic: Boolean,
    val isCrit: Boolean,
    val isInstant: Boolean  // true = melee instant, false = projectile
)
