# JayGame 유닛 시스템 대규모 개편 설계 문서

> 작성일: 2026-03-18
> 상태: Review (v2 — 아키텍처 리뷰 반영)

---

## 1. 개요

JayGame의 유닛 시스템을 단순한 속성(Fire/Frost 등) 기반에서 **다축 분류 체계**로 전면 재설계한다. 근거리/원거리, 물리/마법, 역할(탱커/근딜/원딜/서포터/컨트롤러) 분류를 추가하고, 히든 유닛(레시피 합성)과 특수 유닛(전장 변환)을 도입하여 전략적 깊이를 대폭 향상시킨다.

### 1.1 현재 상태

- 6개 속성(Fire/Frost/Poison/Lightning/Support/Wind) × 7등급 = 42종
- 모든 유닛이 프로젝타일 기반 원거리 공격
- 물리/마법 구분이 속성에 묶여 있음 (Frost/Support만 마법)
- 히든 유닛, 특수 유닛 개념 없음

### 1.2 목표

- 유닛 분류: 속성 + 사거리 + 데미지유형 + 역할 4축
- 유닛 수: ~98종 (일반 70 + 히든 18 + 특수 10)
- 근거리 전투 메커니즘 도입 (탱커 블로킹, 어쌔신 돌진)
- 히든 유닛 레시피 합성 (블라인드 발견)
- 특수 유닛 전장 변환 시스템
- 속성 시너지 + 역할 시너지 병렬 체계
- 유닛 데이터 JSON 외부화
- Strategy 패턴 기반 GameUnit 재설계

---

## 2. 유닛 분류 체계

### 2.1 Enum 정의

```kotlin
enum class UnitFamily {  // 기존 유지
    FIRE, FROST, POISON, LIGHTNING, SUPPORT, WIND
}

enum class UnitRole {  // 신규
    TANK,        // 적 경로 블로킹, 높은 HP/방어
    MELEE_DPS,   // 돌진 공격 후 복귀, 높은 단일 데미지
    RANGED_DPS,  // 타일 고정 프로젝타일, 꾸준한 DPS
    SUPPORT,     // 아군 버프/힐, 적 디버프
    CONTROLLER,  // 군중제어(CC), 감속/스턴/넉백
}

enum class AttackRange {  // 신규
    MELEE,   // 적에게 직접 접근하여 공격
    RANGED,  // 프로젝타일 발사
}

enum class DamageType {  // 기존 GameEngine.kt의 DamageType을 확장 (MAGIC → MAGIC 유지)
    PHYSICAL,  // 적 방어력(Armor)으로 감쇄
    MAGIC,     // 적 마법저항(MagicResist)으로 감쇄 — 기존 네이밍 유지
}
```

### 2.2 역할별 사거리/데미지 매핑

| 역할 | 사거리 | 데미지유형 | 핵심 정체성 |
|------|--------|-----------|------------|
| 탱커 | 근거리 | 물리 | 적 경로 블로킹, 체력벽 |
| 근딜 | 근거리 | 물리/마법 | 돌진→고데미지→복귀 |
| 원딜 | 원거리 | 물리/마법 | 타일 고정, 꾸준한 DPS |
| 서포터 | 원거리 | 마법 | 버프/힐/디버프 |
| 컨트롤러 | 근거리/원거리 | 마법 | CC, 전장 통제 |

---

## 3. UnitBlueprint 데이터 모델

기존 `UnitDef`를 대체하는 유닛 정의 구조.

