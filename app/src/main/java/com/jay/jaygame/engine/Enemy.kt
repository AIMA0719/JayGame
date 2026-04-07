package com.jay.jaygame.engine

import com.jay.jaygame.engine.math.Vec2

class Enemy {
    var alive = false
    var position = Vec2()
    var hp = 0f
    var maxHp = 0f
    var speed = 0f
    var baseSpeed = 0f
    var armor = 0f
    var baseArmor = 0f
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
        blockedPosition = Vec2(position.x, position.y)
    }

    fun releaseBlock() {
        blockedBy = null
        blockedPosition = null
    }

    /** Convenience property: true when bossModifier is non-null */
    val isBoss: Boolean get() = bossModifier != null
    /** 엘리트 여부 — 스폰 시 설정 */
    var isElite: Boolean = false

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
        this.baseArmor = armor
        this.magicResist = magicResist
        this.type = type
        this.position.set(startPos)
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
            if (!isImmune()) {
                hp -= dotDmg
                if (hp <= 0f) { alive = false; return false }
            }
        }

        // Stun: skip movement and boss abilities
        if (buffs.isStunned()) return true

        // Blocked by a unit: skip path movement entirely
        if (blockedBy != null) return true

        val effectiveSpeed = speed * buffs.getSlowFactor()
        var remaining = effectiveSpeed * dt
        var safety = path.size + 1
        while (remaining > 0f && pathIndex < path.size && --safety > 0) {
            val target = path[pathIndex]
            val dx = target.x - position.x
            val dy = target.y - position.y
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)

            if (dist <= remaining) {
                position.x = target.x
                position.y = target.y
                remaining -= dist
                pathIndex++
                if (pathIndex >= path.size) {
                    pathIndex = 0
                }
            } else {
                val ratio = remaining / dist
                position.x += dx * ratio
                position.y += dy * ratio
                remaining = 0f
            }
        }
        return true
    }

    /** SHIELDED boss: shield blocks all damage, cycles 5s on / 3s off */
    var shieldTimer = 0f
    var shieldActive = false

    fun takeDamage(damage: Float, isMagic: Boolean = false, attackRange: Float = 0f): Float {
        if (isImmune()) return 0f

        var adjustedDamage = damage
        // 데미지 감소 적용 (DUAL_MOD면 양쪽 모두 체크)
        adjustedDamage = applyModifierDamageReduction(adjustedDamage, isMagic, attackRange)
        val reduction = if (isMagic) {
            val effectiveMR = (magicResist - buffs.getMagicResistReduction()).coerceAtLeast(0f)
            1f - (effectiveMR / (effectiveMR + 100f))
        } else {
            val effectiveArmor = (armor - buffs.getArmorReduction()).coerceAtLeast(0f)
            1f - (effectiveArmor / (effectiveArmor + 100f))
        }
        val finalDmg = buffs.absorbDamage(adjustedDamage * reduction)
        hp -= finalDmg

        // VAMPIRIC: heal 20% of damage dealt to player
        if (hasModifier(BossModifier.VAMPIRIC)) {
            hp = (hp + finalDmg * 0.2f).coerceAtMost(maxHp)
        }

        // MIRROR: 반사 데미지 기록 (실제 유닛 디버프는 BattleEngine에서 처리)
        if (hasModifier(BossModifier.MIRROR) && mirrorCooldown <= 0f) {
            mirrorCooldown = 0.5f
            lastMirrorDamage = finalDmg * 0.15f
        }

        if (hp <= 0f) {
            alive = false
        }
        return finalDmg
    }

    /** MIRROR: 마지막 반사 데미지 (BattleEngine이 읽고 0으로 리셋) */
    var lastMirrorDamage = 0f

    /** DUAL_MOD: 실제 적용되는 기믹 2개 (BattleEngine이 스폰 시 설정) */
    var dualModFirst: BossModifier? = null
    var dualModSecond: BossModifier? = null

    /** 보스 면역 상태인지 (SHIELDED 보호막 / PHANTOM 투명) */
    fun isImmune(): Boolean =
        (hasModifier(BossModifier.SHIELDED) && shieldActive) ||
        (hasModifier(BossModifier.PHANTOM) && phantomActive)

    /** 이 적이 특정 기믹을 가지고 있는지 확인 (DUAL_MOD 양쪽 모두 체크) */
    fun hasModifier(mod: BossModifier): Boolean {
        if (bossModifier == mod) return true
        if (bossModifier == BossModifier.DUAL_MOD) {
            return dualModFirst == mod || dualModSecond == mod
        }
        return false
    }

    /** 단일 기믹의 데미지 감소 적용 (할당 없음) */
    private fun applySingleModDamageReduction(d: Float, mod: BossModifier, originalDamage: Float, isMagic: Boolean, attackRange: Float): Float {
        return when (mod) {
            BossModifier.PHYSICAL_RESIST -> if (!isMagic) d * 0.4f else d
            BossModifier.MAGIC_RESIST -> if (isMagic) d * 0.4f else d
            BossModifier.RANGED_RESIST -> if (attackRange > 200f) d * 0.5f else d
            BossModifier.BERSERKER -> if (hpRatio < 0.5f) d * 1.15f else d
            BossModifier.ADAPTIVE -> {
                if (isMagic) adaptiveMagicDmg += originalDamage else adaptivePhysicalDmg += originalDamage
                if (adaptiveResistPhysical && !isMagic) d * 0.6f
                else if (!adaptiveResistPhysical && isMagic) d * 0.6f
                else d
            }
            else -> d
        }
    }

    /** 기믹별 데미지 감소 적용 (GC-free: 리스트 할당 없음) */
    private fun applyModifierDamageReduction(damage: Float, isMagic: Boolean, attackRange: Float): Float {
        var d = damage
        if (bossModifier == BossModifier.DUAL_MOD) {
            dualModFirst?.let { d = applySingleModDamageReduction(d, it, damage, isMagic, attackRange) }
            dualModSecond?.let { d = applySingleModDamageReduction(d, it, damage, isMagic, attackRange) }
        } else {
            bossModifier?.let { d = applySingleModDamageReduction(d, it, damage, isMagic, attackRange) }
        }
        return d
    }

    /** Apply CC/DoT immune flags to buff container based on assigned bossModifier */
    fun applyBossModifierFlags() {
        buffs.ccImmune = bossModifier == BossModifier.CC_IMMUNE
        buffs.dotImmune = bossModifier == BossModifier.DOT_IMMUNE
    }

    fun reset() {
        alive = false
        isElite = false
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
        phantomTimer = 0f
        phantomActive = false
        mirrorCooldown = 0f
        adaptivePhysicalDmg = 0f
        adaptiveMagicDmg = 0f
        adaptiveCheckTimer = 5f
        adaptiveResistPhysical = false
        lastMirrorDamage = 0f
        dualModFirst = null
        dualModSecond = null
    }

    /** BERSERKER: track if speed boost was applied */
    var berserkerActivated = false

    /** SPLITTER: track if split already happened */
    var splitterTriggered = false

    // ── PHANTOM 기믹 필드 ──
    var phantomTimer = 0f       // 3초 visible → 1초 invisible 사이클
    var phantomActive = false   // true일 때 투명 (데미지 면역 + 타겟팅 불가)

    // ── MIRROR 기믹 필드 ──
    var mirrorCooldown = 0f     // 반사 쿨타임 (연속 반사 방지)

    // ── ADAPTIVE 기믹 필드 ──
    var adaptivePhysicalDmg = 0f  // 누적 물리 피해
    var adaptiveMagicDmg = 0f     // 누적 마법 피해
    var adaptiveCheckTimer = 5f   // 5초마다 적응 갱신
    var adaptiveResistPhysical = false // true면 물리 저항 +40%

    val hpRatio: Float get() = if (maxHp > 0f) (hp / maxHp).coerceIn(0f, 1f) else 0f
    val size: Float get() = when (type) {
        4, 5 -> 163f
        6 -> 109f
        else -> 82f
    }
}
