package com.example.jaygame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.jaygame.audio.BgmManager
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.STAGES
import com.example.jaygame.navigation.NavGraph
import com.example.jaygame.ui.screens.SplashScreen
import com.example.jaygame.ui.theme.JayGameTheme
import kotlinx.coroutines.delay

class ComposeActivity : ComponentActivity() {
    lateinit var repository: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = GameRepository(this)
        setContent {
            JayGameTheme {
                var showSplash by remember { mutableStateOf(true) }
                val data by repository.gameData.collectAsState()

                LaunchedEffect(Unit) {
                    delay(1500L)
                    showSplash = false
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

                if (showSplash) {
                    SplashScreen()
                } else {
                    NavGraph(
                        repository = repository,
                        onStartBattle = { launchBattle() },
                        modifier = Modifier.fillMaxSize(),
                    )
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
        // Auto-unlock stages based on trophies
        val data = repository.gameData.value
        val newUnlocked = STAGES.filter { it.unlockTrophies <= data.trophies }.map { it.id }
        if (newUnlocked.toSet() != data.unlockedStages.toSet()) {
            repository.save(data.copy(unlockedStages = newUnlocked))
        }
    }

    private fun launchBattle() {
        val data = repository.gameData.value
        BattleBridge.setStageId(data.currentStageId)
        BattleBridge.setDifficulty(data.difficulty)
        // Stop main BGM before launching battle
        BgmManager.stop()
        startActivity(android.content.Intent(this, MainActivity::class.java))
    }
}
