package com.example.jaygame.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.ui.theme.DiamondBlue
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.GoldCoin
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.SubText
import com.example.jaygame.ui.theme.TrophyAmber
import com.example.jaygame.data.ALL_PROFILES
import java.text.NumberFormat

// ── Pre-allocated shimmer colors ──
private val ShimmerTransparent = Color(0x00FFFFFF)
private val ShimmerHighlight = Color(0x22FFFFFF)

private val PatternFill = Gold.copy(alpha = 0.08f)
private val PatternAccent = NeonCyan.copy(alpha = 0.06f)
private val PatternStroke = Gold.copy(alpha = 0.15f)
private val BannerBg = Color(0xFF1A1208)
private val BannerOverlayStart = Color(0xDD1A1208)
private val BannerOverlayMid = Color(0x881A1208)
private val BannerBorder = Gold.copy(alpha = 0.3f)

@Composable
fun ProfileBanner(
    playerLevel: Int,
    trophies: Int,
    gold: Int,
    diamonds: Int,
    totalXP: Int,
    selectedProfileId: Int = 0,
    modifier: Modifier = Modifier,
) {
    val titleName = remember(selectedProfileId) {
        ALL_PROFILES.find { it.id == selectedProfileId }?.name
    }
    val rank = getRankInfo(trophies)
    val fmt = remember { NumberFormat.getIntegerInstance() }
    val xpForCurrentLevel = totalXP % 100
    val xpProgress = xpForCurrentLevel / 100f

    // ── B4: Animated currency counters ──
    val animatedGold by animateIntAsState(
        targetValue = gold,
        animationSpec = tween(durationMillis = 500),
        label = "gold",
    )
    val animatedDiamonds by animateIntAsState(
        targetValue = diamonds,
        animationSpec = tween(durationMillis = 500),
        label = "diamonds",
    )
    val animatedTrophies by animateIntAsState(
        targetValue = trophies,
        animationSpec = tween(durationMillis = 500),
        label = "trophies",
    )

    // ── B5: Shimmer animation ──
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by shimmerTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
        ),
        label = "shimmerProgress",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRect(BannerBg)
                drawEmblemPattern()
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(BannerOverlayStart, BannerOverlayMid, BannerOverlayStart),
                    ),
                )
            }
            .drawWithContent {
                drawContent()
                // Shimmer highlight strip
                val stripWidth = size.width * 0.25f
                val x = shimmerProgress * (size.width + stripWidth) - stripWidth
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(ShimmerTransparent, ShimmerHighlight, ShimmerTransparent),
                        start = Offset(x, 0f),
                        end = Offset(x + stripWidth, size.height),
                    ),
                    blendMode = BlendMode.SrcOver,
                )
            }
            .border(1.dp, BannerBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top row: Level + Name | Trophies
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Lv.${playerLevel}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Gold,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "플레이어",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText,
                )
                if (titleName != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = titleName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_trophy),
                    contentDescription = null,
                    tint = TrophyAmber,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = fmt.format(animatedTrophies),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrophyAmber,
                )
            }

            // Middle row: Rank | Resources
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RankBadge(trophies = trophies)
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_gold),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = fmt.format(animatedGold),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldCoin,
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_diamond),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = fmt.format(animatedDiamonds),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DiamondBlue,
                )
            }

            // Bottom: XP bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "EXP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan.copy(alpha = 0.8f),
                    )
                    Text(
                        text = "${xpForCurrentLevel} / 100",
                        fontSize = 10.sp,
                        color = SubText,
                    )
                }
                Spacer(Modifier.height(3.dp))
                NeonProgressBar(
                    progress = xpProgress,
                    height = 6.dp,
                    barColor = NeonCyan,
                )
            }
        }
    }
}

private fun DrawScope.drawEmblemPattern() {
    val cx = size.width / 2
    val cy = size.height / 2

    // Central diamond
    val diamondSize = size.height * 0.35f
    val diamond = Path().apply {
        moveTo(cx, cy - diamondSize)
        lineTo(cx + diamondSize, cy)
        lineTo(cx, cy + diamondSize)
        lineTo(cx - diamondSize, cy)
        close()
    }
    drawPath(diamond, PatternFill)
    drawPath(diamond, PatternStroke, style = Stroke(width = 1.5f))

    // Inner diamond
    val innerSize = diamondSize * 0.5f
    val innerDiamond = Path().apply {
        moveTo(cx, cy - innerSize)
        lineTo(cx + innerSize, cy)
        lineTo(cx, cy + innerSize)
        lineTo(cx - innerSize, cy)
        close()
    }
    drawPath(innerDiamond, PatternAccent)
    drawPath(innerDiamond, PatternStroke, style = Stroke(width = 1f))

    // Corner circles
    val circleRadius = size.height * 0.12f
    listOf(
        Offset(size.width * 0.15f, size.height * 0.25f),
        Offset(size.width * 0.85f, size.height * 0.25f),
        Offset(size.width * 0.15f, size.height * 0.75f),
        Offset(size.width * 0.85f, size.height * 0.75f),
    ).forEach { center ->
        drawCircle(PatternFill, circleRadius, center)
        drawCircle(PatternStroke, circleRadius, center, style = Stroke(width = 1f))
    }

    // Horizontal lines through center
    drawLine(
        PatternStroke,
        Offset(cx - diamondSize * 1.8f, cy),
        Offset(cx - diamondSize * 1.1f, cy),
        strokeWidth = 1f,
    )
    drawLine(
        PatternStroke,
        Offset(cx + diamondSize * 1.1f, cy),
        Offset(cx + diamondSize * 1.8f, cy),
        strokeWidth = 1f,
    )
}
