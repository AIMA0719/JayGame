# JayGame 다크 네온 UI 전면 리디자인 Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 모든 UI를 다크 네온 스타일로 리디자인하고, 배틀 HUD를 Compose 오버레이로 전환하며, C++ 렌더링 픽셀 문제를 해결한다.

**Architecture:** 하이브리드 방식 — 배틀 필드(유닛/적/투사체)만 C++ OpenGL, 모든 HUD/메뉴는 Compose. C++↔Compose는 JNI 콜백으로 실시간 통신. 기존 중세풍 컴포넌트를 다크 네온 컴포넌트로 교체.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), C++17, OpenGL ES 3.0, JNI

**Design Spec:** `docs/superpowers/specs/2026-03-10-ui-redesign-dark-neon-design.md`

---

## File Structure

### 신규 생성
- `app/src/main/java/com/example/jaygame/ui/theme/DarkNeonColor.kt` — 다크 네온 컬러 팔레트
- `app/src/main/java/com/example/jaygame/ui/components/NeonButton.kt` — MedievalButton 대체
- `app/src/main/java/com/example/jaygame/ui/components/GameCard.kt` — MedievalCard 대체
- `app/src/main/java/com/example/jaygame/ui/components/NeonProgressBar.kt` — 네온 스타일 프로그레스 바
- `app/src/main/java/com/example/jaygame/ui/components/ResourceHeader.kt` — 골드/다이아/스태미나 상단 바
- `app/src/main/java/com/example/jaygame/ui/battle/BattleHud.kt` — 배틀 HUD Compose 오버레이
- `app/src/main/java/com/example/jaygame/bridge/BattleBridge.kt` — C++↔Compose JNI 브릿지

### 수정
- `app/src/main/java/com/example/jaygame/ui/theme/Color.kt` — 다크 네온 컬러로 교체
- `app/src/main/java/com/example/jaygame/ui/theme/Theme.kt` — 다크 네온 테마
- `app/src/main/java/com/example/jaygame/ui/theme/Type.kt` — 폰트 교체 (medieval → 시스템)
- `app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt` — 전면 리디자인
- `app/src/main/java/com/example/jaygame/ui/screens/DeckScreen.kt` — 다크 네온 적용
- `app/src/main/java/com/example/jaygame/ui/screens/CollectionScreen.kt` — 다크 네온 적용
- `app/src/main/java/com/example/jaygame/ui/screens/ResultScreen.kt` — 다크 네온 적용
- `app/src/main/java/com/example/jaygame/ui/screens/ShopScreen.kt` — 다크 네온 적용
- `app/src/main/java/com/example/jaygame/ui/screens/SeasonPassScreen.kt` — 다크 네온 적용
- `app/src/main/java/com/example/jaygame/ui/screens/AchievementsScreen.kt` — 다크 네온 적용
- `app/src/main/java/com/example/jaygame/ui/screens/SettingsScreen.kt` — 다크 네온 적용
- `app/src/main/java/com/example/jaygame/ui/components/BottomNavBar.kt` — 다크 네온 적용
- `app/src/main/java/com/example/jaygame/MainActivity.kt` — 배틀 HUD 교체
- `app/src/main/java/com/example/jaygame/navigation/NavGraph.kt` — 배경색 변경
- `app/src/main/cpp/TextureAsset.cpp` — GL_LINEAR 적용
- `app/src/main/cpp/engine/BattleScene.cpp` — C++ HUD 렌더링 제거

---

## Chunk 1: 디자인 시스템 (테마 + 공통 컴포넌트)

### Task 1: 다크 네온 컬러 팔레트 교체

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/theme/Color.kt`

- [ ] **Step 1: Color.kt를 다크 네온 팔레트로 교체**

```kotlin
package com.example.jaygame.ui.theme

import androidx.compose.ui.graphics.Color

// Background
val DeepDark = Color(0xFF0f0f23)
val DarkNavy = Color(0xFF1a1a3e)
val HoverNavy = Color(0xFF252550)
val DarkSurface = Color(0xFF161630)

// Accent
val NeonRed = Color(0xFFe94560)
val NeonRedDark = Color(0xFFb8354d)
val Gold = Color(0xFFffd700)
val DarkGold = Color(0xFFb8960c)
val NeonCyan = Color(0xFF00d4ff)
val NeonGreen = Color(0xFF4ade80)

