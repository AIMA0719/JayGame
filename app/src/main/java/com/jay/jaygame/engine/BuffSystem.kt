package com.jay.jaygame.engine

enum class BuffType { Slow, DoT, ArmorBreak, AtkUp, SpdUp, Shield, Stun, Silence, DefUp }

data class BuffEntry(
    val type: BuffType,
    var value: Float,
    var remaining: Float,
    var sourceId: Int = -1,
    var tickTimer: Float = 0f,
)

class BuffContainer {
    val buffs = mutableListOf<BuffEntry>()
    private var shieldHP = 0f

    var ccResistance: Float = 0f  // 0.0 ~ 0.9
    /** CC_IMMUNE boss modifier: skips Slow and Stun buffs */
    var ccImmune: Boolean = false
    /** DOT_IMMUNE boss modifier: skips DoT buffs */
    var dotImmune: Boolean = false

    fun addBuff(type: BuffType, value: Float, duration: Float, sourceId: Int = -1) {
        // Boss modifier immunity checks
        if (ccImmune && (type == BuffType.Slow || type == BuffType.Stun || type == BuffType.Silence)) return
        if (dotImmune && type == BuffType.DoT) return

        val effectiveDuration = if (type == BuffType.Slow || type == BuffType.Stun || type == BuffType.Silence) {
            duration * (1f - ccResistance)
        } else duration
        val existing = buffs.filter { it.type == type }
        if (existing.size >= 3) {
            existing.minByOrNull { it.remaining }?.let { buffs.remove(it) }
        }
        buffs.add(BuffEntry(type, value, effectiveDuration, sourceId))
        if (type == BuffType.Shield) shieldHP += value
    }

    fun countBuff(type: BuffType): Int = buffs.count { it.type == type && it.remaining > 0f }
    fun isStunned(): Boolean = buffs.any { it.type == BuffType.Stun && it.remaining > 0f }
    fun isSilenced(): Boolean = buffs.any { it.type == BuffType.Silence && it.remaining > 0f }

    fun update(dt: Float): Float {
        var dotDamage = 0f
        val iter = buffs.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.remaining -= dt
            if (b.remaining <= 0f) {
                iter.remove()
                continue
            }
            if (b.type == BuffType.DoT) {
                b.tickTimer += dt
                if (b.tickTimer >= 0.5f) {
                    b.tickTimer -= 0.5f
                    dotDamage += b.value
                }
            }
        }
        return dotDamage
    }

    fun getSlowFactor(): Float {
        var maxSlow = 0f
        buffs.forEach { if (it.type == BuffType.Slow) maxSlow = maxOf(maxSlow, it.value) }
        return 1f - maxSlow.coerceIn(0f, 0.8f)
    }

    fun getArmorReduction(): Float {
        var total = 0f
        buffs.forEach { if (it.type == BuffType.ArmorBreak) total += it.value }
        return total
    }

    fun getAtkMultiplier(): Float {
        var mult = 1f
        buffs.forEach { if (it.type == BuffType.AtkUp) mult += it.value }
        return mult
    }

    fun getSpdMultiplier(): Float {
        var mult = 1f
        buffs.forEach { if (it.type == BuffType.SpdUp) mult += it.value }
        return mult
    }

    fun getDefMultiplier(): Float {
        var mult = 1f
        buffs.forEach { if (it.type == BuffType.DefUp) mult += it.value }
        return mult
    }

    fun absorbDamage(damage: Float): Float {
        if (shieldHP <= 0f) return damage
        val absorbed = minOf(shieldHP, damage)
        shieldHP -= absorbed
        if (shieldHP <= 0f) {
            buffs.removeAll { it.type == BuffType.Shield }
        }
        return damage - absorbed
    }

    fun clear() {
        buffs.clear()
        shieldHP = 0f
        ccImmune = false
        dotImmune = false
    }

    fun hasBuff(type: BuffType) = buffs.any { it.type == type }
}
