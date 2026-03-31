package com.jay.jaygame.engine.behavior

import com.jay.jaygame.engine.*
import com.jay.jaygame.engine.math.Vec2

class ControllerCCBehavior(
    private val isRanged: Boolean = true
) : UnitBehavior {
    private var attackCooldown: Float = 0f
    private var ccTimer: Float = 0f
    private val CC_DURATION = 2f  // Base CC duration in seconds

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
            else -> {}
        }
    }

    override fun canAttack(): Boolean = attackCooldown <= 0f
    fun getCCDuration(): Float = CC_DURATION

    override fun onAttack(unit: GameUnit, target: Enemy): AttackResult {
        attackCooldown = UnitBehavior.cooldownFor(unit.atkSpeed)
        return AttackResult(
            damage = unit.baseATK * 0.5f,  // Low damage, CC focused
            isMagic = true,
            isCrit = false,
            isInstant = !isRanged
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
        ccTimer = 0f
    }
}
