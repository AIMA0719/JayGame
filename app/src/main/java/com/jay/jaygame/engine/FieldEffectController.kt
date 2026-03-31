package com.jay.jaygame.engine

/**
 * Thin abstraction over the battle environment that field effects interact with.
 * [BattleEngine] is the primary implementation.
 */
interface BattleFieldAccess {
    fun getUnits(): List<GameUnit>
    fun getEnemies(): List<Enemy>
}

interface FieldEffectController {
    fun onPlace(unit: GameUnit, field: BattleFieldAccess)
    fun update(dt: Float, field: BattleFieldAccess)
    fun onRemove()
    fun getEffectRange(): Float
    fun canStack(): Boolean
    fun reset()
}

enum class FieldEffectType {
    BARRIER, PATH_SLOW, TIME_WARP, SUMMON_FIELD, FORGE, ALCHEMY, DISPEL, TOTEM
}
