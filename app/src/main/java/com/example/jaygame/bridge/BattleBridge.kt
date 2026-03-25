package com.example.jaygame.bridge

import com.example.jaygame.data.UnitFamily
import com.example.jaygame.data.UnitRace
import com.example.jaygame.engine.AttackRange
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.BossModifier
import com.example.jaygame.engine.DamageType
import com.example.jaygame.engine.Grid
import com.example.jaygame.engine.UnitCategory
import com.example.jaygame.engine.UnitRole
import com.example.jaygame.engine.UnitState
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

data class BattleState(
    val currentWave: Int = 0,
    val maxWaves: Int = 40,
    val playerHP: Int = 20,
    val maxHP: Int = 20,
    val sp: Float = 100f,
    val elapsedTime: Float = 0f,
    val state: Int = 0, // 0=WaveDelay, 1=Playing, 2=Victory, 3=Defeat
    val summonCost: Int = 50,
    val enemyCount: Int = 0,
    val maxEnemyCount: Int = 100,
    val isBossRound: Boolean = false,
    val waveTimeRemaining: Float = 0f,
    val waveElapsed: Float = 0f,
    val maxUnitSlots: Int = 18,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BattleState) return false
        return currentWave == other.currentWave && maxWaves == other.maxWaves &&
                playerHP == other.playerHP && maxHP == other.maxHP &&
                sp == other.sp && elapsedTime == other.elapsedTime &&
                state == other.state && summonCost == other.summonCost &&
                enemyCount == other.enemyCount && maxEnemyCount == other.maxEnemyCount &&
                isBossRound == other.isBossRound && waveTimeRemaining == other.waveTimeRemaining &&
                waveElapsed == other.waveElapsed && maxUnitSlots == other.maxUnitSlots
    }
    override fun hashCode(): Int = currentWave * 31 + playerHP + enemyCount * 7 + waveElapsed.toBits() + if (isBossRound) 1 else 0
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
const val BUFF_BIT_STUN       = 1 shl 6  // Stun (stars)
const val BUFF_BIT_SILENCE    = 1 shl 7  // Silence

// Unit buff bits (아군 유닛 버프 표시용)
const val UNIT_BUFF_ATK_UP   = 1 shl 0
const val UNIT_BUFF_SPD_UP   = 1 shl 1
const val UNIT_BUFF_SHIELD   = 1 shl 2
const val UNIT_BUFF_DEF_UP   = 1 shl 3

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
    val families: IntArray = IntArray(0),  // UnitFamily ordinal (0-5)
    val grades: IntArray = IntArray(0),    // UnitGrade ordinal (0-4)
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

data class MeleeHitEvent(
    val x: Float,       // normalized 0-1
    val y: Float,       // normalized 0-1
    val family: Int,     // 0-5 (FIRE..WIND)
    val isCrit: Boolean,
    val angle: Float,    // attack direction in radians
    val timestamp: Long = System.currentTimeMillis(),
)

data class UnitPositionData(
    val xs: FloatArray = FloatArray(0),
    val ys: FloatArray = FloatArray(0),
    @Deprecated("Use blueprintIds instead") val unitDefIds: IntArray = IntArray(0),
    val grades: IntArray = IntArray(0),
    val levels: IntArray = IntArray(0),
    val isAttacking: BooleanArray = BooleanArray(0),
    val attackAnimTimers: FloatArray = FloatArray(0),
    val tileIndices: IntArray = IntArray(0),
    val count: Int = 0,
    val frameId: Long = 0L,
    // Blueprint-system fields (Task 18)
    val blueprintIds: Array<String> = emptyArray(),
    val familiesList: Array<List<UnitFamily>> = emptyArray(),
    val roles: Array<UnitRole> = emptyArray(),
    val attackRanges: Array<AttackRange> = emptyArray(),
    val damageTypes: Array<DamageType> = emptyArray(),
    val unitCategories: Array<UnitCategory> = emptyArray(),
    val hps: FloatArray = FloatArray(0),
    val maxHps: FloatArray = FloatArray(0),
    val states: Array<UnitState> = emptyArray(),
    val homeXs: FloatArray = FloatArray(0),
    val homeYs: FloatArray = FloatArray(0),
    val stackCounts: IntArray = IntArray(0),
    val buffs: IntArray = IntArray(0),
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
    val relicDropId: Int = -1,      // -1 = no drop
    val relicDropGrade: Int = -1,   // RelicGrade.ordinal, -1 = no drop
)

