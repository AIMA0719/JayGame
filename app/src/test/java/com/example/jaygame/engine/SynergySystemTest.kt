package com.example.jaygame.engine

import com.example.jaygame.data.UnitFamily
import org.junit.Assert.*
import org.junit.Test

class SynergySystemTest {

    private fun makeUnit(
        families: List<UnitFamily>,
        category: UnitCategory = UnitCategory.NORMAL,
        alive: Boolean = true
    ): GameUnit {
        return GameUnit().apply {
            this.alive = alive
            this.families = families
            this.unitCategory = category
            this.role = UnitRole.RANGED_DPS
        }
    }

    @Test
    fun `dual family counted for both families`() {
        // 1 FIRE-only + 1 FIRE+LIGHTNING dual = FIRE count 2, LIGHTNING count 1
        val units = listOf(
            makeUnit(listOf(UnitFamily.FIRE)),
            makeUnit(listOf(UnitFamily.FIRE, UnitFamily.LIGHTNING))
        )
        val counts = SynergySystem.countFamilies(units)
        assertEquals(2, counts[UnitFamily.FIRE])
        assertEquals(1, counts[UnitFamily.LIGHTNING])

        // FIRE has 2 so it should get a synergy bonus
        val bonus = SynergySystem.getSynergyBonus(units, UnitFamily.FIRE)
        assertEquals(1.04f, bonus.atkMultiplier, 0.001f)
    }

    @Test
    fun `special units excluded from family count`() {
        // 2 FIRE normal + 1 FIRE SPECIAL = only 2 counted
        val units = listOf(
            makeUnit(listOf(UnitFamily.FIRE)),
            makeUnit(listOf(UnitFamily.FIRE)),
            makeUnit(listOf(UnitFamily.FIRE), category = UnitCategory.SPECIAL)
        )
        val counts = SynergySystem.countFamilies(units)
        assertEquals(2, counts[UnitFamily.FIRE])

        // Should get 2-count bonus, not full (3-count) bonus
        val bonus = SynergySystem.getSynergyBonus(units, UnitFamily.FIRE)
        assertEquals(1.04f, bonus.atkMultiplier, 0.001f)
        assertEquals(SynergySystem.SpecialEffect.NONE, bonus.specialEffect)
    }
}
