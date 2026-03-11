package com.example.jaygame.ui.battle

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.ui.theme.*

// ── Top HUD ──────────────────────────────────────────────────

@Composable
fun BattleTopHud(onPauseClick: () -> Unit = {}) {
    val battle by BattleBridge.state.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Main top panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1A1025).copy(alpha = 0.92f),
                            Color(0xFF0D0815).copy(alpha = 0.88f),
                        )
                    )
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Pause button
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF3A2E45), Color(0xFF2A1F35))
                        )
                    )
                    .border(1.dp, Color(0xFF6B5A80).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onPauseClick),
                contentAlignment = Alignment.Center,
            ) {
                Text("☰", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            // Center info panel: WAVE + Timer + Enemy count
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF2A1F35).copy(alpha = 0.6f),
                                Color(0xFF3A2855).copy(alpha = 0.8f),
                                Color(0xFF2A1F35).copy(alpha = 0.6f),
                            )
                        ),
                        RoundedCornerShape(10.dp),
                    )
                    .border(1.dp, Color(0xFF6B5A80).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // WAVE badge
                Text(
                    text = "WAVE",
                    color = NeonCyan.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${battle.currentWave}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Timer
                val totalSeconds = battle.elapsedTime.toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                Text(
                    text = "%02d:%02d".format(minutes, seconds),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Enemy count with skull
                Text(
                    text = "\uD83D\uDC80",
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${battle.enemyCount}/${battle.maxEnemyCount}",
                    color = if (battle.enemyCount > 80) NeonRed else Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // HP bar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1A1025)),
                ) {
                    val hpFrac = if (battle.maxHP > 0)
                        (battle.playerHP.toFloat() / battle.maxHP).coerceIn(0f, 1f) else 1f
                    val hpColor = when {
                        battle.playerHP <= battle.maxHP / 4 -> NeonRed
                        battle.playerHP <= battle.maxHP / 2 -> WarningYellow
                        else -> NeonGreen
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(hpFrac)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(hpColor),
                    )
                }
                Text(
                    text = "HP ${battle.playerHP}",
                    color = if (battle.playerHP <= battle.maxHP / 4) NeonRed
                    else Color.White.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Thin separator glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFF8B6AFF).copy(alpha = 0.4f),
                            Color(0xFF8B6AFF).copy(alpha = 0.4f),
                            Color.Transparent,
                        )
                    )
                ),
        )
    }
}

// ── Bottom HUD ───────────────────────────────────────────────

