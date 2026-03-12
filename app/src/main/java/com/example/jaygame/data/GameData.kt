package com.example.jaygame.data

data class UnitProgress(
    val owned: Boolean = false,
    val cards: Int = 0,
    val level: Int = 1,
)

data class GameData(
    val gold: Int = 10000,
    val diamonds: Int = 0,
    val trophies: Int = 0,
    val playerLevel: Int = 1,
    val totalXP: Int = 0,
    val units: List<UnitProgress> = List(42) { i ->
        // COMMON grade of each family owned by default (IDs 0-4 + 35)
        UnitProgress(owned = i in 0..4 || i == 35, cards = 0, level = 1)
    },
    val deck: List<Int> = listOf(0, 1, 2, 3, 4),  // family ordinals (0=화염,1=냉기,2=독,3=번개,4=보조,5=바람)
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val totalKills: Int = 0,
    val totalMerges: Int = 0,
    val totalGoldEarned: Int = 0,
    val highestWave: Int = 0,
    val maxUnitLevel: Int = 1,
    val wonWithoutDamage: Boolean = false,
    val wonWithSingleType: Boolean = false,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val lastLoginDate: String = "",
    val loginStreak: Int = 0,
    val lastClaimedDay: Int = 0,
    val seasonXP: Int = 0,
    val seasonClaimedTier: Int = 0,
    // 스태미나
    val stamina: Int = 100,
    val maxStamina: Int = 100,
    val lastStaminaRegenTime: Long = System.currentTimeMillis(),
    // 스테이지
    val currentStageId: Int = 0,
    val unlockedStages: List<Int> = listOf(0),
    val stageBestWaves: List<Int> = List(6) { 0 },
    // 난이도
    val difficulty: Int = 0,
    // 가스
    val gas: Int = 0,
    // 패밀리 영구 업그레이드
    val familyUpgrades: Map<String, Int> = emptyMap(),
    val saveVersion: Int = 1,
) {
    val rank: String get() = when {
        trophies >= 4000 -> "마스터"
        trophies >= 3000 -> "다이아몬드"
        trophies >= 2000 -> "골드"
        trophies >= 1000 -> "실버"
        else -> "브론즈"
    }
    val seasonTier: Int get() = seasonXP / 100
    val seasonTierProgress: Float get() = (seasonXP % 100) / 100f
}
