# 조합석(luckyStones) 제거 → 골드(SP) 비용 대체 — 변경 사항

## 개요
레시피 합성에 사용되던 별도 재화 "조합석(luckyStones)"을 완전히 제거하고,
전투 중 SP(골드/코인)로 레시피 합성 비용을 직접 지불하도록 변경.

## 비용 매핑
- 기존 luckyStonesCost: 1 → goldCost: 300 (신화 레시피 15개)
- 기존 luckyStonesCost: 3 → goldCost: 900 (불멸 레시피 5개)

## 변경 파일 (11개)

### 1. `data/GameData.kt`
- EconomyData에서 `luckyStones` 필드 제거
- GameData에서 `luckyStones` 필드 제거
- `economy` computed property에서 luckyStones 매핑 제거
- `fromGrouped()`에서 `luckyStones = economy.luckyStones` 제거
- `copyEconomy()`에서 `luckyStones = updated.luckyStones` 제거

### 2. `data/GameRepository.kt`
- serialize(): `root.put("luckyStones", ...)` 제거
- deserialize(): `root.optInt("luckyStones", 0)` 제거
- GameData 생성자에서 `luckyStones = luckyStones` 제거
- 기존 세이브에 키가 있어도 무시됨 (하위호환)

### 3. `engine/BattleRewardCalculator.kt`
- 전투 결과에서 `luckyStones = 0` 제거

### 4. `engine/RecipeSystem.kt`
- `HiddenRecipe.luckyStonesCost` → `goldCost: Int = 300`
- `findMatchingRecipeOnGrid()`: `availableLuckyStones` → `availableGold`, 비교를 `goldCost` 기반으로
- `findSpecificRecipeOnGrid()`: 동일하게 변경
- `parseRecipe()`: `goldCost` 필드 우선 파싱, 없으면 `luckyStonesCost * 300`으로 하위호환 변환

### 5. `engine/BattleMergeHandler.kt`
- `engine.luckyStones` → `engine.sp.toInt()` (레시피 검색 시 가용 골드)
- 합성 성공 시 `engine.luckyStones -= recipe.luckyStonesCost` → `engine.sp -= recipe.goldCost`

### 6. `engine/BattleEngine.kt`
- `var luckyStones: Int` 속성 삭제
- `BuyLuckyStone` 커맨드 핸들러 삭제 (주석으로 대체)
- `statePublisher.publishLuckyStones()` 호출 삭제

### 7. `engine/BattleStatePublisher.kt`
- `lastPushedLuckyStones` 변수 삭제
- `publishLuckyStones()` 함수 삭제

### 8. `bridge/BattleBridge.kt`
- `BattleCommand.BuyLuckyStone` 삭제 (주석으로 대체)
- `_luckyStones` MutableStateFlow 삭제
- `luckyStones` StateFlow 삭제
- `updateLuckyStones()` 삭제
- `requestBuyLuckyStone()` 삭제
- `resetAll()`에서 `_luckyStones.value = 0` 제거

### 9. `ui/battle/BattleHud.kt`
- `RecipeDisplayInfo.hasEnoughLuckyStones` → `hasEnoughGold`
- RecipeBookDialog: `BattleBridge.luckyStones.collectAsState()` → `BattleBridge.state.collectAsState()` (SP 사용)
- `hasRecipeMissingStones` → `hasRecipeMissingGold`
- "조합석이 부족합니다" → "골드가 부족합니다"
- "조합석 x{cost}" → 코인 아이콘 + "{cost}"
- "조합석 부족 (필요: ...)" → "골드 부족 (필요: ...)"
- 상단 HUD 조합석 카운트 표시 (보석 아이콘 + 숫자) 완전 삭제
- `RecipeCard` 파라미터: `hasEnoughLuckyStones/luckyStonesCost` → `hasEnoughGold/goldCost`

### 10. `ui/battle/UpgradeSheet.kt`
- 조합석 구매 UI 섹션 전체 삭제 (보석 아이콘, 조합석 텍스트, 구매 버튼, 구분선)

### 11. `assets/units/hidden_recipes.json`
- 15개 신화 레시피: `"luckyStonesCost": 1` → `"goldCost": 300`
- 5개 불멸 레시피: `"luckyStonesCost": 3` → `"goldCost": 900`

## 하위호환
- GameRepository: 기존 세이브의 `luckyStones` 키는 무시됨 (파싱하지 않음)
- RecipeSystem: JSON에 `goldCost` 없으면 `luckyStonesCost * 300`으로 자동 변환
