package com.example.jaygame.engine

import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.UNIT_DEFS
import com.example.jaygame.data.unitFamilyOf
import com.example.jaygame.data.unitGradeOf
import com.example.jaygame.data.unitIdOf
import com.example.jaygame.engine.math.GameRect
import com.example.jaygame.engine.math.Vec2
import kotlinx.coroutines.*

class BattleEngine(
    private val stageId: Int,
    private val difficulty: Int,
    private val maxWaves: Int,
    private val deck: IntArray,
) {
    companion object {
        const val W = 1280f
        const val H = 720f
        const val FIXED_DT = 1f / 60f
        const val MAX_ENEMIES = 256
        const val MAX_UNITS = 64
        const val MAX_PROJECTILES = 512
        const val DEFEAT_ENEMY_COUNT = 100
        const val SP_REGEN_PER_SEC = 2f
        const val BASE_SUMMON_COST = 10
        const val WAVE_DELAY = 3f
    }

    enum class State { WaveDelay, Playing, Victory, Defeat }
    var state = State.WaveDelay; private set

    val enemies = ObjectPool(MAX_ENEMIES, { Enemy() }) { it.reset() }
    val units = ObjectPool(MAX_UNITS, { GameUnit() }) { it.reset() }
    val projectiles = ObjectPool(MAX_PROJECTILES, { Projectile() }) { it.reset() }
    val zones = ObjectPool(32, { ZoneEffect() }) { it.reset() }

    val grid = Grid()
    val waveSystem = WaveSystem(maxWaves, difficulty)
    val spatialHash = SpatialHash<Enemy>(64f)
    private val auraTicks = FloatArray(MAX_UNITS)

    var sp = 100f; private set
    var summonCost = BASE_SUMMON_COST; private set
    var elapsedTime = 0f; private set

    private var waveDelayTimer = WAVE_DELAY
    private var isBossRound = false
    private var bossTimeRemaining = 0f

    private var killCount = 0
    private var mergeCount = 0

    private val upgradeLevels = IntArray(5)
    private var upgradeAtkMult = 1f
    private var upgradeSpdMult = 1f
    private var upgradeCritRate = 0f
    private var upgradeRangeMult = 1f
    private var upgradeSpRegen = 0f

    private var gridPushTimer = 0f

    // Square path around grid (480x480 centered at 640,360)
    // Grid: (400,120)~(880,600). Path margin: 60px outside grid.
    // Path outer: (340,60)~(940,660) = 600x600 square
    // Midline of path strip: 30px from grid edge
    private val pathLeft = Grid.ORIGIN_X - 30f    // 370
    private val pathTop = Grid.ORIGIN_Y - 30f     // 90
    private val pathRight = Grid.ORIGIN_X + Grid.GRID_W + 30f  // 910
    private val pathBottom = Grid.ORIGIN_Y + Grid.GRID_H + 30f // 630

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
        sp += (SP_REGEN_PER_SEC + upgradeSpRegen) * dt

        when (state) {
            State.WaveDelay -> {
                waveDelayTimer -= dt
                if (waveDelayTimer <= 0f) {
                    waveSystem.startWave(waveSystem.currentWave)
                    val config = waveSystem.getWaveConfig(waveSystem.currentWave)
                    isBossRound = config.isBoss
                    bossTimeRemaining = if (isBossRound) 60f else 0f
                    state = State.Playing
                }
            }
            State.Playing -> {
                updateSpawning(dt)
                updateEnemies(dt)
                updateUnits(dt)
                updateProjectiles(dt)
                updateZones(dt)

                if (isBossRound) {
                    bossTimeRemaining -= dt
                    if (bossTimeRemaining <= 0f) {
                        enemies.forEach { if (it.alive) { it.alive = false } }
                    }
                }

                if (enemies.activeCount >= DEFEAT_ENEMY_COUNT) {
                    state = State.Defeat
                    onBattleEnd(false)
                }

                if (waveSystem.waveComplete) {
                    if (waveSystem.isLastWave) {
                        state = State.Victory
                        onBattleEnd(true)
                    } else {
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
            enemy.init(
                hp = config.hp, speed = config.speed,
                armor = config.armor, magicResist = config.magicResist,
                type = config.enemyType, startPos = enemyPath.first().copy(),
            )
        }
    }

    private fun updateEnemies(dt: Float) {
        spatialHash.clear()
        enemies.forEach { enemy ->
            if (!enemy.alive) return@forEach
            enemy.update(dt, enemyPath)
            spatialHash.insert(
                enemy,
                enemy.position.x - enemy.size * 0.5f,
                enemy.position.y - enemy.size * 0.5f,
                enemy.size, enemy.size,
            )
        }
        val deadList = mutableListOf<Enemy>()
        enemies.forEach { if (!it.alive) deadList.add(it) }
        deadList.forEach {
            enemies.release(it)
            waveSystem.onEnemyKilled()
            killCount++
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
            unit.update(dt) { pos, range -> findNearestEnemy(pos, range) }
            if (unit.canAttack()) {
                fireProjectile(unit)
                unit.onAttack()
            }
        }
    }

    private fun updateProjectiles(dt: Float) {
        val deadProj = mutableListOf<Projectile>()
        projectiles.forEach { proj ->
            if (!proj.alive) { deadProj.add(proj); return@forEach }
            val stillAlive = proj.update(dt)
            if (!stillAlive && proj.target?.alive == true) {
                AbilitySystem.onProjectileHit(proj, proj.target!!, spatialHash) { from, target, dmg, type ->
                    val chain = projectiles.acquire() ?: return@onProjectileHit
                    chain.init(from, target, dmg, 400f, type, false, false, -1, 0, 0f, 0, 0)
                }
                val nx = proj.target!!.position.x / W
                val ny = proj.target!!.position.y / H
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
    }

    private fun fireProjectile(unit: GameUnit) {
        val target = unit.currentTarget ?: return
        val proj = projectiles.acquire() ?: return
        val isCrit = Math.random() < (0.05 + upgradeCritRate)
        val dmg = unit.effectiveATK() * upgradeAtkMult * (if (isCrit) 2f else 1f)
        val isMagic = unit.family == 1 || unit.family == 4

        proj.init(
            from = unit.position.copy(), target = target,
            damage = dmg, speed = 400f,
            type = unit.family.coerceIn(0, 5),
            isMagic = isMagic, isCrit = isCrit,
            sourceUnitId = unit.tileIndex,
            abilityType = unit.abilityType,
            abilityValue = unit.abilityValue,
            grade = unit.grade, family = unit.family,
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
        if (sp < summonCost || grid.isFull()) return
        sp -= summonCost

        val grade = rollGrade()
        val familyIndex = deck.random()  // deck stores family ordinals directly
        val unitDefId = unitIdOf(grade, familyIndex) ?: return

        val tileIndex = grid.findEmpty()
        if (tileIndex < 0) return

        val def = UNIT_DEFS.find { it.id == unitDefId } ?: return
        val unit = units.acquire() ?: return
        val abilityInfo = abilityForFamily(familyIndex)
        unit.init(
            unitDefId = unitDefId, grade = grade, family = familyIndex, level = 1,
            tileIndex = tileIndex, homePos = grid.cellCenter(tileIndex),
            baseATK = def.baseATK.toFloat(), atkSpeed = def.baseSpeed,
            range = def.range * upgradeRangeMult,
            abilityType = abilityInfo.first, abilityValue = abilityInfo.second,
        )
        UniqueAbilitySystem.initUnit(unit)
        grid.placeUnit(tileIndex, unit)
        BattleBridge.onSummonResult(unitDefId, grade)
    }

    private fun rollGrade(): Int {
        val r = Math.random() * 100
        return when {
            r < 60 -> 0; r < 85 -> 1; r < 97 -> 2; else -> 3
        }
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
    }

    fun applyGamble(newSp: Float) {
        sp = newSp.coerceAtLeast(0f)
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
            enemies.activeCount, if (isBossRound) 1 else 0, bossTimeRemaining,
        )

        // Enemy positions
        val eCount = enemies.activeCount
        val exs = FloatArray(eCount)
        val eys = FloatArray(eCount)
        val etypes = IntArray(eCount)
        val ehp = FloatArray(eCount)
        var ei = 0
        enemies.forEach { e ->
            if (ei < eCount) {
                exs[ei] = e.position.x / W
                eys[ei] = e.position.y / H
                etypes[ei] = e.type
                ehp[ei] = e.hpRatio
                ei++
            }
        }
        BattleBridge.updateEnemyPositions(exs, eys, etypes, ehp, ei)

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
        val goldEarned = if (victory) 100 + waveSystem.currentWave * 10 else waveSystem.currentWave * 5
        val trophyChange = if (victory) 20 + stageId * 5 else -(10 + stageId * 3)
        BattleBridge.onBattleEnd(victory, waveSystem.currentWave + 1, goldEarned, trophyChange, killCount, mergeCount, 0)
    }
}
