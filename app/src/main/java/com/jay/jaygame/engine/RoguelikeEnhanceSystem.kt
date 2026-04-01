package com.jay.jaygame.engine

import com.jay.jaygame.data.UnitRace
import kotlin.random.Random

enum class RoguelikeBuffGrade(val label: String, val colorHex: Long) {
    NORMAL("일반", 0xFF9E9E9E),
    RARE("희귀", 0xFF42A5F5),
    HERO("영웅", 0xFFAB47BC)
}

data class RoguelikeBuff(
    val id: String,
    val name: String,
    val description: String,
    val grade: RoguelikeBuffGrade,
    val minWave: Int,
    val stackable: Boolean,
    val weight: Int = 10,
    val requiredRaces: Set<UnitRace>? = null
)

data class ActiveRoguelikeBuff(
    val buff: RoguelikeBuff,
    var stacks: Int = 1
)

class RoguelikeEnhanceSystem {

    private val buffPool: List<RoguelikeBuff> = listOf(
        // ── 일반 (minWave=0) ──
        RoguelikeBuff("atk_boost", "공격력 강화", "전체 유닛 ATK +10%", RoguelikeBuffGrade.NORMAL, 0, true),
        RoguelikeBuff("spd_boost", "공격속도 강화", "전체 유닛 공속 +8%", RoguelikeBuffGrade.NORMAL, 0, true),
        RoguelikeBuff("range_boost", "사거리 확장", "전체 유닛 사거리 +10%", RoguelikeBuffGrade.NORMAL, 0, true),
        RoguelikeBuff("crit_boost", "치명타 확률", "크리율 +5%", RoguelikeBuffGrade.NORMAL, 0, true),
        RoguelikeBuff("coin_boost", "코인 보너스", "적 처치 코인 +30%", RoguelikeBuffGrade.NORMAL, 0, true),
        RoguelikeBuff("sell_bonus", "되팔이의 눈", "유닛 판매 가격 +75%", RoguelikeBuffGrade.NORMAL, 0, true),
        RoguelikeBuff("cc_duration", "속박 강화", "로그라이크 효과 CC 지속시간 +20%", RoguelikeBuffGrade.NORMAL, 0, true),
        RoguelikeBuff("dot_boost", "맹독 강화", "DoT 데미지 +25%", RoguelikeBuffGrade.NORMAL, 0, true,
            requiredRaces = setOf(UnitRace.SPIRIT, UnitRace.ANIMAL, UnitRace.DEMON)),
        // ── 희귀 (minWave=20) ──
        RoguelikeBuff("summon_discount", "소환 할인", "소환 비용 -15%", RoguelikeBuffGrade.RARE, 20, true),
        RoguelikeBuff("race_atk", "전투 고양", "전체 유닛 ATK +25%", RoguelikeBuffGrade.RARE, 20, true),
        RoguelikeBuff("splash_dmg", "폭발의 여파", "모든 공격에 범위 피해 20%", RoguelikeBuffGrade.RARE, 20, false),
        RoguelikeBuff("slow_on_hit", "동결 타격", "공격 시 15% 확률 슬로우", RoguelikeBuffGrade.RARE, 20, false),
        RoguelikeBuff("chain_lightning", "연쇄 번개", "공격 시 7% 확률 주변 2체 50% 데미지", RoguelikeBuffGrade.RARE, 20, false),
        RoguelikeBuff("vampiric", "생명력 착취", "적 처치 시 30% 확률 코인 2배", RoguelikeBuffGrade.RARE, 20, true),
        RoguelikeBuff("armor_shred", "방어구 파쇄", "공격 시 적 방어력 5% 영구 감소", RoguelikeBuffGrade.RARE, 20, false),
        RoguelikeBuff("summon_upgrade", "소환 축복", "소환 시 7% 확률 1등급 상위", RoguelikeBuffGrade.RARE, 20, false),
        // ── 영웅 (minWave=40) ──
        RoguelikeBuff("boss_slayer", "보스 킬러", "보스/엘리트 데미지 +40%", RoguelikeBuffGrade.HERO, 40, true),
        RoguelikeBuff("double_merge", "합성 행운", "Lucky merge 확률 +15%", RoguelikeBuffGrade.HERO, 40, true),
        RoguelikeBuff("mana_surge", "마나 폭주", "manaPerHit +50%", RoguelikeBuffGrade.HERO, 40, true),
        RoguelikeBuff("execute", "처형자", "적 HP 7% 이하 시 즉사 (보스 제외)", RoguelikeBuffGrade.HERO, 40, false),
        RoguelikeBuff("multishot", "다중 사격", "원거리 유닛 20% 확률 2회 공격", RoguelikeBuffGrade.HERO, 40, false),
        RoguelikeBuff("berserker", "광전사", "웨이브당 ATK +1% 누적", RoguelikeBuffGrade.HERO, 40, true),
    )

