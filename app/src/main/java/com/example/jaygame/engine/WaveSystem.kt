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
)

class WaveSystem(private val maxWaves: Int, private val difficulty: Int) {
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
        val w = wave.coerceIn(0, 39)
        val baseHP = 50f + w * 30f + (w * w * 0.5f)
        val baseSpeed = 60f + (w * 1.5f).coerceAtMost(40f)
        val baseArmor = (w * 2f).coerceAtMost(60f)
        val baseMR = (w * 1.5f).coerceAtMost(40f)
        val count = 8 + (w * 1.2f).toInt().coerceAtMost(25)
        val isBoss = (w + 1) % 10 == 0
        val isMiniBoss = (w + 1) % 5 == 0 && !isBoss

        val enemyType = when {
            isBoss -> 4
            isMiniBoss -> 5
            w % 4 == 1 -> 1
            w % 4 == 2 -> 2
            w % 4 == 3 -> 3
            else -> 0
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
