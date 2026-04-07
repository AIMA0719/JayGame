package com.jay.jaygame.engine.math

import kotlin.math.sqrt

data class Vec2(var x: Float = 0f, var y: Float = 0f) {
    val length: Float get() = sqrt(x * x + y * y)
    val lengthSq: Float get() = x * x + y * y

    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)

    fun dot(o: Vec2) = x * o.x + y * o.y

    fun normalized(): Vec2 {
        val len = length
        return if (len > 0.0001f) Vec2(x / len, y / len) else Vec2()
    }

    fun distanceTo(o: Vec2): Float {
        val dx = x - o.x
        val dy = y - o.y
        return sqrt(dx * dx + dy * dy)
    }
    fun distanceSqTo(o: Vec2): Float {
        val dx = x - o.x
        val dy = y - o.y
        return dx * dx + dy * dy
    }

    /** Mutate this Vec2 to match another — zero allocation alternative to copy(). */
    fun set(other: Vec2) { x = other.x; y = other.y }
    fun set(nx: Float, ny: Float) { x = nx; y = ny }

    companion object {
        fun lerp(a: Vec2, b: Vec2, t: Float) = Vec2(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
        )
    }
}
