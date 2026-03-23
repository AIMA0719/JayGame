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

    /** Blocking support — set when a GameUnit is blocking this enemy */
    var blockedBy: GameUnit? = null
    var blockedPosition: Vec2? = null

    fun applyBlock(blocker: GameUnit) {
        blockedBy = blocker
        blockedPosition = position.copy()
    }

    fun releaseBlock() {
        blockedBy = null
        blockedPosition = null
    }

    /** Convenience property: true when bossModifier is non-null */
    val isBoss: Boolean get() = bossModifier != null

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

        // Blocked by a unit: skip path movement entirely
        if (blockedBy != null) return true

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

    /** SHIELDED boss: shield blocks all damage, cycles 5s on / 3s off */
    var shieldTimer = 0f
    var shieldActive = false

    fun takeDamage(damage: Float, isMagic: Boolean = false, attackRange: Float = 0f): Float {
        // SHIELDED: completely block damage during shield phase
        if (bossModifier == BossModifier.SHIELDED && shieldActive) return 0f

        var adjustedDamage = damage
        // Boss modifier: reduce physical/magic/ranged damage
        when (bossModifier) {
            BossModifier.PHYSICAL_RESIST -> if (!isMagic) adjustedDamage *= 0.4f
            BossModifier.MAGIC_RESIST -> if (isMagic) adjustedDamage *= 0.4f
            BossModifier.RANGED_RESIST -> if (attackRange > 200f) adjustedDamage *= 0.5f
            // BERSERKER: takes 15% more damage as trade-off
            BossModifier.BERSERKER -> if (hpRatio < 0.5f) adjustedDamage *= 1.15f
            else -> {}
        }
        val effectiveArmor = (armor - buffs.getArmorReduction()).coerceAtLeast(0f)
        val reduction = if (isMagic) {
            1f - (magicResist / (magicResist + 100f))
        } else {
            1f - (effectiveArmor / (effectiveArmor + 100f))
        }
        val finalDmg = buffs.absorbDamage(adjustedDamage * reduction)
        hp -= finalDmg

        // VAMPIRIC: heal 20% of damage dealt to player
        if (bossModifier == BossModifier.VAMPIRIC) {
            hp = (hp + finalDmg * 0.2f).coerceAtMost(maxHp)
        }

        if (hp <= 0f) alive = false
        return finalDmg
    }

    /** Apply CC/DoT immune flags to buff container based on assigned bossModifier */
    fun applyBossModifierFlags() {
        buffs.ccImmune = bossModifier == BossModifier.CC_IMMUNE
        buffs.dotImmune = bossModifier == BossModifier.DOT_IMMUNE
    }

    fun reset() {
        alive = false
        buffs.clear()
        recentHitFlags = 0
        recentHitTimer = 0f
        blockedBy = null
        blockedPosition = null
        bossModifier = null
        regenTimer = 10f
        ccResistance = 0f
        shieldTimer = 0f
        shieldActive = false
        berserkerActivated = false
        splitterTriggered = false
    }

    /** BERSERKER: track if speed boost was applied */
    var berserkerActivated = false

    /** SPLITTER: track if split already happened */
    var splitterTriggered = false

    val hpRatio: Float get() = if (maxHp > 0f) (hp / maxHp).coerceIn(0f, 1f) else 0f
    val size: Float get() = when (type) {
        4, 5 -> 96f
        6 -> 64f
        else -> 48f
    }
}
