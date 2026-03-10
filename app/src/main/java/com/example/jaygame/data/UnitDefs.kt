package com.example.jaygame.data

import androidx.compose.ui.graphics.Color
import com.example.jaygame.R

private val UNIT_ICONS = mapOf(
    0 to R.drawable.ic_unit_0, 1 to R.drawable.ic_unit_1, 2 to R.drawable.ic_unit_2,
    3 to R.drawable.ic_unit_3, 4 to R.drawable.ic_unit_4, 5 to R.drawable.ic_unit_5,
    6 to R.drawable.ic_unit_6, 7 to R.drawable.ic_unit_7, 8 to R.drawable.ic_unit_8,
    9 to R.drawable.ic_unit_9, 10 to R.drawable.ic_unit_10, 11 to R.drawable.ic_unit_11,
    12 to R.drawable.ic_unit_12, 13 to R.drawable.ic_unit_13, 14 to R.drawable.ic_unit_14,
    15 to R.drawable.ic_unit_15, 16 to R.drawable.ic_unit_16, 17 to R.drawable.ic_unit_17,
    18 to R.drawable.ic_unit_18, 19 to R.drawable.ic_unit_19, 20 to R.drawable.ic_unit_20,
    21 to R.drawable.ic_unit_21, 22 to R.drawable.ic_unit_22, 23 to R.drawable.ic_unit_23,
    24 to R.drawable.ic_unit_24,
)
private fun iconFor(id: Int) = UNIT_ICONS[id] ?: R.drawable.ic_unit_0

enum class UnitGrade(val label: String, val color: Color, val weight: Int) {
    LOW("하급", Color(0xFF9E9E9E), 80),
    MEDIUM("중급", Color(0xFF2196F3), 15),
    HIGH("상급", Color(0xFFAB47BC), 5),
    SUPREME("최상급", Color(0xFFFF8F00), 0),
    TRANSCENDENT("초월", Color(0xFFE94560), 0),
}

enum class UnitFamily(val label: String, val color: Color) {
    FIRE("화염", Color(0xFFFF6B35)),
    FROST("냉기", Color(0xFF64B5F6)),
    POISON("독", Color(0xFF81C784)),
    LIGHTNING("번개", Color(0xFFFFD54F)),
    SUPPORT("보조", Color(0xFFCE93D8)),
}

data class UnitDef(
    val id: Int,
    val name: String,
    val grade: UnitGrade,
    val family: UnitFamily,
    val baseATK: Int,
    val baseSpeed: Float,
    val range: Float,
    val abilityName: String,
    val description: String,
    val iconRes: Int = 0,
    val isSummonable: Boolean = grade.weight > 0,
    val mergeResultId: Int = -1,
)

// Keep old names as typealiases for backward compat
@Suppress("unused")
typealias UnitRarity = UnitGrade
@Suppress("unused")
typealias UnitElement = UnitFamily

val UPGRADE_COSTS = listOf(2 to 100, 4 to 200, 10 to 500, 20 to 1000, 50 to 2000, 100 to 5000)
val LEVEL_MULTIPLIER = floatArrayOf(1.0f, 1.5f, 2.2f, 3.2f, 4.5f, 6.0f, 8.0f)

