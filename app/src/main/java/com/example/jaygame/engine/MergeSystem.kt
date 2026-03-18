@file:Suppress("DEPRECATION")
package com.example.jaygame.engine

// TODO(Task18): Remove UNIT_DEFS import once legacy tryMerge/findMergeableTiles are removed
import com.example.jaygame.data.UNIT_DEFS

object MergeSystem {
    private const val MERGE_COUNT = 3
    private const val LUCKY_CHANCE = 0.05f

    /** 유물 행운 합성 보너스 (0.0~1.0 추가 확률) */
    var luckyMergeBonus: Float = 0f

    data class MergeResult(
        val resultUnitDefId: Int,
        val consumedTiles: List<Int>,
        val isLucky: Boolean,
    )

    data class BlueprintMergeResult(
        val resultBlueprintId: String,
        val consumedTiles: List<Int>,
        val isLucky: Boolean,
    )

    // ── New blueprint-based API ──

    /**
     * Check whether any group of 3+ units with the same blueprintId exists
     * and is eligible for merging (not HIDDEN/SPECIAL, has a mergeResultId).
     */
    fun canMerge(units: List<GameUnit>, blueprintRegistry: BlueprintRegistry): Boolean {
        return units
            .filter { it.alive && it.blueprintId.isNotEmpty() }
            .groupBy { it.blueprintId }
            .any { (bpId, group) ->
                if (group.size < MERGE_COUNT) return@any false
                val bp = blueprintRegistry.findById(bpId) ?: return@any false
                bp.unitCategory != UnitCategory.HIDDEN &&
                        bp.unitCategory != UnitCategory.SPECIAL &&
                        bp.mergeResultId != null
            }
    }

    /**
     * Look up the merge result blueprint for a given blueprintId.
     */
    fun getMergeResult(blueprintId: String, blueprintRegistry: BlueprintRegistry): UnitBlueprint? {
        val bp = blueprintRegistry.findById(blueprintId) ?: return null
        val resultId = bp.mergeResultId ?: return null
        return blueprintRegistry.findById(resultId)
    }

    /**
     * Blueprint-based tryMerge: finds candidates on the grid by blueprintId,
     * checks eligibility (category, mergeResultId), applies lucky merge logic.
     */
    fun tryMergeBlueprint(
        grid: Grid,
        tileIndex: Int,
        blueprintRegistry: BlueprintRegistry,
    ): BlueprintMergeResult? {
        val unit = grid.getUnit(tileIndex) ?: return null
        if (unit.blueprintId.isEmpty()) return null

        val bp = blueprintRegistry.findById(unit.blueprintId) ?: return null
        if (bp.unitCategory == UnitCategory.HIDDEN || bp.unitCategory == UnitCategory.SPECIAL) return null

        var resultId = bp.mergeResultId ?: return null

        val candidates = grid.findMergeCandidatesByBlueprint(unit.blueprintId)
        if (candidates.size < MERGE_COUNT) return null

        val consumed = candidates.take(MERGE_COUNT)

        val isLucky = Math.random() < (LUCKY_CHANCE + luckyMergeBonus)
        if (isLucky) {
            val nextBp = blueprintRegistry.findById(resultId)
            if (nextBp != null && nextBp.mergeResultId != null) {
                resultId = nextBp.mergeResultId
            }
        }

        return BlueprintMergeResult(resultId, consumed, isLucky)
    }

    /**
     * Blueprint-based findMergeableTiles: returns set of tile indices
     * that belong to groups of 3+ same-blueprint units eligible for merge.
     */
    fun findMergeableTilesByBlueprint(
        grid: Grid,
        blueprintRegistry: BlueprintRegistry,
    ): Set<Int> {
        val mergeable = mutableSetOf<Int>()
        val checked = mutableSetOf<String>()

        for (i in 0 until Grid.TOTAL) {
            val unit = grid.getUnit(i) ?: continue
            if (unit.blueprintId.isEmpty()) continue
            if (unit.blueprintId in checked) continue
            checked.add(unit.blueprintId)

            val bp = blueprintRegistry.findById(unit.blueprintId) ?: continue
            if (bp.unitCategory == UnitCategory.HIDDEN || bp.unitCategory == UnitCategory.SPECIAL) continue
            if (bp.mergeResultId == null) continue

            val candidates = grid.findMergeCandidatesByBlueprint(unit.blueprintId)
            if (candidates.size >= MERGE_COUNT) {
                mergeable.addAll(candidates)
            }
        }
        return mergeable
    }

    // ── Legacy Int-based API (backward compatibility) ──

    @Deprecated("Use tryMergeBlueprint instead")
    fun tryMerge(grid: Grid, tileIndex: Int): MergeResult? {
        val unit = grid.getUnit(tileIndex) ?: return null
        @Suppress("DEPRECATION")
        val candidates = grid.findMergeCandidates(unit.unitDefId, unit.grade)
        if (candidates.size < MERGE_COUNT) return null

        val consumed = candidates.take(MERGE_COUNT)

        val unitDef = UNIT_DEFS.find { it.id == unit.unitDefId } ?: return null
        var resultId = unitDef.mergeResultId
        if (resultId < 0) return null

        val isLucky = Math.random() < (LUCKY_CHANCE + luckyMergeBonus)
        if (isLucky) {
            val nextDef = UNIT_DEFS.find { it.id == resultId }
            if (nextDef != null && nextDef.mergeResultId >= 0) {
                resultId = nextDef.mergeResultId
            }
        }

        return MergeResult(resultId, consumed, isLucky)
    }

    @Deprecated("Use findMergeableTilesByBlueprint instead")
    fun findMergeableTiles(grid: Grid): Set<Int> {
        val mergeable = mutableSetOf<Int>()
        val checked = mutableSetOf<Int>()

        for (i in 0 until Grid.TOTAL) {
            val unit = grid.getUnit(i) ?: continue
            val key = unit.unitDefId * 100 + unit.grade
            if (key in checked) continue
            checked.add(key)

            @Suppress("DEPRECATION")
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
