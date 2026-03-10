# Stitch UI Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Stitch 참조 디자인에 맞게 홈 화면과 배틀 화면을 전면 리디자인하고, 스테이지/스태미나/난이도 시스템을 추가한다.

**Architecture:** Compose 홈 화면 리디자인 + C++ 배틀 HUD 수정 + Compose 배틀 오버레이(하단 탭). 데이터 모델 확장 → 홈 화면 → 배틀 C++ → 배틀 오버레이 순서로 점진적 마이그레이션.

**Tech Stack:** Kotlin/Jetpack Compose, C++/OpenGL ES 3.0, SharedPreferences JSON bridge, JNI

---

### Task 1: 데이터 모델 확장 — StageData.kt 생성

**Files:**
- Create: `app/src/main/java/com/example/jaygame/data/StageData.kt`

**Step 1: StageData.kt 작성**

```kotlin
package com.example.jaygame.data

data class StageDef(
    val id: Int,
    val name: String,
    val description: String,
    val staminaCost: Int,
    val unlockTrophies: Int,
    val maxWaves: Int,
)

val STAGES = listOf(
    StageDef(0, "초원", "평화로운 시작의 땅", 5, 0, 40),
    StageDef(1, "정글", "울창한 밀림 속 전투", 5, 200, 40),
    StageDef(2, "사막", "뜨거운 모래 위의 사투", 6, 500, 45),
    StageDef(3, "설산", "얼어붙은 봉우리", 6, 1000, 45),
    StageDef(4, "화산", "불타는 대지", 7, 2000, 50),
    StageDef(5, "심연", "최후의 시련", 8, 3500, 60),
)
```

**Step 2: 빌드 검증**

Run: `cd /c/Users/Infocar/AndroidStudioProjects/JayGame && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/jaygame/data/StageData.kt
git commit -m "feat: add StageData definitions for 6 stages"
```

---

### Task 2: 데이터 모델 확장 — GameData에 스태미나/스테이지/난이도 필드 추가

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/data/GameData.kt`

**Step 1: GameData.kt에 필드 추가**

`saveVersion` 줄 바로 위에 다음 필드들을 추가:

```kotlin
    // 스태미나
    val stamina: Int = 30,
    val maxStamina: Int = 30,
    val lastStaminaRegenTime: Long = System.currentTimeMillis(),

    // 스테이지
    val currentStageId: Int = 0,
    val unlockedStages: List<Int> = listOf(0),
    val stageBestWaves: List<Int> = List(6) { 0 },

    // 난이도
    val difficulty: Int = 0,  // 0=쉬움, 1=보통, 2=어려움
```

**Step 2: 빌드 검증**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/jaygame/data/GameData.kt
git commit -m "feat: add stamina, stage, difficulty fields to GameData"
```

---

### Task 3: GameRepository 직렬화/역직렬화에 새 필드 추가

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/data/GameRepository.kt`

**Step 1: serialize()에 새 필드 추가**

`root.put("saveVersion", ...)` 줄 바로 위에:

```kotlin
            // stamina
            val staminaObj = JSONObject()
            staminaObj.put("stamina", data.stamina)
            staminaObj.put("maxStamina", data.maxStamina)
            staminaObj.put("lastStaminaRegenTime", data.lastStaminaRegenTime)
            root.put("staminaData", staminaObj)

            // stage
            val stageObj = JSONObject()
            stageObj.put("currentStageId", data.currentStageId)
            val unlockedArr = JSONArray()
            for (s in data.unlockedStages) unlockedArr.put(s)
            stageObj.put("unlockedStages", unlockedArr)
            val bestArr = JSONArray()
            for (b in data.stageBestWaves) bestArr.put(b)
            stageObj.put("stageBestWaves", bestArr)
            root.put("stageData", stageObj)

            // difficulty
            root.put("difficulty", data.difficulty)
```

**Step 2: deserialize()에 새 필드 파싱 추가**

`val saveVersion = ...` 줄 바로 위에:

```kotlin
            // stamina
            val staminaData = root.optJSONObject("staminaData")
            val stamina = staminaData?.optInt("stamina", 30) ?: 30
            val maxStamina = staminaData?.optInt("maxStamina", 30) ?: 30
            val lastStaminaRegenTime = staminaData?.optLong("lastStaminaRegenTime", System.currentTimeMillis())
                ?: System.currentTimeMillis()

            // stage
            val stageData = root.optJSONObject("stageData")
            val currentStageId = stageData?.optInt("currentStageId", 0) ?: 0
            val unlockedStages = mutableListOf<Int>()
            val unlockedArr = stageData?.optJSONArray("unlockedStages")
            if (unlockedArr != null) {
                for (i in 0 until unlockedArr.length()) unlockedStages.add(unlockedArr.getInt(i))
            }
            if (unlockedStages.isEmpty()) unlockedStages.add(0)
            val stageBestWaves = mutableListOf<Int>()
            val bestArr = stageData?.optJSONArray("stageBestWaves")
            if (bestArr != null) {
                for (i in 0 until bestArr.length()) stageBestWaves.add(bestArr.getInt(i))
            }
            while (stageBestWaves.size < 6) stageBestWaves.add(0)

            // difficulty
            val difficulty = root.optInt("difficulty", 0)
