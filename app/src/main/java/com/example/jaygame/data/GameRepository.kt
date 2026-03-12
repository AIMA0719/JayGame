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
        prefs.edit().putString("save_data", json).commit()
        _gameData.value = data
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

            // units array — matches C++ format: [{owned:0/1, cards:N, level:N}, ...]
            val unitsArr = JSONArray()
            for (u in data.units) {
                val obj = JSONObject()
                obj.put("owned", if (u.owned) 1 else 0)
                obj.put("cards", u.cards)
                obj.put("level", u.level)
                unitsArr.put(obj)
            }
            root.put("units", unitsArr)

            // deck array
            val deckArr = JSONArray()
            for (d in data.deck) deckArr.put(d)
            root.put("deck", deckArr)

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

            root.put("saveVersion", data.saveVersion)

            // Compute checksum on the JSON without checksum field
            val payload = root.toString()
            val checksum = fnv1aHash(payload)
            root.put("checksum", checksum)

            return root.toString()
        }

        fun deserialize(jsonStr: String): GameData {
            val root = JSONObject(jsonStr)

            // Verify checksum if present
            if (root.has("checksum")) {
                val savedChecksum = root.getLong("checksum")
                val copy = JSONObject(jsonStr)
                copy.remove("checksum")
                val computed = fnv1aHash(copy.toString())
                if (computed != savedChecksum) {
                    // Checksum mismatch — return default data
                    return GameData()
                }
            }

            val gold = root.optInt("gold", 500)
            val diamonds = root.optInt("diamonds", 0)
            val trophies = root.optInt("trophies", 0)
            val playerLevel = root.optInt("playerLevel", 1)
            val totalXP = root.optInt("totalXP", 0)

            // units
            val units = mutableListOf<UnitProgress>()
            val unitsArr = root.optJSONArray("units")
            if (unitsArr != null) {
                for (i in 0 until unitsArr.length()) {
                    val obj = unitsArr.getJSONObject(i)
                    units.add(
                        UnitProgress(
                            owned = obj.optInt("owned", 0) != 0,
                            cards = obj.optInt("cards", 0),
                            level = obj.optInt("level", 1),
                        )
                    )
                }
            }
            // Pad to 42 if fewer entries (COMMON grade units 0-4 + 35 owned by default)
            while (units.size < 42) {
                units.add(UnitProgress(owned = units.size == 35, cards = 0, level = 1))
            }

            // deck (stores family ordinals 0-5)
            val deck = mutableListOf<Int>()
            val deckArr = root.optJSONArray("deck")
            if (deckArr != null) {
                for (i in 0 until deckArr.length()) {
                    val v = deckArr.getInt(i)
                    // Migration: old saves stored unit IDs, convert to family ordinal
                    val familyOrdinal = if (v in 0 until NUM_FAMILIES) v else unitFamilyOf(v)
                    deck.add(familyOrdinal)
                }
            }
            if (deck.isEmpty()) deck.addAll(listOf(0, 1, 2, 3, 4))

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

            // dailyLogin
            val dailyLogin = root.optJSONObject("dailyLogin")
            val lastLoginDate = dailyLogin?.optString("lastLoginDate", "") ?: ""
            val loginStreak = dailyLogin?.optInt("loginStreak", 0) ?: 0
            val lastClaimedDay = dailyLogin?.optInt("lastClaimedDay", 0) ?: 0

            // seasonPass
            val seasonPass = root.optJSONObject("seasonPass")
            val seasonXP = seasonPass?.optInt("seasonXP", 0) ?: 0
            val seasonClaimedTier = seasonPass?.optInt("seasonClaimedTier", 0) ?: 0

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
            val difficulty = root.optInt("difficulty", 0)

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

            val saveVersion = root.optInt("saveVersion", 1)

            return GameData(
                gold = gold,
                diamonds = diamonds,
                trophies = trophies,
                playerLevel = playerLevel,
                totalXP = totalXP,
                units = units,
                deck = deck,
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
                lastLoginDate = lastLoginDate,
                loginStreak = loginStreak,
                lastClaimedDay = lastClaimedDay,
                seasonXP = seasonXP,
                seasonClaimedTier = seasonClaimedTier,
                stamina = stamina,
                maxStamina = maxStamina,
                lastStaminaRegenTime = lastStaminaRegenTime,
                currentStageId = currentStageId,
                unlockedStages = unlockedStages,
                stageBestWaves = stageBestWaves,
                difficulty = difficulty,
                gas = gas,
                familyUpgrades = familyUpgrades,
                saveVersion = saveVersion,
            )
        }
    }
}
