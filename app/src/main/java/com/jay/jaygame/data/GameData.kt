package com.jay.jaygame.data

data class UnitProgress(
    val owned: Boolean = false,
    val cards: Int = 0,
    val level: Int = 1,
)

data class RelicProgress(
    val relicId: Int = 0,
    val grade: Int = 0,      // RelicGrade.ordinal
    val level: Int = 1,
    val owned: Boolean = false,
)

data class PetProgress(
    val petId: Int = 0,
    val owned: Boolean = false,
    val cards: Int = 0,
    val level: Int = 1,
)

// ── Domain sub-data classes ──
// These provide a logical grouping of GameData fields by domain.
// GameData still holds all fields flat (for backward-compatible copy()),
// and exposes grouped views via computed properties.

/** 재화 (골드, 다이아, 가스) */
data class EconomyData(
    val gold: Int = 10000,
    val diamonds: Int = 0,
    val gas: Int = 0,
)

/** 전투 통계 */
data class BattleStats(
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val totalKills: Int = 0,
    val totalMerges: Int = 0,
    val totalGoldEarned: Int = 0,
    val highestWave: Int = 0,
    val maxUnitLevel: Int = 1,
    val wonWithoutDamage: Boolean = false,
    val wonWithSingleType: Boolean = false,
)

/** 유닛 컬렉션 (유닛 맵, 천장, 패밀리 업그레이드, 무료 소환) */
data class UnitCollectionData(
    val units: Map<String, UnitProgress> = emptyMap(),
    val unitPullPity: Int = 0,
    val familyUpgrades: Map<String, Int> = emptyMap(),
    val lastFreePullTime: Long = 0L,
)

/** 유물 */
data class RelicData(
    val relics: List<RelicProgress> = List(12) { RelicProgress(relicId = it) },
    val equippedRelics: List<Int> = emptyList(),
)

/** 펫 */
data class PetData(
    val pets: List<PetProgress> = List(9) { PetProgress(petId = it) },
    val equippedPets: List<Int> = emptyList(),
    val petPullPity: Int = 0,
)

/** 스태미나 */
data class StaminaData(
    val stamina: Int = 100,
    val maxStamina: Int = 100,
    val lastStaminaRegenTime: Long = System.currentTimeMillis(),
)

/** 스테이지 진행 + 던전 */
data class StageProgressData(
    val currentStageId: Int = 0,
    val unlockedStages: List<Int> = listOf(0),
    val stageBestWaves: List<Int> = List(6) { 0 },
    val difficulty: Int = 0,
    val dungeonClears: Map<Int, Int> = emptyMap(),
    val dungeonDailyCount: Int = 0,
    val lastDungeonResetDate: String = "",
)

/** 게임플레이 설정 */
data class UserSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val defaultBattleSpeed: Float = 2f,
    val showDamageNumbers: Boolean = true,
    val healthBarMode: Int = 0,
    val effectQuality: Int = 1,
    val autoWaveStart: Boolean = false,
)

/** 플레이어 진행 (레벨, XP, 트로피, 시즌, 로그인, 프로필, 튜토리얼, 업적) */
data class PlayerProgressData(
    val playerLevel: Int = 1,
    val totalXP: Int = 0,
    val trophies: Int = 0,
    val seasonXP: Int = 0,
    val seasonClaimedTier: Int = 0,
    val seasonMonth: String = "",
    val lastLoginDate: String = "",
    val loginStreak: Int = 0,
    val lastClaimedDay: Int = 0,
    val selectedProfileId: Int = 0,
    val unlockedProfiles: Set<Int> = setOf(0),
    val tutorialCompleted: Boolean = false,
    val claimedAchievements: Set<Int> = emptySet(),
    val lastKnownSystemTime: Long = System.currentTimeMillis(),
    val saveVersion: Int = 3,
)

