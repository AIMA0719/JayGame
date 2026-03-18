package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.*
import com.example.jaygame.engine.math.Vec2
import org.junit.Assert.*
import org.junit.Test

class AssassinDashBehaviorTest {

    @Test
    fun `enemy detected transitions to DASHING state`() {
        val behavior = AssassinDashBehavior()
        val unit = createTestUnit()
        unit.state = UnitState.IDLE
        unit.range = 100f

        val enemy = createTestEnemy(Vec2(150f, 0f))
        behavior.update(unit, 0.016f) { _, _ -> enemy }

        assertEquals(UnitState.DASHING, unit.state)
        assertSame(enemy, unit.currentTarget)
    }

    @Test
    fun `reach target transitions to RETURNING state`() {
        val behavior = AssassinDashBehavior()
        val unit = createTestUnit()
        unit.state = UnitState.IDLE
        unit.range = 100f

        val enemy = createTestEnemy(Vec2(150f, 0f))
        // First: acquire target
        behavior.update(unit, 0.016f) { _, _ -> enemy }
        assertEquals(UnitState.DASHING, unit.state)

        // Move unit very close to enemy to trigger hit
        unit.position = Vec2(145f, 0f)
        behavior.update(unit, 0.016f) { _, _ -> enemy }

        assertEquals(UnitState.RETURNING, unit.state)
    }

    @Test
    fun `target dies mid-dash causes immediate return with half cooldown`() {
        val behavior = AssassinDashBehavior()
        val unit = createTestUnit()
        unit.state = UnitState.IDLE
        unit.range = 100f

        val enemy = createTestEnemy(Vec2(300f, 0f))
        // Acquire target
        behavior.update(unit, 0.016f) { _, _ -> enemy }
        assertEquals(UnitState.DASHING, unit.state)

        // Kill the enemy mid-dash
        enemy.alive = false
        behavior.update(unit, 0.016f) { _, _ -> null }

        assertEquals(UnitState.RETURNING, unit.state)
        assertNull(unit.currentTarget)
    }

    @Test
    fun `invincible during dash - onTakeDamage ignored`() {
        val behavior = AssassinDashBehavior()
        val unit = createTestUnit()
        unit.hp = 1000f
        unit.maxHp = 1000f
        unit.defense = 0f
        unit.state = UnitState.DASHING

        behavior.onTakeDamage(unit, 500f, isMagic = false)

        assertEquals(1000f, unit.hp, 0.01f)
        assertTrue(unit.alive)
    }

    @Test
    fun `returns to home and transitions to IDLE`() {
        val behavior = AssassinDashBehavior()
        val unit = createTestUnit()
        unit.homePosition = Vec2(0f, 0f)
        unit.position = Vec2(5f, 0f)  // Close to home
        unit.state = UnitState.RETURNING
        unit.moveSpeed = 75f

        // Use small dt steps so the unit doesn't overshoot
        repeat(20) {
            behavior.update(unit, 0.016f) { _, _ -> null }
        }

        assertEquals(UnitState.IDLE, unit.state)
        assertEquals(0f, unit.position.x, 0.01f)
        assertEquals(0f, unit.position.y, 0.01f)
    }

    @Test
    fun `reset clears state`() {
        val behavior = AssassinDashBehavior()
        val unit = createTestUnit()
        unit.state = UnitState.IDLE
        unit.range = 100f

        // Acquire a target to set internal state
        val enemy = createTestEnemy(Vec2(150f, 0f))
        behavior.update(unit, 0.016f) { _, _ -> enemy }
        assertEquals(UnitState.DASHING, unit.state)

        behavior.reset()

        // After reset, update in IDLE should be able to acquire target again immediately
        unit.state = UnitState.IDLE
        behavior.update(unit, 0.016f) { _, _ -> enemy }
        assertEquals(UnitState.DASHING, unit.state)
    }

    @Test
    fun `onTakeDamage applies defense when not dashing`() {
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
