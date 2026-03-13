package com.example.jaygame.data

enum class RelicGrade(val label: String, val colorHex: Long, val maxLevel: Int, val dropWeight: Int) {
    COMMON("일반", 0xFF9E9E9E, 5, 50),
    RARE("희귀", 0xFF42A5F5, 7, 30),
    HERO("영웅", 0xFFAB47BC, 10, 15),
    LEGEND("전설", 0xFFFFCA28, 15, 4),
    MYTHIC("신화", 0xFFEF5350, 20, 1);
}

enum class RelicType { ECONOMY, COMBAT, UTILITY }

data class RelicDef(
    val id: Int,
    val name: String,
    val type: RelicType,
    val minGrade: RelicGrade,
    val description: String,
    val effectPerLevel: Float,
    val maxEffectCap: Float = Float.MAX_VALUE,
)

val ALL_RELICS: List<RelicDef> = listOf(
    // ===== 경제 (Economy) =====
    RelicDef(id = 0, name = "금고", type = RelicType.ECONOMY, minGrade = RelicGrade.COMMON,
        description = "웨이브 클리어 골드 +{lv}0%", effectPerLevel = 10f),
    RelicDef(id = 1, name = "머니건", type = RelicType.ECONOMY, minGrade = RelicGrade.COMMON,
        description = "적 처치 골드 +{lv}×8%", effectPerLevel = 8f),
    RelicDef(id = 2, name = "행운석", type = RelicType.ECONOMY, minGrade = RelicGrade.RARE,
        description = "도박 성공 확률 +{lv}×3%", effectPerLevel = 3f, maxEffectCap = 30f),

    // ===== 전투 (Combat) =====
    RelicDef(id = 3, name = "전쟁의 뿔피리", type = RelicType.COMBAT, minGrade = RelicGrade.COMMON,
        description = "전체 유닛 ATK +{lv}×5%", effectPerLevel = 5f),
    RelicDef(id = 4, name = "신속의 부츠", type = RelicType.COMBAT, minGrade = RelicGrade.COMMON,
        description = "전체 유닛 공속 +{lv}×4%", effectPerLevel = 4f),
    RelicDef(id = 5, name = "파멸의 반지", type = RelicType.COMBAT, minGrade = RelicGrade.RARE,
        description = "크리티컬 확률 +{lv}×2%", effectPerLevel = 2f, maxEffectCap = 30f),
    RelicDef(id = 6, name = "관통의 창", type = RelicType.COMBAT, minGrade = RelicGrade.RARE,
        description = "적 방어력 무시 +{lv}×3%", effectPerLevel = 3f, maxEffectCap = 60f),
    RelicDef(id = 7, name = "마력의 구슬", type = RelicType.COMBAT, minGrade = RelicGrade.HERO,
        description = "마법 피해 +{lv}×6%", effectPerLevel = 6f),

    // ===== 유틸리티 (Utility) =====
    RelicDef(id = 8, name = "소환사의 오브", type = RelicType.UTILITY, minGrade = RelicGrade.COMMON,
        description = "소환 비용 -{lv}×3%", effectPerLevel = 3f, maxEffectCap = 50f),
    RelicDef(id = 9, name = "합성의 돌", type = RelicType.UTILITY, minGrade = RelicGrade.RARE,
        description = "럭키 합성 확률 +{lv}×1%", effectPerLevel = 1f, maxEffectCap = 15f),
    RelicDef(id = 10, name = "시간의 모래", type = RelicType.UTILITY, minGrade = RelicGrade.HERO,
        description = "쿨다운 감소 +{lv}×2%", effectPerLevel = 2f, maxEffectCap = 40f),
    RelicDef(id = 11, name = "생명의 나무", type = RelicType.UTILITY, minGrade = RelicGrade.LEGEND,
        description = "웨이브 시작 SP +{lv}×5", effectPerLevel = 5f),
)

val RELIC_DEFS_MAP: Map<Int, RelicDef> = ALL_RELICS.associateBy { it.id }

val RELIC_UPGRADE_COSTS: List<Int> = listOf(100, 250, 500, 1000, 2000, 4000, 8000, 16000, 32000)

fun relicUpgradeCost(currentLevel: Int): Int {
    val idx = currentLevel - 1
    return if (idx < RELIC_UPGRADE_COSTS.size) RELIC_UPGRADE_COSTS[idx]
    else (RELIC_UPGRADE_COSTS.last() * Math.pow(2.2, (idx - RELIC_UPGRADE_COSTS.size + 1).toDouble())).toInt()
}
