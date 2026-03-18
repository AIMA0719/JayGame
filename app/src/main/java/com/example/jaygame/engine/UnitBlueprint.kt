package com.example.jaygame.engine

import com.example.jaygame.data.UnitFamily

data class UnitBlueprint(
    val id: String,
    val name: String,
    val families: List<UnitFamily>,
    val grade: UnitGrade,
    val role: UnitRole,
    val attackRange: AttackRange,
    val damageType: DamageType,
    val stats: UnitStats,
    val behaviorId: String,
    val ability: AbilityDef?,
    val uniqueAbility: UniqueAbilityDef?,
    val mergeResultId: String?,
    val isSummonable: Boolean,
    val summonWeight: Int,
    val unitCategory: UnitCategory,
    val iconRes: Int,
    val description: String
)

data class UnitStats(
    val hp: Float,
    val baseATK: Float,
    val baseSpeed: Float,
    val range: Float,
    val defense: Float,
    val magicResist: Float,
    val moveSpeed: Float,
    val blockCount: Int
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
