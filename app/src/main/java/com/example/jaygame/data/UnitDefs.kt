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
    35 to R.drawable.ic_unit_35, 36 to R.drawable.ic_unit_36, 37 to R.drawable.ic_unit_37,
    38 to R.drawable.ic_unit_38, 39 to R.drawable.ic_unit_39,
)
private fun iconFor(id: Int) = UNIT_ICONS[id] ?: R.drawable.ic_unit_0

enum class UnitGrade(val label: String, val color: Color, val weight: Int) {
    COMMON("일반", Color(0xFF9E9E9E), 60),
    RARE("희귀", Color(0xFF42A5F5), 25),
    HERO("영웅", Color(0xFFAB47BC), 12),
    LEGEND("전설", Color(0xFFFF8F00), 3),
    MYTHIC("신화", Color(0xFFFBBF24), 0),
    IMMORTAL("불멸", Color(0xFFFF1744), 0),
}

/** 종족 분류 (구 UnitFamily 대체) */
enum class UnitRace(val label: String, val color: Color) {
    HUMAN("인간", Color(0xFFFFCC80)),
    ANIMAL("동물", Color(0xFF81C784)),
    DEMON("악마", Color(0xFFEF5350)),
    SPIRIT("정령", Color(0xFF64B5F6)),
    ROBOT("로봇", Color(0xFF90A4AE)),
}

/** @deprecated 종족(UnitRace)으로 대체됨. 기존 호환용으로만 유지. */
@Deprecated("Use UnitRace instead")
enum class UnitFamily(val label: String, val color: Color) {
    FIRE("화염", Color(0xFFFF6B35)),
    FROST("냉기", Color(0xFF64B5F6)),
    POISON("독", Color(0xFF81C784)),
    LIGHTNING("번개", Color(0xFFFFD54F)),
    SUPPORT("보조", Color(0xFFCE93D8)),
    WIND("바람", Color(0xFF80CBC4)),
}


/**
 * 고유 능력 정보 (고대 등급 이상)
 */
data class UniqueAbility(
    val name: String,
    val type: String,           // "패시브" or "액티브"
    val cooldown: Int = 0,      // seconds (0 = passive)
    val description: String,
)

@Deprecated("Use UnitBlueprint from BlueprintRegistry instead. Data now lives in blueprints.json.")
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
    val uniqueAbility: UniqueAbility? = null,
)


val UPGRADE_COSTS = listOf(2 to 100, 4 to 200, 10 to 500, 20 to 1000, 50 to 2000, 100 to 5000)
val LEVEL_MULTIPLIER = floatArrayOf(1.0f, 1.5f, 2.2f, 3.2f, 4.5f, 6.0f, 8.0f)

