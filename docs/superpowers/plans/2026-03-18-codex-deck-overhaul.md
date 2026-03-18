# 도감 & 소환/시너지 시스템 개편 구현 플랜

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 덱 시스템을 폐지하고, 도감을 BlueprintRegistry 기반 다축 필터로 개편하며, 시너지를 필드 배치 기준 실시간 발동으로 전환한다.

**Architecture:** 레거시 `UNIT_DEFS` + `DeckScreen` 제거 → `BlueprintRegistry`를 앱 레벨 싱글턴으로 승격 → 도감 UI가 Blueprint 데이터 직접 참조. 시너지는 `SynergySystem`(속성)과 `RoleSynergySystem`(역할) 모두 필드 기반으로 통일하여 `BattleEngine` 게임루프에 연결.

**Tech Stack:** Kotlin, Jetpack Compose, JSON assets, manual DI (no Hilt)

**Spec:** `docs/superpowers/specs/2026-03-18-codex-deck-overhaul-design.md`

---

## 파일 구조

### 신규 생성
| 파일 | 역할 |
|------|------|
| `data/LegacyMigration.kt` | 레거시 Int 인덱스 → blueprintId 매핑 + 마이그레이션 |
| `ui/battle/SynergyPanel.kt` | 전투 UI 시너지 패널 컴포넌트 |

### 대규모 수정
| 파일 | 변경 내용 |
|------|-----------|
| `engine/BlueprintRegistry.kt` | special_units.json 추가 로딩, companion object 싱글턴화 |
| `engine/BattleEngine.kt` | deck 파라미터 제거, 레거시 requestSummon() 제거, 시너지 연결 |
| `engine/AbilitySystem.kt` | activeSynergy 필드 이벤트 기반으로 변경 |
| `data/GameData.kt` | deck 제거, units를 Map<String, UnitProgress>로 변경 |
| `data/GameRepository.kt` | saveVersion 3, 마이그레이션 로직 |
| `ui/screens/UnitCollectionScreen.kt` | Blueprint 기반 + 필터 바 + 히든/특수 탭 |
| `ui/screens/CollectionScreen.kt` | HeroCollectionTab Blueprint 기반 전환 |

### 소규모 수정
| 파일 | 변경 내용 |
|------|-----------|
| `engine/SynergySystem.kt` | 덱 기반 오버로드 제거 |
| `engine/RoleSynergySystem.kt` | BattleEngine 연결 (코드 자체 변경 없음) |
| `navigation/Routes.kt` | DECK 라우트 제거 |
| `navigation/NavGraph.kt` | DECK 라우팅 제거 |
| `ui/components/BottomNavBar.kt` | DECK 탭 제거 → 4탭 |
| `MainActivity.kt` | BlueprintRegistry 싱글턴 초기화 |
| `ComposeActivity.kt` | BlueprintRegistry 싱글턴 초기화 |

### 제거
| 파일 | 사유 |
|------|------|
| `ui/screens/DeckScreen.kt` | 덱 시스템 폐지 |

---

## Task 1: BlueprintRegistry 싱글턴화 + special_units.json 로딩

도감과 전투 양쪽에서 BlueprintRegistry에 접근해야 하므로, 앱 레벨 싱글턴으로 승격한다.

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/engine/BlueprintRegistry.kt`
- Modify: `app/src/main/java/com/example/jaygame/MainActivity.kt`
- Modify: `app/src/main/java/com/example/jaygame/ComposeActivity.kt`

- [ ] **Step 1: BlueprintRegistry에 companion object 싱글턴 추가**

`BlueprintRegistry.kt`에 앱 전역 인스턴스를 추가:

```kotlin
companion object {
    // 앱 전역 싱글턴 — MainActivity/ComposeActivity.onCreate()에서 초기화
    lateinit var instance: BlueprintRegistry
        private set

    fun initialize(context: android.content.Context) {
        if (::instance.isInitialized) return
        val registry = BlueprintRegistry()
        val blueprintsJson = context.assets.open("units/blueprints.json")
            .bufferedReader().use { it.readText() }
        registry.loadFromJson(blueprintsJson)
        val specialJson = context.assets.open("units/special_units.json")
            .bufferedReader().use { it.readText() }
        registry.loadFromJson(specialJson)
        instance = registry
    }
}
```

- [ ] **Step 2: loadFromJson()이 기존 데이터에 추가 로딩하도록 확인**

현재 `loadFromJson()`이 내부 맵을 덮어쓰는지 추가하는지 확인. 덮어쓴다면 `addFromJson()` 또는 append 방식으로 변경:

```kotlin
fun loadFromJson(json: String) {
    val newBlueprints = parseBlueprints(json)
    blueprintMap.putAll(newBlueprints.associateBy { it.id })
}
```

- [ ] **Step 3: MainActivity/ComposeActivity에서 초기화 호출**

두 Activity의 `onCreate()` 상단에 추가:

```kotlin
BlueprintRegistry.initialize(applicationContext)
```

- [ ] **Step 4: BattleEngine의 자체 BlueprintRegistry 생성을 싱글턴 참조로 변경**

`BattleEngine.kt` line 152 부근의 `BlueprintRegistry()` 생성을 `BlueprintRegistry.instance` 참조로 변경.

- [ ] **Step 5: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "refactor: BlueprintRegistry 싱글턴화 + special_units.json 로딩"
```

