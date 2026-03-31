package com.jay.jaygame.engine

import com.jay.jaygame.engine.math.GameRect

class SpatialHash<T>(private val cellSize: Float = 64f) {
    private val cells = HashMap<Long, MutableList<T>>()

    fun clear() = cells.clear()

    fun insert(item: T, x: Float, y: Float, w: Float, h: Float) {
        val minCX = (x / cellSize).toInt()
        val minCY = (y / cellSize).toInt()
        val maxCX = ((x + w) / cellSize).toInt()
        val maxCY = ((y + h) / cellSize).toInt()
        for (cy in minCY..maxCY) {
            for (cx in minCX..maxCX) {
                val key = (cx.toLong() shl 32) or (cy.toLong() and 0xFFFFFFFFL)
                cells.getOrPut(key) { mutableListOf() }.add(item)
            }
        }
    }

    fun query(rect: GameRect): List<T> {
        val result = mutableListOf<T>()
        val seen = HashSet<T>()
        val minCX = (rect.x / cellSize).toInt()
        val minCY = (rect.y / cellSize).toInt()
        val maxCX = (rect.right / cellSize).toInt()
        val maxCY = (rect.bottom / cellSize).toInt()
        for (cy in minCY..maxCY) {
            for (cx in minCX..maxCX) {
                val key = (cx.toLong() shl 32) or (cy.toLong() and 0xFFFFFFFFL)
                cells[key]?.forEach { if (seen.add(it)) result.add(it) }
            }
        }
        return result
    }
}
