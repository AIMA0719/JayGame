package com.example.jaygame.engine

import java.util.IdentityHashMap

class ObjectPool<T>(
    private val capacity: Int,
    private val factory: () -> T,
    private val reset: (T) -> Unit = {},
) {
    @PublishedApi internal val objects = ArrayList<T>(capacity)
    @PublishedApi internal val active = BooleanArray(capacity)
    private val freeList = ArrayDeque<Int>()
    private val indexMap = IdentityHashMap<T, Int>(capacity)
    var activeCount = 0; private set

    init {
        repeat(capacity) { i ->
            val obj = factory()
            objects.add(obj)
            freeList.addLast(i)
            indexMap[obj] = i
        }
    }

    fun acquire(): T? {
        if (freeList.isEmpty()) return null
        val idx = freeList.removeFirst()
        active[idx] = true
        activeCount++
        reset(objects[idx])
        return objects[idx]
    }

    fun release(item: T) {
        val idx = indexMap[item] ?: return
        if (active[idx]) {
            active[idx] = false
            activeCount--
            freeList.addLast(idx)
        }
    }

    inline fun forEach(action: (T) -> Unit) {
        for (i in objects.indices) {
            if (active[i]) action(objects[i])
        }
    }

    fun toActiveList(): List<T> = objects.filterIndexed { i, _ -> active[i] }

    fun fillActiveList(out: MutableList<T>) {
        out.clear()
        for (i in objects.indices) {
            if (i < active.size && active[i]) out.add(objects[i])
        }
    }
}
