package com.example.jaygame.engine

import com.example.jaygame.audio.SfxManager
import com.example.jaygame.audio.SoundEvent
import com.example.jaygame.bridge.BattleBridge

/**
 * 배틀 합성 시스템 — 유닛 합성/레시피 로직을 BattleEngine에서 분리.
 * BattleEngine 참조를 통해 grid, units, sp, mergeCount 등에 접근.
 */
class BattleMergeHandler(private val engine: BattleEngine) {

    fun requestMerge(tileIndex: Int) {
        val existingUnit = engine.grid.getUnit(tileIndex) ?: return
        if (existingUnit.unitCategory == UnitCategory.SPECIAL) return

        // 1) 레시피 체크 우선
        if (tryExecuteRecipeCraft()) return

        // 2) 수동 합성: 같은 유닛 3개 소모 → 다음 등급
        tryMergeSlot(tileIndex)
    }

    /** 모든 합성 가능 슬롯에서 자동으로 3개씩 소모하여 합성 */
    fun requestMergeAll() {
        tryExecuteRecipeCraft()
        // 합성이 그리드를 변경하므로 매 합성 후 재스캔
        var safety = 50 // 무한루프 방지
        var merged = true
        while (merged && safety-- > 0) {
            merged = false
            for (i in 0 until Grid.SLOT_COUNT) {
                if (engine.grid.getStackCount(i) >= Grid.MERGE_COST) {
                    if (tryMergeSlot(i)) {
                        merged = true
                        break // 그리드 변경됨, 처음부터 재스캔
                    }
                }
            }
        }
    }

    /** 레시피 조합 요청 — 필드 전체 스캔하여 완성 가능한 레시피 합성 */
    fun requestRecipeCraft() {
        tryExecuteRecipeCraft()
    }

    /** 필드에서 완성 가능한 레시피를 찾아 합성 실행. 성공 시 true 반환. */
    fun tryExecuteRecipeCraft(): Boolean {
        if (!RecipeSystem.isReady) return false
        val (recipe, consumedTiles) = RecipeSystem.instance.findMatchingRecipeOnGrid(engine.grid, engine.luckyStones) ?: return false
        val resultBp = RecipeSystem.instance.completeRecipe(
            recipe, consumedTiles.mapNotNull { engine.grid.getUnit(it) }
        ) ?: return false

        engine.luckyStones = (engine.luckyStones - recipe.luckyStonesCost).coerceAtLeast(0)
        val placeTile = consumedTiles.first()
        consumedTiles.forEach { i ->
            val u = engine.grid.removeUnit(i)
            if (u != null) engine.units.release(u)
        }
        val unit = engine.spawnFromBlueprint(resultBp) ?: return false
        engine.grid.placeUnit(placeTile, unit)
        engine.invalidateMergeCache()
        engine.mergeCount++
        BattleBridge.onMergeComplete(placeTile, true, unit.blueprintId)
        return true
    }

    /** 수동 합성 — 슬롯에서 같은 유닛 3개 소모 → 다음 등급 랜덤 유닛. 성공 시 true. */
    fun tryMergeSlot(slotIndex: Int): Boolean {
        if (engine.grid.getStackCount(slotIndex) < Grid.MERGE_COST) return false
        val representative = engine.grid.getUnit(slotIndex) ?: return false
        val race = representative.race

        // 3개 소모 (앞에서부터) — 소모된 유닛에서 등급 판정
        val consumed = engine.grid.removeUnits(slotIndex, Grid.MERGE_COST)
        if (consumed.size < Grid.MERGE_COST) {
            // 롤백: 소모된 유닛 다시 배치
            consumed.forEach { u -> engine.grid.placeUnit(slotIndex, u) }
            return false
        }

        val maxGrade = consumed.maxOf { UnitGrade.entries.getOrElse(it.grade) { UnitGrade.COMMON } }

        val mergeResult = MergeSystem.determineMergeResult(race, maxGrade, engine.blueprintRegistry, BattleBridge.selectedRaces.value)
        if (mergeResult == null) {
            // 합성 불가 — 롤백
            consumed.forEach { u -> engine.grid.placeUnit(slotIndex, u) }
            return false
        }
        val resultBp = engine.blueprintRegistry.findById(mergeResult.resultBlueprintId)
        if (resultBp == null) {
            consumed.forEach { u -> engine.grid.placeUnit(slotIndex, u) }
            return false
        }

        // 풀에서 결과 유닛 스폰 — 실패 시 소모 유닛 복원
        val resultUnit = engine.spawnFromBlueprint(resultBp)
        if (resultUnit == null) {
            consumed.forEach { u -> engine.grid.placeUnit(slotIndex, u) }
            return false
        }

        consumed.forEach { u -> engine.units.release(u) }

        val stackSlot = engine.grid.findStackableSlot(resultBp.id)
        var actualSlot = when {
            stackSlot >= 0 -> stackSlot
            engine.grid.getStackCount(slotIndex) == 0 -> slotIndex
            else -> engine.grid.findEmpty().takeIf { it >= 0 } ?: slotIndex
        }
        if (!engine.grid.placeUnit(actualSlot, resultUnit)) {
            val fallback = engine.grid.findEmpty()
            if (fallback >= 0) { engine.grid.placeUnit(fallback, resultUnit); actualSlot = fallback }
            else engine.units.release(resultUnit)
        }

        engine.invalidateMergeCache()
        engine.mergeCount++
        SfxManager.play(if (mergeResult.isLucky) SoundEvent.MergeLucky else SoundEvent.Merge)
        BattleBridge.onMergeComplete(actualSlot, mergeResult.isLucky, resultUnit.blueprintId)
        return true
    }
}
