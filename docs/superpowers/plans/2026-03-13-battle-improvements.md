# 전투 시스템 개선 & 장기 콘텐츠 플랜

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** B(보스 특수 메카닉, CC 강화, 도박, 천장) + C(던전/이벤트, 프로필/업적 확장) 일괄 구현

**Architecture:** 기존 WaveSystem/BuffSystem/BattleEngine에 기능 추가. 새 시스템은 독립 클래스로 분리 후 BattleEngine에 연동.

**Tech Stack:** Kotlin, Compose UI, 기존 엔진 확장

---

## Part B: 기존 시스템 개선

---

## Chunk 1: 보스 특수 메카닉

### Task 1: BossModifier 시스템 정의

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/BossModifier.kt`

- [ ] **Step 1: BossModifier enum + 데이터 정의**

```kotlin
package com.example.jaygame.engine

/**
 * 보스 웨이브별 특수 속성.
 * 패밀리 선택의 전략성을 높이기 위한 보스 면역/강화 메카닉.
 */
enum class BossModifier(
    val label: String,
    val description: String,
) {
    /** 물리 피해 60% 감소 → 냉기/번개 유리 */
    PHYSICAL_RESIST("강철 피부", "물리 피해 60% 감소"),

    /** 마법 피해 60% 감소 → 화염/독/바람 유리 */
    MAGIC_RESIST("마법 장벽", "마법 피해 60% 감소"),

    /** 둔화/동결 면역 → DoT/딜 패밀리 필요 */
    CC_IMMUNE("불굴의 의지", "둔화/동결 면역"),

    /** DoT 면역 (독 무효) → 직접 딜 패밀리 필요 */
    DOT_IMMUNE("정화의 피", "지속 피해 면역"),

    /** 원거리 공격 50% 감소 (200+ range) → 근접 유닛 유리 */
    RANGED_RESIST("반사 아우라", "원거리 피해 50% 감소"),

    /** 10초마다 체력 5% 회복 → 높은 DPS 필요 */
    REGENERATION("재생", "10초마다 체력 5% 회복"),

    /** 이동속도 2배 → CC 필수 */
    SWIFT("질풍", "이동속도 2배"),

    /** 주변 적 유닛 방어/마저 +50% 아우라 */
    COMMANDER("지휘관", "주변 몬스터 방어 +50%"),
}

/**
 * 스테이지별 보스 모디파이어 매핑.
 * waveIndex (0-based) → BossModifier
 */
fun getBossModifier(stageId: Int, waveIndex: Int): BossModifier? {
    if ((waveIndex + 1) % 10 != 0) return null  // 보스 웨이브만
    val bossNumber = (waveIndex + 1) / 10  // 1, 2, 3, 4...

    return when (stageId) {
        0 -> when (bossNumber) { // 초원
            1 -> null  // 첫 보스는 모디파이어 없음
            2 -> BossModifier.PHYSICAL_RESIST
            3 -> BossModifier.SWIFT
            4 -> BossModifier.REGENERATION
            else -> null
        }
        1 -> when (bossNumber) { // 정글
            1 -> BossModifier.DOT_IMMUNE
            2 -> BossModifier.MAGIC_RESIST
            3 -> BossModifier.CC_IMMUNE
            4 -> BossModifier.COMMANDER
            else -> null
        }
        2 -> when (bossNumber) { // 사막
            1 -> BossModifier.RANGED_RESIST
            2 -> BossModifier.PHYSICAL_RESIST
            3 -> BossModifier.REGENERATION
            4 -> BossModifier.CC_IMMUNE
            else -> BossModifier.entries.random()
        }
        3 -> when (bossNumber) { // 설산
            1 -> BossModifier.MAGIC_RESIST
            2 -> BossModifier.SWIFT
            3 -> BossModifier.DOT_IMMUNE
            4 -> BossModifier.COMMANDER
            else -> BossModifier.entries.random()
        }
        4, 5 -> { // 화산, 심연: 항상 랜덤 모디파이어
            BossModifier.entries.random()
        }
        else -> null
    }
}
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/BossModifier.kt
git commit -m "feat: add BossModifier system for boss special mechanics"
```

---

### Task 2: BossModifier를 Enemy/데미지 계산에 적용

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/engine/Enemy.kt`
- Modify: `app/src/main/java/com/example/jaygame/engine/DamageCalculator.kt`
- Modify: `app/src/main/java/com/example/jaygame/engine/BattleEngine.kt`
- Modify: `app/src/main/java/com/example/jaygame/engine/WaveSystem.kt`