---

## Task 2: RecipeSystem 싱글턴화

히든 탭에서 RecipeSystem에 접근해야 한다.

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/engine/RecipeSystem.kt`
- Modify: `app/src/main/java/com/example/jaygame/MainActivity.kt`
- Modify: `app/src/main/java/com/example/jaygame/ComposeActivity.kt`

- [ ] **Step 1: RecipeSystem에 companion object 싱글턴 추가**

BlueprintRegistry와 동일 패턴. **주의**: `RecipeSystem` 생성자는 `BlueprintRegistry`를 인자로 받으므로 반드시 `BlueprintRegistry.initialize()` 이후에 호출해야 한다:

```kotlin
companion object {
    lateinit var instance: RecipeSystem
        private set

    /** BlueprintRegistry.initialize() 이후에 호출할 것 */
    fun initialize(context: android.content.Context) {
        if (::instance.isInitialized) return
        val recipesJson = context.assets.open("units/hidden_recipes.json")
            .bufferedReader().use { it.readText() }
        val system = RecipeSystem(BlueprintRegistry.instance)
        system.loadRecipes(recipesJson)
        instance = system
    }
}
```

- [ ] **Step 2: Activity에서 초기화 호출 (BlueprintRegistry 다음)**

```kotlin
// BlueprintRegistry.initialize() 이후에 호출 — 순서 중요!
RecipeSystem.initialize(applicationContext)
```

- [ ] **Step 3: BattleEngine의 자체 RecipeSystem 생성을 싱글턴 참조로 변경**

- [ ] **Step 4: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "refactor: RecipeSystem 싱글턴화"
```

---

## Task 3: 덱 시스템 제거 — 네비게이션 + DeckScreen

**Files:**
- Delete: `app/src/main/java/com/example/jaygame/ui/screens/DeckScreen.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/components/BottomNavBar.kt` (line 35)
- Modify: `app/src/main/java/com/example/jaygame/navigation/Routes.kt` (line 5)
- Modify: `app/src/main/java/com/example/jaygame/navigation/NavGraph.kt` (lines 56, 64, 91-93)

- [ ] **Step 1: BottomNavBar.kt에서 DECK 탭 제거**

`NavTab` enum에서 `DECK` 항목 제거. 남는 탭: COLLECTION, HOME, SHOP, SETTINGS (4탭).
탭 아이콘/라벨도 함께 수정.

- [ ] **Step 2: Routes.kt에서 DECK 라우트 제거**

```kotlin
// 제거: const val DECK = "deck"
```

- [ ] **Step 3: NavGraph.kt에서 DECK 관련 코드 제거**

- `NavTab.DECK` case 제거 (line 56)
- `composable(Routes.DECK)` 블록 제거 (lines 91-93)
- `showBottomBar` 리스트에서 DECK 제거 (line 64)

- [ ] **Step 4: DeckScreen.kt 파일 삭제**

```bash
rm app/src/main/java/com/example/jaygame/ui/screens/DeckScreen.kt
```

- [ ] **Step 5: DeckScreen import 참조 정리**

NavGraph.kt 등에서 DeckScreen import가 있으면 제거.

- [ ] **Step 6: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add -A
git commit -m "feat: 덱 시스템 폐지 — DeckScreen 제거, 4탭 네비게이션"
```

---

## Task 4: GameData 스키마 변경 + 세이브 마이그레이션

**Files:**
- Create: `app/src/main/java/com/example/jaygame/data/LegacyMigration.kt`
- Modify: `app/src/main/java/com/example/jaygame/data/GameData.kt` (lines 29-33)
- Modify: `app/src/main/java/com/example/jaygame/data/GameRepository.kt` (lines 60-492)

- [ ] **Step 1: LegacyMigration.kt 생성 — 매핑 테이블**

레거시 Int 인덱스 → blueprintId 매핑. 기존 `UNIT_DEFS`의 id와 blueprints.json의 id를 대조하여 작성:

```kotlin
package com.example.jaygame.data

/**
 * 레거시 세이브 (saveVersion <= 2) → 신규 세이브 (saveVersion 3) 마이그레이션.
 * 기존 Int 인덱스 기반 units 리스트를 String blueprintId 기반 Map으로 변환.
 */