```kotlin
data class UnitBlueprint(
    val id: String,                       // "fire_tank_01" 형식
    val name: String,                     // "루비 가디언"
    val families: List<UnitFamily>,       // [FIRE] 또는 [FIRE, LIGHTNING] (히든 듀얼)
    val grade: UnitGrade,                 // COMMON ~ IMMORTAL
    val role: UnitRole,                   // TANK
    val attackRange: AttackRange,         // MELEE
    val damageType: DamageType,           // PHYSICAL
    val stats: UnitStats,
    val behaviorId: String,               // "tank_blocker", "assassin_dash" 등
    val ability: AbilityDef?,
    val uniqueAbility: UniqueAbilityDef?,
    val mergeResultId: String?,           // 일반 합성 결과
    val isSummonable: Boolean,
    val summonWeight: Int,                // 소환 확률 가중치 (0 = 소환 불가)
    val unitCategory: UnitCategory,       // NORMAL, HIDDEN, SPECIAL
    val iconRes: Int,
    val description: String
)

data class UnitStats(
    val hp: Float,           // 최대 HP (모든 유닛 보유, 원거리는 높게 설정)
    val baseATK: Float,      // 공격력
    val baseSpeed: Float,    // 공격속도
    val range: Float,        // 사거리
    val defense: Float,      // 물리 방어력
    val magicResist: Float,  // 마법 저항력
    val moveSpeed: Float,    // 이동속도 (근거리 유닛용)
    val blockCount: Int,     // 블로킹 수 (탱커용, 등급별로 블루프린트에 직접 설정)
)

enum class UnitCategory {
    NORMAL,   // 일반 유닛 (소환/합성으로 획득)
    HIDDEN,   // 히든 유닛 (레시피 합성으로만 획득)
    SPECIAL,  // 특수 유닛 (전장 변환/특수 능력)
}

// 능력 정의 (기존 UniqueAbility 확장)
data class AbilityDef(
    val id: String,              // "fire_slash", "frost_nova"
    val name: String,            // "화염참"
    val type: AbilityType,       // PASSIVE, ACTIVE, AURA
    val damageType: DamageType,  // PHYSICAL 또는 MAGIC
    val value: Float,            // 데미지 배율 또는 효과 수치
    val cooldown: Float,         // 쿨다운 (초), 0 = 패시브
    val range: Float,            // 능력 사거리
    val description: String,
)

enum class AbilityType {
    PASSIVE,  // 자동 발동 (N회 공격마다, 조건부)
    ACTIVE,   // 쿨다운 기반 수동 발동
    AURA,     // 지속적 범위 효과
}

data class UniqueAbilityDef(
    val id: String,              // "phoenix_rebirth"
    val name: String,            // "불사조의 부활"
    val passive: AbilityDef?,    // 패시브 효과
    val active: AbilityDef?,     // 액티브 효과
    val requiredGrade: UnitGrade, // 최소 해금 등급 (HERO+)
)
```

### 핵심 변경점

- id: Int → String ("fire_tank_01" 형태로 가독성 확보)
- family → families: List (히든 유닛 듀얼 패밀리 지원)
- HP/방어력/마법저항 추가 (근거리 유닛 생존 + 데미지 대칭)
- behaviorId로 역할별 전투 행동을 외부에서 지정 (미등록 ID는 로드 시 예외 발생)
- blockCount: 등급별로 블루프린트에 직접 값 설정
- summonWeight: 유닛별 개별 소환 가중치
- unitCategory: 일반/히든/특수 유닛 구분
- AbilityDef, UniqueAbilityDef 타입 명시적 정의

---

## 4. 행동 시스템 (Behavior)

Strategy 패턴으로 역할별 전투 행동을 분리한다.

```kotlin
interface UnitBehavior {
    fun update(unit: GameUnit, dt: Float, findEnemy: EnemyFinder)
    fun onAttack(unit: GameUnit, target: Enemy): AttackResult
    fun onTakeDamage(unit: GameUnit, damage: Float, isMagic: Boolean)
    fun reset()  // 오브젝트 풀 반환 시 행동 상태 초기화
}
```

역할 전용 상태(blockedEnemies, dashTarget 등)는 GameUnit이 아닌 각 Behavior 구현체 내부에서 관리한다.

### 4.1 TankBlocker — 탱커

내부 상태: `blockedEnemies: MutableList<Enemy>`, `respawnTimer: Float`

- 적 경로 위로 이동하여 물리적으로 블로킹
- 블로킹된 적은 이동 정지, 탱커를 공격
- `blockCount`만큼 동시 블로킹 (블루프린트에 등급별 값 설정)
- HP 0 → 사망 프레임에 blockedEnemies 즉시 전부 해제(같은 프레임에 이동 재개) → 홈타일 귀환 → 부활 쿨다운
- 부활 쿨다운 중 비활성
- 보스는 블로킹 시간 5초 제한 후 강제 돌파

### 4.2 AssassinDash — 근딜

내부 상태: `dashTarget: Enemy?`, `dashCooldown: Float`

- 평소 홈타일 대기
- 적 감지 시 돌진 → 높은 단일 데미지 → 홈타일 복귀
- 돌진 중 무적 (이동 중 피격 방지)
- **타겟 사망 시 처리**: 돌진 중 타겟이 죽으면 즉시 홈타일 복귀 (데미지 미적용, 쿨다운 절반만 적용)
- 공격 후 쿨다운 동안 대기
- 등급 높을수록 돌진 속도/데미지 증가

