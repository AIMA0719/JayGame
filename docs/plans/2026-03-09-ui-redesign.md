# JayGame UI Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace all menu screens (7) with Jetpack Compose UI in a medieval fantasy theme, keeping the C++ OpenGL battle screen.

**Architecture:** Hybrid app — ComposeActivity hosts all menu screens via Compose Navigation; BattleActivity wraps the existing C++ GameActivity. Data shared via SharedPreferences JSON (existing SaveSystem). Compose reads/writes the same JSON format.

**Tech Stack:** Jetpack Compose (Material3), Compose Navigation, Kotlin Coroutines, Android Vector Drawables (SVG), Compose Canvas for custom medieval UI elements.

---

## Phase 1: Project Foundation

### Task 1: Add Compose Dependencies

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`

**Step 1: Update version catalog**

Add to `gradle/libs.versions.toml`:
```toml
[versions]
# existing...
composeBom = "2025.05.00"
activityCompose = "1.10.1"
navigationCompose = "2.9.0"
lifecycleRuntime = "2.9.0"
coilCompose = "2.7.0"

[libraries]
# existing...
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
compose-animation = { group = "androidx.compose.animation", name = "animation" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntime" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coilCompose" }

[plugins]
# existing...
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version = "2.1.20" }
```

**Step 2: Update app/build.gradle.kts**

Add compose plugin and dependencies:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler) // add
}

android {
    // existing...
    buildFeatures {
        prefab = true
        compose = true  // add
    }
}

dependencies {
    // existing...
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)
}
```

**Step 3: Sync and verify build**

Run: `cd C:/Users/Infocar/AndroidStudioProjects/JayGame && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**
```bash
git add app/build.gradle.kts gradle/libs.versions.toml
git commit -m "build: add Jetpack Compose dependencies for UI redesign"
```

---

### Task 2: Create Medieval Fantasy Theme System

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/theme/Color.kt`
- Create: `app/src/main/java/com/example/jaygame/ui/theme/Type.kt`
- Create: `app/src/main/java/com/example/jaygame/ui/theme/Theme.kt`

**Step 1: Create Color.kt**

```kotlin
package com.example.jaygame.ui.theme

import androidx.compose.ui.graphics.Color

// Primary palette - Medieval Fantasy
val DarkBrown = Color(0xFF2C1810)
val MediumBrown = Color(0xFF5C3A21)
val LeatherBrown = Color(0xFF8B5E3C)
val LightLeather = Color(0xFFD4A574)
val Parchment = Color(0xFFF5E6C8)
val DarkParchment = Color(0xFFE8D5B0)

// Accents
val Gold = Color(0xFFFFD700)
val DarkGold = Color(0xFFB8960C)
val BrightGold = Color(0xFFFFF0A0)
val MetalGray = Color(0xFF8A8A8A)
val DarkMetal = Color(0xFF4A4A4A)

// Rarity colors
val RarityNormal = Color(0xFF9E9E9E)
val RarityRare = Color(0xFF2196F3)
val RarityEpic = Color(0xFFAB47BC)
val RarityLegendary = Color(0xFFFF8F00)

// Element colors
val ElementPhysical = Color(0xFFFF8A65)
val ElementMagic = Color(0xFF64B5F6)
val ElementSupport = Color(0xFF81C784)

// Status
val PositiveGreen = Color(0xFF4CAF50)
val NegativeRed = Color(0xFFE53935)
val WarningYellow = Color(0xFFFFC107)

// Resource colors
val GoldCoin = Color(0xFFFFD700)
val DiamondBlue = Color(0xFF00BCD4)
val TrophyAmber = Color(0xFFFF8F00)
val StaminaGreen = Color(0xFF66BB6A)
```

**Step 2: Create Type.kt**

```kotlin
package com.example.jaygame.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.jaygame.R

val MedievalFont = FontFamily(
    Font(R.font.medieval_regular, FontWeight.Normal),
    Font(R.font.medieval_bold, FontWeight.Bold),
)

val GameTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MedievalFont,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        color = Gold,
    ),
    headlineLarge = TextStyle(
        fontFamily = MedievalFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = MedievalFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = MedievalFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = MedievalFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = MedievalFont,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = MedievalFont,
        fontSize = 14.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = MedievalFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = MedievalFont,
        fontSize = 12.sp,
    ),
)
```

