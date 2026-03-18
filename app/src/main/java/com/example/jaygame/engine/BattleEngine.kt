@file:Suppress("DEPRECATION")
package com.example.jaygame.engine

import android.util.Log
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.DungeonDef
import com.example.jaygame.data.GameData
// TODO(Task18): Remove these legacy imports once requestSummon/requestMerge/requestBuyUnit
//  are fully migrated to blueprint-based paths (requestSummonBlueprint, tryMergeBlueprint, etc.)
import com.example.jaygame.data.UNIT_DEFS
import com.example.jaygame.data.unitFamilyOf
import com.example.jaygame.data.unitGradeOf
import com.example.jaygame.data.unitIdOf
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
    private val deck: IntArray,
    gameData: GameData? = null,
    initialPity: Int = 0,
) {
    companion object {
        const val W = 720f
        const val H = 720f
        const val FIXED_DT = 1f / 60f
        const val MAX_ENEMIES = 256
        const val MAX_UNITS = 64
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

    // 유닛별 영구 레벨 (카드 레벨업)
    private val permanentUnitLevels: Map<Int, Int> = gameData?.let { data ->
        data.units.mapIndexed { idx, progress -> idx to progress.level }.toMap()
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

    // NEW: Blueprint-based system
    val blueprintRegistry = BlueprintRegistry()

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
            val forceBoss = dungeonDef!!.type == com.example.jaygame.data.DungeonType.BOSS_RUSH
            waveSystem = WaveSystem(maxWaves, difficulty, forceBoss = forceBoss)
        }
        // Set active synergy for AbilitySystem
        // Use the most common family in deck for synergy calculation
        if (deck.isNotEmpty()) {
            val mainFamily = deck.groupBy { it }.maxByOrNull { it.value.size }?.key ?: 0
            AbilitySystem.activeSynergy = SynergySystem.getSynergyBonus(deck, mainFamily)
        }
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
                accumulator += frameDt * BattleBridge.battleSpeed.value

                while (accumulator >= FIXED_DT) {
                    update(FIXED_DT)
                    accumulator -= FIXED_DT
                }

                pushStateToCompose()
                delay(8)
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
            val mergeable = MergeSystem.findMergeableTiles(grid)
            if (mergeable.isNotEmpty()) {
                // Auto-merge: prioritize lowest grade first (clear space for better units)
                val bestMerge = mergeable.minByOrNull { tileIdx ->
                    grid.getUnit(tileIdx)?.grade ?: Int.MAX_VALUE
                } ?: mergeable.first()
                requestMerge(bestMerge)
            }
            // Auto-summon: summon when SP is enough
            val effectiveCost = (BASE_SUMMON_COST * (1f - (relicManager?.totalSummonCostReduction() ?: 0f))).toInt().coerceAtLeast(BASE_SUMMON_COST / 2)
            if (sp >= effectiveCost && !grid.isFull()) {
                requestSummon()
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
        val splitQueue = mutableListOf<Enemy>() // Deferred SPLITTER spawns
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
        val deadList = mutableListOf<Enemy>()
        enemies.forEach { if (!it.alive) deadList.add(it) }
        val poisonSpread = AbilitySystem.activeSynergy.specialEffect == SynergySystem.SpecialEffect.POISON_SPREAD
        deadList.forEach { dead ->
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
        val unitList = units.toActiveList()
        AbilitySystem.applyAuraEffects(unitList, dt, auraTicks)

        // Update unique abilities (hero+ grade skills)
        val activeEnemies = enemies.toActiveList()
        UniqueAbilitySystem.update(unitList, dt, activeEnemies)

        units.forEach { unit ->
            if (!unit.alive) return@forEach

            // NEW: Behavior-based update path — units with a behavior delegate to it
            if (unit.behavior != null) {
                unit.behavior!!.update(unit, dt) { pos, range -> findNearestEnemy(pos, range) }
                // For behavior-based units, check if behavior produced an attack
                if (unit.canAttack()) {
                    val target = unit.currentTarget
                    if (target != null && target.alive) {
                        val result = unit.behavior!!.onAttack(unit, target)
                        if (result.isInstant) {
                            // Melee instant damage — apply directly
                            val finalDmg = DamageCalculator.calculatePhysicalDamage(
                                result.damage,
                                if (result.isMagic) 0f else target.armor,
                            )
                            target.hp -= finalDmg
                            if (target.hp <= 0f) target.alive = false
                            val nx = target.position.x / W
                            val ny = target.position.y / H
                            BattleBridge.onDamageDealt(nx, ny, finalDmg.toInt(), result.isCrit)
                        } else {
                            // Ranged — spawn projectile with behavior-computed damage
                            val proj = projectiles.acquire()
                            if (proj != null) {
                                proj.init(
                                    from = unit.position.copy(), target = target,
                                    damage = result.damage, speed = 400f,
                                    type = unit.family.coerceIn(0, 5),
                                    isMagic = result.isMagic, isCrit = result.isCrit,
                                    sourceUnitId = unit.tileIndex,
                                    abilityType = unit.abilityType,
                                    abilityValue = unit.abilityValue,
                                    grade = unit.grade, family = unit.family,
                                    attackerRange = unit.range,
                                )
                            }
                        }
                    }
                    unit.onAttack()
                }
            } else {
                // Legacy update path
                unit.update(dt) { pos, range -> findNearestEnemy(pos, range) }
                if (unit.canAttack()) {
                    fireProjectile(unit)
                    unit.onAttack()
                }
            }
        }
    }

    private fun updateProjectiles(dt: Float) {
        val deadProj = mutableListOf<Projectile>()
        projectiles.forEach { proj ->
            if (!proj.alive) { deadProj.add(proj); return@forEach }
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
            if (!proj.alive) deadProj.add(proj)
        }
        deadProj.forEach { projectiles.release(it) }
    }

    private fun updateZones(dt: Float) {
        val activeEnemies = enemies.toActiveList()
        val deadZones = mutableListOf<ZoneEffect>()
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
        val activeEnemies = enemies.toActiveList()
        val activeUnits = units.toActiveList()
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

        // Synergy bonus
        val synergy = SynergySystem.getSynergyBonus(deck, unit.family)

        val isCrit = Math.random() < (0.05 + upgradeCritRate + relicCritChance)
        val critMultiplier = 2f + relicCritDmg
        val isMagic = unit.family == 1 || unit.family == 4
        val familyUpgradeBonus = 1f + (familyUpgradeLevels[unit.family] ?: 0) * 0.001f // +0.1% per level
        val gradeBonus = DamageCalculator.gradeMultiplier(unit.grade)
        val advantageMult = DamageCalculator.familyAdvantageMultiplier(unit.family, target.type)
        val baseAtk = unit.effectiveATK() * upgradeAtkMult * (1f + relicAtkBonus) * synergy.atkMultiplier * familyUpgradeBonus * gradeBonus * advantageMult
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
            attackerRange = unit.range,
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

    fun requestSummon() {
        val effectiveSummonCost = (BASE_SUMMON_COST * (1f - (relicManager?.totalSummonCostReduction() ?: 0f))).toInt().coerceAtLeast(BASE_SUMMON_COST / 2)
        if (sp < effectiveSummonCost || grid.isFull()) return
        sp -= effectiveSummonCost

        val (rawGrade, resetPity) = probabilityEngine.rollGradeWithPity(currentPity)
        currentPity = if (resetPity) 0 else (currentPity + 1).coerceAtMost(100)
        BattleBridge.updateUnitPullPity(currentPity)
        // Pet ID 7 (9미호): chance to bump summon grade by 1
        val gradeUpChance = petSystem.getSummonGradeUpChance()
        val grade = if (gradeUpChance > 0f && Math.random() < gradeUpChance) {
            (rawGrade + 1).coerceAtMost(3)  // cap at LEGEND (3)
        } else rawGrade
        if (deck.isEmpty()) return
        val familyIndex = deck.random()  // deck stores family ordinals directly
        val unitDefId = unitIdOf(grade, familyIndex) ?: return

        val tileIndex = grid.findEmpty()
        if (tileIndex < 0) return

        val def = UNIT_DEFS.find { it.id == unitDefId } ?: return
        val unit = units.acquire() ?: return
        val abilityInfo = abilityForFamily(familyIndex)
        val synergy = SynergySystem.getSynergyBonus(deck, familyIndex)
        // Apply permanent unit level bonus from card upgrades
        val permLevel = permanentUnitLevels.getOrElse(unitDefId) { 1 }
        val permLevelBonus = 1f + (permLevel - 1) * 0.02f // +2% ATK per permanent level
        unit.init(
            unitDefId = unitDefId, grade = grade, family = familyIndex, level = 1,
            tileIndex = tileIndex, homePos = grid.cellCenter(tileIndex),
            baseATK = def.baseATK.toFloat() * permLevelBonus, atkSpeed = def.baseSpeed * synergy.spdMultiplier,
            range = def.range * upgradeRangeMult * synergy.rangeMultiplier,
            abilityType = abilityInfo.first, abilityValue = abilityInfo.second,
        )
        UniqueAbilitySystem.initUnit(unit)
        grid.placeUnit(tileIndex, unit)
        BattleBridge.onSummonResult(unitDefId, grade)
    }

    // NEW: Blueprint-based summon path (alongside legacy requestSummon)
    fun requestSummonBlueprint(gradeOverride: UnitGrade? = null) {
        val effectiveSummonCost = (BASE_SUMMON_COST * (1f - (relicManager?.totalSummonCostReduction() ?: 0f))).toInt().coerceAtLeast(BASE_SUMMON_COST / 2)
        if (sp < effectiveSummonCost || grid.isFull()) return
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
        unit.position = grid.cellCenter(tileIndex)
        unit.homePosition = grid.cellCenter(tileIndex)
        unit.behavior = BehaviorFactory.create(selected.behaviorId)
        grid.placeUnit(tileIndex, unit)
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
        val result = MergeSystem.tryMerge(grid, tileIndex) ?: return
        result.consumedTiles.forEach { i ->
            val u = grid.removeUnit(i)
            if (u != null) units.release(u)
        }
        val def = UNIT_DEFS.find { it.id == result.resultUnitDefId } ?: return
        val newGrade = unitGradeOf(result.resultUnitDefId)
        val newFamily = unitFamilyOf(result.resultUnitDefId)
        val unit = units.acquire() ?: return
        val abilityInfo = abilityForFamily(newFamily)
        unit.init(
            unitDefId = result.resultUnitDefId, grade = newGrade, family = newFamily, level = 1,
            tileIndex = tileIndex, homePos = grid.cellCenter(tileIndex),
            baseATK = def.baseATK.toFloat(), atkSpeed = def.baseSpeed,
            range = def.range * upgradeRangeMult,
            abilityType = abilityInfo.first, abilityValue = abilityInfo.second,
        )
        UniqueAbilitySystem.initUnit(unit)
        grid.placeUnit(tileIndex, unit)
        mergeCount++
        BattleBridge.onMergeComplete(tileIndex, result.isLucky, result.resultUnitDefId)
    }

    fun requestSell(tileIndex: Int) {
        val unit = grid.removeUnit(tileIndex) ?: return
        sp += 30f + unit.grade * 20f
        units.release(unit)
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
        return count
    }

    fun requestClickTile(tileIndex: Int) {
        val unit = grid.getUnit(tileIndex) ?: return
        val mergeable = MergeSystem.findMergeableTiles(grid)
        BattleBridge.onUnitClicked(
            tileIndex, unit.unitDefId, unit.grade, unit.family,
            tileIndex in mergeable, unit.level,
        )
    }

    fun requestRelocate(tileIndex: Int, normX: Float, normY: Float) {
        val unit = grid.removeUnit(tileIndex) ?: return
        val worldX = normX * W
        val worldY = normY * H
        val newTile = grid.getCellAt(worldX, worldY)
        if (newTile >= 0 && grid.getUnit(newTile) == null) {
            grid.placeUnit(newTile, unit)
        } else {
            grid.placeUnit(tileIndex, unit)
        }
    }

    fun requestSwap(from: Int, to: Int) {
        val unitA = grid.removeUnit(from)
        val unitB = grid.removeUnit(to)
        if (unitA != null) grid.placeUnit(to, unitA)
        if (unitB != null) grid.placeUnit(from, unitB)
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

    fun applyGamble(newSp: Float) {
        sp = newSp.coerceAtLeast(0f)
    }

    fun requestGamble(betPercent: Float, option: GambleSystem.GambleOption): GambleSystem.GambleResult? {
        val bet = sp * betPercent
        if (bet < 10f) return null
        val luckBonus = relicManager?.totalGambleBonus() ?: 0f
        val result = GambleSystem.gamble(bet, option, luckBonus)
        sp = if (result.won) sp - bet + result.reward else sp - bet
        return result
    }

    fun requestBuyUnit(unitDefId: Int, cost: Float) {
        if (sp < cost || grid.isFull()) return
        sp -= cost
        val tileIndex = grid.findEmpty()
        if (tileIndex < 0) return
        val def = UNIT_DEFS.find { it.id == unitDefId } ?: return
        val grade = unitGradeOf(unitDefId)
        val family = unitFamilyOf(unitDefId)
        val unit = units.acquire() ?: return
        val abilityInfo = abilityForFamily(family)
        unit.init(
            unitDefId = unitDefId, grade = grade, family = family, level = 1,
            tileIndex = tileIndex, homePos = grid.cellCenter(tileIndex),
            baseATK = def.baseATK.toFloat(), atkSpeed = def.baseSpeed,
            range = def.range * upgradeRangeMult,
            abilityType = abilityInfo.first, abilityValue = abilityInfo.second,
        )
        UniqueAbilitySystem.initUnit(unit)
        grid.placeUnit(tileIndex, unit)
        BattleBridge.onSummonResult(unitDefId, grade)
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
        val mergeable = MergeSystem.findMergeableTiles(grid)

        BattleBridge.updateState(
            waveSystem.currentWave + 1, maxWaves,
            (DEFEAT_ENEMY_COUNT - enemies.activeCount).coerceAtLeast(0), DEFEAT_ENEMY_COUNT,
            sp, elapsedTime, state.ordinal, summonCost,
            enemies.activeCount, if (isBossRound) 1 else 0, waveSystem.timeRemaining,
        )

        // Enemy positions + buff bitmasks
        val eCount = enemies.activeCount
        val exs = FloatArray(eCount)
        val eys = FloatArray(eCount)
        val etypes = IntArray(eCount)
        val ehp = FloatArray(eCount)
        val ebuffs = IntArray(eCount)
        var ei = 0
        enemies.forEach { e ->
            if (ei < eCount) {
                exs[ei] = e.position.x / W
                eys[ei] = e.position.y / H
                etypes[ei] = e.type
                ehp[ei] = e.hpRatio
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
                ebuffs[ei] = bits
                ei++
            }
        }
        BattleBridge.updateEnemyPositions(exs, eys, etypes, ehp, ebuffs, ei)

        // Unit positions
        val uCount = units.activeCount
        val uxs = FloatArray(uCount)
        val uys = FloatArray(uCount)
        val uDefIds = IntArray(uCount)
        val uGrades = IntArray(uCount)
        val uLevels = IntArray(uCount)
        val uAttacking = BooleanArray(uCount)
        val uTiles = IntArray(uCount)
        var ui = 0
        units.forEach { u ->
            if (ui < uCount) {
                uxs[ui] = u.position.x / W
                uys[ui] = u.position.y / H
                uDefIds[ui] = u.unitDefId
                uGrades[ui] = u.grade
                uLevels[ui] = u.level
                uAttacking[ui] = u.isAttacking
                uTiles[ui] = u.tileIndex
                ui++
            }
        }
        BattleBridge.updateUnitPositions(uxs, uys, uDefIds, uGrades, uLevels, uAttacking, uTiles, ui)

        // Projectiles
        val pCount = projectiles.activeCount
        val psxs = FloatArray(pCount)
        val psys = FloatArray(pCount)
        val pdxs = FloatArray(pCount)
        val pdys = FloatArray(pCount)
        val ptypes = IntArray(pCount)
        var pi = 0
        projectiles.forEach { p ->
            if (pi < pCount) {
                psxs[pi] = p.sourcePos.x / W
                psys[pi] = p.sourcePos.y / H
                pdxs[pi] = p.position.x / W
                pdys[pi] = p.position.y / H
                ptypes[pi] = p.type
                pi++
            }
        }
        BattleBridge.updateProjectiles(psxs, psys, pdxs, pdys, ptypes, pi)

        // Grid state
        if (gridPushTimer >= 0.1f) {
            gridPushTimer = 0f
            val ids = IntArray(Grid.TOTAL)
            val grades = IntArray(Grid.TOTAL)
            val families = IntArray(Grid.TOTAL)
            val canMerge = BooleanArray(Grid.TOTAL)
            val levels = IntArray(Grid.TOTAL)
            for (i in 0 until Grid.TOTAL) {
                val u = grid.getUnit(i)
                if (u != null) {
                    ids[i] = u.unitDefId
                    grades[i] = u.grade
                    families[i] = u.family
                    canMerge[i] = i in mergeable
                    levels[i] = u.level
                } else {
                    ids[i] = -1
                }
            }
            BattleBridge.updateGridState(ids, grades, families, canMerge, levels)
        }
    }

    private fun onBattleEnd(victory: Boolean) {
        val difficultyBonus = if (isDungeonMode && dungeonDef != null) {
            dungeonDef!!.difficultyMultiplier
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

        // Path strip bounds: outer rect (60px outside grid) → inner rect (grid bounds)
        // Outer: (340,60)~(940,660). Inner/grid: (400,120)~(880,600).
        // Path strip = area between outer and inner rects.
        val outerLeft = Grid.ORIGIN_X - 60f
        val outerTop = Grid.ORIGIN_Y - 60f
        val outerRight = Grid.ORIGIN_X + Grid.GRID_W + 60f
        val outerBottom = Grid.ORIGIN_Y + Grid.GRID_H + 60f

        val innerLeft = Grid.ORIGIN_X
        val innerTop = Grid.ORIGIN_Y
        val innerRight = Grid.ORIGIN_X + Grid.GRID_W
        val innerBottom = Grid.ORIGIN_Y + Grid.GRID_H

        var warnings = 0

        for (i in 0 until Grid.TOTAL) {
            val col = i % Grid.COLS
            val row = i / Grid.COLS
            val cx = Grid.ORIGIN_X + col * Grid.CELL_W + Grid.CELL_W * 0.5f
            val cy = Grid.ORIGIN_Y + row * Grid.CELL_H + Grid.CELL_H * 0.5f

            // Check if cell center is inside path strip (between outer and inner rect)
            val inOuter = cx >= outerLeft && cx <= outerRight && cy >= outerTop && cy <= outerBottom
            val inInner = cx > innerLeft && cx < innerRight && cy > innerTop && cy < innerBottom
            val inPathStrip = inOuter && !inInner

            if (inPathStrip) {
                Log.w(tag, "Z17: Cell[$i] center ($cx,$cy) overlaps with path strip!")
                warnings++
            }

            // Check wander range stays inside grid bounds (±30px + margin from GameUnit)
            val wanderRange = 50f  // max wander = 20 + 30 = 50px
            if (cx - wanderRange < innerLeft || cx + wanderRange > innerRight ||
                cy - wanderRange < innerTop || cy + wanderRange > innerBottom) {
                Log.w(tag, "Z17: Cell[$i] wander range (±50px from $cx,$cy) may exit grid bounds")
                warnings++
            }
        }

        if (warnings == 0) {
            Log.i(tag, "Z17: Layout validation passed — all cell centers inside grid, wander ranges OK")
        } else {
            Log.w(tag, "Z17: Layout validation found $warnings warnings")
        }
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