/**
 * 그리드 타일 상태 (18 slots, 3×6 grid)
 */
data class GridTileState(
    @Deprecated("Use blueprintId instead") val unitDefId: Int = -1,    // -1 = empty
    val grade: Int = -1,        // 0=Common, 1=Rare, 2=Hero, 3=Legend, 4=Mythic
    @Deprecated("Use families instead") val family: Int = -1,       // 0=Fire, 1=Frost, 2=Poison, 3=Lightning, 4=Support, 5=Wind
    val canMerge: Boolean = false,
    val level: Int = 0,
    // Blueprint-system fields (Task 18)
    val blueprintId: String = "",
    val families: List<UnitFamily> = emptyList(),
    val role: UnitRole = UnitRole.RANGED_DPS,
)

/**
 * 스킬 VFX 타입 — 각 고유 스킬에 대응하는 시각 효과 종류.
 */
enum class SkillVfxType {
    // Fire family (Hero passive, Legend active, Mythic)
    LINGERING_FLAME, FIRESTORM_METEOR, VOLCANIC_ERUPTION,
    // Frost family (Hero passive, Legend active, Mythic)
    FROST_NOVA, ABSOLUTE_ZERO, ICE_AGE_BLIZZARD,
    // Poison family (Hero passive, Legend active, Mythic)
    POISON_CLOUD, ACID_SPRAY, TOXIC_DOMAIN,
    // Lightning family (Hero passive, Legend active, Mythic)
    LIGHTNING_STRIKE, STATIC_FIELD, THUNDERSTORM,
    // Support family (Hero passive, Legend active, Mythic)
    HEAL_PULSE, WAR_SONG_AURA, DIVINE_SHIELD,
    // Wind family (Hero passive, Legend active, Mythic)
    CYCLONE_PULL, EYE_OF_STORM, VACUUM_SLASH,
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
    val abilityId: String = "", // 궁극기 스프라이트 매핑용
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
    private val enemyFrameCounter = AtomicLong(0)
    private val projFrameCounter = AtomicLong(0)
    private val unitFrameCounter = AtomicLong(0)

    // Pre-allocated event buffers to avoid per-event list allocations
    private val damageBuffer = ArrayDeque<DamageEvent>(32)
    private val meleeHitBuffer = ArrayDeque<MeleeHitEvent>(16)
    private val skillBuffer = ArrayDeque<SkillEvent>(16)
    private val goldPickupBuffer = ArrayDeque<GoldPickupEvent>(16)
    private val levelUpBuffer = ArrayDeque<LevelUpEvent>(16)

    // Lock objects for thread-safe buffer access (game thread writes, UI thread reads)
    private val skillLock = Any()
    private val damageLock = Any()
    private val meleeHitLock = Any()
    private val goldLock = Any()
    private val levelUpLock = Any()

    // ── Thread-safe command queue — UI thread enqueues, game loop dequeues ──
    private val commandQueue = java.util.concurrent.ConcurrentLinkedQueue<BattleCommand>()

    sealed class BattleCommand {
        object Summon : BattleCommand()
        data class Merge(val tileIndex: Int) : BattleCommand()
        data class Sell(val tileIndex: Int) : BattleCommand()
        data class BulkSell(val grade: Int, val result: CompletableDeferred<Int>) : BattleCommand()
        data class GroupUpgrade(val groupIndex: Int) : BattleCommand()
        data class Swap(val from: Int, val to: Int) : BattleCommand()
        data class Gamble(
            val option: com.example.jaygame.engine.GambleSystem.GambleOption,
            val betSize: com.example.jaygame.engine.GambleSystem.BetSize,
            val result: CompletableDeferred<com.example.jaygame.engine.GambleSystem.GambleResult?>,
        ) : BattleCommand()
        object MergeAll : BattleCommand()
        object RecipeCraft : BattleCommand()
        data class BuyUnit(val unitDefId: Int, val cost: Int) : BattleCommand()
        data class BuyBlueprint(val blueprintId: String, val cost: Int) : BattleCommand()
        data class BattleUpgrade(val upgradeType: Int, val level: Int, val cost: Float) : BattleCommand()
    }

    /** Drain all pending commands — called by the game loop on its own thread. */
    fun drainCommands(): List<BattleCommand> {
        val commands = mutableListOf<BattleCommand>()
        while (true) {
            val cmd = commandQueue.poll() ?: break
            commands.add(cmd)
        }
        return commands
    }

