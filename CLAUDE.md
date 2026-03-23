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
- **Grid:** 5×5, cell 96×96px, origin (400, 120) on 1280×720 canvas
- **Monster path:** S-shaped waypoints outside grid, interpolated curves
- **Systems are volatile singletons:** `BlueprintRegistry`, `RecipeSystem`, `UniqueAbilitySystem` — 전역 상태, 매 프레임 BattleEngine에서 갱신

### Canvas Rendering Policy
- **모든 Color, Paint, Brush 객체는 파일 상단에 pre-allocate** — `drawScope` 내부에서 객체 생성 금지
- 렌더링 함수에서 allocation 발생 시 GC 스파이크로 프레임 드롭 발생

## Game Content Structure

### Grade System (7 Tiers)
| Grade | Korean | Ordinal | Summonable | Notes |
|-------|--------|---------|------------|-------|
| COMMON | 일반 | 0 | Yes (weight 60) | 기본 소환 유닛 |
| RARE | 희귀 | 1 | Yes (weight 25) | |
| HERO | 영웅 | 2 | Yes (weight 12) | 유니크 어빌리티 시작 tier |
| LEGEND | 전설 | 3 | Yes (weight 3) | |
| ANCIENT | 고대 | 4 | No | 합성 전용 |
| MYTHIC | 신화 | 5 | No | 합성 전용 |
| IMMORTAL | 불멸 | 6 | No | 합성 전용 |

### Adding New Units
1. **Blueprint JSON** — `assets/units/blueprints.json` 또는 `special_units.json`에 추가
   - 필수 필드: `id`, `name`, `families`, `grade`, `role`, `stats`, `behaviorId`, `isSummonable`, `summonWeight`, `iconRes`
2. **Behavior** (선택) — `engine/behavior/YourBehavior.kt` 생성, `UnitBehavior` interface 구현
3. **BehaviorFactory 등록** — `BehaviorFactory.createBehavior()` when절에 추가
4. **BlueprintRegistry** — 앱 시작 시 자동 로드, 별도 등록 불필요

### Adding New Stages
`data/StageData.kt`의 `STAGES` 리스트에 `StageDef` 추가:
- `unlockTrophies`: 해금 트로피 수
- `maxWaves`: 최대 웨이브 (40, 45, 50, 60)
- `bgAsset`: 배경 이미지 경로
- 색상: `bgColors`, `pathColor`, `fieldColors`

### Wave System (Procedural)
- JSON 없음 — `WaveSystem.getWaveConfig(wave)`에서 절차적 생성
- Base HP: `60 + wave×36 + wave²×0.6`
- Boss: 매 10번째 웨이브 (wave 9, 19, 29...), HP 10배
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

### Merge System
- **4개** 동일 등급 유닛 → 1개 상위 등급 (not 2개)
- Lucky merge: 5% base + 유물 보너스 → +2등급 점프 가능
- HIDDEN 카테고리: 레시피 매칭으로만 생성
- SPECIAL 유닛은 합성 불가

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

### Unique Abilities
- Grade 2(HERO) 이상에서 활성화 — `UniqueAbilitySystem.initUnit()`
- Grade 2: Passive만, Grade 3+: Active (쿨다운 있음)
- 쿨다운은 family × grade 조합으로 결정

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

### Stages (6개)
| ID | Name | Waves | Unlock Trophies | Stamina |
|----|------|-------|----------------|---------|
| 0 | 초원 | 40 | 0 | 5 |
| 1 | 정글 | 40 | 200 | 5 |
| 2 | 사막 | 45 | 500 | 6 |
| 3 | 설산 | 45 | 1000 | 6 |
| 4 | 화산 | 50 | 2000 | 7 |
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
| Summon | 소환 |
| Pity | 천장 |
| Stamina | 스태미나 |
| Trophy | 트로피 |
| Relic | 유물 |
| Dungeon | 던전 |
