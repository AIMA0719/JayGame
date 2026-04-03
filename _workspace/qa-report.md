# QA Report: 조합석(luckyStones) 제거 -> 골드(SP) 비용 대체 검증

날짜: 2026-04-03
상태: **PASS**

---

## 1. luckyStones 잔존 참조 검사

**결과: PASS**

코드 전체(`app/src/main/java`) "luckyStone" 검색 결과, 실제 코드에서의 참조는 **0건**.
남아있는 2건은 모두 **주석**으로, 의도적인 하위호환 설명:
- `RecipeSystem.kt:247` — `// 하위호환: 기존 luckyStonesCost -> x300 변환` (fallback 파싱 로직)
- `RecipeSystem.kt:248` — `obj.optInt("luckyStonesCost", 1) * 300` (하위호환 코드, goldCost 없을 때만 동작)
- `BattleBridge.kt:441` — `// BuyLuckyStone removed` 주석
- `BattleEngine.kt:413` — `// BuyLuckyStone removed` 주석

`_workspace/` 디렉토리의 변경 기록 문서에만 추가 참조 존재 (정상).

## 2. 컴파일 가능성

**결과: PASS**

`./gradlew compileDebugKotlin` — **BUILD SUCCESSFUL** (12s, 오류 0건)

## 3. JSON 정합성 (hidden_recipes.json)

**결과: PASS**

- 신화 레시피 15건: 모두 `"goldCost": 300` 설정 확인
- 불멸 레시피 5건: 모두 `"goldCost": 900` 설정 확인
- `luckyStonesCost` 필드: JSON에서 완전 제거됨
- 총 20개 레시피 모두 `goldCost` 필드 정상

## 4. RecipeSystem 로직

**결과: PASS**

- `HiddenRecipe` data class: `goldCost: Int = 300` (기본값 300)
- `findMatchingRecipeOnGrid()`: `availableGold` 파라미터로 변경, `recipe.goldCost`와 비교 (`<` 연산)
- `findSpecificRecipeOnGrid()`: 동일하게 `availableGold < recipe.goldCost` 체크
- `parseRecipe()`: `goldCost` 필드 우선 파싱, 없으면 `luckyStonesCost * 300` 하위호환 변환 (안전장치)

## 5. BattleMergeHandler SP 차감 로직

**결과: PASS**

- `engine.sp.toInt()`를 `availableGold`로 전달 (RecipeSystem 호출 시)
- 합성 성공 시: `engine.sp = (engine.sp - recipe.goldCost).coerceAtLeast(0f)` -- 음수 방지 포함
- SP(Float) -> goldCost(Int) 변환 시 정밀도 손실 가능성은 `coerceAtLeast(0f)`로 안전하게 처리됨

## 6. UI 텍스트 잔존 검사

**결과: PASS**

- "조합석" 텍스트: UI 코드(`ui/` 패키지)와 리소스(`res/`)에서 **0건**
- RecipeCard: `goldCost` 표시 (코인 이모지 + 금액), `hasEnoughGold`로 색상 분기
- 골드 부족 메시지: `"골드 부족 (필요: $goldCost)"` (BattleHud.kt:707)

## 7. BattleBridge 완전 삭제 확인

**결과: PASS**

- `_luckyStones` MutableStateFlow: 삭제 확인
- `luckyStones` StateFlow: 삭제 확인
- `updateLuckyStones()`: 삭제 확인
- `requestBuyLuckyStone()`: 삭제 확인
- `BattleCommand.BuyLuckyStone`: 삭제 확인 (주석으로 대체)

## 8. GameData 직렬화

**결과: PASS**

- `GameData.kt`: `luckyStones` 필드 **0건** -- 완전 제거
- `GameRepository.kt`: `luckyStones` 참조 **0건** -- serialize/deserialize 모두 제거
- 테스트 파일(`app/src/test/`): `luckyStones` 참조 **0건**

---

## 종합 평가

| 검증 항목 | 결과 |
|-----------|------|
| luckyStones 잔존 참조 | PASS |
| 컴파일 가능성 | PASS |
| JSON 정합성 | PASS |
| RecipeSystem 로직 | PASS |
| BattleMergeHandler SP 차감 | PASS |
| UI 텍스트 잔존 | PASS |
| BattleBridge 삭제 | PASS |
| GameData 직렬화 | PASS |

**전체: PASS (8/8)** -- 조합석 제거 및 골드 비용 대체가 깔끔하게 완료됨.
