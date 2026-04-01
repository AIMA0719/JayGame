# QA Report — 로그라이크 강화 시스템

**상태: PASS (1건 수정 후)**

## 1. 컴파일 체크

### 발견된 오류 (수정 완료)
- **BattleEngine.kt:400** — `return@forEach` 라벨 오류
  - 원인: 커맨드 처리 루프가 `for (cmd in ...)` 문인데, `forEach` 라벨을 사용
  - 수정: `return@forEach` 제거, `choices`를 nullable로 처리하고 `if (choices != null && cmd.index in choices.indices)` 로 변경

### 수정 후 빌드 결과
- `./gradlew compileDebugKotlin` — **BUILD SUCCESSFUL**
- 경고: 기존 deprecation 경고만 (UnitCollectionScreen.kt — 이번 변경과 무관)

## 2. 코드 정합성

| 항목 | 결과 |
|------|------|
| 버프 ID 일치 (RoguelikeEnhanceSystem ↔ BattleEngine) | **PASS** — 22종 버프 ID가 `applyBuff()`의 when절과 BattleEngine 멀티플라이어에 모두 대응 |
| BattleCommand.SelectRoguelikeBuff 정의 → 처리 | **PASS** — BattleBridge:330에 정의, BattleEngine:399에서 처리 |
| BattleBridge.reset() 초기화 | **PASS** — `_roguelikeChoices = null`, `_activeRoguelikeBuffs = emptyList()` (968-969행) |
| BattleScreen collectAsState → Dialog 전달 | **PASS** — 99-100행에서 collect, 321-329행에서 Dialog에 전달 |

### 멀티플라이어 초기값 검증
| 변수 | 초기값 | 사용 패턴 | 결과 |
|------|--------|-----------|------|
| roguelikeAtkMult | 1f | `* roguelikeAtkMult` | **PASS** (1f = 보너스 없음) |
| roguelikeSpdMult | 1f | `* roguelikeSpdMult` | **PASS** |
| roguelikeRangeMult | 1f | `* roguelikeRangeMult` | **PASS** |
| roguelikeCoinMult | 1f | `* roguelikeCoinMult` | **PASS** |
| roguelikeCritBonus | 0f | `+ roguelikeCritBonus` | **PASS** (0 = 추가 없음) |
| roguelikeSellBonus | 0f | `(1f + roguelikeSellBonus)` | **PASS** |
| roguelikeSummonDiscount | 0f | `(1f - roguelikeSummonDiscount)` | **PASS** |
| roguelikeBossBonus | 0f | `(1f + roguelikeBossBonus)` | **PASS** |
| roguelikeManaBonus | 0f | `(1f + roguelikeManaBonus)` | **PASS** |
| roguelikeBerserkerBase | 0f | `if (>0f) 1f + base*wave/100f else 1f` | **PASS** |
| boolean 멀티플라이어들 | false | `if (flag) ...` | **PASS** |

## 3. Canvas GC 규칙

| 파일 | 결과 |
|------|------|
| RoguelikeBuffDialog.kt | **PASS** — Compose UI, Canvas 아님. Color 상수 파일 상단에 pre-allocate |
| RoguelikeBuffHud.kt | **PASS** — Compose UI, Canvas 아님. Color 상수 파일 상단에 pre-allocate |
| BattleEngine.kt 추가 코드 | **PASS** — 엔진 루프 내 새 객체 생성 없음. `activeRoguelikeBuffs.toList()` 호출은 커맨드 처리(UI 이벤트 응답) 시에만 실행되므로 매 프레임 GC 유발 안 함 |

## 4. 스레드 안전성

| 방향 | 메커니즘 | 결과 |
|------|----------|------|
| UI → 엔진 | `BattleBridge.requestSelectRoguelikeBuff()` → `ConcurrentLinkedQueue` → `drainCommands()` | **PASS** |
| 엔진 → UI | `BattleBridge.showRoguelikeChoices()` / `updateActiveRoguelikeBuffs()` → `MutableStateFlow.value` | **PASS** |

## 5. 로직 검증

| 항목 | 결과 | 근거 |
|------|------|------|
| 보스 웨이브 클리어 후 트리거 | **PASS** | `clearedWave % 10 == 0` → 10, 20, 30, 40, 50 |
| 마지막 웨이브(59) 제외 | **PASS** | `isLastWave` 분기가 먼저 Victory로 이동 + `clearedWave < maxWaves` 이중 체크 |
| pendingRoguelike 시 타이머 정지 | **PASS** | WaveDelay 상태에서 `if (pendingRoguelike)` → 빈 블록 (타이머 감소 안 함) |
| non-stackable 중복 제외 | **PASS** | `generateChoices()`에서 `buff.stackable \|\| buff.id !in activeIds` 필터 |
| 선택 후 pendingRoguelike 해제 | **PASS** | `SelectRoguelikeBuff` 커맨드 처리 끝에 `pendingRoguelike = false` |

## 6. import 확인

| 파일 | 결과 |
|------|------|
| RoguelikeEnhanceSystem.kt | **PASS** — `kotlin.random.Random`, `BattleEngine` (같은 패키지) |
| RoguelikeBuffDialog.kt | **PASS** — Compose UI imports + `BattleBridge`, `RoguelikeBuff`, `ActiveRoguelikeBuff`, `RoguelikeBuffGrade`, theme colors |
| RoguelikeBuffHud.kt | **PASS** — Compose UI imports + `ActiveRoguelikeBuff`, `RoguelikeBuffGrade` |
| BattleBridge.kt | **PASS** — `RoguelikeBuff`, `ActiveRoguelikeBuff` import 추가 (13-14행) |
| BattleEngine.kt | **PASS** — 같은 패키지이므로 import 불필요 |
| BattleEconomy.kt | **PASS** — 같은 패키지, `roguelikeSellBonus` 접근 가능 (`internal`) |
| BattleScreen.kt | **PASS** — 같은 패키지(`ui.battle`), Roguelike composable import 불필요 |
| GameUnit.kt | **PASS** — `chargeMana(bonusMult)` 파라미터 추가, 기본값 1f로 하위호환 유지 |

## 7. 마이너 이슈 (비차단)

1. **RoguelikeBuffDialog.kt:57** — `currentWave: Int = 0` 파라미터가 사용되지 않음 (대신 `BattleBridge.state`에서 가져옴). 불필요한 파라미터이나 컴파일에 영향 없음.

## 수정 사항 요약

| 파일 | 수정 내용 |
|------|-----------|
| `engine/BattleEngine.kt:400` | `return@forEach` → null 체크로 변경 (`choices != null &&`) |
