# Settings Redesign + Home Profile Banner + Home BGM Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 설정 화면을 카테고리별 서브페이지 구조로 재설계하고, 홈 화면에 배너형 프로필 + BGM 재생을 추가한다.

**Architecture:** SettingsScreen 내부에서 `AnimatedContent`로 메인/서브페이지를 전환. 홈 화면 상단 Row를 Canvas 기하학 패턴 배경의 배너 카드로 교체. BGM은 Kotlin `MediaPlayer`로 assets에서 재생하며 `musicEnabled` 플래그 연동.

**Tech Stack:** Jetpack Compose, AnimatedContent, Canvas, MediaPlayer, Material3

---

## File Structure

| 파일 | 작업 | 역할 |
|------|------|------|
| `res/drawable/ic_settings_audio.xml` | 신규 | 스피커 아이콘 |
| `res/drawable/ic_settings_gameplay.xml` | 신규 | 게임패드 아이콘 |
| `res/drawable/ic_settings_reward.xml` | 신규 | 선물 아이콘 |
| `res/drawable/ic_settings_data.xml` | 신규 | 데이터/경고 아이콘 |
| `res/drawable/ic_chevron_right.xml` | 신규 | 우측 화살표 |
| `res/drawable/ic_arrow_back.xml` | 신규 | 뒤로가기 화살표 |
| `ui/screens/SettingsScreen.kt` | 전면 재작성 | 카테고리 목록 + AnimatedContent 서브페이지 |
| `ui/screens/HomeScreen.kt` | 수정 | 상단 프로필 Row → 배너형 프로필 |
| `ui/components/ProfileBanner.kt` | 신규 | Canvas 배경 + 프로필 정보 배너 컴포넌트 |
| `audio/BgmManager.kt` | 신규 | MediaPlayer 기반 BGM 관리 싱글톤 |
| `navigation/NavGraph.kt` | 수정 | BgmManager 연동 (홈 진입 시 재생, 이탈 시 정지) |
| `assets/audio/home_bgm.mp3` | 이미 다운로드됨 | 홈 BGM 파일 |

---

## Task 1: 설정 카테고리 아이콘 추가

**Files:**
- Create: `app/src/main/res/drawable/ic_settings_audio.xml`
- Create: `app/src/main/res/drawable/ic_settings_gameplay.xml`
- Create: `app/src/main/res/drawable/ic_settings_reward.xml`
- Create: `app/src/main/res/drawable/ic_settings_data.xml`
- Create: `app/src/main/res/drawable/ic_chevron_right.xml`
- Create: `app/src/main/res/drawable/ic_arrow_back.xml`

- [ ] **Step 1: ic_settings_audio.xml 생성**

Material Icons의 volume_up 스타일 벡터. 24dp, viewportWidth/Height 24.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF"
        android:pathData="M3,9v6h4l5,5V4L7,9H3zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z"/>
</vector>
```

- [ ] **Step 2: ic_settings_gameplay.xml 생성**

게임패드 아이콘.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF"
        android:pathData="M15,7.5V2H9v5.5l3,3 3,-3zM7.5,9H2v6h5.5l3,-3 -3,-3zM9,16.5V22h6v-5.5l-3,-3 -3,3zM16.5,9l-3,3 3,3H22V9h-5.5z"/>
</vector>
```

- [ ] **Step 3: ic_settings_reward.xml 생성**

선물 상자 아이콘.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF"
        android:pathData="M20,6h-2.18c0.11,-0.31 0.18,-0.65 0.18,-1 0,-1.66 -1.34,-3 -3,-3 -1.05,0 -1.96,0.54 -2.5,1.35l-0.5,0.67 -0.5,-0.68C10.96,2.54 10.05,2 9,2 7.34,2 6,3.34 6,5c0,0.35 0.07,0.69 0.18,1H4C2.9,6 2.01,6.9 2.01,8L2,19c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2zM15,4c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1 0.45,-1 1,-1zM9,4c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1 0.45,-1 1,-1zM20,19H4v-2h16v2zM20,15H4V8h5.08L7,10.83 8.62,12 11,8.76l1,-1.36 1,1.36L15.38,12 17,10.83 14.92,8H20v7z"/>
</vector>
```

- [ ] **Step 4: ic_settings_data.xml 생성**

데이터/스토리지 아이콘.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF"
        android:pathData="M2,20h20v-4H2v4zM4,17h2v2H4v-2zM2,4v4h20V4H2zM6,7H4V5h2v2zM2,14h20v-4H2v4zM4,11h2v2H4v-2z"/>
</vector>
```

