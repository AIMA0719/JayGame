package com.example.jaygame.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.jaygame.data.DeckManager
import com.example.jaygame.data.GameData
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.UnitFamily
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.UnitBlueprint
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

data class DeckState(
    val gameData: GameData = GameData(),
    val activePresetIndex: Int = 0,
    val currentDeck: List<String> = emptyList(),
    val availableUnits: List<UnitBlueprint> = emptyList(),
    val selectedFamily: UnitFamily? = null,
    val isDirty: Boolean = false,
)

sealed class DeckSideEffect

class DeckViewModel(private val repository: GameRepository) : ViewModel(),
    ContainerHost<DeckState, DeckSideEffect> {

    override val container = container<DeckState, DeckSideEffect>(
        DeckState(gameData = repository.gameData.value)
    ) {
        observeGameData()
        loadCurrentDeck()
    }

    private fun observeGameData() = intent {
        var prevUnits: Map<String, *>? = null
        repository.gameData.collect { data ->
            val unitsChanged = data.units !== prevUnits
            prevUnits = data.units
            val available = if (unitsChanged && BlueprintRegistry.isReady)
                DeckManager.getAvailableUnits(data) else state.availableUnits
            reduce { state.copy(gameData = data, availableUnits = available) }
        }
    }

    private fun loadCurrentDeck() = intent {
        val data = state.gameData
        reduce {
            state.copy(
                activePresetIndex = data.activeDeckIndex,
                currentDeck = data.deckPresets.getOrElse(data.activeDeckIndex) { emptyList() },
                isDirty = false,
            )
        }
    }

    fun selectPreset(index: Int) = intent {
        if (index == state.activePresetIndex) return@intent
        if (state.isDirty) saveDeckToRepository(state)

        val latestData = repository.gameData.value
        val deck = latestData.deckPresets.getOrElse(index) { emptyList() }
        reduce {
            state.copy(
                gameData = latestData,
                activePresetIndex = index,
                currentDeck = deck,
                isDirty = false,
            )
        }
        repository.save(latestData.copy(activeDeckIndex = index))
    }

    fun addUnit(blueprintId: String) = intent {
        if (state.currentDeck.size >= DeckManager.DECK_SIZE) return@intent
        if (blueprintId in state.currentDeck) return@intent
        reduce { state.copy(currentDeck = state.currentDeck + blueprintId, isDirty = true) }
    }

    fun removeUnit(blueprintId: String) = intent {
        reduce { state.copy(currentDeck = state.currentDeck - blueprintId, isDirty = true) }
    }

    fun autoFill() = intent {
        val filled = DeckManager.autoFill(state.currentDeck, state.gameData)
        reduce { state.copy(currentDeck = filled, isDirty = true) }
    }

    fun saveDeck() = intent {
        saveDeckToRepository(state)
        reduce { state.copy(isDirty = false) }
    }

    fun toggleFamilyFilter(family: UnitFamily?) = intent {
        reduce { state.copy(selectedFamily = if (state.selectedFamily == family) null else family) }
    }

    private fun saveDeckToRepository(s: DeckState) {
        val presets = s.gameData.deckPresets.toMutableList()
        val idx = s.activePresetIndex
        while (presets.size <= idx) presets.add(emptyList())
        presets[idx] = s.currentDeck
        repository.save(s.gameData.copy(deckPresets = presets, activeDeckIndex = idx))
    }
}
