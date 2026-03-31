package com.jay.jaygame.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Centralised haptic feedback helper.
 *
 * Three intensity tiers that map to Android HapticFeedbackConstants:
 *   light  – subtle tap  (merge, button click)
 *   medium – standard    (summon, level up)
 *   heavy  – strong      (boss appear, crit, jackpot)
 */
object HapticManager {

    private var enabled = true

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    /** Subtle tick – merge, ordinary button tap. */
    fun light(view: View) {
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /** Medium buzz – summon, level up. */
    fun medium(view: View) {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /** Strong thud – boss appear, critical hit, jackpot. */
    fun heavy(view: View) {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
