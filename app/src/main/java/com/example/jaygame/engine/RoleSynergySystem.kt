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

    fun getBonus(activeUnits: List<GameUnit>, role: UnitRole): RoleSynergyBonus {
        val count = activeUnits.count {
            it.alive && it.unitCategory != UnitCategory.SPECIAL && it.role == role
        }
        return when (role) {
            UnitRole.TANK -> when {
                count >= 4 -> RoleSynergyBonus(blockTimeBonus = 0.2f, blockCountBonus = 1, specialEffect = RoleSpecialEffect.TAUNT_ON_HIT)
                count >= 3 -> RoleSynergyBonus(blockTimeBonus = 0.2f, blockCountBonus = 1)
                count >= 2 -> RoleSynergyBonus(blockTimeBonus = 0.2f)
                else -> RoleSynergyBonus()
            }
            UnitRole.MELEE_DPS -> when {
                count >= 4 -> RoleSynergyBonus(dashDamageBonus = 0.15f, dashCooldownReduction = 0.2f, specialEffect = RoleSpecialEffect.INSTANT_REDASH)
                count >= 3 -> RoleSynergyBonus(dashDamageBonus = 0.15f, dashCooldownReduction = 0.2f)
                count >= 2 -> RoleSynergyBonus(dashDamageBonus = 0.15f)
                else -> RoleSynergyBonus()
            }
            UnitRole.RANGED_DPS -> when {
                count >= 4 -> RoleSynergyBonus(rangeMultiplier = 1.1f, critBonus = 0.05f, specialEffect = RoleSpecialEffect.PENETRATE_2)
                count >= 3 -> RoleSynergyBonus(rangeMultiplier = 1.1f, critBonus = 0.05f)
                count >= 2 -> RoleSynergyBonus(rangeMultiplier = 1.1f)
                else -> RoleSynergyBonus()
            }
            UnitRole.SUPPORT -> when {
                count >= 4 -> RoleSynergyBonus(buffRangeBonus = 0.15f, specialEffect = RoleSpecialEffect.GLOBAL_MINI_HEAL)
                count >= 3 -> RoleSynergyBonus(buffRangeBonus = 0.15f, specialEffect = RoleSpecialEffect.BUFF_STACK)
                count >= 2 -> RoleSynergyBonus(buffRangeBonus = 0.15f)
                else -> RoleSynergyBonus()
            }
            UnitRole.CONTROLLER -> when {
                count >= 4 -> RoleSynergyBonus(ccChanceBonus = 0.1f, ccDurationBonus = 0.25f, specialEffect = RoleSpecialEffect.CC_HALF_ON_IMMUNE)
                count >= 3 -> RoleSynergyBonus(ccChanceBonus = 0.1f, ccDurationBonus = 0.25f)
                count >= 2 -> RoleSynergyBonus(ccChanceBonus = 0.1f)
                else -> RoleSynergyBonus()
            }
        }
    }
}
