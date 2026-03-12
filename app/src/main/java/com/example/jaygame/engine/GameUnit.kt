package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2
import kotlin.math.cos
import kotlin.math.sin

class GameUnit {
    var alive = false
    var unitDefId = -1
    var grade = 0
    var family = 0
    var level = 1
    var tileIndex = -1
    var position = Vec2()
    var homePosition = Vec2()
    var baseATK = 0f
    var atkSpeed = 0f
    var range = 0f
    var abilityType = 0
    var abilityValue = 0f
    var isAttacking = false
    val buffs = BuffContainer()

    private var wanderTarget = Vec2()
    private var wanderTimer = 0f
    private var moveSpeed = 75f
    private var chaseTarget: Enemy? = null

    private var attackCooldown = 0f
    var currentTarget: Enemy? = null; private set

    fun init(
        unitDefId: Int, grade: Int, family: Int, level: Int,
        tileIndex: Int, homePos: Vec2,
        baseATK: Float, atkSpeed: Float, range: Float,
        abilityType: Int, abilityValue: Float,
    ) {
        this.alive = true
        this.unitDefId = unitDefId
        this.grade = grade
        this.family = family
        this.level = level
        this.tileIndex = tileIndex
        this.position = homePos.copy()
        this.homePosition = homePos.copy()
        this.baseATK = baseATK
        this.atkSpeed = atkSpeed
        this.range = range
        this.abilityType = abilityType
        this.abilityValue = abilityValue
        this.isAttacking = false
        this.attackCooldown = 0f
        this.buffs.clear()
        this.wanderTarget = homePos.copy()
        this.wanderTimer = 0f
        this.moveSpeed = when (family) {
            0 -> 110f
            1 -> 55f
            2 -> 75f
            3 -> 65f
            4 -> 90f
            else -> 75f
        }
    }

    fun update(dt: Float, findEnemy: (Vec2, Float) -> Enemy?) {
        if (!alive) return

        buffs.update(dt)
        val spdMult = buffs.getSpdMultiplier()
        attackCooldown -= dt * spdMult

        val chaseRange = range * 1.5f
        val enemy = findEnemy(position, chaseRange)

        if (enemy != null) {
            chaseTarget = enemy
            val dir = enemy.position - position
            val dist = dir.length

            if (dist > range) {
                val norm = dir.normalized()
                position.x += norm.x * moveSpeed * dt
                position.y += norm.y * moveSpeed * dt
                isAttacking = false
            } else {
                isAttacking = true
                currentTarget = enemy
            }
        } else {
            chaseTarget = null
            isAttacking = false
            currentTarget = null
            wanderTimer -= dt
            if (wanderTimer <= 0f) {
                val angle = Math.random().toFloat() * 6.283f
                val dist = 20f + Math.random().toFloat() * 30f
                wanderTarget = Vec2(
                    homePosition.x + cos(angle) * dist,
                    homePosition.y + sin(angle) * dist,
                )
                wanderTimer = 1f + Math.random().toFloat() * 2f
            }
            val dir = wanderTarget - position
            if (dir.lengthSq > 4f) {
                val norm = dir.normalized()
                val wanderSpeed = moveSpeed * 0.3f
                position.x += norm.x * wanderSpeed * dt
                position.y += norm.y * wanderSpeed * dt
            }
        }
    }

    fun canAttack(): Boolean = isAttacking && attackCooldown <= 0f && currentTarget?.alive == true

    fun onAttack() {
        attackCooldown = 1f / atkSpeed
    }

    fun effectiveATK(): Float {
        val levelMult = LEVEL_MULTIPLIERS.getOrElse(level - 1) { 1f }
        return baseATK * levelMult * buffs.getAtkMultiplier()
    }

    fun reset() {
        alive = false
        currentTarget = null
        chaseTarget = null
        buffs.clear()
    }

    companion object {
        val LEVEL_MULTIPLIERS = floatArrayOf(1f, 1.5f, 2.2f, 3.2f, 4.5f, 6f, 8f)
    }
}
