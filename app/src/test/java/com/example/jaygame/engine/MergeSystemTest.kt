package com.example.jaygame.engine

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MergeSystemTest {

    private lateinit var registry: BlueprintRegistry
    private lateinit var grid: Grid

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
        "mergeResultId": null,
        "isSummonable": true,
        "summonWeight": 60,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "기본 화염 궁수"
      },
      {
        "id": "frost_mage_01",
        "name": "냉기 마법사 1",
        "families": ["FROST"],
        "grade": "COMMON",
        "role": "RANGED_DPS",
        "attackRange": "RANGED",
        "damageType": "MAGIC",
        "stats": { "hp": 60, "baseATK": 15, "baseSpeed": 0.9, "range": 160, "defense": 1, "magicResist": 3, "moveSpeed": 70, "blockCount": 0 },
        "behaviorId": "ranged_basic",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": null,
        "isSummonable": true,
        "summonWeight": 60,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "기본 냉기 마법사"
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
        "mergeResultId": null,
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "강화된 화염 궁수"
      },
      {
        "id": "hidden_unit_01",
        "name": "숨겨진 유닛",
        "families": ["FIRE"],
        "grade": "COMMON",
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
        "description": "숨겨진 유닛"
      }
    ]
    """.trimIndent()

    private fun placeUnit(tileIndex: Int, blueprintId: String): GameUnit {
        val unit = GameUnit()
        val bp = registry.findById(blueprintId)!!
        unit.initFromBlueprint(bp)
        unit.tileIndex = tileIndex
        grid.placeUnit(tileIndex, unit)
        return unit
    }

    @Before
    fun setUp() {
        registry = BlueprintRegistry()
        registry.loadFromJson(testJson)
        grid = Grid()
        MergeSystem.luckyMergeBonus = 0f
    }

    @Test
    fun `3 same grade units can merge to next grade`() {
        placeUnit(0, "fire_archer_01")   // COMMON
        placeUnit(1, "frost_mage_01")    // COMMON
        placeUnit(2, "fire_archer_01")   // COMMON

        val result = MergeSystem.tryMergeBlueprint(grid, 0, registry)
        assertNotNull(result)
        // Result should be a RARE unit (fire_archer_02 is the only RARE NORMAL)
        assertEquals("fire_archer_02", result!!.resultBlueprintId)
        assertEquals(3, result.consumedTiles.size)
    }

    @Test
    fun `2 same grade units cannot merge`() {
        placeUnit(0, "fire_archer_01")  // COMMON
        placeUnit(1, "frost_mage_01")   // COMMON

        val result = MergeSystem.tryMergeBlueprint(grid, 0, registry)
        assertNull(result)
    }

    @Test
    fun `hidden units cannot be merged`() {
        placeUnit(0, "hidden_unit_01")  // HIDDEN COMMON
        placeUnit(1, "hidden_unit_01")
        placeUnit(2, "hidden_unit_01")

        val result = MergeSystem.tryMergeBlueprint(grid, 0, registry)
        assertNull(result)
    }

    @Test
    fun `findMergeableTiles returns tiles of mergeable same-grade groups`() {
        placeUnit(0, "fire_archer_01")   // COMMON
        placeUnit(1, "frost_mage_01")    // COMMON
        placeUnit(2, "fire_archer_01")   // COMMON

        val mergeable = MergeSystem.findMergeableTilesByBlueprint(grid, registry)
        assertEquals(setOf(0, 1, 2), mergeable)
    }

    @Test
    fun `findMergeableTiles excludes hidden units`() {
        placeUnit(0, "hidden_unit_01")
        placeUnit(1, "hidden_unit_01")
        placeUnit(2, "hidden_unit_01")

        val mergeable = MergeSystem.findMergeableTilesByBlueprint(grid, registry)
        assertTrue(mergeable.isEmpty())
    }

    @Test
    fun `max grade units cannot merge further`() {
        // RARE is the highest grade with a next (HERO), but no HERO NORMAL units exist
        placeUnit(0, "fire_archer_02")  // RARE
        placeUnit(1, "fire_archer_02")
        placeUnit(2, "fire_archer_02")

        val result = MergeSystem.tryMergeBlueprint(grid, 0, registry)
        // No HERO NORMAL blueprints → merge fails
        assertNull(result)
    }

    @Test
    fun `merge consumes tileIndex first`() {
        placeUnit(0, "fire_archer_01")
        placeUnit(1, "frost_mage_01")
        placeUnit(2, "fire_archer_01")
        placeUnit(3, "frost_mage_01")  // 4th COMMON

        val result = MergeSystem.tryMergeBlueprint(grid, 2, registry)
        assertNotNull(result)
        assertTrue(2 in result!!.consumedTiles)
        assertEquals(3, result.consumedTiles.size)
    }
}