- [ ] **Step 5: ic_chevron_right.xml 생성**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF"
        android:pathData="M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z"/>
</vector>
```

- [ ] **Step 6: ic_arrow_back.xml 생성**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF"
        android:pathData="M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z"/>
</vector>
```

- [ ] **Step 7: 커밋**

```bash
git add app/src/main/res/drawable/ic_settings_audio.xml app/src/main/res/drawable/ic_settings_gameplay.xml app/src/main/res/drawable/ic_settings_reward.xml app/src/main/res/drawable/ic_settings_data.xml app/src/main/res/drawable/ic_chevron_right.xml app/src/main/res/drawable/ic_arrow_back.xml
git commit -m "feat: 설정 카테고리 아이콘 6개 추가"
```

---

## Task 2: SettingsScreen 전면 재작성

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/SettingsScreen.kt`

**참고 파일:**
- `ui/components/GameCard.kt` — 카드 컴포넌트 (배경 DarkNavy, 테두리 BorderGlow, 12dp 라운드)
- `ui/components/NeonButton.kt` — 버튼 (accentColor, accentColorDark, fontSize 파라미터)
- `ui/components/RankBadge.kt` — DifficultyDialog 정의 포함
- `ui/theme/Color.kt` — Gold, NeonCyan, NeonGreen, NeonRed, SubText, LightText, DimText, DeepDark, DarkNavy, Divider 등
- `navigation/Routes.kt` — Routes.ACHIEVEMENTS, Routes.UNIT_CODEX
- `R.drawable.ic_achievement` — 업적 아이콘
- `R.drawable.ic_nav_collection` — 도감 아이콘

- [ ] **Step 1: SettingsScreen 전면 재작성**

설정 메인에 `currentPage` state를 두고 `AnimatedContent`로 전환.

```kotlin
package com.example.jaygame.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.jaygame.ui.theme.*

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
                    accentColor = NeonRed, accentColorDark = NeonRedDark, fontSize = 13.sp,
                )
            },
            dismissButton = {
                NeonButton(
                    text = "취소",
                    onClick = { showResetDialog = false },
                    accentColor = SubText, accentColorDark = SubText.copy(alpha = 0.6f), fontSize = 13.sp,
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

        GameCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsCategoryRow(
                    iconRes = R.drawable.ic_settings_audio,
                    title = "오디오",
                    iconTint = NeonCyan,
                    onClick = { onPageSelected(SettingsPage.AUDIO) },
                )
                HorizontalDivider(color = Divider)
                SettingsCategoryRow(
                    iconRes = R.drawable.ic_settings_gameplay,
                    title = "게임플레이",
                    iconTint = NeonCyan,
                    onClick = { onPageSelected(SettingsPage.GAMEPLAY) },
                )
                HorizontalDivider(color = Divider)
                SettingsCategoryRow(
                    iconRes = R.drawable.ic_settings_reward,
                    title = "주간보상",
                    iconTint = Gold,
                    onClick = onShowDailyLogin,
                )
                HorizontalDivider(color = Divider)
                SettingsCategoryRow(
                    iconRes = R.drawable.ic_achievement,
                    title = "업적",
                    iconTint = Gold,
                    onClick = { onNavigate(Routes.ACHIEVEMENTS) },
                )
                HorizontalDivider(color = Divider)
                SettingsCategoryRow(
                    iconRes = R.drawable.ic_nav_collection,
                    title = "영웅 도감",
                    iconTint = Gold,
                    onClick = { onNavigate(Routes.UNIT_CODEX) },
                )
                HorizontalDivider(color = Divider)
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
    iconTint: androidx.compose.ui.graphics.Color,
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
                        modifier = Modifier.width(72.dp).height(34.dp),
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
                        modifier = Modifier.width(72.dp).height(34.dp),
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
        Triple(0, "쉬움", "적 체력 -20%, 보상 -20%"),
        Triple(1, "보통", "기본 난이도"),
        Triple(2, "어려움", "적 체력 +50%, 보상 +50%"),
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
```

- [ ] **Step 2: 빌드 확인**

Run: `cd C:\Users\Infocar\AndroidStudioProjects\JayGame && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/SettingsScreen.kt
git commit -m "feat: 설정 화면 카테고리별 서브페이지 구조로 재설계"
```

---

## Task 3: ProfileBanner 컴포넌트 생성

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/components/ProfileBanner.kt`

**참고 파일:**
- `ui/components/RankBadge.kt` — `getRankInfo(trophies)` 함수, `RankInfo` data class
- `ui/components/NeonProgressBar.kt` — 프로그레스 바 스타일 참고
- `ui/theme/Color.kt` — Gold, NeonCyan, LightText, SubText, DarkNavy, GoldCoin, DiamondBlue, TrophyAmber
- `R.drawable.ic_gold`, `R.drawable.ic_diamond` — 리소스 아이콘

- [ ] **Step 1: ProfileBanner.kt 생성**

Canvas로 기하학적 문양 배경을 직접 그리고, 그 위에 프로필 정보를 배치.

```kotlin
package com.example.jaygame.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.ui.theme.*
import java.text.NumberFormat

@Composable
fun ProfileBanner(
    playerLevel: Int,
    trophies: Int,
    gold: Int,
    diamonds: Int,
    totalXP: Int,
    modifier: Modifier = Modifier,
) {
    val rank = getRankInfo(trophies)
    val fmt = remember { NumberFormat.getIntegerInstance() }
    // XP progress within current level (100 XP per level)
    val xpForCurrentLevel = totalXP % 100
    val xpProgress = xpForCurrentLevel / 100f

    val patternGold = Gold.copy(alpha = 0.08f)
    val patternCyan = NeonCyan.copy(alpha = 0.06f)
    val patternGoldStroke = Gold.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                // Dark background
                drawRect(Color(0xFF1A1208))
                // Draw geometric emblem pattern
                drawEmblemPattern(patternGold, patternCyan, patternGoldStroke)
                // Gradient overlay for text readability
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xDD1A1208),
                            Color(0x881A1208),
                            Color(0xDD1A1208),
                        ),
                    ),
                )
            }
            .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
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
                // Level badge
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
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_trophy),
                    contentDescription = null,
                    tint = TrophyAmber,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = fmt.format(trophies),
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
                // Gold
                Icon(
                    painter = painterResource(R.drawable.ic_gold),
                    contentDescription = null,
                    tint = GoldCoin,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = fmt.format(gold),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldCoin,
                )
                Spacer(Modifier.width(10.dp))
                // Diamonds
                Icon(
                    painter = painterResource(R.drawable.ic_diamond),
                    contentDescription = null,
                    tint = DiamondBlue,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = fmt.format(diamonds),
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

private fun DrawScope.drawEmblemPattern(
    fillColor: Color,
    accentColor: Color,
    strokeColor: Color,
) {
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
    drawPath(diamond, fillColor)
    drawPath(diamond, strokeColor, style = Stroke(width = 1.5f))

    // Inner diamond
    val innerSize = diamondSize * 0.5f
    val innerDiamond = Path().apply {
        moveTo(cx, cy - innerSize)
        lineTo(cx + innerSize, cy)
        lineTo(cx, cy + innerSize)
        lineTo(cx - innerSize, cy)
        close()
    }
    drawPath(innerDiamond, accentColor)
    drawPath(innerDiamond, strokeColor, style = Stroke(width = 1f))

    // Corner circles
    val circleRadius = size.height * 0.12f
    listOf(
        Offset(size.width * 0.15f, size.height * 0.25f),
        Offset(size.width * 0.85f, size.height * 0.25f),
        Offset(size.width * 0.15f, size.height * 0.75f),
        Offset(size.width * 0.85f, size.height * 0.75f),
    ).forEach { center ->
        drawCircle(fillColor, circleRadius, center)
        drawCircle(strokeColor, circleRadius, center, style = Stroke(width = 1f))
    }

    // Horizontal lines through center
    drawLine(
        strokeColor,
        Offset(cx - diamondSize * 1.8f, cy),
        Offset(cx - diamondSize * 1.1f, cy),
        strokeWidth = 1f,
    )
    drawLine(
        strokeColor,
        Offset(cx + diamondSize * 1.1f, cy),
        Offset(cx + diamondSize * 1.8f, cy),
        strokeWidth = 1f,
    )
}
```

- [ ] **Step 2: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/components/ProfileBanner.kt
git commit -m "feat: Canvas 기하학 패턴 배경의 프로필 배너 컴포넌트 추가"
```

---

## Task 4: HomeScreen 프로필 배너 적용

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt`

**변경 사항:**
- 기존 상단 프로필 Row (lines 136-211) 제거
- `ProfileBanner` 컴포넌트로 교체
- import 추가: `com.example.jaygame.ui.components.ProfileBanner`

- [ ] **Step 1: HomeScreen 상단 프로필 교체**

기존 `// ── Top: Profile + Resources (compact row) ──` 부터 트로피 텍스트까지를 `ProfileBanner`로 교체.

교체 대상 (lines 136-211):
```kotlin
// 기존 코드 (제거)
// ── Top: Profile + Resources (compact row) ──
Row(...) { ... }
// Trophy count
Text(text = "🏆 ${data.trophies}", ...)
```

교체할 코드:
```kotlin
// ── Top: Profile Banner ──
ProfileBanner(
    playerLevel = data.playerLevel,
    trophies = data.trophies,
    gold = data.gold,
    diamonds = data.diamonds,
    totalXP = data.totalXP,
    modifier = Modifier.padding(horizontal = 16.dp),
)
```

더 이상 사용하지 않는 import 정리:
- `RankBadge` import은 `ProfileBanner`가 내부에서 사용하므로 HomeScreen에서 제거
- `NumberFormat` import 제거 (배너 내부에서 처리)
- `ic_gold`, `ic_diamond` painterResource 관련 코드 제거

- [ ] **Step 2: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt
git commit -m "feat: 홈 화면 상단 프로필을 배너형으로 교체"
```

---

## Task 5: BgmManager + 홈 BGM 재생

**Files:**
- Create: `app/src/main/java/com/example/jaygame/audio/BgmManager.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt`

**에셋:** `assets/audio/home_bgm.mp3` (이미 다운로드됨)

**크레딧:** CC-BY 4.0 — "Music by YannZ https://yziango.itch.io"

- [ ] **Step 1: BgmManager.kt 생성**

```kotlin
package com.example.jaygame.audio

import android.content.Context
import android.media.MediaPlayer

object BgmManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentAsset: String? = null

    fun play(context: Context, assetPath: String, loop: Boolean = true) {
        if (currentAsset == assetPath && mediaPlayer?.isPlaying == true) return
        stop()
        val afd = context.assets.openFd(assetPath)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            isLooping = loop
            setVolume(0.5f, 0.5f)
            prepare()
            start()
        }
        currentAsset = assetPath
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        currentAsset = null
    }

    fun pause() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
    }

    fun resume() {
        mediaPlayer?.takeIf { !it.isPlaying }?.start()
    }
}
```

- [ ] **Step 2: HomeScreen에 BGM 연동**

HomeScreen.kt에 `DisposableEffect`를 추가하여 홈 진입 시 BGM 재생, 이탈 시 정지. `musicEnabled` 플래그 연동.

HomeScreen composable 함수 시작 부분, `val data by ...` 아래에 추가:

```kotlin
val context = LocalContext.current

