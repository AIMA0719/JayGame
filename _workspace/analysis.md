# 로그라이크 강화 선택 시스템 — 영향도 분석

## 핵심 삽입 지점
- `BattleEngine.kt:454-470` — 웨이브 클리어 후 `State.WaveDelay` 전환 직전

## 영향 파일
| 파일 | 역할 |
|------|------|
| `engine/BattleEngine.kt:397-471` | 웨이브 클리어 감지, 보상 지급, 상태 전환 |
| `engine/BattleEngine.kt:340-389` | 게임 루프, battleSpeed 기반 속도 제어 |
| `engine/BattleEngine.kt:1121-1133` | applyBattleUpgrade() 패턴 |
| `engine/BattleEngine.kt:150-160` | upgradeAtkMult, upgradeSpdMult 등 글로벌 멀티플라이어 |
| `engine/WaveSystem.kt:16-17` | currentWave, 보스 웨이브 판별 |
| `bridge/BattleBridge.kt:21-51` | BattleState data class |
| `bridge/BattleBridge.kt:288-328` | BattleCommand sealed class |
| `bridge/BattleBridge.kt:852-863` | battleSpeed StateFlow |
| `engine/UnitUpgradeSystem.kt` | 기존 등급 그룹 강화 패턴 참조 |
| `engine/GameUnit.kt:28-29` | spdMultiplier, groupAtkBonus 필드 |
| `engine/DamageCalculator.kt:40-55` | calculateUnitDamage() |
| `ui/battle/BattleScreen.kt:87-370` | 오버레이 다이얼로그 패턴 |

## 일시정지 방법
- `BattleBridge.setBattleSpeed(0f)` → accumulator에 0 곱셈 = 정지
- 기존 패턴: UI에서 speed 제어, 다이얼로그 닫을 때 복원

## 글로벌 보너스 적용 경로
```
applyGroupBonus(unit)
  ├─ unit.groupAtkBonus = groupAtkBonusCache[grp]
  └─ unit.spdMultiplier = upgradeSpdMult * synergySpdMult * (1 + groupSpdBonusCache[grp])
fireProjectile() → baseDamage = unit.baseATK * (1+groupAtkBonus) * upgradeAtkMult * ...
```

## 리스크
- 스레드 안전성: commandQueue(ConcurrentLinkedQueue)를 통해서만 전달
- autoWaveStart 시 선택 전 다음 웨이브 시작 가능 → 선택 완료 전 waveDelayTimer 차단 필요
- 기존 멀티플라이어와 중복 적용 → 별도 roguelikeAtkMult 필드 사용
