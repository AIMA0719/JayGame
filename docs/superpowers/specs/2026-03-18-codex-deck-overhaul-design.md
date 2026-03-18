# 도감 & 소환/시너지 시스템 개편 설계

**날짜**: 2026-03-18
**상태**: 리뷰 완료 (v2)

---

## 1. 개요

현재 도감과 덱 시스템이 새 유닛 분류 체계(역할, 공격거리, 데미지타입, 등급)를 반영하지 못하고 있다.
덱 시스템을 폐지하고, 도감을 BlueprintRegistry 기반으로 전면 개편하며,
시너지를 필드 배치 기준 실시간 발동으로 전환한다.

### 현재 유닛 구성

| 분류 | 내용 |
|------|------|
| 총 유닛 | 일반 75종 + 히든 18종 + 특수 10종 = 103종 |
| 역할 | 탱커(15), 근딜(26), 원딜(23), 서포터(16), 컨트롤러(13) |
| 속성 | 화염, 냉기, 독, 번개, 보조, 바람 (6종) |
| 등급 | 일반→희귀→영웅→전설→고대→신화→불멸 (7단계) |
| 데미지 타입 | 물리(46) / 마법(47) |
| 공격 거리 | 근접(41) / 원거리(52) |

> **용어 규칙**: 코드에서는 `UnitFamily`, UI에서는 "속성"으로 표기한다.

### 핵심 변경 사항

1. **덱 시스템 폐지** — 전체 풀 랜덤 소환
2. **시너지 시스템** — 필드 배치 기준 실시간 발동 (역할 + 속성)
3. **도감 개편** — BlueprintRegistry 기반, 다축 필터, 히든/특수 탭 구현
4. **데이터 레이어 전환** — 레거시 UNIT_DEFS → BlueprintRegistry

---

## 2. 덱 시스템 폐지 → 전체 풀 랜덤 소환

### 변경 내용

- `DeckScreen.kt` 제거
- 네비게이션에서 덱 탭 제거 → **4탭 구조** (도감, 홈, 상점, 설정)
  - 기존 DECK 탭 위치에 도감(COLLECTION) 승격 또는 4탭으로 축소
  - `NavTab` enum에서 `DECK` 제거, `Routes.DECK` 제거
- `GameData.deck` 필드 deprecated → 역직렬화 시 무시, 직렬화 시 생략

### 소환 로직

`BattleEngine.requestSummonBlueprint()`가 이미 등급 가중치 기반 소환을 구현하고 있음. 변경 사항:

1. **`BattleEngine` 생성자에서 `deck: IntArray` 파라미터 제거**
2. **`requestSummon()` 레거시 fallback 제거** — `requestSummonBlueprint()` 단일 경로로 통일
3. `AbilitySystem.activeSynergy` 초기화: 덱 기반 → 필드 배치 이벤트 기반으로 변경

등급별 소환 가중치 (기존 `UnitGrade.summonWeight`와 동일):

| 등급 | 가중치 |
|------|--------|
| 일반 (COMMON) | 60 |
| 희귀 (RARE) | 25 |
| 영웅 (HERO) | 12 |
| 전설 (LEGEND) | 3 |
| 고대+ | 소환 불가 (합성 전용) |

- 역할/속성 필터 없음 — 순수 랜덤
- 전투 전략의 핵심 = **합성 + 배치**

---

## 3. 시너지 시스템 — 필드 기준 실시간 발동

### 개념

덱 선택이 아닌, **현재 필드에 배치된 유닛 구성**에 따라 시너지가 자동 발동/해제된다.
역할 시너지와 속성 시너지 두 축이 동시에 적용된다.

### 역할 시너지 (기존 RoleSynergySystem — 게임루프 연결 필요)

`RoleSynergySystem.getBonus()`는 이미 `List<GameUnit>` 기반 필드 계산을 지원하나,
**현재 게임 루프에서 호출되지 않고 있음**. 실제 연결 작업 필요:

- `BattleEngine`의 유닛 공격/스탯 계산 시 `RoleSynergySystem.getBonus()` 호출
- 반환된 `RoleSynergyBonus`의 multiplier를 해당 역할 유닛 스탯에 적용
- 유닛 배치/제거 시 캐시 갱신 (매 프레임 재계산 X, 배치 변경 시에만)

필드 위 같은 역할 유닛 수에 따라 보너스:

| 역할 | 2+ | 3+ | 4+ (특수효과) |
|------|----|----|---------------|
| 탱커 | 블록시간 +20% | +블록수 1 | 도발 |
| 근딜 | 대시피해 +15% | +쿨감 20% | 즉시 재대시 |
| 원딜 | 사거리 +10% | +치명타 5% | 관통 2체 |
| 서포터 | 버프범위 +15% | 버프 중첩 | 전체 미니힐 |
| 컨트롤러 | CC확률 +10% | +CC지속 25% | 면역 시 50% 적용 |

