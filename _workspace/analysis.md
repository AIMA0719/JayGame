# JayGame 보스 기믹, 엘리트, 웨이브 시스템 상세 분석

작성 일시: 2026-04-03
분석 대상: BossModifier, BattleEngine, Enemy, WaveSystem, EnemyOverlay

## 1. 현재 BossModifier 구조 분석

### 1.1 Enum 정의 (BossModifier.kt, 줄 3-16)

파일 경로: app/src/main/java/com/jay/jaygame/engine/BossModifier.kt

현재 보스 기믹 12개:
- PHYSICAL_RESIST (줄 4): 물리 피해 60% 감소
- MAGIC_RESIST (줄 5): 마법 피해 60% 감소
- CC_IMMUNE (줄 6): 둔화/동결 면역
- DOT_IMMUNE (줄 7): 지속 피해 면역
- RANGED_RESIST (줄 8): 원거리 피해 50% 감소
- REGENERATION (줄 9): 10초마다 체력 5% 회복
- SWIFT (줄 10): 이동속도 2배
- COMMANDER (줄 11): 주변 몬스터 방어 +50%
- BERSERKER (줄 12): 체력 50% 이하 시 공격속도 3배
- SPLITTER (줄 13): 체력 50% 이하 시 미니 보스 2마리 소환
- SHIELDED (줄 14): 5초마다 모든 피해 무효화 보호막
- VAMPIRIC (줄 15): 공격 피해의 20% 체력 회복

## 2. BattleEngine 보스 기믹 실행 코드 (줄번호 포함)

### 2.1 보스 스폰 초기화 (BattleEngine.kt, 줄 576-597)

줄 577: val isTrueBoss = (waveSystem.currentWave + 1) % 10 == 0
줄 580-584: 기믹 할당
줄 585: enemy.bossModifier = modifier
줄 586-589: SWIFT 초기화 (SWIFT_SPEED_MULT = 2f, 줄 61)
줄 590-593: SHIELDED 초기화
줄 596: BattleBridge.notifyBossModifier(modifier)

### 2.2 매 프레임 기믹 업데이트 (줄 608-654)

COMMANDER (줄 610-621): 200px 범위 내 Shield 버프 (maxHp * 0.02, 2초)
REGENERATION (줄 624-630): 10초마다 maxHp * 0.05 회복
BERSERKER (줄 632-637): 체력 50% 이하 시 속도 1.5배
SPLITTER (줄 639-643): 체력 50% 이하 시 미니보스 2마리 스폰 예약
SHIELDED (줄 646-652): 3초 활성 / 5초 비활성 사이클

### 2.3 미니보스 스폰 (줄 664-678)

repeat(2): 2마리 스폰
hp = parent.maxHp * 0.3f
speed = parent.baseSpeed * 1.3f
armor = parent.armor * 0.7f

### 2.4 데미지 처리 (Enemy.kt, 줄 125-155)

줄 126-127: SHIELDED && shieldActive면 데미지 0 반환
줄 131-138: 기믹별 데미지 감소 적용
  - PHYSICAL_RESIST: x0.4
  - MAGIC_RESIST: x0.4
  - RANGED_RESIST: x0.5 (attackRange > 200px)
  - BERSERKER: x1.15 (체력 50% 이하)
줄 149-151: VAMPIRIC - finalDmg * 0.2f 회복

## 3. 적 스폰/타겟팅/데미지 경로

### 3.1 적 스폰 (BattleEngine.kt, 줄 556-599)

줄 560: enemies.acquire()
줄 561: isElite 판정
줄 562-569: init() 호출
줄 571-575: 엘리트 + 어려움 1+ = REGENERATION 자동 획득

### 3.2 엘리트 시스템 (Enemy.kt, 줄 39)

배수: HP 2.0배, Speed 1.1배, Armor 1.5배, MagicResist 1.3배, CC저항 +0.1

생성 확률 (WaveSystem.kt, 줄 97-104):
Wave 20-29: 10%
Wave 30-44: 20%
Wave 45+: 30%

### 3.3 적 타겟팅 (BattleEngine.kt, 줄 1061-1074)

spatialHash 범위 검색
if (enemy.alive)로만 체크 - 보스 제외 안 함

### 3.4 데미지 흐름

발사체: projectiles.forEach -> proj.update() -> AbilitySystem.onProjectileHit() -> applyAbilityOnHit() -> takeDamage() (줄 1032)
즉시공격: behavior?.onAttack() -> takeDamage() (줄 844)

## 4. 적 렌더링 및 크기 조절

### 4.1 크기 정의 (Enemy.kt, 줄 187-191)