object LegacyMigration {

    /**
     * 레거시 UNIT_DEFS[i].id → blueprints.json의 id 매핑.
     * UNIT_DEFS에는 42종이 있었고, 속성별 7단계(COMMON~IMMORTAL).
     * 매핑은 속성+등급 조합으로 대응하는 blueprint를 찾는다.
     */
    val LEGACY_INDEX_TO_BLUEPRINT_ID: Map<Int, String> = mapOf(
        // 화염 (FIRE)
        0 to "fire_rdps_01",    // 루비 COMMON
        5 to "fire_rdps_02",    // 카르마 RARE
        10 to "fire_rdps_03",   // 이그니스 HERO
        15 to "fire_rdps_04",   // 인페르노 LEGEND
        20 to "fire_rdps_05",   // 화산왕 ANCIENT
        25 to "fire_rdps_06",   // 피닉스 MYTHIC
        30 to "fire_rdps_07",   // 태양신 라 IMMORTAL
        // 냉기 (FROST)
        1 to "frost_rdps_01",   // 미스트 COMMON
        6 to "frost_rdps_02",   // 프로스트 RARE
        11 to "frost_rdps_03",  // 블리자드 HERO
        16 to "frost_rdps_04",  // 아이스본 LEGEND
        21 to "frost_rdps_05",  // 빙하제왕 ANCIENT
        26 to "frost_rdps_06",  // 유키 MYTHIC
        31 to "frost_rdps_07",  // 크로노스 IMMORTAL
        // 독 (POISON)
        2 to "poison_rdps_01",  // 베놈 COMMON
        7 to "poison_rdps_02",  // 바이퍼 RARE
        12 to "poison_rdps_03", // 플레이그 HERO
        17 to "poison_rdps_04", // 코로시브 LEGEND
        22 to "poison_rdps_05", // 헤카테 ANCIENT
        27 to "poison_rdps_06", // 니드호그 MYTHIC
        32 to "poison_rdps_07", // 아포칼립스 IMMORTAL
        // 번개 (LIGHTNING)
        3 to "lightning_rdps_01",  // 스파크 COMMON
        8 to "lightning_rdps_02",  // 볼트 RARE
        13 to "lightning_rdps_03", // 썬더 HERO
        18 to "lightning_rdps_04", // 스톰 LEGEND
        23 to "lightning_rdps_05", // 뇌왕 ANCIENT
        28 to "lightning_rdps_06", // 토르 MYTHIC
        33 to "lightning_rdps_07", // 제우스 IMMORTAL
        // 보조 (SUPPORT)
        4 to "support_support_01",  // 뮤즈 COMMON
        9 to "support_support_02",  // 가디언 RARE
        14 to "support_support_03", // 오라클 HERO
        19 to "support_support_04", // 발키리 LEGEND
        24 to "support_support_05", // 세라핌 ANCIENT
        29 to "support_support_06", // 아르카나 MYTHIC
        34 to "support_support_07", // 가이아 IMMORTAL
        // 바람 (WIND)
        35 to "wind_rdps_01",  // 제피르 COMMON
        36 to "wind_rdps_02",  // 게일 RARE
        37 to "wind_rdps_03",  // 사이클론 HERO
        38 to "wind_rdps_04",  // 태풍 LEGEND
        39 to "wind_rdps_05",  // 하늘군주 ANCIENT
        40 to "wind_rdps_06",  // 실프 MYTHIC
        41 to "wind_rdps_07",  // 바유 IMMORTAL
    )

    /**
     * 레거시 List<UnitProgress> (인덱스 기반) → Map<String, UnitProgress> (blueprintId 기반)
     */
    fun migrateUnits(legacyUnits: List<UnitProgress>): Map<String, UnitProgress> {
        val result = mutableMapOf<String, UnitProgress>()
        legacyUnits.forEachIndexed { index, progress ->
            val blueprintId = LEGACY_INDEX_TO_BLUEPRINT_ID[index]
            if (blueprintId != null && progress.owned) {
                result[blueprintId] = progress
            }
        }
        return result
    }
}
```

> **주의**: 위 매핑 ID는 추정값이다. 반드시 아래 검증 단계를 수행할 것.

- [ ] **Step 1.5: 매핑 테이블 검증 (필수)**

blueprints.json의 실제 id 목록과 매핑 테이블을 대조하여 모든 value가 유효한 blueprintId인지 확인:

```bash
# blueprints.json에서 모든 id 추출
python -c "
import json
with open('app/src/main/assets/units/blueprints.json', encoding='utf-8') as f:
    units = json.load(f)
