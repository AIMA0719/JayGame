package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.*
import com.example.jaygame.engine.math.Vec2

class SupportAuraBehavior : UnitBehavior {
    private var buffTickTimer: Float = 0f
    private val BUFF_INTERVAL = 1f  // Apply buff every 1 second

    override fun update(unit: GameUnit, dt: Float, findEnemy: (Vec2, Float) -> Enemy?) {
        buffTickTimer -= dt
        if (buffTickTimer <= 0f) {
            buffTickTimer = BUFF_INTERVAL
            // Buff logic will be applied by BattleEngine reading this behavior's state
        }
        // Stay near home position
        unit.state = UnitState.IDLE
        // Slow drift toward home position
        val dir = unit.homePosition.minus(unit.position).normalized()
        unit.position = unit.position.plus(dir.times(20f * dt))
    }

    fun shouldApplyBuff(): Boolean = buffTickTimer <= 0f
    fun getBuffRange(): Float = 120f  // Buff range in pixels

    override fun onAttack(unit: GameUnit, target: Enemy): AttackResult {
        return AttackResult(
            damage = unit.baseATK * 0.3f,  // Low damage
            isMagic = true,
            isCrit = false,
            isInstant = false
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
        buffTickTimer = 0f
    }
}
