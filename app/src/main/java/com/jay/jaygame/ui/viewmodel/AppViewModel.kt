package com.jay.jaygame.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.jay.jaygame.bridge.BattleBridge
import com.jay.jaygame.data.GameData
import com.jay.jaygame.data.GameRepository
import com.jay.jaygame.data.STAGES
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

data class AppState(
    val gameData: GameData = GameData(),
)

sealed class AppSideEffect

class AppViewModel(private val repository: GameRepository) : ViewModel(), ContainerHost<AppState, AppSideEffect> {
    override val container = container<AppState, AppSideEffect>(AppState(gameData = repository.gameData.value))

    init {
        observeGameData()
    }

    private fun observeGameData() = intent {
        repository.gameData.collect { data ->
            reduce { state.copy(gameData = data) }
        }
    }

    fun onResume() = intent {
        repository.refresh()
        BattleBridge.clearResult()
        var data = repository.gameData.value

        val newUnlocked = STAGES.filter { it.unlockTrophies <= data.trophies }.map { it.id }
        if (newUnlocked.toSet() != data.unlockedStages.toSet()) {
            data = data.copy(unlockedStages = newUnlocked)
        }

        val currentMonth = java.time.YearMonth.now().toString()
        if (data.seasonMonth != currentMonth && data.seasonMonth.isNotEmpty()) {
            data = data.copy(seasonXP = 0, seasonClaimedTier = 0, seasonMonth = currentMonth)
        } else if (data.seasonMonth.isEmpty()) {
            data = data.copy(seasonMonth = currentMonth)
        }

        repository.save(data)
    }
}
