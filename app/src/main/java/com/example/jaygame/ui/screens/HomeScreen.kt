package com.example.jaygame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.STAGES
import com.example.jaygame.data.StaminaManager
import com.example.jaygame.navigation.Routes
import com.example.jaygame.ui.components.DailyLoginDialog
import com.example.jaygame.ui.components.DifficultyDialog
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.components.NeonProgressBar
import com.example.jaygame.ui.components.RankBadge
import com.example.jaygame.ui.components.ResourceHeader
import com.example.jaygame.ui.components.StageCardPager
import com.example.jaygame.ui.components.canClaim
import com.example.jaygame.ui.components.claimReward
import com.example.jaygame.ui.components.getRankInfo
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DimText
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark
import com.example.jaygame.ui.theme.StaminaGreen
import com.example.jaygame.ui.theme.SubText
import com.example.jaygame.ui.theme.TrophyAmber

@Composable
fun HomeScreen(
    repository: GameRepository,
    onNavigate: (String) -> Unit,
    onStartBattle: () -> Unit,
) {
    val data by repository.gameData.collectAsState()
    var showDailyLogin by remember { mutableStateOf(false) }
    var showDifficulty by remember { mutableStateOf(false) }

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

    if (showDifficulty) {
        DifficultyDialog(
            currentDifficulty = data.difficulty,
            onSelect = { diff ->
                repository.save(data.copy(difficulty = diff))
                showDifficulty = false
            },
            onDismiss = { showDifficulty = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDark),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar: resources + settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ResourceHeader(
                gold = data.gold,
                diamonds = data.diamonds,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { onNavigate(Routes.SETTINGS) },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = "설정",
                    tint = SubText,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // Profile row: level, name, rank, trophies
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val rank = getRankInfo(data.trophies)
            Text(
                text = "Lv.${data.playerLevel}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "플레이어",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LightText,
            )
            Spacer(modifier = Modifier.width(8.dp))
            RankBadge(trophies = data.trophies)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "\uD83C\uDFC6 ${data.trophies}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TrophyAmber,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stage card pager
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

        Spacer(modifier = Modifier.height(12.dp))

        // Quick buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NeonButton(
                text = "주간보상",
                onClick = { showDailyLogin = true },
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                accentColor = NeonCyan,
                accentColorDark = NeonCyan.copy(alpha = 0.5f),
            )
            NeonButton(
                text = "난이도",
                onClick = { showDifficulty = true },
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                accentColor = NeonCyan,
                accentColorDark = NeonCyan.copy(alpha = 0.5f),
            )
            NeonButton(
                text = "업적",
                onClick = { onNavigate(Routes.ACHIEVEMENTS) },
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                accentColor = NeonCyan,
                accentColorDark = NeonCyan.copy(alpha = 0.5f),
            )
            NeonButton(
                text = "시즌패스",
                onClick = { onNavigate(Routes.SEASON_PASS) },
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                accentColor = NeonCyan,
                accentColorDark = NeonCyan.copy(alpha = 0.5f),
            )
            NeonButton(
                text = "도감",
                onClick = { onNavigate(Routes.UNIT_CODEX) },
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                accentColor = Gold,
                accentColorDark = Gold.copy(alpha = 0.5f),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Stamina bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "\u26A1 ${data.stamina}/${data.maxStamina}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = StaminaGreen,
            )
            Spacer(modifier = Modifier.height(4.dp))
            NeonProgressBar(
                progress = data.stamina.toFloat() / data.maxStamina.coerceAtLeast(1),
                barColor = StaminaGreen,
                height = 6.dp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Battle button
        run {
            val stage = STAGES.getOrNull(data.currentStageId) ?: STAGES[0]
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
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                fontSize = 16.sp,
                accentColor = NeonRed,
                accentColorDark = NeonRedDark,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
