package com.jay.jaygame.data

enum class PetGrade(val label: String, val colorHex: Long, val maxLevel: Int, val pullWeight: Int) {
    RARE("희귀", 0xFF42A5F5, 10, 60),
    ANCIENT("고대", 0xFFAB47BC, 15, 25),
    LEGEND("전설", 0xFFFFCA28, 20, 12),
    MYTHIC("신화", 0xFFEF5350, 30, 3);
}

enum class PetCategory { ATTACK, SUPPORT, UTILITY }

data class PetDef(
    val id: Int,
    val name: String,
    val grade: PetGrade,
    val category: PetCategory,
    val skillName: String,
    val skillDescription: String,
    val cooldown: Float,
    val isPassive: Boolean = cooldown == 0f,
)

val ALL_PETS: List<PetDef> = listOf(
    PetDef(0, "화염 드래곤", PetGrade.RARE, PetCategory.ATTACK, "화염 브레스", "범위 내 적 전체에 화염 피해", 8f),
    PetDef(1, "독거미", PetGrade.ANCIENT, PetCategory.ATTACK, "맹독 사출", "HP 최대 적에게 DoT 5초", 10f),
    PetDef(2, "번개 매", PetGrade.LEGEND, PetCategory.ATTACK, "연쇄 낙뢰", "랜덤 3체 연쇄번개", 12f),
    PetDef(3, "요정", PetGrade.RARE, PetCategory.SUPPORT, "격려", "전체 유닛 ATK 증가 8초", 15f),
    PetDef(4, "골렘", PetGrade.ANCIENT, PetCategory.SUPPORT, "대지의 방패", "전체 유닛 쉴드 10초", 20f),
    PetDef(5, "유니콘", PetGrade.LEGEND, PetCategory.SUPPORT, "성스러운 빛", "쿨다운 감소 + 공속 증가 6초", 25f),
    PetDef(6, "두꺼비", PetGrade.RARE, PetCategory.UTILITY, "금빛 혀", "적 처치 골드 증가 (패시브)", 0f),
    PetDef(7, "9미호", PetGrade.ANCIENT, PetCategory.UTILITY, "환술", "소환 등급 상향 확률 (패시브)", 0f),
    PetDef(8, "봉황", PetGrade.MYTHIC, PetCategory.UTILITY, "열반", "패배 시 1회 부활 (패시브)", 0f),
)

fun petCardsRequired(level: Int): Int = level * 2
fun petUpgradeCost(level: Int): Int = level * 200
const val PET_PULL_COST = 50
const val PET_PULL_10_COST = 400
