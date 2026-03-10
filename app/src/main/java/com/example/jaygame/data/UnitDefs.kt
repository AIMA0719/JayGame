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
    // ===== 화염 계열 (Splash) — 불타는 전사들 =====
    UnitDef(id = 0, name = "루비", grade = UnitGrade.LOW, family = UnitFamily.FIRE,
        baseATK = 25, baseSpeed = 1.0f, range = 150f, iconRes = iconFor(0),
        abilityName = "불꽃탄", description = "작지만 뜨거운 화염구를 던진다. 주변 적에게 범위 피해.", mergeResultId = 5),
    UnitDef(id = 5, name = "카르마", grade = UnitGrade.MEDIUM, family = UnitFamily.FIRE,
        baseATK = 50, baseSpeed = 1.0f, range = 160f, iconRes = iconFor(5),
        abilityName = "화염참", description = "불꽃을 두른 검으로 적을 베어넘긴다. 강력한 범위 공격.", mergeResultId = 10),
    UnitDef(id = 10, name = "이그니스", grade = UnitGrade.HIGH, family = UnitFamily.FIRE,
        baseATK = 100, baseSpeed = 1.1f, range = 170f, iconRes = iconFor(10),
        abilityName = "폭염", description = "고대 화염 마법을 계승한 현자. 대지를 불태우는 광역 화염.", mergeResultId = 15),
    UnitDef(id = 15, name = "인페르노", grade = UnitGrade.SUPREME, family = UnitFamily.FIRE,
        baseATK = 200, baseSpeed = 1.2f, range = 180f, iconRes = iconFor(15),
        abilityName = "화염폭풍", description = "화산의 분노를 품은 군주. 모든 것을 잿더미로 만드는 화염 폭풍.", mergeResultId = 20),
    UnitDef(id = 20, name = "피닉스", grade = UnitGrade.TRANSCENDENT, family = UnitFamily.FIRE,
        baseATK = 400, baseSpeed = 1.3f, range = 200f, iconRes = iconFor(20),
        abilityName = "불사조의 날개", description = "재에서 부활하는 불멸의 화신. 그 날개짓 하나로 전장이 불바다가 된다."),

    // ===== 냉기 계열 (Slow) — 얼어붙은 마법사들 =====
    UnitDef(id = 1, name = "미스트", grade = UnitGrade.LOW, family = UnitFamily.FROST,
        baseATK = 20, baseSpeed = 0.8f, range = 160f, iconRes = iconFor(1),
        abilityName = "냉기화살", description = "안개를 얼려 만든 화살. 적의 이동속도를 감소시킨다.", mergeResultId = 6),
    UnitDef(id = 6, name = "프로스트", grade = UnitGrade.MEDIUM, family = UnitFamily.FROST,
        baseATK = 40, baseSpeed = 0.9f, range = 170f, iconRes = iconFor(6),
        abilityName = "동결", description = "차가운 마법으로 적을 얼린다. 강화된 슬로우 효과.", mergeResultId = 11),
    UnitDef(id = 11, name = "블리자드", grade = UnitGrade.HIGH, family = UnitFamily.FROST,
        baseATK = 80, baseSpeed = 1.0f, range = 180f, iconRes = iconFor(11),
        abilityName = "눈보라", description = "눈보라를 일으키는 현자. 광역 빙결로 적을 묶어둔다.", mergeResultId = 16),
    UnitDef(id = 16, name = "아이스본", grade = UnitGrade.SUPREME, family = UnitFamily.FROST,
        baseATK = 160, baseSpeed = 1.0f, range = 190f, iconRes = iconFor(16),
        abilityName = "절대영도", description = "만년빙에서 태어난 존재. 주변의 온도를 극한까지 낮춘다.", mergeResultId = 21),
    UnitDef(id = 21, name = "유키", grade = UnitGrade.TRANSCENDENT, family = UnitFamily.FROST,
        baseATK = 320, baseSpeed = 1.1f, range = 200f, iconRes = iconFor(21),
        abilityName = "빙하시대", description = "시간마저 얼리는 빙설의 화신. 세상을 영원한 겨울로 뒤덮는다."),

    // ===== 독 계열 (DoT) — 그림자 암살자들 =====
    UnitDef(id = 2, name = "베놈", grade = UnitGrade.LOW, family = UnitFamily.POISON,
        baseATK = 15, baseSpeed = 0.9f, range = 140f, iconRes = iconFor(2),
        abilityName = "독침", description = "독을 바른 침으로 공격한다. 적에게 지속 독 피해.", mergeResultId = 7),
    UnitDef(id = 7, name = "바이퍼", grade = UnitGrade.MEDIUM, family = UnitFamily.POISON,
        baseATK = 30, baseSpeed = 1.0f, range = 150f, iconRes = iconFor(7),
        abilityName = "맹독", description = "독사의 기술을 익힌 암살자. 치명적인 독으로 적을 서서히 죽인다.", mergeResultId = 12),
    UnitDef(id = 12, name = "플레이그", grade = UnitGrade.HIGH, family = UnitFamily.POISON,
        baseATK = 60, baseSpeed = 1.1f, range = 160f, iconRes = iconFor(12),
        abilityName = "역병", description = "역병을 퍼뜨리는 마법사. 닿는 모든 것이 썩어간다.", mergeResultId = 17),
    UnitDef(id = 17, name = "코로시브", grade = UnitGrade.SUPREME, family = UnitFamily.POISON,
        baseATK = 120, baseSpeed = 1.2f, range = 170f, iconRes = iconFor(17),
        abilityName = "부식안개", description = "만물을 부식시키는 안개의 군주. 갑옷도 뼈도 녹여버린다.", mergeResultId = 22),
    UnitDef(id = 22, name = "헤카테", grade = UnitGrade.TRANSCENDENT, family = UnitFamily.POISON,
        baseATK = 240, baseSpeed = 1.2f, range = 180f, iconRes = iconFor(22),
        abilityName = "죽음의 안개", description = "독의 여신. 그녀가 지나간 자리엔 풀 한 포기도 남지 않는다."),

    // ===== 번개 계열 (Chain) — 질풍의 사냥꾼들 =====
    UnitDef(id = 3, name = "스파크", grade = UnitGrade.LOW, family = UnitFamily.LIGHTNING,
        baseATK = 22, baseSpeed = 1.2f, range = 170f, iconRes = iconFor(3),
        abilityName = "전격탄", description = "전기를 모아 쏘는 수련생. 2체 연쇄 공격.", mergeResultId = 8),
    UnitDef(id = 8, name = "볼트", grade = UnitGrade.MEDIUM, family = UnitFamily.LIGHTNING,
        baseATK = 45, baseSpeed = 1.3f, range = 180f, iconRes = iconFor(8),
        abilityName = "연쇄번개", description = "번개를 꿰뚫는 궁수. 3체 연쇄 공격으로 적을 관통한다.", mergeResultId = 13),
    UnitDef(id = 13, name = "썬더", grade = UnitGrade.HIGH, family = UnitFamily.LIGHTNING,
        baseATK = 90, baseSpeed = 1.4f, range = 190f, iconRes = iconFor(13),
        abilityName = "낙뢰", description = "하늘에서 번개를 내리꽂는 마법사. 4체 연쇄 공격.", mergeResultId = 18),
    UnitDef(id = 18, name = "스톰", grade = UnitGrade.SUPREME, family = UnitFamily.LIGHTNING,
        baseATK = 180, baseSpeed = 1.5f, range = 200f, iconRes = iconFor(18),
        abilityName = "뇌신의 심판", description = "폭풍을 부르는 군주. 5체 연쇄 공격으로 전장을 지배한다.", mergeResultId = 23),
    UnitDef(id = 23, name = "토르", grade = UnitGrade.TRANSCENDENT, family = UnitFamily.LIGHTNING,
        baseATK = 350, baseSpeed = 1.5f, range = 220f, iconRes = iconFor(23),
        abilityName = "천둥벼락", description = "번개의 신. 하늘이 갈라지며 모든 적에게 벼락이 내리친다."),

    // ===== 보조 계열 (Buff) — 신성한 수호자들 =====
    UnitDef(id = 4, name = "뮤즈", grade = UnitGrade.LOW, family = UnitFamily.SUPPORT,
        baseATK = 10, baseSpeed = 0.5f, range = 100f, iconRes = iconFor(4),
        abilityName = "격려", description = "용기를 북돋는 견습 사제. 아군 공격력 +10%.", mergeResultId = 9),
    UnitDef(id = 9, name = "가디언", grade = UnitGrade.MEDIUM, family = UnitFamily.SUPPORT,
        baseATK = 20, baseSpeed = 0.5f, range = 120f, iconRes = iconFor(9),
        abilityName = "보호막", description = "동료를 지키는 수호 기사. 아군 공격력 +20%.", mergeResultId = 14),
    UnitDef(id = 14, name = "오라클", grade = UnitGrade.HIGH, family = UnitFamily.SUPPORT,
        baseATK = 40, baseSpeed = 0.5f, range = 140f, iconRes = iconFor(14),
        abilityName = "축복", description = "미래를 보는 대사제. 아군 공격력 +30%.", mergeResultId = 19),
    UnitDef(id = 19, name = "발키리", grade = UnitGrade.SUPREME, family = UnitFamily.SUPPORT,
        baseATK = 80, baseSpeed = 0.6f, range = 160f, iconRes = iconFor(19),
        abilityName = "전쟁의 노래", description = "전장의 여신. 그녀의 노래는 전사들에게 광기를 준다. 아군 공격력 +40%.", mergeResultId = 24),
    UnitDef(id = 24, name = "아르카나", grade = UnitGrade.TRANSCENDENT, family = UnitFamily.SUPPORT,
        baseATK = 150, baseSpeed = 0.6f, range = 180f, iconRes = iconFor(24),
        abilityName = "신의 은총", description = "만물의 수호신. 존재만으로 아군 전체가 강화된다. 공격력 +50%."),
)

val UNIT_DEFS_MAP: Map<Int, UnitDef> = UNIT_DEFS.associateBy { it.id }
