package com.example.jaygame.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.jaygame.data.GameData
import com.example.jaygame.data.GameRepository
import com.example.jaygame.ui.components.claimReward
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

data class SettingsState(
    val gameData: GameData = GameData(),
    val showResetDialog: Boolean = false,
    val showDailyLogin: Boolean = false,
)

sealed class SettingsSideEffect {
    data class ShowToast(val message: String) : SettingsSideEffect()
    data object DataReset : SettingsSideEffect()
}

class SettingsViewModel(private val repository: GameRepository) : ViewModel(), ContainerHost<SettingsState, SettingsSideEffect> {
    override val container = container<SettingsState, SettingsSideEffect>(SettingsState(gameData = repository.gameData.value))

    init {
        observeGameData()
    }

    private fun observeGameData() = intent {
        repository.gameData.collect { data ->
            reduce { state.copy(gameData = data) }
        }
    }

    fun toggleSound() = intent {
        val d = state.gameData
        repository.save(d.copy(soundEnabled = !d.soundEnabled))
    }

    fun toggleMusic() = intent {
        val d = state.gameData
        repository.save(d.copy(musicEnabled = !d.musicEnabled))
    }

    fun toggleHaptic() = intent {
        val d = state.gameData
        repository.save(d.copy(hapticEnabled = !d.hapticEnabled))
    }

    fun updateGameplay(updated: GameData) = intent {
        repository.save(updated)
    }

    fun showResetDialog() = intent {
        reduce { state.copy(showResetDialog = true) }
    }

    fun dismissResetDialog() = intent {
        reduce { state.copy(showResetDialog = false) }
    }

    fun resetData() = intent {
        repository.save(GameData())
        reduce { state.copy(showResetDialog = false) }
        postSideEffect(SettingsSideEffect.DataReset)
    }

    fun showDailyLogin() = intent {
        reduce { state.copy(showDailyLogin = true) }
    }

    fun claimDailyLogin() = intent {
        val updated = claimReward(state.gameData)
        repository.save(updated)
        reduce { state.copy(showDailyLogin = false) }
    }

    fun dismissDailyLogin() = intent {
        reduce { state.copy(showDailyLogin = false) }
    }
}
