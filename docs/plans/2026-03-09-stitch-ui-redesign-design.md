# Stitch UI Redesign Design

## Date: 2026-03-09

## Goal
Stitch 참조 디자인에 맞게 홈 화면(Compose)과 배틀 화면(C++ + Compose 오버레이)을 전면 리디자인한다.

## 결정 사항
- 스테이지 시스템 추가 (6개 스테이지, 트로피 해금)
- 스태미나 시스템 추가 (30 max, 5분에 1 회복, 전투 시 소모)
- 배틀 하단 탭은 Compose 오버레이로 구현 (C++ 배틀 위에)
- 난이도 선택 기능 추가 + 기존 업적/시즌패스/설정 유지
- 유닛을 원형 아이콘으로 변경
- 랭크 뱃지 표시 추가

---

## Part 1: 데이터 모델 확장

### GameData.kt 추가 필드
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

### 새 파일: StageData.kt
```kotlin
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

### 스태미나 회복 로직
- `StaminaManager` 유틸: `calculateCurrentStamina(lastRegenTime, currentStamina, maxStamina)` → 경과 시간 기반 계산
- 5분(300초) 당 1 회복
- GameRepository에서 resume 시 자동 갱신

---

## Part 2: 홈 화면 리디자인 (Compose)

### 레이아웃
```
┌──────────────────────────────────────┐
│ [아바타] 닉네임 Lv.N │ 💎 🪙 ⚡stamina │  ← ProfileHeader
├──────────────────────────────────────┤
│                                      │
│     ┌──────────────────────┐         │
│     │ BEST                 │         │
│     │ 초원                 │         │  ← StageCard (swipeable)
│     │ ROUND 40             │         │
│     │ 난이도: 보통          │         │
│     └──────────────────────┘         │
│     ◀  ● ● ● ● ● ●  ▶              │  ← 스테이지 인디케이터
│                                      │
│  [주간보상] [난이도] [업적] [시즌패스]  │  ← QuickButtons
│                                      │
│     ┌──────────────────────┐         │
│     │   ⚡5  전투 시작       │         │  ← StartBattleButton
│     └──────────────────────┘         │
│                                      │
│  [RANK 브론즈]              [설정]   │  ← RankBadge + Settings
├──────────────────────────────────────┤
│  전투 │ 덱 │ 컬렉션 │ 상점          │  ← 기존 BottomNavBar
└──────────────────────────────────────┘
```

### 새 컴포넌트
- `ProfileHeader` — 아바타(원형 플레이스홀더) + 닉네임 + 레벨 + 재화 3종(보석/골드/스태미나)
- `StageCard` — 카드형 UI, 스테이지명, BEST 웨이브, 난이도 표시. HorizontalPager로 좌우 스와이프
- `RankBadge` — 트로피 기반 랭크 아이콘+텍스트 뱃지
- `DifficultyDialog` — 쉬움/보통/어려움 선택 다이얼로그
- `StaminaDisplay` — 현재/최대 + 다음 회복까지 타이머

### 변경 컴포넌트
- `FullHeader` → `ProfileHeader`로 교체
- `HomeScreen` — 전체 레이아웃 재구성

---

## Part 3: 배틀 화면 C++ 수정

### HUD 레이아웃 변경 (Stitch 디자인 기반)
```
┌──────────────────────────────────────┐
│ ❤18  │  Round 30/40  │  00:18  │ SP ██ 46/100 │  ← 상단 HUD (C++)
├──────────────────────────────────────┤
│                                      │
│   ┌─────────────────┐ ┌─────────┐   │
│   │                 │ │ 경로    │   │
│   │  유닛 배치 그리드 │ │         │   │  ← 그리드 + 경로 (C++)
│   │  (원형 아이콘)    │ │         │   │
│   │                 │ │         │   │
│   └─────────────────┘ └─────────┘   │
│                                      │
│  유닛 10/20  [유닛 아이콘들]          │  ← 유닛 카운트 (C++)
├──────────────────────────────────────┤
│  [유닛 인벤토리 바] 🪙724            │  ← Compose 오버레이
│  ┌──────────────────────────────┐   │
│  │    ⚡ 소환 SUMMON  SP 10      │   │  ← Compose 오버레이
│  └──────────────────────────────┘   │
│  에테르│미션│롤러럭키│스크롤│강화    │  ← Compose 하단탭 오버레이
└──────────────────────────────────────┘
```

### C++ 변경
1. **유닛 렌더링**: 사각형 스프라이트 → 원형 아이콘 (컬러 원 + 중앙 심볼)
2. **상단 HUD**: HP + Round + 타이머 + SP 바 (가로 일직선 배치)
3. **타이머 추가**: 웨이브 경과 시간 표시
4. **HUD 하단부 제거**: 소환 버튼, SP 표시 → Compose 오버레이로 이동
5. **C++ 뷰포트 축소**: 하단 ~150dp를 Compose에 양보 (C++ 렌더 영역을 줄임)

### 원형 유닛 아이콘 렌더링
- 아틀라스에 원형 베이스 스프라이트 추가 (또는 프로그래밍으로 원 그리기)
- 유닛 타입별 컬러: 기존 UnitDef의 색상 사용
- 중앙에 유닛 심볼 (아틀라스의 작은 아이콘)
- 별 레벨 표시는 유닛 하단에

---

## Part 4: Compose 배틀 오버레이

### 하이브리드 구조
현재 `MainActivity`는 `GameActivity`(순수 C++)인데, Compose 오버레이를 추가하려면:

**접근:** `MainActivity`를 확장하여 Compose 뷰를 C++ SurfaceView 위에 오버레이
- `GameActivity`는 `SurfaceView`를 사용 → 그 위에 `ComposeView`를 `addContentView`로 추가
- C++ → Kotlin 이벤트: JNI 콜백으로 골드/SP/유닛 수 등 전달
- Kotlin → C++: JNI 호출로 소환/스킬 등 명령 전달

### 오버레이 컴포넌트
- `BattleOverlay` — 전체 오버레이 컨테이너 (하단 고정)
- `UnitInventoryBar` — 현재 소환 가능한 유닛 목록 + 골드 표시
- `SummonButton` — 큰 소환 버튼 + SP 비용
- `BattleTabBar` — 5탭 (에테르/미션/롤러럭키/스크롤/강화)
  - 현재는 UI만 배치, "준비 중" 처리
  - 강화 탭만 기능 연동 (유닛 머지 강화)

### JNI Bridge
```cpp
// C++ → Kotlin (콜백)
void notifyStateUpdate(JNIEnv* env, int gold, int sp, int unitCount, int maxUnits);

