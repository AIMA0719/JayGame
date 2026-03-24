package com.example.jaygame.data

import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.UnitBlueprint
import com.example.jaygame.engine.UnitGrade

/**
 * 덱 시스템 유틸리티
 * 덱 검증, 자동 채움, 합성 체인 조회
 */
object DeckManager {
    const val DECK_SIZE = 5
    const val MAX_PRESETS = 3

    /** 덱이 유효한지 검사: 5개, 중복없음, 소환가능 COMMON 블루프린트만 */
    fun isValid(deck: List<String>, gameData: GameData): Boolean {
        if (deck.size != DECK_SIZE) return false
        if (deck.toSet().size != DECK_SIZE) return false  // 중복 체크
        val registry = if (BlueprintRegistry.isReady) BlueprintRegistry.instance else return false
        return deck.all { id ->
            val bp = registry.findById(id) ?: return false
            bp.isSummonable && bp.grade == UnitGrade.COMMON
        }
    }

    /** 빈 슬롯을 소유한 COMMON 소환가능 유닛으로 자동 채움 */
    fun autoFill(currentDeck: List<String>, gameData: GameData): List<String> {
        val available = getAvailableUnits(gameData)
        val usedIds = currentDeck.toMutableSet()
        val result = currentDeck.toMutableList()

        // 잘못된 ID 제거
        val registry = if (BlueprintRegistry.isReady) BlueprintRegistry.instance else return result
        result.removeAll { id ->
            val bp = registry.findById(id)
            bp == null || !bp.isSummonable || bp.grade != UnitGrade.COMMON
        }
        usedIds.clear()
        usedIds.addAll(result)

        // 빈 슬롯 채우기
        for (bp in available) {
            if (result.size >= DECK_SIZE) break
            if (bp.id !in usedIds) {
                result.add(bp.id)
                usedIds.add(bp.id)
            }
        }
        return result.take(DECK_SIZE)
    }

    /** 덱에 넣을 수 있는 COMMON 소환가능 블루프린트 목록 (소유 우선) */
    fun getAvailableUnits(gameData: GameData): List<UnitBlueprint> {
        if (!BlueprintRegistry.isReady) return emptyList()
        val registry = BlueprintRegistry.instance
        val commonSummonable = registry.findByGradeAndSummonable(UnitGrade.COMMON)
        val (owned, notOwned) = commonSummonable.partition { gameData.units[it.id]?.owned == true }
        return owned + notOwned
    }

    /** COMMON 블루프린트 ID로부터 합성 체인 미리보기 (COMMON→RARE→HERO→LEGEND) */
    fun getMergeChain(blueprintId: String): List<UnitBlueprint> {
        if (!BlueprintRegistry.isReady) return emptyList()
        val registry = BlueprintRegistry.instance
        val chain = mutableListOf<UnitBlueprint>()
        val visited = mutableSetOf<String>()
        var current = registry.findById(blueprintId) ?: return emptyList()
        chain.add(current)
        visited.add(current.id)
        while (current.mergeResultId != null) {
            if (current.mergeResultId!! in visited) break // 순환 참조 방어
            val next = registry.findById(current.mergeResultId!!) ?: break
            chain.add(next)
            visited.add(next.id)
            current = next
        }
        return chain
    }
}
