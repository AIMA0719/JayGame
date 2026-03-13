package com.example.jaygame.data

enum class DungeonType(val label: String) {
    GOLD_RUSH("골드 러시"),
    RELIC_HUNT("유물 사냥"),
    PET_EXPEDITION("펫 탐험"),
    BOSS_RUSH("보스 러시"),
    SURVIVAL("서바이벌"),
}

data class DungeonDef(
    val id: Int,
    val type: DungeonType,
    val name: String,
    val description: String,
    val requiredTrophies: Int,
    val staminaCost: Int,
    val waveCount: Int,
    val difficultyMultiplier: Float,
    val rewardMultiplier: Float,
)

val ALL_DUNGEONS: List<DungeonDef> = listOf(
    DungeonDef(0, DungeonType.GOLD_RUSH, "골드 러시", "골드 보상 3배", 0, 8, 15, 1.2f, 3.0f),
    DungeonDef(1, DungeonType.RELIC_HUNT, "유물 사냥", "유물 드롭률 50%", 500, 10, 20, 1.5f, 1.0f),
    DungeonDef(2, DungeonType.PET_EXPEDITION, "펫 탐험", "펫 카드 보상", 1000, 10, 20, 1.5f, 1.0f),
    DungeonDef(3, DungeonType.BOSS_RUSH, "보스 러시", "10연속 보스전", 1500, 12, 10, 2.0f, 2.0f),
    DungeonDef(4, DungeonType.SURVIVAL, "서바이벌", "무한 웨이브 도전", 2000, 15, 999, 1.0f, 1.0f),
)

const val DUNGEON_DAILY_LIMIT = 3
