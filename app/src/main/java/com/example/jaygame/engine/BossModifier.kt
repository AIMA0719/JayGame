package com.example.jaygame.engine

enum class BossModifier(val label: String, val description: String) {
    PHYSICAL_RESIST("강철 피부", "물리 피해 60% 감소"),
    MAGIC_RESIST("마법 장벽", "마법 피해 60% 감소"),
    CC_IMMUNE("불굴의 의지", "둔화/동결 면역"),
    DOT_IMMUNE("정화의 피", "지속 피해 면역"),
    RANGED_RESIST("반사 아우라", "원거리 피해 50% 감소"),
    REGENERATION("재생", "10초마다 체력 5% 회복"),
    SWIFT("질풍", "이동속도 2배"),
    COMMANDER("지휘관", "주변 몬스터 방어 +50%"),
}

fun getBossModifier(stageId: Int, waveIndex: Int): BossModifier? {
    if ((waveIndex + 1) % 10 != 0) return null
    val bossNumber = (waveIndex + 1) / 10
    return when (stageId) {
        0 -> when (bossNumber) {
            1 -> null; 2 -> BossModifier.PHYSICAL_RESIST
            3 -> BossModifier.SWIFT; 4 -> BossModifier.REGENERATION; else -> null
        }
        1 -> when (bossNumber) {
            1 -> BossModifier.DOT_IMMUNE; 2 -> BossModifier.MAGIC_RESIST
            3 -> BossModifier.CC_IMMUNE; 4 -> BossModifier.COMMANDER; else -> null
        }
        2 -> when (bossNumber) {
            1 -> BossModifier.RANGED_RESIST; 2 -> BossModifier.PHYSICAL_RESIST
            3 -> BossModifier.REGENERATION; 4 -> BossModifier.CC_IMMUNE
            else -> BossModifier.entries.random()
        }
        3 -> when (bossNumber) {
            1 -> BossModifier.MAGIC_RESIST; 2 -> BossModifier.SWIFT
            3 -> BossModifier.DOT_IMMUNE; 4 -> BossModifier.COMMANDER
            else -> BossModifier.entries.random()
        }
        4, 5 -> BossModifier.entries.random()
        else -> null
    }
}
