package com.example.jaygame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.GameData
import com.example.jaygame.data.GameRepository
import com.example.jaygame.ui.components.MedievalButton
import com.example.jaygame.ui.components.ScreenHeader
import com.example.jaygame.ui.components.WoodFrame
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NegativeRed
import com.example.jaygame.ui.theme.PositiveGreen
import com.example.jaygame.ui.theme.SubText

@Composable
fun SettingsScreen(
    repository: GameRepository,
    onBack: () -> Unit,
) {
    val data by repository.gameData.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDark)
            .padding(16.dp),
    ) {
        ScreenHeader(title = "설정", onBack = onBack)

        Spacer(Modifier.height(24.dp))

        WoodFrame(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Sound toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "\uD83D\uDD0A 사운드",
                        fontSize = 18.sp,
                        color = LightText,
                    )
                    Spacer(Modifier.weight(1f))
                    MedievalButton(
                        text = if (data.soundEnabled) "ON" else "OFF",
                        onClick = {
                            repository.save(data.copy(soundEnabled = !data.soundEnabled))
                        },
                        baseColor = if (data.soundEnabled) PositiveGreen else NegativeRed,
                        modifier = Modifier
                            .width(80.dp)
                            .height(36.dp),
                        fontSize = 14.sp,
                    )
                }

                // Music toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "\uD83C\uDFB5 음악",
                        fontSize = 18.sp,
                        color = LightText,
                    )
                    Spacer(Modifier.weight(1f))
                    MedievalButton(
                        text = if (data.musicEnabled) "ON" else "OFF",
                        onClick = {
                            repository.save(data.copy(musicEnabled = !data.musicEnabled))
                        },
                        baseColor = if (data.musicEnabled) PositiveGreen else NegativeRed,
                        modifier = Modifier
                            .width(80.dp)
                            .height(36.dp),
                        fontSize = 14.sp,
                    )
                }

                HorizontalDivider(color = Gold.copy(alpha = 0.3f))

                // Version
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "\uD83D\uDCF1 버전",
                        fontSize = 18.sp,
                        color = LightText,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "v0.4.0",
                        fontSize = 16.sp,
                        color = SubText,
                    )
                }

                HorizontalDivider(color = Gold.copy(alpha = 0.3f))

                // Data reset button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    MedievalButton(
                        text = "\uD83D\uDD04 데이터 초기화",
                        onClick = { showResetDialog = true },
                        baseColor = NegativeRed,
                        fontSize = 16.sp,
                    )
                }
            }
        }
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
                MedievalButton(
                    text = "초기화",
                    onClick = {
                        repository.save(GameData())
                        showResetDialog = false
                    },
                    baseColor = NegativeRed,
                    fontSize = 14.sp,
                )
            },
            dismissButton = {
                MedievalButton(
                    text = "취소",
                    onClick = { showResetDialog = false },
                    fontSize = 14.sp,
                )
            },
        )
    }
}