data class GameData(
    val gold: Int = 10000,
    val diamonds: Int = 0,
    val trophies: Int = 0,
    val playerLevel: Int = 1,
    val totalXP: Int = 0,
    val units: Map<String, UnitProgress> = emptyMap(),
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
    val hapticEnabled: Boolean = true,
    val lastLoginDate: String = "",
    val loginStreak: Int = 0,
    val lastClaimedDay: Int = 0,
    val seasonXP: Int = 0,
    val seasonClaimedTier: Int = 0,
    val seasonMonth: String = "", // "2026-03" format — resets when month changes
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
    // 게임플레이 설정
    val defaultBattleSpeed: Float = 2f,   // 2(x1), 4(x2), 8(x4)
    val showDamageNumbers: Boolean = true,
    val healthBarMode: Int = 0,           // 0=항상, 1=피격 시만, 2=숨김
    val effectQuality: Int = 1,           // 0=저, 1=중, 2=고
    val autoWaveStart: Boolean = false,   // true=웨이브 간 대기 스킵
    // 가스
    val gas: Int = 0,
    // 패밀리 영구 업그레이드
    val familyUpgrades: Map<String, Int> = emptyMap(),
    val lastFreePullTime: Long = 0L,
    // 업적 수령 기록
    val claimedAchievements: Set<Int> = emptySet(),
    val saveVersion: Int = 3,
    // 유물
    val relics: List<RelicProgress> = List(12) { RelicProgress(relicId = it) },
    val equippedRelics: List<Int> = emptyList(),
    // 펫
    val pets: List<PetProgress> = List(9) { PetProgress(petId = it) },
    val equippedPets: List<Int> = emptyList(),
    val petPullPity: Int = 0,
    // 유닛 소환 천장
    val unitPullPity: Int = 0,
    // 던전
    val dungeonClears: Map<Int, Int> = emptyMap(),  // dungeonId → bestWave
    val dungeonDailyCount: Int = 0,
    val lastDungeonResetDate: String = "",
    // 프로필
    val selectedProfileId: Int = 0,
    val unlockedProfiles: Set<Int> = setOf(0),
    // 튜토리얼
    val tutorialCompleted: Boolean = false,
    // 초보자 패키지 구매 여부
    val starterPackPurchased: Boolean = false,
    // 시간 조작 감지용
    val lastKnownSystemTime: Long = System.currentTimeMillis(),
) {
    // ── Grouped views (read-only projections) ──
    // Use these to access logically related fields as a group.
    // To update, still use GameData.copy() with flat fields for now.
    // Future migration: callsites will adopt sub-class copy patterns.

    /** 재화 */
    val economy: EconomyData get() = EconomyData(
        gold = gold,
        diamonds = diamonds,
        gas = gas,
    )

    /** 전투 통계 */
    val battleStats: BattleStats get() = BattleStats(
        totalWins = totalWins,
        totalLosses = totalLosses,
        totalKills = totalKills,
        totalMerges = totalMerges,
        totalGoldEarned = totalGoldEarned,
        highestWave = highestWave,
        maxUnitLevel = maxUnitLevel,
        wonWithoutDamage = wonWithoutDamage,
        wonWithSingleType = wonWithSingleType,
    )

    /** 유닛 컬렉션 */
    val unitCollection: UnitCollectionData get() = UnitCollectionData(
        units = units,
        unitPullPity = unitPullPity,
        familyUpgrades = familyUpgrades,
        lastFreePullTime = lastFreePullTime,
    )

    /** 유물 */
    val relicState: RelicData get() = RelicData(
        relics = relics,
        equippedRelics = equippedRelics,
    )

    /** 펫 */
    val petState: PetData get() = PetData(
        pets = pets,
        equippedPets = equippedPets,
        petPullPity = petPullPity,
    )

    /** 스태미나 */
    val staminaState: StaminaData get() = StaminaData(
        stamina = stamina,
        maxStamina = maxStamina,
        lastStaminaRegenTime = lastStaminaRegenTime,
    )

    /** 스테이지 + 던전 진행 */
    val stageProgress: StageProgressData get() = StageProgressData(
        currentStageId = currentStageId,
        unlockedStages = unlockedStages,
        stageBestWaves = stageBestWaves,
        difficulty = difficulty,
        dungeonClears = dungeonClears,
        dungeonDailyCount = dungeonDailyCount,
        lastDungeonResetDate = lastDungeonResetDate,
    )

    /** 설정 */
    val settings: UserSettings get() = UserSettings(
        soundEnabled = soundEnabled,
        musicEnabled = musicEnabled,
        hapticEnabled = hapticEnabled,
        defaultBattleSpeed = defaultBattleSpeed,
        showDamageNumbers = showDamageNumbers,
        healthBarMode = healthBarMode,
        effectQuality = effectQuality,
        autoWaveStart = autoWaveStart,
    )

    /** 플레이어 진행 */
    val playerProgress: PlayerProgressData get() = PlayerProgressData(
        playerLevel = playerLevel,
        totalXP = totalXP,
        trophies = trophies,
        seasonXP = seasonXP,
        seasonClaimedTier = seasonClaimedTier,
        seasonMonth = seasonMonth,
        lastLoginDate = lastLoginDate,
        loginStreak = loginStreak,
        lastClaimedDay = lastClaimedDay,
        selectedProfileId = selectedProfileId,
        unlockedProfiles = unlockedProfiles,
        tutorialCompleted = tutorialCompleted,
        claimedAchievements = claimedAchievements,
        lastKnownSystemTime = lastKnownSystemTime,
        saveVersion = saveVersion,
    )

    val equippedPetSlotCount: Int get() = if (trophies >= 2000) 2 else 1
    val equippedSlotCount: Int get() = when {
        trophies >= 3000 -> 4
        trophies >= 1500 -> 3
        trophies >= 500 -> 2
        else -> 1
    }
    val rank: String get() = when {
        trophies >= 4000 -> "마스터"
        trophies >= 3000 -> "다이아몬드"
        trophies >= 2000 -> "골드"
        trophies >= 1000 -> "실버"
        else -> "브론즈"
    }
    val seasonTier: Int get() = seasonXP / 100
    val seasonTierProgress: Float get() = (seasonXP % 100) / 100f

    companion object {
        /** Construct GameData from domain sub-classes */
        fun fromGrouped(
            economy: EconomyData = EconomyData(),
            battleStats: BattleStats = BattleStats(),
            unitCollection: UnitCollectionData = UnitCollectionData(),
            relicState: RelicData = RelicData(),
            petState: PetData = PetData(),
            staminaState: StaminaData = StaminaData(),
            stageProgress: StageProgressData = StageProgressData(),
            settings: UserSettings = UserSettings(),
            playerProgress: PlayerProgressData = PlayerProgressData(),
        ) = GameData(
            gold = economy.gold,
            diamonds = economy.diamonds,
            gas = economy.gas,
            totalWins = battleStats.totalWins,
            totalLosses = battleStats.totalLosses,
            totalKills = battleStats.totalKills,
            totalMerges = battleStats.totalMerges,
            totalGoldEarned = battleStats.totalGoldEarned,
            highestWave = battleStats.highestWave,
            maxUnitLevel = battleStats.maxUnitLevel,
            wonWithoutDamage = battleStats.wonWithoutDamage,
            wonWithSingleType = battleStats.wonWithSingleType,
            units = unitCollection.units,
            unitPullPity = unitCollection.unitPullPity,
            familyUpgrades = unitCollection.familyUpgrades,
            lastFreePullTime = unitCollection.lastFreePullTime,
            relics = relicState.relics,
            equippedRelics = relicState.equippedRelics,
            pets = petState.pets,
            equippedPets = petState.equippedPets,
            petPullPity = petState.petPullPity,
            stamina = staminaState.stamina,
            maxStamina = staminaState.maxStamina,
            lastStaminaRegenTime = staminaState.lastStaminaRegenTime,
            currentStageId = stageProgress.currentStageId,
            unlockedStages = stageProgress.unlockedStages,
            stageBestWaves = stageProgress.stageBestWaves,
            difficulty = stageProgress.difficulty,
            dungeonClears = stageProgress.dungeonClears,
            dungeonDailyCount = stageProgress.dungeonDailyCount,
            lastDungeonResetDate = stageProgress.lastDungeonResetDate,
            soundEnabled = settings.soundEnabled,
            musicEnabled = settings.musicEnabled,
            hapticEnabled = settings.hapticEnabled,
            defaultBattleSpeed = settings.defaultBattleSpeed,
            showDamageNumbers = settings.showDamageNumbers,
            healthBarMode = settings.healthBarMode,
            effectQuality = settings.effectQuality,
            autoWaveStart = settings.autoWaveStart,
            playerLevel = playerProgress.playerLevel,
            totalXP = playerProgress.totalXP,
            trophies = playerProgress.trophies,
            seasonXP = playerProgress.seasonXP,
            seasonClaimedTier = playerProgress.seasonClaimedTier,
            seasonMonth = playerProgress.seasonMonth,
            lastLoginDate = playerProgress.lastLoginDate,
            loginStreak = playerProgress.loginStreak,
            lastClaimedDay = playerProgress.lastClaimedDay,
            selectedProfileId = playerProgress.selectedProfileId,
            unlockedProfiles = playerProgress.unlockedProfiles,
            tutorialCompleted = playerProgress.tutorialCompleted,
            claimedAchievements = playerProgress.claimedAchievements,
            lastKnownSystemTime = playerProgress.lastKnownSystemTime,
            saveVersion = playerProgress.saveVersion,
        )
    }
}

