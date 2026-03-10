package com.example.jaygame.engine

/**
 * 유닛 정의 (불변 데이터).
 * 게임 내 존재하는 모든 유닛 종류를 정의.
 */
data class UnitSpec(
    val id: Int,
    val name: String,
    val grade: UnitGrade,
    val family: String,       // 계열 (화염, 냉기 등) — 같은 계열끼리 머지
    val baseATK: Float,
    val atkSpeed: Float,      // 초당 공격 횟수
    val range: Float,         // 사거리 (논리 좌표)
    val abilityType: AbilityType = AbilityType.NONE,
    val description: String = "",
    val mergeResultId: Int = -1, // 3개 머지 시 결과 유닛 ID (-1 = 머지 불가)
)

enum class AbilityType {
    NONE,
    SPLASH,      // 범위 피해
    SLOW,        // 이동속도 감소
    DOT,         // 지속 피해
    CHAIN,       // 연쇄 공격
    BUFF,        // 아군 버프
    SHIELD,      // 보호막
    EXECUTE,     // 처형 (HP % 이하 즉사)
}

/**
 * 배틀 중 그리드 위 실제 유닛 인스턴스.
 */
data class BattleUnit(
    val instanceId: Int,
    val specId: Int,
    val grade: UnitGrade,
    val family: String,
    var gridRow: Int,
    var gridCol: Int,
    var level: Int = 1,
)
