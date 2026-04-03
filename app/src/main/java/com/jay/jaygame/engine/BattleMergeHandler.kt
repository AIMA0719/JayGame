package com.jay.jaygame.engine

import com.jay.jaygame.JayGameApplication
import com.jay.jaygame.audio.SfxManager
import com.jay.jaygame.audio.SoundEvent
import com.jay.jaygame.bridge.BattleBridge

/**
 * 배틀 합성 시스템 — 유닛 합성/레시피 로직을 BattleEngine에서 분리.
 * BattleEngine 참조를 통해 grid, units, sp, mergeCount 등에 접근.
 */
class BattleMergeHandler(private val engine: BattleEngine) {

    fun requestMerge(tileIndex: Int) {
        val existingUnit = engine.grid.getUnit(tileIndex) ?: return
        if (existingUnit.unitCategory == UnitCategory.SPECIAL) return

        // 일반 합성만 실행 (레시피는 별도 "조합법" 버튼으로만 가능)
        tryMergeSlot(tileIndex)
    }

    /** 합성 가능 슬롯에서 1회만 합성 (버튼 1번 = 1회 합성) */
    fun requestMergeAll() {
        for (i in 0 until Grid.SLOT_COUNT) {
            if (engine.grid.getStackCount(i) >= Grid.MERGE_COST) {
                if (tryMergeSlot(i)) return
            }
        }
    }

    /** 레시피 조합 요청 — 특정 레시피 ID가 있으면 해당 레시피만 시도 */
    fun requestRecipeCraft(recipeId: String? = null) {
        tryExecuteRecipeCraft(recipeId)
    }

    /** 필드에서 완성 가능한 레시피를 찾아 합성 실행. 성공 시 true 반환. */
    fun tryExecuteRecipeCraft(targetRecipeId: String? = null): Boolean {
        if (!RecipeSystem.isReady) return false
        val (recipe, consumedTiles) = if (targetRecipeId != null) {
            RecipeSystem.instance.findSpecificRecipeOnGrid(targetRecipeId, engine.grid, engine.sp.toInt())
        } else {
            RecipeSystem.instance.findMatchingRecipeOnGrid(engine.grid, engine.sp.toInt())
        } ?: return false

        // 매칭된 유닛들을 미리 수집 (각 타일에서 1개씩, 중복 타일은 순서대로 제거)
        val unitsToConsume = mutableListOf<Pair<Int, GameUnit>>() // (tileIndex, unit)
        val removeCountPerTile = mutableMapOf<Int, Int>()
        for (tileIdx in consumedTiles) {
            val alreadyTaken = removeCountPerTile.getOrDefault(tileIdx, 0)
            val slotUnits = engine.grid.getUnitsInSlot(tileIdx)
            if (alreadyTaken >= slotUnits.size) return false // 슬롯에 유닛 부족
            unitsToConsume.add(tileIdx to slotUnits[alreadyTaken])
            removeCountPerTile[tileIdx] = alreadyTaken + 1
        }

        // 레시피 검증 (실제 유닛 리스트로)
        val resultBp = RecipeSystem.instance.resolveRecipe(
            recipe, unitsToConsume.map { it.second }
        ) ?: return false

        // ── 배치 가능 여부 사전 검증 (재료 소모 전) ──
        // 소모 후 빈 슬롯이 생기는지, 또는 이미 빈 슬롯이 있는지,
        // 또는 같은 blueprintId 유닛이 이미 있어서 스택 가능한지 확인
        val willFreeSlot = removeCountPerTile.any { (tileIdx, count) ->
            engine.grid.getStackCount(tileIdx) <= count
        }
        val hasEmptySlot = engine.grid.findEmpty() >= 0
        val hasStackableSlot = engine.grid.findStackableSlot(resultBp.id) >= 0
        if (!willFreeSlot && !hasEmptySlot && !hasStackableSlot) {
            // 결과 유닛을 놓을 자리가 없음 — 재료 소모하지 않고 중단
            return false
        }

        // 결과 유닛 먼저 스폰 — 실패 시 아무것도 소모하지 않음
        val resultUnit = engine.spawnFromBlueprint(resultBp) ?: return false

        // 모든 준비 완료 — 이제 소모 실행
        engine.sp = (engine.sp - recipe.goldCost).coerceAtLeast(0f)

        // 타일별 제거 횟수에 따라 유닛 제거
        for ((tileIdx, count) in removeCountPerTile) {
            repeat(count) {
                val u = engine.grid.removeUnit(tileIdx)
                if (u != null) engine.units.release(u)
            }
        }

        // 배치: 같은 유닛 스택 > 비어있는 소모 타일 > 빈 슬롯
        val stackSlot = engine.grid.findStackableSlot(resultBp.id)
        val placeTile = when {
            stackSlot >= 0 -> stackSlot
            else -> consumedTiles.firstOrNull { engine.grid.getStackCount(it) == 0 }
                ?: engine.grid.findEmpty().takeIf { it >= 0 }
                ?: consumedTiles.first()
        }

        if (!engine.grid.placeUnit(placeTile, resultUnit)) {
            val fallback = engine.grid.findEmpty()
            if (fallback >= 0) {
                engine.grid.placeUnit(fallback, resultUnit)
                finalizeRecipeCraft(recipe.id, fallback, resultUnit.blueprintId)
                return true
            }
            // 사전 검증을 통과했으므로 여기 도달은 불가하지만 안전장치
            engine.units.release(resultUnit)
            return false
        }

        finalizeRecipeCraft(recipe.id, placeTile, resultUnit.blueprintId)
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
            consumed.forEach { u -> engine.grid.placeUnit(slotIndex, u) }
            return false
        }

