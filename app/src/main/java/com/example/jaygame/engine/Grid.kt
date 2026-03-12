package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2

class Grid {
    companion object {
        const val COLS = 6
        const val ROWS = 5
        const val TOTAL = 30
        const val CELL_W = 140f
        const val CELL_H = 88f
        const val ORIGIN_X = 200f
        const val ORIGIN_Y = 140f
    }

    val cells = arrayOfNulls<GameUnit>(TOTAL)

    fun placeUnit(index: Int, unit: GameUnit): Boolean {
        if (index !in 0 until TOTAL || cells[index] != null) return false
        cells[index] = unit
        unit.tileIndex = index
        unit.homePosition = cellCenter(index)
        unit.position = unit.homePosition.copy()
        return true
    }

    fun removeUnit(index: Int): GameUnit? {
        val unit = cells[index] ?: return null
        cells[index] = null
        return unit
    }

    fun getUnit(index: Int): GameUnit? = cells.getOrNull(index)

    fun findEmpty(): Int {
        for (i in 0 until TOTAL) if (cells[i] == null) return i
        return -1
    }

    fun isFull() = cells.all { it != null }

    fun cellCenter(index: Int): Vec2 {
        val col = index % COLS
        val row = index / COLS
        return Vec2(
            ORIGIN_X + col * CELL_W + CELL_W * 0.5f,
            ORIGIN_Y + row * CELL_H + CELL_H * 0.5f,
        )
    }

    fun getCellAt(worldX: Float, worldY: Float): Int {
        val col = ((worldX - ORIGIN_X) / CELL_W).toInt()
        val row = ((worldY - ORIGIN_Y) / CELL_H).toInt()
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) return -1
        return row * COLS + col
    }

    fun findMergeCandidates(unitDefId: Int, grade: Int): List<Int> {
        return (0 until TOTAL).filter { i ->
            val u = cells[i]
            u != null && u.unitDefId == unitDefId && u.grade == grade
        }
    }

    fun unitCount(): Int = cells.count { it != null }
}
