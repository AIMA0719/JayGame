package com.example.jaygame.engine

import com.example.jaygame.data.UnitFamily
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RecipeSystemTest {

    private lateinit var registry: BlueprintRegistry
    private lateinit var recipeSystem: RecipeSystem

    private val blueprintJson = """
    [
      {
        "id": "fire_melee_hero",
        "name": "Fire Melee Hero",
        "families": ["FIRE"],
        "grade": "HERO",
        "role": "MELEE_DPS",
        "attackRange": "MELEE",
        "damageType": "PHYSICAL",
        "stats": { "hp": 200, "baseATK": 50, "baseSpeed": 1.2, "range": 60, "defense": 10, "magicResist": 5, "moveSpeed": 100, "blockCount": 0 },
        "behaviorId": "melee_basic",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": null,
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "Fire melee hero"
      },
      {
        "id": "lightning_melee_hero",
        "name": "Lightning Melee Hero",
        "families": ["LIGHTNING"],
        "grade": "HERO",
        "role": "MELEE_DPS",
        "attackRange": "MELEE",
        "damageType": "PHYSICAL",
        "stats": { "hp": 180, "baseATK": 55, "baseSpeed": 1.4, "range": 60, "defense": 8, "magicResist": 3, "moveSpeed": 110, "blockCount": 0 },
        "behaviorId": "melee_basic",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": null,
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "Lightning melee hero"
      },
      {
        "id": "frost_controller_rare",
        "name": "Frost Controller",
        "families": ["FROST"],
        "grade": "RARE",
        "role": "CONTROLLER",
        "attackRange": "RANGED",
        "damageType": "MAGIC",
        "stats": { "hp": 100, "baseATK": 30, "baseSpeed": 0.9, "range": 150, "defense": 4, "magicResist": 6, "moveSpeed": 70, "blockCount": 0 },
        "behaviorId": "controller_basic",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": null,
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "Frost controller"
      },
      {
        "id": "fire_ranged_common",
        "name": "Fire Ranged Common",
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
        "description": "Basic fire ranged"
      },
      {
        "id": "specific_unit_abc",
        "name": "Specific Unit",
        "families": ["WIND"],
        "grade": "LEGEND",
        "role": "RANGED_DPS",
        "attackRange": "RANGED",
        "damageType": "PHYSICAL",
        "stats": { "hp": 150, "baseATK": 40, "baseSpeed": 1.3, "range": 180, "defense": 5, "magicResist": 3, "moveSpeed": 85, "blockCount": 0 },
        "behaviorId": "ranged_basic",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": null,
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "NORMAL",
        "iconRes": 0,
        "description": "Specific unit for recipe test"
      },
      {
        "id": "hidden_test_result",
        "name": "Hidden Test Result",
        "families": ["FIRE", "LIGHTNING"],
        "grade": "LEGEND",
        "role": "MELEE_DPS",
        "attackRange": "MELEE",
        "damageType": "PHYSICAL",
        "stats": { "hp": 400, "baseATK": 100, "baseSpeed": 1.3, "range": 70, "defense": 15, "magicResist": 8, "moveSpeed": 105, "blockCount": 0 },
        "behaviorId": "melee_basic",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": null,
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "HIDDEN",
        "iconRes": 0,
        "description": "Hidden crafted unit"
      },
      {
        "id": "hidden_specific_result",
        "name": "Hidden Specific Result",
        "families": ["WIND"],
        "grade": "ANCIENT",
        "role": "RANGED_DPS",
        "attackRange": "RANGED",
        "damageType": "PHYSICAL",
        "stats": { "hp": 300, "baseATK": 80, "baseSpeed": 1.5, "range": 200, "defense": 10, "magicResist": 5, "moveSpeed": 90, "blockCount": 0 },
        "behaviorId": "ranged_basic",
        "ability": null,
        "uniqueAbility": null,
        "mergeResultId": null,
        "isSummonable": false,
        "summonWeight": 0,
        "unitCategory": "HIDDEN",
        "iconRes": 0,
        "description": "Hidden specific result"
      }
    ]
    """.trimIndent()

    private val testRecipeJson = """[
        {
            "id": "recipe_test",
            "ingredients": [
                { "family": "FIRE", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null },
                { "family": "LIGHTNING", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null }
            ],
            "resultId": "hidden_test_result"
        },
        {
            "id": "recipe_wildcard_family",
            "ingredients": [
                { "family": null, "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null },
                { "family": "LIGHTNING", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null }
            ],
            "resultId": "hidden_test_result"
        },
        {
            "id": "recipe_specific",
            "ingredients": [
                { "family": null, "role": null, "minGrade": "COMMON", "specificUnitId": "specific_unit_abc" },
                { "family": "FIRE", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null }
            ],
            "resultId": "hidden_specific_result"
        }
    ]""".trimIndent()

    private fun makeUnit(blueprintId: String): GameUnit {
        val unit = GameUnit()
        val bp = registry.findById(blueprintId)!!
        unit.initFromBlueprint(bp)
        return unit
    }

    @Before
    fun setUp() {
        registry = BlueprintRegistry()
        registry.loadFromJson(blueprintJson)
        recipeSystem = RecipeSystem(registry)
    }

    @Test
    fun `matchRecipe returns recipe for valid 2-ingredient combination`() {
        recipeSystem.loadRecipes("""[{
            "id": "recipe_test",
            "ingredients": [
                { "family": "FIRE", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null },
                { "family": "LIGHTNING", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null }
            ],
            "resultId": "hidden_test_result"
        }]""")

        val fireUnit = makeUnit("fire_melee_hero")
        val lightningUnit = makeUnit("lightning_melee_hero")

        val recipe = recipeSystem.matchRecipe(fireUnit, lightningUnit)
        assertNotNull(recipe)
        assertEquals("recipe_test", recipe!!.id)
        assertEquals("hidden_test_result", recipe.resultId)
    }

    @Test
    fun `matchRecipe returns null for non-matching combination`() {
        recipeSystem.loadRecipes("""[{
            "id": "recipe_test",
            "ingredients": [
                { "family": "FIRE", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null },
                { "family": "LIGHTNING", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null }
            ],
            "resultId": "hidden_test_result"
        }]""")

        val fireUnit = makeUnit("fire_melee_hero")
        val frostUnit = makeUnit("frost_controller_rare")

        val recipe = recipeSystem.matchRecipe(fireUnit, frostUnit)
        assertNull(recipe)
    }

    @Test
    fun `matchRecipe works regardless of unit order`() {
        recipeSystem.loadRecipes("""[{
            "id": "recipe_test",
            "ingredients": [
                { "family": "FIRE", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null },
                { "family": "LIGHTNING", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null }
            ],
            "resultId": "hidden_test_result"
        }]""")

        val fireUnit = makeUnit("fire_melee_hero")
        val lightningUnit = makeUnit("lightning_melee_hero")

        // Try both orders
        val recipe1 = recipeSystem.matchRecipe(fireUnit, lightningUnit)
        val recipe2 = recipeSystem.matchRecipe(lightningUnit, fireUnit)

        assertNotNull(recipe1)
        assertNotNull(recipe2)
        assertEquals(recipe1!!.id, recipe2!!.id)
    }

    @Test
    fun `null family in slot matches any family`() {
        recipeSystem.loadRecipes("""[{
            "id": "recipe_wildcard",
            "ingredients": [
                { "family": null, "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null },
                { "family": "LIGHTNING", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null }
            ],
            "resultId": "hidden_test_result"
        }]""")

        val fireUnit = makeUnit("fire_melee_hero")
        val lightningUnit = makeUnit("lightning_melee_hero")

        val recipe = recipeSystem.matchRecipe(fireUnit, lightningUnit)
        assertNotNull(recipe)
        assertEquals("recipe_wildcard", recipe!!.id)
    }

    @Test
    fun `grade below minGrade does not match`() {
        recipeSystem.loadRecipes("""[{
            "id": "recipe_high_grade",
            "ingredients": [
                { "family": "FIRE", "role": "RANGED_DPS", "minGrade": "HERO", "specificUnitId": null },
                { "family": "LIGHTNING", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null }
            ],
            "resultId": "hidden_test_result"
        }]""")

        // fire_ranged_common is COMMON grade, recipe requires HERO
        val commonUnit = makeUnit("fire_ranged_common")
        val lightningUnit = makeUnit("lightning_melee_hero")

        val recipe = recipeSystem.matchRecipe(commonUnit, lightningUnit)
        assertNull(recipe)
    }

    @Test
    fun `specificUnitId overrides family and role`() {
        recipeSystem.loadRecipes("""[{
            "id": "recipe_specific",
            "ingredients": [
                { "family": null, "role": null, "minGrade": "COMMON", "specificUnitId": "specific_unit_abc" },
                { "family": "FIRE", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null }
            ],
            "resultId": "hidden_specific_result"
        }]""")

        val specificUnit = makeUnit("specific_unit_abc")
        val fireUnit = makeUnit("fire_melee_hero")

        val recipe = recipeSystem.matchRecipe(specificUnit, fireUnit)
        assertNotNull(recipe)
        assertEquals("recipe_specific", recipe!!.id)

        // Wrong specific unit should not match
        val wrongUnit = makeUnit("fire_ranged_common")
        val recipe2 = recipeSystem.matchRecipe(wrongUnit, fireUnit)
        assertNull(recipe2)
    }

    @Test
    fun `completeRecipe marks as discovered`() {
        recipeSystem.loadRecipes("""[{
            "id": "recipe_test",
            "ingredients": [
                { "family": "FIRE", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null },
                { "family": "LIGHTNING", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null }
            ],
            "resultId": "hidden_test_result"
        }]""")

        val fireUnit = makeUnit("fire_melee_hero")
        val lightningUnit = makeUnit("lightning_melee_hero")

        val recipe = recipeSystem.matchRecipe(fireUnit, lightningUnit)!!
        assertFalse(recipe.discovered)
        assertFalse(recipeSystem.isDiscovered("recipe_test"))

        val resultBp = recipeSystem.completeRecipe(recipe, listOf(fireUnit, lightningUnit))
        assertNotNull(resultBp)
        assertEquals("hidden_test_result", resultBp!!.id)
        assertTrue(recipe.discovered)
        assertTrue(recipeSystem.isDiscovered("recipe_test"))
    }

    @Test
    fun `isDiscovered returns correct state`() {
        recipeSystem.loadRecipes("""[{
            "id": "recipe_test",
            "ingredients": [
                { "family": "FIRE", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null },
                { "family": "LIGHTNING", "role": "MELEE_DPS", "minGrade": "HERO", "specificUnitId": null }
            ],
            "resultId": "hidden_test_result"
        }]""")

        assertFalse(recipeSystem.isDiscovered("recipe_test"))
        assertFalse(recipeSystem.isDiscovered("nonexistent"))

        // Set discovered via setDiscoveredIds
        recipeSystem.setDiscoveredIds(setOf("recipe_test"))
        assertTrue(recipeSystem.isDiscovered("recipe_test"))
        assertFalse(recipeSystem.isDiscovered("nonexistent"))

        // Verify getDiscoveredIds returns the set
        val ids = recipeSystem.getDiscoveredIds()
        assertTrue("recipe_test" in ids)
    }
}
