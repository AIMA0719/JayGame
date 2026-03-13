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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
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
import com.example.jaygame.ui.theme.*
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
                borderColor = Gold,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Title + Close button
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "일일 출석 보상",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Gold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        Text(
                            text = "\u2715",
                            fontSize = 18.sp,
                            color = SubText,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clickable(onClick = onDismiss)
                                .padding(4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "연속 출석 ${data.loginStreak}일",
                        fontSize = 12.sp,
                        color = SubText,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Day grid: Row 1 (Days 1-4), Row 2 (Days 5-7)
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (i in 0 until 4) {
                            DayBox(
                                dayNum = i + 1,
                                reward = DAILY_REWARDS[i],
                                isClaimed = i < claimedInCycle && (!claimable || i < currentDayIndex),
                                isCurrentDay = i == currentDayIndex && claimable,
                                isBonus = false,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (i in 4 until 7) {
                            DayBox(
                                dayNum = i + 1,
                                reward = DAILY_REWARDS[i],
                                isClaimed = i < claimedInCycle && (!claimable || i < currentDayIndex),
                                isCurrentDay = i == currentDayIndex && claimable,
                                isBonus = i == 6,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Empty spacer to align with 4-column top row
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, BorderGlow, Color.Transparent),
                                ),
                            ),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Today's reward summary
                    Text(
                        text = "오늘의 보상",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = LightText,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Gold
                        RewardChip(
                            iconRes = R.drawable.ic_gold,
                            iconTint = GoldCoin,
                            text = "${currentReward.gold}",
                        )
                        if (currentReward.diamonds > 0) {
                            Spacer(modifier = Modifier.width(12.dp))
                            RewardChip(
                                iconRes = R.drawable.ic_diamond,
                                iconTint = DiamondBlue,
                                text = "${currentReward.diamonds}",
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        RewardChip(
                            iconRes = null,
                            iconTint = Color.Unspecified,
                            text = "\uD83C\uDCCF ${currentReward.cards}",
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Claim button
                    NeonButton(
                        text = if (claimable) "수령하기" else "수령 완료",
                        onClick = { if (claimable) onClaim() },
                        enabled = claimable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        fontSize = 15.sp,
                        accentColor = if (claimable) Gold else SubText,
                        accentColorDark = if (claimable) DarkGold else DimText,
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardChip(
    iconRes: Int?,
    iconTint: Color,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = LightText,
        )
    }
}

/**
 * Each day box has FIXED height and consistent layout.
 * All boxes show exactly: Day label → Gold → Diamond → Cards → Status
 */
@Composable
private fun DayBox(
    dayNum: Int,
    reward: DayReward,
    isClaimed: Boolean,
    isCurrentDay: Boolean,
    isBonus: Boolean,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        isCurrentDay -> Gold
        isClaimed -> PositiveGreen.copy(alpha = 0.6f)
        else -> BorderGlow
    }
    val bgColor = when {
        isCurrentDay -> Gold.copy(alpha = 0.1f)
        isClaimed -> PositiveGreen.copy(alpha = 0.06f)
        else -> DarkSurface.copy(alpha = 0.6f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .height(88.dp) // Fixed height for all boxes
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isCurrentDay) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .background(bgColor)
            .padding(vertical = 6.dp, horizontal = 4.dp),
    ) {
        // Day label (always present)
        Text(
            text = if (isBonus) "Day$dayNum★" else "Day$dayNum",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp,
            color = if (isBonus) Gold else if (isCurrentDay) Gold else LightText,
            textAlign = TextAlign.Center,
        )

        // Rewards column (always 3 lines, fixed)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            // Gold (always)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gold),
                    contentDescription = null,
                    tint = GoldCoin,
                    modifier = Modifier.size(10.dp),
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${reward.gold}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText,
                )
            }
            // Diamond (always show line - "—" if 0)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_diamond),
                    contentDescription = null,
                    tint = if (reward.diamonds > 0) DiamondBlue else DimText.copy(alpha = 0.3f),
                    modifier = Modifier.size(10.dp),
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = if (reward.diamonds > 0) "${reward.diamonds}" else "—",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (reward.diamonds > 0) LightText else DimText.copy(alpha = 0.4f),
                )
            }
            // Cards (always)
            Text(
                text = "\uD83C\uDCCF${reward.cards}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = LightText,
            )
        }

        // Status indicator (always present, same height)
        Text(
            text = when {
                isClaimed -> "✓"
                isCurrentDay -> "TODAY"
                else -> ""
            },
            fontSize = if (isClaimed) 13.sp else 8.sp,
            fontWeight = FontWeight.ExtraBold,
            color = when {
                isClaimed -> PositiveGreen
                isCurrentDay -> Gold
                else -> Color.Transparent
            },
            textAlign = TextAlign.Center,
        )
    }
}
