package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.*
import com.example.jaygame.engine.math.Vec2
import org.junit.Assert.*
import org.junit.Test

class SupportAuraBehaviorTest {

    @Test
    fun `stays near home position`() {
        val behavior = SupportAuraBehavior()
        val unit = createTestUnit()
        unit.homePosition = Vec2(100f, 100f)
        unit.position = Vec2(120f, 100f)  // 20 units away from home

        // Run several updates - should drift toward home
        repeat(60) {
            behavior.update(unit, 0.016f) { _, _ -> null }
        }

        // Should be closer to home than before
        val dist = unit.position.distanceTo(unit.homePosition)
        assertTrue("Unit should drift toward home, dist=$dist", dist < 20f)
        assertEquals(UnitState.IDLE, unit.state)
    }

    @Test
    fun `low damage on attack`() {
        val behavior = SupportAuraBehavior()
        val unit = createTestUnit()
        unit.baseATK = 100f

        val enemy = createTestEnemy(Vec2(50f, 0f))
        val result = behavior.onAttack(unit, enemy)

        assertEquals(30f, result.damage, 0.01f)  // 100 * 0.3 = 30
        assertTrue(result.isMagic)
        assertFalse(result.isCrit)
        assertFalse(result.isInstant)
    }

    @Test
    fun `reset clears buff timer`() {
        val behavior = SupportAuraBehavior()
        val unit = createTestUnit()

        // Tick once to set the timer
        behavior.update(unit, 0.5f) { _, _ -> null }
        behavior.reset()

        // After reset, shouldApplyBuff should reflect initial state (timer is 0)
        assertTrue(behavior.shouldApplyBuff())
    }

    @Test
    fun `getBuffRange returns expected range`() {
        val behavior = SupportAuraBehavior()
        assertEquals(90f, behavior.getBuffRange(), 0.01f)
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
