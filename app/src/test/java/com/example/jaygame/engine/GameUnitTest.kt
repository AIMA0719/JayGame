package com.example.jaygame.engine

import com.example.jaygame.data.UnitFamily
import com.example.jaygame.engine.math.Vec2
import org.junit.Assert.*
import org.junit.Test

class GameUnitTest {
    @Test
    fun `initFromBlueprint sets all fields correctly`() {
        val unit = GameUnit()
        val stats = UnitStats(hp=150f, baseATK=10f, baseSpeed=0.8f, range=40f, defense=10f, magicResist=3f, moveSpeed=70f, blockCount=1)
        val bp = UnitBlueprint(
            id="fire_tank_01", name="test", families=listOf(UnitFamily.FIRE),
            grade=UnitGrade.COMMON, role=UnitRole.TANK, attackRange=AttackRange.MELEE,
            damageType=DamageType.PHYSICAL, stats=stats, behaviorId="tank_blocker",
            ability=null, uniqueAbility=null, mergeResultId=null,
            isSummonable=true, summonWeight=60, unitCategory=UnitCategory.NORMAL,
            iconRes=0, description="test"
        )
        unit.initFromBlueprint(bp)
        assertEquals("fire_tank_01", unit.blueprintId)
        assertEquals(listOf(UnitFamily.FIRE), unit.families)
        assertEquals(UnitRole.TANK, unit.role)
        assertEquals(AttackRange.MELEE, unit.attackRange)
        assertEquals(DamageType.PHYSICAL, unit.damageType)
        assertEquals(UnitCategory.NORMAL, unit.unitCategory)
        assertEquals(150f, unit.hp, 0.01f)
        assertEquals(150f, unit.maxHp, 0.01f)
        assertEquals(10f, unit.defense, 0.01f)
        assertEquals(3f, unit.magicResist, 0.01f)
        assertEquals(1, unit.blockCount)
        assertEquals(UnitState.IDLE, unit.state)
    }

    @Test
    fun `reset clears all fields`() {
        val unit = GameUnit()
        // Set some state
        unit.blueprintId = "test"
        unit.hp = 50f
        unit.state = UnitState.ATTACKING
        unit.alive = true
        // Create a mock behavior that tracks reset calls
        var behaviorResetCalled = false
        unit.behavior = object : UnitBehavior {
            override fun update(unit: GameUnit, dt: Float, findEnemy: (Vec2, Float) -> Enemy?) {}
            override fun onAttack(unit: GameUnit, target: Enemy) = AttackResult(0f, false, false, false)
            override fun onTakeDamage(unit: GameUnit, damage: Float, isMagic: Boolean) {}
            override fun reset() { behaviorResetCalled = true }
        }
        unit.reset()
        assertTrue(behaviorResetCalled)
        assertNull(unit.behavior)
        assertFalse(unit.alive)
        assertEquals(UnitState.IDLE, unit.state)
    }

    @Test
    fun `dual family support`() {
        val unit = GameUnit()
        val stats = UnitStats(200f, 50f, 1.2f, 60f, 10f, 5f, 100f, 0)
        val bp = UnitBlueprint(
            id="hidden_test", name="hidden", families=listOf(UnitFamily.FIRE, UnitFamily.LIGHTNING),
            grade=UnitGrade.HERO, role=UnitRole.MELEE_DPS, attackRange=AttackRange.MELEE,
            damageType=DamageType.PHYSICAL, stats=stats, behaviorId="assassin_dash",
            ability=null, uniqueAbility=null, mergeResultId=null,
            isSummonable=false, summonWeight=0, unitCategory=UnitCategory.HIDDEN,
            iconRes=0, description="test"
        )
        unit.initFromBlueprint(bp)
        assertEquals(2, unit.families.size)
        assertTrue(UnitFamily.FIRE in unit.families)
        assertTrue(UnitFamily.LIGHTNING in unit.families)
        assertEquals(UnitCategory.HIDDEN, unit.unitCategory)
    }
}