```

**Step 3: return GameData(...)에 새 필드 추가**

`saveVersion = saveVersion,` 줄 위에:

```kotlin
                stamina = stamina,
                maxStamina = maxStamina,
                lastStaminaRegenTime = lastStaminaRegenTime,
                currentStageId = currentStageId,
                unlockedStages = unlockedStages,
                stageBestWaves = stageBestWaves,
                difficulty = difficulty,
```

**Step 4: 빌드 검증**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/jaygame/data/GameRepository.kt
git commit -m "feat: serialize/deserialize stamina, stage, difficulty in GameRepository"
```

---

### Task 4: 스태미나 매니저 유틸

**Files:**
- Create: `app/src/main/java/com/example/jaygame/data/StaminaManager.kt`

**Step 1: StaminaManager.kt 작성**

```kotlin
package com.example.jaygame.data

object StaminaManager {
    private const val REGEN_INTERVAL_MS = 5 * 60 * 1000L // 5분

    /**
     * 경과 시간 기반으로 현재 스태미나를 계산하고 갱신된 GameData를 반환.
     */
    fun refreshStamina(data: GameData): GameData {
        if (data.stamina >= data.maxStamina) {
            return data.copy(lastStaminaRegenTime = System.currentTimeMillis())
        }
        val now = System.currentTimeMillis()
        val elapsed = now - data.lastStaminaRegenTime
        val regenCount = (elapsed / REGEN_INTERVAL_MS).toInt()
        if (regenCount <= 0) return data
        val newStamina = (data.stamina + regenCount).coerceAtMost(data.maxStamina)
        return data.copy(
            stamina = newStamina,
            lastStaminaRegenTime = data.lastStaminaRegenTime + regenCount * REGEN_INTERVAL_MS,
        )
    }

    /**
     * 다음 스태미나 회복까지 남은 시간 (밀리초). 이미 최대면 0.
     */
    fun msUntilNextRegen(data: GameData): Long {
        if (data.stamina >= data.maxStamina) return 0L
        val now = System.currentTimeMillis()
        val elapsed = now - data.lastStaminaRegenTime
        val remaining = REGEN_INTERVAL_MS - (elapsed % REGEN_INTERVAL_MS)
        return remaining
    }

    /**
     * 스태미나 소모. 부족하면 null 반환.
     */
    fun consume(data: GameData, amount: Int): GameData? {
        val refreshed = refreshStamina(data)
        if (refreshed.stamina < amount) return null
        return refreshed.copy(stamina = refreshed.stamina - amount)
    }
}
```

**Step 2: GameRepository.refresh()에서 스태미나 갱신**

`GameRepository.kt`의 `refresh()` 함수 수정:

```kotlin
    fun refresh() {
        val raw = load()
        _gameData.value = StaminaManager.refreshStamina(raw)
    }
```

그리고 `load()` 호출하는 `_gameData` 초기화도:

```kotlin
    private val _gameData = MutableStateFlow(StaminaManager.refreshStamina(load()))
```

**Step 3: 빌드 검증**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/jaygame/data/StaminaManager.kt \
       app/src/main/java/com/example/jaygame/data/GameRepository.kt
git commit -m "feat: add StaminaManager with time-based regen"
```

---

### Task 5: ProfileHeader 컴포넌트

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/components/ResourceBar.kt`

**Step 1: FullHeader를 ProfileHeader로 교체**

`ResourceBar.kt`에서 기존 `FullHeader`를 완전히 새로운 `ProfileHeader`로 교체한다. `FullHeader` 함수를 제거하고 다음으로 교체:

