package com.jay.jaygame.engine

import android.util.Log
import com.jay.jaygame.audio.SfxManager
import com.jay.jaygame.audio.SoundEvent
import com.jay.jaygame.bridge.BattleBridge
import com.jay.jaygame.data.DungeonDef
import com.jay.jaygame.data.UnitRace
import com.jay.jaygame.data.GameData
import com.jay.jaygame.engine.behavior.BehaviorFactory
import com.jay.jaygame.engine.behavior.BehaviorRegistration
import com.jay.jaygame.engine.math.Vec2
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

class BattleEngine(
    private val stageId: Int,
    private val difficulty: Int,
    private val maxWaves: Int,
    gameData: GameData? = null,
    initialPity: Int = 0,
) {
    // region Constants & Companion
    companion object {
        const val W = 720f
        const val H = 1280f
        const val FIXED_DT = 1f / 60f
        const val MAX_ENEMIES = 256
        const val MAX_UNITS = 128
        const val MAX_PROJECTILES = 512
        const val DEFEAT_ENEMY_COUNT = 100
        const val COIN_PER_KILL = 2
        const val COIN_PER_ELITE_KILL = 6
        const val COIN_PER_BOSS_BASE = 60f
        const val COIN_PER_BOSS_PER_WAVE = 2f
        const val BASE_SUMMON_COST = 10
        const val SUMMON_COST_INCREMENT = 5
        const val MAX_SUMMON_COST = 60
        const val WAVE_CLEAR_BASE = 15f
        const val WAVE_CLEAR_PER_WAVE = 2.5f
        const val SELL_BASE = 8f
        const val SELL_PER_GRADE = 8f
        const val MAX_ZONE_RENDER = 32
        const val WAVE_DELAY = 3f

        // Elite enemy multipliers (relative to base wave config)
        const val ELITE_HP_MULT = 2f
        const val ELITE_SPEED_MULT = 1.1f
        const val ELITE_ARMOR_MULT = 1.5f
        const val ELITE_MAGIC_RESIST_MULT = 1.3f
        const val ELITE_CC_RESIST_BONUS = 0.1f

        // Boss modifier thresholds
        const val BERSERKER_HP_THRESHOLD = 0.5f
        const val SPLITTER_HP_THRESHOLD = 0.5f
        const val COMMANDER_SHIELD_RATIO = 0.02f
        const val REGEN_HEAL_RATIO = 0.05f
        const val REGEN_INTERVAL = 10f
        const val BERSERKER_SPEED_MULT = 1.5f
        const val SWIFT_SPEED_MULT = 2f
        const val SHIELDED_ACTIVE_DURATION = 3f
        const val SHIELDED_COOLDOWN_DURATION = 5f
    }

    // endregion

    // region Core State
    enum class State { WaveDelay, Playing, Victory, Defeat }
    var state = State.WaveDelay; private set

    val maxUnitSlots: Int = Grid.SLOT_COUNT  // 18 slots (3×6 grid)
    private val diffCoinMult: Float = when (difficulty) { 1 -> 1.5f; 2 -> 2.5f; else -> 1f }

    var relicManager: RelicManager? = gameData?.let { RelicManager(it) }
    val petSystem = PetBattleSystem().also { ps -> gameData?.let { ps.init(it) } }

    // 던전 모드
    var isDungeonMode: Boolean = false
    var dungeonDef: DungeonDef? = null

    // 가족 영구 강화 레벨
    private val familyUpgradeLevels: Map<Int, Int> = gameData?.let { data ->
        com.jay.jaygame.data.UnitFamily.entries.associate { family ->
            family.ordinal to (data.familyUpgrades[family.name] ?: 0)
        }
    } ?: emptyMap()

    // 유닛별 영구 레벨 (카드 레벨업) — keyed by blueprintId
    private val permanentUnitLevels: Map<String, Int> = gameData?.let { data ->
        data.units.mapValues { (_, progress) -> progress.level }
    } ?: emptyMap()

    // 천장(Pity) 시스템
    private val probabilityEngine = DefaultProbabilityEngine()
    var currentPity: Int = initialPity.coerceIn(0, 100); private set

    // 조합석 — 배틀 중 구매/소모, 배틀 종료 시 리셋
    var luckyStones: Int = 0; internal set
    private var lastPushedLuckyStones: Int = -1

    val economy = BattleEconomy(this)
    val mergeHandler = BattleMergeHandler(this)

    val enemies = ObjectPool(MAX_ENEMIES, { Enemy() }) { it.reset() }
    val units = ObjectPool(MAX_UNITS, { GameUnit() }) { it.reset() }
    val projectiles = ObjectPool(MAX_PROJECTILES, { Projectile() }) { it.reset() }
    val zones = ObjectPool(32, { ZoneEffect() }) { it.reset() }

    val grid = Grid()
    var waveSystem = WaveSystem(maxWaves, difficulty); private set
    val spatialHash = SpatialHash<Enemy>(64f)
    private val auraTicks = FloatArray(MAX_UNITS)

    /** 배틀 코인 — 적 처치/웨이브 클리어로 획득, 소환/강화에 사용 */
    var sp = 50f; internal set
    var summonCost = BASE_SUMMON_COST; private set
    private var summonCount = 0  // 소환 횟수 (비용 증가 추적)
    var elapsedTime = 0f; private set

    private var waveDelayTimer = WAVE_DELAY
    private var isBossRound = false

    private var killCount = 0
    internal var mergeCount = 0
    private var lastAttackSfxTime = 0L
    private var lastDeathSfxTime = 0L

    private fun tryPlayAttackSfx(isCrit: Boolean, attackRange: AttackRange = AttackRange.MELEE, damageType: DamageType = DamageType.PHYSICAL) {
        val now = System.currentTimeMillis()
        if (now - lastAttackSfxTime > 200) {
            if (isCrit) {
                SfxManager.play(SoundEvent.CriticalHit, 0.5f)
            } else {
                val event = when {
                    attackRange == AttackRange.MELEE -> SoundEvent.AttackMelee
                    damageType == DamageType.MAGIC   -> SoundEvent.AttackMagic
                    else                             -> SoundEvent.AttackRanged
                }
                SfxManager.play(event, 0.4f)
            }
            lastAttackSfxTime = now
        }
    }
    private var peakEnemyCount = 0
    private var hpEverLost = false // Track if player ever took HP damage

    private val upgradeLevels = IntArray(5)
    private var upgradeAtkMult = 1f
    private var upgradeSpdMult = 1f
    private var upgradeCritRate = 0f
    private var upgradeRangeMult = 1f
    private var upgradeSpRegen = 0f

    // ── 등급 그룹 통합 강화 (3그룹) ──
    private val groupUpgradeLevels = IntArray(UnitUpgradeSystem.GROUP_COUNT)
    private val groupAtkBonusCache = FloatArray(UnitUpgradeSystem.GROUP_COUNT)
    private val groupSpdBonusCache = FloatArray(UnitUpgradeSystem.GROUP_COUNT)

    // ── 종족 드래프트 시너지 버프 ──
    private var synergyAtkMult = 1f       // HUMAN: +10% ATK
    private var synergySpdMult = 1f       // SPIRIT: +10% 공격속도
    private var synergyCoinMult = 1f      // ANIMAL: +20% 코인 획득
    private var synergyLuckyMerge = 0f    // ROBOT: 합성 확률 보너스
    private var synergyBossDmgMult = 1f   // DEMON: +15% 보스 데미지

    // ── 로그라이크 강화 멀티플라이어 ──
    internal var roguelikeAtkMult = 1f
    internal var roguelikeSpdMult = 1f
    internal var roguelikeRangeMult = 1f
    internal var roguelikeCritBonus = 0f
    internal var roguelikeCoinMult = 1f
    internal var roguelikeSellBonus = 0f
    internal var roguelikeSummonDiscount = 0f
    internal var roguelikeCCDuration = 0f
    internal var roguelikeDotBoost = 0f
    internal var roguelikeBossBonus = 0f
    internal var roguelikeManaBonus = 0f
    internal var roguelikeLuckyBonus = 0f
    internal var roguelikeBerserkerBase = 0f
    internal var roguelikeVampiricChance = 0f
    internal var roguelikeArmorShred = false
    internal var roguelikeSplash = false
    internal var roguelikeSlowOnHit = false
    internal var roguelikeChainLightning = false
    internal var roguelikeSummonUpgrade = false
    internal var roguelikeExecute = false
    internal var roguelikeMultishot = false
    private var pendingRoguelike = false

    /** 로그라이크 ATK 가산 풀: atk_boost + race_atk + berserker + boss_slayer */
    private fun roguelikeTotalAtk(isBoss: Boolean): Float =
        roguelikeAtkMult +
            (if (roguelikeBerserkerBase > 0f) roguelikeBerserkerBase * waveSystem.currentWave / 100f else 0f) +
            (if (isBoss) roguelikeBossBonus else 0f)

    /** 로그라이크 온힛 효과 공통 적용 (근접/원거리 공용) */
    private fun applyRoguelikeOnHit(target: Enemy, damage: Float, isMagic: Boolean) {
        if (!target.alive) return
        if (roguelikeArmorShred) {
            val minArmor = target.baseArmor * 0.5f
            target.armor = maxOf(target.armor * 0.95f, minArmor)
        }
        if (roguelikeSlowOnHit && Math.random() < 0.15) {
            target.buffs.addBuff(BuffType.Slow, 0.3f, 2f * (1f + roguelikeCCDuration))
        }
        if (roguelikeDotBoost > 0f) {
            target.buffs.addBuff(BuffType.DoT, damage * roguelikeDotBoost / 3f, 3f)
        }
        if (roguelikeSplash) {
            val splashRange = 80f
            val splashDmg = damage * 0.2f
            spatialHash.forEach(
                target.position.x - splashRange, target.position.y - splashRange,
                target.position.x + splashRange, target.position.y + splashRange,
            ) { nearby ->
                if (nearby.alive && nearby !== target) {
                    nearby.takeDamage(splashDmg, isMagic, 0f)
                    BattleBridge.onDamageDealt(nearby.position.x / W, nearby.position.y / H, splashDmg.toInt(), false)
                }
            }
        }
        if (roguelikeChainLightning && Math.random() < 0.10) {
            val chainRange = 120f
            val chainDmg = damage * 0.5f
            var chainCount = 0
            spatialHash.forEach(
                target.position.x - chainRange, target.position.y - chainRange,
                target.position.x + chainRange, target.position.y + chainRange,
            ) { nearby ->
                if (nearby.alive && nearby !== target && chainCount < 2) {
                    nearby.takeDamage(chainDmg, true, 0f)
                    BattleBridge.onDamageDealt(nearby.position.x / W, nearby.position.y / H, chainDmg.toInt(), false)
                    chainCount++
                }
            }
        }
        if (roguelikeExecute && !target.isBoss && target.alive && target.hp <= target.maxHp * 0.07f) {
            target.hp = 0f
            target.alive = false
        }
    }

    private val roguelikeSystem = RoguelikeEnhanceSystem()
    private val activeRoguelikeBuffs = mutableListOf<ActiveRoguelikeBuff>()

    private var gridPushTimer = 0f

    // PERF: Cached mergeable tiles — recalculated only on grid changes (summon/merge/sell/swap)
    private var mergeableTilesCache: Set<Int> = emptySet()
    private var mergeableDirty = true

    internal fun invalidateMergeCache() { mergeableDirty = true }

    private fun getMergeableTiles(): Set<Int> {
        if (mergeableDirty) {
            mergeableTilesCache = MergeSystem.findMergeableSlots(grid, BlueprintRegistry.instance)
            mergeableDirty = false
        }
        return mergeableTilesCache
    }


    // PERF-02: Scratch lists for toActiveList replacements
    private val activeUnitsScratch = ArrayList<GameUnit>(MAX_UNITS)
    private val activeEnemiesScratch = ArrayList<Enemy>(MAX_ENEMIES)

    // PERF-03: Pre-allocated dead/scratch lists (cleared each frame instead of re-created)
    private val deadEnemies = ArrayList<Enemy>(64)
    private val phoenixReleased = HashSet<Enemy>(32)
    private val deadProjectiles = ArrayList<Projectile>(32)
    private val deadZones = ArrayList<ZoneEffect>(8)

    // Pre-allocated zone data arrays (avoid per-frame allocation)
    private var _zoneDataCount = 0
    private val zoneXs = FloatArray(MAX_ZONE_RENDER)
    private val zoneYs = FloatArray(MAX_ZONE_RENDER)
    private val zoneRadii = FloatArray(MAX_ZONE_RENDER)
    private val zoneFamilies = IntArray(MAX_ZONE_RENDER)
    private val zoneProgresses = FloatArray(MAX_ZONE_RENDER)
    private val zoneGrades = IntArray(MAX_ZONE_RENDER)
    private val splitQueue = ArrayList<Enemy>(16)

    // PERF-01: Pre-allocated buffers for pushStateToCompose
    private val enemyXBuf = FloatArray(MAX_ENEMIES)
    private val enemyYBuf = FloatArray(MAX_ENEMIES)
    private val enemyTypeBuf = IntArray(MAX_ENEMIES)
    private val enemyHpBuf = FloatArray(MAX_ENEMIES)
    private val enemyBuffBuf = IntArray(MAX_ENEMIES)

    private val unitXBuf = FloatArray(MAX_UNITS)
    private val unitYBuf = FloatArray(MAX_UNITS)
    private val unitGradeBuf = IntArray(MAX_UNITS)
    private val unitLevelBuf = IntArray(MAX_UNITS)
    private val unitAttackingBuf = BooleanArray(MAX_UNITS)
    private val unitAttackAnimBuf = FloatArray(MAX_UNITS)
    private val unitTileBuf = IntArray(MAX_UNITS)
    private val unitBlueprintIdBuf = Array(MAX_UNITS) { "" }
    private val unitFamiliesListBuf = Array<List<com.jay.jaygame.data.UnitFamily>>(MAX_UNITS) { emptyList() }
    private val unitRoleBuf = Array(MAX_UNITS) { UnitRole.RANGED_DPS }
    private val unitAttackRangeBuf = Array(MAX_UNITS) { AttackRange.RANGED }
    private val unitDamageTypeBuf = Array(MAX_UNITS) { DamageType.PHYSICAL }
    private val unitCategoryBuf = Array(MAX_UNITS) { UnitCategory.NORMAL }
    private val unitHpBuf = FloatArray(MAX_UNITS)
    private val unitMaxHpBuf = FloatArray(MAX_UNITS)
    private val unitStateBuf = Array(MAX_UNITS) { UnitState.IDLE }
    private val unitHomeXBuf = FloatArray(MAX_UNITS)
    private val unitHomeYBuf = FloatArray(MAX_UNITS)
    private val unitRangeBuf = FloatArray(MAX_UNITS)
    private val unitStackCountBuf = IntArray(MAX_UNITS)
    private val unitBuffBuf = IntArray(MAX_UNITS)
    private val unitSkillAnimBuf = FloatArray(MAX_UNITS)
    private val unitCritAnimBuf = FloatArray(MAX_UNITS)

    private val projSrcXBuf = FloatArray(MAX_PROJECTILES)
    private val projSrcYBuf = FloatArray(MAX_PROJECTILES)
    private val projDstXBuf = FloatArray(MAX_PROJECTILES)
    private val projDstYBuf = FloatArray(MAX_PROJECTILES)
    private val projTypeBuf = IntArray(MAX_PROJECTILES)
    private val projFamilyBuf = IntArray(MAX_PROJECTILES)
    private val projGradeBuf = IntArray(MAX_PROJECTILES)

    private val gridGradeBuf = IntArray(Grid.TOTAL)
    private val gridCanMergeBuf = BooleanArray(Grid.TOTAL)
    private val gridLevelBuf = IntArray(Grid.TOTAL)
    private val gridBlueprintIdBuf = Array(Grid.TOTAL) { "" }
    private val gridFamiliesListBuf = Array<List<com.jay.jaygame.data.UnitFamily>>(Grid.TOTAL) { emptyList() }
    private val gridRoleBuf = Array(Grid.TOTAL) { UnitRole.RANGED_DPS }

    // NEW: Blueprint-based system (singleton, loaded at app startup)
    val blueprintRegistry = BlueprintRegistry.instance

    init {
        // Ensure all behavior factories are registered
        BehaviorRegistration.ensureRegistered()
    }

    // Monster path: 경로 타일 중앙선을 따라 적이 이동 (720×1280 space)
    private val pathMarginSide = 66f
    private val pathMarginTB = 76f
    private val pathLeft = Grid.ORIGIN_X - pathMarginSide / 2f
    private val pathTop = Grid.ORIGIN_Y - pathMarginTB / 2f
    private val pathRight = (Grid.ORIGIN_X + Grid.GRID_W) + pathMarginSide / 2f
    private val pathBottom = (Grid.ORIGIN_Y + Grid.GRID_H) + pathMarginTB / 2f

    // Corner radius for smooth turns (24 interpolation points per corner — 고속에서도 호 유지)
    private val cornerR = 30f

    val enemyPath: List<Vec2> = buildList {
        val steps = 24
        // Top edge: left→right
        add(Vec2(pathLeft + cornerR, pathTop))
        add(Vec2(pathRight - cornerR, pathTop))
        // Top-right corner
        for (i in 0..steps) {
            val angle = (-Math.PI / 2 + (Math.PI / 2) * i / steps).toFloat()
            add(Vec2(pathRight - cornerR + cornerR * kotlin.math.cos(angle),
                      pathTop + cornerR + cornerR * kotlin.math.sin(angle)))
        }
        // Right edge: top→bottom
        add(Vec2(pathRight, pathTop + cornerR))
        add(Vec2(pathRight, pathBottom - cornerR))
        // Bottom-right corner
        for (i in 0..steps) {
            val angle = (0.0 + (Math.PI / 2) * i / steps).toFloat()
            add(Vec2(pathRight - cornerR + cornerR * kotlin.math.cos(angle),
                      pathBottom - cornerR + cornerR * kotlin.math.sin(angle)))
        }
        // Bottom edge: right→left
        add(Vec2(pathRight - cornerR, pathBottom))
        add(Vec2(pathLeft + cornerR, pathBottom))
        // Bottom-left corner
        for (i in 0..steps) {
            val angle = (Math.PI / 2 + (Math.PI / 2) * i / steps).toFloat()
            add(Vec2(pathLeft + cornerR + cornerR * kotlin.math.cos(angle),
                      pathBottom - cornerR + cornerR * kotlin.math.sin(angle)))
        }
        // Left edge: bottom→top
        add(Vec2(pathLeft, pathBottom - cornerR))
        add(Vec2(pathLeft, pathTop + cornerR))
        // Top-left corner
        for (i in 0..steps) {
            val angle = (Math.PI + (Math.PI / 2) * i / steps).toFloat()
            add(Vec2(pathLeft + cornerR + cornerR * kotlin.math.cos(angle),
                      pathTop + cornerR + cornerR * kotlin.math.sin(angle)))
        }
    }

    private var job: Job? = null

    // endregion

    // region Game Loop
    fun start(scope: CoroutineScope) {
        // Reinitialize wave system for dungeon mode
        if (isDungeonMode && dungeonDef != null) {
            val forceBoss = dungeonDef?.type == com.jay.jaygame.data.DungeonType.BOSS_RUSH
            waveSystem = WaveSystem(maxWaves, difficulty, forceBoss = forceBoss)
        }
        // 등급 그룹 통합 강화 — groupUpgradeLevels 배틀 시작 시 0으로 초기화
        UniqueAbilitySystem.zonePool = zones
        UniqueAbilitySystem.activeUnits = activeUnitsScratch
        // ── 종족 드래프트 시너지 초기화 ──
        val draftedRaces = BattleBridge.selectedRaces.value
        synergyAtkMult = if (UnitRace.HUMAN in draftedRaces) 1.10f else 1f
        synergySpdMult = if (UnitRace.SPIRIT in draftedRaces) 1.10f else 1f
        synergyCoinMult = if (UnitRace.ANIMAL in draftedRaces) 1.20f else 1f
        synergyLuckyMerge = if (UnitRace.ROBOT in draftedRaces) 0.05f else 0f
        synergyBossDmgMult = if (UnitRace.DEMON in draftedRaces) 1.15f else 1f

        // Wire relic bonuses into subsystems
        MergeSystem.luckyMergeBonus = (relicManager?.totalLuckyMergeBonus() ?: 0f) + synergyLuckyMerge
        // 이펙트 품질 설정 반영
        com.jay.jaygame.ui.battle.ParticleLOD.userQuality = BattleBridge.effectQuality.value
        // 자동 웨이브 시작이면 초기 딜레이도 스킵
        if (BattleBridge.autoWaveStart.value) waveDelayTimer = 0f
        validatePathSpeedUniformity()
        job = scope.launch(Dispatchers.Default) {
            var lastTime = System.nanoTime()
            var accumulator = 0f

            while (isActive) {
                val now = System.nanoTime()
                val frameDt = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.1f)
                lastTime = now
                val speed = BattleBridge.battleSpeed.value
                accumulator += frameDt * speed

                // Process UI commands safely on the game thread
                for (cmd in BattleBridge.drainCommands()) {
                    when (cmd) {
                        is BattleBridge.BattleCommand.Summon -> requestSummonBlueprint()
                        is BattleBridge.BattleCommand.Merge -> mergeHandler.requestMerge(cmd.tileIndex)
                        is BattleBridge.BattleCommand.MergeAll -> mergeHandler.requestMergeAll()
                        is BattleBridge.BattleCommand.Sell -> economy.requestSell(cmd.tileIndex)
                        is BattleBridge.BattleCommand.SellAllSlot -> economy.requestSellAllSlot(cmd.tileIndex)
                        is BattleBridge.BattleCommand.BulkSell -> cmd.result.complete(economy.requestBulkSell(cmd.grade))
                        is BattleBridge.BattleCommand.GroupUpgrade -> requestGroupUpgrade(cmd.groupIndex)
                        is BattleBridge.BattleCommand.RecipeCraft -> mergeHandler.requestRecipeCraft(cmd.recipeId)
                        is BattleBridge.BattleCommand.Swap -> requestSwap(cmd.from, cmd.to)
                        is BattleBridge.BattleCommand.Gamble -> cmd.result.complete(requestGamble(cmd.option, cmd.betSize))
                        is BattleBridge.BattleCommand.BuyBlueprint -> requestBuyBlueprint(cmd.blueprintId, cmd.cost.toFloat())
                        is BattleBridge.BattleCommand.BattleUpgrade -> applyBattleUpgrade(cmd.upgradeType, cmd.level, cmd.cost)
                        is BattleBridge.BattleCommand.BuyLuckyStone -> {
                            if (sp >= cmd.cost) {
                                sp -= cmd.cost
                                luckyStones++
                            }
                        }
                        is BattleBridge.BattleCommand.SelectRoguelikeBuff -> {
                            val choices = BattleBridge.roguelikeChoices.value
                            if (choices != null && cmd.index in choices.indices) {
                                val selectedBuff = choices[cmd.index]
                                roguelikeSystem.applyBuff(selectedBuff, this@BattleEngine)

                                val existing = activeRoguelikeBuffs.find { it.buff.id == selectedBuff.id }
                                if (existing != null) existing.stacks++
                                else activeRoguelikeBuffs.add(ActiveRoguelikeBuff(selectedBuff))

                                // 로그라이크 Lucky merge 보너스 반영
                                MergeSystem.luckyMergeBonus = (relicManager?.totalLuckyMergeBonus() ?: 0f) + synergyLuckyMerge + roguelikeLuckyBonus

                                BattleBridge.updateActiveRoguelikeBuffs(activeRoguelikeBuffs.toList())
                                BattleBridge.clearRoguelikeChoices()
                                pendingRoguelike = false
                            }
                        }
                        is BattleBridge.BattleCommand.RerollRoguelike -> {
                            if (BattleBridge.roguelikeRerollsLeft.value > 0 && pendingRoguelike) {
                                BattleBridge.useReroll()
                                val clearedWave = waveSystem.currentWave + 1
                                val newChoices = roguelikeSystem.generateChoices(clearedWave, activeRoguelikeBuffs, BattleBridge.selectedRaces.value)
                                if (newChoices.isNotEmpty()) {
                                    BattleBridge.showRoguelikeChoices(newChoices)
                                }
                            }
                        }
                    }
                }

                // Cap iterations per frame to avoid spiral-of-death at high speed
                val maxSteps = (speed * 2).toInt().coerceIn(1, 16)
                var steps = 0
                while (accumulator >= FIXED_DT && steps < maxSteps) {
                    update(FIXED_DT)
                    accumulator -= FIXED_DT
                    steps++
                }
                // Drain excess accumulator to prevent unbounded buildup
                if (accumulator > FIXED_DT * 4) accumulator = FIXED_DT * 2

                pushStateToCompose()
                delay(16) // ~60 FPS — sufficient for TD game, halves GC pressure
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun update(dt: Float) {
        elapsedTime += dt
        // 코인 시스템: 자동 회복 없음 — 적 처치/웨이브 클리어로만 획득

        when (state) {
            State.WaveDelay -> {
                if (pendingRoguelike) {
                    // 로그라이크 선택 대기 중 — 타이머 진행 안 함
                } else {
                    waveDelayTimer -= dt
                    if (waveDelayTimer <= 0f) {
                        waveSystem.startWave(waveSystem.currentWave)
                        val config = waveSystem.getWaveConfig(waveSystem.currentWave)
                        isBossRound = config.isBoss
                        SfxManager.play(if (isBossRound) SoundEvent.BossAppear else SoundEvent.WaveStart, if (isBossRound) 1f else 0.7f)
                        sp += relicManager?.totalWaveStartSp() ?: 0f  // 유물 웨이브 시작 보너스 코인
                        UniqueAbilitySystem.onBlueprintPassiveWaveStart(units) { bonus -> sp += bonus }
                        state = State.Playing
                    }
                }
            }
            State.Playing -> {
                updateSpawning(dt)
                updateEnemies(dt)
                updateUnits(dt)
                updateProjectiles(dt)
                updateZones(dt)
                updatePets(dt)

                if (enemies.activeCount > peakEnemyCount) {
                    peakEnemyCount = enemies.activeCount
                }

                // Defeat: 100+ alive enemies
                if (enemies.activeCount >= DEFEAT_ENEMY_COUNT) {
                    if (petSystem.canPhoenixRevive()) {
                        // Pet ID 8 (봉황): one-time revive — clear excess enemies and continue
                        petSystem.usePhoenixRevive()
                        val safeCount = DEFEAT_ENEMY_COUNT / 2
                        val aliveList = mutableListOf<Enemy>()
                        enemies.forEach { if (it.alive) aliveList.add(it) }
                        val toRemove = (aliveList.size - safeCount).coerceAtLeast(0)
                        aliveList.shuffled().take(toRemove).forEach { it.alive = false; phoenixReleased.add(it); enemies.release(it) }
                        BattleBridge.onPhoenixRevive()
                    } else {
                        state = State.Defeat
                        onBattleEnd(false)
                    }
                }

                // 일반 웨이브: 전멸 or 5분 타임아웃으로 클리어
                if (!waveSystem.waveComplete && waveSystem.allSpawned && enemies.activeCount == 0) {
                    waveSystem.forceComplete()
                }

                // 보스 타임아웃 → 즉시 패배 (보스가 살아있으면)
                if (isBossRound && waveSystem.waveComplete && enemies.activeCount > 0) {
                    state = State.Defeat
                    onBattleEnd(false)
                    return
                }

                // 웨이브 클리어 → 보상 지급 및 다음 웨이브 (타임아웃 시 잔존 적 유지)
                if (waveSystem.waveComplete) {
                    SfxManager.play(SoundEvent.WaveClear, 0.8f)
        
                    val waveClearCoins = (WAVE_CLEAR_BASE + waveSystem.currentWave * WAVE_CLEAR_PER_WAVE) * diffCoinMult * synergyCoinMult * roguelikeCoinMult
                    sp += waveClearCoins
                    val waveClearGold = 5 + waveSystem.currentWave
                    BattleBridge.onGoldPickup(0.5f, 0.5f, waveClearGold)

                    if (waveSystem.isLastWave) {
                        state = State.Victory
                        onBattleEnd(true)
                    } else {
                        // 로그라이크 강화: 보스 웨이브(10, 20, 30, 40, 50) 클리어 후 선택지 표시
                        val clearedWave = waveSystem.currentWave + 1  // 1-indexed
                        if (clearedWave % 10 == 0 && clearedWave < maxWaves) {
                            val choices = roguelikeSystem.generateChoices(clearedWave, activeRoguelikeBuffs, BattleBridge.selectedRaces.value)
                            if (choices.isNotEmpty()) {
                                BattleBridge.showRoguelikeChoices(choices)
                                pendingRoguelike = true
                            }
                        }

                        waveSystem.advanceWave()
                        waveDelayTimer = if (BattleBridge.autoWaveStart.value) 0f else WAVE_DELAY
                        state = State.WaveDelay
                    }
                }
            }
            State.Victory, State.Defeat -> { /* frozen */ }
        }
        gridPushTimer += dt
    }

    // endregion

    // region Per-Frame Updates (spawning, enemies, units, projectiles, zones, pets)
    private fun updateSpawning(dt: Float) {
        val toSpawn = waveSystem.update(dt)
        val config = waveSystem.getWaveConfig(waveSystem.currentWave)
        repeat(toSpawn) {
            val enemy = enemies.acquire() ?: return
            val isElite = config.eliteChance > 0f && Math.random() < config.eliteChance
            enemy.init(
                hp = config.hp * (if (isElite) ELITE_HP_MULT else 1f),
                speed = config.speed * (if (isElite) ELITE_SPEED_MULT else 1f),
                armor = config.armor * (if (isElite) ELITE_ARMOR_MULT else 1f),
                magicResist = config.magicResist * (if (isElite) ELITE_MAGIC_RESIST_MULT else 1f),
                type = config.enemyType, startPos = enemyPath.first().copy(),
                ccResistance = config.ccResistance + if (isElite) ELITE_CC_RESIST_BONUS else 0f,
            )
            enemy.isElite = isElite
            // High difficulty: elite enemies get regeneration
            if (isElite && difficulty >= 1) {
                enemy.bossModifier = BossModifier.REGENERATION
                enemy.regenTimer = 8f
            }
            // Assign boss modifier
            val isTrueBoss = (waveSystem.currentWave + 1) % 10 == 0
            val isDungeonBoss = isDungeonMode && dungeonDef?.type == com.jay.jaygame.data.DungeonType.BOSS_RUSH
            if (config.isBoss && (isTrueBoss || isDungeonBoss)) {
                val modifier = if (isDungeonBoss) {
                    BossModifier.entries.random() // Random modifier each boss rush wave
                } else {
                    getBossModifier(stageId, waveSystem.currentWave)
                }
                enemy.bossModifier = modifier
                if (modifier == BossModifier.SWIFT) {
                    enemy.baseSpeed *= SWIFT_SPEED_MULT
                    enemy.speed = enemy.baseSpeed
                }
                if (modifier == BossModifier.SHIELDED) {
                    enemy.shieldTimer = SHIELDED_COOLDOWN_DURATION // Start with shield down
                    enemy.shieldActive = false
                }
                enemy.applyBossModifierFlags()
                // Notify UI of boss modifier
                BattleBridge.notifyBossModifier(modifier)
            }
        }
    }

    private fun updateEnemies(dt: Float) {
        spatialHash.clear()
        splitQueue.clear() // Reuse pre-allocated list
        enemies.forEach { enemy ->
            if (!enemy.alive) return@forEach
            enemy.update(dt, enemyPath)

            when (enemy.bossModifier) {
                // COMMANDER: buff nearby enemy armor (temporarily set higher)
                BossModifier.COMMANDER -> {
                    // No runtime buff needed — COMMANDER effect is applied
                    // during enemy spawn by increasing base armor of nearby enemies
                    // Simple implementation: reduce damage taken by nearby enemies via shield buff
                    spatialHash.forEach(
                        enemy.position.x - 200f, enemy.position.y - 200f,
                        enemy.position.x + 200f, enemy.position.y + 200f,
                    ) { nearby ->
                        if (nearby !== enemy && nearby.alive && !nearby.buffs.hasBuff(BuffType.Shield)) {
                            nearby.buffs.addBuff(BuffType.Shield, nearby.maxHp * COMMANDER_SHIELD_RATIO, 2f)
                        }
                    }
                }
                // REGENERATION: heal 5% maxHp every 10 seconds
                BossModifier.REGENERATION -> {
                    enemy.regenTimer -= dt
                    if (enemy.regenTimer <= 0f) {
                        enemy.hp = (enemy.hp + enemy.maxHp * REGEN_HEAL_RATIO).coerceAtMost(enemy.maxHp)
                        enemy.regenTimer = REGEN_INTERVAL
                    }
                }
                // BERSERKER: triple attack speed (move speed) below 50% HP
                BossModifier.BERSERKER -> {
                    if (!enemy.berserkerActivated && enemy.hpRatio < BERSERKER_HP_THRESHOLD) {
                        enemy.berserkerActivated = true
                        enemy.speed = enemy.baseSpeed * BERSERKER_SPEED_MULT // Faster movement in berserk
                    }
                }
                // SPLITTER: spawn 2 mini-bosses at 50% HP
                BossModifier.SPLITTER -> {
                    if (!enemy.splitterTriggered && enemy.hpRatio < SPLITTER_HP_THRESHOLD) {
                        enemy.splitterTriggered = true
                        splitQueue.add(enemy)
                    }
                }
                // SHIELDED: cycle shield on/off (3s active, 5s cooldown)
                BossModifier.SHIELDED -> {
                    enemy.shieldTimer -= dt
                    if (enemy.shieldTimer <= 0f) {
                        enemy.shieldActive = !enemy.shieldActive
                        enemy.shieldTimer = if (enemy.shieldActive) SHIELDED_ACTIVE_DURATION else SHIELDED_COOLDOWN_DURATION
                    }
                }
                else -> {}
            }

            spatialHash.insert(
                enemy,
                enemy.position.x - enemy.size * 0.5f,
                enemy.position.y - enemy.size * 0.5f,
                enemy.size, enemy.size,
            )
        }
        // Spawn split mini-bosses
        for (parent in splitQueue) {
            repeat(2) {
                val mini = enemies.acquire() ?: return@repeat
                mini.init(
                    hp = parent.maxHp * 0.3f,
                    speed = parent.baseSpeed * 1.3f,
                    armor = parent.armor * 0.7f,
                    magicResist = parent.magicResist * 0.7f,
                    type = 5, // mini-boss type
                    startPos = parent.position.copy(),
                    ccResistance = parent.ccResistance * 0.5f,
                )
                mini.pathIndex = parent.pathIndex
            }
        }
        deadEnemies.clear()
        enemies.forEach { if (!it.alive && it !in phoenixReleased) deadEnemies.add(it) }
        phoenixReleased.clear()
        deadEnemies.forEach { dead ->
            val deathX = dead.position.x / W
            val deathY = dead.position.y / H

            val wasBoss = dead.isBoss
            val wasElite = dead.maxHp > waveSystem.getWaveConfig(waveSystem.currentWave).hp * 1.5f

            // Hell difficulty: enemy death enrages nearby enemies (+15% speed permanently)
            if (difficulty >= 2) {
                spatialHash.forEach(
                    dead.position.x - 80f, dead.position.y - 80f,
                    dead.position.x + 80f, dead.position.y + 80f,
                ) { nearby ->
                    if (nearby.alive && nearby !== dead) {
                        nearby.speed = (nearby.speed * 1.15f).coerceAtMost(nearby.baseSpeed * 2f)
                    }
                }
            }

            // on-kill 처리: AbilityEngine + Blueprint 패시브 (단일 루프)
            var closestKillUnit: GameUnit? = null
            var closestDist = Float.MAX_VALUE
            units.forEach { u ->
                if (!u.alive) return@forEach
                // AbilityEngine ON_KILL: 가장 가까운 유닛 1체 트리거
                val ab = u.activeAbility
                if (ab != null && ab.primitive == AbilityPrimitive.ON_KILL) {
                    val d = u.position.distanceSqTo(dead.position)
                    if (d < closestDist) { closestDist = d; closestKillUnit = u }
                }
                // Blueprint 패시브 on-kill (영구 ATK 증가, 코인 보너스 등)
                if (u.uniqueAbilityType >= UniqueAbilitySystem.BLUEPRINT_ABILITY_BASE) {
                    sp += UniqueAbilitySystem.onBlueprintPassiveKill(u, units)
                }
            }
            closestKillUnit?.let { u ->
                val ab = u.activeAbility ?: return@let
                val result = AbilityEngine.applyOnKill(ab, u)
                if (result.coinBonus > 0) sp += result.coinBonus.toFloat()
            }

            enemies.release(dead)
            killCount++
            val now = System.currentTimeMillis()
            if (now - lastDeathSfxTime > 100) {
                SfxManager.play(SoundEvent.EnemyDeath, 0.5f)
                lastDeathSfxTime = now
            }
            // 코인 획득: 적 처치 시 (보스 60+wave×2, 엘리트 6, 일반 2) × 난이도 × 시너지

            val killCoin = when {
                wasBoss -> COIN_PER_BOSS_BASE + waveSystem.currentWave * COIN_PER_BOSS_PER_WAVE
                wasElite -> COIN_PER_ELITE_KILL.toFloat()
                else -> COIN_PER_KILL.toFloat()
            }
            val vampiricDouble = if (roguelikeVampiricChance > 0f && Math.random() < roguelikeVampiricChance) 2f else 1f
            sp += killCoin * diffCoinMult * synergyCoinMult * roguelikeCoinMult * vampiricDouble
            val eliteGoldMult = if (wasElite) 3f else 1f
            val killGold = (eliteGoldMult * (1f + (relicManager?.totalGoldKillBonus() ?: 0f) + petSystem.getGoldKillBonus())).toInt().coerceAtLeast(1)
            BattleBridge.onGoldPickup(deathX, deathY, killGold)
        }
    }

    private fun updateUnits(dt: Float) {
        units.fillActiveList(activeUnitsScratch)
        val unitList = activeUnitsScratch
        AbilitySystem.applyAuraEffects(unitList, dt, auraTicks)

        // Update unique abilities (hero+ grade skills)
        enemies.fillActiveList(activeEnemiesScratch)
        val activeEnemies = activeEnemiesScratch
        UniqueAbilitySystem.update(unitList, dt, activeEnemies, spatialHash, units)

        // ── AbilityEngine: periodic & aura ticks ──
        units.forEach { u ->
            if (!u.alive) return@forEach
            val ab = u.activeAbility ?: return@forEach
            // Periodic abilities (PERIODIC_AOE, SELF_BUFF_TIMER)
            if (ab.cooldown > 0f && (ab.primitive == AbilityPrimitive.PERIODIC_AOE || ab.primitive == AbilityPrimitive.SELF_BUFF_TIMER)) {
                u.abilityTimer -= dt
                if (u.abilityTimer <= 0f) {
                    val result = AbilityEngine.applyPeriodic(ab, u, activeEnemies, spatialHash, units)
                    if (result.coinBonus > 0) sp += result.coinBonus.toFloat()
                    u.abilityTimer = ab.cooldown
                }
            }
            // Aura abilities
            if (ab.primitive == AbilityPrimitive.AURA_BUFF) {
                u.abilityAuraTick += dt
                val result = AbilityEngine.applyAura(ab, u, units, activeEnemies, spatialHash, dt, u.abilityAuraTick)
                if (result.triggered) {
                    if (result.coinBonus > 0) sp += result.coinBonus.toFloat()
                    u.abilityAuraTick = 0f
                }
            }
        }

        units.forEach { unit ->
            if (!unit.alive && unit.state != UnitState.RESPAWNING) return@forEach

            // NEW: Behavior-based update path — units with a behavior delegate to it
            if (unit.behavior != null) {
                applyGroupBonus(unit)
                unit.behavior?.update(unit, dt) { pos, range ->
                    findNearestEnemy(pos, range)
                }
                if (unit.attackAnimTimer > 0f) unit.attackAnimTimer = (unit.attackAnimTimer - dt).coerceAtLeast(0f)
                if (unit.skillAnimTimer > 0f) unit.skillAnimTimer = (unit.skillAnimTimer - dt).coerceAtLeast(0f)
                if (unit.critAnimTimer > 0f) unit.critAnimTimer = (unit.critAnimTimer - dt).coerceAtLeast(0f)
                // Clamp position — tanks in MOVING/BLOCKING can go to the enemy path area
                val isTankChasing = unit.state == UnitState.MOVING || unit.state == UnitState.BLOCKING
                if (isTankChasing) {
                    unit.position.x = unit.position.x.coerceIn(pathLeft, pathRight)
                    unit.position.y = unit.position.y.coerceIn(pathTop, pathBottom)
                } else {
                    unit.position.x = unit.position.x.coerceIn(Grid.FIELD_MIN_X, Grid.FIELD_MAX_X)
                    unit.position.y = unit.position.y.coerceIn(Grid.FIELD_MIN_Y, Grid.FIELD_MAX_Y)
                }
                // For behavior-based units, use behavior's canAttack() (interface default = true)
                val canFire = unit.behavior?.canAttack() == true
                if (canFire && unit.state == UnitState.ATTACKING && unit.currentTarget?.alive == true) {
                    val target = unit.currentTarget ?: return@forEach
                    val result = unit.behavior?.onAttack(unit, target) ?: return@forEach
                    // Apply relic & synergy bonuses to behavior damage (mirrors fireProjectile logic)
                    val rm = relicManager
                    val relicAtkBonus = rm?.totalAtkPercent() ?: 0f
                    val relicCritChance = rm?.totalCritChanceBonus() ?: 0f
                    val relicCritDmg = rm?.totalCritDamageBonus() ?: 0f
                    val relicArmorPen = rm?.totalArmorPenPercent() ?: 0f
                    val relicMagicDmg = rm?.totalMagicDmgPercent() ?: 0f

                    // Re-evaluate crit with relic + roguelike crit chance bonus
                    val totalExtraCrit = relicCritChance + roguelikeCritBonus
                    val boostedCrit = if (result.isCrit) true
                        else if (totalExtraCrit > 0f)
                            Math.random() < totalExtraCrit
                        else false

                    // Crit damage handling:
                    // - If behavior already flagged isCrit, damage has behavior's own crit multiplier baked in;
                    //   apply only the relic crit damage bonus portion additively (relicCritDmg / behaviorCritMult).
                    // - If crit was newly promoted by relic/role bonus, apply full (2f + relicCritDmg) multiplier.
                    val critBoost = when {
                        result.isCrit && relicCritDmg > 0f -> 1f + relicCritDmg / 2f  // scale up behavior's baked-in crit
                        !result.isCrit && boostedCrit -> 2f + relicCritDmg              // newly promoted crit
                        else -> 1f                                                       // no crit
                    }

                    val familyUpgradeBonus = 1f + (familyUpgradeLevels[unit.familyOrdinal] ?: 0) * 0.05f
                    val gradeBonus = DamageCalculator.gradeMultiplier(unit.grade)
                    val advantageMult = DamageCalculator.familyAdvantageMultiplier(unit.familyOrdinal, target.type)
                    val behBossMult = if (target.isBoss) synergyBossDmgMult else 1f
                    val boostedDamage = result.damage * critBoost *
                        upgradeAtkMult * synergyAtkMult * roguelikeTotalAtk(target.isBoss) * (1f + relicAtkBonus) *
                        familyUpgradeBonus * gradeBonus * advantageMult *
                        (if (result.isMagic) (1f + relicMagicDmg) else 1f) *
                        (if (!result.isMagic && relicArmorPen > 0f) (1f + relicArmorPen * 0.5f) else 1f) *
                        behBossMult

                    if (result.isInstant) {
                        val finalDmg = target.takeDamage(boostedDamage, result.isMagic, unit.range)
                        val nx = target.position.x / W
                        val ny = target.position.y / H
                        BattleBridge.onDamageDealt(nx, ny, finalDmg.toInt(), boostedCrit)
                        val angle = atan2(target.position.y - unit.position.y, target.position.x - unit.position.x)
                        BattleBridge.onMeleeHit(nx, ny, unit.familyOrdinal.coerceIn(0, 5), boostedCrit, angle)
                        tryPlayAttackSfx(boostedCrit, unit.attackRange, unit.damageType)
                    } else {
                        // Projectile — melee slash uses fast projectile, ranged uses normal
                        val isSlash = unit.attackRange == AttackRange.MELEE
                        val projSpeed = if (isSlash) 900f else 400f
                        val proj = projectiles.acquire()
                        if (proj != null) {
                            proj.init(
                                from = unit.position.copy(), target = target,
                                damage = boostedDamage, speed = projSpeed,
                                type = unit.projectileVisualType(),
                                isMagic = result.isMagic, isCrit = boostedCrit,
                                sourceUnitId = unit.tileIndex,
                                abilityType = unit.abilityType,
                                abilityValue = unit.abilityValue,
                                grade = unit.grade, family = unit.familyOrdinal,
                                attackerRange = unit.range,
                            )
                        }
                    }
                    unit.onAttack()
                    unit.chargeMana(1f + roguelikeManaBonus)
                }
            } else {
                applyGroupBonus(unit)
                unit.update(dt) { pos, range ->
                    findNearestEnemy(pos, range)
                }
                if (unit.canAttack()) {
                    fireProjectile(unit)
                    if (roguelikeMultishot && unit.attackRange == AttackRange.RANGED && Math.random() < 0.20) {
                        fireProjectile(unit)
                    }
                    unit.onAttack()
                    unit.chargeMana(1f + roguelikeManaBonus)
                }
            }
        }
    }


    /** Called from onProjectileHit — apply data-driven on-hit ability for the attacking unit. */
    private fun applyAbilityOnHit(sourceUnitTile: Int, target: Enemy) {
        val unit = grid.getUnit(sourceUnitTile) ?: return
        val ab = unit.activeAbility ?: return
        if (ab.primitive == AbilityPrimitive.ON_HIT_CHANCE ||
            ab.primitive == AbilityPrimitive.NTH_ATTACK ||
            ab.primitive == AbilityPrimitive.PASSIVE_STAT) {
            val result = AbilityEngine.applyOnHit(ab, unit, target, spatialHash)
            if (result.coinBonus > 0) sp += result.coinBonus.toFloat()
        }
    }

    private fun updateProjectiles(dt: Float) {
        deadProjectiles.clear()
        projectiles.forEach { proj ->
            if (!proj.alive) { deadProjectiles.add(proj); return@forEach }
            val stillAlive = proj.update(dt)
            val target = proj.target
            if (!stillAlive && target != null && target.alive) {
                AbilitySystem.onProjectileHit(proj, target, spatialHash) { from, t, dmg, type ->
                    val chain = projectiles.acquire() ?: return@onProjectileHit
                    chain.init(from, t, dmg, 400f, type, false, false, -1, 0, 0f, 0, 0)
                }
                // Data-driven ability on-hit (AbilityEngine)
                applyAbilityOnHit(proj.sourceUnitId, target)

                // ── 로그라이크 온힛 효과 ──
                applyRoguelikeOnHit(target, proj.damage, proj.isMagic)

                val nx = target.position.x / W
                val ny = target.position.y / H
                BattleBridge.onDamageDealt(nx, ny, proj.damage.toInt(), proj.isCrit)
                // Melee slash projectile hit effect
                if (proj.speed >= 800f) {
                    val angle = atan2(target.position.y - proj.sourcePos.y, target.position.x - proj.sourcePos.x)
                    BattleBridge.onMeleeHit(nx, ny, proj.family.coerceIn(0, 5), proj.isCrit, angle)
                    tryPlayAttackSfx(proj.isCrit, AttackRange.MELEE, if (proj.isMagic) DamageType.MAGIC else DamageType.PHYSICAL)
                } else {
                    // 원거리 투사체 히트 사운드
                    tryPlayAttackSfx(proj.isCrit, AttackRange.RANGED, if (proj.isMagic) DamageType.MAGIC else DamageType.PHYSICAL)
                }
            }
            if (!proj.alive) deadProjectiles.add(proj)
        }
        deadProjectiles.forEach { projectiles.release(it) }
    }

    private fun updateZones(dt: Float) {
        enemies.fillActiveList(activeEnemiesScratch)
        val activeEnemies = activeEnemiesScratch
        deadZones.clear()
        zones.forEach { zone ->
            if (!zone.alive) { deadZones.add(zone); return@forEach }
            val stillAlive = zone.update(dt, activeEnemies)
            if (!stillAlive) deadZones.add(zone)
        }
        deadZones.forEach { zones.release(it) }

        // Push zone data to UI (reuse pre-allocated arrays)
        val zCount = zones.activeCount
        var zi = 0
        if (zCount > 0) {
            zones.forEach { zone ->
                if (zone.alive && zi < MAX_ZONE_RENDER) {
                    zoneXs[zi] = zone.position.x / W
                    zoneYs[zi] = zone.position.y / H
                    zoneRadii[zi] = zone.radius / W
                    zoneFamilies[zi] = zone.family
                    zoneProgresses[zi] = if (zone.maxDuration > 0f) zone.duration / zone.maxDuration else 1f
                    zoneGrades[zi] = zone.sourceGrade
                    zi++
                }
            }
        }
        // StateFlow는 immutable snapshot 필요 — copyOf로 새 배열 생성 (count=0이면 skip)
        if (zi > 0 || _zoneDataCount > 0) {
            BattleBridge.updateZoneData(
                zoneXs.copyOf(zi), zoneYs.copyOf(zi), zoneRadii.copyOf(zi),
                zoneFamilies.copyOf(zi), zoneProgresses.copyOf(zi), zoneGrades.copyOf(zi), zi,
            )
            _zoneDataCount = zi
        }
    }

    private fun updatePets(dt: Float) {
        enemies.fillActiveList(activeEnemiesScratch)
        val activeEnemies = activeEnemiesScratch
        units.fillActiveList(activeUnitsScratch)
        val activeUnits = activeUnitsScratch
        petSystem.update(
            dt = dt,
            enemies = activeEnemies,
            units = activeUnits,
            onDamageEnemy = { enemy, damage ->
                if (enemy.alive) {
                    enemy.hp -= damage
                    if (enemy.hp <= 0f) enemy.alive = false
                    val nx = enemy.position.x / W
                    val ny = enemy.position.y / H
                    BattleBridge.onDamageDealt(nx, ny, damage.toInt(), false)
                }
            },
            onBuffUnit = { unit, type, value, duration ->
                unit.buffs.addBuff(type, value, duration)
            },
            onDotEnemy = { enemy, dps, duration ->
                if (enemy.alive) {
                    enemy.buffs.addBuff(BuffType.DoT, dps, duration)
                }
            },
        )
    }

    private fun fireProjectile(unit: GameUnit) {
        val target = unit.currentTarget ?: return
        val rm = relicManager
        val relicAtkBonus = rm?.totalAtkPercent() ?: 0f
        val relicCritChance = rm?.totalCritChanceBonus() ?: 0f
        val relicCritDmg = rm?.totalCritDamageBonus() ?: 0f
        val relicArmorPen = rm?.totalArmorPenPercent() ?: 0f
        val relicMagicDmg = rm?.totalMagicDmgPercent() ?: 0f

        val abilityCritBonus = AbilityEngine.getCritBonus(unit.activeAbility)
        val isCrit = Math.random() < (0.05 + upgradeCritRate + relicCritChance + abilityCritBonus + roguelikeCritBonus)
        if (isCrit) unit.critAnimTimer = GameUnit.CRIT_ANIM_DURATION
        val critMultiplier = 2f + relicCritDmg
        val isMagic = unit.damageType == DamageType.MAGIC
        val familyUpgradeBonus = 1f + (familyUpgradeLevels[unit.familyOrdinal] ?: 0) * 0.05f
        val gradeBonus = DamageCalculator.gradeMultiplier(unit.grade)
        val advantageMult = DamageCalculator.familyAdvantageMultiplier(unit.familyOrdinal, target.type)
        val bossMult = if (target.isBoss) synergyBossDmgMult else 1f
        val baseAtk = unit.effectiveATK() * upgradeAtkMult * synergyAtkMult * roguelikeTotalAtk(target.isBoss) * (1f + relicAtkBonus) * familyUpgradeBonus * gradeBonus * advantageMult
        val dmg = baseAtk * (if (isCrit) critMultiplier else 1f) *
            (if (isMagic) (1f + relicMagicDmg) else 1f) *
            (if (!isMagic && relicArmorPen > 0f) (1f + relicArmorPen * 0.5f) else 1f) *
            bossMult

        if (unit.attackRange == AttackRange.MELEE) {
            // 근접: 즉시 데미지 + 슬래시 이펙트
            val finalDmg = target.takeDamage(dmg, isMagic, unit.range)
            val nx = target.position.x / W
            val ny = target.position.y / H
            BattleBridge.onDamageDealt(nx, ny, finalDmg.toInt(), isCrit)
            val angle = kotlin.math.atan2(
                target.position.y - unit.position.y,
                target.position.x - unit.position.x,
            )
            BattleBridge.onMeleeHit(nx, ny, unit.familyOrdinal.coerceIn(0, 5), isCrit, angle)
            tryPlayAttackSfx(isCrit, unit.attackRange, unit.damageType)
            applyAbilityOnHit(unit.tileIndex, target)
            applyRoguelikeOnHit(target, dmg, isMagic)
        } else {
            // 원거리: 투사체 발사
            val proj = projectiles.acquire() ?: return
            proj.init(
                from = unit.position.copy(), target = target,
                damage = dmg, speed = 400f,
                type = unit.projectileVisualType(),
                isMagic = isMagic, isCrit = isCrit,
                sourceUnitId = unit.tileIndex,
                abilityType = unit.abilityType,
                abilityValue = unit.abilityValue,
                grade = unit.grade, family = unit.familyOrdinal,
                attackerRange = unit.range,
            )
        }
    }

    private fun findNearestEnemy(pos: Vec2, range: Float): Enemy? {
        var nearest: Enemy? = null
        var nearestDist = Float.MAX_VALUE
        spatialHash.forEach(pos.x - range, pos.y - range, pos.x + range, pos.y + range) { enemy ->
            if (enemy.alive) {
                val d = enemy.position.distanceSqTo(pos)
                if (d < nearestDist && d <= range * range) {
                    nearestDist = d
                    nearest = enemy
                }
            }
        }
        return nearest
    }

    // endregion

    // region Player Actions

    fun requestSummonBlueprint(gradeOverride: UnitGrade? = null) {
        val effectiveSummonCost = (summonCost * (1f - roguelikeSummonDiscount)).toInt().coerceAtLeast(1)
        if (sp < effectiveSummonCost) return

        val grade = if (gradeOverride != null) {
            gradeOverride
        } else {
            val (rawGrade, resetPity) = probabilityEngine.rollGradeWithPity(currentPity)
            currentPity = if (resetPity) 0 else (currentPity + 1).coerceAtMost(100)
            BattleBridge.updateUnitPullPity(currentPity)
            UnitGrade.entries.getOrElse(rawGrade) { UnitGrade.entries.first() }
        }
        val selectedRaces = BattleBridge.selectedRaces.value
        val candidates = blueprintRegistry.findByRacesAndGradeAndSummonable(selectedRaces, grade)
        if (candidates.isEmpty()) return
        val totalWeight = candidates.sumOf { it.summonWeight }
        if (totalWeight <= 0) return
        var roll = (Math.random() * totalWeight).toInt()
        var sel: UnitBlueprint? = null
        for (bp in candidates) {
            roll -= bp.summonWeight
            if (roll <= 0) { sel = bp; break }
        }
        val selected = sel ?: return

        // Try stacking by same unit first, then find empty slot
        var targetSlot = grid.findStackableSlot(selected.id)
        if (targetSlot < 0) {
            targetSlot = grid.findEmpty()
            if (targetSlot < 0) return  // No available slot
        }

        sp -= effectiveSummonCost
        summonCount++
        summonCost = (BASE_SUMMON_COST + summonCount * SUMMON_COST_INCREMENT).coerceAtMost(MAX_SUMMON_COST)

        // 로그라이크 소환 축복: 7% 확률 1등급 상위 소환
        val finalSelected = if (roguelikeSummonUpgrade && Math.random() < 0.07 && selected.grade < UnitGrade.LEGEND) {
            val nextGrade = UnitGrade.entries.getOrNull(selected.grade.ordinal + 1) ?: selected.grade
            val upgradeCandidates = blueprintRegistry.findByRacesAndGradeAndSummonable(selectedRaces, nextGrade)
            if (upgradeCandidates.isNotEmpty()) upgradeCandidates.random() else selected
        } else selected

        val unit = spawnFromBlueprint(finalSelected) ?: return
        grid.placeUnit(targetSlot, unit)
        invalidateMergeCache()
        when {
            finalSelected.grade >= UnitGrade.LEGEND -> SfxManager.play(SoundEvent.SummonLegend)
            finalSelected.grade >= UnitGrade.RARE   -> SfxManager.play(SoundEvent.SummonRare)
        }
        BattleBridge.onSummonResult(
            grade = finalSelected.grade.ordinal,
            blueprintId = finalSelected.id,
        )
    }

    private fun applyGroupBonus(unit: GameUnit) {
        val grp = UnitUpgradeSystem.gradeToGroup(unit.grade)
        unit.groupAtkBonus = groupAtkBonusCache[grp]
        unit.spdMultiplier = upgradeSpdMult * synergySpdMult * roguelikeSpdMult * (1f + groupSpdBonusCache[grp])
        unit.range = unit.baseRange * upgradeRangeMult * roguelikeRangeMult
    }

    private fun refreshGroupBonusCache() {
        for (g in 0 until UnitUpgradeSystem.GROUP_COUNT) {
            groupAtkBonusCache[g] = UnitUpgradeSystem.getTotalAtkBonus(groupUpgradeLevels[g])
            groupSpdBonusCache[g] = UnitUpgradeSystem.getTotalSpdBonus(groupUpgradeLevels[g])
        }
    }

    /** Acquire a unit from pool, init from blueprint. Does NOT place on grid.
     *  Position is set by grid.placeUnit() → slotCenter(). */
    internal fun spawnFromBlueprint(bp: UnitBlueprint): GameUnit? {
        val unit = units.acquire() ?: return null
        unit.initFromBlueprint(bp)
        unit.race = bp.race
        unit.range = unit.baseRange * upgradeRangeMult * roguelikeRangeMult
        unit.behavior = if (bp.behaviorId.isNotEmpty()) BehaviorFactory.create(bp.behaviorId) else null
        // Data-driven ability engine: parse ability from blueprint
        unit.activeAbility = AbilityEngine.parseAbility(bp.ability)
        unit.abilityTimer = unit.activeAbility?.cooldown ?: 0f
        UniqueAbilitySystem.initUnit(unit)
        return unit
    }


    // Merge methods delegated to mergeHandler

    /** 등급 그룹 통합 강화 요청 — 코인 소모하여 해당 그룹의 모든 유닛 ATK/속도 업그레이드 */
    fun requestGroupUpgrade(groupIndex: Int) {
        if (groupIndex !in 0 until UnitUpgradeSystem.GROUP_COUNT) return
        val currentLevel = groupUpgradeLevels[groupIndex]
        if (currentLevel >= UnitUpgradeSystem.MAX_UPGRADE_LEVEL) return
        val cost = UnitUpgradeSystem.getGroupUpgradeCost(groupIndex, currentLevel)
        if (sp < cost) return
        sp -= cost
        groupUpgradeLevels[groupIndex] = currentLevel + 1
        refreshGroupBonusCache()
        SfxManager.play(SoundEvent.LevelUp, 0.7f)
        BattleBridge.updateGroupUpgradeLevels(groupUpgradeLevels.copyOf())
    }

    // Sell methods delegated to economy

    // tryMergeSlot delegated to mergeHandler

    fun requestSwap(from: Int, to: Int) {
        // 슬롯 전체 스택을 한 묶음으로 이동
        val unitsA = grid.removeAllFromSlot(from)
        val unitsB = grid.removeAllFromSlot(to)
        for (u in unitsA) grid.placeUnit(to, u)
        for (u in unitsB) grid.placeUnit(from, u)
        invalidateMergeCache()
    }


    fun requestGamble(option: GambleSystem.GambleOption, betSize: GambleSystem.BetSize): GambleSystem.GambleResult? {
        if (sp <= 0f) return null
        val luckBonus = relicManager?.totalGambleBonus() ?: 0f
        val result = GambleSystem.gamble(sp, option, betSize, luckBonus)
        sp = result.spAfter
        return result
    }

    fun requestBuyBlueprint(blueprintId: String, cost: Float) {
        if (sp < cost) return
        val bp = blueprintRegistry.findById(blueprintId) ?: return

        var targetSlot = grid.findStackableSlot(bp.id)
        if (targetSlot < 0) {
            targetSlot = grid.findEmpty()
            if (targetSlot < 0) return
        }

        val unit = spawnFromBlueprint(bp) ?: return
        sp -= cost
        grid.placeUnit(targetSlot, unit)
        invalidateMergeCache()
        BattleBridge.onSummonResult(
            grade = bp.grade.ordinal,
            blueprintId = bp.id,
        )
    }

    fun applyBattleUpgrade(type: Int, level: Int, cost: Float) {
        if (sp < cost) return
        if (type !in upgradeLevels.indices) return
        sp -= cost
        upgradeLevels[type] = level
        when (type) {
            0 -> upgradeAtkMult = 1f + level * 0.1f
            1 -> upgradeSpdMult = 1f + level * 0.08f
            2 -> upgradeCritRate = level * 0.05f
            3 -> upgradeRangeMult = 1f + level * 0.1f
            4 -> { /* SP 회복 업그레이드 제거 — 코인 시스템에서는 불필요 */ }
        }
    }

    // endregion

    // region State Sync (Engine → Compose)

    private fun pushStateToCompose() {
        syncBattleState()
        syncEnemyPositions()
        syncUnitPositions()
        syncProjectiles()

        if (luckyStones != lastPushedLuckyStones) {
            lastPushedLuckyStones = luckyStones
            BattleBridge.updateLuckyStones(luckyStones)
        }

        syncGridState()
    }

    private fun syncBattleState() {
        BattleBridge.updateState(
            com.jay.jaygame.bridge.BattleStateUpdate(
                wave = waveSystem.currentWave + 1,
                maxWaves = maxWaves,
                hp = (DEFEAT_ENEMY_COUNT - enemies.activeCount).coerceAtLeast(0),
                maxHp = DEFEAT_ENEMY_COUNT,
                sp = sp,
                elapsed = elapsedTime,
                state = state.ordinal,
                summonCost = summonCost,
                enemyCount = enemies.activeCount,
                isBossRound = if (isBossRound) 1 else 0,
                waveTimeRemaining = waveSystem.timeRemaining,
                waveElapsed = waveSystem.waveElapsed,
                waveDelayRemaining = if (state == State.WaveDelay) waveDelayTimer else 0f,
            )
        )
    }

    private fun syncEnemyPositions() {
        var ei = 0
        enemies.forEach { e ->
            if (ei < enemyXBuf.size) {
                enemyXBuf[ei] = e.position.x / W
                enemyYBuf[ei] = e.position.y / H
                enemyTypeBuf[ei] = if (e.isBoss) WaveSystem.BOSS_ENEMY_TYPE else e.type
                enemyHpBuf[ei] = e.hpRatio
                // Build buff bitmask from enemy buff container
                var bits = 0
                val hasSlow = e.buffs.hasBuff(com.jay.jaygame.engine.BuffType.Slow)
                val hasDot = e.buffs.hasBuff(com.jay.jaygame.engine.BuffType.DoT)
                val hasArmorBreak = e.buffs.hasBuff(com.jay.jaygame.engine.BuffType.ArmorBreak)
                val hasStun = e.buffs.isStunned()
                if (hasSlow) bits = bits or com.jay.jaygame.bridge.BUFF_BIT_SLOW
                if (hasDot) bits = bits or com.jay.jaygame.bridge.BUFF_BIT_DOT
                if (hasArmorBreak) bits = bits or com.jay.jaygame.bridge.BUFF_BIT_ARMOR_BREAK
                if (hasStun) bits = bits or com.jay.jaygame.bridge.BUFF_BIT_STUN
                // Poison = slow + dot combo
                if (hasSlow && hasDot) bits = bits or com.jay.jaygame.bridge.BUFF_BIT_POISON
                // Lightning & Wind tracked via recentHitFlags
                if (e.recentHitFlags and 1 != 0) bits = bits or com.jay.jaygame.bridge.BUFF_BIT_LIGHTNING
                if (e.recentHitFlags and 2 != 0) bits = bits or com.jay.jaygame.bridge.BUFF_BIT_WIND
                enemyBuffBuf[ei] = bits
                ei++
            }
        }
        BattleBridge.updateEnemyPositions(enemyXBuf, enemyYBuf, enemyTypeBuf, enemyHpBuf, enemyBuffBuf, ei)
    }

    private fun syncUnitPositions() {
        var ui = 0
        units.forEach { u ->
            if (ui < unitXBuf.size) {
                unitXBuf[ui] = u.position.x / W
                unitYBuf[ui] = u.position.y / H
                unitGradeBuf[ui] = u.grade
                unitLevelBuf[ui] = u.level
                unitAttackingBuf[ui] = u.isAttacking
                unitAttackAnimBuf[ui] = u.attackAnimTimer
                unitTileBuf[ui] = u.tileIndex
                unitBlueprintIdBuf[ui] = u.blueprintId
                unitFamiliesListBuf[ui] = u.families
                unitRoleBuf[ui] = u.role
                unitAttackRangeBuf[ui] = u.attackRange
                unitDamageTypeBuf[ui] = u.damageType
                unitCategoryBuf[ui] = u.unitCategory
                unitHpBuf[ui] = u.hp
                unitMaxHpBuf[ui] = u.maxHp
                unitStateBuf[ui] = u.state
                unitHomeXBuf[ui] = u.homePosition.x / W
                unitHomeYBuf[ui] = u.homePosition.y / H
                unitRangeBuf[ui] = u.range
                unitStackCountBuf[ui] = grid.getStackCount(u.tileIndex)
                var ubits = 0
                if (u.buffs.hasBuff(BuffType.AtkUp)) ubits = ubits or com.jay.jaygame.bridge.UNIT_BUFF_ATK_UP
                if (u.buffs.hasBuff(BuffType.SpdUp)) ubits = ubits or com.jay.jaygame.bridge.UNIT_BUFF_SPD_UP
                if (u.buffs.hasBuff(BuffType.Shield)) ubits = ubits or com.jay.jaygame.bridge.UNIT_BUFF_SHIELD
                if (u.buffs.hasBuff(BuffType.DefUp)) ubits = ubits or com.jay.jaygame.bridge.UNIT_BUFF_DEF_UP
                unitBuffBuf[ui] = ubits
                unitSkillAnimBuf[ui] = u.skillAnimTimer
                unitCritAnimBuf[ui] = u.critAnimTimer
                ui++
            }
        }
        BattleBridge.updateUnitPositions(
            com.jay.jaygame.bridge.UnitPositionBatch(
                xs = unitXBuf, ys = unitYBuf, grades = unitGradeBuf, levels = unitLevelBuf,
                isAttacking = unitAttackingBuf, tileIndices = unitTileBuf, count = ui,
                attackAnimTimers = unitAttackAnimBuf, blueprintIds = unitBlueprintIdBuf,
                familiesList = unitFamiliesListBuf, roles = unitRoleBuf, attackRanges = unitAttackRangeBuf,
                damageTypes = unitDamageTypeBuf, unitCategories = unitCategoryBuf,
                hps = unitHpBuf, maxHps = unitMaxHpBuf, states = unitStateBuf,
                homeXs = unitHomeXBuf, homeYs = unitHomeYBuf, stackCounts = unitStackCountBuf,
                buffs = unitBuffBuf, skillAnimTimers = unitSkillAnimBuf, critAnimTimers = unitCritAnimBuf,
                ranges = unitRangeBuf,
            )
        )
    }

    private fun syncProjectiles() {
        var pi = 0
        projectiles.forEach { p ->
            if (pi < projSrcXBuf.size) {
                projSrcXBuf[pi] = p.sourcePos.x / W
                projSrcYBuf[pi] = p.sourcePos.y / H
                projDstXBuf[pi] = p.position.x / W
                projDstYBuf[pi] = p.position.y / H
                projTypeBuf[pi] = p.type
                projFamilyBuf[pi] = p.family
                projGradeBuf[pi] = p.grade
                pi++
            }
        }
        BattleBridge.updateProjectiles(projSrcXBuf, projSrcYBuf, projDstXBuf, projDstYBuf, projTypeBuf, pi, projFamilyBuf, projGradeBuf)
    }

    private fun syncGridState() {
        if (gridPushTimer >= 0.1f) {
            gridPushTimer = 0f
            val mergeableTiles = getMergeableTiles()
            for (i in 0 until Grid.SLOT_COUNT) {
                val u = grid.getUnit(i)
                if (u != null) {
                    gridGradeBuf[i] = u.grade
                    // canMerge = 등급/카테고리까지 고려한 합성 가능 여부
                    gridCanMergeBuf[i] = i in mergeableTiles
                    gridLevelBuf[i] = grid.getStackCount(i)  // Show stack count as level for UI
                    gridBlueprintIdBuf[i] = u.blueprintId
                    gridFamiliesListBuf[i] = u.families
                    gridRoleBuf[i] = u.role
                } else {
                    gridGradeBuf[i] = 0
                    gridCanMergeBuf[i] = false
                    gridLevelBuf[i] = 0
                    gridBlueprintIdBuf[i] = ""
                    gridFamiliesListBuf[i] = emptyList()
                    gridRoleBuf[i] = UnitRole.RANGED_DPS
                }
            }
            BattleBridge.updateGridState(gridGradeBuf, gridCanMergeBuf, gridLevelBuf, gridBlueprintIdBuf, gridFamiliesListBuf, gridRoleBuf)
        }
    }

    // endregion

    // region Battle End & Validation
    private fun onBattleEnd(victory: Boolean) {
        SfxManager.play(if (victory) SoundEvent.Victory else SoundEvent.Defeat)
        val difficultyBonus = if (isDungeonMode && dungeonDef != null) {
            dungeonDef?.difficultyMultiplier ?: 1f
        } else {
            when (difficulty) {
                1 -> 1.5f    // 하드
                2 -> 2.5f    // 헬
                else -> 1.0f // 일반
            }
        }
        val dungeonRewardMult = if (isDungeonMode) dungeonDef?.rewardMultiplier ?: 1f else 1f
        val baseGold = if (victory) 100 + waveSystem.currentWave * 10 else waveSystem.currentWave * 5
        val relicWaveBonus = if (victory) (1f + (relicManager?.totalGoldWaveBonus() ?: 0f)) else 1f
        val goldEarned = (baseGold * difficultyBonus * relicWaveBonus * dungeonRewardMult).toInt().coerceAtLeast(1)
        val baseTrophy = if (victory) 20 + stageId * 5 else -(10 + stageId * 3)
        val trophyChange = if (baseTrophy > 0) (baseTrophy * difficultyBonus).toInt() else baseTrophy
        val noHpLost = peakEnemyCount <= DEFEAT_ENEMY_COUNT / 5 // HP never dropped below 80%
        val fastClear = victory && elapsedTime < maxWaves * 8f // cleared quickly
        val baseCards = if (victory) 3 + stageId + difficulty * 2 else 1
        val dungeonCardBonus = if (isDungeonMode && victory) waveSystem.currentWave / 5 else 0
        val cardsEarned = baseCards + dungeonCardBonus
        // Roll relic drop on victory (10% chance, 50% in RELIC_HUNT dungeon)
        val relicDrop = if (victory) {
            val isRelicHunt = isDungeonMode && dungeonDef?.type == com.jay.jaygame.data.DungeonType.RELIC_HUNT
            if (isRelicHunt) relicManager?.rollRelicDropBoosted(0.50) else relicManager?.rollRelicDrop()
        } else null
        BattleBridge.onBattleEnd(
            victory, waveSystem.currentWave + 1, goldEarned, trophyChange,
            killCount, mergeCount, cardsEarned, noHpLost, fastClear,
            relicDropId = relicDrop?.first ?: -1,
            relicDropGrade = relicDrop?.second?.ordinal ?: -1,
        )
    }

    // ── Z18: Path Speed Uniformity Validation ──

    private fun validatePathSpeedUniformity() {
        val tag = "BattlePath"

        if (enemyPath.size < 2) {
            Log.w(tag, "Z18: Enemy path has fewer than 2 waypoints")
            return
        }

        val segmentLengths = mutableListOf<Float>()
        for (i in 0 until enemyPath.size - 1) {
            val a = enemyPath[i]
            val b = enemyPath[i + 1]
            val dx = b.x - a.x
            val dy = b.y - a.y
            segmentLengths.add(sqrt(dx * dx + dy * dy))
        }
        // Closing segment (last → first)
        val last = enemyPath.last()
        val first = enemyPath.first()
        val closeDx = first.x - last.x
        val closeDy = first.y - last.y
        segmentLengths.add(sqrt(closeDx * closeDx + closeDy * closeDy))

        val totalLength = segmentLengths.sum()
        val avgLength = totalLength / segmentLengths.size
        val maxDeviation = segmentLengths.maxOf { abs(it - avgLength) }
        val variancePercent = if (avgLength > 0f) (maxDeviation / avgLength) * 100f else 0f

        Log.i(tag, "Z18: Path stats — ${segmentLengths.size} segments, total=${totalLength.toInt()}px, avg=${avgLength.toInt()}px")

        // Log each segment length
        for (i in segmentLengths.indices) {
            val len = segmentLengths[i]
            val devPct = if (avgLength > 0f) ((len - avgLength) / avgLength * 100f) else 0f
            val label = if (i < segmentLengths.size - 1) "Seg[$i]" else "Close"
            Log.d(tag, "Z18: $label length=${len.toInt()}px (${"%+.1f".format(devPct)}%)")
        }

        if (variancePercent < 10f) {
            Log.i(tag, "Z18: Path uniformity OK — max deviation ${variancePercent.toInt()}% (< 10%)")
        } else {
            Log.w(tag, "Z18: Path NOT uniform — max deviation ${variancePercent.toInt()}% (>= 10%). " +
                    "This is expected for paths with corner interpolation points.")
        }
    }
    // endregion
}