// Text
val LightText = Color(0xFFf0f0f0)
val SubText = Color(0xFF8888aa)
val DimText = Color(0xFF555577)

// Divider / Border
val Divider = Color(0xFF2a2a4a)
val BorderGlow = Color(0xFF3a3a6a)

// Rarity
val RarityNormal = Color(0xFF9ca3af)
val RarityRare = Color(0xFF60a5fa)
val RarityEpic = Color(0xFFc084fc)
val RarityLegendary = Color(0xFFfb923c)
val RarityHidden = Color(0xFFf472b6)

// Resources
val GoldCoin = Color(0xFFffd700)
val DiamondBlue = Color(0xFF00d4ff)
val TrophyAmber = Color(0xFFff8f00)
val StaminaGreen = Color(0xFF66bb6a)

// Status
val PositiveGreen = Color(0xFF4ade80)
val NegativeRed = Color(0xFFe94560)
val WarningYellow = Color(0xFFffc107)
```

- [ ] **Step 2: 빌드 확인**

Run: `cd C:/Users/Infocar/AndroidStudioProjects/JayGame && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: 컴파일 에러 발생 (기존 색상 참조하는 파일들 있음) — 다음 태스크에서 해결

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/theme/Color.kt
git commit -m "feat: replace medieval color palette with dark neon colors"
```

### Task 2: 타이포그래피 교체

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/theme/Type.kt`

- [ ] **Step 1: medieval 폰트를 시스템 기본 폰트로 교체**

```kotlin
package com.example.jaygame.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val GameTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontSize = 12.sp),
    labelSmall = TextStyle(fontSize = 10.sp),
)
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/theme/Type.kt
git commit -m "feat: replace medieval font with system default for dark neon theme"
```

### Task 3: 테마 업데이트

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/theme/Theme.kt`

- [ ] **Step 1: darkColorScheme을 다크 네온으로 변경**

```kotlin
package com.example.jaygame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkNeonColorScheme = darkColorScheme(
    primary = NeonRed,
    onPrimary = LightText,
    primaryContainer = DarkNavy,
    onPrimaryContainer = LightText,
    secondary = NeonCyan,
    onSecondary = DeepDark,
    secondaryContainer = HoverNavy,
    onSecondaryContainer = LightText,
    tertiary = Gold,
    background = DeepDark,
    onBackground = LightText,
    surface = DarkNavy,
    onSurface = LightText,
    surfaceVariant = HoverNavy,
    onSurfaceVariant = SubText,
    outline = Divider,
    error = NegativeRed,
)

@Composable
fun JayGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkNeonColorScheme,
        typography = GameTypography,
        content = content,
    )
}
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/theme/Theme.kt
git commit -m "feat: update theme to dark neon color scheme"
```

### Task 4: NeonButton 컴포넌트

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/components/NeonButton.kt`

- [ ] **Step 1: NeonButton 작성**

```kotlin
package com.example.jaygame.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: TextUnit = 16.sp,
    accentColor: Color = NeonRed,
    accentColorDark: Color = NeonRedDark,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "btnScale",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.4f)
            .drawBehind {
                val cr = CornerRadius(12.dp.toPx())
                // Gradient background
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = if (isPressed) listOf(accentColorDark, accentColor.copy(alpha = 0.8f))
                        else listOf(accentColor, accentColorDark),
                    ),
                    cornerRadius = cr,
                )
                // Glow border
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.5f),
                    cornerRadius = cr,
                    style = Stroke(width = 1.5f.dp.toPx()),
                )
            }
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ) else Modifier,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = LightText,
        )
    }
}
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/components/NeonButton.kt
git commit -m "feat: add NeonButton component for dark neon theme"
```

### Task 5: GameCard 컴포넌트

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/components/GameCard.kt`

- [ ] **Step 1: GameCard 작성**

```kotlin
package com.example.jaygame.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.jaygame.ui.theme.BorderGlow
import com.example.jaygame.ui.theme.DarkNavy

@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = DarkNavy,
    borderColor: Color = BorderGlow,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .drawBehind {
                val cr = CornerRadius(12.dp.toPx())
                drawRoundRect(color = backgroundColor, cornerRadius = cr)
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = cr,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        content = content,
    )
}
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/components/GameCard.kt
git commit -m "feat: add GameCard component for dark neon theme"
```

### Task 6: NeonProgressBar 컴포넌트

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/components/NeonProgressBar.kt`

- [ ] **Step 1: NeonProgressBar 작성**

