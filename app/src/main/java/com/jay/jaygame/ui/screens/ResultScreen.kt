package com.jay.jaygame.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.jaygame.data.ALL_RELICS
import com.jay.jaygame.data.RelicGrade
import com.jay.jaygame.ui.components.GameCard
import com.jay.jaygame.ui.components.NeonButton
import com.jay.jaygame.ui.theme.DeepDark
import com.jay.jaygame.ui.theme.Divider
import com.jay.jaygame.ui.theme.Gold
import com.jay.jaygame.ui.theme.GoldCoin
import com.jay.jaygame.ui.theme.JayGameTheme
import com.jay.jaygame.ui.theme.LightText
import com.jay.jaygame.ui.theme.NeonCyan
import com.jay.jaygame.ui.theme.NeonGreen
import com.jay.jaygame.ui.theme.NeonRed
import com.jay.jaygame.ui.theme.SubText
import com.jay.jaygame.ui.components.LottieAsset
import com.jay.jaygame.ui.theme.TrophyAmber
import kotlinx.coroutines.delay

// Pre-allocated Color constants
private val StarGold = Color(0xFFFFD700)
private val StarEmpty = Color(0xFF4A3A2A)
private val NewRecordRed = Color(0xFFFF4444)
private val NewRecordOrange = Color(0xFFFF8800)
private val NewRecordYellow = Color(0xFFFFDD00)
private val NewRecordGreen = Color(0xFF44FF44)
private val NewRecordCyan = Color(0xFF44DDFF)
private val NewRecordBlue = Color(0xFF4444FF)
private val NewRecordPurple = Color(0xFFDD44FF)
private val RainbowColors = listOf(
    NewRecordRed, NewRecordOrange, NewRecordYellow,
    NewRecordGreen, NewRecordCyan, NewRecordBlue, NewRecordPurple,
)
private val RewardCardBg = Color(0xFF2A1F15)
private val RewardCardBorder = Color(0xFF5A4430)
private val ButtonNeutralColor = Color(0xFF5A5A5A)
private val ButtonNeutralDark = Color(0xFF3A3A3A)
@Composable
fun ResultScreen(
    victory: Boolean,
    waveReached: Int,
    goldEarned: Int,
    trophyChange: Int,
    killCount: Int,
    mergeCount: Int,
    cardsEarned: Int = 0,
    noHpLost: Boolean = false,
    fastClear: Boolean = false,
    isNewRecord: Boolean = victory,
    relicDropId: Int = -1,
    relicDropGrade: Int = -1,
    onGoHome: () -> Unit,
    onRetry: () -> Unit = onGoHome,
) {
    // H1: Star rating calculation
    val starCount = if (!victory) 0 else {
        var stars = 1 // base star for victory
        if (noHpLost || fastClear) stars++
        if (noHpLost && fastClear) stars++
        stars
    }

    // H1: Star bounce animations (sequential)
    val star1Scale = remember { Animatable(0f) }
    val star2Scale = remember { Animatable(0f) }
    val star3Scale = remember { Animatable(0f) }
    val starScales = listOf(star1Scale, star2Scale, star3Scale)

    // H3: Stat count-up targets
    var animateStats by remember { mutableStateOf(false) }
    val animatedWave by animateIntAsState(
        targetValue = if (animateStats) waveReached else 0,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "waveAnim",
    )
    val animatedKills by animateIntAsState(
        targetValue = if (animateStats) killCount else 0,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "killAnim",
    )
    val animatedMerges by animateIntAsState(
        targetValue = if (animateStats) mergeCount else 0,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "mergeAnim",
    )

    // H2: Reward visibility states (staggered)
    var showGoldReward by remember { mutableStateOf(false) }
    var showTrophyReward by remember { mutableStateOf(false) }
    var showCardsReward by remember { mutableStateOf(false) }
    var showRelicReward by remember { mutableStateOf(false) }

    // H4: New record banner visibility
    var showNewRecord by remember { mutableStateOf(false) }

    // H5: Buttons visibility
    var showButtons by remember { mutableStateOf(false) }

    // Sequential animation orchestration
    LaunchedEffect(Unit) {
        // Phase 1: Stars (0-900ms)
        for (i in 0 until starCount) {
            delay(300L)
            starScales[i].animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
        delay(200L)

        // Phase 2: Stat count-up
        animateStats = true
        delay(500L)

        // Phase 3: Rewards one by one
        showGoldReward = true
        delay(400L)
        showTrophyReward = true
        delay(400L)
        if (cardsEarned > 0) {
            showCardsReward = true
            delay(400L)
        }
        if (relicDropId >= 0 && relicDropGrade >= 0) {
            showRelicReward = true
            delay(400L)
        }

        // Phase 4: New record banner
        if (isNewRecord && victory) {
            showNewRecord = true
            delay(300L)
        }

        // Phase 5: Buttons
        showButtons = true
    }

    // Title pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val resultText = if (victory) "승리!" else "패배..."
    val resultColor = if (victory) NeonGreen else NeonRed

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepDark)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Victory / Defeat title with pulse animation
            Text(
                text = resultText,
                fontWeight = FontWeight.Bold,
                color = resultColor,
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                modifier = Modifier.scale(pulseScale),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // H1: Star rating row
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (i in 0 until 3) {
                    val earned = i < starCount
                    val scale = starScales[i].value
                    Text(
                        text = "\u2605",
                        fontSize = 32.sp,
                        color = if (earned && scale > 0f) StarGold else StarEmpty,
                        modifier = Modifier.graphicsLayer {
                            scaleX = if (earned) scale else 1f
                            scaleY = if (earned) scale else 1f
                        },
                    )
                    if (i < 2) Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // H4: New record banner
            if (showNewRecord && victory) {
                NewRecordBanner()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Stats panel
            GameCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (victory) NeonGreen.copy(alpha = 0.4f) else NeonRed.copy(alpha = 0.4f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                ) {
                    // H3: Animated stat rows
                    StatRow(label = "웨이브", value = "$animatedWave")
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow(label = "처치", value = formatNumber(animatedKills))
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow(label = "합성", value = "$animatedMerges")

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Divider)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Rewards header
                    Text(
                        text = "보상",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Gold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // H2: Rewards appearing one by one with spring scale
                    AnimatedVisibility(
                        visible = showGoldReward,
                        enter = scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        ),
                    ) {
                        RewardCard(
                            icon = "\uD83E\uDE99",
                            label = "+${formatNumber(goldEarned)} 골드",
                            color = GoldCoin,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    AnimatedVisibility(
                        visible = showTrophyReward,
                        enter = scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        ),
                    ) {
                        RewardCard(
                            icon = "\uD83C\uDFC6",
                            label = "${if (trophyChange >= 0) "+" else ""}$trophyChange 트로피",
                            color = TrophyAmber,
                        )
                    }

                    if (cardsEarned > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        AnimatedVisibility(
                            visible = showCardsReward,
                            enter = scaleIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            ),
                        ) {
                            RewardCard(
                                icon = "\uD83C\uDCCF",
                                label = "+$cardsEarned 카드",
                                color = NeonCyan,
                            )
                        }
                    }

                    if (relicDropId >= 0 && relicDropGrade >= 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        AnimatedVisibility(
                            visible = showRelicReward,
                            enter = scaleIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            ),
                        ) {
                            val relicName = ALL_RELICS.getOrNull(relicDropId)?.name ?: "유물"
                            val grade = RelicGrade.entries.getOrNull(relicDropGrade)
                            val gradeLabel = grade?.label ?: ""
                            val gradeColor = grade?.let { Color(it.colorHex) } ?: NeonCyan
                            RewardCard(
                                icon = "\uD83D\uDC8E",
                                label = "유물 획득! $relicName ($gradeLabel)",
                                color = gradeColor,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // H5: Single confirm button
            AnimatedVisibility(
                visible = showButtons,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ),
            ) {
                NeonButton(
                    text = "확인",
                    onClick = onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    fontSize = 15.sp,
                    accentColor = Gold,
                    accentColorDark = Gold.copy(alpha = 0.5f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Lottie confetti + celebration overlay for victory
        if (victory) {
            LottieAsset(
                asset = "lottie/victory_celebration.json",
                iterations = 3,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (showNewRecord && victory) {
            LottieAsset(
                asset = "lottie/confetti.json",
                iterations = Int.MAX_VALUE,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * H4: "신기록!" banner with rainbow color cycling + scale pulse
 */
@Composable
private fun NewRecordBanner() {
    val infiniteTransition = rememberInfiniteTransition(label = "newRecord")

    // Rainbow color cycling
    val colorPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = RainbowColors.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "colorPhase",
    )

    // Scale pulse
    val bannerScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bannerScale",
    )

    val idx = colorPhase.toInt() % RainbowColors.size
    val nextIdx = (idx + 1) % RainbowColors.size
    val fraction = colorPhase - colorPhase.toInt()
    val currentColor = lerpColor(RainbowColors[idx], RainbowColors[nextIdx], fraction)

    Text(
        text = "신기록!",
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        color = currentColor,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = bannerScale
                scaleY = bannerScale
            },
    )
}

/**
 * H2: Reward card wrapper with background box
 */
@Composable
private fun RewardCard(
    icon: String,
    label: String,
    color: Color,
) {
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = RewardCardBg,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(text = icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = color,
            )
        }
        LottieAsset(
            asset = "lottie/reward_shine.json",
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(alpha = 0.5f),
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = SubText,
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = LightText,
        )
    }
}

private fun formatNumber(n: Int): String {
    return String.format("%,d", n)
}

private fun lerpColor(a: Color, b: Color, fraction: Float): Color {
    return Color(
        red = a.red + (b.red - a.red) * fraction,
        green = a.green + (b.green - a.green) * fraction,
        blue = a.blue + (b.blue - a.blue) * fraction,
        alpha = 1f,
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ResultScreenVictoryPreview() {
    JayGameTheme {
        ResultScreen(
            victory = true,
            waveReached = 25,
            goldEarned = 1200,
            trophyChange = 15,
            killCount = 150,
            mergeCount = 12,
            cardsEarned = 3,
            noHpLost = true,
            fastClear = true,
            isNewRecord = true,
            onGoHome = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ResultScreenDefeatPreview() {
    JayGameTheme {
        ResultScreen(
            victory = false,
            waveReached = 10,
            goldEarned = 300,
            trophyChange = -5,
            killCount = 42,
            mergeCount = 4,
            onGoHome = {},
            onRetry = {},
        )
    }
}
