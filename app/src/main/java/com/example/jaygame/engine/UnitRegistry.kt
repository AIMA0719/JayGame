package com.example.jaygame.engine

/**
 * 전체 유닛 정의 레지스트리.
 * 메이플 랜덤 디펜스 메커니즘 기반 — 5계열 x 5등급 = 25종 유닛.
 *
 * 머지 규칙: 동일 계열 동일 등급 3개 → 같은 계열 상위 등급 1개
 * 예: 화염(하급) x3 → 화염(중급) x1
 */
object UnitRegistry {

    val specs: List<UnitSpec> = buildList {
        // === 화염 계열 (Splash) ===
        add(UnitSpec(id = 0, name = "화염병", grade = UnitGrade.LOW, family = "화염",
            baseATK = 25f, atkSpeed = 1.0f, range = 150f,
            abilityType = AbilityType.SPLASH, description = "주변 적에게 범위 피해", mergeResultId = 5))
        add(UnitSpec(id = 5, name = "화염술사", grade = UnitGrade.MEDIUM, family = "화염",
            baseATK = 50f, atkSpeed = 1.0f, range = 160f,
            abilityType = AbilityType.SPLASH, description = "강화된 범위 피해", mergeResultId = 10))
        add(UnitSpec(id = 10, name = "화염마도사", grade = UnitGrade.HIGH, family = "화염",
            baseATK = 100f, atkSpeed = 1.1f, range = 170f,
            abilityType = AbilityType.SPLASH, description = "광역 화염 폭발", mergeResultId = 15))
        add(UnitSpec(id = 15, name = "불의 군주", grade = UnitGrade.SUPREME, family = "화염",
            baseATK = 200f, atkSpeed = 1.2f, range = 180f,
            abilityType = AbilityType.SPLASH, description = "대규모 화염 폭풍", mergeResultId = 20))
        add(UnitSpec(id = 20, name = "불사조", grade = UnitGrade.TRANSCENDENT, family = "화염",
            baseATK = 400f, atkSpeed = 1.3f, range = 200f,
            abilityType = AbilityType.SPLASH, description = "불사의 화염으로 전소"))

        // === 냉기 계열 (Slow) ===
        add(UnitSpec(id = 1, name = "냉기병", grade = UnitGrade.LOW, family = "냉기",
            baseATK = 20f, atkSpeed = 0.8f, range = 160f,
            abilityType = AbilityType.SLOW, description = "적 이동속도 감소", mergeResultId = 6))
        add(UnitSpec(id = 6, name = "냉기술사", grade = UnitGrade.MEDIUM, family = "냉기",
            baseATK = 40f, atkSpeed = 0.9f, range = 170f,
            abilityType = AbilityType.SLOW, description = "강화된 빙결 효과", mergeResultId = 11))
        add(UnitSpec(id = 11, name = "냉기마도사", grade = UnitGrade.HIGH, family = "냉기",
            baseATK = 80f, atkSpeed = 1.0f, range = 180f,
            abilityType = AbilityType.SLOW, description = "광역 빙결", mergeResultId = 16))
        add(UnitSpec(id = 16, name = "얼음의 군주", grade = UnitGrade.SUPREME, family = "냉기",
            baseATK = 160f, atkSpeed = 1.0f, range = 190f,
            abilityType = AbilityType.SLOW, description = "절대영도", mergeResultId = 21))
        add(UnitSpec(id = 21, name = "빙하제왕", grade = UnitGrade.TRANSCENDENT, family = "냉기",
            baseATK = 320f, atkSpeed = 1.1f, range = 200f,
            abilityType = AbilityType.SLOW, description = "시간까지 얼리는 극한의 냉기"))

        // === 독 계열 (DoT) ===
        add(UnitSpec(id = 2, name = "독침병", grade = UnitGrade.LOW, family = "독",
            baseATK = 15f, atkSpeed = 0.9f, range = 140f,
            abilityType = AbilityType.DOT, description = "지속 독 피해", mergeResultId = 7))
        add(UnitSpec(id = 7, name = "독술사", grade = UnitGrade.MEDIUM, family = "독",
            baseATK = 30f, atkSpeed = 1.0f, range = 150f,
            abilityType = AbilityType.DOT, description = "강화된 독 피해", mergeResultId = 12))
        add(UnitSpec(id = 12, name = "독마도사", grade = UnitGrade.HIGH, family = "독",
            baseATK = 60f, atkSpeed = 1.1f, range = 160f,
            abilityType = AbilityType.DOT, description = "맹독 피해", mergeResultId = 17))
        add(UnitSpec(id = 17, name = "역병의 군주", grade = UnitGrade.SUPREME, family = "독",
            baseATK = 120f, atkSpeed = 1.2f, range = 170f,
            abilityType = AbilityType.DOT, description = "역병 확산", mergeResultId = 22))
        add(UnitSpec(id = 22, name = "독룡", grade = UnitGrade.TRANSCENDENT, family = "독",
            baseATK = 240f, atkSpeed = 1.2f, range = 180f,
            abilityType = AbilityType.DOT, description = "만물을 부식시키는 맹독"))

        // === 번개 계열 (Chain) ===
        add(UnitSpec(id = 3, name = "전격병", grade = UnitGrade.LOW, family = "번개",
            baseATK = 22f, atkSpeed = 1.2f, range = 170f,
            abilityType = AbilityType.CHAIN, description = "2체 연쇄 공격", mergeResultId = 8))
        add(UnitSpec(id = 8, name = "전격술사", grade = UnitGrade.MEDIUM, family = "번개",
            baseATK = 45f, atkSpeed = 1.3f, range = 180f,
            abilityType = AbilityType.CHAIN, description = "3체 연쇄 공격", mergeResultId = 13))
        add(UnitSpec(id = 13, name = "전격마도사", grade = UnitGrade.HIGH, family = "번개",
            baseATK = 90f, atkSpeed = 1.4f, range = 190f,
            abilityType = AbilityType.CHAIN, description = "4체 연쇄 공격", mergeResultId = 18))
        add(UnitSpec(id = 18, name = "뇌전의 군주", grade = UnitGrade.SUPREME, family = "번개",
            baseATK = 180f, atkSpeed = 1.5f, range = 200f,
            abilityType = AbilityType.CHAIN, description = "5체 연쇄 공격", mergeResultId = 23))
        add(UnitSpec(id = 23, name = "폭풍신", grade = UnitGrade.TRANSCENDENT, family = "번개",
            baseATK = 350f, atkSpeed = 1.5f, range = 220f,
            abilityType = AbilityType.CHAIN, description = "모든 적에게 연쇄 번개"))

        // === 보조 계열 (Buff) ===
        add(UnitSpec(id = 4, name = "격려병", grade = UnitGrade.LOW, family = "보조",
            baseATK = 10f, atkSpeed = 0.5f, range = 100f,
            abilityType = AbilityType.BUFF, description = "아군 공격력 +10%", mergeResultId = 9))
        add(UnitSpec(id = 9, name = "지휘관", grade = UnitGrade.MEDIUM, family = "보조",
            baseATK = 20f, atkSpeed = 0.5f, range = 120f,
            abilityType = AbilityType.BUFF, description = "아군 공격력 +20%", mergeResultId = 14))
        add(UnitSpec(id = 14, name = "대사제", grade = UnitGrade.HIGH, family = "보조",
            baseATK = 40f, atkSpeed = 0.5f, range = 140f,
            abilityType = AbilityType.BUFF, description = "아군 공격력 +30%", mergeResultId = 19))
        add(UnitSpec(id = 19, name = "전쟁의 군주", grade = UnitGrade.SUPREME, family = "보조",
            baseATK = 80f, atkSpeed = 0.6f, range = 160f,
            abilityType = AbilityType.BUFF, description = "아군 공격력 +40%", mergeResultId = 24))
        add(UnitSpec(id = 24, name = "수호신", grade = UnitGrade.TRANSCENDENT, family = "보조",
            baseATK = 150f, atkSpeed = 0.6f, range = 180f,
            abilityType = AbilityType.BUFF, description = "아군 전체 공격력 +50%"))
    }

    val specsMap: Map<Int, UnitSpec> = specs.associateBy { it.id }

    /** 소환 가능한 유닛 (가챠로 뽑을 수 있는 것) */
    val summonableSpecs: List<UnitSpec> = specs.filter { it.grade.canSummon }

    /** 계열별 유닛 맵 */
    val familyMap: Map<String, List<UnitSpec>> = specs.groupBy { it.family }
}
