package com.jay.jaygame.engine

import com.jay.jaygame.data.UnitRace

// ── 데이터 모델 ──────────────────────────────────────────────

enum class AuctionPhase {
    INTRO,        // 매물 소개 (1.5초)
    BIDDING,      // 플레이어 입찰 대기
    NPC_TURN,     // NPC 자동 입찰 (0.5초)
    GOING_ONCE,   // 카운트다운 (3초, 아무도 입찰 안 할 때)
    SOLD,         // 낙찰
    UNSOLD,       // 유찰
}

data class NpcBidder(
    val name: String,
    val maxBid: Int,
    val aggression: Float,  // 0.4~0.9
    var lastBid: Int = 0,
    var retired: Boolean = false,
)

data class AuctionState(
    val blueprintId: String,       // UnitBlueprint.id
    val blueprintName: String,     // 표시용
    val blueprintGrade: Int,       // 등급 ordinal
    val blueprintAtk: Float,
    val blueprintSpd: Float,
    val currentBid: Int,
    val currentBidder: String,     // "" = 없음, "PLAYER", NPC 이름
    val npcs: List<NpcBidder>,
    val timer: Float,              // 현재 phase의 남은 시간
    val phase: AuctionPhase,
    val startingPrice: Int,
    val bidIncrement: Int,
    val playerCanBid: Boolean,
    val playerRetired: Boolean,
)

// ── 경매 시스템 ──────────────────────────────────────────────

class AuctionSystem(private val blueprintRegistry: BlueprintRegistry) {

    companion object {
        const val BID_INCREMENT = 20
        const val ROUND_TIME = 3.0f
        const val INTRO_TIME = 1.5f
        const val NPC_DELAY = 0.5f
        const val BIDDER_PLAYER = "PLAYER"
        const val BIDDER_NONE = ""
        private val NPC_NAMES = listOf("상인 마르코", "수집가 엘리스", "귀족 발렌틴", "떠돌이 루카")
    }

    private val rng = java.util.Random()

    // ── 등급 결정 ────────────────────────────────────────────

    /**
     * 경매에 올라올 유닛의 등급을 웨이브에 따라 결정한다.
     * - wave 10~20 → HERO
     * - wave 30+  → 30% 확률 LEGEND, 나머지 HERO
     * - wave 50   → LEGEND 확정
     */
    fun determineGrade(wave: Int): UnitGrade {
        return when {
            wave >= 50 -> UnitGrade.LEGEND
            wave >= 30 -> if (rng.nextFloat() < 0.3f) UnitGrade.LEGEND else UnitGrade.HERO
            else -> UnitGrade.HERO // wave 10~20
        }
    }

    // ── 시작가 ───────────────────────────────────────────────

    /** 시작가: 30 + (wave/10 - 1) * 20 */
    fun startingPrice(wave: Int): Int {
        return 30 + ((wave / 10) - 1) * 20
    }

    // ── 블루프린트 선택 ──────────────────────────────────────

    /**
     * 경매에 올릴 유닛 블루프린트를 선택한다.
     * 선택된 종족 + 등급으로 1차 조회. 빈 결과면 등급만으로 2차 조회.
     * 결과 중 랜덤 1개 반환. 전부 비어 있으면 null.
     */
    fun pickBlueprint(wave: Int, selectedRaces: Set<UnitRace>): UnitBlueprint? {
        val grade = determineGrade(wave)

        // 1차: 종족 + 등급 + 소환 가능
        val candidates = blueprintRegistry.findByRacesAndGradeAndSummonable(selectedRaces, grade)
        if (candidates.isNotEmpty()) {
            return candidates[rng.nextInt(candidates.size)]
        }

        // 2차: 등급만으로 조회 (종족 무시)
        val fallback = blueprintRegistry.findByGradeAndSummonable(grade)
        if (fallback.isNotEmpty()) {
            return fallback[rng.nextInt(fallback.size)]
        }

        return null
    }

    // ── NPC 생성 ─────────────────────────────────────────────

    /**
     * 경매 NPC 입찰자 생성.
     * - wave < 40 → 2명
     * - wave >= 40 → 3명
     * 예산: startPrice * (1.5~3.0), 공격성: 0.4~0.9
     */
    fun generateNpcs(wave: Int, startPrice: Int): List<NpcBidder> {
        val count = if (wave < 40) 2 else 3
        val names = NPC_NAMES.shuffled(rng).take(count)

        return names.map { name ->
            val budgetMultiplier = 1.5f + rng.nextFloat() * 1.5f  // 1.5 ~ 3.0
            val maxBid = (startPrice * budgetMultiplier).toInt()
            val aggression = 0.4f + rng.nextFloat() * 0.5f        // 0.4 ~ 0.9
            NpcBidder(
                name = name,
                maxBid = maxBid,
                aggression = aggression,
            )
        }
    }

    // ── NPC 입찰 판단 ────────────────────────────────────────

    /**
     * NPC가 nextBid에 입찰할지 결정한다.
     * - retired이거나 nextBid > maxBid이면 false
     * - 예산 대비 현재가 비율이 높을수록 공격성 감소
     * - rng.nextFloat() < adjustedAggression 이면 입찰
     */
    fun npcDecideBid(npc: NpcBidder, nextBid: Int): Boolean {
        if (npc.retired) return false
        if (nextBid > npc.maxBid) {
            npc.retired = true
            return false
        }

        // 예산 대비 현재가 비율 (0.0 ~ 1.0+)
        val ratio = nextBid.toFloat() / npc.maxBid.toFloat()
        // 비율이 높을수록 공격성 감소: aggression * (1 - ratio * 0.5)
        val adjustedAggression = npc.aggression * (1f - ratio * 0.5f)

        return rng.nextFloat() < adjustedAggression
    }
}
