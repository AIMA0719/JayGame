package com.jay.jaygame.engine.behavior

import com.jay.jaygame.engine.*
import com.jay.jaygame.engine.math.Vec2
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BehaviorFactoryTest {
    @Before
    fun setup() { BehaviorFactory.clearForTesting() }

    @Test
    fun `create returns registered behavior`() {
        val mockBehavior = createStubBehavior()
        BehaviorFactory.register("test_behavior") { mockBehavior }
        val result = BehaviorFactory.create("test_behavior")
        assertSame(mockBehavior, result)
    }

    @Test
    fun `create returns null for unknown behaviorId`() {
        assertNull(BehaviorFactory.create("nonexistent"))
    }

    @Test
    fun `isRegistered returns correct values`() {
        assertFalse(BehaviorFactory.isRegistered("test"))
        BehaviorFactory.register("test") { createStubBehavior() }
        assertTrue(BehaviorFactory.isRegistered("test"))
    }

    @Test
    fun `register overwrites existing entry`() {
        val first = createStubBehavior()
        val second = createStubBehavior()
        BehaviorFactory.register("test") { first }
        BehaviorFactory.register("test") { second }
        assertSame(second, BehaviorFactory.create("test"))
    }

    private fun createStubBehavior(): UnitBehavior = object : UnitBehavior {
        override fun update(unit: GameUnit, dt: Float, findEnemy: (Vec2, Float) -> Enemy?) {}
        override fun onAttack(unit: GameUnit, target: Enemy) = AttackResult(0f, false, false, false)
        override fun onTakeDamage(unit: GameUnit, damage: Float, isMagic: Boolean) {}
        override fun reset() {}
    }
}