```kotlin
@Composable
fun ProfileHeader(
    playerName: String,
    level: Int,
    gold: Int,
    diamonds: Int,
    stamina: Int,
    maxStamina: Int,
    modifier: Modifier = Modifier,
) {
    WoodFrame(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
        ) {
            // 프로필: 아바타 + 이름 + 레벨
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(LeatherBrown),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = playerName.take(1),
                    fontFamily = MedievalFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Parchment,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = playerName,
                    fontFamily = MedievalFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Parchment,
                )
                Text(
                    text = "Lv.$level",
                    fontFamily = MedievalFont,
                    fontSize = 11.sp,
                    color = Gold,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 재화: 다이아 + 골드 + 스태미나
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ResourceItem(iconRes = R.drawable.ic_diamond, value = diamonds, tintColor = DiamondBlue)
                ResourceItem(iconRes = R.drawable.ic_gold, value = gold, tintColor = GoldCoin)
                StaminaItem(stamina = stamina, maxStamina = maxStamina)
            }
        }
    }
}

@Composable
fun StaminaItem(
    stamina: Int,
    maxStamina: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_stamina),
            contentDescription = null,
            tint = StaminaGreen,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$stamina/$maxStamina",
            fontFamily = MedievalFont,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Parchment,
        )
    }
}
```

필요한 import 추가:
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.example.jaygame.ui.theme.LeatherBrown
import com.example.jaygame.ui.theme.StaminaGreen
```

**Step 2: ic_stamina 아이콘 생성**

`app/src/main/res/drawable/ic_stamina.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#66BB6A"
        android:pathData="M11,21h-1l1,-7H7.5c-0.58,0 -0.57,-0.32 -0.38,-0.66 0.19,-0.34 0.05,-0.08 0.07,-0.12C8.48,10.94 10.42,7.54 13,3h1l-1,7h3.5c0.49,0 0.56,0.33 0.47,0.51l-0.07,0.15C12.96,17.55 11,21 11,21z"/>
</vector>
```

**Step 3: HomeScreen에서 FullHeader → ProfileHeader로 교체**

`HomeScreen.kt`에서:
- `import com.example.jaygame.ui.components.FullHeader` 를 `import com.example.jaygame.ui.components.ProfileHeader`로 변경
- `FullHeader(level = data.playerLevel, trophies = data.trophies, gold = data.gold, diamonds = data.diamonds)` 를 아래로 교체:

```kotlin
        ProfileHeader(
            playerName = "플레이어",
            level = data.playerLevel,
            gold = data.gold,
            diamonds = data.diamonds,
            stamina = data.stamina,
            maxStamina = data.maxStamina,
        )
```

**Step 4: 다른 화면에서 FullHeader 사용하는 곳 확인 및 수정**

`CurrencyHeader`는 유지. `FullHeader`를 사용하는 다른 화면이 있다면 `ProfileHeader`로 교체하거나, 그 화면에서는 `CurrencyHeader`를 사용.

**Step 5: 빌드 검증**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/jaygame/ui/components/ResourceBar.kt \
       app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt \
       app/src/main/res/drawable/ic_stamina.xml
git commit -m "feat: replace FullHeader with ProfileHeader including stamina display"
```

---

### Task 6: StageCard 컴포넌트

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/components/StageCard.kt`

**Step 1: StageCard.kt 작성**

```kotlin
package com.example.jaygame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.StageDef
import com.example.jaygame.data.STAGES
import com.example.jaygame.ui.theme.*

private val stageColors = listOf(
    listOf(Color(0xFF2E7D32), Color(0xFF4CAF50)),  // 초원
    listOf(Color(0xFF1B5E20), Color(0xFF388E3C)),  // 정글
    listOf(Color(0xFFE65100), Color(0xFFFF9800)),  // 사막
    listOf(Color(0xFF42A5F5), Color(0xFFBBDEFB)),  // 설산
    listOf(Color(0xFFBF360C), Color(0xFFFF5722)),  // 화산
    listOf(Color(0xFF311B92), Color(0xFF7C4DFF)),  // 심연
)

@Composable
fun StageCardPager(
    currentStageId: Int,
    unlockedStages: List<Int>,
    stageBestWaves: List<Int>,
    difficulty: Int,
    onStageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = currentStageId.coerceIn(0, STAGES.size - 1),
        pageCount = { STAGES.size },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onStageChanged(page)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 24.dp),
        ) { page ->
            val stage = STAGES[page]
            val isUnlocked = page in unlockedStages
            val bestWave = stageBestWaves.getOrElse(page) { 0 }
            val colors = stageColors.getOrElse(page) { stageColors[0] }

            StageCardItem(
                stage = stage,
                isUnlocked = isUnlocked,
                bestWave = bestWave,
                difficulty = difficulty,
                gradientColors = colors,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 페이지 인디케이터
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            STAGES.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) Gold
                            else Parchment.copy(alpha = 0.4f)
                        ),
                )
            }
        }
    }
}

