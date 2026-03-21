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
import com.example.jaygame.ui.components.NeonProgressBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.navigationBarsPadding

private enum class SettingsPage { MAIN, AUDIO, GAMEPLAY, UPGRADE, DATA, PROFILE, PRIVACY, LICENSES }

@Composable
fun SettingsScreen(
    repository: GameRepository,
    onNavigate: (String) -> Unit,
) {
    val data by repository.gameData.collectAsState()
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    var showResetDialog by remember { mutableStateOf(false) }

    // System back → return to settings main when on sub-page
    androidx.activity.compose.BackHandler(enabled = currentPage != SettingsPage.MAIN) {
        currentPage = SettingsPage.MAIN
    }
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
                    onToggleHaptic = { repository.save(data.copy(hapticEnabled = !data.hapticEnabled)) },
                )
                SettingsPage.GAMEPLAY -> SettingsGameplay(
                    data = data,
                    onBack = { currentPage = SettingsPage.MAIN },
                    onUpdate = { repository.save(it) },
                )
                SettingsPage.UPGRADE -> SettingsUpgrade(
                    data = data,
                    onBack = { currentPage = SettingsPage.MAIN },
                    onUpdate = { repository.save(it) },
                )
                SettingsPage.DATA -> SettingsData(
                    onBack = { currentPage = SettingsPage.MAIN },
                    onReset = { showResetDialog = true },
                )
                SettingsPage.PROFILE -> SettingsProfile(
                    repository = repository,
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
                    listOf(1f, 2f, 4f, 8f).forEach { speed ->
                        val isSelected = data.defaultBattleSpeed == speed
                        val label = "x${speed.toInt()}"
                        val color = when (speed) {
                            2f -> Gold
                            4f -> NeonRed
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

        // ── 자동 소환 ──
        GameCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("자동 소환 & 조합", fontSize = 14.sp, color = LightText)
                    Text("SP 충분 시 자동 소환 + 조합", fontSize = 11.sp, color = SubText)
                }
                NeonButton(
                    text = if (data.autoSummon) "ON" else "OFF",
                    onClick = { onUpdate(data.copy(autoSummon = !data.autoSummon)) },
                    accentColor = if (data.autoSummon) NeonGreen else NeonRed,
                    accentColorDark = if (data.autoSummon) NeonGreen.copy(alpha = 0.6f) else NeonRedDark,
                    modifier = Modifier
                        .width(72.dp)
                        .height(34.dp),
                    fontSize = 13.sp,
                )
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
    repository: com.example.jaygame.data.GameRepository,
    onBack: () -> Unit,
) {
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
