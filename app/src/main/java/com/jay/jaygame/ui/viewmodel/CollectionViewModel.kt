package com.jay.jaygame.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.jay.jaygame.data.GameData
import com.jay.jaygame.data.GameRepository
import com.jay.jaygame.engine.PetManager
import com.jay.jaygame.engine.RelicManager
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

data class CollectionState(
    val gameData: GameData = GameData(),
)

sealed class CollectionSideEffect {
    data class ShowToast(val message: String) : CollectionSideEffect()
}

class CollectionViewModel(private val repository: GameRepository) : ViewModel(), ContainerHost<CollectionState, CollectionSideEffect> {
    override val container = container<CollectionState, CollectionSideEffect>(CollectionState(gameData = repository.gameData.value))

    init {
        observeGameData()
    }

    private fun observeGameData() = intent {
        repository.gameData.collect { data ->
            reduce { state.copy(gameData = data) }
        }
    }

    fun upgradeRelic(relicId: Int) = intent {
        val mgr = RelicManager(state.gameData)
        val updated = mgr.upgradeRelic(relicId)
        if (updated != null) {
            repository.save(updated)
        } else {
            postSideEffect(CollectionSideEffect.ShowToast("업그레이드 불가"))
        }
    }

    fun equipRelic(relicId: Int) = intent {
        val mgr = RelicManager(state.gameData)
        val updated = mgr.equipRelic(relicId)
        if (updated != null) repository.save(updated)
    }

    fun unequipRelic(relicId: Int) = intent {
        val mgr = RelicManager(state.gameData)
        repository.save(mgr.unequipRelic(relicId))
    }

    fun pullPet() = intent {
        val pm = PetManager(state.gameData)
        val updated = pm.pullPet()
        if (updated != null) {
            repository.save(updated)
        } else {
            postSideEffect(CollectionSideEffect.ShowToast("다이아가 부족합니다."))
        }
    }

    fun pullPet10() = intent {
        val pm = PetManager(state.gameData)
        val updated = pm.pullPet10()
        if (updated != null) {
            repository.save(updated)
        } else {
            postSideEffect(CollectionSideEffect.ShowToast("다이아가 부족합니다."))
        }
    }

    fun upgradePet(petId: Int) = intent {
        val pm = PetManager(state.gameData)
        val updated = pm.upgradePet(petId)
        if (updated != null) repository.save(updated)
    }

    fun equipPet(petId: Int) = intent {
        val pm = PetManager(state.gameData)
        val updated = pm.equipPet(petId)
        if (updated != null) repository.save(updated)
    }

    fun unequipPet(petId: Int) = intent {
        val pm = PetManager(state.gameData)
        repository.save(pm.unequipPet(petId))
    }

    fun upgradeUnit(blueprintId: String, cardCost: Int, goldCost: Int) = intent {
        val d = state.gameData
        val unit = d.units[blueprintId] ?: return@intent
        if (d.gold < goldCost || unit.cards < cardCost) return@intent

        val newMaxLevel = maxOf(d.maxUnitLevel, unit.level + 1)
        val updated = d.copy(
            gold = d.gold - goldCost,
            maxUnitLevel = newMaxLevel,
            units = d.units + (blueprintId to unit.copy(
                level = unit.level + 1,
                cards = unit.cards - cardCost,
            )),
        )
        repository.save(updated)
    }

}
