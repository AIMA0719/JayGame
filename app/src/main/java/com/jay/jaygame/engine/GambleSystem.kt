package com.jay.jaygame.engine

import kotlin.random.Random

/**
 * 도박 시스템 — 카지노 수학 기반.
 *
 * 핵심 공식: EV = successRate × multiplier
 * 모든 티어에서 EV ≈ 0.95 (하우스 엣지 ~5%)
 * → 장기적으로 플레이어가 약간 손해, 단기적으론 운에 따라 이득 가능.
 *
 * 플레이어가 판돈(betAmount)을 선택:
 * - 승리: +betAmount × (multiplier - 1)  (판돈 돌려받고 + 이익)
 * - 패배: -betAmount (판돈 전액 잃음)
 *
 * 연패 보호 없음 — 도박은 도박.
 */
object GambleSystem {

    data class GambleResult(
        val won: Boolean,
        val option: GambleOption,
        val betSize: BetSize,
        val betAmount: Float,       // how much was wagered
        val reward: Float,          // net SP gained on win (0 on loss)
        val penalty: Float,         // SP lost on failure (= betAmount)
        val spBefore: Float,
        val spAfter: Float,
    )

    /**
     * 판돈 크기 — 현재 SP의 비율
     */
    enum class BetSize(val label: String, val ratio: Float) {
        SMALL("10%", 0.10f),
        MEDIUM("25%", 0.25f),
        LARGE("50%", 0.50f),
        ALL_IN("올인", 1.0f),
    }

    /**
     * 리스크 티어 — 배율과 성공률.
     *
     * EV = successRate × multiplier
     * SAFE:    0.48 × 2  = 0.96
     * NORMAL:  0.32 × 3  = 0.96
     * RISKY:   0.19 × 5  = 0.95
     * JACKPOT: 0.09 × 10 = 0.90
     *
     * 높은 리스크일수록 하우스 엣지가 살짝 높음 (현실 카지노와 동일).
     */
    enum class GambleOption(
        val label: String,
        val multiplier: Float,
        val baseSuccessRate: Float,
    ) {
        SAFE("안전", 2f, 0.48f),
        NORMAL("일반", 3f, 0.32f),
        RISKY("위험", 5f, 0.19f),
        JACKPOT("잭팟", 10f, 0.09f),
    }

    fun gamble(
        currentSp: Float,
        option: GambleOption,
        betSize: BetSize,
        luckBonus: Float = 0f,
    ): GambleResult {
        val betAmount = currentSp * betSize.ratio
        if (betAmount <= 0f) return GambleResult(
            won = false, option = option, betSize = betSize,
            betAmount = 0f, reward = 0f, penalty = 0f,
            spBefore = currentSp, spAfter = currentSp,
        )

        val successRate = (option.baseSuccessRate + luckBonus).coerceAtMost(0.85f)
        val won = Random.nextFloat() < successRate

        return if (won) {
            val netGain = betAmount * (option.multiplier - 1f)
            GambleResult(
                won = true, option = option, betSize = betSize,
                betAmount = betAmount, reward = netGain, penalty = 0f,
                spBefore = currentSp, spAfter = currentSp + netGain,
            )
        } else {
            GambleResult(
                won = false, option = option, betSize = betSize,
                betAmount = betAmount, reward = 0f, penalty = betAmount,
                spBefore = currentSp, spAfter = (currentSp - betAmount).coerceAtLeast(0f),
            )
        }
    }
}
