package com.example.jaygame.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

class GameRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jaygame_save", Context.MODE_PRIVATE)

    private val _gameData = MutableStateFlow(StaminaManager.refreshStamina(load()))
    val gameData: StateFlow<GameData> = _gameData

    fun refresh() {
        _gameData.value = StaminaManager.refreshStamina(load())
    }

    fun save(data: GameData) {
        val json = serialize(data)
        prefs.edit().putString("save_data", json).apply()
        _gameData.value = data
    }

    fun saveDiscoveredRecipes(ids: Set<String>) {
        val json = JSONArray(ids.toList()).toString()
        prefs.edit().putString("discovered_recipes", json).apply()
    }

    fun loadDiscoveredRecipes(): Set<String> {
        return try {
            val json = prefs.getString("discovered_recipes", "[]") ?: "[]"
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (e: Exception) {
            android.util.Log.w("GameRepository", "Failed to load discovered recipes", e)
            emptySet()
        }
    }

    private fun load(): GameData {
        val jsonStr = prefs.getString("save_data", null) ?: return GameData()
        return try {
            deserialize(jsonStr)
        } catch (e: Exception) {
            GameData()
        }
    }

    companion object {
        /**
         * FNV-1a 32-bit hash, matching the C++ SaveSystem checksum.
         */
        fun fnv1aHash(data: String): Long {
            var hash = 0x811c9dc5L
            for (b in data.toByteArray(Charsets.UTF_8)) {
                hash = hash xor (b.toLong() and 0xFF)
                hash = (hash * 0x01000193L) and 0xFFFFFFFFL
            }
            return hash
        }

        fun serialize(data: GameData): String {
            val root = JSONObject()

            root.put("gold", data.gold)
            root.put("diamonds", data.diamonds)
            root.put("trophies", data.trophies)
            root.put("playerLevel", data.playerLevel)
            root.put("totalXP", data.totalXP)

            // units map — key=blueprintId, value={owned, cards, level}
            val unitsObj = JSONObject()
            for ((blueprintId, u) in data.units) {
                val obj = JSONObject()
                obj.put("owned", if (u.owned) 1 else 0)
                obj.put("cards", u.cards)
                obj.put("level", u.level)
                unitsObj.put(blueprintId, obj)
            }
            root.put("units", unitsObj)

            // stats object
            val stats = JSONObject()
            stats.put("totalWins", data.totalWins)
            stats.put("totalLosses", data.totalLosses)
            stats.put("totalKills", data.totalKills)
            stats.put("totalMerges", data.totalMerges)
            stats.put("totalGoldEarned", data.totalGoldEarned)
            stats.put("highestWave", data.highestWave)
            stats.put("maxUnitLevel", data.maxUnitLevel)
            stats.put("wonWithoutDamage", if (data.wonWithoutDamage) 1 else 0)
            stats.put("wonWithSingleType", if (data.wonWithSingleType) 1 else 0)
            root.put("stats", stats)

            // settings — booleans as 0/1 ints
            val settings = JSONObject()
            settings.put("soundEnabled", if (data.soundEnabled) 1 else 0)
            settings.put("musicEnabled", if (data.musicEnabled) 1 else 0)
            settings.put("hapticEnabled", if (data.hapticEnabled) 1 else 0)
            settings.put("defaultBattleSpeed", data.defaultBattleSpeed.toDouble())
            settings.put("showDamageNumbers", if (data.showDamageNumbers) 1 else 0)
            settings.put("healthBarMode", data.healthBarMode)
            root.put("settings", settings)

            // dailyLogin
            val dailyLogin = JSONObject()
            dailyLogin.put("lastLoginDate", data.lastLoginDate)
            dailyLogin.put("loginStreak", data.loginStreak)
            dailyLogin.put("lastClaimedDay", data.lastClaimedDay)
            root.put("dailyLogin", dailyLogin)

            // seasonPass
            val seasonPass = JSONObject()
            seasonPass.put("seasonXP", data.seasonXP)
            seasonPass.put("seasonClaimedTier", data.seasonClaimedTier)
            seasonPass.put("seasonMonth", data.seasonMonth)
            root.put("seasonPass", seasonPass)

            // stamina
            val staminaObj = JSONObject()
            staminaObj.put("stamina", data.stamina)
            staminaObj.put("maxStamina", data.maxStamina)
            staminaObj.put("lastStaminaRegenTime", data.lastStaminaRegenTime)
            root.put("staminaData", staminaObj)

            // stage
            val stageObj = JSONObject()
            stageObj.put("currentStageId", data.currentStageId)
            val unlockedArr = JSONArray()
            for (s in data.unlockedStages) unlockedArr.put(s)
            stageObj.put("unlockedStages", unlockedArr)
            val bestArr = JSONArray()
            for (b in data.stageBestWaves) bestArr.put(b)
            stageObj.put("stageBestWaves", bestArr)
            root.put("stageData", stageObj)

            // difficulty
            root.put("difficulty", data.difficulty)

            // gas
            root.put("gas", data.gas)

            // familyUpgrades
            val familyObj = JSONObject()
            for ((key, value) in data.familyUpgrades) {
                familyObj.put(key, value)
            }
            root.put("familyUpgrades", familyObj)

            // freePull
            root.put("lastFreePullTime", data.lastFreePullTime)

            // claimedAchievements
            val claimedArr = JSONArray()
            for (id in data.claimedAchievements) claimedArr.put(id)
            root.put("claimedAchievements", claimedArr)

            root.put("saveVersion", 3)

            // relics
            val relicsArr = JSONArray()
            for (r in data.relics) {
                val rObj = JSONObject()
                rObj.put("relicId", r.relicId)
                rObj.put("grade", r.grade)
                rObj.put("level", r.level)
                rObj.put("owned", if (r.owned) 1 else 0)
                relicsArr.put(rObj)
            }
            root.put("relics", relicsArr)

            val eqArr = JSONArray()
            for (id in data.equippedRelics) eqArr.put(id)
            root.put("equippedRelics", eqArr)

            // pets
            val petsArr = JSONArray()
            for (p in data.pets) {
                val pObj = JSONObject()
                pObj.put("petId", p.petId)
                pObj.put("owned", if (p.owned) 1 else 0)
                pObj.put("cards", p.cards)
                pObj.put("level", p.level)
                petsArr.put(pObj)
            }
            root.put("pets", petsArr)

            val eqPetsArr = JSONArray()
            for (id in data.equippedPets) eqPetsArr.put(id)
            root.put("equippedPets", eqPetsArr)

            root.put("petPullPity", data.petPullPity)

            root.put("unitPullPity", data.unitPullPity)

            // dungeons
            val dungeonClearsObj = JSONObject()
            for ((dungeonId, bestWave) in data.dungeonClears) {
                dungeonClearsObj.put(dungeonId.toString(), bestWave)
            }
            root.put("dungeonClears", dungeonClearsObj)
            root.put("dungeonDailyCount", data.dungeonDailyCount)
            root.put("lastDungeonResetDate", data.lastDungeonResetDate)

            // profile
            root.put("selectedProfileId", data.selectedProfileId)
            val unlockedProfilesArr = JSONArray()
            for (id in data.unlockedProfiles) unlockedProfilesArr.put(id)
            root.put("unlockedProfiles", unlockedProfilesArr)

            // offline reward
            root.put("lastOnlineTime", data.lastOnlineTime)

            // tutorial
            root.put("tutorialCompleted", data.tutorialCompleted)

            // 행운석
            root.put("luckyStones", data.luckyStones)


            // Compute checksum on the JSON without checksum field
            val payload = root.toString()
            val checksum = fnv1aHash(payload)
            root.put("checksum", checksum)

            return root.toString()
        }

        fun deserialize(jsonStr: String): GameData {
            val root = JSONObject(jsonStr)

            // Verify checksum if present (warning only — JSONObject.toString()
            // does not guarantee key ordering across Android versions, so a
            // mismatch is expected after OS upgrades / library changes).
            if (root.has("checksum")) {
                val savedChecksum = root.getLong("checksum")
                val copy = JSONObject(jsonStr)
                copy.remove("checksum")
                val computed = fnv1aHash(copy.toString())
                if (computed != savedChecksum) {
                    android.util.Log.w(
                        "GameRepository",
                        "Save-data checksum mismatch (expected=$savedChecksum, computed=$computed). " +
                                "Proceeding with loaded data."
                    )
                }
            }

            val gold = root.optInt("gold", 500)
            val diamonds = root.optInt("diamonds", 0)
            val trophies = root.optInt("trophies", 0)
            val playerLevel = root.optInt("playerLevel", 1)
            val totalXP = root.optInt("totalXP", 0)

            // units — v3+ stores as JSON object keyed by blueprintId; v1/v2 stores as JSON array
            val saveVersion = root.optInt("saveVersion", 1)
            val units: Map<String, UnitProgress> = if (saveVersion >= 3) {
                // New format: {"blueprintId": {owned, cards, level}, ...}
                val unitsObj = root.optJSONObject("units")
                if (unitsObj != null) {
                    val map = mutableMapOf<String, UnitProgress>()
                    val keys = unitsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val obj = unitsObj.getJSONObject(key)
                        map[key] = UnitProgress(
                            owned = obj.optInt("owned", 0) != 0,
                            cards = obj.optInt("cards", 0),
                            level = obj.optInt("level", 1),
                        )
                    }
                    map
                } else emptyMap()
            } else {
                // Legacy format: [{owned, cards, level}, ...] indexed by Int
                val legacyUnits = mutableListOf<UnitProgress>()
                val unitsArr = root.optJSONArray("units")
                if (unitsArr != null) {
                    for (i in 0 until unitsArr.length()) {
                        val obj = unitsArr.getJSONObject(i)
                        legacyUnits.add(
                            UnitProgress(
                                owned = obj.optInt("owned", 0) != 0,
                                cards = obj.optInt("cards", 0),
                                level = obj.optInt("level", 1),
                            )
                        )
                    }
                }
                while (legacyUnits.size < 42) {
                    legacyUnits.add(UnitProgress(owned = legacyUnits.size == 35, cards = 0, level = 1))
                }
                LegacyMigration.migrateUnits(legacyUnits)
            }
            // deck removed in v3 — skip parsing

            // stats
            val stats = root.optJSONObject("stats")
            val totalWins = stats?.optInt("totalWins", 0) ?: 0
            val totalLosses = stats?.optInt("totalLosses", 0) ?: 0
            val totalKills = stats?.optInt("totalKills", 0) ?: 0
            val totalMerges = stats?.optInt("totalMerges", 0) ?: 0
            val totalGoldEarned = stats?.optInt("totalGoldEarned", 0) ?: 0
            val highestWave = stats?.optInt("highestWave", 0) ?: 0
            val maxUnitLevel = stats?.optInt("maxUnitLevel", 1) ?: 1
            val wonWithoutDamage = (stats?.optInt("wonWithoutDamage", 0) ?: 0) != 0
            val wonWithSingleType = (stats?.optInt("wonWithSingleType", 0) ?: 0) != 0

            // settings
            val settings = root.optJSONObject("settings")
            val soundEnabled = (settings?.optInt("soundEnabled", 1) ?: 1) != 0
            val musicEnabled = (settings?.optInt("musicEnabled", 1) ?: 1) != 0
            val hapticEnabled = (settings?.optInt("hapticEnabled", 1) ?: 1) != 0
            val defaultBattleSpeed = (settings?.optDouble("defaultBattleSpeed", 2.0)?.toFloat() ?: 2f).let {
                // 기존 1f 저장값 마이그레이션 → 새 기본 2f
                if (it < 2f) it * 2f else it
            }
            val showDamageNumbers = (settings?.optInt("showDamageNumbers", 1) ?: 1) != 0
            val healthBarMode = settings?.optInt("healthBarMode", 0) ?: 0
            // dailyLogin
            val dailyLogin = root.optJSONObject("dailyLogin")
            val lastLoginDate = dailyLogin?.optString("lastLoginDate", "") ?: ""
            val loginStreak = dailyLogin?.optInt("loginStreak", 0) ?: 0
            val lastClaimedDay = dailyLogin?.optInt("lastClaimedDay", 0) ?: 0

            // seasonPass
            val seasonPass = root.optJSONObject("seasonPass")
            val seasonXP = seasonPass?.optInt("seasonXP", 0) ?: 0
            val seasonClaimedTier = seasonPass?.optInt("seasonClaimedTier", 0) ?: 0
            val seasonMonth = seasonPass?.optString("seasonMonth", "") ?: ""

            // stamina
            val staminaData = root.optJSONObject("staminaData")
            val stamina = staminaData?.optInt("stamina", 30) ?: 30
            val maxStamina = staminaData?.optInt("maxStamina", 30) ?: 30
            val lastStaminaRegenTime = staminaData?.optLong("lastStaminaRegenTime", System.currentTimeMillis())
                ?: System.currentTimeMillis()

            // stage
            val stageDataObj = root.optJSONObject("stageData")
            val currentStageId = stageDataObj?.optInt("currentStageId", 0) ?: 0
            val unlockedStages = mutableListOf<Int>()
            val unlockedArr = stageDataObj?.optJSONArray("unlockedStages")
            if (unlockedArr != null) {
                for (i in 0 until unlockedArr.length()) unlockedStages.add(unlockedArr.getInt(i))
            }
            if (unlockedStages.isEmpty()) unlockedStages.add(0)
            val stageBestWaves = mutableListOf<Int>()
            val bestArr = stageDataObj?.optJSONArray("stageBestWaves")
            if (bestArr != null) {
                for (i in 0 until bestArr.length()) stageBestWaves.add(bestArr.getInt(i))
            }
            while (stageBestWaves.size < 6) stageBestWaves.add(0)

            // difficulty
            val difficulty = root.optInt("difficulty", 0).coerceIn(0, 2)

            // gas
            val gas = root.optInt("gas", 0)

            // familyUpgrades
            val familyUpgrades = mutableMapOf<String, Int>()
            val familyObj = root.optJSONObject("familyUpgrades")
            if (familyObj != null) {
                val keys = familyObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    familyUpgrades[key] = familyObj.optInt(key, 0)
                }
            }

            // freePull
            val lastFreePullTime = root.optLong("lastFreePullTime", 0L)

            // claimedAchievements
            val claimedAchievements = mutableSetOf<Int>()
            val claimedArr = root.optJSONArray("claimedAchievements")
            if (claimedArr != null) {
                for (i in 0 until claimedArr.length()) claimedAchievements.add(claimedArr.getInt(i))
            }

            val relics = if (root.has("relics")) {
                val arr = root.getJSONArray("relics")
                List(arr.length()) { i ->
                    val obj = arr.getJSONObject(i)
                    RelicProgress(
                        relicId = obj.getInt("relicId"),
                        grade = obj.getInt("grade"),
                        level = obj.getInt("level"),
                        owned = obj.optInt("owned", 0) == 1,
                    )
                }
            } else List(12) { RelicProgress(relicId = it) }

            val equippedRelics = if (root.has("equippedRelics")) {
                val arr = root.getJSONArray("equippedRelics")
                List(arr.length()) { arr.getInt(it) }
            } else emptyList()

            val pets = if (root.has("pets")) {
                val arr = root.getJSONArray("pets")
                List(arr.length()) { i ->
                    val obj = arr.getJSONObject(i)
                    PetProgress(
                        petId = obj.getInt("petId"),
                        owned = obj.optInt("owned", 0) == 1,
                        cards = obj.optInt("cards", 0),
                        level = obj.optInt("level", 1),
                    )
                }
            } else List(9) { PetProgress(petId = it) }

            val equippedPets = if (root.has("equippedPets")) {
                val arr = root.getJSONArray("equippedPets")
                List(arr.length()) { arr.getInt(it) }
            } else emptyList()

            val petPullPity = root.optInt("petPullPity", 0)

            val unitPullPity = root.optInt("unitPullPity", 0)

            // dungeons
            val dungeonClears = mutableMapOf<Int, Int>()
            val dungeonClearsObj = root.optJSONObject("dungeonClears")
            if (dungeonClearsObj != null) {
                val keys = dungeonClearsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    dungeonClears[key.toIntOrNull() ?: continue] = dungeonClearsObj.optInt(key, 0)
                }
            }
            val dungeonDailyCount = root.optInt("dungeonDailyCount", 0)
            val lastDungeonResetDate = root.optString("lastDungeonResetDate", "")

            // profile
            val selectedProfileId = root.optInt("selectedProfileId", 0)
            val unlockedProfiles = mutableSetOf<Int>()
            val unlockedProfilesArr = root.optJSONArray("unlockedProfiles")
            if (unlockedProfilesArr != null) {
                for (i in 0 until unlockedProfilesArr.length()) unlockedProfiles.add(unlockedProfilesArr.getInt(i))
            }
            if (unlockedProfiles.isEmpty()) unlockedProfiles.add(0)

            // offline reward
            val lastOnlineTime = root.optLong("lastOnlineTime", System.currentTimeMillis())

            // tutorial
            val tutorialCompleted = root.optBoolean("tutorialCompleted", false)

            // 행운석
            val luckyStones = root.optInt("luckyStones", 0)

            return GameData(
                gold = gold,
                diamonds = diamonds,
                trophies = trophies,
                playerLevel = playerLevel,
                totalXP = totalXP,
                units = units,
                totalWins = totalWins,
                totalLosses = totalLosses,
                totalKills = totalKills,
                totalMerges = totalMerges,
                totalGoldEarned = totalGoldEarned,
                highestWave = highestWave,
                maxUnitLevel = maxUnitLevel,
                wonWithoutDamage = wonWithoutDamage,
                wonWithSingleType = wonWithSingleType,
                soundEnabled = soundEnabled,
                musicEnabled = musicEnabled,
                hapticEnabled = hapticEnabled,
                defaultBattleSpeed = defaultBattleSpeed,
                showDamageNumbers = showDamageNumbers,
                healthBarMode = healthBarMode,
                lastLoginDate = lastLoginDate,
                loginStreak = loginStreak,
                lastClaimedDay = lastClaimedDay,
                seasonXP = seasonXP,
                seasonClaimedTier = seasonClaimedTier,
                seasonMonth = seasonMonth,
                stamina = stamina,
                maxStamina = maxStamina,
                lastStaminaRegenTime = lastStaminaRegenTime,
                currentStageId = currentStageId,
                unlockedStages = unlockedStages,
                stageBestWaves = stageBestWaves,
                difficulty = difficulty,
                gas = gas,
                familyUpgrades = familyUpgrades,
                lastFreePullTime = lastFreePullTime,
                claimedAchievements = claimedAchievements,
                saveVersion = saveVersion,
                relics = relics,
                equippedRelics = equippedRelics,
                pets = pets,
                equippedPets = equippedPets,
                petPullPity = petPullPity,
                unitPullPity = unitPullPity,
                dungeonClears = dungeonClears,
                dungeonDailyCount = dungeonDailyCount,
                lastDungeonResetDate = lastDungeonResetDate,
                selectedProfileId = selectedProfileId,
                unlockedProfiles = unlockedProfiles,
                lastOnlineTime = lastOnlineTime,
                tutorialCompleted = tutorialCompleted,
                luckyStones = luckyStones,
            )
        }
    }
}
