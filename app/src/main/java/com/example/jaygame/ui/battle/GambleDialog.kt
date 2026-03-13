package com.example.jaygame.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.engine.GambleSystem
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonGreen
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark
import com.example.jaygame.ui.theme.SubText

// Pre-allocated option colors
private val SafeGreen = Color(0xFF43A047)
private val SafeGreenDark = Color(0xFF2E7D32)
private val NormalBlue = Color(0xFF1E88E5)
private val NormalBlueDark = Color(0xFF1565C0)
private val RiskyOrange = Color(0xFFFF8C00)
private val RiskyOrangeDark = Color(0xFFCC6600)
private val JackpotPurple = Color(0xFFAB47BC)
private val JackpotPurpleDark = Color(0xFF7B1FA2)

private val OptionColors = mapOf(
    GambleSystem.GambleOption.SAFE to (SafeGreen to SafeGreenDark),
    GambleSystem.GambleOption.NORMAL to (NormalBlue to NormalBlueDark),
    GambleSystem.GambleOption.RISKY to (RiskyOrange to RiskyOrangeDark),
    GambleSystem.GambleOption.JACKPOT to (JackpotPurple to JackpotPurpleDark),
)

/**
 * 4-tier gamble dialog.
 *
 * Phase 1 — Select option + bet amount, confirm.
 * Phase 2 — Show result (win = gold, lose = red).
 */
@Composable
fun GambleDialog(
    onDismiss: () -> Unit,
) {
    val battle by BattleBridge.state.collectAsState()
    var selectedOption by remember { mutableStateOf(GambleSystem.GambleOption.SAFE) }
    // betPercent: 10% ~ 80%, default 30%
    var betPercent by remember { mutableFloatStateOf(0.30f) }
    var result by remember { mutableStateOf<GambleSystem.GambleResult?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.82f),
                    ),
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        GameCard(
            modifier = Modifier
                .width(320.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
            borderColor = JackpotPurple.copy(alpha = 0.5f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val currentResult = result
                if (currentResult == null) {
                    // ── Phase 1: selection ──

                    // Title + close
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "\uD83C\uDFB2 도박",
                            color = JackpotPurple,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        Text(
                            text = "\u2715",
                            color = SubText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clickable { onDismiss() }
                                .padding(4.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── 4 option buttons ──
                    GambleSystem.GambleOption.entries.forEach { option ->
                        val colors = OptionColors[option]!!
                        val isSelected = selectedOption == option
                        val borderCol = if (isSelected) colors.first else Color.White.copy(alpha = 0.15f)
                        val bgBrush = if (isSelected)
                            Brush.horizontalGradient(listOf(colors.first.copy(alpha = 0.35f), colors.second.copy(alpha = 0.25f)))
                        else
                            Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.04f)))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgBrush)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = borderCol,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { selectedOption = option }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = option.label,
                                        color = if (isSelected) colors.first else LightText,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    )
                                    Text(
                                        text = "성공률 ${(option.baseSuccessRate * 100).toInt()}%",
                                        color = SubText,
                                        fontSize = 11.sp,
                                    )
                                }
                                Text(
                                    text = "x${option.multiplier}",
                                    color = if (isSelected) colors.first else Color.White.copy(alpha = 0.7f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Bet slider ──
                    val betAmount = battle.sp * betPercent
                    Text(
                        text = "베팅 비율: ${(betPercent * 100).toInt()}%",
                        color = Gold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Slider(
                        value = betPercent,
                        onValueChange = { betPercent = it },
                        valueRange = 0.10f..0.80f,
                        steps = 6, // 10, 20, 30, 40, 50, 60, 70, 80 = 8 points, 6 steps between
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = JackpotPurple,
                            activeTrackColor = JackpotPurple,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                        ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("현재 SP: ${battle.sp.toInt()}", color = SubText, fontSize = 12.sp)
                        Text("베팅: ${betAmount.toInt()} SP", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Confirm button ──
                    val canGamble = betAmount >= 10f
                    val optColors = OptionColors[selectedOption]!!
                    NeonButton(
                        text = "\uD83C\uDFB2 도박!",
                        onClick = {
                            if (canGamble) {
                                val r = BattleBridge.requestGamble(betPercent, selectedOption)
                                if (r != null) result = r
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        accentColor = optColors.first,
                        accentColorDark = optColors.second,
                        enabled = canGamble,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    // ── Phase 2: result ──
                    val isWin = currentResult.won
                    val winColor = if (isWin) Gold else NeonRed

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isWin) "\uD83C\uDF89 성공!" else "\uD83D\uDCA5 실패!",
                        color = winColor,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option name
                    val optColors = OptionColors[selectedOption]!!
                    Text(
                        text = selectedOption.label,
                        color = optColors.first,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bet → reward
                    Text(
                        text = "베팅: ${currentResult.bet.toInt()} SP",
                        color = SubText,
                        fontSize = 13.sp,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isWin) {
                        Text(
                            text = "+${currentResult.reward.toInt()} SP",
                            color = NeonGreen,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "x${currentResult.multiplier} 배율 적용",
                            color = Gold,
                            fontSize = 13.sp,
                        )
                    } else {
                        Text(
                            text = "-${currentResult.bet.toInt()} SP",
                            color = NeonRed,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "아깝다...",
                            color = SubText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "남은 SP: ${battle.sp.toInt()}",
                        color = Gold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Again
                        NeonButton(
                            text = "다시",
                            onClick = { result = null },
                            modifier = Modifier.weight(1f).height(40.dp),
                            accentColor = SubText,
                            accentColorDark = SubText.copy(alpha = 0.6f),
                            enabled = battle.sp >= 10f,
                        )
                        // Close
                        NeonButton(
                            text = "확인",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(40.dp),
                            accentColor = if (isWin) NeonGreen else NeonRed,
                            accentColorDark = if (isWin) NeonGreen.copy(alpha = 0.6f) else NeonRedDark,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

data class GambleResult(
    val spChange: Float,
    val newSp: Float,
    val percentage: Float,
)
