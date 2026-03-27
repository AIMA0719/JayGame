package com.example.jaygame.engine

import com.example.jaygame.engine.BattleEngine.Companion.SELL_BASE
import com.example.jaygame.engine.BattleEngine.Companion.SELL_PER_GRADE

/**
 * 배틀 경제 시스템 — 유닛 판매 로직을 BattleEngine에서 분리.
 * BattleEngine 참조를 통해 grid, units, sp 등에 접근.
 */
class BattleEconomy(private val engine: BattleEngine) {

    private fun sellUnit(u: GameUnit) {
        engine.sp += SELL_BASE + u.grade * SELL_PER_GRADE
        engine.units.release(u)
    }

    fun requestSell(tileIndex: Int) {
        val unit = engine.grid.removeUnit(tileIndex) ?: return
        sellUnit(unit)
        engine.invalidateMergeCache()
    }

    fun requestSellAllSlot(tileIndex: Int) {
        val removed = engine.grid.removeAllFromSlot(tileIndex)
        if (removed.isEmpty()) return
        removed.forEach { sellUnit(it) }
        engine.invalidateMergeCache()
    }

    fun requestBulkSell(grade: Int): Int {
        var count = 0
        for (i in 0 until Grid.SLOT_COUNT) {
            val representative = engine.grid.getUnit(i) ?: continue
            if (representative.grade == grade) {
                val removed = engine.grid.removeAllFromSlot(i)
                removed.forEach { sellUnit(it); count++ }
            }
        }
        if (count > 0) engine.invalidateMergeCache()
        return count
    }
}