    /** Z16: Debug overlay toggle */
    private val _debugMode = MutableStateFlow(false)
    val debugMode: StateFlow<Boolean> = _debugMode.asStateFlow()

    fun toggleDebugMode() {
        _debugMode.value = !_debugMode.value
    }

    /** Gameplay settings — set from GameData before battle starts */
    private val _showDamageNumbers = MutableStateFlow(true)
    val showDamageNumbers: StateFlow<Boolean> = _showDamageNumbers.asStateFlow()

    private val _healthBarMode = MutableStateFlow(0) // 0=항상, 1=피격 시만, 2=숨김
    val healthBarMode: StateFlow<Int> = _healthBarMode.asStateFlow()

    fun applyGameplaySettings(showDamage: Boolean, hpBarMode: Int) {
        _showDamageNumbers.value = showDamage
        _healthBarMode.value = hpBarMode
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

    private val _meleeHitEvents = MutableStateFlow<List<MeleeHitEvent>>(emptyList())
    val meleeHitEvents: StateFlow<List<MeleeHitEvent>> = _meleeHitEvents.asStateFlow()

    /** 유닛 슬롯 상태 (18 slots, 3×6 grid) */
    const val GRID_TOTAL = 18

    private val _gridState = MutableStateFlow(List(GRID_TOTAL) { GridTileState() })
    val gridState: StateFlow<List<GridTileState>> = _gridState.asStateFlow()

    /** Whether any merge is possible on the grid */
    val canMergeAny: Boolean
        get() = _gridState.value.any { it.canMerge }

    /** 현재 스테이지 ID */
    private val _stageId = MutableStateFlow(0)
    val stageId: StateFlow<Int> = _stageId.asStateFlow()

    fun setStageId(id: Int) { _stageId.value = id }

    /** 현재 난이도 (0=일반, 1=하드, 2=헬) */
    private val _difficulty = MutableStateFlow(0)
    val difficulty: StateFlow<Int> = _difficulty.asStateFlow()

    fun setDifficulty(diff: Int) { _difficulty.value = diff }

    /** 던전 모드 */
    private val _dungeonId = MutableStateFlow(-1) // -1 = normal battle
    val dungeonId: StateFlow<Int> = _dungeonId.asStateFlow()

    fun setDungeonMode(dungeonId: Int) { _dungeonId.value = dungeonId }
    fun clearDungeonMode() { _dungeonId.value = -1 }
    val isDungeonMode: Boolean get() = _dungeonId.value >= 0

    /** 선택된 타일 인덱스 (-1 = 없음) */
    private val _selectedTile = MutableStateFlow(-1)
    val selectedTile: StateFlow<Int> = _selectedTile.asStateFlow()

    // ── Move mode: tap-to-select, tap-to-move 시스템 ──
    private val EMPTY_BOOL_ARRAY = BooleanArray(0)

    private val _moveModeTile = MutableStateFlow(-1)  // -1 = not in move mode
    val moveModeTile: StateFlow<Int> = _moveModeTile.asStateFlow()

    private val _validMoveTargets = MutableStateFlow(EMPTY_BOOL_ARRAY)
    val validMoveTargets: StateFlow<BooleanArray> = _validMoveTargets.asStateFlow()

    fun enterMoveMode(tileIndex: Int) {
        val gs = _gridState.value
        val sourceTile = gs.getOrNull(tileIndex) ?: return
        if (sourceTile.blueprintId.isEmpty() && sourceTile.grade == -1) return
        _moveModeTile.value = tileIndex
        _selectedTile.value = tileIndex
        // 자기 자신만 제외, 나머지 모든 슬롯으로 이동/스왑 가능
        _validMoveTargets.value = BooleanArray(gs.size) { it != tileIndex }
    }

    fun exitMoveMode() {
        _moveModeTile.value = -1
        _validMoveTargets.value = EMPTY_BOOL_ARRAY
        _selectedTile.value = -1
    }

    fun executeMoveMode(targetTile: Int) {
        val fromTile = _moveModeTile.value
        if (fromTile < 0) return
        commandQueue.add(BattleCommand.Swap(fromTile, targetTile))
        exitMoveMode()
    }

    /** 유닛 상세 팝업 데이터 (롱프레스 시 표시) */
    data class UnitPopupData(
        val tileIndex: Int,
        val grade: Int,
        val blueprintId: String = "",
        val families: List<UnitFamily> = emptyList(),
        val role: UnitRole = UnitRole.RANGED_DPS,
        val attackRange: AttackRange = AttackRange.RANGED,
        val damageType: DamageType = DamageType.PHYSICAL,
    )
    private val _unitPopup = MutableStateFlow<UnitPopupData?>(null)
    val unitPopup: StateFlow<UnitPopupData?> = _unitPopup.asStateFlow()

    fun showUnitPopup(tileIndex: Int) {
        val gs = _gridState.value
        val tile = gs.getOrNull(tileIndex) ?: return
        if (tile.blueprintId.isEmpty() && tile.grade == -1) return
        val bp = if (tile.blueprintId.isNotEmpty() && com.example.jaygame.engine.BlueprintRegistry.isReady)
            com.example.jaygame.engine.BlueprintRegistry.instance.findById(tile.blueprintId) else null
        _unitPopup.value = UnitPopupData(
            tileIndex = tileIndex,
            grade = tile.grade,
            blueprintId = tile.blueprintId,
            families = tile.families,
            role = bp?.role ?: tile.role,
            attackRange = bp?.attackRange ?: AttackRange.RANGED,
            damageType = bp?.damageType ?: DamageType.PHYSICAL,
        )
    }

    fun dismissPopup() {
        _unitPopup.value = null
    }

    /** 소환 결과 데이터 */
    data class SummonResult(
        @Deprecated("Use blueprintId instead") val unitDefId: Int,
        val grade: Int,
        val blueprintId: String = "",
    )

    private val _summonResult = MutableStateFlow<SummonResult?>(null)
    val summonResult: StateFlow<SummonResult?> = _summonResult.asStateFlow()

    /** 머지 이펙트 (타일 인덱스, lucky 여부, 결과 유닛 ID) */
    data class MergeEffect(val tileIndex: Int, val isLucky: Boolean, val resultUnitId: Int = -1, val resultBlueprintId: String = "")
    private val _mergeEffect = MutableStateFlow<MergeEffect?>(null)
    val mergeEffect: StateFlow<MergeEffect?> = _mergeEffect.asStateFlow()

    @JvmStatic
    fun updateState(
        wave: Int, maxWaves: Int,
        hp: Int, maxHp: Int,
        sp: Float, elapsed: Float,
        state: Int, summonCost: Int,
        enemyCount: Int, isBossRound: Int, waveTimeRemaining: Float,
        waveElapsed: Float = 0f,
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
            enemyCount = enemyCount,
            isBossRound = isBossRound != 0,
            waveTimeRemaining = waveTimeRemaining,
            waveElapsed = waveElapsed,
            maxUnitSlots = engine?.maxUnitSlots ?: 50,
        )

        // Clear visual effects on wave end
        if (state == 0 || state == 2 || state == 3) {
            _damageEvents.value = emptyList()
        }
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
        relicDropId: Int = -1,
        relicDropGrade: Int = -1,
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
            relicDropId = relicDropId,
            relicDropGrade = relicDropGrade,
        )
    }

    @JvmStatic
    fun updateEnemyPositions(xs: FloatArray, ys: FloatArray, types: IntArray, hpRatios: FloatArray, buffs: IntArray, count: Int) {
        _enemyPositions.value = EnemyPositionData(xs, ys, types, hpRatios, buffs, count, enemyFrameCounter.incrementAndGet())
    }

    @JvmStatic
    fun updateProjectiles(
        srcXs: FloatArray, srcYs: FloatArray,
        dstXs: FloatArray, dstYs: FloatArray,
        types: IntArray, count: Int,
        families: IntArray = IntArray(0),
        grades: IntArray = IntArray(0),
    ) {
        _projectiles.value = ProjectileData(srcXs, srcYs, dstXs, dstYs, types, families, grades, count, projFrameCounter.incrementAndGet())
    }

    @JvmStatic
    fun updateUnitPositions(
        xs: FloatArray, ys: FloatArray, unitDefIds: IntArray,
        grades: IntArray, levels: IntArray, isAttacking: BooleanArray,
        tileIndices: IntArray, count: Int, attackAnimTimers: FloatArray = FloatArray(0),
        blueprintIds: Array<String> = emptyArray(),
        familiesList: Array<List<UnitFamily>> = emptyArray(),
        roles: Array<UnitRole> = emptyArray(),
        attackRanges: Array<AttackRange> = emptyArray(),
        damageTypes: Array<DamageType> = emptyArray(),
        unitCategories: Array<UnitCategory> = emptyArray(),
        hps: FloatArray = FloatArray(0),
        maxHps: FloatArray = FloatArray(0),
        states: Array<UnitState> = emptyArray(),
        homeXs: FloatArray = FloatArray(0),
        homeYs: FloatArray = FloatArray(0),
        stackCounts: IntArray = IntArray(0),
        buffs: IntArray = IntArray(0),
    ) {
        _unitPositions.value = UnitPositionData(
            xs, ys, unitDefIds, grades, levels, isAttacking, attackAnimTimers, tileIndices, count, unitFrameCounter.incrementAndGet(),
            blueprintIds, familiesList, roles, attackRanges, damageTypes, unitCategories,
            hps, maxHps, states, homeXs, homeYs, stackCounts, buffs,
        )
    }

    // PERF: Throttle damage event list emission to avoid per-hit List allocation
    private var lastDamageEmitTime = 0L
    private const val DAMAGE_EMIT_INTERVAL_MS = 33L // ~30 FPS for damage numbers

    @JvmStatic
    fun onDamageDealt(x: Float, y: Float, damage: Int, isCrit: Boolean) {
        synchronized(damageLock) {
            val now = System.currentTimeMillis()
            val cutoff = now - 800L
            while (damageBuffer.isNotEmpty()) {
                val first = damageBuffer.firstOrNull() ?: break
                if (first.timestamp <= cutoff) damageBuffer.removeFirst() else break
            }
            damageBuffer.addLast(DamageEvent(x, y, damage, isCrit))
            // Only emit a new list at throttled rate (crits always emit immediately)
            if (isCrit || now - lastDamageEmitTime >= DAMAGE_EMIT_INTERVAL_MS) {
                lastDamageEmitTime = now
                _damageEvents.value = damageBuffer.toList()
            }
        }
    }

    @JvmStatic
    fun onMeleeHit(x: Float, y: Float, family: Int, isCrit: Boolean, angle: Float) {
        synchronized(meleeHitLock) {
            val now = System.currentTimeMillis()
            val cutoff = now - 400L
            while (meleeHitBuffer.isNotEmpty()) {
                if ((meleeHitBuffer.firstOrNull()?.timestamp ?: now) <= cutoff) meleeHitBuffer.removeFirst() else break
            }
            meleeHitBuffer.addLast(MeleeHitEvent(x, y, family, isCrit, angle))
            _meleeHitEvents.value = meleeHitBuffer.toList()
        }
    }

    @JvmStatic
    fun updateGridState(
        unitIds: IntArray, grades: IntArray, families: IntArray,
        canMerge: BooleanArray, levels: IntArray,
        blueprintIds: Array<String> = emptyArray(),
        familiesList: Array<List<UnitFamily>> = emptyArray(),
        roles: Array<UnitRole> = emptyArray(),
    ) {
        val count = unitIds.size.coerceAtMost(GRID_TOTAL)
        val tiles = List(count) { i ->
            GridTileState(
                unitDefId = unitIds[i],
                grade = grades[i],
                family = families[i],
                canMerge = canMerge[i],
                level = levels[i],
                blueprintId = blueprintIds.getOrElse(i) { "" },
                families = familiesList.getOrElse(i) { emptyList() },
                role = roles.getOrElse(i) { UnitRole.RANGED_DPS },
            )
        }
        _gridState.value = tiles
    }

    @JvmStatic
    fun onSummonResult(unitDefId: Int, grade: Int, blueprintId: String = "") {
        _summonResult.value = SummonResult(unitDefId, grade, blueprintId)
    }

    @JvmStatic
    fun onMergeComplete(tileIndex: Int, isLucky: Boolean, resultUnitId: Int, resultBlueprintId: String = "") {
        _mergeEffect.value = MergeEffect(tileIndex, isLucky, resultUnitId, resultBlueprintId)
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

    private var lastSkillEmitTime = 0L
    private const val MAX_SKILL_EVENTS = 15
    private const val SKILL_EMIT_THROTTLE_MS = 33L // ~30 FPS for VFX updates

    fun emitSkillEvent(event: SkillEvent) {
        synchronized(skillLock) {
            val now = System.currentTimeMillis()
            // Purge expired
            while (skillBuffer.isNotEmpty()) {
                val first = skillBuffer.firstOrNull() ?: break
                if ((now - first.startTime) >= (first.duration * 1000f).toLong()) {
                    skillBuffer.removeFirst()
                } else break
            }
            // Drop oldest if at capacity
            while (skillBuffer.size >= MAX_SKILL_EVENTS) {
                skillBuffer.removeFirst()
            }
            skillBuffer.addLast(event)
            // Throttle StateFlow emission to avoid excessive recomposition
            if (now - lastSkillEmitTime >= SKILL_EMIT_THROTTLE_MS) {
                lastSkillEmitTime = now
                _skillEvents.value = skillBuffer.toList()
            }
        }
    }

    /** 골드 획득 이벤트 (C6) */
    private val _goldPickupEvents = MutableStateFlow<List<GoldPickupEvent>>(emptyList())
    val goldPickupEvents: StateFlow<List<GoldPickupEvent>> = _goldPickupEvents.asStateFlow()

    // PERF: Throttle gold pickup event list emission
    private var lastGoldEmitTime = 0L

    fun onGoldPickup(x: Float, y: Float, amount: Int) {
        synchronized(goldLock) {
            val now = System.currentTimeMillis()
            val cutoff = now - 1500L
            while (goldPickupBuffer.isNotEmpty()) {
                val first = goldPickupBuffer.firstOrNull() ?: break
                if (first.timestamp <= cutoff) goldPickupBuffer.removeFirst() else break
            }
            goldPickupBuffer.addLast(GoldPickupEvent(x, y, amount))
            if (now - lastGoldEmitTime >= 50L) {
                lastGoldEmitTime = now
                _goldPickupEvents.value = goldPickupBuffer.toList()
            }
        }
    }

    /** 유닛 레벨업 이벤트 (C7) */
    private val _levelUpEvents = MutableStateFlow<List<LevelUpEvent>>(emptyList())
    val levelUpEvents: StateFlow<List<LevelUpEvent>> = _levelUpEvents.asStateFlow()

    fun onUnitLevelUp(x: Float, y: Float) {
        synchronized(levelUpLock) {
            val cutoff = System.currentTimeMillis() - 1500L
            while (levelUpBuffer.isNotEmpty()) {
                val first = levelUpBuffer.firstOrNull() ?: break
                if (first.timestamp <= cutoff) levelUpBuffer.removeFirst() else break
            }
            levelUpBuffer.addLast(LevelUpEvent(x, y))
            _levelUpEvents.value = levelUpBuffer.toList()
        }
    }

    fun clearExpiredSkillEvents() {
        synchronized(skillLock) {
            val now = System.currentTimeMillis()
            var removed = false
            while (skillBuffer.isNotEmpty()) {
                val first = skillBuffer.firstOrNull() ?: break
                if ((now - first.startTime) >= (first.duration * 1000f).toLong()) {
                    skillBuffer.removeFirst()
                    removed = true
                } else {
                    break
                }
            }
            if (removed) _skillEvents.value = skillBuffer.toList()
        }
    }

    /** 종족 드래프트 선택 */
    private val _selectedRaces = MutableStateFlow<Set<UnitRace>>(emptySet())
    val selectedRaces: StateFlow<Set<UnitRace>> = _selectedRaces.asStateFlow()

    fun setSelectedRaces(races: Set<UnitRace>) {
        _selectedRaces.value = races
    }

    /** 유닛 소환 천장(Pity) 카운터 */
    private val _unitPullPity = MutableStateFlow(0)
    val unitPullPity: StateFlow<Int> = _unitPullPity.asStateFlow()

    fun updateUnitPullPity(pity: Int) {
        _unitPullPity.value = pity
    }

    /** 보스 모디파이어 — 보스 웨이브 시작 시 설정, 3초 후 null로 자동 리셋은 UI에서 처리 */
    private val _bossModifier = MutableStateFlow<BossModifier?>(null)
    val bossModifier: StateFlow<BossModifier?> = _bossModifier.asStateFlow()

    fun notifyBossModifier(modifier: BossModifier?) {
        _bossModifier.value = modifier
    }

    /** 봉황 펫: 1회 부활 이벤트 */
    private val _phoenixReviveEvent = MutableStateFlow(0)
    val phoenixReviveEvent: StateFlow<Int> = _phoenixReviveEvent.asStateFlow()

    fun onPhoenixRevive() {
        _phoenixReviveEvent.value = _phoenixReviveEvent.value + 1
    }

    /** Zone effects (persistent ground AoE areas) */
    data class ZoneData(
        val xs: FloatArray = FloatArray(0),  // normalized 0-1
        val ys: FloatArray = FloatArray(0),
        val radii: FloatArray = FloatArray(0), // normalized
        val families: IntArray = IntArray(0),
        val progresses: FloatArray = FloatArray(0), // duration/maxDuration (1.0 → 0.0)
        val grades: IntArray = IntArray(0),
        val count: Int = 0,
        val frameId: Long = 0L,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ZoneData) return false
            return frameId == other.frameId
        }
        override fun hashCode(): Int = frameId.hashCode()
    }

    private val zoneFrameCounter = AtomicLong(0)
    private val _zoneData = MutableStateFlow(ZoneData())
    val zoneData: StateFlow<ZoneData> = _zoneData.asStateFlow()

    fun updateZoneData(
        xs: FloatArray, ys: FloatArray, radii: FloatArray,
        families: IntArray, progresses: FloatArray, grades: IntArray, count: Int,
    ) {
        _zoneData.value = ZoneData(xs, ys, radii, families, progresses, grades, count, zoneFrameCounter.incrementAndGet())
    }

    /** 튜토리얼 모드 (첫 전투) */
    private val _tutorialMode = MutableStateFlow(false)
    val tutorialMode: StateFlow<Boolean> = _tutorialMode.asStateFlow()

    fun setTutorialMode(enabled: Boolean) { _tutorialMode.value = enabled }

    /** 배속 (2f=x1, 4f=x2, 8f=x4) — 내부 기본 2f */
    private val _battleSpeed = MutableStateFlow(2f)
    val battleSpeed: StateFlow<Float> = _battleSpeed.asStateFlow()

    fun cycleBattleSpeed() {
        _battleSpeed.value = when (_battleSpeed.value) {
            2f -> 4f
            4f -> 8f
            else -> 2f
        }
    }

    fun setBattleSpeed(speed: Float) {
        _battleSpeed.value = speed.coerceIn(0f, 8f) // 0 = paused
    }

    /** 라이프사이클 자동 일시정지 — onPause 시 속도 저장 후 0f, onResume 시 복원 */
    @Volatile
    @JvmStatic
    private var speedBeforeLifecyclePause = 0f

    @Volatile
    @JvmStatic
    private var pausedByLifecycle = false

    fun pauseByLifecycle() {
        if (!pausedByLifecycle) {
            speedBeforeLifecyclePause = _battleSpeed.value
            _battleSpeed.value = 0f
            pausedByLifecycle = true
        }
    }

    fun resumeFromLifecycle() {
        if (pausedByLifecycle) {
            pausedByLifecycle = false
            _battleSpeed.value = speedBeforeLifecyclePause
        }
    }

    /** 새 배틀 시작 시 상태 초기화 */
    fun reset() {
        _state.value = BattleState()
        _result.value = null
        enemyFrameCounter.set(0)
        _enemyPositions.value = EnemyPositionData()
        projFrameCounter.set(0)
        _projectiles.value = ProjectileData()
        unitFrameCounter.set(0)
        _unitPositions.value = UnitPositionData()
        synchronized(damageLock) {
            damageBuffer.clear()
            _damageEvents.value = emptyList()
        }
        synchronized(meleeHitLock) {
            meleeHitBuffer.clear()
            _meleeHitEvents.value = emptyList()
        }
        _gridState.value = List(GRID_TOTAL) { GridTileState() }
        _selectedTile.value = -1
        _moveModeTile.value = -1
        _validMoveTargets.value = EMPTY_BOOL_ARRAY
        _unitPopup.value = null
        _mergeEffect.value = null
        _summonResult.value = null
        synchronized(skillLock) {
            skillBuffer.clear()
            _skillEvents.value = emptyList()
        }
        synchronized(goldLock) {
            goldPickupBuffer.clear()
            _goldPickupEvents.value = emptyList()
        }
        synchronized(levelUpLock) {
            levelUpBuffer.clear()
            _levelUpEvents.value = emptyList()
        }
        _bossModifier.value = null
        _unitPullPity.value = 0
        zoneFrameCounter.set(0)
        _zoneData.value = ZoneData()
        // Note: stageId, difficulty, battleSpeed, dungeonId are preserved — set by ComposeActivity before launch
        _battleUpgradeLevels.value = IntArray(5) { 0 }
        _groupUpgradeLevels.value = IntArray(com.example.jaygame.engine.UnitUpgradeSystem.GROUP_COUNT) { 0 }
        _debugMode.value = false
        commandQueue.clear()
    }

    @Volatile
    @JvmStatic
    var engine: com.example.jaygame.engine.BattleEngine? = null

    // ── Request methods: enqueue commands for game-loop-thread processing ──

    fun requestSummon() {
        commandQueue.add(BattleCommand.Summon)
    }

    fun requestMerge(tileIndex: Int) {
        _selectedTile.value = -1
        commandQueue.add(BattleCommand.Merge(tileIndex))
    }

    /** 모든 합성 가능 슬롯에서 한 번씩 합성 실행 */
    fun requestMergeAll() {
        commandQueue.add(BattleCommand.MergeAll)
    }

    fun requestSell(tileIndex: Int) {
        _selectedTile.value = -1
        commandQueue.add(BattleCommand.Sell(tileIndex))
    }

    fun requestBulkSell(grade: Int): Int {
        val deferred = CompletableDeferred<Int>()
        commandQueue.add(BattleCommand.BulkSell(grade, deferred))
        return runBlocking { withTimeoutOrNull(100L) { deferred.await() } ?: 0 }
    }

    fun requestRecipeCraft() {
        commandQueue.add(BattleCommand.RecipeCraft)
    }

    fun requestGroupUpgrade(groupIndex: Int) {
        commandQueue.add(BattleCommand.GroupUpgrade(groupIndex))
    }

    fun requestSwap(fromTile: Int, toTile: Int) {
        commandQueue.add(BattleCommand.Swap(fromTile, toTile))
    }

    // ── Gamble ──────────────────────────────────────────────

    /**
     * Casino-style gamble: player bets a % of current SP.
     * Win → bet × multiplier reward. Lose → lose bet.
     */
    fun requestGamble(
        option: com.example.jaygame.engine.GambleSystem.GambleOption,
        betSize: com.example.jaygame.engine.GambleSystem.BetSize = com.example.jaygame.engine.GambleSystem.BetSize.SMALL,
    ): com.example.jaygame.engine.GambleSystem.GambleResult? {
        val deferred = CompletableDeferred<com.example.jaygame.engine.GambleSystem.GambleResult?>()
        commandQueue.add(BattleCommand.Gamble(option, betSize, deferred))
        return runBlocking { withTimeoutOrNull(100L) { deferred.await() } }
    }

    // ── Buy Unit ────────────────────────────────────────────

    fun requestBuyUnit(unitDefId: Int, cost: Int) {
        commandQueue.add(BattleCommand.BuyUnit(unitDefId, cost))
    }

    fun requestBuyBlueprint(blueprintId: String, cost: Int) {
        commandQueue.add(BattleCommand.BuyBlueprint(blueprintId, cost))
    }

    // ── Battle Upgrades (글로벌 버프) ─────────────────────────────────────

    private val _battleUpgradeLevels = MutableStateFlow(IntArray(5) { 0 })
    val battleUpgradeLevels: StateFlow<IntArray> = _battleUpgradeLevels.asStateFlow()

    fun updateBattleUpgradeLevels(levels: IntArray) {
        _battleUpgradeLevels.value = levels.copyOf()
    }

    fun requestBattleUpgrade(upgradeType: Int, cost: Int) {
        val currentSp = _state.value.sp
        if (currentSp < cost) return
        if (upgradeType !in _battleUpgradeLevels.value.indices) return

        val levels = _battleUpgradeLevels.value.copyOf()
        levels[upgradeType]++
        _battleUpgradeLevels.value = levels

        commandQueue.add(BattleCommand.BattleUpgrade(upgradeType, levels[upgradeType], cost.toFloat()))
    }

    // ── 등급 그룹 통합 강화 ─────────────────────────────────────

    private val _groupUpgradeLevels = MutableStateFlow(IntArray(com.example.jaygame.engine.UnitUpgradeSystem.GROUP_COUNT) { 0 })
    val groupUpgradeLevels: StateFlow<IntArray> = _groupUpgradeLevels.asStateFlow()

    fun updateGroupUpgradeLevels(levels: IntArray) {
        _groupUpgradeLevels.value = levels.copyOf()
    }

}