### 4.3 RangedShooter — 원딜

- 기존 방식 유사. 타일 고정, 프로젝타일 발사
- 물리 원딜: 단일 대상 고데미지 (궁수형)
- 마법 원딜: 범위 공격/관통 (마법사형)

### 4.4 SupportAura — 서포터

- 타일 고정, 일정 범위 내 아군 버프
- 버프 종류: 공격력/공속/방어 증가, HP 회복
- 적에 대한 직접 공격력은 낮음
- 특정 등급 이상은 디버프도 가능

### 4.5 ControllerCC — 컨트롤러

- 근거리형: 범위 내 적에게 CC (스턴/감속/넉백)
- 원거리형: CC 프로젝타일 발사 (빙결/속박/공포)
- 직접 데미지는 낮지만 적 무력화 특화
- 탱커와 조합 시 블로킹 + CC로 적 완전 정지

### 4.6 탱커 블로킹 상세

```
적 이동 경로:  ● → → → → → → 끝 (HP 감소)
탱커 배치 시:  ● → → ■탱커■ (적 정지, 탱커와 교전)
                     ↑ 적이 탱커 HP를 깎음
                     탱커 사망 시 → 적 이동 재개
```

---

## 5. 유닛 구성표 (~98종)

### 5.1 일반 유닛 (~70종)

속성당 2~3역할, 등급 분산 배치.

| 속성 | 탱커 | 근딜 | 원딜 | 서포터 | 컨트롤러 | 소계 |
|------|------|------|------|--------|----------|------|
| Fire | ★ (물리) | ★★ (물리) | ★★ (마법) | - | - | ~12 |
| Frost | ★ (물리) | - | ★★ (마법) | - | ★★ (마법) | ~12 |
| Poison | - | ★★ (물리) | ★ (마법) | - | ★★ (마법) | ~12 |
| Lightning | - | ★★ (물리) | ★★ (마법) | ★ (마법) | - | ~12 |
| Support | ★★ (물리) | - | - | ★★ (마법) | ★ (마법) | ~12 |
| Wind | - | ★★ (물리) | ★★ (물리) | ★ (마법) | - | ~12 |

★ = 3~4등급 라인, ★★ = 5~7등급 라인

### 5.2 히든 유닛 (~18종)

서로 다른 유닛 2~3종 레시피 합성. 완전 블라인드 발견.

예시:

| 히든 유닛 | 재료 | 역할 | 특징 |
|----------|------|------|------|
| 뇌염의 기사 | Fire 근딜 Hero + Lightning 근딜 Hero | 근딜 | 물리+마법 듀얼 데미지 |
| 빙독의 현자 | Frost 컨트롤러 Rare + Poison 컨트롤러 Rare | 컨트롤러 | 빙결+독 동시 부여 |
| 폭풍의 수호자 | Wind 원딜 Legend + Support 탱커 Legend | 탱커 | 블로킹 시 주변 넉백 |
| 그림자 암살자 | Poison 근딜 Hero + Wind 근딜 Hero | 근딜 | 투명화 돌진, 3배 크리티컬 |
| 심판의 대천사 | Support 서포터 Ancient + Lightning 원딜 Ancient | 서포터 | 전체 버프 + 전체 번개 |

- 일반 등급 체계 밖의 고유 등급 (별도 색상/테두리)
- 같은 재료 등급의 일반 유닛보다 강함
- 추가 합성/진화 불가 (최종 형태)

### 5.3 특수 유닛 (~10종)

전장 변환 + 특수 능력. 일반 소환 1% 미만 또는 던전/웨이브 보상.

| 특수 유닛 | 타입 | 효과 |
|----------|------|------|
| 결계사 | 전장 변환 | 3×3 보호 결계, 내부 아군 방어력 +50% |
| 차원술사 | 전장 변환 | 적 경로 특정 구간 -70% 감속 필드 |
| 시간의 현자 | 특수 능력 | 전장 시간 감속 (적만 50% 슬로우) |
| 소환술사 | 특수 능력 | 미니 유닛 3체 자동 소환 |
| 대장장이 | 특수 능력 | 인접 아군 무기 강화 (영구 ATK +10%) |
| 연금술사 | 특수 능력 | 적 처치 시 골드 2배, SP 보너스 |
| 결계파괴자 | 전장 변환 | 보스 실드/버프 디스펠 필드 |
| 토템마스터 | 전장 변환 | 토템 설치 (공격/방어/속도 택1) |

