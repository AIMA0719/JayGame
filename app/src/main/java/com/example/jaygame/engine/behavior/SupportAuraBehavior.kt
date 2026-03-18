package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.*
import com.example.jaygame.engine.math.Vec2

class SupportAuraBehavior : UnitBehavior {
    private var buffTickTimer: Float = 0f
    private val BUFF_INTERVAL = 1f  // Apply buff every 1 second
    var pendingBuff: Boolean = false
        private set

    override fun update(unit: GameUnit, dt: Float, findEnemy: (Vec2, Float) -> Enemy?) {
        buffTickTimer -= dt
        if (buffTickTimer <= 0f) {
            buffTickTimer = BUFF_INTERVAL
            pendingBuff = true
        }
        // Guard against overwriting DEAD/RESPAWNING
        if (unit.state == UnitState.DEAD || unit.state == UnitState.RESPAWNING) return
        unit.state = UnitState.IDLE

        // Find nearest enemy across entire map
        val enemy = findEnemy(unit.position, 720f)
        if (enemy != null) {
            // Move toward enemy but stay at buff range distance (don't go melee)
            val dir = enemy.position.minus(unit.position)
            val dist = dir.length
            val desiredDist = getBuffRange() * 0.8f  // stay within buff range
            if (dist > desiredDist) {
                val norm = dir.normalized()
                unit.position = unit.position.plus(norm.times(unit.moveSpeed * dt))
            }
        } else {
            // No enemies — drift toward home position
            val dir = unit.homePosition.minus(unit.position)
            if (dir.lengthSq > 16f) {
                val norm = dir.normalized()
                unit.position = unit.position.plus(norm.times(20f * dt))
            }
        }
    }

    fun consumeBuff(): Boolean {
        if (pendingBuff) { pendingBuff = false; return true }
        return false
    }

    fun shouldApplyBuff(): Boolean = pendingBuff
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
