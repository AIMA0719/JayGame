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
        // 2 FIRE-only + 1 FIRE+LIGHTNING dual = FIRE count 3, LIGHTNING count 1
        val units = listOf(
            makeUnit(listOf(UnitFamily.FIRE)),
            makeUnit(listOf(UnitFamily.FIRE)),
            makeUnit(listOf(UnitFamily.FIRE, UnitFamily.LIGHTNING))
        )
        val counts = SynergySystem.countFamilies(units)
        assertEquals(3, counts[UnitFamily.FIRE])
        assertEquals(1, counts[UnitFamily.LIGHTNING])

        // 시너지 비활성화 — 종족 시스템으로 전환 예정, 기본값 반환
        val bonus = SynergySystem.getSynergyBonus(units, UnitFamily.FIRE)
        assertEquals(1.0f, bonus.atkMultiplier, 0.001f)
    }

    @Test
    fun `special units excluded from family count`() {
        // 3 FIRE normal + 1 FIRE SPECIAL = only 3 counted
        val units = listOf(
            makeUnit(listOf(UnitFamily.FIRE)),
            makeUnit(listOf(UnitFamily.FIRE)),
            makeUnit(listOf(UnitFamily.FIRE)),
            makeUnit(listOf(UnitFamily.FIRE), category = UnitCategory.SPECIAL)
        )
        val counts = SynergySystem.countFamilies(units)
        assertEquals(3, counts[UnitFamily.FIRE])

        // 시너지 비활성화 — 종족 시스템으로 전환 예정, 기본값 반환
        val bonus = SynergySystem.getSynergyBonus(units, UnitFamily.FIRE)
        assertEquals(1.0f, bonus.atkMultiplier, 0.001f)
        assertEquals(SynergySystem.SpecialEffect.NONE, bonus.specialEffect)
    }
}
