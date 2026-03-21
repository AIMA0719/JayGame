package com.example.jaygame.engine

import com.example.jaygame.data.UnitFamily
import org.json.JSONArray
import org.json.JSONObject

class BlueprintRegistry {
    companion object {
        @Volatile
        lateinit var instance: BlueprintRegistry
            private set

        val isReady: Boolean get() = ::instance.isInitialized

        @Synchronized
        fun initialize(context: android.content.Context) {
            if (::instance.isInitialized) return
            val registry = BlueprintRegistry()
            // Load normal + hidden units from blueprints.json
            val blueprintsJson = context.assets.open("units/blueprints.json")
                .bufferedReader().use { it.readText() }
            registry.loadFromJson(blueprintsJson)
            // Load special units from special_units.json
            val specialJson = context.assets.open("units/special_units.json")
                .bufferedReader().use { it.readText() }
            registry.loadFromJson(specialJson)
            instance = registry
        }
    }

    private val blueprints = mutableMapOf<String, UnitBlueprint>()

    // Pre-computed caches — rebuilt after loadFromJson
    private var summonableCache: List<UnitBlueprint> = emptyList()
    private var summonableByGradeCache: Map<UnitGrade, List<UnitBlueprint>> = emptyMap()
    private var mergeableByGradeCache: Map<UnitGrade, List<UnitBlueprint>> = emptyMap()

    fun loadFromJson(jsonString: String) {
        val arr = JSONArray(jsonString)
        for (i in 0 until arr.length()) {
            val bp = parseBlueprint(arr.getJSONObject(i))
            blueprints[bp.id] = bp
        }
        // Build caches
        summonableCache = blueprints.values.filter { it.isSummonable }
        summonableByGradeCache = summonableCache.groupBy { it.grade }
        mergeableByGradeCache = blueprints.values
            .filter { it.unitCategory == UnitCategory.NORMAL || it.unitCategory == UnitCategory.HIDDEN }
            .groupBy { it.grade }
    }

    fun findById(id: String): UnitBlueprint? = blueprints[id]

    fun findByFamilyAndRole(family: UnitFamily, role: UnitRole): List<UnitBlueprint> =
        blueprints.values.filter { family in it.families && it.role == role }

    fun findSummonable(): List<UnitBlueprint> = summonableCache

    fun findByCategory(category: UnitCategory): List<UnitBlueprint> =
        blueprints.values.filter { it.unitCategory == category }

    fun findByGradeAndSummonable(grade: UnitGrade): List<UnitBlueprint> =
        summonableByGradeCache[grade] ?: emptyList()

    /** NORMAL + HIDDEN 유닛을 등급별로 반환 (합성 결과 풀) */
    fun findMergeableByGrade(grade: UnitGrade): List<UnitBlueprint> =
        mergeableByGradeCache[grade] ?: emptyList()

    fun all(): List<UnitBlueprint> = blueprints.values.toList()

    fun count(): Int = blueprints.size

    private fun parseBlueprint(obj: JSONObject): UnitBlueprint {
        val familiesArr = obj.getJSONArray("families")
        val families = (0 until familiesArr.length()).map { UnitFamily.valueOf(familiesArr.getString(it)) }

        val statsObj = obj.getJSONObject("stats")
        val stats = UnitStats(
            hp = statsObj.getDouble("hp").toFloat(),
            baseATK = statsObj.getDouble("baseATK").toFloat(),
            baseSpeed = statsObj.getDouble("baseSpeed").toFloat(),
            range = statsObj.getDouble("range").toFloat(),
            defense = statsObj.getDouble("defense").toFloat(),
            magicResist = statsObj.getDouble("magicResist").toFloat(),
            moveSpeed = statsObj.getDouble("moveSpeed").toFloat(),
            blockCount = statsObj.getInt("blockCount")
        )

        val ability = if (obj.isNull("ability")) null else parseAbilityDef(obj.getJSONObject("ability"))
        val uniqueAbility = if (obj.isNull("uniqueAbility")) null else parseUniqueAbilityDef(obj.getJSONObject("uniqueAbility"))

        return UnitBlueprint(
            id = obj.getString("id"),
            name = obj.getString("name"),
            families = families,
            grade = UnitGrade.valueOf(obj.getString("grade")),
            role = UnitRole.valueOf(obj.getString("role")),
            attackRange = AttackRange.valueOf(obj.getString("attackRange")),
            damageType = DamageType.valueOf(obj.getString("damageType")),
            stats = stats,
            behaviorId = obj.getString("behaviorId"),
            ability = ability,
            uniqueAbility = uniqueAbility,
            mergeResultId = if (obj.isNull("mergeResultId")) null else obj.getString("mergeResultId"),
            isSummonable = obj.getBoolean("isSummonable"),
            summonWeight = obj.getInt("summonWeight"),
            unitCategory = UnitCategory.valueOf(obj.getString("unitCategory")),
            iconRes = obj.optInt("iconRes", 0),
            description = obj.getString("description")
        )
    }

    private fun parseAbilityDef(obj: JSONObject): AbilityDef = AbilityDef(
        id = obj.getString("id"),
        name = obj.getString("name"),
        type = AbilityTrigger.valueOf(obj.getString("type")),
        damageType = DamageType.valueOf(obj.getString("damageType")),
        value = obj.getDouble("value").toFloat(),
        cooldown = obj.getDouble("cooldown").toFloat(),
        range = obj.getDouble("range").toFloat(),
        description = obj.getString("description")
    )

    private fun parseUniqueAbilityDef(obj: JSONObject): UniqueAbilityDef = UniqueAbilityDef(
        id = obj.getString("id"),
        name = obj.getString("name"),
        passive = if (obj.isNull("passive")) null else parseAbilityDef(obj.getJSONObject("passive")),
        active = if (obj.isNull("active")) null else parseAbilityDef(obj.getJSONObject("active")),
        requiredGrade = UnitGrade.valueOf(obj.getString("requiredGrade"))
    )
}
