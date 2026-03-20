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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun GambleDialog(onDismiss: () -> Unit) {
    val battle by BattleBridge.state.collectAsState()
    var selectedOption by remember { mutableStateOf(GambleSystem.GambleOption.SAFE) }
    var result by remember { mutableStateOf<GambleSystem.GambleResult?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.82f)),
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val currentResult = result
                if (currentResult == null) {
                    GambleSelectionPhase(
                        currentSp = battle.sp,
                        selectedOption = selectedOption,
                        onSelectOption = { selectedOption = it },
                        onConfirm = {
                            val r = BattleBridge.requestGamble(selectedOption)
                            if (r != null) result = r
                        },
                        onDismiss = onDismiss,
                    )
                } else {
                    GambleResultPhase(
                        result = currentResult,
                        currentSp = battle.sp,
                        onRetry = { result = null },
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun GambleSelectionPhase(
    currentSp: Float,
    selectedOption: GambleSystem.GambleOption,
    onSelectOption: (GambleSystem.GambleOption) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val losingStreak = BattleBridge.gambleLosingStreak

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

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "입장료: ${GambleSystem.ENTRY_FEE.toInt()} SP",
        color = SubText,
        fontSize = 12.sp,
    )

    if (losingStreak >= 1) {
        val streakColor = if (losingStreak >= 3) NeonGreen else Color(0xFFFF8844)
        Text(
            text = if (losingStreak >= 3) "\uD83D\uDD25 연패 보호 발동! 성공률 +15%"
            else "연패: ${losingStreak}회 (3연패 시 보호 발동)",
            color = streakColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

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
                .clickable { onSelectOption(option) }
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
                    Row {
                        Text(
                            text = "성공 ${(option.baseSuccessRate * 100).toInt()}%",
                            color = SubText,
                            fontSize = 11.sp,
                        )
                        Text(
                            text = "  실패 시 -${(option.lossPenaltyRate * 100).toInt()}%",
                            color = NeonRed.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "x${option.multiplier}",
                        color = if (isSelected) colors.first else Color.White.copy(alpha = 0.7f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "+${(GambleSystem.ENTRY_FEE * option.multiplier).toInt()} SP",
                        color = if (isSelected) colors.first.copy(alpha = 0.8f) else SubText,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("현재 SP: ${currentSp.toInt()}", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        val lossOnFail = ((currentSp - GambleSystem.ENTRY_FEE) * selectedOption.lossPenaltyRate).toInt()
        Text(
            "실패 시 손실: -${GambleSystem.ENTRY_FEE.toInt() + lossOnFail} SP",
            color = NeonRed.copy(alpha = 0.7f),
            fontSize = 11.sp,
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    val canGamble = currentSp >= GambleSystem.ENTRY_FEE
    val optColors = OptionColors[selectedOption]!!
    NeonButton(
        text = "\uD83C\uDFB2 도박! (${GambleSystem.ENTRY_FEE.toInt()} SP)",
        onClick = { if (canGamble) onConfirm() },
        modifier = Modifier.fillMaxWidth().height(44.dp),
        accentColor = optColors.first,
        accentColorDark = optColors.second,
        enabled = canGamble,
    )

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun GambleResultPhase(
    result: GambleSystem.GambleResult,
    currentSp: Float,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isWin = result.won
    val winColor = if (isWin) Gold else NeonRed
    val optColors = OptionColors[result.option]!!

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = if (isWin) "\uD83C\uDF89 성공!" else "\uD83D\uDCA5 실패!",
        color = winColor,
        fontSize = 26.sp,
        fontWeight = FontWeight.ExtraBold,
    )

    if (result.streakBroken) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "연패 보호 발동!",
            color = NeonGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        text = result.option.label,
        color = optColors.first,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (isWin) {
        Text(
            text = "+${result.reward.toInt()} SP",
            color = NeonGreen,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = "x${result.option.multiplier} 배율 적용",
            color = Gold,
            fontSize = 13.sp,
        )
    } else {
        Text(
            text = "-${result.penalty.toInt()} SP",
            color = NeonRed,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Row {
            Text(
                text = "입장료 ${GambleSystem.ENTRY_FEE.toInt()}",
                color = SubText,
                fontSize = 11.sp,
            )
            Text(
                text = " + 추가 손실 ${(result.penalty - GambleSystem.ENTRY_FEE).toInt()} SP",
                color = NeonRed.copy(alpha = 0.7f),
                fontSize = 11.sp,
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text("${result.spBefore.toInt()}", color = SubText, fontSize = 14.sp)
        Text(" \u2192 ", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        Text(
            "${result.spAfter.toInt()} SP",
            color = if (isWin) NeonGreen else NeonRed,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NeonButton(
            text = "다시",
            onClick = onRetry,
            modifier = Modifier.weight(1f).height(40.dp),
            accentColor = SubText,
            accentColorDark = SubText.copy(alpha = 0.6f),
            enabled = currentSp >= GambleSystem.ENTRY_FEE,
        )
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
