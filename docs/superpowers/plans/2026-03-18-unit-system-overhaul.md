# 유닛 시스템 대규모 개편 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** JayGame의 유닛 시스템을 4축 분류(속성/사거리/데미지유형/역할), 히든/특수 유닛, 이중 시너지, Strategy 패턴 기반으로 전면 재설계한다.

**Architecture:** 기존 `UnitDef` + `GameUnit` 하드코딩 구조를 `UnitBlueprint` + `UnitBehavior` Strategy 패턴으로 전환. 유닛 데이터를 JSON으로 외부화하고, `BlueprintRegistry`가 로드/조회를 담당. 히든 유닛은 `RecipeSystem`, 특수 유닛은 `FieldEffectController`로 분리.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, JSON (org.json — Android 내장)

**Spec:** `docs/superpowers/specs/2026-03-18-unit-system-overhaul-design.md`

**Base path:** `app/src/main/java/com/example/jaygame`
**Test path:** `app/src/test/java/com/example/jaygame`

---

## File Structure

### 신규 파일

| 파일 | 책임 |
|------|------|
| `engine/UnitBlueprint.kt` | UnitBlueprint, UnitStats, UnitCategory, AbilityDef, UniqueAbilityDef, AbilityType data classes |
| `engine/UnitRole.kt` | UnitRole enum |
| `engine/AttackRange.kt` | AttackRange enum |
| `engine/UnitState.kt` | UnitState enum |
| `engine/UnitBehavior.kt` | UnitBehavior interface |
| `engine/FieldEffectController.kt` | FieldEffectController interface, FieldEffectType enum |
| `engine/behavior/TankBlockerBehavior.kt` | 탱커 블로킹 행동 |
| `engine/behavior/AssassinDashBehavior.kt` | 근딜 돌진 행동 |
| `engine/behavior/RangedShooterBehavior.kt` | 원딜 사격 행동 |
| `engine/behavior/SupportAuraBehavior.kt` | 서포터 버프 행동 |
| `engine/behavior/ControllerCCBehavior.kt` | 컨트롤러 CC 행동 |
| `engine/behavior/BehaviorFactory.kt` | behaviorId → UnitBehavior 매핑 |
| `engine/BlueprintRegistry.kt` | JSON 로드, 블루프린트 조회 |
| `engine/RecipeSystem.kt` | 히든 유닛 레시피 매칭/합성 |
| `engine/FieldEffectManager.kt` | 특수 유닛 전장 효과 관리 |
| `engine/RoleSynergySystem.kt` | 역할 시너지 계산 |
| `engine/fieldeffect/BarrierEffect.kt` | 결계 효과 구현 |
| `engine/fieldeffect/PathSlowEffect.kt` | 경로 감속 효과 구현 |
| `engine/fieldeffect/TimeWarpEffect.kt` | 시간 왜곡 효과 구현 |
| `engine/fieldeffect/ForgeEffect.kt` | 강화 영역 효과 구현 |
| `app/src/main/assets/units/blueprints.json` | 일반 유닛 ~70종 + 히든 유닛 ~18종 데이터 |
| `app/src/main/assets/units/hidden_recipes.json` | 히든 레시피 ~18종 |
| `app/src/main/assets/units/special_units.json` | 특수 유닛 ~10종 |

> **NOTE:** 스펙의 `behaviors.json`은 별도 파일로 분리하지 않음. 행동 매핑은 `BehaviorFactory`에서 코드로 관리 — behaviorId가 5종으로 제한적이므로 JSON 외부화 대비 코드 관리가 더 명확함.
> **NOTE:** 스펙의 `UnitCategoryFilter`는 별도 클래스로 분리하지 않음. UI 컴포저블 내에서 `BlueprintRegistry` 쿼리 메서드를 직접 호출하여 필터링.

### 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `engine/GameUnit.kt` | 전면 재설계 — families, role, UnitState, behavior/fieldController 위임, HP/defense/magicResist, reset() |
| `engine/GameEngine.kt` | DamageType enum을 PHYSICAL/MAGIC으로 통합 |
| `engine/Enemy.kt` | blockedBy 상태, 블로킹 시 경로 정지/재개, 탱커 공격 |
| `engine/BattleEngine.kt` | 유닛 업데이트 루프 Behavior 위임, 특수 유닛 FieldEffect, 드래그 우선순위 |
| `engine/SynergySystem.kt` | 듀얼 패밀리 카운팅, 특수 유닛 제외 |
| `engine/MergeSystem.kt` | Int→String ID, BlueprintRegistry 연동 |
| `engine/DamageCalculator.kt` | 유닛 defense/magicResist 적용, 물리/마법 분기 명확화 |
| `engine/Grid.kt` | 드래그 시 RecipeSystem 우선 체크 |
| `engine/ObjectPool.kt` | GameUnit 풀 80, ZoneEffect 풀 64 |
| `engine/UnitGrade.kt` | summonWeight 프로퍼티 제거 (블루프린트로 이전) |
| `engine/UnitRegistry.kt` | BlueprintRegistry로 대체 후 제거 |
| `engine/UnitSpec.kt` | AbilityDef로 대체 후 제거 |
| `data/UnitDefs.kt` | UnitFamily enum 유지, UnitDef/UNIT_DEFS 제거 (JSON 마이그레이션) |
| `data/GameRepository.kt` | 히든 레시피 발견 상태 저장/로드 |
| `bridge/BattleBridge.kt` | 새 유닛 시스템 연동 |
| `ui/battle/UnitDetailPopup.kt` | 역할/사거리/데미지유형 표시 |
| `ui/battle/BattleField.kt` | 탱커 블로킹/근딜 돌진 렌더링, 필드 이펙트 렌더링, 특수 유닛 배치 거부 팝업 |
| `ui/battle/BuyUnitSheet.kt` | 역할 필터, 특수 유닛 표시 |
| `ui/screens/UnitCollectionScreen.kt` | 히든 도감, 역할별 필터 |

---

## Task 1: 새 Enum 및 데이터 모델 정의

**Files:**
- Create: `engine/UnitRole.kt`
- Create: `engine/AttackRange.kt`
- Create: `engine/UnitState.kt`
- Create: `engine/UnitBlueprint.kt`
- Modify: `engine/GameEngine.kt` (DamageType enum)
- Test: `test/java/com/example/jaygame/engine/UnitBlueprintTest.kt`

- [ ] **Step 1: UnitRole enum 생성**

```kotlin
// engine/UnitRole.kt
package com.example.jaygame.engine

enum class UnitRole(val label: String) {
    TANK("탱커"),
    MELEE_DPS("근딜"),
    RANGED_DPS("원딜"),
    SUPPORT("서포터"),
    CONTROLLER("컨트롤러"),
}
```

- [ ] **Step 2: AttackRange enum 생성**

```kotlin
// engine/AttackRange.kt
package com.example.jaygame.engine

enum class AttackRange(val label: String) {
    MELEE("근거리"),
    RANGED("원거리"),
}
```

- [ ] **Step 3: UnitState enum 생성**

```kotlin
// engine/UnitState.kt
package com.example.jaygame.engine

enum class UnitState {
    IDLE, MOVING, ATTACKING, BLOCKING, DASHING, RETURNING, DEAD, RESPAWNING
}
```

- [ ] **Step 4: 기존 DamageType enum 수정 확인**

`engine/GameEngine.kt`의 기존 `DamageType`을 확인하고 `PHYSICAL`, `MAGIC` 두 값이 있는지 검증. 없으면 추가, `MAGICAL`이 있다면 `MAGIC`으로 변경. 기존 참조 전부 업데이트.

- [ ] **Step 4.5: UnitGrade.kt에서 summonWeight 제거**

`engine/UnitGrade.kt`의 `summonWeight` 프로퍼티를 제거. 이 값은 이제 `UnitBlueprint.summonWeight`에서 관리. `UnitGrade.canSummon`도 제거 (블루프린트의 `isSummonable`로 대체). 기존에 `UnitGrade.weight`를 참조하는 코드를 모두 찾아서 `BlueprintRegistry` 기반으로 전환하도록 `@Deprecated` 마킹.

