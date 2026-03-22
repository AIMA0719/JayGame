package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2

class Grid {
    companion object {
        const val TOTAL = 100                       // max 100 units (no grid layout)
        const val GRID_W = 480f                     // field area width
        const val GRID_H = 480f                     // field area height
        private const val CLIFF_DEPTH = 25f
        const val ORIGIN_X = (720f - GRID_W) / 2f            // 120
        const val ORIGIN_Y = (720f - GRID_H - CLIFF_DEPTH) / 2f  // 107.5

        // Field center (spawn point)
        const val CENTER_X = ORIGIN_X + GRID_W / 2f   // 360
        const val CENTER_Y = ORIGIN_Y + GRID_H / 2f   // 347.5

        // Field bounds with margin (shared by GameUnit clamping & requestRelocate)
        const val FIELD_MIN_X = ORIGIN_X + 10f
        const val FIELD_MAX_X = ORIGIN_X + GRID_W - 10f
        const val FIELD_MIN_Y = ORIGIN_Y + 10f
        const val FIELD_MAX_Y = ORIGIN_Y + GRID_H - 10f

        /** Cached center Vec2 — avoid per-call allocation. Callers must .copy() if mutating. */
        val FIELD_CENTER = Vec2(CENTER_X, CENTER_Y)

        // Legacy compat — kept for path calculations & debug overlay
        @Deprecated("Grid is no longer cell-based") const val COLS = 5
        @Deprecated("Grid is no longer cell-based") const val ROWS = 5
        @Deprecated("Grid is no longer cell-based") const val CELL_W = 96f
        @Deprecated("Grid is no longer cell-based") const val CELL_H = 96f
    }

    val cells = arrayOfNulls<GameUnit>(TOTAL)

    fun placeUnit(index: Int, unit: GameUnit): Boolean {
        if (index !in 0 until TOTAL || cells[index] != null) return false
        cells[index] = unit
        unit.tileIndex = index
        return true
    }

    fun removeUnit(index: Int): GameUnit? {
        if (index !in 0 until TOTAL) return null
        val unit = cells[index] ?: return null
        cells[index] = null
        return unit
    }

    fun getUnit(index: Int): GameUnit? = cells.getOrNull(index)

    fun findEmpty(): Int {
        for (i in 0 until TOTAL) if (cells[i] == null) return i
        return -1
    }

    fun findMergeCandidatesByGrade(grade: Int): List<Int> {
        return (0 until TOTAL).filter { i ->
            val u = cells[i]
            u != null && u.grade == grade &&
                    u.unitCategory != UnitCategory.SPECIAL
        }
    }

    fun unitCount(): Int = cells.count { it != null }

    @Deprecated("Grid is no longer cell-based. Use FIELD_CENTER instead.")
    fun cellCenter(index: Int): Vec2 = FIELD_CENTER.copy()

    @Deprecated("Grid is no longer cell-based.")
    fun getCellAt(worldX: Float, worldY: Float): Int = -1
}
