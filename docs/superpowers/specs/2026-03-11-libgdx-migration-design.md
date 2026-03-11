# JayGame libGDX 전면 마이그레이션 설계

## 개요

JayGame의 C++ GameEngine + Compose 하이브리드 구조를 **libGDX 단일 구조**로 전환한다.
운빨존많겜(랜덤다이스)급 물리/이펙트/렌더링을 구현하는 것이 목표.

### 결정 사항

| 항목 | 결정 |
|------|------|
| 전환 범위 | 앱 전체 (Compose + C++ 모두 제거) |
| 코드 전략 | 기존 프로젝트에서 점진 교체, 데이터 레이어 재사용 |
| 이펙트 우선순위 | 파티클, 애니메이션, 물리 모두 동등 |
| 에셋 | AI 생성(Stable Diffusion) + 무료 에셋팩(itch.io) |
| UI 스타일 | 판타지 RPG (나무/가죽/금속 텍스처) |
| 화면 방향 | 세로(Portrait) 고정 |
| 아키텍처 | SpriteBatch + Scene2D (ECS 미사용) |

---

## 1. 프로젝트 구조

```
app/src/main/
├── java/com/example/jaygame/
│   ├── AndroidLauncher.kt          ← 유일한 Activity
│   ├── JayGame.kt                  ← Game 클래스 (Screen 관리)
│   │
│   ├── data/                       ← 기존 그대로 재사용
│   │   ├── GameData.kt
│   │   ├── GameRepository.kt
│   │   ├── UnitDefs.kt
│   │   └── StageData.kt
│   │
│   ├── screen/                     ← libGDX Screen 구현들
│   │   ├── HomeScreen.kt
│   │   ├── BattleScreen.kt
│   │   ├── DeckScreen.kt
│   │   ├── ShopScreen.kt
│   │   ├── CollectionScreen.kt
│   │   ├── SettingsScreen.kt
│   │   ├── AchievementScreen.kt
│   │   └── ResultScreen.kt
│   │
│   ├── battle/                     ← 배틀 전용 시스템
│   │   ├── BattleWorld.kt          ← 게임 로직 (유닛/적/웨이브/SP)
│   │   ├── UnitEntity.kt
│   │   ├── EnemyEntity.kt
│   │   ├── ProjectileEntity.kt
│   │   ├── ParticleManager.kt      ← 파티클 이펙트 시스템
│   │   ├── PhysicsWorld.kt         ← Box2D 래퍼
│   │   └── BattleRenderer.kt       ← SpriteBatch 렌더링
│   │
│   ├── ui/                         ← Scene2D UI 컴포넌트
│   │   ├── FantasySkin.kt          ← 커스텀 스킨
│   │   ├── HudStage.kt            ← 배틀 HUD
│   │   └── Widgets.kt             ← 재사용 위젯
│   │
│   └── asset/                      ← 에셋 관리
│       ├── Assets.kt               ← AssetManager 래퍼
│       └── Atlas.kt                ← TextureAtlas 정의
│
├── assets/                         ← libGDX 에셋 폴더
│   ├── sprites/                    ← 유닛/적/투사체 스프라이트시트
│   ├── particles/                  ← 파티클 이펙트 파일 (.p)
│   ├── ui/                         ← UI 스킨 텍스처
│   ├── fonts/                      ← 비트맵 폰트
│   └── sounds/                     ← 효과음/BGM
│
└── res/                            ← 앱 아이콘 등 최소한만 유지
```

### 삭제 대상
- `app/src/main/cpp/` — C++ 엔진 전체
- `ComposeActivity.kt`, `MainActivity.kt` — 기존 Activity
- `bridge/BattleBridge.kt` — JNI 브릿지
- `ui/battle/` — Compose 배틀 UI 전체
- `ui/screens/` — Compose 화면 전체
- `ui/components/` — Compose 공용 컴포넌트
- `ui/theme/` — Compose 테마
- `navigation/NavGraph.kt` — Compose 네비게이션

### 재사용 대상
- `data/GameData.kt` — 게임 상태 데이터 클래스
- `data/GameRepository.kt` — SharedPreferences 저장 (libGDX에서도 사용 가능)
- `data/UnitDefs.kt` — 유닛 35종 정의 (grade/family/stats/abilities)
- `data/StageData.kt` — 스테이지 6종 정의
- `data/CardUtils.kt`, `data/StaminaManager.kt`

---

## 2. Screen 흐름 & 배틀 엔진

### Screen 전환
```
AndroidLauncher
  └→ JayGame (Game)
       ├→ HomeScreen (메인 로비)
       │    ├→ DeckScreen (덱 편집)
       │    ├→ ShopScreen (상점)
       │    ├→ CollectionScreen (도감)
       │    ├→ SettingsScreen (설정)
       │    └→ AchievementScreen (업적)
       │
       ├→ BattleScreen (전투)
       │    └→ ResultScreen (결과)
       │         └→ HomeScreen (돌아가기)
       │
       └→ Screen 전환: JayGame.setScreen()
```

