package com.jay.jaygame.engine.behavior

import com.jay.jaygame.engine.*
import com.jay.jaygame.engine.math.Vec2
import org.junit.Assert.*
import org.junit.Test

class ControllerCCBehaviorTest {

    @Test
    fun `finds enemy and enters ATTACKING state`() {
        val behavior = ControllerCCBehavior(isRanged = true)
        val unit = createTestUnit()
        unit.state = UnitState.IDLE
        unit.range = 100f

        val enemy = createTestEnemy(Vec2(50f, 0f))
        behavior.update(unit, 0.016f) { _, _ -> enemy }

        assertEquals(UnitState.ATTACKING, unit.state)
        assertSame(enemy, unit.currentTarget)
    }

    @Test
    fun `returns to IDLE when target dies`() {
        val behavior = ControllerCCBehavior(isRanged = true)
        val unit = createTestUnit()
        val enemy = createTestEnemy(Vec2(50f, 0f))
        enemy.alive = false
        unit.currentTarget = enemy
        unit.state = UnitState.ATTACKING

        behavior.update(unit, 0.016f) { _, _ -> null }

        assertEquals(UnitState.IDLE, unit.state)
        assertNull(unit.currentTarget)
    }

    @Test
    fun `ranged mode returns isInstant false, melee returns true`() {
        val rangedBehavior = ControllerCCBehavior(isRanged = true)
        val meleeBehavior = ControllerCCBehavior(isRanged = false)
        val unit = createTestUnit()
        unit.baseATK = 100f
        unit.atkSpeed = 1f
        val enemy = createTestEnemy(Vec2(50f, 0f))

        val rangedResult = rangedBehavior.onAttack(unit, enemy)
        val meleeResult = meleeBehavior.onAttack(unit, enemy)

        assertFalse(rangedResult.isInstant)
        assertTrue(meleeResult.isInstant)
    }

    @Test
    fun `onAttack sets cooldown`() {
        val behavior = ControllerCCBehavior(isRanged = true)
        val unit = createTestUnit()
        unit.baseATK = 100f
        unit.atkSpeed = 1f

        assertTrue(behavior.canAttack())
        behavior.onAttack(unit, createTestEnemy(Vec2(0f, 0f)))
        assertFalse(behavior.canAttack())
    }

    @Test
    fun `getCCDuration returns base duration`() {
        val behavior = ControllerCCBehavior()
        assertEquals(2f, behavior.getCCDuration(), 0.01f)
    }

    @Test
    fun `reset clears cooldown and timer`() {
        val behavior = ControllerCCBehavior()
        val unit = createTestUnit()
        unit.baseATK = 10f
        unit.atkSpeed = 1f

        behavior.onAttack(unit, createTestEnemy(Vec2(0f, 0f)))
        assertFalse(behavior.canAttack())
        behavior.reset()
        assertTrue(behavior.canAttack())
    }

    // --- Helpers ---

    private fun createTestUnit(): GameUnit {
        val unit = GameUnit()
        unit.alive = true
        unit.position = Vec2(0f, 0f)
        unit.homePosition = Vec2(0f, 0f)
        unit.range = 100f
        unit.atkSpeed = 1f
        unit.baseATK = 10f
        unit.hp = 1000f
        unit.maxHp = 1000f
        unit.defense = 0f
        unit.magicResist = 0f
        unit.state = UnitState.IDLE
        unit.damageType = DamageType.PHYSICAL
        unit.moveSpeed = 75f
        return unit
    }

    private fun createTestEnemy(pos: Vec2): Enemy {
        val enemy = Enemy()
        enemy.init(
            hp = 100f,
            speed = 50f,
            armor = 0f,
            magicResist = 0f,
            type = 0,
            startPos = pos
        )
        return enemy
    }
}
