package com.example.jaygame.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.ui.components.GachaProbabilityDialog
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.components.NeonProgressBar
import com.example.jaygame.ui.components.ResourceHeader
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DimText
import com.example.jaygame.ui.theme.Divider
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.DarkGold
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.NeonGreen
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark
import com.example.jaygame.ui.theme.SubText
import com.example.jaygame.ui.viewmodel.ShopViewModel
import com.example.jaygame.ui.viewmodel.ShopSideEffect
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

private enum class CurrencyType { GOLD, DIAMOND, FREE }

private sealed class ShopAction {
    data class GoldPack(val diamondCost: Int, val goldAmount: Int) : ShopAction()
    data class DiamondPack(val goldCost: Int, val diamondAmount: Int) : ShopAction()
    data class RandomCards(val count: Int, val currencyIsGold: Boolean, val cost: Int) : ShopAction()
    data class Stamina(val diamondCost: Int, val amount: Int) : ShopAction()
    data class StarterPack(val diamondCost: Int) : ShopAction()
}

private data class ShopItem(
    val name: String,
    val description: String,
    val currencyType: CurrencyType,
    val priceAmount: String,
    val action: ShopAction,
)

private data class TierReward(
    val tier: Int,
    val gold: Int,
    val diamonds: Int,
    val cards: Int,
)

private val SEASON_REWARDS = (1..30).map { tier ->
    TierReward(
        tier = tier,
        gold = tier * 50,
        diamonds = if (tier % 5 == 0) tier / 5 * 2 else 0,
        cards = if (tier % 3 == 0) tier / 3 else 0,
    )
}

