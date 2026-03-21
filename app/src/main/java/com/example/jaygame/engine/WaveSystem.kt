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
        // Support infinite scaling for survival mode (wave > 39)
        val w = wave
        val capped = w.coerceAtMost(39) // base values cap at 39, then scale exponentially

        // Exponential scaling for survival (waves beyond 40)
        val survivalScale = if (w > 39) 1f + (w - 39) * 0.15f else 1f

        val baseHP = (65f + capped * 39f + (capped * capped * 0.65f)) * survivalScale
        val baseSpeed = (60f + (capped * 1.5f).coerceAtMost(40f)) *
            (if (w > 39) 1f + (w - 39) * 0.02f else 1f).coerceAtMost(1.5f)
        val baseArmor = ((capped * 2f).coerceAtMost(60f) + if (w > 39) (w - 39) * 3f else 0f)
        val baseMR = ((capped * 1.5f).coerceAtMost(40f) + if (w > 39) (w - 39) * 2f else 0f)

        // Count scales more aggressively in later waves
        val baseCount = 8 + (capped * 1.2f).toInt().coerceAtMost(25)
        val survivalCount = if (w > 39) baseCount + (w - 39) / 3 else baseCount
        val count = survivalCount.coerceAtMost(40)

        val isBoss = forceBoss || (w + 1) % 10 == 0
        val isMiniBoss = !forceBoss && (w + 1) % 5 == 0 && !isBoss

        // More diverse enemy types in later waves
        val enemyType = when {
            isBoss -> 4
            isMiniBoss -> 5
            w >= 30 -> w % 6          // 6 enemy type rotation in late game
            w % 4 == 1 -> 1
            w % 4 == 2 -> 2
            w % 4 == 3 -> 3
            else -> 0
        }

        val bossMultHP = when {
            isBoss && w >= 30 -> 15f   // Late-game bosses are tankier
            isBoss -> 10f
            isMiniBoss && w >= 20 -> 7f
            isMiniBoss -> 5f
            else -> 1f
        }
        val bossMultArmor = when {
            isBoss && w >= 30 -> 3f
            isBoss -> 2f
            isMiniBoss -> 1.5f
            else -> 1f
        }

        // CC resistance scales with wave progression
        val baseCcResist = when {
            isBoss -> 0.5f + (w / 100f).coerceAtMost(0.3f)
            isMiniBoss -> 0.3f + (w / 100f).coerceAtMost(0.2f)
            w >= 30 -> 0.1f + (w - 30) * 0.01f
            else -> 0f
        }
        val ccResistance = (baseCcResist + difficulty * 0.05f).coerceAtMost(0.9f)

        // Elite chance increases in later waves
        val eliteChance = when {
            isBoss || isMiniBoss -> 0f // bosses don't have elite variants
            w >= 35 -> 0.3f
            w >= 25 -> 0.2f
            w >= 15 -> 0.1f
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