```kotlin
package com.example.jaygame.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.NeonGreen

@Composable
fun NeonProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    barColor: Color = NeonGreen,
    trackColor: Color = DeepDark,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                val cr = CornerRadius(height.toPx() / 2)
                // Track
                drawRoundRect(color = trackColor, cornerRadius = cr)
                // Bar
                if (clampedProgress > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(barColor.copy(alpha = 0.7f), barColor),
                        ),
                        cornerRadius = cr,
                        size = Size(size.width * clampedProgress, size.height),
                    )
                }
            },
    )
}
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/components/NeonProgressBar.kt
git commit -m "feat: add NeonProgressBar component"
```

### Task 7: ResourceHeader 컴포넌트

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/components/ResourceHeader.kt`

- [ ] **Step 1: ResourceHeader 작성**

```kotlin
package com.example.jaygame.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.ui.theme.DiamondBlue
import com.example.jaygame.ui.theme.GoldCoin
import com.example.jaygame.ui.theme.LightText
import java.text.NumberFormat

@Composable
fun ResourceHeader(
    gold: Int,
    diamonds: Int,
    modifier: Modifier = Modifier,
) {
    val fmt = NumberFormat.getIntegerInstance()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Gold
        Icon(
            painter = painterResource(id = R.drawable.ic_gold),
            contentDescription = "Gold",
            tint = GoldCoin,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = fmt.format(gold),
            color = GoldCoin,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.width(16.dp))
        // Diamonds
        Icon(
            painter = painterResource(id = R.drawable.ic_diamond),
            contentDescription = "Diamond",
            tint = DiamondBlue,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = fmt.format(diamonds),
            color = DiamondBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/components/ResourceHeader.kt
git commit -m "feat: add ResourceHeader component for gold/diamond display"
```

### Task 8: BottomNavBar 다크 네온 적용

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/components/BottomNavBar.kt`

- [ ] **Step 1: WoodFrame → 다크 네온 스타일로 교체**

```kotlin
package com.example.jaygame.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.Divider
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.SubText

enum class NavTab(val label: String, val iconRes: Int) {
    BATTLE("전투", R.drawable.ic_nav_battle),
    DECK("덱", R.drawable.ic_nav_deck),
    HOME("홈", R.drawable.ic_nav_home),
    COLLECTION("컬렉션", R.drawable.ic_nav_collection),
    SHOP("상점", R.drawable.ic_nav_shop),
}

@Composable
fun GameBottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .drawBehind {
                drawRect(color = DarkNavy)
                drawLine(
                    color = Divider,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val color = if (isSelected) NeonRed else SubText

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(tab) },
                    ),
            ) {
                Icon(
                    painter = painterResource(id = tab.iconRes),
                    contentDescription = tab.label,
                    tint = color,
                    modifier = Modifier.size(if (tab == NavTab.HOME) 26.dp else 22.dp),
                )
                Text(
                    text = tab.label,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp,
                    color = color,
                )
            }
        }
    }
}
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/components/BottomNavBar.kt
git commit -m "feat: update BottomNavBar to dark neon style"
```

### Task 9: NavGraph 배경색 변경

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/navigation/NavGraph.kt`

- [ ] **Step 1: DarkBrown → DeepDark 변경**

NavGraph.kt의 `.background(DarkBrown)` → `.background(DeepDark)`로 변경.
import 문에서 `DarkBrown` 제거, `DeepDark` 추가.

- [ ] **Step 2: 빌드 확인**

Run: `cd C:/Users/Infocar/AndroidStudioProjects/JayGame && ./gradlew assembleDebug 2>&1 | tail -10`

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/navigation/NavGraph.kt
git commit -m "feat: update NavGraph background to DeepDark"
```

---

## Chunk 2: 화면 리디자인 (HomeScreen, DeckScreen, CollectionScreen, ResultScreen)

### Task 10: HomeScreen 리디자인

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt`

- [ ] **Step 1: HomeScreen 전면 리디자인**

주요 변경:
- 배경: DeepDark
- 상단: ResourceHeader (골드, 다이아) + 설정 아이콘
- 프로필: 레벨, 닉네임, 랭크뱃지를 한 줄로 작게 표시
- 스테이지 카드: GameCard 사용, 난이도 칩 (NeonButton 소형)
- 퀵 버튼: NeonButton (작은 사이즈, NeonCyan 액센트)
- 스태미나 바: NeonProgressBar (StaminaGreen)
- 배틀 버튼: NeonButton (NeonRed, 큰 사이즈, 전체 폭)
- MedievalButton, WoodFrame, MedievalCard 참조 전부 제거
- MedievalFont 참조 전부 제거
- DarkBrown, Parchment, Gold 등 이전 색상 → 새 색상으로 교체
- 컴포넌트 크기를 레퍼런스 게임처럼 컴팩트하게 (너무 크게 만들지 않기)

- [ ] **Step 2: 빌드 확인**

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt
git commit -m "feat: redesign HomeScreen with dark neon style"
```

### Task 11: DeckScreen 리디자인

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/DeckScreen.kt`

- [ ] **Step 1: DeckScreen 다크 네온 적용**

주요 변경:
- 배경: DeepDark
- 상단: ResourceHeader
- 덱 슬롯 5개: GameCard, 빈 슬롯은 점선 테두리
- 유닛 그리드: GameCard, 레어리티 테두리 글로우
- 덱에 포함된 유닛: 체크마크 오버레이 (알파 줄이기 대신)
- MedievalButton, WoodFrame, MedievalFont 참조 제거
- 이전 색상 → 새 색상

- [ ] **Step 2: 빌드 확인**

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/DeckScreen.kt
git commit -m "feat: redesign DeckScreen with dark neon style"
```

### Task 12: CollectionScreen 리디자인

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/CollectionScreen.kt`

- [ ] **Step 1: CollectionScreen 다크 네온 적용**

주요 변경:
- 배경: DeepDark
- 상단: ResourceHeader + 레어리티 필터 탭 (NeonButton 소형)
- 유닛 그리드: GameCard, 레어리티 테두리
- 미보유 유닛: 실루엣 + 자물쇠 아이콘
- 디테일 패널: GameCard (하단 슬라이드업)
- 업그레이드 버튼: NeonButton (NeonGreen 액센트)
- 스탯/레벨 표시: LightText, SubText 활용
- 이전 색상/컴포넌트 참조 제거

- [ ] **Step 2: 빌드 확인**

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/CollectionScreen.kt
git commit -m "feat: redesign CollectionScreen with dark neon style"
```

### Task 13: ResultScreen 리디자인

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/ResultScreen.kt`

- [ ] **Step 1: ResultScreen 다크 네온 적용**

주요 변경:
- 배경: DeepDark
- 승리 타이틀: NeonGreen 글로우, 펄스 애니메이션 유지
- 패배 타이틀: NeonRed
- 스탯 카드: GameCard로 감싸기
- 보상 표시: 골드=GoldCoin, 트로피=TrophyAmber 아이콘 색상
- 홈 버튼: NeonButton
- WoodFrame, MedievalButton 제거

- [ ] **Step 2: 빌드 확인**

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/ResultScreen.kt
git commit -m "feat: redesign ResultScreen with dark neon style"
```

---

## Chunk 3: 나머지 화면 리디자인 (Shop, SeasonPass, Achievements, Settings)

### Task 14: ShopScreen 리디자인

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/ShopScreen.kt`

- [ ] **Step 1: ShopScreen 다크 네온 적용**

주요 변경:
- 배경: DeepDark
- 탭: NeonButton (선택 시 NeonRed, 비선택 시 SubText 배경)
- 상품 카드: GameCard
- 구매 버튼: NeonButton (Gold 액센트 for 골드팩, NeonCyan for 다이아팩)
- 이전 색상/컴포넌트 참조 제거

- [ ] **Step 2: 빌드 확인**

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/ShopScreen.kt
git commit -m "feat: redesign ShopScreen with dark neon style"
```

### Task 15: SeasonPassScreen 리디자인

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/SeasonPassScreen.kt`

- [ ] **Step 1: SeasonPassScreen 다크 네온 적용**

주요 변경:
- 배경: DeepDark
- XP 바: NeonProgressBar (NeonCyan)
- 티어 카드: GameCard, 현재 티어=NeonCyan 테두리, 수령 완료=NeonGreen, 잠김=SubText
- 수령 버튼: NeonButton (NeonGreen)
- 이전 색상/컴포넌트 참조 제거

- [ ] **Step 2: 빌드 확인**

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/SeasonPassScreen.kt
git commit -m "feat: redesign SeasonPassScreen with dark neon style"
```

### Task 16: AchievementsScreen 리디자인

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/AchievementsScreen.kt`

- [ ] **Step 1: AchievementsScreen 다크 네온 적용**

주요 변경:
- 배경: DeepDark
- 카테고리 탭: NeonButton (선택 시 NeonRed)
- 업적 항목: GameCard
- 프로그레스 바: NeonProgressBar
- 완료된 업적: NeonGreen 체크 아이콘
- 이전 색상/컴포넌트 참조 제거

- [ ] **Step 2: 빌드 확인**

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/AchievementsScreen.kt
git commit -m "feat: redesign AchievementsScreen with dark neon style"
```

### Task 17: SettingsScreen 리디자인

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/SettingsScreen.kt`

- [ ] **Step 1: SettingsScreen 다크 네온 적용**

주요 변경:
- 배경: DeepDark
- 설정 행: GameCard 안에 토글 스위치
- ON 버튼: NeonGreen, OFF 버튼: NeonRed
- 리셋 버튼: NeonButton (NeonRed 액센트)
- 확인 다이얼로그: 다크 네온 테마 적용
- WoodFrame, MedievalButton 제거

- [ ] **Step 2: 빌드 확인**

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/SettingsScreen.kt
git commit -m "feat: redesign SettingsScreen with dark neon style"
```

### Task 18: 기존 중세풍 컴포넌트 정리

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/components/MedievalButton.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/components/MedievalCard.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/components/WoodFrame.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/components/ScreenHeader.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/components/GameProgressBar.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/components/RankBadge.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/components/StageCard.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/components/DailyLoginDialog.kt`

- [ ] **Step 1: 모든 기존 컴포넌트에서 이전 색상/폰트 참조를 새 다크 네온 값으로 교체**

각 컴포넌트:
- `MedievalFont` 참조 제거 (시스템 기본 폰트 사용)
- `DarkBrown`, `MediumBrown`, `LeatherBrown`, `Parchment` 등 → `DeepDark`, `DarkNavy`, `LightText`, `SubText`로
- `WoodFrame` 내부 구현을 심플한 다크 네온 스타일로 변경 (또는 사용처가 없으면 파일 삭제)

- [ ] **Step 2: 전체 빌드 확인**

Run: `cd C:/Users/Infocar/AndroidStudioProjects/JayGame && ./gradlew assembleDebug 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/components/
git commit -m "feat: update all UI components to dark neon theme"
```

---

## Chunk 4: 배틀 HUD Compose 오버레이 + JNI 브릿지

### Task 19: BattleBridge JNI 인터페이스

**Files:**
- Create: `app/src/main/java/com/example/jaygame/bridge/BattleBridge.kt`

- [ ] **Step 1: BattleBridge 작성**

```kotlin
package com.example.jaygame.bridge

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BattleState(
    val currentWave: Int = 0,
    val maxWaves: Int = 40,
    val playerHP: Int = 20,
    val maxHP: Int = 20,
    val sp: Float = 100f,
    val elapsedTime: Float = 0f,
    val state: Int = 0, // 0=WaveDelay, 1=Playing, 2=Victory, 3=Defeat
    val summonCost: Int = 50,
    val deckUnits: IntArray = intArrayOf(0, 1, 2, 3, 4),
)

object BattleBridge {
    private val _state = MutableStateFlow(BattleState())
    val state: StateFlow<BattleState> = _state.asStateFlow()

    // Called from C++ via JNI to update battle state
    @JvmStatic
    fun updateState(
        wave: Int, maxWaves: Int,
        hp: Int, maxHp: Int,
        sp: Float, elapsed: Float,
        state: Int, summonCost: Int,
    ) {
        _state.value = BattleState(
            currentWave = wave,
            maxWaves = maxWaves,
            playerHP = hp,
            maxHP = maxHp,
            sp = sp,
            elapsedTime = elapsed,
            state = state,
            summonCost = summonCost,
            deckUnits = _state.value.deckUnits,
        )
    }

    @JvmStatic
    fun setDeck(units: IntArray) {
        _state.value = _state.value.copy(deckUnits = units)
    }

    // Called from Compose, calls C++ via JNI
    external fun nativeSummon(): Boolean
    external fun nativePause()
}
```

- [ ] **Step 2: C++ JNI 함수 선언 추가**

`app/src/main/cpp/engine/BattleScene.cpp` 끝에 JNI 함수 추가:

```cpp
#include <jni.h>

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_jaygame_bridge_BattleBridge_nativeSummon(JNIEnv* env, jobject /*thiz*/) {
    // Will be connected to BattleScene::summonUnit() via GameEngine singleton
    // For now return false
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_jaygame_bridge_BattleBridge_nativePause(JNIEnv* env, jobject /*thiz*/) {
    // TODO: connect to GameEngine pause
}

} // extern "C"
```

- [ ] **Step 3: BattleScene에 Compose 상태 업데이트 콜백 추가**

`BattleScene.cpp`의 `onUpdate()` 끝에, 상태가 변경될 때 JNI를 통해 `BattleBridge.updateState()` 호출하는 코드 추가.
JNI 호출은 매 프레임이 아니라, wave/hp/sp/state가 실제로 변경될 때만 호출.

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/bridge/BattleBridge.kt
git add app/src/main/cpp/engine/BattleScene.cpp
git commit -m "feat: add BattleBridge JNI interface for C++ <-> Compose communication"
```

### Task 20: 배틀 HUD Compose 오버레이

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/battle/BattleHud.kt`
- Modify: `app/src/main/java/com/example/jaygame/MainActivity.kt`

- [ ] **Step 1: BattleHud 작성**

```kotlin
package com.example.jaygame.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.UNIT_DEFS
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.components.NeonProgressBar
import com.example.jaygame.ui.theme.*

@Composable
fun BattleTopHud(modifier: Modifier = Modifier) {
    val state by BattleBridge.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkNavy.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Wave info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Wave ${state.currentWave}/${state.maxWaves}",
                color = LightText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "HP ${state.playerHP}/${state.maxHP}",
                color = if (state.playerHP <= 5) NeonRed else NeonGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        NeonProgressBar(
            progress = state.currentWave.toFloat() / state.maxWaves.coerceAtLeast(1),
            barColor = NeonCyan,
            height = 6.dp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        // SP + Timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "SP: ${state.sp.toInt()}",
                color = Gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            val mins = (state.elapsedTime / 60).toInt()
            val secs = (state.elapsedTime % 60).toInt()
            Text(
                text = "%02d:%02d".format(mins, secs),
                color = SubText,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
fun BattleBottomHud(modifier: Modifier = Modifier) {
    val state by BattleBridge.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkNavy.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Deck units row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            state.deckUnits.forEach { unitId ->
                val def = UNIT_DEFS.getOrNull(unitId)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = def?.iconRes ?: R.drawable.ic_unit_0),
                        contentDescription = def?.name ?: "Unit",
                        tint = LightText,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        text = def?.name ?: "?",
                        color = SubText,
                        fontSize = 9.sp,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Summon button
        NeonButton(
            text = "소환  ${state.summonCost} SP",
            onClick = { BattleBridge.nativeSummon() },
            enabled = state.sp >= state.summonCost,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            fontSize = 15.sp,
        )
    }
}
```

- [ ] **Step 2: MainActivity 업데이트 — 상단+하단 오버레이**

```kotlin
// MainActivity.kt 의 addBattleOverlay()를 교체:
private fun addBattleOverlay() {
    // Top HUD
    val topView = ComposeView(this).apply {
        setViewTreeLifecycleOwner(this@MainActivity)
        setViewTreeSavedStateRegistryOwner(this@MainActivity)
        setContent {
            JayGameTheme { BattleTopHud() }
        }
    }
    addContentView(topView, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.TOP,
    ))

    // Bottom HUD
    val bottomView = ComposeView(this).apply {
        setViewTreeLifecycleOwner(this@MainActivity)
        setViewTreeSavedStateRegistryOwner(this@MainActivity)
        setContent {
            JayGameTheme { BattleBottomHud() }
        }
    }
    addContentView(bottomView, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.BOTTOM,
    ))
}
```

import 추가: `com.example.jaygame.ui.battle.BattleTopHud`, `com.example.jaygame.ui.battle.BattleBottomHud`

기존 `BattleOverlayContent` 함수 제거.

- [ ] **Step 3: 빌드 확인**

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/battle/BattleHud.kt
git add app/src/main/java/com/example/jaygame/MainActivity.kt
git commit -m "feat: add dark neon battle HUD with top/bottom Compose overlays"
```

---

## Chunk 5: C++ 렌더링 개선 + HUD 제거 + 최종 빌드

### Task 21: C++ 텍스처 필터링 개선

**Files:**
- Modify: `app/src/main/cpp/TextureAsset.cpp`

- [ ] **Step 1: GL_NEAREST → GL_LINEAR 변경**

`TextureAsset.cpp`에서 두 군데 변경:

Line 52-53 (API 30+ 경로):
```cpp
glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
```

Line 87-88 (fallback 경로):
```cpp
glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/cpp/TextureAsset.cpp
git commit -m "fix: change GL_NEAREST to GL_LINEAR for smooth texture rendering"
```

### Task 22: C++ HUD 렌더링 비활성화

**Files:**
- Modify: `app/src/main/cpp/engine/BattleScene.cpp`

- [ ] **Step 1: renderHUD() 호출 비활성화**

`BattleScene::onRender()`에서 `renderHUD(batch)` 호출을 주석 처리 또는 제거.
Compose 오버레이가 HUD를 담당하므로 C++ HUD는 불필요.

```cpp
void BattleScene::onRender(float alpha, SpriteBatch& batch) {
    renderPath(batch);
    // Render game entities
    // ... (유닛, 적, 투사체 렌더링 유지)

    // renderHUD(batch);  // Removed: HUD is now handled by Compose overlay
}
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/cpp/engine/BattleScene.cpp
git commit -m "feat: disable C++ HUD rendering, replaced by Compose overlay"
```

### Task 23: 전체 빌드 + 컴파일 에러 해결

**Files:**
- 모든 파일

- [ ] **Step 1: 전체 빌드**

Run: `cd C:/Users/Infocar/AndroidStudioProjects/JayGame && ./gradlew assembleDebug 2>&1 | tail -30`

- [ ] **Step 2: 컴파일 에러 수정**

이전 색상/컴포넌트 참조가 남아있는 파일이 있으면 수정.
주로:
- `DarkBrown` → `DeepDark`
- `Parchment` → `LightText`
- `MediumBrown` → `DarkNavy`
- `LeatherBrown` → `HoverNavy`
- `MedievalFont` → 제거 (시스템 기본)
- `MedievalButton` → `NeonButton`
- `MedievalCard` → `GameCard`
- `WoodFrame` → `GameCard` 또는 직접 drawBehind

- [ ] **Step 3: 빌드 성공 확인**

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add -A
git commit -m "fix: resolve all compilation errors from dark neon migration"
```

---

## Chunk 6: 데이터 연동 검증 + 최종 테스트

### Task 24: 데이터 연동 검증

- [ ] **Step 1: 모든 화면에서 GameRepository.gameData 연결 확인**

체크리스트:
- HomeScreen: 골드, 다이아, 스태미나, 트로피, 덱, 스테이지 데이터 표시
- DeckScreen: 보유 유닛 필터링, 덱 저장/로드
- CollectionScreen: 유닛 보유 여부, 레벨, 카드 수, 업그레이드
- ShopScreen: 골드/다이아 소비 후 잔액 반영
- ResultScreen: 배틀 결과 데이터 수신
- SeasonPassScreen: 시즌 XP, 티어, 수령 상태
- AchievementsScreen: 진행률 계산 (totalKills, totalWins 등)
- SettingsScreen: 사운드/뮤직 토글 저장

- [ ] **Step 2: 목데이터 사용 여부 확인**

모든 화면에서 하드코딩된 더미 값이 아닌 `repository.gameData.collectAsState()`로 실시간 데이터 사용하는지 확인.

- [ ] **Step 3: 수정 필요 시 수정 + 커밋**

### Task 25: 네비게이션 동작 테스트

- [ ] **Step 1: 화면 전환 확인**

체크리스트:
- Home → Deck → Home (하단 탭)
- Home → Collection → Home (하단 탭)
- Home → Shop → Home (하단 탭)
- Home → Settings → Back (설정 아이콘 → 뒤로가기)
- Home → Achievements → Back
- Home → SeasonPass → Back
- Home → Battle (MainActivity 시작)
- Battle 종료 → Result → Home

- [ ] **Step 2: 화면 전환 시 데이터 유지 확인**

골드 변경 후 화면 전환해도 값이 유지되는지 확인.

### Task 26: 최종 APK 빌드

- [ ] **Step 1: Release 빌드**

Run: `cd C:/Users/Infocar/AndroidStudioProjects/JayGame && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 최종 커밋**

```bash
git add -A
git commit -m "feat: complete dark neon UI redesign with battle HUD and rendering fixes"
```
