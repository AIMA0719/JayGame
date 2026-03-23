@file:Suppress("DEPRECATION")
package com.example.jaygame.engine

import com.example.jaygame.engine.UnitGrade.Companion.nextGrade

object MergeSystem {
    private const val LUCKY_CHANCE = 0.05f

    /** 유물 행운 합성 보너스 */
    @Volatile var luckyMergeBonus: Float = 0f

    /** 테스트용 난수 오버라이드 (null이면 Math.random() 사용) */
    @Volatile var randomOverride: (() -> Double)? = null

    private fun nextRandom(): Double = randomOverride?.invoke() ?: Math.random()

    data class BlueprintMergeResult(
        val resultBlueprintId: String,
        val isLucky: Boolean,
    )

    /**
     * 슬롯에 3개 동일 유닛이 쌓였을 때 합성 결과 결정.
     * 다음 등급의 소환 가능 유닛 풀에서 랜덤 선택.
     * LEGEND(전설) 유닛은 합성 불가 (MYTHIC은 레시피 전용).
     *
     * @param blueprintId 합성할 유닛의 블루프린트 ID
     * @return 결과 블루프린트 ID + 럭키 여부, 합성 불가 시 null
     */
    fun determineMergeResult(
        blueprintId: String,
        blueprintRegistry: BlueprintRegistry,
    ): BlueprintMergeResult? {
        val bp = blueprintRegistry.findById(blueprintId) ?: return null
        val currentGrade = bp.grade

        // LEGEND 이상은 일반 합성 불가 (MYTHIC은 레시피 전용)
        if (currentGrade.ordinal >= UnitGrade.LEGEND.ordinal) return null

        val nextGrade = currentGrade.nextGrade() ?: return null
        // nextGrade가 MYTHIC이면 합성 불가
        if (nextGrade == UnitGrade.MYTHIC) return null

        // Lucky merge: 2단계 점프
        val isLucky = nextRandom() < (LUCKY_CHANCE + luckyMergeBonus)
        val targetGrade = if (isLucky) {
            val luckyGrade = nextGrade.nextGrade()
            // 럭키 등급이 없거나 MYTHIC이면 일반 등급으로 폴백
            if (luckyGrade != null && luckyGrade != UnitGrade.MYTHIC) luckyGrade else nextGrade
        } else {
            nextGrade
        }

        // 해당 등급의 합성 가능 유닛 풀에서 랜덤 선택
        val pool = blueprintRegistry.findMergeableByGrade(targetGrade)
        if (pool.isEmpty()) return null

        val selected = pool[(nextRandom() * pool.size).toInt().coerceIn(0, pool.size - 1)]
        return BlueprintMergeResult(selected.id, isLucky && targetGrade != nextGrade)
    }

    /**
     * 합성 가능한 슬롯 찾기 (스택이 3인 슬롯).
     */
    fun findMergeableSlots(grid: Grid, blueprintRegistry: BlueprintRegistry): Set<Int> {
        val mergeable = mutableSetOf<Int>()
        for (i in 0 until Grid.SLOT_COUNT) {
            if (grid.getStackCount(i) >= Grid.MAX_STACK) {
                val unit = grid.getUnit(i) ?: continue
                if (unit.unitCategory == UnitCategory.SPECIAL) continue
                val bp = blueprintRegistry.findById(unit.blueprintId) ?: continue
                // LEGEND 이상은 일반 합성 불가
                if (bp.grade.ordinal >= UnitGrade.LEGEND.ordinal) continue
                val nextGrade = bp.grade.nextGrade() ?: continue
                if (nextGrade == UnitGrade.MYTHIC) continue
                // 다음 등급 풀이 존재하는지 확인
                if (blueprintRegistry.findMergeableByGrade(nextGrade).isNotEmpty()) {
                    mergeable.add(i)
                }
            }
        }
        return mergeable
    }
}
