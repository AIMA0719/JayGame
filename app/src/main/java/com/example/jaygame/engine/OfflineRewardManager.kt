package com.example.jaygame.engine

import com.example.jaygame.data.GameData

/**
 * 오프라인 보상 — 접속하지 않은 시간에 따라 골드/시즌XP 지급.
 * 최대 24시간까지 누적 가능.
 */
object OfflineRewardManager {
    private const val MAX_OFFLINE_HOURS = 24
    private const val GOLD_PER_HOUR = 50
    private const val SEASON_XP_PER_HOUR = 10
    private const val MIN_OFFLINE_MINUTES = 10L // 최소 10분 이상 오프라인이어야 보상

    data class OfflineReward(
        val offlineMinutes: Long,
        val goldReward: Int,
        val seasonXpReward: Int,
    )

    /**
     * 오프라인 보상 계산 (적용 전 미리보기)
     */
    fun calculateReward(data: GameData): OfflineReward? {
        val now = System.currentTimeMillis()
        val offlineMs = now - data.lastOnlineTime
        val offlineMinutes = offlineMs / 60_000L

        if (offlineMinutes < MIN_OFFLINE_MINUTES) return null

        val hours = (offlineMinutes / 60f).coerceAtMost(MAX_OFFLINE_HOURS.toFloat())
        val goldReward = (hours * GOLD_PER_HOUR * (1f + data.playerLevel * 0.02f)).toInt()
        val seasonXpReward = (hours * SEASON_XP_PER_HOUR).toInt()

        return OfflineReward(
            offlineMinutes = offlineMinutes,
            goldReward = goldReward,
            seasonXpReward = seasonXpReward,
        )
    }

    /**
     * 오프라인 보상 적용 후 GameData 반환
     */
    fun claimReward(data: GameData): GameData {
        val reward = calculateReward(data) ?: return data.copy(lastOnlineTime = System.currentTimeMillis())
        return data.copy(
            gold = data.gold + reward.goldReward,
            seasonXP = data.seasonXP + reward.seasonXpReward,
            lastOnlineTime = System.currentTimeMillis(),
        )
    }
}
