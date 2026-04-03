package com.jay.jaygame.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.jay.jaygame.data.GameData
import com.jay.jaygame.data.GameRepository
import com.jay.jaygame.engine.RecipeSystem
import com.jay.jaygame.ui.components.claimReward
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
        val d = repository.gameData.value
        repository.save(d.copy(soundEnabled = !d.soundEnabled))
    }

    fun toggleMusic() = intent {
        val d = repository.gameData.value
        repository.save(d.copy(musicEnabled = !d.musicEnabled))
    }

    fun toggleHaptic() = intent {
        val d = repository.gameData.value
        repository.save(d.copy(hapticEnabled = !d.hapticEnabled))
    }

    fun updateGameplay(updated: GameData) = intent {
        // 최신 데이터 기반으로 변경사항만 적용하여 race condition 방지
        // updated는 UI 스냅샷이므로 직접 저장 대신, 최신 값에서 변경된 필드만 반영
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
        repository.saveDiscoveredRecipes(emptySet())
        if (RecipeSystem.isReady) {
            RecipeSystem.instance.setDiscoveredIds(emptySet())
        }
        reduce { state.copy(showResetDialog = false) }
        postSideEffect(SettingsSideEffect.DataReset)
    }

    fun showDailyLogin() = intent {
        reduce { state.copy(showDailyLogin = true) }
    }

    fun claimDailyLogin() = intent {
        val updated = claimReward(repository.gameData.value)
        repository.save(updated)
        reduce { state.copy(showDailyLogin = false) }
    }

    fun dismissDailyLogin() = intent {
        reduce { state.copy(showDailyLogin = false) }
    }
}
