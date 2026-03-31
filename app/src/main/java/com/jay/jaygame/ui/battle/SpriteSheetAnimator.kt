package com.jay.jaygame.ui.battle

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * 스프라이트 시트 애니메이터.
 * init 시 모든 srcOffset을 pre-compute하여 렌더 루프에서 allocation 0.
 *
 * 시트 레이아웃: 행(row) = 애니메이션 상태, 열(col) = 프레임
 *   Row 0 = idle, Row 1 = walk, Row 2 = hit, Row 3 = die
 */
class SpriteSheetAnimator(
    val sheet: ImageBitmap,
    val cols: Int,
    val rows: Int,
    val frameWidth: Int,
    val frameHeight: Int,
) {
    // pre-computed src offsets for every (row, col) — zero allocation in draw loop
    private val srcOffsets: Array<IntOffset> = Array(rows * cols) { idx ->
        val col = idx % cols
        val row = idx / cols
        IntOffset(col * frameWidth, row * frameHeight)
    }
    private val srcSizeVal = IntSize(frameWidth, frameHeight)

    /** 해당 상태(행)와 프레임(열)의 src 오프셋 반환. */
    fun srcOffset(state: Int, frame: Int): IntOffset {
        val clampedState = state.coerceIn(0, rows - 1)
        val clampedFrame = frame.coerceIn(0, cols - 1)
        return srcOffsets[clampedState * cols + clampedFrame]
    }

    fun srcSize(): IntSize = srcSizeVal

    /** 열 수 (각 행의 프레임 수). */
    fun frameCount(): Int = cols

    companion object {
        // 애니메이션 상태 상수
        const val STATE_IDLE = 0
        const val STATE_WALK = 1
        const val STATE_HIT = 2
        const val STATE_DIE = 3

        // 상태별 프레임 지속 시간 (초)
        const val IDLE_FRAME_DUR = 0.25f
        const val WALK_FRAME_DUR = 0.15f
        const val HIT_FRAME_DUR = 0.06f
        const val DIE_FRAME_DUR = 0.12f

        fun frameDuration(state: Int): Float = when (state) {
            STATE_IDLE -> IDLE_FRAME_DUR
            STATE_WALK -> WALK_FRAME_DUR
            STATE_HIT -> HIT_FRAME_DUR
            STATE_DIE -> DIE_FRAME_DUR
            else -> WALK_FRAME_DUR
        }
    }
}

/**
 * 적 개별 애니메이션 상태. 256개 pre-allocated, GC-free.
 */
internal class EnemyAnimState {
    var state: Int = SpriteSheetAnimator.STATE_WALK
    var frame: Int = 0
    var frameTimer: Float = 0f
    var stateTimer: Float = 0f

    fun reset() {
        state = SpriteSheetAnimator.STATE_WALK
        frame = 0
        frameTimer = 0f
        stateTimer = 0f
    }

    fun transition(newState: Int) {
        if (state == newState) return
        state = newState
        frame = 0
        frameTimer = 0f
        stateTimer = 0f
    }

    /**
     * 프레임 진행. battleSpeed 반영: 4x 이상이면 프레임 스킵.
     * @return true if animation ended (die 상태에서 마지막 프레임 도달)
     */
    fun advance(dt: Float, maxFrames: Int, battleSpeed: Float): Boolean {
        stateTimer += dt
        frameTimer += dt

        val dur = SpriteSheetAnimator.frameDuration(state)
        // 4x+ 배속에서 프레임 지속 시간 절반 (더 빠르게 재생)
        val effectiveDur = if (battleSpeed >= 4f) dur * 0.5f else dur

        if (frameTimer >= effectiveDur) {
            frameTimer -= effectiveDur
            frame++
            if (state == SpriteSheetAnimator.STATE_DIE) {
                if (frame >= maxFrames) {
                    frame = maxFrames - 1 // 마지막 프레임에서 멈춤
                    return true
                }
            } else {
                frame %= maxFrames // 루프
            }
        }
        return false
    }
}
