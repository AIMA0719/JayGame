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
    val enemyCount: Int = 0,
    val maxEnemyCount: Int = 100,
    val isBossRound: Boolean = false,
    val bossTimeRemaining: Float = 0f,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BattleState) return false
        return currentWave == other.currentWave && maxWaves == other.maxWaves &&
                playerHP == other.playerHP && maxHP == other.maxHP &&
                sp == other.sp && elapsedTime == other.elapsedTime &&
                state == other.state && summonCost == other.summonCost &&
                deckUnits.contentEquals(other.deckUnits) &&
                enemyCount == other.enemyCount && maxEnemyCount == other.maxEnemyCount &&
                isBossRound == other.isBossRound && bossTimeRemaining == other.bossTimeRemaining
    }
    override fun hashCode(): Int = currentWave * 31 + playerHP + enemyCount * 7 + if (isBossRound) 1 else 0
}

/**
 * 배틀 결과 데이터 (C++ → Compose)
 */
data class BattleResultData(
    val victory: Boolean = false,
    val waveReached: Int = 0,
    val goldEarned: Int = 0,
    val trophyChange: Int = 0,
    val killCount: Int = 0,
    val mergeCount: Int = 0,
    val cardsEarned: Int = 0,
)

/**
 * 그리드 타일 상태 (5x3 = 15타일)
 */
data class GridTileState(
    val unitDefId: Int = -1,    // -1 = empty
    val grade: Int = -1,        // 0=Low, 1=Med, 2=High, 3=Supreme, 4=Transcendent
    val family: Int = -1,       // 0=Fire, 1=Frost, 2=Poison, 3=Lightning, 4=Support
    val canMerge: Boolean = false,
)

object BattleBridge {
    private val _state = MutableStateFlow(BattleState())
    val state: StateFlow<BattleState> = _state.asStateFlow()

    /** 배틀 결과 — null이면 아직 배틀 중 */
    private val _result = MutableStateFlow<BattleResultData?>(null)
    val result: StateFlow<BattleResultData?> = _result.asStateFlow()

    /** 15타일 그리드 상태 (5x3) */
    private val _gridState = MutableStateFlow(List(15) { GridTileState() })
    val gridState: StateFlow<List<GridTileState>> = _gridState.asStateFlow()

    /** 선택된 타일 인덱스 (-1 = 없음) */
    private val _selectedTile = MutableStateFlow(-1)
    val selectedTile: StateFlow<Int> = _selectedTile.asStateFlow()

    /** 유닛 정보 팝업 데이터 */
    data class UnitPopupData(
        val tileIndex: Int,
        val unitDefId: Int,
        val grade: Int,
        val family: Int,
        val canMerge: Boolean,
        val level: Int = 1,
    )
    private val _unitPopup = MutableStateFlow<UnitPopupData?>(null)
    val unitPopup: StateFlow<UnitPopupData?> = _unitPopup.asStateFlow()

    /** 머지 이펙트 (타일 인덱스, lucky 여부) */
    data class MergeEffect(val tileIndex: Int, val isLucky: Boolean)
    private val _mergeEffect = MutableStateFlow<MergeEffect?>(null)
    val mergeEffect: StateFlow<MergeEffect?> = _mergeEffect.asStateFlow()

    @JvmStatic
    fun updateState(
        wave: Int, maxWaves: Int,
        hp: Int, maxHp: Int,
        sp: Float, elapsed: Float,
        state: Int, summonCost: Int,
        enemyCount: Int, isBossRound: Int, bossTimeRemaining: Float,
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
            enemyCount = enemyCount,
            isBossRound = isBossRound != 0,
            bossTimeRemaining = bossTimeRemaining,
        )
    }

    @JvmStatic
    fun setDeck(units: IntArray) {
        _state.value = _state.value.copy(deckUnits = units)
    }

    /**
     * C++에서 배틀 종료 시 호출 (JNI).
     * ResultScene 대신 Compose ResultScreen을 표시하기 위한 데이터 전달.
     */
    @JvmStatic
    fun onBattleEnd(
        victory: Boolean,
        waveReached: Int,
        goldEarned: Int,
        trophyChange: Int,
        killCount: Int,
        mergeCount: Int,
        cardsEarned: Int,
    ) {
        _result.value = BattleResultData(
            victory = victory,
            waveReached = waveReached,
            goldEarned = goldEarned,
            trophyChange = trophyChange,
            killCount = killCount,
            mergeCount = mergeCount,
            cardsEarned = cardsEarned,
        )
    }

    @JvmStatic
    fun updateGridState(unitIds: IntArray, grades: IntArray, families: IntArray, canMerge: BooleanArray) {
        val tiles = List(15) { i ->
            GridTileState(
                unitDefId = unitIds[i],
                grade = grades[i],
                family = families[i],
                canMerge = canMerge[i],
            )
        }
        _gridState.value = tiles
    }

    @JvmStatic
    fun onUnitClicked(tileIndex: Int, unitDefId: Int, grade: Int, family: Int, canMerge: Boolean, level: Int) {
        _selectedTile.value = tileIndex
        _unitPopup.value = UnitPopupData(tileIndex, unitDefId, grade, family, canMerge, level)
    }

    @JvmStatic
    fun onMergeComplete(tileIndex: Int, isLucky: Boolean) {
        _mergeEffect.value = MergeEffect(tileIndex, isLucky)
    }

    fun dismissPopup() {
        _selectedTile.value = -1
        _unitPopup.value = null
    }

    fun clearMergeEffect() {
        _mergeEffect.value = null
    }

    /** 결과 화면 닫힐 때 초기화 */
    fun clearResult() {
        _result.value = null
    }

    /** 새 배틀 시작 시 상태 초기화 */
    fun reset() {
        _state.value = BattleState()
        _result.value = null
        _gridState.value = List(15) { GridTileState() }
        _selectedTile.value = -1
        _unitPopup.value = null
        _mergeEffect.value = null
    }

    // JNI native methods — implemented in main.cpp
    @JvmStatic
    private external fun nativeSummon()

    @JvmStatic
    private external fun nativeClickTile(tileIndex: Int)

    @JvmStatic
    private external fun nativeMergeUnit(tileIndex: Int)

    @JvmStatic
    private external fun nativeSellUnit(tileIndex: Int)

    fun requestSummon() {
        nativeSummon()
    }

    fun requestClickTile(tileIndex: Int) {
        nativeClickTile(tileIndex)
    }

    fun requestMerge(tileIndex: Int) {
        dismissPopup()
        nativeMergeUnit(tileIndex)
    }

    fun requestSell(tileIndex: Int) {
        dismissPopup()
        nativeSellUnit(tileIndex)
    }
}
