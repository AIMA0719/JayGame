package com.example.jaygame.engine

import com.example.jaygame.data.UnitFamily
import org.junit.Assert.*
import org.junit.Test

class RoleSynergySystemTest {

    private fun makeUnit(
        role: UnitRole,
        category: UnitCategory = UnitCategory.NORMAL,
        alive: Boolean = true
    ): GameUnit {
        return GameUnit().apply {
            this.alive = alive
            this.role = role
            this.unitCategory = category
            this.families = listOf(UnitFamily.FIRE)
        }
    }

    @Test
    fun `no bonus for 1 tank`() {
        val units = listOf(makeUnit(UnitRole.TANK))
        val bonus = RoleSynergySystem.getBonus(units, UnitRole.TANK)
        assertEquals(0f, bonus.blockTimeBonus, 0.001f)
        assertEquals(0, bonus.blockCountBonus)
        assertEquals(RoleSynergySystem.RoleSpecialEffect.NONE, bonus.specialEffect)
    }

    @Test
    fun `2 tanks gives block time bonus`() {
        val units = listOf(makeUnit(UnitRole.TANK), makeUnit(UnitRole.TANK))
        val bonus = RoleSynergySystem.getBonus(units, UnitRole.TANK)
        assertEquals(0.2f, bonus.blockTimeBonus, 0.001f)
        assertEquals(0, bonus.blockCountBonus)
        assertEquals(RoleSynergySystem.RoleSpecialEffect.NONE, bonus.specialEffect)
    }

    @Test
    fun `3 ranged gives crit bonus`() {
        val units = listOf(
            makeUnit(UnitRole.RANGED_DPS),
            makeUnit(UnitRole.RANGED_DPS),
            makeUnit(UnitRole.RANGED_DPS)
        )
        val bonus = RoleSynergySystem.getBonus(units, UnitRole.RANGED_DPS)
        assertEquals(1.1f, bonus.rangeMultiplier, 0.001f)
        assertEquals(0.05f, bonus.critBonus, 0.001f)
        assertEquals(RoleSynergySystem.RoleSpecialEffect.NONE, bonus.specialEffect)
    }

    @Test
    fun `4 melee gives instant re-dash special effect`() {
        val units = listOf(
            makeUnit(UnitRole.MELEE_DPS),
            makeUnit(UnitRole.MELEE_DPS),
            makeUnit(UnitRole.MELEE_DPS),
            makeUnit(UnitRole.MELEE_DPS)
        )
        val bonus = RoleSynergySystem.getBonus(units, UnitRole.MELEE_DPS)
        assertEquals(0.15f, bonus.dashDamageBonus, 0.001f)
        assertEquals(0.2f, bonus.dashCooldownReduction, 0.001f)
        assertEquals(RoleSynergySystem.RoleSpecialEffect.INSTANT_REDASH, bonus.specialEffect)
    }

    @Test
    fun `special units excluded from count`() {
        // 2 normal tanks + 1 special tank = should only count 2
        val units = listOf(
            makeUnit(UnitRole.TANK),
            makeUnit(UnitRole.TANK),
            makeUnit(UnitRole.TANK, category = UnitCategory.SPECIAL)
        )
        val bonus = RoleSynergySystem.getBonus(units, UnitRole.TANK)
        // Only 2 tanks counted (SPECIAL excluded), so blockCountBonus = 0
        assertEquals(0.2f, bonus.blockTimeBonus, 0.001f)
        assertEquals(0, bonus.blockCountBonus)
        assertEquals(RoleSynergySystem.RoleSpecialEffect.NONE, bonus.specialEffect)
    }
}
