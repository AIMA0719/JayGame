package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 3×6 슬롯 기반 그리드.
 * 같은 유닛(동일 blueprintId)을 무제한 중첩 가능.
 * 수동 합성 버튼으로 같은 유닛 3개 소모 → 다음 등급.
 * 유닛은 슬롯에 고정, 드래그로 슬롯 간 이동.
 */
class Grid {
    companion object {
        const val ROWS = 3
        const val COLS = 6
        const val SLOT_COUNT = ROWS * COLS          // 18 slots
        const val MERGE_COST = 3                     // 합성에 필요한 같은 유닛 수

        const val CANVAS_W = 720f
        const val CANVAS_H = 1280f

        // 그리드 영역 — 1.2x 확대 (캔버스 내 배치)
        const val GRID_W = 576f                                  // 480 × 1.2
        const val GRID_H = 360f                                  // 300 × 1.2
        private const val CLIFF_DEPTH = 30f
        const val ORIGIN_X = (CANVAS_W - GRID_W) / 2f           // 72
        const val ORIGIN_Y = 400f                                // 430→400 (그리드 높이 증가분 보상)

        const val CELL_W = GRID_W / COLS                         // 96
        const val CELL_H = GRID_H / ROWS                        // 120

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

        /** 원형 배치 시 최대 시각적 표시 유닛 수 (나머지는 뱃지로) */
        const val MAX_VISUAL_UNITS = 6
    }

    /**
     * 각 슬롯은 유닛 리스트를 보유 (같은 유닛이면 무제한 중첩).
     * 빈 슬롯 = empty list.
     */
    private val slots = Array(SLOT_COUNT) { mutableListOf<GameUnit>() }

    /** 슬롯의 "대표" 유닛 (첫 번째) 반환. 빈 슬롯이면 null */
    fun getUnit(slotIndex: Int): GameUnit? = slots.getOrNull(slotIndex)?.firstOrNull()

    /** 슬롯의 모든 유닛 반환 (방어적 복사) */
    fun getUnitsInSlot(slotIndex: Int): List<GameUnit> = slots.getOrNull(slotIndex)?.toList() ?: emptyList()

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
        val relX = worldX - ORIGIN_X
        val relY = worldY - ORIGIN_Y
        if (relX < 0f || relY < 0f) return -1
        val col = (relX / CELL_W).toInt()
        val row = (relY / CELL_H).toInt()
        if (col !in 0 until COLS || row !in 0 until ROWS) return -1
        return row * COLS + col
    }

    /**
     * 유닛을 슬롯에 배치.
     * - 빈 슬롯: 바로 배치
     * - 같은 유닛(동일 blueprintId)이 있으면: 무제한 중첩
     * - 다른 유닛이면: 실패
     * @return true if placed successfully
     */
    fun placeUnit(slotIndex: Int, unit: GameUnit): Boolean {
        if (slotIndex !in 0 until SLOT_COUNT) return false
        val slot = slots[slotIndex]

        if (slot.isEmpty()) {
            // 빈 슬롯에 배치
            slot.add(unit)
            unit.tileIndex = slotIndex
            repositionSlotUnits(slotIndex)
            return true
        }

        // 같은 유닛(blueprintId)이면 무제한 중첩
        if (slot.first().blueprintId == unit.blueprintId) {
            slot.add(unit)
            unit.tileIndex = slotIndex
            repositionSlotUnits(slotIndex)
            return true
        }

        return false // 다른 유닛
    }

    /** 슬롯에서 유닛 1개 제거 (마지막 추가된 것) */
    fun removeUnit(slotIndex: Int): GameUnit? {
        if (slotIndex !in 0 until SLOT_COUNT) return null
        val slot = slots[slotIndex]
        if (slot.isEmpty()) return null
        val removed = slot.removeAt(slot.lastIndex)
        repositionSlotUnits(slotIndex)
        return removed
    }

    /** 슬롯에서 유닛 N개 제거 (앞에서부터) */
    fun removeUnits(slotIndex: Int, count: Int): List<GameUnit> {
        if (slotIndex !in 0 until SLOT_COUNT) return emptyList()
        val slot = slots[slotIndex]
        val toRemove = min(count, slot.size)
        val removed = ArrayList<GameUnit>(toRemove)
        repeat(toRemove) { removed.add(slot.removeAt(0)) }
        repositionSlotUnits(slotIndex)
        return removed
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

    /** 같은 유닛이 있는 슬롯 찾기 (소환 시 자동 중첩용) */
    fun findStackableSlot(blueprintId: String): Int {
        if (blueprintId.isEmpty()) return -1
        for (i in 0 until SLOT_COUNT) {
            val slot = slots[i]
            if (slot.isNotEmpty() && slot.first().blueprintId == blueprintId) {
                return i
            }
        }
        return -1
    }


    /** 전체 유닛 수 (모든 슬롯의 스택 합계) */
    fun unitCount(): Int = slots.sumOf { it.size }

    /** 사용 중인 슬롯 수 */
    fun occupiedSlotCount(): Int = slots.count { it.isNotEmpty() }

    // ── Circular formation for stacked units ──

    private fun setUnitPos(unit: GameUnit, x: Float, y: Float) {
        unit.position.x = x; unit.position.y = y
        unit.homePosition.x = x; unit.homePosition.y = y
    }

    /**
     * 슬롯 내 유닛들을 원형 포메이션으로 재배치.
     * - 1개: 중앙
     * - 2개: 좌우 수평 배치
     * - 3개: 삼각형
     * - 4~6개: 원형 배치
     * - 7+개: 6개만 원형, 나머지는 중앙에 겹침 (뱃지로 표시)
     */
    private fun repositionSlotUnits(slotIndex: Int) {
        val slot = slots[slotIndex]
        if (slot.isEmpty()) return
        val center = slotCenter(slotIndex)
        val cx = center.x
        val cy = center.y
        val n = slot.size
        val displayCount = min(n, MAX_VISUAL_UNITS)

        when (displayCount) {
            1 -> setUnitPos(slot[0], cx, cy)
            2 -> {
                val ox = CELL_W * 0.14f  // ~13px
                setUnitPos(slot[0], cx - ox, cy)
                setUnitPos(slot[1], cx + ox, cy)
            }
            3 -> {
                val ox = CELL_W * 0.14f
                val oy = CELL_H * 0.09f  // ~11px
                setUnitPos(slot[0], cx, cy - oy)
                setUnitPos(slot[1], cx - ox, cy + oy * 0.5f)
                setUnitPos(slot[2], cx + ox, cy + oy * 0.5f)
            }
            else -> {
                // 원형 배치: 반지름 R, 각 유닛을 2π/N 간격으로
                val r = CELL_W * 0.18f  // ~17px 반지름
                for (i in 0 until displayCount) {
                    val angle = (2.0 * Math.PI * i / displayCount - Math.PI / 2).toFloat()
                    setUnitPos(slot[i], cx + r * cos(angle), cy + r * sin(angle))
                }
            }
        }
        // 7+개의 나머지 유닛들은 중앙 위치 (렌더링 안 되지만 position 필요)
        for (i in displayCount until n) {
            setUnitPos(slot[i], cx, cy)
        }
    }
}
