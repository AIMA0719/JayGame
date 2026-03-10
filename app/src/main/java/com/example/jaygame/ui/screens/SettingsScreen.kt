package com.example.jaygame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.GameData
import com.example.jaygame.data.GameRepository
import com.example.jaygame.navigation.Routes
import com.example.jaygame.ui.components.DailyLoginDialog
import com.example.jaygame.ui.components.DifficultyDialog
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.components.canClaim
import com.example.jaygame.ui.components.claimReward
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DimText
import com.example.jaygame.ui.theme.Divider
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.NeonGreen
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark
import com.example.jaygame.ui.theme.SubText

@Composable
fun SettingsScreen(
    repository: GameRepository,
    onNavigate: (String) -> Unit,
) {
    val data by repository.gameData.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showDailyLogin by remember { mutableStateOf(false) }
    var showDifficulty by remember { mutableStateOf(false) }

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
            .background(DeepDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // Title
        Text(
            text = "설정",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Gold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        // ── Quick Access Section ──
        Text(
            text = "게임 메뉴",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = SubText,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // Row 1: 주간보상 + 난이도
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NeonButton(
                text = "주간보상",
                onClick = { showDailyLogin = true },
                modifier = Modifier.weight(1f).height(42.dp),
                fontSize = 13.sp,
                accentColor = NeonCyan,
                accentColorDark = NeonCyan.copy(alpha = 0.5f),
            )
            NeonButton(
                text = "난이도",
                onClick = { showDifficulty = true },
                modifier = Modifier.weight(1f).height(42.dp),
                fontSize = 13.sp,
                accentColor = NeonCyan,
                accentColorDark = NeonCyan.copy(alpha = 0.5f),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Row 2: 업적 + 도감
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NeonButton(
                text = "업적",
                onClick = { onNavigate(Routes.ACHIEVEMENTS) },
                modifier = Modifier.weight(1f).height(42.dp),
                fontSize = 13.sp,
                accentColor = Gold,
                accentColorDark = Gold.copy(alpha = 0.5f),
            )
            NeonButton(
                text = "영웅 도감",
                onClick = { onNavigate(Routes.UNIT_CODEX) },
                modifier = Modifier.weight(1f).height(42.dp),
                fontSize = 13.sp,
                accentColor = Gold,
                accentColorDark = Gold.copy(alpha = 0.5f),
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Settings Section ──
        Text(
            text = "환경설정",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = SubText,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        GameCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Sound toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "사운드",
                        fontSize = 14.sp,
                        color = LightText,
                    )
                    Spacer(Modifier.weight(1f))
                    NeonButton(
                        text = if (data.soundEnabled) "ON" else "OFF",
                        onClick = {
                            repository.save(data.copy(soundEnabled = !data.soundEnabled))
                        },
                        accentColor = if (data.soundEnabled) NeonGreen else NeonRed,
                        accentColorDark = if (data.soundEnabled) NeonGreen.copy(alpha = 0.6f) else NeonRedDark,
                        modifier = Modifier
                            .width(72.dp)
                            .height(34.dp),
                        fontSize = 13.sp,
                    )
                }

                // Music toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "음악",
                        fontSize = 14.sp,
                        color = LightText,
                    )
                    Spacer(Modifier.weight(1f))
                    NeonButton(
                        text = if (data.musicEnabled) "ON" else "OFF",
                        onClick = {
                            repository.save(data.copy(musicEnabled = !data.musicEnabled))
                        },
                        accentColor = if (data.musicEnabled) NeonGreen else NeonRed,
                        accentColorDark = if (data.musicEnabled) NeonGreen.copy(alpha = 0.6f) else NeonRedDark,
                        modifier = Modifier
                            .width(72.dp)
                            .height(34.dp),
                        fontSize = 13.sp,
                    )
                }

                HorizontalDivider(color = Divider)

                // Version
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "버전",
                        fontSize = 14.sp,
                        color = LightText,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "v0.5.0",
                        fontSize = 14.sp,
                        color = SubText,
                    )
                }

                HorizontalDivider(color = Divider)

                // Data reset button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    NeonButton(
                        text = "데이터 초기화",
                        onClick = { showResetDialog = true },
                        accentColor = NeonRed,
                        accentColorDark = NeonRedDark,
                        fontSize = 14.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = DarkNavy,
            titleContentColor = Gold,
            textContentColor = LightText,
            title = {
                Text(
                    text = "데이터 초기화",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "모든 게임 데이터가 초기화됩니다.\n이 작업은 되돌릴 수 없습니다.",
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                NeonButton(
                    text = "초기화",
                    onClick = {
                        repository.save(GameData())
                        showResetDialog = false
                    },
                    accentColor = NeonRed,
                    accentColorDark = NeonRedDark,
                    fontSize = 13.sp,
                )
            },
            dismissButton = {
                NeonButton(
                    text = "취소",
                    onClick = { showResetDialog = false },
                    accentColor = SubText,
                    accentColorDark = SubText.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                )
            },
        )
    }
}