- 그리드에 배치하지만 직접 공격하지 않음
- 지속적 전장 효과 제공
- 동시 최대 2체, 같은 특수 유닛 1체 제한

---

## 6. 시너지 시스템

### 6.1 속성 시너지 (기존 확장)

| 속성 | 2개 배치 | 3개 배치 | 풀 시너지 특수 효과 |
|------|---------|---------|-------------------|
| Fire | ATK +5% | ATK +10% | 화상 데미지 연장 +50% |
| Frost | CC 지속 +15% | CC 지속 +30% | 빙결 시 방어력 -20% |
| Poison | DoT +5% | DoT +12% | 처치 시 독 전염 |
| Lightning | 공속 +5% | 공속 +10% | 체인 대상 +1 |
| Support | 버프 효과 +10% | 버프 효과 +20% | 힐량 +25% |
| Wind | 이동속도 +10% | 이동속도 +20% | 넉백 거리 +40% |

### 6.2 역할 시너지 (신규)

| 역할 | 2개 배치 | 3개 배치 | 4개+ 배치 |
|------|---------|---------|----------|
| 탱커 | 블로킹 시간 +20% | 블로킹 수 +1 | 피격 시 주변 적 도발 |
| 근딜 | 돌진 데미지 +15% | 돌진 쿨다운 -20% | 처치 시 즉시 재돌진 |
| 원딜 | 사거리 +10% | 크리티컬 +5% | 관통 공격 (적 2체 히트) |
| 서포터 | 버프 범위 +15% | 버프 중첩 가능 | 전장 전체 미니 힐 |
| 컨트롤러 | CC 확률 +10% | CC 지속 +25% | CC 면역 적에게도 50% 효과 |

### 6.3 시너지 중첩

속성 시너지와 역할 시너지는 독립적으로 동시 발동.

```
예시: Fire 탱커 2 + Fire 근딜 1 + Lightning 원딜 2

발동 시너지:
  ✅ Fire 속성 3개 → ATK +10%, 화상 연장
  ✅ Lightning 속성 2개 → 공속 +5%
  ✅ 탱커 역할 2개 → 블로킹 시간 +20%
  ✅ 원딜 역할 2개 → 사거리 +10%
```

### 6.4 속성 상성 (기존 유지)

```
Fire → Frost → Wind → Lightning → Poison → Fire
유리: 1.2배 / 불리: 0.85배 / Support: 상성 없음
```

### 6.5 시너지 카운트 규칙

- **일반 유닛**: 속성 시너지 + 역할 시너지 모두 카운트
- **히든 유닛 (듀얼 패밀리)**: 두 속성 모두 각각 1회씩 카운트. 예: 뇌염의 기사(Fire+Lightning)는 Fire 시너지에 +1, Lightning 시너지에 +1. 역할 시너지도 정상 카운트
- **특수 유닛**: 속성/역할 시너지 카운트에서 모두 제외

---

## 7. 히든 유닛 합성 시스템 (RecipeSystem)

### 7.1 기존 합성과의 구분

```
기존 합성 (MergeSystem): 동일 유닛 3개 → 1등급 진화
히든 합성 (RecipeSystem): 서로 다른 유닛 2~3개 → 히든 유닛
```

### 7.2 합성 트리거 및 MergeSystem 충돌 해결

드래그 이벤트 처리 우선순위:
1. **RecipeSystem 먼저 체크** — 드래그된 두 유닛이 히든 레시피에 매칭되는지 확인
2. **매칭되면** → 합성 확인 팝업 (MergeSystem 스킵)
3. **매칭 안 되면** → MergeSystem 체크 (동일 유닛 3개 합성)
4. **둘 다 아니면** → 단순 위치 교환

3재료 레시피:
- A→B 위에 올리면 RecipeSystem이 "2재료 부분 매칭" 확인
- 부분 매칭 성공 → "재료 대기" 상태 진입 (UI에 대기 표시)
- 대기 상태에서 C를 올리면 완성
- 대기 상태에서 비매칭 유닛을 올리면 대기 해제 → 일반 드래그 처리
- 대기 상태는 웨이브 종료 시 자동 해제

### 7.3 레시피 데이터 구조

