package com.example.jaygame.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.data.GameData
import com.example.jaygame.data.GameRepository
import com.example.jaygame.navigation.Routes
import com.example.jaygame.ui.components.DailyLoginDialog
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

private enum class SettingsPage { MAIN, AUDIO, GAMEPLAY, DATA }

@Composable
fun SettingsScreen(
    repository: GameRepository,
    onNavigate: (String) -> Unit,
) {
    val data by repository.gameData.collectAsState()
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDailyLogin by remember { mutableStateOf(false) }

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

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = DarkNavy,
            titleContentColor = Gold,
            textContentColor = LightText,
            title = { Text("데이터 초기화", fontWeight = FontWeight.Bold) },
            text = { Text("모든 게임 데이터가 초기화됩니다.\n이 작업은 되돌릴 수 없습니다.", fontSize = 14.sp) },
            confirmButton = {
                NeonButton(
                    text = "초기화",
                    onClick = { repository.save(GameData()); showResetDialog = false },
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDark)
            .padding(16.dp),
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState == SettingsPage.MAIN) {
                    (fadeIn() + slideInHorizontally { -it / 3 })
                        .togetherWith(fadeOut() + slideOutHorizontally { it / 3 })
                } else {
                    (fadeIn() + slideInHorizontally { it / 3 })
                        .togetherWith(fadeOut() + slideOutHorizontally { -it / 3 })
                }
            },
            label = "settings_page",
        ) { page ->
            when (page) {
                SettingsPage.MAIN -> SettingsMain(
                    onPageSelected = { currentPage = it },
                    onShowDailyLogin = { showDailyLogin = true },
                    onNavigate = onNavigate,
                )
                SettingsPage.AUDIO -> SettingsAudio(
                    data = data,
                    onBack = { currentPage = SettingsPage.MAIN },
                    onToggleSound = { repository.save(data.copy(soundEnabled = !data.soundEnabled)) },
                    onToggleMusic = { repository.save(data.copy(musicEnabled = !data.musicEnabled)) },
                )
                SettingsPage.GAMEPLAY -> SettingsGameplay(
                    data = data,
                    onBack = { currentPage = SettingsPage.MAIN },
                    onSelectDifficulty = { repository.save(data.copy(difficulty = it)) },
                )
                SettingsPage.DATA -> SettingsData(
                    onBack = { currentPage = SettingsPage.MAIN },
                    onReset = { showResetDialog = true },
                )
            }
        }
    }
}

// ── Settings Main: Category List ──

@Composable
private fun SettingsMain(
    onPageSelected: (SettingsPage) -> Unit,
    onShowDailyLogin: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    Column {
        Text(
            text = "설정",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Gold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GameCard(modifier = Modifier.fillMaxWidth()) {
                SettingsCategoryRow(
                    iconRes = R.drawable.ic_settings_audio,
                    title = "오디오",
                    iconTint = NeonCyan,
                    onClick = { onPageSelected(SettingsPage.AUDIO) },
                )
            }
            GameCard(modifier = Modifier.fillMaxWidth()) {
                SettingsCategoryRow(
                    iconRes = R.drawable.ic_settings_gameplay,
                    title = "게임플레이",
                    iconTint = NeonCyan,
                    onClick = { onPageSelected(SettingsPage.GAMEPLAY) },
                )
            }
            GameCard(modifier = Modifier.fillMaxWidth()) {
                SettingsCategoryRow(
                    iconRes = R.drawable.ic_settings_reward,
                    title = "주간보상",
                    iconTint = Gold,
                    onClick = onShowDailyLogin,
                )
            }
            GameCard(modifier = Modifier.fillMaxWidth()) {
                SettingsCategoryRow(
                    iconRes = R.drawable.ic_achievement,
                    title = "업적",
                    iconTint = Gold,
                    onClick = { onNavigate(Routes.ACHIEVEMENTS) },
                )
            }
            GameCard(modifier = Modifier.fillMaxWidth()) {
                SettingsCategoryRow(
                    iconRes = R.drawable.ic_nav_collection,
                    title = "영웅 도감",
                    iconTint = Gold,
                    onClick = { onNavigate(Routes.UNIT_CODEX) },
                )
            }
            GameCard(modifier = Modifier.fillMaxWidth()) {
                SettingsCategoryRow(
                    iconRes = R.drawable.ic_settings_data,
                    title = "데이터 관리",
                    iconTint = NeonRed,
                    onClick = { onPageSelected(SettingsPage.DATA) },
                )
            }
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    iconRes: Int,
    title: String,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = LightText,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = DimText,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ── Sub-page Header ──

@Composable
private fun SubPageHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_back),
            contentDescription = "뒤로",
            tint = LightText,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBack),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Gold,
        )
    }
}

