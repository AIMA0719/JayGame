package com.example.jaygame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

                LaunchedEffect(Unit) {
                    delay(1500L)
                    showSplash = false
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

    override fun onResume() {
        super.onResume()
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
        startActivity(android.content.Intent(this, MainActivity::class.java))
    }
}
