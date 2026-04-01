---
name: validate-game
description: "JayGame 데이터 정합성과 코드 규칙 검증. Blueprint JSON과 Kotlin enum 일치, Canvas GC 규칙, Factory 등록 누락, Recipe 재료 존재 여부를 검사한다. '검증', '체크', '정합성', 'QA', '데이터 확인', '빌드 체크' 등의 요청 시 트리거."
---

# Game Validation

JayGame의 데이터 정합성과 코드 규칙을 검증한다.

## 검증 항목

### 1. Blueprint 데이터 정합성
`assets/units/blueprints.json` 파일을 읽고 각 항목에 대해:

**enum 일치 검사:**
- `grade` ∈ {COMMON, RARE, HERO, LEGEND, MYTHIC, IMMORTAL}
- `race` ∈ {HUMAN, SPIRIT, ANIMAL, ROBOT, DEMON}
- `attackRange` ∈ {MELEE, RANGED}
- `damageType` ∈ {PHYSICAL, MAGICAL}

위 값 외에 다른 문자열이 있으면 **FAIL** — RecipeSystem/BlueprintRegistry에서 조용히 스킵됨.

**ID 중복 검사:**
- 모든 blueprint의 id가 유니크한지 확인

**range↔attackRange 정합:**
- MELEE: range 100~145
- RANGED: range 160~440 (등급별 차이)
- 범위 밖이면 WARNING

**summon 정합:**
- MYTHIC/IMMORTAL이면 isSummonable=false 여부
- summonWeight가 등급에 맞는지 (COMMON=60, RARE=25, HERO=12, LEGEND=3)

### 2. Recipe 정합성
`assets/units/hidden_recipes.json` 파일을 읽고:
- 모든 `resultUnitId`가 blueprints에 존재하는지
- 모든 `specificUnitId` (재료)가 blueprints에 존재하는지
- 결과 유닛이 MYTHIC/IMMORTAL인지 확인

### 3. Canvas GC 규칙
`ui/battle/` 디렉토리의 Kotlin 파일에서:
```
Grep 패턴: drawScope 또는 DrawScope 블록 내에서
  Color(, Paint(, Brush., SolidColor( 등 객체 생성 검출
```
- 파일 상단/companion object에 선언되지 않은 Color/Paint가 있으면 **FAIL**
- `remember { Color(...) }` 패턴도 허용

### 4. Factory 등록 확인
- `engine/behavior/` 내 모든 Behavior 클래스가 `BehaviorFactory`에 등록되었는지
- `ui/viewmodel/` 내 모든 ViewModel이 `ViewModelFactory`에 등록되었는지

### 5. 컴파일 체크
```bash
./gradlew compileDebugKotlin
```
에러 발생 시 에러 메시지를 파싱하여 파일:줄번호 + 원인 보고.

## 출력 형식

```markdown
# QA Report

## 요약: PASS / FAIL (N개 이슈)

### Blueprint 정합성: PASS/FAIL
- [항목별 결과]

### Recipe 정합성: PASS/FAIL
- [항목별 결과]

### Canvas GC 규칙: PASS/FAIL
- [위반 위치]

### Factory 등록: PASS/FAIL
- [누락 항목]

### 컴파일: PASS/FAIL
- [에러 내용]
```
