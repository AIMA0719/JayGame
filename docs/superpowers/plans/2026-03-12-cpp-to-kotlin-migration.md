# C++ to Kotlin Migration Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all C++ game logic (~6000 lines) with pure Kotlin, eliminating JNI bridge and NDK dependency while maintaining identical gameplay behavior.

**Architecture:** The current app uses C++ for game logic (BattleScene, Unit, Enemy, Projectile, Wave, Grid, Merge, Ability, Buff systems) with Compose for all rendering. C++ pushes data to Kotlin via JNI callbacks, and Kotlin sends user actions to C++ via atomic flags. The migration replaces this with a Kotlin coroutine-based game loop that directly updates StateFlows consumed by Compose. MainActivity changes from GameActivity to ComponentActivity.

**Tech Stack:** Kotlin, Coroutines, Compose, StateFlow

**Current C++ Architecture (for reference):**
- `main.cpp` — GameActivity entry, JNI bridge functions (10 extern "C" functions)
- `GameEngine` — 60Hz fixed timestep loop, scene management
- `BattleScene` — Core game logic (1290 lines), state machine, JNI push
- `Unit/Enemy/Projectile` — Game entities with ObjectPool
- `Wave/Grid/MergeSystem/Ability/Buff` — Game subsystems
- `SpatialHash` — Spatial partitioning for proximity queries
- `MathTypes` — Vec2, Rect math
- `SaveSystem` — JSON serialization to SharedPreferences via JNI
- Graphics files (Renderer, Shader, SpriteBatch, etc.) — NOT USED (Compose renders)

**Migration Strategy:** Bottom-up. Port data structures first, then entities, then systems, then the game loop. Replace BattleBridge JNI with direct Kotlin. Remove C++/NDK last.

---

## Chunk 1: Core Data Structures & Math

### Task 1: Vec2 and Rect Math Utilities

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/math/Vec2.kt`
- Create: `app/src/main/java/com/example/jaygame/engine/math/GameRect.kt`

- [ ] **Step 1: Create Vec2 data class**

```kotlin
// Vec2.kt
package com.example.jaygame.engine.math

import kotlin.math.sqrt

data class Vec2(var x: Float = 0f, var y: Float = 0f) {
    val length: Float get() = sqrt(x * x + y * y)
    val lengthSq: Float get() = x * x + y * y

    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)

    fun dot(o: Vec2) = x * o.x + y * o.y

    fun normalized(): Vec2 {
        val len = length
        return if (len > 0.0001f) Vec2(x / len, y / len) else Vec2()
    }

    fun distanceTo(o: Vec2): Float = (this - o).length
    fun distanceSqTo(o: Vec2): Float = (this - o).lengthSq

    companion object {
        fun lerp(a: Vec2, b: Vec2, t: Float) = Vec2(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
        )
    }
}
```

- [ ] **Step 2: Create GameRect data class**

```kotlin
// GameRect.kt
package com.example.jaygame.engine.math

data class GameRect(val x: Float, val y: Float, val w: Float, val h: Float) {
    val centerX: Float get() = x + w * 0.5f
    val centerY: Float get() = y + h * 0.5f
    val right: Float get() = x + w
    val bottom: Float get() = y + h

    fun contains(px: Float, py: Float) =
        px >= x && px < x + w && py >= y && py < y + h

