@file:Suppress("DEPRECATION")
package com.example.jaygame.engine

import android.util.Log
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.DungeonDef
import com.example.jaygame.data.GameData
// TODO(Task18): Remove these legacy imports once requestMerge/requestBuyUnit
//  are fully migrated to blueprint-based paths (tryMergeBlueprint, etc.)
import com.example.jaygame.data.UNIT_DEFS
import com.example.jaygame.data.unitFamilyOf
import com.example.jaygame.data.unitGradeOf
import com.example.jaygame.engine.behavior.BehaviorFactory
import com.example.jaygame.engine.behavior.BehaviorRegistration
import com.example.jaygame.engine.math.GameRect
import com.example.jaygame.engine.math.Vec2
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.sqrt

class BattleEngine(
    private val stageId: Int,
    private val difficulty: Int,
    private val maxWaves: Int,
    gameData: GameData? = null,
    initialPity: Int = 0,
) {
    companion object {
        const val W = 720f
        const val H = 720f
        const val FIXED_DT = 1f / 60f
        const val MAX_ENEMIES = 256
        const val MAX_UNITS = 128
        const val MAX_PROJECTILES = 512
        const val DEFEAT_ENEMY_COUNT = 100
        const val SP_REGEN_PER_SEC = 2f
        const val BASE_SUMMON_COST = 35
        const val WAVE_DELAY = 3f
    }

    enum class State { WaveDelay, Playing, Victory, Defeat }
    var state = State.WaveDelay; private set

    var relicManager: RelicManager? = gameData?.let { RelicManager(it) }
    val petSystem = PetBattleSystem().also { ps -> gameData?.let { ps.init(it) } }

    // 던전 모드
    var isDungeonMode: Boolean = false
    var dungeonDef: DungeonDef? = null

    // 가족 영구 강화 레벨
    private val familyUpgradeLevels: Map<Int, Int> = gameData?.let { data ->
        com.example.jaygame.data.UnitFamily.entries.associate { family ->
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

    val enemies = ObjectPool(MAX_ENEMIES, { Enemy() }) { it.reset() }
    val units = ObjectPool(MAX_UNITS, { GameUnit() }) { it.reset() }
    val projectiles = ObjectPool(MAX_PROJECTILES, { Projectile() }) { it.reset() }
    val zones = ObjectPool(32, { ZoneEffect() }) { it.reset() }

    val grid = Grid()
    var waveSystem = WaveSystem(maxWaves, difficulty); private set
    val spatialHash = SpatialHash<Enemy>(64f)
    private val auraTicks = FloatArray(MAX_UNITS)

    var sp = 100f; private set
    var summonCost = BASE_SUMMON_COST; private set
    var elapsedTime = 0f; private set

    private var waveDelayTimer = WAVE_DELAY
    private var isBossRound = false

    private var killCount = 0
    private var mergeCount = 0
    private var peakEnemyCount = 0
    private var hpEverLost = false // Track if player ever took HP damage

    private val upgradeLevels = IntArray(5)
    private var upgradeAtkMult = 1f
    private var upgradeSpdMult = 1f
    private var upgradeCritRate = 0f
    private var upgradeRangeMult = 1f
    private var upgradeSpRegen = 0f

    private var gridPushTimer = 0f

    // PERF: Cached mergeable tiles — recalculated only on grid changes (summon/merge/sell/swap)
    private var mergeableTilesCache: Set<Int> = emptySet()
    private var mergeableDirty = true

    private fun invalidateMergeCache() { mergeableDirty = true }

    private fun getMergeableTiles(): Set<Int> {
        if (mergeableDirty) {
            mergeableTilesCache = MergeSystem.findMergeableTilesByBlueprint(grid, BlueprintRegistry.instance)
            mergeableDirty = false
        }
        return mergeableTilesCache
    }

    // Role synergy cache — refreshed alongside family synergies
    private var roleSynergyCache: Map<UnitRole, RoleSynergySystem.RoleSynergyBonus> = emptyMap()

    // PERF-02: Scratch lists for toActiveList replacements
    private val activeUnitsScratch = ArrayList<GameUnit>(MAX_UNITS)
    private val activeEnemiesScratch = ArrayList<Enemy>(MAX_ENEMIES)

    // PERF-03: Pre-allocated dead/scratch lists (cleared each frame instead of re-created)
    private val deadEnemies = ArrayList<Enemy>(64)
    private val deadProjectiles = ArrayList<Projectile>(32)
    private val deadZones = ArrayList<ZoneEffect>(8)
    private val splitQueue = ArrayList<Enemy>(16)

    // PERF-01: Pre-allocated buffers for pushStateToCompose
    private val enemyXBuf = FloatArray(MAX_ENEMIES)
    private val enemyYBuf = FloatArray(MAX_ENEMIES)
    private val enemyTypeBuf = IntArray(MAX_ENEMIES)
    private val enemyHpBuf = FloatArray(MAX_ENEMIES)
    private val enemyBuffBuf = IntArray(MAX_ENEMIES)

    private val unitXBuf = FloatArray(MAX_UNITS)
    private val unitYBuf = FloatArray(MAX_UNITS)
    private val unitDefIdBuf = IntArray(MAX_UNITS)
    private val unitGradeBuf = IntArray(MAX_UNITS)
    private val unitLevelBuf = IntArray(MAX_UNITS)
    private val unitAttackingBuf = BooleanArray(MAX_UNITS)
    private val unitTileBuf = IntArray(MAX_UNITS)
    private val unitBlueprintIdBuf = Array(MAX_UNITS) { "" }
    private val unitFamiliesListBuf = Array<List<com.example.jaygame.data.UnitFamily>>(MAX_UNITS) { emptyList() }
    private val unitRoleBuf = Array(MAX_UNITS) { UnitRole.RANGED_DPS }
    private val unitAttackRangeBuf = Array(MAX_UNITS) { AttackRange.RANGED }
    private val unitDamageTypeBuf = Array(MAX_UNITS) { DamageType.PHYSICAL }
    private val unitCategoryBuf = Array(MAX_UNITS) { UnitCategory.NORMAL }
    private val unitHpBuf = FloatArray(MAX_UNITS)
    private val unitMaxHpBuf = FloatArray(MAX_UNITS)
    private val unitStateBuf = Array(MAX_UNITS) { UnitState.IDLE }
    private val unitHomeXBuf = FloatArray(MAX_UNITS)
    private val unitHomeYBuf = FloatArray(MAX_UNITS)

    private val projSrcXBuf = FloatArray(MAX_PROJECTILES)
    private val projSrcYBuf = FloatArray(MAX_PROJECTILES)
    private val projDstXBuf = FloatArray(MAX_PROJECTILES)
    private val projDstYBuf = FloatArray(MAX_PROJECTILES)
    private val projTypeBuf = IntArray(MAX_PROJECTILES)

    private val gridIdBuf = IntArray(Grid.TOTAL)
    private val gridGradeBuf = IntArray(Grid.TOTAL)
    private val gridFamilyBuf = IntArray(Grid.TOTAL)
    private val gridCanMergeBuf = BooleanArray(Grid.TOTAL)
    private val gridLevelBuf = IntArray(Grid.TOTAL)
    private val gridBlueprintIdBuf = Array(Grid.TOTAL) { "" }
    private val gridFamiliesListBuf = Array<List<com.example.jaygame.data.UnitFamily>>(Grid.TOTAL) { emptyList() }
    private val gridRoleBuf = Array(Grid.TOTAL) { UnitRole.RANGED_DPS }

    // NEW: Blueprint-based system (singleton, loaded at app startup)
    val blueprintRegistry = BlueprintRegistry.instance

    init {
        // Ensure all behavior factories are registered
        BehaviorRegistration.ensureRegistered()
    }

    // Square path around grid (480x480 centered at 640,360)
    // Grid: (400,120)~(880,600). Path margin: 60px outside grid.
    // Path outer: (340,60)~(940,660) = 600x600 square
    // Midline of path strip: centered in visual path (PATH_MARGIN=80, so 40px from grid edge)
    // Bottom compensates for cliff (25px): center of visible strip = 40 + 25 = 65px from grid
    private val pathLeft = Grid.ORIGIN_X - 40f
    private val pathTop = Grid.ORIGIN_Y - 40f
    private val pathRight = Grid.ORIGIN_X + Grid.GRID_W + 40f
    private val pathBottom = Grid.ORIGIN_Y + Grid.GRID_H + 65f // cliff 25px 보상

    // Corner radius for smooth turns (8 interpolation points per corner)
    private val cornerR = 30f

    val enemyPath: List<Vec2> = buildList {
        val steps = 8
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

    fun start(scope: CoroutineScope) {
        // Reinitialize wave system for dungeon mode
        if (isDungeonMode && dungeonDef != null) {
            val forceBoss = dungeonDef?.type == com.example.jaygame.data.DungeonType.BOSS_RUSH
            waveSystem = WaveSystem(maxWaves, difficulty, forceBoss = forceBoss)
        }
        // Initialize synergy as empty — will be refreshed when units are placed
        AbilitySystem.activeSynergy = SynergySystem.SynergyBonus()
        UniqueAbilitySystem.zonePool = zones
        // Wire relic bonuses into subsystems
        relicManager?.let { rm ->
            MergeSystem.luckyMergeBonus = rm.totalLuckyMergeBonus()
            UniqueAbilitySystem.cooldownReduction = rm.totalCooldownReduction()
        }
        validateLayout()
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
        // SP regen scales slightly with wave progression (+10% per 10 waves)
        val waveSpBonus = 1f + waveSystem.currentWave * 0.01f
        sp += (SP_REGEN_PER_SEC + upgradeSpRegen) * waveSpBonus * dt

        // Auto-summon & auto-merge
        if (BattleBridge.autoSummon.value && state == State.Playing) {
            // Auto-merge first: merge any available units
            val mergeable = getMergeableTiles()
            if (mergeable.isNotEmpty()) {
                // Auto-merge: prioritize lowest grade first (clear space for better units)
                val bestMerge = mergeable.minByOrNull { tileIdx ->
                    grid.getUnit(tileIdx)?.grade ?: Int.MAX_VALUE
                } ?: mergeable.first()
                requestMerge(bestMerge)
            }
            // Auto-summon: summon when SP is enough
            val effectiveCost = (BASE_SUMMON_COST * (1f - (relicManager?.totalSummonCostReduction() ?: 0f))).toInt().coerceAtLeast(BASE_SUMMON_COST / 2)
            if (sp >= effectiveCost && units.activeCount < Grid.TOTAL) {
                requestSummonBlueprint()
            }
        }

        when (state) {
            State.WaveDelay -> {
                waveDelayTimer -= dt
                if (waveDelayTimer <= 0f) {
                    waveSystem.startWave(waveSystem.currentWave)
                    val config = waveSystem.getWaveConfig(waveSystem.currentWave)
                    isBossRound = config.isBoss
                    sp += relicManager?.totalWaveStartSp() ?: 0f
                    state = State.Playing
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
                        var released = 0
                        val excessList = mutableListOf<Enemy>()
                        enemies.forEach { if (it.alive) excessList.add(it) }
                        excessList.forEach { enemies.release(it); released++ }
                        BattleBridge.onPhoenixRevive()
                    } else {
                        state = State.Defeat
                        onBattleEnd(false)
                    }
                }

                // All enemies spawned & killed → immediate next wave
                if (!waveSystem.waveComplete && waveSystem.allSpawned && enemies.activeCount == 0) {
                    waveSystem.forceComplete()
                }

                // Boss timeout → instant defeat
                if (isBossRound && waveSystem.waveComplete) {
                    var bossAlive = false
                    enemies.forEach { if (it.alive) bossAlive = true }
                    if (bossAlive) {
                        state = State.Defeat
                        onBattleEnd(false)
                        return
                    }
                }

                // Wave time expired → next wave (previous enemies stay alive)
                if (waveSystem.waveComplete) {
                    // Wave clear bonus: SP + gold
                    val waveClearSP = 10f + waveSystem.currentWave * 0.5f
                    sp += waveClearSP
                    val waveClearGold = 5 + waveSystem.currentWave
                    BattleBridge.onGoldPickup(0.5f, 0.5f, waveClearGold)

                    if (waveSystem.isLastWave) {
                        state = State.Victory
                        onBattleEnd(true)
                    } else {
                        waveSystem.advanceWave()
                        waveDelayTimer = WAVE_DELAY
                        state = State.WaveDelay
                    }
                }
            }
            State.Victory, State.Defeat -> { /* frozen */ }
        }
        gridPushTimer += dt
    }

    private fun updateSpawning(dt: Float) {
        val toSpawn = waveSystem.update(dt)
        val config = waveSystem.getWaveConfig(waveSystem.currentWave)
        repeat(toSpawn) {
            val enemy = enemies.acquire() ?: return
            val isElite = config.eliteChance > 0f && Math.random() < config.eliteChance
            enemy.init(
                hp = config.hp * (if (isElite) 2f else 1f),
                speed = config.speed * (if (isElite) 1.1f else 1f),
                armor = config.armor * (if (isElite) 1.5f else 1f),
                magicResist = config.magicResist * (if (isElite) 1.3f else 1f),
                type = config.enemyType, startPos = enemyPath.first().copy(),
                ccResistance = config.ccResistance + if (isElite) 0.1f else 0f,
            )
            // High difficulty: elite enemies get regeneration
            if (isElite && difficulty >= 3) {
                enemy.bossModifier = BossModifier.REGENERATION
                enemy.regenTimer = 8f
            }
            // Assign boss modifier
            val isTrueBoss = (waveSystem.currentWave + 1) % 10 == 0
            val isDungeonBoss = isDungeonMode && dungeonDef?.type == com.example.jaygame.data.DungeonType.BOSS_RUSH
            if (config.isBoss && (isTrueBoss || isDungeonBoss)) {
                val modifier = if (isDungeonBoss) {
                    BossModifier.entries.random() // Random modifier each boss rush wave
                } else {
                    getBossModifier(stageId, waveSystem.currentWave)
                }
                enemy.bossModifier = modifier
                if (modifier == BossModifier.SWIFT) {
                    enemy.baseSpeed *= 2f
                    enemy.speed = enemy.baseSpeed
                }
                if (modifier == BossModifier.SHIELDED) {
                    enemy.shieldTimer = 5f // Start with shield down
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
                    val cmdRect = com.example.jaygame.engine.math.GameRect(
                        enemy.position.x - 200f, enemy.position.y - 200f, 400f, 400f,
                    )
                    spatialHash.query(cmdRect).forEach { nearby ->
                        if (nearby !== enemy && nearby.alive && !nearby.buffs.hasBuff(BuffType.Shield)) {
                            nearby.buffs.addBuff(BuffType.Shield, nearby.maxHp * 0.02f, 2f)
                        }
                    }
                }
                // REGENERATION: heal 5% maxHp every 10 seconds
                BossModifier.REGENERATION -> {
                    enemy.regenTimer -= dt
                    if (enemy.regenTimer <= 0f) {
                        enemy.hp = (enemy.hp + enemy.maxHp * 0.05f).coerceAtMost(enemy.maxHp)
                        enemy.regenTimer = 10f
                    }
                }
                // BERSERKER: triple attack speed (move speed) below 50% HP
                BossModifier.BERSERKER -> {
                    if (!enemy.berserkerActivated && enemy.hpRatio < 0.5f) {
                        enemy.berserkerActivated = true
                        enemy.speed = enemy.baseSpeed * 1.5f // Faster movement in berserk
                    }
                }
                // SPLITTER: spawn 2 mini-bosses at 50% HP
                BossModifier.SPLITTER -> {
                    if (!enemy.splitterTriggered && enemy.hpRatio < 0.5f) {
                        enemy.splitterTriggered = true
                        splitQueue.add(enemy)
                    }
                }
                // SHIELDED: cycle shield on/off (3s active, 5s cooldown)
                BossModifier.SHIELDED -> {
                    enemy.shieldTimer -= dt
                    if (enemy.shieldTimer <= 0f) {
                        enemy.shieldActive = !enemy.shieldActive
                        enemy.shieldTimer = if (enemy.shieldActive) 3f else 5f
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
        enemies.forEach { if (!it.alive) deadEnemies.add(it) }
        val poisonSpread = AbilitySystem.activeSynergy.specialEffect == SynergySystem.SpecialEffect.POISON_SPREAD
        deadEnemies.forEach { dead ->
            val deathX = dead.position.x / W
            val deathY = dead.position.y / H

            // POISON_SPREAD synergy: on death, spread poison to nearby enemies
            if (poisonSpread && dead.buffs.hasBuff(BuffType.DoT)) {
                val spreadRect = com.example.jaygame.engine.math.GameRect(
                    dead.position.x - 100f, dead.position.y - 100f, 200f, 200f,
                )
                spatialHash.query(spreadRect).forEach { nearby ->
                    if (nearby.alive && nearby !== dead) {
                        nearby.buffs.addBuff(BuffType.DoT, 8f, 3f)
                    }
                }
            }

            val wasElite = dead.maxHp > waveSystem.getWaveConfig(waveSystem.currentWave).hp * 1.5f

            // Challenger difficulty: enemy death enrages nearby enemies (+15% speed permanently)
            if (difficulty >= 4) {
                val enrageRect = com.example.jaygame.engine.math.GameRect(
                    dead.position.x - 80f, dead.position.y - 80f, 160f, 160f,
                )
                spatialHash.query(enrageRect).forEach { nearby ->
                    if (nearby.alive && nearby !== dead) {
                        nearby.speed = (nearby.speed * 1.15f).coerceAtMost(nearby.baseSpeed * 2f)
                    }
                }
            }

            enemies.release(dead)
            killCount++
            // SP recovery on kill (0.5 base, 1.5 for elite)
            sp += if (wasElite) 1.5f else 0.5f
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
        UniqueAbilitySystem.update(unitList, dt, activeEnemies)

        units.forEach { unit ->
            if (!unit.alive && unit.state != UnitState.RESPAWNING) return@forEach

            // NEW: Behavior-based update path — units with a behavior delegate to it
            if (unit.behavior != null) {
                val roleBonus = getRoleBonus(unit)
                // Apply role synergy range multiplier to enemy detection
                unit.behavior?.update(unit, dt) { pos, range ->
                    findNearestEnemy(pos, range * roleBonus.rangeMultiplier)
                }
                // Clamp position within field bounds (behaviors move units directly)
                unit.position.x = unit.position.x.coerceIn(Grid.FIELD_MIN_X, Grid.FIELD_MAX_X)
                unit.position.y = unit.position.y.coerceIn(Grid.FIELD_MIN_Y, Grid.FIELD_MAX_Y)
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
                    val synergy = AbilitySystem.activeSynergy

                    // Re-evaluate crit with relic crit chance bonus
                    val boostedCrit = if (result.isCrit) true
                        else if (roleBonus.critBonus + relicCritChance > 0f)
                            Math.random() < (roleBonus.critBonus + relicCritChance)
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

                    val familyUpgradeBonus = 1f + (familyUpgradeLevels[unit.family] ?: 0) * 0.001f
                    val gradeBonus = DamageCalculator.gradeMultiplier(unit.grade)
                    val advantageMult = DamageCalculator.familyAdvantageMultiplier(unit.family, target.type)
                    val boostedDamage = result.damage * critBoost *
                        upgradeAtkMult * (1f + relicAtkBonus) * synergy.atkMultiplier *
                        roleBonus.atkMultiplier * familyUpgradeBonus * gradeBonus * advantageMult *
                        (if (result.isMagic) (1f + relicMagicDmg) else 1f) *
                        (if (!result.isMagic && relicArmorPen > 0f) (1f + relicArmorPen * 0.5f) else 1f)
                    val finalBoostedDamage = boostedDamage

                    if (result.isInstant) {
                        // Melee instant damage — use Enemy.takeDamage() for proper boss/buff handling
                        val finalDmg = target.takeDamage(finalBoostedDamage, result.isMagic, unit.range)
                        val nx = target.position.x / W
                        val ny = target.position.y / H
                        BattleBridge.onDamageDealt(nx, ny, finalDmg.toInt(), boostedCrit)
                    } else {
                        // Projectile — melee slash uses fast projectile, ranged uses normal
                        val isSlash = unit.attackRange == AttackRange.MELEE
                        val projSpeed = if (isSlash) 900f else 400f
                        val proj = projectiles.acquire()
                        if (proj != null) {
                            proj.init(
                                from = unit.position.copy(), target = target,
                                damage = finalBoostedDamage, speed = projSpeed,
                                type = unit.family.coerceIn(0, 5),
                                isMagic = result.isMagic, isCrit = boostedCrit,
                                sourceUnitId = unit.tileIndex,
                                abilityType = unit.abilityType,
                                abilityValue = unit.abilityValue,
                                grade = unit.grade, family = unit.family,
                                attackerRange = unit.range * roleBonus.rangeMultiplier,
                            )
                        }
                    }
                    unit.onAttack()
                }
            } else {
                // Legacy update path — apply role synergy range multiplier
                val legacyRoleBonus = getRoleBonus(unit)
                unit.update(dt) { pos, range ->
                    findNearestEnemy(pos, range * legacyRoleBonus.rangeMultiplier)
                }
                if (unit.canAttack()) {
                    fireProjectile(unit)
                    unit.onAttack()
                }
            }
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
                val nx = target.position.x / W
                val ny = target.position.y / H
                BattleBridge.onDamageDealt(nx, ny, proj.damage.toInt(), proj.isCrit)
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

        // Push zone data to UI
        val zCount = zones.activeCount
        if (zCount > 0) {
            val zxs = FloatArray(zCount)
            val zys = FloatArray(zCount)
            val zradii = FloatArray(zCount)
            val zfamilies = IntArray(zCount)
            var zi = 0
            zones.forEach { zone ->
                if (zone.alive && zi < zCount) {
                    zxs[zi] = zone.position.x / W
                    zys[zi] = zone.position.y / H
                    zradii[zi] = zone.radius / W
                    zfamilies[zi] = zone.family
                    zi++
                }
            }
            BattleBridge.updateZoneData(zxs, zys, zradii, zfamilies, zi)
        } else {
            BattleBridge.updateZoneData(FloatArray(0), FloatArray(0), FloatArray(0), IntArray(0), 0)
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
        val proj = projectiles.acquire() ?: return
        val rm = relicManager
        val relicAtkBonus = rm?.totalAtkPercent() ?: 0f
        val relicCritChance = rm?.totalCritChanceBonus() ?: 0f
        val relicCritDmg = rm?.totalCritDamageBonus() ?: 0f
        val relicArmorPen = rm?.totalArmorPenPercent() ?: 0f
        val relicMagicDmg = rm?.totalMagicDmgPercent() ?: 0f

        // Synergy bonus (field-based)
        val synergy = AbilitySystem.activeSynergy
        val roleBonus = getRoleBonus(unit)

        val isCrit = Math.random() < (0.05 + upgradeCritRate + relicCritChance + roleBonus.critBonus)
        val critMultiplier = 2f + relicCritDmg
        val isMagic = unit.family == 1 || unit.family == 4
        val familyUpgradeBonus = 1f + (familyUpgradeLevels[unit.family] ?: 0) * 0.001f // +0.1% per level
        val gradeBonus = DamageCalculator.gradeMultiplier(unit.grade)
        val advantageMult = DamageCalculator.familyAdvantageMultiplier(unit.family, target.type)
        val baseAtk = unit.effectiveATK() * upgradeAtkMult * (1f + relicAtkBonus) * synergy.atkMultiplier * roleBonus.atkMultiplier * familyUpgradeBonus * gradeBonus * advantageMult
        val dmg = baseAtk * (if (isCrit) critMultiplier else 1f) *
            (if (isMagic) (1f + relicMagicDmg) else 1f) *
            (if (!isMagic && relicArmorPen > 0f) (1f + relicArmorPen * 0.5f) else 1f)

        proj.init(
            from = unit.position.copy(), target = target,
            damage = dmg, speed = 400f,
            type = unit.family.coerceIn(0, 5),
            isMagic = isMagic, isCrit = isCrit,
            sourceUnitId = unit.tileIndex,
            abilityType = unit.abilityType,
            abilityValue = unit.abilityValue,
            grade = unit.grade, family = unit.family,
            attackerRange = unit.range * roleBonus.rangeMultiplier,
        )
    }

    private fun findNearestEnemy(pos: Vec2, range: Float): Enemy? {
        val rect = GameRect(pos.x - range, pos.y - range, range * 2, range * 2)
        var nearest: Enemy? = null
        var nearestDist = Float.MAX_VALUE
        spatialHash.query(rect).forEach { enemy ->
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

    // ── Player Actions ──

    fun requestSummonBlueprint(gradeOverride: UnitGrade? = null) {
        val effectiveSummonCost = (BASE_SUMMON_COST * (1f - (relicManager?.totalSummonCostReduction() ?: 0f))).toInt().coerceAtLeast(BASE_SUMMON_COST / 2)
        if (sp < effectiveSummonCost || units.activeCount >= Grid.TOTAL) return
        sp -= effectiveSummonCost

        val grade = if (gradeOverride != null) {
            gradeOverride
        } else {
            val (rawGrade, resetPity) = probabilityEngine.rollGradeWithPity(currentPity)
            currentPity = if (resetPity) 0 else (currentPity + 1).coerceAtMost(100)
            BattleBridge.updateUnitPullPity(currentPity)
            UnitGrade.entries.getOrElse(rawGrade) { UnitGrade.entries.first() }
        }

        val candidates = blueprintRegistry.findByGradeAndSummonable(grade)
        if (candidates.isEmpty()) return

        // Weight-based selection
        val totalWeight = candidates.sumOf { it.summonWeight }
        if (totalWeight <= 0) return
        var roll = (Math.random() * totalWeight).toInt()
        var selected: UnitBlueprint? = null
        for (bp in candidates) {
            roll -= bp.summonWeight
            if (roll <= 0) { selected = bp; break }
        }
        selected ?: return

        val tileIndex = grid.findEmpty()
        if (tileIndex < 0) return

        val unit = units.acquire() ?: return
        unit.initFromBlueprint(selected)
        unit.tileIndex = tileIndex
        unit.position = Grid.FIELD_CENTER.copy()
        unit.homePosition = Grid.FIELD_CENTER.copy()
        unit.behavior = BehaviorFactory.create(selected.behaviorId)
        grid.placeUnit(tileIndex, unit)
        invalidateMergeCache()
        refreshSynergies()
        BattleBridge.onSummonResult(
            unitDefId = -1,
            grade = selected.grade.ordinal,
            blueprintId = selected.id,
        )
    }

    // Pre-allocated containers for refreshSynergies() — avoid per-call allocation
    private val roleSynergyCacheMut = HashMap<UnitRole, RoleSynergySystem.RoleSynergyBonus>(5)
    private val roleCountsScratch = HashMap<UnitRole, Int>(5)

    // Dedicated scratch list for refreshSynergies — separate from activeUnitsScratch
    // to avoid ConcurrentModificationException when UI thread triggers refreshSynergies
    // while game loop is using activeUnitsScratch.
    private val synergyScratch = ArrayList<GameUnit>(MAX_UNITS)

    /** Recalculate field-based synergies from currently alive units.
     *  Picks the dominant family (highest count >= 2) for the global synergy bonus. */
    private fun refreshSynergies() {
        synergyScratch.clear()
        units.forEach { if (it.alive) synergyScratch.add(it) }

        // Family synergy — countFamilies is called once internally
        val familyCounts = SynergySystem.getActiveSynergies(synergyScratch)
        if (familyCounts.isEmpty()) {
            AbilitySystem.activeSynergy = SynergySystem.SynergyBonus()
        } else {
            val dominantFamily = familyCounts.maxByOrNull { it.value }?.key ?: return
            // Reuse cached counts from getActiveSynergies — avoid second countFamilies call
            AbilitySystem.activeSynergy = SynergySystem.getSynergyBonusFromCached(dominantFamily)
        }
        BattleBridge.updateFamilySynergies(familyCounts)

        // Role synergy — single pass: count roles + compute bonuses
        roleCountsScratch.clear()
        for (unit in synergyScratch) {
            if (unit.unitCategory != UnitCategory.SPECIAL) {
                roleCountsScratch[unit.role] = (roleCountsScratch[unit.role] ?: 0) + 1
            }
        }
        roleSynergyCacheMut.clear()
        for (role in UnitRole.entries) {
            val count = roleCountsScratch[role] ?: 0
            roleSynergyCacheMut[role] = RoleSynergySystem.getBonusByCount(role, count)
        }
        roleSynergyCache = roleSynergyCacheMut

        BattleBridge.updateRoleSynergies(roleCountsScratch)
    }

    /** Get role synergy bonus for a unit. SPECIAL category units get no bonus. */
    private fun getRoleBonus(unit: GameUnit): RoleSynergySystem.RoleSynergyBonus {
        if (unit.unitCategory == UnitCategory.SPECIAL) return RoleSynergySystem.NO_BONUS
        return roleSynergyCache[unit.role] ?: RoleSynergySystem.NO_BONUS
    }

    private fun abilityForFamily(family: Int): Pair<Int, Float> = when (family) {
        0 -> 1 to 80f      // Fire: Splash
        1 -> 2 to 0.3f     // Frost: Slow
        2 -> 3 to 10f      // Poison: DoT
        3 -> 4 to 3f       // Lightning: Chain
        4 -> 5 to 0.15f    // Support: Buff
        5 -> 10 to 50f     // Wind: Knockback
        else -> 0 to 0f
    }

    fun requestMerge(tileIndex: Int) {
        val existingUnit = grid.getUnit(tileIndex) ?: return
        if (existingUnit.unitCategory == UnitCategory.HIDDEN || existingUnit.unitCategory == UnitCategory.SPECIAL) return
        // Remember position of the merge anchor before consuming
        val mergePos = existingUnit.position.copy()
        val mergeHomePos = existingUnit.homePosition.copy()
        val result = MergeSystem.tryMergeBlueprint(grid, tileIndex, blueprintRegistry) ?: return
        result.consumedTiles.forEach { i ->
            val u = grid.removeUnit(i)
            if (u != null) units.release(u)
        }
        val bp = blueprintRegistry.findById(result.resultBlueprintId) ?: return
        val unit = units.acquire() ?: return
        unit.initFromBlueprint(bp)
        unit.tileIndex = tileIndex
        unit.position = mergePos
        unit.homePosition = mergeHomePos
        unit.behavior = BehaviorFactory.create(bp.behaviorId)
        grid.placeUnit(tileIndex, unit)
        invalidateMergeCache()
        refreshSynergies()
        mergeCount++
        BattleBridge.onMergeComplete(tileIndex, result.isLucky, -1, unit.blueprintId)
    }

    fun requestSell(tileIndex: Int) {
        val unit = grid.removeUnit(tileIndex) ?: return
        sp += 30f + unit.grade * 20f
        units.release(unit)
        invalidateMergeCache()
        refreshSynergies()
    }

    fun requestBulkSell(grade: Int): Int {
        var count = 0
        for (i in 0 until Grid.TOTAL) {
            val unit = grid.getUnit(i) ?: continue
            if (unit.grade == grade) {
                grid.removeUnit(i)
                sp += 30f + unit.grade * 20f
                units.release(unit)
                count++
            }
        }
        if (count > 0) {
            invalidateMergeCache()
            refreshSynergies()
        }
        return count
    }

    fun requestClickTile(tileIndex: Int) {
        val unit = grid.getUnit(tileIndex) ?: return
        val mergeable = getMergeableTiles()
        BattleBridge.onUnitClicked(
            tileIndex, unit.unitDefId, unit.grade, unit.family,
            tileIndex in mergeable, unit.level,
            blueprintId = unit.blueprintId,
            families = unit.families,
            role = unit.role,
            attackRange = unit.attackRange,
            damageType = unit.damageType,
            hp = unit.hp,
            maxHp = unit.maxHp,
        )
    }

    fun requestRelocate(tileIndex: Int, normX: Float, normY: Float) {
        val unit = grid.getUnit(tileIndex) ?: return
        val worldX = (normX * W).coerceIn(Grid.FIELD_MIN_X, Grid.FIELD_MAX_X)
        val worldY = (normY * H).coerceIn(Grid.FIELD_MIN_Y, Grid.FIELD_MAX_Y)
        unit.homePosition = Vec2(worldX, worldY)
        unit.position = Vec2(worldX, worldY)
    }

    fun requestSwap(from: Int, to: Int) {
        val unitA = grid.removeUnit(from)
        val unitB = grid.removeUnit(to)
        if (unitA != null) grid.placeUnit(to, unitA)
        if (unitB != null) grid.placeUnit(from, unitB)
        invalidateMergeCache()
    }

    fun requestUpgrade(tileIndex: Int) {
        val unit = grid.getUnit(tileIndex) ?: return
        if (unit.level >= 7) return
        val cost = BattleBridge.UPGRADE_COSTS.getOrElse(unit.level - 1) { 999 }
        if (sp < cost) return
        sp -= cost
        unit.level++
        BattleBridge.onUnitLevelUp(unit.position.x / W, unit.position.y / H)
    }

    var gambleLosingStreak: Int = 0
        private set

    fun requestGamble(option: GambleSystem.GambleOption): GambleSystem.GambleResult? {
        if (sp < GambleSystem.ENTRY_FEE) return null
        val luckBonus = relicManager?.totalGambleBonus() ?: 0f
        val result = GambleSystem.gamble(sp, option, luckBonus, gambleLosingStreak)
        sp = result.spAfter
        gambleLosingStreak = if (result.won) 0 else gambleLosingStreak + 1
        return result
    }

    fun requestBuyUnit(unitDefId: Int, cost: Float) {
        if (sp < cost || units.activeCount >= Grid.TOTAL) return
        val tileIndex = grid.findEmpty()
        if (tileIndex < 0) return
        val unit = units.acquire() ?: return

        // Try blueprint-based init first (preferred path)
        val def = UNIT_DEFS.find { it.id == unitDefId }
        val familyEnum = def?.family ?: com.example.jaygame.data.UnitFamily.entries[unitFamilyOf(unitDefId)]
        val gradeOrdinal = unitGradeOf(unitDefId)
        val gradeEnum = UnitGrade.entries.getOrElse(gradeOrdinal) { UnitGrade.COMMON }
        val bp = blueprintRegistry.all().find { it.grade == gradeEnum && familyEnum in it.families }

        if (bp != null) {
            unit.initFromBlueprint(bp)
            unit.tileIndex = tileIndex
            unit.position = Grid.FIELD_CENTER.copy()
            unit.homePosition = Grid.FIELD_CENTER.copy()
            unit.behavior = BehaviorFactory.create(bp.behaviorId)
        } else if (def != null) {
            // Fallback: legacy init with proper field assignments
            val family = unitFamilyOf(unitDefId)
            val abilityInfo = abilityForFamily(family)
            unit.init(
                unitDefId = unitDefId, grade = gradeOrdinal, family = family, level = 1,
                tileIndex = tileIndex, homePos = Grid.FIELD_CENTER.copy(),
                baseATK = def.baseATK.toFloat(), atkSpeed = def.baseSpeed,
                range = def.range * upgradeRangeMult,
                abilityType = abilityInfo.first, abilityValue = abilityInfo.second,
            )
            unit.position = Grid.FIELD_CENTER.copy()
            unit.families = listOf(familyEnum)
            unit.role = inferRole(familyEnum)
            UniqueAbilitySystem.initUnit(unit)
        } else {
            units.release(unit)
            return
        }

        sp -= cost
        grid.placeUnit(tileIndex, unit)
        invalidateMergeCache()
        refreshSynergies()
        BattleBridge.onSummonResult(
            unitDefId = if (bp != null) -1 else unitDefId,
            grade = gradeOrdinal,
            blueprintId = bp?.id ?: "",
        )
    }

    private fun inferRole(family: com.example.jaygame.data.UnitFamily): UnitRole = when (family) {
        com.example.jaygame.data.UnitFamily.SUPPORT -> UnitRole.SUPPORT
        com.example.jaygame.data.UnitFamily.WIND -> UnitRole.CONTROLLER
        else -> UnitRole.RANGED_DPS
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
            4 -> upgradeSpRegen = level * 1f
        }
    }

    // ── Push State to Compose ──

    private fun pushStateToCompose() {
        val mergeable = getMergeableTiles()

        BattleBridge.updateState(
            waveSystem.currentWave + 1, maxWaves,
            (DEFEAT_ENEMY_COUNT - enemies.activeCount).coerceAtLeast(0), DEFEAT_ENEMY_COUNT,
            sp, elapsedTime, state.ordinal, summonCost,
            enemies.activeCount, if (isBossRound) 1 else 0, waveSystem.timeRemaining,
        )

        // Enemy positions + buff bitmasks — reuse pre-allocated buffers
        val eCount = enemies.activeCount
        var ei = 0
        enemies.forEach { e ->
            if (ei < eCount) {
                enemyXBuf[ei] = e.position.x / W
                enemyYBuf[ei] = e.position.y / H
                enemyTypeBuf[ei] = e.type
                enemyHpBuf[ei] = e.hpRatio
                // Build buff bitmask from enemy buff container
                var bits = 0
                val hasSlow = e.buffs.hasBuff(com.example.jaygame.engine.BuffType.Slow)
                val hasDot = e.buffs.hasBuff(com.example.jaygame.engine.BuffType.DoT)
                val hasArmorBreak = e.buffs.hasBuff(com.example.jaygame.engine.BuffType.ArmorBreak)
                if (hasSlow) bits = bits or com.example.jaygame.bridge.BUFF_BIT_SLOW
                if (hasDot) bits = bits or com.example.jaygame.bridge.BUFF_BIT_DOT
                if (hasArmorBreak) bits = bits or com.example.jaygame.bridge.BUFF_BIT_ARMOR_BREAK
                // Poison = slow + dot combo
                if (hasSlow && hasDot) bits = bits or com.example.jaygame.bridge.BUFF_BIT_POISON
                // Lightning & Wind tracked via recentHitFlags
                if (e.recentHitFlags and 1 != 0) bits = bits or com.example.jaygame.bridge.BUFF_BIT_LIGHTNING
                if (e.recentHitFlags and 2 != 0) bits = bits or com.example.jaygame.bridge.BUFF_BIT_WIND
                enemyBuffBuf[ei] = bits
                ei++
            }
        }
        BattleBridge.updateEnemyPositions(enemyXBuf, enemyYBuf, enemyTypeBuf, enemyHpBuf, enemyBuffBuf, ei)

        // Unit positions — reuse pre-allocated buffers
        val uCount = units.activeCount
        var ui = 0
        units.forEach { u ->
            if (ui < uCount) {
                unitXBuf[ui] = u.position.x / W
                unitYBuf[ui] = u.position.y / H
                unitDefIdBuf[ui] = u.unitDefId
                unitGradeBuf[ui] = u.grade
                unitLevelBuf[ui] = u.level
                unitAttackingBuf[ui] = u.isAttacking
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
                ui++
            }
        }
        BattleBridge.updateUnitPositions(
            unitXBuf, unitYBuf, unitDefIdBuf, unitGradeBuf, unitLevelBuf, unitAttackingBuf, unitTileBuf, ui,
            unitBlueprintIdBuf, unitFamiliesListBuf, unitRoleBuf, unitAttackRangeBuf, unitDamageTypeBuf, unitCategoryBuf,
            unitHpBuf, unitMaxHpBuf, unitStateBuf, unitHomeXBuf, unitHomeYBuf,
        )

        // Projectiles — reuse pre-allocated buffers
        val pCount = projectiles.activeCount
        var pi = 0
        projectiles.forEach { p ->
            if (pi < pCount) {
                projSrcXBuf[pi] = p.sourcePos.x / W
                projSrcYBuf[pi] = p.sourcePos.y / H
                projDstXBuf[pi] = p.position.x / W
                projDstYBuf[pi] = p.position.y / H
                projTypeBuf[pi] = p.type
                pi++
            }
        }
        BattleBridge.updateProjectiles(projSrcXBuf, projSrcYBuf, projDstXBuf, projDstYBuf, projTypeBuf, pi)

        // Grid state — reuse pre-allocated buffers
        if (gridPushTimer >= 0.1f) {
            gridPushTimer = 0f
            for (i in 0 until Grid.TOTAL) {
                val u = grid.getUnit(i)
                if (u != null) {
                    gridIdBuf[i] = u.unitDefId
                    gridGradeBuf[i] = u.grade
                    gridFamilyBuf[i] = u.family
                    gridCanMergeBuf[i] = i in mergeable
                    gridLevelBuf[i] = u.level
                    gridBlueprintIdBuf[i] = u.blueprintId
                    gridFamiliesListBuf[i] = u.families
                    gridRoleBuf[i] = u.role
                } else {
                    gridIdBuf[i] = -1
                    gridGradeBuf[i] = 0
                    gridFamilyBuf[i] = 0
                    gridCanMergeBuf[i] = false
                    gridLevelBuf[i] = 0
                    gridBlueprintIdBuf[i] = ""
                    gridFamiliesListBuf[i] = emptyList()
                    gridRoleBuf[i] = UnitRole.RANGED_DPS
                }
            }
            BattleBridge.updateGridState(gridIdBuf, gridGradeBuf, gridFamilyBuf, gridCanMergeBuf, gridLevelBuf, gridBlueprintIdBuf, gridFamiliesListBuf, gridRoleBuf)
        }
    }

    private fun onBattleEnd(victory: Boolean) {
        val difficultyBonus = if (isDungeonMode && dungeonDef != null) {
            dungeonDef?.difficultyMultiplier ?: 1f
        } else {
            1f + difficulty * 0.25f // 초보1.0, 숙련자1.25, 고인물1.5, 썩은물1.75, 챌린저2.0
        }
        val dungeonRewardMult = if (isDungeonMode) dungeonDef?.rewardMultiplier ?: 1f else 1f
        val baseGold = if (victory) 100 + waveSystem.currentWave * 10 else waveSystem.currentWave * 5
        val relicWaveBonus = if (victory) (1f + (relicManager?.totalGoldWaveBonus() ?: 0f)) else 1f
        val goldEarned = (baseGold * difficultyBonus * relicWaveBonus * dungeonRewardMult).toInt()
        val baseTrophy = if (victory) 20 + stageId * 5 else -(10 + stageId * 3)
        val trophyChange = if (baseTrophy > 0) (baseTrophy * difficultyBonus).toInt() else baseTrophy
        val noHpLost = peakEnemyCount <= DEFEAT_ENEMY_COUNT / 5 // HP never dropped below 80%
        val fastClear = victory && elapsedTime < maxWaves * 8f // cleared quickly
        val baseCards = if (victory) 3 + stageId + difficulty else 1
        val dungeonCardBonus = if (isDungeonMode && victory) waveSystem.currentWave / 5 else 0
        val cardsEarned = baseCards + dungeonCardBonus
        // Roll relic drop on victory (10% chance, 50% in RELIC_HUNT dungeon)
        val relicDrop = if (victory) {
            val isRelicHunt = isDungeonMode && dungeonDef?.type == com.example.jaygame.data.DungeonType.RELIC_HUNT
            if (isRelicHunt) relicManager?.rollRelicDropBoosted(0.50) else relicManager?.rollRelicDrop()
        } else null
        BattleBridge.onBattleEnd(
            victory, waveSystem.currentWave + 1, goldEarned, trophyChange,
            killCount, mergeCount, cardsEarned, noHpLost, fastClear,
            relicDropId = relicDrop?.first ?: -1,
            relicDropGrade = relicDrop?.second?.ordinal ?: -1,
        )
    }

    // ── Z17: Unit Position vs Path Overlap Validation ──

    private fun validateLayout() {
        val tag = "BattleLayout"
        // Free-form placement — units are clamped to field bounds by GameUnit.update()
        Log.i(tag, "Z17: Free-form layout — unit positions clamped by field bounds")
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
}