- [ ] **Step 1: Enemy에 modifier 필드 추가**

```kotlin
var bossModifier: BossModifier? = null
```

- [ ] **Step 2: WaveSystem에서 보스 생성 시 modifier 할당**

보스 스폰 로직에서:

```kotlin
if (config.isBoss) {
    enemy.bossModifier = getBossModifier(stageId, waveIndex)
}
```

- [ ] **Step 3: DamageCalculator에서 modifier 반영**

```kotlin
fun calculateDamage(
    baseDmg: Float, isMagic: Boolean, isRanged: Boolean,
    armor: Float, magicResist: Float, armorBreak: Float,
    bossModifier: BossModifier?,
    // ... 기존 파라미터
): Float {
    var dmg = baseDmg

    // 보스 모디파이어 적용
    when (bossModifier) {
        BossModifier.PHYSICAL_RESIST -> if (!isMagic) dmg *= 0.4f
        BossModifier.MAGIC_RESIST -> if (isMagic) dmg *= 0.4f
        BossModifier.RANGED_RESIST -> if (isRanged) dmg *= 0.5f
        else -> { /* 다른 modifier는 별도 처리 */ }
    }

    // 기존 방어/마저 계산...
}
```

- [ ] **Step 4: CC_IMMUNE 처리 — BuffSystem에서**

Enemy에 버프 추가 시 CC_IMMUNE 체크:

```kotlin
fun addBuff(type: BuffType, value: Float, duration: Float, sourceId: Int = -1) {
    // CC 면역 체크 (보스 modifier가 CC_IMMUNE이면 Slow 무시)
    if (bossModifier == BossModifier.CC_IMMUNE && type == BuffType.Slow) return
    // 기존 로직...
}
```

- [ ] **Step 5: DOT_IMMUNE 처리**

```kotlin
if (bossModifier == BossModifier.DOT_IMMUNE && type == BuffType.DoT) return
```

- [ ] **Step 6: REGENERATION — BattleEngine에서**

보스 업데이트 루프에서:

```kotlin
if (enemy.bossModifier == BossModifier.REGENERATION) {
    enemy.regenTimer -= dt
    if (enemy.regenTimer <= 0f) {
        enemy.hp = (enemy.hp + enemy.maxHp * 0.05f).coerceAtMost(enemy.maxHp)
        enemy.regenTimer = 10f
    }
}
```

- [ ] **Step 7: SWIFT — WaveSystem에서 스폰 시**

```kotlin
if (bossModifier == BossModifier.SWIFT) {
    enemy.baseSpeed *= 2f
    enemy.speed = enemy.baseSpeed
}
```

- [ ] **Step 8: COMMANDER — BattleEngine 업데이트에서**

보스 주변 150f 내 적들에게 방어/마저 +50% 버프:

```kotlin
if (enemy.bossModifier == BossModifier.COMMANDER) {
    for (nearby in getEnemiesInRadius(enemy.position, 150f)) {
        nearby.buffs.addBuff(BuffType.ArmorBreak, -enemy.armor * 0.5f, 1f, -99)
        // 또는 별도 commanderBuff 플래그로 처리
    }
}
```

- [ ] **Step 9: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/Enemy.kt \
       app/src/main/java/com/example/jaygame/engine/DamageCalculator.kt \
       app/src/main/java/com/example/jaygame/engine/BattleEngine.kt \
       app/src/main/java/com/example/jaygame/engine/WaveSystem.kt
