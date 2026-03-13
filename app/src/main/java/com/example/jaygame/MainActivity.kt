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
import com.example.jaygame.data.STAGES
import com.example.jaygame.data.addRandomCardsToUnits
import com.example.jaygame.engine.BattleEngine
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

        // Apply default battle speed from settings
        BattleBridge.setBattleSpeed(data.defaultBattleSpeed)
        engine = BattleEngine(
            stageId = stageId,
            difficulty = difficulty,
            maxWaves = stage.maxWaves,
            deck = data.deck.toIntArray(),
        ).also {
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

                            // Stage best waves
                            val stageIdx = BattleBridge.stageId.value
                            val bestWaves = current.stageBestWaves.toMutableList()
                            while (bestWaves.size <= stageIdx) bestWaves.add(0)
                            if (battleResult.waveReached > bestWaves[stageIdx]) {
                                bestWaves[stageIdx] = battleResult.waveReached
                            }

                            // Cards earned → distribute to random units
                            val updatedUnits = addRandomCardsToUnits(current.units, battleResult.cardsEarned)

                            // XP from battle (wave reached * 10, bonus for victory)
                            val xpGained = battleResult.waveReached * 10 + if (battleResult.victory) 50 else 0
                            val newTotalXP = current.totalXP + xpGained
                            val newPlayerLevel = (newTotalXP / 100) + 1 // level up every 100 XP

                            // Season XP
                            val seasonXpGained = battleResult.waveReached * 5 + if (battleResult.victory) 30 else 0
                            val newSeasonXP = current.seasonXP + seasonXpGained

                            // Single-type win detection: deck has only 1 unique family
                            val singleTypeWin = battleResult.victory && current.deck.toSet().size == 1

                            repository.save(current.copy(
                                gold = current.gold + battleResult.goldEarned,
                                trophies = (current.trophies + battleResult.trophyChange).coerceAtLeast(0),
                                totalKills = current.totalKills + battleResult.killCount,
                                totalMerges = current.totalMerges + battleResult.mergeCount,
                                totalGoldEarned = current.totalGoldEarned + battleResult.goldEarned,
                                totalWins = current.totalWins + if (battleResult.victory) 1 else 0,
                                totalLosses = current.totalLosses + if (!battleResult.victory) 1 else 0,
                                highestWave = maxOf(current.highestWave, battleResult.waveReached),
                                wonWithoutDamage = current.wonWithoutDamage || battleResult.noHpLost,
                                wonWithSingleType = current.wonWithSingleType || singleTypeWin,
                                stageBestWaves = bestWaves,
                                units = updatedUnits,
                                totalXP = newTotalXP,
                                playerLevel = newPlayerLevel,
                                seasonXP = newSeasonXP,
                            ))
                        }
                        BattleBridge.clearResult()
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