ids = {u['id'] for u in units}
# LegacyMigration 테이블의 모든 value가 ids에 포함되는지 확인
legacy_map = {
    0: 'fire_rdps_01', 5: 'fire_rdps_02', 10: 'fire_rdps_03',
    # ... 전체 테이블
}
for idx, bid in legacy_map.items():
    if bid not in ids:
        print(f'MISSING: index {idx} -> {bid}')
    else:
        bp = next(u for u in units if u['id'] == bid)
        print(f'OK: index {idx} -> {bid} ({bp[\"name\"]})')
"
```

매핑이 틀린 항목이 있으면 blueprints.json에서 이름/속성/등급이 일치하는 올바른 id로 수정한 뒤 커밋할 것. **검증 없이 커밋 금지.**

- [ ] **Step 2: GameData.kt 스키마 변경**

```kotlin
// 변경 전:
// val units: List<UnitProgress> = List(42) { UnitProgress() },
// val deck: List<Int> = listOf(0, 1, 2),

// 변경 후:
val units: Map<String, UnitProgress> = emptyMap(),
// deck 필드 제거
```

`UnitProgress` 자체는 변경 없음 (owned, level, cards 등).

- [ ] **Step 3: GameRepository serialize() 수정**

`saveVersion`을 3으로 변경. units를 Map으로 직렬화:

```kotlin
// units 직렬화
json.name("units").beginObject()
data.units.forEach { (blueprintId, progress) ->
    json.name(blueprintId).beginObject()
    json.name("owned").value(progress.owned)
    json.name("level").value(progress.level)
    json.name("cards").value(progress.cards)
    json.endObject()
}
json.endObject()

// deck 직렬화 제거
```

- [ ] **Step 4: GameRepository deserialize() 수정**

saveVersion에 따라 분기:

```kotlin
// saveVersion < 3: 레거시 마이그레이션
if (saveVersion < 3) {
    val legacyUnits = parseLegacyUnitsList(reader) // 기존 List<UnitProgress> 파싱
    units = LegacyMigration.migrateUnits(legacyUnits)
    // deck 필드 무시 (있으면 skip)
} else {
    // saveVersion >= 3: Map<String, UnitProgress> 파싱
    units = parseUnitsMap(reader)
}
```

- [ ] **Step 5: GameData를 참조하는 모든 .units 사용처 수정**

`data.units[index]` → `data.units[blueprintId]` 형태로 변경.
주요 사용처:
- `CollectionScreen.kt` — `data.units.count { it.owned }` → `data.units.count { it.value.owned }`
- `UnitCollectionScreen.kt` — 인덱스 기반 접근 → blueprintId 기반
- `BattleEngine.kt` — 소환 시 진행도 확인

- [ ] **Step 6: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add -A
git commit -m "feat: GameData 스키마 변경 — units Map<String, UnitProgress>, 세이브 마이그레이션 v3"
```

---

## Task 5: BattleEngine 덱 제거 + 소환 통일

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/engine/BattleEngine.kt` (lines 22-29, 61-63, 224, 731-814)
- Modify: `app/src/main/java/com/example/jaygame/engine/AbilitySystem.kt` (line 11)
- Modify: `app/src/main/java/com/example/jaygame/engine/SynergySystem.kt` (lines 38-80)
- Modify: `app/src/main/java/com/example/jaygame/bridge/BattleBridge.kt` (lines 672-679)

- [ ] **Step 1: BattleEngine 생성자에서 deck 파라미터 제거**

```kotlin
// 변경 전:
class BattleEngine(
    ...,
    private val deck: IntArray,
    ...
)

// 변경 후:
class BattleEngine(
    ...,
    // deck 파라미터 제거
    ...
)
```

- [ ] **Step 2: requestSummon() 레거시 메서드 제거**

`requestSummon()` (lines 731-768) 전체 삭제. `requestSummonBlueprint()` (lines 771-814)만 유지.
호출부에서 `requestSummon()` → `requestSummonBlueprint()`로 변경.

- [ ] **Step 2.5: BattleBridge.kt 레거시 fallback 제거**

`BattleBridge.requestSummon()` (lines 672-679)에서 레거시 분기 제거:

```kotlin
// 변경 전:
fun requestSummon() {
    val eng = engine ?: return
    if (eng.blueprintRegistry.count() > 0) {
        eng.requestSummonBlueprint()
    } else {
        eng.requestSummon()  // 이 라인 삭제 — 컴파일 에러 발생
    }
}

// 변경 후:
fun requestSummon() {
    val eng = engine ?: return
    eng.requestSummonBlueprint()
}
```

- [ ] **Step 2.6: BattleEngine.permanentUnitLevels 마이그레이션**

`BattleEngine.kt` lines 61-63의 `permanentUnitLevels` 초기화를 `Map<String, UnitProgress>` 기반으로 변경:

```kotlin
// 변경 전:
private val permanentUnitLevels: Map<Int, Int> = gameData?.let { data ->
    data.units.mapIndexed { idx, progress -> idx to progress.level }.toMap()
} ?: emptyMap()

