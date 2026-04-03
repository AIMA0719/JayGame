package com.jay.jaygame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.jaygame.ui.theme.DarkNavy
import com.jay.jaygame.ui.theme.DiamondBlue
import com.jay.jaygame.ui.theme.Gold
import com.jay.jaygame.ui.theme.SubText

data class RankInfo(
    val name: String,
    val color: Color,
)

fun getRankInfo(trophies: Int): RankInfo = when {
    trophies >= 4000 -> RankInfo("마스터", Color(0xFFFF6F00))
    trophies >= 3000 -> RankInfo("다이아몬드", DiamondBlue)
    trophies >= 2000 -> RankInfo("골드", Gold)
    trophies >= 1000 -> RankInfo("실버", SubText)
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
            .background(DarkNavy)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = "\uD83C\uDFC6",
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "RANK",
            fontSize = 10.sp,
            color = SubText,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = rank.name,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = rank.color,
        )
    }
}

