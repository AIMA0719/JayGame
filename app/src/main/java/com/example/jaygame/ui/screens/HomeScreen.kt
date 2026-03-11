package com.example.jaygame.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.STAGES
import com.example.jaygame.data.StaminaManager
import com.example.jaygame.ui.components.DailyLoginDialog
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.components.NeonProgressBar
import com.example.jaygame.ui.components.RankBadge
import com.example.jaygame.ui.components.StageCardPager
import com.example.jaygame.ui.components.canClaim
import com.example.jaygame.ui.components.claimReward
import com.example.jaygame.ui.theme.*
import java.text.NumberFormat

@Composable
fun HomeScreen(
    repository: GameRepository,
    onStartBattle: () -> Unit,
) {
    val data by repository.gameData.collectAsState()
    var showDailyLogin by remember { mutableStateOf(false) }
    val fmt = remember { NumberFormat.getIntegerInstance() }

    LaunchedEffect(data) {
        if (canClaim(data)) showDailyLogin = true
    }

    if (showDailyLogin) {
        DailyLoginDialog(
            data = data,
            onClaim = {
                val updated = claimReward(data)
                repository.save(updated)
                showDailyLogin = false
            },
            onDismiss = { showDailyLogin = false },
        )
    }

    val stage = remember(data.currentStageId) {
        STAGES.getOrNull(data.currentStageId) ?: STAGES[0]
    }

    // Pre-load all stage background bitmaps
    val context = LocalContext.current
    val stageBitmaps = remember {
        STAGES.associate { s ->
            s.id to BitmapFactory.decodeStream(context.assets.open(s.bgAsset)).asImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A)),
    ) {
        // Background image with crossfade
        Crossfade(
            targetState = data.currentStageId,
            animationSpec = tween(durationMillis = 500),
            label = "bg",
        ) { stageId ->
            stageBitmaps[stageId]?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.4f,
                )
            }
        }
        // Dark overlay gradient for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xCC0A0A1A),
                            Color(0x800A0A1A),
                            Color(0xCC0A0A1A),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── Top: Profile + Resources (compact row) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Level + Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Gold.copy(alpha = 0.15f))
                            .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Lv.${data.playerLevel}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Gold,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "플레이어",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RankBadge(trophies = data.trophies)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Resources
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gold),
                        contentDescription = null,
                        tint = GoldCoin,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = fmt.format(data.gold),
                        color = GoldCoin,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_diamond),
                        contentDescription = null,
                        tint = DiamondBlue,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = fmt.format(data.diamonds),
                        color = DiamondBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Trophy count
            Text(
                text = "\uD83C\uDFC6 ${data.trophies}",
                fontSize = 12.sp,
                color = TrophyAmber.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 2.dp),
            )

            Spacer(modifier = Modifier.weight(0.3f))

            // ── Center: Stage selector (big & prominent) ──
            StageCardPager(
                currentStageId = data.currentStageId,
                unlockedStages = data.unlockedStages,
                stageBestWaves = data.stageBestWaves,
                difficulty = data.difficulty,
                onStageChanged = { stageId ->
                    if (stageId != data.currentStageId) {
                        repository.save(data.copy(currentStageId = stageId))
                    }
                },
            )

            Spacer(modifier = Modifier.weight(0.5f))

            // ── Bottom: Stamina + Battle button ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Stamina bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "\u26A1 스태미나",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StaminaGreen,
                    )
                    Text(
                        text = "${data.stamina} / ${data.maxStamina}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StaminaGreen.copy(alpha = 0.8f),
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                NeonProgressBar(
                    progress = data.stamina.toFloat() / data.maxStamina.coerceAtLeast(1),
                    barColor = StaminaGreen,
                    height = 10.dp,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Big battle button
                run {
                    val isUnlocked = data.currentStageId in data.unlockedStages
                    val hasStamina = data.stamina >= stage.staminaCost
                    val canStart = isUnlocked && hasStamina

                    NeonButton(
                        text = "\u26A1${stage.staminaCost}  전투 시작",
                        onClick = {
                            val consumed = StaminaManager.consume(data, stage.staminaCost)
                            if (consumed != null) {
                                repository.save(consumed.copy(currentStageId = data.currentStageId))
                                onStartBattle()
                            }
                        },
                        enabled = canStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        fontSize = 19.sp,
                        accentColor = NeonRed,
                        accentColorDark = NeonRedDark,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