// ── Audio Sub-page ──

@Composable
private fun SettingsAudio(
    data: com.example.jaygame.data.GameData,
    onBack: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
) {
    Column {
        SubPageHeader(title = "오디오", onBack = onBack)
        GameCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("사운드", fontSize = 14.sp, color = LightText)
                    Spacer(Modifier.weight(1f))
                    NeonButton(
                        text = if (data.soundEnabled) "ON" else "OFF",
                        onClick = onToggleSound,
                        accentColor = if (data.soundEnabled) NeonGreen else NeonRed,
                        accentColorDark = if (data.soundEnabled) NeonGreen.copy(alpha = 0.6f) else NeonRedDark,
                        modifier = Modifier
                            .width(72.dp)
                            .height(34.dp),
                        fontSize = 13.sp,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("음악", fontSize = 14.sp, color = LightText)
                    Spacer(Modifier.weight(1f))
                    NeonButton(
                        text = if (data.musicEnabled) "ON" else "OFF",
                        onClick = onToggleMusic,
                        accentColor = if (data.musicEnabled) NeonGreen else NeonRed,
                        accentColorDark = if (data.musicEnabled) NeonGreen.copy(alpha = 0.6f) else NeonRedDark,
                        modifier = Modifier
                            .width(72.dp)
                            .height(34.dp),
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

// ── Gameplay Sub-page ──

@Composable
private fun SettingsGameplay(
    data: com.example.jaygame.data.GameData,
    onBack: () -> Unit,
    onSelectDifficulty: (Int) -> Unit,
) {
    val options = listOf(
        Triple(0, "초보", "기본 난이도"),
        Triple(1, "숙련자", "적 체력 ×1.5"),
        Triple(2, "고인물", "적 체력 ×2.2"),
        Triple(3, "썩은물", "적 체력 ×3.0"),
        Triple(4, "챌린저", "적 체력 ×4.0"),
    )
    Column {
        SubPageHeader(title = "게임플레이", onBack = onBack)
        GameCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("난이도", fontSize = 14.sp, color = LightText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                options.forEach { (id, name, desc) ->
                    val isSelected = id == data.difficulty
                    NeonButton(
                        text = "$name — $desc",
                        onClick = { onSelectDifficulty(id) },
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 13.sp,
                        accentColor = if (isSelected) NeonCyan else SubText,
                        accentColorDark = if (isSelected) NeonCyan.copy(alpha = 0.5f) else SubText.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

// ── Data Sub-page ──

@Composable
private fun SettingsData(
    onBack: () -> Unit,
    onReset: () -> Unit,
) {
    Column {
        SubPageHeader(title = "데이터 관리", onBack = onBack)
        GameCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("버전", fontSize = 14.sp, color = LightText)
                    Spacer(Modifier.weight(1f))
                    Text("v0.5.0", fontSize = 14.sp, color = SubText)
                }
                HorizontalDivider(color = Divider)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    NeonButton(
                        text = "데이터 초기화",
                        onClick = onReset,
                        accentColor = NeonRed,
                        accentColorDark = NeonRedDark,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
