package com.jay.jaygame.engine

enum class BuffType { Slow, DoT, ArmorBreak, MagicResistBreak, AtkUp, SpdUp, Shield, Stun, Silence, DefUp }

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

    private var dirty = true
    private var cachedFlags = 0        // bit per BuffType ordinal
    private var cachedSlowFactor = 1f
    private var cachedArmorReduction = 0f
    private var cachedMagicResistReduction = 0f
    private var cachedAtkMult = 1f
    private var cachedSpdMult = 1f
    private var cachedDefMult = 1f

    private fun markDirty() { dirty = true }

    private fun recomputeIfDirty() {
        if (!dirty) return
        dirty = false
        var flags = 0
        var maxSlow = 0f
        var armorBreak = 0f
        var mrBreak = 0f
        var atkMult = 1f
        var spdMult = 1f
        var defMult = 1f
        for (i in buffs.indices) {
            val b = buffs[i]
            if (b.remaining <= 0f) continue
            flags = flags or (1 shl b.type.ordinal)
            when (b.type) {
                BuffType.Slow -> if (b.value > maxSlow) maxSlow = b.value
                BuffType.ArmorBreak -> armorBreak += b.value
                BuffType.MagicResistBreak -> mrBreak += b.value
                BuffType.AtkUp -> atkMult += b.value
                BuffType.SpdUp -> spdMult += b.value
                BuffType.DefUp -> defMult += b.value
                else -> {}
            }
        }
        cachedFlags = flags
        cachedSlowFactor = 1f - maxSlow.coerceIn(0f, 0.8f)
        cachedArmorReduction = armorBreak
        cachedMagicResistReduction = mrBreak
        cachedAtkMult = atkMult
        cachedSpdMult = spdMult
        cachedDefMult = defMult
    }

    fun addBuff(type: BuffType, value: Float, duration: Float, sourceId: Int = -1) {
        // Boss modifier immunity checks
        if (ccImmune && (type == BuffType.Slow || type == BuffType.Stun || type == BuffType.Silence)) return
        if (dotImmune && type == BuffType.DoT) return

        val effectiveDuration = if (type == BuffType.Slow || type == BuffType.Stun || type == BuffType.Silence) {
            duration * (1f - ccResistance)
        } else duration
        var count = 0
        var oldest: BuffEntry? = null
        for (i in buffs.indices) {
            val b = buffs[i]
            if (b.type == type) {
                count++
                if (oldest == null || b.remaining < oldest!!.remaining) oldest = b
            }
        }
        if (count >= 3) oldest?.let {
            if (it.type == BuffType.Shield) shieldHP = (shieldHP - it.value).coerceAtLeast(0f)
            buffs.remove(it)
        }
        buffs.add(BuffEntry(type, value, effectiveDuration, sourceId))
        if (type == BuffType.Shield) shieldHP += value
        markDirty()
    }

    fun countBuff(type: BuffType): Int {
        var count = 0
        for (i in buffs.indices) {
            val b = buffs[i]
            if (b.type == type && b.remaining > 0f) count++
        }
        return count
    }

    fun isStunned(): Boolean {
        recomputeIfDirty()
        return cachedFlags and (1 shl BuffType.Stun.ordinal) != 0
    }

    fun isSilenced(): Boolean {
        recomputeIfDirty()
        return cachedFlags and (1 shl BuffType.Silence.ordinal) != 0
    }

    fun update(dt: Float): Float {
        var dotDamage = 0f
        var removed = false
        for (i in buffs.lastIndex downTo 0) {
            val b = buffs[i]
            b.remaining -= dt
            if (b.remaining <= 0f) {
                if (b.type == BuffType.Shield) shieldHP = (shieldHP - b.value).coerceAtLeast(0f)
                buffs.removeAt(i)
                removed = true
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
        if (removed) markDirty()
        return dotDamage
    }

    fun getSlowFactor(): Float {
        recomputeIfDirty()
        return cachedSlowFactor
    }

    fun getArmorReduction(): Float {
        recomputeIfDirty()
        return cachedArmorReduction
    }

    fun getMagicResistReduction(): Float {
        recomputeIfDirty()
        return cachedMagicResistReduction
    }

    fun getAtkMultiplier(): Float {
        recomputeIfDirty()
        return cachedAtkMult
    }

    fun getSpdMultiplier(): Float {
        recomputeIfDirty()
        return cachedSpdMult
    }

    fun getDefMultiplier(): Float {
        recomputeIfDirty()
        return cachedDefMult
    }

    fun absorbDamage(damage: Float): Float {
        if (shieldHP <= 0f) return damage
        val absorbed = minOf(shieldHP, damage)
        shieldHP -= absorbed
        if (shieldHP <= 0f) {
            // PERF: reverse-index removal instead of removeAll
            for (i in buffs.lastIndex downTo 0) {
                if (buffs[i].type == BuffType.Shield) buffs.removeAt(i)
            }
            markDirty()
        }
        return damage - absorbed
    }

    fun hasBuff(type: BuffType): Boolean {
        recomputeIfDirty()
        return cachedFlags and (1 shl type.ordinal) != 0
    }

    fun clear() {
        buffs.clear()
        shieldHP = 0f
        ccImmune = false
        dotImmune = false
        markDirty()
    }
}
