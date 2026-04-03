# JayGame 화면별 감사 보고서

> 분석일: 2026-04-03 | 대상: 전체 UI 화면 7개 영역 + 네비게이션/공통 컴포넌트

---

## 목차
1. [홈(Home)](#1-홈home)
2. [도감(Collection)](#2-도감collection)
3. [상점(Shop)](#3-상점shop)
4. [설정(Settings)](#4-설정settings)
5. [업적(Achievements)](#5-업적achievements)
6. [던전(Dungeon)](#6-던전dungeon)
7. [배틀(Battle)](#7-배틀battle)
8. [네비게이션 & 공통 컴포넌트](#8-네비게이션--공통-컴포넌트)
9. [종합 요약](#9-종합-요약)

---

## 1. 홈(Home)

### 파일
- `ui/screens/HomeScreen.kt`
- `ui/viewmodel/HomeViewModel.kt`
- `ui/components/StageCard.kt`, `ProfileBanner.kt`, `PreBattleDialog.kt`, `DailyLoginDialog.kt`

### 구조
```
Box(전체)
├── 배경 이미지 (Crossfade per stage, Coil AsyncImage)
├── 오버레이 그라데이션
├── 파티클 Canvas (20개 금빛 부유 입자, GC-free)
└── Column(메인 콘텐츠)
    ├── ProfileBanner (레벨/트로피/골드/다이아/EXP바/칭호)
    ├── StageCardPager (HorizontalPager, 6개 스테이지, 3D 틸트)
    └── 하단 Row (스태미나바 + 시즌Tier바 + 전투/던전 버튼)
    + PreBattleDialog (오버레이)
    + DailyLoginDialog / NewTitleDialog (Dialog)
```

### 구현된 기능
- 프로필 배너: 레벨, 트로피, 골드, 다이아, EXP바, 칭호 (애니메이션 카운터 + 시머)
- 스테이지 선택: HorizontalPager 6개 카드, 3D 틸트 터치, 잠금/해금, 베스트웨이브
- 배경: 스테이지별 Crossfade 전환 (500ms)
- 스태미나 바 + 시즌 Tier 표시
- 전투 준비 다이얼로그: 난이도 3단계, 스태미나 비용, 부족 시 비활성
- 일일 출석: 7일 주기 보상, 연속 출석 추적, 시즌XP 보너스
- 칭호 획득 팝업 (Lottie 애니메이션)

### 패턴
- Orbit MVI (`HomeViewModel` → `ContainerHost<HomeState, HomeSideEffect>`)
- `repository.gameData.collect` → `reduce`로 상태 동기화
- SideEffect: `LaunchBattle`, `ShowToast`
- GC-free Canvas 파티클

### 미구현
- 시즌 리셋/종료 로직 없음 (XP 표시만) (의도적)
- 칭호 동시 달성 시 첫 번째만 팝업 (`newIds.first()`)

### 죽은 코드 / 미적용 로직

| 항목 | 위치 | 판정 | 근거 |
|------|------|------|------|
| `import mutableStateOf` | HomeScreen.kt:21 | **실수** | `mutableLongStateOf`로 교체 후 import 미정리 |
| PreBattleDialog 미사용 import 15개 | PreBattleDialog.kt:3-58 | **실수** | 추천 유닛 그리드 기능 삭제 후 잔재 (BlueprintRegistry, LazyVerticalGrid 등) |
| `DungeonManager` remember 없이 생성 | HomeScreen.kt:299 | **실수** | recomposition마다 새 인스턴스. `remember(data)`로 감싸야 함 |
| `StageCardItem.cardSize` | StageCard.kt:138 | **실수** | `onSizeChanged`에서 설정만, 읽는 곳 없음 |
| `StageCardItem.difficulty` 표시 | StageCard.kt:124 | 의도적 | 현재 선택 난이도 표시용 (모든 카드 동일) |

---

## 2. 도감(Collection)

### 파일
- `ui/screens/CollectionScreen.kt`
- `ui/viewmodel/CollectionViewModel.kt`
- `ui/components/UnitUiUtils.kt`
- `ui/screens/RelicScreen.kt`, `PetScreen.kt`
- `ui/screens/UnitCollectionScreen.kt` (레거시)

### 구조
```
Column
├── TabRow (유닛 / 유물 / 펫) 3탭
└── HorizontalPager
    ├── Tab 0: HeroCollectionTab
    │   ├── 수집 진행도 바
    │   ├── 필터 Row (종족 칩 + 사거리 칩 + 정렬)
    │   ├── AnimatedVisibility 필터 섹션
    │   ├── LazyVerticalGrid 4열 (유닛 카드)
    │   └── 오버레이: CollectionBlueprintDetailSheet (커스텀 바텀시트)
    ├── Tab 1: RelicScreen
    └── Tab 2: PetScreen
```

### 구현된 기능
- 3탭 전환 (유닛/유물/펫)
- 유닛 수집 진행도 (보유/전체, 퍼센트)
- 종족(Race) 다중 필터 + 사거리 필터 + 정렬 3종 (등급/ATK/이름)
- 보유 유닛 카드: 아이콘, 이름, 등급, ATK, 별(레벨)
- 미보유 유닛: 자물쇠 + ???
- 상세시트: 스탯, 설명, 능력, 합성 정보, 카드+골드 강화 (Lv1~7)
- 유물 업그레이드/장착/해제
- 펫 소환(1회/10회)/업그레이드/장착/해제

### 패턴
- Orbit MVI (`CollectionViewModel`)
- Manager 패턴 (RelicManager, PetManager)
- `derivedStateOf`로 필터/정렬 캐싱
- 커스텀 바텀시트 (scrim + BottomCenter, Material BottomSheet 미사용)

### 미구현
- DamageType이 import만 되고 미사용 → 근거리=물리, 원거리=마법으로 **하드코딩** (근거리+마법 유닛 잘못 표시)
- 바텀시트 드래그 dismiss 없음 (닫기 버튼만)
- 미보유 유닛 카드 클릭 시 아무 동작 없음

### 죽은 코드 / 미적용 로직

| 항목 | 위치 | 판정 | 근거 |
|------|------|------|------|
| `UnitCollectionScreen.kt` 전체 파일 | 전체 | 의도적 (레거시) | 구 Family/Role 기반. CollectionScreen이 대체. 호출처 0 |
| `import DamageType` | CollectionScreen.kt:70 | **실수** | 상세시트에서 사용해야 하나 attackRange로 하드코딩 |
| `import RACE_LABELS` | CollectionScreen.kt:76 | **실수** | `race.label` 프로퍼티로 대체, import 미정리 |
| `ROLE_LABELS` | UnitUiUtils.kt:98 | 의도적 | `@Deprecated` 마킹됨 |
| `FAMILY_ICONS` | UnitUiUtils.kt:117 | 의도적 | `@Deprecated` 마킹됨 |
| `BEHAVIOR_LABELS` | UnitUiUtils.kt:107 | 불확실 | deprecated 없이 정의, 사용처 미확인 |
| `roleColor()` | UnitUiUtils.kt:127 | 불확실 | Role 비활성화 상태, deprecated 없음 |

---

## 3. 상점(Shop)

### 파일
- `ui/screens/ShopScreen.kt`
- `ui/viewmodel/ShopViewModel.kt`
- `ui/components/GachaProbabilityDialog.kt`
- `data/CardUtils.kt`

### 구조
```
Column
├── ResourceHeader (골드/다이아)
├── 타이틀 Row ('상점' + '확률 보기' 버튼)
├── 4-Tab Row (골드팩/다이아팩/스페셜/시즌패스, 슬라이딩 인디케이터)
└── 탭별 콘텐츠
    ├── 탭 0-2: LazyColumn → ShopItemCard 리스트
    └── 탭 3: SeasonPassContent (XP 진행바 + LazyRow 30티어 카드 트랙)
```

### 구현된 기능
- 골드팩 4종 (다이아→골드)
- 다이아팩 3종 (골드→다이아)
- 랜덤 유닛 카드 구매 (골드 5장 / 다이아 20장)
- 스태미나 충전 (다이아 30 → 스태미나 50)
- 초보자 패키지 (다이아 100 → 골드 5000 + 카드 10장)
- 시즌패스: XP 진행률, 30티어 보상, 개별/일괄 수령
- 가차 확률 다이얼로그
- 탭 슬라이딩 인디케이터 애니메이션

### 패턴
- Orbit MVI (`ShopViewModel`)
- `ShopAction` sealed class로 구매 액션 타입 분리
- `ShopItem` data class 표준화
- `remember {}`로 상품 리스트 캐싱

### 미구현
- 초보자 패키지 **1회 구매 제한 없음** (무한 구매 가능) 
- 시즌 XP 구매/부스트 기능 없음 (의도적)
- 시즌패스 프리미엄 트랙 없음 (의도적)
- 시즌 리셋 로직 없음 (티어 30 이후 XP 무의미 누적) (의도적)

### 죽은 코드 / 미적용 로직

| 항목 | 위치 | 판정 | 근거 |
|------|------|------|------|
| `CurrencyType.FREE` | ShopScreen.kt:61 | 의도적 | UI 분기 완성, 사용 아이템 없음 (향후 광고 보상용) |
| `ShopAction.StarterPack` | ShopScreen.kt:68 | 의도적 | 1회 제한 등 전용 로직 추가용 확장 포인트 |
| `TierCard` border 이중 적용 | ShopScreen.kt:488+499 | **실수** | `borderColor=NeonCyan` + `Modifier.border(2.dp, NeonCyan)` 동시 적용 |
| 시즌 티어 30 초과 미처리 | ShopScreen.kt:86-93 | 불확실 | 보상 30까지만, XP는 계속 누적 |

---

## 4. 설정(Settings)

### 파일
- `ui/screens/SettingsScreen.kt`
- `ui/viewmodel/SettingsViewModel.kt`

### 구조
```
AnimatedContent(currentPage)
├── MAIN: Column(verticalScroll) → GameCard 행 9개
├── AUDIO: 사운드/음악/진동 토글
├── GAMEPLAY: 데미지 숫자, 체력바 모드, 이펙트 품질, 자동 웨이브
├── UPGRADE: (숨김 처리 — 진입 불가)
├── DATA: 초기화 버튼 + 확인 다이얼로그
├── PROFILE: 칭호 관리
├── FAQ: 아코디언 15개 섹션
├── PRIVACY: 6개 조항
└── LICENSES: CREDITS.txt + 8 라이브러리 + 2 폰트
```

### 구현된 기능
- 오디오: 사운드/음악/진동 ON/OFF
- 게임플레이: 데미지 숫자, 체력바(3모드), 이펙트 품질(3단계), 자동 웨이브
- 데이터 초기화 (확인 다이얼로그, GameData+레시피 리셋)
- 주간보상: DailyLoginDialog 호출
- 업적: Routes.ACHIEVEMENTS로 네비게이션
- 프로필 칭호 관리
- FAQ 15섹션, 개인정보 처리방침, 오픈소스 라이선스

### 패턴
- Orbit MVI (`SettingsViewModel`)
- 로컬 `currentPage` 상태 (SettingsPage enum)
- `AnimatedContent`로 서브페이지 전환 (slide + fade)
- `BackHandler`로 서브페이지 → 메인 복귀

### 미구현
- 기본 배속 설정: 436행 주석만 (`// ── 기본 배속 ──`)
- `SettingsSideEffect.ShowToast`: 정의만, `postSideEffect` 호출 없음
- `SettingsSideEffect.DataReset`: 발행되지만 **Screen에서 수신 코드 없음** → 토스트/피드백 없이 조용히 초기화

### 죽은 코드 / 미적용 로직

| 항목 | 위치 | 판정 | 근거 |
|------|------|------|------|
| `SettingsPage.UPGRADE` 전체 | SettingsScreen.kt | 의도적 | `// 강화 탭 숨김 처리` 주석. 구 UnitFamily 기반 가족 강화 |
| `SettingsSideEffect` sealed class 전체 | SettingsViewModel.kt | **실수** | ShowToast 발행 0, DataReset 발행되나 수신 0 |
| `FAMILY_UPGRADE_*` 상수 | SettingsScreen.kt | 의도적 | UPGRADE 페이지 전용, 페이지 숨김 상태 |

---

## 5. 업적(Achievements)

### 파일
- `ui/screens/AchievementsScreen.kt`

### 구조
```
Column
├── ResourceHeader
├── 뒤로가기 + 타이틀 Row
├── ScrollableTabRow (8개 카테고리)
├── '모두 수령' 버튼 (조건부)
└── LazyColumn → AchievementItem (GameCard)
    ├── 상태 아이콘 (미달성/달성/수령완료)
    ├── 이름 + 설명 + NeonProgressBar
    └── 보상/수령 버튼
```

### 구현된 기능
- 8개 카테고리 (전투/합성/수집/경제/특수/유물/펫/던전), 총 26개 업적
- 진행도 실시간 추적 (`getProgress()` → GameData)
- 개별/일괄 보상 수령 (골드/다이아 + seasonXP 10)
- 3단계 시각 상태 (미달성/달성/수령완료)

### 패턴
- **ViewModel 없음** — `repository.gameData`를 직접 `collectAsState()`
- 보상 수령 로직이 onClick 람다에 직접 구현 (MVI 미적용)

### 미구현
- 업적 달성 시 팝업/알림 없음 (직접 화면 방문 필요)
- `wonWithSingleType` 필드: 실제 설정 로직 미확인

### 죽은 코드 / 미적용 로직

| 항목 | 위치 | 판정 | 근거 |
|------|------|------|------|
| `import GoldCoin, NeonRed, NeonRedDark, DiamondBlue` | AchievementsScreen.kt | **실수** | 파일 내 미사용 import |

---

## 6. 던전(Dungeon)

### 파일
- `ui/screens/DungeonScreen.kt`
- `engine/DungeonManager.kt`

### 구조
```
Column
├── 상단 바 (뒤로가기 + 타이틀)
├── 일일 입장 횟수 Row
└── LazyColumn → DungeonCard
    ├── 아이콘 + 이름 + 설명
    ├── StatChip 3개 (트로피/스태미나/웨이브)
    ├── 최고기록 + 남은횟수
    └── 입장 버튼 (4가지 상태 텍스트)
```

### 구현된 기능
- 5개 던전 카드 (GOLD_RUSH, RELIC_HUNT, PET_EXPEDITION, BOSS_RUSH, SURVIVAL)
- 트로피 기반 잠금/해제
- 일일 입장 횟수 표시
- 입장 조건 검증 (트로피 + 스태미나 + 일일 횟수)
- 스태미나/횟수 차감 후 전투 시작
- 최고 기록 표시

### 패턴
- **ViewModel 없음** — `repository.gameData`를 직접 `collectAsState()`
- `DungeonManager` 인스턴스화하여 로직 위임
- `onStartDungeonBattle` 콜백 (ComposeActivity에서 주입)

### 미구현
- 던전 전투 결과/보상 반영 (별도 BattleResult에서 처리 추정)
- 던전별 구체적 보상 표시 (description 문자열에만 의존)

### 죽은 코드 / 미적용 로직

| 항목 | 위치 | 판정 | 근거 |
|------|------|------|------|
| `import DeepDark` | DungeonScreen.kt | **실수** | BgScreen 별도 정의로 대체, import 미정리 |
| `DungeonManager` 중복 생성 | DungeonScreen.kt:166 | **실수** | Screen에서 이미 계산한 remaining을 Card에서 다시 생성 |

---

## 7. 배틀(Battle)

### 파일 (25개, ~10,800줄)
- `ui/battle/BattleScreen.kt` (1351)
- `ui/battle/BattleField.kt` (1325)
- `ui/battle/BattleHud.kt` (1883)
- `ui/battle/EnemyOverlay.kt` (706)
- `ui/battle/ProjectileOverlay.kt` (162)
- `ui/battle/SkillEffectOverlay.kt` (402)
- `ui/battle/RoguelikeBuffDialog.kt` (176)
- `ui/battle/RoguelikeBuffHud.kt` (125)
- `ui/battle/MonsterPathOverlay.kt` (562)
- `ui/battle/MergeEffectOverlay.kt` (562)
- `ui/battle/SummonEffectOverlay.kt` (866)
- `ui/battle/DamageNumberOverlay.kt` (102)
- `ui/battle/MeleeHitOverlay.kt` (130)
- `ui/battle/BattleParticleOverlay.kt` (292)
- `ui/battle/WaveAnnouncementOverlay.kt` (305)
- `ui/battle/DebugOverlay.kt` (101)
- `ui/battle/UnitDetailPopup.kt` (326)
- `ui/battle/UpgradeSheet.kt` (291)
- `ui/battle/BulkSellDialog.kt` (157)
- `ui/battle/BuyUnitSheet.kt` (304)
- `ui/battle/GambleDialog.kt` (410)
- `ui/battle/GradeConstants.kt` (14)
- `ui/battle/BitmapUtils.kt` (92)
- `ui/battle/SpriteSheetAnimator.kt` (114)
- `ui/battle/ParticleLOD.kt` (74)

### 구조 (7+ 레이어)
```
Box(fillMaxSize)
├── Layer 0: 배경 (스테이지별 PNG + gradient)
├── Layer 1: 게임 영역 (720:1280, clipToBounds)
│   ├── MonsterPathOverlay
│   ├── ZoneGroundOverlay
│   ├── EnemyOverlay (스프라이트 8x4, 상태머신)
│   ├── BattleField (유닛 렌더링, 그리드)
│   ├── ProjectileOverlay (10종 스프라이트)
│   ├── MeleeHitOverlay
│   ├── DamageNumberOverlay
│   ├── BattleParticleOverlay (LOD)
│   ├── WaveAnnouncementOverlay
│   ├── DebugOverlay
│   ├── GoldCoinOverlay
│   └── LevelUpOverlay
├── Layer 1.5: SkillEffectOverlay (70+ 스프라이트, LRU 캐시 20)
├── Layer 2: HUD (BattleTopHud + BattleBottomHud)
├── Layer 2.5: PetBattleOverlay
├── Layer 3: UnitDetailPopup + MergeEffect + SummonEffect
├── Layer 3.5: BulkSellDialog / GambleDialog / UpgradeSheet
├── Layer 4: RoguelikeBuffDialog
├── Layer 5: ResultScreen (승리=금색방사형, 패배=어두운페이드)
├── Layer 6: BattleMenuDialog (일시정지)
├── Layer 7: QuitBattleDialog
└── Layer 8: TutorialHintOverlay
```

### 구현된 기능 (30+)
- 유닛 소환 (꾹 누르면 연속소환 10ms)
- 유닛 이동/스왑 (move mode)
- 유닛 상세 팝업 (롱프레스)
- 합성 (수동 조합 버튼) + 레시피 조합
- 일괄판매 + 도박
- 등급 그룹 강화 (UpgradeSheet)
- 배속 x1/x2/x3 + 일시정지
- 적 스프라이트 애니메이션 (walk/idle/hit/die), HP바 3모드
- 적 디버프 시각효과 (Slow/DoT/Poison/ArmorBreak/Lightning/Wind/Stun/Freeze)
- 투사체 렌더링 (종족x데미지타입 10종)
- 스킬 이펙트 (70+ 스프라이트시트, LRU 캐시)
- 데미지 숫자, 근접 히트, 파티클 (LOD), 웨이브 발표
- 골드 코인 파티클, 레벨업 이펙트, 소환/합성 이펙트
- 존 지면 이펙트, 몬스터 경로
- 펫 표시 (둥실둥실 + 스킬)
- 로그라이크 강화 (카드 선택/리롤) + 버프 HUD
- 튜토리얼 힌트 (첫 배틀 가이드)
- 전투 포기 + 결과 화면 전환

### 패턴
- BattleBridge StateFlow (더블 버퍼링, AtomicLong 프레임 카운터)
- ConcurrentLinkedQueue<BattleCommand> 커맨드 큐
- Canvas GC-free (모든 Color/Paint/Brush pre-allocate)
- Smooth 보간 (`withFrameNanos` + lerp)
- LOD (ParticleLOD, 고배속 이펙트 생략)
- 스프라이트 시트 (SpriteSheetAnimator 8x4)
- LRU 캐시 (SkillEffect, max 20, ~14MB)
- 이벤트 버퍼 (ArrayDeque + lock)

### 미구현
- **BUFF_BIT_SILENCE 시각효과 누락**: 상수만 선언, EnemyOverlay에 렌더링 없음
- UnitCardStrip: BattleHud에 ~500줄 완전 구현, BattleScreen에서 **호출 없음**
- SSJ Aura 비활성화 (주석 처리)

### 죽은 코드 / 미적용 로직

| 항목 | 위치 | 판정 | 근거 |
|------|------|------|------|
| `*Bright` 색상 6종 | BattleField.kt:45-55 | 의도적 | 향후 이중 파티클용 pre-allocate |
| `*AttackFlash` 색상 6종 | BattleField.kt:59-64 | 의도적 | 공격 플래시 효과용 예약 |
| `AttackGlow` | BattleField.kt:41 | 의도적 | 글로우 효과용 예약 (스프라이트로 대체됨) |
| `CellHighlight`, `CellHighlightBright` | BattleField.kt:100-101 | 의도적 | MoveHighlight로 대체됨 |
| `VignetteColor` | BattleField.kt:93 | 의도적 | BattleScreen 보스 비네트로 이동됨 |
| `PedestalParticleAlpha` | BattleField.kt:90 | **실수** | inline 알파값 사용 중, 이 상수 미참조 |
| `WindLeafIdle03` | BattleField.kt:56 | 불확실 | 동적 alpha 계산으로 대체됨 |
| `UnitCardStrip` + 관련 함수 ~500줄 | BattleHud.kt:788-1300 | 의도적 | HUD 공간 부족으로 뺀 것으로 추정 |
| `BUFF_BIT_SILENCE` 시각효과 | BattleBridge:69 / EnemyOverlay | **실수** | 다른 디버프 7종은 모두 구현, Silence만 누락 |
| `import BossModifier` | BattleHud.kt:76 | **실수** | 미사용 import |

---

## 8. 네비게이션 & 공통 컴포넌트

### 네비게이션 구조
```
NavHost (startDestination = HOME)
├── HOME ← Bottom Bar 탭
├── COLLECTION ← Bottom Bar 탭
├── SHOP ← Bottom Bar 탭
├── SETTINGS ← Bottom Bar 탭
├── ACHIEVEMENTS ← Settings에서 진입
├── DUNGEON ← HomeScreen에서 진입
├── RESULT ← ⚠️ 도달 불가 (navigate 호출 없음)
├── PROFILE ← ⚠️ 도달 불가 (navigate 호출 없음)
└── UNIT_CODEX ← ⚠️ 도달 불가 (navigate 호출 없음)
```

- 탭 전환: `popUpTo(HOME) { saveState=true }` + `launchSingleTop` + `restoreState`
- 전환 애니메이션: fadeIn(300ms) + slideInHorizontally(1/4)
- Deep link: 없음
- 숨겨진 치트: Shop 탭 10초 내 10회 탭 → DEV MODE

### Activity 구조

| Activity | 역할 | 방향 | 특이사항 |
|----------|------|------|----------|
| ComposeActivity | 허브/메뉴 전체 | PORTRAIT 고정 | 스플래시(800ms) → 종족 선택 → 원형 와이프 전환 → 배틀 |
| MainActivity | 배틀 전용 | UNSPECIFIED | FLAG_KEEP_SCREEN_ON, immersive, 종료 시 보상 계산 + finish() |

### 공통 컴포넌트 상태

| 컴포넌트 | 파일 | 상태 |
|----------|------|------|
| NeonButton | NeonButton.kt | **활성** (핵심) |
| GameCard | GameCard.kt | **활성** (현행 표준) |
| NeonProgressBar | NeonProgressBar.kt | **활성** |
| ProfileBanner | ProfileBanner.kt | **활성** |
| ResourceHeader | ResourceHeader.kt | **활성** |
| ScreenHeader | ScreenHeader.kt | **활성** |
| StageCardPager | StageCard.kt | **활성** |
| PreBattleDialog | PreBattleDialog.kt | **활성** |
| RaceDraftDialog | RaceDraftDialog.kt | **활성** |
| GachaProbabilityDialog | GachaProbabilityDialog.kt | **활성** |
| DailyLoginDialog | DailyLoginDialog.kt | **활성** |
| GameBottomNavBar | BottomNavBar.kt | **활성** |
| UnitUiUtils | UnitUiUtils.kt | **활성** (일부 deprecated) |
| MedievalCard | MedievalCard.kt | **레거시** (사용처 0) |
| MedievalButton | MedievalButton.kt | **레거시** (사용처 0) |
| WoodFrame | WoodFrame.kt | **레거시** (사용처 0) |
| GameProgressBar | GameProgressBar.kt | **레거시** (사용처 0) |
| ResourceBar (5개 함수) | ResourceBar.kt | **미사용** (ProfileBanner로 대체) |
| DifficultyDialog | RankBadge.kt | **미사용** (PreBattleDialog로 대체) |

### 죽은 코드 / 미적용 로직

| 항목 | 위치 | 판정 | 근거 |
|------|------|------|------|
| Routes.RESULT (도달 불가) | NavGraph.kt:155-185 | 불확실 | composable 등록 + 파라미터 6개 정의, navigate 호출 0 |
| Routes.PROFILE (도달 불가) | NavGraph.kt:149-154 | 불확실 | composable 등록, 진입 버튼 없음 |
| Routes.UNIT_CODEX (도달 불가) | NavGraph.kt:136-141 | 불확실 | 레거시 UnitCollectionScreen 연결, BGM만 매핑 |
| MedievalCard/Button/WoodFrame | ui/components/ | 의도적 | "Legacy — kept for migration" 주석 |
| GameProgressBar | ui/components/ | 의도적 | "use NeonProgressBar" 주석 |
| ResourceBar.kt 전체 | ui/components/ | 불확실 | 레거시 주석 없으나 ProfileBanner가 대체 |
| DifficultyDialog | RankBadge.kt:78 | 불확실 | PreBattleDialog가 대체, 레거시 주석 없음 |
| `GradeBgAncient` | UnitUiUtils.kt:144 | **실수** | ANCIENT 등급 존재하지 않음, 과거 잔재 |
| `AppSideEffect` (빈 sealed class) | AppViewModel.kt:15 | 의도적 | Orbit 패턴 필수, 확장 대비 |

---

## 9. 종합 요약

### 심각도별 이슈 분류

#### 실수 (수정 필요) — 16건
| # | 화면 | 이슈 | 영향도 |
|---|------|------|--------|
| 1 | 도감 | DamageType 미사용 → 근거리=물리 하드코딩 | **높음** (표시 오류) |
| 2 | 배틀 | BUFF_BIT_SILENCE 시각효과 누락 | **중간** (Silence 디버프 안 보임) |
| 3 | 설정 | SettingsSideEffect 수신 코드 없음 (DataReset 피드백 없음) | **중간** (UX) |
| 4 | 상점 | TierCard border 이중 적용 | **낮음** (시각적) |
| 5 | 홈 | DungeonManager remember 없이 생성 | **낮음** (성능) |
| 6 | 홈 | PreBattleDialog 미사용 import 15개 | **없음** (코드 정리) |
| 7 | 홈 | import mutableStateOf 미정리 | **없음** |
| 8 | 홈 | StageCard.cardSize 미사용 | **없음** |
| 9 | 도감 | import DamageType/RACE_LABELS 미정리 | **없음** |
| 10 | 업적 | 미사용 import 4개 | **없음** |
| 11 | 던전 | import DeepDark 미정리 | **없음** |
| 12 | 던전 | DungeonManager 중복 생성 | **낮음** (성능) |
| 13 | 배틀 | PedestalParticleAlpha 미참조 | **없음** |
| 14 | 배틀 | import BossModifier 미사용 | **없음** |
| 15 | 네비 | GradeBgAncient (존재하지 않는 등급) | **없음** |
| 16 | 상점 | 초보자 패키지 무한 구매 | **중간** (밸런스) |

#### 의도적 비활성화 — 확인됨
- SettingsPage.UPGRADE (가족 강화) — 종족 시스템 전환으로 숨김
- UnitCollectionScreen.kt — CollectionScreen이 대체
- 레거시 컴포넌트 4종 — 마이그레이션 완료, 주석 달림
- ROLE_LABELS, FAMILY_ICONS — @Deprecated 마킹
- BattleField pre-allocated 색상 18종 — 향후 VFX용 예약
- UnitCardStrip — HUD 공간 부족으로 제외

#### 불확실 — 추가 확인 필요
- Routes.RESULT/PROFILE/UNIT_CODEX 도달 불가 (의도적 보류 vs 연결 누락)
- ResourceBar.kt 전체 미사용 (레거시 주석 없음)
- DifficultyDialog 미사용 (PreBattleDialog로 대체 추정)
- BEHAVIOR_LABELS 사용처 불명
- roleColor() 함수 사용처 불명

### 패턴 일관성

| 화면 | ViewModel | MVI 패턴 | SideEffect |
|------|-----------|----------|------------|
| 홈 | HomeViewModel | Orbit MVI | LaunchBattle, ShowToast |
| 도감 | CollectionViewModel | Orbit MVI | ShowToast |
| 상점 | ShopViewModel | Orbit MVI | ShowToast |
| 설정 | SettingsViewModel | Orbit MVI | ShowToast, DataReset (**수신 없음**) |
| 업적 | **없음** | 직접 collectAsState | **없음** |
| 던전 | **없음** | 직접 collectAsState | **없음** |
| 배틀 | **없음** (BattleBridge) | StateFlow 직접 | Command Queue |

> 업적/던전은 ViewModel 없이 repository를 직접 구독하며, 보상 로직이 onClick 람다에 인라인됨.
