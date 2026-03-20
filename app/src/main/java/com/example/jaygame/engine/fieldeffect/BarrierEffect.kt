package com.example.jaygame.engine.fieldeffect

import com.example.jaygame.engine.BattleFieldAccess
import com.example.jaygame.engine.BuffType
import com.example.jaygame.engine.FieldEffectController
import com.example.jaygame.engine.GameUnit
import com.example.jaygame.engine.math.Vec2

class BarrierEffect : FieldEffectController {
    private var centerPosition: Vec2 = Vec2(0f, 0f)
    private val DEFENSE_BONUS = 0.5f  // +50% defense
    private val EFFECT_RANGE = 144f   // 3x3 tiles (48px each)
    private val BUFF_DURATION = 1.5f  // short duration, re-applied each tick

    override fun onPlace(unit: GameUnit, field: BattleFieldAccess) {
        centerPosition = unit.position.copy()
    }

    override fun update(dt: Float, field: BattleFieldAccess) {
        field.getUnits().filter { it.alive && it.position.distanceTo(centerPosition) <= EFFECT_RANGE }
            .forEach { ally ->
                if (!ally.buffs.hasBuff(BuffType.DefUp)) {
                    ally.buffs.addBuff(BuffType.DefUp, DEFENSE_BONUS, BUFF_DURATION)
                }
            }
    }

    override fun onRemove() {}
    override fun getEffectRange(): Float = EFFECT_RANGE
    override fun canStack(): Boolean = false
    override fun reset() { centerPosition = Vec2(0f, 0f) }
}