### 배틀 렌더링 레이어 (아래→위)
```
Layer 1: TileMap                    ← 배경 타일맵 + 몬스터 경로
Layer 2: BattleRenderer (SpriteBatch) ← 유닛, 적, 투사체 스프라이트
Layer 3: ParticleManager            ← 불꽃, 얼음, 독연기, 사망파편, 소환연출
Layer 4: HudStage (Scene2D)         ← WAVE, SP, 소환/도박/구매/강화 버튼
```

### BattleWorld (게임 로직)
```kotlin
class BattleWorld {
    val units: Array<UnitEntity>           // 최대 30
    val enemies: Pool<EnemyEntity>         // 최대 100
    val projectiles: Pool<ProjectileEntity>
    val physics: PhysicsWorld              // Box2D

    // 상태
    var sp: Float
    var currentWave: Int
    var waveTimer: Float

    fun update(dt: Float) {
        waveManager.update(dt)       // 적 스폰
        units.forEach { it.update(dt) }  // 유닛 AI: 탐색→추적→공격
        enemies.forEach { it.update(dt) } // 적 경로 이동
        projectiles.forEach { it.update(dt) } // 투사체 이동
        physics.step(dt)             // Box2D 물리 (넉백, 파편)
        checkCollisions()            // 충돌 판정
        sp += spRegenRate * dt       // SP 리젠
    }
}
```

### PhysicsWorld (Box2D)
- 적 피격 시 **넉백**: impulse 적용, 경로로 복귀
- 사망 시 **파편**: 여러 작은 body 생성 → 중력/마찰로 자연 감속
- 투사체 **궤적**: 화염 포물선, 번개 지그재그
- 보스 등장/사망 시 **화면 흔들림**: 카메라 offset 진동

### ParticleManager (libGDX ParticleEffect)
| 이펙트 | 설명 |
|--------|------|
| fire.p | 주황+빨강 파티클, 위로 올라가며 소멸 |
| frost.p | 하늘색 결정, 바닥에 서리 스프레드 |
| poison.p | 초록 연기, 천천히 퍼지며 투명해짐 |
| lightning.p | 밝은 노랑 스파크, 체인 연결선 |
| death_burst.p | 적 색상 파편 burst |
| summon_beam.p | 바닥에서 빛기둥 올라옴 |
| merge_sparkle.p | 별가루 폭발 + 등급색 글로우 |
| hit_flash.p | 흰색 플래시 + 넉백 |

---

## 3. UI 시스템 (판타지 RPG 스타일)

### Scene2D 커스텀 스킨 에셋
```
assets/ui/
├── panel_wood.9.png          ← 나무 패널 (9-patch)
├── panel_leather.9.png       ← 가죽 스크롤 패널
├── button_gold.9.png         ← 금테 버튼 (normal)
├── button_gold_pressed.9.png ← 금테 버튼 (눌림)
├── button_red.9.png          ← 빨간 버튼 (위험 액션)
├── slot_stone.9.png          ← 돌 테두리 슬롯
├── bar_hp.png                ← HP바 채움
├── bar_sp.png                ← SP바 채움
├── bar_bg.png                ← 바 배경
├── tab_active.9.png          ← 탭 활성
├── tab_inactive.9.png        ← 탭 비활성
├── icon_gold.png             ← 골드 아이콘
├── icon_diamond.png          ← 다이아 아이콘
├── icon_trophy.png           ← 트로피 아이콘
└── font_fantasy.fnt          ← 판타지 비트맵 폰트
```

### HomeScreen 레이아웃
```
┌─────────────────────────┐
│  [골드] [다이아] [트로피] │  ← 상단 리소스 바 (가죽 패널)
│                         │
│    ┌───────────────┐    │
│    │  스테이지 선택  │    │  ← 나무 패널, 카드 가로 스크롤
│    └───────────────┘    │
│                         │
│    ┌───────────────┐    │
│    │  내 덱 미리보기  │    │  ← 유닛 5개 아이콘
│    └───────────────┘    │
│                         │
│   [ ⚔ 출전 ]           │  ← 큰 금테 버튼
│                         │
│ [덱] [도감] [상점] [설정] │  ← 하단 탭 (돌 텍스처)
└─────────────────────────┘
```

### BattleScreen HUD
```
┌─────────────────────────┐
│ [☰] WAVE 9  00:16  💀6  │  ← 가죽 패널
│         HP ████████      │
├─────────────────────────┤
│                         │
│      (게임 영역)         │
│                         │
├─────────────────────────┤
│    💎 SP ████████ 350    │
│  [구매]  [⚔소환]  [도박] │  ← 금테 버튼
│        [강화]            │
└─────────────────────────┘
```

