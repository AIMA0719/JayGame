package com.jay.jaygame.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.jay.jaygame.data.GameData
import com.jay.jaygame.data.GameRepository
import com.jay.jaygame.data.addRandomCardsToUnits
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

data class ShopState(
    val gameData: GameData = GameData(),
    val selectedTab: Int = 0,
    val showProbabilityDialog: Boolean = false,
)

sealed class ShopSideEffect {
    data class ShowToast(val message: String) : ShopSideEffect()
}

class ShopViewModel(private val repository: GameRepository) : ViewModel(), ContainerHost<ShopState, ShopSideEffect> {
    override val container = container<ShopState, ShopSideEffect>(ShopState(gameData = repository.gameData.value))

    init {
        observeGameData()
    }

    private fun observeGameData() = intent {
        repository.gameData.collect { data ->
            reduce { state.copy(gameData = data) }
        }
    }

    fun selectTab(tab: Int) = intent {
        reduce { state.copy(selectedTab = tab) }
    }

    fun showProbabilityDialog() = intent {
        reduce { state.copy(showProbabilityDialog = true) }
    }

    fun dismissProbabilityDialog() = intent {
        reduce { state.copy(showProbabilityDialog = false) }
    }

    fun purchaseGoldPack(diamondCost: Int, goldAmount: Int) = intent {
        val d = state.gameData
        if (d.diamonds >= diamondCost) {
            repository.save(d.copy(diamonds = d.diamonds - diamondCost, gold = d.gold + goldAmount))
            postSideEffect(ShopSideEffect.ShowToast("구매 완료!"))
        } else {
            postSideEffect(ShopSideEffect.ShowToast("다이아가 부족합니다."))
        }
    }

    fun purchaseDiamondPack(goldCost: Int, diamondAmount: Int) = intent {
        val d = state.gameData
        if (d.gold >= goldCost) {
            repository.save(d.copy(gold = d.gold - goldCost, diamonds = d.diamonds + diamondAmount))
            postSideEffect(ShopSideEffect.ShowToast("구매 완료!"))
        } else {
            postSideEffect(ShopSideEffect.ShowToast("골드가 부족합니다."))
        }
    }

    fun purchaseRandomCards(count: Int, currencyIsGold: Boolean, cost: Int) = intent {
        val d = state.gameData
        val hasFunds = if (currencyIsGold) d.gold >= cost else d.diamonds >= cost
        if (hasFunds) {
            val updated = if (currencyIsGold) {
                d.copy(units = addRandomCardsToUnits(d.units, count), gold = d.gold - cost)
            } else {
                d.copy(units = addRandomCardsToUnits(d.units, count), diamonds = d.diamonds - cost)
            }
            repository.save(updated)
            postSideEffect(ShopSideEffect.ShowToast("구매 완료!"))
        } else {
            postSideEffect(ShopSideEffect.ShowToast("재화가 부족합니다."))
        }
    }

    fun purchaseStamina(diamondCost: Int, amount: Int) = intent {
        val d = state.gameData
        if (d.diamonds >= diamondCost) {
            repository.save(d.copy(
                diamonds = d.diamonds - diamondCost,
                stamina = (d.stamina + amount).coerceAtMost(d.maxStamina),
            ))
            postSideEffect(ShopSideEffect.ShowToast("구매 완료!"))
        } else {
            postSideEffect(ShopSideEffect.ShowToast("다이아가 부족합니다."))
        }
    }

    fun purchaseStarterPack(diamondCost: Int) = intent {
        val d = state.gameData
        if (d.starterPackPurchased) {
            postSideEffect(ShopSideEffect.ShowToast("이미 구매한 패키지입니다."))
            return@intent
        }
        if (d.diamonds >= diamondCost) {
            repository.save(d.copy(
                diamonds = d.diamonds - diamondCost,
                gold = d.gold + 5000,
                units = addRandomCardsToUnits(d.units, 10),
                starterPackPurchased = true,
            ))
            postSideEffect(ShopSideEffect.ShowToast("구매 완료!"))
        } else {
            postSideEffect(ShopSideEffect.ShowToast("다이아가 부족합니다."))
        }
    }

    fun claimSeasonTier(tier: Int, goldReward: Int, diamondReward: Int, cardReward: Int) = intent {
        val d = repository.gameData.value // 최신 데이터로 중복 수령 방지
        if (d.seasonTier < tier) {
            postSideEffect(ShopSideEffect.ShowToast("시즌 등급이 부족합니다"))
            return@intent
        }
        if (d.seasonClaimedTier + 1 != tier) {
            postSideEffect(ShopSideEffect.ShowToast("이전 단계를 먼저 수령하세요"))
            return@intent
        }
        if (d.seasonTier >= tier && d.seasonClaimedTier + 1 == tier) {
            var updated = d.copy(
                gold = d.gold + goldReward,
                diamonds = d.diamonds + diamondReward,
                seasonClaimedTier = tier,
            )
            if (cardReward > 0) {
                updated = updated.copy(units = addRandomCardsToUnits(updated.units, cardReward))
            }
            repository.save(updated)
            postSideEffect(ShopSideEffect.ShowToast("시즌 보상 수령!"))
        }
    }

    fun claimAllSeasonTiers(rewards: List<Triple<Int, Int, Int>>, toTier: Int) = intent {
        val d = repository.gameData.value // 최신 데이터로 중복 수령 방지
        if (d.seasonClaimedTier >= toTier) return@intent
        var totalGold = 0
        var totalDiamonds = 0
        var totalCards = 0
        for ((gold, diamonds, cards) in rewards) {
            totalGold += gold
            totalDiamonds += diamonds
            totalCards += cards
        }
        var updated = d.copy(
            gold = d.gold + totalGold,
            diamonds = d.diamonds + totalDiamonds,
            seasonClaimedTier = toTier,
        )
        if (totalCards > 0) {
            updated = updated.copy(units = addRandomCardsToUnits(updated.units, totalCards))
        }
        repository.save(updated)
        postSideEffect(ShopSideEffect.ShowToast("보상 수령! 골드+$totalGold" +
            (if (totalDiamonds > 0) " 다이아+$totalDiamonds" else "") +
            (if (totalCards > 0) " 카드+$totalCards" else "")))
    }
}
