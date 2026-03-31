package com.jay.jaygame.engine

import com.jay.jaygame.engine.math.GameRect
import com.jay.jaygame.engine.math.Vec2

// ── Data models ──

enum class AbilityPrimitive {
    ON_HIT_CHANCE,   // % chance on attack to apply effect
    PERIODIC_AOE,    // every N seconds, AoE damage + debuff
    NTH_ATTACK,      // every Nth attack, trigger special
    AURA_BUFF,       // continuously buff allies
    ON_KILL,         // trigger on enemy kill
    SELF_BUFF_TIMER, // every N seconds, buff self
    PASSIVE_STAT,    // permanent stat modifier (crit, armor pen, etc.)
}

data class ActiveAbility(
    val triggerId: String,
    val trigger: AbilityTrigger,
    val primitive: AbilityPrimitive,
    val effects: List<AbilityEffect>,
    val cooldown: Float,
    val chance: Float,
    val range: Float,
    val nthAttack: Int,
    val isMagic: Boolean,
)

sealed class AbilityEffect {
    data class Damage(val atkPercent: Float, val isMagic: Boolean) : AbilityEffect()
    data class Slow(val strength: Float, val duration: Float) : AbilityEffect()
    data class Stun(val duration: Float) : AbilityEffect()
    data class DoT(val atkPercentPerTick: Float, val duration: Float) : AbilityEffect()
    data class ArmorBreak(val percent: Float, val duration: Float) : AbilityEffect()
    data class AtkBuff(val percent: Float, val duration: Float, val targetAll: Boolean) : AbilityEffect()
    data class SpdBuff(val percent: Float, val duration: Float, val targetAll: Boolean) : AbilityEffect()
    data class CoinBonus(val amount: Int) : AbilityEffect()
    data class SelfAtkBuff(val percent: Float, val duration: Float, val maxStacks: Int) : AbilityEffect()
    data class SelfSpdBuff(val percent: Float, val duration: Float) : AbilityEffect()
    data class CritBonus(val percent: Float) : AbilityEffect()
    data class Penetrate(val count: Int, val damagePercent: Float) : AbilityEffect()
    data class MagicResistBreak(val percent: Float, val duration: Float) : AbilityEffect()
    /** Execute: bonus damage when target HP below threshold */
    data class Execute(val hpThreshold: Float, val damageMultiplier: Float) : AbilityEffect()
    /** Drains % of dealt damage as bonus ATK for next attack */
    data class LifeDrain(val percent: Float) : AbilityEffect()
}

data class AbilityResult(
    val damageDealt: Float = 0f,
    val coinBonus: Int = 0,
    val triggered: Boolean = false,
)

// ── Engine ──

object AbilityEngine {

