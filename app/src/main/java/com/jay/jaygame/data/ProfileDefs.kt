package com.jay.jaygame.data

import com.jay.jaygame.engine.BlueprintRegistry

data class ProfileDef(
    val id: Int,
    val name: String,
    val description: String,
    val condition: (GameData) -> Boolean,
    val isAnimated: Boolean = false,
)

val ALL_PROFILES: List<ProfileDef> = listOf(
    ProfileDef(0, "초보 모험가", "첫 전투 승리", { it.totalWins >= 1 }),
    ProfileDef(1, "베테랑", "50회 승리", { it.totalWins >= 50 }),
    ProfileDef(2, "전설의 전사", "200회 승리", { it.totalWins >= 200 }, isAnimated = true),
    ProfileDef(3, "무상 돌파", "무피해 클리어", { it.wonWithoutDamage }),
    ProfileDef(4, "순혈주의", "단일 패밀리 클리어", { it.wonWithSingleType }),
    ProfileDef(5, "수집가", "유닛 20종 보유", { it.units.count { (_, u) -> u.owned } >= 20 }),
    ProfileDef(6, "만물박사", "전 유닛 보유", { it.units.count { (_, u) -> u.owned } >= BlueprintRegistry.instance.count() }, isAnimated = true),
    ProfileDef(7, "유물 사냥꾼", "유물 6종 보유", { it.relics.count { r -> r.owned } >= 6 }),
    ProfileDef(8, "펫 마스터", "펫 전종 보유", { it.pets.all { p -> p.owned } }, isAnimated = true),
    ProfileDef(9, "부자", "총 골드 100,000 획득", { it.totalGoldEarned >= 100000 }),
    ProfileDef(10, "합성왕", "총 합성 500회", { it.totalMerges >= 500 }),
    ProfileDef(11, "웨이브 마스터", "최고 웨이브 50+", { it.highestWave >= 50 }),
    ProfileDef(12, "던전 탐험가", "첫 던전 클리어", { it.dungeonClears.isNotEmpty() }),
    ProfileDef(13, "심연 정복자", "심연 스테이지 40+ 클리어", {
        // stageBestWaves is List<Int> indexed by stageId; stage 5 is index 5
        it.stageBestWaves.getOrElse(5) { 0 } >= 40
    }, isAnimated = true),
    ProfileDef(14, "고인물", "헬 난이도 승리", { it.difficulty >= 2 && it.totalWins > 0 }),
)
