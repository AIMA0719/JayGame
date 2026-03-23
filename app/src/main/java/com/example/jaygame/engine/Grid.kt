package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2

/**
 * 3×6 슬롯 기반 그리드 (운빨존많겜 스타일).
 * 각 슬롯에 동일 유닛 최대 3개까지 중첩 가능.
 * 3개 중첩 시 자동 합성 트리거.
 * 유닛은 슬롯에 고정, 드래그로 슬롯 간 이동.
 */
class Grid {
    companion object {
        const val ROWS = 3
        const val COLS = 6
        const val SLOT_COUNT = ROWS * COLS          // 18 slots
        const val MAX_STACK = 3                      // 한 슬롯에 최대 3개 중첩

        const val CANVAS_W = 720f
        const val CANVAS_H = 1280f

        // 그리드 영역 (캔버스 내 배치)
        const val GRID_W = 480f
        const val GRID_H = 300f
        private const val CLIFF_DEPTH = 25f
        const val ORIGIN_X = (CANVAS_W - GRID_W) / 2f            // 120
        const val ORIGIN_Y = 430f                                 // 430 (약간 아래쪽, 경로+HUD 공간 확보)

        const val CELL_W = GRID_W / COLS            // 80
        const val CELL_H = GRID_H / ROWS            // 100

        // Field bounds
        const val FIELD_MIN_X = ORIGIN_X
        const val FIELD_MAX_X = ORIGIN_X + GRID_W
        const val FIELD_MIN_Y = ORIGIN_Y
        const val FIELD_MAX_Y = ORIGIN_Y + GRID_H

        // Field center
        const val CENTER_X = ORIGIN_X + GRID_W / 2f
        const val CENTER_Y = ORIGIN_Y + GRID_H / 2f
        val FIELD_CENTER = Vec2(CENTER_X, CENTER_Y)

        // For backward compat - total unit capacity (18 slots × 1 display unit each)
        // Actual unit objects tracked separately
        const val TOTAL = SLOT_COUNT
    }

    /**
     * 각 슬롯은 유닛 리스트를 보유 (최대 MAX_STACK개, 같은 blueprintId만).
     * 빈 슬롯 = empty list.
     */
    private val slots = Array(SLOT_COUNT) { mutableListOf<GameUnit>() }

    /** 슬롯의 "대표" 유닛 (첫 번째) 반환. 빈 슬롯이면 null */
    fun getUnit(slotIndex: Int): GameUnit? = slots.getOrNull(slotIndex)?.firstOrNull()

    /** 슬롯의 모든 유닛 반환 */
    fun getUnitsInSlot(slotIndex: Int): List<GameUnit> = slots.getOrNull(slotIndex) ?: emptyList()

    /** 슬롯에 유닛 수 */
    fun getStackCount(slotIndex: Int): Int = slots.getOrNull(slotIndex)?.size ?: 0

    /** 슬롯 중심의 월드 좌표 */
    fun slotCenter(slotIndex: Int): Vec2 {
        val col = slotIndex % COLS
        val row = slotIndex / COLS
        return Vec2(
            ORIGIN_X + col * CELL_W + CELL_W / 2f,
            ORIGIN_Y + row * CELL_H + CELL_H / 2f,
        )
    }

    /** 월드 좌표에서 슬롯 인덱스 반환. 범위 밖이면 -1 */
    fun getSlotAt(worldX: Float, worldY: Float): Int {
        val col = ((worldX - ORIGIN_X) / CELL_W).toInt()
        val row = ((worldY - ORIGIN_Y) / CELL_H).toInt()
        if (col !in 0 until COLS || row !in 0 until ROWS) return -1
        return row * COLS + col
    }

    /**
     * 유닛을 슬롯에 배치.
     * - 빈 슬롯: 바로 배치
     * - 같은 blueprintId 유닛이 있고 스택 < MAX_STACK: 중첩
     * - 다른 유닛이 있거나 스택 가득: 실패
     * @return true if placed successfully
     */
    fun placeUnit(slotIndex: Int, unit: GameUnit): Boolean {
        if (slotIndex !in 0 until SLOT_COUNT) return false
        val slot = slots[slotIndex]

        if (slot.isEmpty()) {
            // 빈 슬롯에 배치
            slot.add(unit)
            unit.tileIndex = slotIndex
            val center = slotCenter(slotIndex)
            unit.position = center.copy()
            unit.homePosition = center.copy()
            return true
        }

        // 같은 유닛이고 스택 여유 있으면 중첩
        if (slot.size < MAX_STACK && slot.first().blueprintId == unit.blueprintId) {
            slot.add(unit)
            unit.tileIndex = slotIndex
            val center = slotCenter(slotIndex)
            unit.position = center.copy()
            unit.homePosition = center.copy()
            return true
        }

        return false // 다른 유닛이거나 스택 가득
    }

    /** 슬롯에서 유닛 1개 제거 (마지막 추가된 것) */
    fun removeUnit(slotIndex: Int): GameUnit? {
        if (slotIndex !in 0 until SLOT_COUNT) return null
        val slot = slots[slotIndex]
        if (slot.isEmpty()) return null
        return slot.removeAt(slot.lastIndex)
    }

    /** 슬롯의 모든 유닛 제거하고 반환 */
    fun removeAllFromSlot(slotIndex: Int): List<GameUnit> {
        if (slotIndex !in 0 until SLOT_COUNT) return emptyList()
        val slot = slots[slotIndex]
        val removed = slot.toList()
        slot.clear()
        return removed
    }

    /** 빈 슬롯 찾기 */
    fun findEmpty(): Int {
        for (i in 0 until SLOT_COUNT) if (slots[i].isEmpty()) return i
        return -1
    }

    /** 같은 blueprintId 유닛이 있고 스택 여유 있는 슬롯 찾기 */
    fun findStackableSlot(blueprintId: String): Int {
        for (i in 0 until SLOT_COUNT) {
            val slot = slots[i]
            if (slot.isNotEmpty() && slot.size < MAX_STACK && slot.first().blueprintId == blueprintId) {
                return i
            }
        }
        return -1
    }

    /** 스택이 MAX_STACK(3)인 슬롯 찾기 (합성 대상) */
    fun findFullStacks(): List<Int> {
        return (0 until SLOT_COUNT).filter { slots[it].size >= MAX_STACK }
    }

    /** 전체 유닛 수 (모든 슬롯의 스택 합계) */
    fun unitCount(): Int = slots.sumOf { it.size }

    /** 사용 중인 슬롯 수 */
    fun occupiedSlotCount(): Int = slots.count { it.isNotEmpty() }
}
