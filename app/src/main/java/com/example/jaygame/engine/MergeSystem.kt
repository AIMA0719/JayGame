@file:Suppress("DEPRECATION")
package com.example.jaygame.engine

import com.example.jaygame.engine.UnitGrade.Companion.nextGrade

object MergeSystem {
    private const val MERGE_COUNT = 3
    private const val LUCKY_CHANCE = 0.05f

    /** 유물 행운 합성 보너스 (0.0~1.0 추가 확률) */
    @Volatile var luckyMergeBonus: Float = 0f

    data class BlueprintMergeResult(
        val resultBlueprintId: String,
        val consumedTiles: List<Int>,
        val isLucky: Boolean,
    )

    /**
     * Grade+Role merge: 같은 등급 & 같은 역할 3개 → 다음 등급 랜덤 유닛.
     */
    fun tryMergeBlueprint(
        grid: Grid,
        tileIndex: Int,
        blueprintRegistry: BlueprintRegistry,
    ): BlueprintMergeResult? {
        val unit = grid.getUnit(tileIndex) ?: return null
        if (unit.unitCategory == UnitCategory.SPECIAL) return null

        val currentGrade = UnitGrade.entries.getOrNull(unit.grade) ?: return null
        val nextGrade = currentGrade.nextGrade() ?: return null

        val candidates = grid.findMergeCandidates(unit.grade, unit.role)
        if (candidates.size < MERGE_COUNT) return null

        val consumed = if (tileIndex in candidates) {
            listOf(tileIndex) + candidates.filter { it != tileIndex }.take(MERGE_COUNT - 1)
        } else {
            candidates.take(MERGE_COUNT)
        }

        // 다음 등급 블루프린트 중 랜덤 선택
        val nextGradeBps = blueprintRegistry.findMergeableByGrade(nextGrade)
        if (nextGradeBps.isEmpty()) return null

        var resultBp = nextGradeBps.random()

        // Lucky merge: 2단계 업그레이드
        val isLucky = Math.random() < (LUCKY_CHANCE + luckyMergeBonus)
        if (isLucky) {
            val luckyGrade = nextGrade.nextGrade()
            if (luckyGrade != null) {
                val luckyBps = blueprintRegistry.findMergeableByGrade(luckyGrade)
                if (luckyBps.isNotEmpty()) {
                    resultBp = luckyBps.random()
                }
            }
        }

        return BlueprintMergeResult(resultBp.id, consumed, isLucky)
    }

    /**
     * Grade+Role findMergeableTiles: 같은 등급 & 같은 역할 유닛이 3개 이상이고
     * 다음 등급이 존재하는 그룹의 타일 인덱스를 반환.
     */
    fun findMergeableTilesByBlueprint(
        grid: Grid,
        blueprintRegistry: BlueprintRegistry,
    ): Set<Int> {
        val mergeable = mutableSetOf<Int>()
        data class GradeRole(val grade: Int, val role: UnitRole)
        val checked = mutableSetOf<GradeRole>()

        for (i in 0 until Grid.TOTAL) {
            val unit = grid.getUnit(i) ?: continue
            val key = GradeRole(unit.grade, unit.role)
            if (key in checked) continue
            if (unit.unitCategory == UnitCategory.SPECIAL) continue
            checked.add(key)

            val currentGrade = UnitGrade.entries.getOrNull(unit.grade) ?: continue
            if (currentGrade.nextGrade() == null) continue

            val candidates = grid.findMergeCandidates(unit.grade, unit.role)
            if (candidates.size >= MERGE_COUNT) {
                mergeable.addAll(candidates)
            }
        }
        return mergeable
    }
}