// TODO(Task18): Remove UNIT_DEFS once BattleEngine, UI screens, and BattleBridge are fully migrated to BlueprintRegistry.
@Deprecated("Use BlueprintRegistry.allBlueprints() instead. Data now lives in blueprints.json.")
@Suppress("DEPRECATION")
val UNIT_DEFS: List<UnitDef> = listOf(
    // ===== 화염 계열 (Splash → 고유 능력) =====
    UnitDef(id = 0, name = "루비", grade = UnitGrade.COMMON, family = UnitFamily.FIRE,
        baseATK = 25, baseSpeed = 1.0f, range = 150f, iconRes = iconFor(0),
        abilityName = "불꽃탄", description = "작지만 뜨거운 화염구를 던진다. 주변 적에게 범위 피해.", mergeResultId = 5),
    UnitDef(id = 5, name = "카르마", grade = UnitGrade.RARE, family = UnitFamily.FIRE,
        baseATK = 50, baseSpeed = 1.0f, range = 160f, iconRes = iconFor(5),
        abilityName = "화염참", description = "불꽃을 두른 검으로 적을 베어넘긴다. 강력한 범위 공격.", mergeResultId = 10),
    UnitDef(id = 10, name = "이그니스", grade = UnitGrade.HERO, family = UnitFamily.FIRE,
        baseATK = 100, baseSpeed = 1.1f, range = 170f, iconRes = iconFor(10),
        abilityName = "잔불", description = "공격 시 지면에 불꽃 잔해를 남긴다.",
        uniqueAbility = UniqueAbility(
            name = "잔불 (Lingering Flames)",
            type = "패시브",
            description = "공격 적중 지점에 3초간 불꽃 잔해 생성. 잔해 위의 적은 매초 ATK의 15% 추가 화염 피해. 잔해 반경: 60."
        ), mergeResultId = 15),
    UnitDef(id = 15, name = "인페르노", grade = UnitGrade.LEGEND, family = UnitFamily.FIRE,
        baseATK = 200, baseSpeed = 1.2f, range = 180f, iconRes = iconFor(15),
        abilityName = "화염폭풍", description = "화산의 분노를 품은 군주.",
        uniqueAbility = UniqueAbility(
            name = "화염폭풍 (Firestorm)",
            type = "액티브", cooldown = 12,
            description = "적이 가장 밀집한 지점에 운석을 소환. ATK 300% 범위 피해. 피격된 적은 4초간 '소각' 상태: 받는 피해 20% 증가."
        ), mergeResultId = 20),
    UnitDef(id = 20, name = "화산왕", grade = UnitGrade.MYTHIC, family = UnitFamily.FIRE,
        baseATK = 350, baseSpeed = 1.3f, range = 200f, iconRes = iconFor(20),
        abilityName = "화산 분출", description = "대지를 불태우는 군주.",
        uniqueAbility = UniqueAbility(
            name = "화산 분출 (Volcanic Eruption)",
            type = "액티브", cooldown = 20,
            description = "지정 위치에 8초간 화산 소환. 1.5초마다 용암탄 발사 (ATK 200% + 3초 용암 장판). 활성 중 화염 계열 전체 공속 +25%."
        ), mergeResultId = 25),
    UnitDef(id = 25, name = "피닉스", grade = UnitGrade.MYTHIC, family = UnitFamily.FIRE,
        baseATK = 550, baseSpeed = 1.4f, range = 220f, iconRes = iconFor(20),
        abilityName = "불사조의 날개", description = "재에서 부활하는 불멸의 화신.",
        uniqueAbility = UniqueAbility(
            name = "불사 / 불사조의 날개",
            type = "패시브 + 액티브", cooldown = 25,
            description = "[패시브] 제거 시 ATK 500% 폭발 후 10초 뒤 부활 (전투당 1회).\n[액티브] 25초 쿨 — 6초간 비행하며 경로 전체 융단폭격 (ATK 150% 범위). 비행 중 슬롯 비움."
        ), mergeResultId = 30),
    UnitDef(id = 30, name = "태양신 라", grade = UnitGrade.MYTHIC, family = UnitFamily.FIRE,
        baseATK = 800, baseSpeed = 1.5f, range = 250f, iconRes = iconFor(20),
        abilityName = "초신성", description = "태양의 화신. 모든 것을 소각한다.",
        uniqueAbility = UniqueAbility(
            name = "태양의 심판 / 초신성 (Supernova)",
            type = "패시브 + 액티브", cooldown = 45,
            description = "[패시브] 5회 공격마다 태양 플레어: ATK 400% 범위 + 적 버프 제거. 범위 내 적 초당 최대HP 3% 감소.\n[액티브] 45초 쿨 — 2초 충전 후 전체 ATK 1000%. HP 30% 이하 즉사. 10초간 전장 화염 + 아군 ATK +50%."
        )),

    // ===== 냉기 계열 (Slow → 고유 능력) =====
    UnitDef(id = 1, name = "미스트", grade = UnitGrade.COMMON, family = UnitFamily.FROST,
        baseATK = 20, baseSpeed = 0.8f, range = 160f, iconRes = iconFor(1),
        abilityName = "냉기화살", description = "안개를 얼려 만든 화살. 적의 이동속도를 감소시킨다.", mergeResultId = 6),
    UnitDef(id = 6, name = "프로스트", grade = UnitGrade.RARE, family = UnitFamily.FROST,
        baseATK = 40, baseSpeed = 0.9f, range = 170f, iconRes = iconFor(6),
        abilityName = "동결", description = "차가운 마법으로 적을 얼린다. 강화된 슬로우 효과.", mergeResultId = 11),
    UnitDef(id = 11, name = "블리자드", grade = UnitGrade.HERO, family = UnitFamily.FROST,
        baseATK = 80, baseSpeed = 1.0f, range = 180f, iconRes = iconFor(11),
        abilityName = "냉기 파동", description = "눈보라를 일으키는 현자.",
        uniqueAbility = UniqueAbility(
            name = "서리 갑옷 / 냉기 파동 (Frost Nova)",
            type = "패시브 + 액티브", cooldown = 10,
            description = "[패시브] 범위 내 아군에게 서리 방패 부여 (ATK 10% 흡수, 8초 갱신). 방패 공격한 적 2초 30% 둔화.\n[액티브] 10초 쿨 — 범위 내 전체 2초 빙결. 빙결 적 첫 피격 시 50% 추가 피해."
        ), mergeResultId = 16),
    UnitDef(id = 16, name = "아이스본", grade = UnitGrade.LEGEND, family = UnitFamily.FROST,
        baseATK = 160, baseSpeed = 1.0f, range = 190f, iconRes = iconFor(16),
        abilityName = "절대영도", description = "만년빙에서 태어난 존재.",
        uniqueAbility = UniqueAbility(
            name = "빙결 전파 / 절대영도 (Absolute Zero)",
            type = "패시브 + 액티브", cooldown = 15,
            description = "[패시브] 빙결 상태 적 사망 시 2체에 1.5초 빙결 전파 (최대 3회 연쇄).\n[액티브] 15초 쿨 — 전체 적 3초 빙결. 해동 시 현재 HP 15% 파쇄 피해. 보스: 1.5초."
        ), mergeResultId = 21),
    UnitDef(id = 21, name = "빙하제왕", grade = UnitGrade.MYTHIC, family = UnitFamily.FROST,
        baseATK = 280, baseSpeed = 1.1f, range = 200f, iconRes = iconFor(21),
        abilityName = "빙하시대", description = "영구동토의 군주.",
        uniqueAbility = UniqueAbility(
            name = "영구동토 / 빙하시대 (Ice Age)",
            type = "패시브 + 액티브", cooldown = 22,
            description = "[패시브] 3회 빙결당한 적 영구 50% 둔화. 공격 5중첩 시 2초 빙결.\n[액티브] 22초 쿨 — 맵 60% 빙풍 8초: 70% 둔화 + 매초 80 ATK. 4초 체류 시 3초 빙결. 아군 공속 +30%."
        ), mergeResultId = 26),
    UnitDef(id = 26, name = "유키", grade = UnitGrade.MYTHIC, family = UnitFamily.FROST,
        baseATK = 450, baseSpeed = 1.1f, range = 220f, iconRes = iconFor(21),
        abilityName = "영원의 겨울", description = "시간마저 얼리는 빙설의 화신.",
        uniqueAbility = UniqueAbility(
            name = "시간동결 / 영원의 겨울 (Eternal Winter)",
            type = "패시브 + 액티브", cooldown = 30,
            description = "[패시브] 10초마다 최전방 적 3초 무조건 빙결. 유키의 빙결 대상은 받는 피해 2배.\n[액티브] 30초 쿨 — 전체 4초 빙결 + 아군 쿨타임 5초 감소. 해동 시 최대HP 20% 파쇄 + 영구 30% 둔화. 보스: 2초."
        ), mergeResultId = 31),
    UnitDef(id = 31, name = "크로노스", grade = UnitGrade.MYTHIC, family = UnitFamily.FROST,
        baseATK = 700, baseSpeed = 1.2f, range = 250f, iconRes = iconFor(21),
        abilityName = "시간정지", description = "시간의 지배자. 만물을 정지시킨다.",
        uniqueAbility = UniqueAbility(
            name = "절대시간 / 시간정지 (Time Stop)",
            type = "패시브 + 액티브", cooldown = 50,
            description = "[패시브] 범위 내 적 영구 40% 둔화. 처치 시 다음 액티브 지속시간 +0.5초 (최대 +5초).\n[액티브] 50초 쿨 — 전적 시간정지 5초(+보너스). 정지 중 아군 3배속 공격. 정지 해제 시 축적 피해 한방 폭발. HP 25% 이하 파쇄(즉사). 이후 15초간 이속 50% 감소."
        )),

    // ===== 독 계열 (DoT → 고유 능력) =====
    UnitDef(id = 2, name = "베놈", grade = UnitGrade.COMMON, family = UnitFamily.POISON,
        baseATK = 15, baseSpeed = 0.9f, range = 140f, iconRes = iconFor(2),
        abilityName = "독침", description = "독을 바른 침으로 공격한다. 적에게 지속 독 피해.", mergeResultId = 7),
    UnitDef(id = 7, name = "바이퍼", grade = UnitGrade.RARE, family = UnitFamily.POISON,
        baseATK = 30, baseSpeed = 1.0f, range = 150f, iconRes = iconFor(7),
        abilityName = "맹독", description = "독사의 기술을 익힌 암살자. 치명적인 독.", mergeResultId = 12),
    UnitDef(id = 12, name = "플레이그", grade = UnitGrade.HERO, family = UnitFamily.POISON,
        baseATK = 60, baseSpeed = 1.1f, range = 160f, iconRes = iconFor(12),
        abilityName = "전염", description = "역병을 퍼뜨리는 마법사.",
        uniqueAbility = UniqueAbility(
            name = "전염 / 역병 구름 (Contagion)",
            type = "패시브 + 액티브", cooldown = 10,
            description = "[패시브] 독 사망 적에서 1체 전염 (원래 독 80% 피해).\n[액티브] 10초 쿨 — 독구름 5초 (반경 80): 매초 ATK 40% + 힐 50% 감소."
        ), mergeResultId = 17),
    UnitDef(id = 17, name = "코로시브", grade = UnitGrade.LEGEND, family = UnitFamily.POISON,
        baseATK = 120, baseSpeed = 1.2f, range = 170f, iconRes = iconFor(17),
        abilityName = "부식", description = "만물을 부식시키는 안개의 군주.",
        uniqueAbility = UniqueAbility(
            name = "부식 / 맹독 분사 (Corrosion)",
            type = "패시브 + 액티브", cooldown = 14,
            description = "[패시브] 독 대상 방어력 초당 5% 감소 (최대 50%). 최대 중첩 시 모든 피해가 관통 피해.\n[액티브] 14초 쿨 — 전방 부채꼴 산성 분사: ATK 250% + 즉시 부식 최대 중첩 + 6초간 40% 둔화."
        ), mergeResultId = 22),
    UnitDef(id = 22, name = "헤카테", grade = UnitGrade.MYTHIC, family = UnitFamily.POISON,
        baseATK = 220, baseSpeed = 1.2f, range = 180f, iconRes = iconFor(22),
        abilityName = "죽음의 표식", description = "독의 여신.",
        uniqueAbility = UniqueAbility(
            name = "죽음의 표식 / 독의 영역 (Toxic Domain)",
            type = "패시브 + 액티브", cooldown = 20,
            description = "[패시브] 4회 공격마다 표식 8초: 받는 피해 +25%, 힐 불가. 표식 대상 사망 시 독 폭발 (ATK 100%).\n[액티브] 20초 쿨 — 독 늪 10초: 매초 ATK 60% + 50% 둔화 + 방어 30% 감소. 독 계열 ATK +40%."
        ), mergeResultId = 27),
    UnitDef(id = 27, name = "니드호그", grade = UnitGrade.MYTHIC, family = UnitFamily.POISON,
        baseATK = 400, baseSpeed = 1.3f, range = 200f, iconRes = iconFor(22),
        abilityName = "맹독의 숨결", description = "세계수를 갉아먹는 독룡.",
        uniqueAbility = UniqueAbility(
            name = "세계수의 독 / 맹독의 숨결",
            type = "패시브 + 액티브", cooldown = 25,
            description = "[패시브] 독 계열 전체 DoT +30%. 독 사망 적 독시체 5초 잔류.\n[액티브] 25초 쿨 — 전방 40% 맹독 브레스 6초: ATK 400% + 부식/표식/둔화 전부 적용. HP 20% 이하 즉사. 사망 시 독 포자 2개 생성."
        ), mergeResultId = 32),
    UnitDef(id = 32, name = "아포칼립스", grade = UnitGrade.MYTHIC, family = UnitFamily.POISON,
        baseATK = 650, baseSpeed = 1.3f, range = 250f, iconRes = iconFor(22),
        abilityName = "만물부식", description = "종말의 독. 세상을 멸망시킨다.",
        uniqueAbility = UniqueAbility(
            name = "종말의 역병 / 만물부식 (Universal Decay)",
            type = "패시브 + 액티브", cooldown = 50,
            description = "[패시브] 전 적 영구 초당 최대HP 1% 감소. HP 50% 이하 적 독 피해 2배. 모든 아군 공격에 미량 독 부여.\n[액티브] 50초 쿨 — 종말역병 15초: 초당 최대HP 3% 관통, 방어 0, 힐 불가. 생존자 영구 25% 약화."
        )),

    // ===== 번개 계열 (Chain → 고유 능력) =====
    UnitDef(id = 3, name = "스파크", grade = UnitGrade.COMMON, family = UnitFamily.LIGHTNING,
        baseATK = 22, baseSpeed = 1.2f, range = 170f, iconRes = iconFor(3),
        abilityName = "전격탄", description = "전기를 모아 쏘는 수련생. 2체 연쇄 공격.", mergeResultId = 8),
    UnitDef(id = 8, name = "볼트", grade = UnitGrade.RARE, family = UnitFamily.LIGHTNING,
        baseATK = 45, baseSpeed = 1.3f, range = 180f, iconRes = iconFor(8),
        abilityName = "연쇄번개", description = "번개를 꿰뚫는 궁수. 3체 연쇄 공격.", mergeResultId = 13),
    UnitDef(id = 13, name = "썬더", grade = UnitGrade.HERO, family = UnitFamily.LIGHTNING,
        baseATK = 90, baseSpeed = 1.4f, range = 190f, iconRes = iconFor(13),
        abilityName = "과충전", description = "하늘에서 번개를 내리꽂는 마법사.",
        uniqueAbility = UniqueAbility(
            name = "과충전 / 낙뢰 (Overcharge)",
            type = "패시브 + 액티브", cooldown = 8,
            description = "[패시브] 5회마다 과충전 공격: 200% 피해 + 2체 추가 연쇄 + 30% 확률 1초 스턴.\n[액티브] 8초 쿨 — 최고 HP 적에게 낙뢰: ATK 250% + 1.5초 스턴 + 3체 연쇄 (60%)."
        ), mergeResultId = 18),
    UnitDef(id = 18, name = "스톰", grade = UnitGrade.LEGEND, family = UnitFamily.LIGHTNING,
        baseATK = 180, baseSpeed = 1.5f, range = 200f, iconRes = iconFor(18),
        abilityName = "정전기장", description = "폭풍을 부르는 군주.",
        uniqueAbility = UniqueAbility(
            name = "정전기장 / 뇌신의 심판 (Static Field)",
            type = "패시브 + 액티브", cooldown = 14,
            description = "[패시브] 반경 120 전기장: 진입 적 ATK 20 피해 + 2초 20% 둔화. 장내 아군 공속 +15%.\n[액티브] 14초 쿨 — 8체 연쇄번개: ATK 180%, 바운스당 +10% (최종 250%). 전원 1초 스턴."
        ), mergeResultId = 23),
    UnitDef(id = 23, name = "뇌왕", grade = UnitGrade.MYTHIC, family = UnitFamily.LIGHTNING,
        baseATK = 320, baseSpeed = 1.5f, range = 220f, iconRes = iconFor(23),
        abilityName = "뇌격", description = "번개의 군주.",
        uniqueAbility = UniqueAbility(
            name = "번개의 축복 / 뇌격 (Thunderstorm)",
            type = "패시브 + 액티브", cooldown = 20,
            description = "[패시브] 번개 계열 전체 공속 +20%. 항상 2체 동시 공격.\n[액티브] 20초 쿨 — 뇌우 8초: 매초 150% ATK 낙뢰 + 4체 연쇄. 3회 피격 시 '마비' 3초 (스턴 + 받는 피해 +30%)."
        ), mergeResultId = 28),
    UnitDef(id = 28, name = "토르", grade = UnitGrade.MYTHIC, family = UnitFamily.LIGHTNING,
        baseATK = 500, baseSpeed = 1.6f, range = 240f, iconRes = iconFor(23),
        abilityName = "묠니르", description = "천둥의 신.",
        uniqueAbility = UniqueAbility(
            name = "묠니르 / 천둥벼락 (Mjolnir)",
            type = "패시브 + 액티브", cooldown = 28,
            description = "[패시브] 방어 50% 무시. 4회마다 직선 충격파. 액티브 후 5초간 공속 2배.\n[액티브] 28초 쿨 — 묠니르 투척: 전장 관통 ATK 350%. 복귀 시 200% 추가. 양쪽 피격 시 보너스 300%. 전원 2초 스턴."
        ), mergeResultId = 33),
    UnitDef(id = 33, name = "제우스", grade = UnitGrade.MYTHIC, family = UnitFamily.LIGHTNING,
        baseATK = 750, baseSpeed = 1.7f, range = 250f, iconRes = iconFor(23),
        abilityName = "신벌", description = "올림포스의 왕. 하늘이 갈라진다.",
        uniqueAbility = UniqueAbility(
            name = "올림포스의 왕 / 신벌 (Divine Punishment)",
            type = "패시브 + 액티브", cooldown = 50,
            description = "[패시브] 전 아군 공속 +10%. 범위 내 전 적 동시 공격 (연쇄 무제한). 처치 시 분열 방지.\n[액티브] 50초 쿨 — 전 적 ATK 600% 관통 + 3초 스턴. 10초간 모든 아군 공격에 미니번개 (제우스 ATK 50%) 추가. 보스 최대HP 10% 추가."
        )),

    // ===== 보조 계열 (Buff → 고유 능력) =====
    UnitDef(id = 4, name = "뮤즈", grade = UnitGrade.COMMON, family = UnitFamily.SUPPORT,
        baseATK = 10, baseSpeed = 0.5f, range = 100f, iconRes = iconFor(4),
        abilityName = "격려", description = "용기를 북돋는 견습 사제. 아군 공격력 +10%.", mergeResultId = 9),
    UnitDef(id = 9, name = "가디언", grade = UnitGrade.RARE, family = UnitFamily.SUPPORT,
        baseATK = 20, baseSpeed = 0.5f, range = 120f, iconRes = iconFor(9),
        abilityName = "보호막", description = "동료를 지키는 수호 기사. 아군 공격력 +20%.", mergeResultId = 14),
    UnitDef(id = 14, name = "오라클", grade = UnitGrade.HERO, family = UnitFamily.SUPPORT,
        baseATK = 40, baseSpeed = 0.5f, range = 140f, iconRes = iconFor(14),
        abilityName = "예지", description = "미래를 보는 대사제.",
        uniqueAbility = UniqueAbility(
            name = "예지 / 축복 (Foresight)",
            type = "패시브 + 액티브", cooldown = 10,
            description = "[패시브] 범위 내 아군 치명타 확률 +15% (치명타 180% 피해).\n[액티브] 10초 쿨 — 범위 내 아군 HP 10% 회복 + 5초간 공속 +20%."
        ), mergeResultId = 19),
    UnitDef(id = 19, name = "발키리", grade = UnitGrade.LEGEND, family = UnitFamily.SUPPORT,
        baseATK = 80, baseSpeed = 0.6f, range = 160f, iconRes = iconFor(19),
        abilityName = "전쟁의 노래", description = "전장의 여신.",
        uniqueAbility = UniqueAbility(
            name = "전사의 혼 / 전쟁의 노래 (War Song)",
            type = "패시브 + 액티브", cooldown = 15,
            description = "[패시브] 아군 킬 시 전체 ATK +5% 3초 (5중첩, 최대 +25%). 10연속 킬 시 전체 공속 +50% 3초.\n[액티브] 15초 쿨 — 전투 찬가 6초: 전체 ATK +35%, 공속 +25%. 최강 유닛 ATK +100%."
        ), mergeResultId = 24),
    UnitDef(id = 24, name = "세라핌", grade = UnitGrade.MYTHIC, family = UnitFamily.SUPPORT,
        baseATK = 140, baseSpeed = 0.6f, range = 180f, iconRes = iconFor(24),
        abilityName = "부활의 빛", description = "천사의 가호.",
        uniqueAbility = UniqueAbility(
            name = "천사의 가호 / 부활의 빛 (Resurrection)",
            type = "패시브 + 액티브", cooldown = 25,
            description = "[패시브] 15초마다 최약 아군에 신성 방패 (ATK 200% 흡수, 8초). 방패 중 디버프 면역.\n[액티브] 25초 쿨 — 성광 8초: 전체 ATK/공속 +30% + 쿨 5초 감소 + 초당 3% HP 재생. 고대 미만 유닛 1체 8초간 고대급 스탯."
        ), mergeResultId = 29),
    UnitDef(id = 29, name = "아르카나", grade = UnitGrade.MYTHIC, family = UnitFamily.SUPPORT,
        baseATK = 250, baseSpeed = 0.7f, range = 200f, iconRes = iconFor(24),
        abilityName = "만물조율", description = "운명의 실을 다루는 현자.",
        uniqueAbility = UniqueAbility(
            name = "운명의 실 / 만물조율 (Universal Harmony)",
            type = "패시브 + 액티브", cooldown = 30,
            description = "[패시브] 계열별 버프: 화염 범위+20%, 냉기 빙결+1초, 독 DoT+20%, 번개 연쇄+2, 보조 효과+20%. 아군 액티브 시 자신 쿨 10% 감소.\n[액티브] 30초 쿨 — 조율 10초: 전체 ATK +50%, 쿨 2배속, 디버프 50% 강화, 피해의 2% 회복. 종료 시 10% 총피해 폭발."
        ), mergeResultId = 34),
    UnitDef(id = 34, name = "가이아", grade = UnitGrade.MYTHIC, family = UnitFamily.SUPPORT,
        baseATK = 400, baseSpeed = 0.8f, range = 250f, iconRes = iconFor(24),
        abilityName = "창세의 빛", description = "세계의 의지. 존재만으로 모든 것이 강해진다.",
        uniqueAbility = UniqueAbility(
            name = "세계의 의지 / 창세의 빛 (Light of Genesis)",
            type = "패시브 + 액티브", cooldown = 60,
            description = "[패시브] 전 아군 ATK/공속/사거리 +25%. 유닛 제거 방지 1회 (120초 쿨). 골드 수입 +20%.\n[액티브] 60초 쿨 — 창세 15초: 전체 쿨 초기화 + ATK +100%, 공속 +50%. 적 전체 이속/방어/공격 -30%. 처치 시 20% 확률 보너스 골드. 종료 후 5초간 점진 해제."
        )),

    // ===== 바람 계열 (Knockback → 고유 능력) =====
    UnitDef(id = 35, name = "제피르", grade = UnitGrade.COMMON, family = UnitFamily.WIND,
        baseATK = 18, baseSpeed = 1.1f, range = 160f, iconRes = iconFor(35),
        abilityName = "돌풍", description = "산들바람을 모아 쏘는 바람의 견습생. 적을 밀쳐낸다.", mergeResultId = 36),
    UnitDef(id = 36, name = "게일", grade = UnitGrade.RARE, family = UnitFamily.WIND,
        baseATK = 38, baseSpeed = 1.2f, range = 170f, iconRes = iconFor(36),
        abilityName = "질풍참", description = "바람을 두른 칼날로 적을 베어넘긴다. 넉백 강화.", mergeResultId = 37),
    UnitDef(id = 37, name = "사이클론", grade = UnitGrade.HERO, family = UnitFamily.WIND,
        baseATK = 75, baseSpeed = 1.3f, range = 180f, iconRes = iconFor(37),
        abilityName = "회오리", description = "회오리바람을 일으키는 마법사.",
        uniqueAbility = UniqueAbility(
            name = "회오리 / 진공파 (Cyclone)",
            type = "패시브 + 액티브", cooldown = 10,
            description = "[패시브] 공격 시 25% 확률로 소형 회오리 생성 (2초간 범위 내 적 끌어당김 + ATK 30%).\n[액티브] 10초 쿨 — 지정 위치에 회오리 3초: 적 끌어당기며 ATK 150% + 40% 둔화."
        ), mergeResultId = 38),
    UnitDef(id = 38, name = "태풍", grade = UnitGrade.LEGEND, family = UnitFamily.WIND,
        baseATK = 150, baseSpeed = 1.4f, range = 200f, iconRes = iconFor(38),
        abilityName = "폭풍의 눈", description = "태풍을 부르는 군주.",
        uniqueAbility = UniqueAbility(
            name = "폭풍의 눈 / 초속의 칼날 (Eye of Storm)",
            type = "패시브 + 액티브", cooldown = 14,
            description = "[패시브] 범위 내 아군 이속/공속 +15%. 적 투사체 20% 확률 빗나감.\n[액티브] 14초 쿨 — 폭풍의 눈 6초: 범위 내 적 ATK 200% + 지속 넉백 + 아군 회피 +30%."
        ), mergeResultId = 39),
    UnitDef(id = 39, name = "하늘군주", grade = UnitGrade.MYTHIC, family = UnitFamily.WIND,
        baseATK = 270, baseSpeed = 1.5f, range = 220f, iconRes = iconFor(39),
        abilityName = "진공의 지배", description = "하늘을 지배하는 바람의 왕.",
        uniqueAbility = UniqueAbility(
            name = "하늘의 지배 / 진공 절단 (Sky Dominion)",
            type = "패시브 + 액티브", cooldown = 20,
            description = "[패시브] 바람 계열 전체 공속 +20%. 공격 적중 시 적 방어력 10% 무시.\n[액티브] 20초 쿨 — 진공 절단 8초: 직선 범위 ATK 300% + 방어 무시 + 3초 침묵. 바람 계열 ATK +40%."
        ), mergeResultId = 40),
    UnitDef(id = 40, name = "실프", grade = UnitGrade.MYTHIC, family = UnitFamily.WIND,
        baseATK = 430, baseSpeed = 1.6f, range = 240f, iconRes = iconFor(39),
        abilityName = "천공의 날개", description = "바람의 정령왕. 공간을 찢는다.",
        uniqueAbility = UniqueAbility(
            name = "천공의 날개 / 차원 절단 (Dimensional Slash)",
            type = "패시브 + 액티브", cooldown = 28,
            description = "[패시브] 회피 30%. 3회 공격마다 분신 공격 (ATK 60%). 액티브 후 5초간 공속 2배.\n[액티브] 28초 쿨 — 차원 절단: 전장 관통 ATK 400%. 적 버프 제거 + 3초 침묵. 분신 3체 5초 소환 (ATK 40%)."
        ), mergeResultId = 41),
    UnitDef(id = 41, name = "바유", grade = UnitGrade.MYTHIC, family = UnitFamily.WIND,
        baseATK = 720, baseSpeed = 1.8f, range = 250f, iconRes = iconFor(39),
        abilityName = "만물의 숨결", description = "바람의 신. 만물의 숨결을 지배한다.",
        uniqueAbility = UniqueAbility(
            name = "만물의 숨결 / 종극의 바람 (Breath of All)",
            type = "패시브 + 액티브", cooldown = 50,
            description = "[패시브] 전 아군 공속 +15%. 범위 내 적 회피 불가. 처치 시 SP +5 회복.\n[액티브] 50초 쿨 — 종극의 바람 10초: 전 적 ATK 700% + 넉백 + 3초 스턴. 아군 전체 ATK +60%, 공속 +40%. 보스 최대HP 8% 추가."
        )),
)