@Composable
private fun StageCardItem(
    stage: StageDef,
    isUnlocked: Boolean,
    bestWave: Int,
    difficulty: Int,
    gradientColors: List<Color>,
) {
    val difficultyText = when (difficulty) {
        0 -> "쉬움"
        1 -> "보통"
        2 -> "어려움"
        else -> "보통"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (isUnlocked) gradientColors
                    else listOf(DarkMetal, MetalGray),
                )
            )
            .padding(16.dp),
    ) {
        if (bestWave > 0) {
            // BEST 뱃지
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Gold.copy(alpha = 0.9f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "BEST",
                    fontFamily = MedievalFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = DarkBrown,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stage.name,
                fontFamily = MedievalFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Parchment,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isUnlocked) {
                    if (bestWave > 0) "ROUND $bestWave" else "미도전"
                } else {
                    "🏆 ${stage.unlockTrophies} 필요"
                },
                fontFamily = MedievalFont,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = if (isUnlocked) Parchment else Parchment.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
            if (isUnlocked) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "난이도: $difficultyText",
                    fontFamily = MedievalFont,
                    fontSize = 13.sp,
                    color = Parchment.copy(alpha = 0.7f),
                )
            }
        }
    }
}
```

**Step 2: 빌드 검증**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/jaygame/ui/components/StageCard.kt
git commit -m "feat: add StageCard swipeable pager component"
```

---

### Task 7: RankBadge + DifficultyDialog 컴포넌트

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/components/RankBadge.kt`

**Step 1: RankBadge.kt 작성**

```kotlin
package com.example.jaygame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.ui.theme.*

data class RankInfo(
    val name: String,
    val color: Color,
)

fun getRankInfo(trophies: Int): RankInfo = when {
    trophies >= 4000 -> RankInfo("마스터", Color(0xFFFF6F00))
    trophies >= 3000 -> RankInfo("다이아몬드", DiamondBlue)
    trophies >= 2000 -> RankInfo("골드", Gold)
    trophies >= 1000 -> RankInfo("실버", MetalGray)
    else -> RankInfo("브론즈", Color(0xFFCD7F32))
}

@Composable
fun RankBadge(
    trophies: Int,
    modifier: Modifier = Modifier,
) {
    val rank = getRankInfo(trophies)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MediumBrown)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = "🏆",
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "RANK",
            fontFamily = MedievalFont,
            fontSize = 10.sp,
            color = Parchment.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = rank.name,
            fontFamily = MedievalFont,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = rank.color,
        )
    }
}

@Composable
fun DifficultyDialog(
    currentDifficulty: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        Triple(0, "쉬움", "적 체력 -20%, 보상 -20%"),
        Triple(1, "보통", "기본 난이도"),
        Triple(2, "어려움", "적 체력 +50%, 보상 +50%"),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "난이도 선택",
                fontFamily = MedievalFont,
                fontWeight = FontWeight.Bold,
                color = Gold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (id, name, desc) ->
                    val isSelected = id == currentDifficulty
                    MedievalButton(
                        text = "$name — $desc",
                        onClick = { onSelect(id) },
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 13.sp,
                        accentColor = if (isSelected) Gold else MetalGray,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = Parchment)
            }
        },
        containerColor = DarkBrown,
    )
}
```

**Step 2: 빌드 검증**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/jaygame/ui/components/RankBadge.kt
git commit -m "feat: add RankBadge and DifficultyDialog components"
```

---

### Task 8: HomeScreen 리디자인

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt`

**Step 1: HomeScreen 전체 재작성**

기존 HomeScreen의 레이아웃을 Stitch 디자인에 맞게 교체한다.

```kotlin
package com.example.jaygame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.STAGES
import com.example.jaygame.data.StaminaManager
import com.example.jaygame.navigation.Routes
import com.example.jaygame.ui.components.*
import com.example.jaygame.ui.theme.*

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
                text = "⚡${stage.staminaCost}  전투 시작",
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
```

**Step 2: 불필요한 import 및 private 함수 정리**

기존 `ShimmerTitle`, `DeckSlot` private 함수를 제거한다 (더 이상 사용되지 않음).

**Step 3: 빌드 검증**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt
git commit -m "feat: redesign HomeScreen with StageCard, ProfileHeader, RankBadge"
```

---

### Task 9: C++ 배틀 상단 HUD 재배치 + 타이머 추가

**Files:**
- Modify: `app/src/main/cpp/engine/BattleScene.h`
- Modify: `app/src/main/cpp/engine/BattleScene.cpp`

**Step 1: BattleScene.h에 타이머 필드 추가**

`float waveDelayTimer_` 근처에:
```cpp
    float waveTimer_ = 0.f;  // 웨이브 경과 시간
```

**Step 2: BattleScene.cpp — onUpdate에서 타이머 증가**

