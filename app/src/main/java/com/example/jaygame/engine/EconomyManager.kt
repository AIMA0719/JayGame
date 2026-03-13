package com.example.jaygame.engine

/**
 * 경제 시스템 관리자.
 *
 * 재화:
 * - 미네랄: 유닛 소환에 사용 (라운드 시작마다 지급, 몬스터 처치 시 소량 획득)
 * - 골드: 몬스터 처치 & 라운드 클리어 보너스 (게임 외부 보상)
 * - 가스: 특정 유닛 계열의 공격력 영구 강화에 사용
 */
interface EconomyManager {
    var minerals: Int
    var gold: Int
    var gas: Int

    /** 미네랄 충분한지 확인 */
    fun canAfford(cost: Int): Boolean

    /** 미네랄 소모 */
    fun spendMinerals(cost: Int): Boolean

    /** 몬스터 처치 보상 */
    fun onEnemyKilled(reward: Int)

    /** 라운드 클리어 보너스 */
    fun onWaveCleared(waveNumber: Int)

    /** 가스로 계열 영구 강화 — 비용 반환 (0이면 실패) */
    fun upgradeFamilyATK(family: String, gasCost: Int): Boolean

    /** 소환 비용 계산 (소환 횟수에 따라 증가) */
    fun getSummonCost(summonCount: Int): Int
}

class DefaultEconomyManager : EconomyManager {
    override var minerals: Int = 100
    override var gold: Int = 0
    override var gas: Int = 0

    /** 유물 웨이브 클리어 골드 보너스 (0.0~1.0) */
    var relicGoldWaveBonus: Float = 0f

    /** 유물 적 처치 골드 보너스 (0.0~1.0) */
    var relicGoldKillBonus: Float = 0f

    // 계열별 영구 공격력 보너스 (퍼센트)
    private val familyAtkBonus = mutableMapOf<String, Float>()

    override fun canAfford(cost: Int): Boolean = minerals >= cost

    override fun spendMinerals(cost: Int): Boolean {
        if (minerals < cost) return false
        minerals -= cost
        return true
    }

    override fun onEnemyKilled(reward: Int) {
        gold += (reward * (1f + relicGoldKillBonus)).toInt().coerceAtLeast(reward)
        minerals += 1 // 몬스터 처치 시 미네랄 소량 획득
    }

    override fun onWaveCleared(waveNumber: Int) {
        // 라운드 클리어 보너스: 기본 50 + 웨이브 * 10
        val baseBonus = 50 + waveNumber * 10
        val bonus = (baseBonus * (1f + relicGoldWaveBonus)).toInt()
        gold += bonus
        minerals += 20 // 라운드마다 미네랄 추가 지급
        gas += 5       // 라운드마다 가스 소량 지급
    }

    override fun upgradeFamilyATK(family: String, gasCost: Int): Boolean {
        if (gas < gasCost) return false
        gas -= gasCost
        val current = familyAtkBonus.getOrDefault(family, 0f)
        familyAtkBonus[family] = current + 0.1f // +10% per upgrade
        return true
    }

    override fun getSummonCost(summonCount: Int): Int {
        // 비용 = 10 + (소환 횟수 * 2), 최대 50
        return (10 + summonCount * 2).coerceAtMost(50)
    }

    fun getFamilyAtkBonus(family: String): Float =
        familyAtkBonus.getOrDefault(family, 0f)

    fun reset() {
        minerals = 100
        gold = 0
        gas = 0
    }
}
