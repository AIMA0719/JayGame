# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew assembleDebug                    # Build debug APK
./gradlew test                             # Run all unit tests
./gradlew test --tests "com.example.jaygame.engine.DamageCalculatorTest"  # Single test
./gradlew compileDebugKotlin               # Compile check only
./gradlew clean assembleDebug              # Clean build
```

Android SDK 36, min SDK 26, Java 11. Single module (`:app`).

## Architecture

### Two-Activity Model
- `ComposeActivity` — Hub/menu screens (Home, Collection, Shop, Settings) via Compose Navigation
- `MainActivity` — Battle screen, launched via Intent. Runs the game engine

### Data Flow (Orbit MVI)
```
JayGameApplication (singleton GameRepository)
  → GameRepository (SharedPreferences + JSON + FNV-1a checksum)
    → StateFlow<GameData>
      → ViewModel (Orbit ContainerHost) → State/SideEffect
        → Composable Screen (collectAsState / collectSideEffect)

Battle:
  → MainActivity → BattleEngine → BattleBridge (StateFlow) → BattleScreen composables
  → BattleRewardCalculator (pure function for reward calculation)
```

### Orbit MVI Pattern
- **ViewModel**: `ContainerHost<State, SideEffect>`, `container()`, `intent {}`, `reduce {}`, `postSideEffect()`
- **Composable**: `viewModel.collectAsState()`, `viewModel.collectSideEffect { }`
- **ViewModelFactory**: `GameViewModelFactory` — 수동 factory, DI 프레임워크 미사용
- 각 ViewModel은 `init`에서 `repository.gameData.collect`로 상태 동기화
- **새 ViewModel 추가 시**: `ViewModelFactory.kt`의 `when` 블록에 등록 필요

### Initialization Sequence
**JayGameApplication**: `BlueprintRegistry.initialize()` → `RecipeSystem.initialize()` → `GameRepository(ctx)` (singleton)

**ComposeActivity**: `enableEdgeToEdge()` → `SfxManager.init()` → `setContent { AppViewModel + NavGraph }`

**MainActivity**: `BlueprintRegistry.initialize()` (reinit — GC safety) → `RecipeSystem.initialize()` → repository from Application → `BattleEngine(...)` → `setContent { BattleScreen }`

### Package Structure (`com.example.jaygame`)
| Package | Purpose |
|---------|---------|
| `engine/` | BattleEngine (core loop, 6000+ lines), GameUnit, Enemy, Grid, WaveSystem, BuffSystem, MergeSystem, SpatialHash, ability systems |
| `engine/behavior/` | Unit behavior strategies (UnitBehavior interface implementations) |
| `engine/math/` | Vec2, GameRect |
| `data/` | GameData model, GameRepository, `*Defs.kt` definitions, StageData, StaminaManager |
| `bridge/` | BattleBridge — StateFlow-based decoupling of engine ↔ Compose UI |
| `ui/viewmodel/` | Orbit MVI ViewModels: App, Home, Shop, Collection, Settings + ViewModelFactory |
| `ui/battle/` | Canvas-based BattleField, overlay composables (enemies, projectiles, effects, HUD) |
| `ui/screens/` | Hub screen composables |
| `ui/components/` | Reusable UI (NeonButton, MedievalCard, ResourceBar, dialogs) |
| `ui/theme/` | Dark fantasy/neon color palette, Material 3 theme |
| `navigation/` | NavGraph (tab routing + fade transitions), Routes constants |
| `audio/` | BgmManager (MediaPlayer BGM), SfxManager (sound effects) |

## Game Engine Rules

- **Fixed timestep:** `FIXED_DT = 1f/60f`, coroutine on `Dispatchers.Default`
- **Object pooling:** `ObjectPool<T>` — enemies(256), units(128), projectiles(512). Canvas 렌더링에서 GC 절대 유발 금지
- **Spatial hashing:** `SpatialHash<Enemy>` cell 64×64 for range/collision queries
- **Grid:** 3×6 슬롯 기반 그리드 (18슬롯), 480×300 필드, origin (120, 430) on 720×1280 canvas
- **유닛 배치 (운빨존많겜 스타일):**
  - 슬롯에 유닛 배치, 배치 후 **위치 고정** (자동 이동 없음)
  - 사거리 내 적 **자동 공격** (타워 디펜스 방식)
  - 같은 유닛을 **드래그하여 같은 슬롯에 중첩** (최대 3개)
  - **3개 중첩 시 자동 합성** → 상위 등급 유닛 1개
  - 유닛 탭 → **코인으로 강화** (ATK +50%/레벨)
- **Monster path:** 그리드 주변 ㅁ형 직사각형 경로 (pathLeft=85, pathTop=395, pathRight=635, pathBottom=765), 30px 코너 보간
- **슬롯→경로 거리:** 모서리 75px, 상하 가장자리 85px, 내부 가장자리 155px, 중앙 185px
- **사거리 설계 (위치 전략):**
  - TANK: 85~100 (가장자리 14/18슬롯에서만 공격 가능)
  - MELEE_DPS: 90~140 (가장자리 전용, 고등급은 내부 일부 커버)
  - RANGED_DPS: 160~260 (COMMON/RARE는 중앙 8,9 불가, HERO↑ 전체 커버)
  - CONTROLLER: 200~270 (전 슬롯 커버)
- **Systems are volatile singletons:** `BlueprintRegistry`, `RecipeSystem`, `UniqueAbilitySystem` — 전역 상태, 매 프레임 BattleEngine에서 갱신

### Canvas Rendering Policy
- **모든 Color, Paint, Brush 객체는 파일 상단에 pre-allocate** — `drawScope` 내부에서 객체 생성 금지
- 렌더링 함수에서 allocation 발생 시 GC 스파이크로 프레임 드롭 발생

## Game Content Structure

### Grade System (5 Tiers)
| Grade | Korean | Ordinal | Summonable | Notes |
|-------|--------|---------|------------|-------|
| COMMON | 일반 | 0 | Yes (weight 60) | 기본 소환 유닛 |
| RARE | 희귀 | 1 | Yes (weight 25) | |
| HERO | 영웅 | 2 | Yes (weight 12) | 패시브 어빌리티 |
| LEGEND | 전설 | 3 | Yes (weight 3) | 마나 궁극기 |
| MYTHIC | 신화 | 4 | No | **레시피 전용** — 특정 영웅 조합으로만 획득 |

### Adding New Units
1. **Blueprint JSON** — `assets/units/blueprints.json` 또는 `special_units.json`에 추가
   - 필수 필드: `id`, `name`, `families`, `grade`, `role`, `stats`, `behaviorId`, `isSummonable`, `summonWeight`, `iconRes`
2. **Behavior** (선택) — `engine/behavior/YourBehavior.kt` 생성, `UnitBehavior` interface 구현
3. **BehaviorFactory 등록** — `BehaviorFactory.createBehavior()` when절에 추가
4. **BlueprintRegistry** — 앱 시작 시 자동 로드, 별도 등록 불필요

### Adding New Stages
`data/StageData.kt`의 `STAGES` 리스트에 `StageDef` 추가:
- `unlockTrophies`: 해금 트로피 수
- `maxWaves`: **60 고정** (전 스테이지 동일)
- `bgAsset`: 배경 이미지 경로
- 색상: `bgColors`, `pathColor`, `fieldColors`

### Wave System (Procedural)
- JSON 없음 — `WaveSystem.getWaveConfig(wave)`에서 절차적 생성
- **전 스테이지 60웨이브 고정**
- Base HP: `50 + wave×30 + wave²×0.5` (wave 40+ 추가 스케일링)
- Boss: 매 10번째 웨이브 (wave 9, 19, 29, 39, 49, 59), HP 10배
- Elite: 랜덤, HP 2배 + armor 1.5배

### Difficulty Multipliers
| Level | Korean | Multiplier |
|-------|--------|-----------|
| 0 | 초보 | 1.0x |
| 1 | 숙련자 | 1.5x |
| 2 | 고인물 | 2.2x |
| 3 | 썩은물 | 3.0x |
| 4 | 챌린저 | 4.0x |

## Core System Rules (반드시 준수)

### Merge System (운빨존많겜 스타일 — 슬롯 중첩 합성)
- 같은 유닛을 **드래그하여 한 슬롯에 중첩** (최대 3개)
- **3개 중첩 시 자동 합성** → `mergeResultId`가 가리키는 상위 등급 유닛 1개
- 소환 시에도 같은 유닛이 이미 있는 슬롯에 자동 중첩
- Lucky merge: 5% base + 유물 보너스 → 2단계 점프
- LEGEND가 일반 합성 최상위 (MYTHIC은 레시피 전용)
- SPECIAL 유닛은 합성 불가

### Recipe System (신화 전용)
- MYTHIC 유닛은 **특정 영웅 조합 레시피**로만 획득
- `RecipeSystem.findMatchingRecipeOnGrid(grid)` — 필드에 레시피 재료가 모이면 자동 감지
- 합성 시 레시피 체크 우선, 일반 합성보다 먼저 실행
- `hidden_recipes.json`에 레시피 정의 (2~3재료, specificUnitId 매칭)

### Buff System
- `BuffType`: Slow, DoT, ArmorBreak, AtkUp, SpdUp, Shield, Stun, Silence, DefUp
- **동일 타입 최대 3개 스택** — 초과 시 가장 오래된 것 제거
- CC 저항: `ccResistance` (0.0~0.9), 보스는 `ccImmune`/`dotImmune` 가능

### Damage Formula
- 물리: `ATK × (1 - armor/(armor+100))` — armor diminishing returns
- 마법: `ATK × (1 - magicResist/(magicResist+100))`

### Field Effects
- 유닛 당 **1개만** (`fieldController: FieldEffectController?`)
- `BattleFieldAccess` interface를 통해 전장 접근 (DI 패턴)
- 종류: Barrier, Forge, PathSlow, TimeWarp

### Mana/Ultimate System (마나 궁극기)
- Grade 2(HERO): 패시브만 (마나 없음)
- Grade 3(LEGEND): 마나 궁극기 — `manaPerHit=9`, ~11회 공격으로 충전
- Grade 4(MYTHIC): 마나 궁극기 — `manaPerHit=6`, ~17회 공격 (더 강한 궁극기)
- 마나 100 도달 시 자동 발동 → 마나 0 리셋
- `UniqueAbilitySystem.update()`에서 매 프레임 처리

### Battle Economy (코인 시스템)
- **소환**: 코인 소비 (시작 10, 소환마다 +2, 최대 60)
- **코인 획득**: 적 처치 2코인, 엘리트 5코인, 웨이브 클리어 20+wave×3
- **SP 자동 회복 없음** — 코인은 전투 활동으로만 획득
- **유닛 판매**: 5 + grade×5 코인 반환

### Battle Enhancement (등급 그룹 강화)
- `UnitUpgradeSystem` — 등급 그룹 단위로 코인 소모하여 ATK/속도 업그레이드 (Lv0~15)
- 3개 그룹: [일반/희귀] (grade 0,1), [고대/전설] (grade 2,3), [신화] (grade 4)
- 그룹 강화 시 해당 그룹의 **모든 유닛**에 보너스 적용
- 마일스톤: Lv3 ATK+10%, Lv6 ATK+15%, Lv9 속도+10%, Lv12 ATK+15%, Lv15 ATK+속도+10%
- 비용: 그룹 0 base 8 (+0.3/lv), 그룹 1 base 15 (+0.4/lv), 그룹 2 base 30 (+0.5/lv)
- 전략: 초반 [일반/희귀] 저렴하게 강화, 후반 [신화] 고비용 고효율

### Pity System (천장)
- `unitPullPity`: 소환 카운터, 배틀 간 유지 (`BattleBridge.unitPullPity`)
- 별도 pet pity도 존재

## Entity Patterns

### UnitBehavior Interface
```kotlin
interface UnitBehavior {
    fun update(unit: GameUnit, dt: Float, findEnemy: (Vec2, Float) -> Enemy?)
    fun onAttack(unit: GameUnit, target: Enemy): AttackResult
    fun onTakeDamage(unit: GameUnit, damage: Float, isMagic: Boolean)
    fun canAttack(): Boolean = true
    fun reset()
}
```
새 행동 추가 시: 이 interface 구현 → `BehaviorFactory`에 등록

### FieldEffectController Interface
```kotlin
interface FieldEffectController {
    fun onPlace(unit: GameUnit, field: BattleFieldAccess)
    fun update(dt: Float, field: BattleFieldAccess)
    fun onRemove()
    fun getEffectRange(): Float
    fun canStack(): Boolean
    fun reset()
}
```

## State Management Patterns

- **StateFlow 패턴**: `MutableStateFlow` → `StateFlow` expose → Composable에서 `collectAsState()`
- **GameData는 data class** — 항상 `copy()`로 불변 업데이트
- **Composable 상태**: `remember`, `mutableStateOf`, `rememberSaveable`
- **Side effects**: `LaunchedEffect`(애니메이션, 코루틴), `DisposableEffect`(오디오 정리)

## Error Handling Conventions

- JSON 파싱: `try/catch` + `Log.w()` + 기본값 반환
- Nullable 안전: `getOrNull()`, `?: default` 패턴
- JSON 필드: `optInt()`, `optString()` 등으로 missing field 대응
- `@Suppress("DEPRECATION")` — 레거시 `UNIT_DEFS` 코드에 사용

## Persistence (GameRepository)

- SharedPreferences `"jaygame_save"` 단일 파일
- JSON 직렬화 + FNV-1a 32-bit checksum 무결성 검증
- `saveVersion` 필드로 스키마 마이그레이션 (`LegacyMigration.kt`)
- 오프라인 보상: `OfflineRewardManager` — 앱 resume 시 계산

## Navigation

`Routes` 상수: HOME, COLLECTION, SHOP, SETTINGS, ACHIEVEMENTS, RESULT, DUNGEON

- Bottom bar: HOME, COLLECTION, SHOP, SETTINGS에서 표시
- Fade in/out 전환 (300ms)
- Battle은 별도 Activity (MainActivity)

## Testing

16 JUnit4 tests in `app/src/test/`:
- Behavior tests: Assassin, RangedShooter, SupportAura, TankBlocker, ControllerCC, BehaviorFactory
- System tests: DamageCalculator, BlueprintRegistry, FieldEffectManager, GameUnit

**테스트 패턴**: 상태 전이 검증, lambda로 enemy/unit mock, 클래스당 하나의 behavior

## Content Reference

### Stages (6개, 전부 60웨이브)
| ID | Name | Waves | Unlock Trophies | Stamina |
|----|------|-------|----------------|---------|
| 0 | 초원 | 60 | 0 | 5 |
| 1 | 정글 | 60 | 200 | 5 |
| 2 | 사막 | 60 | 500 | 6 |
| 3 | 설산 | 60 | 1000 | 6 |
| 4 | 화산 | 60 | 2000 | 7 |
| 5 | 심연 | 60 | 3500 | 8 |

### Dungeons (5 types)
GOLD_RUSH, RELIC_HUNT, PET_EXPEDITION, BOSS_RUSH, SURVIVAL — 일일 3회 제한

### Battle Result Stars
- 3성: 무피해 + 빠른 클리어 (+50% gold)
- 2성: 둘 중 하나 (+25% gold)
- 1성: 승리만

## Language

UI 텍스트와 문서는 한국어. 코드 식별자는 영어, 주석은 한국어/영어 혼용.

### Korean Terminology
| English | Korean |
|---------|--------|
| Grade | 등급 |
| Merge/Fusion | 합성 |
| Recipe | 레시피 (신화 전용 조합) |
| Summon | 소환 |
| Coin | 코인 (배틀 재화) |
| Upgrade | 강화 (배틀 중) |
| Mana | 마나 (궁극기 게이지) |
| Ultimate | 궁극기 |
| Pity | 천장 |
| Stamina | 스태미나 |
| Trophy | 트로피 |
| Relic | 유물 |
| Dungeon | 던전 |