/** Copy GameData by updating a specific domain sub-class */
fun GameData.copyEconomy(block: EconomyData.() -> EconomyData): GameData {
    val updated = economy.block()
    return copy(
        gold = updated.gold,
        diamonds = updated.diamonds,
        gas = updated.gas,
    )
}

fun GameData.copyBattleStats(block: BattleStats.() -> BattleStats): GameData {
    val updated = battleStats.block()
    return copy(
        totalWins = updated.totalWins,
        totalLosses = updated.totalLosses,
        totalKills = updated.totalKills,
        totalMerges = updated.totalMerges,
        totalGoldEarned = updated.totalGoldEarned,
        highestWave = updated.highestWave,
        maxUnitLevel = updated.maxUnitLevel,
        wonWithoutDamage = updated.wonWithoutDamage,
        wonWithSingleType = updated.wonWithSingleType,
    )
}

fun GameData.copySettings(block: UserSettings.() -> UserSettings): GameData {
    val updated = settings.block()
    return copy(
        soundEnabled = updated.soundEnabled,
        musicEnabled = updated.musicEnabled,
        hapticEnabled = updated.hapticEnabled,
        defaultBattleSpeed = updated.defaultBattleSpeed,
        showDamageNumbers = updated.showDamageNumbers,
        healthBarMode = updated.healthBarMode,
        effectQuality = updated.effectQuality,
        autoWaveStart = updated.autoWaveStart,
    )
}