**Step 3: Create Theme.kt**

```kotlin
package com.example.jaygame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GameColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = DarkBrown,
    primaryContainer = MediumBrown,
    onPrimaryContainer = Parchment,
    secondary = LeatherBrown,
    onSecondary = Parchment,
    secondaryContainer = DarkBrown,
    onSecondaryContainer = LightLeather,
    tertiary = DiamondBlue,
    background = DarkBrown,
    onBackground = Parchment,
    surface = MediumBrown,
    onSurface = Parchment,
    surfaceVariant = LeatherBrown,
    onSurfaceVariant = DarkParchment,
    outline = DarkGold,
    error = NegativeRed,
)

@Composable
fun JayGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GameColorScheme,
        typography = GameTypography,
        content = content,
    )
}
```

**Step 4: Download medieval font**

Download a free medieval/fantasy font (e.g., "MedievalSharp" from Google Fonts) and place:
- `app/src/main/res/font/medieval_regular.ttf`
- `app/src/main/res/font/medieval_bold.ttf`

Use Google Fonts API or download from: https://fonts.google.com/specimen/MedievalSharp
If only one weight available, use same file for both.

**Step 5: Commit**
```bash
git add app/src/main/java/com/example/jaygame/ui/
git add app/src/main/res/font/
git commit -m "feat: add medieval fantasy theme system with colors, typography, and dark scheme"
```

---

### Task 3: Create Reusable Medieval UI Components

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/components/MedievalCard.kt`
- Create: `app/src/main/java/com/example/jaygame/ui/components/MedievalButton.kt`
- Create: `app/src/main/java/com/example/jaygame/ui/components/ResourceBar.kt`
- Create: `app/src/main/java/com/example/jaygame/ui/components/WoodFrame.kt`
- Create: `app/src/main/java/com/example/jaygame/ui/components/BottomNavBar.kt`

**Step 1: Create WoodFrame.kt — Core decorative container**

```kotlin
package com.example.jaygame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jaygame.ui.theme.*

@Composable
fun WoodFrame(
    modifier: Modifier = Modifier,
    borderWidth: Dp = 3.dp,
    cornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            // Parchment background
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MediumBrown.copy(alpha = 0.95f),
                        DarkBrown.copy(alpha = 0.98f),
                    )
                ),
                cornerRadius = CornerRadius(cornerRadius.toPx()),
            )
            // Wood border
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(LeatherBrown, DarkGold, LeatherBrown)
                ),
                cornerRadius = CornerRadius(cornerRadius.toPx()),
                style = Stroke(width = borderWidth.toPx()),
            )
            // Inner glow
            drawRoundRect(
                color = Gold.copy(alpha = 0.1f),
                topLeft = Offset(borderWidth.toPx(), borderWidth.toPx()),
                size = Size(
                    size.width - borderWidth.toPx() * 2,
                    size.height - borderWidth.toPx() * 2
                ),
                cornerRadius = CornerRadius((cornerRadius - 2.dp).toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        content()
    }
}
```

**Step 2: Create MedievalButton.kt**

```kotlin
package com.example.jaygame.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.ui.theme.*

@Composable
fun MedievalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: TextUnit = 16.sp,
    baseColor: Color = MediumBrown,
    accentColor: Color = Gold,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor = if (isPressed) baseColor.copy(alpha = 0.7f)
        else if (!enabled) baseColor.copy(alpha = 0.4f)
        else baseColor

    val textColor = if (!enabled) Parchment.copy(alpha = 0.4f) else Parchment

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                color = bgColor,
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(accentColor, accentColor.copy(alpha = 0.5f), accentColor)
                ),
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = MedievalFont,
        )
    }
}
```

**Step 3: Create MedievalCard.kt**

```kotlin
package com.example.jaygame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.jaygame.ui.theme.*

