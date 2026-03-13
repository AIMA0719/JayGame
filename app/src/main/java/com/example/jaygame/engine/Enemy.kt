package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2

class Enemy {
    var alive = false
    var position = Vec2()
    var hp = 0f
    var maxHp = 0f
    var speed = 0f
    var baseSpeed = 0f
    var armor = 0f
    var magicResist = 0f
    var type = 0
    var pathIndex = 0
    val buffs = BuffContainer()
    /** Bitmask for recent hit effects: bit0=lightning, bit1=wind. Auto-decays. */
    var recentHitFlags = 0
    var recentHitTimer = 0f

    /** Boss modifier — null for non-boss enemies */
    var bossModifier: BossModifier? = null
    /** Timer for REGENERATION modifier: heals 5% maxHp every 10 seconds */
    var regenTimer: Float = 10f
    /** CC resistance: 0.0 ~ 0.9 — reduces duration of Slow/Stun/Silence */
    var ccResistance: Float = 0f

    fun init(
        hp: Float, speed: Float, armor: Float, magicResist: Float,
        type: Int, startPos: Vec2,
        ccResistance: Float = 0f,
    ) {
        this.alive = true
        this.hp = hp
        this.maxHp = hp
        this.speed = speed
        this.baseSpeed = speed
        this.armor = armor
        this.magicResist = magicResist
        this.type = type
        this.position = startPos.copy()
        this.pathIndex = 0
        this.ccResistance = ccResistance
        this.buffs.ccResistance = ccResistance
        this.buffs.clear()
        this.recentHitFlags = 0
        this.recentHitTimer = 0f
    }

    fun update(dt: Float, path: List<Vec2>): Boolean {
        if (!alive) return false

        // Decay recent hit flags
        if (recentHitFlags != 0) {
            recentHitTimer -= dt
            if (recentHitTimer <= 0f) {
                recentHitFlags = 0
            }
        }

        val dotDmg = buffs.update(dt)
        if (dotDmg > 0f) {
            hp -= dotDmg
            if (hp <= 0f) { alive = false; return false }
        }

        // Stun: skip movement and boss abilities
        if (buffs.isStunned()) return true

        val effectiveSpeed = baseSpeed * buffs.getSlowFactor()
        if (pathIndex < path.size) {
            val target = path[pathIndex]
            val dir = target - position
            val dist = dir.length
            val step = effectiveSpeed * dt

            if (dist <= step) {
                position = target.copy()
                pathIndex++
                if (pathIndex >= path.size) {
                    pathIndex = 0
                }
            } else {
                val norm = dir.normalized()
                position.x += norm.x * step
                position.y += norm.y * step
            }
        }
        return true
    }

    fun takeDamage(damage: Float, isMagic: Boolean = false): Float {
        val effectiveArmor = (armor - buffs.getArmorReduction()).coerceAtLeast(0f)
        val reduction = if (isMagic) {
            1f - (magicResist / (magicResist + 100f))
        } else {
            1f - (effectiveArmor / (effectiveArmor + 100f))
        }
        val finalDmg = buffs.absorbDamage(damage * reduction)
        hp -= finalDmg
        if (hp <= 0f) alive = false
        return finalDmg
    }

    fun reset() {
        alive = false
        buffs.clear()
        recentHitFlags = 0
        recentHitTimer = 0f
        bossModifier = null
        regenTimer = 10f
        ccResistance = 0f
    }

    val hpRatio: Float get() = if (maxHp > 0f) (hp / maxHp).coerceIn(0f, 1f) else 0f
    val size: Float get() = if (type == 4 || type == 5) 96f else 48f
}