- [ ] **Step 5: UnitBlueprint 및 관련 data class 생성**

```kotlin
// engine/UnitBlueprint.kt
package com.example.jaygame.engine

data class UnitBlueprint(
    val id: String,
    val name: String,
    val families: List<UnitFamily>,
    val grade: UnitGrade,
    val role: UnitRole,
    val attackRange: AttackRange,
    val damageType: DamageType,
    val stats: UnitStats,
    val behaviorId: String,
    val ability: AbilityDef?,
    val uniqueAbility: UniqueAbilityDef?,
    val mergeResultId: String?,
    val isSummonable: Boolean,
    val summonWeight: Int,
    val unitCategory: UnitCategory,
    val iconRes: Int,
    val description: String
)

data class UnitStats(
    val hp: Float,
    val baseATK: Float,
    val baseSpeed: Float,
    val range: Float,
    val defense: Float,
    val magicResist: Float,
    val moveSpeed: Float,
    val blockCount: Int
)

enum class UnitCategory { NORMAL, HIDDEN, SPECIAL }

data class AbilityDef(
    val id: String,
    val name: String,
    val type: AbilityType,
    val damageType: DamageType,
    val value: Float,
    val cooldown: Float,
    val range: Float,
    val description: String
)

enum class AbilityType { PASSIVE, ACTIVE, AURA }

data class UniqueAbilityDef(
    val id: String,
    val name: String,
    val passive: AbilityDef?,
    val active: AbilityDef?,
    val requiredGrade: UnitGrade
)
```

참고: `UnitFamily`는 `data/UnitDefs.kt`에 이미 정의되어 있음. 해당 enum은 그대로 유지하고 import하여 사용.

- [ ] **Step 6: UnitBlueprint 테스트 작성**

```kotlin
// test/.../engine/UnitBlueprintTest.kt
class UnitBlueprintTest {
    @Test
    fun `UnitBlueprint creation with single family`() {
        val stats = UnitStats(hp=100f, baseATK=10f, baseSpeed=1f, range=50f, defense=5f, magicResist=0f, moveSpeed=80f, blockCount=1)
        val bp = UnitBlueprint(
            id="fire_tank_01", name="루비 가디언", families=listOf(UnitFamily.FIRE),
            grade=UnitGrade.COMMON, role=UnitRole.TANK, attackRange=AttackRange.MELEE,
            damageType=DamageType.PHYSICAL, stats=stats, behaviorId="tank_blocker",
            ability=null, uniqueAbility=null, mergeResultId="fire_tank_02",
            isSummonable=true, summonWeight=60, unitCategory=UnitCategory.NORMAL,
            iconRes=0, description="테스트"
        )
        assertEquals("fire_tank_01", bp.id)
        assertEquals(listOf(UnitFamily.FIRE), bp.families)
        assertEquals(UnitRole.TANK, bp.role)
        assertEquals(1, bp.stats.blockCount)
    }

    @Test
    fun `UnitBlueprint dual family for hidden unit`() {
        val stats = UnitStats(hp=200f, baseATK=50f, baseSpeed=1.2f, range=60f, defense=10f, magicResist=5f, moveSpeed=100f, blockCount=0)
        val bp = UnitBlueprint(
            id="hidden_thunder_flame_knight", name="뇌염의 기사",
            families=listOf(UnitFamily.FIRE, UnitFamily.LIGHTNING),
            grade=UnitGrade.HERO, role=UnitRole.MELEE_DPS, attackRange=AttackRange.MELEE,
            damageType=DamageType.PHYSICAL, stats=stats, behaviorId="assassin_dash",
            ability=null, uniqueAbility=null, mergeResultId=null,
            isSummonable=false, summonWeight=0, unitCategory=UnitCategory.HIDDEN,
            iconRes=0, description="듀얼"
        )
        assertEquals(2, bp.families.size)
        assertTrue(bp.families.contains(UnitFamily.FIRE))
        assertTrue(bp.families.contains(UnitFamily.LIGHTNING))
        assertEquals(UnitCategory.HIDDEN, bp.unitCategory)
    }
}
```

- [ ] **Step 7: 테스트 실행**

Run: `./gradlew test --tests "com.example.jaygame.engine.UnitBlueprintTest" --info`
Expected: PASS

- [ ] **Step 8: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/UnitRole.kt \
       app/src/main/java/com/example/jaygame/engine/AttackRange.kt \
       app/src/main/java/com/example/jaygame/engine/UnitState.kt \
       app/src/main/java/com/example/jaygame/engine/UnitBlueprint.kt \
       app/src/test/java/com/example/jaygame/engine/UnitBlueprintTest.kt
git commit -m "feat: add new unit classification enums and UnitBlueprint data model"
```

---

## Task 2: UnitBehavior 인터페이스 및 BehaviorFactory

**Files:**
- Create: `engine/UnitBehavior.kt`
- Create: `engine/FieldEffectController.kt`
- Create: `engine/behavior/BehaviorFactory.kt`
- Test: `test/.../engine/behavior/BehaviorFactoryTest.kt`

- [ ] **Step 1: UnitBehavior 인터페이스 생성**

```kotlin
// engine/UnitBehavior.kt
package com.example.jaygame.engine

interface UnitBehavior {
    fun update(unit: GameUnit, dt: Float, findEnemy: (position: Vec2, range: Float) -> Enemy?)
    fun onAttack(unit: GameUnit, target: Enemy): AttackResult
    fun onTakeDamage(unit: GameUnit, damage: Float, isMagic: Boolean)
    fun reset()
}

data class AttackResult(
    val damage: Float,
    val isMagic: Boolean,
    val isCrit: Boolean,
    val isInstant: Boolean  // true = 즉발(근거리), false = 프로젝타일
)
```

- [ ] **Step 2: FieldEffectController 인터페이스 생성**

```kotlin
// engine/FieldEffectController.kt
package com.example.jaygame.engine

interface FieldEffectController {
    fun onPlace(unit: GameUnit, field: BattleField)
    fun update(dt: Float, field: BattleField)
    fun onRemove()
    fun getEffectRange(): Float
    fun canStack(): Boolean
    fun reset()
}

// BattleField는 engine/BattleEngine.kt 내 전장 상태를 노출하는 인터페이스
// enemies, allies, grid 등에 접근 가능

enum class FieldEffectType {
    BARRIER, PATH_SLOW, TIME_WARP, SUMMON_FIELD, FORGE, ALCHEMY, DISPEL, TOTEM
}
```

- [ ] **Step 3: BehaviorFactory 생성**

```kotlin
// engine/behavior/BehaviorFactory.kt
package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.UnitBehavior

object BehaviorFactory {
    private val registry = mutableMapOf<String, () -> UnitBehavior>()

    fun register(behaviorId: String, factory: () -> UnitBehavior) {
        registry[behaviorId] = factory
    }

    fun create(behaviorId: String): UnitBehavior {
        val factory = registry[behaviorId]
            ?: throw IllegalArgumentException("Unknown behaviorId: $behaviorId")
        return factory()
    }

    fun isRegistered(behaviorId: String): Boolean = behaviorId in registry

    // 테스트용 초기화
    fun clearForTesting() { registry.clear() }
}
```

- [ ] **Step 4: BehaviorFactory 테스트 작성**

```kotlin
class BehaviorFactoryTest {
    @Before
    fun setup() { BehaviorFactory.clearForTesting() }

