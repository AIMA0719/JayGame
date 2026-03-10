package com.example.jaygame.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.Divider
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.GoldCoin
import com.example.jaygame.ui.theme.JayGameTheme
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.NeonGreen
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark
import com.example.jaygame.ui.theme.SubText
import com.example.jaygame.ui.theme.TrophyAmber

@Composable
fun ResultScreen(
    victory: Boolean,
    waveReached: Int,
    goldEarned: Int,
    trophyChange: Int,
    killCount: Int,
    mergeCount: Int,
    onGoHome: () -> Unit,
) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDark)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Victory / Defeat title with pulse animation
        Text(
            text = resultText,
            style = MaterialTheme.typography.displayLarge,
            color = resultColor,
            textAlign = TextAlign.Center,
            fontSize = 48.sp,
            modifier = Modifier.scale(pulseScale),
        )

        Spacer(modifier = Modifier.height(32.dp))

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
                StatRow(label = "웨이브", value = "$waveReached")
                Spacer(modifier = Modifier.height(8.dp))
                StatRow(label = "처치", value = "$killCount")
                Spacer(modifier = Modifier.height(8.dp))
                StatRow(label = "합성", value = "$mergeCount")

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Divider)
                Spacer(modifier = Modifier.height(12.dp))

                // Rewards
                Text(
                    text = "보상",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Gold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                RewardRow(icon = "\uD83E\uDE99", label = "+${formatNumber(goldEarned)} 골드", color = GoldCoin)
                Spacer(modifier = Modifier.height(6.dp))
                RewardRow(
                    icon = "\uD83C\uDFC6",
                    label = "${if (trophyChange >= 0) "+" else ""}$trophyChange 트로피",
                    color = TrophyAmber,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Home button
        NeonButton(
            text = "홈으로",
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            fontSize = 18.sp,
            accentColor = NeonCyan,
            accentColorDark = NeonCyan.copy(alpha = 0.5f),
        )

        Spacer(modifier = Modifier.height(32.dp))
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
            fontSize = 15.sp,
            color = SubText,
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = LightText,
        )
    }
}

@Composable
private fun RewardRow(
    icon: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = color,
        )
    }
}

private fun formatNumber(n: Int): String {
    return String.format("%,d", n)
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
            onGoHome = {},
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
        )
    }
}