val UNIT_DEFS: List<UnitDef> = listOf(
    // === 화염 계열 (Splash) ===
    UnitDef(id = 0, name = "화염병", grade = UnitGrade.LOW, family = UnitFamily.FIRE,
        baseATK = 25, baseSpeed = 1.0f, range = 150f, iconRes = iconFor(0),
        abilityName = "스플래시", description = "주변 적에게 범위 피해", mergeResultId = 5),
    UnitDef(id = 5, name = "화염술사", grade = UnitGrade.MEDIUM, family = UnitFamily.FIRE,
        baseATK = 50, baseSpeed = 1.0f, range = 160f, iconRes = iconFor(5),
        abilityName = "스플래시", description = "강화된 범위 피해", mergeResultId = 10),
    UnitDef(id = 10, name = "화염마도사", grade = UnitGrade.HIGH, family = UnitFamily.FIRE,
        baseATK = 100, baseSpeed = 1.1f, range = 170f, iconRes = iconFor(10),
        abilityName = "스플래시", description = "광역 화염 폭발", mergeResultId = 15),
    UnitDef(id = 15, name = "불의 군주", grade = UnitGrade.SUPREME, family = UnitFamily.FIRE,
        baseATK = 200, baseSpeed = 1.2f, range = 180f, iconRes = iconFor(15),
        abilityName = "스플래시", description = "대규모 화염 폭풍", mergeResultId = 20),
    UnitDef(id = 20, name = "불사조", grade = UnitGrade.TRANSCENDENT, family = UnitFamily.FIRE,
        baseATK = 400, baseSpeed = 1.3f, range = 200f, iconRes = iconFor(20),
        abilityName = "스플래시", description = "불사의 화염으로 전소"),

    // === 냉기 계열 (Slow) ===
    UnitDef(id = 1, name = "냉기병", grade = UnitGrade.LOW, family = UnitFamily.FROST,
        baseATK = 20, baseSpeed = 0.8f, range = 160f, iconRes = iconFor(1),
        abilityName = "슬로우", description = "적 이동속도 감소", mergeResultId = 6),
    UnitDef(id = 6, name = "냉기술사", grade = UnitGrade.MEDIUM, family = UnitFamily.FROST,
        baseATK = 40, baseSpeed = 0.9f, range = 170f, iconRes = iconFor(6),
        abilityName = "슬로우", description = "강화된 빙결 효과", mergeResultId = 11),
    UnitDef(id = 11, name = "냉기마도사", grade = UnitGrade.HIGH, family = UnitFamily.FROST,
        baseATK = 80, baseSpeed = 1.0f, range = 180f, iconRes = iconFor(11),
        abilityName = "슬로우", description = "광역 빙결", mergeResultId = 16),
    UnitDef(id = 16, name = "얼음의 군주", grade = UnitGrade.SUPREME, family = UnitFamily.FROST,
        baseATK = 160, baseSpeed = 1.0f, range = 190f, iconRes = iconFor(16),
        abilityName = "슬로우", description = "절대영도", mergeResultId = 21),
    UnitDef(id = 21, name = "빙하제왕", grade = UnitGrade.TRANSCENDENT, family = UnitFamily.FROST,
        baseATK = 320, baseSpeed = 1.1f, range = 200f, iconRes = iconFor(21),
        abilityName = "슬로우", description = "시간까지 얼리는 극한의 냉기"),

    // === 독 계열 (DoT) ===
    UnitDef(id = 2, name = "독침병", grade = UnitGrade.LOW, family = UnitFamily.POISON,
        baseATK = 15, baseSpeed = 0.9f, range = 140f, iconRes = iconFor(2),
        abilityName = "지속 피해", description = "지속 독 피해", mergeResultId = 7),
    UnitDef(id = 7, name = "독술사", grade = UnitGrade.MEDIUM, family = UnitFamily.POISON,
        baseATK = 30, baseSpeed = 1.0f, range = 150f, iconRes = iconFor(7),
        abilityName = "지속 피해", description = "강화된 독 피해", mergeResultId = 12),
    UnitDef(id = 12, name = "독마도사", grade = UnitGrade.HIGH, family = UnitFamily.POISON,
        baseATK = 60, baseSpeed = 1.1f, range = 160f, iconRes = iconFor(12),
        abilityName = "지속 피해", description = "맹독 피해", mergeResultId = 17),
    UnitDef(id = 17, name = "역병의 군주", grade = UnitGrade.SUPREME, family = UnitFamily.POISON,
        baseATK = 120, baseSpeed = 1.2f, range = 170f, iconRes = iconFor(17),
        abilityName = "지속 피해", description = "역병 확산", mergeResultId = 22),
    UnitDef(id = 22, name = "독룡", grade = UnitGrade.TRANSCENDENT, family = UnitFamily.POISON,
        baseATK = 240, baseSpeed = 1.2f, range = 180f, iconRes = iconFor(22),
        abilityName = "지속 피해", description = "만물을 부식시키는 맹독"),

    // === 번개 계열 (Chain) ===
    UnitDef(id = 3, name = "전격병", grade = UnitGrade.LOW, family = UnitFamily.LIGHTNING,
        baseATK = 22, baseSpeed = 1.2f, range = 170f, iconRes = iconFor(3),
        abilityName = "체인", description = "2체 연쇄 공격", mergeResultId = 8),
    UnitDef(id = 8, name = "전격술사", grade = UnitGrade.MEDIUM, family = UnitFamily.LIGHTNING,
        baseATK = 45, baseSpeed = 1.3f, range = 180f, iconRes = iconFor(8),
        abilityName = "체인", description = "3체 연쇄 공격", mergeResultId = 13),
    UnitDef(id = 13, name = "전격마도사", grade = UnitGrade.HIGH, family = UnitFamily.LIGHTNING,
        baseATK = 90, baseSpeed = 1.4f, range = 190f, iconRes = iconFor(13),
        abilityName = "체인", description = "4체 연쇄 공격", mergeResultId = 18),
    UnitDef(id = 18, name = "뇌전의 군주", grade = UnitGrade.SUPREME, family = UnitFamily.LIGHTNING,
        baseATK = 180, baseSpeed = 1.5f, range = 200f, iconRes = iconFor(18),
        abilityName = "체인", description = "5체 연쇄 공격", mergeResultId = 23),
    UnitDef(id = 23, name = "폭풍신", grade = UnitGrade.TRANSCENDENT, family = UnitFamily.LIGHTNING,
        baseATK = 350, baseSpeed = 1.5f, range = 220f, iconRes = iconFor(23),
        abilityName = "체인", description = "모든 적에게 연쇄 번개"),

    // === 보조 계열 (Buff) ===
    UnitDef(id = 4, name = "격려병", grade = UnitGrade.LOW, family = UnitFamily.SUPPORT,
        baseATK = 10, baseSpeed = 0.5f, range = 100f, iconRes = iconFor(4),
        abilityName = "버프", description = "아군 공격력 +10%", mergeResultId = 9),
    UnitDef(id = 9, name = "지휘관", grade = UnitGrade.MEDIUM, family = UnitFamily.SUPPORT,
        baseATK = 20, baseSpeed = 0.5f, range = 120f, iconRes = iconFor(9),
        abilityName = "버프", description = "아군 공격력 +20%", mergeResultId = 14),
    UnitDef(id = 14, name = "대사제", grade = UnitGrade.HIGH, family = UnitFamily.SUPPORT,
        baseATK = 40, baseSpeed = 0.5f, range = 140f, iconRes = iconFor(14),
        abilityName = "버프", description = "아군 공격력 +30%", mergeResultId = 19),
    UnitDef(id = 19, name = "전쟁의 군주", grade = UnitGrade.SUPREME, family = UnitFamily.SUPPORT,
        baseATK = 80, baseSpeed = 0.6f, range = 160f, iconRes = iconFor(19),
        abilityName = "버프", description = "아군 공격력 +40%", mergeResultId = 24),
    UnitDef(id = 24, name = "수호신", grade = UnitGrade.TRANSCENDENT, family = UnitFamily.SUPPORT,
        baseATK = 150, baseSpeed = 0.6f, range = 180f, iconRes = iconFor(24),
        abilityName = "버프", description = "아군 전체 공격력 +50%"),
)

val UNIT_DEFS_MAP: Map<Int, UnitDef> = UNIT_DEFS.associateBy { it.id }