    fun intersects(o: GameRect) =
        x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/jaygame/engine/math/
git commit -m "feat: add Vec2 and GameRect math utilities for Kotlin game engine"
```

---

### Task 2: SpatialHash for Enemy Proximity Queries

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/SpatialHash.kt`

- [ ] **Step 1: Create SpatialHash**

```kotlin
// SpatialHash.kt
package com.example.jaygame.engine

import com.example.jaygame.engine.math.GameRect

class SpatialHash<T>(private val cellSize: Float = 64f) {
    private val cells = HashMap<Long, MutableList<T>>()

    fun clear() = cells.clear()

    fun insert(item: T, x: Float, y: Float, w: Float, h: Float) {
        val minCX = (x / cellSize).toInt()
        val minCY = (y / cellSize).toInt()
        val maxCX = ((x + w) / cellSize).toInt()
        val maxCY = ((y + h) / cellSize).toInt()
        for (cy in minCY..maxCY) {
            for (cx in minCX..maxCX) {
                val key = (cx.toLong() shl 32) or (cy.toLong() and 0xFFFFFFFFL)
                cells.getOrPut(key) { mutableListOf() }.add(item)
            }
        }
    }

    fun query(rect: GameRect): List<T> {
        val result = mutableListOf<T>()
        val seen = HashSet<T>()
        val minCX = (rect.x / cellSize).toInt()
        val minCY = (rect.y / cellSize).toInt()
        val maxCX = (rect.right / cellSize).toInt()
        val maxCY = (rect.bottom / cellSize).toInt()
        for (cy in minCY..maxCY) {
            for (cx in minCX..maxCX) {
                val key = (cx.toLong() shl 32) or (cy.toLong() and 0xFFFFFFFFL)
                cells[key]?.forEach { if (seen.add(it)) result.add(it) }
            }
        }
        return result
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/jaygame/engine/SpatialHash.kt
git commit -m "feat: add SpatialHash for enemy proximity queries"
```

---

### Task 3: ObjectPool for Entity Reuse

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/ObjectPool.kt`

- [ ] **Step 1: Create ObjectPool**

```kotlin
// ObjectPool.kt
package com.example.jaygame.engine

class ObjectPool<T>(
    private val capacity: Int,
    private val factory: () -> T,
    private val reset: (T) -> Unit = {},
) {
    private val objects = ArrayList<T>(capacity)
    private val active = BooleanArray(capacity)
    private val freeList = ArrayDeque<Int>()
    var activeCount = 0; private set

    init {
        repeat(capacity) { i ->
            objects.add(factory())
            freeList.addLast(i)
        }
    }

    fun acquire(): T? {
        if (freeList.isEmpty()) return null
        val idx = freeList.removeFirst()
        active[idx] = true
        activeCount++
        reset(objects[idx])
        return objects[idx]
    }

    fun release(item: T) {
        val idx = objects.indexOf(item)
        if (idx >= 0 && active[idx]) {
            active[idx] = false
            activeCount--
            freeList.addLast(idx)
        }
    }

    inline fun forEach(action: (T) -> Unit) {
        for (i in objects.indices) {
            if (active[i]) action(objects[i])
        }
    }

    fun toActiveList(): List<T> = objects.filterIndexed { i, _ -> active[i] }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/jaygame/engine/ObjectPool.kt
git commit -m "feat: add ObjectPool for entity reuse"
```

---

## Chunk 2: Game Entities

### Task 4: Buff System

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/BuffSystem.kt`

Port from C++ `Buff.h/cpp`. The buff system tracks debuffs on enemies and buffs on units.

- [ ] **Step 1: Create BuffSystem**

```kotlin
// BuffSystem.kt
package com.example.jaygame.engine

enum class BuffType { Slow, DoT, ArmorBreak, AtkUp, SpdUp, Shield }

data class BuffEntry(
    val type: BuffType,
    var value: Float,
    var remaining: Float,
    var sourceId: Int = -1,
    var tickTimer: Float = 0f,
)

class BuffContainer {
    val buffs = mutableListOf<BuffEntry>()
    private var shieldHP = 0f

    fun addBuff(type: BuffType, value: Float, duration: Float, sourceId: Int = -1) {
        // Max 3 stacks per source
        val existing = buffs.filter { it.type == type && it.sourceId == sourceId }
        if (existing.size >= 3) {
            existing.minByOrNull { it.remaining }?.let { buffs.remove(it) }
        }
        buffs.add(BuffEntry(type, value, duration, sourceId))
        if (type == BuffType.Shield) shieldHP += value
    }

    fun update(dt: Float): Float {
        var dotDamage = 0f
        val iter = buffs.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.remaining -= dt
            if (b.remaining <= 0f) {
                iter.remove()
                continue
            }
            if (b.type == BuffType.DoT) {
                b.tickTimer += dt
                if (b.tickTimer >= 0.5f) {
                    b.tickTimer -= 0.5f
                    dotDamage += b.value
                }
            }
        }
        return dotDamage
    }

    fun getSlowFactor(): Float {
        var maxSlow = 0f
        buffs.forEach { if (it.type == BuffType.Slow) maxSlow = maxOf(maxSlow, it.value) }
        return 1f - maxSlow.coerceIn(0f, 0.8f)
    }

    fun getArmorReduction(): Float {
        var total = 0f
        buffs.forEach { if (it.type == BuffType.ArmorBreak) total += it.value }
        return total
    }

    fun getAtkMultiplier(): Float {
        var mult = 1f
        buffs.forEach { if (it.type == BuffType.AtkUp) mult += it.value }
        return mult
    }

    fun getSpdMultiplier(): Float {
        var mult = 1f
        buffs.forEach { if (it.type == BuffType.SpdUp) mult += it.value }
        return mult
    }

    fun absorbDamage(damage: Float): Float {
        if (shieldHP <= 0f) return damage
        val absorbed = minOf(shieldHP, damage)
        shieldHP -= absorbed
        if (shieldHP <= 0f) {
            buffs.removeAll { it.type == BuffType.Shield }
        }
        return damage - absorbed
    }

    fun clear() {
        buffs.clear()
        shieldHP = 0f
    }

    fun hasBuff(type: BuffType) = buffs.any { it.type == type }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/jaygame/engine/BuffSystem.kt
git commit -m "feat: add BuffSystem (slow, DoT, armor break, shield, etc.)"
```

---

### Task 5: Enemy Entity

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/Enemy.kt`

Port from C++ `Enemy.h/cpp`. Enemies follow a path, take damage, and have debuffs.

- [ ] **Step 1: Create Enemy class**

```kotlin
// Enemy.kt
package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2

class Enemy {
    var alive = false
    var position = Vec2()
    var hp = 0f
    var maxHp = 0f
    var speed = 0f
    var baseSpeed = 0f
    var armor = 0f
    var magicResist = 0f
    var type = 0  // 0=normal, 1=fast, 2=tank, 3=flying, 4=boss, 5=miniboss
    var pathIndex = 0
    var pathProgress = 0f
    val buffs = BuffContainer()

    fun init(
        hp: Float, speed: Float, armor: Float, magicResist: Float,
        type: Int, startPos: Vec2,
    ) {
        this.alive = true
        this.hp = hp
        this.maxHp = hp
        this.speed = speed
        this.baseSpeed = speed
        this.armor = armor
        this.magicResist = magicResist
        this.type = type
        this.position = startPos.copy()
        this.pathIndex = 0
        this.pathProgress = 0f
        this.buffs.clear()
    }

    fun update(dt: Float, path: List<Vec2>): Boolean {
        if (!alive) return false

        // Apply DoT damage
        val dotDmg = buffs.update(dt)
        if (dotDmg > 0f) {
            hp -= dotDmg
            if (hp <= 0f) { alive = false; return false }
        }

        // Movement along path
        val effectiveSpeed = baseSpeed * buffs.getSlowFactor()
        if (pathIndex < path.size) {
            val target = path[pathIndex]
            val dir = target - position
            val dist = dir.length
            val step = effectiveSpeed * dt

            if (dist <= step) {
                position = target.copy()
                pathIndex++
                if (pathIndex >= path.size) {
                    pathIndex = 0 // Loop
                }
            } else {
                val norm = dir.normalized()
                position.x += norm.x * step
                position.y += norm.y * step
            }
        }
        return true
    }

    fun takeDamage(damage: Float, isMagic: Boolean = false): Float {
        val effectiveArmor = (armor - buffs.getArmorReduction()).coerceAtLeast(0f)
        val reduction = if (isMagic) {
            1f - (magicResist / (magicResist + 100f))
        } else {
            1f - (effectiveArmor / (effectiveArmor + 100f))
        }
        val finalDmg = buffs.absorbDamage(damage * reduction)
        hp -= finalDmg
        if (hp <= 0f) alive = false
        return finalDmg
    }

    fun reset() {
        alive = false
        buffs.clear()
    }

    val hpRatio: Float get() = if (maxHp > 0f) (hp / maxHp).coerceIn(0f, 1f) else 0f
    val size: Float get() = if (type == 4 || type == 5) 96f else 48f
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/jaygame/engine/Enemy.kt
git commit -m "feat: add Enemy entity with path following and damage system"
```

---

### Task 6: Projectile Entity

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/Projectile.kt`

Port from C++ `Projectile.h/cpp`. Projectiles track targets and deal damage on hit.

- [ ] **Step 1: Create Projectile class**

```kotlin
// Projectile.kt
package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2

class Projectile {
    var alive = false
    var position = Vec2()
    var target: Enemy? = null
    var speed = 400f
    var damage = 0f
    var isMagic = false
    var isCrit = false
    var type = 0  // 0=arrow, 1=fire, 2=ice, 3=poison, 4=lightning, 5=generic
    var sourceUnitId = -1
    var abilityType = 0
    var abilityValue = 0f
    var lifetime = 3f
    var grade = 0
    var family = 0
    var sourcePos = Vec2()

    fun init(
        from: Vec2, target: Enemy, damage: Float, speed: Float,
        type: Int, isMagic: Boolean, isCrit: Boolean,
        sourceUnitId: Int, abilityType: Int, abilityValue: Float,
        grade: Int, family: Int,
    ) {
        this.alive = true
        this.position = from.copy()
        this.sourcePos = from.copy()
        this.target = target
        this.damage = damage
        this.speed = speed
        this.type = type
        this.isMagic = isMagic
        this.isCrit = isCrit
        this.sourceUnitId = sourceUnitId
        this.abilityType = abilityType
        this.abilityValue = abilityValue
        this.lifetime = 3f
        this.grade = grade
        this.family = family
    }

    /** Returns true if still alive */
    fun update(dt: Float): Boolean {
        if (!alive) return false
        lifetime -= dt
        if (lifetime <= 0f || target?.alive != true) {
            alive = false
            return false
        }

        val dir = target!!.position - position
        val dist = dir.length
        val step = speed * dt

        if (dist <= step + target!!.size * 0.5f) {
            // Hit!
            alive = false
            return false // caller should handle hit
        }

        val norm = dir.normalized()
        position.x += norm.x * step
        position.y += norm.y * step
        return true
    }

    fun hasHitTarget(): Boolean {
        if (target?.alive != true) return false
        val dist = position.distanceTo(target!!.position)
        return dist <= target!!.size * 0.5f + 8f
    }

    fun reset() {
        alive = false
        target = null
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/jaygame/engine/Projectile.kt
git commit -m "feat: add Projectile entity with homing and hit detection"
```

---

### Task 7: Unit Entity

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/GameUnit.kt`

Port from C++ `Unit.h/cpp`. Units move freely, find targets, and attack. Named `GameUnit` to avoid conflict with `kotlin.Unit`.

- [ ] **Step 1: Create GameUnit class**

```kotlin
// GameUnit.kt
package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2
import kotlin.math.cos
import kotlin.math.sin

class GameUnit {
    var alive = false
    var unitDefId = -1
    var grade = 0
    var family = 0
    var level = 1
    var tileIndex = -1
    var position = Vec2()
    var homePosition = Vec2()
    var baseATK = 0f
    var atkSpeed = 0f
    var range = 0f
    var abilityType = 0
    var abilityValue = 0f
    var isAttacking = false
    val buffs = BuffContainer()

    // Movement
    private var wanderTarget = Vec2()
    private var wanderTimer = 0f
    private var moveSpeed = 75f
    private var chaseTarget: Enemy? = null

    // Attack
    private var attackCooldown = 0f
    var currentTarget: Enemy? = null; private set

    fun init(
        unitDefId: Int, grade: Int, family: Int, level: Int,
        tileIndex: Int, homePos: Vec2,
        baseATK: Float, atkSpeed: Float, range: Float,
        abilityType: Int, abilityValue: Float,
    ) {
        this.alive = true
        this.unitDefId = unitDefId
        this.grade = grade
        this.family = family
        this.level = level
        this.tileIndex = tileIndex
        this.position = homePos.copy()
        this.homePosition = homePos.copy()
        this.baseATK = baseATK
        this.atkSpeed = atkSpeed
        this.range = range
        this.abilityType = abilityType
        this.abilityValue = abilityValue
        this.isAttacking = false
        this.attackCooldown = 0f
        this.buffs.clear()
        this.wanderTarget = homePos.copy()
        this.wanderTimer = 0f
        this.moveSpeed = when (family) {
            0 -> 110f  // Fire
            1 -> 55f   // Frost
            2 -> 75f   // Poison
            3 -> 65f   // Lightning
            4 -> 90f   // Support
            else -> 75f
        }
    }

    fun update(dt: Float, findEnemy: (Vec2, Float) -> Enemy?) {
        if (!alive) return

        buffs.update(dt)
        val atkMult = buffs.getAtkMultiplier()
        val spdMult = buffs.getSpdMultiplier()
        attackCooldown -= dt * spdMult

        // Chase range = 1.5x attack range
        val chaseRange = range * 1.5f
        val enemy = findEnemy(position, chaseRange)

        if (enemy != null) {
            // Chase enemy
            chaseTarget = enemy
            val dir = enemy.position - position
            val dist = dir.length

            if (dist > range) {
                // Move toward enemy
                val norm = dir.normalized()
                position.x += norm.x * moveSpeed * dt
                position.y += norm.y * moveSpeed * dt
                isAttacking = false
            } else {
                // In range — attack
                isAttacking = true
                currentTarget = enemy
            }
        } else {
            // Wander near home
            chaseTarget = null
            isAttacking = false
            currentTarget = null
            wanderTimer -= dt
            if (wanderTimer <= 0f) {
                val angle = Math.random().toFloat() * 6.283f
                val dist = 20f + Math.random().toFloat() * 30f
                wanderTarget = Vec2(
                    homePosition.x + cos(angle) * dist,
                    homePosition.y + sin(angle) * dist,
                )
                wanderTimer = 1f + Math.random().toFloat() * 2f
            }
            val dir = wanderTarget - position
            if (dir.lengthSq > 4f) {
                val norm = dir.normalized()
                val wanderSpeed = moveSpeed * 0.3f
                position.x += norm.x * wanderSpeed * dt
                position.y += norm.y * wanderSpeed * dt
            }
        }
    }

    /** Returns true if ready to fire */
    fun canAttack(): Boolean = isAttacking && attackCooldown <= 0f && currentTarget?.alive == true

    fun onAttack() {
        attackCooldown = 1f / atkSpeed
    }

    /** Effective ATK with level multiplier and buffs */
    fun effectiveATK(): Float {
        val levelMult = LEVEL_MULTIPLIERS.getOrElse(level - 1) { 1f }
        return baseATK * levelMult * buffs.getAtkMultiplier()
    }

    fun reset() {
        alive = false
        currentTarget = null
        chaseTarget = null
        buffs.clear()
    }

    companion object {
        val LEVEL_MULTIPLIERS = floatArrayOf(1f, 1.5f, 2.2f, 3.2f, 4.5f, 6f, 8f)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/jaygame/engine/GameUnit.kt
git commit -m "feat: add GameUnit with free movement, chase, and attack"
```

---

## Chunk 3: Game Systems

### Task 8: Wave System

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/WaveSystem.kt`

Port from C++ `Wave.h/cpp`. Manages enemy wave spawning with progression.

- [ ] **Step 1: Create WaveSystem**

C++ has 40 waves hardcoded. Port the wave table and spawn logic.

```kotlin
// WaveSystem.kt
package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2

data class WaveConfig(
    val enemyCount: Int,
    val hp: Float,
    val speed: Float,
    val armor: Float,
    val magicResist: Float,
    val isBoss: Boolean,
    val spawnInterval: Float,
    val enemyType: Int,  // 0=normal,1=fast,2=tank,3=flying,4=boss,5=miniboss
)

class WaveSystem(private val maxWaves: Int, private val difficulty: Int) {
    var currentWave = 0; private set
    var waveComplete = false; private set
    private var spawnTimer = 0f
    private var spawnedCount = 0
    private var killedCount = 0
    private var totalThisWave = 0

    private val difficultyMult = when (difficulty) {
        0 -> 0.8f   // Easy
        2 -> 1.5f   // Hard
        else -> 1f   // Normal
    }

    fun getWaveConfig(wave: Int): WaveConfig {
        val w = wave.coerceIn(0, 39)
        // Progressive scaling matching C++ Wave.cpp
        val baseHP = 50f + w * 30f + (w * w * 0.5f)
        val baseSpeed = 60f + (w * 1.5f).coerceAtMost(40f)
        val baseArmor = (w * 2f).coerceAtMost(60f)
        val baseMR = (w * 1.5f).coerceAtMost(40f)
        val count = 8 + (w * 1.2f).toInt().coerceAtMost(25)
        val isBoss = (w + 1) % 10 == 0  // Waves 10, 20, 30, 40
        val isMiniBoss = (w + 1) % 5 == 0 && !isBoss

        val enemyType = when {
            isBoss -> 4
            isMiniBoss -> 5
            w % 4 == 1 -> 1  // fast
            w % 4 == 2 -> 2  // tank
            w % 4 == 3 -> 3  // flying
            else -> 0         // normal
        }

        val bossMultHP = if (isBoss) 10f else if (isMiniBoss) 5f else 1f
        val bossMultArmor = if (isBoss) 2f else if (isMiniBoss) 1.5f else 1f

        return WaveConfig(
            enemyCount = if (isBoss) 1 else if (isMiniBoss) 3 else count,
            hp = baseHP * difficultyMult * bossMultHP,
            speed = if (isBoss) baseSpeed * 0.6f else baseSpeed,
            armor = baseArmor * bossMultArmor,
            magicResist = baseMR * bossMultArmor,
            isBoss = isBoss || isMiniBoss,
            spawnInterval = if (isBoss) 0f else 0.5f + (1f / (1 + w * 0.1f)),
            enemyType = enemyType,
        )
    }

    fun startWave(wave: Int) {
        currentWave = wave
        val config = getWaveConfig(wave)
        totalThisWave = config.enemyCount
        spawnedCount = 0
        killedCount = 0
        spawnTimer = 0f
        waveComplete = false
    }

    /** Returns number of enemies to spawn this frame */
    fun update(dt: Float): Int {
        if (waveComplete) return 0
        val config = getWaveConfig(currentWave)
        if (spawnedCount >= config.enemyCount) {
            if (killedCount >= config.enemyCount) waveComplete = true
            return 0
        }

        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            spawnTimer += config.spawnInterval
            spawnedCount++
            return 1
        }
        return 0
    }

    fun onEnemyKilled() { killedCount++ }
    fun allSpawned() = spawnedCount >= totalThisWave
    val isLastWave get() = currentWave >= maxWaves - 1
    val progress get() = if (totalThisWave > 0) killedCount.toFloat() / totalThisWave else 0f
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/jaygame/engine/WaveSystem.kt
git commit -m "feat: add WaveSystem with 40-wave progression"
```

---

### Task 9: Grid System

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/Grid.kt`

Port from C++ `Grid.h`. 6x5 grid for unit placement.

- [ ] **Step 1: Create Grid**

```kotlin
// Grid.kt
package com.example.jaygame.engine

import com.example.jaygame.engine.math.Vec2

class Grid {
    companion object {
        const val COLS = 6
        const val ROWS = 5
        const val TOTAL = 30
        const val CELL_W = 140f
        const val CELL_H = 88f
        const val ORIGIN_X = 200f  // Grid offset in 1280x720 world
        const val ORIGIN_Y = 140f
    }

    val cells = arrayOfNulls<GameUnit>(TOTAL)

    fun placeUnit(index: Int, unit: GameUnit): Boolean {
        if (index !in 0 until TOTAL || cells[index] != null) return false
        cells[index] = unit
        unit.tileIndex = index
        unit.homePosition = cellCenter(index)
        unit.position = unit.homePosition.copy()
        return true
    }

    fun removeUnit(index: Int): GameUnit? {
        val unit = cells[index] ?: return null
        cells[index] = null
        return unit
    }

    fun getUnit(index: Int): GameUnit? = cells.getOrNull(index)

    fun findEmpty(): Int {
        for (i in 0 until TOTAL) if (cells[i] == null) return i
        return -1
    }

    fun isFull() = cells.all { it != null }

    fun cellCenter(index: Int): Vec2 {
        val col = index % COLS
        val row = index / COLS
        return Vec2(
            ORIGIN_X + col * CELL_W + CELL_W * 0.5f,
            ORIGIN_Y + row * CELL_H + CELL_H * 0.5f,
        )
    }

    fun getCellAt(worldX: Float, worldY: Float): Int {
        val col = ((worldX - ORIGIN_X) / CELL_W).toInt()
        val row = ((worldY - ORIGIN_Y) / CELL_H).toInt()
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) return -1
        return row * COLS + col
    }

    /** Find all units matching given unitDefId for merge */
    fun findMergeCandidates(unitDefId: Int, grade: Int): List<Int> {
        return (0 until TOTAL).filter { i ->
            val u = cells[i]
            u != null && u.unitDefId == unitDefId && u.grade == grade
        }
    }

    fun unitCount(): Int = cells.count { it != null }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/jaygame/engine/Grid.kt
git commit -m "feat: add Grid system (6x5, 30 cells)"
```

---

### Task 10: Merge System

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/MergeSystem.kt`

Port from C++ `MergeSystem.h/cpp`. 3 same units merge to next grade, 5% lucky skip.

- [ ] **Step 1: Create MergeSystem**

```kotlin
// MergeSystem.kt
package com.example.jaygame.engine

import com.example.jaygame.data.UNIT_DEFS

object MergeSystem {
    private const val MERGE_COUNT = 3
    private const val LUCKY_CHANCE = 0.05f  // 5%

    data class MergeResult(
        val resultUnitDefId: Int,
        val consumedTiles: List<Int>,
        val isLucky: Boolean,
    )

    /**
     * Check if a merge is possible for the unit at given tile.
     * Returns MergeResult or null if not possible.
     */
    fun tryMerge(grid: Grid, tileIndex: Int): MergeResult? {
        val unit = grid.getUnit(tileIndex) ?: return null
        val candidates = grid.findMergeCandidates(unit.unitDefId, unit.grade)
        if (candidates.size < MERGE_COUNT) return null

        // Take the first 3 (including self)
        val consumed = candidates.take(MERGE_COUNT)

        // Determine result
        val unitDef = UNIT_DEFS.find { it.id == unit.unitDefId } ?: return null
        var resultId = unitDef.mergeResultId
        if (resultId < 0) return null  // Max grade

        // Lucky: skip one grade (5% chance)
        val isLucky = Math.random() < LUCKY_CHANCE
        if (isLucky) {
            val nextDef = UNIT_DEFS.find { it.id == resultId }
            if (nextDef != null && nextDef.mergeResultId >= 0) {
                resultId = nextDef.mergeResultId
            }
        }

        return MergeResult(resultId, consumed, isLucky)
    }

    /** Check which tiles have mergeable units (for canMerge flag) */
    fun findMergeableTiles(grid: Grid): Set<Int> {
        val mergeable = mutableSetOf<Int>()
        val checked = mutableSetOf<Int>()  // unitDefId+grade pairs

        for (i in 0 until Grid.TOTAL) {
            val unit = grid.getUnit(i) ?: continue
            val key = unit.unitDefId * 100 + unit.grade
            if (key in checked) continue
            checked.add(key)

            val candidates = grid.findMergeCandidates(unit.unitDefId, unit.grade)
            if (candidates.size >= MERGE_COUNT) {
                val def = UNIT_DEFS.find { it.id == unit.unitDefId }
                if (def != null && def.mergeResultId >= 0) {
                    mergeable.addAll(candidates)
                }
            }
        }
        return mergeable
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/jaygame/engine/MergeSystem.kt
git commit -m "feat: add MergeSystem (3-to-1 with 5% lucky skip)"
```

---

### Task 11: Ability System

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/AbilitySystem.kt`

Port from C++ `Ability.h/cpp`. Handles projectile hit effects and aura buffs.

- [ ] **Step 1: Create AbilitySystem**

```kotlin
// AbilitySystem.kt
package com.example.jaygame.engine

import com.example.jaygame.engine.math.GameRect
import com.example.jaygame.engine.math.Vec2

object AbilitySystem {
    private const val AURA_RADIUS = 150f
    private const val AURA_TICK = 0.5f

    /**
     * Apply ability effect when a projectile hits an enemy.
     * Returns list of secondary projectiles to spawn (for chain abilities).
     */
    fun onProjectileHit(
        proj: Projectile,
        enemy: Enemy,
        spatialHash: SpatialHash<Enemy>,
        spawnProjectile: (from: Vec2, target: Enemy, damage: Float, type: Int) -> Unit,
    ) {
        // 1. Apply base damage
        val actualDmg = enemy.takeDamage(proj.damage, proj.isMagic)

        when (proj.abilityType) {
            1 -> { // Splash
                val splashRadius = proj.abilityValue.coerceAtLeast(60f)
                val rect = GameRect(
                    enemy.position.x - splashRadius,
                    enemy.position.y - splashRadius,
                    splashRadius * 2, splashRadius * 2,
                )
                spatialHash.query(rect).forEach { nearby ->
                    if (nearby !== enemy && nearby.alive) {
                        val dist = nearby.position.distanceTo(enemy.position)
                        if (dist <= splashRadius) {
                            nearby.takeDamage(proj.damage * 0.5f, proj.isMagic)
                        }
                    }
                }
            }
            2 -> { // Slow
                enemy.buffs.addBuff(BuffType.Slow, proj.abilityValue, 2f, proj.sourceUnitId)
            }
            3 -> { // DoT
                enemy.buffs.addBuff(BuffType.DoT, proj.abilityValue, 3f, proj.sourceUnitId)
            }
            4 -> { // Chain
                val chainCount = proj.abilityValue.toInt().coerceIn(1, 5)
                val rect = GameRect(
                    enemy.position.x - 200f, enemy.position.y - 200f, 400f, 400f,
                )
                var chained = 0
                spatialHash.query(rect).forEach { nearby ->
                    if (chained < chainCount && nearby !== enemy && nearby.alive) {
                        spawnProjectile(enemy.position, nearby, proj.damage * 0.7f, 4)
                        chained++
                    }
                }
            }
            5 -> { // Buff (Support) — handled by aura, not hit
            }
            6 -> { // Debuff (ArmorBreak)
                enemy.buffs.addBuff(BuffType.ArmorBreak, proj.abilityValue, 3f, proj.sourceUnitId)
            }
            9 -> { // Execute — bonus damage to low HP
                if (enemy.hpRatio < 0.3f) {
                    enemy.takeDamage(proj.damage * 2f, proj.isMagic)
                }
            }
        }
    }

    /**
     * Apply aura effects from support/buff units to nearby allies.
     */
    fun applyAuraEffects(
        units: List<GameUnit>,
        dt: Float,
        auraTick: FloatArray, // per-unit aura timer
    ) {
        for (i in units.indices) {
            val unit = units[i]
            if (!unit.alive) continue
            // Only support family (4) or high-grade units with aura
            if (unit.abilityType != 5 && unit.abilityType != 8) continue

            auraTick[i] += dt
            if (auraTick[i] < AURA_TICK) continue
            auraTick[i] -= AURA_TICK

            for (other in units) {
                if (other === unit || !other.alive) continue
                val dist = unit.position.distanceTo(other.position)
                if (dist <= AURA_RADIUS) {
                    when (unit.abilityType) {
                        5 -> { // ATK buff
                            other.buffs.addBuff(BuffType.AtkUp, unit.abilityValue, 1f, i)
                        }
                        8 -> { // Shield
                            if (!other.buffs.hasBuff(BuffType.Shield)) {
                                other.buffs.addBuff(
                                    BuffType.Shield,
                                    unit.effectiveATK() * 0.5f,
                                    5f, i,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/jaygame/engine/AbilitySystem.kt
git commit -m "feat: add AbilitySystem (splash, slow, DoT, chain, aura)"
```

---

## Chunk 4: Battle Engine (Core Game Loop)

### Task 12: BattleEngine — The Core Game Loop

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/BattleEngine.kt`
- Modify: `app/src/main/java/com/example/jaygame/bridge/BattleBridge.kt` — add engine reference

This is the biggest task. Replaces `BattleScene.cpp` (1290 lines). The BattleEngine runs a 60Hz coroutine loop, manages all entities, and pushes state to BattleBridge StateFlows.

- [ ] **Step 1: Create BattleEngine class — state and initialization**

```kotlin
// BattleEngine.kt
package com.example.jaygame.engine

import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.bridge.BattleResultData
import com.example.jaygame.bridge.BattleState
import com.example.jaygame.bridge.DamageEvent
import com.example.jaygame.bridge.EnemyPositionData
import com.example.jaygame.bridge.GridTileState
import com.example.jaygame.bridge.ProjectileData
import com.example.jaygame.bridge.UnitPositionData
import com.example.jaygame.data.UNIT_DEFS
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
        const val BASE_SUMMON_COST = 50
        const val WAVE_DELAY = 3f
    }

    // State machine
    enum class State { WaveDelay, Playing, Victory, Defeat }
    var state = State.WaveDelay; private set

    // Pools
    val enemies = ObjectPool(MAX_ENEMIES, { Enemy() }) { it.reset() }
    val units = ObjectPool(MAX_UNITS, { GameUnit() }) { it.reset() }
    val projectiles = ObjectPool(MAX_PROJECTILES, { Projectile() }) { it.reset() }

    // Systems
    val grid = Grid()
    val waveSystem = WaveSystem(maxWaves, difficulty)
    val spatialHash = SpatialHash<Enemy>(64f)
    private val auraTicks = FloatArray(MAX_UNITS)

    // Economy
    var sp = 100f; private set
    var summonCost = BASE_SUMMON_COST; private set
    var elapsedTime = 0f; private set

    // Wave state
    private var waveDelayTimer = WAVE_DELAY
    private var isBossRound = false
    private var bossTimeRemaining = 0f

    // Stats
    private var killCount = 0
    private var mergeCount = 0

    // Battle upgrades
    private val upgradeLevels = IntArray(5) // ATK, AtkSpd, Crit, Range, SPRegen
    private var upgradeAtkMult = 1f
    private var upgradeSpdMult = 1f
    private var upgradeCritRate = 0f
    private var upgradeRangeMult = 1f
    private var upgradeSpRegen = 0f

    // Push timers
    private var gridPushTimer = 0f

    // Enemy path (donut loop)
    val enemyPath: List<Vec2> = buildEnemyPath()

    private var job: Job? = null

    private fun buildEnemyPath(): List<Vec2> {
        // Rectangular donut path around the field
        val margin = 40f
        val left = margin
        val right = W - margin
        val top = margin
        val bottom = H - margin
        return listOf(
            Vec2(left, top),
            Vec2(right, top),
            Vec2(right, bottom),
            Vec2(left, bottom),
        )
    }

    // ... (continued in next steps)
}
```

- [ ] **Step 2: Add game loop and update methods**

Add to `BattleEngine.kt`:

```kotlin
    fun start(scope: CoroutineScope) {
        job = scope.launch(Dispatchers.Default) {
            var accumulator = 0f
            var lastTime = System.nanoTime()

            while (isActive) {
                val now = System.nanoTime()
                val frameDt = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.1f)
                lastTime = now
                accumulator += frameDt

                while (accumulator >= FIXED_DT) {
                    update(FIXED_DT)
                    accumulator -= FIXED_DT
                }

                pushStateToCompose()
                delay(8) // ~120Hz push rate, Compose lerps the rest
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

                if (isBossRound) {
                    bossTimeRemaining -= dt
                    if (bossTimeRemaining <= 0f) {
                        // Kill remaining boss enemies
                        enemies.forEach { if (it.alive) it.alive = false }
                    }
                }

                // Check defeat: too many enemies
                if (enemies.activeCount >= DEFEAT_ENEMY_COUNT) {
                    state = State.Defeat
                    onBattleEnd(false)
                }

                // Check wave complete
                if (waveSystem.waveComplete) {
                    if (waveSystem.isLastWave) {
                        state = State.Victory
                        onBattleEnd(true)
                    } else {
                        waveSystem.startWave(waveSystem.currentWave + 1)
                        waveDelayTimer = WAVE_DELAY
                        state = State.WaveDelay
                    }
                }
            }
            State.Victory, State.Defeat -> { /* frozen */ }
        }

        gridPushTimer += dt
    }
```

- [ ] **Step 3: Add entity update methods**

```kotlin
    private fun updateSpawning(dt: Float) {
        val toSpawn = waveSystem.update(dt)
        val config = waveSystem.getWaveConfig(waveSystem.currentWave)
        repeat(toSpawn) {
            val enemy = enemies.acquire() ?: return
            enemy.init(
                hp = config.hp,
                speed = config.speed,
                armor = config.armor,
                magicResist = config.magicResist,
                type = config.enemyType,
                startPos = enemyPath.first().copy(),
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
        // Remove dead enemies
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

        units.forEach { unit ->
            if (!unit.alive) return@forEach
            unit.update(dt) { pos, range ->
                findNearestEnemy(pos, range)
            }

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
            val wasAlive = proj.update(dt)
            if (!wasAlive && proj.target?.alive == true) {
                // Hit
                AbilitySystem.onProjectileHit(proj, proj.target!!, spatialHash) { from, target, dmg, type ->
                    val chain = projectiles.acquire() ?: return@onProjectileHit
                    chain.init(from, target, dmg, 400f, type, false, false, -1, 0, 0f, 0, 0)
                }
                // Push damage event
                val nx = proj.target!!.position.x / W
                val ny = proj.target!!.position.y / H
                BattleBridge.onDamageDealt(nx, ny, proj.damage.toInt(), if (proj.isCrit) 1 else 0)
            }
            if (!proj.alive) deadProj.add(proj)
        }
        deadProj.forEach { projectiles.release(it) }
    }

    private fun fireProjectile(unit: GameUnit) {
        val target = unit.currentTarget ?: return
        val proj = projectiles.acquire() ?: return

        val isCrit = Math.random() < (0.05 + upgradeCritRate)
        val dmg = unit.effectiveATK() * upgradeAtkMult * (if (isCrit) 2f else 1f)
        val isMagic = unit.family == 1 || unit.family == 4  // Frost, Support

        proj.init(
            from = unit.position.copy(),
            target = target,
            damage = dmg,
            speed = 400f,
            type = unit.family.coerceIn(0, 5),
            isMagic = isMagic,
            isCrit = isCrit,
            sourceUnitId = unit.tileIndex,
            abilityType = unit.abilityType,
            abilityValue = unit.abilityValue,
            grade = unit.grade,
            family = unit.family,
        )
    }

    private fun findNearestEnemy(pos: Vec2, range: Float): Enemy? {
        val rect = com.example.jaygame.engine.math.GameRect(
            pos.x - range, pos.y - range, range * 2, range * 2,
        )
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
```

- [ ] **Step 4: Add player action handlers (summon, merge, sell, etc.)**

```kotlin
    // ── Player Actions ──

    fun requestSummon() {
        if (sp < summonCost || grid.isFull()) return
        sp -= summonCost

        // Roll random unit from deck
        val grade = rollGrade()
        val familyIndex = deck.random()
        val unitDefId = grade * 5 + familyIndex

        val tileIndex = grid.findEmpty()
        if (tileIndex < 0) return

        val def = UNIT_DEFS.find { it.id == unitDefId } ?: return
        val unit = units.acquire() ?: return
        unit.init(
            unitDefId = unitDefId,
            grade = grade,
            family = familyIndex,
            level = 1,
            tileIndex = tileIndex,
            homePos = grid.cellCenter(tileIndex),
            baseATK = def.baseATK.toFloat(),
            atkSpeed = def.baseSpeed,
            range = def.range.toFloat() * upgradeRangeMult,
            abilityType = def.abilityTypeOrdinal,
            abilityValue = def.abilityValue,
        )
        grid.placeUnit(tileIndex, unit)

        BattleBridge.onSummonResult(unitDefId, grade)
    }

    private fun rollGrade(): Int {
        val r = Math.random() * 100
        return when {
            r < 60 -> 0   // Common
            r < 85 -> 1   // Rare
            r < 97 -> 2   // Hero
            else -> 3     // Legend
        }
    }

    fun requestMerge(tileIndex: Int) {
        val result = MergeSystem.tryMerge(grid, tileIndex) ?: return
        // Remove consumed units
        result.consumedTiles.forEach { i ->
            val u = grid.removeUnit(i)
            if (u != null) units.release(u)
        }
        // Place new unit
        val def = UNIT_DEFS.find { it.id == result.resultUnitDefId } ?: return
        val unit = units.acquire() ?: return
        val newGrade = result.resultUnitDefId / 5
        val newFamily = result.resultUnitDefId % 5
        unit.init(
            unitDefId = result.resultUnitDefId,
            grade = newGrade,
            family = newFamily,
            level = 1,
            tileIndex = tileIndex,
            homePos = grid.cellCenter(tileIndex),
            baseATK = def.baseATK.toFloat(),
            atkSpeed = def.baseSpeed,
            range = def.range.toFloat() * upgradeRangeMult,
            abilityType = def.abilityTypeOrdinal,
            abilityValue = def.abilityValue,
        )
        grid.placeUnit(tileIndex, unit)
        mergeCount++
        BattleBridge.onMergeComplete(tileIndex, if (result.isLucky) 1 else 0, result.resultUnitDefId)
    }

    fun requestSell(tileIndex: Int) {
        val unit = grid.removeUnit(tileIndex) ?: return
        sp += 30f + unit.grade * 20f  // Refund based on grade
        units.release(unit)
    }

    fun requestClickTile(tileIndex: Int) {
        val unit = grid.getUnit(tileIndex) ?: return
        val mergeable = MergeSystem.findMergeableTiles(grid)
        BattleBridge.onUnitClicked(
            tileIndex, unit.unitDefId, unit.grade, unit.family,
            if (tileIndex in mergeable) 1 else 0, unit.level,
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
            // Return to original position
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
        val grade = unitDefId / 5
        val family = unitDefId % 5
        val unit = units.acquire() ?: return
        unit.init(
            unitDefId = unitDefId,
            grade = grade,
            family = family,
            level = 1,
            tileIndex = tileIndex,
            homePos = grid.cellCenter(tileIndex),
            baseATK = def.baseATK.toFloat(),
            atkSpeed = def.baseSpeed,
            range = def.range.toFloat() * upgradeRangeMult,
            abilityType = def.abilityTypeOrdinal,
            abilityValue = def.abilityValue,
        )
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
        BattleBridge.updateBattleUpgradeLevels(upgradeLevels)
    }
```

- [ ] **Step 5: Add state push to Compose**

```kotlin
    // ── Push State to Compose ──

    private fun pushStateToCompose() {
        // Battle state
        val mergeable = MergeSystem.findMergeableTiles(grid)
        BattleBridge.updateState(
            waveSystem.currentWave + 1, maxWaves,
            (DEFEAT_ENEMY_COUNT - enemies.activeCount).coerceAtLeast(0),
            DEFEAT_ENEMY_COUNT,
            sp, elapsedTime,
            state.ordinal, summonCost,
            enemies.activeCount,
            isBossRound, bossTimeRemaining,
        )

        // Enemy positions (normalized)
        val eCount = enemies.activeCount
        if (eCount > 0) {
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
        } else {
            BattleBridge.updateEnemyPositions(FloatArray(0), FloatArray(0), IntArray(0), FloatArray(0), 0)
        }

        // Unit positions (normalized)
        val uCount = units.activeCount
        if (uCount > 0) {
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
        } else {
            BattleBridge.updateUnitPositions(
                FloatArray(0), FloatArray(0), IntArray(0), IntArray(0),
                IntArray(0), BooleanArray(0), IntArray(0), 0,
            )
        }

        // Projectiles (normalized)
        val pCount = projectiles.activeCount
        if (pCount > 0) {
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
        } else {
            BattleBridge.updateProjectiles(FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0), IntArray(0), 0)
        }

        // Grid state (every 0.1s)
        if (gridPushTimer >= 0.1f) {
            gridPushTimer = 0f
            val ids = IntArray(Grid.TOTAL)
            val grades = IntArray(Grid.TOTAL)
            val families = IntArray(Grid.TOTAL)
            val canMerge = IntArray(Grid.TOTAL)
            val levels = IntArray(Grid.TOTAL)
            for (i in 0 until Grid.TOTAL) {
                val u = grid.getUnit(i)
                if (u != null) {
                    ids[i] = u.unitDefId
                    grades[i] = u.grade
                    families[i] = u.family
                    canMerge[i] = if (i in mergeable) 1 else 0
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
        BattleBridge.onBattleEnd(
            victory, waveSystem.currentWave + 1,
            goldEarned, trophyChange,
            killCount, mergeCount, 0,
        )
    }
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/jaygame/engine/BattleEngine.kt
git commit -m "feat: add BattleEngine — full game loop replacing C++ BattleScene"
```

---

## Chunk 5: Bridge Refactor & Integration

### Task 13: Refactor BattleBridge — Remove JNI, Add Direct Kotlin Calls

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/bridge/BattleBridge.kt`

The key change: BattleBridge callback methods (`updateState`, `updateEnemyPositions`, etc.) are currently `@JvmStatic` for C++ JNI calls. They stay the same signature but are now called directly from `BattleEngine` Kotlin code. Remove the `external` native method declarations.

- [ ] **Step 1: Remove JNI native method declarations from BattleBridge**

In `BattleBridge.kt`, find and remove all `external fun native*` declarations:

```kotlin
// DELETE these lines:
external fun nativeSummon()
external fun nativeClickTile(tileIndex: Int)
external fun nativeMergeUnit(tileIndex: Int)
external fun nativeSellUnit(tileIndex: Int)
external fun nativeUpgradeUnit(tileIndex: Int)
external fun nativeSwapUnits(from: Int, to: Int)
external fun nativeRelocateUnit(tile: Int, normX: Float, normY: Float)
external fun nativeGamble(newSp: Float)
external fun nativeBuyUnit(unitDefId: Int, cost: Float)
external fun nativeApplyBattleUpgrade(type: Int, level: Int, cost: Float)
```

- [ ] **Step 2: Add engine reference and redirect request methods**

Replace the `request*` methods to call `BattleEngine` directly instead of JNI:

```kotlin
// Add to BattleBridge object:
var engine: BattleEngine? = null

fun requestSummon() { engine?.requestSummon() }
fun requestMerge(tileIndex: Int) { engine?.requestMerge(tileIndex) }
fun requestSell(tileIndex: Int) { engine?.requestSell(tileIndex) }
fun requestClickTile(tileIndex: Int) { engine?.requestClickTile(tileIndex) }
fun requestUpgrade(tileIndex: Int) { engine?.requestUpgrade(tileIndex) }
fun requestSwap(from: Int, to: Int) { engine?.requestSwap(from, to) }
fun requestRelocate(tileIndex: Int, normX: Float, normY: Float) {
    engine?.requestRelocate(tileIndex, normX, normY)
}
fun requestBuyUnit(unitDefId: Int, cost: Float) {
    engine?.requestBuyUnit(unitDefId, cost)
}
fun requestBattleUpgrade(type: Int, cost: Float) {
    val levels = _battleUpgradeLevels.value
    val typeIdx = type.coerceIn(0, 4)
    val newLevel = levels[typeIdx] + 1
    engine?.applyBattleUpgrade(typeIdx, newLevel, cost)
}
```

- [ ] **Step 3: Ensure UnitDefs has required fields**

Check that `UNIT_DEFS` entries have `abilityTypeOrdinal` and `abilityValue` fields needed by `BattleEngine`. If not present, add them.

Verify in `app/src/main/java/com/example/jaygame/data/UnitDefs.kt` — the `UnitDef` data class needs:
- `abilityTypeOrdinal: Int` (0=None, 1=Splash, 2=Slow, 3=DoT, 4=Chain, 5=Buff, 6=Debuff, 8=Shield, 9=Execute)
- `abilityValue: Float`
- `baseSpeed: Float` (attack speed)

If these fields already exist under different names, map accordingly. If missing, add them to each unit definition matching C++ `UnitData.h` values.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/jaygame/bridge/BattleBridge.kt
git add app/src/main/java/com/example/jaygame/data/UnitDefs.kt
git commit -m "refactor: remove JNI from BattleBridge, route actions to BattleEngine"
```

---

### Task 14: Replace MainActivity — ComponentActivity Instead of GameActivity

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/MainActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

Replace `GameActivity` (C++ native) with `ComponentActivity` (pure Compose). No more C++ SurfaceView.

- [ ] **Step 1: Rewrite MainActivity**

```kotlin
package com.example.jaygame

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.jaygame.audio.BgmManager
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.STAGES
import com.example.jaygame.engine.BattleEngine
import com.example.jaygame.ui.battle.BattleScreen
import com.example.jaygame.ui.theme.JayGameTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {
    private lateinit var repository: GameRepository
    private var engine: BattleEngine? = null
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        repository = GameRepository(this)

        BattleBridge.reset()

        // Create and start Kotlin battle engine
        val stageId = BattleBridge.stageId.value
        val difficulty = BattleBridge.difficulty.value
        val stage = STAGES.getOrNull(stageId) ?: STAGES[0]
        val data = repository.gameData.value
        engine = BattleEngine(
            stageId = stageId,
            difficulty = difficulty,
            maxWaves = stage.maxWaves,
            deck = data.deck.toIntArray(),
        ).also {
            BattleBridge.engine = it
            it.start(engineScope)
        }

        // Play battle BGM
        if (data.musicEnabled) {
            BgmManager.play(this, "audio/battle_bgm.mp3")
        }

        setContent {
            JayGameTheme {
                val result by BattleBridge.result.collectAsState()
                BattleScreen(
                    result = result,
                    onGoHome = {
                        BattleBridge.clearResult()
                        finish()
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        engine?.stop()
        BattleBridge.engine = null
        BgmManager.stop()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
```

- [ ] **Step 2: Update AndroidManifest.xml**

Remove the `android.app.lib_name` meta-data from MainActivity:

```xml
<!-- BEFORE -->
<activity
    android:name=".MainActivity"
    android:exported="false"
    android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|keyboard|density|uiMode|smallestScreenSize|navigation"
    android:launchMode="singleTop">
    <meta-data
        android:name="android.app.lib_name"
        android:value="jaygame" />
</activity>

<!-- AFTER -->
<activity
    android:name=".MainActivity"
    android:exported="false"
    android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
    android:launchMode="singleTop"
    android:theme="@style/Theme.JayGame" />
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/jaygame/MainActivity.kt
git add app/src/main/AndroidManifest.xml
git commit -m "refactor: replace GameActivity with ComponentActivity + Kotlin BattleEngine"
```

---

## Chunk 6: Remove C++ and NDK

### Task 15: Remove C++ Source and NDK Build Configuration

**Files:**
- Delete: `app/src/main/cpp/` (entire directory — 39 .cpp/.h files)
- Modify: `app/build.gradle.kts` — remove `externalNativeBuild` block
- Delete: `app/src/main/java/com/example/jaygame/SaveBridge.kt` (no longer needed, C++ called this)

- [ ] **Step 1: Remove externalNativeBuild from build.gradle.kts**

In `app/build.gradle.kts`, remove:
```kotlin
externalNativeBuild {
    cmake {
        path = file("src/main/cpp/CMakeLists.txt")
        version = "3.22.1"
    }
}
```

And remove from `defaultConfig`:
```kotlin
externalNativeBuild {
    cmake {
        cppFlags += "-std=c++17"
    }
}
```

- [ ] **Step 2: Remove GameActivity dependency from build.gradle.kts**

Remove:
```kotlin
implementation("androidx.games:games-activity:...")
```

- [ ] **Step 3: Remove System.loadLibrary call**

If there is any remaining `System.loadLibrary("jaygame")` call (previously in old MainActivity), ensure it's removed.

- [ ] **Step 4: Delete C++ source directory**

```bash
rm -rf app/src/main/cpp/
```

- [ ] **Step 5: Delete SaveBridge.kt if it only served C++ JNI**

```bash
rm app/src/main/java/com/example/jaygame/SaveBridge.kt
```

- [ ] **Step 6: Build and verify**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL with no NDK compilation step.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "chore: remove C++ source, NDK build config, and GameActivity dependency"
```

---

### Task 16: Final Integration Testing

- [ ] **Step 1: Launch app and verify splash screen**
  - App should launch with dark background (no white flash)
  - Splash screen shows for 1.5s
  - Home screen loads with BGM playing

- [ ] **Step 2: Navigate all screens**
  - Home, Deck, Collection, Shop, Settings
  - BGM should continue playing across all screens

- [ ] **Step 3: Start a battle**
  - Battle screen loads (no C++ SurfaceView, pure Compose)
  - BGM switches to battle music
  - Units can be summoned
  - Enemies spawn and follow path
  - Projectiles fire and hit enemies
  - Damage numbers appear

- [ ] **Step 4: Test battle mechanics**
  - Summon multiple units → verify grid placement
  - Merge 3 same units → verify upgrade
  - Gamble SP → verify SP changes
  - Buy unit → verify placement
  - Upgrade unit → verify level increase
  - Drag relocate unit → verify new position

- [ ] **Step 5: Complete a battle**
  - Play through waves → verify wave progression
  - Result screen shows → verify stats
  - Return home → verify BGM switches back

- [ ] **Step 6: Fix any issues found during testing**

- [ ] **Step 7: Final commit**

```bash
git add -A
git commit -m "feat: complete C++ to Kotlin migration — pure Kotlin game engine"
```

---

## Summary

| Task | Component | Est. Lines |
|------|-----------|-----------|
| 1 | Vec2 + GameRect | ~60 |
| 2 | SpatialHash | ~40 |
| 3 | ObjectPool | ~50 |
| 4 | BuffSystem | ~100 |
| 5 | Enemy | ~90 |
| 6 | Projectile | ~80 |
| 7 | GameUnit | ~150 |
| 8 | WaveSystem | ~100 |
| 9 | Grid | ~80 |
| 10 | MergeSystem | ~60 |
| 11 | AbilitySystem | ~100 |
| 12 | BattleEngine | ~400 |
| 13 | BattleBridge refactor | ~50 (modify) |
| 14 | MainActivity rewrite | ~70 (modify) |
| 15 | Remove C++ / NDK | deletion |
| 16 | Integration testing | manual |

**Total new Kotlin:** ~1,400 lines replacing ~6,000 lines of C++ + JNI bridge

**Key risk:** Wave balance and damage calculation must exactly match C++ behavior. The wave config values in Task 8 are approximations — verify against C++ `Wave.cpp` exact numbers during implementation.
