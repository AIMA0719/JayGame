package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.*
import com.example.jaygame.engine.math.Vec2
import org.junit.Assert.*
import org.junit.Test

class RangedShooterBehaviorTest {

    @Test
    fun `update finds enemy and sets ATTACKING state`() {
        val behavior = RangedShooterBehavior()
        val unit = createTestUnit()
        unit.state = UnitState.IDLE
        unit.range = 100f

        val enemy = createTestEnemy(Vec2(50f, 0f))
        behavior.update(unit, 0.016f) { _, _ -> enemy }

        assertEquals(UnitState.ATTACKING, unit.state)
        assertSame(enemy, unit.currentTarget)
    }

    @Test
    fun `update stays IDLE when no enemy in range`() {
        val behavior = RangedShooterBehavior()
        val unit = createTestUnit()
        unit.state = UnitState.IDLE

        behavior.update(unit, 0.016f) { _, _ -> null }

        assertEquals(UnitState.IDLE, unit.state)
    }

    @Test
    fun `update returns to IDLE when target dies`() {
        val behavior = RangedShooterBehavior()
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
    fun `stays fixed and goes IDLE when target leaves range`() {
        val behavior = RangedShooterBehavior()
        val unit = createTestUnit()
        unit.range = 100f
        unit.moveSpeed = 100f
        val enemy = createTestEnemy(Vec2(200f, 0f))  // Beyond attack range
        unit.currentTarget = enemy
        unit.state = UnitState.ATTACKING

        behavior.update(unit, 0.016f) { _, _ -> null }

        // Unit stays at home position and goes IDLE (no chasing)
        assertEquals(0f, unit.position.x, 0.01f)
        assertEquals(UnitState.IDLE, unit.state)
        assertFalse(unit.isAttacking)
    }

    @Test
    fun `onAttack returns projectile AttackResult`() {
        val behavior = RangedShooterBehavior()
        val unit = createTestUnit()
        unit.baseATK = 100f
        unit.atkSpeed = 1f
        unit.damageType = DamageType.PHYSICAL

        val enemy = createTestEnemy(Vec2(0f, 0f))
        val result = behavior.onAttack(unit, enemy)

        assertFalse(result.isInstant)
        assertFalse(result.isMagic)
        assertTrue(result.damage > 0f)
    }

    @Test
    fun `onAttack returns magic result for MAGIC damageType`() {
        val behavior = RangedShooterBehavior(aoe = true)
        val unit = createTestUnit()
        unit.baseATK = 50f
        unit.atkSpeed = 1f
        unit.damageType = DamageType.MAGIC

        val enemy = createTestEnemy(Vec2(0f, 0f))
        val result = behavior.onAttack(unit, enemy)

        assertTrue(result.isMagic)
        assertFalse(result.isInstant)
    }

    @Test
    fun `onAttack sets cooldown so canAttack returns false`() {
        val behavior = RangedShooterBehavior()
        val unit = createTestUnit()
        unit.baseATK = 10f
        unit.atkSpeed = 1f

        assertTrue(behavior.canAttack())
        behavior.onAttack(unit, createTestEnemy(Vec2(0f, 0f)))
        assertFalse(behavior.canAttack())
    }

    @Test
    fun `reset clears cooldown`() {
        val behavior = RangedShooterBehavior()
        val unit = createTestUnit()
        unit.baseATK = 10f
        unit.atkSpeed = 1f

        behavior.onAttack(unit, createTestEnemy(Vec2(0f, 0f)))
        assertFalse(behavior.canAttack())
        behavior.reset()
        assertTrue(behavior.canAttack())
    }

    @Test
    fun `onTakeDamage reduces hp with defense`() {
        val behavior = RangedShooterBehavior()
        val unit = createTestUnit()
        unit.hp = 1000f
        unit.maxHp = 1000f
        unit.defense = 100f
        unit.magicResist = 0f

        behavior.onTakeDamage(unit, 100f, isMagic = false)

        // defense=100 => reduction = 100/(100+100) = 0.5, so 100 * 0.5 = 50 damage
        assertEquals(950f, unit.hp, 0.01f)
        assertTrue(unit.alive)
    }

    @Test
    fun `onTakeDamage kills unit at zero hp`() {
        val behavior = RangedShooterBehavior()
        val unit = createTestUnit()
        unit.hp = 10f
        unit.maxHp = 100f
        unit.defense = 0f

        behavior.onTakeDamage(unit, 100f, isMagic = false)

        assertFalse(unit.alive)
        assertEquals(UnitState.DEAD, unit.state)
    }

    @Test
    fun `onTakeDamage uses magicResist for magic damage`() {
        val behavior = RangedShooterBehavior()
        val unit = createTestUnit()
        unit.hp = 1000f
        unit.maxHp = 1000f
        unit.defense = 0f
        unit.magicResist = 100f

        behavior.onTakeDamage(unit, 100f, isMagic = true)

        // magicResist=100 => reduction = 100/(100+100) = 0.5, so 100 * 0.5 = 50 damage
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