// TODO(Task18): Remove UNIT_DEFS_MAP and helper functions once BattleEngine is migrated to BlueprintRegistry.
@Deprecated("Use BlueprintRegistry.findById() instead")
@Suppress("DEPRECATION")
val UNIT_DEFS_MAP: Map<Int, UnitDef> = UNIT_DEFS.associateBy { it.id }

/** Look up grade ordinal from unitDefId (replaces id / 5) */
@Deprecated("Use BlueprintRegistry.findById(blueprintId)?.grade instead")
@Suppress("DEPRECATION")
fun unitGradeOf(id: Int): Int = UNIT_DEFS_MAP[id]?.grade?.ordinal ?: 0

/** Look up family ordinal from unitDefId (replaces id % 5) */
@Deprecated("Use BlueprintRegistry.findById(blueprintId)?.families instead")
@Suppress("DEPRECATION")
fun unitFamilyOf(id: Int): Int = UNIT_DEFS_MAP[id]?.family?.ordinal ?: 0

/** Find unitDefId for a given grade+family combination */
@Deprecated("Use BlueprintRegistry.findByGradeAndFamily() instead")
@Suppress("DEPRECATION")
fun unitIdOf(grade: Int, familyOrdinal: Int): Int? =
    UNIT_DEFS.find { it.grade.ordinal == grade && it.family.ordinal == familyOrdinal }?.id
