package com.example.jaygame.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.addRandomCardsToUnits
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.components.NeonProgressBar
import com.example.jaygame.ui.components.ResourceHeader
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DiamondBlue
import com.example.jaygame.ui.theme.Divider
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.GoldCoin
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.NeonGreen
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark
import com.example.jaygame.ui.theme.SubText

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
fun SeasonPassScreen(
    repository: GameRepository,
    onBack: () -> Unit,
) {
    val data by repository.gameData.collectAsState()
    val context = LocalContext.current

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDark),
    ) {
        // Resource Header
        ResourceHeader(gold = data.gold, diamonds = data.diamonds)

        Spacer(modifier = Modifier.height(4.dp))

        // Back button + title row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeonButton(
                text = "\u2190",
                onClick = onBack,
                modifier = Modifier.height(36.dp),
                fontSize = 14.sp,
                accentColor = NeonRed,
                accentColorDark = NeonRedDark,
            )
            Text(
                text = "시즌패스",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = NeonCyan,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(56.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                    height = 14.dp,
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

        Spacer(modifier = Modifier.height(10.dp))

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
                        val currentData = repository.gameData.value
                        val ct = currentData.seasonTier
                        val claimed = currentData.seasonClaimedTier
                        var goldReward = 0
                        var diamondReward = 0
                        var cardReward = 0
                        for (reward in SEASON_REWARDS) {
                            if (reward.tier in (claimed + 1)..ct) {
                                goldReward += reward.gold
                                diamondReward += reward.diamonds
                                cardReward += reward.cards
                            }
                        }
                        val updatedData = currentData.copy(
                            units = addRandomCardsToUnits(currentData.units, cardReward),
                        ).copy(
                            gold = currentData.gold + goldReward,
                            diamonds = currentData.diamonds + diamondReward,
                            seasonClaimedTier = ct,
                        )
                        repository.save(updatedData)
                        Toast.makeText(
                            context,
                            "보상 수령! 골드+$goldReward" +
                                if (diamondReward > 0) " 다이아+$diamondReward" else "" +
                                    if (cardReward > 0) " 카드+$cardReward" else "",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    accentColor = NeonGreen,
                    accentColorDark = NeonGreen.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
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
                        val currentData = repository.gameData.value
                        val updatedData = currentData.copy(
                            units = addRandomCardsToUnits(currentData.units, reward.cards),
                        ).copy(
                            gold = currentData.gold + reward.gold,
                            diamonds = currentData.diamonds + reward.diamonds,
                            seasonClaimedTier = reward.tier,
                        )
                        repository.save(updatedData)
                        Toast.makeText(
                            context,
                            "티어 ${reward.tier} 보상 수령!",
                            Toast.LENGTH_SHORT,
                        ).show()
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
        .then(
            if (isCurrent) {
                Modifier.border(
                    width = 2.dp,
                    color = NeonCyan,
                    shape = RoundedCornerShape(12.dp),
                )
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
            // Tier number
            Text(
                text = "티어 ${reward.tier}",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isCurrent) NeonCyan else LightText,
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Gold reward
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gold),
                    contentDescription = null,
                    tint = GoldCoin,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "${reward.gold}",
                    fontSize = 11.sp,
                    color = if (isLocked) SubText else LightText,
                )
            }

            // Diamond reward (if any)
            if (reward.diamonds > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_diamond),
                        contentDescription = null,
                        tint = DiamondBlue,
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

            // Card reward (if any)
            if (reward.cards > 0) {
                Text(
                    text = "카드 x${reward.cards}",
                    fontSize = 11.sp,
                    color = if (isLocked) SubText else Gold,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Status
            when {
                isClaimed -> {
                    Text(
                        text = "\u2713",
                        fontSize = 18.sp,
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
