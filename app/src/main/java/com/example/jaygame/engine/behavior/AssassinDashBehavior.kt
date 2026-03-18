package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.*
import com.example.jaygame.engine.math.Vec2

class AssassinDashBehavior : UnitBehavior {
    private var attackCooldown: Float = 0f

    override fun update(unit: GameUnit, dt: Float, findEnemy: (Vec2, Float) -> Enemy?) {
        attackCooldown -= dt

        when (unit.state) {
            UnitState.IDLE -> {
                val enemy = findEnemy(unit.position, 720f)
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
                // Move toward target but stay inside field bounds (clamped by BattleEngine)
                val distSq = unit.position.distanceSqTo(target.position)
                if (distSq > unit.range * unit.range) {
                    // Chase toward enemy at 1.5x speed
                    val dir = target.position.minus(unit.position).normalized()
                    unit.position = unit.position.plus(dir.times(unit.moveSpeed * 1.5f * dt))
                    unit.isAttacking = false
                } else {
                    unit.isAttacking = true
                }
            }
            else -> {}
        }
    }

    override fun canAttack(): Boolean = attackCooldown <= 0f

    override fun onAttack(unit: GameUnit, target: Enemy): AttackResult {
        attackCooldown = 1f / unit.atkSpeed.coerceAtLeast(0.1f)
        val isCrit = Math.random() < 0.10
        val damage = unit.baseATK * (if (isCrit) 2.5f else 1f)
        return AttackResult(
            damage = damage,
            isMagic = unit.damageType == DamageType.MAGIC,
            isCrit = isCrit,
            isInstant = false  // Projectile — "검기" slash effect
        )
    }

    override fun onTakeDamage(unit: GameUnit, damage: Float, isMagic: Boolean) {
        val resist = if (isMagic) unit.magicResist else unit.defense
        val reduction = resist / (resist + 100f)
        unit.hp -= damage * (1f - reduction)
        if (unit.hp <= 0f) {
            unit.alive = false
            unit.state = UnitState.DEAD
        }
    }

    override fun reset() {
        attackCooldown = 0f
    }
}
