package com.example.jaygame

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.jaygame.audio.BgmManager
import com.example.jaygame.audio.SfxManager
import com.example.jaygame.util.HapticManager
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.STAGES
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.RecipeSystem
import com.example.jaygame.engine.OfflineRewardManager
import com.example.jaygame.navigation.NavGraph
import com.example.jaygame.ui.components.OfflineRewardDialog
import com.example.jaygame.ui.screens.SplashScreen
import com.example.jaygame.ui.theme.JayGameTheme
import kotlinx.coroutines.delay
import kotlin.math.max

class ComposeActivity : ComponentActivity() {
    lateinit var repository: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        BlueprintRegistry.initialize(applicationContext)
        RecipeSystem.initialize(applicationContext)
        repository = GameRepository(this)
        SfxManager.init(this)
        setContent {
            JayGameTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                val data by repository.gameData.collectAsState()

                // Offline reward dialog
                val offlineReward = remember {
                    OfflineRewardManager.calculateReward(repository.gameData.value)
                }
                var showOfflineReward by remember { mutableStateOf(offlineReward != null) }

                // A2: Battle launch zoom-in wipe state
                var battleTransitionActive by remember { mutableStateOf(false) }
                val battleWipeProgress = remember { Animatable(0f) }

                LaunchedEffect(Unit) {
                    delay(1500L)
                    showSplash = false
                }

                // A2: Trigger battle after wipe animation completes
                LaunchedEffect(battleTransitionActive) {
                    if (battleTransitionActive) {
                        battleWipeProgress.snapTo(0f)
                        battleWipeProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                        actuallyLaunchBattle()
                        // Reset after a short delay so overlay persists during activity switch
                        delay(300L)
                        battleTransitionActive = false
                        battleWipeProgress.snapTo(0f)
                    }
                }

                // Sync SFX enabled state with settings
                androidx.compose.runtime.DisposableEffect(data.soundEnabled) {
                    SfxManager.setEnabled(data.soundEnabled)
                    onDispose { }
                }

                // Sync haptic enabled state with settings
                androidx.compose.runtime.DisposableEffect(data.hapticEnabled) {
                    HapticManager.setEnabled(data.hapticEnabled)
                    onDispose { }
                }

                // Default BGM for all non-battle screens
                androidx.compose.runtime.DisposableEffect(data.musicEnabled) {
                    if (data.musicEnabled) {
                        BgmManager.play(this@ComposeActivity, "audio/home_bgm.mp3")
                    } else {
                        BgmManager.stop()
                    }
                    onDispose { }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (showSplash) {
                        SplashScreen()
                    } else {
                        NavGraph(
                            repository = repository,
                            onStartBattle = { battleTransitionActive = true },
                            onStartDungeonBattle = { dungeonId ->
                                BattleBridge.setDungeonMode(dungeonId)
                                battleTransitionActive = true
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // Offline reward dialog
                    if (showOfflineReward && offlineReward != null && !showSplash) {
                        OfflineRewardDialog(
                            reward = offlineReward,
                            onDismiss = { showOfflineReward = false },
                        )
                    }

                    // A2: Battle zoom-in wipe overlay
                    val wipeVal = battleWipeProgress.value
                    if (wipeVal > 0f) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val maxRadius = max(size.width, size.height) * 1.5f
                            val radius = maxRadius * wipeVal
                            drawCircle(
                                color = Color.Black,
                                radius = radius,
                                center = Offset(cx, cy),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        BgmManager.pause()
        // Save last online time for offline reward calculation
        val data = repository.gameData.value
        repository.save(data.copy(lastOnlineTime = System.currentTimeMillis()))
    }

    override fun onDestroy() {
        BgmManager.stop()
        SfxManager.release()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        // Resume default BGM when coming back from battle
        if (repository.gameData.value.musicEnabled) {
            BgmManager.play(this, "audio/home_bgm.mp3")
        }
        repository.refresh()
        BattleBridge.clearResult()

        // Combine all data transformations into a single save to avoid UI thread blocking
        var data = repository.gameData.value

        // Auto-unlock stages based on trophies
        val newUnlocked = STAGES.filter { it.unlockTrophies <= data.trophies }.map { it.id }
        if (newUnlocked.toSet() != data.unlockedStages.toSet()) {
            data = data.copy(unlockedStages = newUnlocked)
        }

        // Apply offline rewards and update last online time
        data = OfflineRewardManager.claimReward(data)

        // Season reset check (monthly)
        val currentMonth = java.time.YearMonth.now().toString() // "2026-03"
        if (data.seasonMonth != currentMonth && data.seasonMonth.isNotEmpty()) {
            // New month → reset season XP and claimed tier
            data = data.copy(
                seasonXP = 0,
                seasonClaimedTier = 0,
                seasonMonth = currentMonth,
            )
        } else if (data.seasonMonth.isEmpty()) {
            // First time — set current month
            data = data.copy(seasonMonth = currentMonth)
        }

        repository.save(data)
    }

    private fun actuallyLaunchBattle() {
        val data = repository.gameData.value
        BattleBridge.setStageId(data.currentStageId)
        BattleBridge.setDifficulty(data.difficulty)
        // Stop main BGM before launching battle
        BgmManager.stop()
        startActivity(android.content.Intent(this, MainActivity::class.java))
        // A2: No default activity animation (we handle it with the wipe overlay)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