git commit -m "feat: apply BossModifier effects to damage/buff/wave systems"
```

---

### Task 3: 보스 모디파이어 UI 표시

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/battle/BattleHud.kt` (또는 EnemyOverlay)

- [ ] **Step 1: 보스 등장 시 모디파이어 알림 배너**

보스 웨이브 시작 시 화면 상단에 2초간 표시:

```kotlin
// "⚠️ 보스: 강철 피부 — 물리 피해 60% 감소"
AnimatedVisibility(visible = showBossAlert) {
    Box(
        Modifier.fillMaxWidth().background(Color(0xCC000000)).padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "⚠️ 보스: ${modifier.label} — ${modifier.description}",
            color = Color(0xFFFF6B6B),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
```

- [ ] **Step 2: 보스 체력바 옆에 모디파이어 아이콘**

보스 체력바 UI에 모디파이어 라벨 표시.

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/battle/BattleHud.kt
git commit -m "feat: show boss modifier alert and icon in battle HUD"
```

---

## Chunk 2: CC(제어) 역할 강화

### Task 4: 새 디버프 타입 추가 — Stun, Silence

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/engine/BuffSystem.kt`

- [ ] **Step 1: BuffType에 Stun, Silence 추가**

```kotlin
enum class BuffType { Slow, DoT, ArmorBreak, AtkUp, SpdUp, Shield, Stun, Silence }
```

- [ ] **Step 2: Stun 처리 로직 — Enemy 이동/공격 정지**

BuffContainer에:

```kotlin
fun isStunned(): Boolean = buffs.any { it.type == BuffType.Stun && it.remaining > 0f }
```

Enemy 업데이트에서:

```kotlin
if (buffs.isStunned()) {
    // 이동 불가, 스턴 아이콘 표시
    return
}
```

- [ ] **Step 3: Silence 처리 로직 — 보스 특수 능력 차단**

보스의 REGENERATION, COMMANDER 등 능력이 Silence 중 비활성:

```kotlin
fun isSilenced(): Boolean = buffs.any { it.type == BuffType.Silence && it.remaining > 0f }
```

- [ ] **Step 4: 기존 스턴 메카닉을 BuffType.Stun으로 통합**

현재 UnitDefs에 "스턴" 설명이 있는 유닛들 (썬더 30%, 토르 2초, 제우스 3초, 피닉스 등)의 구현을 BuffType.Stun으로 통합:

```kotlin
// 기존 recentHitFlags 기반이 아닌 정식 Stun 버프로
enemy.buffs.addBuff(BuffType.Stun, 0f, stunDuration, unit.tileIndex)
```

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/BuffSystem.kt
git commit -m "feat: add Stun and Silence debuff types to BuffSystem"
```

---

### Task 5: CC 저항 스케일링 (고난이도 밸런스)

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/engine/BuffSystem.kt`
- Modify: `app/src/main/java/com/example/jaygame/engine/Enemy.kt`

- [ ] **Step 1: Enemy에 ccResistance 필드 추가**

```kotlin
var ccResistance: Float = 0f  // 0.0 ~ 0.9 (CC 지속시간 감소율)
```

- [ ] **Step 2: WaveSystem에서 난이도별 CC 저항 설정**

```kotlin
// 보스는 기본 50% CC 저항, 난이도에 따라 증가
val baseCcResist = if (isBoss) 0.5f else if (isMiniBoss) 0.3f else 0f
enemy.ccResistance = (baseCcResist + difficulty * 0.05f).coerceAtMost(0.9f)
```

- [ ] **Step 3: CC 디버프 적용 시 저항 반영**

BuffContainer.addBuff에서 Slow/Stun/Silence 추가 시:

```kotlin
val effectiveDuration = if (type == BuffType.Slow || type == BuffType.Stun || type == BuffType.Silence) {
    duration * (1f - ccResistance)
} else duration
```

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/BuffSystem.kt \
       app/src/main/java/com/example/jaygame/engine/Enemy.kt \
       app/src/main/java/com/example/jaygame/engine/WaveSystem.kt
