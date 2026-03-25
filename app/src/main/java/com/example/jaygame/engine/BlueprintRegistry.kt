package com.example.jaygame.engine

import com.example.jaygame.data.UnitFamily
import com.example.jaygame.data.UnitRace
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
            try {
                val blueprintsJson = context.assets.open("units/blueprints.json")
                    .bufferedReader().use { it.readText() }
                registry.loadFromJson(blueprintsJson)
                val specialJson = context.assets.open("units/special_units.json")
                    .bufferedReader().use { it.readText() }
                registry.loadFromJson(specialJson)
            } catch (e: Exception) {
                android.util.Log.e("BlueprintRegistry", "Failed to load blueprints", e)
            }
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

    /** 선택된 종족들의 소환 가능 유닛을 등급별로 반환 */
    fun findByRacesAndGradeAndSummonable(races: Set<UnitRace>, grade: UnitGrade): List<UnitBlueprint> {
        if (races.isEmpty()) return findByGradeAndSummonable(grade)
        return (summonableByGradeCache[grade] ?: emptyList()).filter { it.race in races }
    }

    /** 선택된 종족들의 합성 가능 유닛을 등급별로 반환 */
    fun findMergeableByRacesAndGrade(races: Set<UnitRace>, grade: UnitGrade): List<UnitBlueprint> {
        if (races.isEmpty()) return findMergeableByGrade(grade)
        return (mergeableByGradeCache[grade] ?: emptyList()).filter { it.race in races }
    }

    fun all(): List<UnitBlueprint> = blueprints.values.toList()

    fun count(): Int = blueprints.size

    private fun parseBlueprint(obj: JSONObject): UnitBlueprint {
        // 신규 포맷: race 필드 (UnitRace)
        val race = if (obj.has("race")) {
            UnitRace.valueOf(obj.getString("race"))
        } else {
            UnitRace.HUMAN // fallback
        }

        // 하위 호환: families 필드 (deprecated)
        val families = if (obj.has("families")) {
            val familiesArr = obj.getJSONArray("families")
            (0 until familiesArr.length()).mapNotNull {
                try { UnitFamily.valueOf(familiesArr.getString(it)) } catch (_: Exception) { null }
            }
        } else emptyList()

        val statsObj = obj.getJSONObject("stats")
        val stats = UnitStats(
            baseATK = statsObj.getDouble("baseATK").toFloat(),
            baseSpeed = statsObj.getDouble("baseSpeed").toFloat(),
            range = statsObj.getDouble("range").toFloat(),
            // 하위 호환 (deprecated fields)
            hp = statsObj.optDouble("hp", 0.0).toFloat(),
            defense = statsObj.optDouble("defense", 0.0).toFloat(),
            magicResist = statsObj.optDouble("magicResist", 0.0).toFloat(),
            moveSpeed = statsObj.optDouble("moveSpeed", 75.0).toFloat(),
            blockCount = statsObj.optInt("blockCount", 0)
        )

        val ability = if (obj.isNull("ability")) null else parseAbilityDef(obj.getJSONObject("ability"))
        val uniqueAbility = if (obj.isNull("uniqueAbility")) null else parseUniqueAbilityDef(obj.getJSONObject("uniqueAbility"))

        return UnitBlueprint(
            id = obj.getString("id"),
            name = obj.getString("name"),
            race = race,
            grade = UnitGrade.valueOf(obj.getString("grade")),
            attackRange = AttackRange.valueOf(obj.getString("attackRange")),
            damageType = DamageType.valueOf(obj.getString("damageType")),
            stats = stats,
            ability = ability,
            uniqueAbility = uniqueAbility,
            mergeResultId = if (obj.isNull("mergeResultId")) null else obj.getString("mergeResultId"),
            isSummonable = obj.getBoolean("isSummonable"),
            summonWeight = obj.getInt("summonWeight"),
            unitCategory = UnitCategory.valueOf(obj.getString("unitCategory")),
            iconRes = obj.optInt("iconRes", 0),
            description = obj.getString("description"),
            // deprecated fields for backward compat
            families = families,
            role = if (obj.has("role")) UnitRole.valueOf(obj.getString("role")) else UnitRole.MELEE_DPS,
            behaviorId = obj.optString("behaviorId", ""),
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
