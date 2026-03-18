package com.example.jaygame.engine

/**
 * 역할(Role) 시너지 — 같은 역할의 유닛을 2개 이상 배치하면 보너스.
 *
 * 2개: 기본 보너스
 * 3개: 중간 보너스
 * 4+개: 최대 보너스 + 특수 효과
 */
object RoleSynergySystem {
    data class RoleSynergyBonus(
        val atkMultiplier: Float = 1f,
        val rangeMultiplier: Float = 1f,
        val critBonus: Float = 0f,
        val blockTimeBonus: Float = 0f,
        val blockCountBonus: Int = 0,
        val dashDamageBonus: Float = 0f,
        val dashCooldownReduction: Float = 0f,
        val buffRangeBonus: Float = 0f,
        val ccChanceBonus: Float = 0f,
        val ccDurationBonus: Float = 0f,
        val specialEffect: RoleSpecialEffect = RoleSpecialEffect.NONE
    )

    enum class RoleSpecialEffect {
        NONE,
        TAUNT_ON_HIT,      // Tank 4+
        INSTANT_REDASH,     // Melee 4+
        PENETRATE_2,        // Ranged 4+
        BUFF_STACK,         // Support 3+
        GLOBAL_MINI_HEAL,   // Support 4+
        CC_HALF_ON_IMMUNE,  // Controller 4+
    }

    // Pre-allocated tier constants to avoid per-call data class allocation
    val NO_BONUS = RoleSynergyBonus()
    private val TANK_2 = RoleSynergyBonus(blockTimeBonus = 0.2f)
    private val TANK_3 = RoleSynergyBonus(blockTimeBonus = 0.2f, blockCountBonus = 1)
    private val TANK_4 = RoleSynergyBonus(blockTimeBonus = 0.2f, blockCountBonus = 1, specialEffect = RoleSpecialEffect.TAUNT_ON_HIT)
    private val MELEE_2 = RoleSynergyBonus(dashDamageBonus = 0.15f)
    private val MELEE_3 = RoleSynergyBonus(dashDamageBonus = 0.15f, dashCooldownReduction = 0.2f)
    private val MELEE_4 = RoleSynergyBonus(dashDamageBonus = 0.15f, dashCooldownReduction = 0.2f, specialEffect = RoleSpecialEffect.INSTANT_REDASH)
    private val RANGED_2 = RoleSynergyBonus(rangeMultiplier = 1.1f)
    private val RANGED_3 = RoleSynergyBonus(rangeMultiplier = 1.1f, critBonus = 0.05f)
    private val RANGED_4 = RoleSynergyBonus(rangeMultiplier = 1.1f, critBonus = 0.05f, specialEffect = RoleSpecialEffect.PENETRATE_2)
    private val SUPPORT_2 = RoleSynergyBonus(buffRangeBonus = 0.15f)
    private val SUPPORT_3 = RoleSynergyBonus(buffRangeBonus = 0.15f, specialEffect = RoleSpecialEffect.BUFF_STACK)
    private val SUPPORT_4 = RoleSynergyBonus(buffRangeBonus = 0.15f, specialEffect = RoleSpecialEffect.GLOBAL_MINI_HEAL)
    private val CONTROLLER_2 = RoleSynergyBonus(ccChanceBonus = 0.1f)
    private val CONTROLLER_3 = RoleSynergyBonus(ccChanceBonus = 0.1f, ccDurationBonus = 0.25f)
    private val CONTROLLER_4 = RoleSynergyBonus(ccChanceBonus = 0.1f, ccDurationBonus = 0.25f, specialEffect = RoleSpecialEffect.CC_HALF_ON_IMMUNE)

    /** Pre-counted version — avoids re-scanning the unit list per role. */
    fun getBonusByCount(role: UnitRole, count: Int): RoleSynergyBonus = when (role) {
        UnitRole.TANK -> when { count >= 4 -> TANK_4; count >= 3 -> TANK_3; count >= 2 -> TANK_2; else -> NO_BONUS }
        UnitRole.MELEE_DPS -> when { count >= 4 -> MELEE_4; count >= 3 -> MELEE_3; count >= 2 -> MELEE_2; else -> NO_BONUS }
        UnitRole.RANGED_DPS -> when { count >= 4 -> RANGED_4; count >= 3 -> RANGED_3; count >= 2 -> RANGED_2; else -> NO_BONUS }
        UnitRole.SUPPORT -> when { count >= 4 -> SUPPORT_4; count >= 3 -> SUPPORT_3; count >= 2 -> SUPPORT_2; else -> NO_BONUS }
        UnitRole.CONTROLLER -> when { count >= 4 -> CONTROLLER_4; count >= 3 -> CONTROLLER_3; count >= 2 -> CONTROLLER_2; else -> NO_BONUS }
    }

    fun getBonus(activeUnits: List<GameUnit>, role: UnitRole): RoleSynergyBonus {
        val count = activeUnits.count {
            it.alive && it.unitCategory != UnitCategory.SPECIAL && it.role == role
        }
        return when (role) {
            UnitRole.TANK -> when {
                count >= 4 -> TANK_4
                count >= 3 -> TANK_3
                count >= 2 -> TANK_2
                else -> NO_BONUS
            }
            UnitRole.MELEE_DPS -> when {
                count >= 4 -> MELEE_4
                count >= 3 -> MELEE_3
                count >= 2 -> MELEE_2
                else -> NO_BONUS
            }
            UnitRole.RANGED_DPS -> when {
                count >= 4 -> RANGED_4
                count >= 3 -> RANGED_3
                count >= 2 -> RANGED_2
                else -> NO_BONUS
            }
            UnitRole.SUPPORT -> when {
                count >= 4 -> SUPPORT_4
                count >= 3 -> SUPPORT_3
                count >= 2 -> SUPPORT_2
                else -> NO_BONUS
            }
            UnitRole.CONTROLLER -> when {
                count >= 4 -> CONTROLLER_4
                count >= 3 -> CONTROLLER_3
                count >= 2 -> CONTROLLER_2
                else -> NO_BONUS
            }
        }
    }
}
