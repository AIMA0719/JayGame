package com.example.jaygame.engine

import com.example.jaygame.engine.math.GameRect
import com.example.jaygame.engine.math.Vec2

object AbilitySystem {
    private const val AURA_RADIUS = 150f
    private const val AURA_TICK = 0.5f

    fun onProjectileHit(
        proj: Projectile,
        enemy: Enemy,
        spatialHash: SpatialHash<Enemy>,
        spawnProjectile: (from: Vec2, target: Enemy, damage: Float, type: Int) -> Unit,
    ) {
        enemy.takeDamage(proj.damage, proj.isMagic)

        when (proj.abilityType) {
            1 -> { // Splash (Fire)
                // Fire DoT on primary target
                enemy.buffs.addBuff(BuffType.DoT, proj.damage * 0.1f, 2f, proj.sourceUnitId)
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
                enemy.buffs.addBuff(BuffType.Slow, proj.abilityValue, 2f, proj.sourceUnitId)
            }
            3 -> { // DoT
                enemy.buffs.addBuff(BuffType.DoT, proj.abilityValue, 3f, proj.sourceUnitId)
            }
            4 -> { // Chain
                // Mark primary target with lightning hit flag
                enemy.recentHitFlags = enemy.recentHitFlags or 1
                enemy.recentHitTimer = 0.5f
                val chainCount = proj.abilityValue.toInt().coerceIn(1, 5)
                val rect = GameRect(
                    enemy.position.x - 200f, enemy.position.y - 200f, 400f, 400f,
                )
                var chained = 0
                spatialHash.query(rect).forEach { nearby ->
                    if (chained < chainCount && nearby !== enemy && nearby.alive) {
                        // Mark chained targets with lightning hit flag
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
                    enemy.position.x += nx * proj.abilityValue
                    enemy.position.y += ny * proj.abilityValue
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
                    when (unit.abilityType) {
                        5 -> other.buffs.addBuff(BuffType.AtkUp, unit.abilityValue, 1f, i)
                        8 -> {
                            if (!other.buffs.hasBuff(BuffType.Shield)) {
                                other.buffs.addBuff(BuffType.Shield, unit.effectiveATK() * 0.5f, 5f, i)
                            }
                        }
                    }
                }
            }
        }
    }
}
