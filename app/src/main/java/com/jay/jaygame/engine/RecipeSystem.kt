@file:Suppress("DEPRECATION")
package com.jay.jaygame.engine

import com.jay.jaygame.data.UnitFamily
import org.json.JSONArray
import org.json.JSONObject

data class HiddenRecipe(
    val id: String,
    val ingredients: List<RecipeSlot>,
    val resultId: String,
    var discovered: Boolean = false,
    val luckyStonesCost: Int = 1,  // 조합석 소모량 (신화 레시피 합성 시 필요)
)

data class RecipeSlot(
    val family: UnitFamily?,
    val role: UnitRole?,
    val minGrade: UnitGrade,
    val specificUnitId: String?
)

class RecipeSystem(private val blueprintRegistry: BlueprintRegistry) {

    companion object {
        @Volatile
        lateinit var instance: RecipeSystem
            private set

        val isReady get() = ::instance.isInitialized

        /** Must be called AFTER BlueprintRegistry.initialize() */
        @Synchronized
        fun initialize(context: android.content.Context): Boolean {
            if (::instance.isInitialized) return true
            val system = RecipeSystem(BlueprintRegistry.instance)
            try {
                val recipesJson = context.assets.open("units/hidden_recipes.json")
                    .bufferedReader().use { it.readText() }
                system.loadRecipes(recipesJson)
            } catch (e: Exception) {
                android.util.Log.e("RecipeSystem", "Failed to load recipes", e)
                return false
            }
            if (system.recipeCount() == 0) {
                android.util.Log.e("RecipeSystem", "Recipe system is empty after initialization")
                return false
            }
            instance = system
            return true
        }
    }

    private val recipes = mutableListOf<HiddenRecipe>()
    private val discoveredIds = mutableSetOf<String>()

    fun loadRecipes(jsonString: String) {
        recipes.clear()
        val arr = JSONArray(jsonString)
        for (i in 0 until arr.length()) {
            try {
                val recipe = parseRecipe(arr.getJSONObject(i))
                if (recipe.ingredients.isNotEmpty()) {
                    recipes.add(recipe)
                }
            } catch (e: IllegalArgumentException) {
                android.util.Log.w("RecipeSystem", "Skipping recipe at index $i: ${e.message}")
            }
        }
        syncDiscoveredFlags()
    }

    fun matchRecipe(unitA: GameUnit, unitB: GameUnit): HiddenRecipe? {
        if (unitA === unitB) return null
        return recipes.firstOrNull { recipe ->
            recipe.ingredients.size == 2 && matchesIngredients(recipe, listOf(unitA, unitB))
        }
    }

    /** 3재료 레시피 매칭 — 필드에 있는 유닛 리스트에서 완성 가능한 레시피 검색 */
    fun matchRecipe3(units: List<GameUnit>): HiddenRecipe? {
        if (units.size < 3) return null
        return recipes.firstOrNull { recipe ->
            recipe.ingredients.size == 3 && matchesIngredients(recipe, units)
        }
    }

    private fun collectGridUnits(grid: Grid): MutableList<Pair<Int, GameUnit>> {
        val allUnits = mutableListOf<Pair<Int, GameUnit>>()
        for (i in 0 until Grid.TOTAL) {
            for (u in grid.getUnitsInSlot(i)) allUnits.add(i to u)
        }
        return allUnits
    }

    /** 필드의 모든 유닛에서 완성 가능한 레시피와 매칭 유닛 인덱스를 반환 */
    fun findMatchingRecipeOnGrid(grid: Grid, availableLuckyStones: Int = Int.MAX_VALUE): Pair<HiddenRecipe, List<Int>>? {
        val allUnits = collectGridUnits(grid)
        for (recipe in recipes) {
            if (availableLuckyStones < recipe.luckyStonesCost) continue
            val matched = findMatchingUnitsForRecipe(recipe, allUnits)
            if (matched != null) return recipe to matched
        }
        return null
    }

    /** 특정 레시피 ID로 필드에서 매칭하여 합성 시도 */
    fun findSpecificRecipeOnGrid(recipeId: String, grid: Grid, availableLuckyStones: Int = Int.MAX_VALUE): Pair<HiddenRecipe, List<Int>>? {
        val recipe = recipes.find { it.id == recipeId } ?: return null
        if (availableLuckyStones < recipe.luckyStonesCost) return null
        val matched = findMatchingUnitsForRecipe(recipe, collectGridUnits(grid)) ?: return null
        return recipe to matched
    }

    /** 특정 레시피에 매칭되는 유닛의 tileIndex 리스트 반환. 매칭 실패 시 null */
    private fun findMatchingUnitsForRecipe(
        recipe: HiddenRecipe,
        candidates: List<Pair<Int, GameUnit>>
    ): List<Int>? {
        val slots = recipe.ingredients
        val used = mutableSetOf<Int>()
        val matchedIndices = mutableListOf<Int>()

        fun tryMatchSlot(slotIdx: Int): Boolean {
            if (slotIdx >= slots.size) return true
            for ((idx, pair) in candidates.withIndex()) {
                if (idx in used) continue
                if (matchesSlot(slots[slotIdx], pair.second)) {
                    used.add(idx)
                    matchedIndices.add(pair.first) // tileIndex
                    if (tryMatchSlot(slotIdx + 1)) return true
                    matchedIndices.removeAt(matchedIndices.lastIndex)
                    used.remove(idx)
                }
            }
            return false
        }

        return if (tryMatchSlot(0)) matchedIndices else null
    }

    fun matchPartial(unitA: GameUnit, unitB: GameUnit): HiddenRecipe? {
        if (unitA === unitB) return null
        return recipes.firstOrNull { recipe ->
            recipe.ingredients.size == 3 && matchesPartialIngredients(recipe, listOf(unitA, unitB))
        }
    }

    fun resolveRecipe(recipe: HiddenRecipe, units: List<GameUnit>): UnitBlueprint? {
        if (!matchesIngredients(recipe, units)) return null
        return blueprintRegistry.findById(recipe.resultId)
    }

    fun completeRecipe(recipe: HiddenRecipe, units: List<GameUnit>): UnitBlueprint? {
        val result = resolveRecipe(recipe, units) ?: return null
        markDiscovered(recipe.id)
        return result
    }

    fun markDiscovered(recipeId: String) {
        discoveredIds.add(recipeId)
        recipes.find { it.id == recipeId }?.discovered = true
    }

    fun isDiscovered(recipeId: String): Boolean = recipeId in discoveredIds
    fun allRecipes(): List<HiddenRecipe> = recipes.toList()

    fun recipeCount(): Int = recipes.size
    fun setDiscoveredIds(ids: Set<String>) {
        discoveredIds.clear()
        discoveredIds.addAll(ids)
        syncDiscoveredFlags()
    }
    fun getDiscoveredIds(): Set<String> = discoveredIds.toSet()

    private fun syncDiscoveredFlags() {
        recipes.forEach { it.discovered = it.id in discoveredIds }
    }

    private fun matchesIngredients(recipe: HiddenRecipe, units: List<GameUnit>): Boolean {
        if (recipe.ingredients.isEmpty()) return false
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
            discovered = false,
            luckyStonesCost = obj.optInt("luckyStonesCost", 1),
        )
    }
}
