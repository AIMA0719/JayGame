package com.example.jaygame.engine

import com.example.jaygame.data.UnitFamily
import org.junit.Assert.*
import org.junit.Test

class UnitBlueprintTest {
    @Test
    fun `UnitBlueprint creation with single family`() {
        val stats = UnitStats(hp=100f, baseATK=10f, baseSpeed=1f, range=50f, defense=5f, magicResist=0f, moveSpeed=80f, blockCount=1)
        val bp = UnitBlueprint(
            id="fire_tank_01", name="루비 가디언", families=listOf(UnitFamily.FIRE),
            grade=UnitGrade.COMMON, role=UnitRole.TANK, attackRange=AttackRange.MELEE,
            damageType=DamageType.PHYSICAL, stats=stats, behaviorId="tank_blocker",
            ability=null, uniqueAbility=null, mergeResultId="fire_tank_02",
            isSummonable=true, summonWeight=60, unitCategory=UnitCategory.NORMAL,
            iconRes=0, description="테스트"
        )
        assertEquals("fire_tank_01", bp.id)
        assertEquals(listOf(UnitFamily.FIRE), bp.families)
        assertEquals(UnitRole.TANK, bp.role)
        assertEquals(1, bp.stats.blockCount)
    }

    @Test
    fun `UnitBlueprint dual family for hidden unit`() {
        val stats = UnitStats(hp=200f, baseATK=50f, baseSpeed=1.2f, range=60f, defense=10f, magicResist=5f, moveSpeed=100f, blockCount=0)
        val bp = UnitBlueprint(
            id="hidden_thunder_flame_knight", name="뇌염의 기사",
            families=listOf(UnitFamily.FIRE, UnitFamily.LIGHTNING),
            grade=UnitGrade.HERO, role=UnitRole.MELEE_DPS, attackRange=AttackRange.MELEE,
            damageType=DamageType.PHYSICAL, stats=stats, behaviorId="assassin_dash",
            ability=null, uniqueAbility=null, mergeResultId=null,
            isSummonable=false, summonWeight=0, unitCategory=UnitCategory.HIDDEN,
            iconRes=0, description="듀얼"
        )
        assertEquals(2, bp.families.size)
        assertTrue(bp.families.contains(UnitFamily.FIRE))
        assertTrue(bp.families.contains(UnitFamily.LIGHTNING))
        assertEquals(UnitCategory.HIDDEN, bp.unitCategory)
    }

    @Test
    fun `UnitStats default values are correct`() {
        val stats = UnitStats(hp=100f, baseATK=10f, baseSpeed=1f, range=50f, defense=0f, magicResist=0f, moveSpeed=0f, blockCount=0)
        assertEquals(100f, stats.hp, 0.01f)
        assertEquals(0f, stats.defense, 0.01f)
        assertEquals(0, stats.blockCount)
    }

    @Test
    fun `AbilityDef creation`() {
        val ability = AbilityDef(
            id="fire_slash", name="화염참", type=AbilityTrigger.ACTIVE,
            damageType=DamageType.PHYSICAL, value=1.5f, cooldown=10f,
            range=60f, description="화염 베기"
        )
        assertEquals("fire_slash", ability.id)
        assertEquals(AbilityTrigger.ACTIVE, ability.type)
        assertEquals(10f, ability.cooldown, 0.01f)
    }

    @Test
    fun `UniqueAbilityDef with passive and active`() {
        val passive = AbilityDef("p1", "패시브", AbilityTrigger.PASSIVE, DamageType.MAGIC, 0.1f, 0f, 100f, "패시브 효과")
        val active = AbilityDef("a1", "액티브", AbilityTrigger.ACTIVE, DamageType.MAGIC, 3.0f, 12f, 150f, "액티브 효과")
        val unique = UniqueAbilityDef(
            id="phoenix_rebirth", name="불사조의 부활",
            passive=passive, active=active, requiredGrade=UnitGrade.HERO
        )
        assertNotNull(unique.passive)
        assertNotNull(unique.active)
        assertEquals(UnitGrade.HERO, unique.requiredGrade)
    }
}
