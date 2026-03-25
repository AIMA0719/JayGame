package com.example.jaygame.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.jaygame.data.ALL_PROFILES
import com.example.jaygame.data.GameData
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.ProfileDef
import com.example.jaygame.data.StaminaManager
import com.example.jaygame.ui.components.canClaim
import com.example.jaygame.ui.components.claimReward
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

data class HomeState(
    val gameData: GameData = GameData(),
    val showDailyLogin: Boolean = false,
    val showPreBattle: Boolean = false,
    val newTitle: ProfileDef? = null,
)

sealed class HomeSideEffect {
    data object LaunchBattle : HomeSideEffect()
    data class ShowToast(val message: String) : HomeSideEffect()
}

class HomeViewModel(private val repository: GameRepository) : ViewModel(), ContainerHost<HomeState, HomeSideEffect> {
    override val container = container<HomeState, HomeSideEffect>(HomeState(gameData = repository.gameData.value))

    init {
        observeGameData()
    }

    private fun observeGameData() = intent {
        repository.gameData.collect { data ->
            reduce { state.copy(gameData = data) }
        }
    }

    fun checkDailyLogin() = intent {
        if (canClaim(state.gameData)) {
            reduce { state.copy(showDailyLogin = true) }
        }
    }

    fun checkNewTitles() = intent {
        val data = state.gameData
        val nowUnlocked = ALL_PROFILES.filter { it.condition(data) }.map { it.id }.toSet()
        val newIds = nowUnlocked - data.unlockedProfiles
        if (newIds.isNotEmpty()) {
            val updated = data.copy(unlockedProfiles = data.unlockedProfiles + nowUnlocked)
            repository.save(updated)
            val firstNew = ALL_PROFILES.firstOrNull { it.id == newIds.first() }
            if (firstNew != null) {
                reduce { state.copy(newTitle = firstNew) }
            }
        }
    }

    fun dismissNewTitle() = intent {
        reduce { state.copy(newTitle = null) }
    }

    fun claimDailyLogin() = intent {
        val updated = claimReward(state.gameData)
        repository.save(updated)
        reduce { state.copy(showDailyLogin = false) }
    }

    fun dismissDailyLogin() = intent {
        reduce { state.copy(showDailyLogin = false) }
    }

    fun selectStage(stageId: Int) = intent {
        if (stageId != state.gameData.currentStageId) {
            repository.save(state.gameData.copy(currentStageId = stageId))
        }
    }

    fun selectDifficulty(diff: Int) = intent {
        repository.save(state.gameData.copy(difficulty = diff))
    }

    fun showPreBattle() = intent {
        reduce { state.copy(showPreBattle = true) }
    }

    fun dismissPreBattle() = intent {
        reduce { state.copy(showPreBattle = false) }
    }

    fun startBattle(staminaCost: Int) = intent {
        val consumed = StaminaManager.consume(state.gameData, staminaCost)
        if (consumed != null) {
            repository.save(consumed.copy(currentStageId = state.gameData.currentStageId))
            reduce { state.copy(showPreBattle = false) }
            postSideEffect(HomeSideEffect.LaunchBattle)
        } else {
            postSideEffect(HomeSideEffect.ShowToast("스태미나가 부족합니다."))
        }
    }
}
