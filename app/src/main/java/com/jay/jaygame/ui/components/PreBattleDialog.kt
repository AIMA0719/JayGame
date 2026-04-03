package com.jay.jaygame.ui.components

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
import com.jay.jaygame.data.StageDef
import com.jay.jaygame.ui.theme.*


private data class DifficultyInfo(
    val id: Int, val name: String, val desc: String,
    val enemyMult: String, val rewardMult: String, val color: Color,
)

private val DialogBgBrush = Brush.verticalGradient(listOf(Color(0xFF2A1F15), Color(0xFF1A0F0A)))

private val DIFFICULTIES = listOf(
    DifficultyInfo(0, "일반", "기본 난이도", "×1.0", "×1.0", NeonGreen),
    DifficultyInfo(1, "하드", "강화된 적과 보상", "×1.5", "×1.5", Color(0xFFFF8800)),
    DifficultyInfo(2, "헬", "극한의 도전", "×2.2", "×2.5", NeonRed),
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
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DialogBgBrush)
                .border(1.5.dp, BorderGlow, RoundedCornerShape(20.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Stage Info ──
            Text(stage.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
            Text(stage.description, fontSize = 12.sp, color = SubText)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                StatChip("\uD83C\uDF0A ${stage.maxWaves} 웨이브")
                Spacer(modifier = Modifier.width(12.dp))
                StatChip(if (bestWave > 0) "\uD83C\uDFC6 BEST $bestWave" else "미도전")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Difficulty Selection ──
            Text("난이도 선택", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LightText)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(DIFFICULTIES) { _, diff ->
                    DifficultyCard(diff, selectedDifficulty == diff.id) { onDifficultySelected(diff.id) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Buttons ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NeonButton(
                    text = "취소",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    accentColor = SubText,
                    accentColorDark = DimText,
                )
                NeonButton(
                    text = "\u26A1$staminaCost  출전",
                    onClick = onStartBattle,
                    enabled = hasStamina,
                    modifier = Modifier.weight(2f).height(48.dp),
                    fontSize = 16.sp,
                    accentColor = NeonRed,
                    accentColorDark = NeonRedDark,
                    glowPulse = true,
                )
            }
        }
    }
}

@Composable
private fun DifficultyCard(info: DifficultyInfo, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        if (isSelected) 1.05f else 1f, spring(dampingRatio = 0.7f), label = "diffScale")
    Column(
        modifier = Modifier
            .width(110.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(info.color.copy(alpha = if (isSelected) 0.25f else 0.08f))
            .border(if (isSelected) 2.dp else 1.dp,
                if (isSelected) info.color else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(info.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
            color = if (isSelected) info.color else LightText)
        Text(info.desc, fontSize = 9.sp, color = SubText, textAlign = TextAlign.Center, lineHeight = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("\u2694 ", fontSize = 10.sp, color = SubText)
            Text(info.enemyMult, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonRed.copy(alpha = 0.9f))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("\uD83C\uDF81 ", fontSize = 10.sp, color = SubText)
            Text(info.rewardMult, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gold)
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
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, fontSize = 11.sp, color = LightText, fontWeight = FontWeight.Medium)
    }
}
