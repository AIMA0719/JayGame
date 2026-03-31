@file:Suppress("DEPRECATION")
package com.jay.jaygame.engine

import com.jay.jaygame.engine.UnitGrade.Companion.nextGrade

/**
 * 유닛 관리자.
 * 소환, 머지(조합), 그리드 배치를 관리.
 *
 * 머지 규칙:
 * - 동일 등급 + 동일 계열(family)인 유닛 3개 → 상위 등급 1개
 * - 초월 등급은 머지 불가 (최고 등급)
 * - 머지 결과는 UnitSpec.mergeResultId로 결정
 */
interface UnitManager {
    /** 현재 그리드 위 모든 유닛 */
    fun getUnits(): List<BattleUnit>

    /** 유닛 소환 (그리드에 배치) */
    fun summon(spec: UnitSpec, row: Int, col: Int): BattleUnit?

    /** 머지 가능 여부 확인 (3개 이상 동일 등급+계열 존재) */
    fun canMerge(unit: BattleUnit): Boolean

    /** 3개 동일 등급+계열 유닛을 머지. 성공 시 상위 유닛 반환 */
    fun tryMerge(unit: BattleUnit): MergeResult

    /** 유닛 제거 */
    fun remove(instanceId: Int)

    /** 그리드 초기화 */
    fun clear()
}

sealed class MergeResult {
    data class Success(
        val resultUnit: BattleUnit,
        val consumedIds: List<Int>,
    ) : MergeResult()

    data object NotEnoughUnits : MergeResult()
    data object MaxGrade : MergeResult()
    data object NoMergeTarget : MergeResult()
}

class DefaultUnitManager(
    private val unitSpecs: Map<Int, UnitSpec>,
    private val gridRows: Int = 5,
    private val gridCols: Int = 4,
) : UnitManager {

    private val units = mutableListOf<BattleUnit>()
    private var nextInstanceId = 1

    override fun getUnits(): List<BattleUnit> = units.toList()

    override fun summon(spec: UnitSpec, row: Int, col: Int): BattleUnit? {
        // 그리드 범위 체크
        if (row !in 0 until gridRows || col !in 0 until gridCols) return null
        // 이미 점유된 셀 체크
        if (units.any { it.gridRow == row && it.gridCol == col }) return null

        val unit = BattleUnit(
            instanceId = nextInstanceId++,
            specId = spec.id,
            grade = spec.grade,
            family = spec.family,
            gridRow = row,
            gridCol = col,
        )
        units.add(unit)
        return unit
    }

    override fun canMerge(unit: BattleUnit): Boolean {
        if (unit.grade.nextGrade() == null) return false // 초월은 머지 불가
        val matches = findMergeGroup(unit)
        return matches.size >= 3
    }

    /**
     * 머지 알고리즘:
     * 1. 동일 등급 + 동일 계열인 유닛을 모두 찾음
     * 2. 3개 이상이면 가장 먼저 발견된 3개를 소비
     * 3. UnitSpec.mergeResultId로 결과 유닛 결정
     * 4. 소비된 3개 중 첫 번째 유닛의 위치에 결과 유닛 배치
     *
     * 시간 복잡도: O(n) where n = 그리드 위 유닛 수 (최대 20)
     */
    override fun tryMerge(unit: BattleUnit): MergeResult {
        val nextGrade = unit.grade.nextGrade()
            ?: return MergeResult.MaxGrade

        val group = findMergeGroup(unit)
        if (group.size < 3) return MergeResult.NotEnoughUnits

        // 머지할 3개 선택 (해당 유닛 포함)
        val toConsume = group.take(3)

        // 결과 유닛 스펙 결정
        val sourceSpec = unitSpecs[unit.specId] ?: return MergeResult.NoMergeTarget
        val resultSpec = if (sourceSpec.mergeResultId >= 0) {
            unitSpecs[sourceSpec.mergeResultId]
        } else {
            // mergeResultId가 없으면 같은 계열의 상위 등급 유닛 검색
            unitSpecs.values.find { it.family == unit.family && it.grade == nextGrade }
        }

        if (resultSpec == null) return MergeResult.NoMergeTarget

        // 소비할 유닛들 제거
        val consumedIds = toConsume.map { it.instanceId }
        units.removeAll { it.instanceId in consumedIds }

        // 결과 유닛 생성 (첫 번째 유닛 위치에)
        val targetPos = toConsume.first()
        val resultUnit = BattleUnit(
            instanceId = nextInstanceId++,
            specId = resultSpec.id,
            grade = resultSpec.grade,
            family = resultSpec.family,
            gridRow = targetPos.gridRow,
            gridCol = targetPos.gridCol,
        )
        units.add(resultUnit)

        return MergeResult.Success(
            resultUnit = resultUnit,
            consumedIds = consumedIds,
        )
    }

    override fun remove(instanceId: Int) {
        units.removeAll { it.instanceId == instanceId }
    }

    override fun clear() {
        units.clear()
        nextInstanceId = 1
    }

    /**
     * 동일 등급 + 동일 계열인 유닛 그룹을 찾음.
     * 대상 유닛을 맨 앞에 배치.
     */
    private fun findMergeGroup(unit: BattleUnit): List<BattleUnit> {
        val matches = units.filter {
            it.grade == unit.grade && it.family == unit.family
        }
        // 대상 유닛을 맨 앞으로
        return listOf(unit) + matches.filter { it.instanceId != unit.instanceId }
    }
}