### 속성 시너지 (기존 SynergySystem 활용 — 덱 오버로드 제거)

`SynergySystem.kt`에 이미 필드 기반 메서드가 존재:
- `getSynergyBonus(units: List<GameUnit>, family: UnitFamily)`
- `getActiveSynergies(units: List<GameUnit>)`
- `countFamilies(units: List<GameUnit>)`

변경 사항:
- 덱 기반 오버로드 `getSynergyBonus(deck: IntArray, ...)` 제거
- 필드 기반 메서드만 유지
- **`FamilySynergySystem.kt` 신규 생성 불필요**

| 속성 | 2+ | 3+ |
|------|----|----|
| 화염 | 공격력 +4% | 공격력 +8%, DoT 연장 |
| 냉기 | 공속 +3% | 공속 +6%, 둔화 강화 |
| 독 | 공격력 +4% | 공격력 +7%, 독 전파 |
| 번개 | 공속 +4% | 공속 +8%, 체인 +1 |
| 보조 | 사거리 +3% | 사거리 +6%, 힐 강화 |
| 바람 | 공격력 +3% | 공격력 +5%, 넉백 강화 |

### 전투 UI — 활성 시너지 패널

- 전투 화면에 현재 활성된 역할/속성 시너지 표시
- 유닛 배치/제거 시 실시간 갱신
- 역할 시너지와 속성 시너지 구분하여 표시

### 구현 위치

- `SynergySystem.kt` (기존) — 덱 기반 오버로드 제거, 필드 기반 메서드 유지
- `RoleSynergySystem.kt` (기존) — `BattleEngine` 게임루프에 연결
- `BattleEngine.kt` — 시너지 보너스 적용 로직 추가, `deck` 파라미터 제거
- `AbilitySystem.kt` — `activeSynergy` 초기화를 필드 이벤트 기반으로 변경
- `BattleField.kt` / 전투 오버레이 — 시너지 패널 UI

---

## 4. 도감 개편 — BlueprintRegistry 기반 + 다축 필터

### 4.0 전제: BlueprintRegistry/RecipeSystem UI 접근성

현재 `BlueprintRegistry`와 `RecipeSystem`은 `BattleEngine` 내부에서만 생성/접근 가능.
도감 화면(전투 외부)에서 접근하려면 DI 방안 필요:

- **방안: 앱 레벨 싱글턴** — `Application.onCreate()`에서 `BlueprintRegistry`를 로드하여
  Hilt `@Singleton`으로 제공. `RecipeSystem`도 동일하게 제공.
- `BattleEngine`은 주입받은 싱글턴을 사용하도록 변경
- 도감 화면 ViewModel에서 `@Inject`로 접근

### 4.0.1 special_units.json 로딩

현재 `special_units.json`은 assets에 존재하지만 `BlueprintRegistry`에 로딩되지 않음.

- **`BlueprintRegistry.loadFromJson()`을 2회 호출** (blueprints.json + special_units.json)
- 또는 `loadFromMultipleJson()` 메서드 추가
- Special 탭 구현 전에 반드시 해결해야 하는 선행 조건

### 4.1 데이터 소스 전환

- 레거시 `UNIT_DEFS` (42종, `UnitDef`) → `BlueprintRegistry` (75종 NORMAL + 18종 HIDDEN, `UnitBlueprint`)
- `special_units.json` (10종 SPECIAL) → `BlueprintRegistry`에 추가 로딩
- `UnitCollectionScreen`, `CollectionScreen`의 HeroCollectionTab 모두 전환
- `GameData.units` 인덱스 방식: `Int` 기반 → `String`(blueprintId) 기반 `Map<String, UnitProgress>`

### 4.2 탭 구조

| 탭 | 내용 |
|----|------|
| 일반 | 75종 — 필터/정렬 지원 |
| 히든 | 18종 — 발견/미발견 표시, 조합 힌트 |
| 특수 | 10종 — 필드 효과 강조 |

### 4.3 필터 바 (일반 탭)

가로 스크롤 칩 바, 다중 선택 가능:

- **역할**: 전체 / 탱커 / 근딜 / 원딜 / 서포터 / 컨트롤러
- **속성**: 전체 / 화염 / 냉기 / 독 / 번개 / 보조 / 바람
- **등급**: 전체 / 일반 ~ 불멸
- **정렬**: 등급순 / 공격력순 / 이름순

### 4.4 유닛 카드 개선

현재 카드에 표시되는 정보:
- 아이콘, 이름, 등급

추가할 정보:
- **역할 아이콘** (탱커 방패, 근딜 검, 원딜 활, 서포터 십자, 컨트롤러 사슬)
- **데미지 타입** 표시 (물리: 주황, 마법: 보라)
- **공격 거리** 표시 (근접/원거리 아이콘)

### 4.5 상세 다이얼로그 개선

현재 표시 정보:
- 공격력, 공속, 사거리, 속성, 능력

