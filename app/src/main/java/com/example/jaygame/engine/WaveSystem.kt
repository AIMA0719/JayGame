package com.example.jaygame.engine

data class WaveConfig(
    val enemyCount: Int,
    val hp: Float,
    val speed: Float,
    val armor: Float,
    val magicResist: Float,
    val isBoss: Boolean,
    val spawnInterval: Float,
    val enemyType: Int,
    val ccResistance: Float = 0f,
    val eliteChance: Float = 0f, // 0-1, chance each normal enemy is elite (2x HP, 1.5x armor)
)

class WaveSystem(private val maxWaves: Int, private val difficulty: Int, val forceBoss: Boolean = false) {
    var currentWave = 0; private set
    var waveComplete = false; private set
    private var spawnTimer = 0f
    private var spawnedCount = 0
    private var waveTimer = 0f

    companion object {
        const val WAVE_DURATION = 180f // 3 minutes per wave
        const val BOSS_DURATION = 300f // 5 minutes for boss waves
    }

    private val difficultyMult = when (difficulty) {
        0 -> 1f       // 초보
        1 -> 1.5f     // 숙련자
        2 -> 2.2f     // 고인물
        3 -> 3.0f     // 썩은물
        4 -> 4.0f     // 챌린저
        else -> 1f
    }

    fun getWaveConfig(wave: Int): WaveConfig {
        // 60-wave standard: base values scale up to 59, late-game (40+) gets extra scaling
        val w = wave
        val capped = w.coerceAtMost(59)

        // Late-game scaling for waves 40+
        val lateScale = if (w >= 40) 1f + (w - 39) * 0.12f else 1f

        val baseHP = (50f + capped * 30f + (capped * capped * 0.5f)) * lateScale
        val baseSpeed = (60f + (capped * 1.2f).coerceAtMost(50f)) *
            (if (w >= 40) 1f + (w - 39) * 0.015f else 1f).coerceAtMost(1.4f)
        val baseArmor = ((capped * 1.8f).coerceAtMost(80f) + if (w >= 40) (w - 39) * 2.5f else 0f)
        val baseMR = ((capped * 1.3f).coerceAtMost(50f) + if (w >= 40) (w - 39) * 1.5f else 0f)

        // Count scales with wave progression
        val baseCount = 8 + (capped * 1.0f).toInt().coerceAtMost(25)
        val lateCount = if (w >= 40) baseCount + (w - 39) / 4 else baseCount
        val count = lateCount.coerceAtMost(40)

        val isBoss = forceBoss || (w + 1) % 10 == 0
        val isMiniBoss = !forceBoss && (w + 1) % 5 == 0 && !isBoss

        // 6 regular types (0-4, 6) + type 5 (miniboss)
        val enemyType = when {
            isBoss -> 4
            isMiniBoss -> 5
            w >= 30 -> {
                val cycle = w % 6
                if (cycle <= 4) cycle else 6
            }
            w >= 20 && w % 5 == 0 -> 6  // 드래곤: w=20,25 (표시 웨이브 21,26)
            w % 4 == 1 -> 1
            w % 4 == 2 -> 2
            w % 4 == 3 -> 3
            else -> 0
        }

        val bossMultHP = when {
            isBoss && w >= 40 -> 15f   // Late-game bosses are tankier
            isBoss -> 10f
            isMiniBoss && w >= 30 -> 7f
            isMiniBoss -> 5f
            else -> 1f
        }
        val bossMultArmor = when {
            isBoss && w >= 40 -> 3f
            isBoss -> 2f
            isMiniBoss -> 1.5f
            else -> 1f
        }

        // CC resistance scales with wave progression
        val baseCcResist = when {
            isBoss -> 0.5f + (w / 120f).coerceAtMost(0.3f)
            isMiniBoss -> 0.3f + (w / 120f).coerceAtMost(0.2f)
            w >= 35 -> 0.1f + (w - 35) * 0.008f
            else -> 0f
        }
        val ccResistance = (baseCcResist + difficulty * 0.05f).coerceAtMost(0.9f)

        // Elite chance increases in later waves
        val eliteChance = when {
            isBoss || isMiniBoss -> 0f // bosses don't have elite variants
            w >= 45 -> 0.3f
            w >= 30 -> 0.2f
            w >= 20 -> 0.1f
            else -> 0f
        }

        return WaveConfig(
            enemyCount = if (isBoss) 1 else if (isMiniBoss) 3 else count,
            hp = baseHP * difficultyMult * bossMultHP,
            speed = if (isBoss) baseSpeed * 0.6f else baseSpeed,
            armor = baseArmor * bossMultArmor,
            magicResist = baseMR * bossMultArmor,
            isBoss = isBoss || isMiniBoss,
            spawnInterval = if (isBoss) 0f else (0.5f + (1f / (1 + w * 0.1f))).coerceAtLeast(0.2f),
            enemyType = enemyType,
            ccResistance = ccResistance,
            eliteChance = eliteChance,
        )
    }

    fun startWave(wave: Int) {
        currentWave = wave
        val config = getWaveConfig(wave)
        spawnedCount = 0
        spawnTimer = 0f
        waveTimer = if (config.isBoss) BOSS_DURATION else WAVE_DURATION
        waveComplete = false
    }

    /** Returns number of enemies to spawn this tick. Wave completes when timer runs out. */
    fun update(dt: Float): Int {
        if (waveComplete) return 0

        // Wave timer countdown
        waveTimer -= dt
        if (waveTimer <= 0f) {
            waveComplete = true
            return 0
        }

        // Spawn enemies until count reached
        val config = getWaveConfig(currentWave)
        if (spawnedCount >= config.enemyCount) return 0

        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            spawnTimer += config.spawnInterval
            spawnedCount++
            return 1
        }
        return 0
    }

    /** All enemies for this wave have been spawned */
    val allSpawned get() = spawnedCount >= getWaveConfig(currentWave).enemyCount

    fun forceComplete() { waveComplete = true }

    fun advanceWave() { currentWave++ }
    val isLastWave get() = currentWave >= maxWaves - 1
    val timeRemaining get() = waveTimer
}
