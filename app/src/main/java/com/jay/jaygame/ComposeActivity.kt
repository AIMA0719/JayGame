package com.jay.jaygame

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
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jay.jaygame.audio.BgmManager
import com.jay.jaygame.audio.SfxManager
import com.jay.jaygame.bridge.BattleBridge
import com.jay.jaygame.data.TimeGuard
import com.jay.jaygame.navigation.NavGraph
import com.jay.jaygame.ui.components.RaceDraftDialog
import com.jay.jaygame.ui.screens.SplashScreen
import com.jay.jaygame.ui.theme.JayGameTheme
import com.jay.jaygame.ui.viewmodel.AppViewModel
import com.jay.jaygame.ui.viewmodel.gameViewModelFactory
import com.jay.jaygame.util.HapticManager
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.compose.collectAsState
import kotlin.math.max

class ComposeActivity : ComponentActivity() {
    private lateinit var appVm: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            JayGameTheme {
                val app = application as JayGameApplication
                val initReady by app.initReady.collectAsState(initial = false)
                val initError by app.initError.collectAsState(initial = null)
                var showSplash by rememberSaveable { mutableStateOf(true) }
                val splashStartTime = remember { System.currentTimeMillis() }

                LaunchedEffect(initReady) {
                    if (initReady) {
                        val elapsed = System.currentTimeMillis() - splashStartTime
                        delay((800L - elapsed).coerceAtLeast(0L))
                        showSplash = false
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (showSplash && initError == null) {
                        SplashScreen()
                    } else if (initError != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = initError ?: "Initialization failed")
                        }
                    } else {
                        val factory = gameViewModelFactory()
                        val vm: AppViewModel = viewModel(factory = factory)
                        appVm = vm

                        val appState by vm.collectAsState()
                        val data = appState.gameData
                        var showRaceDraft by remember { mutableStateOf(false) }

                        // A2: Battle launch zoom-in wipe state
                        var battleTransitionActive by remember { mutableStateOf(false) }
                        val battleWipeProgress = remember { Animatable(0f) }

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
                                delay(300L)
                                battleTransitionActive = false
                                battleWipeProgress.snapTo(0f)
                            }
                        }

                        // Sync SFX enabled state with settings
                        DisposableEffect(data.soundEnabled) {
                            SfxManager.setEnabled(data.soundEnabled)
                            onDispose { }
                        }

                        // Sync haptic enabled state with settings
                        DisposableEffect(data.hapticEnabled) {
                            HapticManager.setEnabled(data.hapticEnabled)
                            onDispose { }
                        }

                        // Default BGM for all non-battle screens
                        DisposableEffect(data.musicEnabled) {
                            if (data.musicEnabled) {
                                BgmManager.play(this@ComposeActivity, "audio/home_bgm.mp3")
                            } else {
                                BgmManager.stop()
                            }
                            onDispose { BgmManager.stop() }
                        }

                        NavGraph(
                            factory = factory,
                            onStartBattle = { showRaceDraft = true },
                            onStartDungeonBattle = { dungeonId ->
                                BattleBridge.setDungeonMode(dungeonId)
                                battleTransitionActive = true
                            },
                            modifier = Modifier.fillMaxSize(),
                        )

                        if (showRaceDraft) {
                            RaceDraftDialog(
                                onConfirm = { selectedRaces ->
                                    BattleBridge.setSelectedRaces(selectedRaces)
                                    showRaceDraft = false
                                    battleTransitionActive = true
                                },
                                onDismiss = { showRaceDraft = false }
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
    }

    override fun onPause() {
        super.onPause()
        BgmManager.pause()
    }

    override fun onDestroy() {
        BgmManager.stop()
        SfxManager.release()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        TimeGuard.onSessionStart()
        val app = application as JayGameApplication
        if (!app.initReady.value) return
        if (::appVm.isInitialized) {
            appVm.onResume()
        }
        if (app.repository.gameData.value.musicEnabled) {
            BgmManager.play(this, "audio/home_bgm.mp3")
        }
    }

    private fun actuallyLaunchBattle() {
        val repo = (application as JayGameApplication).repository
        val data = repo.gameData.value
        BattleBridge.setStageId(data.currentStageId)
        BattleBridge.setDifficulty(data.difficulty)
        BgmManager.stop()
        startActivity(android.content.Intent(this, MainActivity::class.java))
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
