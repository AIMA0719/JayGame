package com.example.jaygame.engine

import kotlin.random.Random

/**
 * 도박 시스템 — 고정 입장료 + 티어별 리스크/리워드.
 *
 * - 입장료: 10 SP (항상 소모)
 * - 승리: 입장료 × 배율 SP 획득
 * - 패배: 입장료 + 잔여 SP의 X% 추가 손실
 * - 연패 보호: 3연패 시 다음 판 성공률 +15%
 */
object GambleSystem {

    const val ENTRY_FEE = 10f
    private const val LOSING_STREAK_THRESHOLD = 3
    private const val LOSING_STREAK_BONUS = 0.15f

    data class GambleResult(
        val won: Boolean,
        val option: GambleOption,
        val reward: Float,          // SP gained on win (0 on loss)
        val penalty: Float,         // total SP lost (entry fee + % loss)
        val spBefore: Float,
        val spAfter: Float,
        val streakBroken: Boolean,  // true if losing streak pity triggered
    )

    enum class GambleOption(
        val label: String,
        val multiplier: Float,
        val baseSuccessRate: Float,
        val lossPenaltyRate: Float,  // % of remaining SP lost on failure
    ) {
        SAFE("안전 베팅", 1.3f, 0.51f, 0.03f),
        NORMAL("일반 베팅", 2.0f, 0.40f, 0.08f),
        RISKY("위험 베팅", 4.0f, 0.25f, 0.15f),
        JACKPOT("잭팟", 8.0f, 0.10f, 0.25f),
    }

    fun gamble(
        currentSp: Float,
        option: GambleOption,
        luckBonus: Float = 0f,
        losingStreak: Int = 0,
    ): GambleResult {
        if (currentSp < ENTRY_FEE) return GambleResult(
            won = false, option = option, reward = 0f, penalty = 0f,
            spBefore = currentSp, spAfter = currentSp, streakBroken = false,
        )

        val spBefore = currentSp
        val spAfterFee = currentSp - ENTRY_FEE

        // Losing streak pity
        val streakBonus = if (losingStreak >= LOSING_STREAK_THRESHOLD) LOSING_STREAK_BONUS else 0f
        val successRate = (option.baseSuccessRate + luckBonus + streakBonus).coerceAtMost(0.90f)
        val won = Random.nextFloat() < successRate
        val usedPity = streakBonus > 0f

        return if (won) {
            val reward = ENTRY_FEE * option.multiplier
            GambleResult(
                won = true,
                option = option,
                reward = reward,
                penalty = ENTRY_FEE,
                spBefore = spBefore,
                spAfter = spAfterFee + reward,
                streakBroken = usedPity,
            )
        } else {
            val extraLoss = spAfterFee * option.lossPenaltyRate
            val totalPenalty = ENTRY_FEE + extraLoss
            GambleResult(
                won = false,
                option = option,
                reward = 0f,
                penalty = totalPenalty,
                spBefore = spBefore,
                spAfter = (spAfterFee - extraLoss).coerceAtLeast(0f),
                streakBroken = false,
            )
        }
    }
}
