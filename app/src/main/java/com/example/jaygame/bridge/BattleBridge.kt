package com.example.jaygame.bridge

import com.example.jaygame.ui.battle.GambleResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

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
data class EnemyPositionData(
    val xs: FloatArray = FloatArray(0),
    val ys: FloatArray = FloatArray(0),
    val types: IntArray = IntArray(0),
    val hpRatios: FloatArray = FloatArray(0),
    val count: Int = 0,
    val frameId: Long = 0L,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnemyPositionData) return false
        return frameId == other.frameId
    }
    override fun hashCode(): Int = frameId.hashCode()
}

data class ProjectileData(
    val srcXs: FloatArray = FloatArray(0),
    val srcYs: FloatArray = FloatArray(0),
    val dstXs: FloatArray = FloatArray(0),
    val dstYs: FloatArray = FloatArray(0),
    val types: IntArray = IntArray(0),  // 0=arrow, 1=fire, 2=ice, 3=poison, 4=lightning
    val count: Int = 0,
    val frameId: Long = 0L,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProjectileData) return false
        return frameId == other.frameId
    }
    override fun hashCode(): Int = frameId.hashCode()
}

data class DamageEvent(
    val x: Float,  // normalized 0-1
    val y: Float,  // normalized 0-1
    val damage: Int,
    val isCrit: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)

data class UnitPositionData(
    val xs: FloatArray = FloatArray(0),
    val ys: FloatArray = FloatArray(0),
    val unitDefIds: IntArray = IntArray(0),
    val grades: IntArray = IntArray(0),
    val levels: IntArray = IntArray(0),
    val isAttacking: BooleanArray = BooleanArray(0),
    val tileIndices: IntArray = IntArray(0),
    val count: Int = 0,
    val frameId: Long = 0L,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnitPositionData) return false
        return frameId == other.frameId
    }
    override fun hashCode(): Int = frameId.hashCode()
}

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
    val level: Int = 0,
)

object BattleBridge {
    private var enemyFrameCounter = 0L
    private var projFrameCounter = 0L
    private var unitFrameCounter = 0L

    private val _state = MutableStateFlow(BattleState())
    val state: StateFlow<BattleState> = _state.asStateFlow()

    /** 배틀 결과 — null이면 아직 배틀 중 */
    private val _result = MutableStateFlow<BattleResultData?>(null)
    val result: StateFlow<BattleResultData?> = _result.asStateFlow()

    /** 적 위치 데이터 (C++ → Compose) */
    private val _enemyPositions = MutableStateFlow(EnemyPositionData())
    val enemyPositions: StateFlow<EnemyPositionData> = _enemyPositions.asStateFlow()

    /** 투사체 위치 데이터 (C++ → Compose) */
    private val _projectiles = MutableStateFlow(ProjectileData())
    val projectiles: StateFlow<ProjectileData> = _projectiles.asStateFlow()

    /** 유닛 위치 데이터 — 자유 이동 (C++ → Compose) */
    private val _unitPositions = MutableStateFlow(UnitPositionData())
    val unitPositions: StateFlow<UnitPositionData> = _unitPositions.asStateFlow()

    /** 데미지 이벤트 (C++ → Compose) */
    private val _damageEvents = MutableStateFlow<List<DamageEvent>>(emptyList())
    val damageEvents: StateFlow<List<DamageEvent>> = _damageEvents.asStateFlow()

    /** 30타일 그리드 상태 (6x5) */
    const val GRID_COLS = 6
    const val GRID_ROWS = 5
    const val GRID_TOTAL = GRID_COLS * GRID_ROWS

    private val _gridState = MutableStateFlow(List(GRID_TOTAL) { GridTileState() })
    val gridState: StateFlow<List<GridTileState>> = _gridState.asStateFlow()

    /** Whether any merge is possible on the grid */
    val canMergeAny: Boolean
        get() = _gridState.value.any { it.canMerge }

    /** 현재 스테이지 ID */
    private val _stageId = MutableStateFlow(0)
    val stageId: StateFlow<Int> = _stageId.asStateFlow()

    fun setStageId(id: Int) { _stageId.value = id }

    /** 현재 난이도 (0=쉬움, 1=보통, 2=어려움) */
    private val _difficulty = MutableStateFlow(1)
    val difficulty: StateFlow<Int> = _difficulty.asStateFlow()

    fun setDifficulty(diff: Int) { _difficulty.value = diff }

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

    /** 소환 결과 데이터 */
    data class SummonResult(val unitDefId: Int, val grade: Int)

    private val _summonResult = MutableStateFlow<SummonResult?>(null)
    val summonResult: StateFlow<SummonResult?> = _summonResult.asStateFlow()

    /** 머지 이펙트 (타일 인덱스, lucky 여부, 결과 유닛 ID) */
    data class MergeEffect(val tileIndex: Int, val isLucky: Boolean, val resultUnitId: Int = -1)
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

        // Clear visual effects on wave end
        if (state == 0 || state == 2 || state == 3) {
            _damageEvents.value = emptyList()
        }
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
    fun updateEnemyPositions(xs: FloatArray, ys: FloatArray, types: IntArray, hpRatios: FloatArray, count: Int) {
        _enemyPositions.value = EnemyPositionData(xs, ys, types, hpRatios, count, ++enemyFrameCounter)
    }

    @JvmStatic
    fun updateProjectiles(srcXs: FloatArray, srcYs: FloatArray, dstXs: FloatArray, dstYs: FloatArray, types: IntArray, count: Int) {
        _projectiles.value = ProjectileData(srcXs, srcYs, dstXs, dstYs, types, count, ++projFrameCounter)
    }