```kotlin
data class HiddenRecipe(
    val id: String,
    val ingredients: List<RecipeSlot>,
    val resultId: String,
    val discovered: Boolean
)

data class RecipeSlot(
    val family: UnitFamily?,      // null이면 아무 속성 허용
    val role: UnitRole?,          // null이면 아무 역할 허용
    val minGrade: UnitGrade,      // 최소 등급 요구
    val specificUnitId: String?   // non-null이면 이 값만으로 매칭 (family/role 무시)
)
```

**RecipeSlot 매칭 규칙:**
- `specificUnitId`가 non-null이면: 해당 유닛 ID와 정확히 일치해야 매칭 (family/role 필드 무시)
- `specificUnitId`가 null이면: family(non-null 시 일치 필요) AND role(non-null 시 일치 필요) AND grade >= minGrade 모두 만족해야 매칭

### 7.4 발견 시스템

- 완전 블라인드: 게임 내 힌트 없음
- 첫 합성 성공 시 "히든 유닛 발견!" 연출
- 도감: 미발견 = 실루엣, 발견 후 = 풀 이미지 + 레시피 기록
- 발견 상태는 GameRepository에 저장

---

## 8. 특수 유닛 시스템

### 8.1 획득 방식

- 일반 소환 1% 미만 확률
- 특수 던전 클리어 보상
- 높은 웨이브(50+) 달성 보상

### 8.2 배치 제한

- 전장 동시 최대 2체
- 같은 특수 유닛 1체만
- 제한 초과 시: 소환은 가능하지만 그리드 배치 시 "특수 유닛 최대 배치 초과" 경고 팝업 → 배치 거부

### 8.3 행동 패턴

특수 유닛은 전투 참여자가 아니므로 UnitBehavior를 상속하지 않고 별도 인터페이스를 사용한다 (컴포지션).

```kotlin
interface FieldEffectController {
    fun onPlace(unit: GameUnit, field: BattleField)   // 배치 시 초기화
    fun update(dt: Float, field: BattleField)          // 매 프레임 효과 갱신
    fun onRemove()                                      // 제거 시 정리
    fun getEffectRange(): Float
    fun canStack(): Boolean
    fun reset()                                         // 오브젝트 풀 반환 시 초기화
}
```

GameUnit에서 특수 유닛 판별: `unitCategory == SPECIAL`이면 `behavior` 대신 `fieldController: FieldEffectController?`를 사용.

```kotlin
enum class FieldEffectType {
    BARRIER,       // 결계 (방어 증가 영역)
    PATH_SLOW,     // 경로 감속 (PATH_ALTER 대체 — 경로 자체를 변경하지 않고, 특정 구간의 적 이동속도를 -70% 감속. 런타임 경로 변경의 복잡도를 회피)
    TIME_WARP,     // 시간 왜곡 (감속 영역)
    SUMMON_FIELD,  // 소환 영역
    FORGE,         // 강화 영역 (유닛 버프)
    ALCHEMY,       // 연금 영역 (골드/SP 보너스)
    DISPEL,        // 디스펠 영역 (적 버프 제거)
    TOTEM,         // 토템 (선택형 버프)
}
```

---

## 9. GameUnit 런타임 재설계

```kotlin
class GameUnit {
    // 식별
    var alive: Boolean
    var blueprintId: String
    var grade: UnitGrade
    var families: List<UnitFamily>      // 듀얼 패밀리 지원
    var role: UnitRole
    var attackRange: AttackRange
    var damageType: DamageType
    var unitCategory: UnitCategory

    // 스탯
    var hp: Float
    var maxHp: Float
    var baseATK: Float
    var atkSpeed: Float
    var range: Float
    var defense: Float
    var magicResist: Float              // 마법 저항력
    var moveSpeed: Float
    var blockCount: Int

    // 위치
    var tileIndex: Int
    var position: Vec2
    var homePosition: Vec2

    // 전장 참조 (배치 시 주입)
    var battleField: BattleField? = null

    // 행동 (Strategy 패턴) — 역할 전용 상태는 Behavior 내부에서 관리
    var behavior: UnitBehavior?                    // 일반/히든 유닛용
    var fieldController: FieldEffectController?    // 특수 유닛용
    var state: UnitState  // 아래 UnitState enum 참조

    // 기존 유지
    var buffs: BuffContainer
    var uniqueAbilityType: Int
    var uniqueAbilityCooldown: Float
    var uniqueAbilityMaxCd: Float
    var passiveCounter: Int
    var currentTarget: Enemy?

    fun update(dt: Float, findEnemy: EnemyFinder) {
        if (unitCategory == UnitCategory.SPECIAL) {
            fieldController?.update(dt, battleField)
        } else {
            behavior?.update(this, dt, findEnemy)
        }
    }

    fun takeDamage(damage: Float, isMagic: Boolean) {
        behavior?.onTakeDamage(this, damage, isMagic)
    }

    fun reset() {
        behavior?.reset()
        fieldController?.reset()
        behavior = null
        fieldController = null
        battleField = null
        // ... 기존 필드 초기화
    }
}

enum class UnitState {
    IDLE,        // 대기
    MOVING,      // 이동 중 (타겟 추적 또는 경로 이동)
    ATTACKING,   // 공격 중
    BLOCKING,    // 블로킹 중 (탱커)
    DASHING,     // 돌진 중 (근딜)
    RETURNING,   // 홈타일 복귀 중
    DEAD,        // 사망 (탱커: 블로킹 해제됨)
    RESPAWNING,  // 부활 쿨다운 중
}
```

