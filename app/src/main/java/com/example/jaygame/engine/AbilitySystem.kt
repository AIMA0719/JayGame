package com.example.jaygame.engine

import com.example.jaygame.engine.math.GameRect
import com.example.jaygame.engine.math.Vec2

object AbilitySystem {
    private const val AURA_RADIUS = 150f
    private const val AURA_TICK = 0.5f

    /** Current field-based synergy — refreshed by BattleEngine when units change */
    var activeSynergy: SynergySystem.SynergyBonus = SynergySystem.SynergyBonus()

    fun onProjectileHit(
        proj: Projectile,
        enemy: Enemy,
        spatialHash: SpatialHash<Enemy>,
        spawnProjectile: (from: Vec2, target: Enemy, damage: Float, type: Int) -> Unit,
    ) {
        enemy.takeDamage(proj.damage, proj.isMagic, proj.attackerRange)

        // Get synergy for this projectile's family
        val synergy = activeSynergy

        when (proj.abilityType) {
            1 -> { // Splash (Fire)
                // Fire DoT — duration boosted by FIRE_BURN_EXTEND synergy
                val dotDuration = if (synergy.specialEffect == SynergySystem.SpecialEffect.FIRE_BURN_EXTEND) 3f else 2f
                enemy.buffs.addBuff(BuffType.DoT, proj.damage * 0.1f, dotDuration, proj.sourceUnitId)
                val splashRadius = proj.abilityValue.coerceAtLeast(60f)
                val rect = GameRect(
                    enemy.position.x - splashRadius,
                    enemy.position.y - splashRadius,
                    splashRadius * 2, splashRadius * 2,
                )
                spatialHash.query(rect).forEach { nearby ->
                    if (nearby !== enemy && nearby.alive) {
                        val dist = nearby.position.distanceTo(enemy.position)
                        if (dist <= splashRadius) {
                            nearby.takeDamage(proj.damage * 0.5f, proj.isMagic)
                        }
                    }
                }
            }
            2 -> { // Slow
                // FROST_SLOW_BOOST: +30% slow strength
                val slowValue = if (synergy.specialEffect == SynergySystem.SpecialEffect.FROST_SLOW_BOOST) {
                    proj.abilityValue * 1.3f
                } else proj.abilityValue
                enemy.buffs.addBuff(BuffType.Slow, slowValue, 2f, proj.sourceUnitId)
            }
            3 -> { // DoT (Poison)
                enemy.buffs.addBuff(BuffType.DoT, proj.abilityValue, 3f, proj.sourceUnitId)
                // POISON_SPREAD: on kill, spread poison to nearby enemies
                // (handled in BattleEngine's enemy death loop)
            }
            4 -> { // Chain (Lightning)
                enemy.recentHitFlags = enemy.recentHitFlags or 1
                enemy.recentHitTimer = 0.5f
                // LIGHTNING_CHAIN_EXTRA: +1 chain target
                val extraChain = if (synergy.specialEffect == SynergySystem.SpecialEffect.LIGHTNING_CHAIN_EXTRA) 1 else 0
                val chainCount = (proj.abilityValue.toInt() + extraChain).coerceIn(1, 6)
                val rect = GameRect(
                    enemy.position.x - 200f, enemy.position.y - 200f, 400f, 400f,
                )
                var chained = 0
                spatialHash.query(rect).forEach { nearby ->
                    if (chained < chainCount && nearby !== enemy && nearby.alive) {
                        nearby.recentHitFlags = nearby.recentHitFlags or 1
                        nearby.recentHitTimer = 0.5f
                        spawnProjectile(enemy.position, nearby, proj.damage * 0.7f, 4)
                        chained++
                    }
                }
            }
            6 -> { // ArmorBreak
                enemy.buffs.addBuff(BuffType.ArmorBreak, proj.abilityValue, 3f, proj.sourceUnitId)
            }
            9 -> { // Execute
                if (enemy.hpRatio < 0.3f) {
                    enemy.takeDamage(proj.damage * 2f, proj.isMagic)
                }
            }
            10 -> { // Knockback (Wind)
                enemy.recentHitFlags = enemy.recentHitFlags or 2
                enemy.recentHitTimer = 0.6f
                val dir = enemy.position - proj.sourcePos
                val len = dir.length
                if (len > 0.01f) {
                    val nx = dir.x / len
                    val ny = dir.y / len
                    // WIND_KNOCKBACK_EXTRA: +40% knockback distance
                    val knockbackMult = if (synergy.specialEffect == SynergySystem.SpecialEffect.WIND_KNOCKBACK_EXTRA) 1.4f else 1f
                    enemy.position.x += nx * proj.abilityValue * knockbackMult
                    enemy.position.y += ny * proj.abilityValue * knockbackMult
                }
            }
        }
    }

    fun applyAuraEffects(
        units: List<GameUnit>,
        dt: Float,
        auraTicks: FloatArray,
    ) {
        for (i in units.indices) {
            val unit = units[i]
            if (!unit.alive) continue
            if (unit.abilityType != 5 && unit.abilityType != 8) continue

            auraTicks[i] += dt
            if (auraTicks[i] < AURA_TICK) continue
            auraTicks[i] -= AURA_TICK

            for (other in units) {
                if (other === unit || !other.alive) continue
                val dist = unit.position.distanceTo(other.position)
                if (dist <= AURA_RADIUS) {
                    // SUPPORT_HEAL_BOOST: +25% aura/buff effectiveness
                    val supportBoost = if (activeSynergy.specialEffect == SynergySystem.SpecialEffect.SUPPORT_HEAL_BOOST) 1.25f else 1f
                    when (unit.abilityType) {
                        5 -> other.buffs.addBuff(BuffType.AtkUp, unit.abilityValue * supportBoost, 1f, i)
                        8 -> {
                            if (!other.buffs.hasBuff(BuffType.Shield)) {
                                other.buffs.addBuff(BuffType.Shield, unit.effectiveATK() * 0.5f * supportBoost, 5f, i)
                            }
                        }
                    }
                }
            }
        }
    }
}
