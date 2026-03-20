package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2

class Projectile {
    var alive = false
    var position = Vec2()
    var target: Enemy? = null
    var speed = 400f
    var damage = 0f
    var isMagic = false
    var isCrit = false
    var type = 0
    var sourceUnitId = -1
    var abilityType = 0
    var abilityValue = 0f
    var lifetime = 3f
    var grade = 0
    var family = 0
    var sourcePos = Vec2()
    /** Attacker's base range — used by RANGED_RESIST boss modifier */
    var attackerRange = 0f

    fun init(
        from: Vec2, target: Enemy, damage: Float, speed: Float,
        type: Int, isMagic: Boolean, isCrit: Boolean,
        sourceUnitId: Int, abilityType: Int, abilityValue: Float,
        grade: Int, family: Int,
        attackerRange: Float = 0f,
    ) {
        this.alive = true
        this.position = from.copy()
        this.sourcePos = from.copy()
        this.target = target
        this.damage = damage
        this.speed = speed
        this.type = type
        this.isMagic = isMagic
        this.isCrit = isCrit
        this.sourceUnitId = sourceUnitId
        this.abilityType = abilityType
        this.abilityValue = abilityValue
        this.lifetime = 3f
        this.grade = grade
        this.family = family
        this.attackerRange = attackerRange
    }

    fun update(dt: Float): Boolean {
        if (!alive) return false
        lifetime -= dt
        val t = target
        if (lifetime <= 0f || t == null || !t.alive) {
            alive = false
            return false
        }

        val dir = t.position - position
        val dist = dir.length
        val step = speed * dt

        if (dist <= step + t.size * 0.5f) {
            alive = false
            return false
        }

        val norm = dir.normalized()
        position.x += norm.x * step
        position.y += norm.y * step
        return true
    }

    fun reset() {
        alive = false
        target = null
    }
}
