---
name: add-unit
description: "JayGame에 새 유닛을 추가하는 워크플로우. Blueprint JSON 작성, 아이콘 확인, Behavior 생성, Factory 등록까지 전체 과정을 처리한다. '유닛 추가', '새 캐릭터', '블루프린트 추가', '몬스터 추가', '유닛 만들어' 등의 요청 시 트리거."
---

# Add Unit Workflow

새 유닛을 JayGame에 추가하는 전체 워크플로우.

## 필수 정보 수집

사용자에게 다음을 확인한다 (미제공 시 질문):
- **이름** (Korean)
- **종족** (Race): HUMAN / SPIRIT / ANIMAL / ROBOT / DEMON
- **등급** (Grade): COMMON / RARE / HERO / LEGEND / MYTHIC / IMMORTAL
- **공격 범위**: MELEE / RANGED
- **피해 타입**: PHYSICAL / MAGICAL
- **스탯**: ATK, Speed (attackInterval)
- **특수 능력** (선택): 패시브, 궁극기 등

## 단계별 구현

### Step 1: Blueprint ID 생성
규칙: `{race}_{grade}_{번호}` (예: `human_common_01`)
- 기존 blueprints.json에서 같은 종족+등급의 마지막 번호 확인 후 +1

### Step 2: blueprints.json에 추가
```json
{
  "id": "{blueprintId}",
  "name": "{한국어 이름}",
  "race": "{HUMAN|SPIRIT|ANIMAL|ROBOT|DEMON}",
  "grade": "{COMMON|RARE|HERO|LEGEND|MYTHIC|IMMORTAL}",
  "attackRange": "{MELEE|RANGED}",
  "damageType": "{PHYSICAL|MAGICAL}",
  "stats": {
    "atk": 0.0,
    "attackInterval": 0.0,
    "range": 0.0
  },
  "isSummonable": true,
  "summonWeight": 0,
  "iconRes": "ic_bp_{blueprintId}"
}
```

**주의사항:**
- grade가 MYTHIC/IMMORTAL이면 `isSummonable: false`
- summonWeight: COMMON=60, RARE=25, HERO=12, LEGEND=3
- range 설계: MELEE 100~145, RANGED COMMON/RARE 160~180, HERO 270~280, LEGEND 295~320, MYTHIC 370~390, IMMORTAL 420~440

### Step 3: 아이콘 확인
- 파일: `res/drawable-xxhdpi/ic_bp_{blueprintId}.png`
- 없으면 사용자에게 알림 (fallback `ic_unit_0.png` 사용됨)

### Step 4: Behavior (선택)
커스텀 행동이 필요한 경우:
1. `engine/behavior/{Name}Behavior.kt` 생성
2. `UnitBehavior` interface 구현 (update, onAttack, onTakeDamage, canAttack, reset)
3. `BehaviorFactory.createBehavior()` when절에 등록

기본 행동이면 이 단계 생략.

### Step 5: Recipe (MYTHIC 전용)
MYTHIC 유닛이면 `hidden_recipes.json`에 레시피 추가:
```json
{
  "resultUnitId": "{blueprintId}",
  "ingredients": [
    { "specificUnitId": "{재료1_id}" },
    { "specificUnitId": "{재료2_id}" }
  ]
}
```
재료 ID가 blueprints.json에 존재하는지 반드시 확인.

### Step 6: 검증
- JSON 문법 검사
- enum 값 일치 확인 (grade, race, attackRange, damageType)
- blueprintId 중복 없는지 확인
- compileDebugKotlin으로 컴파일 체크 (Behavior 추가 시)