// 변경 후:
private val permanentUnitLevels: Map<String, Int> = gameData?.let { data ->
    data.units.mapValues { (_, progress) -> progress.level }
} ?: emptyMap()
```

이 Map을 참조하는 모든 곳도 Int 키 → String 키로 변경.

- [ ] **Step 3: AbilitySystem.activeSynergy 초기화 변경**

```kotlin
// 변경 전 (BattleEngine.start() line 224):
// AbilitySystem.activeSynergy = SynergySystem.getActiveSynergies(deck)

// 변경 후: 초기값 빈 맵, 유닛 배치/제거 시 갱신
AbilitySystem.activeSynergy = emptyMap()
```

유닛 배치/제거 이벤트에서 갱신하는 메서드 추가:

```kotlin
private fun refreshSynergies() {
    val activeUnits = units.filter { it.alive }
    AbilitySystem.activeSynergy = SynergySystem.getActiveSynergies(activeUnits)
}
```

유닛 배치(`placeUnit`), 유닛 사망, 유닛 제거 시 `refreshSynergies()` 호출.

- [ ] **Step 4: SynergySystem.kt에서 덱 기반 오버로드 제거**

```kotlin
// 제거: getSynergyBonus(deck: IntArray, unitFamily: Int) (lines 38-80)
// 제거: getActiveSynergies(deck: IntArray) (line 85)
// 유지: 필드 기반 메서드들 (lines 95-134)
```

- [ ] **Step 5: BattleEngine 생성 호출부 수정**

`MainActivity.kt`, `ComposeActivity.kt` 등에서 `BattleEngine(...)` 호출 시 deck 인자 제거.

- [ ] **Step 6: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add -A
git commit -m "feat: BattleEngine 덱 제거, 소환 requestSummonBlueprint() 단일 경로, 시너지 필드 기반"
```

---

## Task 6: 역할 시너지 게임루프 연결

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/engine/BattleEngine.kt`

- [ ] **Step 1: BattleEngine에 역할 시너지 캐시 추가**

```kotlin
private var roleSynergyCache: Map<UnitRole, RoleSynergySystem.RoleSynergyBonus> = emptyMap()
```

- [ ] **Step 2: refreshSynergies()에 역할 시너지도 포함**

```kotlin
private fun refreshSynergies() {
    val activeUnits = units.filter { it.alive }
    // 속성 시너지
    AbilitySystem.activeSynergy = SynergySystem.getActiveSynergies(activeUnits)
    // 역할 시너지
    roleSynergyCache = UnitRole.entries.associateWith { role ->
        RoleSynergySystem.getBonus(activeUnits, role)
    }
}
```

- [ ] **Step 3: 유닛 스탯 계산에 역할 시너지 보너스 적용**

공격 처리 시 해당 유닛의 역할에 맞는 보너스를 적용:

```kotlin
// 공격 데미지 계산 시:
val roleBonus = roleSynergyCache[unit.role] ?: RoleSynergySystem.NO_BONUS_PLACEHOLDER
val finalAtk = unit.baseATK * roleBonus.atkMultiplier
val finalRange = unit.range * roleBonus.rangeMultiplier
// ... 기타 보너스 적용
```

- [ ] **Step 4: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "feat: RoleSynergySystem 게임루프 연결 — 필드 기반 역할 시너지 적용"
```

---

## Task 7: 도감 — BlueprintRegistry 기반 전환 + 필터 바

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/UnitCollectionScreen.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/CollectionScreen.kt`

- [ ] **Step 1: UnitCollectionScreen — 데이터 소스 전환**

레거시 `UNIT_DEFS` → `BlueprintRegistry.instance`:

```kotlin
// 변경 전:
val unitsByFamily = remember {
    UnitFamily.entries.map { family ->
        family to UNIT_DEFS.filter { it.family == family }
    }
}

// 변경 후:
val allBlueprints = remember {
    BlueprintRegistry.instance.findByCategory(UnitCategory.NORMAL)
}
```

- [ ] **Step 2: 필터 상태 추가**

```kotlin
var selectedRoles by remember { mutableStateOf(emptySet<UnitRole>()) }
var selectedFamilies by remember { mutableStateOf(emptySet<UnitFamily>()) }
var selectedGrades by remember { mutableStateOf(emptySet<UnitGrade>()) }
var sortMode by remember { mutableStateOf(SortMode.GRADE) }

