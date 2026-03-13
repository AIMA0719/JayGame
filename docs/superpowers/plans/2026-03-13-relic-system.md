# 유물(Relic) 시스템 구현 플랜

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 전투 전략과 영구 성장을 좌우하는 유물 수집/강화 시스템 도입

**Architecture:** RelicDef(정의) → RelicManager(로직) → GameData에 저장 → 전투 시 BattleEngine에 보너스 적용. 유물은 전투 보상/상점에서 획득, 재화로 강화. 전투 중 실시간 효과 + 영구 스탯 보너스 이중 구조.

**Tech Stack:** Kotlin, Compose UI, SharedPreferences(JSON), 기존 BattleEngine/EconomyManager 연동

---

## 유물 설계

### 등급 (5단계)
| 등급 | 색상 | 드롭 확률 | 최대 레벨 |
|------|------|----------|----------|
| 일반 (Common) | 회색 | 50% | 5 |
| 희귀 (Rare) | 파랑 | 30% | 7 |
| 영웅 (Hero) | 보라 | 15% | 10 |
| 전설 (Legend) | 금색 | 4% | 15 |
| 신화 (Mythic) | 빨강 | 1% | 20 |

### 유물 목록 (12종)

**경제 유물 (3종)**
| ID | 이름 | 등급범위 | 효과 |
|----|------|---------|------|
| 0 | 금고 | 일반~신화 | 웨이브 클리어 골드 +{lv×10}% |
| 1 | 머니건 | 일반~신화 | 적 처치 골드 +{lv×8}% |
| 2 | 행운석 | 희귀~신화 | 도박 성공 확률 +{lv×3}% |

**전투 유물 (5종)**
| ID | 이름 | 등급범위 | 효과 |
|----|------|---------|------|
| 3 | 전쟁의 뿔피리 | 일반~신화 | 전체 유닛 ATK +{lv×5}% |
| 4 | 신속의 부츠 | 일반~신화 | 전체 유닛 공속 +{lv×4}% |
| 5 | 파멸의 반지 | 희귀~신화 | 크리티컬 확률 +{lv×2}%, 크리뎀 +{lv×10}% |
| 6 | 관통의 창 | 희귀~신화 | 적 방어력 무시 +{lv×3}% |
| 7 | 마력의 구슬 | 영웅~신화 | 마법 피해 +{lv×6}% |

**유틸리티 유물 (4종)**
| ID | 이름 | 등급범위 | 효과 |
|----|------|---------|------|
| 8 | 소환사의 오브 | 일반~신화 | 소환 비용 -{lv×3}% (최소 50%) |
| 9 | 합성의 돌 | 희귀~신화 | 럭키 합성 확률 +{lv×1}% |
| 10 | 시간의 모래 | 영웅~신화 | 쿨다운 감소 +{lv×2}% |
| 11 | 생명의 나무 | 전설~신화 | 웨이브 시작 시 SP +{lv×5} 추가 |

### 강화 비용 (골드)
```
Lv1→2: 100, Lv2→3: 250, Lv3→4: 500, Lv4→5: 1000,
Lv5→6: 2000, Lv6→7: 4000, Lv7→8: 8000, Lv8→9: 16000,
Lv9→10: 32000, Lv10+: 이전 비용 ×2.2 (기하급수)
```

### 장착 슬롯: 최대 4개 (트로피 기반 해금)
- 0 트로피: 1슬롯
- 500 트로피: 2슬롯
- 1500 트로피: 3슬롯
- 3000 트로피: 4슬롯

---

## Chunk 1: 데이터 정의 & 저장

### Task 1: RelicDef 데이터 클래스 정의

**Files:**
- Create: `app/src/main/java/com/example/jaygame/data/RelicDefs.kt`

- [ ] **Step 1: RelicGrade enum 작성**

```kotlin
package com.example.jaygame.data

enum class RelicGrade(val label: String, val colorHex: Long, val maxLevel: Int, val dropWeight: Int) {
    COMMON("일반", 0xFF9E9E9E, 5, 50),
    RARE("희귀", 0xFF42A5F5, 7, 30),
    HERO("영웅", 0xFFAB47BC, 10, 15),
    LEGEND("전설", 0xFFFFCA28, 15, 4),
    MYTHIC("신화", 0xFFEF5350, 20, 1);
}
```

- [ ] **Step 2: RelicType enum 작성**

```kotlin
enum class RelicType { ECONOMY, COMBAT, UTILITY }
```

- [ ] **Step 3: RelicDef data class 작성**

