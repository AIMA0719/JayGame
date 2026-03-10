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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.STAGES
import com.example.jaygame.data.StaminaManager
import com.example.jaygame.navigation.Routes
import com.example.jaygame.ui.components.DailyLoginDialog
import com.example.jaygame.ui.components.DifficultyDialog
import com.example.jaygame.ui.components.MedievalButton
import com.example.jaygame.ui.components.ProfileHeader
import com.example.jaygame.ui.components.RankBadge
import com.example.jaygame.ui.components.StageCardPager
import com.example.jaygame.ui.components.canClaim
import com.example.jaygame.ui.components.claimReward
import com.example.jaygame.ui.theme.DarkBrown
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.MetalGray

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
            .background(DarkBrown),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 프로필 헤더
        ProfileHeader(
            playerName = "플레이어",
            level = data.playerLevel,
            gold = data.gold,
            diamonds = data.diamonds,
            stamina = data.stamina,
            maxStamina = data.maxStamina,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 스테이지 카드 (스와이프)
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

        Spacer(modifier = Modifier.height(16.dp))

        // 퀵 버튼 행
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MedievalButton(
                text = "주간보상",
                onClick = { showDailyLogin = true },
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
            )
            MedievalButton(
                text = "난이도",
                onClick = { showDifficulty = true },
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
            )
            MedievalButton(
                text = "업적",
                onClick = { onNavigate(Routes.ACHIEVEMENTS) },
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
            )
            MedievalButton(
                text = "시즌패스",
                onClick = { onNavigate(Routes.SEASON_PASS) },
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 전투 시작 버튼
        run {
            val stage = STAGES.getOrNull(data.currentStageId) ?: STAGES[0]
            val isUnlocked = data.currentStageId in data.unlockedStages
            val hasStamina = data.stamina >= stage.staminaCost
            val canStart = isUnlocked && hasStamina

            MedievalButton(
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
                    .padding(horizontal = 32.dp)
                    .height(60.dp),
                fontSize = 20.sp,
                accentColor = if (canStart) Gold else MetalGray,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 랭크 뱃지 + 설정
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankBadge(trophies = data.trophies)

            MedievalButton(
                text = "설정",
                onClick = { onNavigate(Routes.SETTINGS) },
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
