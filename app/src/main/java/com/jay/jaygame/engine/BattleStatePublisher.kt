@file:Suppress("DEPRECATION")
package com.jay.jaygame.engine

import com.jay.jaygame.bridge.BUFF_BIT_ARMOR_BREAK
import com.jay.jaygame.bridge.BUFF_BIT_DOT
import com.jay.jaygame.bridge.BUFF_BIT_LIGHTNING
import com.jay.jaygame.bridge.BUFF_BIT_POISON
import com.jay.jaygame.bridge.BUFF_BIT_SLOW
import com.jay.jaygame.bridge.BUFF_BIT_STUN
import com.jay.jaygame.bridge.BUFF_BIT_WIND
import com.jay.jaygame.bridge.BattleBridge
import com.jay.jaygame.bridge.BattleStateUpdate
import com.jay.jaygame.bridge.UNIT_BUFF_ATK_UP
import com.jay.jaygame.bridge.UNIT_BUFF_DEF_UP
import com.jay.jaygame.bridge.UNIT_BUFF_SHIELD
import com.jay.jaygame.bridge.UNIT_BUFF_SPD_UP
import com.jay.jaygame.bridge.UnitPositionBatch
import com.jay.jaygame.data.UnitFamily

internal class BattleStatePublisher(
    maxEnemies: Int,
    maxUnits: Int,
    maxProjectiles: Int,
    gridSlots: Int,
) {
    private var gridPushTimer = 0f

    private val enemyXBuf = FloatArray(maxEnemies)
    private val enemyYBuf = FloatArray(maxEnemies)
    private val enemyTypeBuf = IntArray(maxEnemies)
    private val enemyHpBuf = FloatArray(maxEnemies)
    private val enemyBuffBuf = IntArray(maxEnemies)

    private val unitXBuf = FloatArray(maxUnits)
    private val unitYBuf = FloatArray(maxUnits)
    private val unitGradeBuf = IntArray(maxUnits)
    private val unitLevelBuf = IntArray(maxUnits)
    private val unitAttackingBuf = BooleanArray(maxUnits)
    private val unitAttackAnimBuf = FloatArray(maxUnits)
    private val unitTileBuf = IntArray(maxUnits)
    private val unitBlueprintIdBuf = Array(maxUnits) { "" }
    private val unitFamiliesListBuf = Array<List<UnitFamily>>(maxUnits) { emptyList() }
    private val unitRoleBuf = Array(maxUnits) { UnitRole.RANGED_DPS }
    private val unitAttackRangeBuf = Array(maxUnits) { AttackRange.RANGED }
    private val unitDamageTypeBuf = Array(maxUnits) { DamageType.PHYSICAL }
    private val unitCategoryBuf = Array(maxUnits) { UnitCategory.NORMAL }
    private val unitHpBuf = FloatArray(maxUnits)
    private val unitMaxHpBuf = FloatArray(maxUnits)
    private val unitStateBuf = Array(maxUnits) { UnitState.IDLE }
    private val unitHomeXBuf = FloatArray(maxUnits)
    private val unitHomeYBuf = FloatArray(maxUnits)
    private val unitRangeBuf = FloatArray(maxUnits)
    private val unitStackCountBuf = IntArray(maxUnits)
    private val unitBuffBuf = IntArray(maxUnits)
    private val unitSkillAnimBuf = FloatArray(maxUnits)
    private val unitCritAnimBuf = FloatArray(maxUnits)

    private val projSrcXBuf = FloatArray(maxProjectiles)
    private val projSrcYBuf = FloatArray(maxProjectiles)
    private val projDstXBuf = FloatArray(maxProjectiles)
    private val projDstYBuf = FloatArray(maxProjectiles)
    private val projTypeBuf = IntArray(maxProjectiles)
    private val projFamilyBuf = IntArray(maxProjectiles)
    private val projGradeBuf = IntArray(maxProjectiles)

    private val gridGradeBuf = IntArray(gridSlots)
    private val gridCanMergeBuf = BooleanArray(gridSlots)
    private val gridLevelBuf = IntArray(gridSlots)
    private val gridBlueprintIdBuf = Array(gridSlots) { "" }
    private val gridFamiliesListBuf = Array<List<UnitFamily>>(gridSlots) { emptyList() }
    private val gridRoleBuf = Array(gridSlots) { UnitRole.RANGED_DPS }

    fun advance(dt: Float) {
        gridPushTimer += dt
    }

    fun publishBattleState(
        waveSystem: WaveSystem,
        maxWaves: Int,
        defeatEnemyCount: Int,
        enemyCount: Int,
        sp: Float,
        elapsedTime: Float,
        stateOrdinal: Int,
        summonCost: Int,
        isBossRound: Boolean,
        waveDelayRemaining: Float,
    ) {
        BattleBridge.updateState(
            BattleStateUpdate(
                wave = waveSystem.currentWave + 1,
                maxWaves = maxWaves,
                hp = (defeatEnemyCount - enemyCount).coerceAtLeast(0),
                maxHp = defeatEnemyCount,
                sp = sp,
                elapsed = elapsedTime,
                state = stateOrdinal,
                summonCost = summonCost,
                enemyCount = enemyCount,
                isBossRound = if (isBossRound) 1 else 0,
                waveTimeRemaining = waveSystem.timeRemaining,
                waveElapsed = waveSystem.waveElapsed,
                waveDelayRemaining = waveDelayRemaining,
            )
        )
    }

    fun publishEnemyPositions(enemies: ObjectPool<Enemy>, worldWidth: Float, worldHeight: Float) {
        var index = 0
        enemies.forEach { enemy ->
            if (index >= enemyXBuf.size) return@forEach
            enemyXBuf[index] = enemy.position.x / worldWidth
            enemyYBuf[index] = enemy.position.y / worldHeight
            enemyTypeBuf[index] = when {
                enemy.isBossGuard -> WaveSystem.BOSS_GUARD_ENEMY_TYPE
                enemy.isBoss -> WaveSystem.BOSS_ENEMY_TYPE
                else -> enemy.type
            }
            enemyHpBuf[index] = enemy.hpRatio
            enemyBuffBuf[index] = enemyBuffBits(enemy)
            index++
        }
        BattleBridge.updateEnemyPositions(enemyXBuf, enemyYBuf, enemyTypeBuf, enemyHpBuf, enemyBuffBuf, index)
    }

    fun publishUnitPositions(units: ObjectPool<GameUnit>, grid: Grid, worldWidth: Float, worldHeight: Float) {
        var index = 0
        units.forEach { unit ->
            if (index >= unitXBuf.size) return@forEach
            unitXBuf[index] = unit.position.x / worldWidth
            unitYBuf[index] = unit.position.y / worldHeight
            unitGradeBuf[index] = unit.grade
            unitLevelBuf[index] = unit.level
            unitAttackingBuf[index] = unit.isAttacking
            unitAttackAnimBuf[index] = unit.attackAnimTimer
            unitTileBuf[index] = unit.tileIndex
            unitBlueprintIdBuf[index] = unit.blueprintId
            unitFamiliesListBuf[index] = unit.families
            unitRoleBuf[index] = unit.role
            unitAttackRangeBuf[index] = unit.attackRange
            unitDamageTypeBuf[index] = unit.damageType
            unitCategoryBuf[index] = unit.unitCategory
            unitHpBuf[index] = unit.hp
            unitMaxHpBuf[index] = unit.maxHp
            unitStateBuf[index] = unit.state
            unitHomeXBuf[index] = unit.homePosition.x / worldWidth
            unitHomeYBuf[index] = unit.homePosition.y / worldHeight
            unitRangeBuf[index] = unit.range
            unitStackCountBuf[index] = grid.getStackCount(unit.tileIndex)
            unitBuffBuf[index] = unitBuffBits(unit)
            unitSkillAnimBuf[index] = unit.skillAnimTimer
            unitCritAnimBuf[index] = unit.critAnimTimer
            index++
        }
        BattleBridge.updateUnitPositions(
            UnitPositionBatch(
                xs = unitXBuf,
                ys = unitYBuf,
                grades = unitGradeBuf,
                levels = unitLevelBuf,
                isAttacking = unitAttackingBuf,
                tileIndices = unitTileBuf,
                count = index,
                attackAnimTimers = unitAttackAnimBuf,
                blueprintIds = unitBlueprintIdBuf,
                familiesList = unitFamiliesListBuf,
                roles = unitRoleBuf,
                attackRanges = unitAttackRangeBuf,
                damageTypes = unitDamageTypeBuf,
                unitCategories = unitCategoryBuf,
                hps = unitHpBuf,
                maxHps = unitMaxHpBuf,
                states = unitStateBuf,
                homeXs = unitHomeXBuf,
                homeYs = unitHomeYBuf,
                stackCounts = unitStackCountBuf,
                buffs = unitBuffBuf,
                skillAnimTimers = unitSkillAnimBuf,
                critAnimTimers = unitCritAnimBuf,
                ranges = unitRangeBuf,
            )
        )
    }

    fun publishProjectiles(projectiles: ObjectPool<Projectile>, worldWidth: Float, worldHeight: Float) {
        var index = 0
        projectiles.forEach { projectile ->
            if (index >= projSrcXBuf.size) return@forEach
            projSrcXBuf[index] = projectile.sourcePos.x / worldWidth
            projSrcYBuf[index] = projectile.sourcePos.y / worldHeight
            projDstXBuf[index] = projectile.position.x / worldWidth
            projDstYBuf[index] = projectile.position.y / worldHeight
            projTypeBuf[index] = projectile.type
            projFamilyBuf[index] = projectile.family
            projGradeBuf[index] = projectile.grade
            index++
        }
        BattleBridge.updateProjectiles(
            projSrcXBuf,
            projSrcYBuf,
            projDstXBuf,
            projDstYBuf,
            projTypeBuf,
            index,
            projFamilyBuf,
            projGradeBuf,
        )
    }

    fun publishGridState(grid: Grid, mergeableTiles: Set<Int>) {
        if (gridPushTimer < 0.1f) return
        gridPushTimer = 0f
        for (i in 0 until Grid.SLOT_COUNT) {
            val unit = grid.getUnit(i)
            if (unit != null) {
                gridGradeBuf[i] = unit.grade
                gridCanMergeBuf[i] = i in mergeableTiles
                gridLevelBuf[i] = grid.getStackCount(i)
                gridBlueprintIdBuf[i] = unit.blueprintId
                gridFamiliesListBuf[i] = unit.families
                gridRoleBuf[i] = unit.role
            } else {
                gridGradeBuf[i] = 0
                gridCanMergeBuf[i] = false
                gridLevelBuf[i] = 0
                gridBlueprintIdBuf[i] = ""
                gridFamiliesListBuf[i] = emptyList()
                gridRoleBuf[i] = UnitRole.RANGED_DPS
            }
        }
        BattleBridge.updateGridState(
            gridGradeBuf,
            gridCanMergeBuf,
            gridLevelBuf,
            gridBlueprintIdBuf,
            gridFamiliesListBuf,
            gridRoleBuf,
        )
    }

    private fun enemyBuffBits(enemy: Enemy): Int {
        var bits = 0
        val hasSlow = enemy.buffs.hasBuff(BuffType.Slow)
        val hasDot = enemy.buffs.hasBuff(BuffType.DoT)
        val hasArmorBreak = enemy.buffs.hasBuff(BuffType.ArmorBreak)
        val hasStun = enemy.buffs.isStunned()
        if (hasSlow) bits = bits or BUFF_BIT_SLOW
        if (hasDot) bits = bits or BUFF_BIT_DOT
        if (hasArmorBreak) bits = bits or BUFF_BIT_ARMOR_BREAK
        if (hasStun) bits = bits or BUFF_BIT_STUN
        if (hasSlow && hasDot) bits = bits or BUFF_BIT_POISON
        if (enemy.recentHitFlags and 1 != 0) bits = bits or BUFF_BIT_LIGHTNING
        if (enemy.recentHitFlags and 2 != 0) bits = bits or BUFF_BIT_WIND
        return bits
    }

    private fun unitBuffBits(unit: GameUnit): Int {
        var bits = 0
        if (unit.buffs.hasBuff(BuffType.AtkUp)) bits = bits or UNIT_BUFF_ATK_UP
        if (unit.buffs.hasBuff(BuffType.SpdUp)) bits = bits or UNIT_BUFF_SPD_UP
        if (unit.buffs.hasBuff(BuffType.Shield)) bits = bits or UNIT_BUFF_SHIELD
        if (unit.buffs.hasBuff(BuffType.DefUp)) bits = bits or UNIT_BUFF_DEF_UP
        return bits
    }
}
