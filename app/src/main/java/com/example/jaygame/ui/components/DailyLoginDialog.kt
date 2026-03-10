package com.example.jaygame.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.jaygame.R
import com.example.jaygame.data.GameData
import com.example.jaygame.data.addRandomCardsToUnits
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DiamondBlue
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.GoldCoin
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.PositiveGreen
import com.example.jaygame.ui.theme.SubText
import java.time.LocalDate

data class DayReward(val gold: Int, val diamonds: Int, val cards: Int)

val DAILY_REWARDS = listOf(
    DayReward(100, 0, 1),
    DayReward(150, 0, 1),
    DayReward(200, 0, 2),
    DayReward(250, 1, 2),
    DayReward(300, 1, 3),
    DayReward(400, 2, 3),
    DayReward(1000, 5, 5),
)

fun canClaim(data: GameData): Boolean {
    val today = LocalDate.now().toString()
    return data.lastLoginDate != today || data.lastClaimedDay == 0
}

fun getClaimDayIndex(data: GameData): Int {
    val today = LocalDate.now().toString()
    val newStreak = if (data.lastLoginDate == today) data.loginStreak else data.loginStreak + 1
    return (newStreak - 1).mod(7)
}

fun claimReward(data: GameData): GameData {
    val today = LocalDate.now().toString()
    val newStreak = if (data.lastLoginDate == today) data.loginStreak else data.loginStreak + 1
    val dayIndex = (newStreak - 1).mod(7)
    val reward = DAILY_REWARDS[dayIndex]

    val updatedUnits = addRandomCardsToUnits(data.units, reward.cards)

    return data.copy(
        gold = data.gold + reward.gold,
        diamonds = data.diamonds + reward.diamonds,
        lastLoginDate = today,
        loginStreak = newStreak,
        lastClaimedDay = newStreak,
        units = updatedUnits,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyLoginDialog(
    data: GameData,
    onClaim: () -> Unit,
    onDismiss: () -> Unit,
) {
    val claimable = canClaim(data)
    val currentDayIndex = getClaimDayIndex(data)
    val currentReward = DAILY_REWARDS[currentDayIndex]

    val claimedInCycle = if (data.loginStreak == 0) 0 else {
        val cycleStart = ((data.loginStreak - 1) / 7) * 7
        data.loginStreak - cycleStart
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(animationSpec = tween(300)),
            exit = scaleOut(animationSpec = tween(200)),
        ) {
        GameCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = NeonCyan,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "일일 출석 보상",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Gold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 4,
                ) {
                    DAILY_REWARDS.forEachIndexed { index, reward ->
                        val dayNum = index + 1
                        val isClaimed = index < claimedInCycle && claimable.not()
                                || (index < claimedInCycle && claimable && index < currentDayIndex)
                        val isCurrentDay = index == currentDayIndex && claimable
                        val isBonus = index == 6

                        DayBox(
                            dayNum = dayNum,
                            reward = reward,
                            isClaimed = isClaimed,
                            isCurrentDay = isCurrentDay,
                            isBonus = isBonus,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "오늘의 보상:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = LightText,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gold),
                        contentDescription = null,
                        tint = GoldCoin,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${currentReward.gold} 골드",
                        fontSize = 13.sp,
                        color = LightText,
                    )
                    if (currentReward.diamonds > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "+", fontSize = 13.sp, color = LightText)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_diamond),
                            contentDescription = null,
                            tint = DiamondBlue,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${currentReward.diamonds} 다이아",
                            fontSize = 13.sp,
                            color = LightText,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "+", fontSize = 13.sp, color = LightText)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "\uD83C\uDCCF ${currentReward.cards} 카드",
                        fontSize = 13.sp,
                        color = LightText,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                NeonButton(
                    text = if (claimable) "수령하기" else "수령 완료",
                    onClick = {
                        if (claimable) onClaim()
                    },
                    enabled = claimable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    fontSize = 18.sp,
                    accentColor = NeonCyan,
                    accentColorDark = NeonCyan.copy(alpha = 0.5f),
                )
            }
        }
        }
    }
}

@Composable
private fun DayBox(
    dayNum: Int,
    reward: DayReward,
    isClaimed: Boolean,
    isCurrentDay: Boolean,
    isBonus: Boolean,
) {
    val borderColor = when {
        isCurrentDay -> NeonCyan
        isClaimed -> PositiveGreen
        else -> SubText.copy(alpha = 0.3f)
    }
    val bgColor = when {
        isCurrentDay -> NeonCyan.copy(alpha = 0.15f)
        isClaimed -> PositiveGreen.copy(alpha = 0.1f)
        else -> DeepDark.copy(alpha = 0.5f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isCurrentDay) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp),
            )
            .background(bgColor)
            .padding(6.dp),
    ) {
        Text(
            text = if (isBonus) "Day$dayNum\u2605" else "Day$dayNum",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (isBonus) Gold else LightText,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_gold),
                contentDescription = null,
                tint = GoldCoin,
                modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "${reward.gold}",
                fontSize = 10.sp,
                color = LightText,
            )
        }

        if (reward.diamonds > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_diamond),
                    contentDescription = null,
                    tint = DiamondBlue,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${reward.diamonds}",
                    fontSize = 10.sp,
                    color = LightText,
                )
            }
        }

        Text(
            text = "\uD83C\uDCCF${reward.cards}",
            fontSize = 10.sp,
            color = LightText,
        )

        if (isClaimed) {
            Text(
                text = "\u2713",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PositiveGreen,
            )
        }
    }
}