```kotlin
data class RelicDef(
    val id: Int,
    val name: String,
    val type: RelicType,
    val minGrade: RelicGrade,
    val description: String,         // "{lv}" placeholder for level
    val effectPerLevel: Float,       // 레벨당 효과 수치
    val maxEffectCap: Float = Float.MAX_VALUE,
)
```

- [ ] **Step 4: 12종 유물 정의 목록 작성**

```kotlin
val ALL_RELICS: List<RelicDef> = listOf(
    RelicDef(0, "금고", RelicType.ECONOMY, RelicGrade.COMMON,
        "웨이브 클리어 골드 +{lv}0%", 10f),
    RelicDef(1, "머니건", RelicType.ECONOMY, RelicGrade.COMMON,
        "적 처치 골드 +{lv}×8%", 8f),
    RelicDef(2, "행운석", RelicType.ECONOMY, RelicGrade.RARE,
        "도박 성공 확률 +{lv}×3%", 3f, 30f),
    RelicDef(3, "전쟁의 뿔피리", RelicType.COMBAT, RelicGrade.COMMON,
        "전체 유닛 ATK +{lv}×5%", 5f),
    RelicDef(4, "신속의 부츠", RelicType.COMBAT, RelicGrade.COMMON,
        "전체 유닛 공속 +{lv}×4%", 4f),
    RelicDef(5, "파멸의 반지", RelicType.COMBAT, RelicGrade.RARE,
        "크리티컬 확률 +{lv}×2%", 2f, 30f),
    RelicDef(6, "관통의 창", RelicType.COMBAT, RelicGrade.RARE,
        "적 방어력 무시 +{lv}×3%", 3f, 60f),
    RelicDef(7, "마력의 구슬", RelicType.COMBAT, RelicGrade.HERO,
        "마법 피해 +{lv}×6%", 6f),
    RelicDef(8, "소환사의 오브", RelicType.UTILITY, RelicGrade.COMMON,
        "소환 비용 -{lv}×3%", 3f, 50f),
    RelicDef(9, "합성의 돌", RelicType.UTILITY, RelicGrade.RARE,
        "럭키 합성 확률 +{lv}×1%", 1f, 15f),
    RelicDef(10, "시간의 모래", RelicType.UTILITY, RelicGrade.HERO,
        "쿨다운 감소 +{lv}×2%", 2f, 40f),
    RelicDef(11, "생명의 나무", RelicType.UTILITY, RelicGrade.LEGEND,
        "웨이브 시작 SP +{lv}×5", 5f),
)

/** 강화 비용: level 0→1 은 무료(획득 시), 이후 index=targetLevel-2 */
val RELIC_UPGRADE_COSTS: List<Int> = listOf(
    100, 250, 500, 1000, 2000, 4000, 8000, 16000, 32000
)
fun relicUpgradeCost(currentLevel: Int): Int {
    val idx = currentLevel - 1  // lv1→2 = index 0
    return if (idx < RELIC_UPGRADE_COSTS.size) RELIC_UPGRADE_COSTS[idx]
    else (RELIC_UPGRADE_COSTS.last() * Math.pow(2.2, (idx - RELIC_UPGRADE_COSTS.size + 1).toDouble())).toInt()
}
```

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/data/RelicDefs.kt
git commit -m "feat: add RelicDef data definitions (12 relics, 5 grades)"
```

---

### Task 2: GameData에 유물 저장 구조 추가

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/data/GameData.kt`

- [ ] **Step 1: RelicProgress data class 추가**

GameData.kt 파일 상단(GameData class 밖)에 추가:

```kotlin
data class RelicProgress(
    val relicId: Int = 0,
    val grade: Int = 0,      // RelicGrade.ordinal
    val level: Int = 1,
    val owned: Boolean = false,
)
```

- [ ] **Step 2: GameData에 유물 필드 추가**

GameData data class에 필드 추가:

```kotlin
val relics: List<RelicProgress> = List(12) { RelicProgress(relicId = it) },
val equippedRelics: List<Int> = emptyList(),  // 장착된 relicId 목록 (최대 4개)
```

- [ ] **Step 3: equippedSlotCount 계산 프로퍼티 추가**

GameData 안에:

```kotlin
val equippedSlotCount: Int get() = when {
    trophies >= 3000 -> 4
    trophies >= 1500 -> 3
    trophies >= 500 -> 2
    else -> 1
}
```

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/data/GameData.kt
git commit -m "feat: add relic progress and equipment slots to GameData"
```

---

### Task 3: GameRepository에 유물 직렬화/역직렬화 추가

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/data/GameRepository.kt`

- [ ] **Step 1: toJson에 relics 배열 직렬화 추가**

기존 `toJson()` 함수의 JSON 빌드 부분에 추가:

