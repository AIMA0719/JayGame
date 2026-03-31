package com.example.jaygame

import android.content.pm.ActivityInfo
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
import com.example.jaygame.data.ALL_DUNGEONS
import com.example.jaygame.data.STAGES
import com.example.jaygame.engine.BattleEngine
import com.example.jaygame.engine.BattleRewardCalculator
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.RecipeSystem
import com.example.jaygame.ui.battle.BattleScreen
import com.example.jaygame.ui.theme.JayGameTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {
    private var engine: BattleEngine? = null
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        BlueprintRegistry.initialize(applicationContext)
        RecipeSystem.initialize(applicationContext)
        val repository = (application as JayGameApplication).repository

        // Preserve stageId/difficulty/dungeon set by ComposeActivity, then reset battle state
        val stageId = BattleBridge.stageId.value
        val difficulty = BattleBridge.difficulty.value
        val dungeonId = BattleBridge.dungeonId.value
        BattleBridge.reset()
        BattleBridge.setStageId(stageId)
        BattleBridge.setDifficulty(difficulty)
        if (dungeonId >= 0) BattleBridge.setDungeonMode(dungeonId)
        SfxManager.init(this)

        // Create and start Kotlin battle engine
        val stage = STAGES.getOrNull(stageId) ?: STAGES[0]
        val data = repository.gameData.value
        val dungeonDef = if (dungeonId >= 0) ALL_DUNGEONS.getOrNull(dungeonId) else null

        // Apply gameplay settings
        BattleBridge.setBattleSpeed(data.defaultBattleSpeed)
        BattleBridge.applyGameplaySettings(
            showDamage = data.showDamageNumbers,
            hpBarMode = data.healthBarMode,
            effectQual = data.effectQuality,
            autoWave = data.autoWaveStart,
        )
        val effectivePity = data.unitPullPity
        BattleBridge.updateUnitPullPity(effectivePity)
        BattleBridge.setTutorialMode(!data.tutorialCompleted)
        BattleBridge.setEquippedPets(data.equippedPets)

        val maxWaves = dungeonDef?.waveCount ?: stage.maxWaves
        val effectiveDifficulty = if (dungeonDef != null) {
            (difficulty * dungeonDef.difficultyMultiplier).toInt().coerceAtLeast(difficulty)
        } else difficulty

        engine = BattleEngine(
            stageId = stageId,
            difficulty = effectiveDifficulty,
            maxWaves = maxWaves,
            gameData = data,
            initialPity = effectivePity,
        ).also {
            if (dungeonDef != null) {
                it.isDungeonMode = true
                it.dungeonDef = dungeonDef
            }
            BattleBridge.engine = it
            it.start(engineScope)
        }

        // Play biome-specific battle BGM
        if (data.musicEnabled) {
            val bgmAsset = when (stageId) {
                1 -> "audio/battle_jungle.mp3"
                2 -> "audio/battle_desert.mp3"
                3 -> "audio/battle_glacier.mp3"
                4 -> "audio/battle_volcano.mp3"
                5 -> "audio/battle_abyss.mp3"
                else -> "audio/battle_plains.mp3"
            }
            BgmManager.play(this, bgmAsset)
        }

        setContent {
            JayGameTheme {
                val result by BattleBridge.result.collectAsState()
                val gameData by repository.gameData.collectAsState()
                BattleScreen(
                    result = result,
                    bgmEnabled = gameData.musicEnabled,
                    sfxEnabled = gameData.soundEnabled,
                    onToggleBgm = {
                        val d = repository.gameData.value
                        val newVal = !d.musicEnabled
                        repository.save(d.copy(musicEnabled = newVal))
                        if (newVal) BgmManager.resume() else BgmManager.pause()
                    },
                    onToggleSfx = {
                        val d = repository.gameData.value
                        val newVal = !d.soundEnabled
                        repository.save(d.copy(soundEnabled = newVal))
                        SfxManager.setEnabled(newVal)
                    },
                    onGoHome = {
                        val battleResult = BattleBridge.result.value
                        if (battleResult != null) {
                            val updatedData = BattleRewardCalculator.calculateRewards(
                                current = repository.gameData.value,
                                battleResult = battleResult,
                                stageId = BattleBridge.stageId.value,
                                isDungeon = BattleBridge.isDungeonMode,
                                dungeonId = BattleBridge.dungeonId.value,
                                engine = engine,
                            )
                            repository.save(updatedData)
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
        BattleBridge.pauseByLifecycle()
        BgmManager.pause()
    }

    override fun onResume() {
        super.onResume()
        BattleBridge.resumeFromLifecycle()
        BgmManager.resume()
    }

    override fun onDestroy() {
        engine?.stop()
        BattleBridge.engine = null
        engineScope.cancel()
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