enum class SortMode { GRADE, ATK, NAME }
```

- [ ] **Step 3: 필터 바 Composable 작성**

가로 스크롤 칩 행 3줄 (역할, 속성, 등급) + 정렬 드롭다운:

```kotlin
@Composable
private fun FilterBar(
    selectedRoles: Set<UnitRole>,
    onRolesChange: (Set<UnitRole>) -> Unit,
    selectedFamilies: Set<UnitFamily>,
    onFamiliesChange: (Set<UnitFamily>) -> Unit,
    selectedGrades: Set<UnitGrade>,
    onGradesChange: (Set<UnitGrade>) -> Unit,
    sortMode: SortMode,
    onSortChange: (SortMode) -> Unit,
) {
    Column {
        // 역할 칩 행
        LazyRow { items(UnitRole.entries) { role ->
            FilterChip(
                selected = role in selectedRoles,
                onClick = { /* toggle */ },
                label = { Text(role.label) }
            )
        }}
        // 속성 칩 행
        LazyRow { items(UnitFamily.entries) { family ->
            FilterChip(
                selected = family in selectedFamilies,
                onClick = { /* toggle */ },
                label = { Text(family.label) }
            )
        }}
        // 정렬
        Row {
            SortMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == sortMode,
                    onClick = { onSortChange(mode) },
                    label = { Text(when(mode) {
                        SortMode.GRADE -> "등급순"
                        SortMode.ATK -> "공격력순"
                        SortMode.NAME -> "이름순"
                    })}
                )
            }
        }
    }
}
```

- [ ] **Step 4: 필터/정렬 적용 로직**

```kotlin
val filteredBlueprints = remember(allBlueprints, selectedRoles, selectedFamilies, selectedGrades, sortMode) {
    allBlueprints
        .filter { bp ->
            (selectedRoles.isEmpty() || bp.role in selectedRoles) &&
            (selectedFamilies.isEmpty() || bp.families.any { it in selectedFamilies }) &&
            (selectedGrades.isEmpty() || bp.grade in selectedGrades)
        }
        .sortedWith(when (sortMode) {
            SortMode.GRADE -> compareByDescending { it.grade.ordinal }
            SortMode.ATK -> compareByDescending { it.stats.baseATK }
            SortMode.NAME -> compareBy { it.name }
        })
}
```

- [ ] **Step 5: 유닛 카드 개선 — 역할/데미지타입/거리 표시**

`CodexUnitCard`에 추가 정보 표시:

```kotlin
// 카드 하단에 역할 아이콘 + 데미지 타입 색상 표시
Row {
    Text(
        text = roleIcon(blueprint.role),
        fontSize = 10.sp,
    )
    Spacer(modifier = Modifier.width(2.dp))
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                if (blueprint.damageType == DamageType.PHYSICAL) Color(0xFFFF8A80)
                else Color(0xFFCE93D8),
                CircleShape
            )
    )
    Spacer(modifier = Modifier.width(2.dp))
    // 공격 거리 표시
    Text(
        text = if (blueprint.attackRange == AttackRange.MELEE) "근" else "원",
        fontSize = 8.sp,
        color = SubText,
    )
}
```

역할 아이콘 + 행동 패턴 라벨 유틸:

```kotlin
private fun roleIcon(role: UnitRole): String = when (role) {
    UnitRole.TANK -> "🛡"
    UnitRole.MELEE_DPS -> "⚔"
    UnitRole.RANGED_DPS -> "🏹"
    UnitRole.SUPPORT -> "✚"
    UnitRole.CONTROLLER -> "⛓"
}

private fun behaviorLabel(behaviorId: String): String = when (behaviorId) {
    "tank_blocker" -> "전방 저지형"
    "assassin_dash" -> "돌진 암살형"
    "ranged_mage" -> "원거리 마법형"
    "support_aura" -> "오라 지원형"
    "controller_cc_ranged" -> "원거리 제어형"
    else -> behaviorId
}
```

- [ ] **Step 6: 상세 다이얼로그 개선 — 전체 스탯 + 역할 뱃지**

UnitDetailDialog를 UnitBlueprint 기반으로 수정. 모든 스탯 한국어로 표시:

```kotlin
// 스탯 섹션
StatItem(label = "체력", value = "${bp.stats.hp}")
StatItem(label = "공격력", value = "${bp.stats.baseATK}")
StatItem(label = "공속", value = "%.1f".format(bp.stats.baseSpeed))
StatItem(label = "사거리", value = "${bp.stats.range.toInt()}")
StatItem(label = "방어력", value = "${bp.stats.defense}")
StatItem(label = "마법저항", value = "${bp.stats.magicResist}")
StatItem(label = "이동속도", value = "${bp.stats.moveSpeed}")
if (bp.stats.blockCount > 0) {
    StatItem(label = "블록 수", value = "${bp.stats.blockCount}")
}

