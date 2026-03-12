# Settings Redesign & Home Profile Banner

## Overview

설정 화면을 카테고리별 서브페이지 구조로 재설계하고, 홈 화면 상단에 추상 엠블럼 배경의 배너형 프로필을 추가한다.

## 1. Settings Screen — Category-based Sub-page Structure

### 메인 화면 (SettingsScreen)

카테고리 목록을 카드형 리스트로 표시. 각 항목은 아이콘 + 제목 + 우측 chevron(>) 구조.

| 카테고리 | 아이콘 | 동작 |
|---------|--------|------|
| 오디오 | 스피커 아이콘 | 서브페이지: 사운드/음악 토글 |
| 게임플레이 | 게임패드 아이콘 | 서브페이지: 난이도 설정 |
| 주간보상 | 선물 아이콘 | DailyLoginDialog 표시 |
| 업적 | 트로피 아이콘 | Routes.ACHIEVEMENTS로 네비게이션 |
| 영웅 도감 | 책 아이콘 | Routes.UNIT_CODEX로 네비게이션 |
| 데이터 관리 | 경고 아이콘 | 서브페이지: 버전 정보 + 데이터 초기화 |

### 서브페이지 구조

서브페이지는 별도 Route가 아닌, SettingsScreen 내부에서 `AnimatedContent`로 전환한다. 상단에 뒤로가기 화살표 + 카테고리 제목을 표시.

**오디오 서브페이지:**
- 사운드 ON/OFF 토글 (기존과 동일)
- 음악 ON/OFF 토글 (기존과 동일)

**게임플레이 서브페이지:**
- 난이도 선택 (쉬움/보통/어려움) — 기존 DifficultyDialog 내용을 인라인으로 표시

**데이터 관리 서브페이지:**
- 버전 정보 표시 (v0.5.0)
- 데이터 초기화 버튼 + 확인 다이얼로그

### 아이콘

각 카테고리 아이콘은 XML Vector Drawable로 새로 생성한다:
- `ic_settings_audio.xml` — 스피커
- `ic_settings_gameplay.xml` — 게임패드
- `ic_settings_reward.xml` — 선물 상자
- `ic_settings_data.xml` — 데이터/경고

업적, 영웅 도감은 기존 아이콘 재사용 (`ic_achievement.xml`, `ic_nav_collection.xml`).

## 2. Home Screen — Banner Profile

### 현재 구조 (제거 대상)
- 상단 Row: Lv뱃지 + "플레이어" + RankBadge + 골드/다이아
- 트로피 텍스트

### 새로운 배너 구조

홈 화면 상단에 높이 ~120dp의 배너 영역:

```
┌──────────────────────────────────────────┐
│  [엠블럼 배경 이미지]                       │
│                                          │
│  Lv.5  플레이어          🏆 1,234         │
│  ◆ 골드 랭크             💎500  🪙12,000  │
│  ▓▓▓▓▓▓▓▓▓░░ XP 75%                     │
└──────────────────────────────────────────┘
```

**배너 배경:** 추상적 문양/엠블럼 PNG 이미지. 다크 네온 테마 톤(어두운 배경 + 골드/시안 문양). AI 생성 또는 직접 제작.

**배너 내용:**
- 좌측: 레벨 뱃지 + 닉네임 (기존 스타일 유지)
- 우측 상단: 트로피 수
- 좌측 하단: 랭크 뱃지 (기존 RankBadge)
- 우측 하단: 골드 + 다이아몬드
- 하단: XP 프로그레스 바 (playerLevel 진행도)

**배너 스타일:**
- RoundedCornerShape(16.dp)
- 배경 이미지 위에 반투명 그라데이션 오버레이
- 테두리: Gold.copy(alpha = 0.3f)
- 기존 테마 색상 활용 (Gold, NeonCyan, LightText 등)

### 배경 에셋

`assets/raw/ui/banner_emblem.png` — 추상 문양 배경 이미지
- 크기: 1280x360px (배너 비율)
- 스타일: 다크 배경 + 골드/시안 기하학적 문양 또는 엠블럼
- AI 이미지 생성 도구로 생성 (또는 코드로 Canvas에 직접 그리기)

> **대안:** PNG 에셋 대신 Compose Canvas로 기하학적 패턴을 직접 그릴 수 있다. 에셋 관리 불필요, 해상도 독립적. 추천 방식.

## 3. Affected Files

| 파일 | 변경 |
|------|------|
| `SettingsScreen.kt` | 전면 재작성 — 카테고리 목록 + 서브페이지 구조 |
| `HomeScreen.kt` | 상단 프로필 Row → 배너형 프로필로 교체 |
| `Routes.kt` | 변경 없음 (서브페이지는 내부 state로 관리) |
| `NavGraph.kt` | 변경 없음 |
| `res/drawable/` | 카테고리 아이콘 4개 추가 |
| `GameData.kt` | 변경 없음 |

## 4. Design Decisions

- **서브페이지를 별도 Route로 만들지 않는 이유:** 설정 카테고리가 6개뿐이고, 각 서브페이지 내용이 간단하므로 내부 state 전환이 더 심플.
- **배너 배경을 Canvas로 그리는 것을 추천:** PNG 에셋 없이 기하학적 패턴(원, 선, 다이아몬드 등)을 Canvas로 그리면 해상도 독립적이고 테마 색상 활용 가능.
- **주간보상/업적/도감은 서브페이지가 아닌 기존 다이얼로그/네비게이션 유지:** 이미 잘 동작하는 기능이므로 진입점만 설정 카테고리로 이동.