```kotlin
// relics
val relicsArr = JSONArray()
for (r in data.relics) {
    val rObj = JSONObject()
    rObj.put("relicId", r.relicId)
    rObj.put("grade", r.grade)
    rObj.put("level", r.level)
    rObj.put("owned", if (r.owned) 1 else 0)
    relicsArr.put(rObj)
}
root.put("relics", relicsArr)

val eqArr = JSONArray()
for (id in data.equippedRelics) eqArr.put(id)
root.put("equippedRelics", eqArr)
```

- [ ] **Step 2: fromJson에 relics 역직렬화 추가**

기존 `fromJson()` 함수에 추가:

```kotlin
val relics = if (root.has("relics")) {
    val arr = root.getJSONArray("relics")
    List(arr.length()) { i ->
        val obj = arr.getJSONObject(i)
        RelicProgress(
            relicId = obj.getInt("relicId"),
            grade = obj.getInt("grade"),
            level = obj.getInt("level"),
            owned = obj.optInt("owned", 0) == 1,
        )
    }
} else List(12) { RelicProgress(relicId = it) }

val equippedRelics = if (root.has("equippedRelics")) {
    val arr = root.getJSONArray("equippedRelics")
    List(arr.length()) { arr.getInt(it) }
} else emptyList()
```

그리고 GameData 생성자 호출에 `relics = relics, equippedRelics = equippedRelics` 추가.

- [ ] **Step 3: saveVersion 증가 (1 → 2)**

마이그레이션 안전: fromJson이 `has("relics")` 체크로 v1 세이브와 호환됨.

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/data/GameRepository.kt
git commit -m "feat: serialize/deserialize relic data in GameRepository"
```

---

## Chunk 2: 유물 관리 로직

### Task 4: RelicManager 생성

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/RelicManager.kt`

- [ ] **Step 1: RelicManager 클래스 골격 작성**

```kotlin
package com.example.jaygame.engine

import com.example.jaygame.data.*

class RelicManager(private var gameData: GameData) {

    fun syncData(data: GameData) { gameData = data }

    /** 유물 획득 (드롭 또는 구매) */
    fun acquireRelic(relicId: Int, grade: RelicGrade): GameData {
        val relics = gameData.relics.toMutableList()
        val existing = relics[relicId]
        if (existing.owned) {
            // 이미 보유: 등급이 높으면 교체, 아니면 골드 보상
            if (grade.ordinal > existing.grade) {
                relics[relicId] = existing.copy(grade = grade.ordinal, level = 1)
            } else {
                // 중복 → 골드 보상 (등급 × 100)
                return gameData.copy(
                    relics = relics,
                    gold = gameData.gold + (grade.ordinal + 1) * 100
                )
            }
        } else {
            relics[relicId] = RelicProgress(
                relicId = relicId,
                grade = grade.ordinal,
                level = 1,
                owned = true,
            )
        }
        return gameData.copy(relics = relics)
    }

    /** 유물 강화 */
    fun upgradeRelic(relicId: Int): GameData? {
        val relic = gameData.relics[relicId]
        if (!relic.owned) return null
        val gradeDef = RelicGrade.entries[relic.grade]
        if (relic.level >= gradeDef.maxLevel) return null
        val cost = relicUpgradeCost(relic.level)
        if (gameData.gold < cost) return null

        val relics = gameData.relics.toMutableList()
        relics[relicId] = relic.copy(level = relic.level + 1)
        return gameData.copy(relics = relics, gold = gameData.gold - cost)
    }

    /** 유물 장착 */
    fun equipRelic(relicId: Int): GameData? {
        val relic = gameData.relics[relicId]
        if (!relic.owned) return null
        if (gameData.equippedRelics.contains(relicId)) return null
        if (gameData.equippedRelics.size >= gameData.equippedSlotCount) return null
        return gameData.copy(equippedRelics = gameData.equippedRelics + relicId)
    }

    /** 유물 장착 해제 */
    fun unequipRelic(relicId: Int): GameData {
        return gameData.copy(equippedRelics = gameData.equippedRelics - relicId)
    }

    /** 장착된 유물의 효과 계산 */
    fun getEquippedEffect(relicId: Int): Float {
        val relic = gameData.relics[relicId]
        if (!relic.owned || !gameData.equippedRelics.contains(relicId)) return 0f
        val def = ALL_RELICS[relicId]
        val raw = relic.level * def.effectPerLevel
        return raw.coerceAtMost(def.maxEffectCap)
    }

    /** 특정 효과 타입의 합산 */
    fun totalEffect(relicId: Int): Float = getEquippedEffect(relicId)

    fun totalAtkPercent(): Float = getEquippedEffect(3) / 100f
    fun totalAtkSpeedPercent(): Float = getEquippedEffect(4) / 100f
    fun totalCritChanceBonus(): Float = getEquippedEffect(5) / 100f
    fun totalCritDamageBonus(): Float {
        val relic = gameData.relics[5]
        if (!relic.owned || !gameData.equippedRelics.contains(5)) return 0f
        return (relic.level * 10f) / 100f  // lv×10%
    }
    fun totalArmorPenPercent(): Float = getEquippedEffect(6) / 100f
    fun totalMagicDmgPercent(): Float = getEquippedEffect(7) / 100f
    fun totalSummonCostReduction(): Float = getEquippedEffect(8) / 100f
    fun totalLuckyMergeBonus(): Float = getEquippedEffect(9) / 100f
    fun totalCooldownReduction(): Float = getEquippedEffect(10) / 100f
    fun totalWaveStartSp(): Float = getEquippedEffect(11)
    fun totalGoldWaveBonus(): Float = getEquippedEffect(0) / 100f
    fun totalGoldKillBonus(): Float = getEquippedEffect(1) / 100f
    fun totalGambleBonus(): Float = getEquippedEffect(2) / 100f

    /** 전투 보상으로 랜덤 유물 드롭 */
    fun rollRelicDrop(): Pair<Int, RelicGrade>? {
        // 10% 기본 드롭 확률
        if (Math.random() > 0.10) return null
        val relicId = (0 until ALL_RELICS.size).random()
        val def = ALL_RELICS[relicId]
        val grade = rollGrade(def.minGrade)
        return relicId to grade
    }

    private fun rollGrade(minGrade: RelicGrade): RelicGrade {
        val eligible = RelicGrade.entries.filter { it.ordinal >= minGrade.ordinal }
        val totalWeight = eligible.sumOf { it.dropWeight }
        var roll = (0 until totalWeight).random()
        for (g in eligible) {
            roll -= g.dropWeight
            if (roll < 0) return g
        }
        return eligible.last()
    }
}
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/RelicManager.kt
git commit -m "feat: add RelicManager with acquire/upgrade/equip/effects logic"
```