    /**
     * Parse an AbilityDef from a blueprint into a runtime ActiveAbility.
     * Uses the ability ID to determine which effect primitives to create.
     * Returns null if the ability cannot be parsed or is null.
     */
    fun parseAbility(abilityDef: AbilityDef?): ActiveAbility? {
        if (abilityDef == null) return null
        val id = abilityDef.id
        val v = abilityDef.value
        val cd = abilityDef.cooldown
        val r = abilityDef.range
        val isMagic = abilityDef.damageType == DamageType.MAGIC
        val trigger = abilityDef.type

        return when (id) {
            // ── HUMAN COMMON ──
            "bandit_steal" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.05f, r, isMagic,
                listOf(AbilityEffect.CoinBonus(v.toInt())))

            "militia_grit" -> makeAbility(id, trigger, AbilityPrimitive.SELF_BUFF_TIMER, 10f, 1f, r, isMagic,
                listOf(AbilityEffect.SelfAtkBuff(v / 100f, Float.MAX_VALUE, 3)))

            "archer_focus" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.SelfAtkBuff(v / 100f, 5f, 2)),
                nthAttack = 0) // triggers on same-target consecutive hits, handled specially

            "priest_bless" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, false)))

            "gladiator_fury" -> makeAbility(id, trigger, AbilityPrimitive.ON_KILL, cd, 1f, r, isMagic,
                listOf(AbilityEffect.SelfAtkBuff(v / 100f, 5f, 1)))

            // ── SPIRIT COMMON ──
            "water_sprite_slow" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.15f, r, isMagic,
                listOf(AbilityEffect.Slow(0.3f, 1.5f)))

            "flame_spirit_dot" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.20f, r, isMagic,
                listOf(AbilityEffect.DoT(0.10f, 3f)))

            "wind_spirit_haste" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.SpdBuff(v / 100f, 1f, false)))

            // ── ANIMAL COMMON ──
            "wolf_pack" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, false)))

            "eagle_eye" -> makeAbility(id, trigger, AbilityPrimitive.PASSIVE_STAT, cd, 1f, r, isMagic,
                listOf(AbilityEffect.CritBonus(v / 100f)))

            "bear_roar" -> makeAbility(id, trigger, AbilityPrimitive.NTH_ATTACK, cd, 1f, 80f, isMagic,
                listOf(AbilityEffect.Damage(0.50f, isMagic)), nthAttack = 5)

            // ── ROBOT COMMON ──
            "drone_scan" -> makeAbility(id, trigger, AbilityPrimitive.PASSIVE_STAT, cd, 1f, r, isMagic,
                listOf(AbilityEffect.ArmorBreak(v / 100f, 3f)))

            "turret_lock" -> makeAbility(id, trigger, AbilityPrimitive.NTH_ATTACK, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Stun(1f)), nthAttack = 3)

            // ── DEMON COMMON ──
            "imp_curse" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.10f, r, isMagic,
                listOf(AbilityEffect.MagicResistBreak(0.20f, 3f)))

            "yaksha_frenzy" -> makeAbility(id, trigger, AbilityPrimitive.ON_KILL, cd, 1f, r, isMagic,
                listOf(AbilityEffect.SelfSpdBuff(v / 100f, 3f)))

            // ── HUMAN RARE ──
            "knight_valor" -> makeAbility(id, trigger, AbilityPrimitive.NTH_ATTACK, cd, 1f, r, isMagic,
                listOf(AbilityEffect.SelfAtkBuff(v / 100f, 5f, 3)), nthAttack = 3)

            "crossbow_pierce" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Penetrate(1, 0.50f)))

            "mage_splash" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, isMagic)))

            "rogue_crit" -> makeAbility(id, trigger, AbilityPrimitive.PASSIVE_STAT, cd, 1f, r, isMagic,
                listOf(AbilityEffect.CritBonus(v / 100f)))

            // ── SPIRIT RARE ──
            "ice_spirit_freeze" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.08f, r, isMagic,
                listOf(AbilityEffect.Stun(1f)))

            "lightning_chain" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.30f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, isMagic)))

            // ── ANIMAL RARE ──
            "unicorn_heal" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.SpdBuff(v / 100f, 1f, false), AbilityEffect.CoinBonus(1)))

            "viper_venom" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.40f, r, isMagic,
                listOf(AbilityEffect.DoT(v / 100f, 4f)))

            // ── ROBOT RARE ──
            "sentry_burst" -> makeAbility(id, trigger, AbilityPrimitive.SELF_BUFF_TIMER, cd, 1f, r, isMagic,
                listOf(AbilityEffect.SelfSpdBuff(v / 100f, 3f)))

            "taser_shock" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.12f, r, isMagic,
                listOf(AbilityEffect.Stun(1.5f)))

            // ── DEMON RARE ──
            "succubus_drain" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.LifeDrain(v / 100f)))

            "hellhound_burn" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.DoT(v / 100f, 3f)))

            // ── HUMAN HERO ──
            "paladin_shield" -> makeAbility(id, trigger, AbilityPrimitive.PERIODIC_AOE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 5f, false)))

            "archmage_meteor" -> makeAbility(id, trigger, AbilityPrimitive.PERIODIC_AOE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, true), AbilityEffect.Slow(0.30f, 2f)))

            "assassin_execute" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Execute(0.30f, 2f), AbilityEffect.CoinBonus(1)))

            "commander_rally" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, true)))

            // ── SPIRIT HERO ──
            "ice_queen_blizzard" -> makeAbility(id, trigger, AbilityPrimitive.PERIODIC_AOE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, true), AbilityEffect.Slow(0.30f, 4f)))

            "thunder_god_bolt" -> makeAbility(id, trigger, AbilityPrimitive.NTH_ATTACK, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, true), AbilityEffect.Stun(0.5f)), nthAttack = 5)

            "earth_spirit_quake" -> makeAbility(id, trigger, AbilityPrimitive.PERIODIC_AOE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, true), AbilityEffect.Stun(1.5f)))

            // ── ANIMAL HERO ──
            "phoenix_rebirth" -> makeAbility(id, trigger, AbilityPrimitive.PERIODIC_AOE, cd, 1f, 9999f, isMagic,
                listOf(AbilityEffect.AtkBuff(0.20f, 5f, true)))

            "ninetails_charm" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.10f, r, isMagic,
                listOf(AbilityEffect.Slow(0.50f, 2f), AbilityEffect.ArmorBreak(0.30f, 2f)))

            // ── ROBOT HERO ──
            "heavybot_missile" -> makeAbility(id, trigger, AbilityPrimitive.PERIODIC_AOE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, false), AbilityEffect.Stun(1f)))

            "medicbot_boost" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(0.10f, 1f, false), AbilityEffect.SpdBuff(0.10f, 1f, false)))

            // ── DEMON HERO ──
            "dark_knight_lifesteal" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.LifeDrain(v / 100f)))

            // ── HUMAN LEGEND ──
            "hero_strike" -> makeAbility(id, trigger, AbilityPrimitive.NTH_ATTACK, cd, 1f, 80f, isMagic,
                listOf(AbilityEffect.Damage(3f, false), AbilityEffect.Damage(0.50f, false)), nthAttack = 7)

            "sage_wisdom" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, true)))

            "general_command" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(0.10f, 1f, true), AbilityEffect.SpdBuff(0.10f, 1f, true)))

            // ── SPIRIT LEGEND ──
            "storm_king_tempest" -> makeAbility(id, trigger, AbilityPrimitive.NTH_ATTACK, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, true), AbilityEffect.Slow(0.30f, 1f)), nthAttack = 3)

            "world_tree_aura" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, false), AbilityEffect.SpdBuff(0.08f, 1f, true)))

            // ── ANIMAL LEGEND ──
            "dragon_breath" -> makeAbility(id, trigger, AbilityPrimitive.PERIODIC_AOE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, true), AbilityEffect.DoT(0.10f, 3f)))

            // ── ROBOT LEGEND ──
            "megaton_barrage" -> makeAbility(id, trigger, AbilityPrimitive.PERIODIC_AOE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, false), AbilityEffect.Stun(1f)))

            "overlord_network" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, false), AbilityEffect.SpdBuff(0.15f, 1f, false)))

            // ── HUMAN MYTHIC ──
            "emperor_domain" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, true), AbilityEffect.CoinBonus(3)))

            "judge_verdict" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.20f, r, isMagic,
                listOf(AbilityEffect.ArmorBreak(1f, 3f), AbilityEffect.MagicResistBreak(1f, 3f)))

            "grand_sage_aura" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, true)))

            // ── SPIRIT MYTHIC ──
            "primal_spirit_chaos" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, true)))

            "frost_demon_aura" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Slow(v / 100f, 1f)))

            "illusionist_mirror" -> makeAbility(id, trigger, AbilityPrimitive.SELF_BUFF_TIMER, cd, 1f, r, isMagic,
                listOf(AbilityEffect.SelfAtkBuff(0.50f, 5f, 1)))

            // ── ANIMAL MYTHIC ──
            "divine_beast_blessing" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, true), AbilityEffect.SpdBuff(0.15f, 1f, false)))

            // ── ROBOT MYTHIC ──
            "omega_overclock" -> makeAbility(id, trigger, AbilityPrimitive.SELF_BUFF_TIMER, cd, 1f, r, isMagic,
                listOf(AbilityEffect.SelfAtkBuff(v / 100f, 5f, 1), AbilityEffect.SelfSpdBuff(v / 100f, 5f)))

            "guardian_x_shield" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.SpdBuff(0.10f, 1f, true)))

            // ── DEMON MYTHIC ──
            "demon_king_terror" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Slow(v / 100f, 1f), AbilityEffect.ArmorBreak(0.20f, 1f)))

            // ── HUMAN IMMORTAL ──
            "god_emperor_domain" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, true), AbilityEffect.SpdBuff(0.15f, 1f, true), AbilityEffect.CoinBonus(5)))

            // ── SPIRIT IMMORTAL ──
            "yggdrasil_life" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, true), AbilityEffect.SpdBuff(0.20f, 1f, false)))

            // ── ANIMAL IMMORTAL ──
            "four_beasts_domain" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, true), AbilityEffect.SpdBuff(0.20f, 1f, true)))

            // ── NEW SPIRIT ──
            "earth_power" -> makeAbility(id, trigger, AbilityPrimitive.NTH_ATTACK, cd, 1f, 80f, isMagic,
                listOf(AbilityEffect.Slow(0.20f, 2f)), nthAttack = 3)

            "flash" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.10f, r, isMagic,
                listOf(AbilityEffect.Slow(0.50f, 1f)))

            "root_bind" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.15f, r, isMagic,
                listOf(AbilityEffect.Stun(2f)))

            "combustion" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, 50f, isMagic,
                listOf(AbilityEffect.DoT(v / 100f, 3f)))

            "elemental_harmony" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.25f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, true), AbilityEffect.Slow(0.20f, 1.5f)))

            "dark_veil" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.MagicResistBreak(v / 100f, 1f)))

            // ── NEW ANIMAL ──
            "agility" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.10f, r, isMagic,
                listOf(AbilityEffect.SelfSpdBuff(v / 100f, 3f)))

            "hard_shell" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, false)))

            "venom_sting" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.30f, r, isMagic,
                listOf(AbilityEffect.DoT(v / 100f, 3f)))

            "dive_attack" -> makeAbility(id, trigger, AbilityPrimitive.NTH_ATTACK, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, false)), nthAttack = 5)

            "petrify_gaze" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.08f, r, isMagic,
                listOf(AbilityEffect.Stun(2f), AbilityEffect.ArmorBreak(v / 100f, 3f)))

            "sky_dive" -> makeAbility(id, trigger, AbilityPrimitive.PERIODIC_AOE, 8f, 1f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, false), AbilityEffect.Stun(1f)))

            "xuanwu_barrier" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.SpdBuff(v / 100f, 1f, true)))

            "fierce_tiger" -> makeAbility(id, trigger, AbilityPrimitive.NTH_ATTACK, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, false)), nthAttack = 4)

            "sacred_flame" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, true), AbilityEffect.SpdBuff(0.10f, 1f, false)))

            "justice_flame" -> makeAbility(id, trigger, AbilityPrimitive.NTH_ATTACK, cd, 1f, 100f, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, false), AbilityEffect.Stun(1f)), nthAttack = 3)

            // ── NEW ROBOT ──
            "repair_nano" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.SpdBuff(v / 100f, 1f, false)))

            "self_destruct" -> makeAbility(id, trigger, AbilityPrimitive.ON_KILL, cd, 1f, 60f, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, false)))

            "marking" -> makeAbility(id, trigger, AbilityPrimitive.PASSIVE_STAT, cd, 1f, r, isMagic,
                listOf(AbilityEffect.ArmorBreak(v / 100f, 3f)))

            "energy_shield" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(0.08f, 1f, false), AbilityEffect.SpdBuff(0.05f, 1f, false)))

            "pierce_laser" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Penetrate(2, v / 100f)))

            "plasma_cannon" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, 70f, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, true), AbilityEffect.Stun(1.5f)))

            "energy_charging" -> makeAbility(id, trigger, AbilityPrimitive.PERIODIC_AOE, 10f, 1f, 100f, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, false), AbilityEffect.Stun(1f)))

            "iron_wall" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, false), AbilityEffect.SpdBuff(0.10f, 1f, false)))

            "emp_field" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Slow(v / 100f, 1f)))

            "quantum_network" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, true), AbilityEffect.SpdBuff(0.25f, 1f, false)))

            // ── NEW DEMON ──
            "petrify" -> makeAbility(id, trigger, AbilityPrimitive.NTH_ATTACK, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Stun(1f)), nthAttack = 5)

            "curse_wave" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.15f, r, isMagic,
                listOf(AbilityEffect.ArmorBreak(v / 100f, 3f)))

            "poison_arrow" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.20f, r, isMagic,
                listOf(AbilityEffect.DoT(v / 100f, 3f)))

            "nightmare" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.15f, r, isMagic,
                listOf(AbilityEffect.Slow(0.40f, 2f), AbilityEffect.MagicResistBreak(0.20f, 2f)))

            "blood_magic" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.LifeDrain(v / 100f)))

            "necromancy" -> makeAbility(id, trigger, AbilityPrimitive.ON_KILL, cd, 1f, r, isMagic,
                listOf(AbilityEffect.SelfAtkBuff(v / 100f, 10f, 3)))

            "vampirism" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.LifeDrain(v / 100f)))

            "death_aura" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, false), AbilityEffect.ArmorBreak(0.15f, 1f)))

            "hellfire" -> makeAbility(id, trigger, AbilityPrimitive.NTH_ATTACK, cd, 1f, 100f, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, false)), nthAttack = 4)

            "death_fog" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.ArmorBreak(v / 100f, 1f), AbilityEffect.MagicResistBreak(v / 100f, 1f)))

            "corruption_whisper" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 0.20f, r, isMagic,
                listOf(AbilityEffect.ArmorBreak(1f, 3f), AbilityEffect.MagicResistBreak(1f, 3f)))

            "fallen_angel_light" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, 80f, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, true), AbilityEffect.MagicResistBreak(0.25f, 3f)))

            "chaos_domain" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, true), AbilityEffect.Slow(0.25f, 1f)))

            "destruction_breath" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Slow(v / 100f, 1f), AbilityEffect.ArmorBreak(0.20f, 1f)))

            // ── SPECIAL ──
            "dragon_king_dominion" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, false), AbilityEffect.DoT(0.01f, 1f)))

            "time_keeper_slow" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Slow(v / 100f, 1f)))

            "void_emperor_nullify" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.ArmorBreak(v / 100f, 1f)))

            "infernal_titan_burn" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.DoT(v / 100f, 1f)))

            "primordial_chaos_shift" -> makeAbility(id, trigger, AbilityPrimitive.ON_HIT_CHANCE, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Damage(v / 100f, true)))

            "shadow_monarch_stealth" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.AtkBuff(v / 100f, 1f, false)))

            "frost_emperor_permafrost" -> makeAbility(id, trigger, AbilityPrimitive.AURA_BUFF, cd, 1f, r, isMagic,
                listOf(AbilityEffect.Slow(v / 100f, 1f)))

            else -> {
                // Fallback: attempt generic parsing by trigger type
                parseGenericAbility(abilityDef)
            }
        }
    }

    /** Generic fallback parser for unregistered ability IDs */
    private fun parseGenericAbility(def: AbilityDef): ActiveAbility? {
        val isMagic = def.damageType == DamageType.MAGIC
        return when (def.type) {
            AbilityTrigger.AURA -> makeAbility(def.id, def.type, AbilityPrimitive.AURA_BUFF, def.cooldown, 1f, def.range, isMagic,
                listOf(AbilityEffect.AtkBuff(def.value / 100f, 1f, false)))
            AbilityTrigger.PASSIVE -> {
                if (def.cooldown > 0f) {
                    makeAbility(def.id, def.type, AbilityPrimitive.PERIODIC_AOE, def.cooldown, 1f, def.range.coerceAtLeast(80f), isMagic,
                        listOf(AbilityEffect.Damage(def.value / 100f, isMagic)))
                } else {
                    makeAbility(def.id, def.type, AbilityPrimitive.ON_HIT_CHANCE, 0f, 0.1f, def.range, isMagic,
                        listOf(AbilityEffect.Damage(def.value / 100f, isMagic)))
                }
            }
            AbilityTrigger.ACTIVE -> null // ACTIVE abilities are handled by UniqueAbilitySystem (mana/ultimate)
        }
    }

    private fun makeAbility(
        id: String, trigger: AbilityTrigger, primitive: AbilityPrimitive,
        cooldown: Float, chance: Float, range: Float, isMagic: Boolean,
        effects: List<AbilityEffect>, nthAttack: Int = 0,
    ) = ActiveAbility(
        triggerId = id, trigger = trigger, primitive = primitive,
        effects = effects, cooldown = cooldown, chance = chance,
        range = range, nthAttack = nthAttack, isMagic = isMagic,
    )

    // ──────────────────────────────────────────────
    //  Runtime application methods
    // ──────────────────────────────────────────────

    /**
     * Called when a unit's projectile hits an enemy.
     * Handles ON_HIT_CHANCE, NTH_ATTACK (counter increment), and PASSIVE_STAT effects.
     */
    fun applyOnHit(
        ability: ActiveAbility,
        unit: GameUnit,
        target: Enemy,
        spatialHash: SpatialHash<Enemy>,
    ): AbilityResult {
        when (ability.primitive) {
            AbilityPrimitive.ON_HIT_CHANCE -> {
                if (ability.chance < 1f && Math.random() >= ability.chance) {
                    return AbilityResult()
                }
                return applyEffectsToTarget(ability, unit, target, spatialHash)
            }
            AbilityPrimitive.NTH_ATTACK -> {
                unit.abilityCounter++
                if (unit.abilityCounter >= ability.nthAttack) {
                    unit.abilityCounter = 0
                    return applyEffectsToTarget(ability, unit, target, spatialHash)
                }
                return AbilityResult()
            }
            AbilityPrimitive.PASSIVE_STAT -> {
                // Passive stats are applied continuously; on-hit just refreshes debuffs
                return applyEffectsToTarget(ability, unit, target, spatialHash)
            }
            else -> return AbilityResult()
        }
    }

    /**
     * Called periodically for abilities with cooldown > 0 (PERIODIC_AOE, SELF_BUFF_TIMER).
     * The timer management is done in BattleEngine; this is called when timer fires.
     */
    fun applyPeriodic(
        ability: ActiveAbility,
        unit: GameUnit,
        enemies: List<Enemy>,
        spatialHash: SpatialHash<Enemy>,
        allUnits: ObjectPool<GameUnit>,
    ): AbilityResult {
        when (ability.primitive) {
            AbilityPrimitive.PERIODIC_AOE -> {
                // Damage + debuffs to enemies in range
                var totalDamage = 0f
                val atk = unit.effectiveATK()
                val range = ability.range
                val rect = GameRect(
                    unit.position.x - range, unit.position.y - range,
                    range * 2, range * 2,
                )
                val nearbyEnemies = spatialHash.query(rect)
                for (enemy in nearbyEnemies) {
                    if (!enemy.alive) continue
                    val dist = enemy.position.distanceTo(unit.position)
                    if (dist > range) continue
                    for (effect in ability.effects) {
                        when (effect) {
                            is AbilityEffect.Damage -> {
                                val dmg = atk * effect.atkPercent
                                enemy.takeDamage(dmg, effect.isMagic)
                                totalDamage += dmg
                            }
                            is AbilityEffect.Slow -> enemy.buffs.addBuff(BuffType.Slow, effect.strength, effect.duration, unit.tileIndex)
                            is AbilityEffect.Stun -> enemy.buffs.addBuff(BuffType.Stun, 1f, effect.duration, unit.tileIndex)
                            is AbilityEffect.DoT -> enemy.buffs.addBuff(BuffType.DoT, atk * effect.atkPercentPerTick, effect.duration, unit.tileIndex)
                            is AbilityEffect.ArmorBreak -> enemy.buffs.addBuff(BuffType.ArmorBreak, effect.percent, effect.duration, unit.tileIndex)
                            is AbilityEffect.MagicResistBreak -> enemy.buffs.addBuff(BuffType.ArmorBreak, effect.percent, effect.duration, unit.tileIndex) // MR break → ArmorBreak 대체 (동일 감산 효과)
                            // Buff effects in periodic AOE target allies, not enemies
                            is AbilityEffect.AtkBuff -> applyAllyBuff(unit, allUnits, effect.percent, effect.duration, isAtk = true, targetAll = effect.targetAll)
                            is AbilityEffect.SpdBuff -> applyAllyBuff(unit, allUnits, effect.percent, effect.duration, isAtk = false, targetAll = effect.targetAll)
                            else -> {}
                        }
                    }
                }
                return AbilityResult(damageDealt = totalDamage, triggered = true)
            }
            AbilityPrimitive.SELF_BUFF_TIMER -> {
                for (effect in ability.effects) {
                    when (effect) {
                        is AbilityEffect.SelfAtkBuff -> {
                            // 만료된 스택 보정: AtkUp 버프 수 기준으로 스택 재계산
                            if (effect.maxStacks > 0) {
                                unit.abilityStacks = unit.buffs.countBuff(BuffType.AtkUp)
                            }
                            if (effect.maxStacks <= 0 || unit.abilityStacks < effect.maxStacks) {
                                unit.buffs.addBuff(BuffType.AtkUp, effect.percent, effect.duration, unit.tileIndex)
                                if (effect.maxStacks > 0) unit.abilityStacks++
                            }
                        }
                        is AbilityEffect.SelfSpdBuff -> {
                            unit.buffs.addBuff(BuffType.SpdUp, effect.percent, effect.duration, unit.tileIndex)
                        }
                        else -> {}
                    }
                }
                return AbilityResult(triggered = true)
            }
            else -> return AbilityResult()
        }
    }

    /**
     * Called every frame for AURA abilities.
     * Buffs allies within range (or all allies if targetAll).
     * Also applies enemy debuff auras (like frost_demon_aura, demon_king_terror).
     */
    fun applyAura(
        ability: ActiveAbility,
        unit: GameUnit,
        allUnits: ObjectPool<GameUnit>,
        enemies: List<Enemy>,
        spatialHash: SpatialHash<Enemy>,
        dt: Float,
        auraTick: Float,
    ): AbilityResult {
        if (ability.primitive != AbilityPrimitive.AURA_BUFF) return AbilityResult()

        // Only apply aura buffs every ~0.5s to avoid per-frame overhead
        if (auraTick < 0.5f) return AbilityResult()

        var coinBonus = 0

        for (effect in ability.effects) {
            when (effect) {
                is AbilityEffect.AtkBuff -> applyAllyBuff(unit, allUnits, effect.percent, effect.duration, isAtk = true, targetAll = effect.targetAll)
                is AbilityEffect.SpdBuff -> applyAllyBuff(unit, allUnits, effect.percent, effect.duration, isAtk = false, targetAll = effect.targetAll)
                is AbilityEffect.CoinBonus -> coinBonus += effect.amount
                // Enemy debuff auras (e.g. frost_demon, demon_king)
                is AbilityEffect.Slow -> applyEnemyDebuffInRange(unit, spatialHash, ability.range) { enemy ->
                    enemy.buffs.addBuff(BuffType.Slow, effect.strength, effect.duration, unit.tileIndex)
                }
                is AbilityEffect.ArmorBreak -> applyEnemyDebuffInRange(unit, spatialHash, ability.range) { enemy ->
                    enemy.buffs.addBuff(BuffType.ArmorBreak, effect.percent, effect.duration, unit.tileIndex)
                }
                else -> {}
            }
        }
        return AbilityResult(coinBonus = coinBonus, triggered = true)
    }

    /**
     * Called when a unit kills an enemy.
     * Handles ON_KILL primitive effects.
     */
    fun applyOnKill(ability: ActiveAbility, unit: GameUnit): AbilityResult {
        if (ability.primitive != AbilityPrimitive.ON_KILL) return AbilityResult()
        var coinBonus = 0
        for (effect in ability.effects) {
            when (effect) {
                is AbilityEffect.SelfAtkBuff -> unit.buffs.addBuff(BuffType.AtkUp, effect.percent, effect.duration, unit.tileIndex)
                is AbilityEffect.SelfSpdBuff -> unit.buffs.addBuff(BuffType.SpdUp, effect.percent, effect.duration, unit.tileIndex)
                is AbilityEffect.CoinBonus -> coinBonus += effect.amount
                else -> {}
            }
        }
        return AbilityResult(coinBonus = coinBonus, triggered = true)
    }

    // ──────────────────────────────────────────────
    //  Private helpers
    // ──────────────────────────────────────────────

    private fun applyEffectsToTarget(
        ability: ActiveAbility,
        unit: GameUnit,
        target: Enemy,
        spatialHash: SpatialHash<Enemy>,
    ): AbilityResult {
        val atk = unit.effectiveATK()
        var totalDamage = 0f
        var coinBonus = 0

        for (effect in ability.effects) {
            when (effect) {
                is AbilityEffect.Damage -> {
                    if (ability.range > 0f) {
                        // AoE splash around target
                        val rect = GameRect(
                            target.position.x - ability.range, target.position.y - ability.range,
                            ability.range * 2, ability.range * 2,
                        )
                        for (nearby in spatialHash.query(rect)) {
                            if (!nearby.alive) continue
                            if (nearby.position.distanceTo(target.position) <= ability.range) {
                                val dmg = atk * effect.atkPercent
                                nearby.takeDamage(dmg, effect.isMagic)
                                totalDamage += dmg
                            }
                        }
                    } else {
                        // Single target extra damage
                        val dmg = atk * effect.atkPercent
                        target.takeDamage(dmg, effect.isMagic)
                        totalDamage += dmg
                    }
                }
                is AbilityEffect.Slow -> target.buffs.addBuff(BuffType.Slow, effect.strength, effect.duration, unit.tileIndex)
                is AbilityEffect.Stun -> target.buffs.addBuff(BuffType.Stun, 1f, effect.duration, unit.tileIndex)
                is AbilityEffect.DoT -> target.buffs.addBuff(BuffType.DoT, atk * effect.atkPercentPerTick, effect.duration, unit.tileIndex)
                is AbilityEffect.ArmorBreak -> target.buffs.addBuff(BuffType.ArmorBreak, effect.percent, effect.duration, unit.tileIndex)
                is AbilityEffect.MagicResistBreak -> target.buffs.addBuff(BuffType.ArmorBreak, effect.percent, effect.duration, unit.tileIndex)
                is AbilityEffect.CoinBonus -> coinBonus += effect.amount
                is AbilityEffect.SelfAtkBuff -> {
                    if (effect.maxStacks > 0) unit.abilityStacks = unit.buffs.countBuff(BuffType.AtkUp)
                    if (effect.maxStacks <= 0 || unit.abilityStacks < effect.maxStacks) {
                        unit.buffs.addBuff(BuffType.AtkUp, effect.percent, effect.duration, unit.tileIndex)
                        if (effect.maxStacks > 0) unit.abilityStacks++
                    }
                }
                is AbilityEffect.SelfSpdBuff -> unit.buffs.addBuff(BuffType.SpdUp, effect.percent, effect.duration, unit.tileIndex)
                is AbilityEffect.CritBonus -> { /* Applied via getCritBonus() query */ }
                is AbilityEffect.Penetrate -> {
                    // Hit additional enemies behind the target
                    val rect = GameRect(
                        target.position.x - 100f, target.position.y - 100f, 200f, 200f,
                    )
                    var pierced = 0
                    for (nearby in spatialHash.query(rect)) {
                        if (pierced >= effect.count) break
                        if (!nearby.alive || nearby === target) continue
                        val dmg = atk * effect.damagePercent
                        nearby.takeDamage(dmg, ability.isMagic)
                        totalDamage += dmg
                        pierced++
                    }
                }
                is AbilityEffect.Execute -> {
                    if (target.hpRatio < effect.hpThreshold) {
                        val dmg = atk * (effect.damageMultiplier - 1f) // bonus damage on top of normal
                        target.takeDamage(dmg, ability.isMagic)
                        totalDamage += dmg
                    }
                }
                is AbilityEffect.LifeDrain -> {
                    // Store drained value as a self ATK buff for next attack
                    val drainedAtk = atk * effect.percent
                    unit.buffs.addBuff(BuffType.AtkUp, drainedAtk / atk, 3f, unit.tileIndex)
                }
                is AbilityEffect.AtkBuff, is AbilityEffect.SpdBuff -> { /* ally buffs, not relevant on hit */ }
            }
        }
        return AbilityResult(damageDealt = totalDamage, coinBonus = coinBonus, triggered = true)
    }

    private fun applyAllyBuff(
        source: GameUnit,
        allUnits: ObjectPool<GameUnit>,
        percent: Float,
        duration: Float,
        isAtk: Boolean,
        targetAll: Boolean,
    ) {
        val range = source.range.coerceAtLeast(150f) // minimum effective aura range
        allUnits.forEach { other ->
            if (!other.alive || other === source) return@forEach
            if (!targetAll) {
                val dist = other.position.distanceTo(source.position)
                if (dist > range) return@forEach
            }
            if (isAtk) {
                other.buffs.addBuff(BuffType.AtkUp, percent, duration, source.tileIndex)
            } else {
                other.buffs.addBuff(BuffType.SpdUp, percent, duration, source.tileIndex)
            }
        }
    }

    private inline fun applyEnemyDebuffInRange(
        unit: GameUnit,
        spatialHash: SpatialHash<Enemy>,
        range: Float,
        action: (Enemy) -> Unit,
    ) {
        val r = range.coerceAtLeast(80f)
        val rect = GameRect(unit.position.x - r, unit.position.y - r, r * 2, r * 2)
        for (enemy in spatialHash.query(rect)) {
            if (!enemy.alive) continue
            if (enemy.position.distanceTo(unit.position) <= r) {
                action(enemy)
            }
        }
    }

    // ──────────────────────────────────────────────
    //  Query helpers
    // ──────────────────────────────────────────────

    /** Get extra crit chance from passive stat abilities (e.g., eagle_eye, rogue_crit). */
    fun getCritBonus(ability: ActiveAbility?): Float {
        if (ability == null || ability.primitive != AbilityPrimitive.PASSIVE_STAT) return 0f
        var bonus = 0f
        for (effect in ability.effects) {
            if (effect is AbilityEffect.CritBonus) bonus += effect.percent
        }
        return bonus
    }
}
