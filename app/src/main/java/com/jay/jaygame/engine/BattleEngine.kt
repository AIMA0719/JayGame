@file:Suppress("DEPRECATION")
package com.jay.jaygame.engine

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
import kotlin.math.atan2

class BattleEngine(
    private val stageId: Int,
    private val difficulty: Int,
    private val maxWaves: Int,
    gameData: GameData? = null,
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
        const val HP_PRESSURE_THRESHOLD = DEFEAT_ENEMY_COUNT / 5  // 20+ 동시 생존 적 → 3성 불가
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
        const val COIN_PER_MINIBOSS_KILL = 25f
        const val MAX_SELL_RATIO = 0.8f
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

        // ── 신규 보스 기믹 상수 ──
        const val PHANTOM_VISIBLE_DURATION = 3f   // 3초 보임
        const val PHANTOM_INVISIBLE_DURATION = 1f  // 1초 투명
        const val GRAVITY_RANGE = 250f             // 중력장 범위
        const val GRAVITY_SPEED_REDUCTION = 0.7f   // 공격속도 30% 감소
        const val MIRROR_DISABLE_DURATION = 2f     // 반사 시 유닛 공격 불능 시간
        const val ADAPTIVE_CHECK_INTERVAL = 5f     // 적응 갱신 주기
        const val ADAPTIVE_RESIST_MULT = 0.6f      // 적응 저항 (40% 감소)
        const val MINION_RUSH_GUARD_COUNT = 5      // 호위대 수
        const val MINION_RUSH_GUARD_HP_MULT = 0.15f // 호위대 HP = 보스 HP의 15%
        const val DARKNESS_RANGE_MULT = 0.7f       // 암흑 웨이브 사거리 감소
    }

    // endregion

    // region Core State
    enum class State { WaveDelay, Playing, Victory, Defeat }
    var state = State.WaveDelay; private set

    val maxUnitSlots: Int = Grid.SLOT_COUNT  // 18 slots (3x6 grid)
    private val diffCoinMult: Float = when (difficulty) { 1 -> 1.2f; 2 -> 1.5f; else -> 1f }

    var relicManager: RelicManager? = gameData?.let { RelicManager(it) }
    val petSystem = PetBattleSystem().also { ps -> gameData?.let { ps.init(it) } }

    // Dungeon mode state
    var isDungeonMode: Boolean = false
    var dungeonDef: DungeonDef? = null

    // Family upgrade levels loaded from saved data
    private val familyUpgradeLevels: Map<Int, Int> = gameData?.let { data ->
        com.jay.jaygame.data.UnitFamily.entries.associate { family ->
            family.ordinal to (data.familyUpgrades[family.name] ?: 0)
        }
    } ?: emptyMap()

    // Permanent unit levels loaded from saved data, keyed by blueprintId
    private val permanentUnitLevels: Map<String, Int> = gameData?.let { data ->
        data.units.mapValues { (_, progress) -> progress.level }
    } ?: emptyMap()

    private val probabilityEngine = DefaultProbabilityEngine()

    // Currency for lucky-stone purchases during battle
    var luckyStones: Int = 0; internal set

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

    /** Main battle economy state: SP, summon cost, and elapsed time. */
    var sp = 50f; internal set
    var summonCost = BASE_SUMMON_COST; private set
    private var summonCount = 0  // Used to scale summon cost over time
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

    // Group upgrade levels and cached bonuses
    private val groupUpgradeLevels = IntArray(UnitUpgradeSystem.GROUP_COUNT)
    private val groupAtkBonusCache = FloatArray(UnitUpgradeSystem.GROUP_COUNT)
    private val groupSpdBonusCache = FloatArray(UnitUpgradeSystem.GROUP_COUNT)

    // Drafted race synergy multipliers
    private var synergyAtkMult = 1f       // HUMAN: +10% ATK
    private var synergySpdMult = 1f       // SPIRIT: +10% speed
    private var synergyCoinMult = 1f      // ANIMAL: +20% coin gain
    private var synergyLuckyMerge = 0f    // ROBOT: lucky merge bonus
    private var synergyBossDmgMult = 1f   // DEMON: +15% boss damage

    // Roguelike buff state applied during battle
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

    /** Total roguelike attack multiplier contribution. */
    private fun roguelikeTotalAtk(isBoss: Boolean): Float =
        roguelikeAtkMult +
            (if (roguelikeBerserkerBase > 0f) (roguelikeBerserkerBase * waveSystem.currentWave / 100f).coerceAtMost(0.50f) else 0f) +
            (if (isBoss) roguelikeBossBonus else 0f)

    /** Apply roguelike on-hit side effects without changing attack flow. */
    private fun applyRoguelikeOnHit(target: Enemy, damage: Float, isMagic: Boolean, dotBaseAtk: Float? = null) {
        if (!target.alive) return
        if (roguelikeArmorShred) {
            val minArmor = target.baseArmor * 0.5f
            target.armor = maxOf(target.armor * 0.95f, minArmor)
        }
        if (roguelikeSlowOnHit && Math.random() < 0.15) {
            target.buffs.addBuff(BuffType.Slow, 0.3f, 2f * (1f + roguelikeCCDuration))
        }
        if (roguelikeDotBoost > 0f) {
            val dotBase = dotBaseAtk ?: damage
            target.buffs.addBuff(BuffType.DoT, dotBase * roguelikeDotBoost / 3f, 3f)
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
    private val statePublisher = BattleStatePublisher(MAX_ENEMIES, MAX_UNITS, MAX_PROJECTILES, Grid.TOTAL)

    // PERF: Cached mergeable tiles ??recalculated only on grid changes (summon/merge/sell/swap)
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

    // NEW: Blueprint-based system (singleton, loaded at app startup)
    val blueprintRegistry = BlueprintRegistry.instance

    init {
        // Ensure all behavior factories are registered
        BehaviorRegistration.ensureRegistered()
    }

    // Monster path around the battlefield in 720x1280 space
    private val pathMarginSide = 66f
    private val pathMarginTB = 76f
    private val pathLeft = Grid.ORIGIN_X - pathMarginSide / 2f
    private val pathTop = Grid.ORIGIN_Y - pathMarginTB / 2f
    private val pathRight = (Grid.ORIGIN_X + Grid.GRID_W) + pathMarginSide / 2f
    private val pathBottom = (Grid.ORIGIN_Y + Grid.GRID_H) + pathMarginTB / 2f

    // Corner radius for smooth turns; each corner uses 24 interpolation points
    private val cornerR = 30f

    val enemyPath: List<Vec2> = buildList {
        val steps = 24
        // Top edge: left to right
        add(Vec2(pathLeft + cornerR, pathTop))
        add(Vec2(pathRight - cornerR, pathTop))
        // Top-right corner
        for (i in 0..steps) {
            val angle = (-Math.PI / 2 + (Math.PI / 2) * i / steps).toFloat()
            add(Vec2(pathRight - cornerR + cornerR * kotlin.math.cos(angle),
                      pathTop + cornerR + cornerR * kotlin.math.sin(angle)))
        }
        // Right edge: top to bottom
        add(Vec2(pathRight, pathTop + cornerR))
        add(Vec2(pathRight, pathBottom - cornerR))
        // Bottom-right corner
        for (i in 0..steps) {
            val angle = (0.0 + (Math.PI / 2) * i / steps).toFloat()
            add(Vec2(pathRight - cornerR + cornerR * kotlin.math.cos(angle),
                      pathBottom - cornerR + cornerR * kotlin.math.sin(angle)))
        }
        // Bottom edge: right to left
        add(Vec2(pathRight - cornerR, pathBottom))
        add(Vec2(pathLeft + cornerR, pathBottom))
        // Bottom-left corner
        for (i in 0..steps) {
            val angle = (Math.PI / 2 + (Math.PI / 2) * i / steps).toFloat()
            add(Vec2(pathLeft + cornerR + cornerR * kotlin.math.cos(angle),
                      pathBottom - cornerR + cornerR * kotlin.math.sin(angle)))
        }
        // Left edge: bottom to top
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
        // Initialize group upgrade caches
        UniqueAbilitySystem.zonePool = zones
        UniqueAbilitySystem.activeUnits = activeUnitsScratch
        // Apply drafted race synergies
        val draftedRaces = BattleBridge.selectedRaces.value
        synergyAtkMult = if (UnitRace.HUMAN in draftedRaces) 1.10f else 1f
        synergySpdMult = if (UnitRace.SPIRIT in draftedRaces) 1.10f else 1f
        synergyCoinMult = if (UnitRace.ANIMAL in draftedRaces) 1.20f else 1f
        synergyLuckyMerge = if (UnitRace.ROBOT in draftedRaces) 0.05f else 0f
        synergyBossDmgMult = if (UnitRace.DEMON in draftedRaces) 1.15f else 1f

        // Wire relic bonuses into subsystems
        MergeSystem.luckyMergeBonus = (relicManager?.totalLuckyMergeBonus() ?: 0f) + synergyLuckyMerge
        // Push current effect quality to the UI particle system
        com.jay.jaygame.ui.battle.ParticleLOD.userQuality = BattleBridge.effectQuality.value
        // Skip initial wave delay when auto-wave is enabled
        if (BattleBridge.autoWaveStart.value) waveDelayTimer = 0f
        BattlePathValidator.validate(enemyPath)
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

                                // Update lucky merge bonus after roguelike selection
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
                delay(16) // ~60 FPS ??sufficient for TD game, halves GC pressure
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun update(dt: Float) {
        elapsedTime += dt
        // Advance battle simulation according to the current state

        when (state) {
            State.WaveDelay -> {
                if (pendingRoguelike) {
                    // Waiting for roguelike selection before starting the next wave
                } else {
                    waveDelayTimer -= dt
                    if (waveDelayTimer <= 0f) {
                        waveSystem.startWave(waveSystem.currentWave)
                        val config = waveSystem.getWaveConfig(waveSystem.currentWave)
                        isBossRound = config.isBoss
                        SfxManager.play(if (isBossRound) SoundEvent.BossAppear else SoundEvent.WaveStart, if (isBossRound) 1f else 0.7f)
                        sp += relicManager?.totalWaveStartSp() ?: 0f  // ?좊Ъ ?⑥씠釉??쒖옉 蹂대꼫??肄붿씤
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
                if (!hpEverLost && enemies.activeCount > HP_PRESSURE_THRESHOLD) {
                    hpEverLost = true
                }

                // Defeat: 100+ alive enemies
                if (enemies.activeCount >= DEFEAT_ENEMY_COUNT) {
                    if (petSystem.canPhoenixRevive()) {
                        // Pet ID 8: one-time revive, clear excess enemies, then continue
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

                // Non-boss waves can force-complete after all spawns are dead
                if (!waveSystem.waveComplete && waveSystem.allSpawned && enemies.activeCount == 0) {
                    waveSystem.forceComplete()
                }

                // Boss waves fail if the timer ends while enemies are still alive
                if (isBossRound && waveSystem.waveComplete && enemies.activeCount > 0) {
                    state = State.Defeat
                    onBattleEnd(false)
                    return
                }

                // Handle wave clear rewards and next-wave transition
                if (waveSystem.waveComplete) {
                    SfxManager.play(SoundEvent.WaveClear, 0.8f)
        
                    val waveClearCoins = (WAVE_CLEAR_BASE + waveSystem.currentWave * WAVE_CLEAR_PER_WAVE) * diffCoinMult * synergyCoinMult
                    sp += waveClearCoins
                    val waveClearGold = 5 + waveSystem.currentWave
                    BattleBridge.onGoldPickup(0.5f, 0.5f, waveClearGold)

                    if (waveSystem.isLastWave) {
                        state = State.Victory
                        onBattleEnd(true)
                    } else {
                        // Offer roguelike choices every 10 cleared waves, except the last wave
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
        statePublisher.advance(dt)
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
                    enemy.shieldTimer = SHIELDED_COOLDOWN_DURATION
                    enemy.shieldActive = false
                }
                if (modifier == BossModifier.PHANTOM) {
                    enemy.phantomTimer = PHANTOM_VISIBLE_DURATION // 처음엔 보이는 상태
                    enemy.phantomActive = false
                }
                if (modifier == BossModifier.DUAL_MOD) {
                    // 후반 전용: 기본 풀에서 랜덤 2개를 보조로 설정
                    val pool = BASE_MODIFIER_POOL.filter {
                        it != BossModifier.DUAL_MOD && it != BossModifier.MINION_RUSH
                    }
                    val first = pool.random()
                    val second = pool.filter { it != first }.random()
                    enemy.bossModifier = BossModifier.DUAL_MOD
                    enemy.dualModFirst = first
                    enemy.dualModSecond = second
                    // 양쪽 기믹 초기화
                    initBossModifierState(enemy, first)
                    initBossModifierState(enemy, second)
                    // CC/DOT 면역은 양쪽 모두 체크
                    enemy.buffs.ccImmune = first == BossModifier.CC_IMMUNE || second == BossModifier.CC_IMMUNE
                    enemy.buffs.dotImmune = first == BossModifier.DOT_IMMUNE || second == BossModifier.DOT_IMMUNE
                    BattleBridge.notifyBossModifier(modifier)
                } else {
                    enemy.applyBossModifierFlags()
                    BattleBridge.notifyBossModifier(modifier)
                }
                if (modifier == BossModifier.MINION_RUSH) {
                    enemy.guardsAlive = MINION_RUSH_GUARD_COUNT
                    // 호위대 5마리 스폰 (보스와 같은 타입)
                    repeat(MINION_RUSH_GUARD_COUNT) {
                        val guard = enemies.acquire() ?: return@repeat
                        guard.init(
                            hp = enemy.maxHp * MINION_RUSH_GUARD_HP_MULT,
                            speed = enemy.baseSpeed * 1.2f,
                            armor = enemy.armor * 0.6f,
                            magicResist = enemy.magicResist * 0.6f,
                            type = config.enemyType, // 보스와 같은 타입
                            startPos = enemy.position.copy(),
                            ccResistance = enemy.ccResistance * 0.3f,
                        )
                        guard.isElite = true
                        guard.isBossGuard = true
                        guard.guardBossRef = enemy
                        guard.pathIndex = enemy.pathIndex
                    }
                }
            }
        }
    }

    /** 보스 기믹별 초기 상태 설정 (DUAL_MOD용) */
    private fun initBossModifierState(enemy: Enemy, mod: BossModifier) {
        when (mod) {
            BossModifier.SWIFT -> { enemy.baseSpeed *= SWIFT_SPEED_MULT; enemy.speed = enemy.baseSpeed }
            BossModifier.SHIELDED -> { enemy.shieldTimer = SHIELDED_COOLDOWN_DURATION; enemy.shieldActive = false }
            BossModifier.PHANTOM -> { enemy.phantomTimer = PHANTOM_VISIBLE_DURATION; enemy.phantomActive = false }
            else -> {}
        }
    }

    private fun updateEnemies(dt: Float) {
        spatialHash.clear()
        splitQueue.clear() // Reuse pre-allocated list
        gravityBossPos = null // 매 프레임 리셋
        enemies.forEach { enemy ->
            if (!enemy.alive) return@forEach
            enemy.update(dt, enemyPath)

            // GRAVITY 보스 위치 캐시 (applyGroupBonus에서 사용)
            if (enemy.hasModifier(BossModifier.GRAVITY)) {
                gravityBossPos = enemy.position
            }

            // 보스 기믹 업데이트
            updateBossModifier(enemy, dt)

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

            // On-kill hooks: AbilityEngine and blueprint passive effects
            var closestKillUnit: GameUnit? = null
            var closestDist = Float.MAX_VALUE
            units.forEach { u ->
                if (!u.alive) return@forEach
                // AbilityEngine ON_KILL: choose the nearest eligible unit
                val ab = u.activeAbility
                if (ab != null && ab.primitive == AbilityPrimitive.ON_KILL) {
                    val d = u.position.distanceSqTo(dead.position)
                    if (d < closestDist) { closestDist = d; closestKillUnit = u }
                }
                // Blueprint on-kill passives may add SP or permanent bonuses
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
            // Grant SP and gold for kills based on enemy type and modifiers

            val killCoin = when {
                wasBoss && !waveSystem.currentConfig.isMiniBoss ->
                    COIN_PER_BOSS_BASE + waveSystem.currentWave * COIN_PER_BOSS_PER_WAVE
                wasBoss -> COIN_PER_MINIBOSS_KILL // 3마리 * 25 = 75 ≈ 일반 80
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

    // (DUAL_MOD 기믹은 Enemy.dualModFirst/dualModSecond에 저장됨)

    /** 보스 기믹 매 프레임 업데이트 */
    private fun updateBossModifier(enemy: Enemy, dt: Float) {
        val mod = enemy.bossModifier ?: return

        if (mod == BossModifier.DUAL_MOD) {
            // DUAL_MOD: enemy 자체에 저장된 기믹 2개 실행 (엔진 변수 대신 — 보스 2마리 동시 존재 대비)
            enemy.dualModFirst?.let { executeSingleModifier(enemy, it, dt) }
            enemy.dualModSecond?.let { executeSingleModifier(enemy, it, dt) }
            return
        }

        executeSingleModifier(enemy, mod, dt)
    }

    private fun executeSingleModifier(enemy: Enemy, mod: BossModifier, dt: Float) {
        when (mod) {
            // COMMANDER: 주변 200px 적에게 쉴드 버프
            BossModifier.COMMANDER -> {
                spatialHash.forEach(
                    enemy.position.x - 200f, enemy.position.y - 200f,
                    enemy.position.x + 200f, enemy.position.y + 200f,
                ) { nearby ->
                    if (nearby !== enemy && nearby.alive && !nearby.buffs.hasBuff(BuffType.Shield)) {
                        nearby.buffs.addBuff(BuffType.Shield, nearby.maxHp * COMMANDER_SHIELD_RATIO, 2f)
                    }
                }
            }
            // REGENERATION: 10초마다 5% 회복
            BossModifier.REGENERATION -> {
                enemy.regenTimer -= dt
                if (enemy.regenTimer <= 0f) {
                    enemy.hp = (enemy.hp + enemy.maxHp * REGEN_HEAL_RATIO).coerceAtMost(enemy.maxHp)
                    enemy.regenTimer = REGEN_INTERVAL
                }
            }
            // BERSERKER: 50% 이하 속도 1.5배
            BossModifier.BERSERKER -> {
                if (!enemy.berserkerActivated && enemy.hpRatio < BERSERKER_HP_THRESHOLD) {
                    enemy.berserkerActivated = true
                    enemy.speed = enemy.baseSpeed * BERSERKER_SPEED_MULT
                }
            }
            // SPLITTER: 50% 이하 2마리 분열
            BossModifier.SPLITTER -> {
                if (!enemy.splitterTriggered && enemy.hpRatio < SPLITTER_HP_THRESHOLD) {
                    enemy.splitterTriggered = true
                    splitQueue.add(enemy)
                }
            }
            // SHIELDED: 3초 활성 / 5초 쿨다운 사이클
            BossModifier.SHIELDED -> {
                enemy.shieldTimer -= dt
                if (enemy.shieldTimer <= 0f) {
                    enemy.shieldActive = !enemy.shieldActive
                    enemy.shieldTimer = if (enemy.shieldActive) SHIELDED_ACTIVE_DURATION else SHIELDED_COOLDOWN_DURATION
                }
            }
            // ── 신규 기믹 ──
            // PHANTOM: 3초 보임 → 1초 투명 사이클
            BossModifier.PHANTOM -> {
                enemy.phantomTimer -= dt
                if (enemy.phantomTimer <= 0f) {
                    enemy.phantomActive = !enemy.phantomActive
                    enemy.phantomTimer = if (enemy.phantomActive) PHANTOM_INVISIBLE_DURATION else PHANTOM_VISIBLE_DURATION
                }
            }
            // MIRROR: 반사 쿨타임 감소 + 가장 가까운 유닛에 디버프
            BossModifier.MIRROR -> {
                enemy.mirrorCooldown = (enemy.mirrorCooldown - dt).coerceAtLeast(0f)
                if (enemy.lastMirrorDamage > 0f) {
                    // 가장 가까운 유닛 찾아서 공격 불능
                    var nearest: GameUnit? = null
                    var nearDist = Float.MAX_VALUE
                    units.forEach { u ->
                        if (!u.alive) return@forEach
                        val d = u.position.distanceSqTo(enemy.position)
                        if (d < nearDist) { nearDist = d; nearest = u }
                    }
                    nearest?.let { it.disabledTimer = MIRROR_DISABLE_DURATION }
                    enemy.lastMirrorDamage = 0f
                }
            }
            // GRAVITY: 실제 공격속도 감소는 updateUnits의 applyGravityEffect()에서 처리
            // (updateEnemies가 updateUnits보다 먼저 실행되어 applyGroupBonus가 덮어쓰므로)
            BossModifier.GRAVITY -> { /* gravity position tracked via enemy.position */ }
            // ADAPTIVE: 5초마다 많이 받은 데미지 타입 저항
            BossModifier.ADAPTIVE -> {
                enemy.adaptiveCheckTimer -= dt
                if (enemy.adaptiveCheckTimer <= 0f) {
                    enemy.adaptiveCheckTimer = ADAPTIVE_CHECK_INTERVAL
                    enemy.adaptiveResistPhysical = enemy.adaptivePhysicalDmg >= enemy.adaptiveMagicDmg
                    // 누적 리셋
                    enemy.adaptivePhysicalDmg = 0f
                    enemy.adaptiveMagicDmg = 0f
                }
            }
            // MINION_RUSH: 호위대 관리 (guardsAlive는 Enemy.takeDamage에서 감소)
            BossModifier.MINION_RUSH -> {
                // 호위대 생존 수 실시간 갱신 (가드 참조가 끊어졌을 때 대비)
                var aliveGuards = 0
                enemies.forEach { e ->
                    if (e.alive && e.isBossGuard && e.guardBossRef === enemy) aliveGuards++
                }
                enemy.guardsAlive = aliveGuards
            }
            else -> {}
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

        // AbilityEngine periodic and aura ticks
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

            // NEW: Behavior-based update path ??units with a behavior delegate to it
            if (unit.behavior != null) {
                applyGroupBonus(unit)
                // MIRROR 반사 디버프 감소 (behavior 유닛은 GameUnit.update() 안 타므로 여기서 처리)
                if (unit.disabledTimer > 0f) unit.disabledTimer = (unit.disabledTimer - dt).coerceAtLeast(0f)
                unit.behavior?.update(unit, dt) { pos, range ->
                    findNearestEnemy(pos, range)
                }
                if (unit.attackAnimTimer > 0f) unit.attackAnimTimer = (unit.attackAnimTimer - dt).coerceAtLeast(0f)
                if (unit.skillAnimTimer > 0f) unit.skillAnimTimer = (unit.skillAnimTimer - dt).coerceAtLeast(0f)
                if (unit.critAnimTimer > 0f) unit.critAnimTimer = (unit.critAnimTimer - dt).coerceAtLeast(0f)
                // Clamp position ??tanks in MOVING/BLOCKING can go to the enemy path area
                val isTankChasing = unit.state == UnitState.MOVING || unit.state == UnitState.BLOCKING
                if (isTankChasing) {
                    unit.position.x = unit.position.x.coerceIn(pathLeft, pathRight)
                    unit.position.y = unit.position.y.coerceIn(pathTop, pathBottom)
                } else {
                    unit.position.x = unit.position.x.coerceIn(Grid.FIELD_MIN_X, Grid.FIELD_MAX_X)
                    unit.position.y = unit.position.y.coerceIn(Grid.FIELD_MIN_Y, Grid.FIELD_MAX_Y)
                }
                // For behavior-based units, use behavior's canAttack() (interface default = true)
                val canFire = unit.behavior?.canAttack() == true && unit.disabledTimer <= 0f
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
                        // Projectile ??melee slash uses fast projectile, ranged uses normal
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


    /** Called from onProjectileHit ??apply data-driven on-hit ability for the attacking unit. */
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

                // Additional multishot spawned by roguelike effect
                val sourceUnit = if (proj.sourceUnitId >= 0) grid.getUnit(proj.sourceUnitId) else null
                applyRoguelikeOnHit(target, proj.damage, proj.isMagic, sourceUnit?.effectiveATK())

                val nx = target.position.x / W
                val ny = target.position.y / H
                BattleBridge.onDamageDealt(nx, ny, proj.damage.toInt(), proj.isCrit)
                // Melee slash projectile hit effect
                if (proj.speed >= 800f) {
                    val angle = atan2(target.position.y - proj.sourcePos.y, target.position.x - proj.sourcePos.x)
                    BattleBridge.onMeleeHit(nx, ny, proj.family.coerceIn(0, 5), proj.isCrit, angle)
                    tryPlayAttackSfx(proj.isCrit, AttackRange.MELEE, if (proj.isMagic) DamageType.MAGIC else DamageType.PHYSICAL)
                } else {
                    // Release projectile when it expires or loses its target
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
        // Snapshot publishing uses preallocated buffers and skips empty frames cheaply
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
        val unitAtk = unit.effectiveATK()
        val baseAtk = unitAtk * upgradeAtkMult * synergyAtkMult * roguelikeTotalAtk(target.isBoss) * (1f + relicAtkBonus) * familyUpgradeBonus * gradeBonus * advantageMult
        val dmg = baseAtk * (if (isCrit) critMultiplier else 1f) *
            (if (isMagic) (1f + relicMagicDmg) else 1f) *
            (if (!isMagic && relicArmorPen > 0f) (1f + relicArmorPen * 0.5f) else 1f) *
            bossMult

        if (unit.attackRange == AttackRange.MELEE) {
            // Pet attack: direct damage plus optional DOT
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
            applyRoguelikeOnHit(target, dmg, isMagic, unitAtk)
        } else {
            // Pet support buff application
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
                // PHANTOM: 투명 상태일 때 타겟팅 불가
                if (enemy.hasModifier(BossModifier.PHANTOM) && enemy.phantomActive) return@forEach
                // MINION_RUSH: 호위대 살아있으면 보스 타겟 불가
                if (enemy.bossModifier == BossModifier.MINION_RUSH && enemy.guardsAlive > 0) return@forEach
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
            probabilityEngine.rollGrade()
        }
        val selectedRaces = BattleBridge.selectedRaces.value
        val candidates = blueprintRegistry.findByRacesAndGradeAndSummonable(selectedRaces, grade)
        val selected = BattleSummonPlanner.rollWeightedBlueprint(candidates) ?: return

        // Try stacking by same unit first, then find empty slot
        var targetSlot = grid.findStackableSlot(selected.id)
        if (targetSlot < 0) {
            targetSlot = grid.findEmpty()
            if (targetSlot < 0) return  // No available slot
        }

        sp -= effectiveSummonCost
        summonCount++
        summonCost = (BASE_SUMMON_COST + summonCount * SUMMON_COST_INCREMENT).coerceAtMost(MAX_SUMMON_COST)

        // Roguelike summon upgrade: 7% chance to upgrade by one grade
        val finalSelected = BattleSummonPlanner.maybeUpgradeSummon(
            selected = selected,
            selectedRaces = selectedRaces,
            blueprintRegistry = blueprintRegistry,
            summonUpgradeEnabled = roguelikeSummonUpgrade,
        )

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

    /** GRAVITY 보스 위치 캐시 — updateEnemies에서 매 프레임 갱신 */
    private var gravityBossPos: com.jay.jaygame.engine.math.Vec2? = null

    private fun applyGroupBonus(unit: GameUnit) {
        val grp = UnitUpgradeSystem.gradeToGroup(unit.grade)
        unit.groupAtkBonus = groupAtkBonusCache[grp]
        unit.spdMultiplier = upgradeSpdMult * synergySpdMult * roguelikeSpdMult * (1f + groupSpdBonusCache[grp])

        // GRAVITY: 보스 중력장 범위 내 유닛 공격속도 30% 감소
        val gPos = gravityBossPos
        if (gPos != null) {
            val d = unit.position.distanceSqTo(gPos)
            if (d <= GRAVITY_RANGE * GRAVITY_RANGE) {
                unit.spdMultiplier *= GRAVITY_SPEED_REDUCTION
            }
        }

        val darknessRangeMult = if (waveSystem.currentConfig.specialWave == SpecialWaveType.DARKNESS)
            DARKNESS_RANGE_MULT else 1f
        unit.range = unit.baseRange * upgradeRangeMult * roguelikeRangeMult * darknessRangeMult
    }

    private fun refreshGroupBonusCache() {
        for (g in 0 until UnitUpgradeSystem.GROUP_COUNT) {
            groupAtkBonusCache[g] = UnitUpgradeSystem.getTotalAtkBonus(groupUpgradeLevels[g])
            groupSpdBonusCache[g] = UnitUpgradeSystem.getTotalSpdBonus(groupUpgradeLevels[g])
        }
    }

    /** Acquire a unit from pool, init from blueprint. Does NOT place on grid.
     *  Position is set by grid.placeUnit() ??slotCenter(). */
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

    /** Upgrade one grade group and refresh cached bonuses. */
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
        // Swap all stacked units between two slots
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
            4 -> { /* SP ?뚮났 ?낃렇?덉씠???쒓굅 ??肄붿씤 ?쒖뒪?쒖뿉?쒕뒗 遺덊븘??*/ }
        }
    }

    // endregion

    // region State Sync (Engine ??Compose)

    private fun pushStateToCompose() {
        statePublisher.publishBattleState(
            waveSystem = waveSystem,
            maxWaves = maxWaves,
            defeatEnemyCount = DEFEAT_ENEMY_COUNT,
            enemyCount = enemies.activeCount,
            sp = sp,
            elapsedTime = elapsedTime,
            stateOrdinal = state.ordinal,
            summonCost = summonCost,
            isBossRound = isBossRound,
            waveDelayRemaining = if (state == State.WaveDelay) waveDelayTimer else 0f,
        )
        statePublisher.publishEnemyPositions(enemies, W, H)
        statePublisher.publishUnitPositions(units, grid, W, H)
        statePublisher.publishProjectiles(projectiles, W, H)
        statePublisher.publishLuckyStones(luckyStones)
        statePublisher.publishGridState(grid, getMergeableTiles())
    }


    // endregion

    // region Battle End & Validation
    private fun onBattleEnd(victory: Boolean) {
        SfxManager.play(if (victory) SoundEvent.Victory else SoundEvent.Defeat)
        val summary = BattleOutcomeSummaryCalculator.calculate(
            victory = victory,
            stageId = stageId,
            difficulty = difficulty,
            currentWave = waveSystem.currentWave,
            elapsedTime = elapsedTime,
            maxWaves = maxWaves,
            hpEverLost = hpEverLost,
            isDungeonMode = isDungeonMode,
            dungeonDef = dungeonDef,
            relicManager = relicManager,
        )
        BattleBridge.onBattleEnd(
            victory, waveSystem.currentWave + 1, summary.goldEarned, summary.trophyChange,
            killCount, mergeCount, summary.cardsEarned, summary.noHpLost, summary.fastClear,
            relicDropId = summary.relicDropId,
            relicDropGrade = summary.relicDropGrade,
        )
    }
    // endregion
}
