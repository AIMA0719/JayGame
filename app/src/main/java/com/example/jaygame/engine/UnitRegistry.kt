@file:Suppress("DEPRECATION")
package com.example.jaygame.engine

/**
 * 전체 유닛 정의 레지스트리.
 * 5계열 x 7등급 = 35종 유닛.
 *
 * 머지 규칙: 동일 계열 동일 등급 3개 → 같은 계열 상위 등급 1개
 *
 * @deprecated Use [BlueprintRegistry] instead. Unit data now lives in blueprints.json.
 * TODO(Task18): Remove once all consumers are migrated.
 */
@Deprecated("Use BlueprintRegistry instead. Unit data now lives in blueprints.json.")
object UnitRegistry {

    val specs: List<UnitSpec> = buildList {
        // === 화염 계열 (Splash) ===
        add(UnitSpec(id = 0, name = "화염병", grade = UnitGrade.COMMON, family = "화염",
            baseATK = 25f, atkSpeed = 1.0f, range = 150f,
            abilityType = AbilityType.SPLASH, description = "주변 적에게 범위 피해", mergeResultId = 5))
        add(UnitSpec(id = 5, name = "화염술사", grade = UnitGrade.RARE, family = "화염",
            baseATK = 50f, atkSpeed = 1.0f, range = 160f,
            abilityType = AbilityType.SPLASH, description = "강화된 범위 피해", mergeResultId = 10))
        add(UnitSpec(id = 10, name = "이그니스", grade = UnitGrade.HERO, family = "화염",
            baseATK = 100f, atkSpeed = 1.1f, range = 170f,
            abilityType = AbilityType.LINGERING_FIRE, description = "공격 시 잔불 생성, 3초간 지속 범위 피해", mergeResultId = 15))
        add(UnitSpec(id = 15, name = "인페르노", grade = UnitGrade.LEGEND, family = "화염",
            baseATK = 200f, atkSpeed = 1.2f, range = 180f,
            abilityType = AbilityType.FIRESTORM, description = "12초 쿨 — 밀집 지점에 운석, 300% ATK 범위 피해 + 소각 디버프", mergeResultId = 20))
        add(UnitSpec(id = 20, name = "화산왕", grade = UnitGrade.ANCIENT, family = "화염",
            baseATK = 350f, atkSpeed = 1.3f, range = 200f,
            abilityType = AbilityType.VOLCANIC_ERUPTION, description = "20초 쿨 — 8초간 화산 소환, 용암 폭격 + 화염 계열 공속 +25%", mergeResultId = 25))
        add(UnitSpec(id = 25, name = "피닉스", grade = UnitGrade.MYTHIC, family = "화염",
            baseATK = 550f, atkSpeed = 1.4f, range = 220f,
            abilityType = AbilityType.PHOENIX_REBIRTH, description = "사망 시 500% ATK 폭발 후 10초 뒤 부활 / 25초 쿨 — 6초간 융단폭격", mergeResultId = 30))
        add(UnitSpec(id = 30, name = "태양신 라", grade = UnitGrade.IMMORTAL, family = "화염",
            baseATK = 800f, atkSpeed = 1.5f, range = 250f,
            abilityType = AbilityType.SUPERNOVA, description = "5회 공격마다 태양 플레어 400% ATK / 45초 쿨 — 초신성: 전체 1000% ATK, 30%이하 즉사"))

        // === 냉기 계열 (Slow) ===
        add(UnitSpec(id = 1, name = "냉기병", grade = UnitGrade.COMMON, family = "냉기",
            baseATK = 20f, atkSpeed = 0.8f, range = 160f,
            abilityType = AbilityType.SLOW, description = "적 이동속도 감소", mergeResultId = 6))
        add(UnitSpec(id = 6, name = "냉기술사", grade = UnitGrade.RARE, family = "냉기",
            baseATK = 40f, atkSpeed = 0.9f, range = 170f,
            abilityType = AbilityType.SLOW, description = "강화된 빙결 효과", mergeResultId = 11))
        add(UnitSpec(id = 11, name = "블리자드", grade = UnitGrade.HERO, family = "냉기",
            baseATK = 80f, atkSpeed = 1.0f, range = 180f,
            abilityType = AbilityType.FROST_NOVA, description = "10초 쿨 — 범위 내 전체 2초 빙결 + 피격 시 50% 추가 피해", mergeResultId = 16))
        add(UnitSpec(id = 16, name = "아이스본", grade = UnitGrade.LEGEND, family = "냉기",
            baseATK = 160f, atkSpeed = 1.0f, range = 190f,
            abilityType = AbilityType.ABSOLUTE_ZERO, description = "빙결 전파 (사망 시 2체 확산) / 15초 쿨 — 전체 3초 빙결 + 해동 시 HP 15% 파쇄", mergeResultId = 21))
        add(UnitSpec(id = 21, name = "빙하제왕", grade = UnitGrade.ANCIENT, family = "냉기",
            baseATK = 280f, atkSpeed = 1.1f, range = 200f,
            abilityType = AbilityType.ICE_AGE, description = "3회 빙결 시 영구 50% 둔화 / 22초 쿨 — 8초간 맵 60% 빙풍: 70% 둔화 + 빙결", mergeResultId = 26))
        add(UnitSpec(id = 26, name = "유키", grade = UnitGrade.MYTHIC, family = "냉기",
            baseATK = 450f, atkSpeed = 1.1f, range = 220f,
            abilityType = AbilityType.ETERNAL_WINTER, description = "10초마다 최전방 적 3초 빙결 + 2배 피해 / 30초 쿨 — 전체 4초 빙결 + 아군 쿨 5초 감소", mergeResultId = 31))
        add(UnitSpec(id = 31, name = "크로노스", grade = UnitGrade.IMMORTAL, family = "냉기",
            baseATK = 700f, atkSpeed = 1.2f, range = 250f,
            abilityType = AbilityType.TIME_STOP, description = "범위 내 영구 40% 둔화 / 50초 쿨 — 시간정지 5초: 아군 3배속, 축적 피해 한방 폭발, 25%이하 즉사"))

        // === 독 계열 (DoT) ===
        add(UnitSpec(id = 2, name = "독침병", grade = UnitGrade.COMMON, family = "독",
            baseATK = 15f, atkSpeed = 0.9f, range = 140f,
            abilityType = AbilityType.DOT, description = "지속 독 피해", mergeResultId = 7))
        add(UnitSpec(id = 7, name = "독술사", grade = UnitGrade.RARE, family = "독",
            baseATK = 30f, atkSpeed = 1.0f, range = 150f,
            abilityType = AbilityType.DOT, description = "강화된 독 피해", mergeResultId = 12))
        add(UnitSpec(id = 12, name = "플레이그", grade = UnitGrade.HERO, family = "독",
            baseATK = 60f, atkSpeed = 1.1f, range = 160f,
            abilityType = AbilityType.CONTAGION, description = "독 사망 시 1체 전염 (80% 피해) / 10초 쿨 — 독구름 5초, 힐 50% 감소", mergeResultId = 17))
        add(UnitSpec(id = 17, name = "코로시브", grade = UnitGrade.LEGEND, family = "독",
            baseATK = 120f, atkSpeed = 1.2f, range = 170f,
            abilityType = AbilityType.CORROSION, description = "독 대상 방어력 초당 5% 감소 (최대 50%) / 14초 쿨 — 산성 분사: 즉시 최대 부식 + 40% 둔화", mergeResultId = 22))
        add(UnitSpec(id = 22, name = "헤카테", grade = UnitGrade.ANCIENT, family = "독",
            baseATK = 220f, atkSpeed = 1.2f, range = 180f,
            abilityType = AbilityType.TOXIC_DOMAIN, description = "4회 공격마다 죽음의 표식 (25% 추가 피해, 사망 시 독 폭발) / 20초 쿨 — 독 늪 10초", mergeResultId = 27))
        add(UnitSpec(id = 27, name = "니드호그", grade = UnitGrade.MYTHIC, family = "독",
            baseATK = 400f, atkSpeed = 1.3f, range = 200f,
            abilityType = AbilityType.VENOMOUS_BREATH, description = "독 계열 전체 피해 +30% / 25초 쿨 — 맹독 브레스: 400% ATK + 모든 디버프 적용, 20%이하 즉사", mergeResultId = 32))
        add(UnitSpec(id = 32, name = "아포칼립스", grade = UnitGrade.IMMORTAL, family = "독",
            baseATK = 650f, atkSpeed = 1.3f, range = 250f,
            abilityType = AbilityType.UNIVERSAL_DECAY, description = "전 적 초당 1% HP 감소 / 50초 쿨 — 종말 역병 15초: 초당 3% HP 관통 피해, 방어 0, 생존 시 영구 25% 약화"))

        // === 번개 계열 (Chain) ===
        add(UnitSpec(id = 3, name = "전격병", grade = UnitGrade.COMMON, family = "번개",
            baseATK = 22f, atkSpeed = 1.2f, range = 170f,
            abilityType = AbilityType.CHAIN, description = "2체 연쇄 공격", mergeResultId = 8))
        add(UnitSpec(id = 8, name = "전격술사", grade = UnitGrade.RARE, family = "번개",
            baseATK = 45f, atkSpeed = 1.3f, range = 180f,
            abilityType = AbilityType.CHAIN, description = "3체 연쇄 공격", mergeResultId = 13))
        add(UnitSpec(id = 13, name = "썬더", grade = UnitGrade.HERO, family = "번개",
            baseATK = 90f, atkSpeed = 1.4f, range = 190f,
            abilityType = AbilityType.OVERCHARGE, description = "5회마다 과충전: 200% 피해 + 2체 추가 연쇄 + 30% 스턴 / 8초 쿨 — 최고HP 적에게 낙뢰 250%", mergeResultId = 18))
        add(UnitSpec(id = 18, name = "스톰", grade = UnitGrade.LEGEND, family = "번개",
            baseATK = 180f, atkSpeed = 1.5f, range = 200f,
            abilityType = AbilityType.STATIC_FIELD, description = "정전기장: 진입 시 피해 + 20% 둔화, 아군 공속 +15% / 14초 쿨 — 8체 연쇄번개, 바운스당 +10% 피해", mergeResultId = 23))
        add(UnitSpec(id = 23, name = "뇌왕", grade = UnitGrade.ANCIENT, family = "번개",
            baseATK = 320f, atkSpeed = 1.5f, range = 220f,
            abilityType = AbilityType.THUNDERSTORM, description = "번개 계열 공속 +20% / 20초 쿨 — 뇌우 8초: 매초 150% ATK 낙뢰 + 4체 연쇄, 3회 피격 시 3초 마비", mergeResultId = 28))
        add(UnitSpec(id = 28, name = "토르", grade = UnitGrade.MYTHIC, family = "번개",
            baseATK = 500f, atkSpeed = 1.6f, range = 240f,
            abilityType = AbilityType.MJOLNIR, description = "방어 50% 무시, 4회마다 충격파 / 28초 쿨 — 묠니르 투척: 관통 350% + 복귀 200%, 양쪽 맞으면 추가 300%", mergeResultId = 33))
        add(UnitSpec(id = 33, name = "제우스", grade = UnitGrade.IMMORTAL, family = "번개",
            baseATK = 750f, atkSpeed = 1.7f, range = 250f,
            abilityType = AbilityType.DIVINE_PUNISHMENT, description = "전체 동시 공격 (연쇄 무제한) / 50초 쿨 — 신벌: 전적 600% 관통 + 3초 스턴 + 10초간 아군 공격에 번개 추가"))

        // === 보조 계열 (Buff) ===
        add(UnitSpec(id = 4, name = "뮤즈", grade = UnitGrade.COMMON, family = "보조",
            baseATK = 10f, atkSpeed = 0.5f, range = 100f,
            abilityType = AbilityType.BUFF, description = "아군 공격력 +10%", mergeResultId = 9))
        add(UnitSpec(id = 9, name = "가디언", grade = UnitGrade.RARE, family = "보조",
            baseATK = 20f, atkSpeed = 0.5f, range = 120f,
            abilityType = AbilityType.BUFF, description = "아군 공격력 +20%", mergeResultId = 14))
        add(UnitSpec(id = 14, name = "오라클", grade = UnitGrade.HERO, family = "보조",
            baseATK = 40f, atkSpeed = 0.5f, range = 140f,
            abilityType = AbilityType.FORESIGHT, description = "아군 치명타 +15% / 10초 쿨 — 축복: 범위 내 아군 HP 10% 회복 + 공속 20% 5초", mergeResultId = 19))
        add(UnitSpec(id = 19, name = "발키리", grade = UnitGrade.LEGEND, family = "보조",
            baseATK = 80f, atkSpeed = 0.6f, range = 160f,
            abilityType = AbilityType.WAR_SONG, description = "킬 시 전체 ATK +5% 3초 (5중첩) / 15초 쿨 — 전쟁의 노래 6초: 전체 ATK +35%, 최강 유닛 ATK +100%", mergeResultId = 24))
        add(UnitSpec(id = 24, name = "세라핌", grade = UnitGrade.ANCIENT, family = "보조",
            baseATK = 140f, atkSpeed = 0.6f, range = 180f,
            abilityType = AbilityType.RESURRECTION, description = "15초마다 최약 아군에 보호막 (ATK 200%) / 25초 쿨 — 부활의 빛 8초: 전체 +30% + 쿨 5초 감소", mergeResultId = 29))
        add(UnitSpec(id = 29, name = "아르카나", grade = UnitGrade.MYTHIC, family = "보조",
            baseATK = 250f, atkSpeed = 0.7f, range = 200f,
            abilityType = AbilityType.UNIVERSAL_HARMONY, description = "계열별 특수 버프 (화염 범위+20%, 냉기 빙결+1초, 독 DoT+20%, 번개 연쇄+2) / 30초 쿨 — 조율 10초: 전체 +50%, 쿨 2배속", mergeResultId = 34))
        add(UnitSpec(id = 34, name = "가이아", grade = UnitGrade.IMMORTAL, family = "보조",
            baseATK = 400f, atkSpeed = 0.8f, range = 250f,
            abilityType = AbilityType.GENESIS, description = "전체 스탯 +25%, 골드 수입 +20% / 60초 쿨 — 창세의 빛 15초: 전체 쿨 초기화 + ATK 100% + 적 전체 약화 30%"))
    }

    val specsMap: Map<Int, UnitSpec> = specs.associateBy { it.id }

    /** 소환 가능한 유닛 (가챠로 뽑을 수 있는 것) */
    val summonableSpecs: List<UnitSpec> = specs.filter { it.grade.canSummon }

    /** 계열별 유닛 맵 */
    val familyMap: Map<String, List<UnitSpec>> = specs.groupBy { it.family }
}