@Composable
fun BattleBottomHud(
    onGambleClick: () -> Unit = {},
    onBuyClick: () -> Unit = {},
    onUpgradeClick: () -> Unit = {},
) {
    val battle by BattleBridge.state.collectAsState()
    val gridState by BattleBridge.gridState.collectAsState()
    val gridFull = gridState.all { it.unitDefId >= 0 }
    val canSummon = battle.sp >= battle.summonCost && !gridFull
    val canGamble = battle.sp >= 10

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Separator glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Gold.copy(alpha = 0.4f),
                            Gold.copy(alpha = 0.4f),
                            Color.Transparent,
                        )
                    )
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0D0815).copy(alpha = 0.88f),
                            Color(0xFF1A1025).copy(alpha = 0.92f),
                        )
                    )
                )
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // SP bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\uD83D\uDC8E",
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF1A1025)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((battle.sp / 500f).coerceIn(0f, 1f))
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFF8F00),
                                        Gold,
                                        Color(0xFFFFDD44),
                                    )
                                )
                            ),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "${battle.sp.toInt()}",
                    color = Gold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            // Main buttons row: [구매] [소환] [도박]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // 구매 (Buy) — orange
                HudActionButton(
                    topText = "\uD83D\uDED2",
                    bottomText = "구매",
                    enabled = true,
                    gradientColors = listOf(Color(0xFFFF6D00), Color(0xFFE65100)),
                    borderColor = Color(0xFFFFAB40),
                    onClick = onBuyClick,
                    modifier = Modifier.weight(1f),
                    buttonHeight = 52.dp,
                )

                // 소환 (Summon) — gold/yellow, bigger
                SummonButton(
                    cost = battle.summonCost,
                    enabled = canSummon,
                    gridFull = gridFull,
                    onClick = { BattleBridge.requestSummon() },
                    modifier = Modifier.weight(1.6f),
                )

                // 도박 (Gamble) — green
                HudActionButton(
                    topText = "\uD83C\uDFB2",
                    bottomText = "도박",
                    subText = "10SP",
                    enabled = canGamble,
                    gradientColors = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)),
                    borderColor = Color(0xFF66BB6A),
                    onClick = onGambleClick,
                    modifier = Modifier.weight(1f),
                    buttonHeight = 52.dp,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 강화 (Upgrade) button — below summon, full width
            HudActionButton(
                topText = "\u2B06",
                bottomText = "강화",
                enabled = true,
                gradientColors = listOf(Color(0xFF1565C0), Color(0xFF0D47A1)),
                borderColor = Color(0xFF42A5F5),
                onClick = onUpgradeClick,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(horizontal = 4.dp),
                buttonHeight = 40.dp,
            )

            // Boss timer — fixed height slot
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.height(14.dp)) {
                if (battle.isBossRound) {
                    Text(
                        text = "BOSS ${battle.bossTimeRemaining.toInt()}s",
                        color = NeonRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

// ── Reusable HUD action button ───────────────────────────────

@Composable
private fun HudActionButton(
    topText: String,
    bottomText: String,
    enabled: Boolean,
    gradientColors: List<Color>,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subText: String = "",
    buttonHeight: androidx.compose.ui.unit.Dp = 48.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "actionScale",
    )

    val bgColors = if (enabled) gradientColors
    else listOf(Color(0xFF2A2A3A), Color(0xFF1A1A28))
    val actualBorder = if (enabled) borderColor.copy(alpha = 0.6f) else Color(0xFF3A3A4A)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(buttonHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (enabled) 4.dp else 1.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(bgColors),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                )
                drawRoundRect(
                    color = actualBorder,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f.dp.toPx()),
                )
            }
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = topText,
                fontSize = 16.sp,
            )
            Text(
                text = bottomText,
                color = if (enabled) Color.White else Color(0xFF666680),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            if (subText.isNotEmpty()) {
                Text(
                    text = subText,
                    color = if (enabled) Color.White.copy(alpha = 0.7f) else Color(0xFF555570),
                    fontSize = 9.sp,
                )
            }
        }
    }
}

// ── Summon Button (center, larger, with glow) ────────────────

@Composable
fun SummonButton(
    cost: Int,
    enabled: Boolean,
    gridFull: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "summonScale",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "summonGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    val bgColors = if (enabled) {
        if (isPressed) listOf(Color(0xFFB8860B), Color(0xFF8B6914))
        else listOf(Color(0xFFDAA520), Color(0xFFB8860B))
    } else {
        listOf(Color(0xFF2A2A3A), Color(0xFF1A1A28))
    }

    val glowBorderColor = if (enabled) Gold.copy(alpha = glowAlpha) else Color(0xFF3A3A4A)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(62.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                if (enabled) 8.dp else 2.dp,
                RoundedCornerShape(14.dp),
                ambientColor = if (enabled) Gold.copy(alpha = 0.3f) else Color.Black,
                spotColor = if (enabled) Gold.copy(alpha = 0.4f) else Color.Black,
            )
            .clip(RoundedCornerShape(14.dp))
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(bgColors),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                )
                drawRoundRect(
                    color = glowBorderColor,
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
            }
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "\u2694\uFE0F 소환",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = if (gridFull) "FULL" else "\uD83D\uDC8E$cost",
                color = if (gridFull) NeonRed
                else if (enabled) Color.White.copy(alpha = 0.9f)
                else Color(0xFF555570),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
