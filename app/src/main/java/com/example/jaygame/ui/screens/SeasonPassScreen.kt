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
import com.example.jaygame.ui.components.CurrencyHeader
import com.example.jaygame.ui.components.GameProgressBar
import com.example.jaygame.ui.components.MedievalButton
import com.example.jaygame.ui.components.MedievalCard
import com.example.jaygame.ui.components.ScreenHeader
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DiamondBlue
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.GoldCoin
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.PositiveGreen
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

    // Count how many tiers can be claimed
    val claimableTiers = SEASON_REWARDS.count { it.tier <= currentTier && it.tier > claimedTier }

    val listState = rememberLazyListState()

    // Scroll to current tier on first composition
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
        // Currency Header
        CurrencyHeader(gold = data.gold, diamonds = data.diamonds)

        Spacer(modifier = Modifier.height(8.dp))

        ScreenHeader(title = "\uC2DC\uC98C\uD328\uC2A4", onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        // XP Progress Section
        MedievalCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "\uC2DC\uC98C XP: $totalXP / $nextTierXP",
                                        fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = LightText,
                )

                Spacer(modifier = Modifier.height(8.dp))

                GameProgressBar(progress = tierProgress, height = 20.dp)

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "\uD604\uC7AC \uD2F0\uC5B4: $currentTier / 30",
                                        fontSize = 14.sp,
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
                MedievalButton(
                    text = "\uBAA8\uB450 \uC218\uB839 ($claimableTiers\uAC1C)",
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
                        val updatedData = currentData.copy(units = addRandomCardsToUnits(currentData.units, cardReward)).copy(
                            gold = currentData.gold + goldReward,
                            diamonds = currentData.diamonds + diamondReward,
                            seasonClaimedTier = ct,
                        )
                        repository.save(updatedData)
                        Toast.makeText(
                            context,
                            "\uBCF4\uC0C1 \uC218\uB839! \uAD08\uB4DC+$goldReward" +
                                if (diamondReward > 0) " \uB2E4\uC774\uC544+$diamondReward" else "" +
                                    if (cardReward > 0) " \uCE74\uB4DC+$cardReward" else "",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    accentColor = PositiveGreen,
                    fontSize = 14.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Section title
        Text(
            text = "\u2500\u2500 \uBCF4\uC0C1 \uD2B8\uB799 \u2500\u2500",
                        fontSize = 14.sp,
            color = LightText.copy(alpha = 0.6f),
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
                        val updatedData = currentData.copy(units = addRandomCardsToUnits(currentData.units, reward.cards)).copy(
                            gold = currentData.gold + reward.gold,
                            diamonds = currentData.diamonds + reward.diamonds,
                            seasonClaimedTier = reward.tier,
                        )
                        repository.save(updatedData)
                        Toast.makeText(
                            context,
                            "\uD2F0\uC5B4 ${reward.tier} \uBCF4\uC0C1 \uC218\uB839!",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
            }

            // Bottom spacing
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
        isCurrent -> Gold
        isClaimed -> PositiveGreen.copy(alpha = 0.6f)
        isClaimable -> Gold.copy(alpha = 0.8f)
        else -> SubText.copy(alpha = 0.4f)
    }

    val cardModifier = Modifier
        .width(100.dp)
        .then(
            if (isCurrent) {
                Modifier.border(
                    width = 2.dp,
                    color = Gold,
                    shape = RoundedCornerShape(10.dp),
                )
            } else {
                Modifier
            },
        )

    MedievalCard(
        modifier = cardModifier,
        borderColor = borderColor,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Tier number
            Text(
                text = "\uD2F0\uC5B4 ${reward.tier}",
                                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isCurrent) Gold else LightText,
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Gold reward
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gold),
                    contentDescription = null,
                    tint = GoldCoin,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "${reward.gold}",
                                        fontSize = 12.sp,
                    color = if (isLocked) LightText.copy(alpha = 0.5f) else LightText,
                )
            }

            // Diamond reward (if any)
            if (reward.diamonds > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_diamond),
                        contentDescription = null,
                        tint = DiamondBlue,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${reward.diamonds}",
                                                fontSize = 12.sp,
                        color = if (isLocked) LightText.copy(alpha = 0.5f) else LightText,
                    )
                }
            }

            // Card reward (if any)
            if (reward.cards > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "\uD83C\uDCCF",
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${reward.cards}",
                                                fontSize = 12.sp,
                        color = if (isLocked) LightText.copy(alpha = 0.5f) else LightText,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Status
            when {
                isClaimed -> {
                    Text(
                        text = "\u2713",
                        fontSize = 20.sp,
                        color = PositiveGreen,
                        fontWeight = FontWeight.Bold,
                    )
                }
                isClaimable -> {
                    MedievalButton(
                        text = "\uC218\uB839",
                        onClick = onClaim,
                        fontSize = 11.sp,
                        accentColor = PositiveGreen,
                        modifier = Modifier.height(30.dp),
                    )
                }
                else -> {
                    Text(
                        text = "\uD83D\uDD12",
                        fontSize = 18.sp,
                        color = SubText.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

