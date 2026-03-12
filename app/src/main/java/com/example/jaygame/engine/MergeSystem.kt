package com.example.jaygame.engine

import com.example.jaygame.data.UNIT_DEFS

object MergeSystem {
    private const val MERGE_COUNT = 3
    private const val LUCKY_CHANCE = 0.05f

    data class MergeResult(
        val resultUnitDefId: Int,
        val consumedTiles: List<Int>,
        val isLucky: Boolean,
    )

    fun tryMerge(grid: Grid, tileIndex: Int): MergeResult? {
        val unit = grid.getUnit(tileIndex) ?: return null
        val candidates = grid.findMergeCandidates(unit.unitDefId, unit.grade)
        if (candidates.size < MERGE_COUNT) return null

        val consumed = candidates.take(MERGE_COUNT)

        val unitDef = UNIT_DEFS.find { it.id == unit.unitDefId } ?: return null
        var resultId = unitDef.mergeResultId
        if (resultId < 0) return null

        val isLucky = Math.random() < LUCKY_CHANCE
        if (isLucky) {
            val nextDef = UNIT_DEFS.find { it.id == resultId }
            if (nextDef != null && nextDef.mergeResultId >= 0) {
                resultId = nextDef.mergeResultId
            }
        }

        return MergeResult(resultId, consumed, isLucky)
    }

    fun findMergeableTiles(grid: Grid): Set<Int> {
        val mergeable = mutableSetOf<Int>()
        val checked = mutableSetOf<Int>()

        for (i in 0 until Grid.TOTAL) {
            val unit = grid.getUnit(i) ?: continue
            val key = unit.unitDefId * 100 + unit.grade
            if (key in checked) continue
            checked.add(key)

            val candidates = grid.findMergeCandidates(unit.unitDefId, unit.grade)
            if (candidates.size >= MERGE_COUNT) {
                val def = UNIT_DEFS.find { it.id == unit.unitDefId }
                if (def != null && def.mergeResultId >= 0) {
                    mergeable.addAll(candidates)
                }
            }
        }
        return mergeable
    }
}
