package com.jay.jaygame.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class DamageCalculatorTest {

    @Test
    fun `calculateDamageToUnit physical with defense`() {
        // defense=100 -> reduction = 100/(100+100) = 0.5 -> 50% damage
        val unit = GameUnit()
        unit.defense = 100f
        unit.magicResist = 0f
        unit.hp = 1000f
        val result = DamageCalculator.calculateDamageToUnit(200f, isMagic = false, unit)
        assertEquals(100f, result, 0.01f)
    }

    @Test
    fun `calculateDamageToUnit magic with magicResist`() {
        val unit = GameUnit()
        unit.defense = 0f
        unit.magicResist = 50f
        unit.hp = 1000f
        // reduction = 50/(50+100) = 0.333 -> 66.7% damage
        val result = DamageCalculator.calculateDamageToUnit(150f, isMagic = true, unit)
        assertEquals(100f, result, 0.5f)
    }

    @Test
    fun `calculateDamageToUnit zero defense full damage`() {
        val unit = GameUnit()
        unit.defense = 0f
        unit.magicResist = 0f
        val result = DamageCalculator.calculateDamageToUnit(100f, isMagic = false, unit)
        assertEquals(100f, result, 0.01f)
    }
}
