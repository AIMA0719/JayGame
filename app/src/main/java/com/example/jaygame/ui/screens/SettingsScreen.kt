package com.example.jaygame.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.jaygame.ui.viewmodel.SettingsViewModel
import org.orbitmvi.orbit.compose.collectAsState
import com.example.jaygame.navigation.Routes
import com.example.jaygame.ui.components.DailyLoginDialog
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.components.NeonProgressBar
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
import androidx.compose.ui.platform.LocalContext

private enum class SettingsPage { MAIN, AUDIO, GAMEPLAY, UPGRADE, DATA, PROFILE, FAQ, PRIVACY, LICENSES }

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (String) -> Unit,
) {
    val settingsState by viewModel.collectAsState()
    val data = settingsState.gameData
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }

    // System back → return to settings main when on sub-page
    androidx.activity.compose.BackHandler(enabled = currentPage != SettingsPage.MAIN) {
        currentPage = SettingsPage.MAIN
    }

    if (settingsState.showDailyLogin) {
        DailyLoginDialog(
            data = data,
            onClaim = { viewModel.claimDailyLogin() },
            onDismiss = { viewModel.dismissDailyLogin() },
        )
    }

    if (settingsState.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResetDialog() },
            containerColor = DarkNavy,
            titleContentColor = Gold,
            textContentColor = LightText,
            title = { Text("데이터 초기화", fontWeight = FontWeight.Bold) },
            text = { Text("모든 게임 데이터가 초기화됩니다.\n이 작업은 되돌릴 수 없습니다.", fontSize = 14.sp) },
            confirmButton = {
                NeonButton(
                    text = "초기화",
                    onClick = { viewModel.resetData() },
                    accentColor = NeonRed,
                    accentColorDark = NeonRedDark,
                    fontSize = 13.sp,
                )
            },
            dismissButton = {
                NeonButton(
                    text = "취소",
                    onClick = { viewModel.dismissResetDialog() },
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
                    onShowDailyLogin = { viewModel.showDailyLogin() },
                    onNavigate = onNavigate,
                )
                SettingsPage.AUDIO -> SettingsAudio(
                    data = data,
                    onBack = { currentPage = SettingsPage.MAIN },
                    onToggleSound = { viewModel.toggleSound() },
                    onToggleMusic = { viewModel.toggleMusic() },
                    onToggleHaptic = { viewModel.toggleHaptic() },
                )
                SettingsPage.GAMEPLAY -> SettingsGameplay(
                    data = data,
                    onBack = { currentPage = SettingsPage.MAIN },
                    onUpdate = { viewModel.updateGameplay(it) },
                )
                SettingsPage.UPGRADE -> SettingsUpgrade(
                    data = data,
                    onBack = { currentPage = SettingsPage.MAIN },
                    onUpdate = { viewModel.updateGameplay(it) },
                )
                SettingsPage.DATA -> SettingsData(
                    onBack = { currentPage = SettingsPage.MAIN },
                    onReset = { viewModel.showResetDialog() },
                )
                SettingsPage.PROFILE -> SettingsProfile(
                    onBack = { currentPage = SettingsPage.MAIN },
                )
                SettingsPage.FAQ -> SettingsFaq(
                    onBack = { currentPage = SettingsPage.MAIN },
                )
                SettingsPage.PRIVACY -> SettingsPrivacy(
                    onBack = { currentPage = SettingsPage.MAIN },
                )
                SettingsPage.LICENSES -> SettingsLicenses(
                    onBack = { currentPage = SettingsPage.MAIN },
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
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
            // 강화 탭 숨김 처리
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
                    iconRes = R.drawable.ic_achievement,
                    title = "프로필 칭호",
                    iconTint = Gold,
                    onClick = { onPageSelected(SettingsPage.PROFILE) },
                )
            }
            GameCard(modifier = Modifier.fillMaxWidth()) {
                SettingsCategoryRow(
                    iconRes = R.drawable.ic_settings_gameplay,
                    title = "게임 도움말",
                    iconTint = NeonCyan,
                    onClick = { onPageSelected(SettingsPage.FAQ) },
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
            GameCard(modifier = Modifier.fillMaxWidth()) {
                SettingsCategoryRow(
                    iconRes = R.drawable.ic_settings_privacy,
                    title = "개인정보 처리방침",
                    iconTint = SubText,
                    onClick = { onPageSelected(SettingsPage.PRIVACY) },
                )
            }
            GameCard(modifier = Modifier.fillMaxWidth()) {
                SettingsCategoryRow(
                    iconRes = R.drawable.ic_settings_license,
                    title = "오픈소스 라이선스",
                    iconTint = SubText,
                    onClick = { onPageSelected(SettingsPage.LICENSES) },
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
    onToggleHaptic: () -> Unit,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("진동", fontSize = 14.sp, color = LightText)
                    Spacer(Modifier.weight(1f))
                    NeonButton(
                        text = if (data.hapticEnabled) "ON" else "OFF",
                        onClick = onToggleHaptic,
                        accentColor = if (data.hapticEnabled) NeonGreen else NeonRed,
                        accentColorDark = if (data.hapticEnabled) NeonGreen.copy(alpha = 0.6f) else NeonRedDark,
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
    onUpdate: (com.example.jaygame.data.GameData) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SubPageHeader(title = "게임플레이", onBack = onBack)

        // ── 기본 배속 ──
        GameCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("기본 배속", fontSize = 14.sp, color = LightText, fontWeight = FontWeight.Bold)
                Text("전투 시작 시 적용되는 기본 속도", fontSize = 11.sp, color = SubText)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(2f to "x1", 4f to "x2", 8f to "x4").forEach { (speed, label) ->
                        val isSelected = data.defaultBattleSpeed == speed
                        val color = when (speed) {
                            4f -> Gold
                            8f -> NeonRed
                            else -> LightText
                        }
                        NeonButton(
                            text = label,
                            onClick = { onUpdate(data.copy(defaultBattleSpeed = speed)) },
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            accentColor = if (isSelected) color else SubText,
                            accentColorDark = if (isSelected) color.copy(alpha = 0.5f) else SubText.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        // ── 데미지 숫자 표시 ──
        GameCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("데미지 숫자 표시", fontSize = 14.sp, color = LightText)
                    Text("피해량 팝업 숫자 표시", fontSize = 11.sp, color = SubText)
                }
                NeonButton(
                    text = if (data.showDamageNumbers) "ON" else "OFF",
                    onClick = { onUpdate(data.copy(showDamageNumbers = !data.showDamageNumbers)) },
                    accentColor = if (data.showDamageNumbers) NeonGreen else NeonRed,
                    accentColorDark = if (data.showDamageNumbers) NeonGreen.copy(alpha = 0.6f) else NeonRedDark,
                    modifier = Modifier
                        .width(72.dp)
                        .height(34.dp),
                    fontSize = 13.sp,
                )
            }
        }

        // ── 체력바 표시 ──
        GameCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("체력바 표시", fontSize = 14.sp, color = LightText, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(0 to "항상", 1 to "피격 시", 2 to "숨김").forEach { (mode, label) ->
                        val isSelected = data.healthBarMode == mode
                        NeonButton(
                            text = label,
                            onClick = { onUpdate(data.copy(healthBarMode = mode)) },
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            accentColor = if (isSelected) NeonCyan else SubText,
                            accentColorDark = if (isSelected) NeonCyan.copy(alpha = 0.5f) else SubText.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Upgrade Sub-page ──

private const val FAMILY_UPGRADE_MAX_LEVEL = 10
private const val FAMILY_UPGRADE_PERCENT_PER_LEVEL = 0.1f // +0.1% per level

@Composable
private fun SettingsUpgrade(
    data: com.example.jaygame.data.GameData,
    onBack: () -> Unit,
    onUpdate: (com.example.jaygame.data.GameData) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SubPageHeader(title = "강화", onBack = onBack)

        // ── 설명 ──
        GameCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("가족 강화", fontSize = 14.sp, color = Gold, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "골드를 소비하여 각 가족 유닛의 공격력을 영구적으로 강화합니다.\n레벨당 +0.1%, 최대 Lv.10 (+1.0%)",
                    fontSize = 11.sp,
                    color = SubText,
                    lineHeight = 16.sp,
                )
            }
        }

        // ── 가족별 강화 카드 ──
        val families = com.example.jaygame.data.UnitFamily.entries
        families.forEach { family ->
            val key = family.name
            val level = data.familyUpgrades[key] ?: 0
            val isMaxLevel = level >= FAMILY_UPGRADE_MAX_LEVEL
            val cost = 500 + level * 500 // 500, 1000, 1500, ... 5500
            val bonusPercent = "%.1f".format(level * FAMILY_UPGRADE_PERCENT_PER_LEVEL)
            val canAfford = data.gold >= cost && !isMaxLevel

            GameCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isMaxLevel) Gold.copy(alpha = 0.5f) else family.color.copy(alpha = 0.3f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Family name + color indicator
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = family.label,
                            color = family.color,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isMaxLevel) "MAX (+${bonusPercent}%)" else "Lv.$level (+${bonusPercent}%)",
                            color = if (isMaxLevel) Gold else SubText,
                            fontSize = 12.sp,
                        )
                        // Progress bar
                        Spacer(Modifier.height(4.dp))
                        NeonProgressBar(
                            progress = level.toFloat() / FAMILY_UPGRADE_MAX_LEVEL,
                            barColor = family.color,
                            height = 6.dp,
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Upgrade button
                    if (isMaxLevel) {
                        Text(
                            text = "완료",
                            color = Gold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        NeonButton(
                            text = "${cost}G",
                            onClick = {
                                if (canAfford) {
                                    val newUpgrades = data.familyUpgrades.toMutableMap()
                                    newUpgrades[key] = level + 1
                                    onUpdate(data.copy(
                                        gold = data.gold - cost,
                                        familyUpgrades = newUpgrades,
                                    ))
                                }
                            },
                            enabled = canAfford,
                            modifier = Modifier
                                .width(80.dp)
                                .height(34.dp),
                            fontSize = 12.sp,
                            accentColor = if (canAfford) NeonGreen else DimText,
                            accentColorDark = if (canAfford) NeonGreen.copy(alpha = 0.5f) else DimText.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Profile Sub-page ──

@Composable
private fun SettingsProfile(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as com.example.jaygame.JayGameApplication).repository
    Column(modifier = Modifier.fillMaxSize()) {
        SubPageHeader(title = "프로필 칭호", onBack = onBack)
        ProfileScreen(
            repository = repository,
            onBack = onBack,
            showTopBar = false,
        )
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
                    Text("v1.0.0", fontSize = 14.sp, color = SubText)
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

// ── Privacy Policy Sub-page ──

@Composable
private fun SettingsPrivacy(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SubPageHeader(title = "개인정보 처리방침", onBack = onBack)

        GameCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrivacySection(
                    title = "1. 수집하는 개인정보",
                    body = "본 앱은 별도의 개인정보를 수집하지 않습니다. 게임 진행 데이터(스테이지 진행도, 유닛 보유 현황, 재화 등)는 사용자의 기기에만 저장되며, 외부 서버로 전송되지 않습니다.",
                )
                HorizontalDivider(color = Divider)
                PrivacySection(
                    title = "2. 개인정보의 이용 목적",
                    body = "기기에 저장되는 게임 데이터는 오직 게임 플레이 진행 상태를 유지하기 위한 목적으로만 사용됩니다.",
                )
                HorizontalDivider(color = Divider)
                PrivacySection(
                    title = "3. 개인정보의 보관 및 파기",
                    body = "모든 데이터는 사용자 기기의 로컬 저장소에 보관됩니다. 앱을 삭제하거나 설정에서 데이터를 초기화하면 모든 정보가 영구적으로 삭제됩니다.",
                )
                HorizontalDivider(color = Divider)
                PrivacySection(
                    title = "4. 제3자 제공",
                    body = "본 앱은 어떠한 개인정보도 제3자에게 제공하지 않습니다.",
                )
                HorizontalDivider(color = Divider)
                PrivacySection(
                    title = "5. 광고 및 분석",
                    body = "현재 본 앱은 광고 SDK나 분석 도구를 사용하지 않습니다. 향후 도입 시 본 방침을 업데이트하고 사용자에게 고지합니다.",
                )
                HorizontalDivider(color = Divider)
                PrivacySection(
                    title = "6. 문의",
                    body = "개인정보 처리방침에 대한 문의 사항이 있으시면 앱 스토어 페이지를 통해 연락해 주시기 바랍니다.",
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 14.sp, color = Gold, fontWeight = FontWeight.Bold)
        Text(body, fontSize = 12.sp, color = LightText, lineHeight = 18.sp)
    }
}

// ── Open Source Licenses Sub-page ──

@Composable
private fun SettingsLicenses(onBack: () -> Unit) {
    val context = LocalContext.current
    val creditsText = remember {
        try {
            context.assets.open("CREDITS.txt").bufferedReader().readText()
        } catch (_: Exception) {
            ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SubPageHeader(title = "오픈소스 라이선스", onBack = onBack)

        // Audio credits from CREDITS.txt
        if (creditsText.isNotBlank()) {
            GameCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("오디오 에셋", fontSize = 14.sp, color = Gold, fontWeight = FontWeight.Bold)
                    Text(creditsText, fontSize = 11.sp, color = LightText, lineHeight = 16.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Third-party libraries
        GameCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("사용된 라이브러리", fontSize = 14.sp, color = Gold, fontWeight = FontWeight.Bold)

                LicenseItem(
                    name = "Jetpack Compose",
                    license = "Apache License 2.0",
                    copyright = "Copyright Google LLC",
                )
                HorizontalDivider(color = Divider)
                LicenseItem(
                    name = "AndroidX Core / AppCompat / Lifecycle",
                    license = "Apache License 2.0",
                    copyright = "Copyright Google LLC",
                )
                HorizontalDivider(color = Divider)
                LicenseItem(
                    name = "Material Components for Android",
                    license = "Apache License 2.0",
                    copyright = "Copyright Google LLC",
                )
                HorizontalDivider(color = Divider)
                LicenseItem(
                    name = "Navigation Compose",
                    license = "Apache License 2.0",
                    copyright = "Copyright Google LLC",
                )
                HorizontalDivider(color = Divider)
                LicenseItem(
                    name = "Coil (Image Loading)",
                    license = "Apache License 2.0",
                    copyright = "Copyright Coil Contributors",
                )
                HorizontalDivider(color = Divider)
                LicenseItem(
                    name = "Kotlin / Kotlinx Coroutines",
                    license = "Apache License 2.0",
                    copyright = "Copyright JetBrains s.r.o.",
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LicenseItem(name: String, license: String, copyright: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(name, fontSize = 13.sp, color = LightText, fontWeight = FontWeight.Medium)
        Text(copyright, fontSize = 11.sp, color = SubText)
        Text(license, fontSize = 11.sp, color = DimText)
    }
}

// ── FAQ / Game Guide Sub-page ──

@Composable
private fun SettingsFaq(onBack: () -> Unit) {
    var expandedSection by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SubPageHeader(title = "게임 도움말", onBack = onBack)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // ── 전투 기본 ──
            FaqSection(
                index = 0,
                expanded = expandedSection == 0,
                onToggle = { expandedSection = if (expandedSection == 0) -1 else 0 },
                icon = "\u2694\uFE0F",
                title = "전투는 어떻게 진행되나요?",
            ) {
                FaqBullet("유닛을 소환(코인 소모)하여 밀려오는 적을 처치합니다")
                FaqBullet("3×6 그리드(18슬롯)에 유닛을 배치합니다")
                FaqBullet("적이 필드에 100마리 이상 쌓이면 패배합니다")
                FaqBullet("10웨이브마다 보스가 등장하며, 시간 내 처치하지 못하면 패배합니다")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("코인 획득")
                FaqBullet("적 처치: 2코인 / 엘리트 처치: 6코인")
                FaqBullet("웨이브 클리어: 15 + 웨이브×2.5 코인")
                FaqBullet("유닛 판매: 8 + 등급×8 코인")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("난이도")
                FaqKeyValue("일반", "적 1.0배")
                FaqKeyValue("하드", "적 1.5배")
                FaqKeyValue("헬", "적 2.2배")
            }

            // ── 소환 & 등급 ──
            FaqSection(
                index = 1,
                expanded = expandedSection == 1,
                onToggle = { expandedSection = if (expandedSection == 1) -1 else 1 },
                icon = "\u2728",
                title = "유닛 소환은 어떻게 되나요?",
            ) {
                FaqBullet("코인을 소모하여 유닛을 소환합니다 (시작 10, 소환마다 +5, 최대 60)")
                FaqBullet("유물 '소환사의 오브'로 비용을 최대 50%까지 줄일 수 있습니다")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("소환 확률")
                FaqKeyValue("일반", "60%")
                FaqKeyValue("희귀", "25%")
                FaqKeyValue("영웅", "12%")
                FaqKeyValue("전설", "3%")
                FaqBullet("신화 등급은 레시피 조합으로만 획득 가능합니다")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("천장 시스템")
                FaqBullet("30회 이후 영웅 확률 2배 (24%), 영웅 이상 획득 시 초기화")
                FaqBullet("100회 소환 시 전설 확정")
            }

            // ── 합성 ──
            FaqSection(
                index = 2,
                expanded = expandedSection == 2,
                onToggle = { expandedSection = if (expandedSection == 2) -1 else 2 },
                icon = "\uD83D\uDD2E",
                title = "합성은 어떻게 하나요?",
            ) {
                FaqBullet("같은 유닛을 드래그하여 한 슬롯에 중첩합니다 (최대 3개)")
                FaqBullet("3개 중첩 시 자동 합성 → 상위 등급 유닛 1개")
                FaqBullet("전설이 일반 합성 최상위 (신화는 레시피 전용)")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("행운 합성")
                FaqBullet("5% 확률로 한 등급을 건너뜁니다 (예: 일반 → 영웅)")
                FaqBullet("유물 '합성의 돌'로 최대 +15% 추가 확률")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("신화 레시피")
                FaqBullet("특정 영웅 유닛 조합으로 신화 유닛을 만들 수 있습니다")
                FaqBullet("필드에 레시피 재료가 모이면 자동 감지됩니다")
                FaqBullet("발견한 조합은 도감에 기록됩니다")
            }

            // ── 유닛 강화 ──
            FaqSection(
                index = 3,
                expanded = expandedSection == 3,
                onToggle = { expandedSection = if (expandedSection == 3) -1 else 3 },
                icon = "\u2B06\uFE0F",
                title = "유닛 강화는 어떻게 하나요?",
            ) {
                FaqBullet("전투 중 강화 버튼을 눌러 등급 그룹별 통합 강화를 할 수 있습니다")
                FaqBullet("같은 그룹의 모든 유닛에 동일한 보너스가 적용됩니다")
                FaqBullet("강화당 기본 ATK의 50% 증가 (최대 Lv.15)")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("강화 그룹 (코인, 지수 증가)")
                FaqKeyValue("일반/희귀", "10코인~ (×1.18/Lv)")
                FaqKeyValue("영웅/전설", "20코인~ (×1.22/Lv)")
                FaqKeyValue("신화", "40코인~ (×1.24/Lv)")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("마일스톤 보너스")
                FaqKeyValue("Lv.3", "ATK +10%")
                FaqKeyValue("Lv.6", "ATK +15%")
                FaqKeyValue("Lv.9", "공격속도 +10%")
                FaqKeyValue("Lv.12", "ATK +15%")
                FaqKeyValue("Lv.15", "ATK +10% + 공속 +10%")
            }

            // ── 시너지 ──
            FaqSection(
                index = 4,
                expanded = expandedSection == 4,
                onToggle = { expandedSection = if (expandedSection == 4) -1 else 4 },
                icon = "\uD83D\uDD25",
                title = "패밀리 시너지란?",
            ) {
                FaqBullet("같은 패밀리 유닛을 3개 이상 배치하면 시너지가 발동됩니다")
                FaqBullet("4개 이상이면 강화 시너지 + 특수 효과가 추가됩니다")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("패밀리별 보너스")
                FaqKeyValue("화염", "공격력 +8/15% · DoT 지속↑")
                FaqKeyValue("냉기", "공속 +6/12% · 둔화 +30%")
                FaqKeyValue("독", "공격력 +8/14% · 사망 시 독 전파")
                FaqKeyValue("번개", "공속 +8/15% · 체인 +1")
                FaqKeyValue("보조", "사거리 +6/12%")
                FaqKeyValue("바람", "공격력 +6/10% · 사거리 +5/10%")
            }

            // ── 유닛 조작 ──
            FaqSection(
                index = 5,
                expanded = expandedSection == 5,
                onToggle = { expandedSection = if (expandedSection == 5) -1 else 5 },
                icon = "\uD83D\uDC49",
                title = "유닛을 어떻게 이동하나요?",
            ) {
                FaqBullet("유닛을 탭하면 선택됩니다 (파란 하이라이트)")
                FaqBullet("선택 후 빈 슬롯 또는 같은 유닛 슬롯을 탭하면 이동합니다")
                FaqBullet("같은 유닛이 있는 슬롯으로 이동하면 중첩됩니다 (최대 3개)")
                FaqBullet("이동 가능한 슬롯은 노란색으로 표시됩니다")
                FaqBullet("그리드 밖 또는 이동 불가 슬롯을 탭하면 선택이 해제됩니다")
            }

            // ── 역할 시너지 ──
            FaqSection(
                index = 6,
                expanded = expandedSection == 6,
                onToggle = { expandedSection = if (expandedSection == 6) -1 else 6 },
                icon = "\uD83D\uDEE1\uFE0F",
                title = "역할 시너지란?",
            ) {
                FaqBullet("같은 역할의 유닛을 2개 이상 배치하면 역할 시너지가 발동됩니다")
                FaqBullet("패밀리 시너지와 별도로 중복 적용됩니다")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("역할별 보너스 (2/3/4+ 개수)")
                FaqKeyValue("탱커", "블록 시간 +20% → +블록 수 +1 → 도발 효과")
                FaqKeyValue("근딜", "돌진 데미지 +15% → +쿨타임 -20% → 즉시 재돌진")
                FaqKeyValue("원딜", "사거리 +10% → +치명타 +5% → 관통 2회")
                FaqKeyValue("서포터", "버프 범위 +15% → 버프 중첩 → 전체 미니힐")
                FaqKeyValue("컨트롤러", "CC 확률 +10% → +CC 지속 +25% → CC 면역 관통")
            }

            // ── 웨이브 & 보스 ──
            FaqSection(
                index = 7,
                expanded = expandedSection == 7,
                onToggle = { expandedSection = if (expandedSection == 7) -1 else 7 },
                icon = "\uD83D\uDC79",
                title = "웨이브와 보스는 어떻게 되나요?",
            ) {
                FaqBullet("전 스테이지 60웨이브 고정입니다")
                FaqBullet("보스: 10, 20, 30 웨이브 (HP 10배), 40+ 웨이브 (HP 15배)")
                FaqBullet("미니보스: 5, 15, 25... 웨이브 (3마리, HP 5배/30+는 7배)")
                FaqBullet("엘리트: 웨이브 20+부터 랜덤 등장")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("엘리트 적 스탯")
                FaqBullet("HP 2배, 방어력 1.5배, 마법저항 1.3배")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("엘리트 출현 확률")
                FaqKeyValue("웨이브 20~29", "10%")
                FaqKeyValue("웨이브 30~44", "20%")
                FaqKeyValue("웨이브 45+", "30%")
            }

            // ── 데미지 공식 ──
            FaqSection(
                index = 8,
                expanded = expandedSection == 8,
                onToggle = { expandedSection = if (expandedSection == 8) -1 else 8 },
                icon = "\uD83D\uDCA5",
                title = "데미지는 어떻게 계산되나요?",
            ) {
                FaqBullet("유닛은 물리 또는 마법 피해를 줍니다")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("물리 데미지")
                FaqBullet("피해 × (100 / (100 + 방어력))")
                FaqBullet("방어력이 높을수록 감소하지만 완전 면역은 불가")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("마법 데미지")
                FaqBullet("피해 × (1 - 마법저항), 마법저항 최대 90%")
                FaqBullet("방어력을 무시하며, 마법저항으로만 감소")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("마나 & 궁극기")
                FaqBullet("전설: 공격 시 마나 +9, ~11회 공격으로 궁극기 발동")
                FaqBullet("신화: 공격 시 마나 +6, ~17회 공격으로 궁극기 발동")
                FaqBullet("마나 100 도달 시 자동 발동 → 마나 0으로 리셋")
            }

            // ── 유물 ──
            FaqSection(
                index = 9,
                expanded = expandedSection == 9,
                onToggle = { expandedSection = if (expandedSection == 9) -1 else 9 },
                icon = "\uD83D\uDC8E",
                title = "유물은 어떻게 얻나요?",
            ) {
                FaqBullet("전투 승리 시 10% 확률로 랜덤 유물을 획득합니다")
                FaqBullet("던전 '유물 사냥'에서는 50% 확률로 드롭됩니다")
                FaqBullet("중복 유물은 더 높은 등급이면 승급, 같거나 낮으면 골드로 변환됩니다")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("유물 등급 · 최대 레벨")
                FaqKeyValue("일반", "Lv.5")
                FaqKeyValue("희귀", "Lv.7")
                FaqKeyValue("영웅", "Lv.10")
                FaqKeyValue("전설", "Lv.15")
                FaqKeyValue("신화", "Lv.20")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("장착 슬롯 (트로피)")
                FaqKeyValue("~499", "1칸")
                FaqKeyValue("500+", "2칸")
                FaqKeyValue("1,500+", "3칸")
                FaqKeyValue("3,000+", "4칸")
            }

            // ── 도감 ──
            FaqSection(
                index = 10,
                expanded = expandedSection == 10,
                onToggle = { expandedSection = if (expandedSection == 10) -1 else 10 },
                icon = "\uD83D\uDCD6",
                title = "도감은 무엇인가요?",
            ) {
                FaqBullet("보유한 유닛의 카드와 레벨을 확인할 수 있습니다")
                FaqBullet("카드를 모아 유닛을 영구 강화(레벨업)할 수 있습니다")
                FaqBullet("레벨이 오르면 기본 공격력에 배율이 적용됩니다")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("레벨별 공격력 배율")
                FaqKeyValue("Lv.1", "×1.0")
                FaqKeyValue("Lv.2", "×1.5")
                FaqKeyValue("Lv.3", "×2.2")
                FaqKeyValue("Lv.4", "×3.2")
                FaqKeyValue("Lv.5", "×4.5")
                FaqKeyValue("Lv.6", "×6.0")
                FaqKeyValue("Lv.7", "×8.0")
            }

            // ── 펫 ──
            FaqSection(
                index = 11,
                expanded = expandedSection == 11,
                onToggle = { expandedSection = if (expandedSection == 11) -1 else 11 },
                icon = "\uD83D\uDC3E",
                title = "펫은 어떻게 얻나요?",
            ) {
                FaqBullet("상점에서 다이아 50개(1회) 또는 400개(10회)로 뽑기합니다")
                FaqBullet("10회 뽑기는 20% 할인됩니다")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("펫 등급 · 확률")
                FaqKeyValue("희귀", "60%")
                FaqKeyValue("영웅", "25%")
                FaqKeyValue("전설", "12%")
                FaqKeyValue("신화", "3%")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("천장")
                FaqBullet("50회 뽑기 시 전설 이상 확정")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("장착 슬롯")
                FaqKeyValue("~1,999 트로피", "1칸")
                FaqKeyValue("2,000+ 트로피", "2칸")
                FaqBullet("펫 카드를 모아 레벨업하면 스킬이 강화됩니다")
            }

            // ── 던전 ──
            FaqSection(
                index = 12,
                expanded = expandedSection == 12,
                onToggle = { expandedSection = if (expandedSection == 12) -1 else 12 },
                icon = "\uD83C\uDFF0",
                title = "던전은 어떻게 들어가나요?",
            ) {
                FaqBullet("하루 최대 3회 입장 가능 (매일 자정 초기화)")
                FaqBullet("각 던전마다 트로피 조건과 스태미나 비용이 있습니다")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("던전 종류")
                FaqKeyValue("골드 러시", "골드 3배 보상 · 트로피 0+")
                FaqKeyValue("유물 사냥", "유물 50% 드롭 · 트로피 500+")
                FaqKeyValue("펫 탐험", "펫 카드 보상 · 트로피 1,000+")
                FaqKeyValue("보스 러시", "보상 2배 · 트로피 1,500+")
                FaqKeyValue("서바이벌", "무한 웨이브 · 트로피 2,000+")
            }

            // ── 스태미나 ──
            FaqSection(
                index = 13,
                expanded = expandedSection == 13,
                onToggle = { expandedSection = if (expandedSection == 13) -1 else 13 },
                icon = "\u26A1",
                title = "스태미나는 어떻게 충전되나요?",
            ) {
                FaqBullet("최대 스태미나: 100")
                FaqBullet("5분마다 1씩 자동 회복됩니다")
                FaqBullet("완전 충전까지 약 8시간 20분 소요")
                FaqBullet("상점에서 다이아 30개로 50 스태미나를 충전할 수 있습니다")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("스테이지별 소모량")
                FaqKeyValue("초원·정글", "5")
                FaqKeyValue("사막·설산", "6")
                FaqKeyValue("화산", "7")
                FaqKeyValue("심연", "8")
            }

            // ── 오프라인 보상 ──
            FaqSection(
                index = 14,
                expanded = expandedSection == 14,
                onToggle = { expandedSection = if (expandedSection == 14) -1 else 14 },
                icon = "\uD83C\uDF19",
                title = "오프라인 보상이 있나요?",
            ) {
                FaqBullet("10분 이상 접속하지 않으면 오프라인 보상을 받습니다")
                FaqBullet("최대 24시간까지 누적됩니다")
                FaqBullet("시간당 골드: 50 × (1 + 플레이어 레벨 × 2%)")
                FaqBullet("시간당 시즌 XP: 10")
            }

            // ── 시즌패스 & 랭크 ──
            FaqSection(
                index = 15,
                expanded = expandedSection == 15,
                onToggle = { expandedSection = if (expandedSection == 15) -1 else 15 },
                icon = "\uD83C\uDFC6",
                title = "시즌패스와 랭크는?",
            ) {
                FaqSubTitle("시즌패스")
                FaqBullet("전투와 업적으로 시즌 XP를 얻습니다")
                FaqBullet("100 XP마다 1티어씩 올라가며 보상을 수령합니다")
                FaqBullet("매월 초기화됩니다")
                Spacer(Modifier.height(4.dp))
                FaqSubTitle("랭크 (트로피)")
                FaqKeyValue("브론즈", "0+")
                FaqKeyValue("실버", "1,000+")
                FaqKeyValue("골드", "2,000+")
                FaqKeyValue("다이아몬드", "3,000+")
                FaqKeyValue("마스터", "4,000+")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FaqSection(
    index: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    icon: String,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    GameCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(icon, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontSize = 12.sp,
                    color = DimText,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun FaqBullet(text: String) {
    Row(modifier = Modifier.padding(start = 4.dp)) {
        Text("•", fontSize = 12.sp, color = SubText)
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, color = LightText, lineHeight = 17.sp)
    }
}

@Composable
private fun FaqSubTitle(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = NeonCyan,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun FaqKeyValue(key: String, value: String) {
    Row(modifier = Modifier.padding(start = 8.dp)) {
        Text(key, fontSize = 12.sp, color = LightText, modifier = Modifier.width(90.dp))
        Text(value, fontSize = 12.sp, color = SubText)
    }
}
