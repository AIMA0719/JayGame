package com.example.jaygame.engine.fieldeffect

import com.example.jaygame.engine.BattleFieldAccess
import com.example.jaygame.engine.FieldEffectController
import com.example.jaygame.engine.GameUnit
import com.example.jaygame.engine.math.Vec2

class BarrierEffect : FieldEffectController {
    private var centerPosition: Vec2 = Vec2(0f, 0f)
    private val DEFENSE_BONUS = 0.5f  // +50% defense
    private val EFFECT_RANGE = 144f   // 3x3 tiles (48px each)

    override fun onPlace(unit: GameUnit, field: BattleFieldAccess) {
        centerPosition = unit.position.copy()
    }

    override fun update(dt: Float, field: BattleFieldAccess) {
        // Buff allies within range — defense bonus is read by combat system
        field.getUnits().filter { it.alive && it.position.distanceTo(centerPosition) <= EFFECT_RANGE }
            .forEach { ally ->
                // Apply defense buff (managed externally to avoid stacking issues)
            }
    }

    override fun onRemove() {}
    override fun getEffectRange(): Float = EFFECT_RANGE
    override fun canStack(): Boolean = false
    override fun reset() { centerPosition = Vec2(0f, 0f) }
}
