# 로그라이크 강화 선택 시스템 — 변경 사항

## 새 파일 (3개)

### 1. `engine/RoguelikeEnhanceSystem.kt`
- `RoguelikeBuffGrade` enum (NORMAL/RARE/HERO)
- `RoguelikeBuff` data class (id, name, description, grade, minWave, stackable, weight)
- `ActiveRoguelikeBuff` data class (buff + stacks)
- `RoguelikeEnhanceSystem` class
  - `buffPool`: 22종 버프 정의 (일반 8, 희귀 8, 영웅 6)
  - `generateChoices(wave, activeBuffs)`: 가중치 기반 3개 선택지 생성
  - `applyBuff(buff, engine)`: 버프 ID별 엔진 멀티플라이어 수정

### 2. `ui/battle/RoguelikeBuffDialog.kt`
- 전체 화면 반투명 오버레이 다이얼로그
- 3개 버프 카드 (등급별 색상 테두리: 회/파/보)
- 카드 탭 시 `BattleBridge.requestSelectRoguelikeBuff(index)` 호출
- 이미 보유 중인 stackable 버프는 스택 수 표시

### 3. `ui/battle/RoguelikeBuffHud.kt`
- Row 배치 — 활성 버프를 작은 원형 아이콘으로 표시
- 등급 색상 배경 + 버프 이름 첫 글자 (스택 2+ 시 숫자)

## 수정 파일 (5개)

### 4. `bridge/BattleBridge.kt`
- import 추가: `RoguelikeBuff`, `ActiveRoguelikeBuff`
- `BattleCommand.SelectRoguelikeBuff(index)` 추가
- StateFlow 추가: `roguelikeChoices`, `activeRoguelikeBuffs`
- 함수 추가: `showRoguelikeChoices()`, `clearRoguelikeChoices()`, `updateActiveRoguelikeBuffs()`, `requestSelectRoguelikeBuff()`
- `reset()` 에 `_roguelikeChoices`, `_activeRoguelikeBuffs` 초기화

### 5. `engine/BattleEngine.kt`
- **멤버 변수** (169행 부근): 22개 로그라이크 멀티플라이어 + `roguelikeSystem` + `activeRoguelikeBuffs` + `pendingRoguelike`
- **WaveDelay 분기** (429행): `pendingRoguelike == true` 시 타이머 진행 차단
- **웨이브 클리어** (494행): 보스 웨이브(10/20/30/40/50) 클리어 시 `generateChoices()` → `showRoguelikeChoices()`
- **커맨드 처리** (397행): `SelectRoguelikeBuff` → `applyBuff()` → 활성 버프 업데이트 → `pendingRoguelike = false`
- **applyGroupBonus()**: `roguelikeSpdMult` 적용
- **fireProjectile()**: `roguelikeAtkMult`, `roguelikeCritBonus`, `roguelikeBossBonus`, berserker 적용
- **behavior 경로 데미지**: `roguelikeAtkMult`, `roguelikeBossBonus`, berserker 적용
- **updateProjectiles()**: armor shred, slow on hit, splash, chain lightning, execute 효과 적용
- **non-behavior 공격**: roguelike multishot (원거리 20% 2회)
- **requestSummonBlueprint()**: `roguelikeSummonDiscount` 비용 할인, `roguelikeSummonUpgrade` 등급 업
- **spawnFromBlueprint()**: `roguelikeRangeMult` 사거리 적용
- **chargeMana()**: `roguelikeManaBonus` 전달
- **코인 획득**: `roguelikeCoinMult`, `roguelikeVampiricChance` 적용
- **웨이브 클리어 코인**: `roguelikeCoinMult` 적용

### 6. `engine/BattleEconomy.kt`
- `sellUnit()`: `roguelikeSellBonus` 판매 가격 보너스 적용

### 7. `engine/GameUnit.kt`
- `chargeMana()`: `bonusMult` 파라미터 추가 (기본값 1f)

### 8. `ui/battle/BattleScreen.kt`
- `roguelikeChoices`, `activeRoguelikeBuffs` collectAsState 추가
- `RoguelikeBuffHud` — BattleTopHud 아래 표시
- `RoguelikeBuffDialog` — UpgradeSheet 다음 레이어에 표시

## 밸런스 조정 (2026-04-01)

### RoguelikeEnhanceSystem.kt
- `RoguelikeBuff` data class에 `requiredRaces: Set<UnitRace>?` 필드 추가
- **처형자(execute)**: HP 20% → **7%** 즉사 임계값 하향
- **연쇄 번개(chain_lightning)**: 10% → **7%** 확률 하향
- **소환 축복(summon_upgrade)**: 10% → **7%** 확률 하향
- **되팔이의 눈(sell_bonus)**: +50% → **+75%** 판매 보너스 상향
- **생명력 착취(vampiric)**: 20% → **30%** 확률 상향
- **맹독 강화(dot_boost)**: `requiredRaces = {SPIRIT, ANIMAL, DEMON}` — 해당 종족 드래프트 시에만 출현
- `generateChoices()`: `selectedRaces` 파라미터 추가, `requiredRaces` 필터링 로직 추가

### BattleEngine.kt
- **처형자**: `maxHp * 0.2f` → `maxHp * 0.07f`
- **연쇄 번개**: `Math.random() < 0.10` → `0.07`
- **소환 축복**: `Math.random() < 0.10` → `0.07`
- **방어구 파쇄(armor_shred)**: `target.armor *= 0.95f` → `maxOf(target.armor * 0.95f, target.baseArmor * 0.5f)` (원래 방어력의 50% 하한)
- `generateChoices` 호출 시 `BattleBridge.selectedRaces.value` 전달

### Enemy.kt
- `baseArmor: Float` 필드 추가 — `init()`에서 초기 armor 값 저장

## UX 개선 — 순차 등장 애니메이션 (2026-04-01)

### RoguelikeBuffDialog.kt
- **순차 등장 애니메이션**: 3개 버프 카드가 600ms 간격으로 하나씩 등장
  - `visibleCount` state (0→3), `LaunchedEffect`로 600ms delay 반복
  - `AnimatedVisibility` + `scaleIn(0.5f)` + `fadeIn()` 조합 (300ms)
  - 아직 등장하지 않은 카드는 클릭 불가 (`enabled = isVisible`)
- **"웨이브 N 클리어!" 텍스트 제거**: `currentWave` 파라미터 삭제, 웨이브 표시 Text 제거
- **카드 등장 사운드**: 각 카드 등장 시 `SfxManager.play(SoundEvent.RoguelikeCardReveal)` 호출

### SoundEvent.kt
- `RoguelikeCardReveal` enum 값 추가

### SfxManager.kt
- `RoguelikeCardReveal` → `sfx/level_up.ogg` 매핑 (기존 사운드 재사용)

## 동작 흐름

1. 보스 웨이브(10, 20, 30, 40, 50) 클리어
2. BattleEngine이 `generateChoices()` 호출 → BattleBridge에 선택지 전달
3. `pendingRoguelike = true` → WaveDelay 타이머 정지 (게임 일시정지)
4. BattleScreen에서 RoguelikeBuffDialog 표시
5. 플레이어가 카드 탭 → `SelectRoguelikeBuff` 커맨드 큐잉
6. BattleEngine이 커맨드 처리 → `applyBuff()` → 멀티플라이어 갱신
7. `pendingRoguelike = false` → WaveDelay 타이머 재개 → 다음 웨이브 시작