### 에셋 생성 방법
- **9-patch 텍스처**: AI(Stable Diffusion)로 나무/가죽/돌 텍스처 생성 → 9-patch로 가공
- **아이콘**: 무료 에셋팩(itch.io) + AI 생성
- **폰트**: 무료 판타지 폰트 → BMFont/Hiero로 비트맵 변환
- **파티클**: libGDX Particle Editor로 직접 제작

---

## 4. 에셋 파이프라인

### 스프라이트 에셋
```
assets/sprites/
├── units.atlas + units.png       ← 유닛 35종 × (idle0/idle1/attack0/attack1/skill)
├── enemies.atlas + enemies.png   ← 적 6종 × (walk0/walk1/hit/death0/death1)
├── projectiles.atlas + .png      ← 투사체 5종
└── tiles.atlas + .png            ← 배경 타일
```

### 에셋 생성 워크플로우
1. AI/에셋팩에서 개별 스프라이트 PNG 수집
2. libGDX TexturePacker로 아틀라스 생성
3. libGDX Particle Editor로 파티클 이펙트 제작
4. BMFont/Hiero로 비트맵 폰트 생성

### AssetManager 로딩
```kotlin
object Assets {
    lateinit var manager: AssetManager

    fun load() {
        manager.load("sprites/units.atlas", TextureAtlas::class.java)
        manager.load("sprites/enemies.atlas", TextureAtlas::class.java)
        manager.load("ui/fantasy.atlas", TextureAtlas::class.java)
        // 파티클, 폰트 등
        manager.finishLoading()
    }
}
```

---

## 5. 마이그레이션 순서 (7 Phase)

### Phase 1: libGDX 기반 세팅
- build.gradle에 libGDX + Box2D 의존성 추가
- AndroidLauncher + JayGame 클래스 생성
- 빈 HomeScreen 띄우기 확인
- **완료 조건**: libGDX 빈 화면 표시

### Phase 2: 배틀 코어 (C++ 로직 → Kotlin)
- BattleWorld: 유닛/적/투사체/SP/웨이브 로직 (C++에서 포팅)
- BattleRenderer: SpriteBatch로 임시 사각형/원 렌더링
- UnitEntity, EnemyEntity, ProjectileEntity
- 기본 게임 루프 동작 확인
- **완료 조건**: 소환→적공격→웨이브클리어 동작

### Phase 3: Box2D 물리
- PhysicsWorld 연결
- 넉백 (적 피격 시 impulse)
- 파편 흩어짐 (사망 시 body 생성)
- 화면 흔들림 (카메라 shake)
- 투사체 궤적 (포물선, 지그재그)
- **완료 조건**: 피격 넉백, 사망 파편, 화면 흔들림 동작

### Phase 4: 파티클 & 이펙트
- ParticleManager + 8종 파티클 이펙트 제작
- 피격 플래시 (셰이더: 전체 흰색 0.1초)
- 사망 파편 burst
- 소환 빛기둥
- 합성 별가루
- 계열별 오오라/이펙트 (불꽃, 얼음결정, 독연기, 번개스파크, 버프링)
- **완료 조건**: 모든 이펙트 시각적으로 동작

### Phase 5: 스프라이트 에셋
- AI 생성 + 무료 에셋팩으로 유닛/적 스프라이트 제작
- TexturePacker로 아틀라스 생성
- 애니메이션 프레임 적용 (idle, attack, walk, death)
- 배경 타일맵 제작
- **완료 조건**: 사각형 대신 실제 스프라이트 렌더링

### Phase 6: UI 전환 (Compose → Scene2D)
- FantasySkin 제작 (나무/가죽/금속 텍스처)
- HomeScreen, DeckScreen, ShopScreen, CollectionScreen, SettingsScreen, AchievementScreen
- BattleHud (소환/도박/구매/강화)
- 팝업/다이얼로그 (유닛 정보, 구매 확인, 도박 결과 등)
- ResultScreen
- **완료 조건**: Compose 없이 전체 UI 동작

### Phase 7: 정리 & 삭제
- C++ 코드 전체 삭제 (cpp/)
- Compose 코드 전체 삭제
- BattleBridge, ComposeActivity, MainActivity 삭제
- CMakeLists.txt, games-activity 의존성 제거
- AndroidManifest 정리 (Activity 하나만)
- **완료 조건**: C++/Compose 코드 0, 앱 정상 동작

---

## 6. 기술 스택

| 영역 | 기술 |
|------|------|
| 게임 프레임워크 | libGDX 1.12+ |
| 물리 | Box2D (libGDX 내장) |
| 렌더링 | SpriteBatch + TextureAtlas |
| 파티클 | libGDX ParticleEffect |
| UI | Scene2D + 커스텀 Skin |
| 언어 | Kotlin |
| 저장 | SharedPreferences (기존 GameRepository) |
| 에셋 도구 | TexturePacker, Particle Editor, BMFont |
| 빌드 | Gradle (기존 Android 프로젝트) |