    @Test
    fun `create returns registered behavior`() {
        val mockBehavior = object : UnitBehavior { /* stub implementations */ }
        BehaviorFactory.register("test_behavior") { mockBehavior }
        val result = BehaviorFactory.create("test_behavior")
        assertSame(mockBehavior, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `create throws for unknown behaviorId`() {
        BehaviorFactory.create("nonexistent")
    }

    @Test
    fun `isRegistered returns correct values`() {
        assertFalse(BehaviorFactory.isRegistered("test"))
        BehaviorFactory.register("test") { /* stub */ }
        assertTrue(BehaviorFactory.isRegistered("test"))
    }
}
```

- [ ] **Step 5: 테스트 실행**

Run: `./gradlew test --tests "com.example.jaygame.engine.behavior.BehaviorFactoryTest" --info`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/UnitBehavior.kt \
       app/src/main/java/com/example/jaygame/engine/FieldEffectController.kt \
       app/src/main/java/com/example/jaygame/engine/behavior/BehaviorFactory.kt \
       app/src/test/java/com/example/jaygame/engine/behavior/BehaviorFactoryTest.kt
git commit -m "feat: add UnitBehavior interface, FieldEffectController, and BehaviorFactory"
```

---

## Task 3: BlueprintRegistry — JSON 로드 및 조회

**Files:**
- Create: `engine/BlueprintRegistry.kt`
- Create: `assets/units/blueprints.json` (초기 데이터 — Fire 탱커/근딜/원딜 각 2종씩, 6종으로 시작)
- Test: `test/.../engine/BlueprintRegistryTest.kt`

- [ ] **Step 1: 테스트용 JSON 작성**

테스트 리소스(`test/resources/test_blueprints.json`)에 최소 유닛 3종 정의.

```json
[
  {
    "id": "fire_tank_01",
    "name": "화염 수호자",
    "families": ["FIRE"],
    "grade": "COMMON",
    "role": "TANK",
    "attackRange": "MELEE",
    "damageType": "PHYSICAL",
    "stats": { "hp": 150, "baseATK": 8, "baseSpeed": 0.8, "range": 40, "defense": 10, "magicResist": 3, "moveSpeed": 70, "blockCount": 1 },
    "behaviorId": "tank_blocker",
    "ability": null,
    "uniqueAbility": null,
    "mergeResultId": "fire_tank_02",
    "isSummonable": true,
    "summonWeight": 60,
    "unitCategory": "NORMAL",
    "iconRes": 0,
    "description": "화염의 수호자"
  }
]
```

- [ ] **Step 2: BlueprintRegistry 테스트 작성**

```kotlin
class BlueprintRegistryTest {
    @Test
    fun `loadFromJson parses blueprints correctly`() {
        val json = /* test JSON string */
        val registry = BlueprintRegistry()
        registry.loadFromJson(json)
        val bp = registry.findById("fire_tank_01")
        assertNotNull(bp)
        assertEquals("화염 수호자", bp!!.name)
        assertEquals(UnitRole.TANK, bp.role)
        assertEquals(150f, bp.stats.hp)
    }

    @Test
    fun `findById returns null for unknown id`() {
        val registry = BlueprintRegistry()
        assertNull(registry.findById("nonexistent"))
    }

    @Test
    fun `findByFamilyAndRole returns matching blueprints`() {
        val registry = BlueprintRegistry()
        registry.loadFromJson(/* json with fire_tank and fire_melee */)
        val tanks = registry.findByFamilyAndRole(UnitFamily.FIRE, UnitRole.TANK)
        assertTrue(tanks.isNotEmpty())
        assertTrue(tanks.all { it.role == UnitRole.TANK })
    }

    @Test
    fun `findSummonable returns only summonable units`() {
        val registry = BlueprintRegistry()
        registry.loadFromJson(/* json with summonable and non-summonable */)
        val summonable = registry.findSummonable()
        assertTrue(summonable.all { it.isSummonable })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `loadFromJson throws for unknown behaviorId when validation enabled`() {
        // behaviorId가 BehaviorFactory에 등록 안 된 경우
    }
}
```

- [ ] **Step 3: BlueprintRegistry 구현**

```kotlin
// engine/BlueprintRegistry.kt
package com.example.jaygame.engine

import org.json.JSONArray
import org.json.JSONObject

class BlueprintRegistry {
    private val blueprints = mutableMapOf<String, UnitBlueprint>()

    fun loadFromJson(jsonString: String) {
        val arr = JSONArray(jsonString)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val bp = parseBlueprint(obj)
            blueprints[bp.id] = bp
        }
    }

    fun findById(id: String): UnitBlueprint? = blueprints[id]
    fun findByFamilyAndRole(family: UnitFamily, role: UnitRole): List<UnitBlueprint> =
        blueprints.values.filter { family in it.families && it.role == role }
    fun findSummonable(): List<UnitBlueprint> =
        blueprints.values.filter { it.isSummonable }
    fun findByCategory(category: UnitCategory): List<UnitBlueprint> =
        blueprints.values.filter { it.unitCategory == category }
    fun all(): List<UnitBlueprint> = blueprints.values.toList()

    private fun parseBlueprint(obj: JSONObject): UnitBlueprint {
        val familiesArr = obj.getJSONArray("families")
        val families = (0 until familiesArr.length()).map { UnitFamily.valueOf(familiesArr.getString(it)) }
        val statsObj = obj.getJSONObject("stats")
        val stats = UnitStats(
            hp = statsObj.getDouble("hp").toFloat(),
            baseATK = statsObj.getDouble("baseATK").toFloat(),
            baseSpeed = statsObj.getDouble("baseSpeed").toFloat(),
            range = statsObj.getDouble("range").toFloat(),
            defense = statsObj.getDouble("defense").toFloat(),
            magicResist = statsObj.getDouble("magicResist").toFloat(),
            moveSpeed = statsObj.getDouble("moveSpeed").toFloat(),
            blockCount = statsObj.getInt("blockCount")
        )
        val ability = if (obj.isNull("ability")) null else parseAbilityDef(obj.getJSONObject("ability"))
        val uniqueAbility = if (obj.isNull("uniqueAbility")) null else parseUniqueAbilityDef(obj.getJSONObject("uniqueAbility"))
        return UnitBlueprint(
            id = obj.getString("id"),
            name = obj.getString("name"),
            families = families,
            grade = UnitGrade.valueOf(obj.getString("grade")),
            role = UnitRole.valueOf(obj.getString("role")),
            attackRange = AttackRange.valueOf(obj.getString("attackRange")),
            damageType = DamageType.valueOf(obj.getString("damageType")),
            stats = stats,
            behaviorId = obj.getString("behaviorId"),
            ability = ability,
            uniqueAbility = uniqueAbility,
            mergeResultId = if (obj.isNull("mergeResultId")) null else obj.getString("mergeResultId"),
            isSummonable = obj.getBoolean("isSummonable"),
            summonWeight = obj.getInt("summonWeight"),
            unitCategory = UnitCategory.valueOf(obj.getString("unitCategory")),
            iconRes = obj.optInt("iconRes", 0),
            description = obj.getString("description")
        )
    }

    private fun parseAbilityDef(obj: JSONObject): AbilityDef = AbilityDef(
        id = obj.getString("id"), name = obj.getString("name"),
        type = AbilityType.valueOf(obj.getString("type")),
        damageType = DamageType.valueOf(obj.getString("damageType")),
        value = obj.getDouble("value").toFloat(),
        cooldown = obj.getDouble("cooldown").toFloat(),
        range = obj.getDouble("range").toFloat(),
        description = obj.getString("description")
    )

    private fun parseUniqueAbilityDef(obj: JSONObject): UniqueAbilityDef = UniqueAbilityDef(
        id = obj.getString("id"), name = obj.getString("name"),
        passive = if (obj.isNull("passive")) null else parseAbilityDef(obj.getJSONObject("passive")),
        active = if (obj.isNull("active")) null else parseAbilityDef(obj.getJSONObject("active")),
        requiredGrade = UnitGrade.valueOf(obj.getString("requiredGrade"))
    )
}
```

- [ ] **Step 4: 테스트 실행**

Run: `./gradlew test --tests "com.example.jaygame.engine.BlueprintRegistryTest" --info`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/BlueprintRegistry.kt \
       app/src/test/java/com/example/jaygame/engine/BlueprintRegistryTest.kt
git commit -m "feat: add BlueprintRegistry with JSON loading and query methods"
```

---

## Task 4: GameUnit 재설계

**Files:**
- Modify: `engine/GameUnit.kt` (전면 재설계)
- Test: `test/.../engine/GameUnitTest.kt`

- [ ] **Step 1: GameUnit 재설계 테스트 작성**

```kotlin
class GameUnitTest {
    @Test
    fun `init from blueprint sets all fields correctly`() { /* ... */ }

    @Test
    fun `reset clears all fields and calls behavior reset`() { /* ... */ }

    @Test
    fun `update delegates to behavior for normal units`() { /* ... */ }

    @Test
    fun `update delegates to fieldController for special units`() { /* ... */ }

    @Test
    fun `takeDamage delegates to behavior with isMagic flag`() { /* ... */ }

    @Test
    fun `takeDamage reduces hp by physical formula`() { /* ... */ }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

Run: `./gradlew test --tests "com.example.jaygame.engine.GameUnitTest" --info`
Expected: FAIL (GameUnit에 새 필드/메서드 없음)

- [ ] **Step 3: GameUnit.kt 재설계 구현**

기존 `GameUnit.kt` (158줄)를 새 구조로 전면 교체. 핵심 변경:
- `unitDefId: Int` → `blueprintId: String`
- `family: Int` → `families: List<UnitFamily>`
- `isAttacking: Boolean` → `state: UnitState`
- 신규 필드: `role`, `attackRange`, `damageType`, `unitCategory`, `hp`, `maxHp`, `defense`, `magicResist`, `moveSpeed`, `blockCount`, `behavior`, `fieldController`, `battleField`
- `update()`: unitCategory에 따라 behavior 또는 fieldController에 위임
- `takeDamage()`: isMagic 포함하여 behavior에 위임
- `reset()`: behavior/fieldController reset 호출 후 null 설정
- `initFromBlueprint(bp: UnitBlueprint)`: 블루프린트에서 초기화하는 헬퍼

**주의:** 기존 `update()`, `canAttack()`, `onAttack()` 로직은 `RangedShooterBehavior`로 이전될 것이므로 제거하되, 컴파일 에러를 방지하기 위해 BattleEngine 수정(Task 8) 전까지는 기존 메서드를 `@Deprecated`로 유지.

- [ ] **Step 4: 테스트 실행 — 통과 확인**

Run: `./gradlew test --tests "com.example.jaygame.engine.GameUnitTest" --info`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/GameUnit.kt \
       app/src/test/java/com/example/jaygame/engine/GameUnitTest.kt
git commit -m "feat: redesign GameUnit with Strategy pattern, HP, roles, and dual family"
```

---

## Task 5: Behavior 구현 — RangedShooterBehavior (기존 로직 이전)

**Files:**
- Create: `engine/behavior/RangedShooterBehavior.kt`
- Test: `test/.../engine/behavior/RangedShooterBehaviorTest.kt`

- [ ] **Step 1: 테스트 작성**

```kotlin
class RangedShooterBehaviorTest {
    @Test
    fun `update finds enemy in range and sets ATTACKING state`() { /* ... */ }

    @Test
    fun `update returns to IDLE when no enemy in range`() { /* ... */ }

    @Test
    fun `onAttack returns projectile-based AttackResult`() { /* ... */ }

    @Test
    fun `reset clears internal state`() { /* ... */ }
}
```

- [ ] **Step 2: RangedShooterBehavior 구현**

기존 `GameUnit.update()`의 탐색/추적/공격 로직을 이 클래스로 이전.
- 탐색: `findEnemy(position, range * 1.5f)`
- 공격: `attackCooldown` 관리, `canAttack()` → `AttackResult(isInstant=false)`
- 상태: IDLE ↔ MOVING ↔ ATTACKING

- [ ] **Step 3: 테스트 실행**

Run: `./gradlew test --tests "com.example.jaygame.engine.behavior.RangedShooterBehaviorTest" --info`
Expected: PASS

- [ ] **Step 4: BehaviorFactory에 등록**

```kotlin
BehaviorFactory.register("ranged_shooter") { RangedShooterBehavior() }
BehaviorFactory.register("ranged_mage") { RangedShooterBehavior(aoe = true) }
```

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/behavior/RangedShooterBehavior.kt \
       app/src/test/java/com/example/jaygame/engine/behavior/RangedShooterBehaviorTest.kt
git commit -m "feat: add RangedShooterBehavior — migrate existing unit attack logic"
```

---

## Task 6: Behavior 구현 — TankBlockerBehavior

**Files:**
- Create: `engine/behavior/TankBlockerBehavior.kt`
- Modify: `engine/Enemy.kt` (blockedBy 상태 추가)
- Test: `test/.../engine/behavior/TankBlockerBehaviorTest.kt`

- [ ] **Step 1: Enemy에 블로킹 상태 추가 테스트**

```kotlin
class EnemyBlockingTest {
    @Test
    fun `setBlockedBy stops enemy movement`() { /* ... */ }

    @Test
    fun `releaseBlock resumes enemy movement from blocked position`() { /* ... */ }

    @Test
    fun `enemy attacks blocker when blocked`() { /* ... */ }
}
```

- [ ] **Step 2: Enemy.kt 수정**

```kotlin
// Enemy.kt에 추가
var blockedBy: GameUnit? = null
var blockedPosition: Vec2? = null  // 블로킹 시점의 경로 위치 저장

fun setBlockedBy(blocker: GameUnit) {
    blockedBy = blocker
    blockedPosition = position.copy()
}

fun releaseBlock() {
    blockedBy = null
    blockedPosition = null
    // 현재 위치에서 경로 재개 (pathIndex 유지)
}
```

`update()` 메서드에서 `blockedBy != null`이면 이동 스킵, 대신 blocker를 공격.

- [ ] **Step 3: TankBlockerBehavior 테스트 작성**

```kotlin
class TankBlockerBehaviorTest {
    @Test
    fun `tank moves to enemy path and blocks`() { /* ... */ }

    @Test
    fun `blocked enemies stop moving`() { /* ... */ }

    @Test
    fun `tank respects blockCount limit`() { /* ... */ }

    @Test
    fun `tank death releases all blocked enemies in same frame`() { /* ... */ }

    @Test
    fun `tank enters RESPAWNING after death`() { /* ... */ }

    @Test
    fun `boss breaks free after 5 seconds`() { /* ... */ }

    @Test
    fun `reset clears blockedEnemies and respawnTimer`() { /* ... */ }
}
```

- [ ] **Step 4: TankBlockerBehavior 구현**

```kotlin
class TankBlockerBehavior : UnitBehavior {
    val blockedEnemies = mutableListOf<Enemy>()
    var respawnTimer = 0f
    private val BOSS_BLOCK_DURATION = 5f
    private val RESPAWN_COOLDOWN = 3f

    override fun update(unit: GameUnit, dt: Float, findEnemy: ...) {
        when (unit.state) {
            IDLE -> { /* 적 경로에서 가장 가까운 적 탐색, MOVING으로 전환 */ }
            MOVING -> { /* 적 경로 위치로 이동, 도착하면 BLOCKING */ }
            BLOCKING -> {
                // blockCount까지 새 적 블로킹
                // 블로킹된 적이 탱커 공격 → takeDamage
                // HP <= 0 → 모든 적 해제, DEAD 전환
            }
            DEAD -> { unit.state = RESPAWNING; respawnTimer = RESPAWN_COOLDOWN }
            RESPAWNING -> {
                respawnTimer -= dt
                if (respawnTimer <= 0) { unit.hp = unit.maxHp; unit.state = IDLE }
            }
        }
    }

    override fun onTakeDamage(unit: GameUnit, damage: Float, isMagic: Boolean) {
        val reduction = if (isMagic) unit.magicResist / (unit.magicResist + 100f)
                        else unit.defense / (unit.defense + 100f)
        unit.hp -= damage * (1f - reduction)
        if (unit.hp <= 0f) {
            blockedEnemies.forEach { it.releaseBlock() }
            blockedEnemies.clear()
            unit.state = UnitState.DEAD
        }
    }

    override fun reset() { blockedEnemies.clear(); respawnTimer = 0f }
}
```

- [ ] **Step 5: BehaviorFactory에 등록**

```kotlin
BehaviorFactory.register("tank_blocker") { TankBlockerBehavior() }
```

- [ ] **Step 6: 테스트 실행**

Run: `./gradlew test --tests "com.example.jaygame.engine.behavior.TankBlockerBehaviorTest" --info`
Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/behavior/TankBlockerBehavior.kt \
       app/src/main/java/com/example/jaygame/engine/Enemy.kt \
       app/src/test/java/com/example/jaygame/engine/behavior/TankBlockerBehaviorTest.kt
git commit -m "feat: add TankBlockerBehavior with enemy blocking mechanics"
```

---

## Task 7: Behavior 구현 — AssassinDash, SupportAura, ControllerCC

**Files:**
- Create: `engine/behavior/AssassinDashBehavior.kt`
- Create: `engine/behavior/SupportAuraBehavior.kt`
- Create: `engine/behavior/ControllerCCBehavior.kt`
- Test: `test/.../engine/behavior/AssassinDashBehaviorTest.kt`
- Test: `test/.../engine/behavior/SupportAuraBehaviorTest.kt`
- Test: `test/.../engine/behavior/ControllerCCBehaviorTest.kt`

- [ ] **Step 1: AssassinDashBehavior 테스트**

핵심 테스트 케이스:
- 적 감지 시 DASHING 전환
- 돌진 중 무적
- 타겟 도달 시 데미지 → RETURNING
- 돌진 중 타겟 사망 시 즉시 복귀, 쿨다운 절반
- 홈타일 도착 시 IDLE + 쿨다운 시작
- reset 호출 시 상태 초기화

- [ ] **Step 2: AssassinDashBehavior 구현**

내부 상태: `dashTarget: Enemy?`, `dashCooldown: Float`
상태 머신: IDLE → DASHING → (공격) → RETURNING → IDLE

- [ ] **Step 3: SupportAuraBehavior 테스트 및 구현**

- 범위 내 아군에 버프 적용
- 버프 종류별 효과 검증 (ATK 증가, 공속 증가, 방어 증가, HP 회복)
- 범위 밖 아군 미적용
- 직접 공격력 낮음

- [ ] **Step 4: ControllerCCBehavior 테스트 및 구현**

- 근거리형: 범위 CC (스턴/감속/넉백)
- 원거리형: CC 프로젝타일
- CC 지속시간 관리

- [ ] **Step 5: BehaviorFactory에 모두 등록**

```kotlin
BehaviorFactory.register("assassin_dash") { AssassinDashBehavior() }
BehaviorFactory.register("support_aura") { SupportAuraBehavior() }
BehaviorFactory.register("controller_cc_melee") { ControllerCCBehavior(isRanged = false) }
BehaviorFactory.register("controller_cc_ranged") { ControllerCCBehavior(isRanged = true) }
```

- [ ] **Step 6: 전체 테스트 실행**

Run: `./gradlew test --info`
Expected: ALL PASS

- [ ] **Step 7: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/behavior/ \
       app/src/test/java/com/example/jaygame/engine/behavior/
git commit -m "feat: add AssassinDash, SupportAura, ControllerCC behaviors"
```

---

## Task 8: BattleEngine 통합 — Behavior 위임

**Files:**
- Modify: `engine/BattleEngine.kt`
- Modify: `engine/DamageCalculator.kt`
- Modify: `engine/Projectile.kt`
- Test: `test/.../engine/DamageCalculatorTest.kt`

- [ ] **Step 0: DamageCalculator 테스트 작성**

```kotlin
class DamageCalculatorTest {
    @Test
    fun `physical damage applies defense reduction correctly`() {
        // defense=100 → 50% reduction
        val unit = createTestUnit(defense = 100f, magicResist = 0f)
        val result = DamageCalculator.calculateDamageToUnit(200f, isMagic = false, unit)
        assertEquals(100f, result, 0.01f)
    }

    @Test
    fun `magic damage applies magicResist reduction correctly`() {
        // magicResist=50 → 33% reduction
        val unit = createTestUnit(defense = 0f, magicResist = 50f)
        val result = DamageCalculator.calculateDamageToUnit(150f, isMagic = true, unit)
        assertEquals(100f, result, 0.01f)
    }

    @Test
    fun `zero defense results in full damage`() {
        val unit = createTestUnit(defense = 0f, magicResist = 0f)
        val result = DamageCalculator.calculateDamageToUnit(100f, isMagic = false, unit)
        assertEquals(100f, result, 0.01f)
    }
}
```

- [ ] **Step 0.5: 테스트 실행 — 실패 확인**

Run: `./gradlew test --tests "com.example.jaygame.engine.DamageCalculatorTest" --info`
Expected: FAIL (calculateDamageToUnit 없음)

- [ ] **Step 1: BattleEngine.updateUnits() 수정**

기존 하드코딩된 유닛 업데이트를 behavior 위임으로 교체:

```kotlin
private fun updateUnits(dt: Float) {
    AbilitySystem.applyAuraEffects(activeUnits, dt, auraTicks)
    UniqueAbilitySystem.update(activeUnits, dt, activeEnemies)

    units.forEach { unit ->
        if (!unit.alive) return@forEach
        unit.update(dt) { pos, range -> findNearestEnemy(pos, range) }

        // 원거리 유닛의 프로젝타일 발사는 behavior의 AttackResult로 판단
        if (unit.attackRange == AttackRange.RANGED && unit.state == UnitState.ATTACKING) {
            val result = unit.behavior?.onAttack(unit, unit.currentTarget ?: return@forEach)
            if (result != null && !result.isInstant) {
                fireProjectile(unit, result)
            }
        }
    }
}
```

- [ ] **Step 2: BattleEngine.requestSummon() 수정**

BlueprintRegistry에서 유닛 정보 조회, BehaviorFactory로 행동 주입:

```kotlin
fun requestSummon() {
    val grade = probabilityEngine.rollGrade(...)
    val summonable = blueprintRegistry.findSummonable()
        .filter { it.grade == grade }
    val bp = summonable.weightedRandom { it.summonWeight } ?: return
    val unit = unitPool.acquire() ?: return
    unit.initFromBlueprint(bp)
    unit.behavior = BehaviorFactory.create(bp.behaviorId)
    grid.placeUnit(tileIndex, unit)
}
```

- [ ] **Step 3: DamageCalculator 수정**

유닛 defense/magicResist를 고려한 데미지 계산 추가:

```kotlin
fun calculateDamageToUnit(
    rawDamage: Float, isMagic: Boolean, targetUnit: GameUnit
): Float {
    val resist = if (isMagic) targetUnit.magicResist else targetUnit.defense
    return rawDamage * (100f / (100f + resist))
}
```

- [ ] **Step 4: 테스트 + 컴파일 확인**

Run: `./gradlew test --tests "com.example.jaygame.engine.DamageCalculatorTest" --info`
Expected: PASS

Run: `./gradlew compileDebugKotlin`
Expected: SUCCESS

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/BattleEngine.kt \
       app/src/main/java/com/example/jaygame/engine/DamageCalculator.kt \
       app/src/main/java/com/example/jaygame/engine/Projectile.kt \
       app/src/test/java/com/example/jaygame/engine/DamageCalculatorTest.kt
git commit -m "feat: integrate Behavior pattern into BattleEngine update loop"
```

---

## Task 9: 시너지 시스템 확장 — 역할 시너지

**Files:**
- Create: `engine/RoleSynergySystem.kt`
- Modify: `engine/SynergySystem.kt` (듀얼 패밀리, 특수 유닛 제외)
- Test: `test/.../engine/RoleSynergySystemTest.kt`
- Test: `test/.../engine/SynergySystemTest.kt`

- [ ] **Step 1: RoleSynergySystem 테스트 작성**

```kotlin
class RoleSynergySystemTest {
    @Test
    fun `2 tanks gives blocking time bonus`() { /* ... */ }

    @Test
    fun `3 ranged gives crit bonus`() { /* ... */ }

    @Test
    fun `4 melee gives instant re-dash on kill`() { /* ... */ }

    @Test
    fun `special units are excluded from role count`() { /* ... */ }

    @Test
    fun `no bonus for single role unit`() { /* ... */ }
}
```

- [ ] **Step 2: RoleSynergySystem 구현**

```kotlin
object RoleSynergySystem {
    data class RoleSynergyBonus(
        val atkMultiplier: Float = 1f,
        val rangeMultiplier: Float = 1f,
        val critBonus: Float = 0f,
        val blockTimeBonus: Float = 0f,
        val dashDamageBonus: Float = 0f,
        val dashCooldownReduction: Float = 0f,
        val buffRangeBonus: Float = 0f,
        val ccDurationBonus: Float = 0f,
        val specialEffect: RoleSpecialEffect = RoleSpecialEffect.NONE
    )

    enum class RoleSpecialEffect {
        NONE, TAUNT_ON_HIT, INSTANT_REDASH, PENETRATE_2, BUFF_STACK, CC_HALF_ON_IMMUNE
    }

    fun getBonus(units: List<GameUnit>, role: UnitRole): RoleSynergyBonus {
        val count = units.count { it.unitCategory != UnitCategory.SPECIAL && it.role == role }
        // count별 보너스 반환 (스펙 6.2 참조)
    }
}
```

- [ ] **Step 3: SynergySystem 수정**

듀얼 패밀리 카운팅:
```kotlin
fun countFamilies(units: List<GameUnit>): Map<UnitFamily, Int> {
    val counts = mutableMapOf<UnitFamily, Int>()
    units.filter { it.unitCategory != UnitCategory.SPECIAL }.forEach { unit ->
        unit.families.forEach { family ->
            counts[family] = (counts[family] ?: 0) + 1
        }
    }
    return counts
}
```

- [ ] **Step 4: 테스트 실행**

Run: `./gradlew test --tests "com.example.jaygame.engine.*SynergySystem*" --info`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/RoleSynergySystem.kt \
       app/src/main/java/com/example/jaygame/engine/SynergySystem.kt \
       app/src/test/java/com/example/jaygame/engine/
git commit -m "feat: add role synergy system and update family synergy for dual families"
```

---

## Task 10: RecipeSystem — 히든 유닛 합성

**Files:**
- Create: `engine/RecipeSystem.kt`
- Create: `assets/units/hidden_recipes.json`
- Modify: `engine/Grid.kt` (드래그 우선순위)
- Modify: `data/GameRepository.kt` (발견 상태 저장)
- Test: `test/.../engine/RecipeSystemTest.kt`

- [ ] **Step 1: RecipeSystem 테스트 작성**

```kotlin
class RecipeSystemTest {
    @Test
    fun `matchRecipe returns recipe for valid 2-ingredient combination`() { /* ... */ }

    @Test
    fun `matchRecipe returns null for non-matching combination`() { /* ... */ }

    @Test
    fun `specificUnitId overrides family and role matching`() { /* ... */ }

    @Test
    fun `null family in slot matches any family`() { /* ... */ }

    @Test
    fun `grade below minGrade does not match`() { /* ... */ }

    @Test
    fun `partial match for 3-ingredient recipe`() { /* ... */ }

    @Test
    fun `discovery state persists`() { /* ... */ }
}
```

- [ ] **Step 2: RecipeSystem 구현**

```kotlin
class RecipeSystem(private val blueprintRegistry: BlueprintRegistry) {
    private val recipes = mutableListOf<HiddenRecipe>()
    private val discoveredIds = mutableSetOf<String>()

    fun loadRecipes(jsonString: String) { /* JSON 파싱 */ }

    fun matchRecipe(unitA: GameUnit, unitB: GameUnit): HiddenRecipe? {
        return recipes.firstOrNull { recipe ->
            recipe.ingredients.size == 2 && matchesIngredients(recipe, listOf(unitA, unitB))
        }
    }

    fun matchPartial(unitA: GameUnit, unitB: GameUnit): HiddenRecipe? {
        return recipes.firstOrNull { recipe ->
            recipe.ingredients.size == 3 && matchesPartialIngredients(recipe, listOf(unitA, unitB))
        }
    }

    fun completeRecipe(recipe: HiddenRecipe, units: List<GameUnit>): UnitBlueprint? {
        if (!matchesIngredients(recipe, units)) return null
        discoveredIds.add(recipe.id)
        return blueprintRegistry.findById(recipe.resultId)
    }

    fun isDiscovered(recipeId: String): Boolean = recipeId in discoveredIds

    private fun matchesSlot(slot: RecipeSlot, unit: GameUnit): Boolean {
        if (slot.specificUnitId != null) return unit.blueprintId == slot.specificUnitId
        val familyMatch = slot.family == null || slot.family in unit.families
        val roleMatch = slot.role == null || slot.role == unit.role
        val gradeMatch = unit.grade.tier >= slot.minGrade.tier
        return familyMatch && roleMatch && gradeMatch
    }
}
```

- [ ] **Step 3: Grid.kt 드래그 우선순위 수정**

```kotlin
fun onUnitDraggedOnto(dragged: GameUnit, target: GameUnit): DragResult {
    // 1. RecipeSystem 체크
    val recipe = recipeSystem.matchRecipe(dragged, target)
    if (recipe != null) return DragResult.RecipeMatch(recipe)

    val partial = recipeSystem.matchPartial(dragged, target)
    if (partial != null) return DragResult.PartialRecipe(partial)

    // 2. MergeSystem 체크
    if (mergeSystem.canMerge(dragged, target)) return DragResult.Merge

    // 3. 위치 교환
    return DragResult.Swap
}
```

- [ ] **Step 4: GameRepository에 발견 상태 저장 추가**

```kotlin
// GameRepository.kt에 추가
fun saveDiscoveredRecipes(ids: Set<String>) { /* SharedPreferences JSON */ }
fun loadDiscoveredRecipes(): Set<String> { /* SharedPreferences JSON */ }
```

- [ ] **Step 5: hidden_recipes.json 초기 데이터 작성**

5종의 히든 레시피를 JSON으로 정의 (스펙 5.2 예시 기반).

- [ ] **Step 6: 테스트 실행**

Run: `./gradlew test --tests "com.example.jaygame.engine.RecipeSystemTest" --info`
Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/RecipeSystem.kt \
       app/src/main/java/com/example/jaygame/engine/Grid.kt \
       app/src/main/java/com/example/jaygame/data/GameRepository.kt \
       app/src/main/assets/units/hidden_recipes.json \
       app/src/test/java/com/example/jaygame/engine/RecipeSystemTest.kt
git commit -m "feat: add RecipeSystem for hidden unit crafting with blind discovery"
```

---

## Task 11: FieldEffectManager — 특수 유닛 전장 효과

**Files:**
- Create: `engine/FieldEffectManager.kt`
- Create: `engine/fieldeffect/BarrierEffect.kt`
- Create: `engine/fieldeffect/PathSlowEffect.kt`
- Create: `engine/fieldeffect/TimeWarpEffect.kt`
- Create: `engine/fieldeffect/ForgeEffect.kt`
- Create: `assets/units/special_units.json`
- Test: `test/.../engine/FieldEffectManagerTest.kt`

- [ ] **Step 1: FieldEffectManager 테스트**

```kotlin
class FieldEffectManagerTest {
    @Test
    fun `addEffect registers and starts field effect`() { /* ... */ }

    @Test
    fun `update propagates dt to all active effects`() { /* ... */ }

    @Test
    fun `removeEffect calls onRemove and cleans up`() { /* ... */ }

    @Test
    fun `max 2 special units enforced`() { /* ... */ }

    @Test
    fun `same special unit limited to 1`() { /* ... */ }
}
```

- [ ] **Step 2: FieldEffectManager 구현**

```kotlin
class FieldEffectManager {
    private val activeEffects = mutableListOf<Pair<GameUnit, FieldEffectController>>()
    private val MAX_SPECIAL_UNITS = 2

    fun canPlace(unit: GameUnit): Boolean {
        if (unit.unitCategory != UnitCategory.SPECIAL) return true
        if (activeEffects.size >= MAX_SPECIAL_UNITS) return false
        if (activeEffects.any { it.first.blueprintId == unit.blueprintId }) return false
        return true
    }

    fun addEffect(unit: GameUnit, controller: FieldEffectController) { /* ... */ }
    fun update(dt: Float, enemies: List<Enemy>, allies: List<GameUnit>) { /* ... */ }
    fun removeEffect(unit: GameUnit) { /* ... */ }
}
```

- [ ] **Step 3: 기본 FieldEffect 구현체 4종**

각각 `FieldEffectController`를 구현:
- `BarrierEffect`: 범위 내 아군 defense +50%
- `PathSlowEffect`: 범위 내 적 이동속도 -70%
- `TimeWarpEffect`: 범위 내 적 전체 50% 슬로우
- `ForgeEffect`: 인접 아군 ATK +10% 영구 버프

- [ ] **Step 4: special_units.json 데이터 작성**

4종 특수 유닛 JSON 정의 (결계사, 차원술사, 시간의 현자, 대장장이).

- [ ] **Step 5: 테스트 실행**

Run: `./gradlew test --tests "com.example.jaygame.engine.FieldEffectManagerTest" --info`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/FieldEffectManager.kt \
       app/src/main/java/com/example/jaygame/engine/fieldeffect/ \
       app/src/main/assets/units/special_units.json \
       app/src/test/java/com/example/jaygame/engine/FieldEffectManagerTest.kt
git commit -m "feat: add FieldEffectManager and 4 special unit effects"
```

---

## Task 12: MergeSystem 마이그레이션 (Int → String ID)

**Files:**
- Modify: `engine/MergeSystem.kt`
- Test: `test/.../engine/MergeSystemTest.kt`

- [ ] **Step 1: MergeSystem 테스트**

```kotlin
class MergeSystemTest {
    @Test
    fun `tryMerge with 3 same blueprintId units returns merge result`() { /* ... */ }

    @Test
    fun `tryMerge looks up mergeResultId from BlueprintRegistry`() { /* ... */ }

    @Test
    fun `lucky merge skips one grade`() { /* ... */ }

    @Test
    fun `hidden units cannot be merged further`() { /* ... */ }
}
```

- [ ] **Step 2: MergeSystem 수정**

- `unitDefId: Int` 비교 → `blueprintId: String` 비교
- `UNIT_DEFS.find { it.id == id }` → `blueprintRegistry.findById(id)`
- `mergeResultId: Int` → `mergeResultId: String?`
- HIDDEN/SPECIAL 유닛은 합성 불가 (mergeResultId가 null)

- [ ] **Step 3: 테스트 실행**

Run: `./gradlew test --tests "com.example.jaygame.engine.MergeSystemTest" --info`
Expected: PASS

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/MergeSystem.kt \
       app/src/test/java/com/example/jaygame/engine/MergeSystemTest.kt
git commit -m "refactor: migrate MergeSystem from Int to String ID with BlueprintRegistry"
```

---

## Task 13: 유닛 데이터 JSON 작성 — 전체 70종 일반 유닛

**Files:**
- Create/Update: `assets/units/blueprints.json` (전체 ~70종)

- [ ] **Step 1: Fire 속성 12종 작성**

탱커 라인 (Common~Legend, 4종), 근딜 라인 (Common~Immortal, 7종), 원딜 마법 (Rare 1종)
각 유닛의 id, name, stats, mergeResultId, behaviorId 설정.

- [ ] **Step 2: Frost 속성 12종 작성**

탱커 라인 (3~4종), 원딜 마법 라인 (5~7종), 컨트롤러 라인 (5~7종)

- [ ] **Step 3: Poison 속성 12종 작성**

근딜 라인, 원딜 마법 소수, 컨트롤러 라인

- [ ] **Step 4: Lightning 속성 12종 작성**

근딜 라인, 원딜 마법 라인, 서포터 소수

- [ ] **Step 5: Support 속성 12종 작성**

탱커 라인, 서포터 라인, 컨트롤러 소수

- [ ] **Step 6: Wind 속성 12종 작성**

근딜 라인, 원딜 물리 라인, 서포터 소수

- [ ] **Step 7: BlueprintRegistry 로드 테스트**

전체 JSON 로드 후 70종 파싱 확인:

Run: `./gradlew test --tests "com.example.jaygame.engine.BlueprintRegistryTest" --info`
Expected: PASS

- [ ] **Step 8: 커밋**

```bash
git add app/src/main/assets/units/blueprints.json
git commit -m "data: add all 70 normal unit blueprints across 6 families"
```

---

## Task 14: 히든 유닛 18종 + 특수 유닛 나머지 6종 데이터

**Files:**
- Update: `assets/units/hidden_recipes.json` (18종 완성)
- Update: `assets/units/special_units.json` (10종 완성)
- Update: `assets/units/blueprints.json` (히든 유닛 블루프린트 18종 추가)

- [ ] **Step 1: 히든 유닛 블루프린트 18종 추가**

blueprints.json에 히든 유닛 블루프린트 추가. 각각 `unitCategory: "HIDDEN"`, `isSummonable: false`, 듀얼 families.

- [ ] **Step 2: hidden_recipes.json 18종 완성**

각 레시피의 ingredients, resultId, 매칭 조건 정의.

- [ ] **Step 3: 특수 유닛 나머지 6종 데이터 추가**

연금술사, 결계파괴자, 토템마스터, 소환술사, 시간의현자(이미 있음 확인), 나머지.

- [ ] **Step 4: 전체 로드 테스트**

Run: `./gradlew test --info`
Expected: ALL PASS, 전체 ~98종 로드 확인

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/assets/units/
git commit -m "data: add 18 hidden units, complete 10 special units, total ~98 units"
```

---

## Task 15: UI 업데이트 — UnitDetailPopup, BuyUnitSheet

**Files:**
- Modify: `ui/battle/UnitDetailPopup.kt`
- Modify: `ui/battle/BuyUnitSheet.kt`

- [ ] **Step 1: UnitDetailPopup에 역할/사거리/데미지유형 표시 추가**

```kotlin
// 기존 등급/속성 배지 아래에 추가
Row {
    RoleBadge(unit.role)              // 탱커/근딜/원딜/서포터/컨트롤러
    AttackRangeBadge(unit.attackRange) // 근거리/원거리
    DamageTypeBadge(unit.damageType)  // 물리/마법
}

// HP 바 추가 (근거리 유닛용)
if (unit.attackRange == AttackRange.MELEE) {
    LinearProgressIndicator(progress = unit.hp / unit.maxHp)
    Text("HP: ${unit.hp.toInt()} / ${unit.maxHp.toInt()}")
}
```

- [ ] **Step 2: BuyUnitSheet에 역할 필터 추가**

```kotlin
// 역할별 필터 탭
ScrollableTabRow {
    UnitRole.values().forEach { role ->
        Tab(text = role.label, selected = selectedRole == role)
    }
}
```

- [ ] **Step 3: 컴파일 확인**

Run: `./gradlew compileDebugKotlin`
Expected: SUCCESS

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/battle/UnitDetailPopup.kt \
       app/src/main/java/com/example/jaygame/ui/battle/BuyUnitSheet.kt
git commit -m "feat: update unit UI to show role, attack range, and damage type"
```

---

## Task 16: UI 업데이트 — UnitCollectionScreen (히든 도감)

**Files:**
- Modify: `ui/screens/UnitCollectionScreen.kt`

- [ ] **Step 1: 탭 구조 추가**

```kotlin
// 3탭: 일반 | 히든 | 특수
TabRow {
    Tab("일반", selected = tab == 0)
    Tab("히든", selected = tab == 1)
    Tab("특수", selected = tab == 2)
}
```

- [ ] **Step 2: 히든 도감 UI**

```kotlin
// 미발견: 실루엣 + "???"
// 발견됨: 풀 이미지 + 이름 + 레시피 표시
LazyVerticalGrid {
    items(hiddenBlueprints) { bp ->
        val discovered = recipeSystem.isDiscovered(bp.id)
        if (discovered) {
            UnitCard(bp)  // 풀 정보
        } else {
            SilhouetteCard()  // ??? 실루엣
        }
    }
}
```

- [ ] **Step 3: 역할별 필터**

속성 필터 + 역할 필터 병렬 적용.

- [ ] **Step 4: 컴파일 확인**

Run: `./gradlew compileDebugKotlin`
Expected: SUCCESS

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/UnitCollectionScreen.kt
git commit -m "feat: add hidden unit codex and role filters to collection screen"
```

---

## Task 17: BattleField 렌더링 — 탱커 블로킹, 근딜 돌진, 필드 이펙트

**Files:**
- Modify: `ui/battle/BattleField.kt`

- [ ] **Step 1: 탱커 블로킹 렌더링**

- 탱커가 BLOCKING 상태일 때 방패 아이콘 오버레이
- 블로킹 중인 적과 연결선 표시
- HP 바 표시

- [ ] **Step 2: 근딜 돌진 렌더링**

- DASHING 상태: 유닛이 타겟으로 이동하는 모션 트레일
- RETURNING 상태: 홈타일로 복귀하는 잔상 효과

- [ ] **Step 3: 필드 이펙트 렌더링**

- 특수 유닛 배치 위치에 효과 범위 원형 표시
- 효과 타입별 색상: BARRIER=파란색, PATH_SLOW=보라색, FORGE=주황색 등

- [ ] **Step 3.5: 특수 유닛 배치 거부 팝업**

`FieldEffectManager.canPlace()` 호출 → false일 때 "특수 유닛 최대 배치 초과" 경고 다이얼로그 표시, 배치 거부.

```kotlin
// Grid 드래그 처리 내
if (unit.unitCategory == UnitCategory.SPECIAL && !fieldEffectManager.canPlace(unit)) {
    showWarningPopup("특수 유닛은 최대 2체까지 배치할 수 있습니다.")
    return
}
```

- [ ] **Step 4: 컴파일 확인**

Run: `./gradlew compileDebugKotlin`
Expected: SUCCESS

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/battle/BattleField.kt
git commit -m "feat: add tank blocking, assassin dash, and field effect rendering"
```

---

## Task 18: 기존 UnitDefs 마이그레이션 및 정리

**Files:**
- Modify: `data/UnitDefs.kt`
- Modify: `engine/UnitRegistry.kt`
- Modify: `engine/UnitSpec.kt`
- Modify: `bridge/BattleBridge.kt`

- [ ] **Step 1: UnitDefs.kt의 기존 42종 데이터를 JSON으로 완전 이전 확인**

blueprints.json에 기존 42종이 모두 포함되어 있는지 검증. 기존 `UNIT_DEFS` 리스트의 모든 id/name/stats가 JSON에 대응되는지 확인.

- [ ] **Step 2: UnitDefs.kt에서 UNIT_DEFS 리스트 제거**

`UnitFamily` enum은 유지 (다른 곳에서 참조). `UnitDef` data class와 `UNIT_DEFS` val을 제거.

- [ ] **Step 3: UnitRegistry.kt를 BlueprintRegistry로 대체**

기존 `UnitRegistry`의 모든 참조를 `BlueprintRegistry`로 교체. `UnitRegistry.kt` 파일 제거 또는 `BlueprintRegistry`로 리다이렉트.

- [ ] **Step 4: UnitSpec.kt 정리**

기존 `UnitSpec`의 능력 타입 정의가 새 `AbilityDef`/`AbilityType`으로 대체되었으므로, 기존 코드에서 `UnitSpec` 참조를 `UnitBlueprint`로 교체.

- [ ] **Step 5: BattleBridge.kt 연동 업데이트**

`BattleBridge`에서 기존 `UnitDef` 참조를 `UnitBlueprint`/`BlueprintRegistry`로 교체.

- [ ] **Step 6: 전체 컴파일 확인**

Run: `./gradlew compileDebugKotlin`
Expected: SUCCESS

- [ ] **Step 7: 전체 테스트 실행**

Run: `./gradlew test --info`
Expected: ALL PASS

- [ ] **Step 8: 커밋**

```bash
git add -A
git commit -m "refactor: migrate from UnitDefs/UnitRegistry to BlueprintRegistry JSON system"
```

---

## Task 19: ObjectPool 조정 및 최종 통합 테스트

**Files:**
- Modify: `engine/ObjectPool.kt`
- Modify: `engine/BattleEngine.kt` (풀 사이즈)

- [ ] **Step 1: ObjectPool 사이즈 조정**

```kotlin
// BattleEngine 내 풀 초기화
val unitPool = ObjectPool(80) { GameUnit() }    // 기존 64 → 80
val zonePool = ObjectPool(64) { ZoneEffect() }  // 기존 32 → 64
```

- [ ] **Step 2: 전체 컴파일 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESS

- [ ] **Step 3: 전체 테스트 실행**

Run: `./gradlew test --info`
Expected: ALL PASS

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/ObjectPool.kt \
       app/src/main/java/com/example/jaygame/engine/BattleEngine.kt
git commit -m "chore: adjust object pool sizes for new unit system"
```

---

## Task 20: 바탕화면 리포트 업데이트

**Files:**
- Update: `C:\Users\Infocar\Desktop\JayGame_Analysis_Report.docx`

- [ ] **Step 1: 리포트 내용 업데이트**

유닛 시스템 개편 내용을 반영:
- 섹션 3.3 유닛 시스템: 42종 → ~98종, 4축 분류 체계
- 섹션 3.4 합성 시스템: 히든 유닛 레시피 합성 추가
- 섹션 3.5 시너지 시스템: 역할 시너지 추가
- 신규 섹션: 히든 유닛 시스템, 특수 유닛 시스템
- 섹션 4 성능 최적화: Object Pool 사이즈 조정
- 섹션 7 개선 제안: 유닛 시스템 개편 완료 반영

- [ ] **Step 2: 리포트 파일 저장 확인**

---

## 실행 순서 요약

| 단계 | Task | 의존성 | 병렬 가능 |
|------|------|--------|----------|
| 1 | Task 1: Enum/모델 | 없음 | - |
| 2 | Task 2: Behavior 인터페이스 | Task 1 | Task 3과 병렬 |
| 2 | Task 3: BlueprintRegistry | Task 1 | Task 2와 병렬 |
| 3 | Task 4: GameUnit 재설계 | Task 1, 2 | - |
| 4 | Task 5-7: Behavior 구현 5종 | Task 2, 4 | 5/6/7 병렬 (단, BattleEngine 직접 수정 없음) |
| 4 | Task 9: 시너지 확장 | Task 1, 4 | Task 5-7과 병렬 가능 |
| 4 | Task 12: MergeSystem | Task 3, 4 | Task 5-7과 병렬 가능 |
| 5 | Task 8: BattleEngine 통합 | Task 3, 4, 5-7 | - |
| 5 | Task 10: RecipeSystem | Task 3, 4, 12 | Task 8과 병렬 (단, Grid 수정 시 주의) |
| 5 | Task 11: FieldEffectManager | Task 2, 4 | Task 8과 병렬 |
| 6 | Task 13-14: 데이터 작성 | Task 3 | 13/14 순차 |
| 7 | Task 18: 마이그레이션 정리 | Task 8, 10, 11, 12 | - |
| 8 | Task 15-17: UI 업데이트 | Task 18 | 15/16/17 병렬 |
| 9 | Task 19: 최종 통합 | 전부 | - |
| 10 | Task 20: 리포트 업데이트 | 전부 | - |

**핵심 변경 사항 (리뷰 반영):**
- Task 12(MergeSystem)를 Task 10(RecipeSystem) 이전으로 이동 — Grid 드래그 우선순위가 MergeSystem String ID 전환에 의존
- Task 18(마이그레이션 정리)를 Task 15-17(UI) 이전으로 이동 — UI 코드가 새 API만 참조하도록 보장
- Task 9(시너지)를 단계 4로 앞당김 — Task 8에 의존하지 않으므로 병렬 가능
- Task 8의 의존성에 Task 3(BlueprintRegistry) 추가