`case State::Playing` 블록의 `sp_ += SP_REGEN_RATE * dt;` 바로 아래에:
```cpp
            waveTimer_ += dt;
```

`startNextWave()` 함수에서 `state_ = State::Playing;` 바로 위에:
```cpp
    waveTimer_ = 0.f;
```

**Step 3: renderHUD를 Stitch 디자인 레이아웃으로 재작성**

기존 `renderHUD` 전체를 다음으로 교체:

```cpp
void BattleScene::renderHUD(SpriteBatch& batch) {
    auto& text = engine_.getTextRenderer();
    const auto& tex = *atlas_.getTexture();
    const auto& wp = atlas_.getWhitePixel();
    const auto& panel = atlas_.getHud("panel");
    char buf[64];

    // === 상단 HUD 바 (가로 일직선) ===
    // 배경 패널
    batch.draw(tex, 0.f, 0.f, 1280.f, 50.f,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               0.1f, 0.08f, 0.06f, 0.85f);

    // HP (좌측)
    const auto& hpIcon = atlas_.getHud("icon_hp");
    batch.draw(tex, {10.f, 8.f}, {28.f, 28.f},
               hpIcon.uvRect, {1.f,1.f,1.f,1.f}, 0.f, {0.f,0.f});
    snprintf(buf, sizeof(buf), "%d", playerHP_);
    float hpRatio = static_cast<float>(playerHP_) / 20.f;
    Vec4 hpColor = hpRatio > 0.5f ? Vec4{0.3f, 1.f, 0.4f, 1.f}
                                    : Vec4{1.f, 0.3f, 0.3f, 1.f};
    text.drawText(batch, buf, 42.f, 15.f, 3.f, hpColor);

    // Round (중앙 좌측)
    snprintf(buf, sizeof(buf), "Round %d/%d", currentWave_, MAX_WAVES);
    text.drawText(batch, buf, 400.f, 15.f, 2.8f, {0.5f, 0.8f, 1.f, 1.f}, TextAlign::Center);

    // 타이머 (중앙)
    int minutes = static_cast<int>(waveTimer_) / 60;
    int seconds = static_cast<int>(waveTimer_) % 60;
    snprintf(buf, sizeof(buf), "%02d:%02d", minutes, seconds);
    text.drawText(batch, buf, 640.f, 15.f, 2.8f, {1.f, 1.f, 1.f, 0.9f}, TextAlign::Center);

    // SP 바 (우측)
    const auto& spIcon = atlas_.getHud("icon_sp");
    batch.draw(tex, {880.f, 8.f}, {28.f, 28.f},
               spIcon.uvRect, {1.f,1.f,1.f,1.f}, 0.f, {0.f,0.f});

    // SP 바 배경
    batch.draw(tex, 916.f, 14.f, 280.f, 22.f,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               0.15f, 0.15f, 0.15f, 0.7f);
    // SP 바 채움
    float spRatio = std::min(sp_ / 200.f, 1.f);
    batch.draw(tex, 916.f, 14.f, 280.f * spRatio, 22.f,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               0.9f, 0.8f, 0.2f, 0.85f);
    // SP 텍스트
    snprintf(buf, sizeof(buf), "SP %.0f/200", sp_);
    text.drawText(batch, buf, 1056.f, 15.f, 2.f, {1.f, 1.f, 1.f, 0.9f}, TextAlign::Center);

    // === Wave delay 오버레이 ===
    if (state_ == State::WaveDelay) {
        float pulse = 0.5f + 0.5f * std::sin(waveDelayTimer_ * 4.f);
        batch.draw(tex, 440.f, 330.f, 400.f, 60.f,
                   panel.uvRect.x, panel.uvRect.y, panel.uvRect.w, panel.uvRect.h,
                   0.6f, 0.6f, 0.8f, 0.9f);
        snprintf(buf, sizeof(buf), "NEXT WAVE IN %.0f", waveDelayTimer_ + 1.f);
        text.drawText(batch, buf, 640.f, 345.f, 3.f,
                      {0.5f, 0.8f, 1.f, pulse}, TextAlign::Center);
    }

    // NOTE: 소환 버튼과 SP 하단 표시는 Compose 오버레이로 이동 예정
    // 현재는 기존 소환 버튼 유지 (Task 11에서 제거)
    // --- Summon button (임시 유지) ---
    float cost = getSummonCost();
    bool canSummon = (sp_ >= cost) && (grid_.getEmptyCellCount() > 0);
    const auto& btnSprite = canSummon
        ? atlas_.getHud("btn_normal")
        : atlas_.getHud("btn_disabled");
    Vec4 btnTint = canSummon
        ? Vec4{0.7f, 1.f, 0.8f, 0.95f}
        : Vec4{0.7f, 0.5f, 0.5f, 0.7f};
    batch.draw(tex,
               SUMMON_BUTTON.x, SUMMON_BUTTON.y, SUMMON_BUTTON.w, SUMMON_BUTTON.h,
               btnSprite.uvRect.x, btnSprite.uvRect.y,
               btnSprite.uvRect.w, btnSprite.uvRect.h,
               btnTint.x, btnTint.y, btnTint.z, btnTint.w);
    const auto& summonIcon = atlas_.getHud("icon_summon");
    batch.draw(tex,
               {SUMMON_BUTTON.x + SUMMON_BUTTON.w * 0.5f - 10.f, SUMMON_BUTTON.y + 5.f},
               {20.f, 20.f},
               summonIcon.uvRect, {1.f,1.f,1.f,0.9f}, 0.f, {0.f,0.f});
    snprintf(buf, sizeof(buf), "SUMMON");
    text.drawText(batch, buf,
                  SUMMON_BUTTON.x + SUMMON_BUTTON.w * 0.5f,
                  SUMMON_BUTTON.y + 28.f,
                  2.f, {1.f, 1.f, 1.f, 0.9f}, TextAlign::Center);
    snprintf(buf, sizeof(buf), "%.0f SP", cost);
    text.drawText(batch, buf,
                  SUMMON_BUTTON.x + SUMMON_BUTTON.w * 0.5f,
                  SUMMON_BUTTON.y + 50.f,
                  1.8f, {0.9f, 0.8f, 0.3f, 0.8f}, TextAlign::Center);
}
```

