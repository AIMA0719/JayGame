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

    // ── Unique ability fields (M4) ──
    var uniqueAbilityType = -1       // index into UniqueAbilitySystem types, -1 = none
    var uniqueAbilityCooldown = 0f   // seconds until next activation
    var uniqueAbilityMaxCd = 0f      // max cooldown for this ability
    var passiveCounter = 0           // generic counter for passive triggers (e.g., hit count)

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
        this.uniqueAbilityCooldown = 0f
        this.passiveCounter = 0
        this.wanderTarget = homePos.copy()
        this.wanderTimer = 0f
        this.moveSpeed = when (family) {
            0 -> 110f
            1 -> 55f
            2 -> 75f
            3 -> 65f
            4 -> 90f
            5 -> 100f   // Wind: fast
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

        // Clamp position within grid boundaries (prevent units from entering monster path)
        position.x = position.x.coerceIn(GRID_MIN_X, GRID_MAX_X)
        position.y = position.y.coerceIn(GRID_MIN_Y, GRID_MAX_Y)
    }

    companion object {
        val LEVEL_MULTIPLIERS = floatArrayOf(1f, 1.5f, 2.2f, 3.2f, 4.5f, 6f, 8f)

        // Grid boundaries with margin for unit sprite size
        private const val GRID_MIN_X = 120f + 10f     // Grid.ORIGIN_X + margin
        private const val GRID_MAX_X = 600f - 10f     // Grid.ORIGIN_X + Grid.GRID_W - margin
        private const val GRID_MIN_Y = 107.5f + 10f   // Grid.ORIGIN_Y + margin
        private const val GRID_MAX_Y = 587.5f - 10f   // Grid.ORIGIN_Y + Grid.GRID_H - margin
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
        uniqueAbilityType = -1
        uniqueAbilityCooldown = 0f
        uniqueAbilityMaxCd = 0f
        passiveCounter = 0
    }
}
