package com.example.jaygame

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.jaygame.audio.BgmManager
import com.example.jaygame.audio.SfxManager
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.RelicGrade
import com.example.jaygame.data.STAGES
import com.example.jaygame.data.ALL_DUNGEONS
import com.example.jaygame.data.addRandomCardsToUnits
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.BattleEngine
import com.example.jaygame.engine.DungeonManager
import com.example.jaygame.engine.RelicManager
import com.example.jaygame.ui.battle.BattleScreen
import com.example.jaygame.ui.theme.JayGameTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// A4: Smooth fade transition when returning to home

class MainActivity : ComponentActivity() {
    private lateinit var repository: GameRepository
    private var engine: BattleEngine? = null
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        BlueprintRegistry.initialize(applicationContext)
        repository = GameRepository(this)

        // Preserve stageId/difficulty/speed set by ComposeActivity, then reset battle state
        val stageId = BattleBridge.stageId.value
        val difficulty = BattleBridge.difficulty.value
        BattleBridge.reset()
        BattleBridge.setStageId(stageId)
        BattleBridge.setDifficulty(difficulty)
        SfxManager.init(this)

        // Create and start Kotlin battle engine
        val stage = STAGES.getOrNull(stageId) ?: STAGES[0]
        val data = repository.gameData.value
        val dungeonId = BattleBridge.dungeonId.value
        val dungeonDef = if (dungeonId >= 0) ALL_DUNGEONS.getOrNull(dungeonId) else null

        // Apply gameplay settings
        BattleBridge.setBattleSpeed(data.defaultBattleSpeed)
        BattleBridge.applyGameplaySettings(
            showDamage = data.showDamageNumbers,
            hpBarMode = data.healthBarMode,
            autoSummonOn = data.autoSummon,
        )
        BattleBridge.updateUnitPullPity(data.unitPullPity)
        BattleBridge.setTutorialMode(!data.tutorialCompleted)

        val maxWaves = dungeonDef?.waveCount ?: stage.maxWaves
        val effectiveDifficulty = if (dungeonDef != null) {
            // Scale difficulty by dungeon multiplier
            (difficulty * dungeonDef.difficultyMultiplier).toInt().coerceAtLeast(difficulty)
        } else difficulty

        engine = BattleEngine(
            stageId = stageId,
            difficulty = effectiveDifficulty,
            maxWaves = maxWaves,
            deck = data.deck.toIntArray(),
            gameData = data,
            initialPity = data.unitPullPity,
        ).also {
            if (dungeonDef != null) {
                it.isDungeonMode = true
                it.dungeonDef = dungeonDef
            }
            BattleBridge.engine = it
            it.start(engineScope)
        }

        // Play battle BGM
        if (data.musicEnabled) {
            BgmManager.play(this, "audio/battle_bgm.mp3")
        }

