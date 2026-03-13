package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2

class Grid {
    companion object {
        const val COLS = 5
        const val ROWS = 5
        const val TOTAL = COLS * ROWS          // 25
        const val CELL_W = 96f
        const val CELL_H = 96f
        const val GRID_W = COLS * CELL_W       // 480
        const val GRID_H = ROWS * CELL_H       // 480
        const val ORIGIN_X = (1280f - GRID_W) / 2f  // 400
        const val ORIGIN_Y = (720f - GRID_H) / 2f   // 120
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
