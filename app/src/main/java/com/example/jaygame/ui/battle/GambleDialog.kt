package com.example.jaygame.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.*

/**
 * Gamble dialog: pay 10 SP, random -100% to +100% of current SP.
 * Two states: confirmation → result.
 */
@Composable
fun GambleDialog(
    onDismiss: () -> Unit,
) {
    val battle by BattleBridge.state.collectAsState()
    var gambleResult by remember { mutableStateOf<GambleResult?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.8f),
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
                .width(300.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
            borderColor = Color(0xFF66BB6A).copy(alpha = 0.5f),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (gambleResult == null) {
                    // ── Confirmation state ──
                    // Title row with X close button
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "\uD83C\uDFB2 도박",
                            color = NeonGreen,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
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
                    Text(
                        text = "10 SP를 걸고 도박합니다!\n현재 SP의 -100% ~ +100% 변동",
                        color = LightText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "현재 SP: ${battle.sp.toInt()}",
                        color = Gold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    NeonButton(
                        text = "도박",
                        onClick = {
                            if (battle.sp >= 10) {
                                val result = BattleBridge.performGamble()
                                gambleResult = result
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        accentColor = NeonGreen,
                        accentColorDark = Color(0xFF1B5E20),
                        enabled = battle.sp >= 10,
                    )
                } else {
                    // ── Result state ──
                    val result = gambleResult!!
                    val isWin = result.spChange >= 0

                    Text(
                        text = if (isWin) "\uD83C\uDF89 대박!" else "\uD83D\uDCA5 쪽박!",
                        color = if (isWin) Gold else NeonRed,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${if (isWin) "+" else ""}${result.spChange.toInt()} SP",
                        color = if (isWin) NeonGreen else NeonRed,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "남은 SP: ${result.newSp.toInt()}",
                        color = Gold,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NeonButton(
                        text = "확인",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        accentColor = if (isWin) NeonGreen else NeonRed,
                        accentColorDark = if (isWin) Color(0xFF1B5E20) else NeonRedDark,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

data class GambleResult(
    val spChange: Float,
    val newSp: Float,
    val percentage: Float,
)
