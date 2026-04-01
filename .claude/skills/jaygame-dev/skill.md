---
name: jaygame-dev
description: "JayGame 개발 오케스트레이터. 기능 구현, 버그 수정, 콘텐츠 추가 등 모든 개발 작업을 분석→구현→검증 파이프라인으로 조율한다. '개발해줘', '구현해줘', '만들어줘', '기능 추가', '버그 수정', '밸런스 조정' 등 개발 작업 요청 시 트리거. 단순 질문이나 코드 읽기만 요청할 때는 트리거하지 않는다."
---

# JayGame Development Orchestrator

JayGame의 개발 작업을 분석→구현→검증 파이프라인으로 조율하는 오케스트레이터.

## 실행 모드: 서브 에이전트

## 에이전트 구성

| 에이전트 | 타입 | 역할 | 스킬 | 출력 |
|---------|------|------|------|------|
| jaygame-analyst | Explore | 코드 분석, 영향도 평가 | analyze-game | `_workspace/analysis.md` |
| jaygame-builder | general-purpose | 코드 구현 | add-unit 등 | `_workspace/changes.md` |
| jaygame-qa | general-purpose | 검증, 정합성 체크 | validate-game | `_workspace/qa-report.md` |

## 워크플로우

### Phase 1: 준비
1. 사용자 요청을 분석하여 작업 유형 분류:
   - **콘텐츠 추가**: 유닛, 스테이지, 레시피 → add-unit 스킬 활용
   - **기능 구현**: 새 시스템, UI 화면 → 전체 파이프라인
   - **버그 수정**: 특정 이슈 해결 → analyst로 원인 분석 → builder로 수정
   - **밸런스 조정**: 수치 변경 → analyst로 영향도 확인 → builder로 적용
2. `_workspace/` 디렉토리 생성

### Phase 2: 분석 (jaygame-analyst)
```
Agent(
  description: "게임 코드 분석",
  subagent_type: "Explore",
  model: "opus",
  prompt: "
    JayGame 프로젝트를 분석하라.
    에이전트 정의: .claude/agents/jaygame-analyst.md 를 읽고 따르라.

    요청: {사용자 요청 전문}

    분석 결과를 _workspace/analysis.md에 저장하라.
    영향 받는 파일 목록, 구현 순서, 리스크를 포함하라.
  "
)
```

### Phase 3: 구현 (jaygame-builder)
Phase 2 완료 후, analysis.md를 기반으로 구현:
```
Agent(
  description: "게임 코드 구현",
  subagent_type: "general-purpose",
  model: "opus",
  prompt: "
    JayGame 프로젝트에서 코드를 구현하라.
    에이전트 정의: .claude/agents/jaygame-builder.md 를 읽고 따르라.

    먼저 _workspace/analysis.md를 읽어 구현 가이드를 확인하라.

    요청: {사용자 요청 전문}

    변경 사항을 _workspace/changes.md에 요약하라.
  "
)
```

### Phase 4: 검증 (jaygame-qa)
Phase 3 완료 후, 변경사항 검증:
```
Agent(
  description: "게임 코드 검증",
  subagent_type: "general-purpose",
  model: "opus",
  prompt: "
    JayGame 프로젝트의 변경사항을 검증하라.
    에이전트 정의: .claude/agents/jaygame-qa.md 를 읽고 따르라.

    먼저 _workspace/changes.md를 읽어 변경 내용을 파악하라.

    검증 결과를 _workspace/qa-report.md에 저장하라.
  "
)
```

### Phase 5: 결과 보고
QA 리포트를 사용자에게 요약 보고:
- PASS → 완료 보고
- FAIL → 실패 항목과 수정 제안 보고, 사용자 판단 요청

## 데이터 전달: 파일 기반

```
_workspace/
├── analysis.md    ← analyst 산출물
├── changes.md     ← builder 산출물
└── qa-report.md   ← qa 산출물
```

## 에러 핸들링

| 에러 유형 | 대응 |
|----------|------|
| analyst 실패 | 메인이 직접 Grep/Read로 분석 수행 |
| builder 컴파일 에러 | builder에게 에러 메시지 전달하여 1회 재시도 |
| QA FAIL | 실패 항목을 builder에게 전달하여 수정 후 재검증 |
| 2회 연속 실패 | 사용자에게 상황 보고, 수동 개입 요청 |

## 작업 유형별 단축 경로

모든 작업이 전체 파이프라인을 거칠 필요는 없다:

| 작업 유형 | 단축 경로 |
|----------|----------|
| 단순 수치 변경 | analyst 스킵 → builder → qa |
| 유닛 추가 | add-unit 스킬 직접 사용 → qa |
| 코드 분석만 | analyst만 실행 |
| 데이터 검증만 | qa만 실행 (validate-game) |

## 테스트 시나리오

### 정상 흐름: 새 유닛 추가
1. 사용자: "인간 영웅 원거리 마법 유닛 추가해줘"
2. analyst: blueprints.json 구조 분석, 기존 인간 영웅 확인
3. builder: blueprint JSON 추가, Behavior 생성 (필요시)
4. qa: enum 일치, range 범위, ID 중복 검사 → PASS

### 에러 흐름: Recipe 재료 불일치
1. 사용자: "신화 유닛 추가해줘, 레시피도"
2. analyst: 기존 레시피 구조 분석
3. builder: blueprint + recipe 추가, 재료 ID 오타
4. qa: recipe 재료 ID가 blueprints에 없음 → FAIL
5. builder 재시도: ID 수정
6. qa 재검증 → PASS
