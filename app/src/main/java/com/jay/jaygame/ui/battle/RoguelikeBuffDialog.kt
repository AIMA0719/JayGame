package com.jay.jaygame.ui.battle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.jay.jaygame.audio.SfxManager
import com.jay.jaygame.audio.SoundEvent
import com.jay.jaygame.engine.ActiveRoguelikeBuff
import com.jay.jaygame.engine.RoguelikeBuff
import com.jay.jaygame.engine.RoguelikeBuffGrade
import com.jay.jaygame.ui.theme.LightText
import kotlinx.coroutines.delay

private val CardBg = Color(0xFF1A1A2E)
private val TitleGold = Color(0xFFFFD54F)

private fun gradeColors(grade: RoguelikeBuffGrade): Pair<Color, Color> {
    val primary = Color(grade.colorHex)
    val dark = Color(
        red = primary.red * 0.65f,
        green = primary.green * 0.65f,
        blue = primary.blue * 0.65f,
    )
    return primary to dark
}

@Composable
fun RoguelikeBuffDialog(
    choices: List<RoguelikeBuff>,
    activeBuffs: List<ActiveRoguelikeBuff>,
    rerollsLeft: Int,
    onSelect: (Int) -> Unit,
    onReroll: () -> Unit,
) {
    var visibleCount by remember(choices) { mutableIntStateOf(0) }

    LaunchedEffect(choices) {
        for (i in 1..choices.size) {
            delay(600L)
            visibleCount = i
            SfxManager.play(SoundEvent.RoguelikeCardReveal)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.60f),
                        Color.Black.copy(alpha = 0.85f),
                    ),
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { /* 배경 클릭 무시 — 반드시 선택해야 함 */ },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title
            Text(
                text = "\u26A1 \uAC15\uD654 \uC120\uD0DD",
                color = TitleGold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Buff cards — 순차 등장
            choices.forEachIndexed { index, buff ->
                val isVisible = index < visibleCount
                val (borderColor, borderDark) = gradeColors(buff.grade)
                val existingStacks = activeBuffs.find { it.buff.id == buff.id }?.stacks ?: 0

                AnimatedVisibility(
                    visible = isVisible,
                    enter = scaleIn(initialScale = 0.5f) + fadeIn(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(CardBg, borderDark.copy(alpha = 0.25f))
                                )
                            )
                            .clickable(enabled = isVisible) { onSelect(index) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Column {
                            // Grade label + name
                            Text(
                                text = buildString {
                                    append("[${buff.grade.label}] ${buff.name}")
                                    if (existingStacks > 0) append("  x${existingStacks + 1}")
                                },
                                color = borderColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Description
                            Text(
                                text = buff.description,
                                color = LightText,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            // 리롤 버튼
            if (rerollsLeft > 0 && visibleCount >= choices.size) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF78909C), RoundedCornerShape(10.dp))
                        .background(Color(0xFF263238))
                        .clickable { onReroll() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "\uD83D\uDD04 다시 뽑기 ($rerollsLeft/${com.jay.jaygame.bridge.BattleBridge.MAX_ROGUELIKE_REROLLS})",
                        color = Color(0xFFB0BEC5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
