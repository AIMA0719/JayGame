package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.*
import com.example.jaygame.engine.math.Vec2
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TankBlockerBehaviorTest {

    private lateinit var behavior: TankBlockerBehavior
    private lateinit var unit: GameUnit

    @Before
    fun setUp() {
        behavior = TankBlockerBehavior()
        unit = createTestUnit()
    }

    @Test
    fun `tank detects enemy in range and blocks`() {
        // Place enemy within unit's range (100f)
        val enemy = createTestEnemy(Vec2(25f, 0f))
        unit.blockCount = 1

        // IDLE -> should find enemy in range and transition directly to BLOCKING
        behavior.update(unit, 0.016f) { _, _ -> enemy }
        assertEquals(UnitState.BLOCKING, unit.state)
        assertSame(unit, enemy.blockedBy)
        assertTrue(behavior.blockedEnemies.contains(enemy))
    }

    @Test
    fun `blocked enemy stops moving`() {
        val enemy = createTestEnemy(Vec2(100f, 0f))
        val path = listOf(Vec2(0f, 0f), Vec2(200f, 0f))
        enemy.pathIndex = 1

        // Record position before blocking
        val posBefore = enemy.position.copy()

        // Block the enemy
        enemy.applyBlock(unit)

        // Update enemy - should not move
        enemy.update(0.5f, path)
        assertEquals(posBefore.x, enemy.position.x, 0.01f)
        assertEquals(posBefore.y, enemy.position.y, 0.01f)
        assertNotNull(enemy.blockedBy)
    }

    @Test
    fun `tank respects blockCount limit`() {
        unit.blockCount = 1
        val enemy1 = createTestEnemy(Vec2(10f, 0f))
        val enemy2 = createTestEnemy(Vec2(20f, 0f))

        // Manually block one enemy first
        unit.state = UnitState.BLOCKING
        enemy1.applyBlock(unit)
        behavior.blockedEnemies.add(enemy1)

        // While in BLOCKING with blockCount reached, should NOT block enemy2
        behavior.update(unit, 0.016f) { _, _ -> enemy2 }
        assertEquals(1, behavior.blockedEnemies.size)
        assertNull(enemy2.blockedBy)
    }

    @Test
    fun `tank death releases all blocked enemies`() {
        val enemy1 = createTestEnemy(Vec2(10f, 0f))
        val enemy2 = createTestEnemy(Vec2(20f, 0f))

        // Set up blocking state
        unit.state = UnitState.DEAD
        unit.alive = false
        enemy1.applyBlock(unit)
        enemy2.applyBlock(unit)
        behavior.blockedEnemies.add(enemy1)
        behavior.blockedEnemies.add(enemy2)

        // Update in DEAD state
        behavior.update(unit, 0.016f) { _, _ -> null }

        // All enemies should be released
        assertNull(enemy1.blockedBy)
        assertNull(enemy2.blockedBy)
        assertTrue(behavior.blockedEnemies.isEmpty())
        assertEquals(UnitState.RESPAWNING, unit.state)
    }

    @Test
    fun `tank respawns after cooldown`() {
        unit.state = UnitState.DEAD
        unit.alive = false
        unit.hp = 0f
        unit.maxHp = 1000f
        unit.homePosition = Vec2(50f, 50f)

        // Transition DEAD -> RESPAWNING
        behavior.update(unit, 0.016f) { _, _ -> null }
        assertEquals(UnitState.RESPAWNING, unit.state)

        // Not enough time passed (respawn cooldown = 3s)
        behavior.update(unit, 1f) { _, _ -> null }
        assertEquals(UnitState.RESPAWNING, unit.state)

        // Pass remaining time
        behavior.update(unit, 2.1f) { _, _ -> null }

        // Should be respawned
        assertEquals(UnitState.IDLE, unit.state)
        assertTrue(unit.alive)
        assertEquals(1000f, unit.hp, 0.01f)
        assertEquals(50f, unit.position.x, 0.01f)
        assertEquals(50f, unit.position.y, 0.01f)
    }

    @Test
    fun `boss breaks free after 5 seconds`() {
        val boss = createTestEnemy(Vec2(5f, 0f))
        boss.bossModifier = BossModifier.PHYSICAL_RESIST
        assertTrue(boss.isBoss)

        unit.blockCount = 2

        // IDLE -> find boss in range -> directly BLOCKING (fixed position)
        behavior.update(unit, 0.016f) { _, _ -> boss }
        assertEquals(UnitState.BLOCKING, unit.state)
        assertNotNull(boss.blockedBy)

        // Accumulate time past 5 seconds using small steps to account for
        // BLOCKING->ATTACKING->BLOCKING state transitions each update
        repeat(400) {
            behavior.update(unit, 0.016f) { _, _ -> null }
        }

        // Boss should be released after ~6.4s of updates
        assertNull(boss.blockedBy)
        assertFalse(behavior.blockedEnemies.contains(boss))
    }

    @Test
    fun `reset clears all state`() {
        val enemy1 = createTestEnemy(Vec2(10f, 0f))
        val enemy2 = createTestEnemy(Vec2(20f, 0f))

        enemy_block(enemy1)
        enemy_block(enemy2)
        behavior.respawnTimer = 2f

        behavior.reset()

        assertTrue(behavior.blockedEnemies.isEmpty())
        assertNull(enemy1.blockedBy)
        assertNull(enemy2.blockedBy)
        assertEquals(0f, behavior.respawnTimer, 0.01f)
    }

    @Test
    fun `onAttack returns instant melee result`() {
        unit.baseATK = 50f
        val enemy = createTestEnemy(Vec2(0f, 0f))
        val result = behavior.onAttack(unit, enemy)

        assertEquals(50f, result.damage, 0.01f)
        assertFalse(result.isMagic)
        assertFalse(result.isCrit)
        assertTrue(result.isInstant)
    }

    @Test
    fun `onTakeDamage reduces hp with defense`() {
        unit.hp = 1000f
        unit.maxHp = 1000f
        unit.defense = 100f

        behavior.onTakeDamage(unit, 100f, isMagic = false)
        // defense=100 => reduction = 100/(100+100) = 0.5, so 100 * 0.5 = 50 damage
        assertEquals(950f, unit.hp, 0.01f)
        assertTrue(unit.alive)
    }

    @Test
    fun `onTakeDamage transitions to RESPAWNING at zero hp`() {
        unit.hp = 10f
        unit.maxHp = 100f
        unit.defense = 0f

        behavior.onTakeDamage(unit, 100f, isMagic = false)

        assertEquals(0f, unit.hp, 0.01f)
        // Tank stays alive=true so RESPAWNING state can run in update()
        assertTrue(unit.alive)
        assertEquals(UnitState.RESPAWNING, unit.state)
    }

    // --- Helpers ---

    private fun enemy_block(enemy: Enemy) {
        enemy.applyBlock(unit)
        behavior.blockedEnemies.add(enemy)
        if (enemy.isBoss) {
            // Access boss timer through reflection or just re-block via behavior
            // We need to use the behavior's internal blockEnemy — simulate it
        }
    }

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
        unit.moveSpeed = 75f
        unit.blockCount = 2
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
