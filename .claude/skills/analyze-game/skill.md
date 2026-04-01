---
name: analyze-game
description: "JayGame 코드베이스 영향도 분석. 기능 추가, 버그 수정, 밸런스 조정 전에 변경이 필요한 파일과 시스템을 파악한다. '분석해줘', '영향도', '어디 수정해야', '이거 바꾸면 뭐가 영향받아' 등의 요청 시 트리거."
---

# Game Codebase Analysis

JayGame의 변경 영향도를 분석하여 구현 전 필요한 정보를 수집한다.

## 분석 절차

### 1. 요청 파악
사용자 요청에서 변경 대상 시스템을 식별한다:
- **엔진**: BattleEngine, behavior, systems (merge/recipe/buff/mana/wave)
- **데이터**: blueprints.json, hidden_recipes.json, StageData.kt
- **UI**: Compose screens, battle canvas, components
- **브릿지**: BattleBridge StateFlow

### 2. 관련 코드 탐색
```
패키지 구조:
com.jay.jaygame/
├── engine/          ← BattleEngine, GameUnit, Enemy, 시스템들
│   ├── behavior/    ← UnitBehavior 구현체
│   └── math/        ← Vec2, GameRect
├── data/            ← GameData, GameRepository, Defs
├── bridge/          ← BattleBridge (engine↔UI 디커플링)
├── ui/battle/       ← Canvas 렌더링
├── ui/screens/      ← Hub 화면들
├── ui/viewmodel/    ← Orbit MVI ViewModels
├── ui/components/   ← 공유 UI
└── navigation/      ← NavGraph, Routes
```

Grep으로 관련 함수/클래스를 찾고, 해당 부분만 Read한다. BattleEngine 전체를 읽지 않는다.

### 3. 영향도 매핑

변경 대상 → 직접 영향 → 간접 영향 순서로 추적:

| 변경 유형 | 직접 영향 | 간접 영향 |
|----------|----------|----------|
| 유닛 추가 | blueprints.json, behavior | BehaviorFactory, BlueprintRegistry |
| 스탯 변경 | GameUnit, Defs | DamageCalculator, BattleEngine |
| UI 추가 | screens/, viewmodel/ | ViewModelFactory, NavGraph |
| 이펙트 추가 | engine/ | Canvas 렌더링 (GC 규칙!) |
| 경제 조정 | BattleEngine economy | BattleBridge, BattleHUD |

### 4. 리스크 체크리스트
- [ ] Canvas 렌더링 변경 → GC 규칙 위반 가능성
- [ ] JSON 데이터 변경 → Kotlin enum 불일치 가능성
- [ ] 새 클래스 추가 → Factory 등록 누락 가능성
- [ ] 엔진 로직 변경 → 다른 시스템 사이드이펙트

### 5. 산출물 작성
`_workspace/analysis.md`에 결과를 저장한다:
```markdown
# 변경 분석: {요청 요약}

## 영향 파일
- `path/to/file.kt:123` — {변경 내용}

## 구현 순서
1. {첫 번째 단계}
2. {두 번째 단계}

## 리스크
- {위험 요소와 대응 방안}
```
