package com.example.jaygame.engine

import com.example.jaygame.data.UnitFamily
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MergeSystemTest {

    private lateinit var registry: BlueprintRegistry

    private val testJson = """
    [
      {
        "id": "fire_archer_01",
        "name": "화염 궁수 1",
        "families": ["FIRE"],
        "grade": "COMMON",
        "role": "RANGED_DPS",
        "attackRange": "RANGED",
        "damageType": "PHYSICAL",
        "stats": { "hp": 80, "baseATK": 12, "baseSpeed": 1.0, "range": 150, "defense": 2, "magicResist": 0, "moveSpeed": 75, "blockCount": 0 },
        "behaviorId": "ranged_basic",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": "fire_archer_02",
        "isSummonable": true,
        "summonWeight": 60,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "기본 화염 궁수"
      },
      {
        "id": "fire_archer_02",
        "name": "화염 궁수 2",
        "families": ["FIRE"],
        "grade": "RARE",
        "role": "RANGED_DPS",
        "attackRange": "RANGED",
        "damageType": "PHYSICAL",
        "stats": { "hp": 120, "baseATK": 20, "baseSpeed": 1.2, "range": 160, "defense": 3, "magicResist": 1, "moveSpeed": 75, "blockCount": 0 },
        "behaviorId": "ranged_basic",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": "fire_archer_03",
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "강화된 화염 궁수"
      },
      {
        "id": "fire_archer_03",
        "name": "화염 궁수 3",
        "families": ["FIRE"],
        "grade": "HERO",
        "role": "RANGED_DPS",
        "attackRange": "RANGED",
        "damageType": "PHYSICAL",
        "stats": { "hp": 180, "baseATK": 35, "baseSpeed": 1.4, "range": 170, "defense": 5, "magicResist": 2, "moveSpeed": 80, "blockCount": 0 },
        "behaviorId": "ranged_basic",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": null,
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "최강 화염 궁수"
      },
      {
        "id": "hidden_unit_01",
        "name": "숨겨진 유닛",
        "families": ["FIRE"],
        "grade": "HERO",
        "role": "MELEE_DPS",
        "attackRange": "MELEE",
        "damageType": "PHYSICAL",
        "stats": { "hp": 200, "baseATK": 50, "baseSpeed": 1.2, "range": 60, "defense": 10, "magicResist": 5, "moveSpeed": 100, "blockCount": 0 },
        "behaviorId": "assassin_dash",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": "fire_archer_03",
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "HIDDEN",
        "iconRes": 0,
        "description": "숨겨진 유닛"
      },
      {
        "id": "special_unit_01",
        "name": "특수 유닛",
        "families": ["LIGHTNING"],
        "grade": "LEGEND",
        "role": "SUPPORT",
        "attackRange": "RANGED",
        "damageType": "MAGIC",
        "stats": { "hp": 100, "baseATK": 5, "baseSpeed": 0.8, "range": 200, "defense": 2, "magicResist": 8, "moveSpeed": 60, "blockCount": 0 },
        "behaviorId": "support_heal",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": null,
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "SPECIAL",
        "iconRes": 0,
        "description": "특수 유닛"
      }
    ]
    """.trimIndent()

    private fun makeUnit(blueprintId: String): GameUnit {
        val unit = GameUnit()
        val bp = registry.findById(blueprintId)!!
        unit.initFromBlueprint(bp)
        return unit
    }

    @Before
    fun setUp() {
        registry = BlueprintRegistry()
        registry.loadFromJson(testJson)
        MergeSystem.luckyMergeBonus = 0f
    }

    @Test
    fun `canMerge returns true for 3 same blueprintId units`() {
        val units = listOf(
            makeUnit("fire_archer_01"),
            makeUnit("fire_archer_01"),
            makeUnit("fire_archer_01"),
        )
        assertTrue(MergeSystem.canMerge(units, registry))
    }

    @Test
    fun `canMerge returns false for less than 3 same units`() {
        val units = listOf(
            makeUnit("fire_archer_01"),
            makeUnit("fire_archer_01"),
        )
        assertFalse(MergeSystem.canMerge(units, registry))
    }

    @Test
    fun `getMergeResult looks up mergeResultId from registry`() {
        val result = MergeSystem.getMergeResult("fire_archer_01", registry)
        assertNotNull(result)
        assertEquals("fire_archer_02", result!!.id)
        assertEquals(UnitGrade.RARE, result.grade)
    }

    @Test
    fun `hidden units cannot be merged`() {
        val units = listOf(
            makeUnit("hidden_unit_01"),
            makeUnit("hidden_unit_01"),
            makeUnit("hidden_unit_01"),
        )
        assertFalse(MergeSystem.canMerge(units, registry))
    }

    @Test
    fun `special units cannot be merged`() {
        val units = listOf(
            makeUnit("special_unit_01"),
            makeUnit("special_unit_01"),
            makeUnit("special_unit_01"),
        )
        assertFalse(MergeSystem.canMerge(units, registry))
    }

    @Test
    fun `units with null mergeResultId cannot merge further`() {
        // fire_archer_03 has mergeResultId = null (max grade)
        val units = listOf(
            makeUnit("fire_archer_03"),
            makeUnit("fire_archer_03"),
            makeUnit("fire_archer_03"),
        )
        assertFalse(MergeSystem.canMerge(units, registry))
    }

    @Test
    fun `getMergeResult returns null for null mergeResultId`() {
        val result = MergeSystem.getMergeResult("fire_archer_03", registry)
        assertNull(result)
    }

    @Test
    fun `getMergeResult returns null for unknown blueprintId`() {
        val result = MergeSystem.getMergeResult("nonexistent", registry)
        assertNull(result)
    }

    @Test
    fun `canMerge with mixed blueprintIds only merges matching groups`() {
        val units = listOf(
            makeUnit("fire_archer_01"),
            makeUnit("fire_archer_01"),
            makeUnit("fire_archer_02"),
        )
        assertFalse(MergeSystem.canMerge(units, registry))
    }
}