git commit -m "feat: add CC resistance scaling for enemies (boss/difficulty)"
```

---

## Chunk 3: 도박(갬블) 시스템

### Task 6: GambleSystem 구현

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/GambleSystem.kt`

- [ ] **Step 1: GambleSystem 작성**

전투 중 SP를 베팅해서 높은 보상을 노리는 시스템:

```kotlin
package com.example.jaygame.engine

import kotlin.random.Random

/**
 * 전투 중 SP 도박 시스템.
 * SP를 베팅 → 성공 시 2~5배 반환, 실패 시 전액 손실.
 */
object GambleSystem {

    data class GambleResult(
        val bet: Float,
        val won: Boolean,
        val multiplier: Float,
        val reward: Float,
    )

    /** 도박 옵션 */
    enum class GambleOption(
        val label: String,
        val multiplier: Float,
        val baseSuccessRate: Float,
    ) {
        SAFE("안전 베팅", 1.5f, 0.70f),
        NORMAL("일반 베팅", 2.5f, 0.45f),
        RISKY("위험 베팅", 4.0f, 0.25f),
        JACKPOT("잭팟", 8.0f, 0.10f),
    }

    /**
     * @param bet SP 베팅량
     * @param option 도박 종류
     * @param luckBonus 유물 행운석 보너스 (0.0~0.3)
     */
    fun gamble(bet: Float, option: GambleOption, luckBonus: Float = 0f): GambleResult {
        val successRate = (option.baseSuccessRate + luckBonus).coerceAtMost(0.90f)
        val won = Random.nextFloat() < successRate
        val reward = if (won) bet * option.multiplier else 0f
        return GambleResult(bet, won, option.multiplier, reward)
    }
}
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/GambleSystem.kt
git commit -m "feat: add GambleSystem with 4 risk tiers"
```

---

### Task 7: BattleEngine에 도박 연동 + UI

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/engine/BattleEngine.kt`
- Create: `app/src/main/java/com/example/jaygame/ui/battle/GambleDialog.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/battle/BattleHud.kt`

- [ ] **Step 1: BattleEngine에 gamble 함수 추가**

```kotlin
fun requestGamble(betPercent: Float, option: GambleSystem.GambleOption): GambleSystem.GambleResult? {
    val bet = sp * betPercent
    if (bet < 10f) return null  // 최소 10 SP
    val luckBonus = relicManager?.totalGambleBonus()?.div(100f) ?: 0f
    val result = GambleSystem.gamble(bet, option, luckBonus)
    sp = if (result.won) sp - bet + result.reward else sp - bet
    return result
}
```

- [ ] **Step 2: GambleDialog 작성**

```kotlin
@Composable
fun GambleDialog(
    currentSp: Float,
    onGamble: (betPercent: Float, option: GambleSystem.GambleOption) -> GambleSystem.GambleResult?,
    onDismiss: () -> Unit,
) {
    var selectedOption by remember { mutableStateOf(GambleSystem.GambleOption.NORMAL) }
    var betPercent by remember { mutableFloatStateOf(0.3f) }  // 30% 기본
    var lastResult by remember { mutableStateOf<GambleSystem.GambleResult?>(null) }

    // 도박 옵션 4개 버튼 (각각 성공률/배율 표시)
    // SP 베팅량 슬라이더 (10%~80%)
    // "도박!" 버튼
    // 결과 표시 (성공 → 금색 반짝, 실패 → 빨간 흔들림)
}
```

- [ ] **Step 3: BattleHud에 도박 버튼 추가**

전투 중 HUD에 🎲 아이콘 버튼 → GambleDialog 오픈.

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/BattleEngine.kt \
       app/src/main/java/com/example/jaygame/ui/battle/GambleDialog.kt \
       app/src/main/java/com/example/jaygame/ui/battle/BattleHud.kt
git commit -m "feat: add in-battle gamble system with dialog UI"
```

---

## Chunk 4: 천장(Pity) 시스템

