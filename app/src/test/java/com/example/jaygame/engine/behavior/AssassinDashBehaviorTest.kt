package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.*
import com.example.jaygame.engine.math.Vec2
import org.junit.Assert.*
import org.junit.Test

class AssassinDashBehaviorTest {

    @Test
    fun `enemy detected transitions to ATTACKING state`() {
        val behavior = AssassinDashBehavior()
        val unit = createTestUnit()
        unit.state = UnitState.IDLE
        unit.range = 100f

        val enemy = createTestEnemy(Vec2(150f, 0f))
        behavior.update(unit, 0.016f) { _, _ -> enemy }

        assertEquals(UnitState.ATTACKING, unit.state)
        assertSame(enemy, unit.currentTarget)
    }

    @Test
    fun `chases target when out of range`() {
        val behavior = AssassinDashBehavior()
        val unit = createTestUnit()
        unit.state = UnitState.IDLE
        unit.range = 100f

        val enemy = createTestEnemy(Vec2(300f, 0f))
        // Acquire target
        behavior.update(unit, 0.016f) { _, _ -> enemy }
        assertEquals(UnitState.ATTACKING, unit.state)

        // Chase toward enemy (out of range)
        val prevX = unit.position.x
        behavior.update(unit, 0.016f) { _, _ -> enemy }
        assertTrue(unit.position.x > prevX) // moved closer
        assertFalse(unit.isAttacking)
    }

    @Test
    fun `target dies mid-chase clears target and returns to IDLE`() {
        val behavior = AssassinDashBehavior()
        val unit = createTestUnit()
        unit.state = UnitState.IDLE
        unit.range = 100f

        val enemy = createTestEnemy(Vec2(300f, 0f))
        // Acquire target
        behavior.update(unit, 0.016f) { _, _ -> enemy }
        assertEquals(UnitState.ATTACKING, unit.state)

        // Kill the enemy
        enemy.alive = false
        behavior.update(unit, 0.016f) { _, _ -> null }

        assertEquals(UnitState.IDLE, unit.state)
        assertNull(unit.currentTarget)
    }

    @Test
    fun `onTakeDamage applies defense reduction`() {
        val behavior = AssassinDashBehavior()
        val unit = createTestUnit()
        unit.hp = 1000f
        unit.maxHp = 1000f
        unit.defense = 0f
        unit.state = UnitState.ATTACKING

        behavior.onTakeDamage(unit, 500f, isMagic = false)

        assertEquals(500f, unit.hp, 0.01f)
    }

    @Test
    fun `in range sets isAttacking true`() {
        val behavior = AssassinDashBehavior()
        val unit = createTestUnit()
        unit.state = UnitState.IDLE
        unit.range = 200f

        val enemy = createTestEnemy(Vec2(50f, 0f))
        // Acquire target
        behavior.update(unit, 0.016f) { _, _ -> enemy }
        // Now in range — should set isAttacking
        behavior.update(unit, 0.016f) { _, _ -> enemy }

        assertTrue(unit.isAttacking)
    }

    @Test
    fun `reset clears cooldown`() {
        val behavior = AssassinDashBehavior()
        val unit = createTestUnit()
        unit.state = UnitState.IDLE
        unit.range = 100f

        // Acquire a target
        val enemy = createTestEnemy(Vec2(150f, 0f))
        behavior.update(unit, 0.016f) { _, _ -> enemy }
        assertEquals(UnitState.ATTACKING, unit.state)

        behavior.reset()

        // After reset, behavior should function normally
        unit.state = UnitState.IDLE
        unit.currentTarget = null
        behavior.update(unit, 0.016f) { _, _ -> enemy }
        assertEquals(UnitState.ATTACKING, unit.state)
    }

    @Test
    fun `onTakeDamage applies defense when not attacking`() {
        val behavior = AssassinDashBehavior()
        val unit = createTestUnit()
        unit.hp = 1000f
        unit.maxHp = 1000f
        unit.defense = 100f
        unit.state = UnitState.IDLE

        behavior.onTakeDamage(unit, 100f, isMagic = false)

        // defense=100 => reduction = 100/(100+100) = 0.5, so 100 * 0.5 = 50 damage
        assertEquals(950f, unit.hp, 0.01f)
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