**Step 4: 빌드 검증**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL (4 ABIs)

**Step 5: Commit**

```bash
git add app/src/main/cpp/engine/BattleScene.h app/src/main/cpp/engine/BattleScene.cpp
git commit -m "feat: redesign battle HUD with horizontal layout and wave timer"
```

---

### Task 10: C++ 유닛 원형 아이콘 렌더링

**Files:**
- Modify: `app/src/main/cpp/engine/Unit.cpp` (render 함수)
- Modify: `app/src/main/cpp/engine/SpriteAtlas.h` (원형 베이스 UVs)
- Modify: `app/src/main/cpp/engine/SpriteAtlas.cpp` (원형 타일 초기화)

**Step 1: SpriteAtlas에 원형 베이스 타일 등록**

`SpriteAtlas.h`의 `initTiles()` 또는 `initHud()` 섹션에서:
아틀라스 row 9에 원형 베이스 스프라이트가 있다고 가정. 기존 아틀라스에 없으면 화이트 픽셀 기반으로 프로그래밍 원형을 대체.

실질적 접근: 유닛 렌더링에서 기존 사각형 스프라이트 대신, **화이트 픽셀로 채운 원형 비주얼**을 만든다. OpenGL에서 완전한 원을 그리려면 삼각형 팬이 필요하지만, 간단히 사각형 + 각 유닛 타입별 컬러로 충분히 Stitch 디자인을 근사할 수 있다.

**Step 2: Unit.cpp render() 수정**

기존 Unit::render()에서 아틀라스 스프라이트 대신 컬러 사각형 + 심볼 렌더링으로 변경:

