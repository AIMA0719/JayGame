package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.*
import com.example.jaygame.engine.math.Vec2

class TankBlockerBehavior : UnitBehavior {
    val blockedEnemies = mutableListOf<Enemy>()
    var respawnTimer = 0f
    private val RESPAWN_COOLDOWN = 3f
    private val BOSS_BLOCK_DURATION = 5f
    private val bossBlockTimers = mutableMapOf<Enemy, Float>()

    override fun update(unit: GameUnit, dt: Float, findEnemy: (Vec2, Float) -> Enemy?) {
        when (unit.state) {
            UnitState.IDLE -> {
                // Find nearest enemy to block
                val enemy = findEnemy(unit.position, unit.range * 2f)
                if (enemy != null && blockedEnemies.size < unit.blockCount) {
                    unit.state = UnitState.MOVING
                    unit.currentTarget = enemy
                }
            }
            UnitState.MOVING -> {
                val target = unit.currentTarget
                if (target == null || !target.alive) {
                    unit.state = UnitState.IDLE
                    return
                }
                // Move toward enemy
                val dir = target.position.minus(unit.position).normalized()
                unit.position = unit.position.plus(dir.times(unit.moveSpeed * dt))
                // Close enough to block
                val dist = unit.position.distanceTo(target.position)
                if (dist < 30f) {
                    blockEnemy(target, unit)
                    unit.state = UnitState.BLOCKING
                }
            }
            UnitState.BLOCKING -> {
                // Update boss block timers
                val iterator = bossBlockTimers.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    entry.setValue(entry.value + dt)
                    if (entry.value >= BOSS_BLOCK_DURATION) {
                        val boss = entry.key
                        boss.releaseBlock()
                        blockedEnemies.remove(boss)
                        iterator.remove()
                    }
                }

                // Remove dead enemies from block list
                blockedEnemies.removeAll { !it.alive }

                // Try to block more if under blockCount
                if (blockedEnemies.size < unit.blockCount) {
                    val nearby = findEnemy(unit.position, unit.range)
                    if (nearby != null && nearby.blockedBy == null) {
                        blockEnemy(nearby, unit)
                    }
                }

                // If no enemies left to block, return to idle
                if (blockedEnemies.isEmpty()) {
                    unit.state = UnitState.IDLE
                }
            }
            UnitState.DEAD -> {
                // Release ALL blocked enemies immediately (same frame)
                releaseAllEnemies()
                unit.state = UnitState.RESPAWNING
                respawnTimer = RESPAWN_COOLDOWN
            }
            UnitState.RESPAWNING -> {
                respawnTimer -= dt
                if (respawnTimer <= 0f) {
                    unit.hp = unit.maxHp
                    unit.alive = true
                    // Return to home position
                    unit.position = unit.homePosition.copy()
                    unit.state = UnitState.IDLE
                }
            }
            else -> {}
        }
    }

    override fun onAttack(unit: GameUnit, target: Enemy): AttackResult {
        // Tanks do low damage melee attacks
        return AttackResult(
            damage = unit.baseATK,
            isMagic = false,
            isCrit = false,
            isInstant = true  // Melee = instant
        )
    }

    override fun onTakeDamage(unit: GameUnit, damage: Float, isMagic: Boolean) {
        val resist = if (isMagic) unit.magicResist else unit.defense
        val reduction = resist / (resist + 100f)
        unit.hp -= damage * (1f - reduction)
        if (unit.hp <= 0f) {
            unit.hp = 0f
            unit.alive = false
            unit.state = UnitState.DEAD
        }
    }

    private fun blockEnemy(enemy: Enemy, unit: GameUnit) {
        enemy.applyBlock(unit)
        blockedEnemies.add(enemy)
        // Track boss block timer if enemy is boss
        if (enemy.isBoss) {
            bossBlockTimers[enemy] = 0f
        }
    }

    private fun releaseEnemy(enemy: Enemy) {
        enemy.releaseBlock()
        blockedEnemies.remove(enemy)
        bossBlockTimers.remove(enemy)
    }

    private fun releaseAllEnemies() {
        blockedEnemies.forEach { it.releaseBlock() }
        blockedEnemies.clear()
        bossBlockTimers.clear()
    }

    override fun reset() {
        releaseAllEnemies()
        respawnTimer = 0f
    }
}
