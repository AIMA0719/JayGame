# 펫(Pet) 시스템 구현 플랜

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 전투 보조 펫 시스템 — 수집/성장/장착으로 유닛과 별개의 영구 성장축 제공

**Architecture:** PetDef(정의) → PetManager(로직) → GameData에 저장 → 전투 시 패시브 오라/주기적 스킬로 작동. 펫은 뽑기(다이아)/전투 드롭/업적 보상으로 획득.

**Tech Stack:** Kotlin, Compose UI, SharedPreferences(JSON), BattleEngine 연동

---

## 펫 설계

### 등급 (4단계)
| 등급 | 색상 | 뽑기 확률 | 최대 레벨 |
|------|------|----------|----------|
| 희귀 (Rare) | 파랑 | 60% | 10 |
| 영웅 (Hero) | 보라 | 25% | 15 |
| 전설 (Legend) | 금색 | 12% | 20 |
| 신화 (Mythic) | 빨강 | 3% | 30 |

### 펫 목록 (9종, 3카테고리)

**공격 펫 (3종) — 주기적으로 적에게 피해**
| ID | 이름 | 등급 | 스킬 | 쿨다운 |
|----|------|------|------|--------|
| 0 | 화염 드래곤 | 희귀 | 범위 내 적 전체에 {ATK}×lv 화염 피해 | 8초 |
| 1 | 독거미 | 영웅 | 가장 체력 높은 적에게 {ATK}×lv DoT (5초) | 10초 |
| 2 | 번개 매 | 전설 | 랜덤 적 3체에 {ATK}×lv 연쇄번개 | 12초 |

**보조 펫 (3종) — 아군 유닛 버프**
| ID | 이름 | 등급 | 스킬 | 쿨다운 |
|----|------|------|------|--------|
| 3 | 요정 | 희귀 | 전체 유닛 ATK +{lv×2}% (8초간) | 15초 |
| 4 | 골렘 | 영웅 | 전체 유닛에 {lv×50} 쉴드 (10초) | 20초 |
| 5 | 유니콘 | 전설 | 쿨다운 전체 -{lv×0.5}초 + 공속 +{lv×3}% (6초) | 25초 |

**유틸 펫 (3종) — 경제/특수 효과**
| ID | 이름 | 등급 | 패시브 효과 |
|----|------|------|------------|
| 6 | 두꺼비 | 희귀 | 적 처치 골드 +{lv×5}% (상시) |
| 7 | 9미호 | 영웅 | 소환 시 {lv×2}% 확률로 1등급 상향 |
| 8 | 봉황 | 신화 | 전투 패배 시 1회 부활 (50% 체력으로 적 리셋) |

### 펫 레벨업
- 동일 펫 카드 수집으로 레벨업 (유닛 카드 시스템과 동일 패턴)
- 필요 카드 수: `lv × 2` (lv1→2: 2장, lv2→3: 4장, ...)
- 골드 비용: `lv × 200`

### 장착: 1마리만 장착 가능 (트로피 2000 이상 시 2마리)

---

## Chunk 1: 데이터 정의 & 저장

### Task 1: PetDef 데이터 정의

**Files:**
- Create: `app/src/main/java/com/example/jaygame/data/PetDefs.kt`

- [ ] **Step 1: PetGrade enum 작성**

```kotlin
package com.example.jaygame.data

enum class PetGrade(val label: String, val colorHex: Long, val maxLevel: Int, val pullWeight: Int) {
    RARE("희귀", 0xFF42A5F5, 10, 60),
    HERO("영웅", 0xFFAB47BC, 15, 25),
    LEGEND("전설", 0xFFFFCA28, 20, 12),
    MYTHIC("신화", 0xFFEF5350, 30, 3);
}

enum class PetCategory { ATTACK, SUPPORT, UTILITY }
```

- [ ] **Step 2: PetDef data class 작성**

```kotlin
data class PetDef(
    val id: Int,
    val name: String,
    val grade: PetGrade,
    val category: PetCategory,
    val skillName: String,
    val skillDescription: String,   // "{lv}" placeholder
    val cooldown: Float,            // 초 (0이면 패시브)
    val isPassive: Boolean = cooldown == 0f,
)
```

