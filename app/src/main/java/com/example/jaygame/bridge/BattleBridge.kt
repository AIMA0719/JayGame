package com.example.jaygame.bridge

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BattleState(
    val currentWave: Int = 0,
    val maxWaves: Int = 40,
    val playerHP: Int = 20,
    val maxHP: Int = 20,
    val sp: Float = 100f,
    val elapsedTime: Float = 0f,
    val state: Int = 0, // 0=WaveDelay, 1=Playing, 2=Victory, 3=Defeat
    val summonCost: Int = 50,
    val deckUnits: IntArray = intArrayOf(0, 1, 2, 3, 4),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BattleState) return false
        return currentWave == other.currentWave && maxWaves == other.maxWaves &&
                playerHP == other.playerHP && maxHP == other.maxHP &&
                sp == other.sp && elapsedTime == other.elapsedTime &&
                state == other.state && summonCost == other.summonCost &&
                deckUnits.contentEquals(other.deckUnits)
    }
    override fun hashCode(): Int = currentWave * 31 + playerHP
}

object BattleBridge {
    private val _state = MutableStateFlow(BattleState())
    val state: StateFlow<BattleState> = _state.asStateFlow()

    @JvmStatic
    fun updateState(
        wave: Int, maxWaves: Int,
        hp: Int, maxHp: Int,
        sp: Float, elapsed: Float,
        state: Int, summonCost: Int,
    ) {
        _state.value = BattleState(
            currentWave = wave,
            maxWaves = maxWaves,
            playerHP = hp,
            maxHP = maxHp,
            sp = sp,
            elapsedTime = elapsed,
            state = state,
            summonCost = summonCost,
            deckUnits = _state.value.deckUnits,
        )
    }

    @JvmStatic
    fun setDeck(units: IntArray) {
        _state.value = _state.value.copy(deckUnits = units)
    }

    // Callback-based approach — avoids JNI external functions that have no C++ impl
    var onSummonRequested: (() -> Unit)? = null
    var onPauseRequested: (() -> Unit)? = null

    fun requestSummon() {
        onSummonRequested?.invoke()
    }

    fun requestPause() {
        onPauseRequested?.invoke()
    }
}