        val maxGrade = consumed.maxOf { UnitGrade.entries.getOrElse(it.grade) { UnitGrade.COMMON } }

        val mergeResult = MergeSystem.determineMergeResult(race, maxGrade, engine.blueprintRegistry, BattleBridge.selectedRaces.value)
        if (mergeResult == null) {
            consumed.forEach { u -> engine.grid.placeUnit(slotIndex, u) }
            return false
        }
        val resultBp = engine.blueprintRegistry.findById(mergeResult.resultBlueprintId)
        if (resultBp == null) {
            consumed.forEach { u -> engine.grid.placeUnit(slotIndex, u) }
            return false
        }

        // ── 배치 가능 여부 사전 검증 (consumed는 이미 grid에서 제거된 상태) ──
        val slotBecameEmpty = engine.grid.getStackCount(slotIndex) == 0
        val canStack = engine.grid.findStackableSlot(resultBp.id) >= 0
        val hasEmpty = slotBecameEmpty || engine.grid.findEmpty() >= 0
        if (!canStack && !hasEmpty) {
            // 놓을 자리 없음 — 롤백
            consumed.forEach { u -> engine.grid.placeUnit(slotIndex, u) }
            return false
        }

        // 풀에서 결과 유닛 스폰 — 실패 시 소모 유닛 복원
        val resultUnit = engine.spawnFromBlueprint(resultBp)
        if (resultUnit == null) {
            consumed.forEach { u -> engine.grid.placeUnit(slotIndex, u) }
            return false
        }

        // 배치가 보장됨 — 이제 소모 확정
        consumed.forEach { u -> engine.units.release(u) }

        val stackSlot = engine.grid.findStackableSlot(resultBp.id)
        var actualSlot = when {
            stackSlot >= 0 -> stackSlot
            slotBecameEmpty -> slotIndex
            else -> engine.grid.findEmpty()
        }
        if (actualSlot < 0 || !engine.grid.placeUnit(actualSlot, resultUnit)) {
            val fallback = engine.grid.findEmpty()
            if (fallback >= 0) { engine.grid.placeUnit(fallback, resultUnit); actualSlot = fallback }
            else {
                // 사전 검증을 통과했으므로 도달 불가하지만 안전장치
                android.util.Log.e("BattleMergeHandler", "Merge placement failed after pre-check — unit lost (blueprintId=${resultBp.id})")
                engine.units.release(resultUnit)
                return false
            }
        }

        engine.invalidateMergeCache()
        engine.mergeCount++
        SfxManager.play(if (mergeResult.isLucky) SoundEvent.MergeLucky else SoundEvent.Merge)
        BattleBridge.onMergeComplete(actualSlot, mergeResult.isLucky, resultUnit.blueprintId)
        return true
    }

    private fun finalizeRecipeCraft(recipeId: String, placeTile: Int, blueprintId: String) {
        RecipeSystem.instance.markDiscovered(recipeId)
        runCatching {
            val app = JayGameApplication.appContext as? JayGameApplication
            app?.repository?.saveDiscoveredRecipes(RecipeSystem.instance.getDiscoveredIds())
        }.onFailure { error ->
            android.util.Log.w("BattleMergeHandler", "Failed to persist discovered recipes", error)
        }
        engine.invalidateMergeCache()
        engine.mergeCount++
        BattleBridge.onMergeComplete(placeTile, true, blueprintId)
    }
}