- [ ] **Step 3: 9종 펫 목록 작성**

```kotlin
val ALL_PETS: List<PetDef> = listOf(
    PetDef(0, "화염 드래곤", PetGrade.RARE, PetCategory.ATTACK,
        "화염 브레스", "범위 내 적 전체에 화염 피해", 8f),
    PetDef(1, "독거미", PetGrade.HERO, PetCategory.ATTACK,
        "맹독 사출", "HP 최대 적에게 DoT 5초", 10f),
    PetDef(2, "번개 매", PetGrade.LEGEND, PetCategory.ATTACK,
        "연쇄 낙뢰", "랜덤 3체 연쇄번개", 12f),
    PetDef(3, "요정", PetGrade.RARE, PetCategory.SUPPORT,
        "격려", "전체 유닛 ATK 증가 8초", 15f),
    PetDef(4, "골렘", PetGrade.HERO, PetCategory.SUPPORT,
        "대지의 방패", "전체 유닛 쉴드 10초", 20f),
    PetDef(5, "유니콘", PetGrade.LEGEND, PetCategory.SUPPORT,
        "성스러운 빛", "쿨다운 감소 + 공속 증가 6초", 25f),
    PetDef(6, "두꺼비", PetGrade.RARE, PetCategory.UTILITY,
        "금빛 혀", "적 처치 골드 증가 (패시브)", 0f),
    PetDef(7, "9미호", PetGrade.HERO, PetCategory.UTILITY,
        "환술", "소환 등급 상향 확률 (패시브)", 0f),
    PetDef(8, "봉황", PetGrade.MYTHIC, PetCategory.UTILITY,
        "열반", "패배 시 1회 부활 (패시브)", 0f),
)

/** 펫 레벨업 필요 카드 수 */
fun petCardsRequired(level: Int): Int = level * 2

/** 펫 레벨업 골드 비용 */
fun petUpgradeCost(level: Int): Int = level * 200

/** 펫 뽑기 비용 (다이아) */
const val PET_PULL_COST = 50
const val PET_PULL_10_COST = 400  // 10연차 할인
```

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/data/PetDefs.kt
git commit -m "feat: add PetDef data definitions (9 pets, 4 grades)"
```

---

### Task 2: GameData에 펫 저장 구조 추가

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/data/GameData.kt`

- [ ] **Step 1: PetProgress data class 추가**

```kotlin
data class PetProgress(
    val petId: Int = 0,
    val owned: Boolean = false,
    val cards: Int = 0,
    val level: Int = 1,
)
```

- [ ] **Step 2: GameData에 펫 필드 추가**

```kotlin
val pets: List<PetProgress> = List(9) { PetProgress(petId = it) },
val equippedPets: List<Int> = emptyList(),  // 장착 petId (최대 2)
val petPullPity: Int = 0,  // 천장 카운터 (50회 시 전설 확정)
```

- [ ] **Step 3: equippedPetSlotCount 계산 프로퍼티**

```kotlin
val equippedPetSlotCount: Int get() = if (trophies >= 2000) 2 else 1
```

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/data/GameData.kt
git commit -m "feat: add pet progress and pity counter to GameData"
```

---

### Task 3: GameRepository에 펫 직렬화 추가

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/data/GameRepository.kt`

- [ ] **Step 1: toJson에 pets 직렬화 추가**

```kotlin
val petsArr = JSONArray()
for (p in data.pets) {
    val pObj = JSONObject()
    pObj.put("petId", p.petId)
    pObj.put("owned", if (p.owned) 1 else 0)
    pObj.put("cards", p.cards)
    pObj.put("level", p.level)
    petsArr.put(pObj)
}
root.put("pets", petsArr)

val epArr = JSONArray()
for (id in data.equippedPets) epArr.put(id)
root.put("equippedPets", epArr)
root.put("petPullPity", data.petPullPity)
```

- [ ] **Step 2: fromJson에 pets 역직렬화 추가**

