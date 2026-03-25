@file:Suppress("DEPRECATION")
package com.example.jaygame.engine

import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.UnitRace
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
     * 합성 결과 결정.
     * 같은 유닛 3개 소모 → 다음 등급의 같은 종족 유닛 풀에서 랜덤 선택.
     * 같은 종족 풀이 비어있으면 전체 등급 풀에서 선택.
     *
     * @param race 합성할 유닛의 종족
     * @param currentGrade 소모되는 유닛의 등급
     * @return 결과 블루프린트 ID + 럭키 여부, 합성 불가 시 null
     */
    fun determineMergeResult(
        race: UnitRace,
        currentGrade: UnitGrade,
        blueprintRegistry: BlueprintRegistry,
        selectedRaces: Set<UnitRace> = emptySet(),
    ): BlueprintMergeResult? {
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

        // 선택된 종족들의 해당 등급 합성 가능 유닛 풀
        val racesPool = blueprintRegistry.findMergeableByRacesAndGrade(selectedRaces, targetGrade)

        // 풀이 비어있으면 전체 등급 풀에서 선택
        val pool = racesPool.ifEmpty { blueprintRegistry.findMergeableByGrade(targetGrade) }
        if (pool.isEmpty()) return null

        val selected = pool[(nextRandom() * pool.size).toInt().coerceIn(0, pool.size - 1)]
        return BlueprintMergeResult(selected.id, isLucky && targetGrade != nextGrade)
    }

    /**
     * 합성 가능한 슬롯 찾기: determineMergeResult와 동일한 조건으로 판정.
     */
    fun findMergeableSlots(grid: Grid, blueprintRegistry: BlueprintRegistry): Set<Int> {
        val mergeable = mutableSetOf<Int>()
        for (i in 0 until Grid.SLOT_COUNT) {
            if (grid.getStackCount(i) >= Grid.MERGE_COST) {
                val unit = grid.getUnit(i) ?: continue
                if (unit.unitCategory == UnitCategory.SPECIAL) continue
                val grade = UnitGrade.entries.getOrElse(unit.grade) { UnitGrade.COMMON }
                if (determineMergeResult(unit.race, grade, blueprintRegistry, BattleBridge.selectedRaces.value) != null) {
                    mergeable.add(i)
                }
            }
        }
        return mergeable
    }
}
