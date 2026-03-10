package com.example.jaygame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.STAGES
import com.example.jaygame.navigation.NavGraph
import com.example.jaygame.ui.theme.JayGameTheme

class ComposeActivity : ComponentActivity() {
    lateinit var repository: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = GameRepository(this)
        setContent {
            JayGameTheme {
                NavGraph(
                    repository = repository,
                    onStartBattle = { launchBattle() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        repository.refresh()
        // Auto-unlock stages based on trophies
        val data = repository.gameData.value
        val newUnlocked = STAGES.filter { it.unlockTrophies <= data.trophies }.map { it.id }
        if (newUnlocked.toSet() != data.unlockedStages.toSet()) {
            repository.save(data.copy(unlockedStages = newUnlocked))
        }
    }

    private fun launchBattle() {
        startActivity(android.content.Intent(this, MainActivity::class.java))
    }
}