        setContent {
            JayGameTheme {
                val result by BattleBridge.result.collectAsState()
                BattleScreen(
                    result = result,
                    onGoHome = {
                        // Apply battle rewards to persistent data
                        val battleResult = BattleBridge.result.value
                        if (battleResult != null) {
                            val current = repository.gameData.value
                            val isDungeon = BattleBridge.isDungeonMode
                            val dId = BattleBridge.dungeonId.value
                            val dDef = if (dId >= 0) ALL_DUNGEONS.getOrNull(dId) else null

                            // Apply dungeon reward multiplier to gold
                            val goldMultiplier = dDef?.rewardMultiplier ?: 1f
                            val finalGold = (battleResult.goldEarned * goldMultiplier).toInt()

                            // Stage best waves (skip for dungeon mode)
                            val stageIdx = BattleBridge.stageId.value
                            val bestWaves = current.stageBestWaves.toMutableList()
                            if (!isDungeon) {
                                while (bestWaves.size <= stageIdx) bestWaves.add(0)
                                if (battleResult.waveReached > bestWaves[stageIdx]) {
                                    bestWaves[stageIdx] = battleResult.waveReached
                                }
                            }

                            // Dungeon clears record
                            val dungeonClears = current.dungeonClears.toMutableMap()
                            if (isDungeon && dId >= 0) {
                                val prev = dungeonClears[dId] ?: 0
                                if (battleResult.waveReached > prev) {
                                    dungeonClears[dId] = battleResult.waveReached
                                }
                            }

                            // Dungeon-specific bonus rewards
                            var petCardBonus = 0
                            if (isDungeon && dDef != null) {
                                when (dDef.type) {
                                    com.example.jaygame.data.DungeonType.PET_EXPEDITION -> {
                                        // Award pet cards based on waves cleared
                                        petCardBonus = battleResult.waveReached / 4
                                    }
                                    else -> { /* other bonuses handled by rewardMultiplier */ }
                                }
                            }

                            // Star rating bonus: 3-star = +50% gold, 2-star = +25%
                            val starCount = if (!battleResult.victory) 0 else {
                                var s = 1
                                if (battleResult.noHpLost || battleResult.fastClear) s++
                                if (battleResult.noHpLost && battleResult.fastClear) s++
                                s
                            }
                            val starGoldBonus = when (starCount) {
                                3 -> 0.5f
                                2 -> 0.25f
                                else -> 0f
                            }
                            val starBonusGold = (finalGold * starGoldBonus).toInt()

                            // Cards earned → distribute to random units
                            val updatedUnits = addRandomCardsToUnits(current.units, battleResult.cardsEarned)

                            // XP from battle (wave reached * 10, bonus for victory)
                            val xpGained = battleResult.waveReached * 10 + if (battleResult.victory) 50 else 0
                            val newTotalXP = current.totalXP + xpGained
                            val newPlayerLevel = (newTotalXP / 100) + 1 // level up every 100 XP

                            // Season XP — dungeon clears give bonus
                            val dungeonSeasonBonus = if (isDungeon && battleResult.victory) 50 else 0
                            val seasonXpGained = battleResult.waveReached * 5 + (if (battleResult.victory) 30 else 0) + dungeonSeasonBonus
                            val newSeasonXP = current.seasonXP + seasonXpGained

                            // Single-type win detection: deck has only 1 unique family
                            val singleTypeWin = battleResult.victory && current.deck.toSet().size == 1

                            // Apply relic drop if present (boosted chance in RELIC_HUNT dungeon handled by engine)
                            val afterRelicData = if (battleResult.relicDropId >= 0 && battleResult.relicDropGrade >= 0) {
                                val grade = RelicGrade.entries.getOrNull(battleResult.relicDropGrade)
                                if (grade != null) {
                                    RelicManager(current).acquireRelic(battleResult.relicDropId, grade)
                                } else current
                            } else current

                            // 천장 카운터 저장
                            val finalPity = engine?.currentPity ?: BattleBridge.unitPullPity.value

                            // Pet card bonus for PET_EXPEDITION dungeon
                            val finalPets = if (petCardBonus > 0) {
                                afterRelicData.pets.mapIndexed { idx, pet ->
                                    if (pet.owned) pet.copy(cards = pet.cards + petCardBonus / afterRelicData.pets.count { it.owned }.coerceAtLeast(1))
                                    else pet
                                }
                            } else afterRelicData.pets

                            repository.save(afterRelicData.copy(
                                gold = afterRelicData.gold + finalGold + starBonusGold,
                                trophies = if (isDungeon) afterRelicData.trophies else (afterRelicData.trophies + battleResult.trophyChange).coerceAtLeast(0),
                                totalKills = afterRelicData.totalKills + battleResult.killCount,
                                totalMerges = afterRelicData.totalMerges + battleResult.mergeCount,
                                totalGoldEarned = afterRelicData.totalGoldEarned + finalGold + starBonusGold,
                                totalWins = afterRelicData.totalWins + if (battleResult.victory) 1 else 0,
                                totalLosses = afterRelicData.totalLosses + if (!battleResult.victory) 1 else 0,
                                highestWave = maxOf(afterRelicData.highestWave, battleResult.waveReached),
                                wonWithoutDamage = afterRelicData.wonWithoutDamage || battleResult.noHpLost,
                                wonWithSingleType = afterRelicData.wonWithSingleType || singleTypeWin,
                                stageBestWaves = bestWaves,
                                units = updatedUnits,
                                totalXP = newTotalXP,
                                playerLevel = newPlayerLevel,
                                seasonXP = newSeasonXP,
                                unitPullPity = finalPity,
                                dungeonClears = dungeonClears,
                                pets = finalPets,
                                tutorialCompleted = true,
                            ))
                        }
                        BattleBridge.clearResult()
                        BattleBridge.clearDungeonMode()
                        finish()
                    },
                )
            }
        }
    }

    // A4: Apply smooth fade when finishing (Result→Home)
    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onPause() {
        super.onPause()
        BgmManager.stop()
    }

    override fun onDestroy() {
        engine?.stop()
        BattleBridge.engine = null
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
