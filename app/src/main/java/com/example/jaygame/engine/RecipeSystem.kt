package com.example.jaygame.engine

import com.example.jaygame.data.UnitFamily
import org.json.JSONArray
import org.json.JSONObject

data class HiddenRecipe(
    val id: String,
    val ingredients: List<RecipeSlot>,
    val resultId: String,
    var discovered: Boolean = false,
    val luckyStonesCost: Int = 1,  // 행운석 소모량 (신화 레시피 합성 시 필요)
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
        fun initialize(context: android.content.Context) {
            if (::instance.isInitialized) return
            val recipesJson = context.assets.open("units/hidden_recipes.json")
                .bufferedReader().use { it.readText() }
            val system = RecipeSystem(BlueprintRegistry.instance)
            system.loadRecipes(recipesJson)
            instance = system
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
            } catch (_: IllegalArgumentException) {
                // Skip recipes with invalid enum values in JSON
            }
        }
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

    /** 필드의 모든 유닛에서 완성 가능한 레시피와 매칭 유닛 인덱스를 반환 */
    fun findMatchingRecipeOnGrid(grid: Grid, availableLuckyStones: Int = Int.MAX_VALUE): Pair<HiddenRecipe, List<Int>>? {
        val allUnits = mutableListOf<Pair<Int, GameUnit>>() // (tileIndex, unit)
        for (i in 0 until Grid.TOTAL) {
            val u = grid.getUnit(i) ?: continue
            allUnits.add(i to u)
        }
        for (recipe in recipes) {
            // 행운석 부족 시 해당 레시피 스킵
            if (availableLuckyStones < recipe.luckyStonesCost) continue
            val matched = findMatchingUnitsForRecipe(recipe, allUnits)
            if (matched != null) return recipe to matched
        }
        return null
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