    @JvmStatic
    fun updateUnitPositions(xs: FloatArray, ys: FloatArray, unitDefIds: IntArray,
                            grades: IntArray, levels: IntArray, isAttacking: BooleanArray,
                            tileIndices: IntArray, count: Int) {
        _unitPositions.value = UnitPositionData(xs, ys, unitDefIds, grades, levels, isAttacking, tileIndices, count, ++unitFrameCounter)
    }

    @JvmStatic
    fun onDamageDealt(x: Float, y: Float, damage: Int, isCrit: Boolean) {
        val event = DamageEvent(x, y, damage, isCrit)
        val current = _damageEvents.value.toMutableList()
        current.add(event)
        // Keep only recent events (last 800ms)
        val cutoff = System.currentTimeMillis() - 800L
        _damageEvents.value = current.filter { it.timestamp > cutoff }
    }

    @JvmStatic
    fun updateGridState(unitIds: IntArray, grades: IntArray, families: IntArray, canMerge: BooleanArray, levels: IntArray) {
        val count = unitIds.size.coerceAtMost(GRID_TOTAL)
        val tiles = List(count) { i ->
            GridTileState(
                unitDefId = unitIds[i],
                grade = grades[i],
                family = families[i],
                canMerge = canMerge[i],
                level = levels[i],
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
    fun onSummonResult(unitDefId: Int, grade: Int) {
        _summonResult.value = SummonResult(unitDefId, grade)
    }

    @JvmStatic
    fun onMergeComplete(tileIndex: Int, isLucky: Boolean, resultUnitId: Int) {
        _mergeEffect.value = MergeEffect(tileIndex, isLucky, resultUnitId)
    }

    fun dismissPopup() {
        _selectedTile.value = -1
        _unitPopup.value = null
    }

    fun clearSummonResult() {
        _summonResult.value = null
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
        enemyFrameCounter = 0L
        _enemyPositions.value = EnemyPositionData()
        projFrameCounter = 0L
        _projectiles.value = ProjectileData()
        unitFrameCounter = 0L
        _unitPositions.value = UnitPositionData()
        _damageEvents.value = emptyList()
        _gridState.value = List(GRID_TOTAL) { GridTileState() }
        _selectedTile.value = -1
        _unitPopup.value = null
        _mergeEffect.value = null
        _summonResult.value = null
        _stageId.value = 0
        _battleUpgradeLevels.value = IntArray(5) { 0 }
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

    @JvmStatic
    private external fun nativeUpgradeUnit(tileIndex: Int)

    @JvmStatic
    private external fun nativeSwapUnits(fromTile: Int, toTile: Int)

    @JvmStatic
    private external fun nativeRelocateUnit(tileIndex: Int, normX: Float, normY: Float)

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

    fun requestUpgrade(tileIndex: Int) {
        nativeUpgradeUnit(tileIndex)
    }

    fun requestSwap(fromTile: Int, toTile: Int) {
        nativeSwapUnits(fromTile, toTile)
    }

    fun requestRelocate(tileIndex: Int, normX: Float, normY: Float) {
        nativeRelocateUnit(tileIndex, normX, normY)
    }

    /** In-battle upgrade costs: level 1->2 through 6->7 */
    val UPGRADE_COSTS = intArrayOf(30, 60, 100, 150, 220, 300)

    fun getUpgradeCost(currentLevel: Int): Int {
        val idx = currentLevel - 1
        return if (idx in UPGRADE_COSTS.indices) UPGRADE_COSTS[idx] else -1
    }

    // ── Gamble ──────────────────────────────────────────────

    private const val GAMBLE_COST = 10f

    /**
     * Perform gamble: costs 10 SP, random -100% to +100% of remaining SP.
     * Returns result for UI display.
     * Note: This modifies SP via C++ nativeGamble() JNI call.
     */
    fun performGamble(): GambleResult {
        val currentSp = _state.value.sp
        if (currentSp < GAMBLE_COST) return GambleResult(0f, currentSp, 0f)

        val spAfterCost = currentSp - GAMBLE_COST
        // Random percentage from -100% to +100%
        val percentage = Random.nextFloat() * 2f - 1f  // -1.0 to 1.0
        val spChange = spAfterCost * percentage
        val newSp = (spAfterCost + spChange).coerceAtLeast(0f)

        // Tell C++ to set SP to new value
        nativeGamble(newSp)

        return GambleResult(spChange, newSp, percentage)
    }

    // ── Buy Unit ────────────────────────────────────────────

    fun requestBuyUnit(unitDefId: Int, cost: Int) {
        nativeBuyUnit(unitDefId, cost.toFloat())
    }

    // ── Battle Upgrades ─────────────────────────────────────

    private val _battleUpgradeLevels = MutableStateFlow(IntArray(5) { 0 })
    val battleUpgradeLevels: StateFlow<IntArray> = _battleUpgradeLevels.asStateFlow()

    fun requestBattleUpgrade(upgradeType: Int, cost: Int) {
        val currentSp = _state.value.sp
        if (currentSp < cost) return

        // Update local upgrade level
        val levels = _battleUpgradeLevels.value.copyOf()
        levels[upgradeType]++
        _battleUpgradeLevels.value = levels

        // Tell C++ to apply and deduct SP
        nativeApplyBattleUpgrade(upgradeType, levels[upgradeType], cost.toFloat())
    }

    // ── Additional JNI methods ──────────────────────────────

    @JvmStatic
    private external fun nativeGamble(newSp: Float)

    @JvmStatic
    private external fun nativeBuyUnit(unitDefId: Int, cost: Float)

    @JvmStatic
    private external fun nativeApplyBattleUpgrade(upgradeType: Int, level: Int, cost: Float)
}
