package com.jay.jaygame.engine

/** 특수 웨이브 이벤트 타입 */
enum class SpecialWaveType {
    NONE,
    RUSH,           // 웨이브 13: 80마리, HP 절반, 속도 1.5배
    HEAVY_ARMOR,    // 웨이브 22: 20마리, HP 3배, 방어 3배, 속도 0.5배
    DARKNESS,       // 웨이브 33: 사거리 30% 감소 (유닛 range에 적용)
    ELITE_MARCH,    // 웨이브 43: 전부 엘리트
    LAST_CHARGE,    // 웨이브 53: 60마리, 엘리트 50%, 속도 1.5배
}

data class WaveConfig(
    val enemyCount: Int,
    val hp: Float,
    val speed: Float,
    val armor: Float,
    val magicResist: Float,
    val isBoss: Boolean,
    val isMiniBoss: Boolean = false,
    val spawnInterval: Float,
    val enemyType: Int,
    val ccResistance: Float = 0f,
    val eliteChance: Float = 0f,
    val specialWave: SpecialWaveType = SpecialWaveType.NONE,
)

class WaveSystem(private val maxWaves: Int, private val difficulty: Int, val forceBoss: Boolean = false) {
    var currentWave = 0; private set
    var waveComplete = false; private set
    private var spawnTimer = 0f
    private var spawnedCount = 0
    private var waveTimer = 0f       // 보스: 카운트다운, 일반: 미사용
    var waveElapsed = 0f; private set // 웨이브 경과 시간 (카운트업)
    var currentConfig: WaveConfig = getWaveConfig(0); private set

    companion object {
        const val BOSS_ENEMY_TYPE = 99
        const val WAVE_DURATION = 180f      // 일반 웨이브 3분 제한
        const val BOSS_DURATION = 180f      // 보스 웨이브 3분 제한
        private val MID_GAME_TYPE_POOL = intArrayOf(0, 1, 2, 3, 4, 6, 7, 8, 9)
        const val FAST_KILL_THRESHOLD = 30f // 30초 이내 클리어 시 보너스

        /** 특수 웨이브 판정 (0-indexed wave) */
        fun getSpecialWaveType(wave: Int): SpecialWaveType {
            val waveNum = wave + 1 // 1-indexed
            // 보스(10n) / 미니보스(5n) 웨이브와 겹치지 않도록 조정
            // 미니보스: 5,15,25,35,45,55 / 보스: 10,20,30,40,50,60
            return when (waveNum) {
                13 -> SpecialWaveType.RUSH          // 미니보스(15) 아님 ✓
                22 -> SpecialWaveType.HEAVY_ARMOR    // 미니보스(25) 아님 ✓
                33 -> SpecialWaveType.DARKNESS        // 미니보스(35) 아님 ✓
                43 -> SpecialWaveType.ELITE_MARCH    // 44→43 (44는 미니보스45 아님이지만 45가 미니보스)
                53 -> SpecialWaveType.LAST_CHARGE    // 55→53 (55는 미니보스)
                else -> SpecialWaveType.NONE
            }
        }
    }

    private val difficultyMult = when (difficulty) {
        0 -> 1f       // 일반
        1 -> 1.5f     // 하드
        2 -> 2.2f     // 헬
        else -> 1f
    }

