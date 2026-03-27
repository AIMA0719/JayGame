package com.example.jaygame.data

import androidx.compose.ui.graphics.Color

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

val UPGRADE_COSTS = listOf(3 to 800, 6 to 3_000, 15 to 10_000, 30 to 30_000, 60 to 90_000, 120 to 250_000)
val LEVEL_MULTIPLIER = floatArrayOf(1.0f, 1.5f, 2.2f, 3.2f, 4.5f, 6.0f, 8.0f)
