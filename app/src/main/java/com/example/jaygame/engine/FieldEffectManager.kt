package com.example.jaygame.engine

class FieldEffectManager {
    private val activeEffects = mutableListOf<Pair<GameUnit, FieldEffectController>>()
    private val MAX_SPECIAL_UNITS = 2

    fun canPlace(unit: GameUnit): Boolean {
        if (unit.unitCategory != UnitCategory.SPECIAL) return true
        val specialCount = activeEffects.count { it.first.unitCategory == UnitCategory.SPECIAL }
        if (specialCount >= MAX_SPECIAL_UNITS) return false
        if (activeEffects.any { it.first.blueprintId == unit.blueprintId }) return false
        return true
    }

    fun addEffect(unit: GameUnit, controller: FieldEffectController, field: BattleFieldAccess) {
        controller.onPlace(unit, field)
        activeEffects.add(Pair(unit, controller))
    }

    fun update(dt: Float, field: BattleFieldAccess) {
        val iterator = activeEffects.iterator()
        while (iterator.hasNext()) {
            val (unit, controller) = iterator.next()
            if (!unit.alive) {
                controller.onRemove()
                iterator.remove()
            } else {
                controller.update(dt, field)
            }
        }
    }

    fun removeEffect(unit: GameUnit) {
        val pair = activeEffects.find { it.first === unit }
        pair?.second?.onRemove()
        activeEffects.removeAll { it.first === unit }
    }

    fun getActiveEffects(): List<Pair<GameUnit, FieldEffectController>> = activeEffects.toList()
    fun activeCount(): Int = activeEffects.size

    fun clear() {
        activeEffects.forEach { it.second.onRemove() }
        activeEffects.clear()
    }
}