---

### Task 5: BattleEngine에 유물 효과 연동

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/engine/BattleEngine.kt`
- Modify: `app/src/main/java/com/example/jaygame/engine/EconomyManager.kt`
- Modify: `app/src/main/java/com/example/jaygame/engine/DamageCalculator.kt`

- [ ] **Step 1: BattleEngine에 RelicManager 필드 추가**

BattleEngine 클래스에:

```kotlin
var relicManager: RelicManager? = null
```

init 또는 start 시점에서 gameData로 초기화하도록 호출부 수정.

- [ ] **Step 2: EconomyManager에 유물 골드 보너스 적용**

`onWaveCleared`에서:

```kotlin
val waveGold = (50 + waveNumber * 10)
val relicBonus = relicManager?.totalGoldWaveBonus() ?: 0f
gold += (waveGold * (1f + relicBonus)).toInt()
```

`onEnemyKilled`에서:

```kotlin
val relicBonus = relicManager?.totalGoldKillBonus() ?: 0f
gold += (reward * (1f + relicBonus)).toInt()
```

- [ ] **Step 3: DamageCalculator에 유물 전투 보너스 적용**

유물 ATK 보너스, 크리티컬, 방어 관통, 마법 데미지 보너스를 데미지 공식에 반영:

```kotlin
// ATK 보너스
val relicAtkBonus = relicManager?.totalAtkPercent() ?: 0f
val effectiveATK = baseATK * (1f + relicAtkBonus)

// 크리티컬
val baseCritChance = 0.05f + (relicManager?.totalCritChanceBonus() ?: 0f)
val critMultiplier = 2.0f + (relicManager?.totalCritDamageBonus() ?: 0f)

// 방어 관통
val armorPen = relicManager?.totalArmorPenPercent() ?: 0f
val effectiveArmor = (armor - armorBreak) * (1f - armorPen)

