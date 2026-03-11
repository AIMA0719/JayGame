package com.example.jaygame.engine

/**
 * 유닛 정의 (불변 데이터).
 * 게임 내 존재하는 모든 유닛 종류를 정의.
 */
data class UnitSpec(
    val id: Int,
    val name: String,
    val grade: UnitGrade,
    val family: String,
    val baseATK: Float,
    val atkSpeed: Float,
    val range: Float,
    val abilityType: AbilityType = AbilityType.NONE,
    val description: String = "",
    val mergeResultId: Int = -1,
)

enum class AbilityType {
    NONE,
    SPLASH,
    SLOW,
    DOT,
    CHAIN,
    BUFF,
    SHIELD,
    EXECUTE,
    // 고유 능력 타입 (영웅 등급 이상)
    LINGERING_FIRE,
    FIRESTORM,
    VOLCANIC_ERUPTION,
    PHOENIX_REBIRTH,
    SUPERNOVA,
    FROST_NOVA,
    ABSOLUTE_ZERO,
    ICE_AGE,
    ETERNAL_WINTER,
    TIME_STOP,
    CONTAGION,
    CORROSION,
    TOXIC_DOMAIN,
    VENOMOUS_BREATH,
    UNIVERSAL_DECAY,
    OVERCHARGE,
    STATIC_FIELD,
    THUNDERSTORM,
    MJOLNIR,
    DIVINE_PUNISHMENT,
    FORESIGHT,
    WAR_SONG,
    RESURRECTION,
    UNIVERSAL_HARMONY,
    GENESIS,
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
