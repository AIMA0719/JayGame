package com.jay.jaygame.ui.battle

/**
 * Automatic Level-of-Detail controller for particle systems.
 * Reduces particle rendering when counts exceed thresholds to maintain frame rate.
 *
 * Levels:
 *  0 = full quality (all particles rendered)
 *  1 = reduced (skip every other particle)
 *  2 = minimal (keep only every 3rd particle)
 */
object ParticleLOD {
    var currentLevel: Int = 0
        private set

    @Volatile
    private var frameParticleCount: Int = 0

    /**
     * Returns true if the particle at the given index should be skipped
     * based on the current LOD level.
     */
    fun shouldSkipParticle(index: Int): Boolean {
        return when (currentLevel) {
            1 -> index % 2 == 0       // skip every other
            2 -> index % 3 != 0       // keep only every 3rd
            else -> false             // full quality
        }
    }

    /** 각 오버레이에서 호출: 해당 프레임의 활성 파티클 수를 합산. */
    fun addParticleCount(count: Int) {
        frameParticleCount += count
    }

    /** 프레임 시작 시 합산 리셋. */
    fun resetFrame() {
        frameParticleCount = 0
    }

    /** 프레임 끝에 합산된 카운트로 LOD 갱신. */
    fun commitFrame(battleSpeed: Float = 2f) {
        updateLOD(frameParticleCount, battleSpeed)
    }

    /** 사용자 이펙트 품질 설정 (0=저, 1=중, 2=고). BattleBridge.effectQuality에서 반영. */
    var userQuality: Int = 1

    /**
     * Update LOD level based on the total active particle count across all overlays.
     * Call this once per frame with the aggregate particle count.
     */
    fun updateLOD(particleCount: Int, battleSpeed: Float = 2f) {
        // 사용자 설정이 '저'이면 항상 최소 파티클
        if (userQuality == 0) { currentLevel = 2; return }

        // At higher battle speeds, reduce particle thresholds for better perf
        val speedFactor = when {
            battleSpeed >= 8f -> 0.25f
            battleSpeed >= 4f -> 0.5f
            battleSpeed >= 2f -> 0.75f
            else -> 1f
        }
        // 사용자 설정이 '중'이면 임계값 절반
        val qualityFactor = if (userQuality == 1) 0.6f else 1f
        val highThreshold = (500 * speedFactor * qualityFactor).toInt()
        val midThreshold = (200 * speedFactor * qualityFactor).toInt()
        currentLevel = when {
            particleCount > highThreshold -> 2
            particleCount > midThreshold -> 1
            else -> 0
        }
    }
}