### Task 8: ProbabilityEngine에 천장 추가

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/engine/ProbabilityEngine.kt`
- Modify: `app/src/main/java/com/example/jaygame/data/GameData.kt`

- [ ] **Step 1: GameData에 pity 카운터 추가**

```kotlin
val unitPullPity: Int = 0,  // 유닛 소환 천장 (30회 시 영웅 확정, 100회 시 전설 확정)
```

- [ ] **Step 2: ProbabilityEngine에 천장 로직 추가**

기존 확률 로직 수정:

```kotlin
fun rollGradeWithPity(pity: Int): Pair<Int, Boolean> {
    // 100회 천장: 전설 확정
    if (pity >= 100) return 3 to true  // (grade=LEGEND, pityReset=true)
    // 30회 소프트 천장: 영웅 확률 2배
    val heroBoost = if (pity >= 30) 2f else 1f

    val r = Random.nextFloat() * 100f
    return when {
        r < 60f -> 0 to false               // COMMON
        r < 85f -> 1 to false               // RARE
        r < 85f + 12f * heroBoost -> 2 to (pity >= 30)  // HERO (부스트)
        else -> 3 to true                   // LEGEND
    }
}
```

- [ ] **Step 3: 소환 로직에서 pity 카운터 업데이트**

BattleEngine의 소환 로직에서:

```kotlin
val (grade, pityReset) = ProbabilityEngine.rollGradeWithPity(currentPity)
currentPity = if (pityReset) 0 else currentPity + 1
```

- [ ] **Step 4: UI에 천장 카운터 표시**

소환 버튼 옆에 작게 "천장: {pity}/100" 표시.

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/ProbabilityEngine.kt \
       app/src/main/java/com/example/jaygame/data/GameData.kt \
       app/src/main/java/com/example/jaygame/data/GameRepository.kt
git commit -m "feat: add pity system to unit summon (30 soft/100 hard)"
```

---

## Part C: 장기 콘텐츠

---

## Chunk 5: 던전/이벤트 시스템

### Task 9: DungeonDef 정의

**Files:**
- Create: `app/src/main/java/com/example/jaygame/data/DungeonDefs.kt`

- [ ] **Step 1: 던전 타입 정의**

```kotlin
package com.example.jaygame.data

enum class DungeonType(val label: String) {
    GOLD_RUSH("골드 러시"),      // 골드 대량 획득
    RELIC_HUNT("유물 사냥"),     // 유물 드롭률 극대화
    PET_EXPEDITION("펫 탐험"),  // 펫 카드 획득
    BOSS_RUSH("보스 러시"),      // 연속 보스전
    SURVIVAL("서바이벌"),        // 무한 웨이브
}

data class DungeonDef(
    val id: Int,
    val type: DungeonType,
    val name: String,
    val description: String,
    val requiredTrophies: Int,
    val staminaCost: Int,
    val waveCount: Int,
    val difficultyMultiplier: Float,
    val rewardMultiplier: Float,
)

val ALL_DUNGEONS: List<DungeonDef> = listOf(
    DungeonDef(0, DungeonType.GOLD_RUSH, "골드 러시", "골드 보상 3배", 0, 8, 15, 1.2f, 3.0f),
    DungeonDef(1, DungeonType.RELIC_HUNT, "유물 사냥", "유물 드롭률 50%", 500, 10, 20, 1.5f, 1.0f),
    DungeonDef(2, DungeonType.PET_EXPEDITION, "펫 탐험", "펫 카드 보상", 1000, 10, 20, 1.5f, 1.0f),
    DungeonDef(3, DungeonType.BOSS_RUSH, "보스 러시", "10연속 보스전", 1500, 12, 10, 2.0f, 2.0f),
    DungeonDef(4, DungeonType.SURVIVAL, "서바이벌", "무한 웨이브 도전", 2000, 15, 999, 1.0f, 1.0f),
)

/** 던전 입장 횟수 제한 (일일) */
const val DUNGEON_DAILY_LIMIT = 3
```