// BGM control
DisposableEffect(data.musicEnabled) {
    if (data.musicEnabled) {
        BgmManager.play(context, "audio/home_bgm.mp3")
    } else {
        BgmManager.stop()
    }
    onDispose {
        BgmManager.stop()
    }
}
```

import 추가:
```kotlin
import androidx.compose.runtime.DisposableEffect
import com.example.jaygame.audio.BgmManager
```

참고: `LocalContext.current`는 이미 import 되어 있음 (stage bitmap 로딩에 사용 중). `DisposableEffect`도 이미 사용 가능.

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/audio/BgmManager.kt app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt
git commit -m "feat: 홈 화면 BGM 재생 (MediaPlayer + musicEnabled 연동)"
```

---

## Task 6: 크레딧 표기 + 최종 빌드 확인

**Files:**
- Create: `app/src/main/assets/CREDITS.txt`

- [ ] **Step 1: CREDITS.txt 생성**

```
=== JayGame Audio Credits ===

Home BGM: "Ravi de te revoir" (Main Menu OST)
  By: YannZ (https://yziango.itch.io)
  License: CC-BY 4.0
  Source: https://opengameart.org/content/free-contemplative-fantasy-music-pack
```

- [ ] **Step 2: 전체 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 최종 커밋**

```bash
git add app/src/main/assets/CREDITS.txt app/src/main/assets/audio/home_bgm.mp3
git commit -m "feat: 홈 BGM 에셋 + 크레딧 추가"
```
