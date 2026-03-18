package com.example.jaygame.engine.fieldeffect

import com.example.jaygame.engine.BattleFieldAccess
import com.example.jaygame.engine.BuffType
import com.example.jaygame.engine.FieldEffectController
import com.example.jaygame.engine.GameUnit
import com.example.jaygame.engine.math.Vec2

class TimeWarpEffect : FieldEffectController {
    private var centerPosition: Vec2 = Vec2(0f, 0f)
    private val SLOW_FACTOR = 0.5f    // 50% slow (weaker but global)
    private val EFFECT_RANGE = 480f   // large range — covers most of the field
    private val SLOW_DURATION = 1.0f

    override fun onPlace(unit: GameUnit, field: BattleFieldAccess) {
        centerPosition = unit.position.copy()
    }

    override fun update(dt: Float, field: BattleFieldAccess) {
        // Slows ALL enemies on the field (broader but weaker than PathSlow)
        field.getEnemies()
            .filter { it.alive }
            .forEach { enemy ->
                enemy.buffs.addBuff(BuffType.Slow, SLOW_FACTOR, SLOW_DURATION)
            }
    }

    override fun onRemove() {}
    override fun getEffectRange(): Float = EFFECT_RANGE
    override fun canStack(): Boolean = false
    override fun reset() { centerPosition = Vec2(0f, 0f) }
}