```cpp
void Unit::render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) {
    if (!active) return;

    const auto& tex = *atlas.getTexture();
    const auto& wp = atlas.getWhitePixel();
    float cx = pos.x;
    float cy = pos.y;
    float radius = 24.f;

    // 유닛 타입별 컬러
    static const Vec4 unitColors[] = {
        {0.9f, 0.3f, 0.3f, 1.f},  // 0: 빨강
        {0.3f, 0.5f, 0.9f, 1.f},  // 1: 파랑
        {0.3f, 0.9f, 0.4f, 1.f},  // 2: 초록
        {0.9f, 0.9f, 0.3f, 1.f},  // 3: 노랑
        {0.8f, 0.3f, 0.9f, 1.f},  // 4: 보라
        {0.9f, 0.6f, 0.2f, 1.f},  // 5: 주황
        {0.3f, 0.9f, 0.9f, 1.f},  // 6: 청록
        {0.9f, 0.5f, 0.6f, 1.f},  // 7: 분홍
        {0.6f, 0.6f, 0.6f, 1.f},  // 8: 회색
        {1.0f, 0.8f, 0.4f, 1.f},  // 9: 금색
        {0.4f, 0.3f, 0.8f, 1.f},  // 10: 남색
        {0.2f, 0.7f, 0.5f, 1.f},  // 11: 에메랄드
        {0.8f, 0.2f, 0.5f, 1.f},  // 12: 자홍
        {0.5f, 0.8f, 0.2f, 1.f},  // 13: 연두
        {0.7f, 0.4f, 0.2f, 1.f},  // 14: 갈색
    };

    int colorIdx = unitDefId % 15;
    Vec4 color = unitColors[colorIdx];

    // 밝기: 레벨에 따라 약간 밝아짐
    float brightness = 1.0f + (level - 1) * 0.05f;
    color.x = std::min(color.x * brightness, 1.f);
    color.y = std::min(color.y * brightness, 1.f);
    color.z = std::min(color.z * brightness, 1.f);

    // 원형 베이스 (사각형으로 근사, 아틀라스에 원형 스프라이트가 있으면 그것 사용)
    batch.draw(tex,
               cx - radius, cy - radius, radius * 2.f, radius * 2.f,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               color.x, color.y, color.z, color.w);

    // 외곽선 (약간 더 큰 사각형, 어두운 색)
    float outR = radius + 2.f;
    batch.draw(tex,
               cx - outR, cy - outR, outR * 2.f, outR * 2.f,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               color.x * 0.5f, color.y * 0.5f, color.z * 0.5f, 0.6f);
    // 내부 원 (위에 다시 그려서 외곽선 효과)
    batch.draw(tex,
               cx - radius, cy - radius, radius * 2.f, radius * 2.f,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               color.x, color.y, color.z, color.w);

    // 레벨 별 표시 (하단)
    auto& textR = batch.getTextRenderer(); // 주의: TextRenderer 접근이 필요
    // 별 대신 레벨 숫자로 간단히 표시
    char buf[8];
    snprintf(buf, sizeof(buf), "Lv%d", level);
    // 텍스트는 BattleScene에서 처리하는 것이 더 나을 수 있음
    // 여기서는 단순 컬러 원만 렌더링

    // 공격 중 애니메이션 (펄스 효과)
    if (attacking_) {
        float pulse = 0.5f + 0.5f * std::sin(attackAnimTimer_ * 20.f);
        float pR = radius + 4.f * pulse;
        batch.draw(tex,
                   cx - pR, cy - pR, pR * 2.f, pR * 2.f,
                   wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
                   1.f, 1.f, 1.f, 0.2f * pulse);
    }
}
```

주의: 기존 render()는 아틀라스 스프라이트 + 별 아이콘 + 버프 아이콘을 그리고 있다. 이를 위 코드로 **완전 교체**한다. TextRenderer는 Unit에서 직접 접근하기 어려우므로, 레벨 표시는 BattleScene의 onRender에서 별도로 처리하거나 생략한다.

**Step 3: BattleScene::onRender에서 유닛 레벨 텍스트 렌더링**

유닛 렌더링 후, 레벨 텍스트를 별도로 그린다:

```cpp
    // 4.5. Render unit level text
    {
        auto& tr = engine_.getTextRenderer();
        char buf[8];
        unitPool_.forEach([&](Unit& unit) {
            if (!unit.active) return;
            snprintf(buf, sizeof(buf), "%d", unit.level);
            tr.drawText(batch, buf, unit.pos.x, unit.pos.y + 14.f, 2.f,
                        {1.f, 1.f, 1.f, 0.9f}, TextAlign::Center);
        });
    }
```

**Step 4: 빌드 검증**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/cpp/engine/Unit.cpp app/src/main/cpp/engine/BattleScene.cpp
git commit -m "feat: render units as colored circle icons with level text"
```

---

### Task 11: MainActivity에 Compose 오버레이 기반 추가

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/MainActivity.kt`
- Modify: `app/build.gradle.kts` (Compose 의존성이 이미 있는지 확인)

**Step 1: MainActivity에 ComposeView 오버레이 추가**

현재 `MainActivity`는 `GameActivity`를 상속하며 순수 C++ 렌더링만 한다. `onCreate`에서 Compose 오버레이를 추가한다.

```kotlin
package com.example.jaygame

import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.jaygame.ui.components.MedievalButton
import com.example.jaygame.ui.theme.*
import com.google.androidgamesdk.GameActivity

class MainActivity : GameActivity() {
    companion object {
        init {
            System.loadLibrary("jaygame")
        }
    }

    private var backPressedTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - backPressedTime < 2000) {
                    finish()
                } else {
                    backPressedTime = System.currentTimeMillis()
                    Toast.makeText(this@MainActivity, "한 번 더 누르면 종료됩니다", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // Compose 오버레이를 C++ SurfaceView 위에 추가
        addBattleOverlay()
    }

    private fun addBattleOverlay() {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
            setContent {
                JayGameTheme {
                    BattleOverlayContent()
                }
            }
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM,
        )
        addContentView(composeView, params)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUi()
        }
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
private fun BattleOverlayContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBrown.copy(alpha = 0.9f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 소환 버튼
        MedievalButton(
            text = "⚡ 소환 SUMMON",
            onClick = { /* TODO: JNI call */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            fontSize = 18.sp,
            accentColor = Gold,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 하단 탭 바
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val tabs = listOf("에테르", "미션", "롤러럭키", "스크롤", "강화")
            tabs.forEach { tab ->
                MedievalButton(
                    text = tab,
                    onClick = { /* 준비 중 */ },
                    fontSize = 11.sp,
                    accentColor = MetalGray,
                )
            }
        }
    }
}
```