@Composable
fun MedievalCard(
    modifier: Modifier = Modifier,
    borderColor: Color = DarkGold,
    backgroundColor: Color = MediumBrown,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                color = backgroundColor,
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
            drawRoundRect(
                color = borderColor,
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        content()
    }
}
```

**Step 4: Create ResourceBar.kt — Top header with currencies**

```kotlin
package com.example.jaygame.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.ui.theme.*
import java.text.NumberFormat

@Composable
fun ResourceItem(
    iconRes: Int,
    value: Int,
    tintColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = NumberFormat.getInstance().format(value),
            color = Parchment,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MedievalFont,
        )
    }
}

@Composable
fun FullHeader(
    level: Int,
    trophies: Int,
    gold: Int,
    diamonds: Int,
    modifier: Modifier = Modifier,
) {
    WoodFrame(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Lv.$level",
                color = Gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MedievalFont,
            )
            ResourceItem(R.drawable.ic_trophy, trophies, TrophyAmber)
            ResourceItem(R.drawable.ic_gold, gold, GoldCoin)
            ResourceItem(R.drawable.ic_diamond, diamonds, DiamondBlue)
        }
    }
}

@Composable
fun CurrencyHeader(
    gold: Int,
    diamonds: Int,
    modifier: Modifier = Modifier,
) {
    WoodFrame(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            ResourceItem(R.drawable.ic_gold, gold, GoldCoin)
            Spacer(Modifier.width(16.dp))
            ResourceItem(R.drawable.ic_diamond, diamonds, DiamondBlue)
        }
    }
}
```

**Step 5: Create BottomNavBar.kt**

```kotlin
package com.example.jaygame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.ui.theme.*

enum class NavTab(val labelText: String, val iconRes: Int) {
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
    WoodFrame(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        cornerRadius = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val color = if (isSelected) Gold else Parchment.copy(alpha = 0.5f)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = tab.labelText,
                        tint = color,
                        modifier = Modifier.size(if (tab == NavTab.HOME) 28.dp else 24.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.labelText,
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = MedievalFont,
                    )
                }
            }
        }
    }
}
```

**Step 6: Commit**
```bash
git add app/src/main/java/com/example/jaygame/ui/components/
git commit -m "feat: add medieval UI components - WoodFrame, MedievalButton, MedievalCard, ResourceBar, BottomNavBar"
```

---

### Task 4: Create Icon Assets (Vector Drawables)

**Files:**
- Create: `app/src/main/res/drawable/ic_gold.xml`
- Create: `app/src/main/res/drawable/ic_diamond.xml`
- Create: `app/src/main/res/drawable/ic_trophy.xml`
- Create: `app/src/main/res/drawable/ic_nav_battle.xml`
- Create: `app/src/main/res/drawable/ic_nav_deck.xml`
- Create: `app/src/main/res/drawable/ic_nav_home.xml`
- Create: `app/src/main/res/drawable/ic_nav_collection.xml`
- Create: `app/src/main/res/drawable/ic_nav_shop.xml`
- Create: `app/src/main/res/drawable/ic_settings.xml`
- Create: `app/src/main/res/drawable/ic_achievement.xml`
- Create: `app/src/main/res/drawable/ic_season_pass.xml`
- Create: 15 unit icon drawables (`ic_unit_0.xml` through `ic_unit_14.xml`)

**Strategy for assets:**
1. **Navigation & UI icons:** Create Android Vector Drawables (XML SVG) by hand — simple geometric shapes with medieval flair (swords, shields, crowns, scrolls)
2. **Unit icons:** Create simple but distinctive vector drawables per unit element:
   - Fire: flame shape (orange/red)
   - Frost: snowflake (blue)
   - Poison: skull with drip (green)
   - IronWall: shield (gray)
   - Lightning: bolt (yellow)
   - Sniper: crosshair (dark)
   - Enhance: star burst (gold)
   - Storm: cloud + bolt (purple)
   - Assassin: dagger (dark purple)
   - Dragon: dragon head (orange)
   - Combo units: combinations of base icons
3. **Backgrounds:** Use Compose Canvas with gradients (no bitmap files needed)

**Step 1:** Create all vector drawable XML files. Each is a 24x24dp viewBox with paths.

Example `ic_gold.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFD700"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,18c-3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6 6,2.69 6,6 -2.69,6 -6,6z"/>
    <path
        android:fillColor="#B8960C"
        android:pathData="M12,7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5 5,-2.24 5,-5 -2.24,-5 -5,-5z"/>
