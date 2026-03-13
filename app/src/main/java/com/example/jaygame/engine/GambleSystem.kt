package com.example.jaygame.engine

import kotlin.random.Random

object GambleSystem {

    data class GambleResult(
        val bet: Float,
        val won: Boolean,
        val multiplier: Float,
        val reward: Float,
    )

    enum class GambleOption(val label: String, val multiplier: Float, val baseSuccessRate: Float) {
        SAFE("안전 베팅", 1.5f, 0.70f),
        NORMAL("일반 베팅", 2.5f, 0.45f),
        RISKY("위험 베팅", 4.0f, 0.25f),
        JACKPOT("잭팟", 8.0f, 0.10f),
    }

    fun gamble(bet: Float, option: GambleOption, luckBonus: Float = 0f): GambleResult {
        val successRate = (option.baseSuccessRate + luckBonus).coerceAtMost(0.90f)
        val won = Random.nextFloat() < successRate
        val reward = if (won) bet * option.multiplier else 0f
        return GambleResult(bet, won, option.multiplier, reward)
    }
}
