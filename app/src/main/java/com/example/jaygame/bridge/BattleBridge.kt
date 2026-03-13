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
/**
 * Buff bitmask constants for enemy visual effects.
 * bit0=Slow, bit1=DoT, bit2=ArmorBreak, bit3=Knockback(wind hit)
 */
const val BUFF_BIT_SLOW       = 1 shl 0  // Frost slow
const val BUFF_BIT_DOT        = 1 shl 1  // Fire/Poison DoT
const val BUFF_BIT_ARMOR_BREAK = 1 shl 2 // ArmorBreak
const val BUFF_BIT_POISON     = 1 shl 3  // Poison-family DoT (slow+dot combo)
const val BUFF_BIT_LIGHTNING   = 1 shl 4 // Recently hit by lightning chain
const val BUFF_BIT_WIND       = 1 shl 5  // Recently knocked back

data class EnemyPositionData(
    val xs: FloatArray = FloatArray(0),
    val ys: FloatArray = FloatArray(0),
    val types: IntArray = IntArray(0),
    val hpRatios: FloatArray = FloatArray(0),
    val buffs: IntArray = IntArray(0),
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
    val noHpLost: Boolean = false,
    val fastClear: Boolean = false,
)

/**
 * 그리드 타일 상태 (6x5 = 30타일)
 */
data class GridTileState(
    val unitDefId: Int = -1,    // -1 = empty
    val grade: Int = -1,        // 0=Common, 1=Uncommon, 2=Rare, 3=Epic, 4=Legend, 5=Hero, 6=Immortal
    val family: Int = -1,       // 0=Fire, 1=Frost, 2=Poison, 3=Lightning, 4=Support, 5=Wind
    val canMerge: Boolean = false,
    val level: Int = 0,
)

/**
 * 스킬 VFX 타입 — 각 고유 스킬에 대응하는 시각 효과 종류.
 */
enum class SkillVfxType {
    // Fire family
    LINGERING_FLAME, FIRESTORM_METEOR, VOLCANIC_ERUPTION,
    PHOENIX_CARPET_BOMB, PHOENIX_REVIVE, SUPERNOVA,
    // Frost family
    FROST_NOVA, ABSOLUTE_ZERO, ICE_AGE_BLIZZARD,
    ETERNAL_WINTER, TIME_STOP,
    // Poison family
    POISON_CLOUD, ACID_SPRAY, TOXIC_DOMAIN,
    NIDHOGG_BREATH, UNIVERSAL_DECAY,
    // Lightning family
    LIGHTNING_STRIKE, STATIC_FIELD, THUNDERSTORM,
    MJOLNIR_THROW, DIVINE_PUNISHMENT,
    // Support family
    HEAL_PULSE, WAR_SONG_AURA, DIVINE_SHIELD,
    HARMONY_FIELD, GENESIS_LIGHT,
    // Wind family
    CYCLONE_PULL, EYE_OF_STORM, VACUUM_SLASH,
    DIMENSIONAL_SLASH, BREATH_OF_ALL,
}

/**
 * 스킬 이벤트 — 엔진에서 발생하여 Compose 오버레이에서 렌더링.
 */
data class SkillEvent(
    val type: SkillVfxType,
    val x: Float,           // normalized 0-1
    val y: Float,           // normalized 0-1
    val radius: Float = 0f, // normalized radius (0-1)
    val grade: Int = 0,
    val family: Int = 0,
    val startTime: Long = System.currentTimeMillis(),
    val duration: Float = 1f, // seconds
)

data class GoldPickupEvent(
    val x: Float,       // normalized 0-1
    val y: Float,       // normalized 0-1
    val amount: Int,
    val timestamp: Long = System.currentTimeMillis(),
)

data class LevelUpEvent(
    val x: Float,       // normalized 0-1
    val y: Float,       // normalized 0-1
    val timestamp: Long = System.currentTimeMillis(),
)

object BattleBridge {
    private var enemyFrameCounter = 0L
    private var projFrameCounter = 0L
    private var unitFrameCounter = 0L

    /** Z16: Debug overlay toggle */
    private val _debugMode = MutableStateFlow(false)
    val debugMode: StateFlow<Boolean> = _debugMode.asStateFlow()

    fun toggleDebugMode() {
        _debugMode.value = !_debugMode.value
    }

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

