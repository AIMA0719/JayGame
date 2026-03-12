package com.example.jaygame.engine.math

data class GameRect(val x: Float, val y: Float, val w: Float, val h: Float) {
    val centerX: Float get() = x + w * 0.5f
    val centerY: Float get() = y + h * 0.5f
    val right: Float get() = x + w
    val bottom: Float get() = y + h

    fun contains(px: Float, py: Float) =
        px >= x && px < x + w && py >= y && py < y + h

    fun intersects(o: GameRect) =
        x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y
}