</vector>
```

Each icon follows this pattern with appropriate shapes.

**Step 2: Commit**
```bash
git add app/src/main/res/drawable/ic_*.xml
git commit -m "feat: add medieval game icon assets - nav, currency, unit icons as vector drawables"
```

---

### Task 5: Create Kotlin Data Layer (SharedPreferences Bridge)

**Files:**
- Create: `app/src/main/java/com/example/jaygame/data/GameRepository.kt`
- Create: `app/src/main/java/com/example/jaygame/data/GameData.kt`

**Step 1: Create GameData.kt — Kotlin mirror of PlayerData**

```kotlin
package com.example.jaygame.data

data class UnitProgress(
    val owned: Boolean = false,
    val cards: Int = 0,
    val level: Int = 1,
)

data class GameData(
    val gold: Int = 500,
    val diamonds: Int = 0,
    val trophies: Int = 0,
    val playerLevel: Int = 1,
    val totalXP: Int = 0,
    val units: List<UnitProgress> = List(15) { i ->
        UnitProgress(owned = i < 10, cards = 0, level = 1)
    },
    val deck: List<Int> = listOf(0, 1, 2, 3, 4),
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val totalKills: Int = 0,
    val totalMerges: Int = 0,
    val totalGoldEarned: Int = 0,
    val highestWave: Int = 0,
    val maxUnitLevel: Int = 1,
    val wonWithoutDamage: Boolean = false,
    val wonWithSingleType: Boolean = false,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val lastLoginDate: String = "",
    val loginStreak: Int = 0,
    val lastClaimedDay: Int = 0,
    val seasonXP: Int = 0,
    val seasonClaimedTier: Int = 0,
    val saveVersion: Int = 1,
) {
    val rank: String get() = when {
        trophies >= 4000 -> "마스터"
        trophies >= 3000 -> "다이아몬드"
        trophies >= 2000 -> "골드"
        trophies >= 1000 -> "실버"
        else -> "브론즈"
    }

    val seasonTier: Int get() = seasonXP / 100
    val seasonTierProgress: Float get() = (seasonXP % 100) / 100f
}
```

**Step 2: Create GameRepository.kt — Read/write SharedPreferences JSON**

```kotlin
package com.example.jaygame.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

class GameRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jaygame_save", Context.MODE_PRIVATE)

    private val _gameData = MutableStateFlow(load())
    val gameData: StateFlow<GameData> = _gameData

    fun refresh() {
        _gameData.value = load()
    }

    fun save(data: GameData) {
        val json = serialize(data)
        prefs.edit().putString("save_data", json).apply()
        _gameData.value = data
    }

    private fun load(): GameData {
        val jsonStr = prefs.getString("save_data", null) ?: return GameData()
        return try {
            deserialize(jsonStr)
        } catch (e: Exception) {
            GameData()
        }
    }

    private fun serialize(d: GameData): String {
        val obj = JSONObject()
        obj.put("gold", d.gold)
        obj.put("diamonds", d.diamonds)
        obj.put("trophies", d.trophies)
        obj.put("playerLevel", d.playerLevel)
        obj.put("totalXP", d.totalXP)

        val unitsArr = JSONArray()
        d.units.forEach { u ->
            val uo = JSONObject()
            uo.put("owned", if (u.owned) 1 else 0)
            uo.put("cards", u.cards)
            uo.put("level", u.level)
            unitsArr.put(uo)
        }
        obj.put("units", unitsArr)

        val deckArr = JSONArray()
        d.deck.forEach { deckArr.put(it) }
        obj.put("deck", deckArr)

        obj.put("totalWins", d.totalWins)
        obj.put("totalLosses", d.totalLosses)
        obj.put("totalKills", d.totalKills)
        obj.put("totalMerges", d.totalMerges)
        obj.put("totalGoldEarned", d.totalGoldEarned)
        obj.put("highestWave", d.highestWave)
        obj.put("maxUnitLevel", d.maxUnitLevel)
        obj.put("wonWithoutDamage", if (d.wonWithoutDamage) 1 else 0)
        obj.put("wonWithSingleType", if (d.wonWithSingleType) 1 else 0)
        obj.put("soundEnabled", if (d.soundEnabled) 1 else 0)
        obj.put("musicEnabled", if (d.musicEnabled) 1 else 0)
        obj.put("lastLoginDate", d.lastLoginDate)
        obj.put("loginStreak", d.loginStreak)
        obj.put("lastClaimedDay", d.lastClaimedDay)
        obj.put("seasonXP", d.seasonXP)
        obj.put("seasonClaimedTier", d.seasonClaimedTier)
        obj.put("saveVersion", d.saveVersion)

        // Compute FNV-1a checksum matching C++ side
        val content = obj.toString()
        obj.put("checksum", fnv1aHash(content))
        return obj.toString()
    }

    private fun deserialize(jsonStr: String): GameData {
        val obj = JSONObject(jsonStr)
        val unitsArr = obj.getJSONArray("units")
        val units = (0 until unitsArr.length()).map { i ->
            val u = unitsArr.getJSONObject(i)
            UnitProgress(
                owned = u.getInt("owned") == 1,
                cards = u.getInt("cards"),
                level = u.getInt("level"),
            )
        }
        val deckArr = obj.getJSONArray("deck")
        val deck = (0 until deckArr.length()).map { deckArr.getInt(it) }

        return GameData(
            gold = obj.optInt("gold", 500),
            diamonds = obj.optInt("diamonds", 0),
            trophies = obj.optInt("trophies", 0),
            playerLevel = obj.optInt("playerLevel", 1),
            totalXP = obj.optInt("totalXP", 0),
            units = units,
            deck = deck,
            totalWins = obj.optInt("totalWins", 0),
            totalLosses = obj.optInt("totalLosses", 0),
            totalKills = obj.optInt("totalKills", 0),
            totalMerges = obj.optInt("totalMerges", 0),
            totalGoldEarned = obj.optInt("totalGoldEarned", 0),
            highestWave = obj.optInt("highestWave", 0),
            maxUnitLevel = obj.optInt("maxUnitLevel", 1),
            wonWithoutDamage = obj.optInt("wonWithoutDamage", 0) == 1,
            wonWithSingleType = obj.optInt("wonWithSingleType", 0) == 1,
            soundEnabled = obj.optInt("soundEnabled", 1) == 1,
            musicEnabled = obj.optInt("musicEnabled", 1) == 1,
            lastLoginDate = obj.optString("lastLoginDate", ""),
            loginStreak = obj.optInt("loginStreak", 0),
            lastClaimedDay = obj.optInt("lastClaimedDay", 0),
            seasonXP = obj.optInt("seasonXP", 0),
            seasonClaimedTier = obj.optInt("seasonClaimedTier", 0),
            saveVersion = obj.optInt("saveVersion", 1),
        )
    }

    private fun fnv1aHash(data: String): Long {
        var hash = 2166136261L
        for (c in data) {
            hash = hash xor c.code.toLong()
            hash = (hash * 16777619L) and 0xFFFFFFFFL
        }
        return hash
    }
}
```

**Step 3: Commit**
```bash
git add app/src/main/java/com/example/jaygame/data/
git commit -m "feat: add Kotlin data layer mirroring C++ PlayerData with SharedPreferences JSON bridge"
```

---

### Task 6: Create Unit Data Definitions in Kotlin

**Files:**
- Create: `app/src/main/java/com/example/jaygame/data/UnitDefs.kt`

**Step 1: Create UnitDefs.kt — mirror of UnitData.h**

```kotlin
package com.example.jaygame.data

import com.example.jaygame.R
import com.example.jaygame.ui.theme.*
import androidx.compose.ui.graphics.Color

enum class UnitRarity(val label: String, val color: Color) {
    NORMAL("노말", RarityNormal),
    RARE("레어", RarityRare),
    EPIC("에픽", RarityEpic),
    LEGENDARY("전설", RarityLegendary),
}

