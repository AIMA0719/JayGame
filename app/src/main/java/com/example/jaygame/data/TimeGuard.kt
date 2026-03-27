package com.example.jaygame.data

import android.os.SystemClock
import android.util.Log

/**
 * 시간 조작 감지. 시스템 시계 전진(>24시간)/후진(>5분) 감지.
 * 감지 시 시간 기반 보상 거부 (데이터 삭제 아님).
 */
object TimeGuard {
    private const val MAX_FORWARD_JUMP_MS = 24 * 60 * 60 * 1000L  // 24시간
    private const val MAX_BACKWARD_TOLERANCE_MS = 5 * 60 * 1000L   // 5분 (NTP 보정 허용)

    private var sessionStartSystemTime: Long = 0L
    private var sessionStartElapsed: Long = 0L

    fun onSessionStart() {
        sessionStartSystemTime = System.currentTimeMillis()
        sessionStartElapsed = SystemClock.elapsedRealtime()
    }

    /** 마지막 알려진 시스템 시간 대비 현재 시간이 조작되었는지 확인 */
    fun isTimeManipulated(lastKnownSystemTime: Long): Boolean {
        if (lastKnownSystemTime <= 0L) return false // 첫 실행
        val now = System.currentTimeMillis()
        if (now < lastKnownSystemTime - MAX_BACKWARD_TOLERANCE_MS) {
            Log.w("TimeGuard", "Clock went backward by ${(lastKnownSystemTime - now) / 1000}s")
            return true
        }
        if (now > lastKnownSystemTime + MAX_FORWARD_JUMP_MS) {
            Log.w("TimeGuard", "Clock jumped forward by ${(now - lastKnownSystemTime) / 3600000}h")
            return true
        }
        return false
    }

    /** 앱 resume 시 elapsedRealtime과 시스템 시계의 drift 감지 */
    fun detectResumeManipulation(): Boolean {
        if (sessionStartSystemTime <= 0L) return false
        val elapsedDelta = SystemClock.elapsedRealtime() - sessionStartElapsed
        val systemDelta = System.currentTimeMillis() - sessionStartSystemTime
        val drift = kotlin.math.abs(systemDelta - elapsedDelta)
        if (drift > MAX_BACKWARD_TOLERANCE_MS) {
            Log.w("TimeGuard", "Clock drift ${drift / 1000}s detected on resume")
            return true
        }
        return false
    }
}
