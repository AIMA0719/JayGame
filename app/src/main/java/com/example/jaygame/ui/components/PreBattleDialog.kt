package com.example.jaygame.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.StageDef
import com.example.jaygame.ui.theme.*

private data class DifficultyInfo(
    val id: Int,
    val name: String,
    val desc: String,
    val enemyMult: String,
    val rewardMult: String,
    val color: Color,
)

private val DIFFICULTIES = listOf(
    DifficultyInfo(0, "초보", "입문자를 위한 난이도", "×1.0", "×1.0", NeonGreen),
    DifficultyInfo(1, "숙련자", "조금 더 강한 적", "×1.5", "×1.3", NeonCyan),
    DifficultyInfo(2, "고인물", "상당히 거친 전투", "×2.2", "×1.6", Gold),
    DifficultyInfo(3, "썩은물", "극한의 도전", "×3.0", "×2.0", Color(0xFFFF6B35)),
    DifficultyInfo(4, "챌린저", "최강자만 도전", "×4.0", "×2.5", NeonRed),
)

@Composable
fun PreBattleDialog(
    stage: StageDef,
    bestWave: Int,
    selectedDifficulty: Int,
    staminaCost: Int,
    hasStamina: Boolean,
    onDifficultySelected: (Int) -> Unit,
    onStartBattle: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2A1F15), Color(0xFF1A0F0A))
                    )
                )
                .border(1.5.dp, BorderGlow, RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Stage Info ──
            Text(
                text = stage.name,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Gold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stage.description,
                fontSize = 13.sp,
                color = SubText,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Stage stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatChip("\uD83C\uDF0A ${stage.maxWaves} 웨이브")
                Spacer(modifier = Modifier.width(16.dp))
                val recordText = if (bestWave > 0) "\uD83C\uDFC6 BEST $bestWave" else "미도전"
                StatChip(recordText)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Difficulty Selection ──
            Text(
                text = "난이도 선택",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LightText,
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(DIFFICULTIES) { _, diff ->
                    DifficultyCard(
                        info = diff,
                        isSelected = selectedDifficulty == diff.id,
                        onClick = { onDifficultySelected(diff.id) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Buttons ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Cancel
                NeonButton(
                    text = "취소",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    accentColor = SubText,
                    accentColorDark = DimText,
                )

                // Start Battle
                NeonButton(
                    text = "\u26A1$staminaCost  출전",
                    onClick = onStartBattle,
                    enabled = hasStamina,
                    modifier = Modifier.weight(2f).height(52.dp),
                    fontSize = 18.sp,
                    accentColor = NeonRed,
                    accentColorDark = NeonRedDark,
                    glowPulse = true,
                )
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    info: DifficultyInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "diffScale",
    )

    val borderColor = if (isSelected) info.color else Color.White.copy(alpha = 0.1f)
    val bgAlpha = if (isSelected) 0.25f else 0.08f

    Column(
        modifier = Modifier
            .width(120.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(info.color.copy(alpha = bgAlpha))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Name
        Text(
            text = info.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isSelected) info.color else LightText,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Description
        Text(
            text = info.desc,
            fontSize = 10.sp,
            color = SubText,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Enemy multiplier
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("\u2694 ", fontSize = 11.sp, color = SubText)
            Text(
                text = info.enemyMult,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeonRed.copy(alpha = 0.9f),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Reward multiplier
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("\uD83C\uDF81 ", fontSize = 11.sp, color = SubText)
            Text(
                text = info.rewardMult,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
            )
        }
    }
}

@Composable
private fun StatChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = LightText,
            fontWeight = FontWeight.Medium,
        )
    }
}
