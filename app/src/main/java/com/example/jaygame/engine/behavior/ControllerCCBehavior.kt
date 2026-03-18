package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.*
import com.example.jaygame.engine.math.Vec2

class ControllerCCBehavior(
    private val isRanged: Boolean = true
) : UnitBehavior {
    private var attackCooldown: Float = 0f
    private var ccTimer: Float = 0f
    private val CC_DURATION = 2f  // Base CC duration in seconds

    override fun update(unit: GameUnit, dt: Float, findEnemy: (Vec2, Float) -> Enemy?) {
        attackCooldown -= dt
        when (unit.state) {
            UnitState.IDLE -> {
                val enemy = findEnemy(unit.position, unit.range * (if (isRanged) 1.5f else 1f))
                if (enemy != null) {
                    unit.currentTarget = enemy
                    unit.state = UnitState.ATTACKING
                }
            }
            UnitState.ATTACKING -> {
                val target = unit.currentTarget
                if (target == null || !target.alive) {
                    unit.currentTarget = null
                    unit.state = UnitState.IDLE
                }
            }
            else -> {}
        }
    }

    fun canAttack(): Boolean = attackCooldown <= 0f
    fun getCCDuration(): Float = CC_DURATION

    override fun onAttack(unit: GameUnit, target: Enemy): AttackResult {
        attackCooldown = 1f / unit.atkSpeed
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