// 마법 데미지 보너스 (냉기/번개)
val magicBonus = relicManager?.totalMagicDmgPercent() ?: 0f
// isMagic일 때: finalDmg *= (1f + magicBonus)
```

- [ ] **Step 4: 소환 비용 감소 적용**

BattleEngine의 `getSummonCost` 또는 EconomyManager에서:

```kotlin
val reduction = relicManager?.totalSummonCostReduction() ?: 0f
val cost = (baseCost * (1f - reduction)).toInt().coerceAtLeast(baseCost / 2)
```

- [ ] **Step 5: 럭키 합성 보너스 적용**

MergeSystem 또는 BattleEngine의 merge 로직에서:

```kotlin
val luckyBonus = relicManager?.totalLuckyMergeBonus() ?: 0f
val isLucky = Math.random() < (LUCKY_CHANCE + luckyBonus)
```

- [ ] **Step 6: 쿨다운 감소 적용**

UniqueAbilitySystem에서 쿨다운 리셋 시:

```kotlin
val cdReduction = relicManager?.totalCooldownReduction() ?: 0f
unit.uniqueAbilityCooldown = unit.uniqueAbilityMaxCd * (1f - cdReduction)
```

- [ ] **Step 7: 웨이브 시작 SP 보너스**

BattleEngine의 웨이브 시작 로직에서:

```kotlin
val bonusSp = relicManager?.totalWaveStartSp() ?: 0f
sp += bonusSp
```

- [ ] **Step 8: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/BattleEngine.kt \
       app/src/main/java/com/example/jaygame/engine/EconomyManager.kt \
       app/src/main/java/com/example/jaygame/engine/DamageCalculator.kt \
       app/src/main/java/com/example/jaygame/engine/MergeSystem.kt \
       app/src/main/java/com/example/jaygame/engine/UniqueAbilitySystem.kt
git commit -m "feat: integrate relic effects into battle engine"
```

---

## Chunk 3: 유물 UI

### Task 6: 유물 관리 화면 (RelicScreen)

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/screens/RelicScreen.kt`
- Modify: `app/src/main/java/com/example/jaygame/navigation/Routes.kt`
- Modify: `app/src/main/java/com/example/jaygame/navigation/NavGraph.kt`

- [ ] **Step 1: Routes에 Relic 경로 추가**

```kotlin
object Relic : Routes("relic")
```

- [ ] **Step 2: RelicScreen 컴포저블 작성**

유물 목록 (LazyVerticalGrid 3열) + 선택 시 상세/강화/장착 패널:

```kotlin
@Composable
fun RelicScreen(
    gameData: GameData,
    onUpgrade: (relicId: Int) -> Unit,
    onEquip: (relicId: Int) -> Unit,
    onUnequip: (relicId: Int) -> Unit,
    onBack: () -> Unit,
) {
    var selectedRelicId by remember { mutableIntStateOf(-1) }

    Column(Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {
        // 상단 바: "유물" + 장착 슬롯 표시
        // 장착 슬롯 바: equippedRelics 아이콘 (최대 4칸)
        // 유물 그리드: 12칸, 보유 유물은 등급 색상 테두리, 미보유는 잠금
        // 선택된 유물 상세 패널: 이름, 등급, 레벨, 효과, 강화 버튼, 장착/해제 버튼
    }
}
```

- [ ] **Step 3: NavGraph에 RelicScreen 연결**

```kotlin
composable(Routes.Relic.route) {
    RelicScreen(
        gameData = gameData,
        onUpgrade = { id -> gameData = relicManager.upgradeRelic(id) ?: gameData; repo.save(gameData) },
        onEquip = { id -> gameData = relicManager.equipRelic(id) ?: gameData; repo.save(gameData) },
        onUnequip = { id -> gameData = relicManager.unequipRelic(id); repo.save(gameData) },
        onBack = { navController.popBackStack() },
    )
}
```

- [ ] **Step 4: HomeScreen에 유물 메뉴 버튼 추가**

기존 HomeScreen 버튼 목록에 "유물" 버튼 추가 → Routes.Relic로 네비게이션.

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/RelicScreen.kt \
       app/src/main/java/com/example/jaygame/navigation/Routes.kt \
       app/src/main/java/com/example/jaygame/navigation/NavGraph.kt \
       app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt
git commit -m "feat: add RelicScreen UI with upgrade/equip functionality"
```

---

### Task 7: 전투 결과에 유물 드롭 연동

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/battle/BattleResultDialog.kt` (또는 해당 결과 화면)
- Modify: `app/src/main/java/com/example/jaygame/engine/BattleEngine.kt`

- [ ] **Step 1: 전투 승리 시 유물 드롭 판정**

BattleEngine의 승리 처리 로직에서:

```kotlin
val relicDrop = relicManager?.rollRelicDrop()
// relicDrop을 BattleBridge의 StateFlow로 전달
```

- [ ] **Step 2: BattleResultDialog에 유물 드롭 표시**

획득 유물이 있으면 유물 이름, 등급, 효과를 팝업 표시.

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/BattleEngine.kt \
       app/src/main/java/com/example/jaygame/ui/battle/BattleResultDialog.kt
git commit -m "feat: relic drop on battle victory with result display"
```

---