enum class UnitElement(val label: String, val color: Color) {
    PHYSICAL("물리", ElementPhysical),
    MAGIC("마법", ElementMagic),
    SUPPORT("보조", ElementSupport),
}

data class UnitDef(
    val id: Int,
    val name: String,
    val rarity: UnitRarity,
    val element: UnitElement,
    val baseATK: Int,
    val baseSpeed: Float,
    val range: Float,
    val abilityName: String,
    val description: String,
    val iconRes: Int,
    val isSummonable: Boolean = true,
)

val UPGRADE_COSTS = listOf(
    2 to 100, 4 to 200, 10 to 500, 20 to 1000, 50 to 2000, 100 to 5000,
)

val LEVEL_MULTIPLIER = floatArrayOf(1.0f, 1.5f, 2.2f, 3.2f, 4.5f, 6.0f, 8.0f)

val UNIT_DEFS = listOf(
    UnitDef(0, "화염", UnitRarity.NORMAL, UnitElement.MAGIC, 25, 1.0f, 150f, "스플래시", "주변 적에게 범위 피해", R.drawable.ic_unit_0),
    UnitDef(1, "냉기", UnitRarity.NORMAL, UnitElement.MAGIC, 20, 0.8f, 160f, "감속", "적 이동속도 30% 감소", R.drawable.ic_unit_1),
    UnitDef(2, "독", UnitRarity.NORMAL, UnitElement.MAGIC, 15, 0.9f, 140f, "지속피해", "초당 10 독 데미지", R.drawable.ic_unit_2),
    UnitDef(3, "철벽", UnitRarity.NORMAL, UnitElement.SUPPORT, 10, 0.5f, 100f, "방패", "아군에게 보호막 부여", R.drawable.ic_unit_3),
    UnitDef(4, "번개", UnitRarity.RARE, UnitElement.MAGIC, 30, 1.2f, 170f, "체인", "3명에게 번개 전이", R.drawable.ic_unit_4),
    UnitDef(5, "저격", UnitRarity.RARE, UnitElement.PHYSICAL, 50, 0.5f, 250f, "없음", "높은 단일 대상 피해", R.drawable.ic_unit_5),
    UnitDef(6, "강화", UnitRarity.RARE, UnitElement.SUPPORT, 5, 0.3f, 120f, "버프", "주변 아군 공격력 +20%", R.drawable.ic_unit_6),
    UnitDef(7, "폭풍", UnitRarity.EPIC, UnitElement.MAGIC, 35, 1.0f, 180f, "스플래시+감속", "범위 피해 + 감속", R.drawable.ic_unit_7),
    UnitDef(8, "암살", UnitRarity.EPIC, UnitElement.PHYSICAL, 45, 1.5f, 130f, "처형", "HP 15% 이하 즉사", R.drawable.ic_unit_8),
    UnitDef(9, "용", UnitRarity.LEGENDARY, UnitElement.MAGIC, 60, 0.7f, 200f, "스플래시", "강력한 범위 화염", R.drawable.ic_unit_9),
    UnitDef(10, "전기독", UnitRarity.EPIC, UnitElement.MAGIC, 35, 1.1f, 170f, "체인", "4명에게 전이", R.drawable.ic_unit_10, isSummonable = false),
    UnitDef(11, "처형자", UnitRarity.EPIC, UnitElement.PHYSICAL, 55, 1.0f, 200f, "처형", "HP 30% 이하 즉사", R.drawable.ic_unit_11, isSummonable = false),
    UnitDef(12, "요새", UnitRarity.EPIC, UnitElement.SUPPORT, 15, 0.4f, 130f, "버프", "주변 공격력 +30%", R.drawable.ic_unit_12, isSummonable = false),
    UnitDef(13, "불사조", UnitRarity.LEGENDARY, UnitElement.MAGIC, 70, 0.8f, 190f, "스플래시", "초강력 범위 화염", R.drawable.ic_unit_13, isSummonable = false),
    UnitDef(14, "정령", UnitRarity.LEGENDARY, UnitElement.MAGIC, 40, 1.3f, 180f, "체인", "5명에게 전이", R.drawable.ic_unit_14, isSummonable = false),
)
```

**Step 2: Commit**
```bash
git add app/src/main/java/com/example/jaygame/data/UnitDefs.kt
git commit -m "feat: add Kotlin unit definitions mirroring C++ UnitData.h"
```

---

## Phase 2: Activity Architecture & Navigation

### Task 7: Create ComposeActivity and Navigation

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ComposeActivity.kt`
- Create: `app/src/main/java/com/example/jaygame/navigation/NavGraph.kt`
- Create: `app/src/main/java/com/example/jaygame/navigation/Routes.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Step 1: Create Routes.kt**

```kotlin
package com.example.jaygame.navigation