### 기존 대비 핵심 변경

| 항목 | 기존 | 신규 |
|------|------|------|
| 식별 | unitDefId: Int | blueprintId: String |
| 패밀리 | family: Int (단일) | families: List (듀얼 지원) |
| 생존 | 없음 (불사) | HP/방어력/마법저항/사망/부활 |
| 행동 | update() 내 하드코딩 | UnitBehavior 또는 FieldEffectController 위임 |
| 상태 | isAttacking boolean | UnitState enum |
| 역할 상태 | GameUnit에 직접 | Behavior 내부 캡슐화 |
| 풀 반환 | 기본 초기화 | reset()에서 behavior/fieldController도 초기화 |

---

## 10. 데이터 외부화

### 10.1 파일 구조

```
assets/
  units/
    blueprints.json      — 일반 유닛 ~70종
    hidden_recipes.json   — 히든 레시피 ~18종
    special_units.json    — 특수 유닛 ~10종
    behaviors.json        — 행동 패턴 매핑
```

### 10.2 장점

- 유닛 추가/밸런스 조정이 코드 수정 없이 가능
- 서버 기반 데이터 배포 전환 용이
- 모딩/테스트 편의성

---

## 11. 영향받는 기존 시스템

| 시스템 | 변경 수준 | 내용 |
|--------|----------|------|
| BattleEngine | 대폭 수정 | Behavior 위임, 블로킹/돌진, 필드 이펙트 |
| Enemy | **대폭 수정** | blockedBy 상태, 블로킹 시 경로 중간 지점에서 정지/재개, 탱커 공격 로직 |
| Grid | 중간 수정 | 드래그 시 히든 레시피 매칭 |
| MergeSystem | 중간 수정 | Int→String ID 전환 (모든 lookup 경로 변경), BlueprintRegistry 연동 |
| SynergySystem | 대폭 수정 | 역할 시너지 추가, 이중 계산 |
| DamageCalculator | 중간 수정 | 물리/마법 분기 명확화, 유닛 방어력 |
| Projectile | 소폭 수정 | 즉발 데미지 경로 추가 |
| AbilitySystem | 소폭 수정 | 역할별 능력 타입 |
| UniqueAbilitySystem | 중간 수정 | 새 유닛 고유 능력 |
| UnitDetailPopup | 대폭 수정 | 역할/사거리/데미지유형 표시 |
| BuyUnitSheet | 중간 수정 | 역할 필터, 특수 유닛 |
| UnitCollectionScreen | 대폭 수정 | 히든 도감, 역할 필터 |
| GameRepository | 중간 수정 | JSON 로더, 발견 상태 저장 |

### 11.1 Object Pooling 조정

| 풀 | 기존 | 신규 | 이유 |
|----|------|------|------|
| GameUnit | 64 | 80 | 소환술사 미니 유닛 |
| Enemy | 256 | 256 | 변경 없음 |
| Projectile | 512 | 512 | 변경 없음 |
| ZoneEffect | 32 | 64 | 특수 유닛 필드 이펙트 |

---

## 12. 신규 시스템

| 시스템 | 역할 |
|--------|------|
| RecipeSystem | 히든 유닛 레시피 매칭, 합성 처리 |
| BlueprintRegistry | JSON에서 유닛 데이터 로드, 조회 |
| BehaviorFactory | blueprintId → UnitBehavior 인스턴스 생성 |
| FieldEffectManager | 특수 유닛 전장 효과 관리, 렌더링 |
| UnitCategoryFilter | UI에서 속성/역할/등급 필터링 |