```kotlin
val pets = if (root.has("pets")) {
    val arr = root.getJSONArray("pets")
    List(arr.length()) { i ->
        val obj = arr.getJSONObject(i)
        PetProgress(
            petId = obj.getInt("petId"),
            owned = obj.optInt("owned", 0) == 1,
            cards = obj.optInt("cards", 0),
            level = obj.optInt("level", 1),
        )
    }
} else List(9) { PetProgress(petId = it) }

val equippedPets = if (root.has("equippedPets")) {
    val arr = root.getJSONArray("equippedPets")
    List(arr.length()) { arr.getInt(it) }
} else emptyList()

val petPullPity = root.optInt("petPullPity", 0)
```

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/data/GameRepository.kt
git commit -m "feat: serialize/deserialize pet data in GameRepository"
```

---

## Chunk 2: 펫 관리 로직

### Task 4: PetManager 생성

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/PetManager.kt`

- [ ] **Step 1: PetManager 클래스 작성**

```kotlin
package com.example.jaygame.engine

import com.example.jaygame.data.*

class PetManager(private var gameData: GameData) {

    fun syncData(data: GameData) { gameData = data }

    /** 펫 뽑기 (단일) */
    fun pullPet(): GameData? {
        if (gameData.diamonds < PET_PULL_COST) return null
        var data = gameData.copy(
            diamonds = gameData.diamonds - PET_PULL_COST,
            petPullPity = gameData.petPullPity + 1,
        )
        val grade = rollPetGrade(data.petPullPity)
        if (grade.ordinal >= PetGrade.LEGEND.ordinal) {
            data = data.copy(petPullPity = 0)  // 천장 리셋
        }
        val eligiblePets = ALL_PETS.filter { it.grade == grade }
        val pet = eligiblePets.random()
        return addPetCards(data, pet.id, 1)
    }

    /** 10연차 뽑기 */
    fun pullPet10(): GameData? {
        if (gameData.diamonds < PET_PULL_10_COST) return null
        var data = gameData.copy(diamonds = gameData.diamonds - PET_PULL_10_COST)
        repeat(10) {
            data = data.copy(petPullPity = data.petPullPity + 1)
            val grade = rollPetGrade(data.petPullPity)
            if (grade.ordinal >= PetGrade.LEGEND.ordinal) {
                data = data.copy(petPullPity = 0)
            }
            val eligiblePets = ALL_PETS.filter { it.grade == grade }
            val pet = eligiblePets.random()
            data = addPetCards(data, pet.id, 1)
        }
        return data
    }

    private fun rollPetGrade(pity: Int): PetGrade {
        // 50회 천장: 전설 확정
        if (pity >= 50) return PetGrade.LEGEND
        val totalWeight = PetGrade.entries.sumOf { it.pullWeight }
        var roll = (0 until totalWeight).random()
        for (g in PetGrade.entries) {
            roll -= g.pullWeight
            if (roll < 0) return g
        }
        return PetGrade.RARE
    }

    private fun addPetCards(data: GameData, petId: Int, count: Int): GameData {
        val pets = data.pets.toMutableList()
        val existing = pets[petId]
        pets[petId] = if (existing.owned) {
            existing.copy(cards = existing.cards + count)
        } else {
            existing.copy(owned = true, cards = existing.cards + count, level = 1)
        }
        return data.copy(pets = pets)
    }

    /** 펫 레벨업 */
    fun upgradePet(petId: Int): GameData? {
        val pet = gameData.pets[petId]
        if (!pet.owned) return null
        val def = ALL_PETS[petId]
        if (pet.level >= def.grade.maxLevel) return null
        val cardsNeeded = petCardsRequired(pet.level)
        val goldNeeded = petUpgradeCost(pet.level)
        if (pet.cards < cardsNeeded || gameData.gold < goldNeeded) return null

        val pets = gameData.pets.toMutableList()
        pets[petId] = pet.copy(level = pet.level + 1, cards = pet.cards - cardsNeeded)
        return gameData.copy(pets = pets, gold = gameData.gold - goldNeeded)
    }

    /** 장착/해제 */
    fun equipPet(petId: Int): GameData? {
        if (!gameData.pets[petId].owned) return null
        if (gameData.equippedPets.contains(petId)) return null
        if (gameData.equippedPets.size >= gameData.equippedPetSlotCount) return null
        return gameData.copy(equippedPets = gameData.equippedPets + petId)
    }

    fun unequipPet(petId: Int): GameData {
        return gameData.copy(equippedPets = gameData.equippedPets - petId)
    }
}
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/PetManager.kt
git commit -m "feat: add PetManager with pull/upgrade/equip/pity logic"
```

