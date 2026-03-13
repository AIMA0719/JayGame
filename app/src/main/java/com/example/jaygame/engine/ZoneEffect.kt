package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2

/**
 * Persistent area-of-effect zone placed on the battlefield.
 * Deals tick damage and/or applies debuffs to enemies within radius.
 */
class ZoneEffect {
    var alive = false
    var position = Vec2()
    var radius = 60f
    var duration = 0f
    var maxDuration = 0f
    var tickInterval = 1f      // seconds between ticks
    var tickDamage = 0f        // damage per tick
    var slowPercent = 0f       // 0-1, movement slow applied
    var defReduction = 0f      // flat defense reduction
    var family = 0             // for VFX color
    var sourceGrade = 0

    private var tickTimer = 0f

    fun init(
        pos: Vec2, radius: Float, duration: Float,
        tickInterval: Float, tickDamage: Float,
        slowPercent: Float = 0f, defReduction: Float = 0f,
        family: Int = 0, grade: Int = 0,
    ) {
        this.alive = true
        this.position = pos.copy()
        this.radius = radius
        this.duration = duration
        this.maxDuration = duration
        this.tickInterval = tickInterval
        this.tickDamage = tickDamage
        this.slowPercent = slowPercent
        this.defReduction = defReduction
        this.family = family
        this.sourceGrade = grade
        this.tickTimer = 0f
    }

    /**
     * Update zone and apply effects to enemies within radius.
     * Returns false when zone expires.
     */
    fun update(dt: Float, enemies: List<Enemy>): Boolean {
        if (!alive) return false
        duration -= dt
        if (duration <= 0f) {
            alive = false
            return false
        }

        tickTimer -= dt
        if (tickTimer <= 0f) {
            tickTimer += tickInterval
            // Apply effects to all enemies in radius
            for (enemy in enemies) {
                if (!enemy.alive) continue
                val dx = enemy.position.x - position.x
                val dy = enemy.position.y - position.y
                if (dx * dx + dy * dy <= radius * radius) {
                    if (tickDamage > 0f) {
                        enemy.takeDamage(tickDamage)
                    }
                    if (slowPercent > 0f) {
                        enemy.buffs.addBuff(BuffType.Slow, slowPercent, tickInterval + 0.5f)
                    }
                }
            }
        }
        return true
    }

    fun reset() {
        alive = false
    }
}
