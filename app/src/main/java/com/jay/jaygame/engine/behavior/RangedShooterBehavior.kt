package com.jay.jaygame.engine.behavior

import com.jay.jaygame.engine.*
import com.jay.jaygame.engine.math.Vec2

class RangedShooterBehavior(
    private val aoe: Boolean = false  // true for mage-type (area), false for archer-type (single)
) : UnitBehavior {
    private var attackCooldown: Float = 0f

    override fun update(unit: GameUnit, dt: Float, findEnemy: (Vec2, Float) -> Enemy?) {
        attackCooldown -= dt * unit.spdMultiplier
        // Fixed position — tower defense style
        unit.position.x = unit.homePosition.x
        unit.position.y = unit.homePosition.y

        when (unit.state) {
            UnitState.IDLE -> {
                val enemy = findEnemy(unit.position, unit.range)
                if (enemy != null) {
                    unit.currentTarget = enemy
                    unit.state = UnitState.ATTACKING
                }
            }
            UnitState.ATTACKING -> {
                val target = unit.currentTarget
                if (target == null || !target.alive) {
                    unit.currentTarget = null
                    unit.isAttacking = false
                    unit.state = UnitState.IDLE
                    return
                }
                // Check if target is still in range
                val distSq = unit.position.distanceSqTo(target.position)
                if (distSq > unit.range * unit.range) {
                    unit.isAttacking = false
                    val newTarget = findEnemy(unit.position, unit.range)
                    if (newTarget != null) {
                        unit.currentTarget = newTarget
                    } else {
                        unit.currentTarget = null
                        unit.state = UnitState.IDLE
                    }
                } else {
                    unit.isAttacking = true
                }
            }
            else -> { /* Other states not used by ranged */ }
        }
    }

    override fun canAttack(): Boolean = attackCooldown <= 0f

    override fun onAttack(unit: GameUnit, target: Enemy): AttackResult {
        attackCooldown = UnitBehavior.cooldownFor(unit.atkSpeed)
        val isCrit = Math.random() < 0.05  // Base 5% crit
        val damage = unit.baseATK * (if (isCrit) 2f else 1f)
        val isMagic = unit.damageType == DamageType.MAGIC
        return AttackResult(
            damage = damage,
            isMagic = isMagic,
            isCrit = isCrit,
            isInstant = false  // Ranged = projectile
        )
    }

    override fun onTakeDamage(unit: GameUnit, damage: Float, isMagic: Boolean) {
        unit.applyDamage(damage, isMagic)
    }

    override fun reset() {
        attackCooldown = 0f
    }
}
