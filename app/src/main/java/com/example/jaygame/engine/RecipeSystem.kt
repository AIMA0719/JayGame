package com.example.jaygame.engine

import com.example.jaygame.data.UnitFamily
import org.json.JSONArray
import org.json.JSONObject

data class HiddenRecipe(
    val id: String,
    val ingredients: List<RecipeSlot>,
    val resultId: String,
    var discovered: Boolean = false
)

data class RecipeSlot(
    val family: UnitFamily?,
    val role: UnitRole?,
    val minGrade: UnitGrade,
    val specificUnitId: String?
)

class RecipeSystem(private val blueprintRegistry: BlueprintRegistry) {
    private val recipes = mutableListOf<HiddenRecipe>()
    private val discoveredIds = mutableSetOf<String>()

    fun loadRecipes(jsonString: String) {
        val arr = JSONArray(jsonString)
        for (i in 0 until arr.length()) {
            recipes.add(parseRecipe(arr.getJSONObject(i)))
        }
    }

    fun matchRecipe(unitA: GameUnit, unitB: GameUnit): HiddenRecipe? {
        return recipes.firstOrNull { recipe ->
            recipe.ingredients.size == 2 && matchesIngredients(recipe, listOf(unitA, unitB))
        }
    }

    fun matchPartial(unitA: GameUnit, unitB: GameUnit): HiddenRecipe? {
        return recipes.firstOrNull { recipe ->
            recipe.ingredients.size == 3 && matchesPartialIngredients(recipe, listOf(unitA, unitB))
        }
    }

    fun completeRecipe(recipe: HiddenRecipe, units: List<GameUnit>): UnitBlueprint? {
        if (!matchesIngredients(recipe, units)) return null
        discoveredIds.add(recipe.id)
        recipe.discovered = true
        return blueprintRegistry.findById(recipe.resultId)
    }

    fun isDiscovered(recipeId: String): Boolean = recipeId in discoveredIds
    fun allRecipes(): List<HiddenRecipe> = recipes.toList()
    fun setDiscoveredIds(ids: Set<String>) {
        discoveredIds.addAll(ids)
        recipes.forEach { if (it.id in discoveredIds) it.discovered = true }
    }
    fun getDiscoveredIds(): Set<String> = discoveredIds.toSet()

    private fun matchesIngredients(recipe: HiddenRecipe, units: List<GameUnit>): Boolean {
        if (units.size != recipe.ingredients.size) return false
        // Try all permutations of units against slots
        val slots = recipe.ingredients.toMutableList()
        val usedUnits = mutableSetOf<Int>()
        return tryMatch(slots, units, 0, usedUnits)
    }

    private fun tryMatch(slots: List<RecipeSlot>, units: List<GameUnit>, slotIdx: Int, used: MutableSet<Int>): Boolean {
        if (slotIdx >= slots.size) return true
        for (i in units.indices) {
            if (i in used) continue
            if (matchesSlot(slots[slotIdx], units[i])) {
                used.add(i)
                if (tryMatch(slots, units, slotIdx + 1, used)) return true
                used.remove(i)
            }
        }
        return false
    }

    private fun matchesPartialIngredients(recipe: HiddenRecipe, units: List<GameUnit>): Boolean {
        // Check if 2 of the 3 ingredients are matched
        val slots = recipe.ingredients.toMutableList()
        val used = mutableSetOf<Int>()
        var matchCount = 0
        for (slot in slots) {
            for (i in units.indices) {
                if (i in used) continue
                if (matchesSlot(slot, units[i])) {
                    used.add(i)
                    matchCount++
                    break
                }
            }
        }
        return matchCount >= 2
    }

    private fun matchesSlot(slot: RecipeSlot, unit: GameUnit): Boolean {
        if (slot.specificUnitId != null) return unit.blueprintId == slot.specificUnitId
        val familyMatch = slot.family == null || slot.family in unit.families
        val roleMatch = slot.role == null || slot.role == unit.role
        val gradeMatch = unit.grade >= slot.minGrade.ordinal
        return familyMatch && roleMatch && gradeMatch
    }

    private fun parseRecipe(obj: JSONObject): HiddenRecipe {
        val ingredientsArr = obj.getJSONArray("ingredients")
        val ingredients = (0 until ingredientsArr.length()).map { i ->
            val slot = ingredientsArr.getJSONObject(i)
            RecipeSlot(
                family = if (slot.isNull("family")) null else UnitFamily.valueOf(slot.getString("family")),
                role = if (slot.isNull("role")) null else UnitRole.valueOf(slot.getString("role")),
                minGrade = UnitGrade.valueOf(slot.getString("minGrade")),
                specificUnitId = if (slot.isNull("specificUnitId")) null else slot.getString("specificUnitId")
            )
        }
        return HiddenRecipe(
            id = obj.getString("id"),
            ingredients = ingredients,
            resultId = obj.getString("resultId"),
            discovered = false
        )
    }
}