val size: Float get() = when (type) {
    4, 5 -> 163f
    6 -> 109f
    else -> 82f
}

### 4.2 렌더링 (EnemyOverlay.kt)

줄 140-148: decodeScaledBitmap() - drawable 로드
줄 150: 보스 비트맵 128px
줄 153-168: 스프라이트 시트 assets/enemies/*.png

### 4.3 크기 조절 방법

방법 1: Enemy.kt size property 수정
  if (bossModifier == BossModifier.BERSERKER && hpRatio < 0.5f) baseSize *= 1.2f

## 5. WaveConfig 확장 포인트

### 5.1 구조 (WaveSystem.kt, 줄 3-15)

data class WaveConfig(
    val enemyCount: Int,
    val hp: Float,
    val speed: Float,
    val armor: Float,
    val magicResist: Float,
    val isBoss: Boolean,
    val isMiniBoss: Boolean = false,
    val spawnInterval: Float,
    val enemyType: Int,
    val ccResistance: Float = 0f,
    val eliteChance: Float = 0f,
)

### 5.2 getWaveConfig() 주요 부분 (줄 41-119)

줄 44-54: 기본 스탯 계산
줄 58-59: 웨이브 타입 결정
줄 62-71: 적 타입 결정
줄 73-82: 보스 배수
줄 85-95: CC 저항력
줄 98-104: 엘리트 확률

### 5.3 특수 이벤트 추가 방법

필드 추가: isDarknessWave, isBerserkerWave, isArmorWave, isEliteSurgeWave, isMinionRushWave

판정 로직:
val isDarknessWave = (wave + 1) % 20 == 15
val isBerserkerWave = (wave + 1) % 20 == 16
...

## 6. 구현 순서

### 보스 기믹 6개 추가

Phase 1A (난이도 ⭐⭐):
1. PHASE_CHANGE - 체력 25%, 50%, 75%에서 속성 변화
2. SHIELD_GENERATOR - 5초마다 주변 적 Shield
3. UNSTABLE - DoT 받으면 폭발 데미지

Phase 1B (난이도 ⭐⭐⭐):
4. CURSE - 주변 유닛 공격속도 50% 감소
5. LIFE_DRAIN - 피해 입을 때 일정 거리 유닛 HP 흡수

Phase 1C (난이도 ⭐⭐⭐⭐):
6. FIELD_HAZARD - 시간 경과 랜덤 위치 피해 존

### 웨이브 특수 이벤트 5개

1. DARKNESS_WAVE (Wave 15, 35, 55): 사거리 30% 감소
   위치: BattleEngine.kt 줄 1129 applyGroupBonus()
   unit.range *= (if (isDarknessWave) 0.7f else 1f)

2. BERSERKER_WAVE (Wave 16, 36, 56): 모든 적 BERSERKER
   위치: BattleEngine.kt 줄 597 updateSpawning()
   if (config.isBerserkerWave) enemy.bossModifier = BossModifier.BERSERKER

3. ARMOR_WAVE (Wave 17, 37, 57): 방어력 2배
   위치: WaveSystem.kt 줄 52
   val baseArmor = (...) * (if (isArmorWave) 2f else 1f)

4. ELITE_SURGE (Wave 18, 38, 58): 엘리트 확률 3배
   위치: WaveSystem.kt 줄 104
   val eliteChance = (...) * (if (isEliteSurgeWave) 3f else 1f)

5. MINION_RUSH (Wave 19, 39, 59): 30마리 + SPLITTER
   위치: WaveSystem.kt, BattleEngine.kt updateSpawning()
   난이도: ⭐⭐⭐

## 7. 주요 상수값

| 상수 | 값 | 파일/줄 |
|------|-----|--------|
| ELITE_HP_MULT | 2.0 | 48 |
| ELITE_SPEED_MULT | 1.1 | 49 |
| ELITE_ARMOR_MULT | 1.5 | 50 |
| BERSERKER_HP_THRESHOLD | 0.5 | 55 |
| SPLITTER_HP_THRESHOLD | 0.5 | 56 |
| COMMANDER_SHIELD_RATIO | 0.02 | 57 |
| REGEN_HEAL_RATIO | 0.05 | 58 |
| REGEN_INTERVAL | 10.0 | 59 |
| BERSERKER_SPEED_MULT | 1.5 | 60 |
| SWIFT_SPEED_MULT | 2.0 | 61 |
| SHIELDED_ACTIVE_DURATION | 3.0 | 62 |
| SHIELDED_COOLDOWN_DURATION | 5.0 | 63 |

작성 완료: 2026-04-03
