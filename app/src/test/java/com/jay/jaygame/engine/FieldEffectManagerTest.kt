package com.jay.jaygame.engine

import com.jay.jaygame.engine.math.Vec2
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FieldEffectManagerTest {

    private lateinit var manager: FieldEffectManager
    private lateinit var field: BattleFieldAccess

    @Before
    fun setUp() {
        manager = FieldEffectManager()
        field = object : BattleFieldAccess {
            override fun getUnits(): List<GameUnit> = emptyList()
            override fun getEnemies(): List<Enemy> = emptyList()
        }
    }

    private fun makeSpecialUnit(id: String): GameUnit {
        val unit = GameUnit()
        unit.blueprintId = id
        unit.unitCategory = UnitCategory.SPECIAL
        unit.alive = true
        unit.position = Vec2(100f, 100f)
        return unit
    }

    private fun makeNormalUnit(id: String = "normal_01"): GameUnit {
        val unit = GameUnit()
        unit.blueprintId = id
        unit.unitCategory = UnitCategory.NORMAL
        unit.alive = true
        unit.position = Vec2(100f, 100f)
        return unit
    }

    private fun stubController(): FieldEffectController {
        return object : FieldEffectController {
            var placed = false
            var removed = false
            var updateCount = 0

            override fun onPlace(unit: GameUnit, field: BattleFieldAccess) { placed = true }
            override fun update(dt: Float, field: BattleFieldAccess) { updateCount++ }
            override fun onRemove() { removed = true }
            override fun getEffectRange(): Float = 100f
            override fun canStack(): Boolean = false
            override fun reset() {}
        }
    }

    @Test
    fun `canPlace allows first special unit`() {
        val unit = makeSpecialUnit("barrier_01")
        assertTrue(manager.canPlace(unit))
    }

    @Test
    fun `canPlace allows second different special unit`() {
        val unit1 = makeSpecialUnit("barrier_01")
        manager.addEffect(unit1, stubController(), field)

        val unit2 = makeSpecialUnit("forge_01")
        assertTrue(manager.canPlace(unit2))
    }

    @Test
    fun `canPlace rejects third special unit`() {
        val unit1 = makeSpecialUnit("barrier_01")
        val unit2 = makeSpecialUnit("forge_01")
        manager.addEffect(unit1, stubController(), field)
        manager.addEffect(unit2, stubController(), field)

        val unit3 = makeSpecialUnit("timewarp_01")
        assertFalse(manager.canPlace(unit3))
    }

    @Test
    fun `canPlace rejects duplicate special unit`() {
        val unit1 = makeSpecialUnit("barrier_01")
        manager.addEffect(unit1, stubController(), field)

        val unit2 = makeSpecialUnit("barrier_01")
        assertFalse(manager.canPlace(unit2))
    }

    @Test
    fun `canPlace allows normal units always`() {
        // Fill up special slots
        manager.addEffect(makeSpecialUnit("barrier_01"), stubController(), field)
        manager.addEffect(makeSpecialUnit("forge_01"), stubController(), field)

        val normalUnit = makeNormalUnit()
        assertTrue(manager.canPlace(normalUnit))
    }

    @Test
    fun `addEffect registers and calls onPlace`() {
        val unit = makeSpecialUnit("barrier_01")
        val controller = object : FieldEffectController {
            var placed = false
            override fun onPlace(unit: GameUnit, field: BattleFieldAccess) { placed = true }
            override fun update(dt: Float, field: BattleFieldAccess) {}
            override fun onRemove() {}
            override fun getEffectRange(): Float = 100f
            override fun canStack(): Boolean = false
            override fun reset() {}
        }

        manager.addEffect(unit, controller, field)

        assertTrue(controller.placed)
        assertEquals(1, manager.activeCount())
    }

    @Test
    fun `update propagates to all active effects`() {
        var updateCount1 = 0
        var updateCount2 = 0

        val ctrl1 = object : FieldEffectController {
            override fun onPlace(unit: GameUnit, field: BattleFieldAccess) {}
            override fun update(dt: Float, field: BattleFieldAccess) { updateCount1++ }
            override fun onRemove() {}
            override fun getEffectRange(): Float = 100f
            override fun canStack(): Boolean = false
            override fun reset() {}
        }
        val ctrl2 = object : FieldEffectController {
            override fun onPlace(unit: GameUnit, field: BattleFieldAccess) {}
            override fun update(dt: Float, field: BattleFieldAccess) { updateCount2++ }
            override fun onRemove() {}
            override fun getEffectRange(): Float = 100f
            override fun canStack(): Boolean = false
            override fun reset() {}
        }

        val unit1 = makeSpecialUnit("a")
        val unit2 = makeSpecialUnit("b")
        manager.addEffect(unit1, ctrl1, field)
        manager.addEffect(unit2, ctrl2, field)

        manager.update(0.016f, field)

        assertEquals(1, updateCount1)
        assertEquals(1, updateCount2)
    }

    @Test
    fun `removeEffect calls onRemove and cleans up`() {
        var removed = false
        val ctrl = object : FieldEffectController {
            override fun onPlace(unit: GameUnit, field: BattleFieldAccess) {}
            override fun update(dt: Float, field: BattleFieldAccess) {}
            override fun onRemove() { removed = true }
            override fun getEffectRange(): Float = 100f
            override fun canStack(): Boolean = false
            override fun reset() {}
        }

        val unit = makeSpecialUnit("barrier_01")
        manager.addEffect(unit, ctrl, field)
        assertEquals(1, manager.activeCount())

        manager.removeEffect(unit)

        assertTrue(removed)
        assertEquals(0, manager.activeCount())
    }

    @Test
    fun `dead unit effects are auto-removed on update`() {
        var removed = false
        val ctrl = object : FieldEffectController {
            override fun onPlace(unit: GameUnit, field: BattleFieldAccess) {}
            override fun update(dt: Float, field: BattleFieldAccess) {}
            override fun onRemove() { removed = true }
            override fun getEffectRange(): Float = 100f
            override fun canStack(): Boolean = false
            override fun reset() {}
        }

        val unit = makeSpecialUnit("barrier_01")
        manager.addEffect(unit, ctrl, field)
        assertEquals(1, manager.activeCount())

        // Kill the unit
        unit.alive = false
        manager.update(0.016f, field)

        assertTrue(removed)
        assertEquals(0, manager.activeCount())
    }
}
