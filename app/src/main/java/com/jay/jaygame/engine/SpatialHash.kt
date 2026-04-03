package com.jay.jaygame.engine

import com.jay.jaygame.engine.math.GameRect

/**
 * Zero-allocation spatial hash using pre-allocated 2D grid arrays.
 * Designed for 720×1280 game field with configurable cell size.
 */
class SpatialHash<T>(private val cellSize: Float = 64f) {
    // Grid dimensions: covers 0..799 x 0..1399 with some margin
    private val gridW = (800f / cellSize).toInt() + 1   // 13
    private val gridH = (1400f / cellSize).toInt() + 1   // 22
    private val maxPerCell = 64

    // Flat storage: each cell holds up to maxPerCell item indices
    private val cellItems = Array(gridW * gridH) { IntArray(maxPerCell) }
    private val cellCounts = IntArray(gridW * gridH)

    // Item registry: maps items to internal indices for dedup
    @Suppress("UNCHECKED_CAST")
    private val items = arrayOfNulls<Any>(512) as Array<T?>
    private var itemCount = 0

    // Generation counter for O(1) dedup reset
    private var generation = 0
    private val seenGen = IntArray(512)

    fun clear() {
        for (i in cellCounts.indices) cellCounts[i] = 0
        itemCount = 0
    }

    fun insert(item: T, x: Float, y: Float, w: Float, h: Float) {
        val idx = itemCount++
        if (idx < items.size) items[idx] = item

        val minCX = (x / cellSize).toInt().coerceIn(0, gridW - 1)
        val minCY = (y / cellSize).toInt().coerceIn(0, gridH - 1)
        val maxCX = ((x + w) / cellSize).toInt().coerceIn(0, gridW - 1)
        val maxCY = ((y + h) / cellSize).toInt().coerceIn(0, gridH - 1)
        for (cy in minCY..maxCY) {
            for (cx in minCX..maxCX) {
                val ci = cy * gridW + cx
                val cnt = cellCounts[ci]
                if (cnt < maxPerCell) {
                    cellItems[ci][cnt] = idx
                    cellCounts[ci] = cnt + 1
                }
            }
        }
    }

    fun query(rect: GameRect): List<T> = query(rect.x, rect.y, rect.right, rect.bottom)

    fun query(left: Float, top: Float, right: Float, bottom: Float): List<T> {
        val result = mutableListOf<T>()
        forEach(left, top, right, bottom) { result.add(it) }
        return result
    }

    /**
     * Zero-allocation forEach using generation-counter dedup.
     * SpatialHash is single-threaded (battle thread only).
     */
    fun forEach(left: Float, top: Float, right: Float, bottom: Float, action: (T) -> Unit) {
        val gen = ++generation

        val minCX = (left / cellSize).toInt().coerceIn(0, gridW - 1)
        val minCY = (top / cellSize).toInt().coerceIn(0, gridH - 1)
        val maxCX = (right / cellSize).toInt().coerceIn(0, gridW - 1)
        val maxCY = (bottom / cellSize).toInt().coerceIn(0, gridH - 1)
        for (cy in minCY..maxCY) {
            for (cx in minCX..maxCX) {
                val ci = cy * gridW + cx
                val cnt = cellCounts[ci]
                val arr = cellItems[ci]
                for (s in 0 until cnt) {
                    val idx = arr[s]
                    if (idx < seenGen.size && seenGen[idx] != gen) {
                        seenGen[idx] = gen
                        items[idx]?.let { action(it) }
                    }
                }
            }
        }
    }
}
