package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.*
import com.example.jaygame.engine.math.Vec2

class AssassinDashBehavior : UnitBehavior {
    private var dashTarget: Enemy? = null
    private var dashCooldown: Float = 0f
    private val BASE_COOLDOWN = 2f

    override fun update(unit: GameUnit, dt: Float, findEnemy: (Vec2, Float) -> Enemy?) {
        dashCooldown = (dashCooldown - dt).coerceAtLeast(0f)
        when (unit.state) {
            UnitState.IDLE -> {
                if (dashCooldown <= 0f) {
                    val enemy = findEnemy(unit.position, unit.range * 2f)
                    if (enemy != null) {
                        dashTarget = enemy
                        unit.currentTarget = enemy
                        unit.state = UnitState.DASHING
                    }
                }
            }
            UnitState.DASHING -> {
                val target = dashTarget
                if (target == null || !target.alive) {
                    // Target died mid-dash → return home, half cooldown
                    dashTarget = null
                    unit.currentTarget = null
                    unit.state = UnitState.RETURNING
                    dashCooldown = BASE_COOLDOWN / 2f
                    return
                }
                val dir = target.position.minus(unit.position).normalized()
                unit.position = unit.position.plus(dir.times(unit.moveSpeed * 2f * dt))
                if (unit.position.distanceSqTo(target.position) < 20f * 20f) {
                    // Hit! Deal damage
                    val attackResult = onAttack(unit, target)
                    target.takeDamage(attackResult.damage, attackResult.isMagic)
                    unit.state = UnitState.RETURNING
                    dashCooldown = BASE_COOLDOWN
                    dashTarget = null
                    unit.currentTarget = null
                }
            }
            UnitState.RETURNING -> {
                val dir = unit.homePosition.minus(unit.position).normalized()
                unit.position = unit.position.plus(dir.times(unit.moveSpeed * dt))
                if (unit.position.distanceSqTo(unit.homePosition) < 10f * 10f) {
                    unit.position = unit.homePosition.copy()
                    unit.state = UnitState.IDLE
                    dashTarget = null
                    unit.currentTarget = null
                }
            }
            else -> {}
        }
    }

    override fun onAttack(unit: GameUnit, target: Enemy): AttackResult {
        val isCrit = Math.random() < 0.05
        return AttackResult(
            damage = unit.baseATK * 2f * (if (isCrit) 3f else 1f),  // 2x base, 3x on crit
            isMagic = unit.damageType == DamageType.MAGIC,
            isCrit = isCrit,
            isInstant = true
        )
    }

    override fun onTakeDamage(unit: GameUnit, damage: Float, isMagic: Boolean) {
        if (unit.state == UnitState.DASHING) return  // Invincible during dash
        val resist = if (isMagic) unit.magicResist else unit.defense
        val reduction = resist / (resist + 100f)
        unit.hp -= damage * (1f - reduction)
        if (unit.hp <= 0f) {
            unit.alive = false
            unit.state = UnitState.DEAD
        }
    }

    override fun reset() {
        dashTarget = null
        dashCooldown = 0f
    }
}