// Kotlin → C++ (명령)
extern "C" JNIEXPORT void JNICALL
Java_com_example_jaygame_MainActivity_nativeSummon(JNIEnv* env, jobject thiz);
```

---

## Part 5: 스테이지 → C++ 연동

### SharedPreferences 브릿지 확장
현재 PlayerData가 SharedPreferences에서 deck을 읽는 것처럼:
- `stageId`, `difficulty`, `maxWaves`를 SharedPreferences에 저장
- C++ PlayerData에서 읽어서 BattleScene에 전달
- BattleScene의 `MAX_WAVES`를 스테이지별 값으로 변경

### 전투 종료 후
- ResultScene에서 스테이지 best wave 업데이트
- 스태미나는 전투 시작 시 차감 (Compose에서 처리)

---

## 구현 순서 (점진적)
1. 데이터 모델 확장 (GameData, StageData, StaminaManager)
2. 홈 화면 리디자인 (ProfileHeader, StageCard, RankBadge 등)
3. C++ 배틀 HUD 수정 (상단 HUD 재배치, 하단 제거)
4. C++ 유닛 원형 아이콘 렌더링
5. MainActivity에 Compose 오버레이 추가 (JNI bridge)
6. 배틀 오버레이 UI (SummonButton, UnitInventoryBar, BattleTabBar)
7. 스테이지 → C++ 연동 (SharedPreferences bridge)