**주의**: `GameActivity`는 `ComponentActivity`가 아니라서 `setViewTreeLifecycleOwner`와 `setViewTreeSavedStateRegistryOwner`가 필요하다. `GameActivity`가 이미 `ComponentActivity`를 상속하는 경우 불필요. 빌드 오류 발생 시 조정.

**Step 2: 빌드 검증**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/jaygame/MainActivity.kt
git commit -m "feat: add Compose battle overlay on top of C++ GameActivity"
```

---

### Task 12: 스테이지 → C++ 연동 (SharedPreferences bridge)

**Files:**
- Modify: `app/src/main/cpp/engine/PlayerData.h`
- Modify: `app/src/main/cpp/engine/PlayerData.cpp`
- Modify: `app/src/main/cpp/engine/BattleScene.h`
- Modify: `app/src/main/cpp/engine/BattleScene.cpp`

**Step 1: PlayerData에 스테이지/난이도 필드 추가**

`PlayerData.h`에:
```cpp
    int stageId = 0;
    int difficulty = 0;   // 0=쉬움, 1=보통, 2=어려움
    int stageMaxWaves = 40;
```

**Step 2: PlayerData.cpp에서 SharedPreferences 파싱**

`load()` 함수에서 JSON 파싱 시:
```cpp
    // stageData
    if (root.HasMember("stageData") && root["stageData"].IsObject()) {
        auto& stage = root["stageData"];
        stageId = stage.HasMember("currentStageId") ? stage["currentStageId"].GetInt() : 0;
    }
    difficulty = root.HasMember("difficulty") ? root["difficulty"].GetInt() : 0;

    // stageMaxWaves는 stageId로부터 계산
    static const int stageWaves[] = {40, 40, 45, 45, 50, 60};
    stageMaxWaves = (stageId >= 0 && stageId < 6) ? stageWaves[stageId] : 40;
```

**Step 3: BattleScene에서 MAX_WAVES를 동적으로**

`BattleScene.h`에서:
- `static constexpr int MAX_WAVES = 40;` 를 `int maxWaves_ = 40;` 로 변경
- `int difficulty_ = 0;` 추가

`BattleScene.cpp`의 `onEnter()`에서:
```cpp
    maxWaves_ = pd.stageMaxWaves;
    difficulty_ = pd.difficulty;
```

모든 `MAX_WAVES` 참조를 `maxWaves_`로 변경.

적 체력에 난이도 배수 적용:
- `updateEnemies` 또는 `WaveManager::startWave`에서 적 스폰 시 체력 조정
- 쉬움: 0.8배, 보통: 1.0배, 어려움: 1.5배

**Step 4: 빌드 검증**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/cpp/engine/PlayerData.h \
       app/src/main/cpp/engine/PlayerData.cpp \
       app/src/main/cpp/engine/BattleScene.h \
       app/src/main/cpp/engine/BattleScene.cpp
git commit -m "feat: connect stage/difficulty data from SharedPreferences to C++ battle"
```

---

### Task 13: 전투 결과에서 스테이지 best wave 업데이트

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ComposeActivity.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/ResultScreen.kt`

**Step 1: ComposeActivity.onResume에서 스테이지 best wave 갱신**

`ComposeActivity.kt`의 `onResume()`에서 repository.refresh() 후에 스테이지 해금 체크를 추가:

```kotlin
    override fun onResume() {
        super.onResume()
        repository.refresh()
        // 트로피 기반 스테이지 자동 해금
        val data = repository.gameData.value
        val newUnlocked = STAGES.filter { it.unlockTrophies <= data.trophies }.map { it.id }
        if (newUnlocked.toSet() != data.unlockedStages.toSet()) {
            repository.save(data.copy(unlockedStages = newUnlocked))
        }
    }
```

import 추가: `import com.example.jaygame.data.STAGES`

**Step 2: 빌드 검증**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/jaygame/ComposeActivity.kt
git commit -m "feat: auto-unlock stages based on trophies on resume"
```