@Composable
fun ShopScreen(viewModel: ShopViewModel) {
    val shopState by viewModel.collectAsState()
    val data = shopState.gameData
    val selectedTab = shopState.selectedTab
    val context = LocalContext.current

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is ShopSideEffect.ShowToast ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }

    if (shopState.showProbabilityDialog) {
        GachaProbabilityDialog(onDismiss = { viewModel.dismissProbabilityDialog() })
    }

    val tabNames = listOf("골드팩", "다이아팩", "스페셜", "시즌패스")

    val goldPackItems = remember {
        listOf(
            ShopItem("골드 100개", "소량의 골드를 획득합니다.", CurrencyType.DIAMOND, "10", ShopAction.GoldPack(10, 100)),
            ShopItem("골드 500개", "적당한 양의 골드를 획득합니다.", CurrencyType.DIAMOND, "40", ShopAction.GoldPack(40, 500)),
            ShopItem("골드 2,000개", "대량의 골드를 획득합니다.", CurrencyType.DIAMOND, "150", ShopAction.GoldPack(150, 2000)),
            ShopItem("골드 10,000개", "엄청난 양의 골드를 획득합니다.", CurrencyType.DIAMOND, "700", ShopAction.GoldPack(700, 10000)),
        )
    }

    val diamondPackItems = remember {
        listOf(
            ShopItem("다이아 50개", "소량의 다이아몬드를 획득합니다.", CurrencyType.GOLD, "5,000", ShopAction.DiamondPack(5000, 50)),
            ShopItem("다이아 200개", "적당한 양의 다이아몬드를 획득합니다.", CurrencyType.GOLD, "18,000", ShopAction.DiamondPack(18000, 200)),
            ShopItem("다이아 500개", "대량의 다이아몬드를 획득합니다.", CurrencyType.GOLD, "40,000", ShopAction.DiamondPack(40000, 500)),
        )
    }

    val specialItems = remember {
        listOf(
            ShopItem("랜덤 유닛 카드 5장", "랜덤한 보유 유닛의 카드 5장을 획득합니다.", CurrencyType.GOLD, "1,000", ShopAction.RandomCards(5, currencyIsGold = true, 1000)),
            ShopItem("랜덤 유닛 카드 20장", "랜덤한 보유 유닛의 카드 20장을 획득합니다.", CurrencyType.DIAMOND, "50", ShopAction.RandomCards(20, currencyIsGold = false, 50)),
            ShopItem("스태미나 충전", "스태미나를 50 회복합니다.", CurrencyType.DIAMOND, "30", ShopAction.Stamina(30, 50)),
            ShopItem("초보자 패키지", "골드 5,000 + 랜덤 카드 10장을 획득합니다.", CurrencyType.DIAMOND, "100", ShopAction.StarterPack(100)),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDark),
    ) {
        // Resource Header
        ResourceHeader(gold = data.gold, diamonds = data.diamonds)

        Spacer(modifier = Modifier.height(8.dp))

        // Title row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(modifier = Modifier.width(60.dp))
            Text(
                text = "상점",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = NeonRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            NeonButton(
                text = "확률 보기",
                onClick = { viewModel.showProbabilityDialog() },
                fontSize = 11.sp,
                accentColor = SubText,
                accentColorDark = DimText,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Row with sliding indicator
        val tabCount = tabNames.size
        val indicatorOffset by androidx.compose.animation.core.animateFloatAsState(
            targetValue = selectedTab.toFloat(),
            animationSpec = androidx.compose.animation.core.tween(250),
            label = "tabIndicator",
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tabNames.forEachIndexed { index, name ->
                    NeonButton(
                        text = name,
                        onClick = { viewModel.selectTab(index) },
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        accentColor = if (selectedTab == index) NeonRed else DimText,
                        accentColorDark = if (selectedTab == index) NeonRedDark else DimText.copy(alpha = 0.6f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Sliding indicator line
            Box(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val totalSpacing = 6f * (tabCount - 1) * density
                    val tabW = (size.width - totalSpacing) / tabCount
                    val xOff = indicatorOffset * (tabW + 6f * density)
                    drawRoundRect(
                        color = NeonRed,
                        topLeft = androidx.compose.ui.geometry.Offset(xOff, 0f),
                        size = androidx.compose.ui.geometry.Size(tabW, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        if (selectedTab == 3) {
            // Season Pass tab
            SeasonPassContent(data = data, viewModel = viewModel)
        } else {
            val currentItems = when (selectedTab) {
                0 -> goldPackItems
                1 -> diamondPackItems
                2 -> specialItems
                else -> goldPackItems
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(currentItems) { item ->
                    ShopItemCard(
                        item = item,
                        onBuy = {
                            when (val a = item.action) {
                                is ShopAction.GoldPack -> viewModel.purchaseGoldPack(a.diamondCost, a.goldAmount)
                                is ShopAction.DiamondPack -> viewModel.purchaseDiamondPack(a.goldCost, a.diamondAmount)
                                is ShopAction.RandomCards -> viewModel.purchaseRandomCards(a.count, a.currencyIsGold, a.cost)
                                is ShopAction.Stamina -> viewModel.purchaseStamina(a.diamondCost, a.amount)
                                is ShopAction.StarterPack -> viewModel.purchaseStarterPack(a.diamondCost)
                            }
                        },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SeasonPassContent(data: com.example.jaygame.data.GameData, viewModel: ShopViewModel) {

    val currentTier = data.seasonTier
    val tierProgress = data.seasonTierProgress
    val claimedTier = data.seasonClaimedTier
    val totalXP = data.seasonXP
    val nextTierXP = (currentTier + 1) * 100

    val claimableTiers = SEASON_REWARDS.count { it.tier <= currentTier && it.tier > claimedTier }

    val listState = rememberLazyListState()

    LaunchedEffect(currentTier) {
        if (currentTier > 0) {
            listState.animateScrollToItem((currentTier - 1).coerceIn(0, 29))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // XP Progress Section
        GameCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "시즌 XP: $totalXP / $nextTierXP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = LightText,
                )

                Spacer(modifier = Modifier.height(8.dp))

                NeonProgressBar(
                    progress = tierProgress,
                    height = 10.dp,
                    barColor = NeonCyan,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "현재 티어: $currentTier / 30",
                    fontSize = 13.sp,
                    color = Gold,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Claim All button
        if (claimableTiers > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                NeonButton(
                    text = "모두 수령 (${claimableTiers}개)",
                    onClick = {
                        val ct = data.seasonTier
                        val claimed = data.seasonClaimedTier
                        val claimable = SEASON_REWARDS.filter { it.tier in (claimed + 1)..ct }
                        if (claimable.isNotEmpty()) {
                            viewModel.claimAllSeasonTiers(
                                rewards = claimable.map { Triple(it.gold, it.diamonds, it.cards) },
                                toTier = ct,
                            )
                        }
                    },
                    accentColor = NeonGreen,
                    accentColorDark = NeonGreen.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Section title
        Text(
            text = "\u2500\u2500 보상 트랙 \u2500\u2500",
            fontSize = 13.sp,
            color = SubText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal tier track
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SEASON_REWARDS) { reward ->
                val isCurrent = reward.tier == currentTier
                val isClaimed = reward.tier <= claimedTier
                val isClaimable = reward.tier <= currentTier && reward.tier > claimedTier
                val isLocked = reward.tier > currentTier

                TierCard(
                    reward = reward,
                    isCurrent = isCurrent,
                    isClaimed = isClaimed,
                    isClaimable = isClaimable,
                    isLocked = isLocked,
                    onClaim = {
                        viewModel.claimSeasonTier(reward.tier, reward.gold, reward.diamonds, reward.cards)
                    },
                )
            }

            item {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun ShopItemCard(
    item: ShopItem,
    onBuy: () -> Unit,
) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = LightText,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    color = SubText,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val btnAccent = when (item.currencyType) {
                CurrencyType.DIAMOND -> NeonCyan
                CurrencyType.GOLD -> Gold
                CurrencyType.FREE -> SubText
            }
            val btnAccentDark = when (item.currencyType) {
                CurrencyType.DIAMOND -> NeonCyan.copy(alpha = 0.7f)
                CurrencyType.GOLD -> DarkGold
                CurrencyType.FREE -> SubText.copy(alpha = 0.7f)
            }

            if (item.currencyType == CurrencyType.FREE) {
                NeonButton(
                    text = item.priceAmount,
                    onClick = onBuy,
                    fontSize = 11.sp,
                    enabled = true,
                    accentColor = btnAccent,
                    accentColorDark = btnAccentDark,
                )
            } else {
                NeonButton(
                    onClick = onBuy,
                    enabled = true,
                    accentColor = btnAccent,
                    accentColorDark = btnAccentDark,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(
                                id = if (item.currencyType == CurrencyType.DIAMOND)
                                    R.drawable.ic_diamond else R.drawable.ic_gold,
                            ),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.priceAmount,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = LightText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TierCard(
    reward: TierReward,
    isCurrent: Boolean,
    isClaimed: Boolean,
    isClaimable: Boolean,
    isLocked: Boolean,
    onClaim: () -> Unit,
) {
    val borderColor = when {
        isCurrent -> NeonCyan
        isClaimed -> NeonGreen.copy(alpha = 0.6f)
        isClaimable -> Gold.copy(alpha = 0.8f)
        else -> Divider
    }

    val cardModifier = Modifier
        .width(100.dp)
        .height(150.dp)
        .then(
            if (isCurrent) {
                Modifier.border(2.dp, NeonCyan, RoundedCornerShape(12.dp))
            } else {
                Modifier
            },
        )

    GameCard(
        modifier = cardModifier,
        borderColor = borderColor,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = "티어 ${reward.tier}",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isCurrent) NeonCyan else LightText,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gold),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "${reward.gold}",
                    fontSize = 11.sp,
                    color = if (isLocked) SubText else LightText,
                )
            }

            if (reward.diamonds > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_diamond),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${reward.diamonds}",
                        fontSize = 11.sp,
                        color = if (isLocked) SubText else LightText,
                    )
                }
            }

            if (reward.cards > 0) {
                Text(
                    text = "카드 x${reward.cards}",
                    fontSize = 11.sp,
                    color = if (isLocked) SubText else Gold,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            when {
                isClaimed -> {
                    Text(
                        text = "\u2713",
                        fontSize = 14.sp,
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                    )
                }
                isClaimable -> {
                    NeonButton(
                        text = "수령",
                        onClick = onClaim,
                        fontSize = 10.sp,
                        accentColor = NeonGreen,
                        accentColorDark = NeonGreen.copy(alpha = 0.6f),
                        modifier = Modifier.height(28.dp),
                    )
                }
                else -> {
                    Text(
                        text = "\uD83D\uDD12",
                        fontSize = 16.sp,
                        color = SubText.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}
