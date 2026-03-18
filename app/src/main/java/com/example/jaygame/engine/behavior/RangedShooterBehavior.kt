package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.*
import com.example.jaygame.engine.math.Vec2
import kotlin.math.cos
import kotlin.math.sin

class RangedShooterBehavior(
    private val aoe: Boolean = false  // true for mage-type (area), false for archer-type (single)
) : UnitBehavior {
    private var attackCooldown: Float = 0f
    private var wanderTarget: Vec2 = Vec2()
    private var wanderTimer: Float = 0f

    override fun update(unit: GameUnit, dt: Float, findEnemy: (Vec2, Float) -> Enemy?) {
        attackCooldown -= dt

        when (unit.state) {
            UnitState.IDLE -> {
                // Detect enemies across entire map, chase into attack range
                val enemy = findEnemy(unit.position, 720f)
                if (enemy != null) {
                    unit.currentTarget = enemy
                    unit.state = UnitState.ATTACKING
                } else {
                    // Wander near home position
                    wanderTimer -= dt
                    if (wanderTimer <= 0f) {
                        val angle = Math.random().toFloat() * 6.283f
                        val dist = 20f + Math.random().toFloat() * 30f
                        wanderTarget = Vec2(
                            unit.homePosition.x + cos(angle) * dist,
                            unit.homePosition.y + sin(angle) * dist
                        )
                        wanderTimer = 1f + Math.random().toFloat() * 2f
                    }
                    // Move toward wander target
                    val dir = wanderTarget - unit.position
                    if (dir.lengthSq > 4f) {
                        val norm = dir.normalized()
                        val wanderSpeed = unit.moveSpeed * 0.3f
                        unit.position = unit.position + norm * (wanderSpeed * dt)
                    }
                }
            }
            UnitState.ATTACKING -> {
                val target = unit.currentTarget
                if (target == null || !target.alive) {
                    unit.currentTarget = null
                    unit.isAttacking = false
                    unit.state = UnitState.IDLE
                    return
                }
                // Chase toward target if out of attack range
                val distSq = unit.position.distanceSqTo(target.position)
                if (distSq > unit.range * unit.range) {
                    unit.isAttacking = false
                    val dir = target.position.minus(unit.position).normalized()
                    unit.position = unit.position.plus(dir.times(unit.moveSpeed * dt))
                } else {
                    unit.isAttacking = true
                }
            }
            else -> { /* Other states not used by ranged */ }
        }
    }

    override fun canAttack(): Boolean = attackCooldown <= 0f

    override fun onAttack(unit: GameUnit, target: Enemy): AttackResult {
        attackCooldown = 1f / unit.atkSpeed
        val isCrit = Math.random() < 0.05  // Base 5% crit
        val damage = unit.baseATK * (if (isCrit) 2f else 1f)
        val isMagic = unit.damageType == DamageType.MAGIC
        return AttackResult(
            damage = damage,
            isMagic = isMagic,
            isCrit = isCrit,
            isInstant = false  // Ranged = projectile
        )
    }

    override fun onTakeDamage(unit: GameUnit, damage: Float, isMagic: Boolean) {
        val resist = if (isMagic) unit.magicResist else unit.defense
        val reduction = resist / (resist + 100f)
        unit.hp -= damage * (1f - reduction)
        if (unit.hp <= 0f) {
            unit.alive = false
            unit.state = UnitState.DEAD
        }
    }

    override fun reset() {
        attackCooldown = 0f
        wanderTimer = 0f
    }
}
