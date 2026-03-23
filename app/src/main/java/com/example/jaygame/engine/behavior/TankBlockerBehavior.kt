package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.*
import com.example.jaygame.engine.math.Vec2

class TankBlockerBehavior : UnitBehavior {
    val blockedEnemies = mutableListOf<Enemy>()
    var respawnTimer = 0f
    private val RESPAWN_COOLDOWN = 3f
    private val BOSS_BLOCK_DURATION = 5f
    private val bossBlockTimers = mutableMapOf<Enemy, Float>()
    private var attackCooldown = 0f

    override fun update(unit: GameUnit, dt: Float, findEnemy: (Vec2, Float) -> Enemy?) {
        attackCooldown -= dt * unit.spdMultiplier

        when (unit.state) {
            UnitState.IDLE -> {
                // Fixed position — tower defense style
                unit.position.x = unit.homePosition.x
                unit.position.y = unit.homePosition.y
                // Detect enemies within range
                val enemy = findEnemy(unit.position, unit.range)
                if (enemy != null && blockedEnemies.size < unit.blockCount && enemy.blockedBy == null) {
                    blockEnemy(enemy, unit)
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

                // Remove dead enemies from block list (properly releasing each)
                val iter = blockedEnemies.iterator()
                while (iter.hasNext()) {
                    val enemy = iter.next()
                    if (!enemy.alive) {
                        enemy.releaseBlock()
                        bossBlockTimers.remove(enemy)
                        iter.remove()
                    }
                }

                // Attack blocked enemies periodically
                if (attackCooldown <= 0f && blockedEnemies.isNotEmpty()) {
                    unit.isAttacking = true
                    unit.currentTarget = blockedEnemies.first()
                    unit.state = UnitState.ATTACKING
                } else {
                    unit.isAttacking = blockedEnemies.isNotEmpty()
                }

                // Try to block more if under blockCount
                if (blockedEnemies.size < unit.blockCount) {
                    val nearby = findEnemy(unit.position, unit.range)
                    if (nearby != null && nearby.blockedBy == null) {
                        blockEnemy(nearby, unit)
                    }
                }

                // If no enemies left to block, return to idle
                if (blockedEnemies.isEmpty()) {
                    unit.isAttacking = false
                    unit.state = UnitState.IDLE
                }
            }
            UnitState.ATTACKING -> {
                // Fire attack at blocked enemy, then return to blocking
                unit.state = UnitState.BLOCKING
                attackCooldown = 1f / unit.atkSpeed.coerceAtLeast(0.1f)
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
                    unit.position.x = unit.homePosition.x
                unit.position.y = unit.homePosition.y
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
        if (unit.state == UnitState.RESPAWNING) return // don't take damage while respawning
        val resist = if (isMagic) unit.magicResist else unit.defense
        val reduction = resist / (resist + 100f)
        unit.hp -= damage * (1f - reduction)
        if (unit.hp <= 0f) {
            unit.hp = 0f
            // Release all blocked enemies IMMEDIATELY
            releaseAllEnemies()
            unit.state = UnitState.RESPAWNING
            respawnTimer = RESPAWN_COOLDOWN
            // Keep alive=true so update() can run the RESPAWNING state
            // unit.alive stays true — tank is respawning, not permanently dead
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
        attackCooldown = 0f
    }
}
