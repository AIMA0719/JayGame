package com.example.jaygame.data

import androidx.compose.ui.graphics.Color
import com.example.jaygame.R

enum class UnitRarity(val label: String, val color: Color) {
    NORMAL("노말", Color(0xFF9E9E9E)),
    RARE("레어", Color(0xFF2196F3)),
    EPIC("에픽", Color(0xFFAB47BC)),
    LEGENDARY("전설", Color(0xFFFF8F00)),
}

enum class UnitElement(val label: String, val color: Color) {
    PHYSICAL("물리", Color(0xFFFF8A65)),
    MAGIC("마법", Color(0xFF64B5F6)),
    SUPPORT("보조", Color(0xFF81C784)),
}

data class UnitDef(
    val id: Int,
    val name: String,
    val rarity: UnitRarity,
    val element: UnitElement,
    val baseATK: Int,
    val baseSpeed: Float,
    val range: Float,
    val abilityName: String,
    val description: String,
    val iconRes: Int,
    val isSummonable: Boolean = true,
)

val UPGRADE_COSTS = listOf(2 to 100, 4 to 200, 10 to 500, 20 to 1000, 50 to 2000, 100 to 5000)
val LEVEL_MULTIPLIER = floatArrayOf(1.0f, 1.5f, 2.2f, 3.2f, 4.5f, 6.0f, 8.0f)

val UNIT_DEFS: List<UnitDef> = listOf(
    UnitDef(
        id = 0, name = "화염", rarity = UnitRarity.NORMAL, element = UnitElement.MAGIC,
        baseATK = 25, baseSpeed = 1.0f, range = 150f,
        abilityName = "스플래시", description = "주변 적에게 범위 피해를 입힙니다.",
        iconRes = R.drawable.ic_unit_0,
    ),
    UnitDef(
        id = 1, name = "냉기", rarity = UnitRarity.NORMAL, element = UnitElement.MAGIC,
        baseATK = 20, baseSpeed = 0.8f, range = 160f,
        abilityName = "슬로우", description = "적의 이동 속도를 감소시킵니다.",
        iconRes = R.drawable.ic_unit_1,
    ),
    UnitDef(
        id = 2, name = "독", rarity = UnitRarity.NORMAL, element = UnitElement.MAGIC,
        baseATK = 15, baseSpeed = 0.9f, range = 140f,
        abilityName = "지속 피해", description = "적에게 지속적인 독 피해를 입힙니다.",
        iconRes = R.drawable.ic_unit_2,
    ),
    UnitDef(
        id = 3, name = "철벽", rarity = UnitRarity.NORMAL, element = UnitElement.SUPPORT,
        baseATK = 10, baseSpeed = 0.5f, range = 100f,
        abilityName = "보호막", description = "아군에게 보호막을 부여합니다.",
        iconRes = R.drawable.ic_unit_3,
    ),
    UnitDef(
        id = 4, name = "번개", rarity = UnitRarity.RARE, element = UnitElement.MAGIC,
        baseATK = 30, baseSpeed = 1.2f, range = 170f,
        abilityName = "체인 3", description = "최대 3명의 적에게 연쇄 번개를 발사합니다.",
        iconRes = R.drawable.ic_unit_4,
    ),
    UnitDef(
        id = 5, name = "저격", rarity = UnitRarity.RARE, element = UnitElement.PHYSICAL,
        baseATK = 50, baseSpeed = 0.5f, range = 250f,
        abilityName = "없음", description = "먼 거리에서 강력한 한 발을 쏩니다.",
        iconRes = R.drawable.ic_unit_5,
    ),
    UnitDef(
        id = 6, name = "강화", rarity = UnitRarity.RARE, element = UnitElement.SUPPORT,
        baseATK = 5, baseSpeed = 0.3f, range = 120f,
        abilityName = "버프 +20%", description = "주변 아군의 공격력을 20% 증가시킵니다.",
        iconRes = R.drawable.ic_unit_6,
    ),
    UnitDef(
        id = 7, name = "폭풍", rarity = UnitRarity.EPIC, element = UnitElement.MAGIC,
        baseATK = 35, baseSpeed = 1.0f, range = 180f,
        abilityName = "스플래시+슬로우", description = "범위 피해와 함께 적의 이동 속도를 감소시킵니다.",
        iconRes = R.drawable.ic_unit_7,
    ),
    UnitDef(
        id = 8, name = "암살", rarity = UnitRarity.EPIC, element = UnitElement.PHYSICAL,
        baseATK = 45, baseSpeed = 1.5f, range = 130f,
        abilityName = "처형 15%", description = "체력이 15% 이하인 적을 즉시 처치합니다.",
        iconRes = R.drawable.ic_unit_8,
    ),
    UnitDef(
        id = 9, name = "용", rarity = UnitRarity.LEGENDARY, element = UnitElement.MAGIC,
        baseATK = 60, baseSpeed = 0.7f, range = 200f,
        abilityName = "스플래시", description = "강력한 화염으로 넓은 범위에 피해를 입힙니다.",
        iconRes = R.drawable.ic_unit_9,
    ),
    UnitDef(
        id = 10, name = "전기독", rarity = UnitRarity.EPIC, element = UnitElement.MAGIC,
        baseATK = 35, baseSpeed = 1.1f, range = 170f,
        abilityName = "체인 4", description = "최대 4명의 적에게 전기 독을 연쇄시킵니다.",
        iconRes = R.drawable.ic_unit_10, isSummonable = false,
    ),
    UnitDef(
        id = 11, name = "처형자", rarity = UnitRarity.EPIC, element = UnitElement.PHYSICAL,
        baseATK = 55, baseSpeed = 1.0f, range = 200f,
        abilityName = "처형 30%", description = "체력이 30% 이하인 적을 즉시 처치합니다.",
        iconRes = R.drawable.ic_unit_11, isSummonable = false,
    ),
    UnitDef(
        id = 12, name = "요새", rarity = UnitRarity.EPIC, element = UnitElement.SUPPORT,
        baseATK = 15, baseSpeed = 0.4f, range = 130f,
        abilityName = "버프 30%", description = "주변 아군의 공격력을 30% 증가시킵니다.",
        iconRes = R.drawable.ic_unit_12, isSummonable = false,
    ),
    UnitDef(
        id = 13, name = "불사조", rarity = UnitRarity.LEGENDARY, element = UnitElement.MAGIC,
        baseATK = 70, baseSpeed = 0.8f, range = 190f,
        abilityName = "스플래시", description = "불사의 화염으로 넓은 범위에 피해를 입힙니다.",
        iconRes = R.drawable.ic_unit_13, isSummonable = false,
    ),
    UnitDef(
        id = 14, name = "정령", rarity = UnitRarity.LEGENDARY, element = UnitElement.MAGIC,
        baseATK = 40, baseSpeed = 1.3f, range = 180f,
        abilityName = "체인 5", description = "최대 5명의 적에게 정령의 힘을 연쇄시킵니다.",
        iconRes = R.drawable.ic_unit_14, isSummonable = false,
    ),
)

val UNIT_DEFS_MAP: Map<Int, UnitDef> = UNIT_DEFS.associateBy { it.id }