// 역할 뱃지
RoleBadge(role = bp.role)

// 데미지 타입
Text(text = if (bp.damageType == DamageType.PHYSICAL) "물리" else "마법")

// 공격 거리
Text(text = if (bp.attackRange == AttackRange.MELEE) "근접" else "원거리")

// 행동 패턴 설명
Text(text = behaviorLabel(bp.behaviorId), color = SubText, fontSize = 11.sp)
```

- [ ] **Step 7: CollectionScreen HeroCollectionTab도 동일하게 전환**

`CollectionScreen.kt`의 `HeroCollectionTab`도 BlueprintRegistry 기반으로 전환.
필터 바 공유. `GameData.units` Map 기반 접근으로 수정.

- [ ] **Step 8: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: 커밋**

```bash
git add -A
git commit -m "feat: 도감 BlueprintRegistry 기반 전환 — 다축 필터, 카드/상세 개선, 용어 한국어 통일"
```

---

## Task 8: 도감 — 히든 탭 구현

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/UnitCollectionScreen.kt`

- [ ] **Step 1: 히든 탭 데이터 로딩**

```kotlin
1 -> {
    val hiddenBlueprints = remember {
        BlueprintRegistry.instance.findByCategory(UnitCategory.HIDDEN)
    }
    val recipes = remember {
        RecipeSystem.instance.allRecipes()
    }
    // ...
}
```

- [ ] **Step 2: 히든 유닛 카드 — 발견/미발견 분기**

```kotlin
@Composable
private fun HiddenUnitCard(
    blueprint: UnitBlueprint,
    recipe: HiddenRecipe?,
    discovered: Boolean,
) {
    if (discovered) {
        // 풀 카드 — 일반 탭 카드와 동일 스타일
        CodexBlueprintCard(blueprint = blueprint, isOwned = true, onClick = { ... })
    } else {
        // 실루엣 카드
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("???", color = Color(0xFF333333), fontSize = 16.sp)
                Text("미발견", color = Color(0xFF555555), fontSize = 11.sp)
                if (recipe != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    // 조합 힌트
                    recipe.ingredients.forEach { ingredient ->
                        Text(
                            text = "${ingredient.family} ${ingredient.role.label}(${ingredient.minGrade.label}+)",
                            color = Color(0xFF666666),
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add -A
git commit -m "feat: 도감 히든 탭 구현 — 발견/미발견, 조합 레시피 힌트"
```

---