- [ ] **Step 2: GameData에 던전 진행 데이터 추가**

```kotlin
val dungeonClears: Map<Int, Int> = emptyMap(),  // dungeonId → bestWave
val dungeonDailyCount: Int = 0,
val lastDungeonResetDate: String = "",  // "yyyy-MM-dd"
```

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/data/DungeonDefs.kt \
       app/src/main/java/com/example/jaygame/data/GameData.kt
git commit -m "feat: add DungeonDef definitions (5 dungeon types)"
```

---

### Task 10: DungeonManager + BattleEngine 연동

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/DungeonManager.kt`
- Modify: `app/src/main/java/com/example/jaygame/engine/BattleEngine.kt`

- [ ] **Step 1: DungeonManager 작성**

```kotlin
package com.example.jaygame.engine

import com.example.jaygame.data.*
import java.time.LocalDate

class DungeonManager(private var gameData: GameData) {

    fun syncData(data: GameData) { gameData = data }

    fun canEnter(dungeonId: Int): Boolean {
        val def = ALL_DUNGEONS[dungeonId]
        if (gameData.trophies < def.requiredTrophies) return false
        if (gameData.stamina < def.staminaCost) return false

        val today = LocalDate.now().toString()
        val dailyCount = if (gameData.lastDungeonResetDate == today)
            gameData.dungeonDailyCount else 0
        return dailyCount < DUNGEON_DAILY_LIMIT
    }

    fun enterDungeon(dungeonId: Int): GameData? {
        if (!canEnter(dungeonId)) return null
        val def = ALL_DUNGEONS[dungeonId]
        val today = LocalDate.now().toString()
        val dailyCount = if (gameData.lastDungeonResetDate == today)
            gameData.dungeonDailyCount else 0

        return gameData.copy(
            stamina = gameData.stamina - def.staminaCost,
            dungeonDailyCount = dailyCount + 1,
            lastDungeonResetDate = today,
        )
    }

    fun completeDungeon(dungeonId: Int, waveReached: Int): GameData {
        val clears = gameData.dungeonClears.toMutableMap()
        val best = clears.getOrDefault(dungeonId, 0)
        if (waveReached > best) clears[dungeonId] = waveReached
        return gameData.copy(dungeonClears = clears)
    }
}
```

- [ ] **Step 2: BattleEngine에 던전 모드 플래그 추가**

```kotlin
var isDungeonMode: Boolean = false
var dungeonDef: DungeonDef? = null

// 던전 모드일 때:
// - waveCount를 dungeonDef.waveCount로 제한
// - 난이도를 dungeonDef.difficultyMultiplier로 적용
// - 보상을 dungeonDef.rewardMultiplier로 배율 적용
// - 골드러시: 골드 ×3
// - 유물사냥: rollRelicDrop 확률 50%
// - 펫탐험: 클리어 시 펫 카드 ×3
// - 보스러시: 모든 웨이브가 보스
// - 서바이벌: 무한 웨이브 (패배까지)
```

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/DungeonManager.kt \
       app/src/main/java/com/example/jaygame/engine/BattleEngine.kt
git commit -m "feat: add DungeonManager with 5 dungeon modes"
```

---

### Task 11: 던전 선택 UI

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/screens/DungeonScreen.kt`
- Modify: `app/src/main/java/com/example/jaygame/navigation/Routes.kt`
- Modify: `app/src/main/java/com/example/jaygame/navigation/NavGraph.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt`

- [ ] **Step 1: DungeonScreen 작성**

던전 목록 (카드 형태) + 입장 조건 + 남은 횟수 표시:

```kotlin
@Composable
fun DungeonScreen(
    gameData: GameData,
    onEnterDungeon: (dungeonId: Int) -> Unit,
    onBack: () -> Unit,
) {
    // 5개 던전 카드 (세로 스크롤)
    // 각 카드: 이름, 설명, 트로피 조건, 스태미나 비용, 베스트 기록, 남은 횟수
    // 잠긴 던전은 흐리게 표시
}
```

