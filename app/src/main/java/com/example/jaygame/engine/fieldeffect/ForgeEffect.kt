package com.example.jaygame.engine.fieldeffect

import com.example.jaygame.engine.BattleFieldAccess
import com.example.jaygame.engine.BuffType
import com.example.jaygame.engine.FieldEffectController
import com.example.jaygame.engine.GameUnit
import com.example.jaygame.engine.math.Vec2

class ForgeEffect : FieldEffectController {
    private var centerPosition: Vec2 = Vec2(0f, 0f)
    private val ATK_BONUS = 0.10f     // +10% ATK
    private val EFFECT_RANGE = 96f    // adjacent tiles (2 tiles wide)
    private val BUFF_DURATION = 9999f // effectively permanent
    private val buffedUnits = mutableSetOf<GameUnit>()

    override fun onPlace(unit: GameUnit, field: BattleFieldAccess) {
        centerPosition = unit.position.copy()
    }

    override fun update(dt: Float, field: BattleFieldAccess) {
        field.getUnits()
            .filter { it.alive && it !in buffedUnits && it.position.distanceTo(centerPosition) <= EFFECT_RANGE }
            .forEach { ally ->
                ally.buffs.addBuff(BuffType.AtkUp, ATK_BONUS, BUFF_DURATION)
                buffedUnits.add(ally)
            }
        // Clean up dead units from tracking set
        buffedUnits.removeAll { !it.alive }
    }

    override fun onRemove() {
        buffedUnits.clear()
    }

    override fun getEffectRange(): Float = EFFECT_RANGE
    override fun canStack(): Boolean = false

    override fun reset() {
        centerPosition = Vec2(0f, 0f)
        buffedUnits.clear()
    }
}
