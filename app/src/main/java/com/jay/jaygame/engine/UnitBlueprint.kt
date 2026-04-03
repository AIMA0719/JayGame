@file:Suppress("DEPRECATION")
package com.jay.jaygame.engine

import com.jay.jaygame.data.UnitFamily
import com.jay.jaygame.data.UnitRace

data class UnitBlueprint(
    val id: String,
    val name: String,
    val race: UnitRace,
    val grade: UnitGrade,
    val attackRange: AttackRange,
    val damageType: DamageType,
    val stats: UnitStats,
    val ability: AbilityDef?,
    val uniqueAbility: UniqueAbilityDef?,
    val mergeResultId: String?,
    val isSummonable: Boolean,
    val summonWeight: Int,
    val unitCategory: UnitCategory,
    val iconRes: Int,
    val description: String,
    // 하위 호환용 (deprecated)
    @Deprecated("Use race instead") val families: List<UnitFamily> = emptyList(),
    @Deprecated("Use attackRange for role classification") val role: UnitRole = UnitRole.MELEE_DPS,
    @Deprecated("Unused") val behaviorId: String = "",
)

/** 아군 유닛은 불사 타워 — HP/방어력 없음. 내부 range는 배치 전략용으로 유지. */
data class UnitStats(
    val baseATK: Float,
    val baseSpeed: Float,
    val range: Float,           // 내부 사거리 (도감에서는 숨김, 배치 전략용)
    // 하위 호환용 (deprecated) — 아군 불사 타워이므로 실사용 안 함
    @Deprecated("아군은 불사 타워") val hp: Float = 0f,
    @Deprecated("아군은 불사 타워") val defense: Float = 0f,
    @Deprecated("아군은 불사 타워") val magicResist: Float = 0f,
    @Deprecated("Unused") val moveSpeed: Float = 75f,
    @Deprecated("아군은 불사 타워") val blockCount: Int = 0,
)

enum class UnitCategory { NORMAL, HIDDEN, SPECIAL }

data class AbilityDef(
    val id: String,
    val name: String,
    val type: AbilityTrigger,
    val damageType: DamageType,
    val value: Float,
    val cooldown: Float,
    val range: Float,
    val description: String
)

enum class AbilityTrigger { PASSIVE, ACTIVE, AURA }

data class UniqueAbilityDef(
    val id: String,
    val name: String,
    val passive: AbilityDef?,
    val active: AbilityDef?,
    val requiredGrade: UnitGrade
)