추가할 정보:
- **역할** 뱃지 (색상 + 라벨)
- **데미지 타입** (물리/마법)
- **체력, 방어력, 마법저항, 이동속도, 블록 수** (UnitBlueprint.stats에서)
- **행동 패턴** 설명 (behaviorId 기반)

### 4.6 히든 탭 구현

- `BlueprintRegistry.findByCategory(UnitCategory.HIDDEN)` 연결
- 미발견: 실루엣 카드 + "???" + "미발견"
- 발견: 풀 유닛 카드 (일반 탭과 동일 스타일)
- 조합 레시피 힌트: "화염 근딜(영웅+) + 번개 근딜(영웅+)" 형태로 재료 조건 표시
- `RecipeSystem.isDiscovered(recipeId)` 연동
- 히든 탭에서 `RecipeSystem.allRecipes()`를 순회, 각 recipe.resultId로 `BlueprintRegistry.findById()` 호출하여 카드 렌더링

### 4.7 특수 탭 구현

- `BlueprintRegistry.findByCategory(UnitCategory.SPECIAL)` 연결
  - **전제**: Section 4.0.1의 special_units.json 로딩이 완료되어야 함
- 10종 특수 유닛 풀 카드
- **필드 효과 설명** 강조 표시 (description에 포함된 전장 효과)
- "최대 2기 배치 가능" 안내 유지
- 다중 속성 뱃지 (예: 용왕 = 화염 + 바람)

---

## 5. 세이브 데이터 마이그레이션

### 문제

기존 `GameData.units`는 `List<UnitProgress>` (42개 요소, Int 인덱스 기반).
새 시스템은 `Map<String, UnitProgress>` (blueprintId String 키 기반).
변환 없이 전환하면 **기존 플레이어 진행도가 소실**된다.

### 마이그레이션 전략

1. `saveVersion`을 2 → 3으로 증가
2. 역직렬화 시 `saveVersion < 3`이면 마이그레이션 실행:
   - 레거시 Int 인덱스 → blueprintId 매핑 테이블 사용
   - 매핑 테이블: `UNIT_DEFS[i].id` → 대응되는 `blueprintId`
3. 마이그레이션 테이블 예시:
   ```
   0 (루비, FIRE COMMON) → "fire_rdps_01"
   1 (미스트, FROST COMMON) → "frost_rdps_01"
   5 (카르마, FIRE RARE) → "fire_rdps_02"
   ...
   ```
4. 매핑 테이블은 `LegacyMigration.kt`에 하드코딩
5. 마이그레이션 후 새 포맷으로 저장, `saveVersion = 3`으로 갱신
6. `deck` 필드는 역직렬화 시 무시

### 호환성

- 새 버전 앱이 구 세이브를 읽으면 자동 변환
- 구 버전 앱이 새 세이브를 읽으면 빈 진행도로 시작 (하위 호환 포기)

---

## 6. 영향 범위 — 변경 대상 파일

### 제거
- `DeckScreen.kt` — 덱 편집 화면 전체

### 대규모 수정
- `UnitCollectionScreen.kt` — BlueprintRegistry 기반 + 필터 바 + 히든/특수 탭 구현
- `CollectionScreen.kt` (HeroCollectionTab) — BlueprintRegistry 기반 + 필터 + 카드 개선
- `GameData.kt` — `deck` 필드 제거, `units` 필드를 `Map<String, UnitProgress>`로 변경
- `BattleEngine.kt` — `deck` 파라미터 제거, 시너지 보너스 적용, 레거시 소환 fallback 제거
- `AbilitySystem.kt` — `activeSynergy` 초기화를 필드 이벤트 기반으로 변경
- `BlueprintRegistry.kt` — `special_units.json` 추가 로딩, 앱 레벨 싱글턴화

### 신규
- `LegacyMigration.kt` — Int 인덱스 → blueprintId 매핑 테이블 + 마이그레이션 로직
- 전투 UI 시너지 패널 컴포넌트

### 소규모 수정
- `SynergySystem.kt` — 덱 기반 오버로드 제거, 필드 기반만 유지
- `RoleSynergySystem.kt` — BattleEngine 게임루프에 연결
- 네비게이션 (`NavTab`, `NavGraph`) — DECK 탭 제거, 4탭 구조로 변경
- `GameRepository.kt` — saveVersion 3 마이그레이션 로직

---

## 7. 용어 통일 규칙

**코드**: `UnitFamily` / **UI**: "속성"

UI에 표시되는 모든 스탯 용어를 한국어로 통일:

| 코드 | UI 표시 |
|------|---------|
| hp | 체력 |
| baseATK | 공격력 |
| baseSpeed | 공속 |
| range | 사거리 |
| defense | 방어력 |
| magicResist | 마법저항 |
| moveSpeed | 이동속도 |
| blockCount | 블록 수 |
| damageType PHYSICAL | 물리 |
| damageType MAGIC | 마법 |
| attackRange MELEE | 근접 |
| attackRange RANGED | 원거리 |