    fun getWaveConfig(wave: Int): WaveConfig {
        // 60-wave standard: base values scale up to 59, late-game (40+) gets extra scaling
        val w = wave
        val capped = w.coerceAtMost(59)

        // Late-game scaling: gradual ramp from wave 30+
        val lateScale = if (w >= 30) 1f + (w - 29) * 0.04f else 1f

        val baseHP = (50f + capped * 30f + (capped * capped * 0.5f)) * lateScale
        val baseSpeed = (60f + (capped * 1.2f).coerceAtMost(50f)) *
            (if (w >= 40) 1f + (w - 39) * 0.015f else 1f).coerceAtMost(1.4f)
        val baseArmor = ((capped * 1.5f).coerceAtMost(70f) + if (w >= 30) (w - 29) * 1.5f else 0f)
        val baseMR = ((capped * 1.3f).coerceAtMost(50f) + if (w >= 30) (w - 29) * 1.0f else 0f)

        // 웨이브당 40마리 고정
        val count = 40

        val isBoss = forceBoss || (w + 1) % 10 == 0
        val isMiniBoss = !forceBoss && (w + 1) % 5 == 0 && !isBoss

        // 11 regular types (0-10) + boss uses type 10
        val enemyType = when {
            isBoss -> 10
            isMiniBoss -> 5
            w >= 40 -> w % 11                       // 후반: 전체 타입 순환
            w >= 20 -> MID_GAME_TYPE_POOL[w % MID_GAME_TYPE_POOL.size]
            w % 4 == 1 -> 1
            w % 4 == 2 -> 2
            w % 4 == 3 -> 3
            else -> 0
        }

        val bossMultHP = when {
            isBoss -> 10f + (w.coerceIn(30, 50) - 30) * 0.25f   // W30=10, W50=15
            isMiniBoss -> 5f + (w.coerceIn(20, 40) - 20) * 0.10f // W20=5, W40=7
            else -> 1f
        }
        val bossMultArmor = when {
            isBoss -> 2f + (w.coerceIn(30, 50) - 30) * 0.04f     // W30=2.0, W50=2.8
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
        val ccResistance = (baseCcResist + when (difficulty) {
            1 -> 0.10f
            2 -> 0.15f
            else -> 0f
        }).coerceAtMost(0.9f)

        // Elite chance increases in later waves
        val eliteChance = when {
            isBoss || isMiniBoss -> 0f // bosses don't have elite variants
            w >= 45 -> 0.3f
            w >= 30 -> 0.2f
            w >= 20 -> 0.1f
            else -> 0f
        }

        // ── 특수 웨이브 이벤트 ──
        val specialWave = getSpecialWaveType(w)

        // 특수 웨이브 오버라이드 (보스/미니보스 웨이브는 특수 이벤트 무시)
        val finalCount: Int
        val finalHP: Float
        val finalSpeed: Float
        val finalArmor: Float
        val finalEliteChance: Float

        if (isBoss || isMiniBoss) {
            // 보스/미니보스는 특수 이벤트 무시
            finalCount = if (isBoss) 1 else 3
            finalHP = baseHP * difficultyMult * bossMultHP
            finalSpeed = if (isBoss) baseSpeed * 0.6f else baseSpeed
            finalArmor = baseArmor * bossMultArmor
            finalEliteChance = 0f
        } else {
            when (specialWave) {
                SpecialWaveType.RUSH -> {
                    finalCount = 80
                    finalHP = baseHP * difficultyMult * 0.5f       // HP 절반
                    finalSpeed = baseSpeed * 1.5f                   // 속도 1.5배
                    finalArmor = baseArmor
                    finalEliteChance = eliteChance
                }
                SpecialWaveType.HEAVY_ARMOR -> {
                    finalCount = 20
                    finalHP = baseHP * difficultyMult * 3f          // HP 3배
                    finalSpeed = baseSpeed * 0.5f                   // 속도 0.5배
                    finalArmor = baseArmor * 3f                     // 방어 3배
                    finalEliteChance = eliteChance
                }
                SpecialWaveType.DARKNESS -> {
                    // 사거리 감소는 BattleEngine에서 처리, 웨이브 스탯은 동일
                    finalCount = count
                    finalHP = baseHP * difficultyMult
                    finalSpeed = baseSpeed
                    finalArmor = baseArmor
                    finalEliteChance = eliteChance
                }
                SpecialWaveType.ELITE_MARCH -> {
                    finalCount = count
                    finalHP = baseHP * difficultyMult
                    finalSpeed = baseSpeed
                    finalArmor = baseArmor
                    finalEliteChance = 1f                           // 전부 엘리트
                }
                SpecialWaveType.LAST_CHARGE -> {
                    finalCount = 60
                    finalHP = baseHP * difficultyMult
                    finalSpeed = baseSpeed * 1.5f                   // 속도 1.5배
                    finalArmor = baseArmor
                    finalEliteChance = 0.5f                         // 엘리트 50%
                }
                SpecialWaveType.NONE -> {
                    finalCount = count
                    finalHP = baseHP * difficultyMult
                    finalSpeed = baseSpeed
                    finalArmor = baseArmor
                    finalEliteChance = eliteChance
                }
            }
        }

        return WaveConfig(
            enemyCount = finalCount,
            hp = finalHP,
            speed = finalSpeed,
            armor = finalArmor,
            magicResist = baseMR * bossMultArmor,
            isBoss = isBoss || isMiniBoss,
            isMiniBoss = isMiniBoss,
            spawnInterval = if (isBoss) 0f else if (specialWave == SpecialWaveType.RUSH) 0.15f
                else (0.5f + (1f / (1 + w * 0.1f))).coerceAtLeast(0.2f),
            enemyType = enemyType,
            ccResistance = ccResistance,
            eliteChance = finalEliteChance,
            specialWave = specialWave,
        )
    }

    fun startWave(wave: Int) {
        currentWave = wave
        currentConfig = getWaveConfig(wave)
        spawnedCount = 0
        spawnTimer = 0f
        waveElapsed = 0f
        waveTimer = if (currentConfig.isBoss) BOSS_DURATION else WAVE_DURATION
        waveComplete = false
    }

    /** Returns number of enemies to spawn this tick. */
    fun update(dt: Float): Int {
        if (waveComplete) return 0

        waveElapsed += dt
        waveTimer -= dt

        if (waveTimer <= 0f) {
            waveComplete = true
            return 0
        }

        // Spawn enemies until count reached
        if (spawnedCount >= currentConfig.enemyCount) return 0

        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            spawnTimer += currentConfig.spawnInterval
            spawnedCount++
            return 1
        }
        return 0
    }

    /** All enemies for this wave have been spawned */
    val allSpawned get() = spawnedCount >= currentConfig.enemyCount

    fun forceComplete() { waveComplete = true }

    fun advanceWave() { currentWave++ }
    val isLastWave get() = currentWave >= maxWaves - 1
    val timeRemaining get() = waveTimer
    /** 30초 이내 클리어 여부 */
    val isFastKill get() = waveElapsed < FAST_KILL_THRESHOLD
}