    /** 25타일 그리드 상태 (5x5) */
    const val GRID_COLS = 5
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
        noHpLost: Boolean = false,
        fastClear: Boolean = false,
    ) {
        _result.value = BattleResultData(
            victory = victory,
            waveReached = waveReached,
            goldEarned = goldEarned,
            trophyChange = trophyChange,
            killCount = killCount,
            mergeCount = mergeCount,
            cardsEarned = cardsEarned,
            noHpLost = noHpLost,
            fastClear = fastClear,
        )
    }

    @JvmStatic
    fun updateEnemyPositions(xs: FloatArray, ys: FloatArray, types: IntArray, hpRatios: FloatArray, buffs: IntArray, count: Int) {
        _enemyPositions.value = EnemyPositionData(xs, ys, types, hpRatios, buffs, count, ++enemyFrameCounter)
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

    /** 스킬 이벤트 목록 — SkillEffectOverlay에서 렌더링 */
    private val _skillEvents = MutableStateFlow<List<SkillEvent>>(emptyList())
    val skillEvents: StateFlow<List<SkillEvent>> = _skillEvents.asStateFlow()

    fun emitSkillEvent(event: SkillEvent) {
        val now = System.currentTimeMillis()
        val current = _skillEvents.value.filter {
            (now - it.startTime) < (it.duration * 1000f).toLong()
        }.toMutableList()
        current.add(event)
        _skillEvents.value = current
    }

    /** 골드 획득 이벤트 (C6) */
    private val _goldPickupEvents = MutableStateFlow<List<GoldPickupEvent>>(emptyList())
    val goldPickupEvents: StateFlow<List<GoldPickupEvent>> = _goldPickupEvents.asStateFlow()

    fun onGoldPickup(x: Float, y: Float, amount: Int) {
        val event = GoldPickupEvent(x, y, amount)
        val current = _goldPickupEvents.value.toMutableList()
        current.add(event)
        val cutoff = System.currentTimeMillis() - 1500L
        _goldPickupEvents.value = current.filter { it.timestamp > cutoff }
    }

    /** 유닛 레벨업 이벤트 (C7) */
    private val _levelUpEvents = MutableStateFlow<List<LevelUpEvent>>(emptyList())
    val levelUpEvents: StateFlow<List<LevelUpEvent>> = _levelUpEvents.asStateFlow()

    fun onUnitLevelUp(x: Float, y: Float) {
        val event = LevelUpEvent(x, y)
        val current = _levelUpEvents.value.toMutableList()
        current.add(event)
        val cutoff = System.currentTimeMillis() - 1500L
        _levelUpEvents.value = current.filter { it.timestamp > cutoff }
    }

    fun clearExpiredSkillEvents() {
        val now = System.currentTimeMillis()
        _skillEvents.value = _skillEvents.value.filter {
            (now - it.startTime) < (it.duration * 1000f).toLong()
        }
    }

    /** 배속 (1f, 2f, 4f, 8f) */
    private val _battleSpeed = MutableStateFlow(1f)
    val battleSpeed: StateFlow<Float> = _battleSpeed.asStateFlow()

    fun cycleBattleSpeed() {
        _battleSpeed.value = when (_battleSpeed.value) {
            1f -> 2f
            2f -> 4f
            4f -> 8f
            else -> 1f
        }
    }

    fun setBattleSpeed(speed: Float) {
        _battleSpeed.value = speed.coerceIn(1f, 8f)
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
        _skillEvents.value = emptyList()
        _goldPickupEvents.value = emptyList()
        _levelUpEvents.value = emptyList()
        _stageId.value = 0
        _difficulty.value = 1
        _battleUpgradeLevels.value = IntArray(5) { 0 }
        _battleSpeed.value = 1f
        _debugMode.value = false
    }

    // Kotlin engine reference (replaces C++ JNI)
    var engine: com.example.jaygame.engine.BattleEngine? = null

    fun requestSummon() {
        engine?.requestSummon()
    }

    fun requestClickTile(tileIndex: Int) {
        engine?.requestClickTile(tileIndex)
    }

    fun requestMerge(tileIndex: Int) {
        dismissPopup()
        engine?.requestMerge(tileIndex)
    }

    fun requestSell(tileIndex: Int) {
        dismissPopup()
        engine?.requestSell(tileIndex)
    }

    fun requestUpgrade(tileIndex: Int) {
        engine?.requestUpgrade(tileIndex)
    }

    fun requestSwap(fromTile: Int, toTile: Int) {
        engine?.requestSwap(fromTile, toTile)
    }

    fun requestRelocate(tileIndex: Int, normX: Float, normY: Float) {
        engine?.requestRelocate(tileIndex, normX, normY)
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
     */
    fun performGamble(): GambleResult {
        val currentSp = _state.value.sp
        if (currentSp < GAMBLE_COST) return GambleResult(0f, currentSp, 0f)

        val spAfterCost = currentSp - GAMBLE_COST
        val percentage = Random.nextFloat() * 2f - 1f
        val spChange = spAfterCost * percentage
        val newSp = (spAfterCost + spChange).coerceAtLeast(0f)

        engine?.applyGamble(newSp)

        return GambleResult(spChange, newSp, percentage)
    }

    // ── Buy Unit ────────────────────────────────────────────

    fun requestBuyUnit(unitDefId: Int, cost: Int) {
        engine?.requestBuyUnit(unitDefId, cost.toFloat())
    }

    // ── Battle Upgrades ─────────────────────────────────────

    private val _battleUpgradeLevels = MutableStateFlow(IntArray(5) { 0 })
    val battleUpgradeLevels: StateFlow<IntArray> = _battleUpgradeLevels.asStateFlow()

    fun updateBattleUpgradeLevels(levels: IntArray) {
        _battleUpgradeLevels.value = levels.copyOf()
    }

    fun requestBattleUpgrade(upgradeType: Int, cost: Int) {
        val currentSp = _state.value.sp
        if (currentSp < cost) return

        val levels = _battleUpgradeLevels.value.copyOf()
        levels[upgradeType]++
        _battleUpgradeLevels.value = levels

        engine?.applyBattleUpgrade(upgradeType, levels[upgradeType], cost.toFloat())
    }
}
