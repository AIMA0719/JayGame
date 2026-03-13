package com.example.jaygame.engine

import com.example.jaygame.bridge.GridTileState
import kotlinx.coroutines.flow.StateFlow

/**
 * 게임 엔진 인터페이스.
 * 배틀 루프, 웨이브 시스템, 이코노미를 관리.
 *
 * MVVM 아키텍처에서 Engine 레이어로 동작:
 * GameEngine(로직) → StateFlow(상태) → ViewModel → Compose UI
 */
interface GameEngine {
    /** 현재 배틀 상태 (Compose UI에서 collect) */
    val battleState: StateFlow<BattleState>

    /** 배틀 시작 */
    fun startBattle(config: BattleConfig)

    /** 유닛 소환 (미네랄 소모) */
    fun summonUnit(): SummonResult

    /** 일시정지 / 재개 */
    fun togglePause()

    /** 배틀 종료 (강제) */
    fun endBattle()
}

data class BattleConfig(
    val stageId: Int = 0,
    val maxWaves: Int = 40,
    val difficulty: Int = 0,   // 0=Easy, 1=Normal, 2=Hard
    val deck: List<Int> = listOf(0, 1, 2),
)

/**
 * 데미지 타입 — 물리/마법 이원 체계
 */
enum class DamageType {
    PHYSICAL,  // 방어력(Defense) 감산
    MAGIC,     // 마법 저항(MagicResist) 비율 감산
}

data class BattleState(
    val phase: BattlePhase = BattlePhase.IDLE,
    val currentWave: Int = 0,
    val maxWaves: Int = 40,
    val playerHP: Int = 20,
    val maxHP: Int = 20,
    val minerals: Int = 100,           // 소환 재화
    val gold: Int = 0,                 // 몬스터 처치 시 획득
    val gas: Int = 0,                  // 업그레이드 재화
    val elapsedTime: Float = 0f,
    val enemyCount: Int = 0,           // 현재 화면 내 몬스터 수
    val maxEnemyCount: Int = 100,      // 초과 시 Game Over
    val summonCost: Int = 10,
    val killCount: Int = 0,
    val mergeCount: Int = 0,
    val isBossRound: Boolean = false,
    val bossTimeRemaining: Float = 0f, // 보스 라운드 제한 시간
    val gridTiles: List<GridTileState> = List(15) { GridTileState() }, // 5x3 그리드 상태
)

enum class BattlePhase {
    IDLE,          // 배틀 시작 전
    WAVE_DELAY,    // 웨이브 사이 대기
    PLAYING,       // 웨이브 진행 중
    BOSS,          // 보스 라운드
    VICTORY,       // 승리
    DEFEAT,        // 패배
    PAUSED,        // 일시정지
}

sealed class SummonResult {
    data class Success(val unit: BattleUnit, val costPaid: Int) : SummonResult()
    data object InsufficientMinerals : SummonResult()
    data object GridFull : SummonResult()
}

/**
 * 배틀 결과 데이터 (배틀 종료 시 생성)
 */
data class BattleResult(
    val victory: Boolean,
    val waveReached: Int,
    val goldEarned: Int,
    val trophyChange: Int,
    val killCount: Int,
    val mergeCount: Int,
    val cardsEarned: Int = 0,
    val perfectWin: Boolean = false,
    val monoTypeWin: Boolean = false,
)
