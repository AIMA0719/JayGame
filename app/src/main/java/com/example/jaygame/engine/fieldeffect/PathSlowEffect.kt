package com.example.jaygame.engine.fieldeffect

import com.example.jaygame.engine.BattleFieldAccess
import com.example.jaygame.engine.BuffType
import com.example.jaygame.engine.FieldEffectController
import com.example.jaygame.engine.GameUnit
import com.example.jaygame.engine.math.Vec2

class PathSlowEffect : FieldEffectController {
    private var centerPosition: Vec2 = Vec2(0f, 0f)
    private val SLOW_FACTOR = 0.7f    // 70% slow
    private val EFFECT_RANGE = 144f   // 3x3 tiles (48px each)
    private val SLOW_DURATION = 1.0f  // reapplied each update tick

    override fun onPlace(unit: GameUnit, field: BattleFieldAccess) {
        centerPosition = unit.position.copy()
    }

    override fun update(dt: Float, field: BattleFieldAccess) {
        field.getEnemies()
            .filter { it.alive && it.position.distanceTo(centerPosition) <= EFFECT_RANGE }
            .forEach { enemy ->
                enemy.buffs.addBuff(BuffType.Slow, SLOW_FACTOR, SLOW_DURATION)
            }
    }

    override fun onRemove() {}
    override fun getEffectRange(): Float = EFFECT_RANGE
    override fun canStack(): Boolean = false
    override fun reset() { centerPosition = Vec2(0f, 0f) }
}