object Routes {
    const val HOME = "home"
    const val BATTLE = "battle" // launches C++ activity
    const val DECK = "deck"
    const val COLLECTION = "collection"
    const val SHOP = "shop"
    const val SETTINGS = "settings"
    const val SEASON_PASS = "season_pass"
    const val ACHIEVEMENTS = "achievements"
    const val RESULT = "result/{victory}/{waveReached}/{goldEarned}/{trophyChange}"
}
```

**Step 2: Create ComposeActivity.kt**

```kotlin
package com.example.jaygame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.jaygame.data.GameRepository
import com.example.jaygame.navigation.NavGraph
import com.example.jaygame.ui.theme.JayGameTheme

class ComposeActivity : ComponentActivity() {
    lateinit var repository: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = GameRepository(this)

        setContent {
            JayGameTheme {
                NavGraph(
                    repository = repository,
                    onStartBattle = { launchBattle() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        repository.refresh() // reload after returning from battle
    }

    private fun launchBattle() {
        val intent = android.content.Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}
```

**Step 3: Create NavGraph.kt**

```kotlin
package com.example.jaygame.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.jaygame.data.GameRepository
import com.example.jaygame.ui.components.GameBottomNavBar
import com.example.jaygame.ui.components.NavTab
import com.example.jaygame.ui.screens.*

@Composable
fun NavGraph(
    repository: GameRepository,
    onStartBattle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val selectedTab = when (currentRoute) {
        Routes.HOME -> NavTab.HOME
        Routes.DECK -> NavTab.DECK
        Routes.COLLECTION -> NavTab.COLLECTION
        Routes.SHOP -> NavTab.SHOP
        else -> NavTab.HOME
    }

    val showBottomBar = currentRoute in listOf(
        Routes.HOME, Routes.DECK, Routes.COLLECTION, Routes.SHOP,
    )

    Column(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.weight(1f),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    repository = repository,
                    onNavigate = { route -> navController.navigate(route) },
                    onStartBattle = onStartBattle,
                )
            }
            composable(Routes.DECK) {
                DeckScreen(repository = repository)
            }
            composable(Routes.COLLECTION) {
                CollectionScreen(repository = repository)
            }
            composable(Routes.SHOP) {
                ShopScreen(repository = repository)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SEASON_PASS) {
                SeasonPassScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ACHIEVEMENTS) {
                AchievementsScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        if (showBottomBar) {
            GameBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    val route = when (tab) {
                        NavTab.BATTLE -> { onStartBattle(); return@GameBottomNavBar }
                        NavTab.DECK -> Routes.DECK
                        NavTab.HOME -> Routes.HOME
                        NavTab.COLLECTION -> Routes.COLLECTION
                        NavTab.SHOP -> Routes.SHOP
                    }
                    navController.navigate(route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
```

**Step 4: Update AndroidManifest.xml**

Make ComposeActivity the launcher; MainActivity becomes battle-only:
```xml
<activity
    android:name=".ComposeActivity"
    android:exported="true"
    android:theme="@style/Theme.JayGame"
    android:launchMode="singleTop">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<activity
    android:name=".MainActivity"
    android:exported="false"
    android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|keyboard|density|uiMode|smallestScreenSize|navigation"
    android:launchMode="singleTop">
    <meta-data
        android:name="android.app.lib_name"
        android:value="jaygame" />
</activity>
```

**Step 5: Commit**
```bash
git add app/src/main/java/com/example/jaygame/ComposeActivity.kt
git add app/src/main/java/com/example/jaygame/navigation/
git add app/src/main/AndroidManifest.xml
git commit -m "feat: add ComposeActivity with navigation graph and bottom nav bar"
```

---

## Phase 3: Screen Implementations

### Task 8: Home Screen

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt`

Full lobby with: FullHeader, battle start button, deck preview, daily login popup, season pass / achievement entry buttons, background with medieval feel.

### Task 9: Deck Edit Screen

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/screens/DeckScreen.kt`

5-slot deck area at top, scrollable unit grid below, tap to assign/remove. CurrencyHeader.

### Task 10: Collection Screen

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/screens/CollectionScreen.kt`

Unit grid (3 columns), selected unit detail panel, upgrade button with cost. Rarity-colored borders. CurrencyHeader.

### Task 11: Shop Screen

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/screens/ShopScreen.kt`

Tabs: 골드팩 / 다이아팩 / 스페셜. Product cards with medieval styling. CurrencyHeader.

### Task 12: Settings Screen

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/screens/SettingsScreen.kt`

Sound/music toggles, language, version info. Minimal header with back button.

### Task 13: Season Pass Screen

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/screens/SeasonPassScreen.kt`

Horizontal scrolling tier track with free/paid rows. Progress bar per tier. CurrencyHeader.

### Task 14: Achievements Screen

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/screens/AchievementsScreen.kt`

Category tabs (전투/합성/수집/경제/특수). Achievement list items with progress. Minimal header.

### Task 15: Result Screen (Compose overlay or C++ keep)

**Decision:** Result screen needs to receive data from C++ battle. Options:
- A: Keep ResultScene in C++, only replace lobby screens → simpler
- B: Pass battle result via Intent extras, render in Compose → consistent look

**Recommended: B** — BattleScene finishes, passes result via Intent back to ComposeActivity.

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/screens/ResultScreen.kt`
- Modify: `app/src/main/cpp/engine/BattleScene.cpp` — on battle end, call JNI to finish activity with result data

---

## Phase 4: Polish & Integration

### Task 16: Update C++ Battle → Compose Result Bridge

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/MainActivity.kt` — setResult() with battle data
- Modify: `app/src/main/java/com/example/jaygame/ComposeActivity.kt` — startActivityForResult handling
- Modify C++ to signal battle completion via JNI

### Task 17: Daily Login Popup Component

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/components/DailyLoginDialog.kt`

7-day grid with reward icons, current day highlighted, claim button.

### Task 18: Animations & Transitions

Add Compose animation to:
- Screen transitions (fade + slide)
- Button press effects (scale)
- Gold/diamond counter changes (animated number)
- Daily login popup (scale in/out)

### Task 19: Final Integration Testing

- Verify SharedPreferences data round-trips correctly between C++ and Kotlin
- Test all navigation flows
- Test battle launch and return
- Test all screen rotations (portrait + landscape)
- Verify daily login, achievements, season pass logic

### Task 20: Commit & Clean Up

- Remove old C++ scene code for replaced screens (LobbyScene, CollectionScene, etc.)
- Update CMakeLists.txt to remove unused scene files
- Final commit

---

## Asset Acquisition Guide

### Fonts
- **MedievalSharp** — Google Fonts, free, OFL license
  - Download: `https://fonts.google.com/specimen/MedievalSharp`
  - Place in `res/font/`

### Icons (Vector Drawables)
- **Create by hand** using Android Vector Drawable XML format
- Reference: Material Icons for base shapes, modify with medieval styling
- Can also use https://materialdesignicons.com for base SVGs and customize

### Unit Art
- **Option 1:** Create simple vector drawables (geometric/iconic style)
- **Option 2:** Use free game assets from OpenGameArt.org (check licenses)
- **Option 3:** Use AI image generation (DALL-E, Midjourney) for unit portraits, then convert to Android resources

### Backgrounds & Textures
- **Parchment/wood textures:** Generate via Compose Canvas gradients (no files needed)
- **If bitmap textures desired:** Free textures from textures.com or ambientCG.com (CC0 license)