    /**
     * 현재 웨이브와 이미 활성화된 버프 목록을 기반으로 3개 선택지를 생성.
     * @param wave 1-indexed 클리어된 웨이브 번호 (10, 20, 30, 40, 50)
     * @param activeBuffs 현재 활성화된 버프 목록
     */
    fun generateChoices(wave: Int, activeBuffs: List<ActiveRoguelikeBuff>, selectedRaces: Set<UnitRace> = emptySet()): List<RoguelikeBuff> {
        val activeIds = activeBuffs.map { it.buff.id }.toSet()

        // minWave 필터 + non-stackable이고 이미 보유 중인 버프 제외 + requiredRaces 필터
        val candidates = buffPool.filter { buff ->
            buff.minWave <= wave &&
                (buff.stackable || buff.id !in activeIds) &&
                (buff.requiredRaces == null || buff.requiredRaces.any { it in selectedRaces })
        }

        if (candidates.isEmpty()) return emptyList()
        if (candidates.size <= 3) return candidates.shuffled()

        // 등급별 가중치: NORMAL=60, RARE=30, HERO=10
        val gradeWeights = mapOf(
            RoguelikeBuffGrade.NORMAL to 60,
            RoguelikeBuffGrade.RARE to 30,
            RoguelikeBuffGrade.HERO to 10,
        )

        val weighted = candidates.map { buff ->
            buff to (gradeWeights[buff.grade] ?: 10) * buff.weight
        }

        val selected = mutableListOf<RoguelikeBuff>()
        val remaining = weighted.toMutableList()

        repeat(3) {
            if (remaining.isEmpty()) return@repeat
            val totalWeight = remaining.sumOf { it.second }
            if (totalWeight <= 0) return@repeat
            var roll = Random.nextInt(totalWeight)
            for ((buff, w) in remaining) {
                roll -= w
                if (roll < 0) {
                    selected.add(buff)
                    remaining.removeAll { it.first.id == buff.id }
                    break
                }
            }
        }

        return selected
    }

    /**
     * 선택된 버프를 엔진에 적용. 각 버프 ID에 따라 엔진의 멀티플라이어를 수정.
     */
    fun applyBuff(buff: RoguelikeBuff, engine: BattleEngine) {
        when (buff.id) {
            "atk_boost" -> engine.roguelikeAtkMult += 0.10f
            "spd_boost" -> engine.roguelikeSpdMult += 0.08f
            "range_boost" -> engine.roguelikeRangeMult += 0.10f
            "crit_boost" -> engine.roguelikeCritBonus += 0.05f
            "coin_boost" -> engine.roguelikeCoinMult += 0.30f
            "sell_bonus" -> engine.roguelikeSellBonus += 0.75f
            "cc_duration" -> engine.roguelikeCCDuration += 0.20f
            "dot_boost" -> engine.roguelikeDotBoost += 0.25f
            "summon_discount" -> engine.roguelikeSummonDiscount = (engine.roguelikeSummonDiscount + 0.15f).coerceAtMost(0.75f)
            "race_atk" -> { /* 종족 ATK는 엔진 내부에서 별도 처리 가능 — 현재는 전체 ATK 보너스로 대체 */
                engine.roguelikeAtkMult += 0.25f
            }
            "splash_dmg" -> engine.roguelikeSplash = true
            "slow_on_hit" -> engine.roguelikeSlowOnHit = true
            "chain_lightning" -> engine.roguelikeChainLightning = true
            "vampiric" -> engine.roguelikeVampiricChance += 0.30f
            "armor_shred" -> engine.roguelikeArmorShred = true
            "summon_upgrade" -> engine.roguelikeSummonUpgrade = true
            "boss_slayer" -> engine.roguelikeBossBonus += 0.40f
            "double_merge" -> engine.roguelikeLuckyBonus += 0.15f
            "mana_surge" -> engine.roguelikeManaBonus += 0.50f
            "execute" -> engine.roguelikeExecute = true
            "multishot" -> engine.roguelikeMultishot = true
            "berserker" -> engine.roguelikeBerserkerBase += 1f
        }
    }
}