---

### Task 5: 전투 중 펫 스킬 실행 시스템

**Files:**
- Create: `app/src/main/java/com/example/jaygame/engine/PetBattleSystem.kt`

- [ ] **Step 1: PetBattleSystem 작성**

```kotlin
package com.example.jaygame.engine

import com.example.jaygame.data.*

class PetBattleSystem {

    private data class ActivePet(
        val def: PetDef,
        val level: Int,
        var cooldownRemaining: Float = 0f,
        var phoenixUsed: Boolean = false,
    )

    private val activePets = mutableListOf<ActivePet>()

    fun init(gameData: GameData) {
        activePets.clear()
        for (petId in gameData.equippedPets) {
            val progress = gameData.pets[petId]
            if (progress.owned) {
                activePets.add(ActivePet(
                    def = ALL_PETS[petId],
                    level = progress.level,
                    cooldownRemaining = ALL_PETS[petId].cooldown * 0.5f, // 50% ready
                ))
            }
        }
    }

    /** 매 프레임 호출 */
    fun update(
        dt: Float,
        enemies: List<Enemy>,
        units: List<GameUnit>,
        applyDamageToEnemy: (Enemy, Float) -> Unit,
        applyBuffToUnit: (GameUnit, BuffType, Float, Float) -> Unit,
        applyBuffToEnemy: (Enemy, BuffType, Float, Float) -> Unit,
    ) {
        for (pet in activePets) {
            if (pet.def.isPassive) continue  // 패시브는 별도 처리
            pet.cooldownRemaining -= dt
            if (pet.cooldownRemaining <= 0f) {
                pet.cooldownRemaining = pet.def.cooldown
                executeSkill(pet, enemies, units, applyDamageToEnemy, applyBuffToUnit, applyBuffToEnemy)
            }
        }
    }

    private fun executeSkill(
        pet: ActivePet,
        enemies: List<Enemy>,
        units: List<GameUnit>,
        applyDamage: (Enemy, Float) -> Unit,
        applyBuff: (GameUnit, BuffType, Float, Float) -> Unit,
        applyDebuff: (Enemy, BuffType, Float, Float) -> Unit,
    ) {
        val lv = pet.level
        when (pet.def.id) {
            0 -> { // 화염 드래곤: 범위 피해
                val dmg = 20f * lv
                for (e in enemies) if (e.alive) applyDamage(e, dmg)
            }
            1 -> { // 독거미: 최대 체력 적에게 DoT
                val target = enemies.filter { it.alive }.maxByOrNull { it.hp } ?: return
                applyDebuff(target, BuffType.DoT, 10f * lv, 5f)
            }
            2 -> { // 번개 매: 랜덤 3체
                val targets = enemies.filter { it.alive }.shuffled().take(3)
                val dmg = 30f * lv
                for (t in targets) applyDamage(t, dmg)
            }
            3 -> { // 요정: 전체 ATK 버프
                val bonus = lv * 0.02f  // lv×2%
                for (u in units) if (u.alive) applyBuff(u, BuffType.AtkUp, bonus, 8f)
            }
            4 -> { // 골렘: 전체 쉴드
                val shield = lv * 50f
                for (u in units) if (u.alive) applyBuff(u, BuffType.Shield, shield, 10f)
            }
            5 -> { // 유니콘: 공속 + 쿨다운
                val spdBonus = lv * 0.03f
                for (u in units) if (u.alive) applyBuff(u, BuffType.SpdUp, spdBonus, 6f)
                // 쿨다운 감소는 BattleEngine에서 처리
            }
        }
    }

    // === 패시브 효과 접근자 ===

    /** 두꺼비: 골드 보너스 */
    fun getGoldKillBonus(): Float {
        val toad = activePets.find { it.def.id == 6 } ?: return 0f
        return toad.level * 0.05f  // lv×5%
    }

    /** 9미호: 소환 등급 상향 확률 */
    fun getSummonGradeUpChance(): Float {
        val fox = activePets.find { it.def.id == 7 } ?: return 0f
        return fox.level * 0.02f  // lv×2%
    }

    /** 봉황: 부활 가능 여부 */
    fun canPhoenixRevive(): Boolean {
        val phoenix = activePets.find { it.def.id == 8 } ?: return false
        return !phoenix.phoenixUsed
    }

    fun usePhoenixRevive() {
        val phoenix = activePets.find { it.def.id == 8 } ?: return
        phoenix.phoenixUsed = true
    }

    fun reset() { activePets.clear() }
}
```

