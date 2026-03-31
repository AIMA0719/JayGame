package com.jay.jaygame.engine

import com.jay.jaygame.data.UnitFamily
import org.junit.Assert.*
import org.junit.Test

class BlueprintRegistryTest {
    private val testJson = """
    [
      {
        "id": "fire_tank_01",
        "name": "화염 수호자",
        "families": ["FIRE"],
        "grade": "COMMON",
        "role": "TANK",
        "attackRange": "MELEE",
        "damageType": "PHYSICAL",
        "stats": { "hp": 150, "baseATK": 8, "baseSpeed": 0.8, "range": 40, "defense": 10, "magicResist": 3, "moveSpeed": 70, "blockCount": 1 },
        "behaviorId": "tank_blocker",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": "fire_tank_02",
        "isSummonable": true,
        "summonWeight": 60,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "화염의 수호자"
      },
      {
        "id": "fire_melee_01",
        "name": "화염 검사",
        "families": ["FIRE"],
        "grade": "COMMON",
        "role": "MELEE_DPS",
        "attackRange": "MELEE",
        "damageType": "PHYSICAL",
        "stats": { "hp": 80, "baseATK": 15, "baseSpeed": 1.2, "range": 50, "defense": 3, "magicResist": 0, "moveSpeed": 120, "blockCount": 0 },
        "behaviorId": "assassin_dash",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": "fire_melee_02",
        "isSummonable": true,
        "summonWeight": 60,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "화염의 검사"
      },
      {
        "id": "hidden_flame_knight",
        "name": "뇌염의 기사",
        "families": ["FIRE", "LIGHTNING"],
        "grade": "HERO",
        "role": "MELEE_DPS",
        "attackRange": "MELEE",
        "damageType": "PHYSICAL",
        "stats": { "hp": 200, "baseATK": 50, "baseSpeed": 1.2, "range": 60, "defense": 10, "magicResist": 5, "moveSpeed": 100, "blockCount": 0 },
        "behaviorId": "assassin_dash",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": null,
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "HIDDEN",
        "iconRes": 0,
        "description": "듀얼 패밀리"
      }
    ]
    """.trimIndent()

    @Test
    fun `loadFromJson parses blueprints correctly`() {
        val registry = BlueprintRegistry()
        registry.loadFromJson(testJson)
        assertEquals(3, registry.count())
        val bp = registry.findById("fire_tank_01")
        assertNotNull(bp)
        assertEquals("화염 수호자", bp!!.name)
        assertEquals(UnitRole.TANK, bp.role)
        assertEquals(150f, bp.stats.hp, 0.01f)
        assertEquals(1, bp.stats.blockCount)
    }

    @Test
    fun `findById returns null for unknown id`() {
        val registry = BlueprintRegistry()
        registry.loadFromJson(testJson)
        assertNull(registry.findById("nonexistent"))
    }

    @Test
    fun `findByFamilyAndRole returns matching blueprints`() {
        val registry = BlueprintRegistry()
        registry.loadFromJson(testJson)
        val tanks = registry.findByFamilyAndRole(UnitFamily.FIRE, UnitRole.TANK)
        assertEquals(1, tanks.size)
        assertEquals("fire_tank_01", tanks[0].id)
    }

    @Test
    fun `findSummonable returns only summonable units`() {
        val registry = BlueprintRegistry()
        registry.loadFromJson(testJson)
        val summonable = registry.findSummonable()
        assertEquals(2, summonable.size)
        assertTrue(summonable.all { it.isSummonable })
        assertFalse(summonable.any { it.id == "hidden_flame_knight" })
    }

    @Test
    fun `findByCategory filters correctly`() {
        val registry = BlueprintRegistry()
        registry.loadFromJson(testJson)
        val hidden = registry.findByCategory(UnitCategory.HIDDEN)
        assertEquals(1, hidden.size)
        assertEquals("hidden_flame_knight", hidden[0].id)
        assertEquals(2, hidden[0].families.size)
    }

    @Test
    fun `dual family blueprint parsed correctly`() {
        val registry = BlueprintRegistry()
        registry.loadFromJson(testJson)
        val bp = registry.findById("hidden_flame_knight")!!
        assertEquals(listOf(UnitFamily.FIRE, UnitFamily.LIGHTNING), bp.families)
        assertNull(bp.mergeResultId)
        assertFalse(bp.isSummonable)
    }
}
