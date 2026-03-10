package com.example.jaygame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.ui.theme.DarkBrown
import com.example.jaygame.ui.theme.DiamondBlue
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.MedievalFont
import com.example.jaygame.ui.theme.MediumBrown
import com.example.jaygame.ui.theme.MetalGray
import com.example.jaygame.ui.theme.Parchment

data class RankInfo(
    val name: String,
    val color: Color,
)

fun getRankInfo(trophies: Int): RankInfo = when {
    trophies >= 4000 -> RankInfo("마스터", Color(0xFFFF6F00))
    trophies >= 3000 -> RankInfo("다이아몬드", DiamondBlue)
    trophies >= 2000 -> RankInfo("골드", Gold)
    trophies >= 1000 -> RankInfo("실버", MetalGray)
    else -> RankInfo("브론즈", Color(0xFFCD7F32))
}

@Composable
fun RankBadge(
    trophies: Int,
    modifier: Modifier = Modifier,
) {
    val rank = getRankInfo(trophies)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MediumBrown)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = "\uD83C\uDFC6",
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "RANK",
            fontFamily = MedievalFont,
            fontSize = 10.sp,
            color = Parchment.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = rank.name,
            fontFamily = MedievalFont,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = rank.color,
        )
    }
}

@Composable
fun DifficultyDialog(
    currentDifficulty: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        Triple(0, "쉬움", "적 체력 -20%, 보상 -20%"),
        Triple(1, "보통", "기본 난이도"),
        Triple(2, "어려움", "적 체력 +50%, 보상 +50%"),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "난이도 선택",
                fontFamily = MedievalFont,
                fontWeight = FontWeight.Bold,
                color = Gold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (id, name, desc) ->
                    val isSelected = id == currentDifficulty
                    MedievalButton(
                        text = "$name — $desc",
                        onClick = { onSelect(id) },
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 13.sp,
                        accentColor = if (isSelected) Gold else MetalGray,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = Parchment)
            }
        },
        containerColor = DarkBrown,
    )
}