- [ ] **Step 2: Routes + NavGraph + HomeScreen 연결**

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/DungeonScreen.kt \
       app/src/main/java/com/example/jaygame/navigation/Routes.kt \
       app/src/main/java/com/example/jaygame/navigation/NavGraph.kt \
       app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt
git commit -m "feat: add DungeonScreen UI with dungeon selection"
```

---

## Chunk 6: 프로필 & 업적 확장

### Task 12: 프로필 시스템 확장

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/data/GameData.kt`
- Create: `app/src/main/java/com/example/jaygame/data/ProfileDefs.kt`

- [ ] **Step 1: ProfileDef 정의**

```kotlin
package com.example.jaygame.data

data class ProfileDef(
    val id: Int,
    val name: String,
    val description: String,
    val condition: (GameData) -> Boolean,
    val isAnimated: Boolean = false,
)

val ALL_PROFILES: List<ProfileDef> = listOf(
    // 전투 프로필
    ProfileDef(0, "초보 모험가", "첫 전투 승리", { it.totalWins >= 1 }),
    ProfileDef(1, "베테랑", "50회 승리", { it.totalWins >= 50 }),
    ProfileDef(2, "전설의 전사", "200회 승리", { it.totalWins >= 200 }, isAnimated = true),
    ProfileDef(3, "무상 돌파", "무피해 클리어", { it.wonWithoutDamage }),
    ProfileDef(4, "순혈주의", "단일 패밀리 클리어", { it.wonWithSingleType }),

    // 수집 프로필
    ProfileDef(5, "수집가", "유닛 20종 보유", { it.units.count { u -> u.owned } >= 20 }),
    ProfileDef(6, "만물박사", "전 유닛 보유", { it.units.all { u -> u.owned } }, isAnimated = true),
    ProfileDef(7, "유물 사냥꾼", "유물 6종 보유", { it.relics.count { r -> r.owned } >= 6 }),
    ProfileDef(8, "펫 마스터", "펫 전종 보유", { it.pets.all { p -> p.owned } }, isAnimated = true),

    // 경제 프로필
    ProfileDef(9, "부자", "총 골드 100,000 획득", { it.totalGoldEarned >= 100000 }),
    ProfileDef(10, "도박꾼", "도박 50회 성공", { (it.stats["gambleWins"] ?: 0) >= 50 }),

    // 난이도 프로필
    ProfileDef(11, "고인물", "챌린저 난이도 클리어", { it.difficulty >= 4 && it.totalWins > 0 }),
    ProfileDef(12, "심연 정복자", "심연 스테이지 클리어", {
        (it.stageBestWaves["5"] ?: 0) >= 60
    }, isAnimated = true),

    // 특수 프로필
    ProfileDef(13, "합성왕", "총 합성 500회", { it.totalMerges >= 500 }),
    ProfileDef(14, "웨이브 마스터", "최고 웨이브 50+", { it.highestWave >= 50 }),
)
```

- [ ] **Step 2: GameData에 프로필 필드 추가**

```kotlin
val selectedProfileId: Int = 0,
val unlockedProfiles: Set<Int> = setOf(0),
```

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/data/ProfileDefs.kt \
       app/src/main/java/com/example/jaygame/data/GameData.kt
git commit -m "feat: add 15 profile definitions with unlock conditions"
```

---

### Task 13: 업적 시스템 확장

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/AchievementsScreen.kt` (기존 20개에 추가)

- [ ] **Step 1: 유물/펫/던전 관련 업적 추가**

기존 업적 리스트에 추가:

```kotlin
// 유물 업적 (ID 20-24)
Achievement(20, "유물 수집가", "유물 3종 획득", AchievementCategory.COLLECTION,
    { it.relics.count { r -> r.owned } >= 3 }, 500, 50),
Achievement(21, "유물 강화 마스터", "유물 레벨 10 달성", AchievementCategory.COLLECTION,
    { it.relics.any { r -> r.level >= 10 } }, 1000, 100),

// 펫 업적 (ID 25-29)
Achievement(25, "펫 친구", "첫 펫 획득", AchievementCategory.COLLECTION,
    { it.pets.any { p -> p.owned } }, 200, 20),
Achievement(26, "펫 마스터", "펫 3종 레벨 10+", AchievementCategory.COLLECTION,
    { it.pets.count { p -> p.level >= 10 } >= 3 }, 2000, 200),

// 던전 업적 (ID 30-34)
Achievement(30, "던전 탐험가", "던전 첫 클리어", AchievementCategory.BATTLE,
    { it.dungeonClears.isNotEmpty() }, 300, 30),
Achievement(31, "던전 정복자", "모든 던전 클리어", AchievementCategory.BATTLE,
    { it.dungeonClears.size >= 5 }, 3000, 300),

// 도박 업적 (ID 35-37)
Achievement(35, "럭키 스트라이크", "잭팟 도박 성공", AchievementCategory.SPECIAL,
    { (it.stats["jackpotWins"] ?: 0) >= 1 }, 500, 50),
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/AchievementsScreen.kt
git commit -m "feat: add 10+ achievements for relics, pets, dungeons, gamble"
```

---

### Task 14: 프로필 선택 UI

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/screens/ProfileScreen.kt`
- Modify: `app/src/main/java/com/example/jaygame/navigation/Routes.kt`
- Modify: `app/src/main/java/com/example/jaygame/navigation/NavGraph.kt`

- [ ] **Step 1: ProfileScreen 작성**

```kotlin
@Composable
fun ProfileScreen(
    gameData: GameData,
    onSelectProfile: (profileId: Int) -> Unit,
    onBack: () -> Unit,
) {
    // 프로필 그리드 (3열)
    // 잠긴 프로필: 흐리게 + 조건 표시
    // 해금 프로필: 선택 가능, 현재 선택 표시 (금테)
    // 움직이는 프로필 (isAnimated): 특별 효과
}
```

- [ ] **Step 2: HomeScreen에서 프로필 아이콘 클릭 → ProfileScreen**

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/ProfileScreen.kt \
       app/src/main/java/com/example/jaygame/navigation/Routes.kt \
       app/src/main/java/com/example/jaygame/navigation/NavGraph.kt \
       app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt
git commit -m "feat: add ProfileScreen UI with unlock/select functionality"
```

---

## Chunk 7: GameRepository 최종 통합

### Task 15: 새 필드 전체 직렬화 마무리

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/data/GameRepository.kt`

- [ ] **Step 1: 던전/프로필/천장/도박 통계 직렬화 추가**

```kotlin
// toJson 추가:
root.put("unitPullPity", data.unitPullPity)

val dcObj = JSONObject()
for ((k, v) in data.dungeonClears) dcObj.put(k.toString(), v)
root.put("dungeonClears", dcObj)
root.put("dungeonDailyCount", data.dungeonDailyCount)
root.put("lastDungeonResetDate", data.lastDungeonResetDate)

root.put("selectedProfileId", data.selectedProfileId)
val upArr = JSONArray()
for (id in data.unlockedProfiles) upArr.put(id)
root.put("unlockedProfiles", upArr)
```

- [ ] **Step 2: fromJson에 역직렬화 추가**

모든 새 필드에 `root.has()` 가드 + 기본값 fallback.

- [ ] **Step 3: saveVersion 증가 (2 → 3)**

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/data/GameRepository.kt
git commit -m "feat: complete serialization for dungeon/profile/pity/gamble data"
```

---

## 구현 순서 요약

```
Phase 1 (B - 핵심 전투 개선):
  Task 1-3: 보스 모디파이어 (정의 → 적용 → UI)
  Task 4-5: CC 강화 (Stun/Silence + CC 저항)
  Task 6-7: 도박 시스템
  Task 8: 천장 시스템

Phase 2 (C - 장기 콘텐츠):
  Task 9-11: 던전 시스템 (정의 → 로직 → UI)
  Task 12-14: 프로필/업적 확장
  Task 15: 직렬화 통합
```

---