## Task 9: 도감 — 특수 탭 구현

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/UnitCollectionScreen.kt`

- [ ] **Step 1: 특수 탭 데이터 로딩 + 카드 렌더링**

```kotlin
2 -> {
    val specialBlueprints = remember {
        BlueprintRegistry.instance.findByCategory(UnitCategory.SPECIAL)
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("최대 2기 배치 가능", color = SubText, fontSize = 12.sp)
        specialBlueprints.forEach { bp ->
            SpecialUnitCard(blueprint = bp)
        }
    }
}
```

- [ ] **Step 2: SpecialUnitCard 작성 — 다중 속성 뱃지 + 필드 효과 강조**

```kotlin
@Composable
private fun SpecialUnitCard(blueprint: UnitBlueprint) {
    GameCard(borderColor = blueprint.grade.color) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // 이름 + 등급
            Text(blueprint.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = LightText)
            Text(blueprint.grade.label, color = blueprint.grade.color, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(6.dp))

            // 다중 속성 뱃지
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                blueprint.families.forEach { family ->
                    Box(
                        modifier = Modifier
                            .background(family.color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(family.label, color = family.color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 역할 + 타입 표시
            Row {
                Text("${roleIcon(blueprint.role)} ${blueprint.role.label}", color = SubText, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (blueprint.damageType == DamageType.PHYSICAL) "물리" else "마법",
                    color = SubText, fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 필드 효과 설명 (강조)
            Text(
                text = blueprint.description,
                color = Gold,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
```

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add -A
git commit -m "feat: 도감 특수 탭 구현 — 10종 특수 유닛, 다중 속성 뱃지, 필드 효과"
```

---

## Task 10: 전투 UI — 시너지 패널

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/battle/SynergyPanel.kt`
- Modify: `app/src/main/java/com/example/jaygame/bridge/BattleBridge.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/battle/BattleField.kt` (또는 전투 오버레이)

- [ ] **Step 1: SynergyPanel Composable 작성**

필드 배치 유닛의 활성 시너지를 표시하는 컴팩트 패널:

```kotlin
@Composable
fun SynergyPanel(
    familySynergies: Map<UnitFamily, Int>,  // 속성별 유닛 수
    roleSynergies: Map<UnitRole, Int>,      // 역할별 유닛 수
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(4.dp)) {
        // 역할 시너지 (2+ 활성된 것만)
        roleSynergies.filter { it.value >= 2 }.forEach { (role, count) ->
            SynergyChip(
                icon = roleIcon(role),
                label = role.label,
                count = count,
                color = roleColor(role),
                maxTier = 4,
            )
        }
        // 속성 시너지 (2+ 활성된 것만)
        familySynergies.filter { it.value >= 2 }.forEach { (family, count) ->
            SynergyChip(
                icon = familyIcon(family),
                label = family.label,
                count = count,
                color = family.color,
                maxTier = 3,
            )
        }
    }
}

@Composable
private fun SynergyChip(
    icon: String,
    label: String,
    count: Int,
    color: Color,
    maxTier: Int,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 1.dp)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 10.sp)
        Spacer(modifier = Modifier.width(3.dp))
        Text(label, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(3.dp))
        Text("$count", fontSize = 9.sp, color = color)
    }
}
```

- [ ] **Step 2: BattleBridge에서 시너지 데이터 StateFlow 추가**

```kotlin
// BattleBridge.kt에 추가:
private val _activeFamilySynergies = MutableStateFlow<Map<UnitFamily, Int>>(emptyMap())
val activeFamilySynergies: StateFlow<Map<UnitFamily, Int>> = _activeFamilySynergies

private val _activeRoleSynergies = MutableStateFlow<Map<UnitRole, Int>>(emptyMap())
val activeRoleSynergies: StateFlow<Map<UnitRole, Int>> = _activeRoleSynergies
```

BattleEngine의 `refreshSynergies()`에서 BattleBridge로 push.

- [ ] **Step 3: 전투 오버레이에 SynergyPanel 배치**

전투 화면 좌측 상단 또는 적절한 위치에 배치:

```kotlin
val familySynergies by bridge.activeFamilySynergies.collectAsState()
val roleSynergies by bridge.activeRoleSynergies.collectAsState()

SynergyPanel(
    familySynergies = familySynergies,
    roleSynergies = roleSynergies,
    modifier = Modifier.align(Alignment.TopStart),
)
```

- [ ] **Step 4: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "feat: 전투 UI 시너지 패널 — 역할/속성 시너지 실시간 표시"
```

---

## Task 11: 레거시 코드 정리

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/data/UnitDefs.kt`

- [ ] **Step 1: UNIT_DEFS 사용처가 남아있는지 확인**

```bash
grep -r "UNIT_DEFS" --include="*.kt" app/src/main/java/ | grep -v "LegacyMigration"
```

모든 참조가 BlueprintRegistry로 전환되었으면 `@Deprecated` 어노테이션 유지하되 주석으로 "마이그레이션 전용으로만 참조" 표기.

완전히 참조 없으면 삭제 가능하나, `LegacyMigration`에서 매핑 검증용으로 유지 권장.

- [ ] **Step 2: import 정리**

사용하지 않는 import 제거 (DeckScreen 관련, UNIT_DEFS 관련 등).

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add -A
git commit -m "chore: 레거시 코드 정리 — 미사용 import/참조 제거"
```

---

## Task 12: 최종 통합 테스트

- [ ] **Step 1: 앱 실행 — 네비게이션 확인**

- 4탭 구조 (도감, 홈, 상점, 설정) 정상 표시
- 덱 메뉴 진입점 없음

- [ ] **Step 2: 도감 — 일반 탭 확인**

- 75종 유닛 표시
- 필터 바 작동 (역할/속성/등급)
- 정렬 작동 (등급순/공격력순/이름순)
- 카드에 역할 아이콘, 데미지 타입 표시
- 상세 다이얼로그에 전체 스탯 + 한국어 용어

- [ ] **Step 3: 도감 — 히든 탭 확인**

- 18종 히든 유닛 (발견/미발견)
- 조합 레시피 힌트 표시

- [ ] **Step 4: 도감 — 특수 탭 확인**

- 10종 특수 유닛
- 다중 속성 뱃지 표시
- 필드 효과 설명 강조

- [ ] **Step 5: 전투 진입 — 소환 확인**

- 덱 없이 전체 풀 랜덤 소환 작동
- 등급 가중치 기반 (일반 > 희귀 > 영웅 > 전설)

- [ ] **Step 6: 전투 — 시너지 패널 확인**

- 같은 역할/속성 유닛 2+ 배치 시 시너지 패널 표시
- 유닛 제거 시 실시간 갱신

- [ ] **Step 7: 세이브/로드 확인**

- 기존 세이브 파일 → 마이그레이션 정상
- 새 세이브 → 로드 정상

- [ ] **Step 8: 최종 커밋**

```bash
git add -A
git commit -m "test: 도감/시너지 개편 통합 테스트 완료"
```
