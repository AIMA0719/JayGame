package com.jay.jaygame.engine

import com.jay.jaygame.data.UnitRace
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test

class MergeSystemTest {

    private lateinit var registry: BlueprintRegistry
    private lateinit var grid: Grid

    /**
     * 테스트용 블루프린트 JSON:
     * - COMMON 2종 (fire_archer_01, ice_mage_01)
     * - RARE 2종 (fire_archer_02, ice_mage_02)
     * - HERO 1종 (fire_hero_01)
     * - LEGEND 1종 (fire_legend_01) — 합성 불가 (MYTHIC은 레시피 전용)
     * - HIDDEN COMMON 1종 (hidden_unit_01) — 합성 가능
     * - SPECIAL COMMON 1종 (special_unit_01) — 합성 불가
     *
     * 모든 유닛은 race=HUMAN (종족 기반 합성 시스템)
     */
    private val testJson = """
    [
      {
        "id": "fire_archer_01",
        "name": "화염 궁수 1",
        "race": "HUMAN",
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
        "id": "ice_mage_01",
        "name": "냉기 마법사 1",
        "race": "HUMAN",
        "families": ["FROST"],
        "grade": "COMMON",
        "role": "RANGED_DPS",
        "attackRange": "RANGED",
        "damageType": "MAGIC",
        "stats": { "hp": 70, "baseATK": 15, "baseSpeed": 0.9, "range": 160, "defense": 1, "magicResist": 3, "moveSpeed": 70, "blockCount": 0 },
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
        "race": "HUMAN",
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
        "id": "ice_mage_02",
        "name": "냉기 마법사 2",
        "race": "HUMAN",
        "families": ["FROST"],
        "grade": "RARE",
        "role": "RANGED_DPS",
        "attackRange": "RANGED",
        "damageType": "MAGIC",
        "stats": { "hp": 100, "baseATK": 25, "baseSpeed": 1.0, "range": 170, "defense": 2, "magicResist": 5, "moveSpeed": 70, "blockCount": 0 },
        "behaviorId": "ranged_basic",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": null,
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "강화된 냉기 마법사"
      },
      {
        "id": "fire_hero_01",
        "name": "화염 고대",
        "race": "HUMAN",
        "families": ["FIRE"],
        "grade": "HERO",
        "role": "RANGED_DPS",
        "attackRange": "RANGED",
        "damageType": "PHYSICAL",
        "stats": { "hp": 200, "baseATK": 40, "baseSpeed": 1.3, "range": 180, "defense": 5, "magicResist": 3, "moveSpeed": 75, "blockCount": 0 },
        "behaviorId": "ranged_basic",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": null,
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "화염 고대 유닛"
      },
      {
        "id": "fire_legend_01",
        "name": "화염 전설",
        "race": "HUMAN",
        "families": ["FIRE"],
        "grade": "LEGEND",
        "role": "RANGED_DPS",
        "attackRange": "RANGED",
        "damageType": "PHYSICAL",
        "stats": { "hp": 400, "baseATK": 80, "baseSpeed": 1.5, "range": 200, "defense": 10, "magicResist": 5, "moveSpeed": 75, "blockCount": 0 },
        "behaviorId": "ranged_basic",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": null,
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "화염 전설 유닛"
      },
      {
        "id": "hidden_unit_01",
        "name": "숨겨진 유닛",
        "race": "HUMAN",
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
      },
      {
        "id": "special_unit_01",
        "name": "특수 유닛",
        "race": "HUMAN",
        "families": ["FIRE"],
        "grade": "COMMON",
        "role": "MELEE_DPS",
        "attackRange": "MELEE",
        "damageType": "PHYSICAL",
        "stats": { "hp": 150, "baseATK": 30, "baseSpeed": 1.0, "range": 60, "defense": 5, "magicResist": 2, "moveSpeed": 80, "blockCount": 0 },
        "behaviorId": "ranged_basic",
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
        grid = Grid()
        MergeSystem.luckyMergeBonus = 0f
        MergeSystem.randomOverride = null
    }

    @After
    fun tearDown() {
        MergeSystem.randomOverride = null
    }

    @Test
    fun `COMMON merge produces random RARE unit`() {
        var callCount = 0
        MergeSystem.randomOverride = {
            when (callCount++) {
                0 -> 0.5  // lucky check: not lucky
                1 -> 0.0  // pool selection: first unit
                else -> 0.5
            }
        }
        val result = MergeSystem.determineMergeResult(UnitRace.HUMAN, UnitGrade.COMMON, registry)
        assertNotNull(result)
        val resultBp = registry.findById(result!!.resultBlueprintId)!!
        assertEquals(UnitGrade.RARE, resultBp.grade)
        assertFalse(result.isLucky)
    }

    @Test
    fun `COMMON merge result is from RARE pool`() {
        var callCount = 0
        MergeSystem.randomOverride = {
            when (callCount++) {
                0 -> 0.5  // lucky check: not lucky
                1 -> 0.99 // pool selection: last unit
                else -> 0.5
            }
        }
        val result = MergeSystem.determineMergeResult(UnitRace.HUMAN, UnitGrade.COMMON, registry)
        assertNotNull(result)
        val resultBp = registry.findById(result!!.resultBlueprintId)!!
        assertEquals(UnitGrade.RARE, resultBp.grade)
    }

    @Test
    fun `RARE merge produces random HERO unit`() {
        var callCount = 0
        MergeSystem.randomOverride = {
            when (callCount++) {
                0 -> 0.5  // not lucky
                1 -> 0.0  // first in pool
                else -> 0.5
            }
        }
        val result = MergeSystem.determineMergeResult(UnitRace.HUMAN, UnitGrade.RARE, registry)
        assertNotNull(result)
        val resultBp = registry.findById(result!!.resultBlueprintId)!!
        assertEquals(UnitGrade.HERO, resultBp.grade)
        assertFalse(result.isLucky)
    }

    @Test
    fun `HERO merge produces random LEGEND unit`() {
        var callCount = 0
        MergeSystem.randomOverride = {
            when (callCount++) {
                0 -> 0.5  // not lucky
                1 -> 0.0  // first in pool
                else -> 0.5
            }
        }
        val result = MergeSystem.determineMergeResult(UnitRace.HUMAN, UnitGrade.HERO, registry)
        assertNotNull(result)
        val resultBp = registry.findById(result!!.resultBlueprintId)!!
        assertEquals(UnitGrade.LEGEND, resultBp.grade)
        assertFalse(result.isLucky)
    }

    @Test
    fun `LEGEND cannot merge - MYTHIC is recipe only`() {
        val result = MergeSystem.determineMergeResult(UnitRace.HUMAN, UnitGrade.LEGEND, registry)
        assertNull("LEGEND should not merge via normal merge", result)
    }

    @Test
    fun `lucky merge skips one grade - COMMON to HERO`() {
        var callCount = 0
        MergeSystem.randomOverride = {
            when (callCount++) {
                0 -> 0.01 // lucky check: lucky! (< 0.05)
                1 -> 0.0  // pool selection
                else -> 0.5
            }
        }
        val result = MergeSystem.determineMergeResult(UnitRace.HUMAN, UnitGrade.COMMON, registry)
        assertNotNull(result)
        assertTrue(result!!.isLucky)
        val resultBp = registry.findById(result.resultBlueprintId)!!
        assertEquals(UnitGrade.HERO, resultBp.grade)
    }

    @Test
    fun `lucky merge RARE to LEGEND`() {
        var callCount = 0
        MergeSystem.randomOverride = {
            when (callCount++) {
                0 -> 0.01 // lucky!
                1 -> 0.0  // pool selection
                else -> 0.5
            }
        }
        val result = MergeSystem.determineMergeResult(UnitRace.HUMAN, UnitGrade.RARE, registry)
        assertNotNull(result)
        assertTrue(result!!.isLucky)
        val resultBp = registry.findById(result.resultBlueprintId)!!
        assertEquals(UnitGrade.LEGEND, resultBp.grade)
    }

    @Test
    fun `lucky merge HERO falls back to LEGEND - MYTHIC is recipe only`() {
        // HERO lucky would be MYTHIC, but MYTHIC is recipe-only so falls back to LEGEND
        var callCount = 0
        MergeSystem.randomOverride = {
            when (callCount++) {
                0 -> 0.01 // lucky!
                1 -> 0.0  // pool selection
                else -> 0.5
            }
        }
        val result = MergeSystem.determineMergeResult(UnitRace.HUMAN, UnitGrade.HERO, registry)
        assertNotNull(result)
        // isLucky is false because targetGrade == nextGrade (fell back)
        assertFalse(result!!.isLucky)
        val resultBp = registry.findById(result.resultBlueprintId)!!
        assertEquals(UnitGrade.LEGEND, resultBp.grade)
    }

    @Test
    fun `HIDDEN unit can also merge by grade`() {
        // HIDDEN units with race HUMAN, COMMON grade should still merge
        var callCount = 0
        MergeSystem.randomOverride = {
            when (callCount++) {
                0 -> 0.5  // not lucky
                1 -> 0.0  // pool selection
                else -> 0.5
            }
        }
        val result = MergeSystem.determineMergeResult(UnitRace.HUMAN, UnitGrade.COMMON, registry)
        assertNotNull(result)
        val resultBp = registry.findById(result!!.resultBlueprintId)!!
        assertEquals(UnitGrade.RARE, resultBp.grade)
    }

    @Test
    fun `slot stacking places same units together`() {
        val u1 = makeUnit("fire_archer_01")
        val u2 = makeUnit("fire_archer_01")
        val u3 = makeUnit("fire_archer_01")

        assertTrue(grid.placeUnit(0, u1))
        assertTrue(grid.placeUnit(0, u2))  // stacks on same slot
        assertTrue(grid.placeUnit(0, u3))  // stacks to 3

        assertEquals(3, grid.getStackCount(0))
    }

    @Test
    fun `different units cannot stack in same slot`() {
        val u1 = makeUnit("fire_archer_01")
        val u2 = makeUnit("hidden_unit_01")  // different blueprint

        assertTrue(grid.placeUnit(0, u1))
        assertFalse(grid.placeUnit(0, u2))  // should fail

        assertEquals(1, grid.getStackCount(0))
    }

    @Test
    fun `slots with 3 units have merge-ready stack count`() {
        repeat(3) { grid.placeUnit(0, makeUnit("fire_archer_01")) }
        grid.placeUnit(1, makeUnit("fire_archer_01"))  // only 1

        assertEquals(3, grid.getStackCount(0))
        assertEquals(1, grid.getStackCount(1))
    }

    @Test
    fun `findMergeableSlots returns full stacks with valid next grade`() {
        repeat(3) { grid.placeUnit(0, makeUnit("fire_archer_01")) }

        val mergeable = MergeSystem.findMergeableSlots(grid, registry)
        assertTrue(0 in mergeable)
    }

    @Test
    fun `findMergeableSlots excludes LEGEND slots`() {
        repeat(3) { grid.placeUnit(0, makeUnit("fire_legend_01")) }

        val mergeable = MergeSystem.findMergeableSlots(grid, registry)
        assertFalse(0 in mergeable)
    }

    @Test
    fun `findMergeableSlots excludes SPECIAL units`() {
        repeat(3) { grid.placeUnit(0, makeUnit("special_unit_01")) }

        val mergeable = MergeSystem.findMergeableSlots(grid, registry)
        assertFalse(0 in mergeable)
    }

    @Test
    fun `removeAllFromSlot clears the slot`() {
        repeat(3) { grid.placeUnit(0, makeUnit("fire_archer_01")) }
        val removed = grid.removeAllFromSlot(0)
        assertEquals(3, removed.size)
        assertEquals(0, grid.getStackCount(0))
    }

    @Test
    fun `lucky merge bonus increases lucky chance`() {
        MergeSystem.luckyMergeBonus = 0.95f  // 총 1.0 확률
        var callCount = 0
        MergeSystem.randomOverride = {
            when (callCount++) {
                0 -> 0.5  // 0.5 < 1.0 so lucky
                1 -> 0.0  // pool selection
                else -> 0.5
            }
        }
        val result = MergeSystem.determineMergeResult(UnitRace.HUMAN, UnitGrade.COMMON, registry)
        assertNotNull(result)
        assertTrue(result!!.isLucky)
        val resultBp = registry.findById(result.resultBlueprintId)!!
        assertEquals(UnitGrade.HERO, resultBp.grade)
    }
}