- [ ] **Step 2: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/PetBattleSystem.kt
git commit -m "feat: add PetBattleSystem for in-battle pet skills"
```

---

### Task 6: BattleEngine에 펫 시스템 연동

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/engine/BattleEngine.kt`

- [ ] **Step 1: BattleEngine에 PetBattleSystem 필드 추가**

```kotlin
val petSystem = PetBattleSystem()
```

- [ ] **Step 2: 전투 시작 시 init 호출**

start/init 함수에서:

```kotlin
petSystem.init(gameData)
```

- [ ] **Step 3: update 루프에서 petSystem.update 호출**

메인 업데이트 루프에서 유닛/적 업데이트 후:

```kotlin
petSystem.update(dt, aliveEnemies, aliveUnits, ::dealDamageToEnemy, ::applyBuffToUnit, ::applyDebuffToEnemy)
```

- [ ] **Step 4: 패시브 효과 연동**

- 골드 보너스: EconomyManager.onEnemyKilled에 `petSystem.getGoldKillBonus()` 추가
- 소환 등급 상향: 소환 로직에 `petSystem.getSummonGradeUpChance()` 추가
- 봉황 부활: 패배 판정 시 `petSystem.canPhoenixRevive()` 체크

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/engine/BattleEngine.kt
git commit -m "feat: integrate PetBattleSystem into BattleEngine"
```

---

## Chunk 3: 펫 UI

### Task 7: 펫 관리 화면 + 뽑기 화면

**Files:**
- Create: `app/src/main/java/com/example/jaygame/ui/screens/PetScreen.kt`
- Modify: `app/src/main/java/com/example/jaygame/navigation/Routes.kt`
- Modify: `app/src/main/java/com/example/jaygame/navigation/NavGraph.kt`
- Modify: `app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt`

- [ ] **Step 1: Routes에 Pet 경로 추가**

```kotlin
object Pet : Routes("pet")
```

- [ ] **Step 2: PetScreen 컴포저블 작성**

탭 2개 구조:
- **내 펫 탭**: 보유 펫 그리드 + 선택 시 상세/레벨업/장착
- **뽑기 탭**: 단일 뽑기 (50다이아) / 10연차 (400다이아) + 천장 카운터 표시

```kotlin
@Composable
fun PetScreen(
    gameData: GameData,
    onPull: () -> Unit,
    onPull10: () -> Unit,
    onUpgrade: (petId: Int) -> Unit,
    onEquip: (petId: Int) -> Unit,
    onUnequip: (petId: Int) -> Unit,
    onBack: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPetId by remember { mutableIntStateOf(-1) }

    Column(Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {
        // 탭 바: [내 펫] [뽑기]
        // Tab 0: 펫 그리드 (3×3) + 장착 슬롯 + 상세 패널
        // Tab 1: 뽑기 연출 + 천장 카운터 ({pity}/50)
    }
}
```

- [ ] **Step 3: NavGraph + HomeScreen 연결**

기존 패턴 따라 연결.

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/example/jaygame/ui/screens/PetScreen.kt \
       app/src/main/java/com/example/jaygame/navigation/Routes.kt \
       app/src/main/java/com/example/jaygame/navigation/NavGraph.kt \
       app/src/main/java/com/example/jaygame/ui/screens/HomeScreen.kt
git commit -m "feat: add PetScreen UI with pull/upgrade/equip tabs"
```

---