fun GameData.copyStageProgress(block: StageProgressData.() -> StageProgressData): GameData {
    val updated = stageProgress.block()
    return copy(
        currentStageId = updated.currentStageId,
        unlockedStages = updated.unlockedStages,
        stageBestWaves = updated.stageBestWaves,
        difficulty = updated.difficulty,
        dungeonClears = updated.dungeonClears,
        dungeonDailyCount = updated.dungeonDailyCount,
        lastDungeonResetDate = updated.lastDungeonResetDate,
    )
}

fun GameData.copyPlayerProgress(block: PlayerProgressData.() -> PlayerProgressData): GameData {
    val updated = playerProgress.block()
    return copy(
        playerLevel = updated.playerLevel,
        totalXP = updated.totalXP,
        trophies = updated.trophies,
        seasonXP = updated.seasonXP,
        seasonClaimedTier = updated.seasonClaimedTier,
        seasonMonth = updated.seasonMonth,
        lastLoginDate = updated.lastLoginDate,
        loginStreak = updated.loginStreak,
        lastClaimedDay = updated.lastClaimedDay,
        selectedProfileId = updated.selectedProfileId,
        unlockedProfiles = updated.unlockedProfiles,
        tutorialCompleted = updated.tutorialCompleted,
        claimedAchievements = updated.claimedAchievements,
        lastKnownSystemTime = updated.lastKnownSystemTime,
        saveVersion = updated.saveVersion,
    )
}

fun GameData.copyStamina(block: StaminaData.() -> StaminaData): GameData {
    val updated = staminaState.block()
    return copy(
        stamina = updated.stamina,
        maxStamina = updated.maxStamina,
        lastStaminaRegenTime = updated.lastStaminaRegenTime,
    )
}

fun GameData.copyRelics(block: RelicData.() -> RelicData): GameData {
    val updated = relicState.block()
    return copy(
        relics = updated.relics,
        equippedRelics = updated.equippedRelics,
    )
}

fun GameData.copyPets(block: PetData.() -> PetData): GameData {
    val updated = petState.block()
    return copy(
        pets = updated.pets,
        equippedPets = updated.equippedPets,
        petPullPity = updated.petPullPity,
    )
}

fun GameData.copyUnits(block: UnitCollectionData.() -> UnitCollectionData): GameData {
    val updated = unitCollection.block()
    return copy(
        units = updated.units,
        unitPullPity = updated.unitPullPity,
        familyUpgrades = updated.familyUpgrades,
        lastFreePullTime = updated.lastFreePullTime,
    )
}
