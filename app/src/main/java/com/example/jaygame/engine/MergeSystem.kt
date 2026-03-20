@file:Suppress("DEPRECATION")
package com.example.jaygame.engine

import com.example.jaygame.engine.UnitGrade.Companion.nextGrade

object MergeSystem {
    private const val MERGE_COUNT = 3
    private const val LUCKY_CHANCE = 0.05f

    /** 유물 행운 합성 보너스 (0.0~1.0 추가 확률) */
    var luckyMergeBonus: Float = 0f

    data class BlueprintMergeResult(
        val resultBlueprintId: String,
        val consumedTiles: List<Int>,
        val isLucky: Boolean,
    )

    /**
     * Grade-based merge: 같은 등급 3개 → 다음 등급 랜덤 유닛.
     * tileIndex의 유닛 등급과 같은 등급 유닛 3개를 소모하고,
     * 다음 등급 블루프린트 중 랜덤으로 하나를 결과로 반환.
     */
    fun tryMergeBlueprint(
        grid: Grid,
        tileIndex: Int,
        blueprintRegistry: BlueprintRegistry,
    ): BlueprintMergeResult? {
        val unit = grid.getUnit(tileIndex) ?: return null
        if (unit.unitCategory == UnitCategory.HIDDEN || unit.unitCategory == UnitCategory.SPECIAL) return null

        val currentGrade = UnitGrade.entries.getOrNull(unit.grade) ?: return null
        val nextGrade = currentGrade.nextGrade() ?: return null // 최고 등급이면 합성 불가

        val candidates = grid.findMergeCandidatesByGrade(unit.grade)
        if (candidates.size < MERGE_COUNT) return null

        // tileIndex를 우선 포함, 나머지 채움
        val consumed = if (tileIndex in candidates) {
            listOf(tileIndex) + candidates.filter { it != tileIndex }.take(MERGE_COUNT - 1)
        } else {
            candidates.take(MERGE_COUNT)
        }

        // 다음 등급 블루프린트 중 랜덤 선택
        val nextGradeBps = blueprintRegistry.findNormalByGrade(nextGrade)
        if (nextGradeBps.isEmpty()) return null

        var resultBp = nextGradeBps.random()

        // Lucky merge: 2단계 업그레이드
        val isLucky = Math.random() < (LUCKY_CHANCE + luckyMergeBonus)
        if (isLucky) {
            val luckyGrade = nextGrade.nextGrade()
            if (luckyGrade != null) {
                val luckyBps = blueprintRegistry.findNormalByGrade(luckyGrade)
                if (luckyBps.isNotEmpty()) {
                    resultBp = luckyBps.random()
                }
            }
        }

        return BlueprintMergeResult(resultBp.id, consumed, isLucky)
    }

    /**
     * Grade-based findMergeableTiles: 같은 등급 유닛이 3개 이상이고
     * 다음 등급이 존재하는 그룹의 타일 인덱스를 반환.
     */
    fun findMergeableTilesByBlueprint(
        grid: Grid,
        blueprintRegistry: BlueprintRegistry,
    ): Set<Int> {
        val mergeable = mutableSetOf<Int>()
        val checked = mutableSetOf<Int>()

        for (i in 0 until Grid.TOTAL) {
            val unit = grid.getUnit(i) ?: continue
            if (unit.grade in checked) continue
            if (unit.unitCategory == UnitCategory.HIDDEN || unit.unitCategory == UnitCategory.SPECIAL) continue
            checked.add(unit.grade)

            val currentGrade = UnitGrade.entries.getOrNull(unit.grade) ?: continue
            if (currentGrade.nextGrade() == null) continue // 최고 등급은 합성 불가

            val candidates = grid.findMergeCandidatesByGrade(unit.grade)
            if (candidates.size >= MERGE_COUNT) {
                mergeable.addAll(candidates)
            }
        }
        return mergeable
    }
}
